import React from 'react';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { defaultTableProps } from '../config/tableConfig';
import '../styles/Tables.css';

interface RoomData {
    id: string;
    type: string;
    people: string;
    co2: string;
    temp: string;
    humidity: string;
    status: 'red' | 'green' | 'yellow' | 'gray';
}

// Dummy-Daten
const mockRooms: RoomData[] = [
    { id: '001', type: 'Office', people: '6/5', co2: '1049 ppm', temp: '26,5 °C', humidity: '53 %', status: 'red' },
    { id: '002', type: 'Common Area', people: '1/--', co2: '1020 ppm', temp: '25,5 °C', humidity: '50 %', status: 'red' },
    { id: '003', type: 'Office', people: '2/5', co2: 'n/a', temp: 'n/a', humidity: 'n/a', status: 'gray' },
    { id: '004', type: 'Office', people: '5/5', co2: '970 ppm', temp: '22,2 °C', humidity: '45 %', status: 'green' },
    { id: '005', type: 'Office', people: '7/5', co2: '950 ppm', temp: '21,9 °C', humidity: '48 %', status: 'green' },
    { id: '020', type: 'Office', people: '7/5', co2: '950 ppm', temp: '21,9 °C', humidity: '48 %', status: 'yellow' },
];

export const RoomListTable: React.FC = () => {
    const isPrivacyMasked = (peopleStr: string) => {
        const currentPeople = parseInt(peopleStr.split('/')[0], 10);
        return currentPeople < 5;
    };

    const handleRowClick = (e: any) => {
        const room = e.data as RoomData;
        if (isPrivacyMasked(room.people)) return;
        alert(`Navigiere zu: Overview Bureau ${room.id}`);
    };

    // --- Verwaschenen Spalten ---
    const blurTemplate = (rowData: RoomData, field: 'co2' | 'temp' | 'humidity') => {
        const masked = isPrivacyMasked(rowData.people);
        return <span className={masked ? 'blurred-text' : ''}>{masked ? 'n/a' : rowData[field]}</span>;
    };

    const statusTemplate = (rowData: RoomData) => {
        return <span className={`status-indicator status-${rowData.status}`}></span>;
    };

    const rowClassRules = (rowData: RoomData) => {
        return {
            'row-disabled': isPrivacyMasked(rowData.people),
            'row-clickable': !isPrivacyMasked(rowData.people)
        };
    };

    return (
        <div className="table-container">
            <h3>Room list</h3>
            <DataTable
                value={mockRooms}
                {...defaultTableProps}
                onRowClick={handleRowClick}
                rowClassName={rowClassRules}
            >
                <Column field="id" header="Room" sortable></Column>
                <Column field="type" header="Type" sortable></Column>
                <Column field="people" header="People"></Column>
                <Column header="CO2" body={(data) => blurTemplate(data, 'co2')}></Column>
                <Column header="Temp" body={(data) => blurTemplate(data, 'temp')}></Column>
                <Column header="Humidity" body={(data) => blurTemplate(data, 'humidity')}></Column>
                <Column header="Status" body={statusTemplate}></Column>
            </DataTable>
        </div>
    );
};