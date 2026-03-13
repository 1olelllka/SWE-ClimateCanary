/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import React from "react";

import {Button} from "primereact/button";
import {Column} from "primereact/column";
import {DataTable} from "primereact/datatable";

import {Checkbox} from "primereact/checkbox";
import {rolesBodyTemplate} from "./rolesBodyTemplate";
import {UserxCreateDTO} from "../generated-skeleton-api";

interface UserListProps {
    users: UserxCreateDTO[];
    loading: boolean;
    onEditUser: (user: UserxCreateDTO) => void;
}


/**
 * Component for displaying a list of users in a DataTable.
 * @param users the users to display
 * @param loading whether the users are loading
 * @param onEditUser callback when a user is edited
 */
const UserListComponent: React.FC<UserListProps> = ({users, loading, onEditUser}) => {

    /**
     * Renders the edit button for a user.
     * @param rowData
     */
    const editButtonTemplate = (rowData: UserxCreateDTO) => {
        return (<Button
            label={"Details"}
            icon="pi pi-external-link"
            onClick={() => onEditUser(rowData)}
            aria-label={`Details for ${rowData.username}`}
        />);
    };


    /**
     * Renders the enable button for a user.
     * @param rowData
     */
    const enableButtonTemplate = (rowData: UserxCreateDTO) => {
        return (
            <Checkbox checked={rowData.enabled ?? false} disabled={true}
                      className="p-mr-2"/>
        )
    }


    return (
        // DataTable for displaying users
        <DataTable value={users} loading={loading}>
            <Column field="username" header="Username" sortable></Column>
            <Column field="firstName" header="First Name" sortable></Column>
            <Column field="lastName" header="Last Name" sortable></Column>
            <Column field="roles" header="Roles" body={rolesBodyTemplate}></Column>
            <Column field="enabled" header="Enabled" body={enableButtonTemplate}></Column>
            <Column body={editButtonTemplate} exportable={false}
                    style={{minWidth: '8rem'}}></Column>
        </DataTable>
    )
};

export default UserListComponent;
