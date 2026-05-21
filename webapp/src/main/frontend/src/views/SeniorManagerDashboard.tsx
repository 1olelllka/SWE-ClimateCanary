import React, { useState, useEffect } from 'react';
import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import { DepartmentAveragesTable } from '../components/DepartmentAveragesTable';
import { ViolationsBarChart } from '../components/ViolationsBarChart';
import { FooterComponent } from '../components/FooterComponent';
import {
    AggregatedDataPointDTO,
    BuildingTrendControllerApi,
    BuildingTrendDTO,
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
const trendApi      = new BuildingTrendControllerApi();

const pad2    = (n: number) => String(n).padStart(2, '0');
const fmtDate = (d: Date)   => `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;

const EPOCH_START = '1970-01-01';
const FAR_FUTURE  = '2099-12-31';

export const SeniorManagerDashboard: React.FC = () => {
    const [sidebarVisible, setSidebarVisible] = useState(false);
    const [departments, setDepartments]        = useState<DepartmentWithStats[]>([]);
    const [loading, setLoading]                = useState(true);
    const [error, setError]                    = useState<string | null>(null);
    const [companyScore, setCompanyScore]      = useState<number | null>(null);
    const [trendLoading, setTrendLoading]      = useState(false);

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

    useEffect(() => {
        if (departments.length === 0) return;
        const now = new Date();
        const weekAgo = new Date(now); weekAgo.setDate(weekAgo.getDate() - 7);
        const startDate = fmtDate(weekAgo);
        const endDate   = fmtDate(now);

        setTrendLoading(true);
        Promise.all(
            departments.map(d =>
                trendApi
                    .getTrendsForSpecificDepartment({ departmentId: d.id, startDate, endDate })
                    .then(r => r.data ?? [])
                    .catch(() => [] as BuildingTrendDTO[])
            )
        ).then(allRows => {
            const latestValues: number[] = [];
            for (const rows of allRows) {
                if (rows.length === 0) continue;
                const latest = rows.reduce((a, b) =>
                    new Date(a.date ?? 0) >= new Date(b.date ?? 0) ? a : b
                );
                if (latest.value != null) latestValues.push(latest.value);
            }
            if (latestValues.length > 0) {
                const avg = latestValues.reduce((s, v) => s + v, 0) / latestValues.length;
                setCompanyScore(Math.round(avg * 100));
            }
        }).finally(() => setTrendLoading(false));
    }, [departments]);

    const totalViolations = departments.reduce((s, d) => s + d.activeViolations, 0);

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
                        <span className="bm-kpi-title">Current Climate Score</span>
                        <span className={`bm-kpi-value${companyScore !== null && companyScore < 30 ? ' bm-kpi-value--red' : ''}`}>
                            {trendLoading ? '…' : companyScore !== null ? companyScore : '—'}
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
