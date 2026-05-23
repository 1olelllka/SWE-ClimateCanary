import React, { useState, useEffect, useCallback, useRef } from 'react';
import ReactECharts from 'echarts-for-react';
import { GetClimateHistoryGranularityEnum, RoomControllerApi } from '../generated-skeleton-api';
import { DashboardCalendar } from './Calendar';
import { Toast } from 'primereact/toast';
import '../styles/TimeFilter.css';
import '../styles/ClimateHistoryChart.css';

const COLORS = {
    temperature: '#e05252',
    humidity:    '#26a69a',
    airQuality:  '#d4891a',
} as const;

type Metric     = 'All' | 'Temperature' | 'Humidity' | 'Air Quality';
type TimeFilter = 'Day' | 'Week' | 'Month' | 'Custom';

interface DataPoint {
    label:       string;
    temperature: number;
    humidity:    number;
    airQuality:  number;
}

interface Limits {
    tempMin: number;
    tempMax: number;
    humMin:  number;
    humMax:  number;
    co2Max:  number;
}

interface RawPoint {
    timestamp:  string;
    temperature: number;
    humidity:    number;
    airQuality:  number;
}

interface AggPoint {
    date:            string;
    avgTemperature:  number;
    avgHumidity:     number;
    avgAirQuality:   number;
}

interface Props {
    roomId: string;
    hideDayView?: boolean;
}

const mean = (nums: number[]) =>
    nums.length ? nums.reduce((a, b) => a + b, 0) / nums.length : 0;

function groupByHour(raw: RawPoint[]): DataPoint[] {
    const p2 = (n: number) => String(n).padStart(2, '0');
    const groups = new Map<string, RawPoint[]>();
    raw.forEach(p => {
        const d = new Date(p.timestamp);
        const key = `${d.getFullYear()}-${p2(d.getMonth() + 1)}-${p2(d.getDate())}T${p2(d.getHours())}`;
        groups.set(key, [...(groups.get(key) ?? []), p]);
    });
    return [...groups.entries()]
        .sort(([a], [b]) => a.localeCompare(b))
        .map(([key, pts]) => ({
            label:       key.slice(11) + ':00',
            temperature: mean(pts.map(p => p.temperature)),
            humidity:    mean(pts.map(p => p.humidity)),
            airQuality:  mean(pts.map(p => p.airQuality)),
        }));
}

function groupByDay(raw: RawPoint[]): DataPoint[] {
    const p2 = (n: number) => String(n).padStart(2, '0');
    const groups = new Map<string, RawPoint[]>();
    raw.forEach(p => {
        const d = new Date(p.timestamp);
        const key = `${d.getFullYear()}-${p2(d.getMonth() + 1)}-${p2(d.getDate())}`;
        groups.set(key, [...(groups.get(key) ?? []), p]);
    });
    return [...groups.entries()]
        .sort(([a], [b]) => a.localeCompare(b))
        .map(([key, pts]) => ({
            label:       key,
            temperature: mean(pts.map(p => p.temperature)),
            humidity:    mean(pts.map(p => p.humidity)),
            airQuality:  mean(pts.map(p => p.airQuality)),
        }));
}

function round2(n: number) { return Math.round(n * 100) / 100; }

function findNoDataZones(pts: DataPoint[]): Array<[{ xAxis: string }, { xAxis: string }]> {
    const out: Array<[{ xAxis: string }, { xAxis: string }]> = [];
    let start: string | null = null;
    for (const pt of pts) {
        const empty = Math.abs(pt.temperature) < 0.001
                   && Math.abs(pt.humidity) < 0.001
                   && Math.abs(pt.airQuality) < 0.001;
        if (empty && start == null) { start = pt.label; }
        else if (!empty && start != null) { out.push([{ xAxis: start }, { xAxis: pt.label }]); start = null; }
    }
    if (start != null && pts.length > 0) {
        out.push([{ xAxis: start }, { xAxis: pts[pts.length - 1].label }]);
    }
    return out;
}

const pad2 = (n: number) => String(n).padStart(2, '0');
const fmtDate = (d: Date) => `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
const fmtTime = (d: Date) => `${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`;

export const ClimateHistoryChart: React.FC<Props> = ({ roomId, hideDayView = false }) => {
    const toastRef = useRef<Toast>(null);
    const [timeFilter, setTimeFilter] = useState<TimeFilter>(hideDayView ? 'Week' : 'Day');
    const [dateRange,  setDateRange]  = useState<Date[] | null>(null);
    const [metric,     setMetric]     = useState<Metric>('All');
    const [data,       setData]       = useState<DataPoint[]>([]);
    const [limits,     setLimits]     = useState<Limits | null>(null);
    const [loading,    setLoading]    = useState(false);

    useEffect(() => {
        new RoomControllerApi().getLimitsForRoom({ roomId })
            .then(r => setLimits(r.data as any))
            .catch(() => {});
    }, [roomId]);

    const fetchData = useCallback(() => {
        setLoading(true);
        let req: Promise<DataPoint[]>;

        if (timeFilter === 'Day') {
            const now = new Date();
            // Align to last complete hour so labels are symmetric (e.g. 10:00–10:00)
            const end = new Date(now);
            end.setMinutes(0, 0, 0);
            end.setHours(end.getHours() - 1);
            const start = new Date(end.getTime() - 24 * 60 * 60 * 1000);
            req = new RoomControllerApi().getOvertimeClimateData({
                    roomId,
                    startDate: fmtDate(start),
                    endDate:   fmtDate(end),
                    startTime: fmtTime(start),
                    endTime:   fmtTime(end),
                })
                .then(r => groupByHour(r.data as RawPoint[]));

        } else if (timeFilter === 'Custom' && dateRange?.[0] && dateRange?.[1]) {
            const startDate = dateRange[0].toISOString().slice(0, 10);
            const endDate   = dateRange[1].toISOString().slice(0, 10);
            const diffDays  = Math.round(
                (dateRange[1].getTime() - dateRange[0].getTime()) / 86_400_000,
            );

            if (diffDays < 3) {
                setLoading(false);
                toastRef.current?.show({
                    severity: 'warn',
                    summary:  'Range too short',
                    detail:   'Please select a range of at least 3 days.',
                    life:     4000,
                });
                setTimeFilter('Week');
                setDateRange(null);
                return;
            } else {
                // Multi-day — use aggregated climate-history with smart granularity
                const granularity = diffDays <= 4
                    ? 'HOUR'
                    : diffDays > 4 && diffDays < 45 ? 'DAY'
                    : 'WEEK';

                req = new RoomControllerApi().getClimateHistory({
                        roomId,
                        startDate,
                        endDate,
                        granularity: granularity as GetClimateHistoryGranularityEnum,
                    })
                    .then(r => (r.data as AggPoint[]).map(d => ({
                        label:       d.date,
                        temperature: d.avgTemperature,
                        humidity:    d.avgHumidity,
                        airQuality:  d.avgAirQuality,
                    })));
                req.then(setData).catch(() => setData([])).finally(() => setLoading(false));
            }
        } else {
            // const tfMap: Record<string, string> = { Week: 'WEEK', Month: 'MONTH' };
            let startDate;
            if (timeFilter == 'Month') {
                startDate = fmtDate(new Date(new Date().getTime() - 1000 * 60 * 60 * 24 * 31))
            } else {
                startDate = fmtDate(new Date(new Date().getTime() - 1000 * 60 * 60 * 24 * 7))
            }

            req = new RoomControllerApi().getClimateHistory({
                    roomId,
                    startDate,
                    endDate: fmtDate(new Date()),
                    granularity: GetClimateHistoryGranularityEnum.DAY,
                })
                .then(r => (r.data as AggPoint[]).map(d => ({
                    label:       d.date,
                    temperature: d.avgTemperature,
                    humidity:    d.avgHumidity,
                    airQuality:  d.avgAirQuality,
                })));
        }

        req.then(setData).catch(() => setData([])).finally(() => setLoading(false));
    }, [roomId, timeFilter, dateRange]);

    useEffect(() => {
        if (timeFilter === 'Custom' && (!dateRange?.[0] || !dateRange?.[1])) return;
        fetchData();
    }, [fetchData, timeFilter, dateRange]);

    const showTemp = metric === 'All' || metric === 'Temperature';
    const showHum  = metric === 'All' || metric === 'Humidity';
    const showAQ   = metric === 'All' || metric === 'Air Quality';

    const L = limits;

    // Contiguous ranges where a metric violates its limits
    function violationRanges(
        pts: DataPoint[],
        getValue: (d: DataPoint) => number,
        min: number | null,
        max: number | null,
    ): Array<[{ xAxis: string }, { xAxis: string }]> {
        if (!pts.length || (min == null && max == null)) return [];
        const out: Array<[{ xAxis: string }, { xAxis: string }]> = [];
        let start: string | null = null;
        for (const pt of pts) {
            const v = getValue(pt);
            const bad = (min != null && v < min) || (max != null && v > max);
            if (bad && start == null) { start = pt.label; }
            else if (!bad && start != null) { out.push([{ xAxis: start }, { xAxis: pt.label }]); start = null; }
        }
        if (start != null) out.push([{ xAxis: start }, { xAxis: pts[pts.length - 1].label }]);
        return out;
    }

    function mkLimitLines(entries: Array<{ yAxis: number; name: string }>, color: string) {
        const valid = entries.filter(e => e.yAxis != null && Number.isFinite(e.yAxis));
        if (!valid.length) return undefined;
        return {
            silent:    true,
            symbol:    ['none', 'none'] as const,
            animation: false,
            lineStyle: { color, type: 'dashed' as const, width: 1.5, opacity: 0.8 },
            label:     { show: true, fontSize: 9, color, position: 'insideEndTop' as const, formatter: '{b}' },
            data:      valid,
        };
    }

    function mkViolationArea(
        ranges: Array<[{ xAxis: string }, { xAxis: string }]>,
        color: string,
    ) {
        if (!ranges.length) return undefined;
        return {
            silent:    true,
            itemStyle: { color: color + '33', borderWidth: 0 },
            label:     { show: false },
            data:      ranges,
        };
    }

    const noDataZones = findNoDataZones(data);

    const series = [
        // Phantom series — only used to render the no-data grey zones across all metrics
        {
            name: '__nodata__',
            type: 'line' as const,
            data: [],
            lineStyle: { opacity: 0 },
            itemStyle: { opacity: 0 },
            markArea: noDataZones.length > 0 ? {
                silent: true,
                itemStyle: { color: 'rgba(148, 163, 184, 0.18)', borderWidth: 0 },
                label: {
                    show: true,
                    position: 'insideTop' as const,
                    color: '#94a3b8',
                    fontSize: 10,
                    formatter: () => 'No data',
                },
                data: noDataZones,
            } : undefined,
        },
        showTemp && {
            name:      'Temperature (°C)',
            type:      'line',
            smooth:    true,
            symbol:    'none',
            lineStyle: { color: COLORS.temperature, width: 2 },
            itemStyle: { color: COLORS.temperature },
            data:      data.map(d => round2(d.temperature)),
            markLine:  L && metric === 'Temperature' ? mkLimitLines([
                { yAxis: L.tempMin, name: `T ${L.tempMin}°C` },
                { yAxis: L.tempMax, name: `T ${L.tempMax}°C` },
            ], COLORS.temperature) : undefined,
            markArea: L ? mkViolationArea(
                violationRanges(data, d => d.temperature, L.tempMin ?? null, L.tempMax ?? null),
                COLORS.temperature,
            ) : undefined,
        },
        showHum && {
            name:      'Humidity (%)',
            type:      'line',
            smooth:    true,
            symbol:    'none',
            lineStyle: { color: COLORS.humidity, width: 2 },
            itemStyle: { color: COLORS.humidity },
            data:      data.map(d => round2(d.humidity)),
            markLine:  L && metric === 'Humidity' ? mkLimitLines([
                { yAxis: L.humMin, name: `H ${L.humMin}%` },
                { yAxis: L.humMax, name: `H ${L.humMax}%` },
            ], COLORS.humidity) : undefined,
            markArea: L ? mkViolationArea(
                violationRanges(data, d => d.humidity, L.humMin ?? null, L.humMax ?? null),
                COLORS.temperature,
            ) : undefined,
        },
        showAQ && {
            name:      'Air Quality (ppm)',
            type:      'line',
            smooth:    true,
            symbol:    'none',
            lineStyle: { color: COLORS.airQuality, width: 2 },
            itemStyle: { color: COLORS.airQuality },
            data:      data.map(d => round2(d.airQuality)),
            markLine:  L && metric === 'Air Quality' ? mkLimitLines([
                { yAxis: L.co2Max, name: `CO₂ ${L.co2Max}` },
            ], COLORS.airQuality) : undefined,
            markArea: L ? mkViolationArea(
                violationRanges(data, d => d.airQuality, null, L.co2Max ?? null),
                COLORS.temperature,
            ) : undefined,
        },
    ].filter(Boolean);

    const rotateLabels = timeFilter === 'Day' || timeFilter === 'Custom';
    const showLegend   = metric === 'All';

    // Rotated labels need ~44 px; legend needs ~28 px; combine when both are present
    const gridBottom = (rotateLabels ? 44 : 20) + (showLegend ? 28 : 0);

    const yAxisMax: number | undefined = (() => {
        if (L == null || !data.length) return undefined;
        if (metric === 'Temperature') {
            const top = Math.max(data.reduce((m, d) => Math.max(m, d.temperature), -Infinity), L.tempMax);
            return Math.ceil(top + Math.abs(top) * 0.1);
        }
        if (metric === 'Humidity') {
            const top = Math.max(data.reduce((m, d) => Math.max(m, d.humidity), 0), L.humMax);
            return Math.ceil(top * 1.1);
        }
        if (metric === 'Air Quality') {
            const top = Math.max(data.reduce((m, d) => Math.max(m, d.airQuality), 0), L.co2Max);
            return Math.ceil(top * 1.1);
        }
        return undefined;
    })();

    const yAxisMin: number | undefined = (() => {
        if (L == null || !data.length) return undefined;
        if (metric === 'Temperature') {
            const bottom = Math.min(data.reduce((m, d) => Math.min(m, d.temperature), Infinity), L.tempMin);
            return Math.floor(bottom - Math.abs(bottom) * 0.1);
        }
        if (metric === 'Humidity') {
            const bottom = Math.min(data.reduce((m, d) => Math.min(m, d.humidity), Infinity), L.humMin);
            return Math.max(0, Math.floor(bottom * 0.9));
        }
        return undefined;
    })();

    const option = {
        tooltip: {
            trigger:     'axis' as const,
            axisPointer: { type: 'cross' as const },
            formatter: (params: any[]) => {
                if (!params.length) return '';
                let out = `<div style="margin-bottom:4px;font-weight:600">${params[0].name}</div>`;
                for (const p of params) {
                    if (p.seriesName === '__nodata__') continue;
                    const v = p.value as number;
                    let violated = false;
                    if (L) {
                        if (p.seriesName === 'Temperature (°C)')
                            violated = (L.tempMin != null && v < L.tempMin) || (L.tempMax != null && v > L.tempMax);
                        else if (p.seriesName === 'Humidity (%)')
                            violated = (L.humMin != null && v < L.humMin) || (L.humMax != null && v > L.humMax);
                        else if (p.seriesName === 'Air Quality (ppm)')
                            violated = L.co2Max != null && v > L.co2Max;
                    }
                    const color = violated ? '#ef4444' : 'inherit';
                    out += `<div style="display:flex;align-items:center;gap:6px;margin-top:3px">
                        <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${p.color}"></span>
                        <span style="color:${color}">${p.seriesName}:&nbsp;<b>${v}</b></span>
                    </div>`;
                }
                return out;
            },
        },
        legend: {
            show:      showLegend,
            bottom:    0,
            data:      ['Temperature (°C)', 'Humidity (%)', 'Air Quality (ppm)'],
            textStyle: { fontSize: 11 },
        },
        grid: {
            left:         48,
            right:        24,
            top:          16,
            bottom:       gridBottom,
            containLabel: false,
        },
        xAxis: {
            type:        'category' as const,
            boundaryGap: false,
            data:        data.map(d => d.label),
            axisLabel: {
                rotate:   rotateLabels ? 35 : 0,
                fontSize: 11,
                align:    rotateLabels ? 'right' : 'center',
            },
            axisLine: { lineStyle: { color: '#e2e8f0' } },
        },
        yAxis: {
            type:      'value' as const,
            max:       yAxisMax,
            min:       yAxisMin,
            nice: true,
            splitLine: { lineStyle: { type: 'dashed' as const, color: '#f1f5f9' } },
            axisLabel: { fontSize: 11 },
        },
        series,
    };

    return (
        <div className="chart-card">
            <Toast ref={toastRef} position="top-right" />
            <div className="chart-header">
                <div className="time-filters">
                    {(['Day', 'Week', 'Month'] as TimeFilter[]).filter(f => !(hideDayView && f === 'Day')).map(f => (
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

                <select
                    className="metric-select"
                    value={metric}
                    onChange={e => setMetric(e.target.value as Metric)}
                >
                    <option value="All">All metrics</option>
                    <option value="Temperature">Temperature</option>
                    <option value="Humidity">Humidity</option>
                    <option value="Air Quality">Air Quality</option>
                </select>
            </div>

            {loading ? (
                <div className="chart-loading">Loading…</div>
            ) : data.length === 0 ? (
                <div className="chart-loading">No data available</div>
            ) : (
                <ReactECharts option={option} style={{ height: 300 }} notMerge />
            )}
        </div>
    );
};
