import React from 'react';
import { Dropdown } from 'primereact/dropdown';
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

    const roomOptions = rooms.map(room => ({
        label: getRoomDisplayName(room),
        value: room.id,
    }));

    return (
        <section className="department-chart-section">
            <div className="department-chart-room-selector">
                <label htmlFor="department-selected-room">Room</label>

                <Dropdown
                    inputId="department-selected-room"
                    value={selectedRoomId}
                    options={roomOptions}
                    onChange={event => onSelectRoom(event.value)}
                    className="department-chart-room-dropdown"
                    panelClassName="department-chart-room-dropdown-panel"
                    appendTo="self"
                />
            </div>

            <ClimateHistoryChart roomId={selectedRoomId} />
        </section>
    );
};