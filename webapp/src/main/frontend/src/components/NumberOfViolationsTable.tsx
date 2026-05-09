import React from 'react';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { defaultTableProps } from '../config/tableConfig';
import '../styles/Tables.css';

export interface ViolationStatsData {
    readonly room: string;
    readonly type: string;
    readonly violationsCount: number;
    readonly lastViolation: string;
}

interface NumberOfViolationsTableProps {
    readonly stats?: ViolationStatsData[];
    readonly loading?: boolean;
}

export const NumberOfViolationsTable: React.FC<NumberOfViolationsTableProps> = ({
                                                                                    stats = [],
                                                                                    loading = false
                                                                                }) => {
    const countBodyTemplate = (rowData: ViolationStatsData) => {
        if (rowData.violationsCount > 10) {
            return (
                <span style={{ color: '#f44336', fontWeight: 'bold' }}>
                    {rowData.violationsCount}
                </span>
            );
        }

        if (rowData.violationsCount > 0) {
            return (
                <span style={{ color: '#ff9800', fontWeight: 'bold' }}>
                    {rowData.violationsCount}
                </span>
            );
        }

        return (
            <span style={{ color: '#4caf50', fontWeight: 'bold' }}>
                {rowData.violationsCount}
            </span>
        );
    };

    return (
        <div className="table-container">
            <h3>Number of Violations</h3>

            <DataTable
                value={stats}
                {...defaultTableProps}
                loading={loading}
                emptyMessage="No violation statistics found."
            >
                <Column field="room" header="Room" sortable />
                <Column field="type" header="Type" sortable />
                <Column
                    field="violationsCount"
                    header="Total Violations"
                    body={countBodyTemplate}
                    sortable
                />
                <Column field="lastViolation" header="Last Violation" sortable />
            </DataTable>
        </div>
    );
};