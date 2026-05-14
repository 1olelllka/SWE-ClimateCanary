import React, { useState, useMemo } from 'react';
import { Chart } from 'primereact/chart';
import { Dropdown } from 'primereact/dropdown';
import { TimeFilter } from './TimeFilter';

export const AirQualityTrendChart: React.FC = () => {
    const [timeRange, setTimeRange] = useState('Week');
    const [metric, setMetric] = useState('Temperature');
    const [dateRange, setDateRange] = useState<Date[] | null>(null);
    const [selectedDep, setSelectedDep] = useState<string | null>(null);

    const timeOptions = ['Day', 'Week', 'Month', 'Year', 'All'];
    const metricOptions = ['Temperature', 'CO2', 'Humidity'];
    const departmentOptions = ['All Departments', 'Finance', 'Human Resources', 'Marketing', 'Sales', 'IT'];

    const { labels, allDatasets } = useMemo(() => {
        let newLabels: string[] = [];

        if (timeRange === 'Day') newLabels = ['08:00', '10:00', '12:00', '14:00', '16:00', '18:00'];
        else if (timeRange === 'Week') newLabels = ['Mo', 'Di', 'Mi', 'Do', 'Fr', 'Sa', 'So'];
        else if (timeRange === 'Month') newLabels = ['Woche 1', 'Woche 2', 'Woche 3', 'Woche 4'];
        else if (timeRange === 'Year') newLabels = ['Jan', 'Feb', 'Mar', 'Apr', 'Mai', 'Jun', 'Jul', 'Aug', 'Sep', 'Okt', 'Nov', 'Dez'];
        else if (timeRange === 'All') newLabels = ['2020', '2021', '2022', '2023', '2024'];
        else newLabels = ['Mo', 'Di', 'Mi', 'Do', 'Fr', 'Sa', 'So']; // Fallback

        // Dummy-Daten-Generator:
        const padData = (base: number[]) => newLabels.map((_, i) => base[i % base.length]);

        const datasets = [
            { label: 'Company', data: padData([22, 23, 22.5, 24, 23.5, 21, 22, 23.2, 24.1, 22.8, 21.5, 22.2]), borderColor: '#f44336', tension: 0.4, fill: false },
            { label: 'Finance', data: padData([21, 21.5, 21, 22, 21.8, 20.5, 21, 21.2, 22.1, 21.4, 20.8, 21.1]), borderColor: '#4caf50', tension: 0.4, fill: false },
            { label: 'Human Resources', data: padData([23, 24, 23.5, 25, 24.5, 22, 23, 24.2, 25.1, 23.8, 22.5, 23.2]), borderColor: '#4285f4', tension: 0.4, fill: false },
            { label: 'Marketing', data: padData([22.5, 23, 22.8, 23.5, 23, 21.5, 22, 23.2, 23.8, 22.4, 21.8, 22.1]), borderColor: '#9c27b0', tension: 0.4, fill: false },
            { label: 'Sales', data: padData([21.5, 22, 21.8, 22.5, 22, 21, 21.5, 22.2, 22.8, 21.4, 20.8, 21.2]), borderColor: '#e91e63', tension: 0.4, fill: false },
            { label: 'IT', data: padData([24, 25, 24.5, 26, 25.5, 23, 24, 25.2, 26.1, 24.8, 23.5, 24.2]), borderColor: '#ffeb3b', tension: 0.4, fill: false }
        ];

        return { labels: newLabels, allDatasets: datasets };
    }, [timeRange]);

    // Filter-Logik für Departments
    const filteredDatasets = selectedDep && selectedDep !== 'All Departments'
        ? allDatasets.filter(ds => ds.label === 'Company' || ds.label === selectedDep)
        : allDatasets;

    const chartData = {
        labels: labels,
        datasets: filteredDatasets
    };

    // Chart Konfiguration
    const chartOptions = {
        maintainAspectRatio: false,
        aspectRatio: 0.6,
        plugins: {
            legend: {
                position: 'top',
                labels: {
                    usePointStyle: true,
                    pointStyle: 'line',
                    boxWidth: 30,
                    padding: 20
                }
            }
        },
        scales: {
            y: { title: { display: true, text: metric, color: '#6c757d', font: { weight: 'bold' } } },
            x: { title: { display: true, text: 'Time', color: '#6c757d', font: { weight: 'bold' } } }
        }
    };

    return (
        <div className="table-container card chart-card">
            <div className="chart-card-header">
                <h3 style={{ margin: 0 }}>Trend of Air Quality per Department</h3>

                <TimeFilter
                    options={timeOptions}
                    value={timeRange}
                    onChange={setTimeRange}
                    dateRange={dateRange}
                    setDateRange={setDateRange}
                />
            </div>

            <Chart type="line" data={chartData} options={chartOptions} style={{ height: '350px' }} />

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem', marginTop: '1rem' }}>
                <Dropdown
                    value={selectedDep}
                    options={departmentOptions}
                    onChange={(e) => setSelectedDep(e.value)}
                    placeholder="Department Filter ▼"
                    showClear
                    style={{ width: '200px', borderRadius: '20px' }}
                />
                <Dropdown
                    value={metric}
                    options={metricOptions}
                    onChange={(e) => setMetric(e.value)}
                    style={{ width: '200px', borderRadius: '20px' }}
                />
            </div>
        </div>
    );
};