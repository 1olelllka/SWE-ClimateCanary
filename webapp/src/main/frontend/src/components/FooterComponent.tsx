import React from "react";
import {useUser} from "../Contexts/AuthenticatedUserContext";
import "../styles/Footer.css"
import {rolesBodyTemplate} from "./rolesBodyTemplate";


export const FooterComponent: React.FC = () => {

    // get user context
    const {currentUser} = useUser();

    return <footer>
        <span>Logged in as: {currentUser?.firstName} {currentUser?.lastName} ({currentUser?.username})</span>
        <span>Roles:&ensp; {currentUser ? rolesBodyTemplate(currentUser) : null}</span>
    </footer>;
};
