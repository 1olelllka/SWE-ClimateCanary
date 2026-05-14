import React from 'react';
import {
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
    onToggleRoom: (roomId: string) => void;
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
                                                                                  onToggleRoom
                                                                              }) => {
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
                                            <strong>{formatTemperature(currentClimate?.temperature)}</strong>
                                        </div>

                                        <div>
                                            <span>Current Humidity</span>
                                            <strong>{formatHumidity(currentClimate?.humidity)}</strong>
                                        </div>

                                        <div>
                                            <span>Current Air Quality</span>
                                            <strong>{formatAirQuality(currentClimate?.airQuality)}</strong>
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