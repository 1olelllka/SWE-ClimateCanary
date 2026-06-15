import React from 'react';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';

interface AdminTableHeaderProps {
    readonly title: string;
    readonly search: string;
    readonly onSearch: (value: string) => void;
    readonly searchPlaceholder: string;
    readonly filterEl?: React.ReactNode;
    readonly onAdd?: () => void;
    readonly addLabel?: string;
}

const AdminTableHeader: React.FC<AdminTableHeaderProps> = ({
    title,
    search,
    onSearch,
    searchPlaceholder,
    filterEl,
    onAdd,
    addLabel,
}) => (
    <>
        <div className="flex-header">
            <h3 style={{ margin: 0 }}>{title}</h3>
            {onAdd && <Button label={addLabel ?? 'Add'} icon="pi pi-plus" className="admin-add-button" onClick={onAdd}/>}
        </div>
        <div className="table-filter-row">
            <span className="p-input-icon-left">
                <i className="pi pi-search"/>
                <InputText
                    value={search}
                    onChange={e => onSearch(e.target.value)}
                    placeholder={searchPlaceholder}
                    style={{ borderRadius: '20px', paddingLeft: '2.0rem' }}
                />
            </span>
            {filterEl}
        </div>
    </>
);

export default AdminTableHeader;
