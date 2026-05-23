import React from 'react';
import {
    ActiveWarning,
    ClimateDataPointDTO,
    getRoomDisplayName,
    RoomDTO
} from '../views/EmployeeDepartmentPage';
import '../styles/DepartmentRoomOverview.css';

interface DepartmentRoomOverviewProps {
    rooms: RoomDTO[];
    expandedRoomId: string | null;
    currentClimate: ClimateDataPointDTO | null;
    loadingClimate: boolean;
    warnings: ActiveWarning[];
    onToggleRoom: (roomId: string) => void;
}

function warningColor(status?: string): string | undefined {
    if (status === 'RED') return '#dc2626';
    if (status === 'YELLOW') return '#eab308';
    return undefined;
}

const formatTemperature = (value?: number) => {
    if (value === undefined || value === null) {
        return '-';
    }

    return `${value.toFixed(1).replace('.', ',')} °C`;
};

const formatHumidity = (value?: number) => {
    if (value === undefined || value === null) {
        return '-';
    }

    return `${Math.round(value)} %`;
};

const formatAirQuality = (value?: number) => {
    if (value === undefined || value === null) {
        return '-';
    }

    return `${Math.round(value)} ppm`;
};

export const DepartmentRoomOverview: React.FC<DepartmentRoomOverviewProps> = ({
                                                                                  rooms,
                                                                                  expandedRoomId,
                                                                                  currentClimate,
                                                                                  loadingClimate,
                                                                                  warnings,
                                                                                  onToggleRoom
                                                                              }) => {
    const tempWarning = warnings.find(w => w.measurementType === 'TEMPERATURE');
    const humWarning  = warnings.find(w => w.measurementType === 'HUMIDITY');
    const aqWarning   = warnings.find(w => w.measurementType === 'CO2');
    if (rooms.length === 0) {
        return (
            <section className="department-room-list">
                <div className="department-room-empty">
                    No rooms found.
                </div>
            </section>
        );
    }

    return (
        <section className="department-room-list">
            {rooms.map(room => {
                const expanded = room.id === expandedRoomId;

                return (
                    <button
                        key={room.id}
                        type="button"
                        className={expanded ? 'department-room-card selected' : 'department-room-card'}
                        onClick={() => onToggleRoom(room.id)}
                        aria-expanded={expanded}
                    >
                        <div className="department-room-header">
                            <span className="department-room-title">
                                {getRoomDisplayName(room)}
                            </span>

                            <i
                                className={expanded ? 'pi pi-chevron-up' : 'pi pi-chevron-down'}
                                aria-hidden="true"
                            />
                        </div>

                        {expanded && (
                            <div className="department-room-details">
                                {loadingClimate ? (
                                    <p className="department-room-loading">
                                        Loading climate data...
                                    </p>
                                ) : (
                                    <div className="department-room-values">
                                        <div>
                                            <span>Current Temperature</span>
                                            <strong style={{ color: warningColor(tempWarning?.status) }}>
                                                {currentClimate ? formatTemperature(currentClimate.temperature) : 'N/A'}
                                            </strong>
                                            {tempWarning?.tip && tempWarning.tip !== "There's no tip." && (
                                                <small className="department-room-tip">{tempWarning.tip}</small>
                                            )}
                                        </div>

                                        <div>
                                            <span>Current Humidity</span>
                                            <strong style={{ color: warningColor(humWarning?.status) }}>
                                                {currentClimate ? formatHumidity(currentClimate.humidity) : 'N/A'}
                                            </strong>
                                            {humWarning?.tip && humWarning.tip !== "There's no tip." && (
                                                <small className="department-room-tip">{humWarning.tip}</small>
                                            )}
                                        </div>

                                        <div>
                                            <span>Current Air Quality</span>
                                            <strong style={{ color: warningColor(aqWarning?.status) }}>
                                                {currentClimate ? formatAirQuality(currentClimate.airQuality) : 'N/A'}
                                            </strong>
                                            {aqWarning?.tip && aqWarning.tip !== "There's no tip." && (
                                                <small className="department-room-tip">{aqWarning.tip}</small>
                                            )}
                                        </div>
                                    </div>
                                )}
                            </div>
                        )}
                    </button>
                );
            })}
        </section>
    );
};