import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { InputSwitch } from 'primereact/inputswitch';
import { Dropdown } from 'primereact/dropdown';
import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import { ROUTES } from '../utilities/routes.paths';
import '../styles/SettingsPage.css';
import '../styles/Tables.css';

export const SettingsPage: React.FC = () => {
    const [sidebarVisible, setSidebarVisible] = useState(false);
    const navigate = useNavigate();

    const [email, setEmail] = useState('');

    const [language, setLanguage] = useState('English');
    const [temperatureUnit, setTemperatureUnit] = useState('°C');
    const [timeFormat, setTimeFormat] = useState('24-hour (14:30)');
    const [dayFormat, setDayFormat] = useState('DD/MM/YYYY');

    const [notifications, setNotifications] = useState({
        warnings: false,
        tips: false,
        absences: false,
        problemRooms: false,
    });

    const toggleNotification = (key: keyof typeof notifications) => {
        setNotifications(prev => ({ ...prev, [key]: !prev[key] }));
    };

    const handleSave = () => {
        alert('Settings saved. Backend integration will be added later.');
    };

    const PREFS = [
        { label: 'Language',         value: language,         set: setLanguage,         opts: ['English', 'Deutsch'] },
        { label: 'Temperature Unit', value: temperatureUnit,  set: setTemperatureUnit,  opts: ['°C', '°F'] },
        { label: 'Time Format',      value: timeFormat,       set: setTimeFormat,       opts: ['24-hour (14:30)', '12-hour (2:30 PM)'] },
        { label: 'Date Format',      value: dayFormat,        set: setDayFormat,        opts: ['DD/MM/YYYY', 'MM/DD/YYYY', 'YYYY-MM-DD'] },
    ] as const;

    const NOTIFS: { key: keyof typeof notifications; label: string }[] = [
        { key: 'warnings',     label: 'Warnings' },
        { key: 'tips',         label: 'Tips' },
        { key: 'absences',     label: 'Absences' },
        { key: 'problemRooms', label: 'Problem rooms' },
    ];

    return (
        <div className="dashboard-layout">
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
                                    options={opts as unknown as string[]}
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
                    <button className="settings-save-btn" onClick={handleSave}>
                        Save changes
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
