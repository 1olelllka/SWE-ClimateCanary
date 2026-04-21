import React from 'react';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import { Password } from 'primereact/password';
import { Dropdown } from 'primereact/dropdown';
import { MultiSelect } from 'primereact/multiselect';
import { Dialog } from 'primereact/dialog';
import { UserxRole } from '../generated-skeleton-api';

export interface UserFormState {
    firstName: string;
    lastName: string;
    username: string;
    roomId: string;
    roles: UserxRole[];
    password: string;
    repeatPassword: string;
    enabled: boolean;
}

export const emptyForm = (): UserFormState => ({
    firstName: '', lastName: '', username: '',
    roomId: '', roles: [], password: '', repeatPassword: '', enabled: true,
});

const ALL_ROLES = Object.values(UserxRole);

const labelStyle: React.CSSProperties = {
    display: 'block',
    marginBottom: '0.35rem',
    fontWeight: 500,
    fontSize: '0.9rem',
};

interface Props {
    readonly visible: boolean;
    readonly isNewUser: boolean;
    readonly form: UserFormState;
    readonly formErrors: Partial<Record<keyof UserFormState, string>>;
    readonly roomOptions: { label: string; value: string }[];
    readonly loading: boolean;
    readonly onHide: () => void;
    readonly onSave: () => void;
    readonly onChange: (patch: Partial<UserFormState>) => void;
}

const UserFormDialog: React.FC<Props> = ({
    visible, isNewUser, form, formErrors, roomOptions, loading, onHide, onSave, onChange,
}) => {
    const roleOptions = ALL_ROLES.map(r => ({ label: r, value: r }));

    const footer = (
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
            <Button label="Cancel" severity="secondary" outlined onClick={onHide} />
            <Button
                label={isNewUser ? 'Create' : 'Save'}
                icon="pi pi-check"
                loading={loading}
                onClick={onSave}
            />
        </div>
    );

    return (
        <Dialog
            header={isNewUser ? 'Add User' : 'Edit User'}
            visible={visible}
            style={{ width: '500px' }}
            onHide={onHide}
            footer={footer}
            draggable={false}
        >
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                <div>
                    <label htmlFor="uf-firstname" style={labelStyle}>First Name *</label>
                    <InputText
                        id="uf-firstname"
                        value={form.firstName}
                        onChange={e => onChange({ firstName: e.target.value })}
                        placeholder="First Name"
                        style={{ width: '100%' }}
                        className={formErrors.firstName ? 'p-invalid' : ''}
                    />
                    {formErrors.firstName && <small className="p-error">{formErrors.firstName}</small>}
                </div>

                <div>
                    <label htmlFor="uf-lastname" style={labelStyle}>Last Name *</label>
                    <InputText
                        id="uf-lastname"
                        value={form.lastName}
                        onChange={e => onChange({ lastName: e.target.value })}
                        placeholder="Last Name"
                        style={{ width: '100%' }}
                        className={formErrors.lastName ? 'p-invalid' : ''}
                    />
                    {formErrors.lastName && <small className="p-error">{formErrors.lastName}</small>}
                </div>

                <div>
                    <label htmlFor="uf-username" style={labelStyle}>Username *</label>
                    <InputText
                        id="uf-username"
                        value={form.username}
                        onChange={e => onChange({ username: e.target.value })}
                        placeholder="Username"
                        style={{ width: '100%' }}
                        className={formErrors.username ? 'p-invalid' : ''}
                    />
                    {formErrors.username && <small className="p-error">{formErrors.username}</small>}
                </div>

                <div>
                    <label htmlFor="uf-room" style={labelStyle}>Room</label>
                    <Dropdown
                        inputId="uf-room"
                        value={form.roomId}
                        options={roomOptions}
                        onChange={e => onChange({ roomId: e.value })}
                        placeholder="Select room"
                        style={{ width: '100%' }}
                        showClear
                        filter
                    />
                </div>

                <div>
                    <label htmlFor="uf-roles" style={labelStyle}>Role *</label>
                    <MultiSelect
                        inputId="uf-roles"
                        value={form.roles}
                        options={roleOptions}
                        onChange={e => onChange({ roles: e.value })}
                        placeholder="Select roles"
                        style={{ width: '100%' }}
                        className={formErrors.roles ? 'p-invalid' : ''}
                        display="chip"
                    />
                    {formErrors.roles && <small className="p-error">{formErrors.roles}</small>}
                </div>

                {isNewUser && (
                    <>
                        <div>
                            <label htmlFor="uf-password" style={labelStyle}>Initial Password *</label>
                            <Password
                                inputId="uf-password"
                                value={form.password}
                                onChange={e => onChange({ password: e.target.value })}
                                placeholder="Password"
                                style={{ width: '100%' }}
                                inputStyle={{ width: '100%' }}
                                feedback={false}
                                toggleMask
                                className={formErrors.password ? 'p-invalid' : ''}
                            />
                            {formErrors.password && <small className="p-error">{formErrors.password}</small>}
                        </div>

                        <div>
                            <label htmlFor="uf-repeat-password" style={labelStyle}>Repeat Password *</label>
                            <Password
                                inputId="uf-repeat-password"
                                value={form.repeatPassword}
                                onChange={e => onChange({ repeatPassword: e.target.value })}
                                placeholder="Repeat Password"
                                style={{ width: '100%' }}
                                inputStyle={{ width: '100%' }}
                                feedback={false}
                                toggleMask
                                className={formErrors.repeatPassword ? 'p-invalid' : ''}
                            />
                            {formErrors.repeatPassword && <small className="p-error">{formErrors.repeatPassword}</small>}
                        </div>
                    </>
                )}
            </div>
        </Dialog>
    );
};

export default UserFormDialog;
