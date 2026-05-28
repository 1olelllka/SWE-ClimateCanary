import React from 'react';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import { InputNumber } from 'primereact/inputnumber';
import { Dropdown } from 'primereact/dropdown';
import { MultiSelect } from 'primereact/multiselect';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Tag } from 'primereact/tag';
import { RoomCreateDTORoomTypeEnum, RoomDTO } from '../generated-skeleton-api';

export interface RoomDraft {
    name: string;
    roomType: RoomCreateDTORoomTypeEnum;
    defaultPeopleCount: number;
}

export interface DepartmentFormState {
    name: string;
    buildingID: string;
    currentRoomIds: string[];
    existingRoomIds: string[];
    rooms: RoomDraft[];
    roomIdsToDelete: string[];
}

export const emptyDepartmentForm = (): DepartmentFormState => ({
    name: '',
    buildingID: '',
    currentRoomIds: [],
    existingRoomIds: [],
    rooms: [],
    roomIdsToDelete: [],
});

type CurrentRow  = { kind: 'current'; id: string; name: string; roomType: string; defaultPeopleCount: number };
type AssignedRow = { kind: 'assigned'; id: string; name: string; roomType: string; defaultPeopleCount: number };
type NewRow      = { kind: 'new'; draftIndex: number } & RoomDraft;
type RoomRow = CurrentRow | AssignedRow | NewRow;

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
    readonly availableRooms: RoomDTO[];
    readonly loading: boolean;
    readonly onHide: () => void;
    readonly onSave: () => void;
    readonly onChange: (patch: Partial<DepartmentFormState>) => void;
}

const DepartmentFormDialog: React.FC<Props> = ({
                                                   visible, isNew, form, formErrors, buildingOptions, availableRooms, loading, onHide, onSave, onChange,
                                               }) => {
    const addRoom = () => {
        onChange({ rooms: [...form.rooms, { name: '', roomType: RoomCreateDTORoomTypeEnum.OFFICE, defaultPeopleCount: 1 }] });
    };

    const updateRoom = (draftIndex: number, patch: Partial<RoomDraft>) => {
        onChange({ rooms: form.rooms.map((r, i) => i === draftIndex ? { ...r, ...patch } : r) });
    };

    const removeCurrent = (id: string) => {
        onChange({
            currentRoomIds: form.currentRoomIds.filter(rid => rid !== id),
            roomIdsToDelete: [...form.roomIdsToDelete, id],
        });
    };

    const removeAssigned = (id: string) => {
        onChange({ existingRoomIds: form.existingRoomIds.filter(rid => rid !== id) });
    };

    const removeNew = (draftIndex: number) => {
        onChange({ rooms: form.rooms.filter((_, i) => i !== draftIndex) });
    };

    const lookupRoom = (id: string): RoomDTO | undefined => availableRooms.find(r => r.id === id);

    const currentRows: CurrentRow[] = form.currentRoomIds.map(id => {
        const r = lookupRoom(id);
        return { kind: 'current', id, name: r?.name ?? id, roomType: r?.roomType ?? '', defaultPeopleCount: r?.defaultPeopleCount ?? 0 };
    });

    const assignedRows: AssignedRow[] = form.existingRoomIds.map(id => {
        const r = lookupRoom(id);
        return { kind: 'assigned', id, name: r?.name ?? id, roomType: r?.roomType ?? '', defaultPeopleCount: r?.defaultPeopleCount ?? 0 };
    });

    const newRows: NewRow[] = form.rooms.map((r, i) => ({ kind: 'new', draftIndex: i, ...r }));
    const allRows: RoomRow[] = [...currentRows, ...assignedRows, ...newRows];

    const sourceTemplate = (row: RoomRow) => {
        if (row.kind === 'new') return <Tag value="New" severity="success" />;
        return <Tag value="Existing" severity="info" />;
    };

    const nameTemplate = (row: RoomRow) =>
        row.kind === 'new' ? (
            <InputText value={row.name} onChange={e => updateRoom(row.draftIndex, { name: e.target.value })} placeholder="Room name" style={{ width: '100%' }} />
        ) : <span>{row.name}</span>;

    const typeTemplate = (row: RoomRow) =>
        row.kind === 'new' ? (
            <Dropdown value={row.roomType} options={ROOM_TYPE_OPTIONS} onChange={e => updateRoom(row.draftIndex, { roomType: e.value })} style={{ width: '100%' }} />
        ) : <span>{row.roomType}</span>;

    const capacityTemplate = (row: RoomRow) =>
        row.kind === 'new' ? (
            <InputNumber value={row.defaultPeopleCount} onValueChange={e => updateRoom(row.draftIndex, { defaultPeopleCount: e.value ?? 1 })} min={1} style={{ width: '100%' }} inputStyle={{ width: '100%' }} />
        ) : <span>{row.defaultPeopleCount}</span>;

    const actionsTemplate = (row: RoomRow) => (
        <div className="admin-table-actions">
            <Button icon="pi pi-trash" rounded text severity="danger" title="Remove" onClick={() => {
                if (row.kind === 'current')  removeCurrent(row.id);
                if (row.kind === 'assigned') removeAssigned(row.id);
                if (row.kind === 'new')      removeNew(row.draftIndex);
            }} />
        </div>
    );

    const excludedIds = new Set([...form.currentRoomIds, ...form.existingRoomIds, ...form.roomIdsToDelete]);

    const multiSelectOptions = availableRooms
        .filter(r => r.id && r.name && !excludedIds.has(r.id))
        .map(r => ({
            label: r.departmentName ? `${r.name} (${r.departmentName})` : r.name!,
            value: r.id!,
        }));

    const footer = (
        <div className="admin-dialog-footer">
            <Button label="Cancel" severity="secondary" outlined onClick={onHide} />
            <Button label={isNew ? 'Create' : 'Save'} icon="pi pi-check" loading={loading} onClick={onSave} />
        </div>
    );

    const roomsSection = (
        <div>
            <div className="admin-dialog-section-header">
                <span style={labelStyle as React.CSSProperties}>Rooms</span>
                <Button label="Add New Room" icon="pi pi-plus" size="small" className="admin-add-button" onClick={addRoom} />
            </div>

            <MultiSelect
                value={form.existingRoomIds}
                options={multiSelectOptions}
                onChange={e => onChange({ existingRoomIds: e.value })}
                placeholder="Search and assign existing rooms…"
                filter
                style={{ width: '100%', marginBottom: '0.75rem' }}
                display="comma"
                selectedItemsLabel="{0} existing room(s) selected"
                emptyFilterMessage="No rooms found"
                emptyMessage="No rooms available"
            />

            {allRows.length > 0 ? (
                <DataTable value={allRows} size="small" emptyMessage="No rooms." className="admin-dialog-table">
                    <Column header="" body={sourceTemplate} style={{ width: '6rem' }} />
                    <Column header="Name" body={nameTemplate} />
                    <Column header="Type" body={typeTemplate} style={{ width: '9rem' }} />
                    <Column header="Capacity" body={capacityTemplate} style={{ width: '7rem' }} />
                    <Column header="" body={actionsTemplate} style={{ width: '3rem' }} />
                </DataTable>
            ) : (
                <span className="admin-dialog-empty-text">
                    No rooms yet. Search above to assign existing ones, or click "Add New Room".
                </span>
            )}
        </div>
    );

    return (
        <Dialog
            header={isNew ? 'Add Department' : 'Edit Department'}
            visible={visible}
            className="admin-form-dialog admin-form-dialog-wide"
            style={{ width: '700px' }}
            onHide={onHide}
            footer={footer}
            draggable={false}
        >
            <div className="admin-dialog-body">
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

                {roomsSection}
            </div>
        </Dialog>
    );
};

export default DepartmentFormDialog;