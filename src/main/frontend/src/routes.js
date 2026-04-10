/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */

import ManageUsers from "./views/ManageUsers";
import Login from "./views/Login";
import Logout from "./views/Logout";
import RoleBasedHome from "./views/RoleBasedHome";

/**
 * Define the routes of the application.
 */

export const HomePageRoute = {
    url: '/',
    component: RoleBasedHome
}

export const ManageUsersRoute = {
    url: '/manage-users',
    component: ManageUsers
}
export const LoginsRoute = {
    url: '/login',
    component: Login
}
export const LogoutsRoute = {
    url: '/logout',
    component: Logout
}