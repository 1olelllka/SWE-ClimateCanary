import React, { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { InputSwitch } from 'primereact/inputswitch';
import { Dropdown } from 'primereact/dropdown';
import { Toast } from 'primereact/toast';
import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import { ROUTES } from '../utilities/routes.paths';
import { useUserPreferences } from '../Contexts/UserPreferencesContext';
import { useTheme } from '../Contexts/ThemeContext';
import { UserSettingsPatchDTOFormatEnum } from '../generated-skeleton-api';
import '../styles/SettingsPage.css';
import '../styles/Tables.css';

// ── mapping helpers ──────────────────────────────────────────────────────────

const TEMP_OPTS = ['°C', '°F'];
const TIME_OPTS = ['24-hour (14:30)', '12-hour (2:30 PM)'];
const DATE_OPTS = ['DD/MM/YYYY', 'MM/DD/YYYY', 'YYYY-MM-DD'];

const tempFromBackend  = (v?: boolean) => v ? '°F' : '°C';
const tempToBackend    = (v: string)   => v === '°F';

const timeFromBackend  = (v?: boolean) => v ? '12-hour (2:30 PM)' : '24-hour (14:30)';
const timeToBackend    = (v: string)   => v === '12-hour (2:30 PM)';

const dateFromBackend  = (v?: string): string => {
    if (v === 'MM_DD_YYYY') return 'MM/DD/YYYY';
    if (v === 'YYYY_MM_DD') return 'YYYY-MM-DD';
    return 'DD/MM/YYYY';
};
const dateToBackend = (v: string): UserSettingsPatchDTOFormatEnum => {
    if (v === 'MM/DD/YYYY') return UserSettingsPatchDTOFormatEnum.MM_DD_YYYY;
    if (v === 'YYYY-MM-DD') return UserSettingsPatchDTOFormatEnum.YYYY_MM_DD;
    return UserSettingsPatchDTOFormatEnum.DD_MM_YYYY;
};

// ── component ────────────────────────────────────────────────────────────────

export const SettingsPage: React.FC = () => {
    const [sidebarVisible, setSidebarVisible] = useState(false);
    const navigate = useNavigate();
    const toastRef = useRef<Toast>(null);

    const { prefs, savePrefs } = useUserPreferences();
    const { isDarkMode } = useTheme();

    const [email,           setEmail]           = useState('');
    const [temperatureUnit, setTemperatureUnit] = useState('°C');
    const [timeFormat,      setTimeFormat]      = useState('24-hour (14:30)');
    const [dayFormat,       setDayFormat]       = useState('DD/MM/YYYY');
    const [saving,          setSaving]          = useState(false);

    const [notifications, setNotifications] = useState({
        warnings:     false,
        absences:     false,
    });

    // Populate local state once settings are loaded from backend
    useEffect(() => {
        if (!prefs) return;
        setTemperatureUnit(tempFromBackend(prefs.fahrenheit));
        setTimeFormat(timeFromBackend(prefs.twelveHourFormat));
        setDayFormat(dateFromBackend(prefs.format));
        setEmail(prefs.notificationEmail ?? '');
        setNotifications({
            warnings: prefs.emailWarnings ?? false,
            absences: prefs.emailAbsences ?? false,
        });
    }, [prefs]);

    const warnNoEmail = () => {
        toastRef.current?.show({
            severity: 'warn',
            summary:  'No email address',
            detail:   'Add your email address first to enable notifications.',
            life:     4000,
        });
    };

    const toggleNotification = (key: keyof typeof notifications) => {
        const turningOn = !notifications[key];
        if (turningOn && !email.trim()) {
            warnNoEmail();
            return;
        }
        setNotifications(prev => ({ ...prev, [key]: !prev[key] }));
    };

    const handleSave = async () => {
        const anyNotifOn = notifications.warnings || notifications.absences;
        if (anyNotifOn && !email.trim()) {
            warnNoEmail();
            return;
        }
        setSaving(true);
        try {
            await savePrefs({
                darkMode:          isDarkMode,
                fahrenheit:        tempToBackend(temperatureUnit),
                twelveHourFormat:  timeToBackend(timeFormat),
                format:            dateToBackend(dayFormat),
                notificationEmail: email.trim() || undefined,
                emailWarnings:     notifications.warnings,
                emailAbsences:     notifications.absences,
            });
            toastRef.current?.show({
                severity: 'success',
                summary:  'Saved',
                detail:   'Your preferences have been saved.',
                life:     3000,
            });
        } catch {
            toastRef.current?.show({
                severity: 'error',
                summary:  'Error',
                detail:   'Could not save preferences. Please try again.',
                life:     3000,
            });
        } finally {
            setSaving(false);
        }
    };

    const PREFS = [
        { label: 'Temperature Unit', value: temperatureUnit, set: setTemperatureUnit, opts: TEMP_OPTS },
        { label: 'Time Format',      value: timeFormat,      set: setTimeFormat,      opts: TIME_OPTS },
        { label: 'Date Format',      value: dayFormat,       set: setDayFormat,       opts: DATE_OPTS },
    ];

    const NOTIFS: { key: keyof typeof notifications; label: string }[] = [
        { key: 'warnings', label: 'Warnings' },
        { key: 'absences', label: 'Absences' },
    ];

    return (
        <div className="dashboard-layout">
            <Toast ref={toastRef} />
            <PageHeader title="Settings" onMenuClick={() => setSidebarVisible(true)} />
            <SidebarComponent visible={sidebarVisible} onHide={() => setSidebarVisible(false)} />

            <div className="dashboard-content settings-wrap">

                <div className="table-container">
                    <div className="flex-header">
                        <h3>Email Address</h3>
                    </div>
                    <div className="settings-card-body">
                        <p className="settings-desc">
                            Manage the email address used for notifications.
                        </p>
                        <div className="settings-field">
                            <label className="settings-field-label">Email</label>
                            <input
                                type="email"
                                className="settings-input"
                                value={email}
                                onChange={e => setEmail(e.target.value)}
                                placeholder="your@email.com"
                            />
                        </div>
                    </div>
                </div>

                <div className="table-container">
                    <div className="flex-header">
                        <h3>Preferences</h3>
                    </div>
                    <div className="settings-card-body">
                        <p className="settings-desc">Personalize your experience.</p>
                        {PREFS.map(({ label, value, set, opts }) => (
                            <div className="settings-pref-row" key={label}>
                                <span className="settings-pref-label">{label}</span>
                                <Dropdown
                                    value={value}
                                    options={opts}
                                    onChange={e => set(e.value)}
                                    className="settings-dropdown"
                                />
                            </div>
                        ))}
                    </div>
                </div>

                <div className="table-container">
                    <div className="flex-header">
                        <h3>Notifications</h3>
                    </div>
                    <div className="settings-card-body">
                        {NOTIFS.map(({ key, label }) => (
                            <div className="settings-notif-row" key={key}>
                                <span className="settings-notif-label">{label}</span>
                                <InputSwitch
                                    checked={notifications[key]}
                                    onChange={() => toggleNotification(key)}
                                />
                            </div>
                        ))}
                    </div>
                </div>

                <div className="settings-action-row">
                    <button className="settings-save-btn" onClick={handleSave} disabled={saving}>
                        {saving ? 'Saving…' : 'Save changes'}
                    </button>
                    <button className="settings-cancel-btn" onClick={() => navigate(ROUTES.HOME)}>
                        Cancel
                    </button>
                </div>

            </div>
        </div>
    );
};

export default SettingsPage;
