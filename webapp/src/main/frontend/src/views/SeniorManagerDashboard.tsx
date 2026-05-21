import React, { useState, useEffect } from 'react';
import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import { DepartmentAveragesTable } from '../components/DepartmentAveragesTable';
import { ViolationsBarChart } from '../components/ViolationsBarChart';
import { FooterComponent } from '../components/FooterComponent';
import {
    AggregatedDataPointDTO,
    ClimateStatsControllerApi,
    DepartmentControllerApi,
    DepartmentListDTO,
    WarningControllerApi,
} from '../generated-skeleton-api';
import '../styles/BuildingManagerDashboard.css';
import '../styles/Tables.css';

export interface DepartmentWithStats {
    id: string;
    name: string;
    stats: AggregatedDataPointDTO | null;
    activeViolations: number;
}

const climateApi    = new ClimateStatsControllerApi();
const departmentApi = new DepartmentControllerApi();
const warningApi    = new WarningControllerApi();

const EPOCH_START = '1970-01-01';
const FAR_FUTURE  = '2099-12-31';

export const SeniorManagerDashboard: React.FC = () => {
    const [sidebarVisible, setSidebarVisible] = useState(false);
    const [departments, setDepartments]        = useState<DepartmentWithStats[]>([]);
    const [loading, setLoading]                = useState(true);
    const [error, setError]                    = useState<string | null>(null);

    useEffect(() => {
        departmentApi
            .getPageOfDepartments({ pageable: { page: 0, size: 200 } })
            .then(async (res) => {
                const list: DepartmentListDTO[] = res.data?.content ?? [];
                const validDepts = list.filter(
                    (d): d is DepartmentListDTO & { id: string; name: string } => !!d.id && !!d.name
                );

                const withStats = await Promise.all(
                    validDepts.map(async (dept) => {
                        const [statsResult, violationsResult] = await Promise.allSettled([
                            climateApi.getLastAggregationForDepartment({ id: dept.id }),
                            warningApi.getWarningsSummaryForDepartment({
                                departmentId: dept.id,
                                onlyActive:   true,
                                startDate:    EPOCH_START,
                                endDate:      FAR_FUTURE,
                            }),
                        ]);

                        const stats = statsResult.status === 'fulfilled' ? statsResult.value.data : null;
                        const activeViolations = violationsResult.status === 'fulfilled'
                            ? (violationsResult.value.data?.length ?? 0)
                            : 0;

                        return { id: dept.id, name: dept.name, stats, activeViolations };
                    })
                );
                setDepartments(withStats);
            })
            .catch(() => setError('Failed to load department data.'))
            .finally(() => setLoading(false));
    }, []);

    const totalViolations = departments.reduce((s, d) => s + d.activeViolations, 0);

    const deptsWithData = departments.filter(d => d.stats !== null);
    const avgCO2 = deptsWithData.length > 0
        ? deptsWithData.reduce((s, d) => s + (d.stats?.avgAirQuality ?? 0), 0) / deptsWithData.length
        : null;

    return (
        <div className="bm-page">
            <PageHeader
                title="Company Overview"
                onMenuClick={() => setSidebarVisible(true)}
            />
            <SidebarComponent visible={sidebarVisible} onHide={() => setSidebarVisible(false)} />

            <div className="bm-content" style={{ maxWidth: '1100px' }}>
                {error && <div className="bm-error">{error}</div>}

                <div className="bm-kpi-row">
                    <div className="bm-kpi-card">
                        <span className="bm-kpi-title">Active Violations</span>
                        <span className={`bm-kpi-value${totalViolations > 0 ? ' bm-kpi-value--red' : ''}`}>
                            {loading ? '…' : totalViolations}
                        </span>
                    </div>
                    <div className="bm-kpi-card">
                        <span className="bm-kpi-title">Avg CO₂</span>
                        <span className="bm-kpi-value">
                            {loading ? '…' : avgCO2 !== null ? `${avgCO2.toFixed(0)} ppm` : '—'}
                        </span>
                    </div>
                </div>

                <DepartmentAveragesTable departments={departments} loading={loading} />
                <ViolationsBarChart     departments={departments} loading={loading} />
            </div>

            <FooterComponent />
        </div>
    );
};
