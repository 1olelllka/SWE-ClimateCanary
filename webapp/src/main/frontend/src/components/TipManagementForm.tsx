import React, { useEffect, useMemo, useRef, useState } from 'react';
import { TipControllerApi } from '../generated-skeleton-api';
import { Dialog } from 'primereact/dialog';
import { InputText } from 'primereact/inputtext';
import { Toast } from 'primereact/toast';

import {
    buildCreateDTO,
    getOptionKey,
    getTipKey,
    TipConditionOption,
    TipDTO,
    ViolatedSensor,
    WarningStatus,
    tipConditionOptions,
} from '../utilities/tipManagementUtils';

type StatusFilter = 'ALL' | WarningStatus;
type SensorFilter = 'ALL' | ViolatedSensor;
type SortDir = 'none' | 'asc' | 'desc';

const STATUS_DOT: Record<WarningStatus, string> = {
    GREEN:  '#16a34a',
    YELLOW: '#ca8a04',
    RED:    '#dc2626',
};

const SENSOR_LABEL: Record<ViolatedSensor, string> = {
    AIR:         'CO₂',
    TEMPERATURE: 'Temp.',
    HUMIDITY:    'Humidity',
};

export const TipManagementForm: React.FC = () => {
    const toast = useRef<Toast>(null);

    const [tips, setTips] = useState<TipDTO[]>([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);

    const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
    const [sensorFilter, setSensorFilter] = useState<SensorFilter>('ALL');
    const [sortDir, setSortDir] = useState<SortDir>('none');

    const [editingOption, setEditingOption] = useState<TipConditionOption | null>(null);
    const [editMessage, setEditMessage] = useState('');

    useEffect(() => {
        new TipControllerApi().getAllTips()
            .then(response => {
                setTips(Array.isArray(response.data) ? response.data as TipDTO[] : []);
            })
            .catch(err => {
                console.error('Could not load tips', err.response?.data || err);
                toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Tips could not be loaded.', life: 3000 });
            })
            .finally(() => setLoading(false));
    }, []);

    const tipByKey = useMemo(() => {
        const map: Record<string, TipDTO> = {};
        tips.forEach(t => { map[getTipKey(t)] = t; });
        return map;
    }, [tips]);

    const editingTip = useMemo(
        () => editingOption ? tipByKey[getOptionKey(editingOption)] : undefined,
        [editingOption, tipByKey],
    );

    const openEdit = (option: TipConditionOption) => {
        setEditingOption(option);
        setEditMessage(tipByKey[getOptionKey(option)]?.message ?? '');
    };

    const closeEdit = () => {
        setEditingOption(null);
        setEditMessage('');
    };

    const handleSave = () => {
        if (!editingOption) return;
        if (!editMessage.trim()) {
            toast.current?.show({ severity: 'warn', summary: 'Validation', detail: 'Message must not be empty.', life: 3000 });
            return;
        }

        setSaving(true);

        const tipApi = new TipControllerApi();
        const request = editingTip
            ? tipApi.patchTip({ tipId: editingTip.id, tipPatchDTO: { message: editMessage } })
            : tipApi.createNewTip({ tipCreateDTO: buildCreateDTO(editingOption, editMessage) as any });

        request
            .then(response => {
                const saved = response.data;
                setTips(prev => {
                    const exists = prev.some(t => t.id === saved.id);
                    return exists ? prev.map(t => t.id === saved.id ? saved : t) : [...prev, saved];
                });
                toast.current?.show({ severity: 'success', summary: 'Saved', detail: 'Tip saved successfully.', life: 3000 });
                closeEdit();
            })
            .catch(err => {
                console.error('Could not save tip', err.response?.data || err);
                toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Tip could not be saved.', life: 3000 });
            })
            .finally(() => setSaving(false));
    };

    const cycleSortDir = () => {
        setSortDir(prev => prev === 'none' ? 'asc' : prev === 'asc' ? 'desc' : 'none');
    };

    const sortIcon = sortDir === 'asc' ? ' ↑' : sortDir === 'desc' ? ' ↓' : '';

    const filteredOptions = useMemo(() => {
        let rows = tipConditionOptions.filter(o => {
            if (statusFilter !== 'ALL' && o.violationStatus !== statusFilter) return false;
            if (sensorFilter !== 'ALL' && o.violatedSensor !== sensorFilter) return false;
            return true;
        });

        if (sortDir !== 'none') {
            rows = [...rows].sort((a, b) => {
                const msgA = tipByKey[getOptionKey(a)]?.message ?? '';
                const msgB = tipByKey[getOptionKey(b)]?.message ?? '';
                const hasA = msgA !== '';
                const hasB = msgB !== '';
                if (!hasA && !hasB) return 0;
                if (!hasA) return 1;
                if (!hasB) return -1;
                const cmp = msgA.localeCompare(msgB);
                return sortDir === 'asc' ? cmp : -cmp;
            });
        }

        return rows;
    }, [statusFilter, sensorFilter, sortDir, tipByKey]);

    const statusFilters: { key: StatusFilter; label: string }[] = [
        { key: 'ALL',    label: 'All' },
        { key: 'GREEN',  label: 'Green' },
        { key: 'YELLOW', label: 'Yellow' },
        { key: 'RED',    label: 'Red' },
    ];

    const sensorFilters: { key: SensorFilter; label: string }[] = [
        { key: 'ALL',         label: 'All' },
        { key: 'AIR',         label: 'CO₂' },
        { key: 'TEMPERATURE', label: 'Temperature' },
        { key: 'HUMIDITY',    label: 'Humidity' },
    ];

    return (
        <>
            <Toast ref={toast} />
            <p className="tip-mgmt-section-heading">Tips</p>

            <div className="tip-mgmt-controls">
                <div className="tip-mgmt-filter-group">
                    {statusFilters.map(f => (
                        <button
                            key={f.key}
                            type="button"
                            className={`tip-filter-btn${statusFilter === f.key ? ' active' : ''}`}
                            onClick={() => setStatusFilter(f.key)}
                        >
                            {f.key !== 'ALL' && (
                                <span
                                    className="tip-filter-dot"
                                    style={{ background: STATUS_DOT[f.key as WarningStatus] }}
                                />
                            )}
                            {f.label}
                        </button>
                    ))}
                </div>

                <div className="tip-mgmt-filter-group">
                    {sensorFilters.map(f => (
                        <button
                            key={f.key}
                            type="button"
                            className={`tip-filter-btn${sensorFilter === f.key ? ' active' : ''}`}
                            onClick={() => setSensorFilter(f.key)}
                        >
                            {f.label}
                        </button>
                    ))}
                </div>

                <button
                    type="button"
                    className={`tip-sort-btn${sortDir !== 'none' ? ' active' : ''}`}
                    onClick={cycleSortDir}
                >
                    Message{sortIcon}
                </button>
            </div>

            <div className="tip-mgmt-table-wrapper">
                {loading ? (
                    <p className="tip-mgmt-loading">Loading tips…</p>
                ) : (
                    <table className="tip-mgmt-table">
                        <thead>
                            <tr>
                                <th>Status</th>
                                <th>Sensor</th>
                                <th>Condition</th>
                                <th
                                    className="tip-col-message-header"
                                    onClick={cycleSortDir}
                                    title="Sort by message"
                                >
                                    Message{sortIcon}
                                </th>
                                <th />
                            </tr>
                        </thead>
                        <tbody>
                            {filteredOptions.length === 0 ? (
                                <tr>
                                    <td colSpan={5} className="tip-mgmt-empty">
                                        No conditions match the selected filters.
                                    </td>
                                </tr>
                            ) : (
                                filteredOptions.map(option => {
                                    const key = getOptionKey(option);
                                    const tip = tipByKey[key];
                                    return (
                                        <tr key={key}>
                                            <td>
                                                <span
                                                    className="tip-status-dot"
                                                    style={{ background: STATUS_DOT[option.violationStatus] }}
                                                    title={option.violationStatus}
                                                />
                                            </td>
                                            <td>{SENSOR_LABEL[option.violatedSensor]}</td>
                                            <td>{option.label}</td>
                                            <td className={tip ? '' : 'tip-no-message'}>
                                                {tip ? tip.message : 'No tip set yet'}
                                            </td>
                                            <td>
                                                <button
                                                    type="button"
                                                    className="tip-edit-trigger"
                                                    onClick={() => openEdit(option)}
                                                    title="Edit tip"
                                                >
                                                    <i className="pi pi-pencil" />
                                                </button>
                                            </td>
                                        </tr>
                                    );
                                })
                            )}
                        </tbody>
                    </table>
                )}
            </div>

            <Dialog
                visible={editingOption !== null}
                onHide={closeEdit}
                header={editingOption?.label ?? 'Edit Tip'}
                className="tip-edit-dialog"
                modal
                draggable={false}
                resizable={false}
            >
                <div className="tip-edit-body">
                    <label className="tip-edit-label" htmlFor="tipMessage">Tip message</label>
                    <InputText
                        id="tipMessage"
                        value={editMessage}
                        onChange={e => setEditMessage(e.target.value.slice(0, 50))}
                        maxLength={50}
                        className="tip-edit-input"
                        placeholder="Max 50 characters"
                    />
                    <span className="tip-edit-char-count">{editMessage.length}/50</span>
<div className="tip-edit-actions">
                        <button type="button" className="tip-edit-cancel-btn" onClick={closeEdit}>
                            Cancel
                        </button>
                        <button
                            type="button"
                            className="tip-edit-save-btn"
                            onClick={handleSave}
                            disabled={saving}
                        >
                            {saving ? 'Saving…' : 'Save'}
                        </button>
                    </div>
                </div>
            </Dialog>
        </>
    );
};
