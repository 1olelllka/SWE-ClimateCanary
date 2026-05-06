import React, { useState } from 'react';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import { RoomDTO } from '../generated-skeleton-api';

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
        <div style={{ display: 'flex', gap: '0.25rem', justifyContent: 'flex-end' }}>
            <Button icon="pi pi-cog" rounded text severity="secondary" title="Edit room" onClick={() => onEdit(row)} />
            <Button icon="pi pi-trash" rounded text severity="danger" title="Delete room" onClick={() => onDelete(row.id!)} />
        </div>
    );

    return (
        <div className="table-container">
            <div className="flex-header" style={{ marginBottom: '1rem', flexWrap: 'wrap', gap: '0.75rem' }}>
                <h3 style={{ margin: 0 }}>Room List</h3>
                <Button label="Add Room" icon="pi pi-plus" onClick={onAdd} />
            </div>

            <div style={{ marginBottom: '1rem' }}>
                <span className="p-input-icon-left">
                    <i className="pi pi-search" style={{ marginLeft: '0.7rem' }} />
                    <InputText
                        value={search}
                        onChange={e => setSearch(e.target.value)}
                        placeholder="Search by name"
                        style={{ borderRadius: '20px', paddingLeft: '2rem' }}
                    />
                </span>
            </div>

            <DataTable value={filtered} loading={loading} stripedRows emptyMessage="No rooms found." responsiveLayout="scroll">
                <Column field="id" header="ID" style={{ maxWidth: '10rem', overflow: 'hidden', textOverflow: 'ellipsis' }} />
                <Column field="name" header="Name" sortable />
                <Column field="departmentName" header="Department" sortable />
                <Column field="roomType" header="Type" />
                <Column field="defaultPeopleCount" header="Capacity" sortable />
                <Column header="" body={actionsTemplate} style={{ width: '6rem' }} exportable={false} />
            </DataTable>
        </div>
    );
};

export default RoomTable;
