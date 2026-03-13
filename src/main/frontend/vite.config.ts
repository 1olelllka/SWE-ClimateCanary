import {defineConfig, loadEnv} from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({command, mode}) => {
    const env = loadEnv(mode, process.cwd(), '')
    return {
        build: {
            outDir: 'build',
        },
        server: {
            port: 3000
        },
        plugins: [react()],
        define: {
            REACT_APP_BACKEND_SERVER_URL: JSON.stringify(env.REACT_APP_BACKEND_SERVER_URL),
        },
    };
});