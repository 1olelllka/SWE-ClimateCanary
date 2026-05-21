import '../styles/Cards.css';
import React from 'react';
import { LineChart } from './LineChart';

type WarningStatus = 'GREEN' | 'YELLOW' | 'RED';

const STATUS_CONFIG: Record<WarningStatus, { label: string}> = {
    GREEN:  { label: '⚠ Mild alert'  },
    YELLOW: { label: '⚠ Moderate warning' },
    RED:    { label: '⚠ Critical warning'  },
};

interface KpiCardProps {
    readonly title: string;
    readonly value: string;
    readonly unit: string;
    readonly color: string;
    readonly points?: string;         // legacy SVG polyline string (DepartmentDetailPage etc.)
    readonly dataPoints?: number[];   // actual metric values for dynamic sparkline
    readonly trendText: string;
    readonly trendIcon: string;
    readonly warningStatus?: WarningStatus;
    readonly tip?: string;
}

export const Cards: React.FC<KpiCardProps> = ({
    title, value, unit, color,
    points, dataPoints,
    trendText, trendIcon,
    warningStatus, tip,
}) => {
    const violated = warningStatus != null;
    const statusCfg = warningStatus ? STATUS_CONFIG[warningStatus] : null;
    const showTip = violated && tip && tip !== "There's no tip.";
    return (
        <div className={`card-wrapper${violated ? ' card-violated' : ''}`}>
            <div className="card-header" style={{ color }}>
                {violated && <span className="card-alert-dot" />}
                <span>{title}</span>
                {statusCfg && (
                    <span className="card-alert-badge">{statusCfg.label}</span>
                )}
            </div>
            <div className="card-value-row" style={{ display: 'flex', alignItems: 'baseline', gap: '0.2rem' }}>
                <span className="card-value" style={{ color }}>{value}</span>
                <span className="card-unit">{unit}</span>
            </div>
            <LineChart color={color} points={points} dataPoints={dataPoints} />
            <div className="card-trend">
                <i className={`pi ${trendIcon}`} style={{ color }}></i>
                <span>{trendText}</span>
            </div>
            {showTip && (
                <div className="card-tip">
                    <span className="card-tip-icon">💡</span>
                    <span>{tip}</span>
                </div>
            )}
        </div>
    );
};
