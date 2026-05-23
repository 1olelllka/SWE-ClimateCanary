import React, { useEffect, useMemo, useState } from 'react';
import { AbsenceControllerApi } from '../generated-skeleton-api';
import { Calendar } from 'primereact/calendar';
import { Dropdown } from 'primereact/dropdown';
import '../styles/CreateAbsenceForm.css';

interface CreateAbsenceFormProps {
    currentUserId: string | null;
    onSuccess: () => void;
    onCancel: () => void;
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
                                                                    }) => {
    const [reason, setReason] = useState<AbsenceReason>('VACATION');
    const [startDate, setStartDate] = useState<Date | null>(null);
    const [endDate, setEndDate] = useState<Date | null>(null);
    const [includeTime, setIncludeTime] = useState(false);
    const [startTime, setStartTime] = useState('07:00');
    const [endTime, setEndTime] = useState('15:00');
    const [managerId, setManagerId] = useState('');
    const [managers, setManagers] = useState<ManagerDTO[]>([]);
    const [comment, setComment] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [managerLoading, setManagerLoading] = useState(true);

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
            alert('Error: User ID not found. Please log in again.');
            return;
        }

        if (!startDate || !endDate) {
            alert('Please select start and end date.');
            return;
        }

        if (!managerId) {
            alert('Please select a manager.');
            return;
        }

        setIsSubmitting(true);

        const startDateString = formatDateForBackend(startDate);
        const endDateString = formatDateForBackend(endDate);

        const startIso = includeTime
            ? `${startDateString}T${startTime}:00`
            : `${startDateString}T00:00:00`;

        const endIso = includeTime
            ? `${endDateString}T${endTime}:00`
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
                alert('Absence request submitted successfully!');
                onSuccess();
            })
            .catch(err => {
                const serverError = err.response?.data?.message || err.response?.data || err.message;
                console.error('Server error:', serverError);
                alert('Server error:\n' + JSON.stringify(serverError, null, 2));
            })
            .finally(() => setIsSubmitting(false));
    };

    const submitDisabled = isSubmitting || managerLoading || managers.length === 0;

    return (
        <div className="absence-form-card">
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
                        <label className="absence-form-time-label">From</label>

                        <input
                            className="absence-form-input"
                            type="time"
                            value={startTime}
                            onChange={e => setStartTime(e.target.value)}
                        />

                        <label className="absence-form-time-label">Till</label>

                        <input
                            className="absence-form-input"
                            type="time"
                            value={endTime}
                            onChange={e => setEndTime(e.target.value)}
                        />
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