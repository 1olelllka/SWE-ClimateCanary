import React, { useMemo, useState } from 'react';
import ReactECharts from 'echarts-for-react';
import { ThresholdViolationData } from './ThresholdViolationsTable';
import '../styles/Tables.css';
import '../styles/TimeFilter.css';

type TimeFilter = 'Week' | 'Month' | 'Year';

const ROOM_COLORS = [
    '#4a8fb5',
    '#5ca689',
    '#3b7ab0',
    '#6dba9e',
    '#2d6b93',
    '#7ccfb5',
    '#4a7da6',
    '#5db58d',
];

const parseDT = (dt: string): Date => {
    const [datePart = '', timePart = '00:00'] = dt.split(' ');
    const [dd, mm, yyyy] = datePart.split('.');
    const [hh, min] = timePart.split(':');
    return new Date(+yyyy, +mm - 1, +dd, +hh, +min);
};

interface Props {
    violations: ThresholdViolationData[];
    loading?: boolean;
}

export const RoomViolationsBarChart: React.FC<Props> = ({ violations, loading = false }) => {
    const [timeFilter, setTimeFilter] = useState<TimeFilter>('Week');
    const [showAll, setShowAll] = useState(false);

    const cutoff = useMemo(() => {
        const now = new Date();
        if (timeFilter === 'Week') {
            const d = new Date(now); d.setDate(d.getDate() - 7); return d;
        }
        if (timeFilter === 'Month') {
            const d = new Date(now); d.setMonth(d.getMonth() - 1); return d;
        }
        const d = new Date(now); d.setFullYear(d.getFullYear() - 1); return d;
    }, [timeFilter]);

    const rankedRooms = useMemo(() => {
        const counts = new Map<string, number>();
        violations.forEach(v => {
            // Use sortKey (ISO string) for reliable comparison regardless of display date format
            if (new Date(v.sortKey ?? v.datetime) >= cutoff) {
                counts.set(v.room, (counts.get(v.room) ?? 0) + 1);
            }
        });
        return [...counts.entries()].sort((a, b) => b[1] - a[1]);
    }, [violations, cutoff]);

    const displayed = showAll ? rankedRooms : rankedRooms.slice(0, 5);
    const hasMore = rankedRooms.length > 5;

    // Reverse so the most violated room appears at the top of the horizontal chart
    const reversed = [...displayed].reverse();
    const rooms  = reversed.map(([room]) => room);
    const counts = reversed.map(([, count]) => count);

    const chartHeight = Math.max(160, rooms.length * 56);

    const option = {
        tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'shadow' },
            formatter: (params: any[]) => {
                const p = params[0];
                return `<b>${p.name}</b><br/>${p.value} violation${p.value !== 1 ? 's' : ''}`;
            },
        },
        grid: { left: 12, right: 36, top: 12, bottom: 8, containLabel: true },
        xAxis: {
            type: 'value',
            minInterval: 1,
            axisLabel: { fontSize: 11, color: '#94a3b8' },
            splitLine: { lineStyle: { type: 'dashed' as const, color: '#f1f5f9' } },
        },
        yAxis: {
            type: 'category',
            data: rooms,
            axisLabel: {
                fontSize: 11,
                color: '#64748b',
                overflow: 'truncate',
                width: 80,
            },
            axisTick: { show: false },
            axisLine: { lineStyle: { color: '#e2e8f0' } },
        },
        series: [{
            type: 'bar',
            data: counts.map((val, i) => ({
                value: val,
                itemStyle: {
                    color: ROOM_COLORS[i % ROOM_COLORS.length],
                    borderRadius: [0, 4, 4, 0],
                },
            })),
            barMaxWidth: 30,
            label: {
                show: true,
                position: 'right',
                fontSize: 11,
                color: '#64748b',
                formatter: (p: any) => (p.value > 0 ? String(p.value) : ''),
            },
        }],
    };

    return (
        <div className="table-container">
            <div className="flex-header">
                <h3>Violations by Room</h3>
                <div className="time-filters">
                    {(['Week', 'Month', 'Year'] as TimeFilter[]).map(f => (
                        <button
                            key={f}
                            className={`time-filter-btn${timeFilter === f ? ' active' : ''}`}
                            onClick={() => { setTimeFilter(f); setShowAll(false); }}
                        >
                            {f}
                        </button>
                    ))}
                </div>
            </div>

            <div style={{ padding: '1rem 1.5rem 0.75rem' }}>
                {loading ? (
                    <div style={{ color: '#64748b', fontSize: '0.85rem', padding: '2rem 0', textAlign: 'center' }}>
                        Loading…
                    </div>
                ) : rankedRooms.length === 0 ? (
                    <div style={{ color: '#64748b', fontSize: '0.85rem', padding: '2rem 0', textAlign: 'center' }}>
                        No violations recorded for this period.
                    </div>
                ) : (
                    <ReactECharts
                        option={option}
                        style={{ height: `${chartHeight}px` }}
                        notMerge
                    />
                )}
            </div>

            {hasMore && rankedRooms.length > 0 && !loading && (
                <div style={{ padding: '0 1.5rem 1rem', textAlign: 'center' }}>
                    <button
                        className="btn-secondary"
                        onClick={() => setShowAll(v => !v)}
                        style={{ fontSize: '0.8rem' }}
                    >
                        {showAll ? 'Show top 5' : `See all ${rankedRooms.length} rooms`}
                    </button>
                </div>
            )}
        </div>
    );
};
