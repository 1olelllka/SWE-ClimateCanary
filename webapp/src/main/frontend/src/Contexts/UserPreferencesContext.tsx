import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { UserxControllerApi, UserSettingsDTO, UserSettingsPatchDTO } from '../generated-skeleton-api';
import { useTheme } from './ThemeContext';
import { useUser } from './AuthenticatedUserContext';

interface UserPrefsContextValue {
    readonly prefs: UserSettingsDTO | null;
    readonly loading: boolean;
    readonly savePrefs: (patch: UserSettingsPatchDTO) => Promise<void>;
}

const UserPreferencesContext = createContext<UserPrefsContextValue | undefined>(undefined);

export const UserPreferencesProvider: React.FC<React.PropsWithChildren> = ({ children }) => {
    const [prefs, setPrefs] = useState<UserSettingsDTO | null>(null);
    const [loading, setLoading] = useState(false);
    const { setTheme } = useTheme();
    const { currentUser } = useUser();

    // Fetch settings whenever the user logs in or out
    useEffect(() => {
        if (!currentUser) {
            setPrefs(null);
            return;
        }

        setLoading(true);
        new UserxControllerApi().getUserSettings()
            .then(res => {
                const s = res.data;
                setPrefs(s);
                // Apply persisted dark-mode preference immediately
                if (s.darkMode !== undefined) {
                    setTheme(s.darkMode ? 'dark' : 'light');
                }
            })
            .catch(() => {
                // Silently ignore – defaults stay in place
            })
            .finally(() => setLoading(false));
    }, [currentUser, setTheme]);

    const savePrefs = useCallback(async (patch: UserSettingsPatchDTO) => {
        const res = await new UserxControllerApi().updateUserSettings({ userSettingsPatchDTO: patch });
        setPrefs(res.data);
        if (patch.darkMode !== undefined) {
            setTheme(patch.darkMode ? 'dark' : 'light');
        }
    }, [setTheme]);

    return (
        <UserPreferencesContext.Provider value={{ prefs, loading, savePrefs }}>
            {children}
        </UserPreferencesContext.Provider>
    );
};

export const useUserPreferences = () => {
    const ctx = useContext(UserPreferencesContext);
    if (!ctx) throw new Error('useUserPreferences must be used inside UserPreferencesProvider');
    return ctx;
};
