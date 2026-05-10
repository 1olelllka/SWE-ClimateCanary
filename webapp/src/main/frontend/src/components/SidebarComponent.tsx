import React from 'react';
import { Sidebar } from 'primereact/sidebar';
import { useNavigate, useLocation } from 'react-router-dom';
import { useUser } from '../Contexts/AuthenticatedUserContext';
import '../styles/Sidebar.css';
import { ROUTES } from '../utilities/routes.paths';
import ClockInOutButton from './ClockInOutButton';

interface SidebarProps {
    readonly visible: boolean;
    readonly onHide: () => void;
}

const SidebarComponent: React.FC<SidebarProps> = ({ visible, onHide }) => {
    const navigate = useNavigate();
    const location = useLocation();

    const {
        currentUser,
        isAdmin,
        isEmployee,
        isSeniorManager,
        isDepartmentManager,
        isBuildingManager
    } = useUser();

    const deptMatch = location.pathname.match(/^\/senior\/department\/(.+)$/);
    const currentDept = deptMatch ? decodeURIComponent(deptMatch[1]) : null;

    // Exakte Bezeichnungen für die Startseite je nach Rolle
    const getOverviewLabel = () => {
        if (isSeniorManager) return 'Departments Overview';
        if (isDepartmentManager) return 'Overview Department XY';
        if (isBuildingManager) return 'Building Overview';
        if (isEmployee) return 'My Office';
        return 'Overview'; // Für SysAdmin
    };

    // Konfiguration der Menüpunkte
    const allMenuItems = [
        {
            label: getOverviewLabel(),
            icon: 'pi-home',
            route: '/',
            visible: true // Jeder sieht eigene Startseite
        },
        {
            label: 'Departments List',
            icon: 'pi-list',
            route: '/',
            visible: isSeniorManager && currentDept !== null
        },
        {
            label: currentDept ? `${currentDept} Overview` : '',
            icon: 'pi-chart-bar',
            route: location.pathname,
            visible: isSeniorManager && currentDept !== null
        },
        {
            label: 'My Department',
            icon: 'pi-sitemap',
            route: ROUTES.EMPLOYEE_DEPARTMENT,
            visible: isEmployee
        },
        {
            label: 'My Absences',
            icon: 'pi-calendar-times',
            route: '/absences',
            visible: isEmployee || isSeniorManager || isDepartmentManager
        },
        {
            label: 'Absences',
            icon: 'pi-list',
            route: '/department-absences',
            visible: isDepartmentManager
        },
        // --- SYSADMIN MENÜPUNKTE ---
        {
            label: 'Device Configuration',
            icon: 'pi-desktop',
            route: '/device-configuration',
            visible: isAdmin
        },
        {
            label: 'User Configuration',
            icon: 'pi-user-edit',
            route: '/user-configuration',
            visible: isAdmin
        },
        {
            label: 'Building Configuration',
            icon: 'pi-building',
            route: '/building-configuration',
            visible: isAdmin
        },
        {
            label: 'Tip Management',
            icon: 'pi pi-lightbulb',
            route: '/tip-management',
            visible: isAdmin
        },
        {
            label: 'Settings',
            icon: 'pi-cog',
            route: ROUTES.SETTINGS,
            visible: true
        }
    ];

    // Filtert alle Menüpunkte raus, bei denen visible: false ist
    const allowedMenuItems = allMenuItems.filter(item => item.visible);

    const handleNavigation = (route: string) => {
        navigate(route);
        onHide();
    };

    return (
        <Sidebar visible={visible} onHide={onHide} className="custom-sidebar">

            <div className="sidebar-brand-header">
                <p className="brand-title-sidebar">ClimateCanary</p>
            </div>

            <div className="sidebar-menu">
                {allowedMenuItems.map((item, index) => (
                    <button key={index} className="menu-item" onClick={() => handleNavigation(item.route)}>
                        <i className={`pi ${item.icon}`} aria-hidden="true" style={{ marginRight: '15px' }}></i>
                        <span>{item.label}</span>
                    </button>
                ))}
            </div>

            <div className="sidebar-footer">
                {isEmployee && <ClockInOutButton />}
                <div className="user-profile">
                    <div className="user-avatar">
                        <i className="pi pi-user" style={{ fontSize: '1.5rem' }}></i>
                    </div>
                    <div className="user-info">
                        <span className="user-name">{currentUser?.username || 'User'}</span>
                        <span className="user-email">Logged in</span>
                    </div>
                </div>
            </div>

        </Sidebar>
    );
};

export default SidebarComponent;