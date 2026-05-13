import React, { useRef } from 'react';
import { OverlayPanel } from 'primereact/overlaypanel';
import { Calendar } from 'primereact/calendar';

interface Props {
    readonly dateRange: Date[] | null;
    readonly setDateRange: (dates: Date[] | null) => void;
    readonly isActive: boolean;
    readonly onActivate: () => void;
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
                        numberOfMonths={2}
                        inline
                    />
                </div>
            </OverlayPanel>
        </>
    );
};