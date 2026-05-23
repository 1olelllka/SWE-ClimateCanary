import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import { AbsenceControllerApi, AggregatedDataPointDTO, ClimateStatsControllerApi, DepartmentControllerApi, RoomControllerApi, UserxControllerApi, WarningControllerApi } from '../generated-skeleton-api';

import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import { RoomListTable, RoomData } from '../components/RoomListTable';
import { ThresholdViolationData } from '../components/ThresholdViolationsTable';
import {PendingRequestsTable, PendingRequestData} from '../components/PendingRequestsTable';
import { RoomViolationsBarChart } from '../components/RoomViolationsBarChart';
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

const getMeasurementUnit = (measurementType?: string): string => {
    if (measurementType === 'TEMPERATURE') return '°C';
    if (measurementType === 'HUMIDITY') return '%';
    if (measurementType === 'AIR') return 'ppm';
    return '';
};

const formatSensorName = (measurementType?: string): string => {
    if (measurementType === 'TEMPERATURE') return 'Temperature';
    if (measurementType === 'HUMIDITY') return 'Humidity';
    if (measurementType === 'AIR') return 'CO₂';
    return measurementType ?? 'Unknown';
};

const formatDatetime = (dateString?: string): string => {
    if (!dateString) return 'n/a';
    const d = new Date(dateString);
    const p = (n: number) => String(n).padStart(2, '0');
    return `${p(d.getDate())}.${p(d.getMonth() + 1)}.${d.getFullYear()} ${p(d.getHours())}:${p(d.getMinutes())}`;
};

const formatViolationNumber = (
    value: number | null | undefined,
    measurementType?: string
): string => {
    if (value === null || value === undefined || Number.isNaN(value)) return 'n/a';
    const unit = getMeasurementUnit(measurementType);
    if (measurementType === 'AIR') return `${value.toFixed(0)} ${unit}`.trim();
    return `${value.toFixed(1).replace('.', ',')} ${unit}`.trim();
};

const mapWarningToThresholdViolation = (
    warning: WarningDTO,
    roomLabel: string,
): ThresholdViolationData => {
    const isMin = warning.triggeredValue < warning.activeLimitAtTime;
    return {
        id:           warning.id,
        status:       warning.status,
        active:       warning.active,
        sensor:       formatSensorName(warning.measurementType),
        room:         roomLabel,
        limit:        `${isMin ? 'Min' : 'Max'}: ${formatViolationNumber(warning.activeLimitAtTime, warning.measurementType)}`,
        measuredValue: formatViolationNumber(warning.triggeredValue, warning.measurementType),
        datetime:     formatDatetime(warning.createdAt),
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


export const DepartmentHeadDashboard: React.FC = () => {
    const navigate = useNavigate();
    const [sidebarVisible, setSidebarVisible] = useState(false);

    const [rooms, setRooms] = useState<RoomData[]>([]);
    const [roomBackendIds, setRoomBackendIds] = useState<string[]>([]);
    const stompClient = useRef<Client | null>(null);
    const [thresholdViolations, setThresholdViolations] = useState<ThresholdViolationData[]>([]);
    const [pendingRequests, setPendingRequests] = useState<PendingRequestData[]>([]);

    const [departmentName, setDepartmentName] = useState<string | null>(null);
    const [departmentId, setDepartmentId] = useState<string | null>(null);
    const [deptAggregation, setDeptAggregation] = useState<AggregatedDataPointDTO | null>(null);
    const [loading, setLoading] = useState(true);
    const [violationsLoading, setViolationsLoading] = useState(false);
    const [pendingRequestsLoading, setPendingRequestsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const fetchRoomWithLiveData = useCallback((room: RoomDTO): Promise<RoomData> => {
        return Promise.all([
            new RoomControllerApi().getCurrentClimate({ roomId: room.id! })
                .then(response => response.data as ClimateData)
                .catch(() => null),
            new WarningControllerApi().getWarningsForRoom({
                    roomId: room.id!,
                    activeOnly: true,
                    startDate: '2000-01-01',
                    endDate: new Date().toISOString().slice(0, 10),
                })
                .then(response => extractArrayResponse<ActiveWarning>(response.data))
                .catch(() => [])
        ]).then(([climate, warnings]) => {
            const isStale = climate !== null
                && (Date.now() - new Date(climate.timestamp).getTime()) > 5 * 60 * 1000;
            return mapToRoomData(room, isStale ? null : climate, warnings);
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
            roomsWithBackendId.map(room =>
                new WarningControllerApi().getWarningsForRoom({
                        roomId: room.backendId!,
                        activeOnly: false,
                        startDate: '2000-01-01',
                        endDate: new Date().toISOString().slice(0, 10),
                    })
                    .then(response => {
                        const warnings = Array.isArray(response.data)
                            ? response.data as WarningDTO[]
                            : [];

                        return warnings.map(warning =>
                            mapWarningToThresholdViolation(warning, room.id)
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
                    const parse = (s: string) => {
                        const [d, t = '00:00'] = s.split(' ');
                        const [dd, mm, yyyy] = d.split('.');
                        const [hh, min] = t.split(':');
                        return new Date(+yyyy, +mm - 1, +dd, +hh, +min).getTime();
                    };
                    return parse(b.datetime) - parse(a.datetime);
                });

                setThresholdViolations(flattened);
            })
            .finally(() => {
                setViolationsLoading(false);
            });
    }, []);

    const fetchPendingRequests = useCallback(() => {
        setPendingRequestsLoading(true);

        new AbsenceControllerApi().getAllAbsences({ pageable: { page: 0, size: 100, sort: [] } })
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

    const fetchDeptAggregation = useCallback((deptId: string) => {
        new ClimateStatsControllerApi()
            .getLastAggregationForDepartment({ id: deptId })
            .then(r => setDeptAggregation(r.data as AggregatedDataPointDTO))
            .catch(() => setDeptAggregation(null));
    }, []);

    const fetchDepartmentRooms = useCallback(() => {
        setLoading(true);
        setError(null);

        new UserxControllerApi().getAuthenticatedUser()
            .then(response => {
                const id = (response.data as UserxDTO).myRoom?.departmentID;
                if (!id) throw new Error("User has no department assigned.");
                setDepartmentId(id);
                fetchDeptAggregation(id);
                return id;
            })
            .then(id => new DepartmentControllerApi().getSpecificDepartment({ departmentId: id }))
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
                        setRoomBackendIds(roomData.map(r => r.backendId!).filter(Boolean));
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
    }, [fetchRoomWithLiveData, fetchThresholdViolations, fetchDeptAggregation]);

    useEffect(() => {
        fetchDepartmentRooms();
        fetchPendingRequests();

        const interval = window.setInterval(() => {
            fetchPendingRequests();
        }, 30_000);

        return () => window.clearInterval(interval);
    }, [fetchDepartmentRooms, fetchPendingRequests]);

    useEffect(() => {
        if (roomBackendIds.length === 0) return;

        stompClient.current?.deactivate();

        stompClient.current = new Client({
            webSocketFactory: () => new SockJS('http://localhost:8080/active-warnings'),
            reconnectDelay: 5000,
            onConnect: () => {
                roomBackendIds.forEach(backendId => {
                    stompClient.current?.subscribe(`/topic/climate-data/${backendId}`, (message) => {
                        const data: ClimateData = JSON.parse(message.body);
                        setRooms(prev => prev.map(r => {
                            if (r.backendId !== backendId) return r;
                            return {
                                ...r,
                                temp:     data.temperature != null ? `${formatNumber(data.temperature, 1)} °C` : r.temp,
                                humidity: data.humidity    != null ? `${formatNumber(data.humidity, 0)} %`     : r.humidity,
                                co2:      data.airQuality  != null ? `${formatNumber(data.airQuality, 0)} ppm` : r.co2,
                            };
                        }));
                    });
                });
            },
            onStompError: (frame) => { console.error('STOMP error:', frame); },
        });

        stompClient.current.activate();

        return () => { stompClient.current?.deactivate(); };
    }, [roomBackendIds]);

    const problemRoomsCount = useMemo(() => {
        return rooms.filter(room => room.status === 'red' || room.status === 'yellow').length;
    }, [rooms]);

    return (
        <div className="dashboard-layout">
            <PageHeader
                title={'Department Overview'}
                subtitle={departmentName ?? undefined}
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
                        <h4>Average Temperature</h4>
                        <h2>{loading ? '…' : deptAggregation?.avgTemperature != null ? `${deptAggregation.avgTemperature.toFixed(1).replace('.', ',')} °C` : 'n/a'}</h2>
                    </div>

                    <div className="kpi-card">
                        <h4>Average Air Quality</h4>
                        <h2>{loading ? '…' : deptAggregation?.avgAirQuality != null ? `${Math.round(deptAggregation.avgAirQuality)} ppm` : 'n/a'}</h2>
                    </div>

                    <div className="kpi-card">
                        <h4>Average Humidity</h4>
                        <h2>{loading ? '…' : deptAggregation?.avgHumidity != null ? `${Math.round(deptAggregation.avgHumidity)} %` : 'n/a'}</h2>
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

                <RoomListTable
                    rooms={rooms}
                    onRowClick={(displayId) => {
                        const room = rooms.find(r => r.id === displayId);
                        if (!room?.backendId) return;
                        const params = new URLSearchParams();
                        params.set('name', displayId);
                        params.set('shared', String(room.type === 'Common Area'));
                        navigate(`/senior/department/${encodeURIComponent(room.backendId)}?${params.toString()}`);
                    }}
                />

                <PendingRequestsTable
                    requests={pendingRequests}
                    loading={pendingRequestsLoading}
                />

                <RoomViolationsBarChart
                    violations={thresholdViolations}
                    loading={violationsLoading}
                />
            </div>
        </div>
    );
};

export default DepartmentHeadDashboard;