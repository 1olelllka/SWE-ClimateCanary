import '../styles/LogoutButton.css';
import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useUser } from "../Contexts/AuthenticatedUserContext";

export const LogoutButton: React.FC = () => {
    const navigate = useNavigate();
    const { logout } = useUser();

    const handleLogout = async () => {
        await logout();
        navigate('/login', { replace: true });
    };

    return (
        <button className="logout-btn" onClick={handleLogout}>
            <i className="pi pi-sign-out"></i>
            <span className="hide-on-mobile">Logout</span>
        </button>
    );
};