import React, { useState } from 'react';
import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import { DepartmentAveragesTable } from '../components/DepartmentAveragesTable';
import { ViolationsBarChart } from '../components/ViolationsBarChart';
import { AirQualityTrendChart } from '../components/AirQualityTrendChart';

export const SeniorManagerDashboard: React.FC = () => {
    const [sidebarVisible, setSidebarVisible] = useState(false);

    return (
        <div className="dashboard-layout">
            <PageHeader
                title="Departments Overview"
                onMenuClick={() => setSidebarVisible(true)}
            />
            <SidebarComponent visible={sidebarVisible} onHide={() => setSidebarVisible(false)} />

            <div className="dashboard-content">
                <div className="cards-grid" style={{ gridTemplateColumns: 'repeat(2, 1fr)' }}>
                    <div className="kpi-card" style={{ backgroundColor: '#fff', border: '1px solid #ddd', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}>
                        <h4 style={{ color: '#f44336', fontWeight: 'bold' }}>Active Violations</h4>
                        <h2 style={{ color: '#f44336', fontSize: '2.5rem' }}>2</h2>
                    </div>
                    <div className="kpi-card" style={{ backgroundColor: '#fff', border: '1px solid #ddd', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}>
                        <h4 style={{ color: '#4caf50', fontWeight: 'bold' }}>Air Quality Score</h4>
                        <h2 style={{ color: '#4caf50', fontSize: '2.5rem', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px' }}>
                            Good <i className="pi pi-arrow-up-right" style={{ fontSize: '2rem' }}></i>
                        </h2>
                    </div>
                </div>

                <DepartmentAveragesTable />

                <ViolationsBarChart />

                <AirQualityTrendChart />

            </div>
        </div>
    );
};