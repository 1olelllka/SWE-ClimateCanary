import React, { useState, useEffect } from 'react';
import { PageHeader } from '../components/PageHeader';
import SidebarComponent from '../components/SidebarComponent';
import { CompanyTrendChart } from '../components/CompanyTrendChart';
import { DepartmentControllerApi, DepartmentListDTO } from '../generated-skeleton-api';
import { DepartmentWithStats } from './SeniorManagerDashboard';
import '../styles/BuildingManagerDashboard.css';

const departmentApi = new DepartmentControllerApi();

export const CompanyTrendPage: React.FC = () => {
    const [sidebarVisible, setSidebarVisible] = useState(false);
    const [departments,    setDepartments]    = useState<DepartmentWithStats[]>([]);
    const [error,          setError]          = useState<string | null>(null);

    useEffect(() => {
        departmentApi
            .getPageOfDepartments({ pageable: { page: 0, size: 200 } })
            .then(res => {
                const list: DepartmentListDTO[] = res.data?.content ?? [];
                const depts = list
                    .filter((d): d is DepartmentListDTO & { id: string; name: string } => !!d.id && !!d.name)
                    .map(d => ({ id: d.id, name: d.name, stats: null, activeViolations: 0 }));
                setDepartments(depts);
            })
            .catch(() => setError('Failed to load departments.'));
    }, []);

    return (
        <div className="bm-page">
            <PageHeader
                title="Climate Trends"
                onMenuClick={() => setSidebarVisible(true)}
            />
            <SidebarComponent visible={sidebarVisible} onHide={() => setSidebarVisible(false)} />

            <div className="bm-content" style={{ maxWidth: '1100px' }}>
                {error && <div className="bm-error">{error}</div>}
                <CompanyTrendChart departments={departments} />
            </div>
        </div>
    );
};
