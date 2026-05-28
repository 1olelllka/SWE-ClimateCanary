import { useCallback } from 'react';
import { useUserPreferences } from '../Contexts/UserPreferencesContext';

/**
 * Returns helpers for time/date/datetime display that respect the user's
 * 24-hour / 12-hour and date-format preferences.
 *
 * `formatTime(date)`      — "14:30"  or "2:30 PM"
 * `formatDate(date)`      — "24.05.2025"  /  "05/24/2025"  /  "2025-05-24"
 * `formatDatetime(date)`  — combines formatDate + formatTime
 * `is12Hour`              — boolean flag
 * `dateFormat`            — raw enum value: 'DD_MM_YYYY' | 'MM_DD_YYYY' | 'YYYY_MM_DD'
 */
export function useTimeFormat() {
    const { prefs } = useUserPreferences();
    const is12Hour  = prefs?.twelveHourFormat ?? false;
    const dateFormat = (prefs?.format ?? 'DD_MM_YYYY') as string;

    const formatTime = useCallback((date: Date | string | null | undefined): string => {
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
    }, [is12Hour]);

    /**
     * Format date part only, using the user's date-format preference.
     * DD_MM_YYYY → "24.05.2025"
     * MM_DD_YYYY → "05/24/2025"
     * YYYY_MM_DD → "2025-05-24"
     */
    const formatDate = useCallback((
        date: Date | string | null | undefined,
        fallback = '-',
    ): string => {
        if (!date) return fallback;
        const d = typeof date === 'string' ? new Date(date) : date;
        const p  = (n: number) => String(n).padStart(2, '0');
        const dd   = p(d.getDate());
        const mm   = p(d.getMonth() + 1);
        const yyyy = String(d.getFullYear());
        if (dateFormat === 'MM_DD_YYYY') return `${mm}/${dd}/${yyyy}`;
        if (dateFormat === 'YYYY_MM_DD') return `${yyyy}-${mm}-${dd}`;
        return `${dd}.${mm}.${yyyy}`;          // DD_MM_YYYY (default)
    }, [dateFormat]);

    /**
     * Full date + time.
     * @param fallback  Returned when date is falsy (default '-').
     */
    const formatDatetime = useCallback((
        date: Date | string | null | undefined,
        fallback = '-',
    ): string => {
        if (!date) return fallback;
        const d = typeof date === 'string' ? new Date(date) : date;
        return `${formatDate(d)} ${formatTime(d)}`;
    }, [formatDate, formatTime]);

    return { is12Hour, dateFormat, formatTime, formatDate, formatDatetime };
}
