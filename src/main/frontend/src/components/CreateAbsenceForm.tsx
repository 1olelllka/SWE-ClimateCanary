import React, { useEffect, useState } from 'react';
import globalAxios from 'axios';

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

export const CreateAbsenceForm: React.FC<CreateAbsenceFormProps> = ({ currentUserId, onSuccess, onCancel }) => {
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
            .then(res => {
                setManagers(res.data || []);
            })
            .catch(err => {
                console.error('Manager konnten nicht geladen werden', err);
                setManagers([]);
            })
            .finally(() => {
                setManagerLoading(false);
            });
    }, []);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();

        if (!currentUserId) {
            alert('Fehler: User-ID nicht gefunden. Bitte neu einloggen.');
            return;
        }

        if (!managerId) {
            alert('Bitte wähle einen Manager aus.');
            return;
        }

        setIsSubmitting(true);

        const startIso = includeTime ? `${startDate}T${startTime}:00` : `${startDate}T00:00:00`;
        const endIso = includeTime ? `${endDate}T${endTime}:00` : `${endDate}T23:59:59`;

        const payload = {
            userId: currentUserId,
            startDate: startIso,
            endDate: endIso,
            reason,
            comment,
            assignedTo: managerId
        };

        globalAxios.post('/api/absences', payload)
            .then(() => {
                alert('Absence request submitted successfully!');
                onSuccess();
            })
            .catch(err => {
                const serverFehler = err.response?.data?.message || err.response?.data || err.message;
                console.error('Exakter Fehler vom Server:', serverFehler);
                alert('Fehler vom Server:\n' + JSON.stringify(serverFehler, null, 2));
            })
            .finally(() => {
                setIsSubmitting(false);
            });
    };

    const labelStyle = { display: 'block', fontSize: '1rem', color: '#0f172a', marginBottom: '0.5rem' };
    const inputStyle = { width: '100%', padding: '0.75rem', borderRadius: '8px', border: 'none', backgroundColor: '#ffffff', fontSize: '1rem' };

    return (
        <div style={{ background: '#e2e8f0', padding: '2rem', borderRadius: '12px', maxWidth: '500px', margin: '0 auto' }}>
            <h2 style={{ marginTop: 0, textAlign: 'center', fontSize: '1.8rem', color: '#0f172a', marginBottom: '2rem' }}>
                Absence Request
            </h2>

            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                <div>
                    <label style={labelStyle}>Reason of absence</label>
                    <select
                        value={reason}
                        onChange={e => setReason(e.target.value as 'VACATION' | 'ILLNESS' | 'OTHER')}
                        style={{ ...inputStyle, width: 'auto', paddingRight: '2rem' }}
                    >
                        <option value="VACATION">Vacation</option>
                        <option value="ILLNESS">Illness</option>
                        <option value="OTHER">Other</option>
                    </select>
                </div>

                <div style={{ display: 'flex', gap: '1rem' }}>
                    <div style={{ flex: 1 }}>
                        <label style={labelStyle}>Start Date</label>
                        <input
                            type="date"
                            value={startDate}
                            onChange={e => setStartDate(e.target.value)}
                            required
                            style={inputStyle}
                        />
                    </div>
                    <div style={{ flex: 1 }}>
                        <label style={labelStyle}>End Date</label>
                        <input
                            type="date"
                            value={endDate}
                            onChange={e => setEndDate(e.target.value)}
                            required
                            style={inputStyle}
                        />
                    </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                    <input
                        type="checkbox"
                        id="includeTime"
                        checked={includeTime}
                        onChange={e => setIncludeTime(e.target.checked)}
                        style={{ width: '20px', height: '20px', borderRadius: '4px', border: 'none' }}
                    />
                    <label htmlFor="includeTime" style={{ fontSize: '1.1rem', color: '#0f172a' }}>
                        Include time
                    </label>
                </div>

                {includeTime && (
                    <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
                        <label style={{ fontSize: '1.1rem' }}>From</label>
                        <input
                            type="time"
                            value={startTime}
                            onChange={e => setStartTime(e.target.value)}
                            style={{ ...inputStyle, flex: 1 }}
                        />
                        <label style={{ fontSize: '1.1rem' }}>Till</label>
                        <input
                            type="time"
                            value={endTime}
                            onChange={e => setEndTime(e.target.value)}
                            style={{ ...inputStyle, flex: 1 }}
                        />
                    </div>
                )}

                <div>
                    <label style={labelStyle}>Select your manager</label>
                    <select
                        value={managerId}
                        onChange={e => setManagerId(e.target.value)}
                        required
                        disabled={managerLoading || managers.length === 0}
                        style={inputStyle}
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

                <div>
                    <label style={labelStyle}>Optional message</label>
                    <textarea
                        value={comment}
                        onChange={e => setComment(e.target.value)}
                        rows={4}
                        placeholder="Dear David, I need to go on a vacation..."
                        style={{ ...inputStyle, resize: 'vertical' }}
                    />
                </div>

                <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '1rem' }}>
                    <button
                        type="button"
                        onClick={onCancel}
                        style={{ padding: '0.75rem 2rem', background: '#9ca3af', color: '#fff', border: 'none', borderRadius: '20px', cursor: 'pointer', fontSize: '1rem', fontWeight: '500' }}
                    >
                        Cancel
                    </button>
                    <button
                        type="submit"
                        disabled={isSubmitting || managerLoading || managers.length === 0}
                        style={{ padding: '0.75rem 2rem', background: '#3b82f6', color: '#fff', border: 'none', borderRadius: '20px', cursor: isSubmitting || managerLoading || managers.length === 0 ? 'not-allowed' : 'pointer', fontSize: '1rem', fontWeight: '500' }}
                    >
                        {isSubmitting ? 'Sending...' : 'Send request'}
                    </button>
                </div>
            </form>
        </div>
    );
};