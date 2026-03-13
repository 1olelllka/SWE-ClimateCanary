import {ROUTES} from "../utilities/routes.paths";
import {UserxRole} from "../generated-skeleton-api";

export type MenuItemConfig = {
    label: string;
    icon?: string;
    route?: string;
    roles?: UserxRole[];
    items?: MenuItemConfig[];
};

export const menuConfig: MenuItemConfig[] = [
    {
        label: 'Home', icon: 'pi pi-home', route: ROUTES.HOME
    }, {
        label: 'Admin Submenu', icon: 'pi pi-star',
        roles: [UserxRole.ADMIN],
        items: [{
            label: 'Manage Users', icon: 'pi pi-star', route: ROUTES.MANAGE_USERS, roles: [UserxRole.ADMIN]
        }]
    }, {
        label: "Logout", icon: "pi pi-sign-out", route: ROUTES.LOGOUT
    }
];