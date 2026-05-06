import React, { useState, useEffect, useCallback } from 'react';
import ReactECharts from 'echarts-for-react';
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

interface Props {
    departmentName: string;
}

function round2(n: number) { return Math.round(n * 100) / 100; }

function generateMockData(tf: TimeFilter, dateRange: Date[] | null): DataPoint[] {
    if (tf === 'Day') {
        return Array.from({ length: 24 }, (_, i) => ({
            label:       `${String(i).padStart(2, '0')}:00`,
            temperature: round2(20 + Math.sin(i / 4) * 3),
            humidity:    round2(50 + Math.cos(i / 5) * 8),
            airQuality:  round2(700 + Math.sin(i / 3) * 200),
        }));
    }
    if (tf === 'Month') {
        const today = new Date();
        return Array.from({ length: 30 }, (_, i) => {
            const d = new Date(today); d.setDate(today.getDate() - 29 + i);
            return {
                label:       d.toISOString().slice(0, 10),
                temperature: round2(22 + Math.sin(i / 8) * 2),
                humidity:    round2(52 + Math.cos(i / 6) * 6),
                airQuality:  round2(720 + Math.sin(i / 5) * 150),
            };
        });
    }
    if (tf === 'Custom' && dateRange?.[0] && dateRange?.[1]) {
        const days = Math.round((dateRange[1].getTime() - dateRange[0].getTime()) / 86_400_000) + 1;
        return Array.from({ length: days }, (_, i) => {
            const d = new Date(dateRange[0]!); d.setDate(d.getDate() + i);
            return {
                label:       d.toISOString().slice(0, 10),
                temperature: round2(21 + Math.sin(i / 4) * 2.5),
                humidity:    round2(51 + Math.cos(i / 5) * 7),
                airQuality:  round2(710 + Math.sin(i / 4) * 180),
            };
        });
    }
    const today = new Date();
    return Array.from({ length: 7 }, (_, i) => {
        const d = new Date(today); d.setDate(today.getDate() - 6 + i);
        return {
            label:       d.toISOString().slice(0, 10),
            temperature: round2(23 + Math.sin(i / 2) * 1.5),
            humidity:    round2(53 + Math.cos(i / 3) * 5),
            airQuality:  round2(730 + Math.sin(i / 2) * 120),
        };
    });
}

export const DepartmentTrendChart: React.FC<Props> = ({ departmentName }) => {
    const [timeFilter, setTimeFilter] = useState<TimeFilter>('Week');
    const [dateRange,  setDateRange]  = useState<Date[] | null>(null);
    const [metric,     setMetric]     = useState<Metric>('All');
    const [data,       setData]       = useState<DataPoint[]>([]);

    const refresh = useCallback(() => {
        setData(generateMockData(timeFilter, dateRange));
    }, [timeFilter, dateRange]);

    useEffect(() => {
        if (timeFilter === 'Custom' && (!dateRange?.[0] || !dateRange?.[1])) return;
        refresh();
    }, [refresh, timeFilter, dateRange]);

    const showTemp = metric === 'All' || metric === 'Temperature';
    const showHum  = metric === 'All' || metric === 'Humidity';
    const showAQ   = metric === 'All' || metric === 'Air Quality';

    const series = [
        showTemp && {
            name:      'Temperature (°C)',
            type:      'line',
            smooth:    true,
            symbol:    'none',
            lineStyle: { color: COLORS.temperature, width: 2 },
            itemStyle: { color: COLORS.temperature },
            data:      data.map(d => d.temperature),
        },
        showHum && {
            name:      'Humidity (%)',
            type:      'line',
            smooth:    true,
            symbol:    'none',
            lineStyle: { color: COLORS.humidity, width: 2 },
            itemStyle: { color: COLORS.humidity },
            data:      data.map(d => d.humidity),
        },
        showAQ && {
            name:      'Air Quality (ppm)',
            type:      'line',
            smooth:    true,
            symbol:    'none',
            lineStyle: { color: COLORS.airQuality, width: 2 },
            itemStyle: { color: COLORS.airQuality },
            data:      data.map(d => d.airQuality),
        },
    ].filter(Boolean);

    const option = {
        tooltip: { trigger: 'axis' as const, axisPointer: { type: 'cross' as const } },
        legend: {
            show:      metric === 'All',
            bottom:    0,
            data:      ['Temperature (°C)', 'Humidity (%)', 'Air Quality (ppm)'],
            textStyle: { fontSize: 11 },
        },
        grid: { left: 48, right: 24, top: 16, bottom: metric === 'All' ? 48 : 28 },
        xAxis: {
            type:        'category' as const,
            boundaryGap: false,
            data:        data.map(d => d.label),
            axisLabel:   { rotate: timeFilter === 'Day' ? 35 : 0, fontSize: 11 },
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
                <h3 style={{ margin: 0, fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-primary, #1e293b)' }}>
                    {departmentName} — Climate Trend
                </h3>
                <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center', flexWrap: 'wrap' }}>
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
            </div>

            {data.length === 0 ? (
                <div className="chart-loading">Loading…</div>
            ) : (
                <ReactECharts option={option} style={{ height: 300 }} notMerge />
            )}
        </div>
    );
};
