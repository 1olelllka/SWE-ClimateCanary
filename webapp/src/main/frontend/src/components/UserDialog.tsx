/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import React from 'react';
import {Dialog} from 'primereact/dialog';
import {Button} from "primereact/button";

import UserForm from './UserForm';
import {InputMaskChangeEvent} from "primereact/inputmask";
import {CheckboxChangeEvent} from "primereact/checkbox";
import {UserxValidationResult} from "../utilities/userxUtilities";
import {Message} from "primereact/message";
import {UserxCreateDTO} from "../generated-skeleton-api";

interface UserDialogProps {
    readonly visible: boolean;
    readonly user: UserxCreateDTO | null;
    readonly isNewUser: boolean;
    readonly validation: UserxValidationResult;
    readonly onHide: () => void;
    readonly onSubmit: () => void;
    readonly onInputChange: (event: React.ChangeEvent<HTMLInputElement> | InputMaskChangeEvent) => void;
    readonly onRolesChange: (event: { value: string[] }) => void;
    readonly onUserEnabledChange: (event: CheckboxChangeEvent) => void;
}

/**
 * Dialog for creating or editing a user.
 * @param visible whether the dialog is visible
 * @param user the user to be edited
 * @param isNewUser whether the user is new
 * @param validation field validation information
 * @param onHide callback when the dialog is hidden
 * @param onSubmit callback when the user is submitted
 * @param onInputChange callback when the input changes
 * @param onRolesChange callback when the roles change
 * @param onUserEnabledChange callback when the user is enabled or disabled
 */
const UserDialog: React.FC<UserDialogProps> = ({
                                                   visible,
                                                   user,
                                                   isNewUser,
                                                   validation,
                                                   onHide,
                                                   onSubmit,
                                                   onInputChange,
                                                   onRolesChange,
                                                   onUserEnabledChange
                                               }) => {

    /**
     * Renders the footer of the dialog.
     */
    const renderFooter = () => (
        <div>
            <Button label="Cancel" icon="pi pi-times" onClick={onHide} className="p-button-text"/>
            <Button label={isNewUser ? "Create" : "Save"} icon="pi pi-check" onClick={onSubmit}
                    autoFocus/>
        </div>
    );

    return (
        <Dialog
            header={isNewUser ? "Create New User" : "Edit User"}
            visible={visible}
            style={{width: '50vw'}}
            onHide={onHide}
            footer={renderFooter}
        >
            {validation.message && (<Message severity="error" text={validation.message} className="mb-3"/>)}
            {user && (
                <UserForm
                    user={user}
                    isNewUser={isNewUser}
                    fieldErrors={validation.fieldErrors}
                    onInputChange={onInputChange}
                    onRolesChange={onRolesChange}
                    onUserEnabledChange={onUserEnabledChange}
                />
            )}
        </Dialog>
    );
};

export default UserDialog;
