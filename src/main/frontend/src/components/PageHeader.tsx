import React from 'react';
import { LogoutButton } from './LogoutButton';
import '../styles/PageHeader.css';

interface PageHeaderProps {
    readonly title: string;
    readonly subtitle?: string;       // Optional
    readonly lastUpdated?: string;    // Optional
    readonly onMenuClick: () => void;
    readonly showLogout?: boolean;    // Optional
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
                    <button className="menu-icon-button" onClick={onMenuClick} aria-label="Open menu">
                        <i className="pi pi-bars menu-icon"></i>
                    </button>
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