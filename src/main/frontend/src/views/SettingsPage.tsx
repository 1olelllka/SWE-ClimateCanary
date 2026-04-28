import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import { ROUTES } from '../utilities/routes.paths';

import '../styles/SettingsPage.css';

export const SettingsPage: React.FC = () => {
    const [sidebarVisible, setSidebarVisible] = useState(false);
    const navigate = useNavigate();

    const [firstName, setFirstName] = useState('Georg');
    const [lastName, setLastName] = useState('Moser');
    const [email, setEmail] = useState('georg.moser@uibk.ac.at');
    const [currentPassword, setCurrentPassword] = useState('password');
    const [newPassword, setNewPassword] = useState('password');

    const [language, setLanguage] = useState('English');
    const [temperatureUnit, setTemperatureUnit] = useState('°C');
    const [timeFormat, setTimeFormat] = useState('24-hour (14:30)');
    const [dayFormat, setDayFormat] = useState('DD/MM/YYYY');

    const [notifications, setNotifications] = useState({
        warnings: false,
        tips: false,
        absences: false,
        problemRooms: false
    });

    const handleNotificationChange = (key: keyof typeof notifications) => {
        setNotifications(previous => ({
            ...previous,
            [key]: !previous[key]
        }));
    };

    const handleSave = () => {
        alert('Settings saved. Backend integration will be added later.');
    };

    return (
        <div className="settings-page">
            <PageHeader
                title="Settings"
                onMenuClick={() => setSidebarVisible(true)}
            />

            <SidebarComponent
                visible={sidebarVisible}
                onHide={() => setSidebarVisible(false)}
            />

            <main className="settings-content">
                <section className="settings-section">
                    <h2>Account</h2>

                    <div className="settings-profile-row">
                        <div className="settings-avatar" />

                        <div className="settings-profile-info">
                            <span className="settings-profile-title">Profile picture</span>
                            <span className="settings-profile-subtitle">PNG, JPEG under 15 MB</span>
                        </div>

                        <button type="button" className="settings-secondary-button">
                            Upload new picture
                        </button>

                        <button type="button" className="settings-secondary-button">
                            Delete picture
                        </button>
                    </div>

                    <div className="settings-form-group">
                        <h3>Full Name</h3>

                        <div className="settings-two-column">
                            <label>
                                <span>First name</span>
                                <input
                                    type="text"
                                    value={firstName}
                                    onChange={event => setFirstName(event.target.value)}
                                />
                            </label>

                            <label>
                                <span>Last name</span>
                                <input
                                    type="text"
                                    value={lastName}
                                    onChange={event => setLastName(event.target.value)}
                                />
                            </label>
                        </div>
                    </div>

                    <div className="settings-form-group">
                        <h3>Contact email</h3>
                        <p>Manage your accounts email address for the notifications.</p>

                        <div className="settings-email-row">
                            <label>
                                <span>Email</span>
                                <input
                                    type="email"
                                    value={email}
                                    onChange={event => setEmail(event.target.value)}
                                />
                            </label>

                            <button type="button" className="settings-primary-small-button">
                                Add another email
                            </button>
                        </div>
                    </div>

                    <div className="settings-form-group">
                        <h3>Password</h3>
                        <p>Modify your current password</p>

                        <div className="settings-two-column">
                            <label>
                                <span>Current password</span>
                                <input
                                    type="password"
                                    value={currentPassword}
                                    onChange={event => setCurrentPassword(event.target.value)}
                                />
                            </label>

                            <label>
                                <span>New password</span>
                                <input
                                    type="password"
                                    value={newPassword}
                                    onChange={event => setNewPassword(event.target.value)}
                                />
                            </label>
                        </div>
                    </div>
                </section>

                <section className="settings-section settings-preferences-section">
                    <h2>Preferences</h2>
                    <p>Personalize your experience.</p>

                    <div className="settings-preference-row">
                        <span>Language</span>
                        <select value={language} onChange={event => setLanguage(event.target.value)}>
                            <option>English</option>
                            <option>Deutsch</option>
                        </select>
                    </div>

                    <div className="settings-preference-row">
                        <span>Temperature units</span>
                        <select value={temperatureUnit} onChange={event => setTemperatureUnit(event.target.value)}>
                            <option>°C</option>
                            <option>°F</option>
                        </select>
                    </div>

                    <div className="settings-preference-row">
                        <span>Time Format</span>
                        <select value={timeFormat} onChange={event => setTimeFormat(event.target.value)}>
                            <option>24-hour (14:30)</option>
                            <option>12-hour (2:30 PM)</option>
                        </select>
                    </div>

                    <div className="settings-preference-row">
                        <span>Day Format</span>
                        <select value={dayFormat} onChange={event => setDayFormat(event.target.value)}>
                            <option>DD/MM/YYYY</option>
                            <option>MM/DD/YYYY</option>
                            <option>YYYY-MM-DD</option>
                        </select>
                    </div>
                </section>

                <section className="settings-section settings-notifications-section">
                    <h2>Notifications</h2>

                    <label className="settings-checkbox-row">
                        <input
                            type="checkbox"
                            checked={notifications.warnings}
                            onChange={() => handleNotificationChange('warnings')}
                        />
                        <span>Warnings</span>
                    </label>

                    <label className="settings-checkbox-row">
                        <input
                            type="checkbox"
                            checked={notifications.tips}
                            onChange={() => handleNotificationChange('tips')}
                        />
                        <span>Tips</span>
                    </label>

                    <label className="settings-checkbox-row">
                        <input
                            type="checkbox"
                            checked={notifications.absences}
                            onChange={() => handleNotificationChange('absences')}
                        />
                        <span>Absences</span>
                    </label>

                    <label className="settings-checkbox-row">
                        <input
                            type="checkbox"
                            checked={notifications.problemRooms}
                            onChange={() => handleNotificationChange('problemRooms')}
                        />
                        <span>Problem rooms</span>
                    </label>
                </section>

                <div className="settings-action-row">
                    <button
                        type="button"
                        className="settings-save-button"
                        onClick={handleSave}
                    >
                        Save changes
                    </button>

                    <button
                        type="button"
                        className="settings-cancel-button"
                        onClick={() => navigate(ROUTES.HOME)}
                    >
                        Cancel
                    </button>
                </div>
            </main>
        </div>
    );
};

export default SettingsPage;