import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

const port = Number(process.env.VITE_PORT || process.env.MANGO_LINK_PAGE_PORT || 30004);
const host = process.env.VITE_HOST || '127.0.0.1';
const backendTarget = process.env.VITE_ADMIN_PROXY_PATH
  || `http://127.0.0.1:${process.env.MANGO_BACKEND_PORT || 18080}`;

export default defineConfig({
  plugins: [vue()],
  root: 'dev',
  server: {
    host,
    port,
    strictPort: true,
    proxy: {
      '/auth': {
        target: backendTarget,
        changeOrigin: true,
      },
      '/link': {
        target: backendTarget,
        changeOrigin: true,
      },
      '/system': {
        target: backendTarget,
        changeOrigin: true,
      },
    },
  },
});
