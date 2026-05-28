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
import { Toast } from 'primereact/toast';
import {
    RaspberryControllerApi,
    SensorStationControllerApi,
    RoomControllerApi,
    RaspberryDTO,
    SensorStationDTO,
    RoomDTO,
} from '../generated-skeleton-api';
import ConfirmDeleteDialog from '../components/ConfirmDeleteDialog';
import '../styles/Tables.css';

const PAGEABLE = { page: 0, size: 100, sort: [] };

type ActiveTab = 'raspberry' | 'sensor';

/** Real API shape — generated TS DTO has wrong flat roomId/roomNumber fields */
interface RaspberryRoomRef { roomId?: string; roomName?: string; }
type RaspberryDTOReal = Omit<RaspberryDTO, 'roomId' | 'roomNumber'> & { room?: RaspberryRoomRef };

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

const labelStyle: React.CSSProperties = {
    display: 'block',
    marginBottom: '0.35rem',
    fontWeight: 500,
    fontSize: '0.9rem',
};

const DeviceConfigurationPage: React.FC = () => {
    const toast = useRef<Toast>(null);
    const [sidebarVisible, setSidebarVisible] = useState(false);
    const [activeTab, setActiveTab] = useState<ActiveTab>('raspberry');

    const [raspberries, setRaspberries] = useState<RaspberryDTOReal[]>([]);
    const [sensors, setSensors] = useState<SensorStationDTO[]>([]);
    const [rooms, setRooms] = useState<RoomDTO[]>([]);
    const [loading, setLoading] = useState(false);

    // Search & filter
    const [roomSearch, setRoomSearch] = useState('');
    const [statusFilter, setStatusFilter] = useState<string | null>(null);
    const [idFilter, setIdFilter] = useState('');

    // Pi dialog
    const [showPiDialog, setShowPiDialog] = useState(false);
    const [editingPiId, setEditingPiId] = useState<string | null>(null);
    const [editingPiOriginalRoomId, setEditingPiOriginalRoomId] = useState<string>('');
    const [piLoading, setPiLoading] = useState(false);
    const [piName, setPiName] = useState('');
    const [piRoomId, setPiRoomId] = useState('');
    const [piIpAddress, setPiIpAddress] = useState('');
    const [piPort, setPiPort] = useState<number | null>(8080);
    const [piInterval, setPiInterval] = useState<number | null>(null);

    // Confirm delete
    const [confirmDelete, setConfirmDelete] = useState<{ message: string; onConfirm: () => void } | null>(null);

    // Sensor dialog
    const [showSensorDialog, setShowSensorDialog] = useState(false);
    const [editingSensorId, setEditingSensorId] = useState<string | null>(null);
    const [editingSensorWriteId, setEditingSensorWriteId] = useState<string | null>(null);
    const [sensorLoading, setSensorLoading] = useState(false);
    const [sensorName, setSensorName] = useState('');
    const [sensorRoomId, setSensorRoomId] = useState('');

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

    // Server may return room as nested { room: { roomId } } or flat { roomId }
    const getPiRoomId = (pi: RaspberryDTOReal): string =>
        pi.room?.roomId ?? (pi as any).roomId ?? '';

    const takenRoomIds = new Set(raspberries.map(getPiRoomId).filter(Boolean));
    const availableRoomOptions = roomOptions.filter(o => !takenRoomIds.has(o.value));

    const editAvailableRoomOptions = editingPiId
        ? roomOptions.filter(o =>
            !raspberries
                .filter(pi => pi.id !== editingPiId)
                .some(pi => getPiRoomId(pi) === o.value)
        )
        : [];

    const getRoomName = (roomId?: string) =>
        rooms.find(r => r.id === roomId)?.name ?? roomId ?? 'N/A';

    const getAssignedSensors = (piId?: string) => {
        if (!piId) return [];
        return sensors.filter(s => s.connectedToPiId === piId);
    };

    const filteredRaspberries = raspberries.filter(pi => {
        if (roomSearch && !(pi.room?.roomName ?? '').toLowerCase().includes(roomSearch.toLowerCase())) return false;
        if (statusFilter && pi.status !== statusFilter) return false;
        if (idFilter && !(pi.id ?? '').toLowerCase().includes(idFilter.toLowerCase())) return false;
        return true;
    });

    const filteredSensors = sensors.filter(s => {
        if (roomSearch && !getRoomName(s.roomId).toLowerCase().includes(roomSearch.toLowerCase())) return false;
        if (statusFilter && s.status !== statusFilter) return false;
        return true;
    });

    const handleDeletePi = (id?: string) => {
        if (!id) return;
        setConfirmDelete({
            message: 'Are you sure you want to delete this Raspberry Pi? This action cannot be undone.',
            onConfirm: () => {
                setConfirmDelete(null);
                new RaspberryControllerApi().deleteSpecificRaspberry({ raspberryId: id })
                    .then(() => {
                        toast.current?.show({ severity: 'success', summary: 'Deleted', detail: 'Raspberry Pi deleted.', life: 3000 });
                        fetchData();
                    })
                    .catch(() => {
                        toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to delete Raspberry Pi.', life: 3000 });
                    });
            },
        });
    };

    const handleRetryPiConnection = (id?: string) => {
        if (!id) return;
        new RaspberryControllerApi().retryDevicesConnection({ raspberryId: id })
            .then(() => {
                toast.current?.show({ severity: 'success', summary: 'Retry sent', detail: 'Reconnection request sent to Raspberry Pi.', life: 3000 });
            })
            .catch(() => {
                toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to send reconnection request.', life: 3000 });
            });
    };

    const handleDeleteSensor = (id?: string) => {
        if (!id) return;
        setConfirmDelete({
            message: 'Are you sure you want to delete this Sensor Station? This action cannot be undone.',
            onConfirm: () => {
                setConfirmDelete(null);
                new SensorStationControllerApi().removeSpecificSensor({ sensorId: id })
                    .then(() => {
                        toast.current?.show({ severity: 'success', summary: 'Deleted', detail: 'Sensor Station deleted.', life: 3000 });
                        fetchData();
                    })
                    .catch(() => {
                        toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to delete Sensor Station.', life: 3000 });
                    });
            },
        });
    };

    const handleSettings = (label: string, id?: string) => {
        // TODO: navigate to detail/edit page
        toast.current?.show({ severity: 'info', summary: 'Settings', detail: `Open settings for ${label} (id: ${id})`, life: 3000 });
    };

    const actionsTemplate = (label: string, onDelete: (id?: string) => void, idField = 'id') => (row: Record<string, any>) => (
        <div style={{ display: 'flex', gap: '0.25rem', justifyContent: 'flex-end' }}>
            <Button icon="pi pi-cog" rounded text severity="secondary" onClick={() => handleSettings(label, row[idField])} title={`${label} settings / details`} />
            <Button icon="pi pi-trash" rounded text severity="danger" onClick={() => onDelete(row[idField])} title={`Delete ${label}`} />
        </div>
    );

    const openPiDialog = () => {
        setEditingPiId(null);
        setPiName(''); setPiRoomId(''); setPiIpAddress(''); setPiPort(8080);
        setPiInterval(null);
        setShowPiDialog(true);
    };

    const openEditPiDialog = (row: RaspberryDTOReal) => {
        const roomId = getPiRoomId(row);
        setEditingPiId(row.id ?? null);
        setEditingPiOriginalRoomId(roomId);
        setPiName(row.name ?? '');
        setPiRoomId(roomId);
        setPiIpAddress(row.ipAddress ?? '');
        setPiPort(row.port ?? 8080);
        setPiInterval(row.frequency ?? null);
        setShowPiDialog(true);
    };

    const openSensorDialog = () => {
        setEditingSensorId(null);
        setEditingSensorWriteId(null);
        setSensorName(''); setSensorRoomId('');
        setShowSensorDialog(true);
    };

    const openEditSensorDialog = (row: SensorStationDTO) => {
        setEditingSensorId(row.readId ?? null);
        setEditingSensorWriteId(row.writeId ?? null);
        setSensorName(row.name ?? '');
        setSensorRoomId(row.roomId ?? '');
        setShowSensorDialog(true);
    };

    const handleSavePi = async () => {
        if (editingPiId) {
            if (!piName || !piIpAddress || piPort == null) {
                toast.current?.show({ severity: 'warn', summary: 'Validation', detail: 'Name, IP address, port and frequency are required.', life: 3000 });
                return;
            }
            setPiLoading(true);
            try {
                await new RaspberryControllerApi().patchSpecificRaspberry({
                    raspberryId: editingPiId,
                    raspberryPatchDTO: {
                        name: piName,
                        ipAddress: piIpAddress,
                        port: piPort ?? undefined,
                        frequency: piInterval ?? undefined,
                    },
                });
                if (piRoomId !== editingPiOriginalRoomId) {
                    const api = new RaspberryControllerApi();
                    const currentRoomId = editingPiOriginalRoomId
                        || getPiRoomId(raspberries.find(pi => pi.id === editingPiId)!);
                    if (currentRoomId) {
                        await api.removeRoomFromRaspberry({ raspberryId: editingPiId, roomId: currentRoomId });
                    }
                    if (piRoomId) {
                        await api.addRoomToRaspberry({ raspberryId: editingPiId, roomId: piRoomId });
                    }
                }
                toast.current?.show({ severity: 'success', summary: 'Saved', detail: 'Raspberry Pi updated successfully.', life: 3000 });
                setShowPiDialog(false);
                fetchData();
            } catch (err: any) {
                const msg = err?.response?.data?.message ?? 'Failed to update Raspberry Pi.';
                toast.current?.show({ severity: 'error', summary: 'Error', detail: msg, life: 4000 });
            } finally {
                setPiLoading(false);
            }
        } else {
            if (!piName || !piRoomId || !piIpAddress || piPort == null || piInterval == null) {
                toast.current?.show({ severity: 'warn', summary: 'Validation', detail: 'Name, IP address, port, and room are required.', life: 3000 });
                return;
            }
            setPiLoading(true);
            try {
                await new RaspberryControllerApi().createNewRaspberry({
                    raspberryCreateDTO: { name: piName, ipAddress: piIpAddress, port: piPort, roomId: piRoomId, frequency: piInterval },
                });
                toast.current?.show({ severity: 'success', summary: 'Created', detail: 'Raspberry Pi created successfully.', life: 3000 });
                setShowPiDialog(false);
                fetchData();
            } catch (err: any) {
                const msg = err?.response?.data?.message ?? 'Failed to create Raspberry Pi.';
                toast.current?.show({ severity: 'error', summary: 'Conflict', detail: msg, life: 4000 });
            } finally {
                setPiLoading(false);
            }
        }
    };

    const handleSaveSensor = async () => {
        if (!sensorName || !sensorRoomId) {
            toast.current?.show({ severity: 'warn', summary: 'Validation', detail: 'Name and room are required.', life: 3000 });
            return;
        }
        setSensorLoading(true);
        try {
            if (editingSensorId) {
                await new SensorStationControllerApi().patchExistingSensorStation({
                    sensorId: editingSensorId,
                    sensorStationPatchDTO: { name: sensorName, roomId: sensorRoomId },
                });
                toast.current?.show({ severity: 'success', summary: 'Saved', detail: 'Sensor Station updated successfully.', life: 3000 });
            } else {
                await new SensorStationControllerApi().createNewSensorStation({
                    sensorStationCreateDTO: { name: sensorName, roomId: sensorRoomId },
                });
                toast.current?.show({ severity: 'success', summary: 'Created', detail: 'Sensor Station created successfully.', life: 3000 });
            }
            setShowSensorDialog(false);
            fetchData();
        } catch {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: `Failed to ${editingSensorId ? 'update' : 'create'} Sensor Station.`, life: 3000 });
        } finally {
            setSensorLoading(false);
        }
    };

    const refreshSensors = () => {
        new SensorStationControllerApi().getAllSensorStations({ pageable: PAGEABLE })
            .then(res => setSensors(res.data.content ?? []))
            .catch(() => {});
    };

    const handleDisconnectSensor = (sensorId: string) => {
        new SensorStationControllerApi().disconnectSensorFromRoom({ sensorId })
            .then(() => {
                toast.current?.show({ severity: 'success', summary: 'Disconnected', detail: 'Sensor station disconnected from Pi.', life: 3000 });
                refreshSensors();
            })
            .catch(() => {
                toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to disconnect sensor station.', life: 3000 });
            });
    };

    const handleRetryConnection = (sensorId: string) => {
        new SensorStationControllerApi().retrySensorStation({ sensorId })
            .then(() => {
                toast.current?.show({ severity: 'success', summary: 'Retry sent', detail: 'Reconnection request sent to Raspberry Pi.', life: 3000 });
            })
            .catch(() => {
                toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to send reconnection request.', life: 3000 });
            });
    };

    const handleRemoveSensorFromPi = (row: SensorStationDTO) => {
        setConfirmDelete({
            message: `Remove "${row.name ?? row.readId}" from this Raspberry Pi? This action cannot be undone.`,
            onConfirm: () => {
                setConfirmDelete(null);
                handleDisconnectSensor(row.readId!);
            },
        });
    };

const TableSectionHeader: React.FC<{ title: string; tab: ActiveTab }> = ({ title, tab }) => (
        <div className="flex-header" style={{ marginBottom: '1rem', flexWrap: 'wrap', gap: '0.75rem' }}>
            <h3 style={{ margin: 0 }}>{title}</h3>
            <Button
                label={tab === 'raspberry' ? 'Add Raspberry Pi' : 'Add Sensor Station'}
                icon="pi pi-plus"
                size="small"
                onClick={tab === 'raspberry' ? openPiDialog : openSensorDialog}
            />
        </div>
    );

    return (
        <div className="dashboard-layout">
            <Toast ref={toast} />
            <PageHeader title="Device Configuration" onMenuClick={() => setSidebarVisible(true)} />
            <SidebarComponent visible={sidebarVisible} onHide={() => setSidebarVisible(false)} />

            <div className="dashboard-content">

                {/* ── Tab switcher ── */}
                <div className="device-tab-row">
                    <Button label="Raspberry Pi List" severity={activeTab === 'raspberry' ? undefined : 'secondary'} outlined={activeTab !== 'raspberry'} onClick={() => setActiveTab('raspberry')} className="admin-tab-btn" />
                    <Button label="Sensor Station List" severity={activeTab === 'sensor' ? undefined : 'secondary'} outlined={activeTab !== 'sensor'} onClick={() => { setActiveTab('sensor'); setIdFilter(''); }} className="admin-tab-btn" />
                </div>

                {/* ── Raspberry Pi table ── */}
                {activeTab === 'raspberry' && (
                    <div className="table-container">
                        <div className="flex-header">
                            <h3>Raspberry Pi List</h3>
                            <Button label="Add Raspberry Pi" icon="pi pi-plus" className="admin-add-button" onClick={openPiDialog} />
                        </div>

                        <div className="table-filter-row">
                <span className="p-input-icon-left">
                    <i className="pi pi-search" />
                    <InputText value={roomSearch} onChange={e => setRoomSearch(e.target.value)} placeholder="Search by room" />
                </span>
                            <Dropdown value={statusFilter} options={statusOptions} onChange={e => setStatusFilter(e.value)} placeholder="Status Filter" showClear />
                        </div>

                        <DataTable value={filteredRaspberries} loading={loading} stripedRows emptyMessage="No Raspberry Pis found." responsiveLayout="scroll">
                            <Column field="name" header="Name" sortable />
                            <Column header="Room" body={(row: RaspberryDTOReal) => row.room?.roomName ?? <span style={{ color: '#9e9e9e' }}>N/A</span>} />
                            <Column header="Sensors" body={(row: RaspberryDTOReal) => {
                                const assigned = getAssignedSensors(row.id);
                                return assigned.length > 0 ? <span title={assigned.map(s => s.name).join(', ')}>{assigned.length}</span> : <span style={{ color: '#9e9e9e' }}>None</span>;
                            }} />
                            <Column header="Status" body={(row: RaspberryDTOReal) => statusBadge(row.status)} />
                            <Column header="" className="admin-actions-column admin-actions-column-wide" headerClassName="admin-actions-column admin-actions-column-wide" exportable={false} body={(row: RaspberryDTOReal) => (
                                <div className="admin-table-actions">
                                    <Button icon="pi pi-refresh" rounded text severity="warning" title="Retry connection" onClick={() => handleRetryPiConnection(row.id)} />
                                    <Button icon="pi pi-cog" rounded text severity="secondary" title="Edit Raspberry Pi" onClick={() => openEditPiDialog(row)} />
                                    <Button icon="pi pi-trash" rounded text severity="danger" title="Delete Raspberry Pi" onClick={() => handleDeletePi(row.id)} />
                                </div>
                            )} />
                        </DataTable>
                    </div>
                )}

                {/* ── Sensor Station table ── */}
                {activeTab === 'sensor' && (
                    <div className="table-container">
                        <div className="flex-header">
                            <h3>Sensor Station List</h3>
                            <Button label="Add Sensor Station" icon="pi pi-plus" className="admin-add-button" onClick={openSensorDialog} />
                        </div>

                        <div className="table-filter-row">
                <span className="p-input-icon-left">
                    <i className="pi pi-search" />
                    <InputText value={roomSearch} onChange={e => setRoomSearch(e.target.value)} placeholder="Search by room" />
                </span>
                            <Dropdown value={statusFilter} options={statusOptions} onChange={e => setStatusFilter(e.value)} placeholder="Status Filter" showClear />
                        </div>

                        <DataTable value={filteredSensors} loading={loading} stripedRows emptyMessage="No Sensor Stations found." responsiveLayout="scroll">
                            <Column field="name" header="Name" sortable />
                            <Column header="Room" body={(row: SensorStationDTO) => getRoomName(row.roomId)} />
                            <Column header="Assigned Pi" body={(row: SensorStationDTO) => {
                                const pi = raspberries.find(p => p.id === row.connectedToPiId);
                                return pi ? (pi.name ?? pi.id) : <span style={{ color: '#9e9e9e' }}>None</span>;
                            }} />
                            <Column header="Status" body={(row: SensorStationDTO) => statusBadge(row.status)} />
                            <Column header="" className="admin-actions-column admin-actions-column-wide" headerClassName="admin-actions-column admin-actions-column-wide" exportable={false} body={(row: SensorStationDTO) => (
                                <div className="admin-table-actions">
                                    <Button icon="pi pi-refresh" rounded text severity="warning" title="Retry connection to Raspberry Pi" onClick={() => handleRetryConnection(row.readId!)} />
                                    <Button icon="pi pi-cog" rounded text severity="secondary" title="Edit Sensor Station" onClick={() => openEditSensorDialog(row)} />
                                    <Button icon="pi pi-trash" rounded text severity="danger" title="Delete Sensor Station" onClick={() => handleDeleteSensor(row.readId)} />
                                </div>
                            )} />
                        </DataTable>
                    </div>
                )}
            </div>

            {/* ── Add / Edit Raspberry Pi Dialog ── */}
            {/* ── Add / Edit Raspberry Pi Dialog ── */}
            <Dialog
                header={editingPiId ? 'Edit Raspberry Pi' : 'Add Raspberry Pi'}
                visible={showPiDialog}
                className="admin-form-dialog"
                style={{ width: '480px' }}
                onHide={() => setShowPiDialog(false)}
                footer={
                    <div className="admin-dialog-footer">
                        <Button label="Cancel" severity="secondary" outlined onClick={() => setShowPiDialog(false)} />
                        <Button label={editingPiId ? 'Save' : 'Create'} icon="pi pi-check" loading={piLoading} onClick={handleSavePi} />
                    </div>
                }
                draggable={false}
            >
                <div className="admin-dialog-body">
                    <div>
                        <label htmlFor="pi-name" style={labelStyle}>Name *</label>
                        <InputText id="pi-name" value={piName} onChange={e => setPiName(e.target.value)} placeholder="e.g. Pi-Lab-01" style={{ width: '100%' }} />
                    </div>

                    {!editingPiId ? (
                        <div>
                            <label htmlFor="pi-room" style={labelStyle}>Room *</label>
                            <Dropdown inputId="pi-room" value={piRoomId} options={availableRoomOptions} onChange={e => setPiRoomId(e.value)} placeholder="Select room" style={{ width: '100%' }} filter />
                        </div>
                    ) : (
                        <div>
                            <label htmlFor="pi-room-edit" style={labelStyle}>Room</label>
                            <Dropdown inputId="pi-room-edit" value={piRoomId} options={editAvailableRoomOptions} onChange={e => setPiRoomId(e.value)} placeholder="Select room" style={{ width: '100%' }} filter />
                        </div>
                    )}

                    <div>
                        <label htmlFor="pi-ip" style={labelStyle}>IP Address *</label>
                        <InputText id="pi-ip" value={piIpAddress} onChange={e => setPiIpAddress(e.target.value)} placeholder="e.g. 192.168.1.10" style={{ width: '100%' }} />
                    </div>

                    <div>
                        <label htmlFor="pi-port" style={labelStyle}>Port *</label>
                        <InputNumber inputId="pi-port" value={piPort} onValueChange={e => setPiPort(e.value ?? null)} placeholder="e.g. 1000–9999" style={{ width: '100%' }} inputStyle={{ width: '100%' }} min={1000} max={9999} useGrouping={false} />
                    </div>

                    <div>
                        <label htmlFor="pi-interval" style={labelStyle}>Pushing Data Interval (seconds)</label>
                        <InputNumber inputId="pi-interval" value={piInterval} onValueChange={e => setPiInterval(e.value ?? null)} placeholder="e.g. 60" style={{ width: '100%' }} inputStyle={{ width: '100%' }} min={1} />
                    </div>

                    {editingPiId && (
                        <div>
                            <label style={labelStyle}>Connected Sensor Stations</label>
                            <DataTable value={sensors.filter(s => s.connectedToPiId === editingPiId)} size="small" emptyMessage="No sensor stations connected.">
                                <Column field="name" header="Name" />
                                <Column field="writeId" header="Write ID" style={{ maxWidth: '8rem', overflow: 'hidden', textOverflow: 'ellipsis' }} />
                                <Column field="readId" header="Read ID" style={{ maxWidth: '8rem', overflow: 'hidden', textOverflow: 'ellipsis' }} />
                                <Column header="" className="admin-actions-column" headerClassName="admin-actions-column" body={(row: SensorStationDTO) => (
                                    <div className="admin-table-actions">
                                        <Button icon="pi pi-refresh" rounded text severity="warning" title="Retry connection" onClick={() => handleRetryConnection(row.readId!)} />
                                        <Button icon="pi pi-trash" rounded text severity="danger" title="Remove from this Raspberry Pi" onClick={() => handleRemoveSensorFromPi(row)} />
                                    </div>
                                )} />
                            </DataTable>
                        </div>
                    )}
                </div>
            </Dialog>

            <ConfirmDeleteDialog
                visible={confirmDelete !== null}
                message={confirmDelete?.message ?? ''}
                onConfirm={confirmDelete?.onConfirm ?? (() => {})}
                onHide={() => setConfirmDelete(null)}
            />

            {/* ── Add / Edit Sensor Station Dialog ── */}
            <Dialog
                header={editingSensorId ? 'Edit Sensor Station' : 'Add Sensor Station'}
                visible={showSensorDialog}
                className="admin-form-dialog"
                style={{ width: '480px' }}
                onHide={() => setShowSensorDialog(false)}
                footer={
                    <div className="admin-dialog-footer">
                        <Button label="Cancel" severity="secondary" outlined onClick={() => setShowSensorDialog(false)} />
                        <Button label={editingSensorId ? 'Save' : 'Create'} icon="pi pi-check" loading={sensorLoading} onClick={handleSaveSensor} />
                    </div>
                }
                draggable={false}
            >
                <div className="admin-dialog-body">
                    <div>
                        <label htmlFor="sensor-name" style={labelStyle}>Name *</label>
                        <InputText id="sensor-name" value={sensorName} onChange={e => setSensorName(e.target.value)} placeholder="e.g. Station-A1" style={{ width: '100%' }} />
                    </div>

                    <div>
                        <label htmlFor="sensor-room" style={labelStyle}>Room *</label>
                        <Dropdown inputId="sensor-room" value={sensorRoomId} options={roomOptions} onChange={e => setSensorRoomId(e.value)} placeholder="Select room" style={{ width: '100%' }} filter />
                    </div>

                    {editingSensorId && (
                        <>
                            <div>
                                <label style={labelStyle}>Read ID</label>
                                <InputText value={String(editingSensorId)} readOnly style={{ width: '100%', fontFamily: 'monospace', fontSize: '0.85rem', background: '#f5f5f5', color: '#555' }} />
                            </div>
                            <div>
                                <label style={labelStyle}>Write ID</label>
                                <InputText value={editingSensorWriteId ? String(editingSensorWriteId) : ''} readOnly style={{ width: '100%', fontFamily: 'monospace', fontSize: '0.85rem', background: '#f5f5f5', color: '#555' }} />
                            </div>
                        </>
                    )}
                </div>
            </Dialog>
        </div>
    );
};

export default DeviceConfigurationPage;
