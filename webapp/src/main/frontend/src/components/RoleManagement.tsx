import React, { useRef, useState } from 'react';
import { Button } from 'primereact/button';
import { Dropdown } from 'primereact/dropdown';
import { Toast } from 'primereact/toast';
import { UserRoleControllerApi, UserRoleDTO, UserRoleDTOPermissionsEnum } from '../generated-skeleton-api';
import '../styles/Tables.css';

const ALL_PERMISSIONS = Object.values(UserRoleDTOPermissionsEnum);

const labelStyle: React.CSSProperties = {
    fontWeight: 500,
    fontSize: '0.9rem',
    whiteSpace: 'nowrap',
};

interface Props {
    readonly roleDTOs: UserRoleDTO[];
    readonly onRoleDTOsChange: (updated: UserRoleDTO[]) => void;
}

const RoleManagement: React.FC<Props> = ({ roleDTOs, onRoleDTOsChange }) => {
    const [selectedRoleDTO, setSelectedRoleDTO] = useState<UserRoleDTO | null>(null);
    const [editedPermissions, setEditedPermissions] = useState<UserRoleDTOPermissionsEnum[]>([]);
    const [addPermission, setAddPermission] = useState<UserRoleDTOPermissionsEnum | null>(null);
    const [saveLoading, setSaveLoading] = useState(false);
    const toast = useRef<Toast>(null);

    const roleDTOOptions = roleDTOs.map(r => ({ label: r.name ?? r.id ?? '', value: r }));

    const availablePermissions = ALL_PERMISSIONS
        .filter(p => !editedPermissions.includes(p))
        .map(p => ({ label: p, value: p }));

    const handleSelectRole = (dto: UserRoleDTO) => {
        setSelectedRoleDTO(dto);
        setEditedPermissions(Array.from(dto.permissions ?? []) as UserRoleDTOPermissionsEnum[]);
        setAddPermission(null);
    };

    const handleRemove = (perm: UserRoleDTOPermissionsEnum) => {
        setEditedPermissions(prev => prev.filter(p => p !== perm));
    };

    const handleAdd = () => {
        if (!addPermission || editedPermissions.includes(addPermission)) return;
        setEditedPermissions(prev => [...prev, addPermission]);
        setAddPermission(null);
    };

    const handleSave = async () => {
        if (!selectedRoleDTO?.id) return;
        setSaveLoading(true);
        try {
            await new UserRoleControllerApi().updatePermission({
                roleId: selectedRoleDTO.id,
                userRoleCreateDTO: {
                    name: selectedRoleDTO.name,
                    permissions: new Set(editedPermissions),
                },
            });
            onRoleDTOsChange(roleDTOs.map(r =>
                r.id === selectedRoleDTO.id
                    ? { ...r, permissions: new Set(editedPermissions) }
                    : r
            ));
            toast.current?.show({ severity: 'success', summary: 'Saved', detail: 'Role updated successfully.', life: 3000 });
            setSelectedRoleDTO(null);
            setEditedPermissions([]);
            setAddPermission(null);
        } catch {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to save changes. Please try again.', life: 3000 });
        } finally {
            setSaveLoading(false);
        }
    };

    const handleCancel = () => {
        setSelectedRoleDTO(null);
        setEditedPermissions([]);
        setAddPermission(null);
    };

    return (
        <>
        <Toast ref={toast} />
        <div className="table-container">
            <div className="flex-header">
                <h3 style={{ margin: 0 }}>Role Management</h3>
            </div>

            <div style={{ padding: '1.25rem 1.5rem' }}>
            <div style={{ marginBottom: '1.25rem', maxWidth: '360px' }}>
                <label htmlFor="role-select" style={{ ...labelStyle, display: 'block', marginBottom: '0.35rem' }}>
                    Role
                </label>
                <Dropdown
                    inputId="role-select"
                    value={selectedRoleDTO}
                    options={roleDTOOptions}
                    onChange={e => handleSelectRole(e.value)}
                    placeholder="Select a role to configure"
                    style={{ width: '100%' }}
                />
            </div>

            {selectedRoleDTO && (
                <>
                    <div style={{ marginBottom: '1.25rem' }}>
                        <p style={{ margin: '0 0 0.6rem 0', fontWeight: 500, fontSize: '0.9rem' }}>Rights</p>
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', minHeight: '2rem' }}>
                            {editedPermissions.length === 0 && (
                                <span style={{ color: '#9e9e9e', fontSize: '0.9rem' }}>No rights assigned.</span>
                            )}
                            {editedPermissions.map(perm => (
                                <span key={perm} className="permission-tag">
                                    {perm}
                                    <button
                                        className="permission-tag-remove"
                                        aria-label={`Remove ${perm}`}
                                        onClick={() => handleRemove(perm)}
                                    >
                                        <i className="pi pi-times" style={{ fontSize: '0.65rem' }} />
                                    </button>
                                </span>
                            ))}
                        </div>
                    </div>

                    <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap' }}>
                        <label htmlFor="add-permission" style={labelStyle}>Add right</label>
                        <Dropdown
                            inputId="add-permission"
                            value={addPermission}
                            options={availablePermissions}
                            onChange={e => setAddPermission(e.value)}
                            placeholder="Select permission"
                            style={{ minWidth: '280px' }}
                            filter
                        />
                        <Button
                            label="Add"
                            icon="pi pi-plus"
                            size="small"
                            disabled={!addPermission}
                            onClick={handleAdd}
                        />
                    </div>

                    <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
                        <Button label="Save" icon="pi pi-check" loading={saveLoading} onClick={handleSave} />
                        <Button label="Cancel" severity="secondary" outlined onClick={handleCancel} />
                    </div>
                </>
            )}
            </div>
        </div>
        </>
    );
};

export default RoleManagement;
