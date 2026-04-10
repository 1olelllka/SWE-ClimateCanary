import '../styles/WarningBanner.css';
import React from 'react';

interface WarningBannerProps {
    boldPart: string;
    regularPart: string;
}

export const WarningBanner: React.FC<WarningBannerProps> = ({ boldPart, regularPart }) => {
    return (
        <div className="warning-banner-wrapper">
            <div className="warning-banner">
                <span className="warning-icon">🌿</span>
                <span className="warning-text">
                    <strong className="warning-bold">{boldPart}</strong>
                    <span className="warning-regular">{regularPart}</span>
                </span>
            </div>
        </div>
    );
};