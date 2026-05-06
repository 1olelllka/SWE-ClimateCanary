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
    readonly message: string | null;
    readonly onLimitChange: (key: keyof LimitState, value: string) => void;
    readonly onSaveLimits: () => void;
}

export const RoomLimitSettings: React.FC<RoomLimitSettingsProps> = ({
                                                                        limits,
                                                                        saving,
                                                                        message,
                                                                        onLimitChange,
                                                                        onSaveLimits
                                                                    }) => {
    return (
        <section className="room-analysis-section">
            <h2>Limit Settings</h2>

            <div className="room-analysis-limits-table">
                <div className="room-analysis-limits-header">
                    <span>Sensor Type</span>
                    <span>Min</span>
                    <span>Max</span>
                </div>

                <div className="room-analysis-limit-row">
                    <span>Temperature</span>

                    <input
                        value={limits.temperatureMin}
                        onChange={event =>
                            onLimitChange('temperatureMin', event.target.value)
                        }
                    />

                    <input
                        value={limits.temperatureMax}
                        onChange={event =>
                            onLimitChange('temperatureMax', event.target.value)
                        }
                    />
                </div>

                <div className="room-analysis-limit-row">
                    <span>Humidity</span>

                    <input
                        value={limits.humidityMin}
                        onChange={event =>
                            onLimitChange('humidityMin', event.target.value)
                        }
                    />

                    <input
                        value={limits.humidityMax}
                        onChange={event =>
                            onLimitChange('humidityMax', event.target.value)
                        }
                    />
                </div>

                <div className="room-analysis-limit-row">
                    <span>Air Quality</span>

                    <input
                        value="-"
                        disabled
                        aria-label="Air quality minimum is not configurable"
                    />

                    <input
                        value={limits.airQualityMax}
                        onChange={event =>
                            onLimitChange('airQualityMax', event.target.value)
                        }
                    />
                </div>
            </div>

            <div className="room-analysis-save-row">
                <button
                    type="button"
                    className="room-analysis-save-button"
                    onClick={onSaveLimits}
                    disabled={saving}
                >
                    {saving ? 'Saving...' : 'Save Limits'}
                </button>

                {message && (
                    <span className="room-analysis-message">
                        {message}
                    </span>
                )}
            </div>
        </section>
    );
};

export default RoomLimitSettings;