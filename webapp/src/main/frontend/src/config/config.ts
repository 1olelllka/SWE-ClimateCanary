/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import globalAxios from "axios";

// Configure the global axios which is used by the generated API
globalAxios.defaults.baseURL = REACT_APP_BACKEND_SERVER_URL;

// Add a request interceptor to add the bearer token to all requests if available
globalAxios.interceptors.request.use(request => {
    const accessToken = localStorage.getItem(BEARER_TOKEN_LOCAL_STORAGE_KEY);
    if (accessToken) {
        request.headers['Authorization'] = `Bearer ${accessToken}`;
    }
    return request;
}, error => {
    return Promise.reject(error instanceof Error ? error : new Error(String(error)));
});


export const BEARER_TOKEN_LOCAL_STORAGE_KEY: string = 'bearerToken';
