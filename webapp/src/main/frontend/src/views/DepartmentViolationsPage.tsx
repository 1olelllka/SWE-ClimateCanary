import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { DepartmentControllerApi, UserxControllerApi, WarningControllerApi } from '../generated-skeleton-api';
import { UserxDTO } from '../generated-skeleton-api';
import { ThresholdViolationsTable, ThresholdViolationData } from '../components/ThresholdViolationsTable';
import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import { Dropdown } from 'primereact/dropdown';
import { Calendar } from 'primereact/calendar';
import { InputSwitch } from 'primereact/inputswitch';

interface WarningDTO {
    id: string;
    roomId: string;
    measurementType: string;
    status: 'GREEN' | 'YELLOW' | 'RED' | string;
    message: string;
    triggeredValue: number;
    activeLimitAtTime: number;
    createdAt: string;
    active: boolean;
}

interface RoomDTO {
    id: string;
    roomNumber?: string | null;
    name?: string | null;
}

const extractArrayResponse = <T,>(data: unknown): T[] => {
    const paged = data as { content?: unknown };
    if (Array.isArray(paged?.content)) return paged.content as T[];
    if (Array.isArray(data)) return data as T[];
    return [];
};

const getMeasurementUnit = (t?: string) => t === 'TEMPERATURE' ? '°C' : t === 'HUMIDITY' ? '%' : t === 'AIR' ? 'ppm' : '';

const formatSensorName = (t?: string) =>
    t === 'TEMPERATURE' ? 'Temperature' : t === 'HUMIDITY' ? 'Humidity' : t === 'AIR' ? 'CO₂' : (t ?? 'Unknown');

const formatNumber = (value: number | null | undefined, t?: string): string => {
    if (value == null || Number.isNaN(value)) return 'n/a';
    const unit = getMeasurementUnit(t);
    return t === 'AIR' ? `${value.toFixed(0)} ${unit}`.trim() : `${value.toFixed(1).replace('.', ',')} ${unit}`.trim();
};

const formatDatetime = (iso?: string): string => {
    if (!iso) return 'n/a';
    const d = new Date(iso);
    const p = (n: number) => String(n).padStart(2, '0');
    return `${p(d.getDate())}.${p(d.getMonth() + 1)}.${d.getFullYear()} ${p(d.getHours())}:${p(d.getMinutes())}`;
};

const parseDT = (dt: string): Date => {
    const [datePart = '', timePart = '00:00'] = dt.split(' ');
    const [dd, mm, yyyy] = datePart.split('.');
    const [hh, min] = timePart.split(':');
    return new Date(+yyyy, +mm - 1, +dd, +hh, +min);
};

const mapWarning = (w: WarningDTO, roomLabel: string): ThresholdViolationData => ({
    id:           w.id,
    status:       w.status,
    active:       w.active,
    sensor:       formatSensorName(w.measurementType),
    room:         roomLabel,
    limit:        `${w.triggeredValue < w.activeLimitAtTime ? 'Min' : 'Max'}: ${formatNumber(w.activeLimitAtTime, w.measurementType)}`,
    measuredValue: formatNumber(w.triggeredValue, w.measurementType),
    datetime:     formatDatetime(w.createdAt),
});

export const DepartmentViolationsPage: React.FC = () => {
    const [sidebarVisible, setSidebarVisible] = useState(false);
    const [violations, setViolations] = useState<ThresholdViolationData[]>([]);
    const [loading, setLoading]       = useState(true);
    const [error, setError]           = useState<string | null>(null);

    const [showActiveOnly, setShowActiveOnly] = useState(false);
    const [selectedRoom,   setSelectedRoom]   = useState<string | null>(null);
    const [selectedSensor, setSelectedSensor] = useState<string | null>(null);
    const [dateRange,      setDateRange]      = useState<Date[] | null>(null);

    const fetchViolations = useCallback(() => {
        setLoading(true);
        setError(null);
        const today = new Date().toISOString().slice(0, 10);

        new UserxControllerApi().getAuthenticatedUser()
            .then(res => {
                const deptId = (res.data as UserxDTO).myRoom?.departmentID;
                if (!deptId) throw new Error('No department assigned.');
                return new DepartmentControllerApi().getSpecificDepartment({ departmentId: deptId });
            })
            .then(res => {
                const rooms = extractArrayResponse<RoomDTO>(res.data.rooms);
                if (rooms.length === 0) return [];
                return Promise.all(
                    rooms.map(room =>
                        new WarningControllerApi().getWarningsForRoom({
                            roomId: room.id!,
                            activeOnly: false,
                            startDate: '2000-01-01',
                            endDate: today,
                        })
                        .then(r => {
                            const warnings = Array.isArray(r.data) ? r.data as WarningDTO[] : [];
                            const label = room.roomNumber || room.name || room.id;
                            return warnings.map(w => mapWarning(w, label));
                        })
                        .catch(() => [])
                    )
                ).then(results => results.flat());
            })
            .then(flat => {
                flat.sort((a, b) => parseDT(b.datetime).getTime() - parseDT(a.datetime).getTime());
                setViolations(flat);
            })
            .catch(() => setError('Could not load threshold violations.'))
            .finally(() => setLoading(false));
    }, []);

    useEffect(() => { fetchViolations(); }, [fetchViolations]);

    const roomOptions = useMemo(() =>
        [...new Set(violations.map(v => v.room))].sort().map(r => ({ label: r, value: r })),
    [violations]);

    const sensorOptions = useMemo(() =>
        [...new Set(violations.map(v => v.sensor))].sort().map(s => ({ label: s, value: s })),
    [violations]);

    const filtered = useMemo(() => {
        const [rangeStart, rangeEnd] = dateRange ?? [null, null];
        const start = rangeStart ? new Date(rangeStart.setHours(0, 0, 0, 0)) : null;
        const end   = rangeEnd   ? new Date(rangeEnd.setHours(23, 59, 59, 999)) : null;

        return violations.filter(v => {
            if (showActiveOnly && !v.active) return false;
            if (selectedRoom   && v.room   !== selectedRoom)   return false;
            if (selectedSensor && v.sensor !== selectedSensor) return false;
            if (start && end) {
                const vt = parseDT(v.datetime).getTime();
                if (vt < start.getTime() || vt > end.getTime()) return false;
            }
            return true;
        });
    }, [violations, showActiveOnly, selectedRoom, selectedSensor, dateRange]);

    return (
        <div className="dashboard-layout">
            <PageHeader title="Threshold Violations" onMenuClick={() => setSidebarVisible(true)} />
            <SidebarComponent visible={sidebarVisible} onHide={() => setSidebarVisible(false)} />

            <div className="dashboard-content">
                {error && <p style={{ color: '#dc2626', marginBottom: '1rem' }}>{error}</p>}

                <div style={{
                    display:        'flex',
                    alignItems:     'center',
                    gap:            '0.75rem',
                    marginBottom:   '1.25rem',
                    flexWrap:       'wrap',
                }}>
                    <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer' }}>
                        <InputSwitch
                            checked={showActiveOnly}
                            onChange={e => setShowActiveOnly(e.value ?? false)}
                        />
                        <span style={{ fontSize: '0.875rem', fontWeight: 500, whiteSpace: 'nowrap' }}>
                            Active only
                        </span>
                    </label>

                    <Dropdown
                        value={selectedRoom}
                        options={roomOptions}
                        onChange={e => setSelectedRoom(e.value ?? null)}
                        placeholder="All rooms"
                        showClear
                        style={{ minWidth: '9rem' }}
                    />

                    <Dropdown
                        value={selectedSensor}
                        options={sensorOptions}
                        onChange={e => setSelectedSensor(e.value ?? null)}
                        placeholder="All sensors"
                        showClear
                        style={{ minWidth: '9rem' }}
                    />

                    <Calendar
                        value={dateRange as Date[]}
                        onChange={e => setDateRange((e.value as Date[] | null) ?? null)}
                        selectionMode="range"
                        numberOfMonths={1}
                        readOnlyInput
                        showIcon
                        showButtonBar
                        dateFormat="dd.mm.yy"
                        placeholder="Date range"
                        style={{ minWidth: '13rem' }}
                        panelClassName="compact-datepicker"
                    />
                </div>

                <ThresholdViolationsTable violations={filtered} loading={loading} fullPage />
            </div>
        </div>
    );
};

export default DepartmentViolationsPage;
