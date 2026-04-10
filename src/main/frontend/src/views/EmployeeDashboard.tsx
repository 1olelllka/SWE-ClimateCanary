import React, { useState } from 'react';
import { Cards } from '../components/Cards';
import '../styles/EmployeeDashboard.css';
import { FooterComponent } from "../components/FooterComponent";
import SidebarComponent from '../components/SidebarComponent';
import '../styles/TimeFilters.css';
import { WarningBanner } from '../components/WarningBanner';
import { DashboardCalendar } from '../components/Calendar';
import { PageHeader } from "../components/PageHeader";

export const EmployeeDashboard: React.FC = () => {
    const [timeFilter, setTimeFilter] = useState('Week');
    const [sidebarVisible, setSidebarVisible] = useState(false);
    const [dateRange, setDateRange] = useState<Date[] | null>(null);

    return (
        <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', backgroundColor: 'var(--page-bg)' }}>

            <PageHeader
                title="Office 001"
                subtitle="My Office"
                lastUpdated="12:34"
                onMenuClick={() => setSidebarVisible(true)}
            />

            <SidebarComponent visible={sidebarVisible} onHide={() => setSidebarVisible(false)} />

            <div className="employee-dashboard-container" style={{ flexGrow: 1 }}>
                <div className="card-grid">
                    <Cards
                        title="Temperature"
                        value="26.5"
                        unit="°C"
                        color="#e05252"
                        points="0,20 20,15 40,25 60,10 80,18 100,10"
                        trendIcon="pi-caret-up"
                        trendText="1.2° since yesterday"
                    />
                    <Cards
                        title="Humidity"
                        value="53"
                        unit="%"
                        color="#26a69a"
                        points="0,25 20,22 40,18 60,12 80,8 100,5"
                        trendIcon="pi-minus"
                        trendText="steady range"
                    />
                    <Cards
                        title="Air Quality (CO₂)"
                        value="1049"
                        unit="ppm"
                        color="#d4891a"
                        points="0,10 20,12 40,15 60,18 80,22 100,25"
                        trendIcon="pi-caret-up"
                        trendText="Rising — ventilate recommended"
                    />
                </div>

                <WarningBanner
                    boldPart="CO₂ levels are high. "
                    regularPart="Please ventilate the room to refresh the air."
                />

                <div className="chart-card">
                    <div className="chart-header">
                        <div className="time-filters">
                            {['Day', 'Week', 'Month'].map(f => (
                                <button
                                    key={f}
                                    className={`time-filter-btn ${timeFilter === f ? 'active' : ''}`}
                                    onClick={() => { setTimeFilter(f); setDateRange(null); }}
                                >
                                    {f}
                                </button>
                            ))}
                            <DashboardCalendar
                                dateRange={dateRange}
                                setDateRange={setDateRange}
                                isActive={timeFilter === 'Custom'}
                                onActivate={() => setTimeFilter('Custom')}
                            />
                        </div>
                    </div>

                    <div style={{ height: '250px', backgroundColor: '#f8fafc', borderRadius: '8px', border: '1px dashed #dde4ec', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#94a3b8' }}>
                        PrimeReact Line Chart Placeholder
                    </div>
                </div>
            </div>

            <FooterComponent />
        </div>
    );
};

export default EmployeeDashboard;