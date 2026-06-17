/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import {Navigate, Outlet, useLocation} from 'react-router-dom';
import {useEffect, useState} from 'react';
import {ProgressSpinner} from 'primereact/progressspinner';

import {useUser} from "../Contexts/AuthenticatedUserContext";
import {ROUTES} from "../utilities/routes.paths";

interface PrivateRouteProps {
    readonly requiredPermission?: string;
    readonly requiredPermissions?: string[];
}

/**
 * Private route component that checks if the user is authenticated and,
 * if configured, whether the user has the required permission.
 */

const PrivateRoute: React.FC<PrivateRouteProps> = ({ requiredPermission, requiredPermissions }) => {

    enum AuthStatus {
        AUTHENTICATED = 200,
        UNAUTHENTICATED = 401,
        UNKNOWN = 0
    }

    const { userIsAuthenticated, hasPermission } = useUser();
    const location = useLocation();

    const [authStatus, setAuthStatus] = useState(AuthStatus.UNKNOWN); // authentication status is initially unknown

    // eslint-disable-next-line react-hooks/exhaustive-deps
    useEffect(() => {
        const checkAuthentication = async () => {
            try {
                const isAuthenticated = await userIsAuthenticated();

                if (isAuthenticated) {
                    setAuthStatus(AuthStatus.AUTHENTICATED);
                } else {
                    setAuthStatus(AuthStatus.UNAUTHENTICATED);
                }
            } catch (err: any) {
                console.warn('Backend not available:', err);
                setAuthStatus(AuthStatus.UNAUTHENTICATED);
            }
        };
        void checkAuthentication(); // to mark the returned Promise as explicitly and intentionally unawaited (ESLint)
    }, []); // an empty dependency array signals that the effect is executed only once on mount

    // loading spinner
    if (authStatus === AuthStatus.UNKNOWN) {
        return <ProgressSpinner />;
    }

    if (authStatus !== AuthStatus.AUTHENTICATED) {
        return <Navigate to={ROUTES.LOGIN} replace state={{ from: location }} />;
    }

    const permissionsToCheck = requiredPermissions ?? (requiredPermission ? [requiredPermission] : []);

    if (permissionsToCheck.length > 0 && !permissionsToCheck.some(hasPermission)) {
        return <Navigate to={ROUTES.FORBIDDEN} replace />;
    }

    return <Outlet />;
};

export default PrivateRoute;
