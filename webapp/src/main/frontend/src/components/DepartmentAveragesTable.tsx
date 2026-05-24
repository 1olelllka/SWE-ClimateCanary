import React from 'react';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { defaultTableProps } from '../config/tableConfig';
import { useNavigate } from 'react-router-dom';
import { DepartmentWithStats } from '../views/SeniorManagerDashboard';

interface Props {
    departments: DepartmentWithStats[];
    loading: boolean;
}

interface TableRow {
    id:         string;
    department: string;
    co2:        string;
    temp:       string;
    humidity:   string;
    status:     'red' | 'yellow' | 'green' | 'gray';
}

function computeStatus(dept: DepartmentWithStats): 'red' | 'yellow' | 'green' | 'gray' {
    if (!dept.stats) return 'gray';
    const n = dept.activeViolations;
    if (n === 0) return 'green';
    if (n <= 3)  return 'yellow';
    return 'red';
}

export const DepartmentAveragesTable: React.FC<Props> = ({ departments, loading }) => {
    const navigate = useNavigate();

    const tableData: TableRow[] = departments.map(d => ({
        id:         d.id,
        department: d.name,
        co2:      d.stats?.avgAirQuality  != null ? `${d.stats.avgAirQuality.toFixed(0)} ppm`  : 'N/A',
        temp:     d.stats?.avgTemperature != null ? `${d.stats.avgTemperature.toFixed(1)} °C`  : 'N/A',
        humidity: d.stats?.avgHumidity    != null ? `${d.stats.avgHumidity.toFixed(1)} %`      : 'N/A',
        status:   computeStatus(d),
    }));

    const handleRowClick = (e: { data: TableRow }) => {
        navigate(`/senior/department/${encodeURIComponent(e.data.id)}?name=${encodeURIComponent(e.data.department)}`);
    };

    const statusTemplate = (row: TableRow) => (
        <span className={`status-indicator status-${row.status}`} />
    );

    return (
        <div className="table-container">
            <div className="flex-header">
                <h3>Avg. Values per Department</h3>
            </div>
            <DataTable
                value={tableData}
                {...defaultTableProps}
                loading={loading}
                onRowClick={handleRowClick}
                className="row-clickable"
                rows={10}
            >
                <Column field="department" header="Department" sortable />
                <Column field="co2"        header="CO₂"        sortable />
                <Column field="temp"       header="Temp"       sortable />
                <Column field="humidity"   header="Humidity"   sortable />
                <Column header="Status"    body={statusTemplate} />
            </DataTable>
        </div>
    );
};
