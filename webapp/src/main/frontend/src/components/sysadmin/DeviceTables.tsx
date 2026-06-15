import React from 'react';
import { Button } from 'primereact/button';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';
import { Dropdown } from 'primereact/dropdown';
import { SensorStationDTO, RoomDTO } from '../../generated-skeleton-api';
import AdminTableHeader from './AdminTableHeader';
import { mutedValue, statusBadge } from './adminBadges';
import { RaspberryDTOReal, SelectOption } from './sysAdminTypes';

interface DeviceTablesProps {
    readonly raspberries: RaspberryDTOReal[];
    readonly sensors: SensorStationDTO[];
    readonly rooms: RoomDTO[];
    readonly raspberrySearch: string;
    readonly onRaspberrySearch: (value: string) => void;
    readonly raspberryStatusFilter: string | null;
    readonly onRaspberryStatusFilter: (value: string | null) => void;
    readonly sensorSearch: string;
    readonly onSensorSearch: (value: string) => void;
    readonly sensorStatusFilter: string | null;
    readonly onSensorStatusFilter: (value: string | null) => void;
    readonly statusOptions: SelectOption[];
    readonly onAddPi: () => void;
    readonly onEditPi: (row: RaspberryDTOReal) => void;
    readonly onDeletePi: (id?: string) => void;
    readonly onRetryPiConnection: (id?: string) => void;
    readonly onAddSensor: () => void;
    readonly onEditSensor: (row: SensorStationDTO) => void;
    readonly onDeleteSensor: (id?: string) => void;
    readonly onRetrySensorConnection: (sensorId: string) => void;
}

const DeviceTables: React.FC<DeviceTablesProps> = ({
    raspberries,
    sensors,
    rooms,
    raspberrySearch,
    onRaspberrySearch,
    raspberryStatusFilter,
    onRaspberryStatusFilter,
    sensorSearch,
    onSensorSearch,
    sensorStatusFilter,
    onSensorStatusFilter,
    statusOptions,
    onAddPi,
    onEditPi,
    onDeletePi,
    onRetryPiConnection,
    onAddSensor,
    onEditSensor,
    onDeleteSensor,
    onRetrySensorConnection,
}) => {
    const normalizedRaspberrySearch = raspberrySearch.toLowerCase();
    const normalizedSensorSearch = sensorSearch.toLowerCase();
    const filteredRaspberries = raspberries.filter(pi =>
        (!raspberrySearch || (pi.name ?? '').toLowerCase().includes(normalizedRaspberrySearch)) &&
        (!raspberryStatusFilter || pi.status === raspberryStatusFilter)
    );

    const filteredSensors = sensors.filter(s =>
        (!sensorSearch || (s.name ?? '').toLowerCase().includes(normalizedSensorSearch)) &&
        (!sensorStatusFilter || s.status === sensorStatusFilter)
    );

    return (
        <>
            <div className="table-container">
                <AdminTableHeader
                    title="Raspberry Pi List"
                    search={raspberrySearch}
                    onSearch={onRaspberrySearch}
                    searchPlaceholder="Search by name"
                    onAdd={onAddPi}
                    addLabel="Add Raspberry Pi"
                    filterEl={
                        <Dropdown
                            value={raspberryStatusFilter}
                            options={statusOptions}
                            onChange={e => onRaspberryStatusFilter(e.value)}
                            placeholder="Status Filter"
                            showClear
                            style={{ borderRadius: '20px', minWidth: '160px' }}
                        />
                    }
                />
                <DataTable value={filteredRaspberries} stripedRows emptyMessage="No Raspberry Pis found.">
                    <Column field="name" header="Name" sortable/>
                    <Column header="Room" body={(row: RaspberryDTOReal) => row.room?.roomName ?? mutedValue()}/>
                    <Column header="Sensors" body={(row: RaspberryDTOReal) => {
                        const count = sensors.filter(s => s.connectedToPiId === row.id).length;
                        return count > 0 ? <span>{count}</span> : mutedValue('None');
                    }}/>
                    <Column header="Status" body={row => statusBadge(row.status)}/>
                    <Column
                        header=""
                        className="admin-actions-column admin-actions-column-wide"
                        headerClassName="admin-actions-column admin-actions-column-wide"
                        exportable={false}
                        body={(row: RaspberryDTOReal) => (
                            <div className="admin-table-actions">
                                <Button icon="pi pi-refresh" rounded text severity="warning" title="Retry connection" onClick={() => onRetryPiConnection(row.id)}/>
                                <Button icon="pi pi-cog" rounded text severity="secondary" title="Edit Raspberry Pi" onClick={() => onEditPi(row)}/>
                                <Button icon="pi pi-trash" rounded text severity="danger" title="Delete Raspberry Pi" onClick={() => onDeletePi(row.id)}/>
                            </div>
                        )}
                    />
                </DataTable>
            </div>

            <div className="table-container">
                <AdminTableHeader
                    title="Sensor Station List"
                    search={sensorSearch}
                    onSearch={onSensorSearch}
                    searchPlaceholder="Search by name"
                    onAdd={onAddSensor}
                    addLabel="Add Sensor Station"
                    filterEl={
                        <Dropdown
                            value={sensorStatusFilter}
                            options={statusOptions}
                            onChange={e => onSensorStatusFilter(e.value)}
                            placeholder="Status Filter"
                            showClear
                            style={{ borderRadius: '20px', minWidth: '160px' }}
                        />
                    }
                />
                <DataTable value={filteredSensors} stripedRows emptyMessage="No Sensor Stations found.">
                    <Column field="name" header="Name" sortable/>
                    <Column header="Room" body={(row: SensorStationDTO) => {
                        const room = rooms.find(r => r.id === row.roomId);
                        return room ? (room.name ?? room.id) : mutedValue();
                    }}/>
                    <Column header="Assigned Pi" body={(row: SensorStationDTO) => {
                        const pi = raspberries.find(p => p.id === row.connectedToPiId);
                        return pi ? (pi.name ?? pi.id) : mutedValue('None');
                    }}/>
                    <Column header="Status" body={row => statusBadge(row.status)}/>
                    <Column
                        header=""
                        className="admin-actions-column admin-actions-column-wide"
                        headerClassName="admin-actions-column admin-actions-column-wide"
                        exportable={false}
                        body={(row: SensorStationDTO) => (
                            <div className="admin-table-actions">
                                <Button icon="pi pi-refresh" rounded text severity="warning" title="Retry connection" onClick={() => onRetrySensorConnection(row.readId!)}/>
                                <Button icon="pi pi-cog" rounded text severity="secondary" title="Edit Sensor Station" onClick={() => onEditSensor(row)}/>
                                <Button icon="pi pi-trash" rounded text severity="danger" title="Delete Sensor Station" onClick={() => onDeleteSensor(row.readId)}/>
                            </div>
                        )}
                    />
                </DataTable>
            </div>
        </>
    );
};

export default DeviceTables;
