import React, { useState } from 'react';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Button } from 'primereact/button';
import { RoomDTO } from '../generated-skeleton-api';
import AdminTableShell from './AdminTableShell';

interface Props {
    readonly rooms: RoomDTO[];
    readonly loading: boolean;
    readonly onAdd: () => void;
    readonly onEdit: (room: RoomDTO) => void;
    readonly onDelete: (id: string) => void;
}

const RoomTable: React.FC<Props> = ({ rooms, loading, onAdd, onEdit, onDelete }) => {
    const [search, setSearch] = useState('');

    const filtered = rooms.filter(r =>
        (r.name ?? '').toLowerCase().includes(search.toLowerCase())
    );

    const actionsTemplate = (row: RoomDTO) => (
        <div className="admin-table-actions">
            <Button icon="pi pi-cog" rounded text severity="secondary" title="Edit room" onClick={() => onEdit(row)} />
            <Button icon="pi pi-trash" rounded text severity="danger" title="Delete room" onClick={() => onDelete(row.id!)} />
        </div>
    );

    return (
        <AdminTableShell title="Room List" addLabel="Add Room" onAdd={onAdd} searchValue={search} searchPlaceholder="Search by name" onSearchChange={setSearch}>
            <DataTable value={filtered} loading={loading} stripedRows emptyMessage="No rooms found." responsiveLayout="scroll" className="admin-table admin-rooms-table table-scroll">
                <Column field="name" header="Name" sortable />
                <Column field="departmentName" header="Department" sortable />
                <Column field="roomType" header="Type" />
                <Column field="defaultPeopleCount" header="Capacity" sortable />
                <Column header="" className="admin-actions-column" headerClassName="admin-actions-column" body={actionsTemplate} exportable={false} />
            </DataTable>
        </AdminTableShell>
    );
};

export default RoomTable;