import React, { useState } from 'react';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Button } from 'primereact/button';
import { DepartmentListDTO } from '../generated-skeleton-api';
import AdminTableShell from './AdminTableShell';

interface Props {
    readonly departments: DepartmentListDTO[];
    readonly loading: boolean;
    readonly onAdd: () => void;
    readonly onEdit: (department: DepartmentListDTO) => void;
    readonly onDelete: (id: string) => void;
}

const DepartmentTable: React.FC<Props> = ({ departments, loading, onAdd, onEdit, onDelete }) => {
    const [search, setSearch] = useState('');

    const filtered = departments.filter(d =>
        (d.name ?? '').toLowerCase().includes(search.toLowerCase())
    );

    const actionsTemplate = (row: DepartmentListDTO) => (
        <div className="admin-table-actions">
            <Button icon="pi pi-cog" rounded text severity="secondary" title="Edit department" onClick={() => onEdit(row)} />
            <Button icon="pi pi-trash" rounded text severity="danger" title="Delete department" onClick={() => onDelete(row.id!)} />
        </div>
    );

    return (
        <AdminTableShell title="Department List" addLabel="Add Department" onAdd={onAdd} searchValue={search} searchPlaceholder="Search by name" onSearchChange={setSearch}>
            <DataTable value={filtered} loading={loading} stripedRows emptyMessage="No departments found." responsiveLayout="scroll" className="admin-table table-scroll">
                <Column field="name" header="Name" sortable />
                <Column field="buildingName" header="Building" sortable />
                <Column header="" className="admin-actions-column" headerClassName="admin-actions-column" body={actionsTemplate} exportable={false} />
            </DataTable>
        </AdminTableShell>
    );
};

export default DepartmentTable;