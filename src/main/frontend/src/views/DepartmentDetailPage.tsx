import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import { FooterComponent } from '../components/FooterComponent';
import { Cards } from '../components/Cards';
import { DepartmentViolationsTable } from '../components/DepartmentViolationsTable';
import { DepartmentTrendChart } from '../components/DepartmentTrendChart';
import '../styles/EmployeeDashboard.css';

export const DepartmentDetailPage: React.FC = () => {
    const { departmentName } = useParams<{ departmentName: string }>();
    const navigate = useNavigate();
    const [sidebarVisible, setSidebarVisible] = useState(false);

    const dept = decodeURIComponent(departmentName ?? 'Department');

    return (
        <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', backgroundColor: 'var(--page-bg)' }}>
            <PageHeader
                title={`${dept} Overview`}
                onMenuClick={() => setSidebarVisible(true)}
            />
            <SidebarComponent visible={sidebarVisible} onHide={() => setSidebarVisible(false)} />

            <div className="employee-dashboard-container" style={{ flexGrow: 1 }}>
                <button
                    onClick={() => navigate(-1)}
                    style={{
                        background:    'none',
                        border:        'none',
                        cursor:        'pointer',
                        color:         '#64748b',
                        display:       'flex',
                        alignItems:    'center',
                        gap:           '0.4rem',
                        fontSize:      '0.85rem',
                        padding:       0,
                        marginBottom:  '1.25rem',
                    }}
                >
                    <i className="pi pi-arrow-left" /> Back to Overview
                </button>

                <div className="card-grid">
                    <Cards
                        title="Active Violations"
                        value="2"
                        unit=""
                        color="#f44336"
                        points="0,5 25,5 25,20 50,20 50,5 75,5 100,5"
                        trendIcon="pi-exclamation-triangle"
                        trendText="Requires attention"
                    />
                    <Cards
                        title="Trend This Week"
                        value="24.8"
                        unit="°C avg"
                        color="#e05252"
                        points="0,25 20,22 40,20 60,18 80,22 100,24"
                        trendIcon="pi-caret-up"
                        trendText="+1.2 °C vs last week"
                    />
                    <Cards
                        title="Trend This Month"
                        value="23.5"
                        unit="°C avg"
                        color="#26a69a"
                        points="0,20 20,18 40,22 60,24 80,23 100,21"
                        trendIcon="pi-minus"
                        trendText="Stable this month"
                    />
                </div>

                <DepartmentViolationsTable departmentName={dept} />

                <div style={{ marginTop: '1.5rem' }}>
                    <DepartmentTrendChart departmentName={dept} />
                </div>
            </div>

            <FooterComponent />
        </div>
    );
};

export default DepartmentDetailPage;
