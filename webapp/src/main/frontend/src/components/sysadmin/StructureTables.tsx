import React from 'react';
import { Button } from 'primereact/button';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';
import { Dropdown } from 'primereact/dropdown';
import { BuildingListDTO, DepartmentListDTO, RoomDTO } from '../../generated-skeleton-api';
import AdminTableHeader from './AdminTableHeader';
import { SelectOption } from './sysAdminTypes';

interface StructureTablesProps {
    readonly buildings: BuildingListDTO[];
    readonly departments: DepartmentListDTO[];
    readonly rooms: RoomDTO[];
    readonly buildingSearch: string;
    readonly onBuildingSearch: (value: string) => void;
    readonly departmentSearch: string;
    readonly onDepartmentSearch: (value: string) => void;
    readonly departmentBuildingFilter: string | null;
    readonly onDepartmentBuildingFilter: (value: string | null) => void;
    readonly buildingNameOptions: string[];
    readonly roomSearch: string;
    readonly onRoomSearch: (value: string) => void;
    readonly roomTypeFilter: string | null;
    readonly onRoomTypeFilter: (value: string | null) => void;
    readonly roomTypeOptions: SelectOption[];
    readonly onAddBuilding: () => void;
    readonly onEditBuilding: (building: BuildingListDTO) => void;
    readonly onDeleteBuilding: (id?: string) => void;
    readonly onAddDepartment: () => void;
    readonly onEditDepartment: (department: DepartmentListDTO) => void;
    readonly onDeleteDepartment: (id?: string) => void;
    readonly onAddRoom: () => void;
    readonly onEditRoom: (room: RoomDTO) => void;
    readonly onDeleteRoom: (id?: string) => void;
}

const StructureTables: React.FC<StructureTablesProps> = ({
    buildings,
    departments,
    rooms,
    buildingSearch,
    onBuildingSearch,
    departmentSearch,
    onDepartmentSearch,
    departmentBuildingFilter,
    onDepartmentBuildingFilter,
    buildingNameOptions,
    roomSearch,
    onRoomSearch,
    roomTypeFilter,
    onRoomTypeFilter,
    roomTypeOptions,
    onAddBuilding,
    onEditBuilding,
    onDeleteBuilding,
    onAddDepartment,
    onEditDepartment,
    onDeleteDepartment,
    onAddRoom,
    onEditRoom,
    onDeleteRoom,
}) => {
    const filteredBuildings = buildings.filter(b =>
        !buildingSearch || (b.name ?? '').toLowerCase().includes(buildingSearch.toLowerCase())
    );

    const normalizedDepartmentSearch = departmentSearch.toLowerCase();
    const normalizedRoomSearch = roomSearch.toLowerCase();
    const filteredDepartments = departments.filter(d =>
        (!departmentSearch || (d.name ?? '').toLowerCase().includes(normalizedDepartmentSearch)) &&
        (!departmentBuildingFilter || d.buildingName === departmentBuildingFilter)
    );

    const filteredRooms = rooms.filter(r =>
        (!roomSearch || (r.name ?? '').toLowerCase().includes(normalizedRoomSearch)) &&
        (!roomTypeFilter || r.roomType === roomTypeFilter)
    );

    return (
        <>
            <div className="table-container">
                <AdminTableHeader
                    title="Departments"
                    search={departmentSearch}
                    onSearch={onDepartmentSearch}
                    searchPlaceholder="Search by name"
                    onAdd={onAddDepartment}
                    addLabel="Add Department"
                    filterEl={
                        <Dropdown
                            value={departmentBuildingFilter}
                            options={buildingNameOptions}
                            onChange={e => onDepartmentBuildingFilter(e.value)}
                            placeholder="Building Filter"
                            showClear
                            style={{ borderRadius: '20px', minWidth: '180px' }}
                        />
                    }
                />
                <DataTable value={filteredDepartments} stripedRows emptyMessage="No departments found.">
                    <Column field="name" header="Name" sortable/>
                    <Column field="buildingName" header="Building" sortable/>
                    <Column
                        header=""
                        className="admin-actions-column"
                        headerClassName="admin-actions-column"
                        exportable={false}
                        body={(row: DepartmentListDTO) => (
                            <div className="admin-table-actions">
                                <Button icon="pi pi-cog" rounded text severity="secondary" title="Edit department" onClick={() => onEditDepartment(row)}/>
                                <Button icon="pi pi-trash" rounded text severity="danger" title="Delete department" onClick={() => onDeleteDepartment(row.id)}/>
                            </div>
                        )}
                    />
                </DataTable>
            </div>

            <div className="table-container">
                <AdminTableHeader
                    title="Rooms"
                    search={roomSearch}
                    onSearch={onRoomSearch}
                    searchPlaceholder="Search by name"
                    onAdd={onAddRoom}
                    addLabel="Add Room"
                    filterEl={
                        <Dropdown
                            value={roomTypeFilter}
                            options={roomTypeOptions}
                            onChange={e => onRoomTypeFilter(e.value)}
                            placeholder="Type Filter"
                            showClear
                            style={{ borderRadius: '20px', minWidth: '160px' }}
                        />
                    }
                />
                <DataTable value={filteredRooms} stripedRows emptyMessage="No rooms found." className="admin-rooms-table">
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
                                <Button icon="pi pi-cog" rounded text severity="secondary" title="Edit room" onClick={() => onEditRoom(row)}/>
                                <Button icon="pi pi-trash" rounded text severity="danger" title="Delete room" onClick={() => onDeleteRoom(row.id)}/>
                            </div>
                        )}
                    />
                </DataTable>
            </div>

            <div className="table-container">
                <AdminTableHeader
                    title="Buildings"
                    search={buildingSearch}
                    onSearch={onBuildingSearch}
                    searchPlaceholder="Search by name"
                    onAdd={onAddBuilding}
                    addLabel="Add Building"
                />
                <DataTable value={filteredBuildings} stripedRows emptyMessage="No buildings found.">
                    <Column field="name" header="Name" sortable/>
                    <Column field="address" header="Address"/>
                    <Column
                        header=""
                        className="admin-actions-column"
                        headerClassName="admin-actions-column"
                        exportable={false}
                        body={(row: BuildingListDTO) => (
                            <div className="admin-table-actions">
                                <Button icon="pi pi-cog" rounded text severity="secondary" title="Edit building" onClick={() => onEditBuilding(row)}/>
                                <Button icon="pi pi-trash" rounded text severity="danger" title="Delete building" onClick={() => onDeleteBuilding(row.id)}/>
                            </div>
                        )}
                    />
                </DataTable>
            </div>
        </>
    );
};

export default StructureTables;
