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
    readonly disablePrivacyMask?: boolean;
    readonly onRowClick?: (roomId: string) => void;
    readonly onSettingsClick?: (roomId: string) => void;
}

export const RoomListTable: React.FC<RoomListTableProps> = ({
                                                                rooms,
                                                                showDepartment = false,
                                                                showSettings = false,
                                                                disablePrivacyMask = false,
                                                                onRowClick,
                                                                onSettingsClick
                                                            }) => {

    const isPrivacyMasked = (peopleStr: string) => {
        if (disablePrivacyMask) return false;
        const currentPeople = parseInt(peopleStr.split('/')[0], 10);
        return currentPeople < 5;
    };

    const handleRowClick = (e: any) => {
        const room = e.data as RoomData;
        if (isPrivacyMasked(room.people)) return;
        if (e.originalEvent.target.closest('.p-button')) return;
        if (onRowClick) onRowClick(room.id);
    };

    // Templates
    const blurTemplate = (rowData: RoomData, field: 'co2' | 'temp' | 'humidity') => {
        const masked = isPrivacyMasked(rowData.people);
        return <span className={masked ? 'blurred-text' : ''}>{masked ? 'n/a' : rowData[field]}</span>;
    };

    const statusTemplate = (rowData: RoomData) => {
        return <span className={`status-indicator status-${rowData.status}`}></span>;
    };

    // Zahnrad-Template für Building Manager
    const settingsTemplate = (rowData: RoomData) => {
        return (
            <Button
                icon="pi pi-cog"
                rounded
                text
                severity="secondary"
                onClick={() => onSettingsClick && onSettingsClick(rowData.id)}
                title="Analysis & Settings"
            />
        );
    };

    const rowClassRules = (rowData: RoomData) => {
        return {
            'row-disabled': isPrivacyMasked(rowData.people),
            'row-clickable': !isPrivacyMasked(rowData.people)
        };
    };

    return (
        <div className="table-container">
            <DataTable
                value={rooms}
                {...defaultTableProps}
                onRowClick={handleRowClick}
                rowClassName={rowClassRules}
            >
                <Column field="id" header="Room" sortable></Column>
                {/* Nur rendern, wenn showDepartment true ist */}
                {showDepartment && <Column field="department" header="Dep." sortable></Column>}

                <Column field="type" header="Type" sortable></Column>
                <Column header="CO2" body={(data) => blurTemplate(data, 'co2')}></Column>
                <Column header="Temp" body={(data) => blurTemplate(data, 'temp')}></Column>
                <Column header="Humidity" body={(data) => blurTemplate(data, 'humidity')}></Column>
                <Column header="Status" body={statusTemplate}></Column>

                {/* Zahnrad rendern */}
                {showSettings && <Column body={settingsTemplate} exportable={false} style={{ width: '4rem' }}></Column>}
            </DataTable>
        </div>
    );
};