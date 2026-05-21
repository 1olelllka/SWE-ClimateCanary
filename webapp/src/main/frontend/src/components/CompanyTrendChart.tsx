import React, { useState, useEffect, useCallback, useRef } from 'react';
import ReactECharts from 'echarts-for-react';
import { DashboardCalendar } from './Calendar';
import { BuildingTrendControllerApi, BuildingTrendDTO } from '../generated-skeleton-api';
import { DepartmentWithStats } from '../views/SeniorManagerDashboard';
import { Toast } from 'primereact/toast';
import '../styles/TimeFilter.css';
import '../styles/ClimateHistoryChart.css';

type TimeFilter = 'Week' | 'Month' | 'Custom';
type Selection  = 'all' | 'company' | string;

interface Props {
    departments: DepartmentWithStats[];
}

const trendApi = new BuildingTrendControllerApi();

const DEPT_COLORS = [
    '#3b82f6', '#10b981', '#f59e0b', '#8b5cf6',
    '#ef4444', '#06b6d4', '#84cc16', '#f97316',
    '#6366f1', '#14b8a6', '#e879f9', '#fb923c',
];
const COMPANY_COLOR = '#1e3a8a';

const pad2    = (n: number) => String(n).padStart(2, '0');
const fmtDate = (d: Date)   => `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;

function dateRange(filter: TimeFilter, custom: Date[] | null): { startDate: string; endDate: string } | null {
    const now = new Date();
    const end = fmtDate(now);
    if (filter === 'Week') {
        const s = new Date(now); s.setDate(s.getDate() - 7);
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
                    smooth:    true,
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
                smooth:    true,
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
                smooth:    true,
                symbol:    'none',
                z:         10,
                lineStyle: { color: COMPANY_COLOR, width: 3 },
                itemStyle: { color: COMPANY_COLOR },
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
        },
        legend: {
            bottom:    0,
            data:      legendNames,
            textStyle: { fontSize: 11 },
            type:      'scroll' as const,
        },
        grid: { left: 52, right: 24, top: 16, bottom: 60 },
        xAxis: {
            type:        'category' as const,
            boundaryGap: false,
            data:        allDates,
            axisLabel:   { rotate: 20, fontSize: 11 },
            axisLine:    { lineStyle: { color: '#e2e8f0' } },
        },
        yAxis: {
            type:      'value' as const,
            min:       0,
            max:       1,
            splitLine: { lineStyle: { type: 'dashed' as const, color: '#f1f5f9' } },
            axisLabel: { fontSize: 11 },
        },
        series,
    };

    const noData = !loading && allDates.length === 0;

    return (
        <div className="table-container card chart-card">
            <Toast ref={toastRef} />
            <div className="flex-header">
                <h3>Climate Trend Indicator</h3>
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
            </div>

            <div style={{ padding: '1rem 1.5rem 0.5rem' }}>
                <select
                    className="metric-select"
                    value={selection}
                    onChange={e => setSelection(e.target.value)}
                    style={{ minWidth: '180px' }}
                >
                    <option value="all">All</option>
                    <option value="company">Company</option>
                    {departments.map(d => (
                        <option key={d.id} value={d.id}>{d.name}</option>
                    ))}
                </select>
            </div>

            <div style={{ padding: '0.5rem 1.5rem 1.5rem' }}>
                {loading ? (
                    <div className="chart-loading">Loading…</div>
                ) : error ? (
                    <div className="chart-loading">Failed to load trend data.</div>
                ) : noData ? (
                    <div className="chart-loading">No trend data available for this period.</div>
                ) : (
                    <ReactECharts option={option} style={{ height: '340px' }} notMerge />
                )}
            </div>
        </div>
    );
};
