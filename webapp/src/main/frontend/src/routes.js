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
import DepartmentDetailPage from "./views/DepartmentDetailPage";
import BuildingRoomAnalysisPage from "./views/BuildingRoomAnalysisPage";

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

export const BuildingRoomAnalysisRoute = {
    url: '/building-room-analysis/:roomId',
    component: BuildingRoomAnalysisPage
}