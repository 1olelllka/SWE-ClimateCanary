import React, { useState } from 'react';
import { useUser } from "../Contexts/AuthenticatedUserContext";
import EmployeeDashboard from "./EmployeeDashboard";
import HomePage from "./HomePage";
import SidebarComponent from '../components/SidebarComponent';
import { PageHeader } from "../components/PageHeader";
import {DepartmentHeadDashboard} from "./DepartmentHeadDashboard";

const PlaceholderDashboard: React.FC<{ title: string, content: string }> = ({ title, content }) => {
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
        return <PlaceholderDashboard title="SysAdmin Dashboard" content="Device, User & Building Configuration" />;
    }

    if (isSeniorManager) {
        return <PlaceholderDashboard title="Departments Overview" content="Welcome to the Senior Management Dashboard" />;
    }

    if (isDepartmentManager) {
        return <DepartmentHeadDashboard />;
    }

    if (isBuildingManager) {
        return <PlaceholderDashboard title="Building Management" content="Welcome to the Building Management Dashboard" />;
    }

    if (isEmployee) {
        return <EmployeeDashboard />;
    }

    // Fallback
    return <HomePage />;
};

export default RoleBasedHome;