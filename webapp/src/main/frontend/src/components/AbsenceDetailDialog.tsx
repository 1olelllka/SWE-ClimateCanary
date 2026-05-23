import React, { useState } from 'react';
import { AbsenceControllerApi } from '../generated-skeleton-api';
import '../styles/CreateAbsenceForm.css';

export interface AbsenceListDTO {
    id: string;
    typeOfAbsence: string;
    startDate: string;
    endDate: string;
    status: string;
    createdAt?: string | null;
    comment?: string | null;
    managerFirstName?: string | null;
    managerLastName?: string | null;
}

interface AbsenceDetailDialogProps {
    absence: AbsenceListDTO;
    onClose: () => void;
    onCancelled: () => void;
}

const formatEnum = (value?: string | null) => {
    if (!value) return '-';
    return value.charAt(0) + value.slice(1).toLowerCase().replaceAll('_', ' ');
};

const toDateInput = (iso: string) => iso.slice(0, 10);

const toTimeInput = (iso: string) => {
    const d = new Date(iso);
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
};

const hasSpecificTime = (iso: string, isEnd: boolean) => {
    const d = new Date(iso);
    return isEnd
        ? !(d.getHours() === 23 && d.getMinutes() === 59)
        : !(d.getHours() === 0  && d.getMinutes() === 0);
};

export const AbsenceDetailDialog: React.FC<AbsenceDetailDialogProps> = ({
    absence,
    onClose,
    onCancelled,
}) => {
    const [cancelling, setCancelling] = useState(false);

    const showTime =
        hasSpecificTime(absence.startDate, false) ||
        hasSpecificTime(absence.endDate, true);

    const managerName =
        [absence.managerFirstName, absence.managerLastName].filter(Boolean).join(' ') || '-';

    const handleCancelRequest = () => {
        setCancelling(true);
        new AbsenceControllerApi().cancelAbsence({ absenceId: absence.id })
            .then(() => onCancelled())
            .catch(err => {
                const msg = err.response?.data?.message || err.message;
                alert('Could not cancel absence:\n' + msg);
            })
            .finally(() => setCancelling(false));
    };

    return (
        <div className="absence-form-card">
            <h2 className="absence-form-title">Absence Details</h2>

            <div className="absence-form-body">
                <div className="absence-form-field">
                    <span className="absence-form-label">Reason of absence</span>
                    <input
                        className="absence-form-input"
                        type="text"
                        value={formatEnum(absence.typeOfAbsence)}
                        readOnly
                    />
                </div>

                <div className="absence-form-date-row">
                    <div className="absence-form-field">
                        <span className="absence-form-label">Start Date</span>
                        <input
                            className="absence-form-input"
                            type="date"
                            value={toDateInput(absence.startDate)}
                            readOnly
                        />
                    </div>
                    <div className="absence-form-field">
                        <span className="absence-form-label">End Date</span>
                        <input
                            className="absence-form-input"
                            type="date"
                            value={toDateInput(absence.endDate)}
                            readOnly
                        />
                    </div>
                </div>

                {showTime && (
                    <div className="absence-form-time-row">
                        <span className="absence-form-time-label">From</span>
                        <input
                            className="absence-form-input"
                            type="time"
                            value={toTimeInput(absence.startDate)}
                            readOnly
                        />
                        <span className="absence-form-time-label">Till</span>
                        <input
                            className="absence-form-input"
                            type="time"
                            value={toTimeInput(absence.endDate)}
                            readOnly
                        />
                    </div>
                )}

                <div className="absence-form-field">
                    <span className="absence-form-label">Manager</span>
                    <input
                        className="absence-form-input"
                        type="text"
                        value={managerName}
                        readOnly
                    />
                </div>

                <div className="absence-form-field">
                    <span className="absence-form-label">Optional message</span>
                    <textarea
                        className="absence-form-textarea"
                        value={absence.comment ?? ''}
                        rows={3}
                        readOnly
                        placeholder="No message provided"
                    />
                </div>

                <div className="absence-form-field">
                    <span className="absence-form-label">Status</span>
                    <input
                        className="absence-form-input"
                        type="text"
                        value={formatEnum(absence.status)}
                        readOnly
                    />
                </div>

                <div className="absence-form-actions">
                    <button
                        type="button"
                        className="absence-form-cancel-btn"
                        onClick={onClose}
                    >
                        Close
                    </button>

                    {absence.status === 'PENDING' && (
                        <button
                            type="button"
                            className="absence-detail-cancel-btn"
                            onClick={handleCancelRequest}
                            disabled={cancelling}
                        >
                            {cancelling ? 'Cancelling...' : 'Cancel Request'}
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
};
