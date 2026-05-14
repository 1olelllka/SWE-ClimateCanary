import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';

import lightThemeUrl from 'primereact/resources/themes/lara-light-cyan/theme.css?url';
import darkThemeUrl from 'primereact/resources/themes/lara-dark-cyan/theme.css?url';

type ThemeMode = 'light' | 'dark';

interface ThemeContextValue {
    readonly theme: ThemeMode;
    readonly isDarkMode: boolean;
    readonly toggleTheme: () => void;
}

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

const THEME_STORAGE_KEY = 'app-theme';
const THEME_LINK_ID = 'primereact-theme-link';

const getInitialTheme = (): ThemeMode => {
    const saved = localStorage.getItem(THEME_STORAGE_KEY);

    if (saved === 'light' || saved === 'dark') {
        return saved;
    }

    return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
};

const applyTheme = (theme: ThemeMode) => {
    const href = theme === 'dark' ? darkThemeUrl : lightThemeUrl;

    let link = document.getElementById(THEME_LINK_ID) as HTMLLinkElement | null;

    if (!link) {
        link = document.createElement('link');
        link.id = THEME_LINK_ID;
        link.rel = 'stylesheet';
        document.head.appendChild(link);
    }

    link.href = href;

    document.documentElement.setAttribute('data-theme', theme);
};

export const ThemeProvider: React.FC<React.PropsWithChildren> = ({ children }) => {
    const [theme, setTheme] = useState<ThemeMode>(getInitialTheme);

    useEffect(() => {
        applyTheme(theme);
        localStorage.setItem(THEME_STORAGE_KEY, theme);
    }, [theme]);

    const toggleTheme = () => {
        setTheme(current => current === 'dark' ? 'light' : 'dark');
    };

    const value = useMemo(() => ({
        theme,
        isDarkMode: theme === 'dark',
        toggleTheme
    }), [theme]);

    return (
        <ThemeContext.Provider value={value}>
            {children}
        </ThemeContext.Provider>
    );
};

export const useTheme = () => {
    const context = useContext(ThemeContext);

    if (!context) {
        throw new Error('useTheme must be used inside ThemeProvider');
    }

    return context;
};