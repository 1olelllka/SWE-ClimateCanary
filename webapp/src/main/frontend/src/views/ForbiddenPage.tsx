import { Button } from 'primereact/button';
import { useNavigate } from 'react-router-dom';
import logo from '../logo-transparent.png';
import '../styles/Login.css';
import { ROUTES } from '../utilities/routes.paths';

const ForbiddenPage = () => {
    const navigate = useNavigate();

    return (
        <div className="login-page-wrapper">
            <div className="login-mockup-card">
                <div className="login-header">
                    <img src={logo} alt="ClimateCanary Logo" className="brand-logo" />
                    <h1 className="brand-title" style={{ fontSize: '72px' }}>403</h1>
                    <h2 style={{ fontWeight: 'normal', margin: '8px 0' }}>Access Denied</h2>
                    <p className="brand-subtitle">You don't have permission to view this page.</p>
                </div>
                <Button
                    label="Go to Home"
                    className="mockup-button"
                    onClick={() => navigate(ROUTES.HOME, { replace: true })}
                />
            </div>
        </div>
    );
};

export default ForbiddenPage;
