import React from 'react';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';

export interface BuildingFormState {
    name: string;
    address: string;
}

export const emptyBuildingForm = (): BuildingFormState => ({ name: '', address: '' });

const labelStyle: React.CSSProperties = {
    display: 'block',
    marginBottom: '0.35rem',
    fontWeight: 500,
    fontSize: '0.9rem',
};

interface Props {
    readonly visible: boolean;
    readonly isNew: boolean;
    readonly form: BuildingFormState;
    readonly formErrors: Partial<Record<keyof BuildingFormState, string>>;
    readonly loading: boolean;
    readonly onHide: () => void;
    readonly onSave: () => void;
    readonly onChange: (patch: Partial<BuildingFormState>) => void;
}

const BuildingFormDialog: React.FC<Props> = ({
                                                 visible, isNew, form, formErrors, loading, onHide, onSave, onChange,
                                             }) => {
    const footer = (
        <div className="admin-dialog-footer">
            <Button label="Cancel" severity="secondary" outlined onClick={onHide} />
            <Button label={isNew ? 'Create' : 'Save'} icon="pi pi-check" loading={loading} onClick={onSave} />
        </div>
    );

    return (
        <Dialog
            header={isNew ? 'Add Building' : 'Edit Building'}
            visible={visible}
            className="admin-form-dialog"
            style={{ width: '460px' }}
            onHide={onHide}
            footer={footer}
            draggable={false}
        >
            <div className="admin-dialog-body">
                <div>
                    <label htmlFor="bf-name" style={labelStyle}>Name *</label>
                    <InputText
                        id="bf-name"
                        value={form.name}
                        onChange={e => onChange({ name: e.target.value })}
                        placeholder="Building name"
                        style={{ width: '100%' }}
                        className={formErrors.name ? 'p-invalid' : ''}
                    />
                    {formErrors.name && <small className="p-error">{formErrors.name}</small>}
                </div>

                <div>
                    <label htmlFor="bf-address" style={labelStyle}>Address</label>
                    <InputText
                        id="bf-address"
                        value={form.address}
                        onChange={e => onChange({ address: e.target.value })}
                        placeholder="Address"
                        style={{ width: '100%' }}
                    />
                </div>
            </div>
        </Dialog>
    );
};

export default BuildingFormDialog;