import React, { useEffect, useState } from 'react';
import globalAxios from 'axios';
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

export const CreateAbsenceForm: React.FC<CreateAbsenceFormProps> = ({
    currentUserId,
    onSuccess,
    onCancel,
}) => {
    const [reason, setReason] = useState<'VACATION' | 'ILLNESS' | 'OTHER'>('VACATION');
    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');
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
        globalAxios.get('/api/absences/managers')
            .then(res => setManagers(res.data || []))
            .catch(err => {
                console.error('Could not load managers', err);
                setManagers([]);
            })
            .finally(() => setManagerLoading(false));
    }, []);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();

        if (!currentUserId) {
            alert('Error: User ID not found. Please log in again.');
            return;
        }

        if (!managerId) {
            alert('Please select a manager.');
            return;
        }

        setIsSubmitting(true);

        const startIso = includeTime ? `${startDate}T${startTime}:00` : `${startDate}T00:00:00`;
        const endIso   = includeTime ? `${endDate}T${endTime}:00`   : `${endDate}T23:59:59`;

        globalAxios.post('/api/absences', {
            userId:     currentUserId,
            startDate:  startIso,
            endDate:    endIso,
            reason,
            comment,
            assignedTo: managerId,
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
                    <select
                        className="absence-form-select"
                        value={reason}
                        onChange={e => setReason(e.target.value as 'VACATION' | 'ILLNESS' | 'OTHER')}
                    >
                        <option value="VACATION">Vacation</option>
                        <option value="ILLNESS">Illness</option>
                        <option value="OTHER">Other</option>
                    </select>
                </div>

                <div className="absence-form-date-row">
                    <div className="absence-form-field">
                        <label className="absence-form-label">Start Date</label>
                        <input
                            className="absence-form-input"
                            type="date"
                            value={startDate}
                            onChange={e => setStartDate(e.target.value)}
                            required
                        />
                    </div>
                    <div className="absence-form-field">
                        <label className="absence-form-label">End Date</label>
                        <input
                            className="absence-form-input"
                            type="date"
                            value={endDate}
                            onChange={e => setEndDate(e.target.value)}
                            required
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
                    <select
                        className="absence-form-select"
                        value={managerId}
                        onChange={e => setManagerId(e.target.value)}
                        required
                        disabled={managerLoading || managers.length === 0}
                    >
                        <option value="" disabled>
                            {managerLoading
                                ? 'Loading managers...'
                                : managers.length === 0
                                    ? 'No manager available'
                                    : 'Select a manager...'}
                        </option>
                        {managers.map(manager => (
                            <option key={manager.id} value={manager.id}>
                                {manager.firstName} {manager.lastName}
                            </option>
                        ))}
                    </select>
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
