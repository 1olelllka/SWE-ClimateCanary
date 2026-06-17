import './styles/App.css';
import "primeicons/primeicons.css";
import { ThemeProvider } from "./Contexts/ThemeContext";
import React, { Suspense } from "react";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
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
    TipManagementRoute,
    MyRoomRoute,
    CompanyTrendsRoute,
    DepartmentViolationsRoute,
    RoomDetailRoute,
    SysAdminDashboardRoute,
    SeniorManagerDashboardRoute,
    DepartmentManagerDashboardRoute,
    BuildingManagerDashboardRoute,
    EmployeeDashboardRoute
} from "./routes";
import PrivateRoute from './components/PrivateRoute';
import { UserProvider } from "./Contexts/AuthenticatedUserContext";
import { UserPreferencesProvider } from "./Contexts/UserPreferencesContext";
import NotFoundPage from './views/NotFoundPage';
import ForbiddenPage from './views/ForbiddenPage';
import { ROUTES } from './utilities/routes.paths';

const App: React.FC = () => {
    return (
        <ThemeProvider>
            <UserProvider>
                <UserPreferencesProvider>
                <Suspense fallback={<div>Loading...</div>}>
                    <BrowserRouter>
                        <Routes>
                            <Route path={LoginsRoute.url} Component={LoginsRoute.component}/>
                            <Route path={ROUTES.NOT_FOUND} element={<NotFoundPage />} />

                            {/* Alle eingeloggten User */}
                            <Route element={<PrivateRoute/>}>
                                <Route path={HomePageRoute.url} Component={HomePageRoute.component}/>
                                <Route path={LogoutsRoute.url} Component={LogoutsRoute.component}/>
                                <Route path={SettingsRoute.url} Component={SettingsRoute.component}/>
                                <Route path={ROUTES.FORBIDDEN} element={<ForbiddenPage />} />
                            </Route>

                            {/* Employee routes */}
                            <Route element={<PrivateRoute requiredPermission="CAN_MANAGE_OWN_ABSENCE" />}>
                                <Route path={EmployeeAbsencesRoute.url} Component={EmployeeAbsencesRoute.component}/>
                            </Route>

                            <Route element={<PrivateRoute requiredPermission="CAN_VIEW_OWN_OFFICE_CLIMATE" />}>
                                <Route path={MyRoomRoute.url} Component={MyRoomRoute.component}/>
                                <Route path={EmployeeDashboardRoute.url} Component={EmployeeDashboardRoute.component}/>
                            </Route>

                            {/* Department Head routes */}
                            <Route element={<PrivateRoute requiredPermission="CAN_MANAGE_ABSENCES" />}>
                                <Route path={DepartmentAbsencesRoute.url} Component={DepartmentAbsencesRoute.component}/>
                            </Route>

                            <Route element={<PrivateRoute requiredPermission="CAN_VIEW_OWN_SHARED_CLIMATE" />}>
                                <Route path={EmployeeDepartmentRoute.url} Component={EmployeeDepartmentRoute.component}/>
                            </Route>
                            <Route element={<PrivateRoute requiredPermission="CAN_VIEW_OWN_DEPARTMENT_MEASURES" />}>
                                <Route path={DepartmentManagerDashboardRoute.url} Component={DepartmentManagerDashboardRoute.component}/>
                                <Route path={RoomDetailRoute.url} Component={RoomDetailRoute.component}/>
                            </Route>


                            {/* Building Manager routes */}
                            <Route element={<PrivateRoute requiredPermission="CAN_VIEW_ALL_ROOMS" />}>
                                <Route path={BuildingRoomAnalysisRoute.url} Component={BuildingRoomAnalysisRoute.component}/>
                                <Route path={RoomDetailRoute.url} Component={RoomDetailRoute.component}/>
                            </Route>

                            <Route element={<PrivateRoute requiredPermission="CAN_MANAGE_TIPS" />}>
                                <Route path={TipManagementRoute.url} Component={TipManagementRoute.component}/>
                                <Route path={BuildingManagerDashboardRoute.url} Component={BuildingManagerDashboardRoute.component}/>
                            </Route>

                            {/* Senior Manager routes */}
                            <Route element={<PrivateRoute requiredPermission="CAN_VIEW_COMPANY_AGGR" />}>
                                <Route path={CompanyTrendsRoute.url} Component={CompanyTrendsRoute.component}/>
                                <Route path={SeniorManagerDashboardRoute.url} Component={SeniorManagerDashboardRoute.component}/>
                                <Route path={DepartmentDetailRoute.url} Component={DepartmentDetailRoute.component}/>[]
                            </Route>

                            <Route element={<PrivateRoute requiredPermissions={[
                                "CAN_VIEW_OWN_DEPARTMENT_WARNINGS",
                                "CAN_VIEW_VIOLATIONS_PER_DEPARTMENT"
                            ]} />}>
                                <Route path={DepartmentViolationsRoute.url} Component={DepartmentViolationsRoute.component}/>
                            </Route>

                            {/* Sysadmin routes */}
                            <Route element={<PrivateRoute requiredPermission="CAN_MANAGE_USERS" />}>
                                <Route path={ManageUsersRoute.url} Component={ManageUsersRoute.component}/>
                                <Route path={UserConfigurationRoute.url} Component={UserConfigurationRoute.component}/>
                                <Route path={SysAdminDashboardRoute.url} Component={SysAdminDashboardRoute.component}/>
                            </Route>

                            <Route element={<PrivateRoute requiredPermission="CAN_MANAGE_DEVICES" />}>
                                <Route path={DeviceConfigurationRoute.url} Component={DeviceConfigurationRoute.component}/>
                            </Route>

                            <Route element={<PrivateRoute requiredPermission="CAN_MANAGE_BUILDING_STRUCTURE" />}>
                                <Route path={BuildingConfigurationRoute.url} Component={BuildingConfigurationRoute.component}/>
                            </Route>

                            <Route path="*" element={<Navigate to={ROUTES.NOT_FOUND} replace />} />
                        </Routes>
                    </BrowserRouter>
                </Suspense>
                </UserPreferencesProvider>
            </UserProvider>
        </ThemeProvider>
    );
}

export default App;