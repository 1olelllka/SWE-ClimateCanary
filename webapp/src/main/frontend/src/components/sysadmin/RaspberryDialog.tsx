import React from 'react';
import { Button } from 'primereact/button';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';
import { Dialog } from 'primereact/dialog';
import { Dropdown } from 'primereact/dropdown';
import { InputNumber } from 'primereact/inputnumber';
import { InputText } from 'primereact/inputtext';
import { SensorStationDTO } from '../../generated-skeleton-api';
import { ConfirmDeleteState, SelectOption } from './sysAdminTypes';

const labelStyle: React.CSSProperties = {
    display: 'block',
    marginBottom: '0.35rem',
    fontWeight: 500,
    fontSize: '0.9rem',
};

interface RaspberryDialogProps {
    readonly visible: boolean;
    readonly editingPiId: string | null;
    readonly piName: string;
    readonly onPiNameChange: (value: string) => void;
    readonly piRoomId: string;
    readonly onPiRoomIdChange: (value: string) => void;
    readonly piIpAddress: string;
    readonly onPiIpAddressChange: (value: string) => void;
    readonly piPort: number | null;
    readonly onPiPortChange: (value: number | null) => void;
    readonly piInterval: number | null;
    readonly onPiIntervalChange: (value: number | null) => void;
    readonly loading: boolean;
    readonly availableRoomOptions: SelectOption[];
    readonly editAvailableRoomOptions: SelectOption[];
    readonly connectedSensors: SensorStationDTO[];
    readonly onSave: () => void;
    readonly onHide: () => void;
    readonly onDisconnectSensor: (sensorId: string) => void;
    readonly onConfirmDelete: (state: ConfirmDeleteState) => void;
}

const RaspberryDialog: React.FC<RaspberryDialogProps> = ({
    visible,
    editingPiId,
    piName,
    onPiNameChange,
    piRoomId,
    onPiRoomIdChange,
    piIpAddress,
    onPiIpAddressChange,
    piPort,
    onPiPortChange,
    piInterval,
    onPiIntervalChange,
    loading,
    availableRoomOptions,
    editAvailableRoomOptions,
    connectedSensors,
    onSave,
    onHide,
    onDisconnectSensor,
    onConfirmDelete,
}) => (
    <Dialog
        header={editingPiId ? 'Edit Raspberry Pi' : 'Add Raspberry Pi'}
        visible={visible}
        style={{ width: '480px' }}
        onHide={onHide}
        footer={
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
                <Button label="Cancel" severity="secondary" outlined onClick={onHide}/>
                <Button label={editingPiId ? 'Save' : 'Create'} icon="pi pi-check" loading={loading} onClick={onSave}/>
            </div>
        }
        draggable={false}
    >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div>
                <label htmlFor="pi-name" style={labelStyle}>Name *</label>
                <InputText id="pi-name" value={piName} onChange={e => onPiNameChange(e.target.value)} placeholder="e.g. Pi-Lab-01" style={{ width: '100%' }}/>
            </div>

            <div>
                <label htmlFor="pi-room" style={labelStyle}>Room {!editingPiId && '*'}</label>
                <Dropdown
                    inputId="pi-room"
                    value={piRoomId}
                    options={editingPiId ? editAvailableRoomOptions : availableRoomOptions}
                    onChange={e => onPiRoomIdChange(e.value)}
                    placeholder="Select room"
                    style={{ width: '100%' }}
                    filter
                />
            </div>

            <div>
                <label htmlFor="pi-ip" style={labelStyle}>IP Address *</label>
                <InputText id="pi-ip" value={piIpAddress} onChange={e => onPiIpAddressChange(e.target.value)} placeholder="e.g. 192.168.1.10" style={{ width: '100%' }}/>
            </div>

            <div>
                <label htmlFor="pi-port" style={labelStyle}>Port *</label>
                <InputNumber inputId="pi-port" value={piPort} onValueChange={e => onPiPortChange(e.value ?? null)} placeholder="e.g. 1000-9999" style={{ width: '100%' }} min={1000} max={9999} useGrouping={false}/>
            </div>

            <div>
                <label htmlFor="pi-interval" style={labelStyle}>Pushing Data Interval (seconds) {!editingPiId && '*'}</label>
                <InputNumber inputId="pi-interval" value={piInterval} onValueChange={e => onPiIntervalChange(e.value ?? null)} placeholder="e.g. 60" style={{ width: '100%' }} min={1}/>
            </div>

            {editingPiId && (
                <div>
                    <label style={labelStyle}>Connected Sensor Stations</label>
                    <DataTable value={connectedSensors} size="small" emptyMessage="No sensor stations connected.">
                        <Column field="name" header="Name"/>
                        <Column field="writeId" header="Write ID" style={{ maxWidth: '9rem', overflow: 'hidden', textOverflow: 'ellipsis' }}/>
                        <Column field="readId" header="Read ID" style={{ maxWidth: '9rem', overflow: 'hidden', textOverflow: 'ellipsis' }}/>
                        <Column
                            header=""
                            className="admin-actions-column"
                            headerClassName="admin-actions-column"
                            body={(row: SensorStationDTO) => (
                                <div className="admin-table-actions">
                                    <Button
                                        icon="pi pi-trash"
                                        rounded
                                        text
                                        severity="danger"
                                        title="Remove from this Raspberry Pi"
                                        onClick={() => onConfirmDelete({
                                            message: `Remove "${row.name ?? row.readId}" from this Raspberry Pi?`,
                                            onConfirm: () => onDisconnectSensor(row.readId!),
                                        })}
                                    />
                                </div>
                            )}
                        />
                    </DataTable>
                </div>
            )}
        </div>
    </Dialog>
);

export default RaspberryDialog;
