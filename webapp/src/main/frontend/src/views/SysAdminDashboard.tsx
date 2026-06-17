import React, { useState, useEffect, useRef } from 'react';
import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import UserListComponent from '../components/UserListComponent';
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
    BuildingControllerApi,
    DepartmentControllerApi,
    UserRoleControllerApi,
    UserxControllerApi,
    FormulaWeightsConfigurationApi,
    RaspberryDTO,
    SensorStationDTO,
    RoomDTO,
    BuildingListDTO,
    DepartmentListDTO,
    UserRoleDTO,
    RoomCreateDTORoomTypeEnum,
    RoomPatchDTORoomTypeEnum,
} from '../generated-skeleton-api';
import KlimaScoreWeights, { KlimaScoreWeightsState } from '../components/KlimaScoreWeights';
import BuildingFormDialog, { BuildingFormState, emptyBuildingForm } from '../components/BuildingFormDialog';
import DepartmentFormDialog, { DepartmentFormState, emptyDepartmentForm } from '../components/DepartmentFormDialog';
import RoomFormDialog, { RoomFormState, emptyRoomForm } from '../components/RoomFormDialog';
import UserFormDialog, { UserFormState, emptyForm } from '../components/UserFormDialog';
import ConfirmDeleteDialog from '../components/ConfirmDeleteDialog';
import '../styles/Tables.css';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

interface RaspberryRoomRef { roomId?: string; roomName?: string; }
type RaspberryDTOReal = Omit<RaspberryDTO, 'roomId' | 'roomNumber'> & { room?: RaspberryRoomRef };

const labelStyle: React.CSSProperties = {
    display: 'block',
    marginBottom: '0.35rem',
    fontWeight: 500,
    fontSize: '0.9rem',
};

interface UserRoleSummary { id: string; name: string; }
interface UserRoomSummary { id: string; departmentName: string; roomNumber: string; }
interface FullUser {
    id: string;
    username: string;
    firstName: string;
    lastName: string;
    enabled: boolean;
    roles: UserRoleSummary[];
    myRoom: UserRoomSummary | null;
}

const PAGEABLE = { page: 0, size: 200, sort: [] };

const statusBadge = (status?: string) => {
    const online = status === 'ONLINE';
    return (
        <span style={{
            background: online ? '#4caf50' : '#9e9e9e',
            color: 'white', padding: '1px 7px', borderRadius: '12px',
            fontSize: '0.72rem', fontWeight: 600,
        }}>
            {online ? 'Online' : status ? 'Offline' : 'N/A'}
        </span>
    );
};

const enabledBadge = (enabled?: boolean) => (
    <span style={{
        background: enabled ? '#4caf50' : '#9e9e9e',
        color: 'white', padding: '2px 10px', borderRadius: '12px',
        fontSize: '0.8rem', fontWeight: 500,
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
    readonly onAdd?: () => void;
    readonly addLabel?: string;
}

const TableHeader: React.FC<TableHeaderProps> = ({ title, search, onSearch, searchPlaceholder, filterEl, onAdd, addLabel }) => (
    <>
        <div className="flex-header">
            <h3 style={{ margin: 0 }}>{title}</h3>
            {onAdd && <Button label={addLabel ?? `Add`} icon="pi pi-plus" className="admin-add-button" onClick={onAdd} />}
        </div>
        <div className="table-filter-row">
            <span className="p-input-icon-left">
                <i className="pi pi-search"/>
                <InputText
                    value={search}
                    onChange={e => onSearch(e.target.value)}
                    placeholder={searchPlaceholder}
                    style={{ borderRadius: '20px', paddingLeft: '2.0rem' }}
                />
            </span>
            {filterEl}
        </div>
    </>
);

const SysAdminDashboard: React.FC = () => {
    const toast = useRef<Toast>(null);
    const [sidebarVisible, setSidebarVisible] = useState(false);
    const [isMobile, setIsMobile] = useState(window.innerWidth <= 700);

    // --- Data ---
    const [raspberries, setRaspberries] = useState<RaspberryDTOReal[]>([]);
    const [sensors, setSensors] = useState<SensorStationDTO[]>([]);
    const [users, setUsers] = useState<FullUser[]>([]);
    const [rooms, setRooms] = useState<RoomDTO[]>([]);
    const [buildings, setBuildings] = useState<BuildingListDTO[]>([]);
    const [departments, setDepartments] = useState<DepartmentListDTO[]>([]);
    const [roleDTOs, setRoleDTOs] = useState<UserRoleDTO[]>([]);

    // --- Klima score weights ---
    const [weights, setWeights] = useState<KlimaScoreWeightsState | null>(null);
    const [weightsSaving, setWeightsSaving] = useState(false);

    // --- Confirm delete ---
    const [confirmDelete, setConfirmDelete] = useState<{ message: string; onConfirm: () => void } | null>(null);

    // --- Pi dialog ---
    const [showPiDialog, setShowPiDialog] = useState(false);
    const [editingPiId, setEditingPiId] = useState<string | null>(null);
    const [editingPiOriginalRoomId, setEditingPiOriginalRoomId] = useState('');
    const [piLoading, setPiLoading] = useState(false);
    const [piName, setPiName] = useState('');
    const [piRoomId, setPiRoomId] = useState('');
    const [piIpAddress, setPiIpAddress] = useState('');
    const [piPort, setPiPort] = useState<number | null>(8080);
    const [piInterval, setPiInterval] = useState<number | null>(null);

    // --- Sensor dialog ---
    const [showSensorDialog, setShowSensorDialog] = useState(false);
    const [editingSensorId, setEditingSensorId] = useState<string | null>(null);
    const [editingSensorWriteId, setEditingSensorWriteId] = useState<string | null>(null);
    const [sensorLoading, setSensorLoading] = useState(false);
    const [sensorName, setSensorName] = useState('');
    const [sensorRoomId, setSensorRoomId] = useState('');

    // --- Building dialog ---
    const [showBuildingEditDialog, setShowBuildingEditDialog] = useState(false);
    const [isNewBuilding, setIsNewBuilding] = useState(true);
    const [editingBuildingId, setEditingBuildingId] = useState<string | undefined>();
    const [buildingForm, setBuildingForm] = useState<BuildingFormState>(emptyBuildingForm());
    const [buildingFormErrors, setBuildingFormErrors] = useState<Partial<Record<keyof BuildingFormState, string>>>({});
    const [buildingDialogLoading, setBuildingDialogLoading] = useState(false);

    // --- Department dialog ---
    const [showDeptEditDialog, setShowDeptEditDialog] = useState(false);
    const [isNewDepartment, setIsNewDepartment] = useState(true);
    const [editingDeptId, setEditingDeptId] = useState<string | undefined>();
    const [deptForm, setDeptForm] = useState<DepartmentFormState>(emptyDepartmentForm());
    const [deptFormErrors, setDeptFormErrors] = useState<Partial<Record<keyof DepartmentFormState, string>>>({});
    const [deptDialogLoading, setDeptDialogLoading] = useState(false);

    // --- Room dialog ---
    const [showRoomEditDialog, setShowRoomEditDialog] = useState(false);
    const [isNewRoom, setIsNewRoom] = useState(true);
    const [editingRoomId, setEditingRoomId] = useState<string | undefined>();
    const [roomForm, setRoomForm] = useState<RoomFormState>(emptyRoomForm());
    const [roomFormErrors, setRoomFormErrors] = useState<Partial<Record<keyof RoomFormState, string>>>({});
    const [roomDialogLoading, setRoomDialogLoading] = useState(false);

    // --- User dialog ---
    const [showUserDialog, setShowUserDialog] = useState(false);
    const [isNewUser, setIsNewUser] = useState(true);
    const [editingUserId, setEditingUserId] = useState<string | undefined>();
    const [userForm, setUserForm] = useState<UserFormState>(emptyForm());
    const [userFormErrors, setUserFormErrors] = useState<Partial<Record<keyof UserFormState, string>>>({});
    const [userDialogLoading, setUserDialogLoading] = useState(false);

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
    const [userRoleFilter, setUserRoleFilter] = useState<string | null>(null);
    const [userRoomFilter, setUserRoomFilter] = useState<string | null>(null);
    const [roomTypeFilter, setRoomTypeFilter] = useState<string | null>(null);
    const [departmentBuildingFilter, setDepartmentBuildingFilter] = useState<string | null>(null);

    const stompClient = useRef<Client | null>(null);

    const fetchRaspberries = () =>
        new RaspberryControllerApi().getAllRaspberries({ pageable: PAGEABLE })
            .then(res => setRaspberries(res.data.content ?? [])).catch(() => {});

    const refreshSensors = () =>
        new SensorStationControllerApi().getAllSensorStations({ pageable: PAGEABLE })
            .then(res => setSensors(res.data.content ?? [])).catch(() => {});

    const fetchRooms = () =>
        new RoomControllerApi().getPageOfRooms({ pageable: PAGEABLE })
            .then(res => setRooms(res.data.content ?? [])).catch(() => {});

    const fetchDepartments = () =>
        new DepartmentControllerApi().getPageOfDepartments({ pageable: PAGEABLE })
            .then(res => setDepartments(res.data.content ?? [])).catch(() => {});

    useEffect(() => {
        if ((!sensors || sensors.length == 0) && (!raspberries || raspberries.length == 0)) return;

        stompClient.current = new Client({
        webSocketFactory: () =>
            new SockJS("http://localhost:8080/active-events"),
        reconnectDelay: 5000,
        connectHeaders: {
            Authorization: `Bearer ${localStorage.getItem("bearerToken")}`,
        },
        onConnect: () => {
            console.log("Listening to active devices events");
            sensors.forEach(sensor => {
                stompClient.current?.subscribe(
                `/topic/sensor-status/${sensor.readId}`,
                (message) => {
                    const status = JSON.parse(message.body);
                    setSensors((prev) =>
                        prev.map((s) =>
                            s.readId === sensor.readId
                            ? { ...s, status: status }
                            : s
                        )
                    );
                });
            })
            raspberries.forEach(raspberry => {
                stompClient.current?.subscribe(
                    `/topic/raspberry-status/${raspberry.id}`,
                    (message) => {
                        const status = JSON.parse(message.body);
                        setRaspberries((prev) => 
                            prev.map(r => 
                                r.id == raspberry.id
                                ? { ...r, status: status}
                                : r
                            )
                        )
                    }
                )
            })
        },
        onStompError: (frame) => {
            console.error("STOMP error:", frame);
        },
        });

        stompClient.current.activate();

        return () => {
        stompClient.current?.deactivate();
        };
    }, [sensors, raspberries]);


    useEffect(() => {
        const handler = () => setIsMobile(window.innerWidth <= 700);
        window.addEventListener('resize', handler);
        return () => window.removeEventListener('resize', handler);
    }, []);

    useEffect(() => {
        fetchRaspberries();
        refreshSensors();
        new UserxControllerApi().getPageOfUsers({ pageable: PAGEABLE })
            .then(res => setUsers((res.data.content as any) ?? [])).catch(() => {});
        new UserRoleControllerApi().getAllPermissions()
            .then(res => setRoleDTOs(res.data ?? [])).catch(() => {});
        fetchRooms();
        new BuildingControllerApi().getPageOfBuildings({ pageable: PAGEABLE })
            .then(res => setBuildings(res.data.content ?? [])).catch(() => {});
        fetchDepartments();
        new FormulaWeightsConfigurationApi().getCurrentFormulaWeights()
            .then(res => {
                const d = res.data;
                if (d.tempWeight != null && d.humWeight != null && d.co2Weight != null) {
                    setWeights({
                        temperature: Math.round(d.tempWeight * 100),
                        humidity:    Math.round(d.humWeight * 100),
                        co2:         Math.round(d.co2Weight * 100),
                    });
                }
            }).catch(() => {});
    }, []);

    // --- Filtered data ---
    const getPiRoomId = (pi: RaspberryDTOReal): string =>
        pi.room?.roomId ?? (pi as any).roomId ?? '';

    const takenRoomIds = new Set(raspberries.map(getPiRoomId).filter(Boolean));
    const availableRoomOptions = rooms
        .filter(r => !takenRoomIds.has(r.id ?? ''))
        .map(r => ({ label: r.name ?? r.id ?? '', value: r.id ?? '' }));
    const editAvailableRoomOptions = editingPiId
        ? rooms
            .filter(r => !raspberries.filter(pi => pi.id !== editingPiId).some(pi => getPiRoomId(pi) === r.id))
            .map(r => ({ label: r.name ?? r.id ?? '', value: r.id ?? '' }))
        : [];

    const filteredRaspberries = raspberries.filter(pi => {
        if (raspberrySearch && !(pi.name ?? '').toLowerCase().includes(raspberrySearch.toLowerCase())) return false;
        if (raspberryStatusFilter && pi.status !== raspberryStatusFilter) return false;
        return true;
    });

    const filteredSensors = sensors.filter(s => {
        if (sensorSearch && !(s.name ?? '').toLowerCase().includes(sensorSearch.toLowerCase())) return false;
        if (sensorStatusFilter && s.status !== sensorStatusFilter) return false;
        return true;
    });

    const filteredUsers = users.filter(u => {
        if (u.roles.some(r => r.name === 'RASPBERRY_PI')) return false;
        if (userSearch && !(u.lastName ?? '').toLowerCase().includes(userSearch.toLowerCase())) return false;
        if (userRoleFilter && !u.roles.some(r => r.name === userRoleFilter)) return false;
        if (userRoomFilter && u.myRoom?.id !== userRoomFilter) return false;
        return true;
    });

    const filteredRooms = rooms.filter(r => {
        if (roomSearch && !(r.name ?? '').toLowerCase().includes(roomSearch.toLowerCase())) return false;
        if (roomTypeFilter && r.roomType !== roomTypeFilter) return false;
        return true;
    });

    const filteredBuildings = buildings.filter(b =>
        !buildingSearch || (b.name ?? '').toLowerCase().includes(buildingSearch.toLowerCase())
    );

    const filteredDepartments = departments.filter(d => {
        if (departmentSearch && !(d.name ?? '').toLowerCase().includes(departmentSearch.toLowerCase())) return false;
        if (departmentBuildingFilter && d.buildingName !== departmentBuildingFilter) return false;
        return true;
    });

    const buildingNameOptions = [...new Set(departments.map(d => d.buildingName).filter(Boolean))] as string[];
    const buildingOptions = buildings.map(b => ({ label: b.name ?? b.id ?? '', value: b.id ?? '' }));
    const departmentOptions = departments.map(d => ({
        label: d.buildingName ? `${d.name ?? ''} (${d.buildingName})` : (d.name ?? d.id ?? ''),
        value: d.id ?? '',
    }));
    const roomOptions = rooms.map(r => ({ label: r.name ?? r.id ?? '', value: r.id ?? '' }));
    const roomFilterOptions = rooms.map(r => ({
        label: r.departmentName ? `${r.name ?? r.id} (${r.departmentName})` : `${r.name ?? r.id}`,
        value: r.id ?? '',
    }));
    const roleOptions = roleDTOs.map(r => ({ label: r.name ?? '', value: r.id ?? '' }));
    const roleFilterOptions = roleDTOs.map(r => ({ label: r.name ?? '', value: r.name ?? '' }));

    // --- Retry handlers ---
    const handleRetryPiConnection = (id?: string) => {
        if (!id) return;
        new RaspberryControllerApi().retryDevicesConnection({ raspberryId: id })
            .then(() => toast.current?.show({ severity: 'success', summary: 'Retry sent', detail: 'Reconnection request sent to Raspberry Pi.', life: 3000 }))
            .catch(() => toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to send reconnection request.', life: 3000 }));
    };

    const handleRetrySensorConnection = (sensorId: string) => {
        new SensorStationControllerApi().retrySensorStation({ sensorId })
            .then(() => toast.current?.show({ severity: 'success', summary: 'Retry sent', detail: 'Reconnection request sent.', life: 3000 }))
            .catch(() => toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to send reconnection request.', life: 3000 }));
    };

    // --- Pi handlers ---
    const openAddPiDialog = () => {
        setEditingPiId(null);
        setEditingPiOriginalRoomId('');
        setPiName(''); setPiRoomId(''); setPiIpAddress(''); setPiPort(8080); setPiInterval(null);
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

    const handleSavePi = async () => {
        if (editingPiId) {
            if (!piName || !piIpAddress || piPort == null) {
                toast.current?.show({ severity: 'warn', summary: 'Validation', detail: 'Name, IP address, and port are required.', life: 3000 });
                return;
            }
            setPiLoading(true);
            try {
                await new RaspberryControllerApi().patchSpecificRaspberry({
                    raspberryId: editingPiId,
                    raspberryPatchDTO: { name: piName, ipAddress: piIpAddress, port: piPort, frequency: piInterval ?? undefined },
                });
                if (piRoomId !== editingPiOriginalRoomId) {
                    const api = new RaspberryControllerApi();
                    if (editingPiOriginalRoomId) {
                        await api.removeRoomFromRaspberry({ raspberryId: editingPiId, roomId: editingPiOriginalRoomId });
                    }
                    if (piRoomId) {
                        await api.addRoomToRaspberry({ raspberryId: editingPiId, roomId: piRoomId });
                    }
                }
                toast.current?.show({ severity: 'success', summary: 'Saved', detail: 'Raspberry Pi updated successfully.', life: 3000 });
                setShowPiDialog(false);
                fetchRaspberries();
            } catch {
                toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to update Raspberry Pi.', life: 3000 });
            } finally {
                setPiLoading(false);
            }
        } else {
            if (!piName || !piRoomId || !piIpAddress || piPort == null || piInterval == null) {
                toast.current?.show({ severity: 'warn', summary: 'Validation', detail: 'Name, IP address, port, room and interval are required.', life: 3000 });
                return;
            }
            setPiLoading(true);
            try {
                await new RaspberryControllerApi().createNewRaspberry({
                    raspberryCreateDTO: { name: piName, ipAddress: piIpAddress, port: piPort, roomId: piRoomId, frequency: piInterval },
                });
                toast.current?.show({ severity: 'success', summary: 'Created', detail: 'Raspberry Pi created successfully.', life: 3000 });
                setShowPiDialog(false);
                fetchRaspberries();
            } catch (err: any) {
                const msg = err?.response?.data?.detail ?? 'Failed to create Raspberry Pi.';
                toast.current?.show({ severity: 'error', summary: 'Error', detail: msg, life: 4000 });
            } finally {
                setPiLoading(false);
            }
        }
    };

    const handleDeletePi = (id?: string) => {
        if (!id) return;
        setConfirmDelete({
            message: 'Are you sure you want to delete this Raspberry Pi? This action cannot be undone.',
            onConfirm: () => {
                setConfirmDelete(null);
                new RaspberryControllerApi().deleteSpecificRaspberry({ raspberryId: id })
                    .then(() => {
                        setRaspberries(prev => prev.filter(p => p.id !== id));
                        setSensors(prev => prev.map(s => s.connectedToPiId === id ? { ...s, connectedToPiId: undefined } : s));
                        toast.current?.show({ severity: 'success', summary: 'Deleted', detail: 'Raspberry Pi deleted.', life: 3000 });
                    })
                    .catch(() => toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to delete Raspberry Pi.', life: 3000 }));
            },
        });
    };

    const handleDisconnectSensor = (sensorId: string) => {
        new SensorStationControllerApi().disconnectSensorFromRoom({ sensorId })
            .then(() => {
                toast.current?.show({ severity: 'success', summary: 'Disconnected', detail: 'Sensor station disconnected from Pi.', life: 3000 });
                refreshSensors();
            })
            .catch(() => toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to disconnect sensor station.', life: 3000 }));
    };

    // --- Sensor handlers ---
    const openAddSensorDialog = () => {
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
            refreshSensors();
        } catch {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: `Failed to ${editingSensorId ? 'update' : 'create'} Sensor Station.`, life: 3000 });
        } finally {
            setSensorLoading(false);
        }
    };

    const handleDeleteSensor = (id?: string) => {
        if (!id) return;
        setConfirmDelete({
            message: 'Are you sure you want to delete this Sensor Station? This action cannot be undone.',
            onConfirm: () => {
                setConfirmDelete(null);
                new SensorStationControllerApi().removeSpecificSensor({ sensorId: id })
                    .then(() => {
                        setSensors(prev => prev.filter(s => s.readId !== id));
                        toast.current?.show({ severity: 'success', summary: 'Deleted', detail: 'Sensor Station deleted.', life: 3000 });
                    })
                    .catch(() => toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to delete Sensor Station.', life: 3000 }));
            },
        });
    };

    // --- Building handlers ---
    const openAddBuilding = () => {
        setBuildingForm(emptyBuildingForm());
        setBuildingFormErrors({});
        setIsNewBuilding(true);
        setEditingBuildingId(undefined);
        setShowBuildingEditDialog(true);
    };

    const openEditBuilding = (b: BuildingListDTO) => {
        setBuildingForm({ name: b.name ?? '', address: b.address ?? '' });
        setBuildingFormErrors({});
        setIsNewBuilding(false);
        setEditingBuildingId(b.id);
        setShowBuildingEditDialog(true);
    };

    const handleSaveBuilding = async () => {
        const errors: Partial<Record<keyof BuildingFormState, string>> = {};
        if (!buildingForm.name.trim()) errors.name = 'Required';
        if (Object.keys(errors).length > 0) { setBuildingFormErrors(errors); return; }
        setBuildingDialogLoading(true);
        try {
            if (isNewBuilding) {
                const res = await new BuildingControllerApi().createNewBuilding({
                    buildingCreateDTO: { name: buildingForm.name, address: buildingForm.address || undefined },
                });
                setBuildings(prev => [...prev, { id: res.data.id, name: res.data.name, address: res.data.address }]);
                toast.current?.show({ severity: 'success', summary: 'Created', detail: 'Building created.', life: 3000 });
            } else {
                const res = await new BuildingControllerApi().patchSpecificBuilding({
                    buildingId: editingBuildingId!,
                    buildingCreateDTO: { name: buildingForm.name, address: buildingForm.address || undefined },
                });
                setBuildings(prev => prev.map(b => b.id === res.data.id
                    ? { id: res.data.id, name: res.data.name, address: res.data.address } : b));
                toast.current?.show({ severity: 'success', summary: 'Saved', detail: 'Building updated.', life: 3000 });
            }
            setShowBuildingEditDialog(false);
        } catch {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to save building.', life: 3000 });
        } finally {
            setBuildingDialogLoading(false);
        }
    };

    const handleDeleteBuilding = (id?: string) => {
        if (!id) return;
        setConfirmDelete({
            message: 'Are you sure you want to delete this building? This action cannot be undone.',
            onConfirm: () => {
                setConfirmDelete(null);
                new BuildingControllerApi().deleteSpecificBuilding({ buildingId: id })
                    .then(() => {
                        setBuildings(prev => prev.filter(b => b.id !== id));
                        toast.current?.show({ severity: 'success', summary: 'Deleted', detail: 'Building deleted.', life: 3000 });
                    })
                    .catch(() => toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to delete building.', life: 3000 }));
            },
        });
    };

    // --- Department handlers ---
    const openAddDepartment = () => {
        setDeptForm(emptyDepartmentForm());
        setDeptFormErrors({});
        setIsNewDepartment(true);
        setEditingDeptId(undefined);
        setShowDeptEditDialog(true);
    };

    const openEditDepartment = (d: DepartmentListDTO) => {
        const currentRoomIds = rooms.filter(r => r.departmentID === d.id && r.id).map(r => r.id!);
        setDeptForm({
            name: d.name ?? '',
            buildingID: d.buildingID ?? '',
            currentRoomIds,
            existingRoomIds: [],
            rooms: [],
            roomIdsToDelete: [],
        });
        setDeptFormErrors({});
        setIsNewDepartment(false);
        setEditingDeptId(d.id);
        setShowDeptEditDialog(true);
    };

    const handleSaveDepartment = async () => {
        const errors: Partial<Record<keyof DepartmentFormState, string>> = {};
        if (!deptForm.name.trim()) errors.name = 'Required';
        if (!deptForm.buildingID) errors.buildingID = 'Required';
        if (Object.keys(errors).length > 0) { setDeptFormErrors(errors); return; }
        setDeptDialogLoading(true);
        try {
            if (isNewDepartment) {
                const res = await new DepartmentControllerApi().createNewDepartment({
                    departmentCreateDTO: {
                        name: deptForm.name,
                        buildingID: deptForm.buildingID,
                        existingRoomIds: deptForm.existingRoomIds,
                        newRooms: deptForm.rooms.map(r => ({ name: r.name, roomType: r.roomType, defaultPeopleCount: r.defaultPeopleCount })),
                    },
                });
                const newDept: DepartmentListDTO = {
                    id: res.data.id,
                    name: res.data.name,
                    buildingID: res.data.buildingID,
                    buildingName: buildings.find(b => b.id === res.data.buildingID)?.name,
                };
                setDepartments(prev => [...prev, newDept]);
                if (deptForm.existingRoomIds.length > 0 || deptForm.rooms.length > 0) fetchRooms();
                toast.current?.show({ severity: 'success', summary: 'Created', detail: 'Department created.', life: 3000 });
            } else {
                const res = await new DepartmentControllerApi().patchSpecificDepartment({
                    departmentId: editingDeptId!,
                    departmentEditWithRoomsDTO: {
                        name: deptForm.name,
                        buildingID: deptForm.buildingID,
                        roomIdsToDelete: deptForm.roomIdsToDelete,
                        existingRoomIdsToAssign: deptForm.existingRoomIds,
                        newRooms: deptForm.rooms.map(r => ({ name: r.name, roomType: r.roomType, defaultPeopleCount: r.defaultPeopleCount })),
                    },
                });
                setDepartments(prev => prev.map(d => d.id === res.data.id
                    ? { id: res.data.id, name: res.data.name, buildingID: res.data.buildingID, buildingName: buildings.find(b => b.id === res.data.buildingID)?.name }
                    : d
                ));
                const roomsChanged = deptForm.roomIdsToDelete.length > 0 || deptForm.existingRoomIds.length > 0 || deptForm.rooms.length > 0;
                if (roomsChanged) fetchRooms();
                toast.current?.show({ severity: 'success', summary: 'Saved', detail: 'Department updated.', life: 3000 });
            }
            setShowDeptEditDialog(false);
        } catch {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to save department.', life: 3000 });
        } finally {
            setDeptDialogLoading(false);
        }
    };

    const handleDeleteDepartment = (id?: string) => {
        if (!id) return;
        setConfirmDelete({
            message: 'Are you sure you want to delete this department? This action cannot be undone.',
            onConfirm: () => {
                setConfirmDelete(null);
                new DepartmentControllerApi().deleteSpecificDepartment({ departmentId: id })
                    .then(() => {
                        setDepartments(prev => prev.filter(d => d.id !== id));
                        toast.current?.show({ severity: 'success', summary: 'Deleted', detail: 'Department deleted.', life: 3000 });
                    })
                    .catch(() => toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to delete department.', life: 3000 }));
            },
        });
    };

    // --- Room handlers ---
    const openAddRoom = () => {
        setRoomForm(emptyRoomForm());
        setRoomFormErrors({});
        setIsNewRoom(true);
        setEditingRoomId(undefined);
        setShowRoomEditDialog(true);
    };

    const openEditRoom = (r: RoomDTO) => {
        setRoomForm({
            name: r.name ?? '',
            departmentID: r.departmentID ?? '',
            roomType: (r.roomType as unknown as RoomCreateDTORoomTypeEnum) ?? RoomCreateDTORoomTypeEnum.OFFICE,
            defaultPeopleCount: r.defaultPeopleCount ?? 1,
        });
        setRoomFormErrors({});
        setIsNewRoom(false);
        setEditingRoomId(r.id);
        setShowRoomEditDialog(true);
    };

    const handleSaveRoom = async () => {
        const errors: Partial<Record<keyof RoomFormState, string>> = {};
        if (!roomForm.name.trim()) errors.name = 'Required';
        if (!roomForm.departmentID) errors.departmentID = 'Required';
        if (Object.keys(errors).length > 0) { setRoomFormErrors(errors); return; }
        setRoomDialogLoading(true);
        try {
            if (isNewRoom) {
                const res = await new RoomControllerApi().createNewRoom({
                    roomCreateDTO: {
                        name: roomForm.name,
                        departmentID: roomForm.departmentID,
                        roomType: roomForm.roomType,
                        defaultPeopleCount: roomForm.defaultPeopleCount,
                    },
                });
                setRooms(prev => [...prev, res.data]);
                toast.current?.show({ severity: 'success', summary: 'Created', detail: 'Room created.', life: 3000 });
            } else {
                const res = await new RoomControllerApi().patchSpecificRoom({
                    roomId: editingRoomId!,
                    roomPatchDTO: {
                        name: roomForm.name,
                        departmentID: roomForm.departmentID,
                        roomType: roomForm.roomType as unknown as RoomPatchDTORoomTypeEnum,
                        defaultPeopleCount: roomForm.defaultPeopleCount,
                    },
                });
                setRooms(prev => prev.map(r => r.id === res.data.id ? res.data : r));
                toast.current?.show({ severity: 'success', summary: 'Saved', detail: 'Room updated.', life: 3000 });
            }
            setShowRoomEditDialog(false);
        } catch {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to save room.', life: 3000 });
        } finally {
            setRoomDialogLoading(false);
        }
    };

    const handleDeleteRoom = (id?: string) => {
        if (!id) return;
        setConfirmDelete({
            message: 'Are you sure you want to delete this room? This action cannot be undone.',
            onConfirm: () => {
                setConfirmDelete(null);
                new RoomControllerApi().deleteSpecificRoom({ roomId: id })
                    .then(() => {
                        setRooms(prev => prev.filter(r => r.id !== id));
                        toast.current?.show({ severity: 'success', summary: 'Deleted', detail: 'Room deleted.', life: 3000 });
                    })
                    .catch(() => toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to delete room.', life: 3000 }));
            },
        });
    };

    // --- User handlers ---
    const openAddUser = () => {
        setUserForm(emptyForm());
        setUserFormErrors({});
        setIsNewUser(true);
        setEditingUserId(undefined);
        setShowUserDialog(true);
    };

    const openEditUser = (user: FullUser) => {
        setUserForm({
            firstName: user.firstName ?? '',
            lastName: user.lastName ?? '',
            username: user.username ?? '',
            roomId: user.myRoom?.id ?? '',
            roleIds: user.roles.map(r => r.id),
            password: '',
            repeatPassword: '',
            enabled: user.enabled ?? true,
        });
        setUserFormErrors({});
        setIsNewUser(false);
        setEditingUserId(user.id);
        setShowUserDialog(true);
    };

    const handleSaveUser = async () => {
        const errors: Partial<Record<keyof UserFormState, string>> = {};
        if (!userForm.firstName.trim()) errors.firstName = 'Required';
        if (!userForm.lastName.trim()) errors.lastName = 'Required';
        if (!userForm.username.trim()) errors.username = 'Required';
        if (isNewUser) {
            if (!userForm.password.trim()) errors.password = 'Required';
            if (userForm.password !== userForm.repeatPassword) errors.repeatPassword = 'Passwords do not match';
        }
        if (userForm.roleIds.length === 0) errors.roleIds = 'At least one role required';
        if (Object.keys(errors).length > 0) { setUserFormErrors(errors); return; }
        setUserDialogLoading(true);
        try {
            if (isNewUser) {
                const res = await new UserxControllerApi().createNewUser({
                    userxCreateDTO: {
                        firstName: userForm.firstName,
                        lastName: userForm.lastName,
                        username: userForm.username,
                        enabled: userForm.enabled,
                        roles: userForm.roleIds,
                        password: userForm.password,
                        roomId: userForm.roomId || null,
                    } as any,
                });
                setUsers(prev => [...prev, res.data as any]);
                toast.current?.show({ severity: 'success', summary: 'Created', detail: 'User created successfully.', life: 3000 });
            } else {
                const res = await new UserxControllerApi().patchSpecificUser({
                    userId: editingUserId!,
                    userxPatchDTO: {
                        firstName: userForm.firstName,
                        lastName: userForm.lastName,
                        username: userForm.username,
                        isEnabled: userForm.enabled,
                        roles: userForm.roleIds as any,
                        roomId: userForm.roomId || null,
                    } as any,
                });
                setUsers(prev => prev.map(u => u.id === (res.data as any).id ? res.data as any : u));
                toast.current?.show({ severity: 'success', summary: 'Saved', detail: 'User updated successfully.', life: 3000 });
            }
            setShowUserDialog(false);
        } catch {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to save user.', life: 3000 });
        } finally {
            setUserDialogLoading(false);
        }
    };

    const handleDeleteUser = (user: FullUser) => {
        if (!user.id) return;
        setConfirmDelete({
            message: `Are you sure you want to delete user "${user.username}"? This action cannot be undone.`,
            onConfirm: () => {
                setConfirmDelete(null);
                new UserxControllerApi().deleteSpecificUser({ userId: user.id })
                    .then(() => {
                        setUsers(prev => prev.filter(u => u.id !== user.id));
                        toast.current?.show({ severity: 'success', summary: 'Deleted', detail: 'User deleted.', life: 3000 });
                    })
                    .catch(() => toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to delete user.', life: 3000 }));
            },
        });
    };

    // --- Weights handler ---
    const handleSaveWeights = async (w: KlimaScoreWeightsState) => {
        setWeightsSaving(true);
        try {
            const res = await new FormulaWeightsConfigurationApi().patchCurrentFormulaWeights({
                formulaWeightCreateDTO: {
                    tempWeight: w.temperature / 100,
                    humWeight:  w.humidity / 100,
                    co2Weight:  w.co2 / 100,
                },
            });
            const d = res.data;
            if (d.tempWeight != null && d.humWeight != null && d.co2Weight != null) {
                setWeights({
                    temperature: Math.round(d.tempWeight * 100),
                    humidity:    Math.round(d.humWeight * 100),
                    co2:         Math.round(d.co2Weight * 100),
                });
            }
            toast.current?.show({ severity: 'success', summary: 'Saved', detail: 'Klima score weights updated.', life: 3000 });
        } catch {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to save weights.', life: 3000 });
        } finally {
            setWeightsSaving(false);
        }
    };

    // --- Dropdown options ---
    const statusOptions = [{ label: 'Online', value: 'ONLINE' }, { label: 'Offline', value: 'OFFLINE' }];
    const roomTypeOptions = [{ label: 'Office', value: 'OFFICE' }, { label: 'Shared', value: 'SHARED' }];

    return (
        <div className="dashboard-layout">
            <Toast ref={toast}/>
            <PageHeader title="Dashboard" onMenuClick={() => setSidebarVisible(true)}/>
            <SidebarComponent visible={sidebarVisible} onHide={() => setSidebarVisible(false)}/>

            <div className="dashboard-content">

                {/* ── Raspberry Pi List ── */}
                <div className="table-container">
                    <TableHeader
                        title="Raspberry Pi List"
                        search={raspberrySearch}
                        onSearch={setRaspberrySearch}
                        searchPlaceholder="Search by name"
                        onAdd={openAddPiDialog}
                        addLabel="Add Raspberry Pi"
                        filterEl={
                            <Dropdown
                                value={raspberryStatusFilter}
                                options={statusOptions}
                                onChange={e => setRaspberryStatusFilter(e.value)}
                                placeholder="Status Filter"
                                showClear
                                style={{ borderRadius: '20px', minWidth: '160px' }}
                            />
                        }
                    />
                    <DataTable value={filteredRaspberries} stripedRows emptyMessage="No Raspberry Pis found." responsiveLayout="scroll">
                        <Column field="name" header="Name" sortable/>
                        <Column header="Room" body={(row: RaspberryDTOReal) =>
                            row.room?.roomName ?? <span style={{ color: '#9e9e9e' }}>N/A</span>}
                        />
                        <Column header="Sensors" body={(row: RaspberryDTOReal) => {
                            const count = sensors.filter(s => s.connectedToPiId === row.id).length;
                            return count > 0 ? <span>{count}</span> : <span style={{ color: '#9e9e9e' }}>None</span>;
                        }}/>
                        <Column header="Status" body={row => statusBadge(row.status)}/>
                        <Column
                            header=""
                            className="admin-actions-column admin-actions-column-wide"
                            headerClassName="admin-actions-column admin-actions-column-wide"
                            exportable={false}
                            body={(row: RaspberryDTOReal) => (
                                <div className="admin-table-actions">
                                    <Button icon="pi pi-refresh" rounded text severity="warning" title="Retry connection" onClick={() => handleRetryPiConnection(row.id)}/>
                                    <Button icon="pi pi-cog" rounded text severity="secondary" title="Edit Raspberry Pi" onClick={() => openEditPiDialog(row)}/>
                                    <Button icon="pi pi-trash" rounded text severity="danger" title="Delete Raspberry Pi" onClick={() => handleDeletePi(row.id)}/>
                                </div>
                            )}
                        />
                    </DataTable>
                </div>

                {/* ── Sensor Station List ── */}
                <div className="table-container">
                    <TableHeader
                        title="Sensor Station List"
                        search={sensorSearch}
                        onSearch={setSensorSearch}
                        searchPlaceholder="Search by name"
                        onAdd={openAddSensorDialog}
                        addLabel="Add Sensor Station"
                        filterEl={
                            <Dropdown
                                value={sensorStatusFilter}
                                options={statusOptions}
                                onChange={e => setSensorStatusFilter(e.value)}
                                placeholder="Status Filter"
                                showClear
                                style={{ borderRadius: '20px', minWidth: '160px' }}
                            />
                        }
                    />
                    <DataTable value={filteredSensors} stripedRows emptyMessage="No Sensor Stations found." responsiveLayout="scroll">
                        <Column field="name" header="Name" sortable/>
                        <Column header="Room" body={(row: SensorStationDTO) => {
                            const room = rooms.find(r => r.id === row.roomId);
                            return room ? (room.name ?? room.id) : <span style={{ color: '#9e9e9e' }}>N/A</span>;
                        }}/>
                        <Column header="Assigned Pi" body={(row: SensorStationDTO) => {
                            const pi = raspberries.find(p => p.id === row.connectedToPiId);
                            return pi ? (pi.name ?? pi.id) : <span style={{ color: '#9e9e9e' }}>None</span>;
                        }}/>
                        <Column header="Status" body={row => statusBadge(row.status)}/>
                        <Column
                            header=""
                            className="admin-actions-column admin-actions-column-wide"
                            headerClassName="admin-actions-column admin-actions-column-wide"
                            exportable={false}
                            body={(row: SensorStationDTO) => (
                                <div className="admin-table-actions">
                                    <Button icon="pi pi-refresh" rounded text severity="warning" title="Retry connection" onClick={() => handleRetrySensorConnection(row.readId!)}/>
                                    <Button icon="pi pi-cog" rounded text severity="secondary" title="Edit Sensor Station" onClick={() => openEditSensorDialog(row)}/>
                                    <Button icon="pi pi-trash" rounded text severity="danger" title="Delete Sensor Station" onClick={() => handleDeleteSensor(row.readId)}/>
                                </div>
                            )}
                        />
                    </DataTable>
                </div>

                {/* ── User List ── */}
                <div className="table-container">
                    <TableHeader
                        title="User List"
                        search={userSearch}
                        onSearch={setUserSearch}
                        searchPlaceholder="Search by last name"
                        onAdd={openAddUser}
                        addLabel="Add User"
                        filterEl={
                            <>
                                <Dropdown
                                    value={userRoleFilter}
                                    options={roleFilterOptions}
                                    onChange={e => setUserRoleFilter(e.value)}
                                    placeholder="Role Filter"
                                    showClear
                                    style={{ borderRadius: '20px', minWidth: '160px' }}
                                />
                                <Dropdown
                                    value={userRoomFilter}
                                    options={roomFilterOptions}
                                    onChange={e => setUserRoomFilter(e.value)}
                                    placeholder="Room Filter"
                                    showClear
                                    filter
                                    style={{ borderRadius: '20px', minWidth: '180px' }}
                                />
                            </>
                        }
                    />
                    <UserListComponent
                        users={filteredUsers}
                        loading={false}
                        onEditUser={openEditUser}
                        onDeleteUser={handleDeleteUser}
                        showDelete
                    />
                </div>

                {/* ── Departments ── */}
                <div className="table-container">
                    <TableHeader
                        title="Departments"
                        search={departmentSearch}
                        onSearch={setDepartmentSearch}
                        searchPlaceholder="Search by name"
                        onAdd={openAddDepartment}
                        addLabel="Add Department"
                        filterEl={
                            <Dropdown
                                value={departmentBuildingFilter}
                                options={buildingNameOptions}
                                onChange={e => setDepartmentBuildingFilter(e.value)}
                                placeholder="Building Filter"
                                showClear
                                style={{ borderRadius: '20px', minWidth: '180px' }}
                            />
                        }
                    />
                    <DataTable value={filteredDepartments} stripedRows emptyMessage="No departments found." responsiveLayout="scroll">
                        <Column field="name" header="Name" sortable/>
                        <Column field="buildingName" header="Building" sortable/>
                        <Column
                            header=""
                            className="admin-actions-column"
                            headerClassName="admin-actions-column"
                            exportable={false}
                            body={(row: DepartmentListDTO) => (
                                <div className="admin-table-actions">
                                    <Button icon="pi pi-cog" rounded text severity="secondary" title="Edit department" onClick={() => openEditDepartment(row)}/>
                                    <Button icon="pi pi-trash" rounded text severity="danger" title="Delete department" onClick={() => handleDeleteDepartment(row.id)}/>
                                </div>
                            )}
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
                        onAdd={openAddRoom}
                        addLabel="Add Room"
                        filterEl={
                            <Dropdown
                                value={roomTypeFilter}
                                options={roomTypeOptions}
                                onChange={e => setRoomTypeFilter(e.value)}
                                placeholder="Type Filter"
                                showClear
                                style={{ borderRadius: '20px', minWidth: '160px' }}
                            />
                        }
                    />
                    <DataTable value={filteredRooms} stripedRows emptyMessage="No rooms found." responsiveLayout="scroll" className="admin-rooms-table">
                        <Column field="name" header="Name" sortable/>
                        <Column field="departmentName" header="Department" sortable/>
                        <Column field="roomType" header="Type"/>
                        <Column field="defaultPeopleCount" header="Capacity" sortable/>
                        <Column
                            header=""
                            className="admin-actions-column"
                            headerClassName="admin-actions-column"
                            exportable={false}
                            body={(row: RoomDTO) => (
                                <div className="admin-table-actions">
                                    <Button icon="pi pi-cog" rounded text severity="secondary" title="Edit room" onClick={() => openEditRoom(row)}/>
                                    <Button icon="pi pi-trash" rounded text severity="danger" title="Delete room" onClick={() => handleDeleteRoom(row.id)}/>
                                </div>
                            )}
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
                        onAdd={openAddBuilding}
                        addLabel="Add Building"
                    />
                    <DataTable value={filteredBuildings} stripedRows emptyMessage="No buildings found." responsiveLayout="scroll">
                        <Column field="name" header="Name" sortable/>
                        <Column field="address" header="Address"/>
                        <Column
                            header=""
                            className="admin-actions-column"
                            headerClassName="admin-actions-column"
                            exportable={false}
                            body={(row: BuildingListDTO) => (
                                <div className="admin-table-actions">
                                    <Button icon="pi pi-cog" rounded text severity="secondary" title="Edit building" onClick={() => openEditBuilding(row)}/>
                                    <Button icon="pi pi-trash" rounded text severity="danger" title="Delete building" onClick={() => handleDeleteBuilding(row.id)}/>
                                </div>
                            )}
                        />
                    </DataTable>
                </div>

                {/* ── Klima Score Weights ── */}
                <div className="table-container">
                    <div className="flex-header">
                        <h3 style={{ margin: 0 }}>Klima Score Weights</h3>
                    </div>
                    {weights && (
                        <KlimaScoreWeights
                            weights={weights}
                            saving={weightsSaving}
                            onSave={handleSaveWeights}
                        />
                    )}
                </div>
            </div>

            <ConfirmDeleteDialog
                visible={confirmDelete !== null}
                message={confirmDelete?.message ?? ''}
                onConfirm={confirmDelete?.onConfirm ?? (() => {})}
                onHide={() => setConfirmDelete(null)}
            />

            <BuildingFormDialog
                visible={showBuildingEditDialog}
                isNew={isNewBuilding}
                form={buildingForm}
                formErrors={buildingFormErrors}
                loading={buildingDialogLoading}
                onHide={() => setShowBuildingEditDialog(false)}
                onSave={handleSaveBuilding}
                onChange={patch => setBuildingForm(f => ({ ...f, ...patch }))}
            />

            <DepartmentFormDialog
                visible={showDeptEditDialog}
                isNew={isNewDepartment}
                form={deptForm}
                formErrors={deptFormErrors}
                buildingOptions={buildingOptions}
                availableRooms={rooms}
                loading={deptDialogLoading}
                onHide={() => setShowDeptEditDialog(false)}
                onSave={handleSaveDepartment}
                onChange={patch => setDeptForm(f => ({ ...f, ...patch }))}
            />

            <RoomFormDialog
                visible={showRoomEditDialog}
                isNew={isNewRoom}
                form={roomForm}
                formErrors={roomFormErrors}
                departmentOptions={departmentOptions}
                loading={roomDialogLoading}
                onHide={() => setShowRoomEditDialog(false)}
                onSave={handleSaveRoom}
                onChange={patch => setRoomForm(f => ({ ...f, ...patch }))}
            />

            <UserFormDialog
                visible={showUserDialog}
                isNewUser={isNewUser}
                form={userForm}
                formErrors={userFormErrors}
                roomOptions={roomOptions}
                roleOptions={roleOptions}
                loading={userDialogLoading}
                onHide={() => setShowUserDialog(false)}
                onSave={handleSaveUser}
                onChange={patch => setUserForm(f => ({ ...f, ...patch }))}
            />

            {/* ── Add / Edit Raspberry Pi Dialog ── */}
            <Dialog
                header={editingPiId ? 'Edit Raspberry Pi' : 'Add Raspberry Pi'}
                visible={showPiDialog}
                style={{ width: '480px' }}
                onHide={() => setShowPiDialog(false)}
                footer={
                    <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
                        <Button label="Cancel" severity="secondary" outlined onClick={() => setShowPiDialog(false)}/>
                        <Button label={editingPiId ? 'Save' : 'Create'} icon="pi pi-check" loading={piLoading} onClick={handleSavePi}/>
                    </div>
                }
                draggable={false}
            >
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                    <div>
                        <label htmlFor="pi-name" style={labelStyle}>Name *</label>
                        <InputText id="pi-name" value={piName} onChange={e => setPiName(e.target.value)} placeholder="e.g. Pi-Lab-01" style={{ width: '100%' }}/>
                    </div>

                    <div>
                        <label htmlFor="pi-room" style={labelStyle}>Room {!editingPiId && '*'}</label>
                        <Dropdown
                            inputId="pi-room"
                            value={piRoomId}
                            options={editingPiId ? editAvailableRoomOptions : availableRoomOptions}
                            onChange={e => setPiRoomId(e.value)}
                            placeholder="Select room"
                            style={{ width: '100%' }}
                            filter
                        />
                    </div>

                    <div>
                        <label htmlFor="pi-ip" style={labelStyle}>IP Address *</label>
                        <InputText id="pi-ip" value={piIpAddress} onChange={e => setPiIpAddress(e.target.value)} placeholder="e.g. 192.168.1.10" style={{ width: '100%' }}/>
                    </div>

                    <div>
                        <label htmlFor="pi-port" style={labelStyle}>Port *</label>
                        <InputNumber inputId="pi-port" value={piPort} onValueChange={e => setPiPort(e.value ?? null)} placeholder="e.g. 1000–9999" style={{ width: '100%' }} min={1000} max={9999} useGrouping={false}/>
                    </div>

                    <div>
                        <label htmlFor="pi-interval" style={labelStyle}>Pushing Data Interval (seconds) {!editingPiId && '*'}</label>
                        <InputNumber inputId="pi-interval" value={piInterval} onValueChange={e => setPiInterval(e.value ?? null)} placeholder="e.g. 60" style={{ width: '100%' }} min={1}/>
                    </div>

                    {editingPiId && (
                        <div>
                            <label style={labelStyle}>Connected Sensor Stations</label>
                            <DataTable value={sensors.filter(s => s.connectedToPiId === editingPiId)} size="small" emptyMessage="No sensor stations connected.">
                                <Column field="name" header="Name"/>
                                <Column field="writeId" header="Write ID" style={{ maxWidth: '9rem', overflow: 'hidden', textOverflow: 'ellipsis' }}/>
                                <Column field="readId" header="Read ID" style={{ maxWidth: '9rem', overflow: 'hidden', textOverflow: 'ellipsis' }}/>
                                <Column
                                    header=""
                                    className="admin-actions-column"
                                    headerClassName="admin-actions-column"
                                    body={(row: SensorStationDTO) => (
                                        <div className="admin-table-actions">
                                            <Button
                                                icon="pi pi-trash"
                                                rounded text severity="danger"
                                                title="Remove from this Raspberry Pi"
                                                onClick={() => setConfirmDelete({
                                                    message: `Remove "${row.name ?? row.readId}" from this Raspberry Pi?`,
                                                    onConfirm: () => { setConfirmDelete(null); handleDisconnectSensor(row.readId!); },
                                                })}
                                            />
                                        </div>
                                    )}
                                />
                            </DataTable>
                        </div>
                    )}
                </div>
            </Dialog>

            {/* ── Add / Edit Sensor Station Dialog ── */}
            <Dialog
                header={editingSensorId ? 'Edit Sensor Station' : 'Add Sensor Station'}
                visible={showSensorDialog}
                style={{ width: '480px' }}
                onHide={() => setShowSensorDialog(false)}
                footer={
                    <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
                        <Button label="Cancel" severity="secondary" outlined onClick={() => setShowSensorDialog(false)}/>
                        <Button label={editingSensorId ? 'Save' : 'Create'} icon="pi pi-check" loading={sensorLoading} onClick={handleSaveSensor}/>
                    </div>
                }
                draggable={false}
            >
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                    <div>
                        <label htmlFor="sensor-name" style={labelStyle}>Name *</label>
                        <InputText id="sensor-name" value={sensorName} onChange={e => setSensorName(e.target.value)} placeholder="e.g. Station-A1" style={{ width: '100%' }}/>
                    </div>
                    <div>
                        <label htmlFor="sensor-room" style={labelStyle}>Room *</label>
                        <Dropdown inputId="sensor-room" value={sensorRoomId} options={roomOptions} onChange={e => setSensorRoomId(e.value)} placeholder="Select room" style={{ width: '100%' }} filter/>
                    </div>
                    {editingSensorId && (
                        <>
                            <div>
                                <label style={labelStyle}>Read ID</label>
                                <InputText value={String(editingSensorId)} readOnly style={{ width: '100%', fontFamily: 'monospace', fontSize: '0.85rem', background: '#f5f5f5', color: '#555' }}/>
                            </div>
                            <div>
                                <label style={labelStyle}>Write ID</label>
                                <InputText value={editingSensorWriteId ? String(editingSensorWriteId) : ''} readOnly style={{ width: '100%', fontFamily: 'monospace', fontSize: '0.85rem', background: '#f5f5f5', color: '#555' }}/>
                            </div>
                        </>
                    )}
                </div>
            </Dialog>
        </div>
    );
};

export default SysAdminDashboard;
