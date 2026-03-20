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
import { TestControllerApi } from "../generated-skeleton-api";

/**
 * The home page of the application.
 */
class HomePage extends React.Component<{}, { piMessage: string }> {


    constructor(props: any) {
        super(props);
        this.state = {
            piMessage: "Waiting for Backend..."
        };
    }

    componentDidMount() {
        const api = new TestControllerApi();
        api.sayHello()
            .then((response) => {
                this.setState({ piMessage: response.data.message });
                console.log("Durchstich Erfolg:", response.data.message);
            })
            .catch((error) => {
                this.setState({ piMessage: "Fehler: Backend nicht erreichbar!" });
                console.error("Durchstich Fehler:", error);
            });
    }


    render() {
        return (
            <div>
                <NavbarComponent/>
                <div className="App">
                    <header className="App-header">
                        <img src={logo} className="App-logo" alt="logo"/>


                        {/* --- DURCHSTICH ANZEIGE --- */}
                        <div style={{ margin: '20px', padding: '15px', border: '2px dashed #61dafb', borderRadius: '10px' }}>
                            <h3 style={{ color: '#61dafb' }}>🚀 Durchstich 23.03.2026</h3>
                            <p>Status: <strong>{this.state.piMessage}</strong></p>
                        </div>
                        {/* -------------------------- */}


                        <p>
                            Welcome to the SWA Skeleton Project!
                        </p>
                        <Message severity={"success"} text={"PrimeReact is installed!"}/>
                    </header>
                </div>
                <FooterComponent/>
            </div>
        );
    }
}


export default HomePage;


