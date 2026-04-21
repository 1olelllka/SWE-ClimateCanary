import React, { useState } from 'react';
import { Button } from 'primereact/button';
import { Dropdown } from 'primereact/dropdown';
import { Tag } from 'primereact/tag';
import { UserRoleControllerApi, UserRoleDTO, UserRoleDTOPermissionsEnum } from '../generated-skeleton-api';

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
    const [status, setStatus] = useState<'saved' | 'error' | null>(null);
    const [statusMsg, setStatusMsg] = useState('');
    const [saveLoading, setSaveLoading] = useState(false);

    const roleDTOOptions = roleDTOs.map(r => ({ label: r.name ?? r.id ?? '', value: r }));

    const availablePermissions = ALL_PERMISSIONS
        .filter(p => !editedPermissions.includes(p))
        .map(p => ({ label: p, value: p }));

    const handleSelectRole = (dto: UserRoleDTO) => {
        setSelectedRoleDTO(dto);
        setEditedPermissions(Array.from(dto.permissions ?? []) as UserRoleDTOPermissionsEnum[]);
        setAddPermission(null);
        setStatus(null);
        setStatusMsg('');
    };

    const handleRemove = (perm: UserRoleDTOPermissionsEnum) => {
        setEditedPermissions(prev => prev.filter(p => p !== perm));
        setStatus(null);
    };

    const handleAdd = () => {
        if (!addPermission || editedPermissions.includes(addPermission)) return;
        setEditedPermissions(prev => [...prev, addPermission]);
        setAddPermission(null);
        setStatus(null);
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
            setStatus('saved');
            setStatusMsg('Changes are saved.');
        } catch {
            setStatus('error');
            setStatusMsg('Failed to save changes. Please try again.');
        } finally {
            setSaveLoading(false);
        }
    };

    const handleCancel = () => {
        if (selectedRoleDTO) {
            setEditedPermissions(Array.from(selectedRoleDTO.permissions ?? []) as UserRoleDTOPermissionsEnum[]);
        }
        setStatus(null);
        setStatusMsg('');
    };

    return (
        <div className="table-container">
            <h3 style={{ marginBottom: '1.25rem' }}>Role Management</h3>

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
                                <Tag
                                    key={perm}
                                    style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem', padding: '0.25rem 0.6rem' }}
                                >
                                    <span style={{ fontSize: '0.8rem' }}>{perm}</span>
                                    <button
                                        aria-label={`Remove ${perm}`}
                                        onClick={() => handleRemove(perm)}
                                        style={{
                                            background: 'none', border: 'none', cursor: 'pointer',
                                            padding: 0, lineHeight: 1, color: 'inherit',
                                            display: 'flex', alignItems: 'center',
                                        }}
                                    >
                                        <i className="pi pi-times" style={{ fontSize: '0.7rem' }} />
                                    </button>
                                </Tag>
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
                        {status === 'saved' && (
                            <span style={{ color: '#4caf50', fontWeight: 500, fontSize: '0.9rem' }}>{statusMsg}</span>
                        )}
                        {status === 'error' && (
                            <span style={{ color: '#e05252', fontWeight: 500, fontSize: '0.9rem' }}>{statusMsg}</span>
                        )}
                    </div>
                </>
            )}
        </div>
    );
};

export default RoleManagement;
