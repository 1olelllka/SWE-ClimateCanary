import '../styles/Cards.css';
import React from 'react';
import { LineChart } from './LineChart';

interface KpiCardProps {
    readonly title: string;
    readonly value: string;
    readonly unit: string;
    readonly color: string;
    readonly points: string;
    readonly trendText: string;
    readonly trendIcon: string;
}

export const Cards: React.FC<KpiCardProps> = ({ title, value, unit, color, points, trendText, trendIcon }) => {
    return (
        <div className="card-wrapper">
            <div className="card-header" style={{ color: color }}>{title}</div>
            <div className="card-value-row" style={{ display: 'flex', alignItems: 'baseline', gap: '0.2rem' }}>
                <span className="card-value" style={{ color: color }}>{value}</span>
                <span className="card-unit">{unit}</span>
            </div>
            <LineChart color={color} points={points} />
            <div className="card-trend">
                <i className={`pi ${trendIcon}`} style={{ color: color }}></i>
                <span>{trendText}</span>
            </div>
        </div>
    );
};