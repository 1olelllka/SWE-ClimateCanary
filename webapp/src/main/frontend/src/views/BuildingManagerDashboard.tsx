import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import globalAxios from 'axios';

import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import { RoomListTable, RoomData } from '../components/RoomListTable';

import { InputText } from 'primereact/inputtext';
import { Dropdown } from 'primereact/dropdown';
import { Checkbox } from 'primereact/checkbox';

import '../styles/BuildingManagerDashboard.css';

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

const getDepartmentName = (room: RoomDTO) => {
    return room.departmentName || room.department?.name || 'Unknown';
};

const getRoomDisplayId = (room: RoomDTO) => {
    return room.roomNumber || room.name || room.id;
};

const formatRoomType = (type?: string) => {
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
) => {
    if (value === null || value === undefined || Number.isNaN(value)) {
        return fallback;
    }

    return value.toFixed(decimals).replace('.', ',');
};

const getPeopleLabel = (room: RoomDTO) => {
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
    const hasActiveWarning = warnings.some(warning => warning.active !== false);

    if (hasActiveWarning) {
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
        department: getDepartmentName(room),
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

export const BuildingManagerDashboard: React.FC = () => {
    const [sidebarVisible, setSidebarVisible] = useState(false);

    const [rooms, setRooms] = useState<RoomData[]>([]);
    const [globalFilter, setGlobalFilter] = useState('');
    const [selectedDepartment, setSelectedDepartment] = useState<string | null>(null);
    const [showOnlyViolations, setShowOnlyViolations] = useState(false);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const navigate = useNavigate();

    const fetchRoomWithLiveData = useCallback((room: RoomDTO): Promise<RoomData> => {
        return Promise.all([
            globalAxios.get<ClimateData>(`/api/rooms/${room.id}/current-climate`)
                .then(response => response.data)
                .catch(() => null),
            globalAxios.get<ActiveWarning[]>(`/api/warnings/rooms/${room.id}`, {
                params: {
                    activeOnly: false,
                    startDate: '2000-01-01',
                    endDate: new Date().toISOString().slice(0, 10),
                },
            }).then(response => response.data)
                .catch(() => [])
        ]).then(([climate, warnings]) => {
            return mapToRoomData(room, climate, warnings ?? []);
        });
    }, []);

    const fetchBuildingRooms = useCallback(() => {
        setLoading(true);
        setError(null);

        globalAxios.get('/api/rooms')
            .then(response => {
                const apiRooms = extractArrayResponse<RoomDTO>(response.data);

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
                    'Could not load building rooms',
                    error.response?.status,
                    error.response?.data || error
                );
                setRooms([]);
                setError('Building rooms could not be loaded.');
            })
            .finally(() => {
                setLoading(false);
            });
    }, [fetchRoomWithLiveData]);

    useEffect(() => {
        fetchBuildingRooms();

        const interval = window.setInterval(fetchBuildingRooms, 30_000);

        return () => window.clearInterval(interval);
    }, [fetchBuildingRooms]);

    const departments = useMemo(() => {
        return Array.from(
            new Set(
                rooms
                    .map(room => room.department)
                    .filter((department): department is string => Boolean(department))
            )
        );
    }, [rooms]);

    const activeViolationsCount = useMemo(() => {
        return rooms.filter(room => room.status === 'red').length;
    }, [rooms]);

    const filteredRooms = useMemo(() => {
        const normalizedSearch = globalFilter.trim().toLowerCase();

        return rooms.filter(room => {
            if (showOnlyViolations && room.status !== 'red') {
                return false;
            }

            if (selectedDepartment && room.department !== selectedDepartment) {
                return false;
            }

            if (
                normalizedSearch &&
                !room.id.toLowerCase().includes(normalizedSearch) &&
                !(room.department || '').toLowerCase().includes(normalizedSearch) &&
                !room.type.toLowerCase().includes(normalizedSearch)
            ) {
                return false;
            }

            return true;
        });
    }, [rooms, globalFilter, selectedDepartment, showOnlyViolations]);

    const handleSettingsClick = (roomId: string) => {
        const room = rooms.find(candidate => candidate.id === roomId);
        const backendId = (room as any)?.backendId || roomId;

        navigate(`/building-room-analysis/${backendId}`);
    };

    return (
        <div className="dashboard-layout">
            <PageHeader
                title="Building Overview"
                onMenuClick={() => setSidebarVisible(true)}
            />

            <SidebarComponent
                visible={sidebarVisible}
                onHide={() => setSidebarVisible(false)}
            />

            <main className="dashboard-content building-manager-dashboard">
                <section className="building-manager-kpi-row">
                    <div className="building-manager-kpi-card">
                        <h4>Active Violations</h4>
                        <h2>{loading ? '…' : activeViolationsCount}</h2>
                    </div>
                </section>

                <section className="table-container building-manager-room-list-card">
                    <div className="building-manager-table-header">
                        <h2>Room list</h2>

                        <div className="building-manager-table-controls">
                            <span className="p-input-icon-left building-manager-search-wrapper">
                                <i className="pi pi-search" />

                                <InputText
                                    value={globalFilter}
                                    onChange={event => setGlobalFilter(event.target.value)}
                                    placeholder="Search for a Room"
                                    className="building-manager-search-input"
                                />
                            </span>

                            <Dropdown
                                value={selectedDepartment}
                                options={departments}
                                onChange={event => setSelectedDepartment(event.value)}
                                placeholder="Department Filter"
                                showClear
                                className="building-manager-department-dropdown"
                            />
                        </div>
                    </div>

                    <div className="building-manager-checkbox-row">
                        <Checkbox
                            inputId="violations"
                            checked={showOnlyViolations}
                            onChange={event => setShowOnlyViolations(Boolean(event.checked))}
                        />

                        <label htmlFor="violations">
                            Show only rooms with active violations
                        </label>
                    </div>

                    {loading && (
                        <p className="building-manager-info-text">
                            Loading rooms...
                        </p>
                    )}

                    {error && (
                        <p className="building-manager-error-text">
                            {error}
                        </p>
                    )}

                    {!loading && !error && filteredRooms.length === 0 && (
                        <p className="building-manager-info-text">
                            No rooms found.
                        </p>
                    )}

                    {!error && filteredRooms.length > 0 && (
                        <RoomListTable
                            rooms={filteredRooms}
                            showDepartment={true}
                            showSettings={true}
                            onSettingsClick={handleSettingsClick}
                        />
                    )}
                </section>
            </main>
        </div>
    );
};

export default BuildingManagerDashboard;