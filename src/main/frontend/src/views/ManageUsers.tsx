/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import '../styles/App.css';
import "primereact/resources/themes/lara-light-cyan/theme.css";
import React from "react";
import NavbarComponent from "../components/NavbarComponent";
import UserTableComponent from "../components/UserTableComponent";
import {FooterComponent} from "../components/FooterComponent";

/**
 * Component / View for managing users.
 */

class ManageUsers extends React.Component {

    render() {
        return (
            <div>
                <NavbarComponent/>
                <UserTableComponent/>
                <FooterComponent/>
            </div>
        );
    }
}

export default ManageUsers;
