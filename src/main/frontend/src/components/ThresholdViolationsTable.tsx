import React from 'react';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { defaultTableProps } from '../config/tableConfig';
import '../styles/Tables.css';

const mockViolations = [
    { warning: 'High Temperature', room: '020', type: 'Bureau', max: '25,5', real: '32', date: '01.07.2026' },
    { warning: 'High Temperature', room: '020', type: 'Bureau', max: '25,5', real: '35', date: '02.07.2026' },
];

export const ThresholdViolationsTable: React.FC = () => {
    return (
        <div className="table-container">
            <h3>Threshold violations</h3>
            <DataTable value={mockViolations} {...defaultTableProps} rows={5}>
                <Column field="warning" header="Warning" sortable></Column>
                <Column field="room" header="Room" sortable></Column>
                <Column field="type" header="Type"></Column>
                <Column field="max" header="Max"></Column>
                <Column field="real" header="Real value"></Column>
                <Column field="date" header="Date" sortable></Column>
            </DataTable>
            <div className="btn-right-align" style={{ marginTop: '1rem' }}>
                <button className="btn-secondary">View more</button>
            </div>
        </div>
    );
};