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
 */
export function useTemperature() {
    const { prefs } = useUserPreferences();
    const isFahrenheit = prefs?.fahrenheit ?? false;

    const convert = (celsius: number): number =>
        isFahrenheit ? cToF(celsius) : celsius;

    /** Converts a Celsius delta (difference) to the preferred unit. */
    const convertDelta = (deltaC: number): number =>
        isFahrenheit ? deltaC * 9 / 5 : deltaC;

    /** Converts celsius and formats to string; returns 'N/A' for undefined/null. */
    const formatTemp = (celsius: number | undefined | null, decimals = 1): string => {
        if (celsius == null) return 'N/A';
        return convert(celsius).toFixed(decimals);
    };

    const unit = isFahrenheit ? '°F' : '°C';

    return { isFahrenheit, convert, convertDelta, formatTemp, unit };
}
