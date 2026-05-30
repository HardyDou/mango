import vue from '@vitejs/plugin-vue';
import { defineConfig, loadEnv, type ConfigEnv } from 'vite';
import { resolve } from 'path';
import { mangoMicroManualChunks } from '../../build-config/microChunks';

const ALLOWED_PROXY_HOSTS = ['127.0.0.1', 'localhost'];
const DEV_ALLOWED_HOSTS = ['localhost', '127.0.0.1', 'a.mango.io', 'b.mango.io', 'c.mango.io', 'd.mango.io'];

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
      alias: [
        { find: '@', replacement: resolve(__dirname, './src') },
        { find: '@mango/admin/style.css', replacement: resolve(__dirname, '../../packages/admin/style.css') },
        { find: '@mango/admin-shell/style.css', replacement: resolve(__dirname, '../../packages/admin-shell/style.css') },
        { find: '@mango/auth/style.css', replacement: resolve(__dirname, '../../packages/auth/dist/style.css') },
        { find: /^@mango\/common\/(.*)$/, replacement: `${resolve(__dirname, '../../packages/common')}/$1` },
        { find: '@mango/common', replacement: resolve(__dirname, '../../packages/common/index.ts') },
        { find: '@mango/admin-pages/core', replacement: resolve(__dirname, '../../packages/admin-pages/src/core.ts') },
        { find: '@mango/admin-pages/defaults', replacement: resolve(__dirname, '../../packages/admin-pages/src/defaults.ts') },
        { find: '@mango/admin-pages/dev-component-pages', replacement: resolve(__dirname, '../../packages/admin-pages/src/devComponentPages.ts') },
        { find: '@mango/admin-pages', replacement: resolve(__dirname, '../../packages/admin-pages/src/index.ts') },
        { find: '@mango/app-runtime', replacement: resolve(__dirname, '../../packages/app-runtime/src/index.ts') },
        { find: '@mango/auth', replacement: resolve(__dirname, '../../packages/auth/src/index.ts') },
        { find: '@mango/rbac', replacement: resolve(__dirname, '../../packages/rbac/src/index.ts') },
        { find: '@mango/system', replacement: resolve(__dirname, '../../packages/system/src/index.ts') },
        { find: '@mango/workflow', replacement: resolve(__dirname, '../../packages/workflow/src/index.ts') },
        { find: '@mango/workflow-business-example', replacement: resolve(__dirname, '../../packages/workflow-business-example/src/index.ts') },
        { find: '@mango/template', replacement: resolve(__dirname, '../../packages/template/src/index.ts') },
        { find: '@mango/file', replacement: resolve(__dirname, '../../packages/file/src/index.ts') },
        { find: '@mango/calendar', replacement: resolve(__dirname, '../../packages/calendar/src/index.ts') },
        { find: '@mango/numgen', replacement: resolve(__dirname, '../../packages/numgen/src/index.ts') },
      ],
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
