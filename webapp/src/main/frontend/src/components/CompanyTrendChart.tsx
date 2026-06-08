import React, { useState, useEffect, useCallback, useRef } from 'react';
import ReactECharts from 'echarts-for-react';
import { DashboardCalendar } from './Calendar';
import { BuildingTrendControllerApi, BuildingTrendDTO } from '../generated-skeleton-api';
import { DepartmentWithStats } from '../views/SeniorManagerDashboard';
import { Toast } from 'primereact/toast';
import '../styles/TimeFilter.css';
import '../styles/ClimateHistoryChart.css';

const useIsMobile = () => {
    const [isMobile, setIsMobile] = useState(window.innerWidth <= 899);
    useEffect(() => {
        const handler = () => setIsMobile(window.innerWidth <= 899);
        window.addEventListener('resize', handler);
        return () => window.removeEventListener('resize', handler);
    }, []);
    return isMobile;
};

type TimeFilter = 'Week' | 'Month' | 'Custom';
type Selection  = 'all' | 'company' | string;

interface Props {
    departments: DepartmentWithStats[];
}

const trendApi = new BuildingTrendControllerApi();

const DEPT_COLORS = [
    '#1A5F96', // deep blue
    '#2DAA9E', // teal
    '#66BB6A', // green
    '#00BCD4', // cyan
    '#2E7D32', // dark green
    '#4FC3F7', // sky blue
    '#004D40', // dark teal
    '#81D4FA', // light blue
    '#1565C0', // royal blue
    '#26A69A', // medium teal
    '#43A047', // forest green
    '#0288D1', // ocean blue
];
const COMPANY_COLOR = '#0f2d54'; // deep navy — always distinct from dept lines

const pad2    = (n: number) => String(n).padStart(2, '0');
const fmtDate = (d: Date)   => `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;

function dateRange(filter: TimeFilter, custom: Date[] | null): { startDate: string; endDate: string } | null {
    const now = new Date();
    const end = fmtDate(now);
    if (filter === 'Week') {
        const s = new Date(now); s.setDate(s.getDate() - 6);
        return { startDate: fmtDate(s), endDate: end };
    }
    if (filter === 'Month') {
        const s = new Date(now); s.setMonth(s.getMonth() - 1);
        return { startDate: fmtDate(s), endDate: end };
    }
    if (filter === 'Custom' && custom?.[0] && custom?.[1]) {
        return { startDate: fmtDate(custom[0]), endDate: fmtDate(custom[1]) };
    }
    return null;
}

export const CompanyTrendChart: React.FC<Props> = ({ departments }) => {
    const toastRef = useRef<Toast>(null);
    const isMobile = useIsMobile();

    const [selection,   setSelection]   = useState<Selection>('all');
    const [timeFilter,  setTimeFilter]  = useState<TimeFilter>('Week');
    const [customRange, setCustomRange] = useState<Date[] | null>(null);
    const [deptData,    setDeptData]    = useState<Map<string, BuildingTrendDTO[]>>(new Map());
    const [loading,     setLoading]     = useState(false);
    const [error,       setError]       = useState(false);

    const handleDateRangeChange = (dates: Date[] | null) => {
        if (dates?.[0]) {
            const minAllowed = new Date();
            minAllowed.setMonth(minAllowed.getMonth() - 3);
            if (dates[0] < minAllowed) {
                toastRef.current?.show({
                    severity: 'warn',
                    summary:  'Date out of range',
                    detail:   'You can only select dates up to 3 months in the past.',
                    life:     4000,
                });
                setCustomRange(null);
                return;
            }
        }
        setCustomRange(dates);
    };

    const fetchAll = useCallback(() => {
        if (departments.length === 0) return;
        const range = dateRange(timeFilter, customRange);
        if (!range) return;

        setLoading(true);
        setError(false);

        Promise.all(
            departments.map(d =>
                trendApi
                    .getTrendsForSpecificDepartment({
                        departmentId: d.id,
                        startDate:    range.startDate,
                        endDate:      range.endDate,
                    })
                    .then(r => ({ id: d.id, rows: r.data ?? [] }))
                    .catch(() => ({ id: d.id, rows: [] as BuildingTrendDTO[] }))
            )
        )
            .then(results => {
                const map = new Map<string, BuildingTrendDTO[]>();
                results.forEach(r => map.set(r.id, r.rows));
                setDeptData(map);
            })
            .catch(() => setError(true))
            .finally(() => setLoading(false));
    }, [departments, timeFilter, customRange]);

    useEffect(() => {
        if (timeFilter === 'Custom' && (!customRange?.[0] || !customRange?.[1])) return;
        fetchAll();
    }, [fetchAll, timeFilter, customRange]);

    // Build sorted unique dates across all departments
    const allDates = (() => {
        const set = new Set<string>();
        deptData.forEach(rows => rows.forEach(r => r.date && set.add(r.date)));
        return [...set].sort((a, b) => a.localeCompare(b));
    })();

    // Per-dept lookup: deptId -> date -> value
    const deptLookup = new Map<string, Map<string, number>>();
    deptData.forEach((rows, deptId) => {
        const byDate = new Map<string, number>();
        rows.forEach(r => { if (r.date != null && r.value != null) byDate.set(r.date, r.value); });
        deptLookup.set(deptId, byDate);
    });

    // Company aggregate: average of all departments per date
    const companyValues = allDates.map(date => {
        const vals: number[] = [];
        deptLookup.forEach(byDate => { const v = byDate.get(date); if (v != null) vals.push(v); });
        return vals.length ? vals.reduce((a, b) => a + b, 0) / vals.length : null;
    });

    const buildSeries = () => {
        const series: object[] = [];

        const showAll     = selection === 'all';
        const showCompany = selection === 'all' || selection === 'company';

        if (showAll) {
            departments.forEach((dept, i) => {
                const color  = DEPT_COLORS[i % DEPT_COLORS.length];
                const byDate = deptLookup.get(dept.id);
                series.push({
                    name:      dept.name,
                    type:      'line',
                    smooth:    0.3,
                    symbol:    'none',
                    lineStyle: { color, width: 1.5, opacity: 0.75 },
                    itemStyle: { color },
                    data:      allDates.map(d => byDate?.get(d) ?? null),
                });
            });
        } else if (selection !== 'company') {
            const idx    = departments.findIndex(d => d.id === selection);
            const color  = idx >= 0 ? DEPT_COLORS[idx % DEPT_COLORS.length] : DEPT_COLORS[0];
            const byDate = deptLookup.get(selection);
            const dept   = departments.find(d => d.id === selection);
            series.push({
                name:      dept?.name ?? 'Department',
                type:      'line',
                smooth:    0.3,
                symbol:    'none',
                lineStyle: { color, width: 2 },
                itemStyle: { color },
                data:      allDates.map(d => byDate?.get(d) ?? null),
            });
        }

        if (showCompany) {
            series.push({
                name:      'Company',
                type:      'line',
                smooth:    0.3,
                symbol:    'none',
                z:         10,
                lineStyle: { color: COMPANY_COLOR, width: 5 },
                itemStyle: { color: COMPANY_COLOR },
                areaStyle: { color: COMPANY_COLOR, opacity: 0.06 },
                data:      companyValues,
            });
        }

        return series;
    };

    const series = buildSeries();

    const legendNames =
        selection === 'all'
            ? [...departments.map(d => d.name), 'Company']
            : selection === 'company'
            ? ['Company']
            : [departments.find(d => d.id === selection)?.name ?? ''];

    const option = {
        tooltip: {
            trigger:     'axis' as const,
            axisPointer: { type: 'cross' as const },
            confine:     true,
            textStyle:   { fontSize: isMobile ? 11 : 12 },
            formatter:   (params: any[]) =>
                params.map(p =>
                    `${p.marker}${p.seriesName}: ${p.value != null ? Number(p.value).toFixed(2) : '—'}`
                ).join('<br/>'),
        },
        legend: {
            type:      'scroll' as const,
            orient:    'horizontal' as const,
            bottom:    0,
            height:    42,
            data:      legendNames,
            textStyle: { fontSize: isMobile ? 10 : 11 },
            formatter: isMobile
                ? (name: string) => name.length > 14 ? name.slice(0, 13) + '…' : name
                : undefined,
        },
        grid: {
            left:   isMobile ? 32 : 36,
            right:  isMobile ? 8  : 12,
            top:    isMobile ? 8  : 12,
            bottom: isMobile ? 72 : 80,
        },
        xAxis: {
            type:        'category' as const,
            boundaryGap: false,
            data:        allDates,
            axisLabel:   { rotate: isMobile ? 30 : 20, fontSize: isMobile ? 9 : 11 },
            axisLine:    { lineStyle: { color: '#e2e8f0' } },
        },
        yAxis: {
            type:      'value' as const,
            min:       0,
            max:       100,
            splitLine: { lineStyle: { type: 'dashed' as const, color: '#f1f5f9' } },
            axisLabel: { fontSize: isMobile ? 9 : 11 },
        },
        series,
    };

    const noData = !loading && allDates.length === 0;

    return (
        <div className="table-container">
            <Toast ref={toastRef} />
            <div className="flex-header">
                <h3>Climate Trend Indicator</h3>
                <div className="company-trend-controls">
                    <div className="time-filters">
                        {(['Week', 'Month'] as TimeFilter[]).map(f => (
                            <button
                                key={f}
                                className={`time-filter-btn${timeFilter === f ? ' active' : ''}`}
                                onClick={() => { setTimeFilter(f); setCustomRange(null); }}
                            >
                                {f}
                            </button>
                        ))}
                        <DashboardCalendar
                            dateRange={customRange}
                            setDateRange={handleDateRangeChange}
                            isActive={timeFilter === 'Custom'}
                            onActivate={() => setTimeFilter('Custom')}
                        />
                    </div>
                    <select
                        className="trend-dept-select"
                        value={selection}
                        onChange={e => setSelection(e.target.value)}
                    >
                        <option value="all">All departments</option>
                        <option value="company">Company average</option>
                        {departments.map(d => (
                            <option key={d.id} value={d.id}>{d.name}</option>
                        ))}
                    </select>
                </div>
            </div>

            <div className="company-trend-chart-area">
                {loading ? (
                    <div className="chart-loading">Loading…</div>
                ) : error ? (
                    <div className="chart-loading">Failed to load trend data.</div>
                ) : noData ? (
                    <div className="chart-loading">No trend data available for this period.</div>
                ) : (
                    <div className="company-trend-echarts-wrapper">
                        <ReactECharts option={option} style={{ height: '100%', width: '100%' }} notMerge />
                    </div>
                )}
            </div>
        </div>
    );
};
