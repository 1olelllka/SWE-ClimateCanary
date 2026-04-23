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
        <div style={{ display: 'flex', gap: '0.25rem', justifyContent: 'flex-end' }}>
            <Button icon="pi pi-cog" rounded text severity="secondary" title="Edit department" onClick={() => onEdit(row)} />
            <Button icon="pi pi-trash" rounded text severity="danger" title="Delete department" onClick={() => onDelete(row.id!)} />
        </div>
    );

    return (
        <div className="table-container">
            <div className="flex-header" style={{ marginBottom: '1rem', flexWrap: 'wrap', gap: '0.75rem' }}>
                <h3 style={{ margin: 0 }}>Department List</h3>
                <Button label="Add Department" icon="pi pi-plus" onClick={onAdd} />
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

            <DataTable value={filtered} loading={loading} stripedRows emptyMessage="No departments found." responsiveLayout="scroll">
                <Column field="id" header="ID" style={{ maxWidth: '10rem', overflow: 'hidden', textOverflow: 'ellipsis' }} />
                <Column field="name" header="Name" sortable />
                <Column field="buildingName" header="Building" />
                <Column header="" body={actionsTemplate} style={{ width: '6rem' }} exportable={false} />
            </DataTable>
        </div>
    );
};

export default DepartmentTable;
