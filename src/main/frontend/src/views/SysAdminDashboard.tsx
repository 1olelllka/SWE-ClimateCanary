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
    BuildingControllerApi,
    DepartmentControllerApi,
    UserRoleControllerApi,
    RaspberryDTO,
    SensorStationDTO,
    RoomDTO,
    BuildingListDTO,
    DepartmentListDTO,
    UserRoleDTO,
    RoomCreateDTORoomTypeEnum,
    RoomPatchDTORoomTypeEnum,
} from '../generated-skeleton-api';
import BuildingFormDialog, { BuildingFormState, emptyBuildingForm } from '../components/BuildingFormDialog';
import DepartmentFormDialog, { DepartmentFormState, emptyDepartmentForm } from '../components/DepartmentFormDialog';
import RoomFormDialog, { RoomFormState, emptyRoomForm } from '../components/RoomFormDialog';
import UserFormDialog, { UserFormState, emptyForm } from '../components/UserFormDialog';
import globalAxios from 'axios';
import axios from 'axios';
import { BASE_PATH } from '../generated-skeleton-api/base';
import '../styles/Tables.css';

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

const PAGEABLE = { page: 0, size: 100, sort: [] };
const PREVIEW_ROWS = 3;

const statusBadge = (status?: string) => {
    const online = status === 'ONLINE';
    return (
        <span style={{
            background: online ? '#4caf50' : '#9e9e9e',
            color: 'white', padding: '2px 10px', borderRadius: '12px',
            fontSize: '0.8rem', fontWeight: 500,
        }}>
            {status ?? 'N/A'}
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
    const toast = useRef<Toast>(null);
    const [sidebarVisible, setSidebarVisible] = useState(false);

    // --- Data ---
    const [raspberries, setRaspberries] = useState<RaspberryDTOReal[]>([]);
    const [sensors, setSensors] = useState<SensorStationDTO[]>([]);
    const [users, setUsers] = useState<FullUser[]>([]);
    const [rooms, setRooms] = useState<RoomDTO[]>([]);
    const [buildings, setBuildings] = useState<BuildingListDTO[]>([]);
    const [departments, setDepartments] = useState<DepartmentListDTO[]>([]);
    const [roleDTOs, setRoleDTOs] = useState<UserRoleDTO[]>([]);

    // --- Pi dialog ---
    const [showPiDialog, setShowPiDialog] = useState(false);
    const [editingPiId, setEditingPiId] = useState<string | null>(null);
    const [piLoading, setPiLoading] = useState(false);
    const [piName, setPiName] = useState('');
    const [piRoomId, setPiRoomId] = useState('');
    const [piIpAddress, setPiIpAddress] = useState('');
    const [piPort, setPiPort] = useState<number | null>(8080);
    const [piInterval, setPiInterval] = useState<number | null>(null);

    // --- Sensor dialog ---
    const [showSensorDialog, setShowSensorDialog] = useState(false);
    const [editingSensorId, setEditingSensorId] = useState<string | null>(null);
    const [sensorLoading, setSensorLoading] = useState(false);
    const [sensorName, setSensorName] = useState('');
    const [sensorRoomId, setSensorRoomId] = useState('');

    // --- Building edit dialog ---
    const [showBuildingEditDialog, setShowBuildingEditDialog] = useState(false);
    const [editingBuildingId, setEditingBuildingId] = useState<string | undefined>();
    const [buildingForm, setBuildingForm] = useState<BuildingFormState>(emptyBuildingForm());
    const [buildingFormErrors, setBuildingFormErrors] = useState<Partial<Record<keyof BuildingFormState, string>>>({});
    const [buildingDialogLoading, setBuildingDialogLoading] = useState(false);

    // --- Department edit dialog ---
    const [showDeptEditDialog, setShowDeptEditDialog] = useState(false);
    const [editingDeptId, setEditingDeptId] = useState<string | undefined>();
    const [deptForm, setDeptForm] = useState<DepartmentFormState>(emptyDepartmentForm());
    const [deptFormErrors, setDeptFormErrors] = useState<Partial<Record<keyof DepartmentFormState, string>>>({});
    const [deptDialogLoading, setDeptDialogLoading] = useState(false);

    // --- Room edit dialog ---
    const [showRoomEditDialog, setShowRoomEditDialog] = useState(false);
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
    const [roomTypeFilter, setRoomTypeFilter] = useState<string | null>(null);
    const [departmentBuildingFilter, setDepartmentBuildingFilter] = useState<string | null>(null);

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
        fetchRaspberries();
        refreshSensors();
        globalAxios.get<{ content: FullUser[] }>('/api/users?size=100')
            .then(res => setUsers(res.data.content ?? [])).catch(() => {});
        new UserRoleControllerApi().getAllPermissions()
            .then(res => setRoleDTOs(res.data ?? [])).catch(() => {});
        fetchRooms();
        new BuildingControllerApi().getPageOfBuildings({ pageable: PAGEABLE })
            .then(res => setBuildings(res.data.content ?? [])).catch(() => {});
        fetchDepartments();
    }, []);

    // --- Filtered data (capped at PREVIEW_ROWS) ---
    const filterList = <T extends Record<string, any>>(
        data: T[], nameKey: string, search: string, filterKey?: string, filterValue?: string | null
    ): T[] =>
        data.filter(item => {
            if (search && !(item[nameKey] ?? '').toString().toLowerCase().includes(search.toLowerCase())) return false;
            if (filterKey && filterValue != null && String(item[filterKey]) !== filterValue) return false;
            return true;
        }).slice(0, PREVIEW_ROWS);

    const filteredRaspberries  = filterList(raspberries,  'name',     raspberrySearch,   'status',      raspberryStatusFilter);
    const filteredSensors      = filterList(sensors,      'name',     sensorSearch,      'status',      sensorStatusFilter);
    const filteredUsers        = users.filter(u => {
        if (userSearch && !(u.username ?? '').toLowerCase().includes(userSearch.toLowerCase())) return false;
        if (userRoleFilter && !u.roles.some(r => r.name === userRoleFilter)) return false;
        return true;
    }).slice(0, PREVIEW_ROWS);
    const filteredRooms        = filterList(rooms,        'name',     roomSearch,        'roomType',    roomTypeFilter);
    const filteredBuildings    = filterList(buildings,    'name',     buildingSearch);
    const filteredDepartments  = filterList(departments,  'name',     departmentSearch,  'buildingName', departmentBuildingFilter);

    const buildingNameOptions = [...new Set(departments.map(d => d.buildingName).filter(Boolean))] as string[];
    const buildingOptions     = buildings.map(b => ({ label: b.name ?? b.id ?? '', value: b.id ?? '' }));
    const departmentOptions   = departments.map(d => ({
        label: d.buildingName ? `${d.name ?? ''} (${d.buildingName})` : (d.name ?? d.id ?? ''),
        value: d.id ?? '',
    }));
    const roomOptions         = rooms.map(r => ({ label: r.name ?? r.id ?? '', value: r.id ?? '' }));
    const roleOptions         = roleDTOs.map(r => ({ label: r.name ?? '', value: r.id ?? '' }));

    // --- Pi handlers ---
    const openEditPiDialog = (row: RaspberryDTOReal) => {
        setEditingPiId(row.id ?? null);
        setPiName(row.name ?? '');
        setPiRoomId(row.room?.roomId ?? '');
        setPiIpAddress(row.ipAddress ?? '');
        setPiPort(row.port ?? 8080);
        setPiInterval(null);
        setShowPiDialog(true);
    };

    const handleSavePi = async () => {
        if (!piName || !piIpAddress || piPort == null) {
            toast.current?.show({ severity: 'warn', summary: 'Validation', detail: 'Name, IP address, and port are required.', life: 3000 });
            return;
        }
        setPiLoading(true);
        try {
            await new RaspberryControllerApi().patchSpecificRaspberry({
                raspberryId: editingPiId!,
                raspberryPatchDTO: { name: piName, ipAddress: piIpAddress, port: piPort, frequency: piInterval ?? undefined },
            });
            toast.current?.show({ severity: 'success', summary: 'Saved', detail: 'Raspberry Pi updated successfully.', life: 3000 });
            setShowPiDialog(false);
            fetchRaspberries();
        } catch {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to update Raspberry Pi.', life: 3000 });
        } finally {
            setPiLoading(false);
        }
    };

    const handleDeletePi = async (id?: string) => {
        if (!id || !globalThis.confirm('Delete this Raspberry Pi?')) return;
        try {
            await new RaspberryControllerApi().deleteSpecificRaspberry({ raspberryId: id });
            setRaspberries(prev => prev.filter(p => p.id !== id));
            setSensors(prev => prev.map(s => s.connectedToPiId === id ? { ...s, connectedToPiId: undefined } : s));
            toast.current?.show({ severity: 'success', summary: 'Deleted', detail: 'Raspberry Pi deleted.', life: 3000 });
        } catch {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to delete Raspberry Pi.', life: 3000 });
        }
    };

    const handleDisconnectSensor = async (sensorId: string) => {
        try {
            await axios.delete(`${BASE_PATH}/api/sensor-stations/${sensorId}/room`);
            toast.current?.show({ severity: 'success', summary: 'Disconnected', detail: 'Sensor station disconnected from Pi.', life: 3000 });
            refreshSensors();
        } catch {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to disconnect sensor station.', life: 3000 });
        }
    };

    // --- Sensor handlers ---
    const openEditSensorDialog = (row: SensorStationDTO) => {
        setEditingSensorId(row.readId ?? null);
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
            await new SensorStationControllerApi().patchExistingSensorStation({
                sensorId: editingSensorId!,
                sensorStationPatchDTO: { name: sensorName, roomId: sensorRoomId },
            });
            toast.current?.show({ severity: 'success', summary: 'Saved', detail: 'Sensor Station updated successfully.', life: 3000 });
            setShowSensorDialog(false);
            refreshSensors();
        } catch {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to update Sensor Station.', life: 3000 });
        } finally {
            setSensorLoading(false);
        }
    };

    const handleDeleteSensor = async (id?: string) => {
        if (!id || !globalThis.confirm('Delete this Sensor Station?')) return;
        try {
            await new SensorStationControllerApi().removeSpecificSensor({ sensorId: id });
            setSensors(prev => prev.filter(s => s.readId !== id));
            toast.current?.show({ severity: 'success', summary: 'Deleted', detail: 'Sensor Station deleted.', life: 3000 });
        } catch {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to delete Sensor Station.', life: 3000 });
        }
    };

    // --- Building handlers ---
    const openEditBuilding = (b: BuildingListDTO) => {
        setBuildingForm({ name: b.name ?? '', address: b.address ?? '' });
        setBuildingFormErrors({});
        setEditingBuildingId(b.id);
        setShowBuildingEditDialog(true);
    };

    const handleSaveBuilding = async () => {
        const errors: Partial<Record<keyof BuildingFormState, string>> = {};
        if (!buildingForm.name.trim()) errors.name = 'Required';
        if (Object.keys(errors).length > 0) { setBuildingFormErrors(errors); return; }
        setBuildingDialogLoading(true);
        try {
            const res = await new BuildingControllerApi().patchSpecificBuilding({
                buildingId: editingBuildingId!,
                buildingCreateDTO: { name: buildingForm.name, address: buildingForm.address },
            });
            setBuildings(prev => prev.map(b => b.id === res.data.id ? { ...b, name: res.data.name, address: res.data.address } : b));
            setShowBuildingEditDialog(false);
            toast.current?.show({ severity: 'success', summary: 'Saved', detail: 'Building updated.', life: 3000 });
        } catch {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to update building.', life: 3000 });
        } finally {
            setBuildingDialogLoading(false);
        }
    };

    const handleDeleteBuilding = async (id?: string) => {
        if (!id || !globalThis.confirm('Delete this building?')) return;
        try {
            await new BuildingControllerApi().deleteSpecificBuilding({ buildingId: id });
            setBuildings(prev => prev.filter(b => b.id !== id));
            toast.current?.show({ severity: 'success', summary: 'Deleted', detail: 'Building deleted.', life: 3000 });
        } catch {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to delete building.', life: 3000 });
        }
    };

    // --- Department handlers ---
    const openEditDepartment = (d: DepartmentListDTO) => {
        const currentRoomIds = rooms.filter(r => r.departmentID === d.id && r.id).map(r => r.id!);
        setDeptForm({ name: d.name ?? '', buildingID: d.buildingID ?? '', currentRoomIds, existingRoomIds: [], rooms: [], roomIdsToDelete: [] });
        setDeptFormErrors({});
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
                ? { ...d, name: res.data.name, buildingID: res.data.buildingID, buildingName: buildings.find(b => b.id === res.data.buildingID)?.name }
                : d
            ));
            const roomsChanged = deptForm.roomIdsToDelete.length > 0 || deptForm.existingRoomIds.length > 0 || deptForm.rooms.length > 0;
            if (roomsChanged) fetchRooms();
            setShowDeptEditDialog(false);
            toast.current?.show({ severity: 'success', summary: 'Saved', detail: 'Department updated.', life: 3000 });
        } catch {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to update department.', life: 3000 });
        } finally {
            setDeptDialogLoading(false);
        }
    };

    const handleDeleteDepartment = async (id?: string) => {
        if (!id || !globalThis.confirm('Delete this department?')) return;
        try {
            await new DepartmentControllerApi().deleteSpecificDepartment({ departmentId: id });
            setDepartments(prev => prev.filter(d => d.id !== id));
            toast.current?.show({ severity: 'success', summary: 'Deleted', detail: 'Department deleted.', life: 3000 });
        } catch {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to delete department.', life: 3000 });
        }
    };

    // --- Room handlers ---
    const openEditRoom = (r: RoomDTO) => {
        setRoomForm({
            name: r.name ?? '',
            departmentID: r.departmentID ?? '',
            roomType: (r.roomType as unknown as RoomCreateDTORoomTypeEnum) ?? RoomCreateDTORoomTypeEnum.OFFICE,
            defaultPeopleCount: r.defaultPeopleCount ?? 1,
        });
        setRoomFormErrors({});
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
            const res = await new RoomControllerApi().patchSpecificRoom({
                roomId: editingRoomId!,
                roomPatchDTO: {
                    roomNumber: roomForm.name,
                    departmentID: roomForm.departmentID,
                    roomType: roomForm.roomType as unknown as RoomPatchDTORoomTypeEnum,
                    defaultPeopleCount: roomForm.defaultPeopleCount,
                },
            });
            setRooms(prev => prev.map(r => r.id === res.data.id ? { ...r, ...res.data } : r));
            setShowRoomEditDialog(false);
            toast.current?.show({ severity: 'success', summary: 'Saved', detail: 'Room updated.', life: 3000 });
        } catch {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to update room.', life: 3000 });
        } finally {
            setRoomDialogLoading(false);
        }
    };

    const handleDeleteRoom = async (id?: string) => {
        if (!id || !globalThis.confirm('Delete this room?')) return;
        try {
            await new RoomControllerApi().deleteSpecificRoom({ roomId: id });
            setRooms(prev => prev.filter(r => r.id !== id));
            toast.current?.show({ severity: 'success', summary: 'Deleted', detail: 'Room deleted.', life: 3000 });
        } catch {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to delete room.', life: 3000 });
        }
    };

    // --- User handlers ---
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
        if (userForm.roleIds.length === 0) errors.roleIds = 'At least one role required';
        if (Object.keys(errors).length > 0) { setUserFormErrors(errors); return; }
        setUserDialogLoading(true);
        try {
            const res = await globalAxios.patch<FullUser>(`/api/users/${editingUserId}`, {
                firstName: userForm.firstName,
                lastName: userForm.lastName,
                username: userForm.username,
                isEnabled: userForm.enabled,
                roles: userForm.roleIds,
                roomId: userForm.roomId || null,
            });
            setUsers(prev => prev.map(u => u.id === res.data.id ? res.data : u));
            toast.current?.show({ severity: 'success', summary: 'Saved', detail: 'User updated successfully.', life: 3000 });
            setShowUserDialog(false);
        } catch {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to save user.', life: 3000 });
        } finally {
            setUserDialogLoading(false);
        }
    };

    const handleDeleteUser = (user: FullUser) => {
        if (!user.id || !globalThis.confirm(`Delete user "${user.username}"?`)) return;
        globalAxios.delete(`/api/users/${user.id}`)
            .then(() => {
                setUsers(prev => prev.filter(u => u.id !== user.id));
                toast.current?.show({ severity: 'success', summary: 'Deleted', detail: 'User deleted.', life: 3000 });
            })
            .catch(() => toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to delete user.', life: 3000 }));
    };

    // --- Dropdown options ---
    const statusOptions      = [{ label: 'Online', value: 'ONLINE' }, { label: 'Offline', value: 'OFFLINE' }];
    const roomTypeOptions    = [{ label: 'Office', value: 'OFFICE' }, { label: 'Shared', value: 'SHARED' }];
    const roleFilterOptions  = roleDTOs.map(r => ({ label: r.name ?? '', value: r.name ?? '' }));

    return (
        <div className="dashboard-layout">
            <Toast ref={toast} />
            <PageHeader title="SysAdmin Dashboard" onMenuClick={() => setSidebarVisible(true)} />
            <SidebarComponent visible={sidebarVisible} onHide={() => setSidebarVisible(false)} />

            <div className="dashboard-content">

                {/* ── Raspberry Pi List ── */}
                <div className="table-container">
                    <TableHeader title="Raspberry Pi List" search={raspberrySearch} onSearch={setRaspberrySearch} searchPlaceholder="Search by name"
                        filterEl={<Dropdown value={raspberryStatusFilter} options={statusOptions} onChange={e => setRaspberryStatusFilter(e.value)} placeholder="Status Filter" showClear style={{ borderRadius: '20px', minWidth: '160px' }} />}
                    />
                    <DataTable value={filteredRaspberries} stripedRows emptyMessage="No Raspberry Pis found." responsiveLayout="scroll">
                        <Column field="id" header="ID" style={{ maxWidth: '10rem', overflow: 'hidden', textOverflow: 'ellipsis' }} />
                        <Column field="name" header="Name" sortable />
                        <Column header="Room" body={(row: RaspberryDTOReal) => row.room?.roomName ?? <span style={{ color: '#9e9e9e' }}>N/A</span>} />
                        <Column field="ipAddress" header="IP Address" />
                        <Column field="port" header="Port" style={{ width: '6rem' }} />
                        <Column header="Sensor Stations" body={(row: RaspberryDTOReal) => {
                            const count = sensors.filter(s => s.connectedToPiId === row.id).length;
                            return count > 0
                                ? <span>{count} station{count !== 1 ? 's' : ''}</span>
                                : <span style={{ color: '#9e9e9e' }}>None</span>;
                        }} />
                        <Column header="Status" body={row => statusBadge(row.status)} />
                        <Column header="" style={{ width: '6rem' }} exportable={false} body={(row: RaspberryDTOReal) => (
                            <div style={{ display: 'flex', gap: '0.25rem', justifyContent: 'flex-end' }}>
                                <Button icon="pi pi-cog" rounded text severity="secondary" title="Edit Raspberry Pi" onClick={() => openEditPiDialog(row)} />
                                <Button icon="pi pi-trash" rounded text severity="danger" title="Delete Raspberry Pi" onClick={() => handleDeletePi(row.id)} />
                            </div>
                        )} />
                    </DataTable>
                </div>

                {/* ── Sensor Station List ── */}
                <div className="table-container">
                    <TableHeader title="Sensor Station List" search={sensorSearch} onSearch={setSensorSearch} searchPlaceholder="Search by name"
                        filterEl={<Dropdown value={sensorStatusFilter} options={statusOptions} onChange={e => setSensorStatusFilter(e.value)} placeholder="Status Filter" showClear style={{ borderRadius: '20px', minWidth: '160px' }} />}
                    />
                    <DataTable value={filteredSensors} stripedRows emptyMessage="No Sensor Stations found." responsiveLayout="scroll">
                        <Column field="readId" header="Read ID" style={{ maxWidth: '10rem', overflow: 'hidden', textOverflow: 'ellipsis' }} />
                        <Column field="name" header="Name" sortable />
                        <Column header="Room" body={(row: SensorStationDTO) => {
                            const room = rooms.find(r => r.id === row.roomId);
                            return room ? (room.name ?? room.id) : <span style={{ color: '#9e9e9e' }}>N/A</span>;
                        }} />
                        <Column header="Assigned To" body={(row: SensorStationDTO) => {
                            const pi = raspberries.find(p => p.id === row.connectedToPiId);
                            return pi ? (pi.name ?? pi.id) : <span style={{ color: '#9e9e9e' }}>None</span>;
                        }} />
                        <Column header="Status" body={row => statusBadge(row.status)} />
                        <Column header="Last Measurement" body={() => <span style={{ color: '#9e9e9e' }}>N/A</span>} />
                        <Column header="" style={{ width: '6rem' }} exportable={false} body={(row: SensorStationDTO) => (
                            <div style={{ display: 'flex', gap: '0.25rem', justifyContent: 'flex-end' }}>
                                <Button icon="pi pi-cog" rounded text severity="secondary" title="Edit Sensor Station" onClick={() => openEditSensorDialog(row)} />
                                <Button icon="pi pi-trash" rounded text severity="danger" title="Delete Sensor Station" onClick={() => handleDeleteSensor(row.readId)} />
                            </div>
                        )} />
                    </DataTable>
                </div>

                {/* ── User List ── */}
                <div className="table-container">
                    <TableHeader title="User List" search={userSearch} onSearch={setUserSearch} searchPlaceholder="Search by username"
                        filterEl={<Dropdown value={userRoleFilter} options={roleFilterOptions} onChange={e => setUserRoleFilter(e.value)} placeholder="Role Filter" showClear style={{ borderRadius: '20px', minWidth: '160px' }} />}
                    />
                    <DataTable value={filteredUsers} stripedRows emptyMessage="No users found." responsiveLayout="scroll">
                        <Column field="id" header="ID" style={{ maxWidth: '10rem', overflow: 'hidden', textOverflow: 'ellipsis' }} />
                        <Column field="firstName" header="First Name" sortable />
                        <Column field="lastName" header="Last Name" sortable />
                        <Column header="Room" body={(u: FullUser) => u.myRoom
                            ? <span>{u.myRoom.roomNumber} ({u.myRoom.departmentName})</span>
                            : <span style={{ color: '#9e9e9e' }}>N/A</span>
                        } />
                        <Column header="Roles" body={(u: FullUser) => <span>{u.roles.map(r => r.name).join(', ') || '—'}</span>} />
                        <Column header="" style={{ width: '6rem' }} exportable={false} body={(u: FullUser) => (
                            <div style={{ display: 'flex', gap: '0.25rem', justifyContent: 'flex-end' }}>
                                <Button icon="pi pi-pencil" rounded text severity="secondary" onClick={() => openEditUser(u)} title="Edit user" />
                                <Button icon="pi pi-trash" rounded text severity="danger" onClick={() => handleDeleteUser(u)} title="Delete user" />
                            </div>
                        )} />
                    </DataTable>
                </div>

                {/* ── Departments ── */}
                <div className="table-container">
                    <TableHeader title="Departments" search={departmentSearch} onSearch={setDepartmentSearch} searchPlaceholder="Search by name"
                        filterEl={<Dropdown value={departmentBuildingFilter} options={buildingNameOptions} onChange={e => setDepartmentBuildingFilter(e.value)} placeholder="Building Filter" showClear style={{ borderRadius: '20px', minWidth: '180px' }} />}
                    />
                    <DataTable value={filteredDepartments} stripedRows emptyMessage="No departments found." responsiveLayout="scroll">
                        <Column field="id" header="ID" style={{ maxWidth: '10rem', overflow: 'hidden', textOverflow: 'ellipsis' }} />
                        <Column field="name" header="Name" sortable />
                        <Column field="buildingName" header="Building" />
                        <Column header="" style={{ width: '6rem' }} exportable={false} body={(row: DepartmentListDTO) => (
                            <div style={{ display: 'flex', gap: '0.25rem', justifyContent: 'flex-end' }}>
                                <Button icon="pi pi-cog" rounded text severity="secondary" title="Edit department" onClick={() => openEditDepartment(row)} />
                                <Button icon="pi pi-trash" rounded text severity="danger" title="Delete department" onClick={() => handleDeleteDepartment(row.id)} />
                            </div>
                        )} />
                    </DataTable>
                </div>

                {/* ── Rooms ── */}
                <div className="table-container">
                    <TableHeader title="Rooms" search={roomSearch} onSearch={setRoomSearch} searchPlaceholder="Search by name"
                        filterEl={<Dropdown value={roomTypeFilter} options={roomTypeOptions} onChange={e => setRoomTypeFilter(e.value)} placeholder="Type Filter" showClear style={{ borderRadius: '20px', minWidth: '160px' }} />}
                    />
                    <DataTable value={filteredRooms} stripedRows emptyMessage="No rooms found." responsiveLayout="scroll">
                        <Column field="id" header="ID" style={{ maxWidth: '10rem', overflow: 'hidden', textOverflow: 'ellipsis' }} />
                        <Column field="name" header="Name" sortable />
                        <Column field="departmentName" header="Department" sortable />
                        <Column field="roomType" header="Type" />
                        <Column field="defaultPeopleCount" header="Capacity" sortable />
                        <Column header="" style={{ width: '6rem' }} exportable={false} body={(row: RoomDTO) => (
                            <div style={{ display: 'flex', gap: '0.25rem', justifyContent: 'flex-end' }}>
                                <Button icon="pi pi-cog" rounded text severity="secondary" title="Edit room" onClick={() => openEditRoom(row)} />
                                <Button icon="pi pi-trash" rounded text severity="danger" title="Delete room" onClick={() => handleDeleteRoom(row.id)} />
                            </div>
                        )} />
                    </DataTable>
                </div>

                {/* ── Buildings ── */}
                <div className="table-container">
                    <TableHeader title="Buildings" search={buildingSearch} onSearch={setBuildingSearch} searchPlaceholder="Search by name" />
                    <DataTable value={filteredBuildings} stripedRows emptyMessage="No buildings found." responsiveLayout="scroll">
                        <Column field="id" header="ID" style={{ maxWidth: '10rem', overflow: 'hidden', textOverflow: 'ellipsis' }} />
                        <Column field="name" header="Name" sortable />
                        <Column field="address" header="Address" />
                        <Column header="" style={{ width: '6rem' }} exportable={false} body={(row: BuildingListDTO) => (
                            <div style={{ display: 'flex', gap: '0.25rem', justifyContent: 'flex-end' }}>
                                <Button icon="pi pi-cog" rounded text severity="secondary" title="Edit building" onClick={() => openEditBuilding(row)} />
                                <Button icon="pi pi-trash" rounded text severity="danger" title="Delete building" onClick={() => handleDeleteBuilding(row.id)} />
                            </div>
                        )} />
                    </DataTable>
                </div>

            </div>

            <BuildingFormDialog
                visible={showBuildingEditDialog}
                isNew={false}
                form={buildingForm}
                formErrors={buildingFormErrors}
                loading={buildingDialogLoading}
                onHide={() => setShowBuildingEditDialog(false)}
                onSave={handleSaveBuilding}
                onChange={patch => setBuildingForm(f => ({ ...f, ...patch }))}
            />

            <DepartmentFormDialog
                visible={showDeptEditDialog}
                isNew={false}
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
                isNew={false}
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

            {/* ── Edit Raspberry Pi Dialog ── */}
            <Dialog
                header="Edit Raspberry Pi"
                visible={showPiDialog}
                style={{ width: '480px' }}
                onHide={() => setShowPiDialog(false)}
                footer={
                    <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
                        <Button label="Cancel" severity="secondary" outlined onClick={() => setShowPiDialog(false)} />
                        <Button label="Save" icon="pi pi-check" loading={piLoading} onClick={handleSavePi} />
                    </div>
                }
                draggable={false}
            >
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                    <div>
                        <label htmlFor="pi-name" style={labelStyle}>Name *</label>
                        <InputText id="pi-name" value={piName} onChange={e => setPiName(e.target.value)} placeholder="e.g. Pi-Lab-01" style={{ width: '100%' }} />
                    </div>
                    <div>
                        <label style={labelStyle}>Room</label>
                        <InputText
                            value={rooms.find(r => r.id === piRoomId)?.name ?? piRoomId ?? 'N/A'}
                            readOnly
                            style={{ width: '100%', background: '#f5f5f5', cursor: 'default' }}
                        />
                    </div>
                    <div>
                        <label htmlFor="pi-ip" style={labelStyle}>IP Address *</label>
                        <InputText id="pi-ip" value={piIpAddress} onChange={e => setPiIpAddress(e.target.value)} placeholder="e.g. 192.168.1.10" style={{ width: '100%' }} />
                    </div>
                    <div>
                        <label htmlFor="pi-port" style={labelStyle}>Port *</label>
                        <InputNumber inputId="pi-port" value={piPort} onValueChange={e => setPiPort(e.value ?? null)} placeholder="e.g. 1000–9999" style={{ width: '100%' }} min={1000} max={9999} useGrouping={false} />
                    </div>
                    <div>
                        <label htmlFor="pi-interval" style={labelStyle}>Pushing Data Interval (seconds)</label>
                        <InputNumber inputId="pi-interval" value={piInterval} onValueChange={e => setPiInterval(e.value ?? null)} placeholder="e.g. 60" style={{ width: '100%' }} min={1} />
                    </div>
                    <div>
                        <label style={labelStyle}>Connected Sensor Stations</label>
                        <DataTable
                            value={sensors.filter(s => s.connectedToPiId === editingPiId)}
                            size="small"
                            emptyMessage="No sensor stations connected."
                        >
                            <Column field="name" header="Name" />
                            <Column field="writeId" header="Write ID" style={{ maxWidth: '9rem', overflow: 'hidden', textOverflow: 'ellipsis' }} />
                            <Column field="readId" header="Read ID" style={{ maxWidth: '9rem', overflow: 'hidden', textOverflow: 'ellipsis' }} />
                            <Column header="" style={{ width: '3.5rem' }} body={(row: SensorStationDTO) => (
                                <Button
                                    icon="pi pi-trash"
                                    rounded
                                    text
                                    severity="danger"
                                    title="Remove from this Raspberry Pi"
                                    onClick={() => {
                                        if (globalThis.confirm(`Remove "${row.name ?? row.readId}" from this Raspberry Pi?`)) {
                                            handleDisconnectSensor(row.readId!);
                                        }
                                    }}
                                />
                            )} />
                        </DataTable>
                    </div>
                </div>
            </Dialog>

            {/* ── Edit Sensor Station Dialog ── */}
            <Dialog
                header="Edit Sensor Station"
                visible={showSensorDialog}
                style={{ width: '480px' }}
                onHide={() => setShowSensorDialog(false)}
                footer={
                    <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
                        <Button label="Cancel" severity="secondary" outlined onClick={() => setShowSensorDialog(false)} />
                        <Button label="Save" icon="pi pi-check" loading={sensorLoading} onClick={handleSaveSensor} />
                    </div>
                }
                draggable={false}
            >
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                    <div>
                        <label htmlFor="sensor-name" style={labelStyle}>Name *</label>
                        <InputText id="sensor-name" value={sensorName} onChange={e => setSensorName(e.target.value)} placeholder="e.g. Station-A1" style={{ width: '100%' }} />
                    </div>
                    <div>
                        <label htmlFor="sensor-room" style={labelStyle}>Room *</label>
                        <Dropdown inputId="sensor-room" value={sensorRoomId} options={roomOptions} onChange={e => setSensorRoomId(e.value)} placeholder="Select room" style={{ width: '100%' }} filter />
                    </div>
                </div>
            </Dialog>
        </div>
    );
};

export default SysAdminDashboard;
