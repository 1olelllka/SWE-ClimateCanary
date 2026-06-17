import { Button } from 'primereact/button';
import { useNavigate } from 'react-router-dom';
import logo from '../logo-transparent.png';
import '../styles/Login.css';
import { ROUTES } from '../utilities/routes.paths';

const NotFoundPage = () => {
    const navigate = useNavigate();

    return (
        <div className="login-page-wrapper">
            <div className="login-mockup-card">
                <div className="login-header">
                    <img src={logo} alt="ClimateCanary Logo" className="brand-logo" />
                    <h1 className="brand-title" style={{ fontSize: '72px' }}>404</h1>
                    <h2 style={{ fontWeight: 'normal', margin: '8px 0' }}>Page Not Found</h2>
                    <p className="brand-subtitle">The page you're looking for doesn't exist.</p>
                </div>
                <Button
                    label="Go to Login"
                    className="mockup-button"
                    onClick={() => navigate(ROUTES.LOGIN, { replace: true })}
                />
            </div>
        </div>
    );
};

export default NotFoundPage;
