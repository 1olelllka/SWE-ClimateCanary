import React from 'react';

export interface LimitState {
    temperatureMin: string;
    temperatureMax: string;
    humidityMin: string;
    humidityMax: string;
    airQualityMin: string;
    airQualityMax: string;
}

interface RoomLimitSettingsProps {
    readonly limits: LimitState;
    readonly saving: boolean;
    readonly onLimitChange: (key: keyof LimitState, value: string) => void;
    readonly onSaveLimits: () => void;
}

export const RoomLimitSettings: React.FC<RoomLimitSettingsProps> = ({
    limits,
    saving,
    onLimitChange,
    onSaveLimits,
}) => {
    return (
        <div>
            <div className="bra-limits-head-row">
                <span>Sensor</span>
                <span>Min</span>
                <span>Max</span>
            </div>

            <div className="bra-limit-row">
                <span className="bra-limit-label">Temperature (°C)</span>
                <input
                    className="bra-limit-input"
                    value={limits.temperatureMin}
                    onChange={e => onLimitChange('temperatureMin', e.target.value)}
                />
                <input
                    className="bra-limit-input"
                    value={limits.temperatureMax}
                    onChange={e => onLimitChange('temperatureMax', e.target.value)}
                />
            </div>

            <div className="bra-limit-row">
                <span className="bra-limit-label">Humidity (%)</span>
                <input
                    className="bra-limit-input"
                    value={limits.humidityMin}
                    onChange={e => onLimitChange('humidityMin', e.target.value)}
                />
                <input
                    className="bra-limit-input"
                    value={limits.humidityMax}
                    onChange={e => onLimitChange('humidityMax', e.target.value)}
                />
            </div>

            <div className="bra-limit-row">
                <span className="bra-limit-label">CO₂ (ppm)</span>
                <input
                    className="bra-limit-input"
                    value="—"
                    disabled
                    aria-label="CO₂ minimum is not configurable"
                />
                <input
                    className="bra-limit-input"
                    value={limits.airQualityMax}
                    onChange={e => onLimitChange('airQualityMax', e.target.value)}
                />
            </div>

            <div className="bra-save-row">
                <button
                    type="button"
                    className="bra-save-btn"
                    onClick={onSaveLimits}
                    disabled={saving}
                >
                    {saving ? 'Saving…' : 'Save Limits'}
                </button>
            </div>
        </div>
    );
};

export default RoomLimitSettings;
