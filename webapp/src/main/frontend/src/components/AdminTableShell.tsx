import React from 'react';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';

interface AdminTableShellProps {
    readonly title: string;
    readonly addLabel?: string;
    readonly onAdd?: () => void;
    readonly searchValue?: string;
    readonly searchPlaceholder?: string;
    readonly onSearchChange?: (value: string) => void;
    readonly filters?: React.ReactNode;
    readonly children: React.ReactNode;
    readonly className?: string;
}

const AdminTableShell: React.FC<AdminTableShellProps> = ({
    title,
    addLabel,
    onAdd,
    searchValue,
    searchPlaceholder = 'Search',
    onSearchChange,
    filters,
    children,
    className,
}) => {
    const hasSearch = searchValue !== undefined && onSearchChange !== undefined;
    const hasFilters = hasSearch || filters;

    return (
        <div className={['table-container', className].filter(Boolean).join(' ')}>
            <div className="flex-header">
                <h3>{title}</h3>
                {addLabel && onAdd && (
                    <Button label={addLabel} icon="pi pi-plus" className="admin-add-button" onClick={onAdd} />
                )}
            </div>

            {hasFilters && (
                <div className={`table-filter-row${filters ? '' : ' table-filter-row-single'}`}>
                    {hasSearch && (
                        <span className="p-input-icon-left">
                            <i className="pi pi-search" />
                            <InputText
                                value={searchValue}
                                onChange={e => onSearchChange(e.target.value)}
                                placeholder={searchPlaceholder}
                            />
                        </span>
                    )}
                    {filters}
                </div>
            )}

            {children}
        </div>
    );
};

export default AdminTableShell;
