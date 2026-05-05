import React, { useState } from 'react';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Checkbox } from 'primereact/checkbox';
import { DashboardCalendar } from './Calendar';
import { defaultTableProps } from '../config/tableConfig';
import '../styles/TimeFilter.css';
import '../styles/ClimateHistoryChart.css';

type TimeFilter = 'Day' | 'Week' | 'Month' | 'Custom';
type Importance = 'High' | 'Medium' | 'Low';
type Timeframe  = 'Day' | 'Week' | 'Month';

interface ViolationRow {
    id:         string;
    violation:  string;
    room:       string;
    maxAllowed: string;
    current:    string;
    importance: Importance;
    active:     boolean;
    timeframe:  Timeframe;
}

const MOCK_VIOLATIONS: ViolationRow[] = [
    { id: '1', violation: 'CO2 Level',   room: 'FIN-101', maxAllowed: '1000 ppm', current: '1049 ppm', importance: 'High',   active: true,  timeframe: 'Day'   },
    { id: '2', violation: 'Temperature', room: 'FIN-102', maxAllowed: '26 °C',    current: '28.1 °C',  importance: 'Medium', active: true,  timeframe: 'Day'   },
    { id: '3', violation: 'Humidity',    room: 'FIN-101', maxAllowed: '70 %',     current: '74 %',     importance: 'Low',    active: false, timeframe: 'Week'  },
    { id: '4', violation: 'CO2 Level',   room: 'FIN-103', maxAllowed: '1000 ppm', current: '1120 ppm', importance: 'High',   active: false, timeframe: 'Week'  },
    { id: '5', violation: 'Temperature', room: 'FIN-101', maxAllowed: '26 °C',    current: '27.5 °C',  importance: 'Medium', active: false, timeframe: 'Month' },
    { id: '6', violation: 'CO2 Level',   room: 'FIN-102', maxAllowed: '1000 ppm', current: '1090 ppm', importance: 'High',   active: false, timeframe: 'Month' },
    { id: '7', violation: 'Humidity',    room: 'FIN-103', maxAllowed: '70 %',     current: '78 %',     importance: 'Medium', active: false, timeframe: 'Month' },
];

const IMPORTANCE_COLOR: Record<Importance, string> = {
    High:   '#f44336',
    Medium: '#ff9800',
    Low:    '#4caf50',
};

interface Props {
    departmentName: string;
}

export const DepartmentViolationsTable: React.FC<Props> = () => {
    const [timeFilter, setTimeFilter] = useState<TimeFilter>('Week');
    const [dateRange,  setDateRange]  = useState<Date[] | null>(null);
    const [activeOnly, setActiveOnly] = useState(false);

    const rows = MOCK_VIOLATIONS.filter(v => {
        if (activeOnly && !v.active) return false;
        if (timeFilter !== 'Custom' && v.timeframe !== timeFilter) return false;
        return true;
    });

    const activeTemplate = (row: ViolationRow) => (
        <span
            title={row.active ? 'Active' : 'Resolved'}
            style={{
                display:         'inline-block',
                width:           9,
                height:          9,
                borderRadius:    '50%',
                backgroundColor: row.active ? '#f44336' : '#94a3b8',
            }}
        />
    );

    const importanceTemplate = (row: ViolationRow) => (
        <span style={{
            display:         'inline-block',
            padding:         '0.15rem 0.6rem',
            borderRadius:    '999px',
            fontSize:        '0.72rem',
            fontWeight:      700,
            backgroundColor: IMPORTANCE_COLOR[row.importance] + '22',
            color:           IMPORTANCE_COLOR[row.importance],
        }}>
            {row.importance}
        </span>
    );

    return (
        <div className="chart-card" style={{ marginTop: '1.5rem' }}>
            <div className="chart-header">
                <h3 style={{ margin: 0, fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-primary, #1e293b)' }}>
                    Violations Table
                </h3>
                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', flexWrap: 'wrap' }}>
                    <label style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.82rem', color: '#64748b', cursor: 'pointer', userSelect: 'none' }}>
                        <Checkbox
                            checked={activeOnly}
                            onChange={e => setActiveOnly(e.checked ?? false)}
                            inputId="activeOnly"
                        />
                        Active only
                    </label>

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
                </div>
            </div>

            <DataTable
                value={rows}
                {...defaultTableProps}
                rows={10}
                emptyMessage="No violations for this period."
            >
                <Column body={activeTemplate} style={{ width: '2.5rem', textAlign: 'center' }} />
                <Column field="violation"  header="Violation"    sortable />
                <Column field="room"       header="Room"         sortable />
                <Column field="maxAllowed" header="Max Allowed"  sortable />
                <Column field="current"    header="Current"      sortable />
                <Column header="Importance" body={importanceTemplate} sortField="importance" sortable />
            </DataTable>
        </div>
    );
};
