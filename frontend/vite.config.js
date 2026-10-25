import {defineConfig} from 'vite';
import react from '@vitejs/plugin-react';
import {resolve} from 'path';

export default defineConfig({
  base: '/it/',
  plugins: [react()],
  build: {outDir: resolve(__dirname, '../src/main/resources/static'), emptyOutDir: true},
  server: {port: 5173, proxy: {'/it/api': 'http://localhost:3013'}},
});
