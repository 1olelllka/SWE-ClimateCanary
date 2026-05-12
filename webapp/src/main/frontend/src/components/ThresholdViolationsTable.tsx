import React from 'react';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { defaultTableProps } from '../config/tableConfig';
import '../styles/Tables.css';

export interface ThresholdViolationData {
    readonly id: string;
    readonly warning: string;
    readonly room: string;
    readonly type: string;
    readonly max: string;
    readonly real: string;
    readonly date: string;
}

interface ThresholdViolationsTableProps {
    readonly violations: ThresholdViolationData[];
    readonly loading?: boolean;
}

export const ThresholdViolationsTable: React.FC<ThresholdViolationsTableProps> = ({
                                                                                      violations,
                                                                                      loading = false
                                                                                  }) => {
    return (
        <div className="table-container">
            <h3>Threshold Violations</h3>

            <DataTable
                value={violations}
                {...defaultTableProps}
                rows={5}
                loading={loading}
                emptyMessage="No threshold violations found."
            >
                <Column field="warning" header="Warning" sortable />
                <Column field="room" header="Room" sortable />
                <Column field="type" header="Type" />
                <Column field="max" header="Max" />
                <Column field="real" header="Real value" />
                <Column field="date" header="Date" sortable />
            </DataTable>

            <div className="btn-right-align" style={{ marginTop: '1rem' }}>
                <button className="btn-secondary">View more</button>
            </div>
        </div>
    );
};