/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */

import HomePage from "./views/HomePage";
import UserConfigurationPage from "./views/UserConfigurationPage";
import Login from "./views/Login";
import Logout from "./views/Logout";
import {ROUTES} from "./utilities/routes.paths";
import EmployeeDashboard from "./views/EmployeeDashboard";
import RoleBasedHome from "./views/RoleBasedHome";
import DeviceConfigurationPage from "./views/DeviceConfigurationPage";
import UserConfigurationPage from "./views/UserConfigurationPage";
import BuildingConfigurationPage from "./views/BuildingConfigurationPage";

/**
 * Define the routes of the application.
 */

export const HomePageRoute = {
    url: ROUTES.HOME,
    component: RoleBasedHome
}

export const ManageUsersRoute = {
    url: ROUTES.MANAGE_USERS,
    component: UserConfigurationPage
}
export const LoginsRoute = {
    url: ROUTES.LOGIN,
    component: Login
}
export const LogoutsRoute = {
    url: ROUTES.LOGOUT,
    component: Logout
}

export const DeviceConfigurationRoute = {
    url: ROUTES.DEVICE_CONFIGURATION,
    component: DeviceConfigurationPage
}

export const UserConfigurationRoute = {
    url: ROUTES.USER_CONFIGURATION,
    component: UserConfigurationPage
}

export const BuildingConfigurationRoute = {
    url: ROUTES.BUILDING_CONFIGURATION,
    component: BuildingConfigurationPage
}