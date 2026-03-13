import {Tag} from "primereact/tag";
import React from "react";
import {UserxDTO} from "../generated-skeleton-api";

/**
 * Renders the roles of a user as tags (such beautiful).
 * @param rowData
 */
export const rolesBodyTemplate = (rowData: UserxDTO) => {
    if (!rowData.roles) return null;
    return <>
        {[...rowData.roles].map(role => {
            return <Tag key={role} value={role} severity="info" style={{marginRight: '.5em'}}/>
        })}
    </>;
};