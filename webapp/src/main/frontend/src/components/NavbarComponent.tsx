/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import React from 'react';
import {Menubar} from "primereact/menubar";

import {useUser} from "../Contexts/AuthenticatedUserContext";
import {menuConfig, MenuItemConfig} from "../config/menuConfig";
import {UserxRole} from "../generated-skeleton-api";
import {MenuItem} from "primereact/menuitem";
import {Link} from "react-router-dom";

/**
 * Navbar component.
 */
const NavbarComponent: React.FC = () => {
    const {currentUser: user} = useUser();

    const filterMenu = React.useCallback((items: MenuItemConfig[]): MenuItemConfig[] => {
        if (!user) return [];

        const hasRole = (required?: UserxRole[]) =>
            !required || required.some(r => {
                return user.roles?.has(r) ?? false;
            });

        return items
            .map(item => {
                const visibleChildren = item.items ? filterMenu(item.items) : undefined;
                return {...item, items: visibleChildren};
            })
            .filter(item => {
                const visible = hasRole(item.roles);
                const hasChildren = !!item.items?.length;
                // Keep if user can see it, or it has visible children
                return visible || hasChildren;
            });
    }, [user]);

    // we want to use navigate (react router) to ensure pure client-side navigation on menu item click
    // incidentally, we also want to fix primereact component-related aria warnings
    const buildMenubar = React.useCallback((items: MenuItemConfig[]): MenuItem[] => {
        return items.map(configItem => {
            const children = configItem.items ? buildMenubar(configItem.items) : undefined;

            const menuItem: MenuItem = {
                label: configItem.label,
                icon: configItem.icon,
                items: children,
            };

            menuItem.template = (menuItem, options) => {
                const handleClick = (e: React.MouseEvent<HTMLElement>) => {
                    options.onClick?.(e);
                };

                return (
                    <Link
                        to={configItem.route ?? "#"}
                        className={`${options.className ?? ""} p-menuitem-link`}
                        onClick={handleClick}
                    >
                        {menuItem.icon && <span className={options.iconClassName}/>}
                        <span className={options.labelClassName}>{menuItem.label}</span>
                    </Link>
                );
            }
            return menuItem;
        });
    }, []);

    const filteredItems = React.useMemo(() => filterMenu(menuConfig), [filterMenu]);

    const model = React.useMemo(() => buildMenubar(filteredItems), [filteredItems, buildMenubar]);

    // don't render Menubar if no user is logged in
    if (!user) {
        return null;
    }

    return (
        <div className="card">
            <Menubar model={model}/>
        </div>
    );
}

export default NavbarComponent;
