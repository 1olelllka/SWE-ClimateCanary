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
    const { currentUser, isAdmin, isManager, isEmployee } = useUser();
    const navigate = useNavigate();
    const location = useLocation();
    const [isDarkMode, setIsDarkMode] = useState(false);

    // Navigiert zur Seite und schließt danach die Sidebar
    const handleNavigation = (path: string) => {
        navigate(path);
        onHide();
    };

    // Baut Menüpunkt auf und checkt, ob er gerade "aktiv" ist
    const renderMenuItem = (label: string, path: string) => (
        <div
            className={`menu-item ${location.pathname === path ? 'active' : ''}`}
            onClick={() => handleNavigation(path)}
        >
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
                        {renderMenuItem('Home (Overview)', '/')}
                        {renderMenuItem('Device Configuration', '/admin/devices')}
                        {renderMenuItem('User Configuration', '/admin/users')}
                        {renderMenuItem('Building Configuration', '/admin/building')}
                    </>
                )}

                {/* DEPARTMENT HEAD */}
                {isManager && !isAdmin && (
                    <>
                        <div className="menu-category">Department Head</div>
                        {renderMenuItem('Home (Deptartment Overview)', '/')}
                        {renderMenuItem('Absences', '/absences')}
                    </>
                )}

                {/* EMPLOYEE */}
                {isEmployee && !isManager && !isAdmin && (
                    <>
                        <div className="menu-category">Employee</div>
                        {renderMenuItem('Home (Dashboard)', '/')}
                        {renderMenuItem('My Department', '/department')}
                        {renderMenuItem('My Absences', '/absences')}
                    </>
                )}

                {/* SENIOR MANAGEMENT
                {isSeniorManagement && (
                    <>
                        <div className="menu-category">Senior Management</div>
                        {renderMenuItem('Home (Department Overview)', '/')}
                    </>
                )}
                */}

                {/* BUILDING MANAGEMENT
                {isBuildingManagement && (
                    <>
                        <div className="menu-category">Building Management</div>
                        {renderMenuItem('Home (Building Overview)', '/')}
                    </>
                )}
                */}

                {/* Settings für alle sichtbar */}
                <div className="mt-auto">
                    {renderMenuItem('Settings', '/settings')}
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
                    <div className="user-avatar"></div>
                    <div className="user-info">
                        <span className="user-name">{currentUser?.username || 'Benutzer'}</span>
                    </div>
                </div>
            </div>
        </Sidebar>
    );
};

export default SidebarComponent;