import React, { useState, useEffect, useRef } from 'react';
import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import { Dropdown } from 'primereact/dropdown';
import { Divider } from 'primereact/divider';
import { Toast } from 'primereact/toast';
import {
    AdminControllerApi,
    UserRoleControllerApi,
    RoomControllerApi,
    UserxDTO,
    UserxRole,
    UserRoleDTO,
    RoomDTO,
} from '../generated-skeleton-api';
import RoleManagement from '../components/RoleManagement';
import UserFormDialog, { UserFormState, emptyForm } from '../components/UserFormDialog';
import '../styles/Tables.css';

const PAGEABLE = { page: 0, size: 100, sort: [] };
const ALL_ROLES = Object.values(UserxRole);

const VALIDATION_MESSAGES = {
    required: 'Required',
    confirmationMismatch: 'Passwords do not match',
    roleRequired: 'At least one role required',
} as const;

const UserConfigurationPage: React.FC = () => {
    const toast = useRef<Toast>(null);
    const [sidebarVisible, setSidebarVisible] = useState(false);

    const [users, setUsers] = useState<UserxDTO[]>([]);
    const [roleDTOs, setRoleDTOs] = useState<UserRoleDTO[]>([]);
    const [rooms, setRooms] = useState<RoomDTO[]>([]);
    const [loading, setLoading] = useState(false);

    const [lastNameSearch, setLastNameSearch] = useState('');
    const [roleFilter, setRoleFilter] = useState<string | null>(null);

    const [showDialog, setShowDialog] = useState(false);
    const [isNewUser, setIsNewUser] = useState(true);
    const [editingUserId, setEditingUserId] = useState<string | undefined>();
    const [form, setForm] = useState<UserFormState>(emptyForm());
    const [formErrors, setFormErrors] = useState<Partial<Record<keyof UserFormState, string>>>({});
    const [dialogLoading, setDialogLoading] = useState(false);

    const fetchData = () => {
        setLoading(true);
        Promise.all([
            new AdminControllerApi().getAllUsers(),
            new UserRoleControllerApi().getAllPermissions(),
            new RoomControllerApi().getPageOfRooms({ pageable: PAGEABLE }),
        ]).then(([usersRes, rolesRes, roomsRes]) => {
            setUsers(usersRes.data ?? []);
            setRoleDTOs(rolesRes.data ?? []);
            setRooms(roomsRes.data.content ?? []);
        }).catch(() => {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to load data', life: 3000 });
        }).finally(() => setLoading(false));
    };

    useEffect(() => { fetchData(); }, []);

    const roomOptions = rooms.map(r => ({ label: r.name ?? r.id ?? '', value: r.id ?? '' }));
    const roleOptions = ALL_ROLES.map(r => ({ label: r, value: r }));
    const getRoomName = (roomId?: string) => rooms.find(r => r.id === roomId)?.name ?? null;

    const filteredUsers = users.filter(u => {
        if (lastNameSearch && !(u.lastName ?? '').toLowerCase().includes(lastNameSearch.toLowerCase())) return false;
        if (roleFilter && !(u.roles ?? new Set<UserxRole>()).has(roleFilter as UserxRole)) return false;
        return true;
    });

    const openCreate = () => {
        setForm(emptyForm());
        setFormErrors({});
        setIsNewUser(true);
        setEditingUserId(undefined);
        setShowDialog(true);
    };

    const openEdit = (user: UserxDTO) => {
        setForm({
            firstName: user.firstName ?? '',
            lastName: user.lastName ?? '',
            username: user.username ?? '',
            roomId: user.myRoom?.id ?? '',
            roles: Array.from(user.roles ?? []) as UserxRole[],
            password: '',
            repeatPassword: '',
            enabled: user.enabled ?? true,
        });
        setFormErrors({});
        setIsNewUser(false);
        setEditingUserId(user.id);
        setShowDialog(true);
    };

    const validate = (): boolean => {
        const errors: Partial<Record<keyof UserFormState, string>> = {};
        if (!form.firstName.trim()) errors.firstName = VALIDATION_MESSAGES.required;
        if (!form.lastName.trim()) errors.lastName = VALIDATION_MESSAGES.required;
        if (!form.username.trim()) errors.username = VALIDATION_MESSAGES.required;
        if (isNewUser) {
            const passwordEmpty = !form.password.trim();
            const passwordMismatch = form.password !== form.repeatPassword;
            if (passwordEmpty) errors.password = VALIDATION_MESSAGES.required;
            if (passwordMismatch) errors.repeatPassword = VALIDATION_MESSAGES.confirmationMismatch;
        }
        if (form.roles.length === 0) errors.roles = VALIDATION_MESSAGES.roleRequired;
        setFormErrors(errors);
        return Object.keys(errors).length === 0;
    };

    const handleSave = async () => {
        if (!validate()) return;
        setDialogLoading(true);
        try {
            if (isNewUser) {
                const res = await new AdminControllerApi().createUser({
                    userxCreateDTO: {
                        firstName: form.firstName,
                        lastName: form.lastName,
                        username: form.username,
                        enabled: form.enabled,
                        roles: new Set(form.roles),
                        password: form.password,
                    },
                });
                setUsers(prev => [...prev, res.data]);
                toast.current?.show({ severity: 'success', summary: 'Created', detail: 'User created successfully.', life: 3000 });
            } else {
                const res = await new AdminControllerApi().updateUser({
                    id: editingUserId!,
                    userxDTO: {
                        id: editingUserId,
                        firstName: form.firstName,
                        lastName: form.lastName,
                        username: form.username,
                        enabled: form.enabled,
                        roles: new Set(form.roles),
                    },
                });
                setUsers(prev => prev.map(u => u.id === res.data.id ? res.data : u));
                toast.current?.show({ severity: 'success', summary: 'Saved', detail: 'User updated successfully.', life: 3000 });
            }
            setShowDialog(false);
        } catch {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to save user.', life: 3000 });
        } finally {
            setDialogLoading(false);
        }
    };

    const handleDelete = (user: UserxDTO) => {
        if (!user.id || !globalThis.confirm(`Delete user "${user.username}"?`)) return;
        new AdminControllerApi().deleteUser({ id: user.id })
            .then(() => {
                setUsers(prev => prev.filter(u => u.id !== user.id));
                toast.current?.show({ severity: 'success', summary: 'Deleted', detail: 'User deleted.', life: 3000 });
            })
            .catch(() => toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to delete user.', life: 3000 }));
    };

    const actionsTemplate = (user: UserxDTO) => (
        <div style={{ display: 'flex', gap: '0.25rem', justifyContent: 'flex-end' }}>
            <Button icon="pi pi-pencil" rounded text severity="secondary" onClick={() => openEdit(user)} title="Edit user" />
            <Button icon="pi pi-trash" rounded text severity="danger" onClick={() => handleDelete(user)} title="Delete user" />
        </div>
    );

    const rolesTemplate = (user: UserxDTO) => (
        <span>{Array.from(user.roles ?? new Set()).join(', ') || '—'}</span>
    );

    return (
        <div className="dashboard-layout">
            <Toast ref={toast} />
            <PageHeader title="User Configuration" onMenuClick={() => setSidebarVisible(true)} />
            <SidebarComponent visible={sidebarVisible} onHide={() => setSidebarVisible(false)} />

            <div className="dashboard-content">

                <div className="table-container">
                    <div className="flex-header" style={{ marginBottom: '1rem', flexWrap: 'wrap', gap: '0.75rem' }}>
                        <h3 style={{ margin: 0 }}>User List</h3>
                        <Button label="Add User" icon="pi pi-user-plus" onClick={openCreate} />
                    </div>

                    <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '1rem', flexWrap: 'wrap', alignItems: 'center' }}>
                        <span className="p-input-icon-left">
                            <i className="pi pi-search" style={{ marginLeft: '0.7rem' }} />
                            <InputText
                                value={lastNameSearch}
                                onChange={e => setLastNameSearch(e.target.value)}
                                placeholder="Search by last name"
                                style={{ borderRadius: '20px', paddingLeft: '2.0rem' }}
                            />
                        </span>
                        <Dropdown
                            value={roleFilter}
                            options={roleOptions}
                            onChange={e => setRoleFilter(e.value)}
                            placeholder="Role Filter ▼"
                            showClear
                            style={{ borderRadius: '20px', minWidth: '180px' }}
                        />
                    </div>

                    <DataTable value={filteredUsers} loading={loading} stripedRows emptyMessage="No users found." responsiveLayout="scroll">
                        <Column field="id" header="ID" style={{ maxWidth: '10rem', overflow: 'hidden', textOverflow: 'ellipsis' }} />
                        <Column field="firstName" header="First Name" sortable />
                        <Column field="lastName" header="Last Name" sortable />
                        <Column
                            header="Room"
                            body={(u: UserxDTO) => {
                                const name = getRoomName(u.myRoom?.id);
                                return name ? <span>{name}</span> : <span style={{ color: '#9e9e9e' }}>N/A</span>;
                            }}
                        />
                        <Column header="Roles" body={rolesTemplate} />
                        <Column header="" body={actionsTemplate} style={{ width: '6rem' }} exportable={false} />
                    </DataTable>
                </div>

                <Divider />

                <RoleManagement roleDTOs={roleDTOs} onRoleDTOsChange={setRoleDTOs} />
            </div>

            <UserFormDialog
                visible={showDialog}
                isNewUser={isNewUser}
                form={form}
                formErrors={formErrors}
                roomOptions={roomOptions}
                loading={dialogLoading}
                onHide={() => setShowDialog(false)}
                onSave={handleSave}
                onChange={patch => setForm(f => ({ ...f, ...patch }))}
            />
        </div>
    );
};

export default UserConfigurationPage;
