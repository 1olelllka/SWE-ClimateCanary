import React, { useState, useEffect, useRef } from 'react';
import { useParams, useSearchParams, useNavigate } from 'react-router-dom';
import ReactECharts from 'echarts-for-react';
import {
    ClimateStatsControllerApi,
    WarningControllerApi,
    BuildingTrendControllerApi,
    AggregatedDataPointDTO,
    SummaryWarningDTO,
    BuildingTrendDTO,
} from '../generated-skeleton-api';
import { Cards } from '../components/Cards';
import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import '../styles/BuildingManagerDashboard.css';
import '../styles/EmployeeDashboard.css';
import '../styles/ClimateHistoryChart.css';
import '../styles/TimeFilter.css';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useTemperature } from '../hooks/useTemperature';

const climateApi = new ClimateStatsControllerApi();
const warningApi = new WarningControllerApi();
const trendApi   = new BuildingTrendControllerApi();

const pad2    = (n: number) => String(n).padStart(2, '0');
const fmtDate = (d: Date)   => `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;

const MONTHS   = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
const WEEKDAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

type ViolView = 'Week' | 'Month' | 'Year';

/** Colour for a 0-100 climate trend score */
function scoreColor(score: number | null): string {
    if (score === null) return '#94a3b8';
    if (score >= 70)   return '#2DAA9E'; // teal — good
    if (score >= 40)   return '#1A5F96'; // blue — ok
    return '#e05252';                    // red  — poor
}

export const DepartmentDetailPage: React.FC = () => {
    const { departmentName: deptIdParam } = useParams<{ departmentName: string }>();
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const [sidebarVisible, setSidebarVisible] = useState(false);

    const deptId   = decodeURIComponent(deptIdParam ?? '');
    const deptName = searchParams.get('name') ?? 'Department';

    const [lastStats,   setLastStats]   = useState<AggregatedDataPointDTO | null>(null);
    const [weeklyData,  setWeeklyData]  = useState<AggregatedDataPointDTO[]>([]);
    const [violations,  setViolations]  = useState<SummaryWarningDTO[]>([]);
    const [activeWarnings, setViolationsCnt] = useState(0);
    const [trendWeek,   setTrendWeek]   = useState<BuildingTrendDTO[]>([]);
    const [trendMonth,  setTrendMonth]  = useState<BuildingTrendDTO[]>([]);
    const [loading,     setLoading]     = useState(true);
    const [violView,    setViolView]    = useState<ViolView>('Week');
    const stompClient = useRef<Client | null>(null);

    useEffect(() => {
        if (!deptId) return;
        const now      = new Date();
        const weekAgo  = new Date(now); weekAgo.setDate(weekAgo.getDate() - 6);
        const monthAgo = new Date(now); monthAgo.setDate(monthAgo.getDate() - 29);
        // First day of month 11 months ago = start of the 12-month violations window
        const yearAgo  = new Date(now);
        yearAgo.setDate(1);
        yearAgo.setMonth(yearAgo.getMonth() - 11);

        setLoading(true);
        Promise.all([
            // last aggregated reading
            climateApi
                .getLastAggregationForDepartment({ id: deptId })
                .then(r => r.data ?? null)
                .catch(() => null),
            // weekly climate aggregation (sparklines for climate cards)
            climateApi
                .getClimateAggregationForDepartment({ id: deptId, startDate: fmtDate(weekAgo), endDate: fmtDate(now) })
                .then(r => (r.data as AggregatedDataPointDTO[]) ?? [])
                .catch(() => [] as AggregatedDataPointDTO[]),
            // all violations for the year (used for chart + active count)
            warningApi
                .getWarningsSummaryForDepartment({
                    departmentId: deptId,
                    onlyActive:   false,
                    startDate:    fmtDate(yearAgo),
                    endDate:      fmtDate(now),
                })
                .then(r => (r.data as SummaryWarningDTO[]) ?? [])
                .catch(() => [] as SummaryWarningDTO[]),
            // trend score — last 7 days
            trendApi
                .getTrendsForSpecificDepartment({ departmentId: deptId, startDate: fmtDate(weekAgo), endDate: fmtDate(now) })
                .then(r => (r.data as BuildingTrendDTO[]) ?? [])
                .catch(() => [] as BuildingTrendDTO[]),
            // trend score — last 30 days
            trendApi
                .getTrendsForSpecificDepartment({ departmentId: deptId, startDate: fmtDate(monthAgo), endDate: fmtDate(now) })
                .then(r => (r.data as BuildingTrendDTO[]) ?? [])
                .catch(() => [] as BuildingTrendDTO[]),
        ])
            .then(([last, weekly, viols, tw, tm]) => {
                setLastStats(last);
                setWeeklyData(weekly);
                setViolations(viols);
                setViolationsCnt(viols.filter(v => v.active).length);
                setTrendWeek(tw);
                setTrendMonth(tm);
            })
            .finally(() => setLoading(false));
    }, [deptId]);

    useEffect(() => {
        if (!deptId) return;

        stompClient.current = new Client({
            webSocketFactory: () => new SockJS('http://localhost:8080/active-events'),
            reconnectDelay: 5000,
            connectHeaders: {
                "Authorization": `Bearer ${localStorage.getItem("bearerToken")}`
            },
            onConnect: () => {
                console.log('Connected');
                stompClient.current?.subscribe(`/topic/active-warnings-department/${deptId}`, (message) => {
                    const newWarning: SummaryWarningDTO = JSON.parse(message.body);
                    setViolations(prev => [...prev, newWarning]);
                    setViolationsCnt((prev) => prev + 1);
                });
                stompClient.current?.subscribe(`/topic/resolve-warnings-department/${deptId}`, (message) => {
                    const resolved: SummaryWarningDTO = JSON.parse(message.body);
                    setViolations(prev =>
                        prev.map(v =>
                            v.id === resolved.id ? { ...v, active: false } : v
                        )
                    );
                    setViolationsCnt((prev) => prev - 1);
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
    }, [deptId]);

    const { convert: convertTemp, convertDelta, unit: tempUnit } = useTemperature();

    /* ── Climate sparklines ─────────────────────────────────────────────── */
    const tempSparkline = weeklyData.map(d => convertTemp(d.avgTemperature ?? 0));
    const humSparkline  = weeklyData.map(d => d.avgHumidity    ?? 0);
    const aqSparkline   = weeklyData.map(d => d.avgAirQuality  ?? 0);

    /* ── Trend score sparklines ──────────────────────────────────────────── */
    const trendWeekValues  = trendWeek.map(d => d.value ?? 0);
    const trendMonthValues = trendMonth.map(d => d.value ?? 0);

    const trendWeekCurrent  = trendWeekValues.length  > 0 ? trendWeekValues[trendWeekValues.length - 1]   : null;
    const trendMonthCurrent = trendMonthValues.length > 0 ? trendMonthValues[trendMonthValues.length - 1] : null;

    /* ── Generic trend helper ────────────────────────────────────────────── */
    function climateCardTrend(
        sparkline: number[],
        suffix: string,
        decimals = 1,
        convertDeltaFn?: (v: number) => number,
    ): { text: string; icon: string } {
        const clean = sparkline.filter(v => v !== 0);
        if (clean.length < 2) return { text: 'No trend data', icon: 'pi-minus' };
        const delta = clean[clean.length - 1] - clean[0];
        if (Math.abs(delta) < 0.05) return { text: 'Stable this week', icon: 'pi-minus' };
        const displayDelta = convertDeltaFn ? convertDeltaFn(delta) : delta;
        const icon = delta > 0 ? 'pi-caret-up' : 'pi-caret-down';
        return { text: `${displayDelta > 0 ? '+' : ''}${displayDelta.toFixed(decimals)}${suffix} vs Mon`, icon };
    }

    function scoreTrend(
        values: number[],
        label: string,
    ): { text: string; icon: string } {
        const clean = values.filter(v => v > 0);
        if (clean.length < 2) return { text: 'No trend data', icon: 'pi-minus' };
        const delta = clean[clean.length - 1] - clean[0];
        if (Math.abs(delta) < 0.5) return { text: `Stable this ${label}`, icon: 'pi-minus' };
        const icon = delta > 0 ? 'pi-caret-up' : 'pi-caret-down';
        return { text: `${delta > 0 ? '+' : ''}${delta.toFixed(1)} pts this ${label}`, icon };
    }

    const tempTrend       = climateCardTrend(tempSparkline, tempUnit, 1, v => convertDelta(v));
    const humTrend        = climateCardTrend(humSparkline,  '%');
    const aqTrend         = climateCardTrend(aqSparkline,   ' ppm', 0);
    const weekScoreTrend  = scoreTrend(trendWeekValues,  'week');
    const monthScoreTrend = scoreTrend(trendMonthValues, 'month');

    const fmt = (v: number | undefined, decimals = 1): string =>
        v !== undefined ? v.toFixed(decimals) : (loading ? '…' : 'N/A');
    const fmtTemp = (v: number | undefined, decimals = 1): string =>
        v !== undefined ? convertTemp(v).toFixed(decimals) : (loading ? '…' : 'N/A');

    const fmtScore = (v: number | null): string =>
        v !== null ? Math.round(v).toString() : (loading ? '…' : 'N/A');

    /* ── Violations chart ───────────────────────────────────────────────── */
    function buildChart(view: ViolView): { labels: string[]; counts: number[] } {
        const now = new Date();

        if (view === 'Week') {
            const days = Array.from({ length: 7 }, (_, i) => {
                const d = new Date(now); d.setDate(d.getDate() - (6 - i)); return d;
            });
            return {
                labels: days.map(d => `${WEEKDAYS[d.getDay()]} ${d.getDate()}`),
                counts: days.map(d => {
                    const s = fmtDate(d);
                    return violations.filter(v => v.createdAt?.slice(0, 10) === s).length;
                }),
            };
        }

        if (view === 'Month') {
            const days = Array.from({ length: 30 }, (_, i) => {
                const d = new Date(now); d.setDate(d.getDate() - (29 - i)); return d;
            });
            return {
                labels: days.map(d => `${d.getDate()}/${d.getMonth() + 1}`),
                counts: days.map(d => {
                    const s = fmtDate(d);
                    return violations.filter(v => v.createdAt?.slice(0, 10) === s).length;
                }),
            };
        }

        // Year — last 12 months
        const months = Array.from({ length: 12 }, (_, i) => {
            const d = new Date(now); d.setDate(1); d.setMonth(d.getMonth() - (11 - i));
            return { year: d.getFullYear(), month: d.getMonth() };
        });
        return {
            labels: months.map(m => `${MONTHS[m.month]} ${m.year}`),
            counts: months.map(({ year, month }) =>
                violations.filter(v => {
                    if (!v.createdAt) return false;
                    const d = new Date(v.createdAt);
                    return d.getFullYear() === year && d.getMonth() === month;
                }).length
            ),
        };
    }

    const { labels: violLabels, counts: violCounts } = buildChart(violView);
    const maxViol = Math.max(...violCounts, 1);

    const chartTextColor = getComputedStyle(document.documentElement)
        .getPropertyValue('--chart-text-color')
        .trim() || '#475569';

    const violOption = {
        tooltip: {
            trigger: 'axis' as const,
            formatter: (params: any[]) => {
                const p = params[0];
                return `${p.name}<br/>Violations: <b>${p.value}</b>`;
            },
        },
        grid: { left: 44, right: 20, top: 16, bottom: violView === 'Month' ? 56 : 36 },
        xAxis: {
            type:      'category' as const,
            data:      violLabels,
            axisLabel: { rotate: violView === 'Month' ? 45 : 0, fontSize: 11, color: chartTextColor },
            axisLine:  { lineStyle: { color: '#e2e8f0' } },
        },
        yAxis: {
            type:        'value' as const,
            minInterval: 1,
            min:         0,
            max:         maxViol + 1,
            splitLine:   { lineStyle: { type: 'dashed' as const, color: '#f1f5f9' } },
            axisLabel:   { fontSize: 11, color: chartTextColor },
        },
        series: [{
            type:        'bar' as const,
            barMaxWidth: 40,
            data: violCounts.map(v => ({
                value:     v,
                itemStyle: { color: v === 0 ? '#e2e8f0' : '#1A5F96', borderRadius: [4, 4, 0, 0] },
            })),
        }],
    };

    return (
        <div className="bm-page">
            <PageHeader
                title={deptName}
                subtitle="Department Overview"
                onMenuClick={() => setSidebarVisible(true)}
            />
            <SidebarComponent visible={sidebarVisible} onHide={() => setSidebarVisible(false)} />

            <div className="bm-content" style={{ maxWidth: '1100px' }}>

                {/* Breadcrumb + active-warnings indicator */}
                <div className="bm-breadcrumb">
                    <button className="bm-breadcrumb-btn" onClick={() => navigate(-1)}>
                        <i className="pi pi-arrow-left" style={{ marginRight: '0.3rem', fontSize: '0.8rem' }} />
                        Company Overview
                    </button>
                    <span className="bm-breadcrumb-sep">›</span>
                    <span className="bm-breadcrumb-current">{deptName}</span>

                    {/* right-side warning pill */}
                    {!loading && (
                        <span style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                            {activeWarnings > 0 ? (
                                <>
                                    <span className="card-alert-dot" />
                                    <span style={{ fontSize: '0.78rem', fontWeight: 600, color: '#e05252' }}>
                                        {activeWarnings} active {activeWarnings === 1 ? 'warning' : 'warnings'}
                                    </span>
                                </>
                            ) : (
                                <span style={{ fontSize: '0.78rem', fontWeight: 600, color: '#2DAA9E' }}>
                                    <i className="pi pi-check-circle" style={{ marginRight: '0.3rem', fontSize: '0.8rem' }} />
                                    No warnings
                                </span>
                            )}
                        </span>
                    )}
                </div>

                {/* ── Climate averages ── */}
                <p className="bm-section-heading">Climate Averages · Weekly Trend</p>
                <div className="card-grid" style={{ marginBottom: '1.25rem' }}>
                    <Cards
                        title="Temperature"
                        value={fmtTemp(lastStats?.avgTemperature)}
                        unit={tempUnit}
                        color="#e05252"
                        dataPoints={tempSparkline}
                        trendIcon={tempTrend.icon}
                        trendText={tempTrend.text}
                    />
                    <Cards
                        title="Humidity"
                        value={fmt(lastStats?.avgHumidity)}
                        unit="%"
                        color="#26a69a"
                        dataPoints={humSparkline}
                        trendIcon={humTrend.icon}
                        trendText={humTrend.text}
                    />
                    <Cards
                        title="Air Quality (CO₂)"
                        value={fmt(lastStats?.avgAirQuality, 0)}
                        unit="ppm"
                        color="#d4891a"
                        dataPoints={aqSparkline}
                        trendIcon={aqTrend.icon}
                        trendText={aqTrend.text}
                    />
                </div>

                {/* ── Trend score cards ── */}
                <div className="card-grid" style={{ gridTemplateColumns: '1fr 1fr', marginBottom: '2rem' }}>

                    {/* Trend score — week */}
                    <Cards
                        title="Trend Score · Week"
                        value={fmtScore(trendWeekCurrent)}
                        unit="/ 100"
                        color={scoreColor(trendWeekCurrent)}
                        dataPoints={trendWeekValues}
                        trendIcon={weekScoreTrend.icon}
                        trendText={weekScoreTrend.text}
                    />

                    {/* Trend score — month */}
                    <Cards
                        title="Trend Score · Month"
                        value={fmtScore(trendMonthCurrent)}
                        unit="/ 100"
                        color={scoreColor(trendMonthCurrent)}
                        dataPoints={trendMonthValues}
                        trendIcon={monthScoreTrend.icon}
                        trendText={monthScoreTrend.text}
                    />
                </div>

                {/* ── Violations chart ── */}
                <div className="table-container card chart-card">
                    <div className="flex-header">
                        <h3>Threshold Violations</h3>
                        <div className="time-filters">
                            {(['Week', 'Month', 'Year'] as ViolView[]).map(v => (
                                <button
                                    key={v}
                                    className={`time-filter-btn${violView === v ? ' active' : ''}`}
                                    onClick={() => setViolView(v)}
                                >
                                    {v}
                                </button>
                            ))}
                        </div>
                    </div>
                    <div style={{ padding: '0.5rem 1.5rem 1.5rem' }}>
                        {loading ? (
                            <div className="chart-loading">Loading…</div>
                        ) : (
                            <ReactECharts
                                option={violOption}
                                style={{ height: '260px' }}
                                notMerge
                            />
                        )}
                    </div>
                </div>

            </div>
        </div>
    );
};

export default DepartmentDetailPage;
