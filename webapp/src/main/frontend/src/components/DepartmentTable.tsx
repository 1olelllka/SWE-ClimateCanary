import React, { useState } from 'react';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import { DepartmentListDTO } from '../generated-skeleton-api';

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
        <div className="table-container">
            <div className="flex-header">
                <h3>Department List</h3>
                <Button label="Add Department" icon="pi pi-plus" className="admin-add-button" onClick={onAdd} />
            </div>

            <div className="table-filter-row table-filter-row-single">
                <span className="p-input-icon-left">
                    <i className="pi pi-search" />
                    <InputText value={search} onChange={e => setSearch(e.target.value)} placeholder="Search by name" />
                </span>
            </div>

            <DataTable value={filtered} loading={loading} stripedRows emptyMessage="No departments found." responsiveLayout="scroll">
                <Column field="name" header="Name" sortable />
                <Column field="buildingName" header="Building" sortable />
                <Column header="" className="admin-actions-column" headerClassName="admin-actions-column" body={actionsTemplate} exportable={false} />
            </DataTable>
        </div>
    );
};

export default DepartmentTable;