import React, { useState, useEffect, useRef } from 'react';
import { Toast } from 'primereact/toast';
import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import {
    BuildingControllerApi,
    DepartmentControllerApi,
    RoomControllerApi,
    BuildingListDTO,
    DepartmentListDTO,
    RoomDTO,
    RoomCreateDTORoomTypeEnum,
    RoomPatchDTORoomTypeEnum,
} from '../generated-skeleton-api';
import BuildingTable from '../components/BuildingTable';
import DepartmentTable from '../components/DepartmentTable';
import RoomTable from '../components/RoomTable';
import BuildingFormDialog, { BuildingFormState, emptyBuildingForm } from '../components/BuildingFormDialog';
import DepartmentFormDialog, { DepartmentFormState, emptyDepartmentForm } from '../components/DepartmentFormDialog';
import RoomFormDialog, { RoomFormState, emptyRoomForm } from '../components/RoomFormDialog';
import '../styles/Tables.css';

type TabKey = 'buildings' | 'departments' | 'rooms';

const PAGEABLE = { page: 0, size: 200, sort: [] };

const BuildingConfigurationPage: React.FC = () => {
    const toast = useRef<Toast>(null);
    const [sidebarVisible, setSidebarVisible] = useState(false);
    const [activeTab, setActiveTab] = useState<TabKey>('buildings');

    const [buildings, setBuildings] = useState<BuildingListDTO[]>([]);
    const [departments, setDepartments] = useState<DepartmentListDTO[]>([]);
    const [rooms, setRooms] = useState<RoomDTO[]>([]);
    const [loadingBuildings, setLoadingBuildings] = useState(false);
    const [loadingDepartments, setLoadingDepartments] = useState(false);
    const [loadingRooms, setLoadingRooms] = useState(false);

    // Building dialog
    const [showBuildingDialog, setShowBuildingDialog] = useState(false);
    const [isNewBuilding, setIsNewBuilding] = useState(true);
    const [editingBuildingId, setEditingBuildingId] = useState<string | undefined>();
    const [buildingForm, setBuildingForm] = useState<BuildingFormState>(emptyBuildingForm());
    const [buildingFormErrors, setBuildingFormErrors] = useState<Partial<Record<keyof BuildingFormState, string>>>({});
    const [buildingDialogLoading, setBuildingDialogLoading] = useState(false);

    // Department dialog
    const [showDepartmentDialog, setShowDepartmentDialog] = useState(false);
    const [isNewDepartment, setIsNewDepartment] = useState(true);
    const [editingDepartmentId, setEditingDepartmentId] = useState<string | undefined>();
    const [departmentForm, setDepartmentForm] = useState<DepartmentFormState>(emptyDepartmentForm());
    const [departmentFormErrors, setDepartmentFormErrors] = useState<Partial<Record<keyof DepartmentFormState, string>>>({});
    const [departmentDialogLoading, setDepartmentDialogLoading] = useState(false);

    // Room dialog
    const [showRoomDialog, setShowRoomDialog] = useState(false);
    const [isNewRoom, setIsNewRoom] = useState(true);
    const [editingRoomId, setEditingRoomId] = useState<string | undefined>();
    const [roomForm, setRoomForm] = useState<RoomFormState>(emptyRoomForm());
    const [roomFormErrors, setRoomFormErrors] = useState<Partial<Record<keyof RoomFormState, string>>>({});
    const [roomDialogLoading, setRoomDialogLoading] = useState(false);

    const fetchBuildings = () => {
        setLoadingBuildings(true);
        new BuildingControllerApi().getPageOfBuildings({ pageable: PAGEABLE })
            .then(res => setBuildings(res.data.content ?? []))
            .catch(() => toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to load buildings', life: 3000 }))
            .finally(() => setLoadingBuildings(false));
    };

    const fetchDepartments = () => {
        setLoadingDepartments(true);
        new DepartmentControllerApi().getPageOfDepartments({ pageable: PAGEABLE })
            .then(res => setDepartments(res.data.content ?? []))
            .catch(() => toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to load departments', life: 3000 }))
            .finally(() => setLoadingDepartments(false));
    };

    const fetchRooms = () => {
        setLoadingRooms(true);
        new RoomControllerApi().getPageOfRooms({ pageable: PAGEABLE })
            .then(res => setRooms(res.data.content ?? []))
            .catch(() => toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to load rooms', life: 3000 }))
            .finally(() => setLoadingRooms(false));
    };

    useEffect(() => {
        fetchBuildings();
        fetchDepartments();
        fetchRooms();
    }, []);

    const buildingOptions = buildings.map(b => ({ label: b.name ?? b.id ?? '', value: b.id ?? '' }));
    const departmentOptions = departments.map(d => ({ label: d.name ?? d.id ?? '', value: d.id ?? '' }));

    // --- Building handlers ---
    const openCreateBuilding = () => {
        setBuildingForm(emptyBuildingForm());
        setBuildingFormErrors({});
        setIsNewBuilding(true);
        setEditingBuildingId(undefined);
        setShowBuildingDialog(true);
    };

    const openEditBuilding = (b: BuildingListDTO) => {
        setBuildingForm({ name: b.name ?? '', address: b.address ?? '' });
        setBuildingFormErrors({});
        setIsNewBuilding(false);
        setEditingBuildingId(b.id);
        setShowBuildingDialog(true);
    };

    const validateBuilding = (): boolean => {
        const errors: Partial<Record<keyof BuildingFormState, string>> = {};
        if (!buildingForm.name.trim()) errors.name = 'Required';
        setBuildingFormErrors(errors);
        return Object.keys(errors).length === 0;
    };

    const handleSaveBuilding = async () => {
        if (!validateBuilding()) return;
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
                    ? { id: res.data.id, name: res.data.name, address: res.data.address }
                    : b
                ));
                toast.current?.show({ severity: 'success', summary: 'Saved', detail: 'Building updated.', life: 3000 });
            }
            setShowBuildingDialog(false);
        } catch {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to save building.', life: 3000 });
        } finally {
            setBuildingDialogLoading(false);
        }
    };

    const handleDeleteBuilding = (id: string) => {
        if (!globalThis.confirm('Delete this building?')) return;
        new BuildingControllerApi().deleteSpecificBuilding({ buildingId: id })
            .then(() => {
                setBuildings(prev => prev.filter(b => b.id !== id));
                toast.current?.show({ severity: 'success', summary: 'Deleted', detail: 'Building deleted.', life: 3000 });
            })
            .catch(() => toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to delete building.', life: 3000 }));
    };

    // --- Department handlers ---
    const openCreateDepartment = () => {
        setDepartmentForm(emptyDepartmentForm());
        setDepartmentFormErrors({});
        setIsNewDepartment(true);
        setEditingDepartmentId(undefined);
        setShowDepartmentDialog(true);
    };

    const openEditDepartment = (d: DepartmentListDTO) => {
        setDepartmentForm({ name: d.name ?? '', buildingID: d.buildingID ?? '', rooms: [] });
        setDepartmentFormErrors({});
        setIsNewDepartment(false);
        setEditingDepartmentId(d.id);
        setShowDepartmentDialog(true);
    };

    const validateDepartment = (): boolean => {
        const errors: Partial<Record<keyof DepartmentFormState, string>> = {};
        if (!departmentForm.name.trim()) errors.name = 'Required';
        if (!departmentForm.buildingID) errors.buildingID = 'Required';
        setDepartmentFormErrors(errors);
        return Object.keys(errors).length === 0;
    };

    const handleSaveDepartment = async () => {
        if (!validateDepartment()) return;
        setDepartmentDialogLoading(true);
        try {
            if (isNewDepartment) {
                const res = await new DepartmentControllerApi().createNewDepartment({
                    departmentCreateDTO: { name: departmentForm.name, buildingID: departmentForm.buildingID },
                });
                const newDept: DepartmentListDTO = {
                    id: res.data.id,
                    name: res.data.name,
                    buildingID: res.data.buildingID,
                    buildingName: buildings.find(b => b.id === res.data.buildingID)?.name,
                };
                setDepartments(prev => [...prev, newDept]);

                // Create any rooms added in the dialog
                if (departmentForm.rooms.length > 0) {
                    await Promise.all(departmentForm.rooms.map(r =>
                        new RoomControllerApi().createNewRoom({
                            roomCreateDTO: {
                                name: r.name,
                                departmentID: res.data.id!,
                                roomType: r.roomType,
                                defaultPeopleCount: r.defaultPeopleCount,
                            },
                        })
                    ));
                    fetchRooms();
                }
                toast.current?.show({ severity: 'success', summary: 'Created', detail: 'Department created.', life: 3000 });
            } else {
                const res = await new DepartmentControllerApi().patchSpecificDepartment({
                    departmentId: editingDepartmentId!,
                    departmentCreateDTO: { name: departmentForm.name, buildingID: departmentForm.buildingID },
                });
                setDepartments(prev => prev.map(d => d.id === res.data.id
                    ? { id: res.data.id, name: res.data.name, buildingID: res.data.buildingID, buildingName: buildings.find(b => b.id === res.data.buildingID)?.name }
                    : d
                ));
                toast.current?.show({ severity: 'success', summary: 'Saved', detail: 'Department updated.', life: 3000 });
            }
            setShowDepartmentDialog(false);
        } catch {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to save department.', life: 3000 });
        } finally {
            setDepartmentDialogLoading(false);
        }
    };

    const handleDeleteDepartment = (id: string) => {
        if (!globalThis.confirm('Delete this department?')) return;
        new DepartmentControllerApi().deleteSpecificDepartment({ departmentId: id })
            .then(() => {
                setDepartments(prev => prev.filter(d => d.id !== id));
                toast.current?.show({ severity: 'success', summary: 'Deleted', detail: 'Department deleted.', life: 3000 });
            })
            .catch(() => toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to delete department.', life: 3000 }));
    };

    // --- Room handlers ---
    const openCreateRoom = () => {
        setRoomForm(emptyRoomForm());
        setRoomFormErrors({});
        setIsNewRoom(true);
        setEditingRoomId(undefined);
        setShowRoomDialog(true);
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
        setShowRoomDialog(true);
    };

    const validateRoom = (): boolean => {
        const errors: Partial<Record<keyof RoomFormState, string>> = {};
        if (!roomForm.name.trim()) errors.name = 'Required';
        if (!roomForm.departmentID) errors.departmentID = 'Required';
        setRoomFormErrors(errors);
        return Object.keys(errors).length === 0;
    };

    const handleSaveRoom = async () => {
        if (!validateRoom()) return;
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
            setShowRoomDialog(false);
        } catch {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to save room.', life: 3000 });
        } finally {
            setRoomDialogLoading(false);
        }
    };

    const handleDeleteRoom = (id: string) => {
        if (!globalThis.confirm('Delete this room?')) return;
        new RoomControllerApi().deleteSpecificRoom({ roomId: id })
            .then(() => {
                setRooms(prev => prev.filter(r => r.id !== id));
                toast.current?.show({ severity: 'success', summary: 'Deleted', detail: 'Room deleted.', life: 3000 });
            })
            .catch(() => toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to delete room.', life: 3000 }));
    };

    const TAB_LABELS: Record<TabKey, string> = {
        buildings: 'Buildings',
        departments: 'Departments',
        rooms: 'Rooms',
    };

    return (
        <div className="dashboard-layout">
            <Toast ref={toast} />
            <PageHeader title="Building Configuration" onMenuClick={() => setSidebarVisible(true)} />
            <SidebarComponent visible={sidebarVisible} onHide={() => setSidebarVisible(false)} />

            <div className="dashboard-content">
                <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.5rem', flexWrap: 'wrap' }}>
                    {(Object.keys(TAB_LABELS) as TabKey[]).map(tab => (
                        <button
                            key={tab}
                            onClick={() => setActiveTab(tab)}
                            style={{
                                padding: '0.5rem 1.25rem',
                                borderRadius: '20px',
                                border: '1px solid var(--primary-color, #6366f1)',
                                background: activeTab === tab ? 'var(--primary-color, #6366f1)' : 'transparent',
                                color: activeTab === tab ? '#fff' : 'var(--primary-color, #6366f1)',
                                cursor: 'pointer',
                                fontWeight: 500,
                                fontSize: '0.9rem',
                            }}
                        >
                            {TAB_LABELS[tab]}
                        </button>
                    ))}
                </div>

                {activeTab === 'buildings' && (
                    <BuildingTable
                        buildings={buildings}
                        loading={loadingBuildings}
                        onAdd={openCreateBuilding}
                        onEdit={openEditBuilding}
                        onDelete={handleDeleteBuilding}
                    />
                )}
                {activeTab === 'departments' && (
                    <DepartmentTable
                        departments={departments}
                        loading={loadingDepartments}
                        onAdd={openCreateDepartment}
                        onEdit={openEditDepartment}
                        onDelete={handleDeleteDepartment}
                    />
                )}
                {activeTab === 'rooms' && (
                    <RoomTable
                        rooms={rooms}
                        loading={loadingRooms}
                        onAdd={openCreateRoom}
                        onEdit={openEditRoom}
                        onDelete={handleDeleteRoom}
                    />
                )}
            </div>

            <BuildingFormDialog
                visible={showBuildingDialog}
                isNew={isNewBuilding}
                form={buildingForm}
                formErrors={buildingFormErrors}
                loading={buildingDialogLoading}
                onHide={() => setShowBuildingDialog(false)}
                onSave={handleSaveBuilding}
                onChange={patch => setBuildingForm(f => ({ ...f, ...patch }))}
            />

            <DepartmentFormDialog
                visible={showDepartmentDialog}
                isNew={isNewDepartment}
                form={departmentForm}
                formErrors={departmentFormErrors}
                buildingOptions={buildingOptions}
                loading={departmentDialogLoading}
                onHide={() => setShowDepartmentDialog(false)}
                onSave={handleSaveDepartment}
                onChange={patch => setDepartmentForm(f => ({ ...f, ...patch }))}
            />

            <RoomFormDialog
                visible={showRoomDialog}
                isNew={isNewRoom}
                form={roomForm}
                formErrors={roomFormErrors}
                departmentOptions={departmentOptions}
                loading={roomDialogLoading}
                onHide={() => setShowRoomDialog(false)}
                onSave={handleSaveRoom}
                onChange={patch => setRoomForm(f => ({ ...f, ...patch }))}
            />
        </div>
    );
};

export default BuildingConfigurationPage;
