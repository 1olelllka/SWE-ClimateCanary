/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import React from "react";

import {Button} from "primereact/button";
import {Column} from "primereact/column";
import {DataTable} from "primereact/datatable";

import {Checkbox} from "primereact/checkbox";

interface UserRoleSummary {
    id: string;
    name: string;
}

interface UserRoomSummary {
    id: string;
    departmentName: string;
    roomNumber: string;
}

export interface UserListItem {
    id: string;
    username: string;
    firstName: string;
    lastName: string;
    enabled: boolean;
    roles: UserRoleSummary[];
    myRoom: UserRoomSummary | null;
}

interface UserListProps {
    readonly users: UserListItem[];
    readonly loading: boolean;
    readonly onEditUser: (user: UserListItem) => void;
    readonly onDeleteUser?: (user: UserListItem) => void;
    readonly showDelete?: boolean;
}

const getRoomLabel = (user: UserListItem) => {
    return (
        user.myRoom?.roomNumber ||
        "—"
    );
};

const rolesBodyTemplate = (user: UserListItem) => {
    return (
        <span>{user.roles.map(role => role.name).join(", ") || "—"}</span>
    );
};

/**
 * Component for displaying a list of users in a DataTable.
 * @param users the users to display
 * @param loading whether the users are loading
 * @param onEditUser callback when a user is edited
 */
const UserListComponent: React.FC<UserListProps> = ({
                                                        users,
                                                        loading,
                                                        onEditUser,
                                                        onDeleteUser,
                                                        showDelete = false
                                                    }) => {

    /**
     * Renders the edit button for a user.
     * @param rowData
     */
    const actionsBodyTemplate = (rowData: UserListItem) => {
        return (
            <div className="admin-table-actions">
                <Button
                    icon="pi pi-cog"
                    rounded
                    text
                    severity="secondary"
                    onClick={() => onEditUser(rowData)}
                    aria-label={`Edit ${rowData.username}`}
                    title="Edit user"
                />

                {showDelete && onDeleteUser && (
                    <Button
                        icon="pi pi-trash"
                        rounded
                        text
                        severity="danger"
                        onClick={() => onDeleteUser(rowData)}
                        aria-label={`Delete ${rowData.username}`}
                        title="Delete user"
                    />
                )}
            </div>
        );
    };

    /**
     * Renders the enable button for a user.
     * @param rowData
     */
    const enableButtonTemplate = (rowData: UserListItem) => {
        return (
            <Checkbox checked={rowData.enabled ?? false} disabled={true}
                      className="p-mr-2"/>
        )
    }

    const roomBodyTemplate = (rowData: UserListItem) => {
        return getRoomLabel(rowData);
    };

    const fullNameBodyTemplate = (rowData: UserListItem) => {
        return `${rowData.firstName} ${rowData.lastName}`;
    };

    return (
        // DataTable for displaying users
        <DataTable value={users} loading={loading} emptyMessage="No users found." className="user-list-compact-table" tableStyle={{ width: 'auto', minWidth: '0' }}>
            <Column header="User" body={fullNameBodyTemplate} sortable sortField="lastName" />
            <Column field="username" header="Username" sortable />
            <Column header="Room" body={roomBodyTemplate} sortable />
            <Column header="" className="admin-actions-column" headerClassName="admin-actions-column" exportable={false} body={actionsBodyTemplate} />
        </DataTable>
    )
};

export default UserListComponent;