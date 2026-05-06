import React, { useMemo, useState } from 'react';

export interface ViolationDTO {
    readonly id?: string;
    readonly date?: string;
    readonly timestamp?: string;
    readonly sensorType?: string;
    readonly type?: string;
    readonly measured?: number | string | null;
    readonly measuredValue?: number | string | null;
    readonly max?: number | string | null;
    readonly maxValue?: number | string | null;
    readonly min?: number | string | null;
    readonly minValue?: number | string | null;
    readonly duration?: string | number | null;
}

interface RoomViolationLogTableProps {
    readonly violations: ViolationDTO[];
}

const formatDate = (value?: string) => {
    if (!value) return '-';

    return new Date(value).toLocaleDateString('de-DE', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
    });
};

const normalizeValue = (value: number | string | null | undefined) => {
    if (value === null || value === undefined || value === '') return '-';
    return String(value).replace('.', ',');
};

export const RoomViolationLogTable: React.FC<RoomViolationLogTableProps> = ({ violations }) => {
    const [violationFilter, setViolationFilter] = useState('ALL');

    const filteredViolations = useMemo(() => {
        if (violationFilter === 'ALL') {
            return violations;
        }

        return violations.filter(violation => {
            const label = (violation.sensorType || violation.type || '').toLowerCase();
            return label.includes(violationFilter.toLowerCase());
        });
    }, [violations, violationFilter]);

    return (
        <section className="room-analysis-section">
            <div className="room-analysis-section-header">
                <h2>Violation Log</h2>

                <select
                    className="room-analysis-filter-select"
                    value={violationFilter}
                    onChange={event => setViolationFilter(event.target.value)}
                >
                    <option value="ALL">Sensor Type Filter</option>
                    <option value="Temperature">Temperature</option>
                    <option value="Humidity">Humidity</option>
                    <option value="Air Quality">Air Quality</option>
                </select>
            </div>

            <div className="room-analysis-table-wrapper">
                <table className="room-analysis-table">
                    <thead>
                    <tr>
                        <th>Date</th>
                        <th>Sensor Type</th>
                        <th>Measured</th>
                        <th>Max</th>
                        <th>Min</th>
                        <th>Duration</th>
                    </tr>
                    </thead>

                    <tbody>
                    {filteredViolations.map((violation, index) => {
                        const sensorLabel = violation.sensorType || violation.type || '-';
                        const measured = violation.measured ?? violation.measuredValue;
                        const max = violation.max ?? violation.maxValue;
                        const min = violation.min ?? violation.minValue;
                        const date = violation.date ?? violation.timestamp;

                        return (
                            <tr key={violation.id || `${sensorLabel}-${index}`}>
                                <td>{formatDate(date)}</td>
                                <td>{sensorLabel}</td>
                                <td>{normalizeValue(measured)}</td>
                                <td>{normalizeValue(max)}</td>
                                <td>{normalizeValue(min)}</td>
                                <td>{violation.duration || '-'}</td>
                            </tr>
                        );
                    })}

                    {filteredViolations.length === 0 && (
                        <tr>
                            <td colSpan={6} className="room-analysis-empty-row">
                                No violations found.
                            </td>
                        </tr>
                    )}
                    </tbody>
                </table>
            </div>
        </section>
    );
};

export default RoomViolationLogTable;