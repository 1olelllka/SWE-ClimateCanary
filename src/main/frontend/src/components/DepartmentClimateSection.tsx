import React from 'react';
import {
    getRoomDisplayName,
    RoomDTO
} from '../views/EmployeeDepartmentPage';

interface DepartmentClimateSectionProps {
    rooms: RoomDTO[];
    selectedRoomId: string | null;
    selectedRoom: RoomDTO | null;
    onSelectRoom: (roomId: string) => void;
}

export const DepartmentClimateSection: React.FC<DepartmentClimateSectionProps> = ({
                                                                                      rooms,
                                                                                      selectedRoomId,
                                                                                      selectedRoom,
                                                                                      onSelectRoom
                                                                                  }) => {
    if (rooms.length === 0 || !selectedRoomId) {
        return null;
    }

    return (
        <section className="department-chart-section">
            <div className="department-chart-controls">
                <label htmlFor="department-selected-room">Select room</label>

                <select
                    id="department-selected-room"
                    value={selectedRoomId}
                    onChange={event => onSelectRoom(event.target.value)}
                >
                    {rooms.map(room => (
                        <option key={room.id} value={room.id}>
                            {getRoomDisplayName(room)}
                        </option>
                    ))}
                </select>
            </div>

            <div className="department-time-tabs">
                <button type="button">Day</button>
                <button type="button" className="active">Week</button>
                <button type="button">Month</button>
                <button type="button" aria-label="Open calendar">
                    <i className="pi pi-calendar" />
                </button>
            </div>

            <div className="department-chart-wrapper">
                <p className="department-chart-placeholder-text">
                    Climate chart will be added here.
                </p>
            </div>

            {selectedRoom && (
                <p className="department-selected-room-note">
                    Showing overview for {getRoomDisplayName(selectedRoom)}.
                </p>
            )}
        </section>
    );
};