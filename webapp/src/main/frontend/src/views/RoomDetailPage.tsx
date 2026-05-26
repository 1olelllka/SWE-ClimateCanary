import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { RoomControllerApi, WarningControllerApi } from '../generated-skeleton-api';
import { Cards } from '../components/Cards';
import '../styles/EmployeeDashboard.css';
import SidebarComponent from '../components/SidebarComponent';
import { PageHeader } from '../components/PageHeader';
import { ClimateHistoryChart } from '../components/ClimateHistoryChart';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import { useTemperature } from '../hooks/useTemperature';

interface ClimateData {
    timestamp: string;
    temperature: number;
    humidity: number;
    airQuality: number;
}

interface ActiveWarning {
    id: string;
    measurementType: string;
    message: string;
    active: boolean;
    createdAt: string;
    tip: string;
    status: 'GREEN' | 'YELLOW' | 'RED';
}

interface RawPoint {
    timestamp: string;
    temperature: number;
    humidity: number;
    airQuality: number;
}

const pad2 = (n: number) => String(n).padStart(2, '0');
const fmtDate = (d: Date) =>
    `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
const fmtTime = (d: Date) =>
    `${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`;

function calcTrend(
    current: number | undefined,
    points: RawPoint[],
    field: 'temperature' | 'humidity' | 'airQuality',
    fmtDelta: (v: number) => string,
    threshold: number,
): { text: string; icon: string } {
    if (current == null || points.length === 0) return { text: '', icon: 'pi-minus' };

    const tenMinRef = Date.now() - 10 * 60 * 1000;
    let closest: RawPoint | null = null;
    let closestDiff = Infinity;
    for (const p of points) {
        const diff = Math.abs(new Date(p.timestamp).getTime() - tenMinRef);
        if (diff < closestDiff) { closestDiff = diff; closest = p; }
    }

    if (!closest || closestDiff > 3 * 60 * 1000) return { text: 'Stable', icon: 'pi-minus' };

    const delta = current - closest[field];
    if (Math.abs(delta) < threshold) return { text: 'Stable', icon: 'pi-minus' };

    const icon = delta > 0 ? 'pi-caret-up' : 'pi-caret-down';
    return { text: ` ${fmtDelta(Math.abs(delta))} vs 10 min ago`, icon };
}

export const RoomDetailPage: React.FC = () => {
    const { roomId: roomIdParam } = useParams<{ roomId: string }>();
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const [sidebarVisible, setSidebarVisible] = useState(false);

    const roomId = decodeURIComponent(roomIdParam ?? '');
    const roomName = searchParams.get('name') ?? 'Room Details';
    const isShared = searchParams.get('shared') === 'true';

    const [climate, setClimate] = useState<ClimateData | null>(null);
    const [warnings, setWarnings] = useState<ActiveWarning[]>([]);
    const [historyPoints, setHistoryPoints] = useState<RawPoint[]>([]);
    const [loading, setLoading] = useState(true);

    const stompClient = useRef<Client | null>(null);

    useEffect(() => {
        if (!roomId) return;

        stompClient.current = new Client({
            webSocketFactory: () => new SockJS('http://localhost:8080/active-events'),
            reconnectDelay: 5000,
            connectHeaders: {
                "Authorization":`Bearer ${localStorage.getItem("bearerToken")}`
            },
            onConnect: () => {
                stompClient.current?.subscribe(`/topic/active-warnings/${roomId}`, (message) => {
                    const warning: ActiveWarning = JSON.parse(message.body);
                    setWarnings(prev => [warning, ...prev.filter(w => w.measurementType !== warning.measurementType)]);
                });
                stompClient.current?.subscribe(`/topic/resolve-warnings/${roomId}`, (message) => {
                    const warning: ActiveWarning = JSON.parse(message.body);
                    setWarnings(prev => prev.filter(w => w.id !== warning.id));
                });
                stompClient.current?.subscribe(`/topic/climate-data/${roomId}`, (message) => {
                    const data: ClimateData = JSON.parse(message.body);
                    setClimate(data);
                    setHistoryPoints(prev => [...prev, data]);
                });
            },
            onStompError: (frame) => { console.error('STOMP error:', frame); },
        });

        stompClient.current.activate();

        return () => { stompClient.current?.deactivate(); };
    }, [roomId]);

    useEffect(() => {
        if (!roomId) return;
        const now = new Date();
        const twentyMinAgo = new Date(now.getTime() - 20 * 60 * 1000);
        new RoomControllerApi().getOvertimeClimateData({
            roomId,
            startDate: fmtDate(twentyMinAgo),
            endDate: fmtDate(now),
            startTime: fmtTime(twentyMinAgo),
            endTime: fmtTime(now),
        }).then(r => setHistoryPoints(r.data ?? [])).catch(() => {});
    }, [roomId]);

    useEffect(() => {
        if (!roomId) return;
        setLoading(true);
        new RoomControllerApi().getCurrentClimate({ roomId })
            .then(r => { setClimate(r.data as ClimateData); setLoading(false); })
            .catch(() => setLoading(false));
    }, [roomId]);

    useEffect(() => {
        if (!roomId) return;
        new WarningControllerApi().getWarningsForRoom({
            roomId,
            activeOnly: true,
            startDate: '2000-01-01',
            endDate: fmtDate(new Date()),
        }).then(r => {
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
        }).catch(() => {});
    }, [roomId]);

    const isClimateStale = climate !== null
        && (Date.now() - new Date(climate.timestamp).getTime()) > 30 * 1000;
    const currentClimate = isClimateStale ? null : climate;

    const { convert: convertTemp, convertDelta, unit: tempUnit } = useTemperature();

    const fmt = (v: number | undefined, decimals = 1): string =>
        v !== undefined ? v.toFixed(decimals) : (loading ? '…' : 'N/A');
    const fmtTemp = (v: number | undefined, decimals = 1): string =>
        v !== undefined ? convertTemp(v).toFixed(decimals) : (loading ? '…' : 'N/A');

    const updatedAt = currentClimate
        ? new Date(currentClimate.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        : '--:--';

    const tenMinAgo = Date.now() - 10 * 60 * 1000;
    const last10min = historyPoints.filter(p => new Date(p.timestamp).getTime() >= tenMinAgo);
    const tempSparkline = last10min.map(p => p.temperature);
    const humSparkline  = last10min.map(p => p.humidity);
    const aqSparkline   = last10min.map(p => p.airQuality);

    const tempTrend = calcTrend(currentClimate?.temperature, historyPoints, 'temperature', v => `${convertDelta(v).toFixed(1)}${tempUnit}`, 0.2);
    const humTrend  = calcTrend(currentClimate?.humidity,    historyPoints, 'humidity',    v => `${v.toFixed(1)}%`,    1.0);
    const aqTrend   = calcTrend(currentClimate?.airQuality,  historyPoints, 'airQuality',  v => `${Math.round(v)} ppm`, 10);

    const tempWarning = warnings.find(w => w.measurementType === 'TEMPERATURE');
    const humWarning  = warnings.find(w => w.measurementType === 'HUMIDITY');
    const aqWarning   = warnings.find(w => w.measurementType === 'CO2');

    return (
        <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', backgroundColor: 'var(--page-bg)' }}>
            <PageHeader
                title={roomName}
                subtitle="Room Detail"
                lastUpdated={updatedAt}
                onMenuClick={() => setSidebarVisible(true)}
            />

            <SidebarComponent visible={sidebarVisible} onHide={() => setSidebarVisible(false)} />

            <div className="employee-dashboard-container" style={{ flexGrow: 1 }}>
                <button
                    onClick={() => navigate(-1)}
                    style={{
                        background:   'none',
                        border:       'none',
                        cursor:       'pointer',
                        color:        '#64748b',
                        display:      'flex',
                        alignItems:   'center',
                        gap:          '0.4rem',
                        fontSize:     '0.85rem',
                        padding:      0,
                        marginBottom: '1.25rem',
                    }}
                >
                    <i className="pi pi-arrow-left" /> Back to Overview
                </button>

                <div className="card-grid">
                    <Cards
                        title="Temperature"
                        value={fmtTemp(currentClimate?.temperature)}
                        unit={tempUnit}
                        color="#e05252"
                        showSparkline={false}
                        warningStatus={tempWarning?.status}
                        tip={tempWarning?.tip}
                    />
                    <Cards
                        title="Humidity"
                        value={fmt(currentClimate?.humidity)}
                        unit="%"
                        color="#26a69a"
                        showSparkline={false}
                        warningStatus={humWarning?.status}
                        tip={humWarning?.tip}
                    />
                    <Cards
                        title="Air Quality (CO₂)"
                        value={fmt(currentClimate?.airQuality, 0)}
                        unit="ppm"
                        color="#d4891a"
                        showSparkline={false}
                        warningStatus={aqWarning?.status}
                        tip={aqWarning?.tip}
                    />
                </div>

                {roomId && <ClimateHistoryChart roomId={roomId} hideDayView={!isShared} />}
            </div>
        </div>
    );
};

export default RoomDetailPage;
