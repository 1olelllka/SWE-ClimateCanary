import React from 'react';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import { InputNumber } from 'primereact/inputnumber';
import { Dropdown } from 'primereact/dropdown';
import { RoomCreateDTORoomTypeEnum } from '../generated-skeleton-api';

export interface RoomFormState {
    name: string;
    departmentID: string;
    roomType: RoomCreateDTORoomTypeEnum;
    defaultPeopleCount: number;
}

export const emptyRoomForm = (): RoomFormState => ({
    name: '',
    departmentID: '',
    roomType: RoomCreateDTORoomTypeEnum.OFFICE,
    defaultPeopleCount: 1,
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
    readonly form: RoomFormState;
    readonly formErrors: Partial<Record<keyof RoomFormState, string>>;
    readonly departmentOptions: { label: string; value: string }[];
    readonly loading: boolean;
    readonly onHide: () => void;
    readonly onSave: () => void;
    readonly onChange: (patch: Partial<RoomFormState>) => void;
}

const RoomFormDialog: React.FC<Props> = ({
    visible, isNew, form, formErrors, departmentOptions, loading, onHide, onSave, onChange,
}) => {
    const footer = (
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
            <Button label="Cancel" severity="secondary" outlined onClick={onHide} />
            <Button label={isNew ? 'Create' : 'Save'} icon="pi pi-check" loading={loading} onClick={onSave} />
        </div>
    );

    return (
        <Dialog
            header={isNew ? 'Add Room' : 'Edit Room'}
            visible={visible}
            style={{ width: '460px' }}
            onHide={onHide}
            footer={footer}
            draggable={false}
        >
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                <div>
                    <label htmlFor="rf-name" style={labelStyle}>Name *</label>
                    <InputText
                        id="rf-name"
                        value={form.name}
                        onChange={e => onChange({ name: e.target.value })}
                        placeholder="Room name"
                        style={{ width: '100%' }}
                        className={formErrors.name ? 'p-invalid' : ''}
                    />
                    {formErrors.name && <small className="p-error">{formErrors.name}</small>}
                </div>

                <div>
                    <label htmlFor="rf-dept" style={labelStyle}>Department *</label>
                    <Dropdown
                        inputId="rf-dept"
                        value={form.departmentID}
                        options={departmentOptions}
                        onChange={e => onChange({ departmentID: e.value })}
                        placeholder="Select department"
                        style={{ width: '100%' }}
                        filter
                        className={formErrors.departmentID ? 'p-invalid' : ''}
                    />
                    {formErrors.departmentID && <small className="p-error">{formErrors.departmentID}</small>}
                </div>

                <div>
                    <label htmlFor="rf-type" style={labelStyle}>Room Type *</label>
                    <Dropdown
                        inputId="rf-type"
                        value={form.roomType}
                        options={ROOM_TYPE_OPTIONS}
                        onChange={e => onChange({ roomType: e.value })}
                        style={{ width: '100%' }}
                    />
                </div>

                <div>
                    <label htmlFor="rf-capacity" style={labelStyle}>Default Capacity *</label>
                    <InputNumber
                        inputId="rf-capacity"
                        value={form.defaultPeopleCount}
                        onValueChange={e => onChange({ defaultPeopleCount: e.value ?? 1 })}
                        min={1}
                        style={{ width: '100%' }}
                        inputStyle={{ width: '100%' }}
                        className={formErrors.defaultPeopleCount ? 'p-invalid' : ''}
                    />
                    {formErrors.defaultPeopleCount && <small className="p-error">{formErrors.defaultPeopleCount}</small>}
                </div>
            </div>
        </Dialog>
    );
};

export default RoomFormDialog;
