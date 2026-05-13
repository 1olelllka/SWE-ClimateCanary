import React, { useCallback, useEffect, useState } from 'react';
import globalAxios from 'axios';
import { useNavigate, useParams } from 'react-router-dom';

import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import RoomSensorHistoryCard from '../components/RoomSensorHistoryCard';
import RoomViolationLogTable, { ViolationDTO } from '../components/RoomViolationLogTable';
import RoomLimitSettings, { LimitState } from '../components/RoomLimitSettings';
import { FooterComponent } from '../components/FooterComponent';

import { ROUTES } from '../utilities/routes.paths';
import { extractArrayResponse, getApiErrorMessage } from '../utilities/apiUtils';
import {
    emptyLimits,
    LimitDTO,
    mapLimitDtoToState,
    mapLimitStateToDto
} from '../utilities/limitUtils';
import { getRoomDisplayName, RoomDTO } from '../utilities/roomUtils';

import '../styles/BuildingRoomAnalysisPage.css';

export const BuildingRoomAnalysisPage: React.FC = () => {
    const { roomId } = useParams<{ roomId: string }>();
    const navigate = useNavigate();

    const [sidebarVisible, setSidebarVisible] = useState(false);

    const [violations, setViolations] = useState<ViolationDTO[]>([]);
    const [limits, setLimits] = useState<LimitState>(emptyLimits);
    const [roomLabel, setRoomLabel] = useState('Room');

    const [loading, setLoading] = useState(true);
    const [savingLimits, setSavingLimits] = useState(false);
    const [limitsMessage, setLimitsMessage] = useState<string | null>(null);

    const fetchRoomAnalysisData = useCallback(() => {
        if (!roomId) {
            return;
        }

        setLoading(true);
        setLimitsMessage(null);

        Promise.allSettled([
            globalAxios.get<LimitDTO>(`/api/rooms/${roomId}/limits`),
            globalAxios.get(`/api/rooms/${roomId}/violations`),
            globalAxios.get(`/api/rooms`)
        ])
            .then(results => {
                const [
                    limitsResult,
                    violationsResult,
                    roomsResult
                ] = results;

                if (roomsResult.status === 'fulfilled') {
                    const rooms = extractArrayResponse<RoomDTO>(
                        roomsResult.value.data,
                        []
                    );

                    const currentRoom = rooms.find(room => room.id === roomId);

                    setRoomLabel(
                        getRoomDisplayName(currentRoom, `Room ${roomId}`)
                    );
                } else {
                    console.warn('Could not load room name', roomsResult.reason);
                    setRoomLabel(`Room ${roomId}`);
                }

                if (limitsResult.status === 'fulfilled') {
                    setLimits(mapLimitDtoToState(limitsResult.value.data));
                } else {
                    console.warn('Could not load limits', limitsResult.reason);
                    setLimits(emptyLimits);
                }

                if (violationsResult.status === 'fulfilled') {
                    setViolations(
                        extractArrayResponse<ViolationDTO>(
                            violationsResult.value.data,
                            []
                        )
                    );
                } else {
                    console.warn('Could not load violations', violationsResult.reason);
                    setViolations([]);
                }
            })
            .finally(() => {
                setLoading(false);
            });
    }, [roomId]);

    useEffect(() => {
        fetchRoomAnalysisData();
    }, [fetchRoomAnalysisData]);

    const handleLimitChange = (key: keyof LimitState, value: string) => {
        if (key === 'airQualityMin') {
            return;
        }

        setLimits(previous => ({
            ...previous,
            [key]: value
        }));
    };

    const handleSaveLimits = () => {
        if (!roomId) {
            return;
        }

        setSavingLimits(true);
        setLimitsMessage(null);

        const payload = mapLimitStateToDto(roomId, limits);

        globalAxios.patch<LimitDTO>(`/api/rooms/${roomId}/limits`, payload)
            .then(response => {
                setLimits(mapLimitDtoToState(response.data));
                setLimitsMessage('Limits saved.');
            })
            .catch(error => {
                console.error(
                    'Could not save room limits',
                    error.response?.status,
                    error.response?.data || error
                );

                setLimitsMessage(
                    getApiErrorMessage(
                        error,
                        'Room limits could not be saved.'
                    )
                );
            })
            .finally(() => {
                setSavingLimits(false);
            });
    };

    return (
        <div className="building-room-analysis-page">
            <PageHeader
                title="Analysis & Settings"
                subtitle={roomLabel}
                onMenuClick={() => setSidebarVisible(true)}
            />

            <SidebarComponent
                visible={sidebarVisible}
                onHide={() => setSidebarVisible(false)}
            />

            <main className="building-room-analysis-container">
                <button
                    type="button"
                    className="building-room-analysis-back-button"
                    onClick={() => navigate(ROUTES.HOME)}
                    aria-label="Back"
                >
                    <i className="pi pi-arrow-circle-left" />
                </button>

                <div className="building-room-analysis-grid">
                    {roomId && (
                        <RoomSensorHistoryCard roomId={roomId} />
                    )}

                    <RoomViolationLogTable violations={violations} />

                    <RoomLimitSettings
                        limits={limits}
                        saving={savingLimits}
                        message={limitsMessage}
                        onLimitChange={handleLimitChange}
                        onSaveLimits={handleSaveLimits}
                    />
                </div>

                {loading && (
                    <p className="room-analysis-loading-note">
                        Loading room analysis data...
                    </p>
                )}
            </main>

            <FooterComponent />
        </div>
    );
};

export default BuildingRoomAnalysisPage;