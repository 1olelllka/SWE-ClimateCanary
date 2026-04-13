import React from 'react';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { defaultTableProps } from '../config/tableConfig';
import '../styles/Tables.css';

const mockRequests = [
    { first: 'Sarah', last: 'Deng', room: '015', date: '23.05.26 - 27.05.26', reason: 'Illness' },
];

export const PendingRequestsTable: React.FC = () => {
    const actionBodyTemplate = () => {
        return <button className="btn-primary-small">View</button>;
    };

    return (
        <div className="table-container">
            <h3>Pending requests</h3>
            <DataTable value={mockRequests} {...defaultTableProps}>
                <Column field="first" header="Firstname" sortable></Column>
                <Column field="last" header="Lastname" sortable></Column>
                <Column field="room" header="Room" sortable></Column>
                <Column field="date" header="Date"></Column>
                <Column field="reason" header="Reason"></Column>
                <Column body={actionBodyTemplate} exportable={false} style={{ minWidth: '8rem' }}></Column>
            </DataTable>
        </div>
    );
};