import {defineConfig, loadEnv} from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({mode}) => {
    const env = loadEnv(mode, process.cwd(), '')

    return {
        build: {
            outDir: 'build',
        },
        server: {
            port: 3000,
            proxy: {
                '/api': {
                    target: 'http://localhost:8080',
                    changeOrigin: true,
                },
                '/active-warnings': {
                    target: 'http://localhost:8080',
                    changeOrigin: true,
                    ws: true,
                },
            },
        },
        plugins: [react()],
        define: {
            REACT_APP_BACKEND_SERVER_URL: JSON.stringify(env.REACT_APP_BACKEND_SERVER_URL),
            global: 'globalThis',
        },
    };
});