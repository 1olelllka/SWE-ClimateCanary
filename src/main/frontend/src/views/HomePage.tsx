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
import {createLogger} from "vite";

/**
 * The home page of the application.
 */
class HomePage extends React.Component<any, any> {
    constructor(props: any) {
        super(props);
        this.state = {
            message: "",
            status: ""
        };
    }

    handleSend = async () => {
        const { message } = this.state;
        if (!message.trim()) return; // Nichts senden, wenn das Feld leer ist

        console.log("Versuche, Nachricht ans Backend zu senden:", message);

        try {
            // Nachricht senden
            const response = await fetch('http://172.20.10.5:8080/test-raspberry', {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                },
            });
            console.log(response);
            if (response.ok) {
                // Wenn Backend antwortet
                this.setState({
                    status: "Erfolgreich ans Backend gesendet!",
                    message: ""
                });
            } else {
                // Wenn das Backend läuft, aber die URL noch nicht kennt
                this.setState({ status: `Warte auf Backend... (Fehler ${response.status})` });
            }

        } catch (error) {
            // Wenn Backend ausgeschaltet ist
            console.error("Netzwerkfehler:", error);
            this.setState({ status: "Backend ist nicht erreichbar." });
        }

        // Meldung nach 4 Sekunden wieder ausblenden
        setTimeout(() => this.setState({ status: "" }), 4000);
    };

    render() {
        return (
            <div>
                <NavbarComponent/>
                <div className="App">
                    <header className="App-header">

                        {/* Components Anfang */}
                        <div style={{ padding: '20px', background: 'rgba(255, 255, 255, 0.1)', borderRadius: '8px', marginTop: '40px' }}>
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
                            {/* Zeigt an, ob es geklappt hat, ob wir warten oder ob es einen Fehler gab */}
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
                        {/* Components Ende */}

                    </header>
                </div>
                <FooterComponent/>
            </div>
        );
    }
}

export default HomePage;
