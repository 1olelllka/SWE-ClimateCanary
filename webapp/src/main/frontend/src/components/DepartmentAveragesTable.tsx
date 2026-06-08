import React from 'react';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { defaultTableProps } from '../config/tableConfig';
import { useNavigate } from 'react-router-dom';
import { DepartmentWithStats } from '../views/SeniorManagerDashboard';
import { useTemperature } from '../hooks/useTemperature';

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
    const { convert: convertTemp, unit: tempUnit } = useTemperature();

    const tableData: TableRow[] = departments.map(d => ({
        id:         d.id,
        department: d.name,
        co2:      d.stats?.avgAirQuality  != null ? `${d.stats.avgAirQuality.toFixed(0)} ppm`  : 'N/A',
        temp:     d.stats?.avgTemperature != null ? `${convertTemp(d.stats.avgTemperature).toFixed(1)} ${tempUnit}` : 'N/A',
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
                <Column field="department" header="Department" sortable style={{ width: '32%' }} />
                <Column field="co2"        header="CO₂"        sortable style={{ width: '18%' }} />
                <Column field="temp"       header="Temp"       sortable style={{ width: '18%' }} />
                <Column field="humidity"   header="Humidity"   sortable style={{ width: '19%' }} />
                <Column header="Status"    body={statusTemplate} style={{ width: '13%', textAlign: 'center' }} />
            </DataTable>
        </div>
    );
};
