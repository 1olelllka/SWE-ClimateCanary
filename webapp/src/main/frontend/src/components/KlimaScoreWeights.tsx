import React, { useState } from 'react';

export interface KlimaScoreWeightsState {
    temperature: number;
    humidity: number;
    co2: number;
}

interface KlimaScoreWeightsProps {
    readonly weights: KlimaScoreWeightsState;
    readonly saving: boolean;
    readonly onSave: (weights: KlimaScoreWeightsState) => void;
}

const TOTAL = 100;

export const KlimaScoreWeights: React.FC<KlimaScoreWeightsProps> = ({
    weights,
    saving,
    onSave,
}) => {
    const [local, setLocal] = useState<KlimaScoreWeightsState>(weights);

    const total = local.temperature + local.humidity + local.co2;
    const isValid = total === TOTAL;

    const handleChange = (field: keyof KlimaScoreWeightsState, raw: string) => {
        const parsed = parseInt(raw, 10);
        const value = isNaN(parsed) || parsed < 0 ? 0 : parsed > 100 ? 100 : parsed;
        setLocal(prev => ({ ...prev, [field]: value }));
    };

    return (
        <div>
            <div className="bra-limits-head-row">
                <span>Sensor</span>
                <span>Weight (%)</span>
            </div>

            <div className="bra-limit-row">
                <span className="bra-limit-label">Temperature</span>
                <input
                    className="bra-limit-input"
                    type="number"
                    min={0}
                    max={100}
                    value={local.temperature}
                    onChange={e => handleChange('temperature', e.target.value)}
                />
            </div>

            <div className="bra-limit-row">
                <span className="bra-limit-label">Humidity</span>
                <input
                    className="bra-limit-input"
                    type="number"
                    min={0}
                    max={100}
                    value={local.humidity}
                    onChange={e => handleChange('humidity', e.target.value)}
                />
            </div>

            <div className="bra-limit-row">
                <span className="bra-limit-label">CO₂</span>
                <input
                    className="bra-limit-input"
                    type="number"
                    min={0}
                    max={100}
                    value={local.co2}
                    onChange={e => handleChange('co2', e.target.value)}
                />
            </div>

            <div className="bra-limit-row">
                <span className="bra-limit-label">
                    <strong>Total</strong>
                </span>
                <span
                    className="bra-limit-input"
                    style={{ color: isValid ? 'green' : 'red', fontWeight: 'bold' }}
                >
                    {total}%
                </span>
            </div>

            {!isValid && (
                <small className="p-error" style={{ display: 'block', marginTop: '4px' }}>
                    Weights must sum to exactly 100%. Currently: {total}%.
                </small>
            )}

            <div className="bra-save-row">
                <button
                    type="button"
                    className="bra-save-btn"
                    onClick={() => onSave(local)}
                    disabled={saving || !isValid}
                >
                    {saving ? 'Saving…' : 'Save Weights'}
                </button>
            </div>
        </div>
    );
};

export default KlimaScoreWeights;
