import React from 'react';
import { ClimateHistoryChart } from './ClimateHistoryChart';

interface RoomSensorHistoryCardProps {
    readonly roomId: string;
}

export const RoomSensorHistoryCard: React.FC<RoomSensorHistoryCardProps> = ({ roomId }) => {
    return (
        <section className="room-analysis-section room-analysis-chart-section">
            <h2>Historical Sensor Data</h2>
            <ClimateHistoryChart roomId={roomId} />
        </section>
    );
};

export default RoomSensorHistoryCard;