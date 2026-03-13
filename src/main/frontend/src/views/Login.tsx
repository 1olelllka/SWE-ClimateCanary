/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import {useState} from "react";

import {Button} from "primereact/button";
import {FloatLabel} from 'primereact/floatlabel';
import {InputText} from "primereact/inputtext";
import {Password} from "primereact/password";

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
        <div className="login-container">
            <div className="login-card">
                <h2>Login</h2>
                <form onSubmit={handleLogin}>
                    <FloatLabel style={{marginTop: 50}}>
                        <InputText
                            id="username"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            required
                            autoComplete="off"
                            className="input-field"
                        />
                        <label htmlFor="username">Username:</label>
                    </FloatLabel>

                    <FloatLabel style={{marginTop: 25}}>
                        <Password
                            inputId="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                            feedback={false}
                            autoComplete="off"
                            className="input-field"
                        />
                        <label htmlFor="password">Password:</label>
                    </FloatLabel>
                    <Button type="submit" label="Login" className="loginButton"/>
                </form>
                {error && <p style={{color: 'red', marginTop: 25}}>{error}</p>}
            </div>
        </div>
    );
};


export default Login
