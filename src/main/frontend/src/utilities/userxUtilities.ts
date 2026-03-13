/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import {UserxDTO, UserxRole} from "../generated-skeleton-api";

export type UserxValidationResult = {
    valid: boolean;
    message?: string;
    fieldErrors?: Partial<Record<keyof UserxDTO, string>>
};


/**
 * Create a UserxRole array from a string array of roles
 * @param roles
 *
 * @returns UserxRole[]
 * @throws Error if an invalid role is provided
 */
export const createUserxRoleArrayFromStrings = (roles: string[]): UserxRole[] => {
    return roles.map(role => {
        switch (role) {
            case UserxRole.ADMIN.valueOf():
                return UserxRole.ADMIN;
            case UserxRole.MANAGER.valueOf():
                return UserxRole.MANAGER;
            case UserxRole.EMPLOYEE.valueOf():
                return UserxRole.EMPLOYEE;
            default:
                throw new Error(`Invalid role: ${role}`);
        }
    });
}
