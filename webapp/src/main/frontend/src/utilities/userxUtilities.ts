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
            case UserxRole.SYSADMIN:
                return UserxRole.SYSADMIN;
            case UserxRole.HIGHER_MANAGER:
                return UserxRole.HIGHER_MANAGER;
            case UserxRole.DEPARTMENT_MANAGER:
                return UserxRole.DEPARTMENT_MANAGER;
            case UserxRole.BUILDING_MANAGER:
                return UserxRole.BUILDING_MANAGER;
            case UserxRole.EMPLOYEE:
                return UserxRole.EMPLOYEE;
            default:
                throw new Error(`Invalid role: ${role}`);
        }
    });
}
