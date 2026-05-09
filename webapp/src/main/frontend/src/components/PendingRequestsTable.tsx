import React from 'react';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { defaultTableProps } from '../config/tableConfig';
import '../styles/Tables.css';

export interface PendingRequestData {
    readonly id: string;
    readonly first: string;
    readonly last: string;
    readonly room: string;
    readonly date: string;
    readonly reason: string;
}

interface PendingRequestsTableProps {
    readonly requests?: PendingRequestData[];
    readonly loading?: boolean;
}

export const PendingRequestsTable: React.FC<PendingRequestsTableProps> = ({
                                                                              requests = [],
                                                                              loading = false
                                                                          }) => {
    const actionBodyTemplate = () => {
        return <button className="btn-primary-small">View</button>;
    };

    return (
        <div className="table-container">
            <h3>Pending Requests</h3>

            <DataTable
                value={requests}
                {...defaultTableProps}
                loading={loading}
                emptyMessage="No pending requests found."
            >
                <Column field="first" header="Firstname" sortable />
                <Column field="last" header="Lastname" sortable />
                <Column field="room" header="Room" sortable />
                <Column field="date" header="Date" />
                <Column field="reason" header="Reason" />
                <Column
                    body={actionBodyTemplate}
                    exportable={false}
                    style={{ minWidth: '8rem' }}
                />
            </DataTable>
        </div>
    );
};