import React from 'react';
import { LogoutButton } from './LogoutButton';
import '../styles/PageHeader.css';

interface PageHeaderProps {
    title: string;
    subtitle?: string;       // Optional
    lastUpdated?: string;    // Optional
    onMenuClick: () => void;
    showLogout?: boolean;    // Optional
}

export const PageHeader: React.FC<PageHeaderProps> = ({
                                                          title,
                                                          subtitle,
                                                          lastUpdated,
                                                          onMenuClick,
                                                          showLogout = true
                                                      }) => {
    return (
        <div className="page-header">
            <div className="page-header-top">
                <div className="header-column-left">
                    <i className="pi pi-bars menu-icon" onClick={onMenuClick}></i>
                </div>

                <div className="header-column-center header-titles">
                    <div className="page-title">{title}</div>
                    {subtitle && <div className="page-subtitle">{subtitle}</div>}
                </div>

                <div className="header-column-right header-actions">
                    {showLogout && <LogoutButton />}
                </div>
            </div>

            {lastUpdated && (
                <div className="header-bottom">Last updated at {lastUpdated}</div>
            )}
        </div>
    );
};