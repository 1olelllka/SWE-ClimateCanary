import React from 'react';
import { ClimateHistoryChart } from './ClimateHistoryChart';
import { getRoomDisplayName, RoomDTO } from '../views/EmployeeDepartmentPage';
import '../styles/DepartmentClimateSection.css';

interface DepartmentClimateSectionProps {
    rooms: RoomDTO[];
    selectedRoomId: string | null;
    onSelectRoom: (roomId: string) => void;
}

export const DepartmentClimateSection: React.FC<DepartmentClimateSectionProps> = ({
    rooms,
    selectedRoomId,
    onSelectRoom,
}) => {
    if (rooms.length === 0 || !selectedRoomId) {
        return null;
    }

    return (
        <section className="department-chart-section">
            <div className="department-chart-room-selector">
                <label htmlFor="department-selected-room">Room</label>

                <select
                    id="department-selected-room"
                    className="department-chart-room-select"
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

            <ClimateHistoryChart roomId={selectedRoomId} />
        </section>
    );
};
