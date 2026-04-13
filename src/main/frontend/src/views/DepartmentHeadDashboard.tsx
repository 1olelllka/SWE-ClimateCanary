import React, { useState } from 'react';
import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import { RoomListTable } from '../components/RoomListTable';
import { ThresholdViolationsTable } from '../components/ThresholdViolationsTable';
import { PendingRequestsTable } from '../components/PendingRequestsTable';
import { Cards } from '../components/Cards';
import {NumberOfViolationsTable} from "../components/NumberOfViolationsTable";

export const DepartmentHeadDashboard: React.FC = () => {
    const [sidebarVisible, setSidebarVisible] = useState(false);

    return (
        <div className="dashboard-layout">
            <PageHeader
                title="Overview Department Finance"
                onMenuClick={() => setSidebarVisible(true)}
            />
            <SidebarComponent visible={sidebarVisible} onHide={() => setSidebarVisible(false)} />

            <div className="dashboard-content">
                <div className="cards-grid">
                    {/* Placeholder für Cards.tsx */}
                    <div className="kpi-card"><h4>Problem Rooms</h4><h2>12</h2></div>
                    <div className="kpi-card"><h4>Active Alerts</h4><h2>2</h2></div>
                    <div className="kpi-card"><h4>Air Quality</h4><h2>Good</h2></div>
                    <div className="kpi-card"><h4>Average Temperature</h4><h2>25 °C</h2></div>
                </div>

                <RoomListTable />

                <ThresholdViolationsTable />

                <PendingRequestsTable />

                <NumberOfViolationsTable />
            </div>
        </div>
    );
};