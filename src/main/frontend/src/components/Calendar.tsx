import React, { useRef } from 'react';
import { OverlayPanel } from 'primereact/overlaypanel';
import { Calendar } from 'primereact/calendar';

interface Props {
    dateRange: Date[] | null;
    setDateRange: (dates: Date[] | null) => void;
    isActive: boolean;
    onActivate: () => void;
}

export const DashboardCalendar: React.FC<Props> = ({ dateRange, setDateRange, isActive, onActivate }) => {
    const op = useRef<OverlayPanel>(null);

    return (
        <>
            <button
                className={`time-filter-btn ${isActive ? 'active' : ''}`}
                onClick={(e) => op.current?.toggle(e)}
            >
                <i className="pi pi-calendar"></i>
                {isActive && dateRange && dateRange[0] && (
                    <span className="selected-date-text">
                        {dateRange[0].toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
                        {dateRange[1] ? ` - ${dateRange[1].toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}` : '...'}
                    </span>
                )}
            </button>

            <OverlayPanel ref={op}>
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
                    inline
                />
            </OverlayPanel>
        </>
    );
};