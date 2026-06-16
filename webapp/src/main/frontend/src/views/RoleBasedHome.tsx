import React, { useState } from 'react';
import { Navigate } from 'react-router-dom';
import { useUser } from "../Contexts/AuthenticatedUserContext";
import HomePage from "./HomePage";
import SidebarComponent from '../components/SidebarComponent';
import { PageHeader } from "../components/PageHeader";
import { ROUTES } from "../utilities/routes.paths";

const PlaceholderDashboard: React.FC<{ readonly title: string, readonly content: string }> = ({ title, content }) => {
    const [sidebarVisible, setSidebarVisible] = useState(false);

    return (
        <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', backgroundColor: 'var(--page-bg, #f0f4f8)' }}>
            <PageHeader
                title="ClimateCanary"
                subtitle={title}
                lastUpdated="Just now"
                onMenuClick={() => setSidebarVisible(true)}
            />

            <SidebarComponent visible={sidebarVisible} onHide={() => setSidebarVisible(false)} />

            <div style={{ padding: '2rem', flexGrow: 1 }}>
                <h1>{title}</h1>
                <p>{content}</p>
            </div>
        </div>
    );
};

/**
 * Every role now has its own dedicated dashboard URL (see routes.paths.ts / routes.js).
 * A user can hold several roles at once, so instead of inline-rendering a single
 * dashboard picked by priority, '/' just redirects to the highest-priority dashboard
 * the user has access to. All of the user's other dashboards remain reachable directly
 * via their own URLs, which are listed in the sidebar (see SidebarComponent).
 */
const RoleBasedHome: React.FC = () => {
    const {
        currentUser,
        isAdmin,
        isEmployee,
        isSeniorManager,
        isDepartmentManager,
        isBuildingManager
    } = useUser();

    if (!currentUser) return <div style={{ padding: '50px' }}>Lädt Benutzerdaten...</div>;

    if (isAdmin) {
        return <Navigate to={ROUTES.SYSADMIN_DASHBOARD} replace />;
    }

    if (isSeniorManager) {
        return <Navigate to={ROUTES.SENIOR_MANAGER_DASHBOARD} replace />;
    }

    if (isDepartmentManager) {
        return <Navigate to={ROUTES.DEPARTMENT_MANAGER_DASHBOARD} replace />;
    }

    if (isBuildingManager) {
        return <Navigate to={ROUTES.BUILDING_MANAGER_DASHBOARD} replace />;
    }

    if (isEmployee) {
        return <Navigate to={ROUTES.EMPLOYEE_DASHBOARD} replace />;
    }

    // Fallback
    return <HomePage />;
};

export default RoleBasedHome;
