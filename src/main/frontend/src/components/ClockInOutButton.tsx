import React, { useCallback, useEffect, useState } from 'react';
import globalAxios from 'axios';
import '../styles/ClockInOutButton.css';

export const ClockInOutButton: React.FC = () => {
    const [clockedIn, setClockedIn] = useState(false);
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState<string | null>(null);

    const fetchClockStatus = useCallback(() => {
        setLoading(true);
        setMessage(null);

        globalAxios.get('/api/absences/clock-status')
            .then(response => {
                setClockedIn(Boolean(response.data.clockedIn));
            })
            .catch(error => {
                console.error('Could not load clock status', error.response?.status, error.response?.data || error);
                setMessage('Clock status could not be loaded.');
            })
            .finally(() => {
                setLoading(false);
            });
    }, []);

    useEffect(() => {
        fetchClockStatus();
    }, [fetchClockStatus]);

    const getErrorMessage = (error: any) => {
        const status = error.response?.status;
        const detail = error.response?.data?.detail;

        if (detail) {
            return detail;
        }

        if (status === 409) {
            return clockedIn
                ? 'You are not clocked in.'
                : 'You are already clocked in.';
        }

        if (status === 401) {
            return 'You are not logged in.';
        }

        if (status === 403) {
            return 'You are not allowed to perform this action.';
        }

        return 'Clock action failed.';
    };

    const handleClockAction = () => {
        setLoading(true);
        setMessage(null);

        const request = clockedIn
            ? globalAxios.delete('/api/absences/clock-out')
            : globalAxios.post('/api/absences/clock-in');

        request
            .then(() => {
                setClockedIn(previous => !previous);
                setMessage(clockedIn ? 'Clocked out.' : 'Clocked in.');
            })
            .catch(error => {
                console.error('Clock action failed', error.response?.status, error.response?.data || error);
                setMessage(getErrorMessage(error));

                fetchClockStatus();
            })
            .finally(() => {
                setLoading(false);
            });
    };

    return (
        <div className="clock-in-out-container">
            <button
                type="button"
                className={clockedIn ? 'clock-button clock-button-out' : 'clock-button clock-button-in'}
                onClick={handleClockAction}
                disabled={loading}
            >
                <i
                    className={clockedIn ? 'pi pi-sign-out' : 'pi pi-sign-in'}
                    aria-hidden="true"
                />
                <span>
                    {loading
                        ? 'Loading...'
                        : clockedIn
                            ? 'Clock out'
                            : 'Clock in'}
                </span>
            </button>

            {message && (
                <span className="clock-message">
                    {message}
                </span>
            )}
        </div>
    );
};

export default ClockInOutButton;