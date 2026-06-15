import React, { useState } from 'react';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Button } from 'primereact/button';
import { BuildingListDTO } from '../generated-skeleton-api';
import AdminTableShell from './AdminTableShell';

interface Props {
    readonly buildings: BuildingListDTO[];
    readonly loading: boolean;
    readonly onAdd: () => void;
    readonly onEdit: (building: BuildingListDTO) => void;
    readonly onDelete: (id: string) => void;
}

const BuildingTable: React.FC<Props> = ({ buildings, loading, onAdd, onEdit, onDelete }) => {
    const [search, setSearch] = useState('');

    const filtered = buildings.filter(b =>
        (b.name ?? '').toLowerCase().includes(search.toLowerCase())
    );

    const actionsTemplate = (row: BuildingListDTO) => (
        <div className="admin-table-actions">
            <Button icon="pi pi-cog" rounded text severity="secondary" title="Edit building" onClick={() => onEdit(row)} />
            <Button icon="pi pi-trash" rounded text severity="danger" title="Delete building" onClick={() => onDelete(row.id!)} />
        </div>
    );

    return (
        <AdminTableShell
            title="Building List"
            addLabel="Add Building"
            onAdd={onAdd}
            searchValue={search}
            searchPlaceholder="Search by name"
            onSearchChange={setSearch}
        >
            <DataTable value={filtered} loading={loading} stripedRows emptyMessage="No buildings found." className="admin-table table-scroll">
                <Column field="name" header="Name" sortable />
                <Column field="address" header="Address" sortable />
                <Column header="" className="admin-actions-column" headerClassName="admin-actions-column" body={actionsTemplate} exportable={false} />
            </DataTable>
        </AdminTableShell>
    );
};

export default BuildingTable;