import React from 'react';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Button } from 'primereact/button';
import { defaultTableProps } from '../config/tableConfig';
import '../styles/Tables.css';

export interface RoomData {
    readonly id: string;
    readonly backendId?: string;
    readonly department?: string;
    readonly type: string;
    readonly people: string;
    readonly co2: string;
    readonly temp: string;
    readonly humidity: string;
    readonly status: 'red' | 'green' | 'yellow' | 'gray';
}

interface RoomListTableProps {
    readonly rooms: RoomData[];
    readonly showDepartment?: boolean;
    readonly showSettings?: boolean;
    readonly onRowClick?: (roomId: string) => void;
    readonly onSettingsClick?: (roomId: string) => void;
}

export const RoomListTable: React.FC<RoomListTableProps> = ({
    rooms,
    showDepartment = false,
    showSettings = false,
    onRowClick,
    onSettingsClick,
}) => {
    const handleRowClick = (e: any) => {
        if (e.originalEvent.target.closest('.p-button')) return;
        if (onRowClick) onRowClick((e.data as RoomData).id);
    };

    const statusTemplate = (rowData: RoomData) => (
        <span className={`status-indicator status-${rowData.status}`} />
    );

    const settingsTemplate = (rowData: RoomData) => (
        <Button
            icon="pi pi-cog"
            rounded
            text
            severity="secondary"
            onClick={() => onSettingsClick && onSettingsClick(rowData.id)}
            title="Analysis & Settings"
        />
    );

    return (
        <div className="table-container">
            <DataTable
                value={rooms}
                {...defaultTableProps}
                onRowClick={handleRowClick}
                rowClassName={() => ({ 'row-clickable': !!onRowClick })}
            >
                <Column field="id" header="Room" sortable />
                {showDepartment && <Column field="department" header="Dep." sortable />}
                <Column field="type" header="Type" sortable />
                <Column field="co2" header="CO2" />
                <Column field="temp" header="Temp" />
                <Column field="humidity" header="Humidity" />
                <Column header="Status" body={statusTemplate} />
                {showSettings && <Column body={settingsTemplate} exportable={false} style={{ width: '4rem' }} />}
            </DataTable>
        </div>
    );
};
