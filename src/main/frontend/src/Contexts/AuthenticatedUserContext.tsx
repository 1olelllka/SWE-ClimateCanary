/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import React, {createContext, useContext, useEffect, useMemo, useState} from 'react';
import {BEARER_TOKEN_LOCAL_STORAGE_KEY} from "../config/config";
import {jwtDecode, JwtPayload} from "jwt-decode";
import {LoginRequestDTO, UserxControllerApi, UserxDTO} from '../generated-skeleton-api';
import {AuthApi} from "../utilities/authApi";

/**
 * A context allows us to access the current user from any component in the component tree.
 * This is useful for components that need to know the current user, but are not directly
 * connected to the component that manages the current user state.
 * For more information, please refer to the React documentation:
 * https://react.dev/learn/passing-data-deeply-with-context
 */


/**
 * The UserContextType defines the shape of the context object, which is used to provide
 * the current user state to the components in the component tree.
 */
interface UserContextType {
    currentUser: UserxDTO | null;
    login: (loginRequestDTO: LoginRequestDTO) => Promise<void>;
    logout: () => void;
    error: Error | null;
    isAdmin: boolean;
    isEmployee: boolean;
    isSeniorManager: boolean;
    isBuildingManager: boolean;
    isDepartmentManager: boolean;
    userIsAuthenticated: () => Promise<boolean>;
}

// Create a new context object
export const UserContext = createContext<UserContextType | null>(null);

type CustomJwtPayload = JwtPayload & { roles: string[], name: string, username: string }

/**
 * The UserProvider component is a wrapper component that provides the current user state
 * to all components in the component tree.
 * It also provides functions to update the current user state.
 *
 * @param children The child components of the UserProvider
 * @returns The UserContext.Provider component
 */
export function UserProvider({children}: { children: React.ReactNode }) {

    // States the UserProvider manages
    // Docs: https://react.dev/reference/react/useState
    const [error, setError] = useState<Error | null>(null);

    const [token, setToken] = useState<string | null>(() => {
        return localStorage.getItem(BEARER_TOKEN_LOCAL_STORAGE_KEY);
    });

    // keep tabs/windows in sync on login/logout
    useEffect(() => {
        const handler = (e: StorageEvent) => {
            if (e.key === BEARER_TOKEN_LOCAL_STORAGE_KEY) {
                setToken(e.newValue);
            }
        };

        window.addEventListener("storage", handler);
        return () => window.removeEventListener("storage", handler);
    }, []);

    /**
     * Login the user by setting the bearer token in the state and local storage.
     * @param loginDto the login data
     */
    const login = async (loginDto: LoginRequestDTO): Promise<void> => {
        const {bearerToken} = await AuthApi.login(loginDto);
        if (!bearerToken || bearerToken.length < 10) {
            setError(new Error('Missing or invalid bearer token in response!'));
            console.log('Error: missing or invalid bearer token in response');
            return;
        }

        localStorage.setItem(BEARER_TOKEN_LOCAL_STORAGE_KEY, bearerToken);
        setToken(bearerToken); // trigger re-render
        setError(null);
    };

    /**
     * Logout the current user by removing the bearer token from the state and local storage. Note
     * that this does not actually invalidate the token on the server side. It only removes the
     * token from the client side.
     */
    const logout = async () => {
        localStorage.removeItem(BEARER_TOKEN_LOCAL_STORAGE_KEY);
        setToken(null);
    };

    /**
     * Get the current user by decoding the bearer token stored in the local storage.
     */
    const currentUser = useMemo<UserxDTO | null>(() => {
        if (!token) {
            return null;
        }

        try {
            const decoded = jwtDecode<CustomJwtPayload>(token);
            const fullName = decoded.name ?? "";
            const [firstName = "", lastName = ""] = fullName.split(" ");
            const roles = decoded.roles ?? [];
            return {
                username: decoded.username ?? "",
                firstName,
                lastName,
                email: "",
                phone: "",
                enabled: true,
                roles: new Set(roles) as any,            };
        } catch {
            // invalid token -> treat as logged out
            return null;
        }
    }, [token]);

    const userIsAuthenticated = async (): Promise<boolean> => {
        if (!token) {
            return false;
        }

        try {
            const decodedUser = jwtDecode<CustomJwtPayload>(token);

            if (decodedUser.exp && Date.now() >= decodedUser.exp! * 1000) {
                console.info("JWT Token expired at " + decodedUser.exp! * 1000);
                void logout(); // ignore the returned promise; void explicit so ESLint doesn’t complain
                return false;
            }

            const userxControllerApi = new UserxControllerApi();
            const isAuthenticated = await userxControllerApi.isAuthenticated();

            if (isAuthenticated) {
                return true;
            } else {
                setError(new Error('Authentication failed'));
                void logout(); // ignore the returned promise; void explicit so ESLint doesn’t complain
                return false;
            }
        } catch (err: any) {
            setError(err instanceof Error ? err : new Error("Invalid Token"));
            void logout(); // ignore the returned promise; void explicit so ESLint doesn’t complain
            return false;
        }
    };

    const rolesArray = Array.from(currentUser?.roles || []).map(role => JSON.stringify(role).toUpperCase());

    // ==========================================
    // TODO: WORKAROUND FÜR BACKEND-BUG
    // Aktuell schickt das Backend leere depthead und senior Rollen-Arrays ([]).
    // Bis das im Backend repariert ist, lesen wir den Username aus.
    // ==========================================
    const uname = currentUser?.username?.toLowerCase() || "";

    const isAdmin = rolesArray.some(r => r.includes("SYSADMIN") || r.includes("ADMIN")) || uname.includes("admin") || uname.includes("sysadmin");
    const isEmployee = rolesArray.some(r => r.includes("EMPLOYEE")) || uname === "employee";
    const isSeniorManager = rolesArray.some(r => r.includes("SENIOR")) || uname.includes("senior");
    const isBuildingManager = rolesArray.some(r => r.includes("BUILDING")) || uname.includes("building");
    const isDepartmentManager = rolesArray.some(r => r.includes("DEPT") || r.includes("DEPARTMENT")) || uname.includes("depthead");
    // ==========================================
/*
    Saubere Code:
    const rolesArray = Array.from(currentUser?.roles || []).map(role => JSON.stringify(role).toUpperCase());

    const isAdmin = rolesArray.some(r => r.includes("SYSADMIN") || r.includes("ADMIN"));
    const isEmployee = rolesArray.some(r => r.includes("EMPLOYEE"));
    const isSeniorManager = rolesArray.some(r => r.includes("SENIOR"));
    const isBuildingManager = rolesArray.some(r => r.includes("BUILDING"));
    const isDepartmentManager = rolesArray.some(r => r.includes("DEPT") || r.includes("DEPARTMENT"));
*/

    return (
        <UserContext.Provider
            value={{
                currentUser,
                login,
                logout,
                error,
                isAdmin,
                isEmployee,
                isSeniorManager,
                isDepartmentManager,
                isBuildingManager,
                userIsAuthenticated
            }}
        >
            {children}
        </UserContext.Provider>
    );
}

/**
 * A custom hook that provides access to the current user state.
 * This hook can be used in any component that is a child of the UserProvider.
 *
 * @returns The current user state and functions to update the current user state
 */
export function useUser() {
    const context = useContext(UserContext);
    if (!context) {
        throw new Error('useUser must be used within a UserProvider');
    }
    return context;
}
