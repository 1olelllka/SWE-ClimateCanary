/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import {useNavigate} from "react-router-dom";
import {useUser} from "../Contexts/AuthenticatedUserContext";
import {useEffect} from "react";
import {ROUTES} from "../utilities/routes.paths";

/**
 * Logout component
 */

const Logout = () => {

    const navigate = useNavigate();
    const {logout} = useUser();

    useEffect(() => {
        const handleLogout = async () => {
            // clear user data via user context regardless of success or failure
            logout();
            // redirect to login page
            navigate(ROUTES.LOGIN);
        };
        void handleLogout();
    }, [logout, navigate]);

    return (
        <div>
        </div>
    )

}

export default Logout
