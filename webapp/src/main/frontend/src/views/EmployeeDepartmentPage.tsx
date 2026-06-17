import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { DepartmentControllerApi, RoomControllerApi, UserxControllerApi, WarningControllerApi } from '../generated-skeleton-api';
import { Toast } from 'primereact/toast';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import { useTimeFormat } from '../hooks/useTimeFormat';

import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import { DepartmentRoomOverview } from '../components/DepartmentRoomOverview';
import { DepartmentClimateSection } from '../components/DepartmentClimateSection';

import '../styles/EmployeeDepartmentPage.css';

export interface UserRoomDTO {
    id: string;
    departmentID: string;
    departmentName: string;
    roomType: 'OFFICE' | 'SHARED';
    roomNumber: string;
}

interface UserxDTO {
    id: string;
    username: string;
    firstName: string;
    lastName: string;
    myRoom?: UserRoomDTO | null;
}

export interface RoomDTO {
    id: string;
    departmentID: string;
    departmentName: string;
    isActive: boolean;
    roomType: 'OFFICE' | 'SHARED';
    defaultPeopleCount: number;
    roomNumber?: string | null;
    name?: string | null;
}

export interface ClimateDataPointDTO {
    timestamp: string;
    temperature: number;
    humidity: number;
    airQuality: number;
}

export interface ActiveWarning {
    id: string;
    measurementType: string;
    message: string;
    tip: string;
    createdAt: string;
    status: 'GREEN' | 'YELLOW' | 'RED';
}

const pad2 = (n: number) => String(n).padStart(2, '0');
const fmtDate = (d: Date) =>
    `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;

export const getRoomDisplayName = (room: RoomDTO) => {
    return room.name || room.roomNumber || 'Unnamed room';
};


export const EmployeeDepartmentPage: React.FC = () => {
    const [sidebarVisible, setSidebarVisible] = useState(false);
    const { formatTime } = useTimeFormat();
    const [currentUser, setCurrentUser] = useState<UserxDTO | null>(null);
    const [rooms, setRooms] = useState<RoomDTO[]>([]);

    // selectedRoomId = Raum, der im Chart ausgewählt ist
    const [selectedRoomId, setSelectedRoomId] = useState<string | null>(null);

    // expandedRoomId = Raum, der oben in der Liste aufgeklappt ist
    const [expandedRoomId, setExpandedRoomId] = useState<string | null>(null);

    const [currentClimate, setCurrentClimate] = useState<ClimateDataPointDTO | null>(null);
    const [warnings, setWarnings] = useState<ActiveWarning[]>([]);
    const toastRef = useRef<Toast>(null);
    const shownWarningIds = useRef<Set<string>>(new Set());
    const stompClient = useRef<Client | null>(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [loadingRooms, setLoadingRooms] = useState(true);
    const [loadingClimate, setLoadingClimate] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const fetchDepartmentRooms = useCallback(() => {
        setLoadingRooms(true);
        setError(null);

        new UserxControllerApi().getAuthenticatedUser()
            .then(userResponse => {
                const user: UserxDTO = userResponse.data as any;
                const departmentId = user.myRoom?.departmentID;

                setCurrentUser(user);

                if (!departmentId) {
                    setRooms([]);
                    setSelectedRoomId(null);
                    setExpandedRoomId(null);
                    setError('No department assigned to current user.');
                    return;
                }

                return new DepartmentControllerApi().getSpecificDepartment({ departmentId })
                    .then(roomResponse => {
                        const departmentRooms: RoomDTO[] = (roomResponse.data as any).rooms || [];
                        console.log(departmentRooms)
                        setRooms(departmentRooms)
                        if (departmentRooms.length === 0) {
                            setSelectedRoomId(null);
                            setExpandedRoomId(null);
                            return;
                        }

                        setSelectedRoomId(previousSelectedRoomId => {
                            const nextSelectedRoomId =
                                previousSelectedRoomId &&
                                departmentRooms.some(room => room.id === previousSelectedRoomId)
                                    ? previousSelectedRoomId
                                    : departmentRooms[0].id;

                            setExpandedRoomId(previousExpandedRoomId =>
                                previousExpandedRoomId &&
                                departmentRooms.some(room => room.id === previousExpandedRoomId)
                                    ? previousExpandedRoomId
                                    : nextSelectedRoomId
                            );

                            return nextSelectedRoomId;
                        });
                    });
            })
            .catch(err => {
                console.error(
                    'Could not load department rooms',
                    err.response?.status,
                    err.response?.data || err
                );
                setError('Department rooms could not be loaded.');
            })
            .finally(() => {
                setLoadingRooms(false);
            });
    }, []);

    const fetchWarnings = useCallback((roomId: string) => {
        new WarningControllerApi().getWarningsForRoom({
            roomId,
            activeOnly: true,
            startDate: '2000-01-01',
            endDate: fmtDate(new Date()),
        })
        .then(r => {
            setWarnings(
                Object.values(
                    (r.data as ActiveWarning[]).reduce((acc, warning) => {
                        const existing = acc[warning.measurementType];
                        if (!existing || new Date(warning.createdAt) > new Date(existing.createdAt)) {
                            acc[warning.measurementType] = warning;
                        }
                        return acc;
                    }, {} as Record<string, ActiveWarning>)
                )
            );
        })
        .catch(() => {});
    }, []);

    const fetchCurrentClimate = useCallback((roomId: string) => {
        setLoadingClimate(true);

        new RoomControllerApi().getCurrentClimate({ roomId })
            .then(response => {
                const data: ClimateDataPointDTO = response.data as any;
                const isStale = data !== null
                    && (Date.now() - new Date(data.timestamp).getTime()) > 30 * 1000;
                setCurrentClimate(isStale ? null : data);
            })
            .catch(err => {
                console.error(
                    'Could not load current climate',
                    err.response?.status,
                    err.response?.data || err
                );
                setCurrentClimate(null);
            })
            .finally(() => {
                setLoadingClimate(false);
            });
    }, []);

    useEffect(() => {
        fetchDepartmentRooms();
    }, [fetchDepartmentRooms]);

    useEffect(() => {
        if (!expandedRoomId) {
            setCurrentClimate(null);
            setWarnings([]);
            return;
        }

        fetchCurrentClimate(expandedRoomId);
        fetchWarnings(expandedRoomId);
    }, [expandedRoomId, fetchCurrentClimate, fetchWarnings]);

    useEffect(() => {
        if (!expandedRoomId) return;

        stompClient.current?.deactivate();

        stompClient.current = new Client({
            webSocketFactory: () => new SockJS('http://localhost:8080/active-events'),
            reconnectDelay: 5000,
            connectHeaders: {
                "Authorization": `Bearer ${localStorage.getItem('bearerToken')}`
            },
            onConnect: () => {
                stompClient.current?.subscribe(`/topic/active-warnings/${expandedRoomId}`, (message) => {
                    const warning: ActiveWarning = JSON.parse(message.body);
                    setWarnings(prev => [warning, ...prev.filter(w => w.measurementType !== warning.measurementType)]);
                });
                stompClient.current?.subscribe(`/topic/resolve-warnings/${expandedRoomId}`, (message) => {
                    const warning: ActiveWarning = JSON.parse(message.body);
                    setWarnings(prev => prev.filter(w => w.id !== warning.id));
                });
                stompClient.current?.subscribe(`/topic/climate-data/${expandedRoomId}`, (message) => {
                    const data: ClimateDataPointDTO = JSON.parse(message.body);
                    setCurrentClimate(data);
                });
            },
            onStompError: (frame) => {
                console.error('STOMP error:', frame);
            },
        });

        stompClient.current.activate();

        return () => {
            stompClient.current?.deactivate();
        };
    }, [expandedRoomId]);

    useEffect(() => {
        if (!toastRef.current || warnings.length === 0) return;

        for (const warning of warnings) {
            if (shownWarningIds.current.has(warning.id)) continue;
            shownWarningIds.current.add(warning.id);

            const label =
                warning.measurementType === 'TEMPERATURE' ? 'Temperature' :
                warning.measurementType === 'HUMIDITY'    ? 'Humidity'    : 'CO₂';

            const hasTip = warning.tip && warning.tip !== "There's no tip.";

            toastRef.current.show({
                severity: 'warn',
                summary: `${label}: ${warning.message}`,
                detail: hasTip ? warning.tip : undefined,
                life: 5000,
            });
        }
    }, [warnings]);

    const filteredRooms = useMemo(() => {
        const normalizedSearchTerm = searchTerm.trim().toLowerCase();

        if (!normalizedSearchTerm) {
            return rooms;
        }

        return rooms.filter(room =>
            getRoomDisplayName(room).toLowerCase().includes(normalizedSearchTerm)
        );
    }, [rooms, searchTerm]);

    return (
        <div className="employee-department-page">
            <Toast ref={toastRef} position="top-right" />
            <PageHeader
                title="My Department"
                subtitle={currentUser?.myRoom?.departmentName || 'Overview'}
                onMenuClick={() => setSidebarVisible(true)}
            />

            <SidebarComponent
                visible={sidebarVisible}
                onHide={() => setSidebarVisible(false)}
            />

            <main className="employee-department-content">

                <div className="employee-department-toolbar">
                    <span className="employee-department-last-updated">
                        Last updated at {currentClimate?.timestamp ? formatTime(currentClimate.timestamp) : '-'}
                    </span>

                    <input
                        className="employee-department-search"
                        type="text"
                        placeholder="Search by name"
                        value={searchTerm}
                        onChange={event => setSearchTerm(event.target.value)}
                    />
                </div>

                {loadingRooms && (
                    <p className="employee-department-info-text">
                        Loading department rooms...
                    </p>
                )}

                {error && (
                    <p className="employee-department-error-text">
                        {error}
                    </p>
                )}

                {!loadingRooms && !error && (
                    <>
                        <DepartmentRoomOverview
                            rooms={filteredRooms}
                            expandedRoomId={expandedRoomId}
                            currentClimate={currentClimate}
                            loadingClimate={loadingClimate}
                            warnings={warnings}
                            onToggleRoom={(roomId) => {
                                setExpandedRoomId(previousRoomId =>
                                    previousRoomId === roomId ? null : roomId
                                );
                            }}
                        />

                        <DepartmentClimateSection
                            rooms={rooms}
                            selectedRoomId={selectedRoomId}
                            onSelectRoom={setSelectedRoomId}
                        />
                    </>
                )}
            </main>
        </div>
    );
};

export default EmployeeDepartmentPage;