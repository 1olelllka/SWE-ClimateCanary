/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import {useState} from "react";

import {Button} from "primereact/button";
import {InputText} from "primereact/inputtext";
import {Password} from "primereact/password";

import logo from '../logo-transparent.png';
import '../styles/Login.css';

import {useNavigate} from 'react-router-dom';
import {useUser} from "../Contexts/AuthenticatedUserContext";

/**
 * Login component
 */

const Login = () => {

    // States
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);
    // use the user context to set the current user
    const {login} = useUser();

    const navigate = useNavigate();


    /**
     * Handle login event and send login request to the server
     * @param e Form event
     *
     * Sets error eventMessage if login fails
     * Redirects to home page if login is successful
     *
     */
    const handleLogin = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        if (loading) {
            return;
        }

        setError(null);
        setLoading(true);

        try {
            await login({username, password});
            // Redirect to home page
            navigate("/", {replace: true});
        } catch (err: any) {
            const status = err?.response?.status as number | undefined;
            if (status === 401 || status === 403) {
                setError('Wrong username or password');
            } else if (status === 500) {
                setError('Server error');
            } else if (status === undefined) {
                setError('No connection to server. Try again later');
            } else {
                setError('Login failed. Please try again.')
            }
            console.error('Login failed:', error);
        } finally {
            setPassword("");
            setLoading(false);
        }
    };

    return (
        <div className="login-page-wrapper">
            <div className="login-mockup-card">

                {/* Header Bereich mit Logo und Slogan */}
                <div className="login-header">
                    <img src={logo} alt="ClimateCanary Logo" className="brand-logo" />
                    <h1 className="brand-title">Welcome to<br/>ClimateCanary</h1>
                    <p className="brand-subtitle">Your early warning system for office climate.</p>
                </div>

                {/* Login Formular */}
                <div className="form-section">
                    <h2 className="login-heading">Login</h2>
                    <form onSubmit={handleLogin} className="login-form">

                        <div className="input-group">
                            <InputText
                                id="username"
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                required
                                autoComplete="off"
                                placeholder="Username"
                                className="mockup-input"
                            />
                        </div>

                        <div className="input-group password-group">
                            <Password
                                inputId="password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                required
                                feedback={false}
                                toggleMask={false}
                                autoComplete="off"
                                placeholder="Password"
                                className="password-input"
                            />
                            <button type="button" className="forgot-link">
                                Forgot?
                            </button>
                        </div>

                        <Button
                            type="submit"
                            label="Sign in"
                            className="mockup-button"
                            loading={loading}
                        />
                    </form>

                    {error && <p className="error-message">{error}</p>}
                </div>
            </div>
        </div>
    );
};


export default Login
