import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { UserxControllerApi } from '../generated-skeleton-api';
import { Toast } from 'primereact/toast';
import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import { CreateAbsenceForm } from '../components/CreateAbsenceForm';
import { AbsenceDetailDialog } from '../components/AbsenceDetailDialog';
import type { AbsenceListDTO } from '../components/AbsenceDetailDialog';
import '../styles/EmployeeAbsencesPage.css';

const TOTAL_VACATION_DAYS = 25;
const MAX_IGNORE_MINUTES = 120;

const formatEnum = (value?: string | null) => {
    if (!value) return '-';
    return value.charAt(0) + value.slice(1).toLowerCase().replaceAll('_', ' ');
};

const formatDate = (dateString: string) =>
    new Date(dateString).toLocaleDateString('de-DE', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
    });

const formatDateRange = (startDate: string, endDate: string) => {
    const start = formatDate(startDate);
    const end = formatDate(endDate);
    return start === end ? start : `${start} – ${end}`;
};

const calculateAbsenceDays = (startDate: string, endDate: string): number => {
    const startDay = new Date(startDate);
    startDay.setHours(0, 0, 0, 0);
    const endDay = new Date(endDate);
    endDay.setHours(0, 0, 0, 0);
    return Math.max(
        1,
        Math.floor((endDay.getTime() - startDay.getTime()) / (1000 * 60 * 60 * 24)) + 1,
    );
};

const calculateAbsenceHours = (startDate: string, endDate: string) =>
    calculateAbsenceDays(startDate, endDate) * 8;

const formatIgnoreTime = (minutes: number): string => {
    if (minutes === 0) return '—';
    if (minutes < 60) return `${minutes} min`;
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    return m > 0 ? `${h}h ${m}m` : `${h}h`;
};

const StatusBadge = ({ status }: { status: string }) => {
    const key = status.toLowerCase();
    const knownKeys = ['approved', 'pending', 'rejected', 'cancelled'];
    const cls = knownKeys.includes(key) ? key : 'other';
    return (
        <span className={`absence-status-badge ${cls}`}>
            {formatEnum(status)}
        </span>
    );
};

interface KpiCardProps {
    title: string;
    value: number;
    max: number;
    displayValue?: string;
    barColor?: string;
}

const KpiCard = ({ title, value, max, displayValue, barColor = '#22c55e' }: KpiCardProps) => {
    const percentage = Math.min(100, (value / max) * 100);
    return (
        <div className="absence-kpi-card">
            <div className="absence-kpi-title">{title}</div>
            <div className="absence-kpi-bar-track">
                <div
                    className="absence-kpi-bar-fill"
                    style={{ width: `${percentage}%`, background: barColor }}
                />
            </div>
            <div className="absence-kpi-value">{displayValue ?? value}</div>
        </div>
    );
};

export const EmployeeAbsencesPage: React.FC = () => {
    const toast = useRef<Toast>(null);
    const [sidebarVisible, setSidebarVisible] = useState(false);
    const [showRequestForm, setShowRequestForm] = useState(false);
    const [selectedAbsence, setSelectedAbsence] = useState<AbsenceListDTO | null>(null);
    const [absences, setAbsences] = useState<AbsenceListDTO[]>([]);
    const [loading, setLoading] = useState(true);
    const [currentUserId, setCurrentUserId] = useState<string | null>(null);
    const [sortAscending, setSortAscending] = useState(false);
    const [statusFilter, setStatusFilter] = useState('');

    const fetchAbsences = useCallback(() => {
        setLoading(true);

        new UserxControllerApi().getAuthenticatedUser()
            .then(res => setCurrentUserId((res.data as any).id))
            .catch(err => console.error('Could not load user', err));

        new UserxControllerApi().getPageOfAbsencesOfAuthenticatedUser({ pageable: { page: 0, size: 100, sort: [] } })
            .then(res => setAbsences((res.data.content as any) || []))
            .catch(err => console.error('Could not load absences', err))
            .finally(() => setLoading(false));
    }, []);

    useEffect(() => {
        fetchAbsences();
    }, [fetchAbsences]);

    // ── KPI computations ──────────────────────────────────────────────────────

    const currentYear = new Date().getFullYear();

    const usedVacationDays = useMemo(() =>
        absences
            .filter(a =>
                a.status === 'APPROVED' &&
                a.typeOfAbsence === 'VACATION' &&
                new Date(a.startDate).getFullYear() === currentYear,
            )
            .reduce((sum, a) => sum + calculateAbsenceDays(a.startDate, a.endDate), 0),
        [absences, currentYear],
    );

    const vacationRemaining = Math.max(0, TOTAL_VACATION_DAYS - usedVacationDays);

    const oldestPendingMinutes = useMemo(() => {
        const pending = absences
            .filter(a => a.status === 'PENDING' && a.createdAt)
            .map(a => ({ ...a, createdAtMs: new Date(a.createdAt!).getTime() }))
            .sort((a, b) => a.createdAtMs - b.createdAtMs);

        if (pending.length === 0) return 0;
        return Math.max(0, Math.floor((Date.now() - pending[0].createdAtMs) / 60_000));
    }, [absences]);

    // ── Table sort ────────────────────────────────────────────────────────────

    const displayedAbsences = useMemo(() =>
        [...absences]
            .filter(a => !statusFilter || a.status === statusFilter)
            .sort((a, b) => {
                const diff = new Date(a.startDate).getTime() - new Date(b.startDate).getTime();
                return sortAscending ? diff : -diff;
            }),
        [absences, sortAscending, statusFilter],
    );

    // ─────────────────────────────────────────────────────────────────────────

    return (
        <div className="employee-absences-page">
            <Toast ref={toast} position="top-right" />
            <PageHeader title="My Absences" onMenuClick={() => setSidebarVisible(true)} />
            <SidebarComponent visible={sidebarVisible} onHide={() => setSidebarVisible(false)} />

            <div className="employee-absences-content">

                <p className="employee-absences-section-heading">Overview</p>

                <div className="employee-absences-kpi-grid">
                    <KpiCard
                        title="Vacation Days Remaining"
                        value={vacationRemaining}
                        max={TOTAL_VACATION_DAYS}
                    />
                    <KpiCard
                        title="Used This Year"
                        value={usedVacationDays}
                        max={TOTAL_VACATION_DAYS}
                    />
                    <KpiCard
                        title="Pending Requests"
                        value={absences.filter(a => a.status === 'PENDING').length}
                        max={10}
                    />
                    <KpiCard
                        title="How Long Manager Ignores Me"
                        value={Math.min(oldestPendingMinutes, MAX_IGNORE_MINUTES)}
                        max={MAX_IGNORE_MINUTES}
                        displayValue={formatIgnoreTime(oldestPendingMinutes)}
                        barColor="#f59e0b"
                    />
                </div>

                <div className="employee-absences-controls">
                    <select
                        className="absence-status-filter"
                        value={statusFilter}
                        onChange={e => setStatusFilter(e.target.value)}
                    >
                        <option value="">All statuses</option>
                        <option value="PENDING">Pending</option>
                        <option value="APPROVED">Approved</option>
                        <option value="REJECTED">Rejected</option>
                        <option value="CANCELLED">Cancelled</option>
                    </select>
                    <button
                        type="button"
                        className="absence-request-button"
                        onClick={() => setShowRequestForm(true)}
                    >
                        + Request Absence
                    </button>
                </div>

                {showRequestForm && (
                    <div className="absence-dialog-overlay">
                        <CreateAbsenceForm
                            currentUserId={currentUserId}
                            onSuccess={() => {
                                setShowRequestForm(false);
                                fetchAbsences();
                            }}
                            onCancel={() => setShowRequestForm(false)}
                            onToast={opts => toast.current?.show(opts)}
                        />
                    </div>
                )}

                {selectedAbsence && (
                    <div className="absence-dialog-overlay">
                        <AbsenceDetailDialog
                            absence={selectedAbsence}
                            onClose={() => setSelectedAbsence(null)}
                            onCancelled={() => {
                                setSelectedAbsence(null);
                                fetchAbsences();
                            }}
                        />
                    </div>
                )}

                <div className="employee-absences-table-wrapper">
                    {loading ? (
                        <p className="employee-absences-loading">Loading absences...</p>
                    ) : (
                        <table className="employee-absences-table">
                            <thead>
                                <tr>
                                    <th>Request</th>
                                    <th
                                        className="absence-sortable-th"
                                        onClick={() => setSortAscending(prev => !prev)}
                                    >
                                        Date {sortAscending ? '↑' : '↓'}
                                    </th>
                                    <th>Hours</th>
                                    <th>Status</th>
                                    <th />
                                </tr>
                            </thead>
                            <tbody>
                                {displayedAbsences.length === 0 ? (
                                    <tr>
                                        <td colSpan={5} className="employee-absences-empty-cell">
                                            No absences found.
                                        </td>
                                    </tr>
                                ) : (
                                    displayedAbsences.map(abs => (
                                        <tr key={abs.id}>
                                            <td>{formatEnum(abs.typeOfAbsence)}</td>
                                            <td>{formatDateRange(abs.startDate, abs.endDate)}</td>
                                            <td>{calculateAbsenceHours(abs.startDate, abs.endDate)} h</td>
                                            <td><StatusBadge status={abs.status} /></td>
                                            <td>
                                                <button
                                                    type="button"
                                                    className="absence-detail-trigger"
                                                    onClick={() => setSelectedAbsence(abs)}
                                                    title="View details"
                                                >
                                                    <i className="pi pi-ellipsis-h" />
                                                </button>
                                            </td>
                                        </tr>
                                    ))
                                )}
                            </tbody>
                        </table>
                    )}
                </div>
            </div>
        </div>
    );
};

export default EmployeeAbsencesPage;
