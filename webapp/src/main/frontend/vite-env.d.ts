declare const REACT_APP_BACKEND_SERVER_URL: string;

declare module '*.css?url' {
    const url: string;
    export default url;
}

declare module '*.png' {
    const src: string;
    export default src;
}