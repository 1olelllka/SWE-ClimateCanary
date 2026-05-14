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
    const editButtonTemplate = (rowData: UserListItem) => {
        return (
            <Button
                label={"Edit"}
                onClick={() => onEditUser(rowData)}
                aria-label={`Edit ${rowData.username}`}
            />
        );
    };

    /**
     * Renders the delete button for a user.
     * @param rowData
     */
    const deleteButtonTemplate = (rowData: UserListItem) => {
        if (!showDelete || !onDeleteUser) {
            return null;
        }

        return (
            <Button
                label={"Delete"}
                onClick={() => onDeleteUser(rowData)}
                aria-label={`Delete ${rowData.username}`}
            />
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

    return (
        // DataTable for displaying users
        <DataTable value={users} loading={loading} emptyMessage="No users found.">
            <Column field="id" header="ID"></Column>
            <Column field="firstName" header="First Name" sortable></Column>
            <Column field="lastName" header="Last Name" sortable></Column>
            <Column header="Room" body={roomBodyTemplate} sortable />
            <Column field="roles" header="Roles" body={rolesBodyTemplate}></Column>
            <Column field="enabled" header="Enabled" body={enableButtonTemplate}></Column>
            {showDelete && <Column body={deleteButtonTemplate} exportable={false}
                                   style={{minWidth: '6rem'}}></Column>}
            <Column body={editButtonTemplate} exportable={false}
                    style={{minWidth: '6rem'}}></Column>
        </DataTable>
    )
};

export default UserListComponent;