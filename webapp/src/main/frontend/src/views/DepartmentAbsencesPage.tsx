import React, { useCallback, useEffect, useMemo, useState } from 'react';
import globalAxios from 'axios';
import SidebarComponent from '../components/SidebarComponent';
import { PageHeader } from '../components/PageHeader';
import '../styles/DepartmentAbsencesPage.css';

interface AbsenceListDTO {
    id: string;
    userId?: string | null;
    firstName?: string | null;
    lastName?: string | null;
    roomNumber?: string | null;
    startDate: string;
    endDate: string;
    typeOfAbsence?: string | null;
    status?: string | null;
    createdAt?: string | null;
}

const formatEnum = (value?: string | null) => {
    if (!value) return '-';
    return value.charAt(0) + value.slice(1).toLowerCase().replaceAll('_', ' ');
};

const formatDateRange = (start?: string, end?: string) => {
    if (!start || !end) return '-';

    const startFormatted = new Date(start).toLocaleDateString('de-DE', {
        day: '2-digit',
        month: '2-digit',
        year: '2-digit'
    });

    const endFormatted = new Date(end).toLocaleDateString('de-DE', {
        day: '2-digit',
        month: '2-digit',
        year: '2-digit'
    });

    return `${startFormatted} - ${endFormatted}`;
};

export const DepartmentAbsencesPage: React.FC = () => {
    const [sidebarVisible, setSidebarVisible] = useState(false);
    const [absences, setAbsences] = useState<AbsenceListDTO[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const fetchAbsences = useCallback(() => {
        setLoading(true);
        setError(null);

        globalAxios.get('/api/absences')
            .then(res => {
                setAbsences(res.data.content || []);
            })
            .catch(err => {
                console.error('Fehler beim Laden der Abwesenheiten', err);
                setError('Absences could not be loaded.');
            })
            .finally(() => {
                setLoading(false);
            });
    }, []);

    const updateAbsenceStatus = (absenceId: string, status: 'APPROVED' | 'REJECTED') => {
        globalAxios.patch(`/api/absences/${absenceId}`, { status })
            .then(() => {
                fetchAbsences();
            })
            .catch(err => {
                const serverError = err.response?.data?.detail || err.response?.data?.message || err.message;
                console.error('Fehler beim Aktualisieren der Absence:', serverError);
                alert('Fehler vom Server:\n' + JSON.stringify(serverError, null, 2));
            });
    };

    useEffect(() => {
        fetchAbsences();
    }, [fetchAbsences]);

    const pendingAbsences = useMemo(() => {
        return absences.filter(abs => abs.status === 'PENDING');
    }, [absences]);

    const nonPendingAbsences = useMemo(() => {
        return absences.filter(abs => abs.status !== 'PENDING');
    }, [absences]);

    const todayAbsentCount = useMemo(() => {
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        return absences.filter(abs => {
            const start = new Date(abs.startDate);
            const end = new Date(abs.endDate);

            start.setHours(0, 0, 0, 0);
            end.setHours(0, 0, 0, 0);

            return abs.status === 'APPROVED' && start <= today && today <= end;
        }).length;
    }, [absences]);

    const getReason = (absence: AbsenceListDTO) => {
        return absence.typeOfAbsence ?? null;
    };

    const AbsenceRow = ({ absence, showActions = false }: { absence: AbsenceListDTO; showActions?: boolean }) => (
        <tr className="absence-table-row">
            <td>{absence.firstName || '-'}</td>
            <td>{absence.lastName || '-'}</td>
            <td>{absence.roomNumber || '-'}</td>
            <td>{formatDateRange(absence.startDate, absence.endDate)}</td>
            <td>{formatEnum(getReason(absence))}</td>
            <td>
                {showActions ? (
                    <div className="absence-action-buttons">
                        <button
                            title="Approve"
                            className="absence-action-button approve"
                            onClick={() => updateAbsenceStatus(absence.id, 'APPROVED')}
                        >
                            ✓
                        </button>

                        <button
                            title="Reject"
                            className="absence-action-button reject"
                            onClick={() => updateAbsenceStatus(absence.id, 'REJECTED')}
                        >
                            ✕
                        </button>
                    </div>
                ) : (
                    formatEnum(absence.status)
                )}
            </td>
        </tr>
    );

    return (
        <div className="department-absences-page">
            <PageHeader
                title="Absences"
                subtitle="Department Head"
                onMenuClick={() => setSidebarVisible(true)}
            />

            <SidebarComponent visible={sidebarVisible} onHide={() => setSidebarVisible(false)} />

            <main className="department-absences-main">

                <section className="department-absences-kpi-grid">
                    <KpiBox title="Today absent" value={todayAbsentCount} />
                    <KpiBox title="Current Absence Requests" value={pendingAbsences.length} />
                </section>

                {loading && <p>Loading absences...</p>}
                {error && <p className="department-absences-error">{error}</p>}

                {!loading && !error && (
                    <>
                        <section className="department-absences-section">
                            <h2>List of Absences</h2>
                            <AbsenceTable>
                                {nonPendingAbsences.length === 0 ? (
                                    <EmptyRow />
                                ) : (
                                    nonPendingAbsences.map(absence => (
                                        <AbsenceRow key={absence.id} absence={absence} />
                                    ))
                                )}
                            </AbsenceTable>
                        </section>

                        <section className="department-absences-section">
                            <h2>Current Absence Requests</h2>
                            <AbsenceTable>
                                {pendingAbsences.length === 0 ? (
                                    <EmptyRow />
                                ) : (
                                    pendingAbsences.map(absence => (
                                        <AbsenceRow key={absence.id} absence={absence} showActions />
                                    ))
                                )}
                            </AbsenceTable>
                        </section>
                    </>
                )}
            </main>
        </div>
    );
};

const KpiBox = ({ title, value }: { title: string; value: number }) => (
    <div className="kpi-box">
        <div className="kpi-box-title">{title}</div>
        <div className="kpi-box-value">{value}</div>
    </div>
);

const AbsenceTable = ({ children }: { children: React.ReactNode }) => (
    <div className="absence-table-wrapper">
        <table className="absence-table">
            <thead>
            <tr>
                <th>Firstname</th>
                <th>Lastname</th>
                <th>Room</th>
                <th>Date</th>
                <th>Reason</th>
                <th>Status</th>
            </tr>
            </thead>
            <tbody>
            {children}
            </tbody>
        </table>
    </div>
);

const EmptyRow = () => (
    <tr>
        <td colSpan={6} className="absence-empty-row">
            No absences found.
        </td>
    </tr>
);

export default DepartmentAbsencesPage;