import React from 'react';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { defaultTableProps } from '../config/tableConfig';

export interface DepartmentData {
    department: string;
    co2: string;
    temp: string;
    humidity: string;
    status: 'red' | 'green' | 'yellow' | 'gray';
}

const mockDepartments: DepartmentData[] = [
    { department: 'Finance', co2: '1049 ppm', temp: '26,5 °C', humidity: '53 %', status: 'red' },
    { department: 'IT', co2: '1020 ppm', temp: '25,5 °C', humidity: '50 %', status: 'red' },
    { department: 'Human Resources', co2: '970 ppm', temp: '22,2 °C', humidity: '45 %', status: 'green' },
    { department: 'Marketing', co2: '950 ppm', temp: '21,9 °C', humidity: '48 %', status: 'yellow' },
    { department: 'Sales', co2: '950 ppm', temp: '21,9 °C', humidity: '48 %', status: 'green' },
];

export const DepartmentAveragesTable: React.FC = () => {
    const handleRowClick = (e: any) => {
        const dep = e.data as DepartmentData;
        alert(`Navigiere zu: ${dep.department} Overview`);
    };

    const statusTemplate = (rowData: DepartmentData) => {
        return <span className={`status-indicator status-${rowData.status}`}></span>;
    };

    return (
        <div className="table-container">
            <h3>Avg. Values per Department</h3>
            <DataTable
                value={mockDepartments}
                {...defaultTableProps}
                onRowClick={handleRowClick}
                className="row-clickable"
                rows={5}
            >
                <Column field="department" header="Department" sortable></Column>
                <Column field="co2" header="CO2" sortable></Column>
                <Column field="temp" header="Temp" sortable></Column>
                <Column field="humidity" header="Humidity" sortable></Column>
                <Column header="Status" body={statusTemplate}></Column>
            </DataTable>
        </div>
    );
};