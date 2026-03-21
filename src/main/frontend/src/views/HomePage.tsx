import logo from '../logo.svg';
import '../styles/App.css';
import "primereact/resources/themes/lara-light-cyan/theme.css";
import { Message } from 'primereact/message';
import React from "react";
import NavbarComponent from "../components/NavbarComponent";
import { FooterComponent } from "../components/FooterComponent";

class HomePage extends React.Component<any, any> {

    constructor(props: any) {
        super(props);
        this.state = {
            piMessage: "Warten auf das Signal...", //message from Pi
            message: "", //message to send to Pi
            status: "" //status for sending the message
        };
    }

    getStatusColor(): string {
        const { status } = this.state;
        if (status.startsWith('Erfolgreich')) return '#4ade80';
        if (status.startsWith('Fehler')) return '#facc15';        // backend responded but with error
        if (status.startsWith('Sende')) return '#aaaaaa';
        return '#fca5a5';                                         // unreachable
    }

    //Message received from Backend is shown here
    componentDidMount() {
        fetch('http://172.20.10.5:8080/test-info')
            .then((response) => response.json())
            .then((data) => {
                this.setState({ piMessage: data.message });
                console.log("Server hat eine Nachricht vom Raspberry Pi erhalten:", data.message);
            })
            .catch((error) => {
                this.setState({ piMessage: "Server ist nicht erreichbar" });
                console.error("Serververbindung fehlgeschlagen", error);
            });
    }

    //Send message to Backend
    handleSend = async () => {
        const { message } = this.state;
        if (!message.trim()) return;

        this.setState({ message: "", status: "Sende..." });

        console.log("Versuche, Nachricht ans Backend zu senden:", message);

        try {
            const response = await fetch('http://172.20.10.5:8080/send-to-raspberry', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ message })  // actually sends the message written
            });

            if (response.ok) {
                this.setState({ status:  `Erfolgreich gesendet: "${message}"`, message: "" }); //resets field for input with ""
            } else {
                this.setState({ status: `Fehler ${response.status}` });
            }
        } catch (error) {
            console.error("Netzwerkfehler:", error);
            this.setState({ status: "Backend ist nicht erreichbar." });
        }
    };

    render() {
        return (
            <div>
                <NavbarComponent/>
                <div className="App">
                    <header className="App-header">
                        <img src={logo} className="App-logo" alt="logo"/>

                        {/*  Pi Message */}
                        <div style={{ margin: '20px', padding: '15px', border: '2px dashed #61dafb', borderRadius: '10px' }}>
                            <h3 style={{ color: '#61dafb' }}>🚀 Durchstich 23.03.2026</h3>
                            <p>Status: <strong>{this.state.piMessage}</strong></p>
                        </div>

                        {/* Send Message to Arduino */}
                        <div style={{ padding: '20px', background: 'rgba(255, 255, 255, 0.1)', borderRadius: '8px', marginTop: '20px' }}>
                            <h3 style={{ margin: '0 0 15px 0' }}>Nachricht an Arduino</h3>
                            <div style={{display: 'flex', gap: '10px', justifyContent: 'center'}}>
                                <input
                                    type="text"
                                    value={this.state.message}
                                    onChange={(e) => this.setState({message: e.target.value})}
                                    onKeyDown={(e) => {
                                        if (e.key === 'Enter') this.handleSend();
                                    }} // to be able to send with Enter
                                    placeholder="z.B. Webapp Says Hello"
                                    style={{
                                        padding: '10px',
                                        width: '250px',
                                        borderRadius: '4px',
                                        border: 'none',
                                        outline: 'none'
                                    }}
                                />
                                <button
                                    onClick={this.handleSend}
                                    style={{
                                        padding: '10px 20px',
                                        cursor: 'pointer',
                                        background: '#007ad9',
                                        color: 'white',
                                        border: 'none',
                                        borderRadius: '4px',
                                        fontWeight: 'bold'
                                    }}
                                >
                                    Senden
                                </button>
                            </div>
                            {this.state.status && (
                                <p style={{
                                    color: this.getStatusColor(),
                                    marginTop: '15px',
                                    fontWeight: 'bold',
                                    fontSize: '16px'
                                }}>
                                    {this.state.status}
                                </p>
                            )}
                        </div>
                        <Message severity={"success"} text={"PrimeReact is installed!"}/>
                    </header>
                </div>
                <FooterComponent/>
            </div>
        );
    }
}

export default HomePage;