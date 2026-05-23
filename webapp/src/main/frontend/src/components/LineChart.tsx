import React from 'react';
import ReactECharts from 'echarts-for-react';

interface MiniSparklineProps {
    readonly color: string;
    readonly points?: string;        // legacy SVG string — used by RoomDetailPage etc.
    readonly dataPoints?: number[];  // ECharts mode: actual metric values
}

function buildOption(color: string, dataPoints: number[]) {
    const allValues = dataPoints.filter(Number.isFinite);
    const rawMin = Math.min(...allValues);
    const rawMax = Math.max(...allValues);
    const spread = rawMax - rawMin || 1;
    const pad = spread * 0.2;

    return {
        animation: false,
        tooltip: { show: false },
        grid: { top: 6, right: 4, bottom: 6, left: 4, containLabel: false },
        xAxis: {
            type:        'category' as const,
            show:        false,
            boundaryGap: false,
            data:        dataPoints.map((_, i) => i),
        },
        yAxis: {
            type: 'value' as const,
            show: false,
            min:  rawMin - pad,
            max:  rawMax + pad,
        },
        series: [
            {
                type:      'line' as const,
                smooth:    0.4,
                symbol:    'none',
                lineStyle: { color, width: 2 },
                itemStyle: { color },
                areaStyle: {
                    color: {
                        type: 'linear' as const,
                        x: 0, y: 0, x2: 0, y2: 1,
                        colorStops: [
                            { offset: 0, color: color + '30' },
                            { offset: 1, color: color + '05' },
                        ],
                        global: false,
                    },
                },
                data: dataPoints,
            },
        ],
    };
}

export const LineChart: React.FC<MiniSparklineProps> = ({
    color,
    points,
    dataPoints,
}) => {
    // ── ECharts mode (EmployeeDashboard) ──────────────────────────────────────
    if (dataPoints !== undefined) {
        if (dataPoints.length === 0) {
            return (
                <div style={{ height: 56, display: 'flex', alignItems: 'center', margin: '0.25rem 0' }}>
                    <svg width="100%" height="2" style={{ display: 'block', opacity: 0.2 }}>
                        <line x1="0" y1="1" x2="100%" y2="1"
                            stroke={color} strokeWidth="1.5" strokeDasharray="4 3" />
                    </svg>
                </div>
            );
        }

        return (
            <ReactECharts
                option={buildOption(color, dataPoints)}
                style={{ height: 56, width: '100%', margin: '0.25rem 0' }}
                opts={{ renderer: 'svg' }}
                notMerge
            />
        );
    }

    // ── Legacy SVG mode (RoomDetailPage, static mock data) ─────────────
    return (
        <div style={{ height: '40px', width: '100%' }}>
            <svg viewBox="0 0 100 30" preserveAspectRatio="none" style={{ width: '100%', height: '100%' }}>
                <line x1="0" y1="20" x2="100" y2="20"
                    stroke="#e8eef4" strokeWidth="1" strokeDasharray="4 4" />
                {points && (
                    <polyline
                        points={points}
                        fill="none"
                        stroke={color}
                        strokeWidth="2"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                    />
                )}
            </svg>
        </div>
    );
};
