import React, { useEffect, useState } from 'react';
import { DepartmentControllerApi, UserxControllerApi } from '../generated-skeleton-api';
import { Sidebar } from 'primereact/sidebar';
import { useNavigate, useLocation } from 'react-router-dom';
import { useUser } from '../Contexts/AuthenticatedUserContext';
import '../styles/Sidebar.css';
import { ROUTES } from '../utilities/routes.paths';
import ClockInOutButton from './ClockInOutButton';
import { InputSwitch } from 'primereact/inputswitch';
import { useTheme } from '../Contexts/ThemeContext';
import { useUserPreferences } from '../Contexts/UserPreferencesContext';

interface SidebarProps {
    readonly visible: boolean;
    readonly onHide: () => void;
}

const SidebarComponent: React.FC<SidebarProps> = ({ visible, onHide }) => {
    const navigate = useNavigate();
    const location = useLocation();
    const { isDarkMode, setTheme } = useTheme();
    const { savePrefs } = useUserPreferences();
    const [themeSaving, setThemeSaving] = useState(false);

    const {
        currentUser,
        isAdmin,
        isEmployee,
        isSeniorManager,
        isDepartmentManager,
        isBuildingManager
    } = useUser();

    const [deptManagerDeptName, setDeptManagerDeptName] = useState<string | null>(null);

    useEffect(() => {
        if (!isDepartmentManager) return;
        new UserxControllerApi().getAuthenticatedUser().then(res => {
            const deptId = (res.data as any)?.myRoom?.departmentID;
            if (!deptId) return;
            return new DepartmentControllerApi().getSpecificDepartment({ departmentId: deptId }).then(r => {
                const name = r.data?.name || (r.data as any)?.departmentName || null;
                setDeptManagerDeptName(name);
            });
        }).catch(() => {});
    }, [isDepartmentManager]);

    // Exakte Bezeichnungen für die Startseite je nach Rolle
    const getOverviewLabel = () => {
        if (isSeniorManager) return 'Company Overview';
        if (isDepartmentManager) return 'Department Overview';
        if (isBuildingManager) return 'Building Overview';
        if (isEmployee) return 'My Office';
        return 'Overview'; // Für SysAdmin
    };

    // Konfiguration der Menüpunkte
    const allMenuItems = [
        {
            label: getOverviewLabel(),
            subtitle: isDepartmentManager ? (deptManagerDeptName ?? undefined) : undefined,
            icon: 'pi-home',
            route: '/',
            visible: true // Jeder sieht eigene Startseite
        },
        {
            label: 'My Department',
            icon: 'pi-sitemap',
            route: ROUTES.EMPLOYEE_DEPARTMENT,
            visible: isEmployee
        },
        {
            label: 'Threshold Violations',
            icon: 'pi-exclamation-triangle',
            route: ROUTES.DEPARTMENT_VIOLATIONS,
            visible: isDepartmentManager
        },
        {
            label: 'My Room',
            icon: 'pi-th-large',
            route: ROUTES.MY_ROOM,
            visible: isDepartmentManager
        },
        {
            label: 'My Absences',
            icon: 'pi-calendar-times',
            route: '/absences',
            visible: isEmployee || isDepartmentManager
        },
        {
            label: 'Team Absences',
            icon: 'pi-users',
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
            label: 'Climate Trends',
            icon: 'pi-chart-line',
            route: ROUTES.COMPANY_TRENDS,
            visible: isSeniorManager
        },
        {
            label: 'Tip Management',
            icon: 'pi pi-lightbulb',
            route: ROUTES.TIPMANAGEMENT,
            visible: isBuildingManager
        },
    ];

    // Filtert alle Menüpunkte raus, bei denen visible: false ist
    const allowedMenuItems = allMenuItems.filter(item => item.visible);

    const handleNavigation = (route: string) => {
        navigate(route);
        onHide();
    };

    const handleThemeToggle = async () => {
        const nextDarkMode = !isDarkMode;
        setTheme(nextDarkMode ? 'dark' : 'light');
        setThemeSaving(true);

        try {
            await savePrefs({ darkMode: nextDarkMode });
        } catch {
            setTheme(isDarkMode ? 'dark' : 'light');
        } finally {
            setThemeSaving(false);
        }
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
                        <span className="menu-item-text">
                            <span>{item.label}</span>
                            {item.subtitle && <span className="menu-item-subtitle">{item.subtitle}</span>}
                        </span>
                    </button>
                ))}
            </div>

            <div className="sidebar-footer">
                <div className="theme-toggle-card">
                    <div className="theme-toggle-left">
                        <div className="theme-toggle-icon">
                            <i className={`pi ${isDarkMode ? 'pi-moon' : 'pi-sun'}`} aria-hidden="true"></i>
                        </div>

                        <div className="theme-toggle-text">
                            <span className="theme-toggle-title">
                                {isDarkMode ? 'Dark Mode' : 'Light Mode'}
                            </span>
                        </div>
                    </div>

                    <InputSwitch
                        checked={isDarkMode}
                        onChange={handleThemeToggle}
                        disabled={themeSaving}
                        aria-label="Toggle dark mode"
                    />
                </div>

                {(isEmployee || isDepartmentManager) && (
                    <div className="sidebar-clock-wrapper">
                        <ClockInOutButton />
                    </div>
                )}

                <div className="user-profile">
                    <div className="user-info">
                        <span className="user-name">{currentUser?.username || 'User'}</span>
                        <span className="user-email">Logged in</span>
                    </div>
                    <button
                        className="user-settings-btn"
                        onClick={() => handleNavigation(ROUTES.SETTINGS)}
                        aria-label="Settings"
                    >
                        <i className="pi pi-cog" aria-hidden="true" />
                    </button>
                </div>
            </div>

        </Sidebar>
    );
};

export default SidebarComponent;