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
        const handleResize = () => {
            setIsMobile(window.innerWidth <= 768);
        };

        window.addEventListener('resize', handleResize);

        return () => {
            window.removeEventListener('resize', handleResize);
        };
    }, []);

    return isMobile;
};

export const DashboardCalendar: React.FC<Props> = ({
                                                       dateRange,
                                                       setDateRange,
                                                       isActive,
                                                       onActivate
                                                   }) => {
    const op = useRef<OverlayPanel>(null);
    const isMobile = useIsMobile();

    return (
        <>
            <button
                type="button"
                className={`time-filter-btn ${isActive ? 'active' : ''}`}
                onClick={(e) => op.current?.toggle(e)}
            >
                <i className="pi pi-calendar"></i>

                {isActive && dateRange && dateRange[0] && (
                    <span className="selected-date-text">
                        {dateRange[0].toLocaleDateString('en-US', {
                            month: 'short',
                            day: 'numeric'
                        })}
                        {dateRange[1]
                            ? ` - ${dateRange[1].toLocaleDateString('en-US', {
                                month: 'short',
                                day: 'numeric'
                            })}`
                            : '...'}
                    </span>
                )}
            </button>

            <OverlayPanel ref={op} className="compact-calendar-overlay">
                <div className="compact-calendar">
                    <Calendar
                        value={dateRange as any}
                        onChange={(e) => {
                            const dates = e.value as Date[];
                            setDateRange(dates);

                            if (dates && dates[1]) {
                                onActivate();
                                op.current?.hide();
                            }
                        }}
                        selectionMode="range"
                        numberOfMonths={isMobile ? 1 : 2}
                        inline
                    />
                </div>
            </OverlayPanel>
        </>
    );
};