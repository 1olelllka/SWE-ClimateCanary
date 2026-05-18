import React, { useState, useEffect, useCallback, useRef } from 'react';
import globalAxios from 'axios';
import { Cards } from '../components/Cards';
import '../styles/EmployeeDashboard.css';
import { FooterComponent } from "../components/FooterComponent";
import SidebarComponent from '../components/SidebarComponent';
import { PageHeader } from "../components/PageHeader";
import { ClimateHistoryChart } from '../components/ClimateHistoryChart';
import { Toast } from 'primereact/toast';

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
}

interface RawPoint {
    timestamp: string;
    temperature: number;
    humidity: number;
    airQuality: number;
}

// Local-time formatting helpers (server stores LocalDateTime without timezone)
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

    // Only use reference if it's within ±3 min of 10 min ago
    if (!closest || closestDiff > 3 * 60 * 1000) return { text: 'Stable', icon: 'pi-minus' };

    const delta = current - closest[field];
    if (Math.abs(delta) < threshold) return { text: 'Stable', icon: 'pi-minus' };

    const icon  = delta > 0 ? 'pi-caret-up' : 'pi-caret-down';
    return { text: ` ${fmtDelta(Math.abs(delta))} vs 10 min ago`, icon };
}

export const EmployeeDashboard: React.FC = () => {
    const [sidebarVisible, setSidebarVisible] = useState(false);

    const toastRef = useRef<Toast>(null);
    const shownWarningIds = useRef<Set<string>>(new Set());

    const [roomId, setRoomId] = useState<string | null>(null);
    const [roomName, setRoomName] = useState('My Office');
    const [noRoom, setNoRoom] = useState(false);
    const [serverOffline, setServerOffline] = useState(false);
    const [climate, setClimate] = useState<ClimateData | null>(null);
    const [warnings, setWarnings] = useState<ActiveWarning[]>([]);
    const [historyPoints, setHistoryPoints] = useState<RawPoint[]>([]);
    const [loading, setLoading] = useState(true);

    // Resolve current user's room on mount
    useEffect(() => {
        globalAxios.get('/api/users/me')
            .then(res => {
                const room = res.data?.myRoom;
                if (room?.id) {
                    setRoomId(room.id);
                    if (room.roomNumber) setRoomName(room.roomNumber);
                } else {
                    setNoRoom(true);
                    setLoading(false);
                }
            })
            .catch(() => { setServerOffline(true); setLoading(false); });
    }, []);

    const fetchLiveData = useCallback(() => {
        if (!roomId) return;

        const now = new Date();
        const twentyMinAgo = new Date(now.getTime() - 20 * 60 * 1000);

        Promise.all([
            globalAxios.get<ClimateData>(`/api/rooms/${roomId}/current-climate`)
                .then(r => r.data).catch(() => null),
            globalAxios.get<ActiveWarning[]>(`/api/warnings/rooms/${roomId}`, {
                params: {
                    activeOnly: false,
                    startDate: '2000-01-01',
                    endDate: fmtDate(new Date()),
                },
            }).then(r => r.data).catch(() => []),
            globalAxios.get<RawPoint[]>(`/api/rooms/${roomId}/overtime`, {
                params: {
                    startDate: fmtDate(twentyMinAgo),
                    endDate:   fmtDate(now),
                    startTime: fmtTime(twentyMinAgo),
                    endTime:   fmtTime(now),
                },
            }).then(r => r.data).catch(() => []),
        ]).then(([climateData, warningData, histData]) => {
            setClimate(climateData);
            // Find the latest warning per measurement type regardless of status,
            // then keep only those where the latest is still active (not resolved).
            const latestPerType = new Map<string, ActiveWarning>();
            for (const w of (warningData ?? [])) {
                const existing = latestPerType.get(w.measurementType);
                if (!existing || new Date(w.createdAt) > new Date(existing.createdAt))
                    latestPerType.set(w.measurementType, w);
            }
            setWarnings([...latestPerType.values()].filter(w => w.active));
            setHistoryPoints(histData ?? []);
            setLoading(false);
        });
    }, [roomId]);

    // Fetch immediately when room is known, then every 30 s
    useEffect(() => {
        if (!roomId) return;
        fetchLiveData();
        const interval = setInterval(fetchLiveData, 30_000);
        return () => clearInterval(interval);
    }, [roomId, fetchLiveData]);

    // Show a toast the first time each unique warning (by ID) is observed
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


    const isClimateStale = climate !== null
        && (Date.now() - new Date(climate.timestamp).getTime()) > 5 * 60 * 1000;
    const currentClimate = isClimateStale ? null : climate;

    const fmt = (v: number | undefined, decimals = 1): string =>
        v !== undefined ? v.toFixed(decimals) : (loading ? '…' : 'N/A');

    const updatedAt = currentClimate
        ? new Date(currentClimate.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        : '--:--';

    // Sparkline: display only the last 10 minutes; trend uses full historyPoints to find 10-min-ago reference
    const tenMinAgo = Date.now() - 10 * 60 * 1000;
    const last10min = historyPoints.filter(p => new Date(p.timestamp).getTime() >= tenMinAgo);
    const tempSparkline = last10min.map(p => p.temperature);
    const humSparkline  = last10min.map(p => p.humidity);
    const aqSparkline   = last10min.map(p => p.airQuality);

    // Trend text: current value vs 10 min ago
    const tempTrend = calcTrend(currentClimate?.temperature, historyPoints, 'temperature', v => `${v.toFixed(1)}°`,    0.2);
    const humTrend  = calcTrend(currentClimate?.humidity,    historyPoints, 'humidity',    v => `${v.toFixed(1)}%`,    1.0);
    const aqTrend   = calcTrend(currentClimate?.airQuality,  historyPoints, 'airQuality',  v => `${Math.round(v)} ppm`, 10);

    const tempWarning = warnings.find(w => w.measurementType === 'TEMPERATURE');
    const humWarning  = warnings.find(w => w.measurementType === 'HUMIDITY');
    const aqWarning   = warnings.find(w => w.measurementType === 'CO2');

    return (
        <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', backgroundColor: 'var(--page-bg)' }}>
            <Toast ref={toastRef} position="top-right" />
            <PageHeader
                title={roomName}
                subtitle="My Office"
                lastUpdated={updatedAt}
                onMenuClick={() => setSidebarVisible(true)}
            />

            <SidebarComponent visible={sidebarVisible} onHide={() => setSidebarVisible(false)} />

            <div className="employee-dashboard-container" style={{ flexGrow: 1 }}>
                {serverOffline ? (
                    <div style={{ padding: '2rem', textAlign: 'center', color: '#64748b' }}>
                        Server is not reachable. Please try again later.
                    </div>
                ) : noRoom ? (
                    <div style={{ padding: '2rem', textAlign: 'center', color: '#64748b' }}>
                        No room is assigned to your account. Please contact your system administrator.
                    </div>
                ) : (
                    <>
                        <div className="card-grid">
                            <Cards
                                title="Temperature"
                                value={fmt(currentClimate?.temperature)}
                                unit="°C"
                                color="#e05252"
                                dataPoints={tempSparkline}
                                trendIcon={tempTrend.icon}
                                trendText={tempTrend.text}
                                violated={!!tempWarning}
                                tip={tempWarning?.tip}
                            />
                            <Cards
                                title="Humidity"
                                value={fmt(currentClimate?.humidity)}
                                unit="%"
                                color="#26a69a"
                                dataPoints={humSparkline}
                                trendIcon={humTrend.icon}
                                trendText={humTrend.text}
                                violated={!!humWarning}
                                tip={humWarning?.tip}
                            />
                            <Cards
                                title="Air Quality (CO₂)"
                                value={fmt(currentClimate?.airQuality, 0)}
                                unit="ppm"
                                color="#d4891a"
                                dataPoints={aqSparkline}
                                trendIcon={aqTrend.icon}
                                trendText={aqTrend.text}
                                violated={!!aqWarning}
                                tip={aqWarning?.tip}
                            />
                        </div>

                        {roomId && <ClimateHistoryChart roomId={roomId} />}
                    </>
                )}
            </div>

            <FooterComponent />
        </div>
    );
};

export default EmployeeDashboard;
