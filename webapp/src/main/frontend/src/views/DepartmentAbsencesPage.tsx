import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { AbsenceControllerApi, AbsenceDTO, AbsencePatchDTOStatusEnum } from '../generated-skeleton-api';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';
import { Toast } from 'primereact/toast';
import SidebarComponent from '../components/SidebarComponent';
import { PageHeader } from '../components/PageHeader';
import '../styles/DepartmentAbsencesPage.css';
import '../styles/Tables.css';
import '../styles/CreateAbsenceForm.css';

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
    const opts: Intl.DateTimeFormatOptions = { day: '2-digit', month: '2-digit', year: '2-digit' };
    return `${new Date(start).toLocaleDateString('de-DE', opts)} - ${new Date(end).toLocaleDateString('de-DE', opts)}`;
};

const formatDate = (iso?: string | null) => {
    if (!iso) return '-';
    const d = new Date(iso);
    const p = (n: number) => String(n).padStart(2, '0');
    return `${p(d.getDate())}.${p(d.getMonth() + 1)}.${d.getFullYear()}`;
};

const formatDatetime = (iso?: string | null) => {
    if (!iso) return '-';
    const d = new Date(iso);
    const p = (n: number) => String(n).padStart(2, '0');
    return `${p(d.getDate())}.${p(d.getMonth() + 1)}.${d.getFullYear()} ${p(d.getHours())}:${p(d.getMinutes())}`;
};

export const DepartmentAbsencesPage: React.FC = () => {
    const toastRef = useRef<Toast>(null);
    const [sidebarVisible, setSidebarVisible] = useState(false);
    const [absences, setAbsences] = useState<AbsenceListDTO[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // --- View dialog ---
    const [viewDialogVisible, setViewDialogVisible] = useState(false);
    const [viewingAbsence, setViewingAbsence] = useState<AbsenceListDTO | null>(null);
    const [viewingAbsenceDetail, setViewingAbsenceDetail] = useState<AbsenceDTO | null>(null);
    const [absenceDetailLoading, setAbsenceDetailLoading] = useState(false);
    const [absenceActionLoading, setAbsenceActionLoading] = useState(false);

    const fetchAbsences = useCallback(() => {
        setLoading(true);
        setError(null);
        new AbsenceControllerApi().getAllAbsences({ pageable: { page: 0, size: 100, sort: [] } })
            .then(res => setAbsences((res.data.content as any) || []))
            .catch(() => setError('Absences could not be loaded.'))
            .finally(() => setLoading(false));
    }, []);

    useEffect(() => { fetchAbsences(); }, [fetchAbsences]);

    const pendingAbsences = useMemo(() => absences.filter(a => a.status === 'PENDING'), [absences]);
    const nonPendingAbsences = useMemo(() => absences.filter(a => a.status !== 'PENDING'), [absences]);

    const todayAbsentCount = useMemo(() => {
        const today = new Date(); today.setHours(0, 0, 0, 0);
        return absences.filter(a => {
            const s = new Date(a.startDate); s.setHours(0, 0, 0, 0);
            const e = new Date(a.endDate);   e.setHours(0, 0, 0, 0);
            return a.status === 'APPROVED' && s <= today && today <= e;
        }).length;
    }, [absences]);

    const handleViewRequest = useCallback((absence: AbsenceListDTO) => {
        setViewingAbsence(absence);
        setViewingAbsenceDetail(null);
        setViewDialogVisible(true);
        setAbsenceDetailLoading(true);
        new AbsenceControllerApi().getSpecificAbsence({ absenceId: absence.id })
            .then(res => setViewingAbsenceDetail(res.data))
            .catch(() => setViewingAbsenceDetail(null))
            .finally(() => setAbsenceDetailLoading(false));
    }, []);

    const handleAbsenceAction = useCallback(async (status: 'APPROVED' | 'REJECTED') => {
        if (!viewingAbsence) return;
        setAbsenceActionLoading(true);
        try {
            await new AbsenceControllerApi().updateStatusOfAbsence({
                absenceId: viewingAbsence.id,
                absencePatchDTO: { status: status as AbsencePatchDTOStatusEnum },
            });
            toastRef.current?.show({
                severity: status === 'APPROVED' ? 'success' : 'warn',
                summary: status === 'APPROVED' ? 'Approved' : 'Rejected',
                detail: `Request for ${viewingAbsence.firstName ?? ''} ${viewingAbsence.lastName ?? ''} has been ${status.toLowerCase()}.`,
                life: 3000,
            });
            setViewDialogVisible(false);
            fetchAbsences();
        } catch {
            toastRef.current?.show({
                severity: 'error',
                summary: 'Error',
                detail: `Failed to ${status === 'APPROVED' ? 'approve' : 'reject'} the request.`,
                life: 3000,
            });
        } finally {
            setAbsenceActionLoading(false);
        }
    }, [viewingAbsence, fetchAbsences]);

    return (
        <div className="department-absences-page">
            <Toast ref={toastRef} />
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
                            <AbsenceTable lastColHeader="">
                                {pendingAbsences.length === 0 ? (
                                    <EmptyRow />
                                ) : (
                                    pendingAbsences.map(absence => (
                                        <AbsenceRow
                                            key={absence.id}
                                            absence={absence}
                                            showActions
                                            onView={handleViewRequest}
                                        />
                                    ))
                                )}
                            </AbsenceTable>
                        </section>
                    </>
                )}
            </main>

            {/* ── Absence Request Dialog ── */}
            <Dialog
                header="Absence Request"
                visible={viewDialogVisible}
                className="admin-form-dialog"
                style={{ width: '480px' }}
                onHide={() => setViewDialogVisible(false)}
                draggable={false}
                footer={
                    <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.65rem' }}>
                        <Button
                            label="Reject"
                            icon="pi pi-times"
                            severity="danger"
                            loading={absenceActionLoading}
                            onClick={() => handleAbsenceAction('REJECTED')}
                        />
                        <Button
                            label="Approve"
                            icon="pi pi-check"
                            severity="success"
                            loading={absenceActionLoading}
                            onClick={() => handleAbsenceAction('APPROVED')}
                        />
                    </div>
                }
            >
                {absenceDetailLoading ? (
                    <div style={{ padding: '1.5rem', textAlign: 'center', color: '#64748b' }}>Loading…</div>
                ) : viewingAbsence && (
                    <div className="admin-dialog-body">

                        <div className="absence-form-date-row">
                            <div className="absence-form-field">
                                <span className="absence-form-label">First Name</span>
                                <input className="absence-form-input" value={viewingAbsence.firstName ?? '-'} readOnly />
                            </div>
                            <div className="absence-form-field">
                                <span className="absence-form-label">Last Name</span>
                                <input className="absence-form-input" value={viewingAbsence.lastName ?? '-'} readOnly />
                            </div>
                        </div>

                        <div className="absence-form-date-row">
                            <div className="absence-form-field">
                                <span className="absence-form-label">Room</span>
                                <input className="absence-form-input" value={viewingAbsence.roomNumber ?? '-'} readOnly />
                            </div>
                            <div className="absence-form-field">
                                <span className="absence-form-label">Reason</span>
                                <input className="absence-form-input" value={formatEnum(viewingAbsence.typeOfAbsence)} readOnly />
                            </div>
                        </div>

                        <div className="absence-form-date-row">
                            <div className="absence-form-field">
                                <span className="absence-form-label">Start Date</span>
                                <input className="absence-form-input" value={formatDate(viewingAbsence.startDate)} readOnly />
                            </div>
                            <div className="absence-form-field">
                                <span className="absence-form-label">End Date</span>
                                <input className="absence-form-input" value={formatDate(viewingAbsence.endDate)} readOnly />
                            </div>
                        </div>

                        {viewingAbsence.createdAt && (
                            <div className="absence-form-field">
                                <span className="absence-form-label">Submitted On</span>
                                <input className="absence-form-input" value={formatDatetime(viewingAbsence.createdAt)} readOnly />
                            </div>
                        )}

                        <div className="absence-form-field">
                            <span className="absence-form-label">Message</span>
                            <textarea
                                className="absence-form-textarea"
                                value={viewingAbsenceDetail?.comment ?? ''}
                                rows={3}
                                readOnly
                                placeholder="No message provided"
                            />
                        </div>

                    </div>
                )}
            </Dialog>
        </div>
    );
};

const KpiBox = ({ title, value }: { title: string; value: number }) => (
    <div className="kpi-box">
        <div className="kpi-box-title">{title}</div>
        <div className="kpi-box-value">{value}</div>
    </div>
);

const AbsenceTable = ({ children, lastColHeader = 'Status' }: {
    children: React.ReactNode;
    lastColHeader?: string;
}) => (
    <div className="absence-table-wrapper">
        <table className="absence-table">
            <thead>
                <tr>
                    <th>Firstname</th>
                    <th>Lastname</th>
                    <th>Room</th>
                    <th>Date</th>
                    <th>Reason</th>
                    <th>{lastColHeader}</th>
                </tr>
            </thead>
            <tbody>
                {children}
            </tbody>
        </table>
    </div>
);

const AbsenceRow = ({
    absence,
    showActions = false,
    onView,
}: {
    absence: AbsenceListDTO;
    showActions?: boolean;
    onView?: (absence: AbsenceListDTO) => void;
}) => (
    <tr className="absence-table-row">
        <td>{absence.firstName || '-'}</td>
        <td>{absence.lastName || '-'}</td>
        <td>{absence.roomNumber || '-'}</td>
        <td>{formatDateRange(absence.startDate, absence.endDate)}</td>
        <td>{formatEnum(absence.typeOfAbsence)}</td>
        <td>
            {showActions
                ? <button className="btn-primary-small" onClick={() => onView?.(absence)}>View</button>
                : formatEnum(absence.status)
            }
        </td>
    </tr>
);

const EmptyRow = () => (
    <tr>
        <td colSpan={6} className="absence-empty-row">No absences found.</td>
    </tr>
);

export default DepartmentAbsencesPage;
