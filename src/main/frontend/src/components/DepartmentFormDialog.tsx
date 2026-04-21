import React from 'react';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import { InputNumber } from 'primereact/inputnumber';
import { Dropdown } from 'primereact/dropdown';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { RoomCreateDTORoomTypeEnum } from '../generated-skeleton-api';

export interface RoomDraft {
    name: string;
    roomType: RoomCreateDTORoomTypeEnum;
    defaultPeopleCount: number;
}

export interface DepartmentFormState {
    name: string;
    buildingID: string;
    rooms: RoomDraft[];
}

export const emptyDepartmentForm = (): DepartmentFormState => ({
    name: '',
    buildingID: '',
    rooms: [],
});

const labelStyle: React.CSSProperties = {
    display: 'block',
    marginBottom: '0.35rem',
    fontWeight: 500,
    fontSize: '0.9rem',
};

const ROOM_TYPE_OPTIONS = Object.values(RoomCreateDTORoomTypeEnum).map(v => ({ label: v, value: v }));

interface Props {
    readonly visible: boolean;
    readonly isNew: boolean;
    readonly form: DepartmentFormState;
    readonly formErrors: Partial<Record<keyof DepartmentFormState, string>>;
    readonly buildingOptions: { label: string; value: string }[];
    readonly loading: boolean;
    readonly onHide: () => void;
    readonly onSave: () => void;
    readonly onChange: (patch: Partial<DepartmentFormState>) => void;
}

const DepartmentFormDialog: React.FC<Props> = ({
    visible, isNew, form, formErrors, buildingOptions, loading, onHide, onSave, onChange,
}) => {
    const addRoom = () => {
        onChange({
            rooms: [...form.rooms, { name: '', roomType: RoomCreateDTORoomTypeEnum.OFFICE, defaultPeopleCount: 1 }],
        });
    };

    const updateRoom = (index: number, patch: Partial<RoomDraft>) => {
        const updated = form.rooms.map((r, i) => i === index ? { ...r, ...patch } : r);
        onChange({ rooms: updated });
    };

    const removeRoom = (index: number) => {
        onChange({ rooms: form.rooms.filter((_, i) => i !== index) });
    };

    const roomActionsTemplate = (_: RoomDraft, options: { rowIndex: number }) => (
        <Button
            icon="pi pi-trash"
            rounded
            text
            severity="danger"
            title="Remove room"
            onClick={() => removeRoom(options.rowIndex)}
        />
    );

    const roomNameTemplate = (row: RoomDraft, options: { rowIndex: number }) => (
        <InputText
            value={row.name}
            onChange={e => updateRoom(options.rowIndex, { name: e.target.value })}
            placeholder="Room name"
            style={{ width: '100%' }}
        />
    );

    const roomTypeTemplate = (row: RoomDraft, options: { rowIndex: number }) => (
        <Dropdown
            value={row.roomType}
            options={ROOM_TYPE_OPTIONS}
            onChange={e => updateRoom(options.rowIndex, { roomType: e.value })}
            style={{ width: '100%' }}
        />
    );

    const roomCapacityTemplate = (row: RoomDraft, options: { rowIndex: number }) => (
        <InputNumber
            value={row.defaultPeopleCount}
            onValueChange={e => updateRoom(options.rowIndex, { defaultPeopleCount: e.value ?? 1 })}
            min={1}
            style={{ width: '100%' }}
            inputStyle={{ width: '100%' }}
        />
    );

    const footer = (
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
            <Button label="Cancel" severity="secondary" outlined onClick={onHide} />
            <Button label={isNew ? 'Create' : 'Save'} icon="pi pi-check" loading={loading} onClick={onSave} />
        </div>
    );

    return (
        <Dialog
            header={isNew ? 'Add Department' : 'Edit Department'}
            visible={visible}
            style={{ width: '640px' }}
            onHide={onHide}
            footer={footer}
            draggable={false}
        >
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                <div>
                    <label htmlFor="df-name" style={labelStyle}>Name *</label>
                    <InputText
                        id="df-name"
                        value={form.name}
                        onChange={e => onChange({ name: e.target.value })}
                        placeholder="Department name"
                        style={{ width: '100%' }}
                        className={formErrors.name ? 'p-invalid' : ''}
                    />
                    {formErrors.name && <small className="p-error">{formErrors.name}</small>}
                </div>

                <div>
                    <label htmlFor="df-building" style={labelStyle}>Building *</label>
                    <Dropdown
                        inputId="df-building"
                        value={form.buildingID}
                        options={buildingOptions}
                        onChange={e => onChange({ buildingID: e.value })}
                        placeholder="Select building"
                        style={{ width: '100%' }}
                        filter
                        className={formErrors.buildingID ? 'p-invalid' : ''}
                    />
                    {formErrors.buildingID && <small className="p-error">{formErrors.buildingID}</small>}
                </div>

                {isNew && (
                    <div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                            <span style={{ fontWeight: 500, fontSize: '0.9rem' }}>Rooms</span>
                            <Button label="Add Room" icon="pi pi-plus" size="small" onClick={addRoom} />
                        </div>
                        {form.rooms.length > 0 && (
                            <DataTable value={form.rooms} emptyMessage="No rooms added." size="small">
                                <Column header="Name" body={roomNameTemplate} />
                                <Column header="Type" body={roomTypeTemplate} style={{ width: '9rem' }} />
                                <Column header="Capacity" body={roomCapacityTemplate} style={{ width: '7rem' }} />
                                <Column header="" body={roomActionsTemplate} style={{ width: '3rem' }} />
                            </DataTable>
                        )}
                        {form.rooms.length === 0 && (
                            <span style={{ color: '#9e9e9e', fontSize: '0.9rem' }}>No rooms added yet.</span>
                        )}
                    </div>
                )}
            </div>
        </Dialog>
    );
};

export default DepartmentFormDialog;
