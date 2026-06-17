/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */

import HomePage from "./views/HomePage";
import UserConfigurationPage from "./views/UserConfigurationPage";
import Login from "./views/Login";
import Logout from "./views/Logout";
import { ROUTES } from "./utilities/routes.paths";
import EmployeeDashboard from "./views/EmployeeDashboard";
import RoleBasedHome from "./views/RoleBasedHome";
import DeviceConfigurationPage from "./views/DeviceConfigurationPage";
import BuildingConfigurationPage from "./views/BuildingConfigurationPage";
import EmployeeAbsencesPage from "./views/EmployeeAbsencesPage";
import DepartmentAbsencesPage from "./views/DepartmentAbsencesPage";
import EmployeeDepartmentPage from "./views/EmployeeDepartmentPage";
import SettingsPage from "./views/SettingsPage";
import RoomDetailPage from "./views/RoomDetailPage";
import DepartmentDetailPage from "./views/DepartmentDetailPage";
import BuildingRoomAnalysisPage from "./views/BuildingRoomAnalysisPage";
import TipManagement from "./views/TipManagement";
import { CompanyTrendPage } from "./views/CompanyTrendPage";
import DepartmentViolationsPage from "./views/DepartmentViolationsPage";
import SysAdminDashboard from "./views/SysAdminDashboard";
import { SeniorManagerDashboard } from "./views/SeniorManagerDashboard";
import DepartmentHeadDashboard from "./views/DepartmentHeadDashboard";
import BuildingManagerDashboard from "./views/BuildingManagerDashboard";

export const HomePageRoute = {
    url: ROUTES.HOME,
    component: RoleBasedHome,
};

export const ManageUsersRoute = {
    url: ROUTES.MANAGE_USERS,
    component: UserConfigurationPage,
};

export const LoginsRoute = {
    url: ROUTES.LOGIN,
    component: Login,
};

export const LogoutsRoute = {
    url: ROUTES.LOGOUT,
    component: Logout,
};

export const DeviceConfigurationRoute = {
    url: ROUTES.DEVICE_CONFIGURATION,
    component: DeviceConfigurationPage,
};

export const UserConfigurationRoute = {
    url: ROUTES.USER_CONFIGURATION,
    component: UserConfigurationPage,
};

export const BuildingConfigurationRoute = {
    url: ROUTES.BUILDING_CONFIGURATION,
    component: BuildingConfigurationPage,
};

export const EmployeeAbsencesRoute = {
    url: ROUTES.EMPLOYEE_ABSENCES,
    component: EmployeeAbsencesPage,
};

export const DepartmentAbsencesRoute = {
    url: ROUTES.DEPARTMENT_ABSENCES,
    component: DepartmentAbsencesPage,
};

export const EmployeeDepartmentRoute = {
    url: ROUTES.EMPLOYEE_DEPARTMENT,
    component: EmployeeDepartmentPage,
};

export const SettingsRoute = {
    url: ROUTES.SETTINGS,
    component: SettingsPage,
};

export const DepartmentDetailRoute = {
    url: ROUTES.DEPARTMENT_DETAIL,
    component: DepartmentDetailPage,
};

export const BuildingRoomAnalysisRoute = {
    url: ROUTES.BUILDING_ROOM_ANALYSIS,
    component: BuildingRoomAnalysisPage
};

export const TipManagementRoute = {
    url: ROUTES.TIPMANAGEMENT,
    component: TipManagement
}

export const CompanyTrendsRoute = {
    url: ROUTES.COMPANY_TRENDS,
    component: CompanyTrendPage
}

export const DepartmentViolationsRoute = {
    url: ROUTES.DEPARTMENT_VIOLATIONS,
    component: DepartmentViolationsPage,
}

export const RoomDetailRoute = {
    url: ROUTES.ROOM_DETAIL,
    component: RoomDetailPage,
}

export const MyRoomRoute = {
    url: ROUTES.MY_ROOM,
    component: EmployeeDashboard,
}

// One dedicated, directly linkable URL per role dashboard. A user holding multiple
// roles gets a route (and sidebar entry, see SidebarComponent) for every dashboard
// their roles grant them, instead of only the highest-priority one.
export const SysAdminDashboardRoute = {
    url: ROUTES.SYSADMIN_DASHBOARD,
    component: SysAdminDashboard,
}

export const SeniorManagerDashboardRoute = {
    url: ROUTES.SENIOR_MANAGER_DASHBOARD,
    component: SeniorManagerDashboard,
}

export const DepartmentManagerDashboardRoute = {
    url: ROUTES.DEPARTMENT_MANAGER_DASHBOARD,
    component: DepartmentHeadDashboard,
}

export const BuildingManagerDashboardRoute = {
    url: ROUTES.BUILDING_MANAGER_DASHBOARD,
    component: BuildingManagerDashboard,
}

export const EmployeeDashboardRoute = {
    url: ROUTES.EMPLOYEE_DASHBOARD,
    component: EmployeeDashboard,
}

// referenced but not yet routed — kept to avoid unused-import warnings
const _unused = [HomePage];