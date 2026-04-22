import React, { useState, useEffect } from 'react';
import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import { Dropdown } from 'primereact/dropdown';
import {
    RaspberryControllerApi,
    SensorStationControllerApi,
    RoomControllerApi,
    BuildingControllerApi,
    DepartmentControllerApi,
    RaspberryDTO,
    SensorStationDTO,
    RoomDTO,
    BuildingListDTO,
    DepartmentListDTO,
} from '../generated-skeleton-api';
import globalAxios from 'axios';
import '../styles/Tables.css';

interface UserPreview { id?: string; username?: string; firstName?: string; lastName?: string; enabled?: boolean; }

const PAGEABLE = { page: 0, size: 100, sort: [] };
const PREVIEW_ROWS = 3;

const statusBadge = (status?: string) => {
    const online = status === 'ONLINE';
    return (
        <span style={{
            background: online ? '#4caf50' : '#9e9e9e',
            color: 'white',
            padding: '2px 10px',
            borderRadius: '12px',
            fontSize: '0.8rem',
            fontWeight: 500,
        }}>
            {status ?? 'N/A'}
        </span>
    );
};

const enabledBadge = (enabled?: boolean) => (
    <span style={{
        background: enabled ? '#4caf50' : '#9e9e9e',
        color: 'white',
        padding: '2px 10px',
        borderRadius: '12px',
        fontSize: '0.8rem',
        fontWeight: 500,
    }}>
        {enabled ? 'Active' : 'Inactive'}
    </span>
);

interface TableHeaderProps {
    readonly title: string;
    readonly search: string;
    readonly onSearch: (v: string) => void;
    readonly searchPlaceholder: string;
    readonly filterEl?: React.ReactNode;
}

const TableHeader: React.FC<TableHeaderProps> = ({ title, search, onSearch, searchPlaceholder, filterEl }) => (
    <div className="flex-header" style={{ marginBottom: '1rem', flexWrap: 'wrap', gap: '0.75rem' }}>
        <h3 style={{ margin: 0 }}>{title}</h3>
        <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center', flexWrap: 'wrap' }}>
            <span className="p-input-icon-left">
                <i className="pi pi-search" style={{ marginLeft: '0.7rem' }} />
                <InputText
                    value={search}
                    onChange={e => onSearch(e.target.value)}
                    placeholder={searchPlaceholder}
                    style={{ borderRadius: '20px', paddingLeft: '2.0rem' }}
                />
            </span>
            {filterEl}
        </div>
    </div>
);

const SysAdminDashboard: React.FC = () => {
    const [sidebarVisible, setSidebarVisible] = useState(false);

    // --- Data ---
    const [raspberries, setRaspberries] = useState<RaspberryDTO[]>([]);
    const [sensors, setSensors] = useState<SensorStationDTO[]>([]);
    const [users, setUsers] = useState<UserPreview[]>([]);
    const [rooms, setRooms] = useState<RoomDTO[]>([]);
    const [buildings, setBuildings] = useState<BuildingListDTO[]>([]);
    const [departments, setDepartments] = useState<DepartmentListDTO[]>([]);

    // --- Search ---
    const [raspberrySearch, setRaspberrySearch] = useState('');
    const [sensorSearch, setSensorSearch] = useState('');
    const [userSearch, setUserSearch] = useState('');
    const [roomSearch, setRoomSearch] = useState('');
    const [buildingSearch, setBuildingSearch] = useState('');
    const [departmentSearch, setDepartmentSearch] = useState('');

    // --- Filters ---
    const [raspberryStatusFilter, setRaspberryStatusFilter] = useState<string | null>(null);
    const [sensorStatusFilter, setSensorStatusFilter] = useState<string | null>(null);
    const [userStatusFilter, setUserStatusFilter] = useState<string | null>(null);
    const [roomTypeFilter, setRoomTypeFilter] = useState<string | null>(null);
    const [departmentBuildingFilter, setDepartmentBuildingFilter] = useState<string | null>(null);

    useEffect(() => {
        new RaspberryControllerApi()
            .getAllRaspberries({ pageable: PAGEABLE })
            .then(res => setRaspberries(res.data.content ?? []))
            .catch(() => {});

        new SensorStationControllerApi()
            .getAllSensorStations({ pageable: PAGEABLE })
            .then(res => setSensors(res.data.content ?? []))
            .catch(() => {});

        globalAxios.get<{ content: UserPreview[] }>('/api/users?size=100')
            .then(res => setUsers(res.data.content ?? []))
            .catch(() => {});

        new RoomControllerApi()
            .getPageOfRooms({ pageable: PAGEABLE })
            .then(res => setRooms(res.data.content ?? []))
            .catch(() => {});

        new BuildingControllerApi()
            .getPageOfBuildings({ pageable: PAGEABLE })
            .then(res => setBuildings(res.data.content ?? []))
            .catch(() => {});

        new DepartmentControllerApi()
            .getPageOfDepartments({ pageable: PAGEABLE })
            .then(res => setDepartments(res.data.content ?? []))
            .catch(() => {});
    }, []);

    // --- Filtered data (search + optional filter, capped at PREVIEW_ROWS) ---
    const filterList = <T extends Record<string, any>>(
        data: T[],
        nameKey: string,
        search: string,
        filterKey?: string,
        filterValue?: string | null
    ): T[] =>
        data
            .filter(item => {
                if (search && !(item[nameKey] ?? '').toString().toLowerCase().includes(search.toLowerCase())) return false;
                if (filterKey && filterValue != null && String(item[filterKey]) !== filterValue) return false;
                return true;
            })
            .slice(0, PREVIEW_ROWS);

    const filteredRaspberries = filterList(raspberries, 'name', raspberrySearch, 'status', raspberryStatusFilter);
    const filteredSensors = filterList(sensors, 'name', sensorSearch, 'status', sensorStatusFilter);
    const filteredUsers = filterList(users, 'username', userSearch, 'enabled', userStatusFilter);
    const filteredRooms = filterList(rooms, 'name', roomSearch, 'roomType', roomTypeFilter);
    const filteredBuildings = filterList(buildings, 'name', buildingSearch);
    const filteredDepartments = filterList(departments, 'name', departmentSearch, 'buildingName', departmentBuildingFilter);

    const buildingNameOptions = [...new Set(departments.map(d => d.buildingName).filter(Boolean))] as string[];

    // --- Action handlers ---
    const handleDelete = (label: string, id?: string, refresh?: () => void) => {
        if (!id) return;
        if (globalThis.confirm(`Delete ${label}?`)) {
            // TODO: call respective delete API and then refresh()
        }
    };

    const handleSettings = (label: string, id?: string) => {
        // TODO: navigate to detail page
        alert(`Settings for ${label} (id: ${id})`);
    };

    const actionsTemplate = (label: string) => (row: Record<string, any>) => (
        <div style={{ display: 'flex', gap: '0.25rem', justifyContent: 'flex-end' }}>
            <Button
                icon="pi pi-cog"
                rounded
                text
                severity="secondary"
                onClick={() => handleSettings(label, row.id)}
                title={`${label} details`}
            />
            <Button
                icon="pi pi-trash"
                rounded
                text
                severity="danger"
                onClick={() => handleDelete(label, row.id)}
                title={`Delete ${label}`}
            />
        </div>
    );

    // --- Dropdown options ---
    const statusOptions = [
        { label: 'Online', value: 'ONLINE' },
        { label: 'Offline', value: 'OFFLINE' },
    ];
    const userStatusOptions = [
        { label: 'Active', value: 'true' },
        { label: 'Inactive', value: 'false' },
    ];
    const roomTypeOptions = [
        { label: 'Office', value: 'OFFICE' },
        { label: 'Shared', value: 'SHARED' },
    ];

    return (
        <div className="dashboard-layout">
            <PageHeader
                title="SysAdmin Dashboard"
                onMenuClick={() => setSidebarVisible(true)}
            />
            <SidebarComponent visible={sidebarVisible} onHide={() => setSidebarVisible(false)} />

            <div className="dashboard-content">

                {/* ── Raspberry Pi List ── */}
                <div className="table-container">
                    <TableHeader
                        title="Raspberry Pi List"
                        search={raspberrySearch}
                        onSearch={setRaspberrySearch}
                        searchPlaceholder="Search by name"
                        filterEl={
                            <Dropdown
                                value={raspberryStatusFilter}
                                options={statusOptions}
                                onChange={e => setRaspberryStatusFilter(e.value)}
                                placeholder="Status Filter ▼"
                                showClear
                                style={{ borderRadius: '20px', minWidth: '160px' }}
                            />
                        }
                    />
                    <DataTable value={filteredRaspberries} stripedRows emptyMessage="No Raspberry Pis found." responsiveLayout="scroll">
                        <Column field="name" header="Name" sortable />
                        <Column field="ipAddress" header="IP Address" />
                        <Column field="port" header="Port" />
                        <Column field="roomNumber" header="Room" />
                        <Column header="Status" body={row => statusBadge(row.status)} />
                    </DataTable>
                </div>

                {/* ── Sensor Station List ── */}
                <div className="table-container">
                    <TableHeader
                        title="Sensor Station List"
                        search={sensorSearch}
                        onSearch={setSensorSearch}
                        searchPlaceholder="Search by name"
                        filterEl={
                            <Dropdown
                                value={sensorStatusFilter}
                                options={statusOptions}
                                onChange={e => setSensorStatusFilter(e.value)}
                                placeholder="Status Filter ▼"
                                showClear
                                style={{ borderRadius: '20px', minWidth: '160px' }}
                            />
                        }
                    />
                    <DataTable value={filteredSensors} stripedRows emptyMessage="No Sensor Stations found." responsiveLayout="scroll">
                        <Column field="name" header="Name" sortable />
                        <Column field="roomId" header="Room ID" />
                        <Column field="connectedToPiId" header="Connected Pi" />
                        <Column header="Status" body={row => statusBadge(row.status)} />
                    </DataTable>
                </div>

                {/* ── User List ── */}
                <div className="table-container">
                    <TableHeader
                        title="User List"
                        search={userSearch}
                        onSearch={setUserSearch}
                        searchPlaceholder="Search by username"
                        filterEl={
                            <Dropdown
                                value={userStatusFilter}
                                options={userStatusOptions}
                                onChange={e => setUserStatusFilter(e.value)}
                                placeholder="User Filter ▼"
                                showClear
                                style={{ borderRadius: '20px', minWidth: '160px' }}
                            />
                        }
                    />
                    <DataTable value={filteredUsers} stripedRows emptyMessage="No users found." responsiveLayout="scroll">
                        <Column field="username" header="Username" sortable />
                        <Column field="firstName" header="First Name" />
                        <Column field="lastName" header="Last Name" />
                        <Column header="Status" body={row => enabledBadge(row.enabled)} />
                    </DataTable>
                </div>

                {/* ── Departments ── */}
                <div className="table-container">
                    <TableHeader
                        title="Departments"
                        search={departmentSearch}
                        onSearch={setDepartmentSearch}
                        searchPlaceholder="Search by name"
                        filterEl={
                            <Dropdown
                                value={departmentBuildingFilter}
                                options={buildingNameOptions}
                                onChange={e => setDepartmentBuildingFilter(e.value)}
                                placeholder="Building Filter ▼"
                                showClear
                                style={{ borderRadius: '20px', minWidth: '180px' }}
                            />
                        }
                    />
                    <DataTable value={filteredDepartments} stripedRows emptyMessage="No departments found." responsiveLayout="scroll">
                        <Column field="name" header="Name" sortable />
                        <Column field="buildingName" header="Building" />
                        <Column
                            header=""
                            body={actionsTemplate('Department')}
                            style={{ width: '6rem' }}
                            exportable={false}
                        />
                    </DataTable>
                </div>

                {/* ── Rooms ── */}
                <div className="table-container">
                    <TableHeader
                        title="Rooms"
                        search={roomSearch}
                        onSearch={setRoomSearch}
                        searchPlaceholder="Search by name"
                        filterEl={
                            <Dropdown
                                value={roomTypeFilter}
                                options={roomTypeOptions}
                                onChange={e => setRoomTypeFilter(e.value)}
                                placeholder="Type Filter ▼"
                                showClear
                                style={{ borderRadius: '20px', minWidth: '160px' }}
                            />
                        }
                    />
                    <DataTable value={filteredRooms} stripedRows emptyMessage="No rooms found." responsiveLayout="scroll">
                        <Column field="name" header="Name" sortable />
                        <Column field="departmentName" header="Department" />
                        <Column field="roomType" header="Type" />
                        <Column
                            header=""
                            body={actionsTemplate('Room')}
                            style={{ width: '6rem' }}
                            exportable={false}
                        />
                    </DataTable>
                </div>

                {/* ── Buildings ── */}
                <div className="table-container">
                    <TableHeader
                        title="Buildings"
                        search={buildingSearch}
                        onSearch={setBuildingSearch}
                        searchPlaceholder="Search by name"
                    />
                    <DataTable value={filteredBuildings} stripedRows emptyMessage="No buildings found." responsiveLayout="scroll">
                        <Column field="name" header="Name" sortable />
                        <Column field="address" header="Address" />
                        <Column
                            header=""
                            body={actionsTemplate('Building')}
                            style={{ width: '6rem' }}
                            exportable={false}
                        />
                    </DataTable>
                </div>

            </div>
        </div>
    );
};

export default SysAdminDashboard;
