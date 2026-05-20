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
import DepartmentDetailPage from "./views/DepartmentDetailPage";
import BuildingRoomAnalysisPage from "./views/BuildingRoomAnalysisPage";
import TipManagement from "./views/TipManagement";
import { CompanyTrendPage } from "./views/CompanyTrendPage";

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

// referenced but not yet routed — kept to avoid unused-import warnings
const _unused = [HomePage, EmployeeDashboard];