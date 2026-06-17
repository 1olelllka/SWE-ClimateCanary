import React, { useEffect, useState } from 'react';
import { Button } from 'primereact/button';

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

interface LocalState {
    temperature: string;
    humidity: string;
    co2: string;
}

const parse = (s: string): number => {
    const v = parseFloat(s);
    return isNaN(v) ? NaN : v;
};

const isValidWeight = (v: number) =>
    !isNaN(v) && isFinite(v) && v >= 0 && Math.round(v * 2) === v * 2;

const ROW_STYLE: React.CSSProperties = {
    display: 'grid',
    gridTemplateColumns: '1fr 6rem',
    alignItems: 'center',
    gap: '0 1rem',
    padding: '0.7rem 0',
    borderBottom: '1px solid var(--card-border, #e8eef4)',
};

const LABEL_STYLE: React.CSSProperties = {
    fontSize: '0.82rem',
    color: 'var(--text-primary, #1a3a4a)',
};

const INPUT_STYLE: React.CSSProperties = {
    width: '100%',
    height: '2.25rem',
    border: '1px solid #cbd5e1',
    borderRadius: '8px',
    padding: '0 0.6rem',
    fontSize: '0.82rem',
    textAlign: 'right',
    background: 'var(--card-bg, #ffffff)',
    color: 'var(--text-primary, #1a3a4a)',
    boxSizing: 'border-box',
    outline: 'none',
};

export const KlimaScoreWeights: React.FC<KlimaScoreWeightsProps> = ({ weights, saving, onSave }) => {
    const [local, setLocal] = useState<LocalState>({
        temperature: String(weights.temperature),
        humidity:    String(weights.humidity),
        co2:         String(weights.co2),
    });

    useEffect(() => {
        setLocal({
            temperature: String(weights.temperature),
            humidity:    String(weights.humidity),
            co2:         String(weights.co2),
        });
    }, [weights]);

    const tVal = parse(local.temperature);
    const hVal = parse(local.humidity);
    const cVal = parse(local.co2);

    const safeNum = (v: number) => (isNaN(v) ? 0 : v);
    const total = safeNum(tVal) + safeNum(hVal) + safeNum(cVal);
    const totalOk = Math.abs(total - 100) < 0.001;

    const allValid = isValidWeight(tVal) && isValidWeight(hVal) && isValidWeight(cVal);
    const canSave  = allValid && totalOk;

    const handleChange = (field: keyof LocalState, value: string) =>
        setLocal(prev => ({ ...prev, [field]: value }));

    const handleSave = () => {
        if (!canSave) return;
        onSave({ temperature: tVal, humidity: hVal, co2: cVal });
    };

    const HEADER_LABEL: React.CSSProperties = {
        fontSize: '0.7rem',
        fontWeight: 700,
        textTransform: 'uppercase',
        letterSpacing: '0.4px',
        color: 'var(--text-secondary, #64748b)',
    };

    return (
        <div style={{ padding: '1.25rem 1.5rem' }}>

            {/* Column headers */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 6rem', gap: '0 1rem', paddingBottom: '0.5rem', borderBottom: '1px solid var(--card-border, #e8eef4)' }}>
                <span style={HEADER_LABEL}>Sensor</span>
                <span style={{ ...HEADER_LABEL, textAlign: 'right' }}>Weight&nbsp;(%)</span>
            </div>

            {/* Input rows */}
            {([
                { field: 'temperature' as const, label: 'Temperature' },
                { field: 'humidity'    as const, label: 'Humidity' },
                { field: 'co2'         as const, label: 'CO₂' },
            ] as const).map(({ field, label }) => (
                <div key={field} style={ROW_STYLE}>
                    <span style={LABEL_STYLE}>{label}</span>
                    <input
                        type="number"
                        step="0.5"
                        min={0}
                        max={100}
                        value={local[field]}
                        onChange={e => handleChange(field, e.target.value)}
                        style={INPUT_STYLE}
                    />
                </div>
            ))}

            {/* Total row */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 6rem', gap: '0 1rem', alignItems: 'center', padding: '0.7rem 0' }}>
                <span style={{ ...LABEL_STYLE, fontWeight: 700 }}>Total</span>
                <span style={{ fontSize: '0.82rem', fontWeight: 700, textAlign: 'right', color: totalOk ? '#16a34a' : '#dc2626' }}>
                    {parseFloat(total.toFixed(2))}%
                </span>
            </div>

            {/* Validation messages */}
            {!allValid && (
                <p style={{ margin: '0 0 0.5rem', fontSize: '0.78rem', color: '#dc2626' }}>
                    Each weight must be a positive number in increments of 0.5.
                </p>
            )}
            {allValid && !totalOk && (
                <p style={{ margin: '0 0 0.5rem', fontSize: '0.78rem', color: '#dc2626' }}>
                    Weights must sum to exactly 100%. Currently: {parseFloat(total.toFixed(2))}%.
                </p>
            )}

            {/* Save button */}
            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '0.75rem' }}>
                <Button
                    label={saving ? 'Saving…' : 'Save Weights'}
                    icon="pi pi-check"
                    className="admin-add-button"
                    onClick={handleSave}
                    disabled={saving || !canSave}
                    loading={saving}
                />
            </div>
        </div>
    );
};

export default KlimaScoreWeights;
