import React, { useCallback, useEffect, useMemo, useState } from 'react';
import globalAxios from 'axios';
import { BASE_PATH } from '../generated-skeleton-api/base';

import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import { RoomListTable, RoomData } from '../components/RoomListTable';
import {ThresholdViolationsTable, ThresholdViolationData} from '../components/ThresholdViolationsTable';
import {PendingRequestsTable, PendingRequestData} from '../components/PendingRequestsTable';
import {NumberOfViolationsTable, ViolationStatsData} from '../components/NumberOfViolationsTable';
import { UserxDTO } from '../generated-skeleton-api';
interface RoomDTO {
    id: string;
    departmentID?: string;
    departmentName?: string;
    department?: {
        id?: string;
        name?: string;
    };
    isActive?: boolean;
    roomType?: 'OFFICE' | 'SHARED' | string;
    defaultPeopleCount?: number | null;
    roomNumber?: string | null;
    name?: string | null;
}

interface ClimateData {
    timestamp: string;
    temperature?: number | null;
    humidity?: number | null;
    airQuality?: number | null;
}

interface ActiveWarning {
    id: string;
    measurementType?: string;
    message?: string;
    active?: boolean;
    status?: string;
    warningStatus?: string;
    violationStatus?: string;
}

interface WarningDTO {
    id: string;
    roomId: string;
    deviceName?: string;
    measurementType: string;
    status: 'GREEN' | 'YELLOW' | 'RED' | string;
    message: string;
    triggeredValue: number;
    activeLimitAtTime: number;
    createdAt: string;
    resolvedAt?: string | null;
    active: boolean;
}

interface AbsenceListDTO {
    id: string;
    userId: string;
    firstName: string;
    lastName: string;
    roomNumber?: string | null;
    startDate: string;
    endDate: string;
    typeOfAbsence: string;
    status: string;
    createdAt: string;
}

const extractArrayResponse = <T,>(responseData: unknown): T[] => {
    const maybePaged = responseData as { content?: unknown };

    if (Array.isArray(maybePaged?.content)) {
        return maybePaged.content as T[];
    }

    if (Array.isArray(responseData)) {
        return responseData as T[];
    }

    return [];
};

const getRoomDisplayId = (room: RoomDTO): string => {
    return room.roomNumber || room.name || room.id;
};

const formatRoomType = (type?: string): string => {
    if (!type) {
        return 'Room';
    }

    if (type === 'OFFICE') {
        return 'Bureau';
    }

    if (type === 'SHARED') {
        return 'Common Area';
    }

    return type
        .toLowerCase()
        .replace(/_/g, ' ')
        .replace(/\b\w/g, char => char.toUpperCase());
};

const formatNumber = (
    value: number | null | undefined,
    decimals: number,
    fallback = 'n/a'
): string => {
    if (value === null || value === undefined || Number.isNaN(value)) {
        return fallback;
    }

    return value.toFixed(decimals).replace('.', ',');
};

const getPeopleLabel = (room: RoomDTO): string => {
    const roomAsAny = room as any;

    const currentPeople =
        roomAsAny.peopleCount ??
        roomAsAny.currentPeopleCount ??
        roomAsAny.occupancy ??
        roomAsAny.currentOccupancy ??
        null;

    const maxPeople =
        room.defaultPeopleCount ??
        roomAsAny.maxPeopleCount ??
        roomAsAny.capacity ??
        null;

    if (currentPeople !== null && currentPeople !== undefined) {
        return `${currentPeople}/${maxPeople ?? '--'}`;
    }

    return `0/${maxPeople ?? '--'}`;
};

const getRoomStatus = (
    climate: ClimateData | null,
    warnings: ActiveWarning[]
): RoomData['status'] => {
    const hasRedWarning = warnings.some(warning =>
        warning.active !== false &&
        (
            warning.status === 'RED' ||
            warning.warningStatus === 'RED' ||
            warning.violationStatus === 'RED'
        )
    );

    if (hasRedWarning) {
        return 'red';
    }

    const hasYellowWarning = warnings.some(warning =>
        warning.active !== false &&
        (
            warning.status === 'YELLOW' ||
            warning.warningStatus === 'YELLOW' ||
            warning.violationStatus === 'YELLOW'
        )
    );

    if (hasYellowWarning) {
        return 'yellow';
    }

    const hasAnyActiveWarning = warnings.some(warning => warning.active !== false);

    if (hasAnyActiveWarning) {
        return 'red';
    }

    if (!climate) {
        return 'gray';
    }

    return 'green';
};

const mapToRoomData = (
    room: RoomDTO,
    climate: ClimateData | null,
    warnings: ActiveWarning[]
): RoomData => {
    return {
        id: getRoomDisplayId(room),
        backendId: room.id,
        type: formatRoomType(room.roomType),
        people: getPeopleLabel(room),
        co2: climate?.airQuality !== null && climate?.airQuality !== undefined
            ? `${formatNumber(climate.airQuality, 0)} ppm`
            : 'n/a',
        temp: climate?.temperature !== null && climate?.temperature !== undefined
            ? `${formatNumber(climate.temperature, 1)} °C`
            : 'n/a',
        humidity: climate?.humidity !== null && climate?.humidity !== undefined
            ? `${formatNumber(climate.humidity, 0)} %`
            : 'n/a',
        status: getRoomStatus(climate, warnings)
    } as RoomData;
};

const formatMeasurementType = (measurementType?: string): string => {
    if (!measurementType) {
        return 'Warning';
    }

    if (measurementType === 'TEMPERATURE') {
        return 'Temperature';
    }

    if (measurementType === 'HUMIDITY') {
        return 'Humidity';
    }

    if (measurementType === 'AIR') {
        return 'Air Quality';
    }

    return measurementType
        .toLowerCase()
        .replace(/_/g, ' ')
        .replace(/\b\w/g, char => char.toUpperCase());
};

const getMeasurementUnit = (measurementType?: string): string => {
    if (measurementType === 'TEMPERATURE') {
        return '°C';
    }

    if (measurementType === 'HUMIDITY') {
        return '%';
    }

    if (measurementType === 'AIR') {
        return 'ppm';
    }

    return '';
};

const formatDate = (dateString?: string): string => {
    if (!dateString) {
        return 'n/a';
    }

    return new Date(dateString).toLocaleDateString('de-DE');
};

const formatViolationNumber = (
    value: number | null | undefined,
    measurementType?: string
): string => {
    if (value === null || value === undefined || Number.isNaN(value)) {
        return 'n/a';
    }

    const unit = getMeasurementUnit(measurementType);

    if (measurementType === 'AIR') {
        return `${value.toFixed(0)} ${unit}`.trim();
    }

    return `${value.toFixed(1).replace('.', ',')} ${unit}`.trim();
};

const mapWarningToThresholdViolation = (
    warning: WarningDTO,
    roomLabel: string,
    roomType: string
): ThresholdViolationData => {
    const measurementLabel = formatMeasurementType(warning.measurementType);

    return {
        id: warning.id,
        warning: warning.message || `${measurementLabel} ${warning.status}`,
        room: roomLabel,
        type: roomType,
        max: formatViolationNumber(warning.activeLimitAtTime, warning.measurementType),
        real: formatViolationNumber(warning.triggeredValue, warning.measurementType),
        date: formatDate(warning.createdAt)
    };
};

const formatAbsenceDateRange = (
    startDate?: string,
    endDate?: string
): string => {
    if (!startDate || !endDate) {
        return 'n/a';
    }

    const start = new Date(startDate).toLocaleDateString('de-DE');
    const end = new Date(endDate).toLocaleDateString('de-DE');

    return `${start} - ${end}`;
};

const formatAbsenceReason = (reason?: string): string => {
    if (!reason) {
        return 'n/a';
    }

    return reason
        .toLowerCase()
        .replace(/_/g, ' ')
        .replace(/\b\w/g, char => char.toUpperCase());
};

const mapAbsenceToPendingRequest = (
    absence: AbsenceListDTO
): PendingRequestData => {
    return {
        id: absence.id,
        first: absence.firstName,
        last: absence.lastName,
        room: absence.roomNumber || 'n/a',
        date: formatAbsenceDateRange(absence.startDate, absence.endDate),
        reason: formatAbsenceReason(absence.typeOfAbsence)
    };
};

const buildViolationStats = (
    rooms: RoomData[],
    violations: ThresholdViolationData[]
): ViolationStatsData[] => {
    return rooms.map(room => {
        const roomViolations = violations.filter(
            violation => violation.room === room.id
        );

        const lastViolation = roomViolations.length > 0
            ? roomViolations
                .map(violation => violation.date)
                .sort((a, b) => {
                    const dateA = new Date(a.split('.').reverse().join('-')).getTime();
                    const dateB = new Date(b.split('.').reverse().join('-')).getTime();

                    return dateB - dateA;
                })[0]
            : '-';

        return {
            room: room.id,
            type: room.type,
            violationsCount: roomViolations.length,
            lastViolation
        };
    });
};

export const DepartmentHeadDashboard: React.FC = () => {
    const [sidebarVisible, setSidebarVisible] = useState(false);

    const [rooms, setRooms] = useState<RoomData[]>([]);
    const [thresholdViolations, setThresholdViolations] = useState<ThresholdViolationData[]>([]);
    const [pendingRequests, setPendingRequests] = useState<PendingRequestData[]>([]);

    const [departmentName, setDepartmentName] = useState<string | null>(null);
    const [departmentId, setDepartmentId] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);
    const [violationsLoading, setViolationsLoading] = useState(false);
    const [pendingRequestsLoading, setPendingRequestsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const fetchRoomWithLiveData = useCallback((room: RoomDTO): Promise<RoomData> => {
        return Promise.all([
            globalAxios.get<ClimateData>(`${BASE_PATH}/api/rooms/${room.id}/current-climate`)
                .then(response => response.data)
                .catch(() => null),
            // TODO: CHANGE THIS, just testing...
            globalAxios.get<ActiveWarning[]>(`${BASE_PATH}/api/warnings/rooms/${room.id}?activeOnly=true&startDate=2025-04-04&endDate=2025-04-04`)
                .then(response => extractArrayResponse<ActiveWarning>(response.data))
                .catch(() => [])
        ]).then(([climate, warnings]) => {
            return mapToRoomData(room, climate, warnings);
        });
    }, []);

    const fetchThresholdViolations = useCallback((roomData: RoomData[]) => {
        setViolationsLoading(true);

        const roomsWithBackendId = roomData.filter(room => Boolean(room.backendId));

        if (roomsWithBackendId.length === 0) {
            setThresholdViolations([]);
            setViolationsLoading(false);
            return;
        }

        Promise.all(
            // TODO: CHANGE THIS, just testing...
            roomsWithBackendId.map(room =>
                globalAxios.get<WarningDTO[]>(`${BASE_PATH}/api/warnings/rooms/${room.backendId}?activeOnly=false&startDate=2025-04-04&endDate=2025-04-04`)
                    .then(response => {
                        const warnings = Array.isArray(response.data)
                            ? response.data
                            : [];

                        return warnings.map(warning =>
                            mapWarningToThresholdViolation(
                                warning,
                                room.id,
                                room.type
                            )
                        );
                    })
                    .catch(error => {
                        console.warn(
                            'Could not load violations for room',
                            room.backendId,
                            error.response?.status
                        );

                        return [];
                    })
            )
        )
            .then(results => {
                const flattened = results.flat();

                flattened.sort((a, b) => {
                    const dateA = new Date(a.date.split('.').reverse().join('-')).getTime();
                    const dateB = new Date(b.date.split('.').reverse().join('-')).getTime();

                    return dateB - dateA;
                });

                setThresholdViolations(flattened);
            })
            .finally(() => {
                setViolationsLoading(false);
            });
    }, []);

    const fetchPendingRequests = useCallback(() => {
        setPendingRequestsLoading(true);

        globalAxios.get(`${BASE_PATH}/api/absences?page=0&size=100`)
            .then(response => {
                const absences = extractArrayResponse<AbsenceListDTO>(response.data);

                const pending = absences
                    .filter(absence => absence.status === 'PENDING')
                    .map(mapAbsenceToPendingRequest);

                setPendingRequests(pending);
            })
            .catch(error => {
                console.warn(
                    'Could not load pending absence requests',
                    error.response?.status,
                    error.response?.data || error
                );

                setPendingRequests([]);
            })
            .finally(() => {
                setPendingRequestsLoading(false);
            });
    }, []);

    const fetchDepartmentRooms = useCallback(() => {
        setLoading(true);
        setError(null);

        globalAxios.get("/api/users/me")
            .then(response => {
                const id = (response.data as UserxDTO).myRoom?.departmentID;
                if (!id) throw new Error("User has no department assigned.");
                setDepartmentId(id);
                return id;
            })
            .then(id => globalAxios.get(`${BASE_PATH}/api/departments/${id}`))
            .then(response => {
                const apiRooms = extractArrayResponse<RoomDTO>(response.data.rooms);
                if (apiRooms.length > 0) {
                    const firstRoom = apiRooms[0];
                    const detectedDepartmentName =
                        firstRoom.departmentName ||
                        firstRoom.department?.name ||
                        'Department';
                    setDepartmentName(detectedDepartmentName);
                }
                if (apiRooms.length === 0) {
                    setRooms([]);
                    setThresholdViolations([]);
                    return;
                }
                return Promise.all(apiRooms.map(fetchRoomWithLiveData))
                    .then(roomData => {
                        setRooms(roomData);
                        fetchThresholdViolations(roomData);
                    });
            })
            .catch(error => {
                console.error(
                    'Could not load department rooms',
                    error.response?.status,
                    error.response?.data || error
                );

                setRooms([]);
                setThresholdViolations([]);
                setError('Department rooms could not be loaded.');
            })
            .finally(() => {
                setLoading(false);
            });
    }, [fetchRoomWithLiveData, fetchThresholdViolations]);

    useEffect(() => {
        fetchDepartmentRooms();
        fetchPendingRequests();

        const interval = window.setInterval(() => {
            fetchDepartmentRooms();
            fetchPendingRequests();
        }, 30_000);

        return () => window.clearInterval(interval);
    }, [fetchDepartmentRooms, fetchPendingRequests]);

    const problemRoomsCount = useMemo(() => {
        return rooms.filter(room => room.status === 'red' || room.status === 'yellow').length;
    }, [rooms]);

    const activeAlertsCount = useMemo(() => {
        return rooms.filter(room => room.status === 'red').length;
    }, [rooms]);

    const averageTemperature = useMemo(() => {
        const temperatures = rooms
            .map(room => {
                const parsed = parseFloat(
                    room.temp
                        .replace(' °C', '')
                        .replace(',', '.')
                );

                return Number.isNaN(parsed) ? null : parsed;
            })
            .filter((value): value is number => value !== null);

        if (temperatures.length === 0) {
            return 'n/a';
        }

        const average =
            temperatures.reduce((sum, value) => sum + value, 0) / temperatures.length;

        return `${average.toFixed(1).replace('.', ',')} °C`;
    }, [rooms]);

    const airQualityLabel = useMemo(() => {
        const redRooms = rooms.filter(room => room.status === 'red').length;
        const yellowRooms = rooms.filter(room => room.status === 'yellow').length;

        if (redRooms > 0) {
            return 'Critical';
        }

        if (yellowRooms > 0) {
            return 'Warning';
        }

        if (rooms.length === 0) {
            return 'n/a';
        }

        return 'Good';
    }, [rooms]);

    const violationStats = useMemo(() => {
        return buildViolationStats(rooms, thresholdViolations);
    }, [rooms, thresholdViolations]);

    return (
        <div className="dashboard-layout">
            <PageHeader
                title={departmentName ? `Overview Department ${departmentName}` : 'Department Overview'}
                onMenuClick={() => setSidebarVisible(true)}
            />

            <SidebarComponent
                visible={sidebarVisible}
                onHide={() => setSidebarVisible(false)}
            />

            <div className="dashboard-content">
                <div className="cards-grid">
                    <div className="kpi-card">
                        <h4>Problem Rooms</h4>
                        <h2>{loading ? '…' : problemRoomsCount}</h2>
                    </div>

                    <div className="kpi-card">
                        <h4>Active Alerts</h4>
                        <h2>{loading ? '…' : activeAlertsCount}</h2>
                    </div>

                    <div className="kpi-card">
                        <h4>Air Quality</h4>
                        <h2>{loading ? '…' : airQualityLabel}</h2>
                    </div>

                    <div className="kpi-card">
                        <h4>Average Temperature</h4>
                        <h2>{loading ? '…' : averageTemperature}</h2>
                    </div>
                </div>

                {loading && (
                    <p style={{ color: '#64748b', marginBottom: '1rem' }}>
                        Loading department rooms...
                    </p>
                )}

                {error && (
                    <p style={{ color: '#dc2626', marginBottom: '1rem' }}>
                        {error}
                    </p>
                )}

                {!loading && !error && rooms.length === 0 && (
                    <p style={{ color: '#64748b', marginBottom: '1rem' }}>
                        No rooms found for your department.
                    </p>
                )}

                <RoomListTable rooms={rooms} />

                <ThresholdViolationsTable
                    violations={thresholdViolations}
                    loading={violationsLoading}
                />

                <PendingRequestsTable
                    requests={pendingRequests}
                    loading={pendingRequestsLoading}
                />

                <NumberOfViolationsTable
                    stats={violationStats}
                    loading={violationsLoading}
                />
            </div>
        </div>
    );
};

export default DepartmentHeadDashboard;