import React from 'react';

interface MiniSparklineProps {
    readonly color: string;
    readonly points: string;
}

export const LineChart: React.FC<MiniSparklineProps> = ({ color, points }) => {
    return (
        <div style={{ height: '40px', width: '100%' }}>
            <svg viewBox="0 0 100 30" preserveAspectRatio="none" style={{ width: '100%', height: '100%' }}>
                <line x1="0" y1="20" x2="100" y2="20" stroke="#e8eef4" strokeWidth="1" strokeDasharray="4 4" />
                <polyline points={points} fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
        </div>
    );
};