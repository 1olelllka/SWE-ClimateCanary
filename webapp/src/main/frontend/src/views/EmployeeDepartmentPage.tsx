import React, { useCallback, useEffect, useMemo, useState } from 'react';
import globalAxios from 'axios';

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

export const getRoomDisplayName = (room: RoomDTO) => {
    return room.name || room.roomNumber || 'Unnamed room';
};

const formatLastUpdated = (timestamp?: string | null) => {
    if (!timestamp) {
        return '-';
    }

    return new Date(timestamp).toLocaleTimeString('de-DE', {
        hour: '2-digit',
        minute: '2-digit'
    });
};

export const EmployeeDepartmentPage: React.FC = () => {
    const [sidebarVisible, setSidebarVisible] = useState(false);
    const [currentUser, setCurrentUser] = useState<UserxDTO | null>(null);
    const [rooms, setRooms] = useState<RoomDTO[]>([]);

    // selectedRoomId = Raum, der im Chart ausgewählt ist
    const [selectedRoomId, setSelectedRoomId] = useState<string | null>(null);

    // expandedRoomId = Raum, der oben in der Liste aufgeklappt ist
    const [expandedRoomId, setExpandedRoomId] = useState<string | null>(null);

    const [currentClimate, setCurrentClimate] = useState<ClimateDataPointDTO | null>(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [loadingRooms, setLoadingRooms] = useState(true);
    const [loadingClimate, setLoadingClimate] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const fetchDepartmentRooms = useCallback(() => {
        setLoadingRooms(true);
        setError(null);

        globalAxios.get('/api/users/me')
            .then(userResponse => {
                const user: UserxDTO = userResponse.data;
                const departmentId = user.myRoom?.departmentID;

                setCurrentUser(user);

                if (!departmentId) {
                    setRooms([]);
                    setSelectedRoomId(null);
                    setExpandedRoomId(null);
                    setError('No department assigned to current user.');
                    return;
                }

                return globalAxios.get('/api/departments/'+departmentId)
                    .then(roomResponse => {
                        const departmentRooms: RoomDTO[] = roomResponse.data.rooms || [];
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

    const fetchCurrentClimate = useCallback((roomId: string) => {
        setLoadingClimate(true);

        globalAxios.get(`/api/rooms/${roomId}/current-climate`)
            .then(response => {
                const data: ClimateDataPointDTO = response.data;
                const isStale = data !== null
                    && (Date.now() - new Date(data.timestamp).getTime()) > 5 * 60 * 1000;
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
            return;
        }

        fetchCurrentClimate(expandedRoomId);
    }, [expandedRoomId, fetchCurrentClimate]);

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
                        Last updated at {formatLastUpdated(currentClimate?.timestamp)}
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