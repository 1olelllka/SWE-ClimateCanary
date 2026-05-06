/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import React, {useEffect, useRef, useState} from 'react';

import {Button} from "primereact/button";
import {Card} from 'primereact/card';
import {InputMaskChangeEvent} from "primereact/inputmask";
import {Toast} from 'primereact/toast';
import 'primeicons/primeicons.css';

import UserListComponent from "./UserListComponent";
import UserDialog from "./UserDialog";


import {createUserxRoleArrayFromStrings, UserxValidationResult} from '../utilities/userxUtilities';
import {CheckboxChangeEvent} from "primereact/checkbox";
import {AdminControllerApi, UserxCreateDTO, UserxDTO} from "../generated-skeleton-api";

/**
 * Component for managing users.
 */
const UserTable = () => {
    const [users, setUsers] = useState<UserxDTO[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [selectedUser, setSelectedUser] = useState<UserxDTO | UserxCreateDTO | null>(null);
    const [isNewUser, setIsNewUser] = useState<boolean>(false);
    const [dialogVisible, setDialogVisible] = useState<boolean>(false);
    const [validation, setValidation] = useState<UserxValidationResult>({valid: true});

    const toast = useRef<Toast | null>(null);

    /**
     * Fetch all users from the backend on mount once.
     */
    useEffect(() => {
        const fetchUsers = async () => {
            try {
                const adminControllerApi = new AdminControllerApi();
                const userxData = await adminControllerApi.getAllUsers({withCredentials: true}).then(response => response.data);
                setUsers(userxData);
            } catch (err: any) {
                console.error('Error fetching users:', err);
            } finally {
                setLoading(false); // Set loading to false regardless of success or failure
            }
        };
        void fetchUsers(); // ignore the returned promise; void explicit so ESLint doesn’t complain
    }, []); // empty dependency array means this effect will only run once on mount

    /**
     * Validate the user object.
     * @param user
     * @param opts
     */
    const validateUser = (user: UserxCreateDTO | null, opts: {
        requirePassword?: boolean
    } = {requirePassword: true}): UserxValidationResult => {

        if (!user) return {valid: false, message: 'No user selected'};

        const required: (keyof UserxCreateDTO)[] = ['firstName', 'lastName', 'username'];
        const {requirePassword = true} = opts; // password input on edit user not needed
        const fieldErrors: Partial<Record<keyof UserxCreateDTO, string>> = {};

        required.forEach((k) => {
            const v = (user[k] as unknown as string) ?? '';
            if (!v.trim()) fieldErrors[k] = 'Required';
        });

        // check for password required
        const pwd = (user.password as unknown as string) ?? '';
        if (requirePassword && !pwd.trim()) fieldErrors.password = 'Required';

        // at least one role required (see also UserxCreateDTO in backend
        if (!Array.isArray(user.roles) || user.roles.length === 0) {
            fieldErrors.roles = 'Required';
        }

        const valid = Object.keys(fieldErrors).length === 0;
        return valid
            ? {valid}
            : {valid, message: 'Please fill in all required fields', fieldErrors};
    }

    /**
     * Handle the submit event for the user dialog.
     */
    const handleSubmit = async () => {
        const validationResult = validateUser(selectedUser, {requirePassword: isNewUser});
        if (!validationResult.valid) {
            // Display an error eventMessage or handle the validation error
            setValidation(validationResult);
            console.error('Please fill in all required fields.');
            return;
        }

        setValidation({valid: true});

        if (isNewUser) {
            await createUser();
        } else {
            await updateUser();
        }
        hideDialog();
    };

    /**
     * Create a new user and update the state.
     */
    const createUser = async () => {
        if (!selectedUser) return;

        // assert type of selectedUser to UserxCreateDTO
        const userToCreate = selectedUser as UserxCreateDTO;

        if (userToCreate.password === undefined) {
            return;
        }

        try {
            const adminControllerAPI = new AdminControllerApi();
            const newUser: UserxCreateDTO = await adminControllerAPI.createUser({userxCreateDTO: userToCreate}).then(response => response.data);
            setUsers([...users, newUser]);
        } catch (err: any) {
            console.error('Error saving user:', err);
            toast.current?.show({severity: 'error', summary: 'Error', detail: 'Error saving user', life: 3000});
        }
    }

    /**
     * Update an existing user and update the state.
     */
    const updateUser = async () => {
        if (!selectedUser) return;

        // assert type of selectedUser to UserxDTO
        const userToUpdate = selectedUser as UserxDTO;

        if (userToUpdate.id === undefined) return;

        try {
            const adminControllerAPI = new AdminControllerApi();
            const updatedUser = await adminControllerAPI.updateUser({
                userxDTO: userToUpdate,
                id: userToUpdate.id
            }).then(response => response.data);
            setUsers(users.map((user: UserxDTO) => user.id === updatedUser.id ? updatedUser : user));
            hideDialog();
        } catch (err: any) {
            console.error('Error updating user:', err);
            toast.current?.show({severity: 'error', summary: 'Error', detail: 'Error updating user', life: 3000});
        }
    }

    /**
     * Open the edit dialog for a user.
     * @param user
     */
    const openEditDialog = (user: UserxDTO) => {
        setSelectedUser(user);
        setValidation({valid: true});
        setIsNewUser(false);
        showDialog()
    };

    /**
     * Open the dialog for creating a new user.
     */
    const openNewUserDialog = () => {
        const newUser: UserxCreateDTO = {
            username: '',
            firstName: '',
            lastName: '',
            email: '',
            phone: '',
            enabled: true,
            roles: [],
            password: ''
        };
        setSelectedUser(newUser);
        showDialog()
        setIsNewUser(true);
    }

    /**
     * Show the dialog.
     */
    const showDialog = () => {
        setValidation({valid: true});
        setDialogVisible(true);
    }

    /**
     * Hide the dialog.
     */
    const hideDialog = () => {
        setValidation({valid: true});
        setDialogVisible(false);
    };

    /**
     * Handle input changes for the user dialog.
     * @param event
     */
    const handleInputChange = (event: React.ChangeEvent<HTMLInputElement> | InputMaskChangeEvent) => {
        if (!selectedUser) return;

        const {name, value} = event.target;

        setSelectedUser({...selectedUser, [name]: value});
    }

    /**
     * Handle user enabled change for the user dialog.
     * @param event
     */
    const handleUserEnabledChange = (event: CheckboxChangeEvent) => {
        if (!selectedUser) return;

        const {name, checked} = event.target;

        setSelectedUser({...selectedUser, [name]: checked});
    }

    /**
     * Handle roles change for the user dialog.
     * @param event
     */
    const handleRolesChange = (event: { value: string[] }) => {
        if (!selectedUser) return;

        const roles = createUserxRoleArrayFromStrings(event.value);

        setSelectedUser({...selectedUser, roles: roles});
    }


    return (<Card title="User List" className="m-4">
            <Toast ref={toast}/>
            {/* Button that opens a new user dialog on click */}
            <Button label="Add User" icon="pi pi-plus" className="p-button-raised p-button-rounded"
                    style={{marginBottom: "10px"}} onClick={openNewUserDialog}/>
            <UserListComponent users={users} loading={loading} onEditUser={openEditDialog}/>

            {/* Dialog for creating or editing a user */}
            <UserDialog visible={dialogVisible} user={selectedUser} isNewUser={isNewUser} validation={validation}
                        onHide={hideDialog} onSubmit={handleSubmit}
                        onInputChange={handleInputChange} onRolesChange={handleRolesChange}
                        onUserEnabledChange={handleUserEnabledChange}/>
        </Card>
    );
};

export default UserTable;

