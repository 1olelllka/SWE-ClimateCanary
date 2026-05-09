import './styles/App.css';
import "primereact/resources/themes/lara-light-cyan/theme.css";
import "primeicons/primeicons.css";
import React, { Suspense } from "react";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import {
    HomePageRoute,
    LoginsRoute,
    LogoutsRoute,
    ManageUsersRoute,
    DeviceConfigurationRoute,
    UserConfigurationRoute,
    BuildingConfigurationRoute,
    EmployeeAbsencesRoute,
    DepartmentAbsencesRoute,
    EmployeeDepartmentRoute,
    SettingsRoute,
    DepartmentDetailRoute,
    BuildingRoomAnalysisRoute,
    TipManagementRoute
} from "./routes";
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
                            <Route path={EmployeeAbsencesRoute.url} Component={EmployeeAbsencesRoute.component}/>
                            <Route path={ManageUsersRoute.url} Component={ManageUsersRoute.component}/>
                            <Route path={LogoutsRoute.url} Component={LogoutsRoute.component}/>
                            <Route path={DeviceConfigurationRoute.url} Component={DeviceConfigurationRoute.component}/>
                            <Route path={UserConfigurationRoute.url} Component={UserConfigurationRoute.component}/>
                            <Route path={BuildingConfigurationRoute.url} Component={BuildingConfigurationRoute.component}/>
                            <Route path={DepartmentAbsencesRoute.url} Component={DepartmentAbsencesRoute.component}/>
                            <Route path={EmployeeDepartmentRoute.url} Component={EmployeeDepartmentRoute.component}/>
                            <Route path={SettingsRoute.url} Component={SettingsRoute.component}/>
                            <Route path={DepartmentDetailRoute.url} Component={DepartmentDetailRoute.component}/>
                            <Route path={BuildingRoomAnalysisRoute.url} Component={BuildingRoomAnalysisRoute.component}/>
                            <Route path={TipManagementRoute.url} Component={TipManagementRoute.component}/>
                        </Route>
                    </Routes>
                </BrowserRouter>
            </Suspense>
        </UserProvider>
    );
}

export default App;