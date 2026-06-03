import vue from '@vitejs/plugin-vue';
import { defineConfig, loadEnv, type ConfigEnv } from 'vite';

const ALLOWED_PROXY_HOSTS = ['127.0.0.1', 'localhost'];
const DEV_ALLOWED_HOSTS = ['localhost', '127.0.0.1', 'a.mango.io', 'b.mango.io', 'c.mango.io', 'd.mango.io'];

function mangoManualChunks(id: string): string | undefined {
  const normalized = id.replace(/\\/g, '/');
  if (!normalized.includes('/node_modules/')) {
    return undefined;
  }
  if (normalized.includes('/node_modules/@mango/workflow')) {
    return 'mango-workflow';
  }
  if (normalized.includes('/node_modules/@mango/file')) {
    return 'mango-file';
  }
  if (normalized.includes('/node_modules/@mango/template')) {
    return 'mango-template';
  }
  if (normalized.includes('/node_modules/@mango/notice')) {
    return 'mango-notice';
  }
  if (normalized.includes('/node_modules/@mango/calendar')) {
    return 'mango-calendar';
  }
  if (normalized.includes('/node_modules/@mango/numgen')) {
    return 'mango-numgen';
  }
  if (normalized.includes('/node_modules/@mango/')) {
    return 'mango-platform';
  }
  if (normalized.includes('/node_modules/element-plus') || normalized.includes('/node_modules/@element-plus/')) {
    return 'element-plus';
  }
  if (normalized.includes('/node_modules/form-create-designer')) {
    return 'form-create-designer';
  }
  if (normalized.includes('/node_modules/@form-create/') || normalized.includes('/node_modules/form-create')) {
    return 'form-create-runtime';
  }
  if (normalized.includes('/node_modules/zrender')) {
    return 'charts-renderer';
  }
  if (normalized.includes('/node_modules/echarts/lib/chart')) {
    return 'charts-series';
  }
  if (normalized.includes('/node_modules/echarts/lib/component')) {
    return 'charts-components';
  }
  if (normalized.includes('/node_modules/echarts')) {
    return 'charts-core';
  }
  if (normalized.includes('/node_modules/@wangeditor/') || normalized.includes('/node_modules/wangeditor')) {
    return 'richtext-vendor';
  }
  if (
    normalized.includes('/node_modules/xlsx')
    || normalized.includes('/node_modules/codepage')
    || normalized.includes('/node_modules/cfb')
    || normalized.includes('/node_modules/ssf')
  ) {
    return 'spreadsheet-vendor';
  }
  if (normalized.includes('/node_modules/codemirror') || normalized.includes('/node_modules/@codemirror/')) {
    return 'editor-vendor';
  }
  if (normalized.includes('/node_modules/@vueuse/')) {
    return 'vueuse-vendor';
  }
  if (normalized.includes('/node_modules/lodash') || normalized.includes('/node_modules/lodash-es')) {
    return 'utility-vendor';
  }
  if (normalized.includes('/node_modules/axios')) {
    return 'http-vendor';
  }
  if (normalized.includes('/node_modules/dayjs')) {
    return 'date-vendor';
  }
  if (
    normalized.includes('/node_modules/vue')
    || normalized.includes('/node_modules/@vue/')
    || normalized.includes('/node_modules/vue-router')
    || normalized.includes('/node_modules/vue-i18n')
    || normalized.includes('/node_modules/pinia')
  ) {
    return 'vue-vendor';
  }
  return 'vendor';
}

function validateProxyTarget(target: string): string {
  const url = new URL(target);
  if (!ALLOWED_PROXY_HOSTS.includes(url.hostname)) {
    throw new Error(`Proxy target hostname '${url.hostname}' not allowed`);
  }
  return target;
}

export default defineConfig((mode: ConfigEnv) => {
  const env = loadEnv(mode.mode, process.cwd());
  const proxyTarget = validateProxyTarget(env.VITE_ADMIN_PROXY_PATH || 'http://127.0.0.1:5555');

  return {
    base: env.VITE_PUBLIC_PATH || '/',
    plugins: [vue()],
    define: {
      __VUE_I18N_FULL_INSTALL__: true,
      __VUE_I18N_LEGACY_API__: false,
      __INTLIFY_PROD_DEVTOOLS__: false,
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
      chunkSizeWarningLimit: 1200,
      rollupOptions: {
        output: {
          manualChunks: mangoManualChunks,
        },
      },
    },
  };
});
