import { useUserPreferences } from '../Contexts/UserPreferencesContext';

/**
 * Returns helpers for time/datetime display that respect the user's
 * 24-hour / 12-hour preference.
 *
 * `formatTime(date)`      — returns "14:30"  or "2:30 PM"
 * `formatDatetime(date)`  — returns "24.05.2025 14:30"  or "24.05.2025 2:30 PM"
 * `is12Hour`              — boolean flag
 */
export function useTimeFormat() {
    const { prefs } = useUserPreferences();
    const is12Hour = prefs?.twelveHourFormat ?? false;

    const formatTime = (date: Date | string | null | undefined): string => {
        if (!date) return '--:--';
        const d = typeof date === 'string' ? new Date(date) : date;
        const p = (n: number) => String(n).padStart(2, '0');

        if (is12Hour) {
            let h = d.getHours();
            const ampm = h >= 12 ? 'PM' : 'AM';
            h = h % 12 || 12;
            return `${h}:${p(d.getMinutes())} ${ampm}`;
        }
        return `${p(d.getHours())}:${p(d.getMinutes())}`;
    };

    /**
     * Full date + time: "24.05.2025 14:30" or "24.05.2025 2:30 PM".
     * @param fallback  Returned when date is falsy (default '-').
     */
    const formatDatetime = (
        date: Date | string | null | undefined,
        fallback = '-',
    ): string => {
        if (!date) return fallback;
        const d = typeof date === 'string' ? new Date(date) : date;
        const p = (n: number) => String(n).padStart(2, '0');
        const datePart = `${p(d.getDate())}.${p(d.getMonth() + 1)}.${d.getFullYear()}`;
        return `${datePart} ${formatTime(d)}`;
    };

    return { is12Hour, formatTime, formatDatetime };
}
