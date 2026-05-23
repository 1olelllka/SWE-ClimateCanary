import React, { useState, useEffect, useRef } from 'react';
import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import { Dropdown } from 'primereact/dropdown';
import { Toast } from 'primereact/toast';
import { UserRoleControllerApi, RoomControllerApi, UserRoleDTO, RoomDTO, UserxControllerApi } from '../generated-skeleton-api';
import RoleManagement from '../components/RoleManagement';
import UserFormDialog, { UserFormState, emptyForm } from '../components/UserFormDialog';
import UserListComponent from '../components/UserListComponent';
import ConfirmDeleteDialog from '../components/ConfirmDeleteDialog';
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
    const [roomFilter, setRoomFilter] = useState<string | null>(null);

    const [confirmDelete, setConfirmDelete] = useState<{ message: string; onConfirm: () => void } | null>(null);

    const [showDialog, setShowDialog] = useState(false);
    const [isNewUser, setIsNewUser] = useState(true);
    const [editingUserId, setEditingUserId] = useState<string | undefined>();
    const [form, setForm] = useState<UserFormState>(emptyForm());
    const [formErrors, setFormErrors] = useState<Partial<Record<keyof UserFormState, string>>>({});
    const [dialogLoading, setDialogLoading] = useState(false);

    const fetchData = () => {
        setLoading(true);
        Promise.all([
            new UserxControllerApi().getPageOfUsers({ pageable: { page: 0, size: 1000, sort: [] } }),
            new UserRoleControllerApi().getAllPermissions(),
            new RoomControllerApi().getPageOfRooms({ pageable: PAGEABLE }),
        ]).then(([usersRes, rolesRes, roomsRes]) => {
            setUsers((usersRes.data as any).content ?? []);
            setRoleDTOs(rolesRes.data ?? []);
            setRooms(roomsRes.data.content ?? []);
        }).catch(() => {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to load data', life: 3000 });
        }).finally(() => setLoading(false));
    };

    useEffect(() => { fetchData(); }, []);

    const roomOptions = rooms.map(r => ({ label: r.name ?? r.id ?? '', value: r.id ?? '' }));
    const roomFilterOptions = rooms.map(r => ({label: r.departmentName ? `${r.name ?? r.id} (${r.departmentName})` : `${r.name ?? r.id}`, value: r.id ?? '',}));
    const roleOptions = roleDTOs.map(r => ({ label: r.name ?? '', value: r.id ?? '' }));
    const roleFilterOptions = roleDTOs.map(r => ({ label: r.name ?? '', value: r.name ?? '' }));

    const filteredUsers = users.filter(u => {
        if (u.roles.some(r => r.name === 'RASPBERRY_PI')) return false;
        if (lastNameSearch && !(u.lastName ?? '').toLowerCase().includes(lastNameSearch.toLowerCase())) return false;
        if (roleFilter && !u.roles.some(r => r.name === roleFilter)) return false;
        if (roomFilter && u.myRoom?.id !== roomFilter) return false;
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
                const res = await new UserxControllerApi().createNewUser({
                    userxCreateDTO: {
                        firstName: form.firstName,
                        lastName: form.lastName,
                        username: form.username,
                        enabled: form.enabled,
                        roles: form.roleIds,
                        password: form.password,
                        roomId: form.roomId || null,
                    } as any,
                });
                setUsers(prev => [...prev, res.data as any]);
                toast.current?.show({ severity: 'success', summary: 'Created', detail: 'User created successfully.', life: 3000 });
            } else {
                const res = await new UserxControllerApi().patchSpecificUser({
                    userId: editingUserId!,
                    userxPatchDTO: {
                        firstName: form.firstName,
                        lastName: form.lastName,
                        username: form.username,
                        isEnabled: form.enabled,
                        roles: form.roleIds,
                        roomId: form.roomId || null,
                    } as any,
                });
                setUsers(prev => prev.map(u => u.id === (res.data as any).id ? res.data as any : u));
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
        if (!user.id) return;
        setConfirmDelete({
            message: `Are you sure you want to delete user "${user.username}"? This action cannot be undone.`,
            onConfirm: () => {
                setConfirmDelete(null);
                new UserxControllerApi().deleteSpecificUser({ userId: user.id })
                    .then(() => {
                        setUsers(prev => prev.filter(u => u.id !== user.id));
                        toast.current?.show({ severity: 'success', summary: 'Deleted', detail: 'User deleted.', life: 3000 });
                    })
                    .catch(() => toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to delete user.', life: 3000 }));
            },
        });
    };

    return (
        <div className="dashboard-layout">
            <Toast ref={toast} />
            <PageHeader title="User Configuration" onMenuClick={() => setSidebarVisible(true)} />
            <SidebarComponent visible={sidebarVisible} onHide={() => setSidebarVisible(false)} />

            <div className="dashboard-content">

                {/* ── User List ── */}
                <div className="table-container">
                    <div className="flex-header">
                        <h3>User List</h3>
                        <Button label="Add User" icon="pi pi-user-plus" className="admin-add-button" onClick={openCreate} />
                    </div>

                    <div className="table-filter-row">
                    <span className="p-input-icon-left">
                        <i className="pi pi-search" />
                        <InputText value={lastNameSearch} onChange={e => setLastNameSearch(e.target.value)} placeholder="Search by last name" />
                    </span>

                        <Dropdown value={roleFilter} options={roleFilterOptions} onChange={e => setRoleFilter(e.value)} placeholder="Role Filter" showClear />
                        <Dropdown value={roomFilter} options={roomFilterOptions} onChange={e => setRoomFilter(e.value)} placeholder="Room Filter" showClear filter />
                    </div>

                    <UserListComponent users={filteredUsers} loading={loading} onEditUser={openEdit} onDeleteUser={handleDelete} showDelete />
                </div>

                {/* ── Role Management ── */}
                <RoleManagement roleDTOs={roleDTOs} onRoleDTOsChange={setRoleDTOs} />

            </div>

            <ConfirmDeleteDialog
                visible={confirmDelete !== null}
                message={confirmDelete?.message ?? ''}
                onConfirm={confirmDelete?.onConfirm ?? (() => {})}
                onHide={() => setConfirmDelete(null)}
            />

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
