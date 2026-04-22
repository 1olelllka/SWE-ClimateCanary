import React, { useState, useEffect, useCallback } from 'react';
import globalAxios from 'axios';
import { Cards } from '../components/Cards';
import '../styles/EmployeeDashboard.css';
import { FooterComponent } from "../components/FooterComponent";
import SidebarComponent from '../components/SidebarComponent';
import '../styles/TimeFilter.css';
import { WarningBanner } from '../components/WarningBanner';
import { DashboardCalendar } from '../components/Calendar';
import { PageHeader } from "../components/PageHeader";

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
}

export const EmployeeDashboard: React.FC = () => {
    const [timeFilter, setTimeFilter] = useState('Week');
    const [sidebarVisible, setSidebarVisible] = useState(false);
    const [dateRange, setDateRange] = useState<Date[] | null>(null);

    const [roomId, setRoomId] = useState<string | null>(null);
    const [roomLabel, setRoomLabel] = useState('My Office');
    const [noRoom, setNoRoom] = useState(false);
    const [climate, setClimate] = useState<ClimateData | null>(null);
    const [warnings, setWarnings] = useState<ActiveWarning[]>([]);
    const [loading, setLoading] = useState(true);

    // Resolve the current user's room from /api/users/me on mount
    useEffect(() => {
        globalAxios.get('/api/users/me')
            .then(res => {
                const room = res.data?.myRoom;
                if (room?.id) {
                    setRoomId(room.id);
                    if (room.departmentName) setRoomLabel(room.departmentName);
                } else {
                    setNoRoom(true);
                    setLoading(false);
                }
            })
            .catch(() => { setNoRoom(true); setLoading(false); });
    }, []);

    const fetchLiveData = useCallback(() => {
        if (!roomId) return;
        Promise.all([
            globalAxios.get<ClimateData>(`/rooms/${roomId}/current-climate`)
                .then(r => r.data).catch(() => null),
            globalAxios.get<ActiveWarning[]>(`/warnings?roomId=${roomId}`)
                .then(r => r.data).catch(() => []),
        ]).then(([climateData, warningData]) => {
            setClimate(climateData);
            setWarnings((warningData ?? []).filter(w => w.active));
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

    const activeWarning = warnings[0] ?? null;

    const fmt = (v: number | undefined, decimals = 1): string =>
        v !== undefined ? v.toFixed(decimals) : (loading ? '…' : 'N/A');

    const updatedAt = climate
        ? new Date(climate.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        : '--:--';

    return (
        <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', backgroundColor: 'var(--page-bg)' }}>
            <PageHeader
                title={roomLabel}
                subtitle="My Office"
                lastUpdated={updatedAt}
                onMenuClick={() => setSidebarVisible(true)}
            />

            <SidebarComponent visible={sidebarVisible} onHide={() => setSidebarVisible(false)} />

            <div className="employee-dashboard-container" style={{ flexGrow: 1 }}>
                {noRoom ? (
                    <div style={{ padding: '2rem', textAlign: 'center', color: '#64748b' }}>
                        No room is assigned to your account. Please contact your system administrator.
                    </div>
                ) : (
                    <>
                        <div className="card-grid">
                            <Cards
                                title="Temperature"
                                value={fmt(climate?.temperature)}
                                unit="°C"
                                color="#e05252"
                                points="0,20 20,15 40,25 60,10 80,18 100,10"
                                trendIcon="pi-caret-up"
                                trendText={climate ? `Updated ${updatedAt}` : ''}
                            />
                            <Cards
                                title="Humidity"
                                value={fmt(climate?.humidity)}
                                unit="%"
                                color="#26a69a"
                                points="0,25 20,22 40,18 60,12 80,8 100,5"
                                trendIcon="pi-minus"
                                trendText=""
                            />
                            <Cards
                                title="Air Quality (CO₂)"
                                value={fmt(climate?.airQuality, 0)}
                                unit="ppm"
                                color="#d4891a"
                                points="0,10 20,12 40,15 60,18 80,22 100,25"
                                trendIcon={activeWarning ? 'pi-caret-up' : 'pi-check'}
                                trendText={activeWarning ? 'Elevated — ventilate recommended' : ''}
                            />
                        </div>

                        {activeWarning && (
                            <WarningBanner
                                boldPart={`${activeWarning.measurementType.replace(/_/g, ' ')} alert. `}
                                regularPart={activeWarning.message}
                            />
                        )}

                        <div className="chart-card">
                            <div className="chart-header">
                                <div className="time-filters">
                                    {['Day', 'Week', 'Month'].map(f => (
                                        <button
                                            key={f}
                                            className={`time-filter-btn ${timeFilter === f ? 'active' : ''}`}
                                            onClick={() => { setTimeFilter(f); setDateRange(null); }}
                                        >
                                            {f}
                                        </button>
                                    ))}
                                    <DashboardCalendar
                                        dateRange={dateRange}
                                        setDateRange={setDateRange}
                                        isActive={timeFilter === 'Custom'}
                                        onActivate={() => setTimeFilter('Custom')}
                                    />
                                </div>
                            </div>

                            <div style={{ height: '250px', backgroundColor: '#f8fafc', borderRadius: '8px', border: '1px dashed #dde4ec', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#94a3b8' }}>
                                PrimeReact Line Chart Placeholder
                            </div>
                        </div>
                    </>
                )}
            </div>

            <FooterComponent />
        </div>
    );
};

export default EmployeeDashboard;
