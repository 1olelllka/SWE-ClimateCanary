import React from 'react';
import ReactECharts from 'echarts-for-react';
import { DepartmentWithStats } from '../views/SeniorManagerDashboard';

interface Props {
    departments: DepartmentWithStats[];
    loading: boolean;
}

const BAR_COLORS = ['#4caf50', '#2196f3', '#ff9800', '#9c27b0', '#e91e63', '#00bcd4', '#f44336', '#795548'];

export const ViolationsBarChart: React.FC<Props> = ({ departments, loading }) => {
    const labels = departments.map(d => d.name);
    const values = departments.map(d => d.activeViolations);

    const option = {
        tooltip: {
            trigger: 'axis' as const,
            formatter: (params: any[]) => {
                const p = params[0];
                return `${p.name}<br/>Active violations: <b>${p.value}</b>`;
            },
        },
        grid: { left: 130, right: 30, top: 10, bottom: 30 },
        xAxis: {
            type: 'value' as const,
            minInterval: 1,
            min: 0,
            splitLine: { lineStyle: { type: 'dashed' as const, color: '#f1f5f9' } },
            axisLabel: { fontSize: 11 },
        },
        yAxis: {
            type: 'category' as const,
            data: [...labels].reverse(),
            axisLabel: { fontSize: 11 },
            axisLine: { lineStyle: { color: '#e2e8f0' } },
        },
        series: [{
            type: 'bar' as const,
            data: [...values].reverse().map((v, i) => ({
                value: v,
                itemStyle: { color: BAR_COLORS[(values.length - 1 - i) % BAR_COLORS.length], borderRadius: [0, 4, 4, 0] },
            })),
            barMaxWidth: 36,
        }],
    };

    return (
        <div className="table-container card chart-card">
            <div className="flex-header">
                <h3>Active Violations per Department</h3>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary, #64748b)' }}>
                    Based on real-time limit breaches from sensor data
                </span>
            </div>
            <div style={{ padding: '1rem 1.5rem 1.5rem' }}>
                {loading ? (
                    <div className="bm-loading">Loading…</div>
                ) : departments.length === 0 ? (
                    <div className="bm-empty">No department data available.</div>
                ) : (
                    <ReactECharts option={option} style={{ height: '300px' }} notMerge />
                )}
            </div>
        </div>
    );
};
