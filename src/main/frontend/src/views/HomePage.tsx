/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import logo from '../logo.svg';
import '../styles/App.css';
import "primereact/resources/themes/lara-light-cyan/theme.css";
import {Message} from 'primereact/message';
import React from "react";
import NavbarComponent from "../components/NavbarComponent";
import {FooterComponent} from "../components/FooterComponent";

/**
 * The home page of the application.
 */
class HomePage extends React.Component {
    render() {
        return (
            <div>
                <NavbarComponent/>
                <div className="App">
                    <header className="App-header">
                        <img src={logo} className="App-logo" alt="logo"/>
                        <p>
                            Welcome to the SWA Skeleton Project!
                        </p>
                        <a
                            className="App-link"
                            href="https://reactjs.org"
                            target="_blank"
                            rel="noopener noreferrer"
                        >
                            Learn React
                        </a>
                        <span style={{paddingTop: '20px'}}/>
                        <Message severity={"success"} text={"PrimeReact is installed!"}/>
                    </header>
                </div>
                <FooterComponent/>
            </div>
        );
    }
}

export default HomePage;
