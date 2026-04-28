import React, { useState, useEffect, useCallback, useMemo } from 'react';
import globalAxios from 'axios';
import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import { CreateAbsenceForm } from '../components/CreateAbsenceForm';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '../utilities/routes.paths';

interface AbsenceListDTO {
    id: string;
    typeOfAbsence: string;
    startDate: string;
    endDate: string;
    status: string;
}

export const EmployeeAbsencesPage: React.FC = () => {
    const [sidebarVisible, setSidebarVisible] = useState(false);
    const [showRequestForm, setShowRequestForm] = useState(false);
    const [absences, setAbsences] = useState<AbsenceListDTO[]>([]);
    const [loading, setLoading] = useState(true);
    const [currentUserId, setCurrentUserId] = useState<string | null>(null);
    const navigate = useNavigate();
    const [sortAscending, setSortAscending] = useState(false);

    const fetchAbsences = useCallback(() => {
        setLoading(true);
        globalAxios.get('/api/users/me')
            .then(res => {
                setCurrentUserId(res.data.id);
            })
            .catch(err => console.error("Konnte User nicht laden", err));

        globalAxios.get('/api/users/me/absences')
            .then(res => {
                setAbsences(res.data.content || []);
            })
            .catch(err => console.error("Fehler beim Laden der Abwesenheiten", err))
            .finally(() => setLoading(false));
    }, []);

    useEffect(() => {
        fetchAbsences();
    }, [fetchAbsences]);

    const formatDate = (dateString: string) => {
        return new Date(dateString).toLocaleDateString('de-DE', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric'
        });
    };

    const formatDateRange = (startDate: string, endDate: string) => {
        const start = formatDate(startDate);
        const end = formatDate(endDate);

        if (start === end) {
            return start;
        }

        return `${start} - ${end}`;
    };

    const KpiCard = ({ title, value, max }: { title: string, value: number, max: number }) => {
        const percentage = Math.min(100, (value / max) * 100);
        return (
            <div style={{ background: '#e2e8f0', padding: '1rem', borderRadius: '4px', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                <div style={{ fontSize: '0.9rem', color: '#334155', minHeight: '40px' }}>{title}</div>
                <div style={{ background: '#f1f5f9', height: '12px', width: '100%', borderRadius: '6px', overflow: 'hidden' }}>
                    <div style={{ background: '#4ade80', height: '100%', width: `${percentage}%` }}></div>
                </div>
                <div style={{ textAlign: 'right', fontWeight: 'bold', color: '#0f172a' }}>{value}</div>
            </div>
        );
    };

    const formatEnum = (value?: string | null) => {
        if (!value) return '-';
        return value.charAt(0) + value.slice(1).toLowerCase();
    };

    const calculateAbsenceHours = (startDate: string, endDate: string) => {
        const start = new Date(startDate);
        const end = new Date(endDate);

        const startDay = new Date(start);
        startDay.setHours(0, 0, 0, 0);

        const endDay = new Date(end);
        endDay.setHours(0, 0, 0, 0);

        const dayDiff =
            Math.floor((endDay.getTime() - startDay.getTime()) / (1000 * 60 * 60 * 24)) + 1;

        return Math.max(dayDiff, 1) * 8;
    };

    const sortedAbsences = useMemo(() => {
        return [...absences].sort((a, b) => {
            const dateA = new Date(a.startDate).getTime();
            const dateB = new Date(b.startDate).getTime();

            return sortAscending ? dateA - dateB : dateB - dateA;
        });
    }, [absences, sortAscending]);

    return (
        <div style={{ minHeight: '100vh', backgroundColor: '#f1f5f9', display: 'flex', flexDirection: 'column' }}>
            <PageHeader title="My Absences" onMenuClick={() => setSidebarVisible(true)} />
            <SidebarComponent visible={sidebarVisible} onHide={() => setSidebarVisible(false)} />

            <div style={{ padding: '1.5rem', maxWidth: '800px', margin: '0 auto', width: '100%', flexGrow: 1 }}>

                {/* Zurück Button */}
                <button
                    onClick={() => navigate(ROUTES.HOME)}
                    style={{ background: 'none', border: 'none', cursor: 'pointer', marginBottom: '1.5rem', display: 'flex', alignItems: 'center' }}
                >
                    <i className="pi pi-arrow-circle-left" style={{ fontSize: '2rem', color: '#0f172a' }}></i>
                </button>

                <h3 style={{ marginTop: 0, marginBottom: '1rem' }}>Overview</h3>

                {/* KPI Grid (Aktuell mit statischen Werten, da API noch keine Quoten liefert) */}
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '2rem' }}>
                    <KpiCard title="Vacation Days Remaining" value={18} max={25} />
                    <KpiCard title="Used This Year" value={7} max={25} />
                    <KpiCard title="Pending Requests" value={absences.filter(a => a.status === 'PENDING').length} max={10} />
                    <KpiCard title="Zeitausgleich?" value={18} max={25} />
                </div>

                {/* Controls */}
                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem', marginBottom: '1.5rem' }}>
                    <button
                        type="button"
                        onClick={() => setSortAscending(prev => !prev)}
                        style={{ padding: '0.5rem 1rem', background: '#e2e8f0', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
                    >
                        Sort by date {sortAscending ? '↑' : '↓'}
                    </button>
                    <button
                        onClick={() => setShowRequestForm(!showRequestForm)}
                        style={{ padding: '0.5rem 1rem', background: '#e2e8f0', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
                    >
                        + Request Absence
                    </button>
                </div>

                {/* Overlay für das Formular */}
                {showRequestForm && (
                    <div style={{
                        position: 'fixed',
                        top: 0,
                        left: 0,
                        width: '100vw',
                        height: '100vh',
                        backgroundColor: 'rgba(15, 23, 42, 0.7)',
                        display: 'flex',
                        justifyContent: 'center',
                        alignItems: 'center',
                        zIndex: 9999,
                        padding: '1rem'
                    }}>
                        <div style={{ position: 'relative', width: '100%', maxWidth: '550px' }}>
                            <CreateAbsenceForm
                                currentUserId={currentUserId}
                                onSuccess={() => {
                                    setShowRequestForm(false);
                                    fetchAbsences();
                                }}
                                onCancel={() => setShowRequestForm(false)}
                            />
                        </div>
                    </div>
                )}

                {/* Tabelle */}
                <div style={{ overflowX: 'auto' }}>
                    {loading ? (
                        <p>Loading absences...</p>
                    ) : (
                        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
                            <thead>
                            <tr style={{ borderBottom: '2px solid #cbd5e1' }}>
                                <th style={{ padding: '0.75rem 0.5rem' }}>Request</th>
                                <th style={{ padding: '0.75rem 0.5rem' }}>Date</th>
                                <th style={{ padding: '0.75rem 0.5rem' }}>Hours</th>
                                <th style={{ padding: '0.75rem 0.5rem' }}>Status</th>
                                <th style={{ padding: '0.75rem 0.5rem' }}></th>
                            </tr>
                            </thead>
                            <tbody>
                            {sortedAbsences.map((abs, index) => (                                <tr key={abs.id} style={{ backgroundColor: index % 2 !== 0 ? '#94a3b8' : 'transparent', color: index % 2 !== 0 ? 'white' : 'inherit' }}>
                                    <td style={{ padding: '0.75rem 0.5rem' }}>
                                        {formatEnum(abs.typeOfAbsence)}                                    </td>
                                    <td style={{ padding: '0.75rem 0.5rem' }}>
                                        {formatDateRange(abs.startDate, abs.endDate)}
                                    </td>                                    <td style={{ padding: '0.75rem 0.5rem' }}>
                                        {calculateAbsenceHours(abs.startDate, abs.endDate)} h
                                    </td>
                                    <td style={{ padding: '0.75rem 0.5rem' }}>
                                        {formatEnum(abs.status)}                                    </td>
                                    <td style={{ padding: '0.75rem 0.5rem', textAlign: 'right' }}>
                                        <i className="pi pi-ellipsis-h" style={{ cursor: 'pointer' }}></i>
                                    </td>
                                </tr>
                            ))}
                            {absences.length === 0 && (
                                <tr>
                                    <td colSpan={5} style={{ textAlign: 'center', padding: '1rem' }}>No absences found.</td>
                                </tr>
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