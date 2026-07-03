import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [vue()],
  root: 'dev',
  server: {
    host: '127.0.0.1',
    port: 30004,
    strictPort: true,
    proxy: {
      '/auth': {
        target: 'http://127.0.0.1:18002',
        changeOrigin: true,
      },
      '/link': {
        target: 'http://127.0.0.1:18002',
        changeOrigin: true,
      },
      '/system': {
        target: 'http://127.0.0.1:18002',
        changeOrigin: true,
      },
    },
  },
});
