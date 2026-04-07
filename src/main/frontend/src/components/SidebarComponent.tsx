import React, { useState } from 'react';
import { Sidebar } from 'primereact/sidebar';
import { InputSwitch } from 'primereact/inputswitch';
import { useNavigate, useLocation } from 'react-router-dom';
import { useUser } from '../Contexts/AuthenticatedUserContext';
import '../styles/Sidebar.css';

interface SidebarComponentProps {
    visible: boolean;
    onHide: () => void;
}

const SidebarComponent: React.FC<SidebarComponentProps> = ({ visible, onHide }) => {
    const { currentUser, isAdmin, isEmployee, isSeniorManager, isBuildingManager, isDepartmentManager} = useUser();

    const navigate = useNavigate();
    const location = useLocation();
    const [isDarkMode, setIsDarkMode] = useState(false);

    const handleNavigation = (path: string) => {
        navigate(path);
        onHide();
    };

    const renderMenuItem = (label: string, path: string, icon: string) => (
        <div
            className={`menu-item ${location.pathname === path ? 'active' : ''}`}
            onClick={() => handleNavigation(path)}
        >
            <i className={`${icon} menu-icon`} style={{ marginRight: '12px', fontSize: '1.2rem' }}></i>
            {label}
        </div>
    );

    return (
        <Sidebar visible={visible} onHide={onHide} className="custom-sidebar">
            <div className="sidebar-brand-header">
                <h2 className="brand-title-sidebar">ClimateCanary</h2>
            </div>

            <div className="sidebar-menu">

                {/* SYSADMIN */}
                {isAdmin && (
                    <>
                        <div className="menu-category">Sysadmin</div>
                        {renderMenuItem('Home (Overview)', '/', 'pi pi-home')}
                        {renderMenuItem('Device Configuration', '/admin/devices', 'pi pi-server')}
                        {renderMenuItem('User Configuration', '/admin/users', 'pi pi-users')}
                        {renderMenuItem('Building Configuration', '/admin/building', 'pi pi-building')}
                    </>
                )}

                {/* DEPARTMENT HEAD */}
                {isDepartmentManager && !isAdmin && (
                    <>
                        <div className="menu-category">Department Head</div>
                        {renderMenuItem('Home (Deptartment Overview)', '/', 'pi pi-home')}
                        {renderMenuItem('Absences', '/absences', 'pi pi-calendar-times')}
                    </>
                )}

                {/* EMPLOYEE */}
                {isEmployee && !isDepartmentManager && !isAdmin && (
                    <>
                        <div className="menu-category">Employee</div>
                        {renderMenuItem('Home (Dashboard)', '/', 'pi pi-home')}
                        {renderMenuItem('My Department', '/department', 'pi pi-sitemap')}
                        {renderMenuItem('My Absences', '/absences', 'pi pi-calendar')}
                    </>
                )}

                {/* SENIOR MANAGEMENT */}
                {isSeniorManager && (
                    <>
                        <div className="menu-category">Senior Management</div>
                        {renderMenuItem('Home (Department Overview)', '/', 'pi pi-chart-line')}
                    </>
                )}

                {/* BUILDING MANAGEMENT */}
                {isBuildingManager && (
                    <>
                        <div className="menu-category">Building Management</div>
                        {renderMenuItem('Home (Building Overview)', '/', 'pi pi-map')}
                    </>
                )}

                {/* Settings für alle sichtbar */}
                <div className="mt-auto" style={{ marginTop: '2rem' }}>
                    {renderMenuItem('Settings', '/settings', 'pi pi-cog')}
                </div>
            </div>

            {/* Footer */}
            <div className="sidebar-footer">
                <div className="theme-toggle-container">
                    <span>Light</span>
                    <InputSwitch
                        checked={isDarkMode}
                        onChange={(e) => setIsDarkMode(e.value ?? false)}
                    />
                    <span>Dark</span>
                </div>

                <div className="user-profile">
                    <div className="user-avatar">
                        <i className="pi pi-user" style={{ fontSize: '1.5rem' }}></i>
                    </div>
                    <div className="user-info">
                        <span className="user-name">{currentUser?.username || 'Benutzer'}</span>
                    </div>
                </div>
            </div>
        </Sidebar>
    );
};

export default SidebarComponent;