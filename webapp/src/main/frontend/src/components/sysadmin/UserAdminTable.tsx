import React from 'react';
import { Dropdown } from 'primereact/dropdown';
import UserListComponent from '../UserListComponent';
import AdminTableHeader from './AdminTableHeader';
import { FullUser, SelectOption } from './sysAdminTypes';

interface UserAdminTableProps {
    readonly users: FullUser[];
    readonly search: string;
    readonly onSearch: (value: string) => void;
    readonly roleFilter: string | null;
    readonly onRoleFilter: (value: string | null) => void;
    readonly roomFilter: string | null;
    readonly onRoomFilter: (value: string | null) => void;
    readonly roleFilterOptions: SelectOption[];
    readonly roomFilterOptions: SelectOption[];
    readonly onAddUser: () => void;
    readonly onEditUser: (user: FullUser) => void;
    readonly onDeleteUser: (user: FullUser) => void;
}

const UserAdminTable: React.FC<UserAdminTableProps> = ({
    users,
    search,
    onSearch,
    roleFilter,
    onRoleFilter,
    roomFilter,
    onRoomFilter,
    roleFilterOptions,
    roomFilterOptions,
    onAddUser,
    onEditUser,
    onDeleteUser,
}) => {
    const normalizedSearch = search.toLowerCase();
    const filteredUsers = users.filter(u =>
        !u.roles.some(r => r.name === 'RASPBERRY_PI') &&
        (!search || (u.lastName ?? '').toLowerCase().includes(normalizedSearch)) &&
        (!roleFilter || u.roles.some(r => r.name === roleFilter)) &&
        (!roomFilter || u.myRoom?.id === roomFilter)
    );

    return (
        <div className="table-container">
            <AdminTableHeader
                title="User List"
                search={search}
                onSearch={onSearch}
                searchPlaceholder="Search by last name"
                onAdd={onAddUser}
                addLabel="Add User"
                filterEl={
                    <>
                        <Dropdown
                            value={roleFilter}
                            options={roleFilterOptions}
                            onChange={e => onRoleFilter(e.value)}
                            placeholder="Role Filter"
                            showClear
                            style={{ borderRadius: '20px', minWidth: '160px' }}
                        />
                        <Dropdown
                            value={roomFilter}
                            options={roomFilterOptions}
                            onChange={e => onRoomFilter(e.value)}
                            placeholder="Room Filter"
                            showClear
                            filter
                            style={{ borderRadius: '20px', minWidth: '180px' }}
                        />
                    </>
                }
            />
            <UserListComponent
                users={filteredUsers}
                loading={false}
                onEditUser={onEditUser}
                onDeleteUser={onDeleteUser}
                showDelete
            />
        </div>
    );
};

export default UserAdminTable;
