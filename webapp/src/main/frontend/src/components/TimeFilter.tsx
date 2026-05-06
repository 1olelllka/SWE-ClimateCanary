import React from 'react';
import { DashboardCalendar } from './Calendar';
import '../styles/TimeFilter.css';

interface TimeFilterProps {
    options: string[];
    value: string;
    onChange: (val: string) => void;
    dateRange: Date[] | null;
    setDateRange: (val: Date[] | null) => void;
}

export const TimeFilter: React.FC<TimeFilterProps> = ({
                                                          options,
                                                          value,
                                                          onChange,
                                                          dateRange,
                                                          setDateRange
                                                      }) => {
    return (
        <div className="time-filters">
            {options.map(opt => (
                <button
                    key={opt}
                    className={`time-filter-btn ${value === opt ? 'active' : ''}`}
                    onClick={() => {
                        onChange(opt);
                        setDateRange(null); // Setzt das Datum zurück, wenn man z.B. wieder auf "Week" klickt
                    }}
                >
                    {opt}
                </button>
            ))}

            <DashboardCalendar
                dateRange={dateRange}
                setDateRange={setDateRange}
                isActive={value === 'Custom'}
                onActivate={() => onChange('Custom')}
            />
        </div>
    );
};