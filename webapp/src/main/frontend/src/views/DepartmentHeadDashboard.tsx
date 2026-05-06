import React, { useState } from 'react';
import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import { RoomListTable, RoomData } from '../components/RoomListTable';
import { ThresholdViolationsTable } from '../components/ThresholdViolationsTable';
import { PendingRequestsTable } from '../components/PendingRequestsTable';
import { Cards } from '../components/Cards';
import { NumberOfViolationsTable } from "../components/NumberOfViolationsTable";

// DUMMY-DATEN FÜR DEPARTMENT HEAD
const mockDeptRooms: RoomData[] = [
    { id: '015', type: 'Bureau', people: '6/5', co2: '1049 ppm', temp: '26,5 °C', humidity: '53 %', status: 'red' },
    { id: '016', type: 'Common Area', people: '1/--', co2: '1020 ppm', temp: '25,5 °C', humidity: '50 %', status: 'red' },
    { id: '017', type: 'Bureau', people: '2/5', co2: 'n/a', temp: 'n/a', humidity: 'n/a', status: 'gray' },
    { id: '018', type: 'Bureau', people: '5/5', co2: '970 ppm', temp: '22,2 °C', humidity: '45 %', status: 'green' },
    { id: '020', type: 'Bureau', people: '7/5', co2: '950 ppm', temp: '21,9 °C', humidity: '48 %', status: 'yellow' },
];

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

                <RoomListTable rooms={mockDeptRooms} />

                <ThresholdViolationsTable />

                <PendingRequestsTable />

                <NumberOfViolationsTable />
            </div>
        </div>
    );
};