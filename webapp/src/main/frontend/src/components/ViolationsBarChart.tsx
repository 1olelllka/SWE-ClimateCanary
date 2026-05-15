import React, { useState } from 'react';
import { Chart } from 'primereact/chart';
import { TimeFilter } from './TimeFilter';

export const ViolationsBarChart: React.FC = () => {
    const [timeRange, setTimeRange] = useState('Month');
    const [dateRange, setDateRange] = useState<Date[] | null>(null);
    const timeOptions = ['Current', 'Day', 'Week', 'Month'];

    const chartData = {
        labels: ['Finance', 'IT', 'Human Resources', 'Marketing', 'Sales'],
        datasets: [
            {
                label: 'Nr. of violations',
                backgroundColor: ['#4caf50', '#ffeb3b', '#4285f4', '#9c27b0', '#e91e63'],
                data: [12, 7, 15, 2, 9]
            }
        ]
    };

    const chartOptions = {
        maintainAspectRatio: false,
        aspectRatio: 0.8,
        plugins: { legend: { display: false } },
        scales: {
            y: {
                beginAtZero: true,
                grid: { color: '#eee' },
                title: { display: true, text: 'Nr. of violations', color: '#6c757d', font: { weight: 'bold' } }
            },
            x: { grid: { display: false } }
        }
    };

    return (
        <div className="table-container card chart-card">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
                <h3 style={{ margin: 0 }}>Number of Violations per Department</h3>
                <TimeFilter
                    options={timeOptions}
                    value={timeRange}
                    onChange={setTimeRange}
                    dateRange={dateRange}
                    setDateRange={setDateRange}
                />
            </div>
            <Chart type="bar" data={chartData} options={chartOptions} style={{ height: '300px' }} />
        </div>
    );
};