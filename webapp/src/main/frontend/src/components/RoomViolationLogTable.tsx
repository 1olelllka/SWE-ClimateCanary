import React, { useMemo, useState } from 'react';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { useTimeFormat } from '../hooks/useTimeFormat';
import { Dropdown } from "primereact/dropdown";

export interface ViolationDTO {
    readonly id?: string;
    readonly measurementType?: 'TEMPERATURE' | 'HUMIDITY' | 'CO2' | string;
    readonly status?: 'GREEN' | 'YELLOW' | 'RED' | string;
    readonly message?: string | null;
    readonly triggeredValue?: number | null;
    readonly activeLimitAtTime?: number | null;
    readonly createdAt?: string | null;
    readonly resolvedAt?: string | null;
    readonly active?: boolean;
    readonly tip?: string | null;
}

interface RoomViolationLogTableProps {
    readonly violations: ViolationDTO[];
}

const SENSOR_LABEL: Record<string, string> = {
    TEMPERATURE: 'Temperature',
    HUMIDITY: 'Humidity',
    CO2: 'CO₂',
};

const STATUS_COLOR: Record<string, string> = {
    RED: '#ef4444',
    YELLOW: '#eab308',
    GREEN: '#22c55e',
};


const fmt = (v?: number | null) =>
    v == null ? '—' : v.toFixed(1).replace('.', ',');

export const RoomViolationLogTable: React.FC<RoomViolationLogTableProps> = ({ violations }) => {
    const [sensorFilter, setSensorFilter] = useState('ALL');
    const [statusFilter, setStatusFilter] = useState('ALL');
    const [activeOnly, setActiveOnly] = useState(false);
    const { formatDatetime } = useTimeFormat();

    const filtered = useMemo(() => violations.filter(v => {
        if (sensorFilter !== 'ALL' && v.measurementType !== sensorFilter) return false;
        if (statusFilter !== 'ALL' && v.status !== statusFilter) return false;
        if (activeOnly && !v.active) return false;
        return true;
    }), [violations, sensorFilter, statusFilter, activeOnly]);

    const dateTemplate    = (v: ViolationDTO) => v.createdAt ? formatDatetime(v.createdAt, '—') : '—';
    const sensorTemplate  = (v: ViolationDTO) => SENSOR_LABEL[v.measurementType ?? ''] ?? v.measurementType ?? '—';
    const measuredTemplate = (v: ViolationDTO) => fmt(v.triggeredValue);
    const limitTemplate   = (v: ViolationDTO) => fmt(v.activeLimitAtTime);

    const statusTemplate = (v: ViolationDTO) => (
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
            <span style={{
                display: 'inline-block', width: 9, height: 9, borderRadius: '50%',
                backgroundColor: STATUS_COLOR[v.status ?? ''] ?? '#9e9e9e', flexShrink: 0,
            }} />
            {v.status ?? '—'}
        </span>
    );

    const resolvedTemplate = (v: ViolationDTO) => v.active
        ? <span className="bra-active-text">Active</span>
        : formatDatetime(v.resolvedAt ?? undefined, '—');

    const sensorOptions = [
        { label: 'All Sensors', value: 'ALL' },
        { label: 'Temperature', value: 'TEMPERATURE' },
        { label: 'Humidity', value: 'HUMIDITY' },
        { label: 'CO₂', value: 'CO2' },
    ];

    const statusOptions = [
        { label: 'All Statuses', value: 'ALL' },
        { label: 'Green', value: 'GREEN' },
        { label: 'Yellow', value: 'YELLOW' },
        { label: 'Red', value: 'RED' },
    ];

    return (
        <div className="table-container">
            <div className="bra-table-controls">
                <Dropdown
                    value={sensorFilter}
                    options={sensorOptions}
                    onChange={e => setSensorFilter(e.value)}
                    className="metric-select"
                    panelClassName="metric-select-panel"
                />

                <Dropdown
                    value={statusFilter}
                    options={statusOptions}
                    onChange={e => setStatusFilter(e.value)}
                    className="metric-select"
                    panelClassName="metric-select-panel"
                />
                <button
                    type="button"
                    className={`bra-toggle-btn${activeOnly ? ' active' : ''}`}
                    onClick={() => setActiveOnly(v => !v)}
                    title="Show active warnings only"
                >
                    {activeOnly && <i className="pi pi-check" style={{ fontSize: '0.75rem' }} />}
                    Active only
                </button>
            </div>

            <DataTable
                value={filtered}
                paginator
                rows={10}
                rowsPerPageOptions={[5, 10, 20]}
                paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown"
                currentPageReportTemplate="{first}-{last} of {totalRecords}"
                stripedRows
                emptyMessage="No warnings found."
                responsiveLayout="scroll"
            >
                <Column header="Date"     body={dateTemplate} />
                <Column header="Sensor"   body={sensorTemplate} />
                <Column header="Status"   body={statusTemplate} />
                <Column header="Measured" body={measuredTemplate} />
                <Column header="Limit"    body={limitTemplate} />
                <Column header="Resolved" body={resolvedTemplate} />
            </DataTable>
        </div>
    );
};

export default RoomViolationLogTable;
