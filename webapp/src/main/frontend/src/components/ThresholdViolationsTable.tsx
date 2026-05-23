import React from 'react';
import { useNavigate } from 'react-router-dom';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { defaultTableProps } from '../config/tableConfig';
import { ROUTES } from '../utilities/routes.paths';
import '../styles/Tables.css';

export interface ThresholdViolationData {
    readonly id: string;
    readonly status: string;
    readonly active: boolean;
    readonly sensor: string;
    readonly room: string;
    readonly limit: string;
    readonly measuredValue: string;
    readonly datetime: string;
}

interface ThresholdViolationsTableProps {
    readonly violations: ThresholdViolationData[];
    readonly loading?: boolean;
    readonly fullPage?: boolean;
}

const STATUS_LABEL: Record<string, string> = {
    RED:      'Red',
    YELLOW:   'Yellow',
    GREEN:    'Green',
    RESOLVED: 'Resolved',
};

const statusBody = (row: ThresholdViolationData) => {
    const key = row.active ? row.status : 'RESOLVED';
    const label = STATUS_LABEL[key] ?? 'Resolved';
    const cssKey = key.toLowerCase() as 'red' | 'yellow' | 'green' | 'resolved';
    return (
        <span className={`status-badge status-badge-${cssKey}`}>
            {label}
        </span>
    );
};

export const ThresholdViolationsTable: React.FC<ThresholdViolationsTableProps> = ({
    violations,
    loading = false,
    fullPage = false,
}) => {
    const navigate = useNavigate();

    return (
        <div className="table-container">
            <DataTable
                value={violations}
                {...defaultTableProps}
                rows={fullPage ? 20 : 5}
                loading={loading}
                emptyMessage="No threshold violations found."
            >
                <Column header="Status" body={statusBody} sortable sortField="status" style={{ minWidth: '7rem' }} />
                <Column field="sensor" header="Violated Sensor" sortable style={{ minWidth: '9rem' }} />
                <Column field="room" header="Room" sortable style={{ minWidth: '6rem' }} />
                <Column field="limit" header="Limit" style={{ minWidth: '7rem' }} />
                <Column field="measuredValue" header="Measured Value" style={{ minWidth: '8rem' }} />
                <Column field="datetime" header="Date & Time" sortable style={{ minWidth: '10rem' }} />
            </DataTable>

            {!fullPage && (
                <div className="btn-right-align" style={{ marginTop: '1rem' }}>
                    <button className="btn-secondary" onClick={() => navigate(ROUTES.DEPARTMENT_VIOLATIONS)}>
                        View more
                    </button>
                </div>
            )}
        </div>
    );
};