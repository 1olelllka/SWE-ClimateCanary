/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import logo from '../logo.svg';
import '../styles/App.css';
import "primereact/resources/themes/lara-light-cyan/theme.css";
import { Message } from 'primereact/message';
import React from "react";
import NavbarComponent from "../components/NavbarComponent";
import { FooterComponent } from "../components/FooterComponent";
import { TestControllerApi } from "../generated-skeleton-api";

class HomePage extends React.Component<any, any> {

    constructor(props: any) {
        super(props);
        this.state = {
            piMessage: "Backend wartet...",
            message: "",
            status: ""
        };
    }

    componentDidMount() {
        const api = new TestControllerApi();
        api.sayHello()
            .then((response) => {
                this.setState({ piMessage: response.data.message });
                console.log("Connection Successful:", response.data.message);
            })
            .catch((error) => {
                this.setState({ piMessage: "Error: Backend is not accessable" });
                console.error("Error:", error);
            });
    }

    handleSend = async () => {
        const { message } = this.state;
        if (!message.trim()) return;

        console.log("Versuche, Nachricht ans Backend zu senden:", message);

        try {
            const response = await fetch('http://172.20.10.5:8080/test-raspberry', {
                method: 'GET',
                headers: { 'Content-Type': 'application/json' },
            });

            if (response.ok) {
                this.setState({ status: "Erfolgreich ans Backend gesendet!", message: "" });
            } else {
                this.setState({ status: `Warte auf Backend... (Fehler ${response.status})` });
            }
        } catch (error) {
            console.error("Netzwerkfehler:", error);
            this.setState({ status: "Backend ist nicht erreichbar." });
        }

        setTimeout(() => this.setState({ status: "" }), 4000);
    };

    render() {
        return (
            <div>
                <NavbarComponent/>
                <div className="App">
                    <header className="App-header">
                        <img src={logo} className="App-logo" alt="logo"/>

                        {/* Durchstich - Pi Message */}
                        <div style={{ margin: '20px', padding: '15px', border: '2px dashed #61dafb', borderRadius: '10px' }}>
                            <h3 style={{ color: '#61dafb' }}>🚀 Durchstich 23.03.2026</h3>
                            <p>Status: <strong>{this.state.piMessage}</strong></p>
                        </div>

                        {/* Send Message to Arduino */}
                        <div style={{ padding: '20px', background: 'rgba(255, 255, 255, 0.1)', borderRadius: '8px', marginTop: '20px' }}>
                            <h3 style={{ margin: '0 0 15px 0' }}>Nachricht an Arduino</h3>
                            <div style={{ display: 'flex', gap: '10px', justifyContent: 'center' }}>
                                <input
                                    type="text"
                                    value={this.state.message}
                                    onChange={(e) => this.setState({ message: e.target.value })}
                                    placeholder="z.B. Webapp Says Hello"
                                    style={{ padding: '10px', width: '250px', borderRadius: '4px', border: 'none', outline: 'none' }}
                                />
                                <button
                                    onClick={this.handleSend}
                                    style={{ padding: '10px 20px', cursor: 'pointer', background: '#007ad9', color: 'white', border: 'none', borderRadius: '4px', fontWeight: 'bold' }}
                                >
                                    Senden
                                </button>
                            </div>
                            {this.state.status && (
                                <p style={{
                                    color: this.state.status.startsWith('Erfolgreich') ? '#4ade80' :
                                        this.state.status.startsWith('Warte') ? '#facc15' : '#fca5a5',
                                    marginTop: '15px',
                                    fontWeight: 'bold',
                                    fontSize: '16px'
                                }}>
                                    {this.state.status}
                                </p>
                            )}
                        </div>

                        <p>Welcome to the SWA Skeleton Project!</p>
                        <Message severity={"success"} text={"PrimeReact is installed!"}/>
                    </header>
                </div>
                <FooterComponent/>
            </div>
        );
    }
}

export default HomePage;
