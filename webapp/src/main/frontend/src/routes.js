/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */

import Login from "./views/Login";
import Logout from "./views/Logout";
import RoleBasedHome from "./views/RoleBasedHome";
import DeviceConfigurationPage from "./views/DeviceConfigurationPage";
import UserConfigurationPage from "./views/UserConfigurationPage";
import BuildingConfigurationPage from "./views/BuildingConfigurationPage";
import EmployeeAbsencesPage from "./views/EmployeeAbsencesPage";
import DepartmentAbsencesPage from "./views/DepartmentAbsencesPage";
import EmployeeDepartmentPage from "./views/EmployeeDepartmentPage";
import SettingsPage from "./views/SettingsPage";
import RoomDetailPage from "./views/RoomDetailPage";
import DepartmentDetailPage from "./views/DepartmentDetailPage";
import BuildingRoomAnalysisPage from "./views/BuildingRoomAnalysisPage";
import TipManagement from "./views/TipManagement";
import EmployeeDashboard from "./views/EmployeeDashboard";
import { CompanyTrendPage } from "./views/CompanyTrendPage";
import DepartmentViolationsPage from "./views/DepartmentViolationsPage";
import SysAdminDashboard from "./views/SysAdminDashboard";
import { SeniorManagerDashboard } from "./views/SeniorManagerDashboard";
import DepartmentHeadDashboard from "./views/DepartmentHeadDashboard";
import BuildingManagerDashboard from "./views/BuildingManagerDashboard";
import { ROUTES } from "./utilities/routes.paths";

/**
 * Define the routes of the application.
 */

export const HomePageRoute = {
    url: '/',
    component: RoleBasedHome
}

export const ManageUsersRoute = {
    url: '/manage-users',
    component: UserConfigurationPage
}
export const LoginsRoute = {
    url: '/login',
    component: Login
}
export const LogoutsRoute = {
    url: '/logout',
    component: Logout
}

export const DeviceConfigurationRoute = {
    url: '/device-configuration',
    component: DeviceConfigurationPage
}

export const UserConfigurationRoute = {
    url: '/user-configuration',
    component: UserConfigurationPage
}

export const BuildingConfigurationRoute = {
    url: '/building-configuration',
    component: BuildingConfigurationPage
}

export const EmployeeAbsencesRoute = {
    url: '/absences',
    component: EmployeeAbsencesPage
}

export const DepartmentAbsencesRoute = {
    url: '/department-absences',
    component: DepartmentAbsencesPage
}

export const EmployeeDepartmentRoute = {
    url: '/my-department',
    component: EmployeeDepartmentPage
}

export const SettingsRoute = {
    url: '/settings',
    component: SettingsPage
}

export const DepartmentDetailRoute = {
    url: '/senior/department/:departmentName',
    component: DepartmentDetailPage
}

export const RoomDetailRoute = {
    url: '/department/room/:roomId',
    component: RoomDetailPage
}

export const BuildingRoomAnalysisRoute = {
    url: '/building-room-analysis/:roomId',
    component: BuildingRoomAnalysisPage
}

export const TipManagementRoute = {
    url: '/tipmanagement',
    component: TipManagement
}

export const MyRoomRoute = {
    url: '/my-room',
    component: EmployeeDashboard
}

export const CompanyTrendsRoute = {
    url: '/senior/trends',
    component: CompanyTrendPage
}

export const DepartmentViolationsRoute = {
    url: '/department-violations',
    component: DepartmentViolationsPage
}

// One dedicated, directly linkable URL per role dashboard. A user holding multiple
// roles gets a route (and sidebar entry, see SidebarComponent) for every dashboard
// their roles grant them, instead of only the highest-priority one.
export const SysAdminDashboardRoute = {
    url: ROUTES.SYSADMIN_DASHBOARD,
    component: SysAdminDashboard
}

export const SeniorManagerDashboardRoute = {
    url: ROUTES.SENIOR_MANAGER_DASHBOARD,
    component: SeniorManagerDashboard
}

export const DepartmentManagerDashboardRoute = {
    url: ROUTES.DEPARTMENT_MANAGER_DASHBOARD,
    component: DepartmentHeadDashboard
}

export const BuildingManagerDashboardRoute = {
    url: ROUTES.BUILDING_MANAGER_DASHBOARD,
    component: BuildingManagerDashboard
}

export const EmployeeDashboardRoute = {
    url: ROUTES.EMPLOYEE_DASHBOARD,
    component: EmployeeDashboard
}