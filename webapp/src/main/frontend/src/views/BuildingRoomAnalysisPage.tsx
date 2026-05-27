import React, { useCallback, useEffect, useRef, useState } from 'react';
import { RoomControllerApi, WarningControllerApi } from '../generated-skeleton-api';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { Toast } from 'primereact/toast';

import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import RoomSensorHistoryCard from '../components/RoomSensorHistoryCard';
import RoomViolationLogTable, { ViolationDTO } from '../components/RoomViolationLogTable';
import RoomLimitSettings, { LimitState } from '../components/RoomLimitSettings';

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
    const location = useLocation();
    const toast = useRef<Toast>(null);

    const [sidebarVisible, setSidebarVisible] = useState(false);
    const [violations, setViolations] = useState<ViolationDTO[]>([]);
    const [limits, setLimits] = useState<LimitState>(emptyLimits);
    const [roomLabel, setRoomLabel] = useState('Room');
    const [loading, setLoading] = useState(true);
    const [savingLimits, setSavingLimits] = useState(false);

    const fetchRoomAnalysisData = useCallback(() => {
        if (!roomId) return;
        setLoading(true);

        Promise.allSettled([
            new RoomControllerApi().getLimitsForRoom({ roomId }),
            new WarningControllerApi().getWarningsForRoom({
                roomId,
                activeOnly: false,
                startDate: '2000-01-01',
                endDate: new Date().toISOString().slice(0, 10),
            }),
            new RoomControllerApi().getPageOfRooms({ pageable: { page: 0, size: 500, sort: [] } }),
        ]).then(([limitsResult, warningsResult, roomsResult]) => {
            if (roomsResult.status === 'fulfilled') {
                const rooms = extractArrayResponse<RoomDTO>(roomsResult.value.data, []);
                setRoomLabel(getRoomDisplayName(rooms.find(r => r.id === roomId), `Room ${roomId}`));
            } else {
                setRoomLabel(`Room ${roomId}`);
            }

            setLimits(limitsResult.status === 'fulfilled'
                ? mapLimitDtoToState(limitsResult.value.data)
                : emptyLimits);

            setViolations(warningsResult.status === 'fulfilled'
                ? extractArrayResponse<ViolationDTO>(warningsResult.value.data, [])
                : []);
        }).finally(() => setLoading(false));
    }, [roomId]);

    useEffect(() => {
        fetchRoomAnalysisData();
    }, [fetchRoomAnalysisData]);

    const handleLimitChange = (key: keyof LimitState, value: string) => {
        if (key === 'airQualityMin') return;
        setLimits(prev => ({ ...prev, [key]: value }));
    };

    const handleSaveLimits = () => {
        if (!roomId) return;
        setSavingLimits(true);

        new RoomControllerApi().updateLimitsForRoom({ roomId, limitDTO: mapLimitStateToDto(roomId, limits) as any })
            .then(response => {
                setLimits(mapLimitDtoToState(response.data));
                toast.current?.show({ severity: 'success', summary: 'Saved', detail: 'Limits saved successfully.', life: 3000 });
            })
            .catch(error => {
                toast.current?.show({
                    severity: 'error',
                    summary: 'Error',
                    detail: getApiErrorMessage(error, 'Room limits could not be saved.'),
                    life: 4000,
                });
            })
            .finally(() => setSavingLimits(false));
    };

    return (
        <div className="bra-page">
            <Toast ref={toast} />
            <PageHeader title="Room Analysis" subtitle={roomLabel} onMenuClick={() => setSidebarVisible(true)} />
            <SidebarComponent visible={sidebarVisible} onHide={() => setSidebarVisible(false)} />

            <div className="bra-content">
                <button type="button" className="bra-back-btn" onClick={() => navigate(ROUTES.HOME, { state: location.state })}>
                    <i className="pi pi-arrow-left" />
                    Back to rooms
                </button>

                {loading && <p className="bra-loading">Loading room data…</p>}

                <p className="bra-section-heading">Climate History</p>
                <div className="bra-card">
                    {roomId && <RoomSensorHistoryCard roomId={roomId} />}
                </div>

                <p className="bra-section-heading">Warning Log</p>
                <RoomViolationLogTable violations={violations} />

                <p className="bra-section-heading">Limit Settings</p>
                <div className="bra-card">
                    <RoomLimitSettings
                        limits={limits}
                        saving={savingLimits}
                        onLimitChange={handleLimitChange}
                        onSaveLimits={handleSaveLimits}
                    />
                </div>
            </div>
        </div>
    );
};

export default BuildingRoomAnalysisPage;
