import './styles/App.css';
import "primereact/resources/themes/lara-light-cyan/theme.css";
import "primeicons/primeicons.css";
import React, { Suspense } from "react";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { HomePageRoute, LoginsRoute, LogoutsRoute, ManageUsersRoute } from "./routes";
import PrivateRoute from './components/PrivateRoute';
import { UserProvider } from "./Contexts/AuthenticatedUserContext";

const App: React.FC = () => {
    return (
        <UserProvider>
            <Suspense fallback={<div>Loading...</div>}>
                <BrowserRouter>
                    <Routes>
                        <Route path={LoginsRoute.url} Component={LoginsRoute.component}/>
                        <Route element={<PrivateRoute/>}>
                            <Route path={HomePageRoute.url} Component={HomePageRoute.component}/>
                            <Route path={ManageUsersRoute.url} Component={ManageUsersRoute.component}/>
                            <Route path={LogoutsRoute.url} Component={LogoutsRoute.component}/>
                        </Route>
                    </Routes>
                </BrowserRouter>
            </Suspense>
        </UserProvider>
    );
}

export default App;