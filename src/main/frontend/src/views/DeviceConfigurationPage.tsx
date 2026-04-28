import React, { useState, useEffect, useRef } from 'react';
import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import { InputNumber } from 'primereact/inputnumber';
import { Dropdown } from 'primereact/dropdown';
import { Dialog } from 'primereact/dialog';
import { MultiSelect } from 'primereact/multiselect';
import { Toast } from 'primereact/toast';
import {
    RaspberryControllerApi,
    SensorStationControllerApi,
    RoomControllerApi,
    RaspberryDTO,
    SensorStationDTO,
    RoomDTO,
} from '../generated-skeleton-api';
import '../styles/Tables.css';

const PAGEABLE = { page: 0, size: 100, sort: [] };

type ActiveTab = 'raspberry' | 'sensor';

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

const DeviceConfigurationPage: React.FC = () => {
    const toast = useRef<Toast>(null);
    const [sidebarVisible, setSidebarVisible] = useState(false);
    const [activeTab, setActiveTab] = useState<ActiveTab>('raspberry');

    const [raspberries, setRaspberries] = useState<RaspberryDTO[]>([]);
    const [sensors, setSensors] = useState<SensorStationDTO[]>([]);
    const [rooms, setRooms] = useState<RoomDTO[]>([]);
    const [loading, setLoading] = useState(false);

    // Search & filter
    const [roomSearch, setRoomSearch] = useState('');
    const [statusFilter, setStatusFilter] = useState<string | null>(null);
    const [idFilter, setIdFilter] = useState('');

    // Create dialog
    const [showCreateDialog, setShowCreateDialog] = useState(false);
    const [createLoading, setCreateLoading] = useState(false);

    // Pi form
    const [piName, setPiName] = useState('');
    const [piRoomId, setPiRoomId] = useState('');
    const [piIpAddress, setPiIpAddress] = useState('');
    const [piPort, setPiPort] = useState<number | null>(8080);
    const [piInterval, setPiInterval] = useState<number | null>(null);
    const [piAssignedSensors, setPiAssignedSensors] = useState<string[]>([]);

    // Sensor form
    const [sensorName, setSensorName] = useState('');
    const [sensorRoomId, setSensorRoomId] = useState('');
    const [sensorPiId, setSensorPiId] = useState('');

    const fetchData = () => {
        setLoading(true);
        Promise.all([
            new RaspberryControllerApi().getAllRaspberries({ pageable: PAGEABLE }),
            new SensorStationControllerApi().getAllSensorStations({ pageable: PAGEABLE }),
            new RoomControllerApi().getPageOfRooms({ pageable: PAGEABLE }),
        ]).then(([piRes, sensorRes, roomRes]) => {
            setRaspberries(piRes.data.content ?? []);
            setSensors(sensorRes.data.content ?? []);
            setRooms(roomRes.data.content ?? []);
        }).catch(() => {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to load devices', life: 3000 });
        }).finally(() => setLoading(false));
    };

    useEffect(() => { fetchData(); }, []);

    const statusOptions = [
        { label: 'Online', value: 'ONLINE' },
        { label: 'Offline', value: 'OFFLINE' },
    ];

    const roomOptions = rooms.map(r => ({ label: r.name ?? r.id ?? '', value: r.id ?? '' }));
    const piOptions = raspberries.map(p => ({ label: p.name ?? p.id ?? '', value: p.id ?? '' }));
    const sensorOptions = sensors.map(s => ({ label: s.name ?? s.id ?? '', value: s.id ?? '' }));

    const getRoomName = (roomId?: string) =>
        rooms.find(r => r.id === roomId)?.name ?? roomId ?? 'N/A';

    const getAssignedSensors = (piId?: string) => {
        if (!piId) return [];
        return sensors.filter(s => s.connectedToPiId === piId);
    };

    const applyFilters = <T extends Record<string, any>>(data: T[]): T[] =>
        data.filter(item => {
            if (roomSearch) {
                const roomName = getRoomName(item.roomId).toLowerCase();
                if (!roomName.includes(roomSearch.toLowerCase())) return false;
            }
            if (statusFilter && item.status !== statusFilter) return false;
            if (idFilter && !(item.id ?? '').toLowerCase().includes(idFilter.toLowerCase())) return false;
            return true;
        });

    const filteredRaspberries = applyFilters(raspberries);
    const filteredSensors = applyFilters(sensors);

    const handleDelete = (label: string, id?: string) => {
        if (!id || !globalThis.confirm(`Delete ${label}?`)) return;
        // TODO: call delete API
        toast.current?.show({ severity: 'info', summary: 'Deleted', detail: `${label} deleted (TODO: API call)`, life: 3000 });
    };

    const handleSettings = (label: string, id?: string) => {
        // TODO: navigate to detail/edit page
        toast.current?.show({ severity: 'info', summary: 'Settings', detail: `Open settings for ${label} (id: ${id})`, life: 3000 });
    };

    const actionsTemplate = (label: string) => (row: Record<string, any>) => (
        <div style={{ display: 'flex', gap: '0.25rem', justifyContent: 'flex-end' }}>
            <Button
                icon="pi pi-cog"
                rounded text severity="secondary"
                onClick={() => handleSettings(label, row.id)}
                title={`${label} settings / details`}
            />
            <Button
                icon="pi pi-trash"
                rounded text severity="danger"
                onClick={() => handleDelete(label, row.id)}
                title={`Delete ${label}`}
            />
        </div>
    );

    const openCreate = (tab?: ActiveTab) => {
        if (tab) setActiveTab(tab);
        resetForms();
        setShowCreateDialog(true);
    };

    const resetForms = () => {
        setPiName(''); setPiRoomId(''); setPiIpAddress(''); setPiPort(8080);
        setPiInterval(null); setPiAssignedSensors([]);
        setSensorName(''); setSensorRoomId(''); setSensorPiId('');
    };

    const handleCreate = async () => {
        setCreateLoading(true);
        try {
            if (activeTab === 'raspberry') {
                if (!piName || !piRoomId || !piIpAddress || piPort == null) {
                    toast.current?.show({ severity: 'warn', summary: 'Validation', detail: 'Name, IP address, port, and room are required.', life: 3000 });
                    setCreateLoading(false);
                    return;
                }
                await new RaspberryControllerApi().createNewRaspberry({
                    raspberryCreateDTO: { name: piName, ipAddress: piIpAddress, port: piPort, roomId: piRoomId },
                });
                toast.current?.show({ severity: 'success', summary: 'Created', detail: 'Raspberry Pi created successfully.', life: 3000 });
            } else {
                if (!sensorName || !sensorRoomId) {
                    toast.current?.show({ severity: 'warn', summary: 'Validation', detail: 'Name and room are required.', life: 3000 });
                    setCreateLoading(false);
                    return;
                }
                await new SensorStationControllerApi().createNewSensorStation({
                    sensorStationCreateDTO: { name: sensorName, roomId: sensorRoomId },
                });
                toast.current?.show({ severity: 'success', summary: 'Created', detail: 'Sensor Station created successfully.', life: 3000 });
            }
            setShowCreateDialog(false);
            fetchData();
        } catch {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to create device.', life: 3000 });
        } finally {
            setCreateLoading(false);
        }
    };

    const dialogFooter = (
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
            <Button label="Cancel" severity="secondary" outlined onClick={() => setShowCreateDialog(false)} />
            <Button label="Create" icon="pi pi-check" loading={createLoading} onClick={handleCreate} />
        </div>
    );

    const TableSectionHeader: React.FC<{ title: string; tab: ActiveTab }> = ({ title, tab }) => (
        <div className="flex-header" style={{ marginBottom: '1rem', flexWrap: 'wrap', gap: '0.75rem' }}>
            <h3 style={{ margin: 0 }}>{title}</h3>
            <Button
                label="Create Device"
                icon="pi pi-plus"
                size="small"
                onClick={() => openCreate(tab)}
            />
        </div>
    );

    return (
        <div className="dashboard-layout">
            <Toast ref={toast} />
            <PageHeader title="Device Configuration" onMenuClick={() => setSidebarVisible(true)} />
            <SidebarComponent visible={sidebarVisible} onHide={() => setSidebarVisible(false)} />

            <div className="dashboard-content">

                {/* ── Top action bar ── */}
                <div style={{ marginBottom: '1.5rem' }}>
                    <Button
                        label="Create Device"
                        icon="pi pi-plus"
                        onClick={() => openCreate()}
                    />
                </div>

                {/* ── Tab switcher ── */}
                <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.5rem' }}>
                    <Button
                        label="Raspberry Pi List"
                        severity={activeTab === 'raspberry' ? undefined : 'secondary'}
                        outlined={activeTab !== 'raspberry'}
                        onClick={() => setActiveTab('raspberry')}
                        style={{ borderRadius: '20px' }}
                    />
                    <Button
                        label="Sensor Station List"
                        severity={activeTab === 'sensor' ? undefined : 'secondary'}
                        outlined={activeTab !== 'sensor'}
                        onClick={() => setActiveTab('sensor')}
                        style={{ borderRadius: '20px' }}
                    />
                </div>

                {/* ── Shared search & filter bar ── */}
                <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '1.5rem', flexWrap: 'wrap', alignItems: 'center' }}>
                    <span className="p-input-icon-left">
                        <i className="pi pi-search" style={{ marginLeft: '0.7rem' }} />
                        <InputText
                            value={roomSearch}
                            onChange={e => setRoomSearch(e.target.value)}
                            placeholder="Search by room"
                            style={{ borderRadius: '20px', paddingLeft: '2.0rem' }}
                        />
                    </span>
                    <span className="p-input-icon-left">
                        <i className="pi pi-filter" style={{ marginLeft: '0.7rem' }} />
                        <InputText
                            value={idFilter}
                            onChange={e => setIdFilter(e.target.value)}
                            placeholder="Filter by ID"
                            style={{ borderRadius: '20px', paddingLeft: '2.0rem' }}
                        />
                    </span>
                    <Dropdown
                        value={statusFilter}
                        options={statusOptions}
                        onChange={e => setStatusFilter(e.value)}
                        placeholder="Status Filter ▼"
                        showClear
                        style={{ borderRadius: '20px', minWidth: '160px' }}
                    />
                </div>

                {/* ── Raspberry Pi table ── */}
                {activeTab === 'raspberry' && (
                    <div className="table-container">
                        <TableSectionHeader title="Raspberry Pi List" tab="raspberry" />
                        <DataTable
                            value={filteredRaspberries}
                            loading={loading}
                            stripedRows
                            emptyMessage="No Raspberry Pis found."
                            responsiveLayout="scroll"
                        >
                            <Column field="id" header="ID" style={{ maxWidth: '12rem', overflow: 'hidden', textOverflow: 'ellipsis' }} />
                            <Column header="Room" body={row => getRoomName(row.roomId)} />
                            <Column
                                header="Assigned Sensor Stations"
                                body={row => {
                                    const assigned = getAssignedSensors(row.id);
                                    if (assigned.length === 0) return <span style={{ color: '#9e9e9e' }}>None</span>;
                                    return (
                                        <span title={assigned.map(s => s.name).join(', ')}>
                                            {assigned.length} station{assigned.length !== 1 ? 's' : ''}
                                        </span>
                                    );
                                }}
                            />
                            <Column header="Status" body={row => statusBadge(row.status)} />
                            <Column
                                header=""
                                body={actionsTemplate('Raspberry Pi')}
                                style={{ width: '6rem' }}
                                exportable={false}
                            />
                        </DataTable>
                    </div>
                )}

                {/* ── Sensor Station table ── */}
                {activeTab === 'sensor' && (
                    <div className="table-container">
                        <TableSectionHeader title="Sensor Station List" tab="sensor" />
                        <DataTable
                            value={filteredSensors}
                            loading={loading}
                            stripedRows
                            emptyMessage="No Sensor Stations found."
                            responsiveLayout="scroll"
                        >
                            <Column field="id" header="ID" style={{ maxWidth: '12rem', overflow: 'hidden', textOverflow: 'ellipsis' }} />
                            <Column header="Room" body={row => getRoomName(row.roomId)} />
                            <Column
                                header="Assigned To"
                                body={row => {
                                    const pi = raspberries.find(p => p.id === row.connectedToPiId);
                                    return pi ? (pi.name ?? pi.id) : <span style={{ color: '#9e9e9e' }}>None</span>;
                                }}
                            />
                            <Column header="Status" body={row => statusBadge(row.status)} />
                            <Column
                                header="Last Measurement"
                                body={() => <span style={{ color: '#9e9e9e' }}>N/A</span>}
                            />
                            <Column
                                header=""
                                body={actionsTemplate('Sensor Station')}
                                style={{ width: '6rem' }}
                                exportable={false}
                            />
                        </DataTable>
                    </div>
                )}
            </div>

            {/* ── Create Device Dialog ── */}
            <Dialog
                header={activeTab === 'raspberry' ? 'Create Raspberry Pi' : 'Create Sensor Station'}
                visible={showCreateDialog}
                style={{ width: '480px' }}
                onHide={() => setShowCreateDialog(false)}
                footer={dialogFooter}
                draggable={false}
            >
                {/* ── Tab switcher inside dialog ── */}
                <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.5rem' }}>
                    <Button
                        label="Raspberry Pi"
                        size="small"
                        severity={activeTab === 'raspberry' ? undefined : 'secondary'}
                        outlined={activeTab !== 'raspberry'}
                        onClick={() => { setActiveTab('raspberry'); resetForms(); }}
                        style={{ borderRadius: '20px' }}
                    />
                    <Button
                        label="Sensor Station"
                        size="small"
                        severity={activeTab === 'sensor' ? undefined : 'secondary'}
                        outlined={activeTab !== 'sensor'}
                        onClick={() => { setActiveTab('sensor'); resetForms(); }}
                        style={{ borderRadius: '20px' }}
                    />
                </div>

                {activeTab === 'raspberry' ? (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                        <div>
                            <label htmlFor="pi-name" style={labelStyle}>Name *</label>
                            <InputText
                                id="pi-name"
                                value={piName}
                                onChange={e => setPiName(e.target.value)}
                                placeholder="e.g. Pi-Lab-01"
                                style={{ width: '100%' }}
                            />
                        </div>
                        <div>
                            <label htmlFor="pi-room" style={labelStyle}>Room *</label>
                            <Dropdown
                                inputId="pi-room"
                                value={piRoomId}
                                options={roomOptions}
                                onChange={e => setPiRoomId(e.value)}
                                placeholder="Select room"
                                style={{ width: '100%' }}
                                filter
                            />
                        </div>
                        <div>
                            <label htmlFor="pi-ip" style={labelStyle}>IP Address *</label>
                            <InputText
                                id="pi-ip"
                                value={piIpAddress}
                                onChange={e => setPiIpAddress(e.target.value)}
                                placeholder="e.g. 192.168.1.10"
                                style={{ width: '100%' }}
                            />
                        </div>
                        <div>
                            <label htmlFor="pi-port" style={labelStyle}>Port *</label>
                            <InputNumber
                                inputId="pi-port"
                                value={piPort}
                                onValueChange={e => setPiPort(e.value ?? null)}
                                placeholder="e.g. 8080"
                                style={{ width: '100%' }}
                                min={1} max={65535}
                            />
                        </div>
                        <div>
                            <label htmlFor="pi-interval" style={labelStyle}>Pushing Data Interval (seconds)</label>
                            <InputNumber
                                inputId="pi-interval"
                                value={piInterval}
                                onValueChange={e => setPiInterval(e.value ?? null)}
                                placeholder="e.g. 60"
                                style={{ width: '100%' }}
                                min={1}
                            />
                        </div>
                        <div>
                            <label htmlFor="pi-sensors" style={labelStyle}>Assigned Sensor Stations (optional)</label>
                            <MultiSelect
                                inputId="pi-sensors"
                                value={piAssignedSensors}
                                options={sensorOptions}
                                onChange={e => setPiAssignedSensors(e.value)}
                                placeholder="Add sensor stations"
                                style={{ width: '100%' }}
                                display="chip"
                                filter
                            />
                        </div>
                    </div>
                ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                        <div>
                            <label htmlFor="sensor-name" style={labelStyle}>Name *</label>
                            <InputText
                                id="sensor-name"
                                value={sensorName}
                                onChange={e => setSensorName(e.target.value)}
                                placeholder="e.g. Station-A1"
                                style={{ width: '100%' }}
                            />
                        </div>
                        <div>
                            <label htmlFor="sensor-room" style={labelStyle}>Room *</label>
                            <Dropdown
                                inputId="sensor-room"
                                value={sensorRoomId}
                                options={roomOptions}
                                onChange={e => setSensorRoomId(e.value)}
                                placeholder="Select room"
                                style={{ width: '100%' }}
                                filter
                            />
                        </div>
                        <div>
                            <label htmlFor="sensor-pi" style={labelStyle}>Assigned Raspberry Pi</label>
                            <Dropdown
                                inputId="sensor-pi"
                                value={sensorPiId}
                                options={piOptions}
                                onChange={e => setSensorPiId(e.value)}
                                placeholder="Select Raspberry Pi"
                                style={{ width: '100%' }}
                                showClear
                                filter
                            />
                        </div>
                    </div>
                )}
            </Dialog>
        </div>
    );
};

const labelStyle: React.CSSProperties = {
    display: 'block',
    marginBottom: '0.35rem',
    fontWeight: 500,
    fontSize: '0.9rem',
};

export default DeviceConfigurationPage;
