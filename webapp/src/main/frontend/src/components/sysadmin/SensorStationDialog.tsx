import React from 'react';
import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import { Dropdown } from 'primereact/dropdown';
import { InputText } from 'primereact/inputtext';
import { SelectOption } from './sysAdminTypes';

const labelStyle: React.CSSProperties = {
    display: 'block',
    marginBottom: '0.35rem',
    fontWeight: 500,
    fontSize: '0.9rem',
};

interface SensorStationDialogProps {
    readonly visible: boolean;
    readonly editingSensorId: string | null;
    readonly editingSensorWriteId: string | null;
    readonly sensorName: string;
    readonly onSensorNameChange: (value: string) => void;
    readonly sensorRoomId: string;
    readonly onSensorRoomIdChange: (value: string) => void;
    readonly roomOptions: SelectOption[];
    readonly loading: boolean;
    readonly onSave: () => void;
    readonly onHide: () => void;
}

const SensorStationDialog: React.FC<SensorStationDialogProps> = ({
    visible,
    editingSensorId,
    editingSensorWriteId,
    sensorName,
    onSensorNameChange,
    sensorRoomId,
    onSensorRoomIdChange,
    roomOptions,
    loading,
    onSave,
    onHide,
}) => (
    <Dialog
        header={editingSensorId ? 'Edit Sensor Station' : 'Add Sensor Station'}
        visible={visible}
        style={{ width: '480px' }}
        onHide={onHide}
        footer={
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
                <Button label="Cancel" severity="secondary" outlined onClick={onHide}/>
                <Button label={editingSensorId ? 'Save' : 'Create'} icon="pi pi-check" loading={loading} onClick={onSave}/>
            </div>
        }
        draggable={false}
    >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div>
                <label htmlFor="sensor-name" style={labelStyle}>Name *</label>
                <InputText id="sensor-name" value={sensorName} onChange={e => onSensorNameChange(e.target.value)} placeholder="e.g. Station-A1" style={{ width: '100%' }}/>
            </div>
            <div>
                <label htmlFor="sensor-room" style={labelStyle}>Room *</label>
                <Dropdown inputId="sensor-room" value={sensorRoomId} options={roomOptions} onChange={e => onSensorRoomIdChange(e.value)} placeholder="Select room" style={{ width: '100%' }} filter/>
            </div>
            {editingSensorId && (
                <>
                    <div>
                        <label style={labelStyle}>Read ID</label>
                        <InputText value={String(editingSensorId)} readOnly style={{ width: '100%', fontFamily: 'monospace', fontSize: '0.85rem', background: '#f5f5f5', color: '#555' }}/>
                    </div>
                    <div>
                        <label style={labelStyle}>Write ID</label>
                        <InputText value={editingSensorWriteId ? String(editingSensorWriteId) : ''} readOnly style={{ width: '100%', fontFamily: 'monospace', fontSize: '0.85rem', background: '#f5f5f5', color: '#555' }}/>
                    </div>
                </>
            )}
        </div>
    </Dialog>
);

export default SensorStationDialog;
