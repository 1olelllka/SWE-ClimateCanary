import React, { useState } from 'react';
import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import { InputText } from 'primereact/inputtext';
import { Dropdown } from 'primereact/dropdown';
import { Checkbox } from 'primereact/checkbox';
import { RoomListTable, RoomData } from '../components/RoomListTable';

// Dummy-Daten
const mockBuildingRooms: RoomData[] = [
    { id: '001', department: 'Sales', type: 'Bureau', people: '6/5', co2: '1049 ppm', temp: '26,5 °C', humidity: '53 %', status: 'red' },
    { id: '002', department: 'IT', type: 'Common Area', people: '1/--', co2: '1020 ppm', temp: '25,5 °C', humidity: '50 %', status: 'red' },
    { id: '003', department: 'HR', type: 'Bureau', people: '2/5', co2: 'n/a', temp: 'n/a', humidity: 'n/a', status: 'gray' },
    { id: '004', department: 'Sales', type: 'Bureau', people: '5/5', co2: '970 ppm', temp: '22,2 °C', humidity: '45 %', status: 'green' },
];

export const BuildingManagerDashboard: React.FC = () => {
    const [sidebarVisible, setSidebarVisible] = useState(false);

    const [globalFilter, setGlobalFilter] = useState<string>('');
    const [selectedDepartment, setSelectedDepartment] = useState<string | null>(null);
    const [showOnlyViolations, setShowOnlyViolations] = useState<boolean>(false);

    const departments = ['Sales', 'IT', 'HR'];

    // Filter-Logik
    const filteredRooms = mockBuildingRooms.filter(room => {
        if (showOnlyViolations && room.status !== 'red') return false;
        if (selectedDepartment && room.department !== selectedDepartment) return false;
        if (globalFilter && !room.id.toLowerCase().includes(globalFilter.toLowerCase())) return false;
        return true;
    });

    const handleSettingsClick = (roomId: string) => {
        alert(`Analysis & Settings für Room ${roomId} geöffnet!`);
    };

    return (
        <div className="dashboard-layout">
            <PageHeader
                title="Building Overview"
                onMenuClick={() => setSidebarVisible(true)}
            />

            <SidebarComponent
                visible={sidebarVisible}
                onHide={() => setSidebarVisible(false)}
            />

            <div className="dashboard-content">

                {/* Cards */}
                <div className="kpi-card" style={{ width: '250px', backgroundColor: '#fff', border: '1px solid #ddd', textAlign: 'center', marginBottom: '2rem', boxShadow: '0 2px 4px rgba(0,0,0,0.1)' }}>
                    <h4 style={{ color: '#f44336', margin: '0 0 10px 0', fontSize: '0.9rem', fontWeight: 'bold' }}>Active Violations</h4>
                    <h2 style={{ color: '#f44336', margin: 0, fontSize: '2.5rem', fontWeight: 'bold' }}>
                        {mockBuildingRooms.filter(r => r.status === 'red').length}
                    </h2>
                </div>

                <div className="table-container">

                    {/* TABLE CONTROLS */}
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: '1rem', flexWrap: 'wrap', gap: '1rem' }}>
                        <h2 style={{ margin: 0 }}>Room list</h2>

                        <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
                            <span className="p-input-icon-left">
                                <i className="pi pi-search" style={{ marginLeft: '0.7rem' }} />
                                <InputText
                                    value={globalFilter}
                                    onChange={(e) => setGlobalFilter(e.target.value)}
                                    placeholder="Search for a Room"
                                    style={{
                                        borderRadius: '20px',
                                        paddingLeft: '2.0rem'
                                    }}
                                />
                            </span>

                            <Dropdown
                                value={selectedDepartment}
                                options={departments}
                                onChange={(e) => setSelectedDepartment(e.value)}
                                placeholder="Department Filter ▼"
                                showClear
                                style={{ borderRadius: '20px', minWidth: '200px' }}
                            />
                        </div>
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', marginBottom: '1rem' }}>
                        <Checkbox inputId="violations" onChange={e => setShowOnlyViolations(e.checked || false)} checked={showOnlyViolations}></Checkbox>
                        <label htmlFor="violations" style={{ marginLeft: '0.5rem', fontSize: '0.9rem' }}>Show only rooms with active violations</label>
                    </div>

                    <RoomListTable
                        rooms={filteredRooms}
                        showDepartment={true}
                        showSettings={true}
                        onSettingsClick={handleSettingsClick}
                    />

                </div>
            </div>
        </div>
    );
};