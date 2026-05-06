import React, { useState, useEffect, useCallback } from 'react';
import ReactECharts from 'echarts-for-react';
import globalAxios from 'axios';
import { DashboardCalendar } from './Calendar';
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
}

const mean = (nums: number[]) =>
    nums.length ? nums.reduce((a, b) => a + b, 0) / nums.length : 0;

function groupByHour(raw: RawPoint[]): DataPoint[] {
    const groups = new Map<string, RawPoint[]>();
    raw.forEach(p => {
        const key = p.timestamp.slice(0, 13); // "2024-06-15T10"
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
    const groups = new Map<string, RawPoint[]>();
    raw.forEach(p => {
        const key = p.timestamp.slice(0, 10);
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

export const ClimateHistoryChart: React.FC<Props> = ({ roomId }) => {
    const [timeFilter, setTimeFilter] = useState<TimeFilter>('Week');
    const [dateRange,  setDateRange]  = useState<Date[] | null>(null);
    const [metric,     setMetric]     = useState<Metric>('All');
    const [data,       setData]       = useState<DataPoint[]>([]);
    const [limits,     setLimits]     = useState<Limits | null>(null);
    const [loading,    setLoading]    = useState(false);

    useEffect(() => {
        globalAxios.get<Limits>(`/api/rooms/${roomId}/climate-limits`)
            .then(r => setLimits(r.data))
            .catch(() => {});
    }, [roomId]);

    const fetchData = useCallback(() => {
        setLoading(true);
        let req: Promise<DataPoint[]>;

        if (timeFilter === 'Day') {
            const today = new Date().toISOString().slice(0, 10);
            req = globalAxios
                .get<RawPoint[]>(`/api/rooms/${roomId}/overtime`, {
                    params: { startDate: today, endDate: today },
                })
                .then(r => groupByHour(r.data));

        } else if (timeFilter === 'Custom' && dateRange?.[0] && dateRange?.[1]) {
            const startDate = dateRange[0].toISOString().slice(0, 10);
            const endDate   = dateRange[1].toISOString().slice(0, 10);
            const diffDays  = Math.round(
                (dateRange[1].getTime() - dateRange[0].getTime()) / 86_400_000,
            );
            req = globalAxios
                .get<RawPoint[]>(`/api/rooms/${roomId}/overtime`, {
                    params: { startDate, endDate },
                })
                .then(r => diffDays > 2 ? groupByDay(r.data) : groupByHour(r.data));

        } else {
            const tfMap: Record<string, string> = { Week: 'WEEK', Month: 'MONTH' };
            req = globalAxios
                .get<AggPoint[]>(`/api/rooms/${roomId}/climate-history`, {
                    params: { timeframe: tfMap[timeFilter] ?? 'WEEK', granularity: 'DAY' },
                })
                .then(r => r.data.map(d => ({
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

    // Reference lines only when a single metric is selected (avoids Y-axis scaling conflicts)
    const markLine = (
        minVal: number | null,
        maxVal: number | null,
        color:  string,
    ) => ({
        silent: true,
        symbol: ['none', 'none'],
        lineStyle: { color, type: 'dashed' as const, width: 1, opacity: 0.75 },
        label:     { fontSize: 10, color },
        data: [
            ...(minVal != null ? [{ yAxis: minVal, name: `Min ${minVal}` }] : []),
            ...(maxVal != null ? [{ yAxis: maxVal, name: `Max ${maxVal}` }] : []),
        ],
    });

    const series = [
        showTemp && {
            name:      'Temperature (°C)',
            type:      'line',
            smooth:    true,
            symbol:    'none',
            lineStyle: { color: COLORS.temperature, width: 2 },
            itemStyle: { color: COLORS.temperature },
            data:      data.map(d => round2(d.temperature)),
            markLine:  metric === 'Temperature' && limits
                ? markLine(limits.tempMin, limits.tempMax, COLORS.temperature)
                : undefined,
        },
        showHum && {
            name:      'Humidity (%)',
            type:      'line',
            smooth:    true,
            symbol:    'none',
            lineStyle: { color: COLORS.humidity, width: 2 },
            itemStyle: { color: COLORS.humidity },
            data:      data.map(d => round2(d.humidity)),
            markLine:  metric === 'Humidity' && limits
                ? markLine(limits.humMin, limits.humMax, COLORS.humidity)
                : undefined,
        },
        showAQ && {
            name:      'Air Quality (ppm)',
            type:      'line',
            smooth:    true,
            symbol:    'none',
            lineStyle: { color: COLORS.airQuality, width: 2 },
            itemStyle: { color: COLORS.airQuality },
            data:      data.map(d => round2(d.airQuality)),
            markLine:  metric === 'Air Quality' && limits
                ? markLine(null, limits.co2Max, COLORS.airQuality)
                : undefined,
        },
    ].filter(Boolean);

    const rotateLabels = timeFilter === 'Day' || timeFilter === 'Custom';

    const option = {
        tooltip: {
            trigger:     'axis' as const,
            axisPointer: { type: 'cross' as const },
        },
        legend: {
            show:   metric === 'All',
            bottom: 0,
            data:   ['Temperature (°C)', 'Humidity (%)', 'Air Quality (ppm)'],
            textStyle: { fontSize: 11 },
        },
        grid: {
            left:         48,
            right:        24,
            top:          16,
            bottom:       metric === 'All' ? 48 : 28,
            containLabel: false,
        },
        xAxis: {
            type:        'category' as const,
            boundaryGap: false,
            data:        data.map(d => d.label),
            axisLabel:   { rotate: rotateLabels ? 35 : 0, fontSize: 11 },
            axisLine:    { lineStyle: { color: '#e2e8f0' } },
        },
        yAxis: {
            type:      'value' as const,
            splitLine: { lineStyle: { type: 'dashed' as const, color: '#f1f5f9' } },
            axisLabel: { fontSize: 11 },
        },
        series,
    };

    return (
        <div className="chart-card">
            <div className="chart-header">
                <div className="time-filters">
                    {(['Day', 'Week', 'Month'] as TimeFilter[]).map(f => (
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
