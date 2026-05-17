import vue from '@vitejs/plugin-vue';
import { defineConfig, loadEnv, type ConfigEnv } from 'vite';
import { resolve } from 'path';
import { mangoMicroManualChunks } from '../../build-config/microChunks';

const ALLOWED_PROXY_HOSTS = ['127.0.0.1', 'localhost'];
const DEV_ALLOWED_HOSTS = ['localhost', '127.0.0.1', 'a.mango.io', 'b.mango.io', 'c.mango.io'];

function validateProxyTarget(target: string): string {
  const url = new URL(target);
  if (!ALLOWED_PROXY_HOSTS.includes(url.hostname)) {
    throw new Error(`Proxy target hostname '${url.hostname}' not allowed`);
  }
  return target;
}

export default defineConfig((mode: ConfigEnv) => {
  const env = loadEnv(mode.mode, process.cwd());
  const proxyTarget = validateProxyTarget(env.VITE_ADMIN_PROXY_PATH || 'http://127.0.0.1:18081');

  return {
    base: env.VITE_PUBLIC_PATH || '/',
    plugins: [vue()],
    resolve: {
      alias: {
        '@': resolve(__dirname, './src'),
      },
    },
    server: {
      host: env.VITE_HOST || '0.0.0.0',
      port: Number(env.VITE_PORT || 5176),
      allowedHosts: DEV_ALLOWED_HOSTS,
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, ''),
        },
      },
    },
    build: {
      outDir: 'dist',
      target: 'es2020',
      rollupOptions: {
        output: {
          manualChunks: mangoMicroManualChunks,
        },
      },
    },
  };
});
