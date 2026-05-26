import { useCallback } from 'react';
import { useUserPreferences } from '../Contexts/UserPreferencesContext';

const cToF = (c: number) => (c * 9) / 5 + 32;

/**
 * Returns helpers for temperature display that respect the user's °C / °F preference.
 *
 * `convert(celsius)`  — returns the value in the preferred unit (number)
 * `formatTemp(celsius, decimals?)` — converts + formats to string (e.g. "23.4")
 * `unit`             — "°C" or "°F"
 * `isFahrenheit`     — boolean flag
 * `convertDelta(deltaC)` — converts a Celsius *delta* (for trend text)
 *
 * All returned functions are wrapped in useCallback so their references are
 * stable across re-renders (only change when the °C/°F preference changes).
 * This prevents them from being listed as changed deps and triggering
 * useEffect / useCallback re-runs in every consumer component.
 */
export function useTemperature() {
    const { prefs } = useUserPreferences();
    const isFahrenheit = prefs?.fahrenheit ?? false;

    const convert = useCallback(
        (celsius: number): number => (isFahrenheit ? cToF(celsius) : celsius),
        [isFahrenheit],
    );

    /** Converts a Celsius delta (difference) to the preferred unit. */
    const convertDelta = useCallback(
        (deltaC: number): number => (isFahrenheit ? (deltaC * 9) / 5 : deltaC),
        [isFahrenheit],
    );

    /** Converts celsius and formats to string; returns 'N/A' for undefined/null. */
    const formatTemp = useCallback(
        (celsius: number | undefined | null, decimals = 1): string => {
            if (celsius == null) return 'N/A';
            return (isFahrenheit ? cToF(celsius) : celsius).toFixed(decimals);
        },
        [isFahrenheit],
    );

    const unit = isFahrenheit ? '°F' : '°C';

    return { isFahrenheit, convert, convertDelta, formatTemp, unit };
}
