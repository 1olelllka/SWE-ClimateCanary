import React from 'react';
import { ClimateHistoryChart } from './ClimateHistoryChart';

interface RoomSensorHistoryCardProps {
    readonly roomId: string;
}

export const RoomSensorHistoryCard: React.FC<RoomSensorHistoryCardProps> = ({ roomId }) => {
    return <ClimateHistoryChart roomId={roomId} />;
};

export default RoomSensorHistoryCard;