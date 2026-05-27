import React, { useEffect, useRef, useState } from 'react';
import { OverlayPanel } from 'primereact/overlaypanel';
import { Calendar } from 'primereact/calendar';

interface Props {
    readonly dateRange: Date[] | null;
    readonly setDateRange: (dates: Date[] | null) => void;
    readonly isActive: boolean;
    readonly onActivate: () => void;
}

const useIsMobile = () => {
    const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);
    useEffect(() => {
        const handler = () => setIsMobile(window.innerWidth <= 768);
        window.addEventListener('resize', handler);
        return () => window.removeEventListener('resize', handler);
    }, []);
    return isMobile;
};

export const DashboardCalendar: React.FC<Props> = ({
    dateRange,
    setDateRange,
    isActive,
    onActivate,
}) => {
    const op         = useRef<OverlayPanel>(null);
    const buttonRef  = useRef<HTMLButtonElement>(null);
    const panelRef   = useRef<HTMLDivElement>(null);
    const isMobile   = useIsMobile();
    const [mobileOpen, setMobileOpen]   = useState(false);
    const [panelStyle, setPanelStyle]   = useState<React.CSSProperties>({});

    const handleChange = (dates: Date[]) => {
        setDateRange(dates);
        if (dates && dates[1]) {
            onActivate();
            if (isMobile) {
                setMobileOpen(false);
            } else {
                op.current?.hide();
            }
        }
    };

    /** Open the panel anchored to the button, fully within the viewport. */
    const openMobilePanel = () => {
        if (!mobileOpen && buttonRef.current) {
            const rect       = buttonRef.current.getBoundingClientRect();
            const vw         = window.innerWidth;
            const panelWidth = Math.min(308, vw - 16); // 308 px fits one PrimeReact month comfortably
            // Prefer right-aligning with the button; clamp so it never exits the viewport
            const rightFromViewport = vw - rect.right;
            const clampedRight      = Math.max(8, Math.min(rightFromViewport, vw - panelWidth - 8));
            setPanelStyle({
                position: 'fixed',
                top:      rect.bottom + 6,
                right:    clampedRight,
                width:    panelWidth,
                zIndex:   9999,
            });
        }
        setMobileOpen(v => !v);
    };

    // Close when clicking outside the panel or the toggle button
    useEffect(() => {
        if (!mobileOpen) return;
        const onOutside = (e: MouseEvent) => {
            if (
                panelRef.current  && !panelRef.current.contains(e.target as Node) &&
                buttonRef.current && !buttonRef.current.contains(e.target as Node)
            ) {
                setMobileOpen(false);
            }
        };
        // Also close if the user scrolls the page (panel position would be stale)
        const onScroll = () => setMobileOpen(false);
        document.addEventListener('mousedown', onOutside);
        window.addEventListener('scroll', onScroll, { capture: true, passive: true });
        return () => {
            document.removeEventListener('mousedown', onOutside);
            window.removeEventListener('scroll', onScroll, true);
        };
    }, [mobileOpen]);

    const buttonLabel = isActive && dateRange?.[0] ? (
        <>
            <i className="pi pi-calendar" />
            <span className="selected-date-text">
                {dateRange[0].toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
                {dateRange[1]
                    ? ` - ${dateRange[1].toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}`
                    : '…'}
            </span>
        </>
    ) : (
        <i className="pi pi-calendar" />
    );

    if (isMobile) {
        return (
            <div className="dashboard-calendar-mobile">
                <button
                    ref={buttonRef}
                    type="button"
                    className={`time-filter-btn ${isActive ? 'active' : ''}`}
                    onClick={openMobilePanel}
                >
                    {buttonLabel}
                </button>
                {mobileOpen && (
                    <div
                        ref={panelRef}
                        className="dashboard-calendar-mobile-panel"
                        style={panelStyle}
                    >
                        <Calendar
                            value={dateRange as any}
                            onChange={e => handleChange(e.value as Date[])}
                            selectionMode="range"
                            numberOfMonths={1}
                            inline
                        />
                    </div>
                )}
            </div>
        );
    }

    return (
        <>
            <button
                type="button"
                className={`time-filter-btn ${isActive ? 'active' : ''}`}
                onClick={e => op.current?.toggle(e)}
            >
                {buttonLabel}
            </button>
            <OverlayPanel ref={op} className="compact-calendar-overlay">
                <div className="compact-calendar">
                    <Calendar
                        value={dateRange as any}
                        onChange={e => handleChange(e.value as Date[])}
                        selectionMode="range"
                        numberOfMonths={2}
                        inline
                    />
                </div>
            </OverlayPanel>
        </>
    );
};
