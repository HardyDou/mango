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
        { find: '@mango/common/theme/index.css', replacement: resolve(__dirname, '../../packages/common/theme/index.css') },
        { find: '@mango/common/style.css', replacement: resolve(__dirname, '../../packages/common/dist/style.css') },
        { find: '@mango/auth/style.css', replacement: resolve(__dirname, '../../packages/auth/dist/style.css') },
        { find: '@mango/rbac/style.css', replacement: resolve(__dirname, '../../packages/rbac/dist/style.css') },
        { find: '@mango/system/style.css', replacement: resolve(__dirname, '../../packages/system/dist/style.css') },
        { find: '@mango/workflow/style.css', replacement: resolve(__dirname, '../../packages/workflow/dist/style.css') },
        { find: '@mango/workflow-business-example/style.css', replacement: resolve(__dirname, '../../packages/workflow-business-example/dist/style.css') },
        { find: '@mango/file/style.css', replacement: resolve(__dirname, '../../packages/file/dist/style.css') },
        { find: '@mango/calendar/style.css', replacement: resolve(__dirname, '../../packages/calendar/dist/style.css') },
        { find: '@mango/numgen/style.css', replacement: resolve(__dirname, '../../packages/numgen/dist/style.css') },
        { find: '@mango/template/style.css', replacement: resolve(__dirname, '../../packages/template/dist/style.css') },
        { find: '@mango/notice/style.css', replacement: resolve(__dirname, '../../packages/notice/dist/style.css') },
        { find: /^@mango\/common\/(.*)$/, replacement: `${resolve(__dirname, '../../packages/common')}/$1` },
        { find: /^@mango\/common$/, replacement: resolve(__dirname, '../../packages/common/index.ts') },
        { find: '@mango/admin/full', replacement: resolve(__dirname, '../../packages/admin/src/full.ts') },
        { find: /^@mango\/admin$/, replacement: resolve(__dirname, '../../packages/admin/src/index.ts') },
        { find: '@mango/admin-shell/runtime', replacement: resolve(__dirname, '../../packages/admin-shell/src/runtime/runtimeHost.ts') },
        { find: '@mango/admin-shell/menu', replacement: resolve(__dirname, '../../packages/admin-shell/src/runtime/menuHost.ts') },
        { find: '@mango/admin-shell/stores', replacement: resolve(__dirname, '../../packages/admin-shell/src/stores/index.ts') },
        { find: '@mango/admin-shell/router', replacement: resolve(__dirname, '../../packages/admin-shell/src/router.ts') },
        { find: '@mango/admin-shell/home', replacement: resolve(__dirname, '../../packages/admin-shell/src/views/home/index.vue') },
        { find: '@mango/admin-shell/dev-pages', replacement: resolve(__dirname, '../../packages/admin-shell/src/views/demo/registerDevPages.ts') },
        { find: '@mango/admin-shell/dev-base-pages', replacement: resolve(__dirname, '../../packages/admin-shell/src/views/demo/registerBaseDevPages.ts') },
        { find: '@mango/admin-shell/dev-upload-page', replacement: resolve(__dirname, '../../packages/admin-shell/src/views/demo/components/UploadView.vue') },
        { find: '@mango/admin-shell/dev-workflow-page', replacement: resolve(__dirname, '../../packages/admin-shell/src/views/demo/components/WorkflowComponentsView.vue') },
        { find: /^@mango\/admin-shell$/, replacement: resolve(__dirname, '../../packages/admin-shell/src/index.ts') },
        { find: '@mango/admin-pages/core', replacement: resolve(__dirname, '../../packages/admin-pages/src/core.ts') },
        { find: '@mango/admin-pages/defaults', replacement: resolve(__dirname, '../../packages/admin-pages/src/defaults.ts') },
        { find: '@mango/admin-pages/dev-pages', replacement: resolve(__dirname, '../../packages/admin-pages/src/dev-pages.ts') },
        { find: '@mango/admin-pages/dev-component-pages', replacement: resolve(__dirname, '../../packages/admin-pages/src/devComponentPages.ts') },
        { find: '@mango/admin-pages/features', replacement: resolve(__dirname, '../../packages/admin-pages/src/features.ts') },
        { find: '@mango/admin-pages/notice', replacement: resolve(__dirname, '../../packages/admin-pages/src/notice.ts') },
        { find: /^@mango\/admin-pages$/, replacement: resolve(__dirname, '../../packages/admin-pages/src/index.ts') },
        { find: /^@mango\/app-runtime$/, replacement: resolve(__dirname, '../../packages/app-runtime/src/index.ts') },
        { find: /^@mango\/auth$/, replacement: resolve(__dirname, '../../packages/auth/src/index.ts') },
        { find: /^@mango\/rbac$/, replacement: resolve(__dirname, '../../packages/rbac/src/index.ts') },
        { find: /^@mango\/system$/, replacement: resolve(__dirname, '../../packages/system/src/index.ts') },
        { find: '@mango/calendar/admin-pages', replacement: resolve(__dirname, '../../packages/calendar/src/admin-pages.ts') },
        { find: '@mango/file/admin-pages', replacement: resolve(__dirname, '../../packages/file/src/admin-pages.ts') },
        { find: '@mango/notice/admin-pages', replacement: resolve(__dirname, '../../packages/notice/src/admin-pages.ts') },
        { find: '@mango/notice/admin-shell', replacement: resolve(__dirname, '../../packages/notice/src/admin-shell.ts') },
        { find: '@mango/numgen/admin-pages', replacement: resolve(__dirname, '../../packages/numgen/src/admin-pages.ts') },
        { find: '@mango/template/admin-pages', replacement: resolve(__dirname, '../../packages/template/src/admin-pages.ts') },
        { find: '@mango/workflow/admin-pages', replacement: resolve(__dirname, '../../packages/workflow/src/admin-pages.ts') },
        { find: '@mango/workflow-business-example/admin-pages', replacement: resolve(__dirname, '../../packages/workflow-business-example/src/admin-pages.ts') },
        { find: /^@mango\/workflow$/, replacement: resolve(__dirname, '../../packages/workflow/src/index.ts') },
        { find: /^@mango\/workflow-business-example$/, replacement: resolve(__dirname, '../../packages/workflow-business-example/src/index.ts') },
        { find: /^@mango\/template$/, replacement: resolve(__dirname, '../../packages/template/src/index.ts') },
        { find: /^@mango\/file$/, replacement: resolve(__dirname, '../../packages/file/src/index.ts') },
        { find: /^@mango\/calendar$/, replacement: resolve(__dirname, '../../packages/calendar/src/index.ts') },
        { find: /^@mango\/numgen$/, replacement: resolve(__dirname, '../../packages/numgen/src/index.ts') },
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
