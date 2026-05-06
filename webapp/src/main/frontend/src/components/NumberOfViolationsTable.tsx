import React from 'react';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { defaultTableProps } from '../config/tableConfig';
import '../styles/Tables.css';

//  Interface für Daten
interface ViolationStats {
    room: string;
    type: string;
    violationsCount: number;
    lastViolation: string;
}

// Dummy-Daten
const mockStats: ViolationStats[] = [
    { room: '020', type: 'Office', violationsCount: 14, lastViolation: '02.07.2026' },
    { room: '015', type: 'Common Area', violationsCount: 5, lastViolation: '28.06.2026' },
    { room: '102', type: 'Office', violationsCount: 2, lastViolation: '15.05.2026' },
    { room: '105', type: 'Office', violationsCount: 0, lastViolation: '-' },
    { room: '099', type: 'Office', violationsCount: 21, lastViolation: '05.07.2026' },
];

export const NumberOfViolationsTable: React.FC = () => {

    const countBodyTemplate = (rowData: ViolationStats) => {
        // Über 10 Verstöße = Rot
        if (rowData.violationsCount > 10) {
            return <span style={{ color: '#f44336', fontWeight: 'bold' }}>{rowData.violationsCount}</span>;
        }
        // Über 0 Verstöße = Gelb/Orange
        if (rowData.violationsCount > 0) {
            return <span style={{ color: '#ff9800', fontWeight: 'bold' }}>{rowData.violationsCount}</span>;
        }
        // 0 Verstöße = Grün
        return <span style={{ color: '#4caf50', fontWeight: 'bold' }}>{rowData.violationsCount}</span>;
    };

    return (
        <div className="table-container">
            <h3>Number of Violations</h3>
            <DataTable value={mockStats} {...defaultTableProps}>
                <Column field="room" header="Room" sortable></Column>
                <Column field="type" header="Type" sortable></Column>
                <Column field="violationsCount" header="Total Violations" body={countBodyTemplate} sortable></Column>
                <Column field="lastViolation" header="Last Violation" sortable></Column>
            </DataTable>
        </div>
    );
};