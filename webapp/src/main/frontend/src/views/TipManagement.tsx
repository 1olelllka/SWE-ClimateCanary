import React, { useState } from 'react';

import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import { TipManagementForm } from '../components/TipManagementForm';

import '../styles/TipManagement.css';

export const TipManagement: React.FC = () => {
    const [sidebarVisible, setSidebarVisible] = useState(false);

    return (
        <div className="tip-mgmt-page">
            <PageHeader
                title="Tip Management"
                onMenuClick={() => setSidebarVisible(true)}
            />
            <SidebarComponent
                visible={sidebarVisible}
                onHide={() => setSidebarVisible(false)}
            />
            <div className="tip-mgmt-content">
                <TipManagementForm />
            </div>
        </div>
    );
};

export default TipManagement;
