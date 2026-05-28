import React, { useState, useEffect } from 'react';
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
    readonly onView?: (id: string) => void;
}

export const PendingRequestsTable: React.FC<PendingRequestsTableProps> = ({
    requests = [],
    loading = false,
    onView,
}) => {
    const [isMobile, setIsMobile] = useState(window.innerWidth <= 700);
    useEffect(() => {
        const handler = () => setIsMobile(window.innerWidth <= 700);
        window.addEventListener('resize', handler);
        return () => window.removeEventListener('resize', handler);
    }, []);

    const handleRowClick = (e: any) => {
        if (onView) onView((e.data as PendingRequestData).id);
    };

    const actionBodyTemplate = (row: PendingRequestData) => (
        <button className="btn-primary-small" onClick={() => onView?.(row.id)}>View</button>
    );

    return (
        <div className="table-container">
            <div className="flex-header">
                <h3>Pending Requests</h3>
            </div>

            <DataTable
                value={requests}
                {...defaultTableProps}
                loading={loading}
                emptyMessage="No pending requests found."
                onRowClick={handleRowClick}
                rowClassName={() => ({ 'row-clickable': true })}
            >
                <Column field="first" header="First" sortable />
                <Column field="last" header="Last" sortable />
                <Column field="room" header="Room" sortable />
                {!isMobile && <Column field="date" header="Date" />}
                <Column field="reason" header="Reason" />
                {!isMobile && (
                    <Column
                        body={actionBodyTemplate}
                        exportable={false}
                        style={{ width: '4rem' }}
                    />
                )}
            </DataTable>
        </div>
    );
};
