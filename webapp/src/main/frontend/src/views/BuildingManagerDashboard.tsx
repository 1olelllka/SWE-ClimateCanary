import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import globalAxios from 'axios';

import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import { RoomListTable, RoomData } from '../components/RoomListTable';
import { BuildingListDTO, DepartmentListDTO } from '../generated-skeleton-api/api';
import { extractArrayResponse } from '../utilities/apiUtils';

import '../styles/BuildingManagerDashboard.css';

// ── Local types ────────────────────────────────────────────────────────────────

interface RoomDTO {
    id: string;
    departmentID?: string;
    departmentName?: string;
    department?: { id?: string; name?: string };
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
    status?: 'GREEN' | 'YELLOW' | 'RED';
}

// ── Pure helpers ───────────────────────────────────────────────────────────────

const getDepartmentName = (room: RoomDTO) =>
    room.departmentName || room.department?.name || 'Unknown';

const getRoomDisplayId = (room: RoomDTO) =>
    room.roomNumber || room.name || room.id;

const formatRoomType = (type?: string) => {
    if (!type) return 'Room';
    if (type === 'OFFICE') return 'Bureau';
    if (type === 'SHARED') return 'Common Area';
    return type.toLowerCase().replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
};

const formatNumber = (value: number | null | undefined, decimals: number, fallback = 'n/a') => {
    if (value === null || value === undefined || Number.isNaN(value)) return fallback;
    return value.toFixed(decimals).replace('.', ',');
};

const getPeopleLabel = (room: RoomDTO) => {
    const r = room as any;
    const current = r.peopleCount ?? r.currentPeopleCount ?? r.occupancy ?? r.currentOccupancy ?? null;
    const max = room.defaultPeopleCount ?? r.maxPeopleCount ?? r.capacity ?? null;
    return current !== null ? `${current}/${max ?? '--'}` : `0/${max ?? '--'}`;
};

const getRoomStatus = (warnings: ActiveWarning[]): RoomData['status'] => {
    const active = warnings.filter(w => w.active === true);
    if (active.length === 0) return 'green';
    if (active.some(w => w.status === 'RED')) return 'red';
    return 'yellow';
};

const makeInitialRow = (room: RoomDTO): RoomData => ({
    id: getRoomDisplayId(room),
    backendId: room.id,
    department: getDepartmentName(room),
    type: formatRoomType(room.roomType),
    people: getPeopleLabel(room),
    co2: 'n/a',
    temp: 'n/a',
    humidity: 'n/a',
    status: 'gray',
} as RoomData);

const mapToRoomData = (room: RoomDTO, climate: ClimateData | null, warnings: ActiveWarning[]): RoomData => ({
    id: getRoomDisplayId(room),
    backendId: room.id,
    department: getDepartmentName(room),
    type: formatRoomType(room.roomType),
    people: getPeopleLabel(room),
    co2: climate?.airQuality != null ? `${formatNumber(climate.airQuality, 0)} ppm` : 'n/a',
    temp: climate?.temperature != null ? `${formatNumber(climate.temperature, 1)} °C` : 'n/a',
    humidity: climate?.humidity != null ? `${formatNumber(climate.humidity, 0)} %` : 'n/a',
    status: getRoomStatus(warnings),
} as RoomData);

const plural = (n: number, word: string) => `${n} ${word}${n !== 1 ? 's' : ''}`;

// ── Component ──────────────────────────────────────────────────────────────────

export const BuildingManagerDashboard: React.FC = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const [sidebarVisible, setSidebarVisible] = useState(false);

    const [buildings, setBuildings] = useState<BuildingListDTO[]>([]);
    const [allDepts, setAllDepts] = useState<DepartmentListDTO[]>([]);
    const [allRooms, setAllRooms] = useState<RoomData[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const [selectedBuilding, setSelectedBuilding] = useState<BuildingListDTO | null>(null);
    const [selectedDept, setSelectedDept] = useState<DepartmentListDTO | null>(null);

    const navLevel = selectedDept ? 'rooms' : selectedBuilding ? 'departments' : 'buildings';

    // Keeps the raw DTOs so the interval can re-fetch live data without re-fetching the room list
    const roomDTOsRef = useRef<RoomDTO[]>([]);

    // ── Data fetching ──────────────────────────────────────────────────────────

    // Fetch live data for one room and update its row in state immediately
    const updateRoomLiveData = useCallback((room: RoomDTO): void => {
        Promise.all([
            globalAxios.get<ClimateData>(`/api/rooms/${room.id}/current-climate`)
                .then(r => r.data).catch(() => null),
            globalAxios.get<ActiveWarning[]>(`/api/warnings/rooms/${room.id}`, {
                params: {
                    activeOnly: true,
                    startDate: '2000-01-01',
                    endDate: new Date().toISOString().slice(0, 10),
                },
            }).then(r => r.data).catch(() => []),
        ]).then(([climate, warnings]) => {
            const roomData = mapToRoomData(room, climate, warnings ?? []);
            setAllRooms(prev => prev.map(r => r.backendId === room.id ? roomData : r));
        }).catch(() => {});
    }, []);

    // Fetch room list, show rows immediately with N/A, then update each row as live data arrives
    const loadRooms = useCallback(async (): Promise<void> => {
        const r = await globalAxios.get('/api/rooms');
        const rooms = extractArrayResponse<RoomDTO>(r.data, []);
        roomDTOsRef.current = rooms;
        setAllRooms(rooms.map(makeInitialRow));
        rooms.forEach(updateRoomLiveData);
    }, [updateRoomLiveData]);

    useEffect(() => {
        setLoading(true);
        setError(null);

        Promise.all([
            globalAxios.get('/api/buildings', { params: { size: 500 } })
                .then(r => extractArrayResponse<BuildingListDTO>(r.data, [])),
            globalAxios.get('/api/departments', { params: { size: 500 } })
                .then(r => extractArrayResponse<DepartmentListDTO>(r.data, [])),
            loadRooms(),
        ])
            .then(([blds, depts]) => {
                setBuildings(blds);
                setAllDepts(depts);
            })
            .catch(err => {
                console.error('Could not load building data', err);
                setError('Building data could not be loaded.');
            })
            .finally(() => setLoading(false));

        // On interval, only refresh live data — no N/A flash, no re-fetch of room list
        const interval = window.setInterval(() => {
            roomDTOsRef.current.forEach(updateRoomLiveData);
        }, 30_000);

        return () => window.clearInterval(interval);
    }, [loadRooms, updateRoomLiveData]);

    // Restore building/dept selection when returning from room analysis
    useEffect(() => {
        const state = location.state as { buildingId?: string; deptId?: string } | null;
        if (!state || buildings.length === 0 || allDepts.length === 0) return;
        if (state.buildingId && !selectedBuilding) {
            const b = buildings.find(b => b.id === state.buildingId);
            if (b) setSelectedBuilding(b);
        }
        if (state.deptId && !selectedDept) {
            const d = allDepts.find(d => d.id === state.deptId);
            if (d) setSelectedDept(d);
        }
    }, [location.state, buildings, allDepts]); // eslint-disable-line react-hooks/exhaustive-deps

    // ── Derived data ───────────────────────────────────────────────────────────

    const currentDepts = useMemo(
        () => selectedBuilding
            ? allDepts.filter(d => d.buildingID === selectedBuilding.id)
            : [],
        [selectedBuilding, allDepts],
    );

    const currentRooms = useMemo(
        () => selectedDept
            ? allRooms.filter(r => r.department === selectedDept.name)
            : [],
        [selectedDept, allRooms],
    );

    const getBuildingStats = useCallback((b: BuildingListDTO) => {
        const depts = allDepts.filter(d => d.buildingID === b.id);
        const deptNames = new Set(depts.map(d => d.name));
        const rooms = allRooms.filter(r => deptNames.has(r.department || ''));
        return {
            deptCount: depts.length,
            roomCount: rooms.length,
            violationCount: rooms.filter(r => r.status === 'red').length,
        };
    }, [allDepts, allRooms]);

    const getDeptStats = useCallback((d: DepartmentListDTO) => {
        const rooms = allRooms.filter(r => r.department === d.name);
        return {
            roomCount: rooms.length,
            violationCount: rooms.filter(r => r.status === 'red').length,
        };
    }, [allRooms]);

    const totalViolations = useMemo(() => {
        if (navLevel === 'rooms') return currentRooms.filter(r => r.status === 'red').length;
        if (navLevel === 'departments') {
            const names = new Set(currentDepts.map(d => d.name));
            return allRooms.filter(r => names.has(r.department || '') && r.status === 'red').length;
        }
        return allRooms.filter(r => r.status === 'red').length;
    }, [navLevel, currentRooms, currentDepts, allRooms]);

    // ── Handlers ───────────────────────────────────────────────────────────────

    const handleRoomClick = (roomId: string) => {
        const room = allRooms.find(r => r.id === roomId);
        const backendId = room?.backendId ?? roomId;
        navigate(`/building-room-analysis/${backendId}`, {
            state: { buildingId: selectedBuilding?.id, deptId: selectedDept?.id },
        });
    };

    const selectBuilding = (b: BuildingListDTO) => {
        setSelectedBuilding(b);
        setSelectedDept(null);
    };

    const goToRoot = () => {
        setSelectedBuilding(null);
        setSelectedDept(null);
    };

    // ── Render ─────────────────────────────────────────────────────────────────

    return (
        <div className="bm-page">
            <PageHeader title="Buildings Overview" onMenuClick={() => setSidebarVisible(true)} />
            <SidebarComponent visible={sidebarVisible} onHide={() => setSidebarVisible(false)} />

            <div className="bm-content">

                {/* ── Breadcrumb ── */}
                <nav className="bm-breadcrumb" aria-label="Navigation">
                    {selectedBuilding ? (
                        <button type="button" className="bm-breadcrumb-btn" onClick={goToRoot}>
                            All Buildings
                        </button>
                    ) : (
                        <span className="bm-breadcrumb-current">All Buildings</span>
                    )}

                    {selectedBuilding && (
                        <>
                            <span className="bm-breadcrumb-sep">›</span>
                            {selectedDept ? (
                                <button
                                    type="button"
                                    className="bm-breadcrumb-btn"
                                    onClick={() => setSelectedDept(null)}
                                >
                                    {selectedBuilding.name}
                                </button>
                            ) : (
                                <span className="bm-breadcrumb-current">{selectedBuilding.name}</span>
                            )}
                        </>
                    )}

                    {selectedDept && (
                        <>
                            <span className="bm-breadcrumb-sep">›</span>
                            <span className="bm-breadcrumb-current">{selectedDept.name}</span>
                        </>
                    )}
                </nav>

                {/* ── KPI row ── */}
                <p className="bm-section-heading">Overview</p>
                <div className="bm-kpi-row">
                    <div className="bm-kpi-card">
                        <div className="bm-kpi-title">Active Violations</div>
                        <div className={`bm-kpi-value${totalViolations > 0 ? ' bm-kpi-value--red' : ''}`}>
                            {loading ? '…' : totalViolations}
                        </div>
                    </div>
                    <div className="bm-kpi-card">
                        <div className="bm-kpi-title">
                            {navLevel === 'buildings' ? 'Buildings' : navLevel === 'departments' ? 'Departments' : 'Rooms'}
                        </div>
                        <div className="bm-kpi-value">
                            {loading ? '…'
                                : navLevel === 'buildings' ? buildings.length
                                : navLevel === 'departments' ? currentDepts.length
                                : currentRooms.length}
                        </div>
                    </div>
                </div>

                {error && <p className="bm-error">{error}</p>}
                {loading && <p className="bm-loading">Loading building data…</p>}

                {/* ── Level 1: Buildings ── */}
                {!loading && navLevel === 'buildings' && (
                    <>
                        <p className="bm-section-heading">Buildings</p>
                        {buildings.length === 0 ? (
                            <p className="bm-empty">No buildings found.</p>
                        ) : (
                            <div className="bm-cards-grid">
                                {buildings.map(b => {
                                    const s = getBuildingStats(b);
                                    return (
                                        <button
                                            key={b.id}
                                            type="button"
                                            className={`bm-nav-card${s.violationCount > 0 ? ' bm-nav-card--alert' : ''}`}
                                            onClick={() => selectBuilding(b)}
                                        >
                                            <div className="bm-card-header">
                                                <span className="bm-card-name">{b.name}</span>
                                                {s.violationCount > 0 && (
                                                    <span className="bm-badge bm-badge--red">
                                                        {plural(s.violationCount, 'violation')}
                                                    </span>
                                                )}
                                            </div>
                                            {b.address && <p className="bm-card-sub">{b.address}</p>}
                                            <div className="bm-card-meta">
                                                <span>{plural(s.deptCount, 'dept')}</span>
                                                <span className="bm-meta-sep">·</span>
                                                <span>{plural(s.roomCount, 'room')}</span>
                                            </div>
                                        </button>
                                    );
                                })}
                            </div>
                        )}
                    </>
                )}

                {/* ── Level 2: Departments ── */}
                {!loading && navLevel === 'departments' && (
                    <>
                        <p className="bm-section-heading">Departments</p>
                        {currentDepts.length === 0 ? (
                            <p className="bm-empty">No departments found for this building.</p>
                        ) : (
                            <div className="bm-cards-grid">
                                {currentDepts.map(d => {
                                    const s = getDeptStats(d);
                                    return (
                                        <button
                                            key={d.id}
                                            type="button"
                                            className={`bm-nav-card${s.violationCount > 0 ? ' bm-nav-card--alert' : ''}`}
                                            onClick={() => setSelectedDept(d)}
                                        >
                                            <div className="bm-card-header">
                                                <span className="bm-card-name">{d.name}</span>
                                                {s.violationCount > 0 && (
                                                    <span className="bm-badge bm-badge--red">
                                                        {plural(s.violationCount, 'violation')}
                                                    </span>
                                                )}
                                            </div>
                                            <div className="bm-card-meta">
                                                <span>{plural(s.roomCount, 'room')}</span>
                                            </div>
                                        </button>
                                    );
                                })}
                            </div>
                        )}
                    </>
                )}

                {/* ── Level 3: Rooms ── */}
                {!loading && navLevel === 'rooms' && (
                    <>
                        <p className="bm-section-heading">Rooms</p>
                        {currentRooms.length === 0 ? (
                            <p className="bm-empty">No rooms found for this department.</p>
                        ) : (
                            <RoomListTable
                                rooms={currentRooms}
                                showDepartment={false}
                                showSettings={false}
                                disablePrivacyMask={true}
                                onRowClick={handleRoomClick}
                            />
                        )}
                    </>
                )}

            </div>
        </div>
    );
};

export default BuildingManagerDashboard;
