import React, { useEffect, useMemo, useRef, useState } from 'react';
import { AbsenceControllerApi } from '../generated-skeleton-api';
import { Calendar } from 'primereact/calendar';
import { Dropdown } from 'primereact/dropdown';
import { Toast } from 'primereact/toast';
import '../styles/CreateAbsenceForm.css';

interface ToastOptions {
    severity: 'success' | 'error' | 'warn' | 'info';
    summary: string;
    detail: string;
    life?: number;
}

interface CreateAbsenceFormProps {
    currentUserId: string | null;
    onSuccess: () => void;
    onCancel: () => void;
    onToast?: (options: ToastOptions) => void;
}

interface ManagerDTO {
    id: string;
    firstName: string;
    lastName: string;
    username: string;
}

type AbsenceReason = 'VACATION' | 'ILLNESS' | 'OTHER';

interface DropdownOption {
    label: string;
    value: string;
}

const reasonOptions: DropdownOption[] = [
    { label: 'Vacation', value: 'VACATION' },
    { label: 'Illness', value: 'ILLNESS' },
    { label: 'Other', value: 'OTHER' },
];

const formatDateForBackend = (date: Date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
};

export const CreateAbsenceForm: React.FC<CreateAbsenceFormProps> = ({
                                                                        currentUserId,
                                                                        onSuccess,
                                                                        onCancel,
                                                                        onToast,
                                                                    }) => {
    const [reason, setReason] = useState<AbsenceReason>('VACATION');
    const [startDate, setStartDate] = useState<Date | null>(null);
    const [endDate, setEndDate] = useState<Date | null>(null);
    const [includeTime, setIncludeTime] = useState(false);
    const [startHour, setStartHour] = useState('07');
    const [startMinute, setStartMinute] = useState('00');
    const [endHour, setEndHour] = useState('15');
    const [endMinute, setEndMinute] = useState('00');
    const [managerId, setManagerId] = useState('');
    const [managers, setManagers] = useState<ManagerDTO[]>([]);
    const [comment, setComment] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [managerLoading, setManagerLoading] = useState(true);
    const toast = useRef<Toast>(null);

    useEffect(() => {
        setManagerLoading(true);

        new AbsenceControllerApi().getAvailableManagers()
            .then(res => setManagers((res.data as any) || []))
            .catch(err => {
                console.error('Could not load managers', err);
                setManagers([]);
            })
            .finally(() => setManagerLoading(false));
    }, []);

    const managerOptions = useMemo<DropdownOption[]>(() => {
        return managers.map(manager => ({
            label: `${manager.firstName} ${manager.lastName}`,
            value: manager.id,
        }));
    }, [managers]);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();

        if (!currentUserId) {
            toast.current?.show({ severity: 'error', summary: 'Error', detail: 'User ID not found. Please log in again.', life: 4000 });
            return;
        }

        if (!startDate || !endDate) {
            toast.current?.show({ severity: 'warn', summary: 'Validation', detail: 'Please select a start and end date.', life: 4000 });
            return;
        }

        if (!managerId) {
            toast.current?.show({ severity: 'warn', summary: 'Validation', detail: 'Please select a manager.', life: 4000 });
            return;
        }

        setIsSubmitting(true);

        const startDateString = formatDateForBackend(startDate);
        const endDateString = formatDateForBackend(endDate);

        const startIso = includeTime
            ? `${startDateString}T${startHour}:${startMinute}:00`
            : `${startDateString}T00:00:00`;

        const endIso = includeTime
            ? `${endDateString}T${endHour}:${endMinute}:00`
            : `${endDateString}T23:59:59`;

        new AbsenceControllerApi().createNewAbsence({
            absenceCreateDTO: {
                userId: currentUserId!,
                startDate: startIso,
                endDate: endIso,
                reason: reason as any,
                comment,
                assignedTo: managerId,
            },
        })
            .then(() => {
                onSuccess();
                onToast?.({ severity: 'success', summary: 'Submitted', detail: 'Absence request sent successfully.', life: 3000 });
            })
            .catch(err => {
                const detail = err.response?.data?.detail || err.response?.data?.message || err.message || 'An unexpected error occurred.';
                console.error('Server error:', err.response?.data || err);
                toast.current?.show({ severity: 'error', summary: 'Error', detail: String(detail), life: 5000 });
            })
            .finally(() => setIsSubmitting(false));
    };

    const submitDisabled = isSubmitting || managerLoading || managers.length === 0;

    return (
        <div className="absence-form-card">
            <Toast ref={toast} position="top-right" />
            <h2 className="absence-form-title">Absence Request</h2>

            <form className="absence-form-body" onSubmit={handleSubmit}>
                <div className="absence-form-field">
                    <label className="absence-form-label">Reason of absence</label>

                    <Dropdown
                        value={reason}
                        options={reasonOptions}
                        onChange={(e) => setReason(e.value as AbsenceReason)}
                        className="absence-form-dropdown"
                        panelClassName="absence-form-dropdown-panel"
                        appendTo={document.body}
                    />
                </div>

                <div className="absence-form-date-row">
                    <div className="absence-form-field">
                        <label className="absence-form-label">Start Date</label>

                        <Calendar
                            value={startDate}
                            onChange={(e) => setStartDate(e.value as Date | null)}
                            dateFormat="dd.mm.yy"
                            showIcon
                            placeholder="tt.mm.jjjj"
                            className="absence-form-calendar"
                            inputClassName="absence-form-input"
                            panelClassName="absence-form-calendar-panel"
                            appendTo={document.body}
                            baseZIndex={12000}
                        />
                    </div>

                    <div className="absence-form-field">
                        <label className="absence-form-label">End Date</label>

                        <Calendar
                            value={endDate}
                            onChange={(e) => setEndDate(e.value as Date | null)}
                            dateFormat="dd.mm.yy"
                            showIcon
                            placeholder="tt.mm.jjjj"
                            className="absence-form-calendar"
                            inputClassName="absence-form-input"
                            panelClassName="absence-form-calendar-panel"
                            appendTo={document.body}
                            baseZIndex={12000}
                        />
                    </div>
                </div>

                <div className="absence-form-checkbox-row">
                    <input
                        className="absence-form-checkbox"
                        type="checkbox"
                        id="includeTime"
                        checked={includeTime}
                        onChange={e => setIncludeTime(e.target.checked)}
                    />

                    <label className="absence-form-checkbox-label" htmlFor="includeTime">
                        Include time
                    </label>
                </div>

                {includeTime && (
                    <div className="absence-form-time-row">
                        <div className="absence-form-time-group">
                            <label className="absence-form-label">From</label>
                            <div className="absence-form-time-selects">
                                <select className="absence-form-time-select" value={startHour} onChange={e => setStartHour(e.target.value)}>
                                    {Array.from({ length: 24 }, (_, i) => String(i).padStart(2, '0')).map(h => (
                                        <option key={h} value={h}>{h}</option>
                                    ))}
                                </select>
                                <span className="absence-form-time-colon">:</span>
                                <select className="absence-form-time-select" value={startMinute} onChange={e => setStartMinute(e.target.value)}>
                                    {Array.from({ length: 12 }, (_, i) => String(i * 5).padStart(2, '0')).map(m => (
                                        <option key={m} value={m}>{m}</option>
                                    ))}
                                </select>
                            </div>
                        </div>

                        <div className="absence-form-time-group">
                            <label className="absence-form-label">Till</label>
                            <div className="absence-form-time-selects">
                                <select className="absence-form-time-select" value={endHour} onChange={e => setEndHour(e.target.value)}>
                                    {Array.from({ length: 24 }, (_, i) => String(i).padStart(2, '0')).map(h => (
                                        <option key={h} value={h}>{h}</option>
                                    ))}
                                </select>
                                <span className="absence-form-time-colon">:</span>
                                <select className="absence-form-time-select" value={endMinute} onChange={e => setEndMinute(e.target.value)}>
                                    {Array.from({ length: 12 }, (_, i) => String(i * 5).padStart(2, '0')).map(m => (
                                        <option key={m} value={m}>{m}</option>
                                    ))}
                                </select>
                            </div>
                        </div>
                    </div>
                )}

                <div className="absence-form-field">
                    <label className="absence-form-label">Select your manager</label>

                    <Dropdown
                        value={managerId}
                        options={managerOptions}
                        onChange={(e) => setManagerId(e.value)}
                        placeholder={
                            managerLoading
                                ? 'Loading managers...'
                                : managers.length === 0
                                    ? 'No manager available'
                                    : 'Select a manager...'
                        }
                        disabled={managerLoading || managers.length === 0}
                        className="absence-form-dropdown"
                        panelClassName="absence-form-dropdown-panel"
                        appendTo={document.body}
                    />
                </div>

                <div className="absence-form-field">
                    <label className="absence-form-label">Optional message</label>

                    <textarea
                        className="absence-form-textarea"
                        value={comment}
                        onChange={e => setComment(e.target.value)}
                        rows={4}
                        placeholder="Dear David, I need to go on a vacation..."
                    />
                </div>

                <div className="absence-form-actions">
                    <button
                        type="button"
                        className="absence-form-cancel-btn"
                        onClick={onCancel}
                    >
                        Cancel
                    </button>

                    <button
                        type="submit"
                        className="absence-form-submit-btn"
                        disabled={submitDisabled}
                    >
                        {isSubmitting ? 'Sending...' : 'Send request'}
                    </button>
                </div>
            </form>
        </div>
    );
};