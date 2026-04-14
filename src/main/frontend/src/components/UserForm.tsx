/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import React from "react";
import {InputMask, InputMaskChangeEvent} from "primereact/inputmask";
import {InputText} from "primereact/inputtext";
import {Password} from "primereact/password";
import {MultiSelect} from "primereact/multiselect";
import {Checkbox, CheckboxChangeEvent} from "primereact/checkbox";
import {UserxCreateDTO, UserxRole} from "../generated-skeleton-api";


interface UserFormProps {
    readonly user: UserxCreateDTO,
    readonly isNewUser: boolean,
    readonly fieldErrors?: Partial<Record<keyof UserxCreateDTO, string>>,
    readonly onInputChange: (event: React.ChangeEvent<HTMLInputElement> | InputMaskChangeEvent) => void,
    readonly onRolesChange: (event: { value: string[] }) => void,
    readonly onUserEnabledChange: (event: CheckboxChangeEvent) => void
}

/**
 * Form for creating or editing a user.
 * @param user the user to be edited
 * @param isNewUser describes if the user is new, i.e., created
 * @param fieldErrors field validation
 * @param onInputChange callback when the input changes
 * @param onRolesChange callback when the roles change
 * @param onUserEnabledChange callback when the user is enabled or disabled
 */
const UserForm: React.FC<UserFormProps> =
    ({
         user,
         isNewUser,
         fieldErrors,
         onInputChange,
         onRolesChange,
         onUserEnabledChange
     }) => {
        const userRoles = Object.values(UserxRole).map(role => ({label: role, value: role}));

        return (
            <div>
                <h1>{user.firstName} {user.lastName}</h1>
                <h5><span className="p-text-secondary">{user.username}</span></h5>
                {/* create form */}
                <form>
                    <div className="card p-fluid flex flex-wrap gap-3">
                        <div className="flex-auto mb-3">
                            <label htmlFor="username" className="font-bold block">Username</label>
                            <InputText id="username" name="username" value={user.username}
                                       onChange={onInputChange} required={true}
                                       placeholder="Username"
                                       autoComplete="off"
                                       className={fieldErrors?.username ? 'p-invalid' : undefined}
                            />
                            {fieldErrors?.username && <small className="p-error">{fieldErrors.username}</small>}
                        </div>
                        <div className="flex-auto mb-3">
                            <label htmlFor="firstName" className="font-bold block">First
                                Name</label>
                            <InputText id="firstName" name="firstName" value={user.firstName}
                                       onChange={onInputChange}
                                       placeholder="First Name"
                                       autoComplete="off"
                                       className={fieldErrors?.firstName ? 'p-invalid' : undefined}
                            />
                            {fieldErrors?.firstName && <small className="p-error">{fieldErrors.firstName}</small>}
                        </div>
                        <div className="flex-auto mb-3">
                            <label htmlFor="lastName" className="font-bold block">Last Name</label>
                            <InputText id="lastName" name="lastName" value={user.lastName}
                                       onChange={onInputChange}
                                       placeholder="Last Name"
                                       autoComplete="off"
                                       className={fieldErrors?.lastName ? 'p-invalid' : undefined}
                            />
                            {fieldErrors?.lastName && <small className="p-error">{fieldErrors.lastName}</small>}
                        </div>
                        <div className="flex-auto mb-3">
                            <label htmlFor="email" className="font-bold block">E-Mail</label>
                            <InputText id="email" name="email" value={user.email ?? ''}
                                       onChange={onInputChange} placeholder="E-Mail" autoComplete="off"
                            />
                        </div>
                        {isNewUser && (
                            <div className="flex-auto mb-3">
                                <label htmlFor="password" className="font-bold block">Password</label>
                                <Password inputId="password" name="password" value={user.password}
                                          onChange={onInputChange}
                                          placeholder="Password"
                                          autoComplete="off"
                                          className={fieldErrors?.password ? 'p-invalid' : undefined}
                                />
                                {fieldErrors?.password && <small className="p-error">{fieldErrors.password}</small>}
                            </div>
                        )}
                        <div className="flex-auto mb-3">
                            <label htmlFor="roles" className="font-bold block">Roles</label>
                            <MultiSelect inputId="roles" name="roles" value={user.roles} onChange={onRolesChange}
                                         options={userRoles} optionLabel="label"
                                         placeholder="Select Roles"
                                         className="w-full md:w-20rem"
                                         invalid={!!fieldErrors?.roles}
                            />
                            {fieldErrors?.roles && <small className="p-error">{fieldErrors.roles}</small>}
                        </div>
                        <div className="flex-auto mb-3">
                            <label htmlFor="phone" className="font-bold block">Phone</label>
                            <InputMask id="phone" name="phone" mask="+99 999 9999999"
                                       onChange={onInputChange}
                                       placeholder="+43 123 1234567"
                                       autoComplete="off"
                                       value={user.phone ?? ''}>
                            </InputMask>
                        </div>
                        <div className="flex-auto mb-3">
                            <label htmlFor="enabled" className="font-bold block">Enabled</label>
                            <Checkbox inputId="enabled" name="enabled"
                                      style={{float: "right"}}
                                      onChange={onUserEnabledChange}
                                      checked={user.enabled ?? false}>
                            </Checkbox>
                        </div>
                    </div>
                </form>
            </div>
        )

    }

export default UserForm;
