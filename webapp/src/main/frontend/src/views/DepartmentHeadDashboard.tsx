import React, { useCallback, useEffect, useMemo, useState } from 'react';
import globalAxios from 'axios';
import { BASE_PATH } from '../generated-skeleton-api/base';

import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import { RoomListTable, RoomData } from '../components/RoomListTable';
import { ThresholdViolationsTable } from '../components/ThresholdViolationsTable';
import { PendingRequestsTable } from '../components/PendingRequestsTable';
import { NumberOfViolationsTable } from '../components/NumberOfViolationsTable';

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

export const DepartmentHeadDashboard: React.FC = () => {
    const [sidebarVisible, setSidebarVisible] = useState(false);

    const [rooms, setRooms] = useState<RoomData[]>([]);
    const [departmentName, setDepartmentName] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const fetchRoomWithLiveData = useCallback((room: RoomDTO): Promise<RoomData> => {
        return Promise.all([
            globalAxios.get<ClimateData>(`${BASE_PATH}/api/rooms/${room.id}/current-climate`)
                .then(response => response.data)
                .catch(() => null),

            globalAxios.get<ActiveWarning[]>(`${BASE_PATH}/api/warnings/rooms/${room.id}/violations`)
                .then(response => extractArrayResponse<ActiveWarning>(response.data))
                .catch(() => [])
        ]).then(([climate, warnings]) => {
            return mapToRoomData(room, climate, warnings);
        });
    }, []);

    const fetchDepartmentRooms = useCallback(() => {
        setLoading(true);
        setError(null);

        globalAxios.get(`${BASE_PATH}/api/users/me/department/rooms`)
            .then(response => {
                const apiRooms = extractArrayResponse<RoomDTO>(response.data);

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
                    return;
                }

                return Promise.all(apiRooms.map(fetchRoomWithLiveData))
                    .then(roomData => {
                        setRooms(roomData);
                    });
            })
            .catch(error => {
                console.error(
                    'Could not load department rooms',
                    error.response?.status,
                    error.response?.data || error
                );

                setRooms([]);
                setError('Department rooms could not be loaded.');
            })
            .finally(() => {
                setLoading(false);
            });
    }, [fetchRoomWithLiveData]);

    useEffect(() => {
        fetchDepartmentRooms();

        const interval = window.setInterval(fetchDepartmentRooms, 30_000);

        return () => window.clearInterval(interval);
    }, [fetchDepartmentRooms]);

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

                <ThresholdViolationsTable />

                <PendingRequestsTable />

                <NumberOfViolationsTable />
            </div>
        </div>
    );
};

export default DepartmentHeadDashboard;