import React, { useState, useEffect, useRef } from 'react';
import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import { Toast } from 'primereact/toast';
import {
    RaspberryControllerApi,
    SensorStationControllerApi,
    RoomControllerApi,
    BuildingControllerApi,
    DepartmentControllerApi,
    UserRoleControllerApi,
    UserxControllerApi,
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
import ConfirmDeleteDialog from '../components/ConfirmDeleteDialog';
import DeviceTables from '../components/sysadmin/DeviceTables';
import RaspberryDialog from '../components/sysadmin/RaspberryDialog';
import SensorStationDialog from '../components/sysadmin/SensorStationDialog';
import StructureTables from '../components/sysadmin/StructureTables';
import UserAdminTable from '../components/sysadmin/UserAdminTable';
import { ConfirmDeleteState, FullUser, RaspberryDTOReal } from '../components/sysadmin/sysAdminTypes';
import '../styles/Tables.css';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const PAGEABLE = { page: 0, size: 200, sort: [] };

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

    // --- Confirm delete ---
    const [confirmDelete, setConfirmDelete] = useState<ConfirmDeleteState | null>(null);

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
    }, []);

    // --- Shared table/dialog options ---
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

    // --- Dropdown options ---
    const statusOptions = [{ label: 'Online', value: 'ONLINE' }, { label: 'Offline', value: 'OFFLINE' }];
    const roomTypeOptions = [{ label: 'Office', value: 'OFFICE' }, { label: 'Shared', value: 'SHARED' }];

    return (
        <div className="dashboard-layout">
            <Toast ref={toast}/>
            <PageHeader title="Dashboard" onMenuClick={() => setSidebarVisible(true)}/>
            <SidebarComponent visible={sidebarVisible} onHide={() => setSidebarVisible(false)}/>

            <div className="dashboard-content">
                <DeviceTables
                    raspberries={raspberries}
                    sensors={sensors}
                    rooms={rooms}
                    raspberrySearch={raspberrySearch}
                    onRaspberrySearch={setRaspberrySearch}
                    raspberryStatusFilter={raspberryStatusFilter}
                    onRaspberryStatusFilter={setRaspberryStatusFilter}
                    sensorSearch={sensorSearch}
                    onSensorSearch={setSensorSearch}
                    sensorStatusFilter={sensorStatusFilter}
                    onSensorStatusFilter={setSensorStatusFilter}
                    statusOptions={statusOptions}
                    onAddPi={openAddPiDialog}
                    onEditPi={openEditPiDialog}
                    onDeletePi={handleDeletePi}
                    onRetryPiConnection={handleRetryPiConnection}
                    onAddSensor={openAddSensorDialog}
                    onEditSensor={openEditSensorDialog}
                    onDeleteSensor={handleDeleteSensor}
                    onRetrySensorConnection={handleRetrySensorConnection}
                />

                <UserAdminTable
                    users={users}
                    search={userSearch}
                    onSearch={setUserSearch}
                    roleFilter={userRoleFilter}
                    onRoleFilter={setUserRoleFilter}
                    roomFilter={userRoomFilter}
                    onRoomFilter={setUserRoomFilter}
                    roleFilterOptions={roleFilterOptions}
                    roomFilterOptions={roomFilterOptions}
                    onAddUser={openAddUser}
                    onEditUser={openEditUser}
                    onDeleteUser={handleDeleteUser}
                />

                <StructureTables
                    buildings={buildings}
                    departments={departments}
                    rooms={rooms}
                    buildingSearch={buildingSearch}
                    onBuildingSearch={setBuildingSearch}
                    departmentSearch={departmentSearch}
                    onDepartmentSearch={setDepartmentSearch}
                    departmentBuildingFilter={departmentBuildingFilter}
                    onDepartmentBuildingFilter={setDepartmentBuildingFilter}
                    buildingNameOptions={buildingNameOptions}
                    roomSearch={roomSearch}
                    onRoomSearch={setRoomSearch}
                    roomTypeFilter={roomTypeFilter}
                    onRoomTypeFilter={setRoomTypeFilter}
                    roomTypeOptions={roomTypeOptions}
                    onAddBuilding={openAddBuilding}
                    onEditBuilding={openEditBuilding}
                    onDeleteBuilding={handleDeleteBuilding}
                    onAddDepartment={openAddDepartment}
                    onEditDepartment={openEditDepartment}
                    onDeleteDepartment={handleDeleteDepartment}
                    onAddRoom={openAddRoom}
                    onEditRoom={openEditRoom}
                    onDeleteRoom={handleDeleteRoom}
                />
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

            <RaspberryDialog
                visible={showPiDialog}
                editingPiId={editingPiId}
                piName={piName}
                onPiNameChange={setPiName}
                piRoomId={piRoomId}
                onPiRoomIdChange={setPiRoomId}
                piIpAddress={piIpAddress}
                onPiIpAddressChange={setPiIpAddress}
                piPort={piPort}
                onPiPortChange={setPiPort}
                piInterval={piInterval}
                onPiIntervalChange={setPiInterval}
                loading={piLoading}
                availableRoomOptions={availableRoomOptions}
                editAvailableRoomOptions={editAvailableRoomOptions}
                connectedSensors={sensors.filter(s => s.connectedToPiId === editingPiId)}
                onSave={handleSavePi}
                onHide={() => setShowPiDialog(false)}
                onDisconnectSensor={sensorId => {
                    setConfirmDelete(null);
                    handleDisconnectSensor(sensorId);
                }}
                onConfirmDelete={setConfirmDelete}
            />

            <SensorStationDialog
                visible={showSensorDialog}
                editingSensorId={editingSensorId}
                editingSensorWriteId={editingSensorWriteId}
                sensorName={sensorName}
                onSensorNameChange={setSensorName}
                sensorRoomId={sensorRoomId}
                onSensorRoomIdChange={setSensorRoomId}
                roomOptions={roomOptions}
                loading={sensorLoading}
                onSave={handleSaveSensor}
                onHide={() => setShowSensorDialog(false)}
            />
        </div>
    );
};

export default SysAdminDashboard;
