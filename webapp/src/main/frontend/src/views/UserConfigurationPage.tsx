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
import { UserRoleControllerApi, RoomControllerApi, UserRoleDTO, RoomDTO } from '../generated-skeleton-api';
import globalAxios from 'axios';
import RoleManagement from '../components/RoleManagement';
import UserFormDialog, { UserFormState, emptyForm } from '../components/UserFormDialog';
import '../styles/Tables.css';

const PAGEABLE = { page: 0, size: 100, sort: [] };

interface UserRoleSummary { id: string; name: string; }
interface UserRoomSummary { id: string; departmentName: string; roomNumber: string; }
interface FullUser {
    id: string;
    username: string;
    firstName: string;
    lastName: string;
    enabled: boolean;
    roles: UserRoleSummary[];
    myRoom: UserRoomSummary | null;
}

const VALIDATION_MESSAGES = {
    required: 'Required',
    confirmationMismatch: 'Passwords do not match',
    roleRequired: 'At least one role required',
} as const;

const UserConfigurationPage: React.FC = () => {
    const toast = useRef<Toast>(null);
    const [sidebarVisible, setSidebarVisible] = useState(false);

    const [users, setUsers] = useState<FullUser[]>([]);
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
            globalAxios.get<{ content: FullUser[] }>('/api/users?size=1000'),
            new UserRoleControllerApi().getAllPermissions(),
            new RoomControllerApi().getPageOfRooms({ pageable: PAGEABLE }),
        ]).then(([usersRes, rolesRes, roomsRes]) => {
            setUsers(usersRes.data.content ?? []);
            setRoleDTOs(rolesRes.data ?? []);
            setRooms(roomsRes.data.content ?? []);
        }).catch(() => {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to load data', life: 3000 });
        }).finally(() => setLoading(false));
    };

    useEffect(() => { fetchData(); }, []);

    const roomOptions = rooms.map(r => ({ label: r.name ?? r.id ?? '', value: r.id ?? '' }));
    const roleOptions = roleDTOs.map(r => ({ label: r.name ?? '', value: r.id ?? '' }));
    const roleFilterOptions = roleDTOs.map(r => ({ label: r.name ?? '', value: r.name ?? '' }));

    const filteredUsers = users.filter(u => {
        if (u.roles.some(r => r.name === 'RASPBERRY_PI')) return false;
        if (lastNameSearch && !(u.lastName ?? '').toLowerCase().includes(lastNameSearch.toLowerCase())) return false;
        if (roleFilter && !u.roles.some(r => r.name === roleFilter)) return false;
        return true;
    });

    const openCreate = () => {
        setForm(emptyForm());
        setFormErrors({});
        setIsNewUser(true);
        setEditingUserId(undefined);
        setShowDialog(true);
    };

    const openEdit = (user: FullUser) => {
        setForm({
            firstName: user.firstName ?? '',
            lastName: user.lastName ?? '',
            username: user.username ?? '',
            roomId: user.myRoom?.id ?? '',
            roleIds: user.roles.map(r => r.id),
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
            if (!form.password.trim()) errors.password = VALIDATION_MESSAGES.required;
            if (form.password !== form.repeatPassword) errors.repeatPassword = VALIDATION_MESSAGES.confirmationMismatch;
        }
        if (form.roleIds.length === 0) errors.roleIds = VALIDATION_MESSAGES.roleRequired;
        setFormErrors(errors);
        return Object.keys(errors).length === 0;
    };

    const handleSave = async () => {
        if (!validate()) return;
        setDialogLoading(true);
        try {
            if (isNewUser) {
                const res = await globalAxios.post<FullUser>('/api/users', {
                    firstName: form.firstName,
                    lastName: form.lastName,
                    username: form.username,
                    enabled: form.enabled,
                    roles: form.roleIds,
                    password: form.password,
                    roomId: form.roomId || null,
                });
                setUsers(prev => [...prev, res.data]);
                toast.current?.show({ severity: 'success', summary: 'Created', detail: 'User created successfully.', life: 3000 });
            } else {
                const res = await globalAxios.patch<FullUser>(`/api/users/${editingUserId}`, {
                    firstName: form.firstName,
                    lastName: form.lastName,
                    username: form.username,
                    isEnabled: form.enabled,
                    roles: form.roleIds,
                    roomId: form.roomId || null,
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

    const handleDelete = (user: FullUser) => {
        if (!user.id || !globalThis.confirm(`Delete user "${user.username}"?`)) return;
        globalAxios.delete(`/api/users/${user.id}`)
            .then(() => {
                setUsers(prev => prev.filter(u => u.id !== user.id));
                toast.current?.show({ severity: 'success', summary: 'Deleted', detail: 'User deleted.', life: 3000 });
            })
            .catch(() => toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to delete user.', life: 3000 }));
    };

    const actionsTemplate = (user: FullUser) => (
        <div style={{ display: 'flex', gap: '0.25rem', justifyContent: 'flex-end' }}>
            <Button icon="pi pi-pencil" rounded text severity="secondary" onClick={() => openEdit(user)} title="Edit user" />
            <Button icon="pi pi-trash" rounded text severity="danger" onClick={() => handleDelete(user)} title="Delete user" />
        </div>
    );

    const rolesTemplate = (user: FullUser) => (
        <span>{user.roles.map(r => r.name).join(', ') || '—'}</span>
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
                            options={roleFilterOptions}
                            onChange={e => setRoleFilter(e.value)}
                            placeholder="Role Filter"
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
                            body={(u: FullUser) => u.myRoom
                                ? <span>{u.myRoom.roomNumber} ({u.myRoom.departmentName})</span>
                                : <span style={{ color: '#9e9e9e' }}>N/A</span>
                            }
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
                roleOptions={roleOptions}
                loading={dialogLoading}
                onHide={() => setShowDialog(false)}
                onSave={handleSave}
                onChange={patch => setForm(f => ({ ...f, ...patch }))}
            />
        </div>
    );
};

export default UserConfigurationPage;
