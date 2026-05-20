import React, { useState, useEffect, useMemo } from 'react';
import ReactECharts from 'echarts-for-react';
import { BuildingTrendControllerApi, BuildingTrendDTO } from '../generated-skeleton-api';
import { DepartmentWithStats } from '../views/SeniorManagerDashboard';
import '../styles/ClimateHistoryChart.css';

interface Props {
    departments: DepartmentWithStats[];
}

const trendApi = new BuildingTrendControllerApi();

const DAY_SHORT = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
const pad2      = (n: number) => String(n).padStart(2, '0');
const fmtDate   = (d: Date)  => `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
const dayLabel  = (d: Date)  => `${DAY_SHORT[d.getDay()]} ${pad2(d.getDate())}`;

const NO_DATA_SENTINEL = -1;

function statusLabel(trend: string) {
    if (trend === 'UP')   return 'Worsening';
    if (trend === 'DOWN') return 'Improving';
    return 'Stable';
}

function trendArrow(trend: string) {
    if (trend === 'UP')   return '↑';
    if (trend === 'DOWN') return '↓';
    return '→';
}

export const DepartmentHeatmap: React.FC<Props> = ({ departments }) => {
    const [deptData, setDeptData] = useState<Map<string, BuildingTrendDTO[]>>(new Map());
    const [loading,  setLoading]  = useState(false);

    const last7 = useMemo(() => Array.from({ length: 7 }, (_, i) => {
        const d = new Date();
        d.setDate(d.getDate() - (6 - i));
        return d;
    }), []);

    const xLabels     = useMemo(() => last7.map(dayLabel), [last7]);
    const dateStrings = useMemo(() => last7.map(fmtDate),  [last7]);

    useEffect(() => {
        if (departments.length === 0) return;
        setLoading(true);
        Promise.all(
            departments.map(d =>
                trendApi
                    .getTrendsForSpecificDepartment({
                        departmentId: d.id,
                        startDate:    dateStrings[0],
                        endDate:      dateStrings[6],
                    })
                    .then(r  => ({ id: d.id, rows: r.data ?? [] }))
                    .catch(() => ({ id: d.id, rows: [] as BuildingTrendDTO[] }))
            )
        )
            .then(results => {
                const map = new Map<string, BuildingTrendDTO[]>();
                results.forEach(r => map.set(r.id, r.rows));
                setDeptData(map);
            })
            .finally(() => setLoading(false));
    }, [departments, dateStrings]);

    const yLabels = departments.map(d => d.name);

    // [xIdx, yIdx, value (−1 = no data), trend string]
    const heatData = departments.flatMap((dept, yi) => {
        const byDate = new Map<string, BuildingTrendDTO>();
        (deptData.get(dept.id) ?? []).forEach(r => r.date && byDate.set(r.date, r));

        return dateStrings.map((date, xi): [number, number, number, string] => {
            const row = byDate.get(date);
            return [xi, yi, row?.value ?? NO_DATA_SENTINEL, row?.trend ?? ''];
        });
    });

    const option = {
        tooltip: {
            trigger:         'item' as const,
            position:        'top' as const,
            backgroundColor: '#1e293b',
            borderColor:     '#1e293b',
            borderRadius:    8,
            padding:         [8, 12],
            textStyle:       { color: '#f8fafc', fontSize: 12 },
            formatter: (params: any) => {
                const [xi, yi, value, trend] = params.data as [number, number, number, string];
                const dept = yLabels[yi] ?? '';
                const day  = xLabels[xi] ?? '';
                if (value === NO_DATA_SENTINEL) {
                    return `<b style="font-size:13px">${dept}</b><br/><span style="color:#94a3b8">${day} · No data</span>`;
                }
                const arrow = trendArrow(trend);
                const label = statusLabel(trend);
                const scoreBar = Math.round(value * 100);
                return (
                    `<b style="font-size:13px">${dept}</b><br/>` +
                    `<span style="color:#94a3b8">${day}</span><br/>` +
                    `<span style="color:#cbd5e1">Score: </span><b>${value.toFixed(3)}</b><br/>` +
                    `<span style="color:#cbd5e1">Status: </span><b>${arrow} ${label}</b>`
                );
            },
        },
        grid:  { left: 112, right: 16, top: 8, bottom: 36 },
        xAxis: {
            type:      'category' as const,
            data:      xLabels,
            splitArea: { show: false },
            axisLine:  { show: false },
            axisTick:  { show: false },
            axisLabel: { fontSize: 12, color: '#64748b', fontWeight: '600', margin: 10 },
        },
        yAxis: {
            type:      'category' as const,
            data:      yLabels,
            splitArea: { show: false },
            axisLine:  { show: false },
            axisTick:  { show: false },
            axisLabel: { fontSize: 12, color: '#374151', margin: 10 },
        },
        visualMap: {
            type:       'continuous' as const,
            min:        0,
            max:        1,
            dimension:  2,
            show:       false,
            inRange:    { color: ['#10b981', '#f59e0b', '#ef4444'] },
            outOfRange: { color: '#f1f5f9' },
        },
        series: [
            {
                type: 'heatmap' as const,
                data: heatData,
                label: {
                    show:      true,
                    formatter: (params: any) => {
                        const [, , value, trend] = params.data as [number, number, number, string];
                        if (value === NO_DATA_SENTINEL) return '';
                        return trendArrow(trend);
                    },
                    fontSize:   15,
                    color:      'rgba(255,255,255,0.9)',
                    fontWeight: 'bold',
                },
                itemStyle: {
                    borderRadius: 8,
                    borderWidth:  3,
                    borderColor:  '#f8fafc',
                },
                emphasis: {
                    itemStyle: {
                        borderRadius: 8,
                        borderWidth:  3,
                        borderColor:  '#f8fafc',
                        shadowBlur:   12,
                        shadowColor:  'rgba(0,0,0,0.15)',
                    },
                },
            },
        ],
    };

    const chartHeight = Math.max(160, departments.length * 54 + 56);

    return (
        <div className="table-container card chart-card">
            <div className="flex-header">
                <h3>Department Climate Status — Last 7 Days</h3>
            </div>

            <div style={{ padding: '0 1.5rem 1.5rem' }}>
                {loading ? (
                    <div className="chart-loading">Loading…</div>
                ) : departments.length === 0 ? (
                    <div className="chart-loading">No departments available.</div>
                ) : (
                    <>
                        <ReactECharts
                            option={option}
                            style={{ height: `${chartHeight}px` }}
                            notMerge
                        />
                        {/* Custom colour-scale legend */}
                        <div style={{
                            display:        'flex',
                            alignItems:     'center',
                            gap:            '8px',
                            justifyContent: 'flex-end',
                            marginTop:      '4px',
                            paddingRight:   '16px',
                        }}>
                            <span style={{ fontSize: '11px', color: '#64748b', fontWeight: 600 }}>Better</span>
                            <div style={{
                                width:        '100px',
                                height:       '8px',
                                borderRadius: '4px',
                                background:   'linear-gradient(to right, #10b981, #f59e0b, #ef4444)',
                            }} />
                            <span style={{ fontSize: '11px', color: '#64748b', fontWeight: 600 }}>Worse</span>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
};
