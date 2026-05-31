import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';
import { defineConfig, loadEnv, ConfigEnv } from 'vite';
import vueSetupExtend from 'vite-plugin-vue-setup-extend';
import AutoImport from 'unplugin-auto-import/vite';
import topLevelAwait from 'vite-plugin-top-level-await';
import { createStyleImportPlugin, VxeTableResolve } from 'vite-plugin-style-import';
import viteCompression from 'vite-plugin-compression';

const ALLOWED_PROXY_HOSTS = ['127.0.0.1', 'localhost'];

function validateProxyTarget(target: string): string {
  try {
    const url = new URL(target);
    if (!ALLOWED_PROXY_HOSTS.includes(url.hostname)) {
      throw new Error(
        `Proxy target hostname '${url.hostname}' not allowed. Allowed: ${ALLOWED_PROXY_HOSTS.join(', ')}`
      );
    }
    return target;
  } catch {
    throw new Error(`Invalid VITE_ADMIN_PROXY_PATH: ${target}`);
  }
}

const pathResolve = (dir: string) => {
  return resolve(__dirname, '.', dir);
};

const createManualChunks = (id: string): string | undefined => {
  if (!id.includes('node_modules')) {
    return undefined;
  }

  if (id.includes('/vue/') || id.includes('/vue-router/') || id.includes('/pinia/')) {
    return 'vue';
  }

  if (id.includes('/element-plus/') || id.includes('/@element-plus/')) {
    return 'element-plus';
  }

  if (id.includes('/echarts/')) {
    return 'echarts';
  }

  if (id.includes('/codemirror/')) {
    return 'codemirror';
  }

  if (id.includes('/@wangeditor/')) {
    return 'wangeditor';
  }

  if (id.includes('/xlsx/')) {
    return 'xlsx';
  }

  if (id.includes('/crypto-js/') || id.includes('/sm-crypto/')) {
    return 'crypto';
  }

  if (id.includes('/axios/') || id.includes('/qs/') || id.includes('/lodash/') || id.includes('/mitt/')) {
    return 'utils';
  }

  return undefined;
};

const alias: Record<string, string> = {
  '@': pathResolve('./src/'),
  '@mango/admin/style.css': resolve(__dirname, '../../packages/admin/style.css'),
  '@mango/admin-shell/style.css': resolve(__dirname, '../../packages/admin-shell/style.css'),
  '@mango/common/style.css': resolve(__dirname, '../../packages/common/dist/style.css'),
  '@mango/common/theme/index.css': resolve(__dirname, '../../packages/common/theme/index.css'),
  '@mango/auth/style.css': resolve(__dirname, '../../packages/auth/dist/style.css'),
  '@mango/rbac/style.css': resolve(__dirname, '../../packages/rbac/dist/style.css'),
  '@mango/system/style.css': resolve(__dirname, '../../packages/system/dist/style.css'),
  '@mango/workflow/style.css': resolve(__dirname, '../../packages/workflow/dist/style.css'),
  '@mango/workflow-business-example/style.css': resolve(__dirname, '../../packages/workflow-business-example/dist/style.css'),
  '@mango/file/style.css': resolve(__dirname, '../../packages/file/dist/style.css'),
  '@mango/calendar/style.css': resolve(__dirname, '../../packages/calendar/dist/style.css'),
  '@mango/numgen/style.css': resolve(__dirname, '../../packages/numgen/dist/style.css'),
  '@mango/template/style.css': resolve(__dirname, '../../packages/template/dist/style.css'),
  '@mango/notice/style.css': resolve(__dirname, '../../packages/notice/dist/style.css'),
  '@mango/admin-pages/core': resolve(__dirname, '../../packages/admin-pages/src/core.ts'),
  '@mango/admin-pages/defaults': resolve(__dirname, '../../packages/admin-pages/src/defaults.ts'),
  '@mango/admin-pages/dev-component-pages': resolve(__dirname, '../../packages/admin-pages/src/devComponentPages.ts'),
  '@mango/admin-pages': resolve(__dirname, '../../packages/admin-pages/src/index.ts'),
  '@mango/admin-shell/dev-pages': resolve(__dirname, '../../packages/admin-shell/src/views/demo/registerDevPages.ts'),
  '@mango/file': resolve(__dirname, '../../packages/file/src/index.ts'),
  '@mango/workflow': resolve(__dirname, '../../packages/workflow/src/index.ts'),
  '@mango/notice/admin': resolve(__dirname, '../../packages/notice/src/admin.ts'),
  '@mango/notice/client': resolve(__dirname, '../../packages/notice/src/client.ts'),
  '@mango/notice/realtime': resolve(__dirname, '../../packages/notice/src/realtime.ts'),
  'vue-i18n': 'vue-i18n/dist/vue-i18n.cjs.js',
};

const viteConfig = defineConfig((mode: ConfigEnv) => {
  const env = loadEnv(mode.mode, process.cwd());
  const isDev = mode.command === 'serve';
  const enableCompression = env.VITE_ENABLE_COMPRESSION === 'true';
  const proxyTarget = validateProxyTarget(env.VITE_ADMIN_PROXY_PATH || 'http://127.0.0.1:5555');

  return {
    plugins: [
      vue(),
      // vueSetupExtend(), // Temporarily disabled due to TypeScript type import issue
      AutoImport({
        imports: ['vue', 'vue-router', 'pinia'],
        dts: isDev ? './auto-imports.d.ts' : false,
      }),
      createStyleImportPlugin({
        resolves: [VxeTableResolve()],
      }),
      topLevelAwait({
        promiseExportName: '__tla',
        promiseImportName: (i) => `__tla_${i}`,
      }),
      ...(enableCompression ? [viteCompression({
        deleteOriginFile: false,
      })] : []),
    ],
    root: process.cwd(),
    resolve: { alias },
    base: isDev ? './' : (env.VITE_PUBLIC_PATH || '/'),
    optimizeDeps: {
      entries: ['index.html', 'src/main.ts'],
      include: ['element-plus/es/locale/lang/zh-cn', 'element-plus/es/locale/lang/en'],
    },
    server: {
      host: env.VITE_HOST || '127.0.0.1',
      port: (env.VITE_PORT as unknown as number) || 5173,
      open: env.VITE_OPEN === 'true',
      // SECURITY: Restrict allowed hosts in production
      allowedHosts: isDev ? ['localhost', '127.0.0.1'] : false,
      hmr: true,
      proxy: {
        // Mock 模式下跳过 /api/* 路径，由 MSW 处理
        ...(env.VITE_USE_MOCK !== 'true' ? {
          '/api': {
            target: proxyTarget,
            ws: true,
            changeOrigin: true,
            rewrite: (path) => path.replace(/^\/api/, ''),
          },
          '/mango-message': {
            target: proxyTarget,
            ws: true,
            changeOrigin: true,
            rewrite: (path) => path.replace(/^\/mango-message/, ''),
          },
          '/ai': {
            target: proxyTarget,
            ws: true,
            changeOrigin: true,
          },
        } : {}),
      },
    },
    build: {
      outDir: 'dist',
      chunkSizeWarningLimit: 1500,
      minify: isDev ? 'esbuild' : 'terser',
      terserOptions: {
        compress: {
          drop_console: true,
          drop_debugger: true,
        },
        format: {
          comments: false,
        },
      },
      rollupOptions: {
        output: {
          entryFileNames: `assets/[name].[hash].js`,
          chunkFileNames: `assets/[name].[hash].js`,
          assetFileNames: `assets/[name].[hash].[ext]`,
          compact: true,
          manualChunks: createManualChunks,
        },
      },
    },
    css: {
      preprocessorOptions: {
        css: { charset: false },
      },
    },
    define: {
      __VUE_I18N_LEGACY_API__: JSON.stringify(false),
      __VUE_I18N_FULL_INSTALL__: JSON.stringify(false),
      __INTLIFY_PROD_DEVTOOLS__: JSON.stringify(false),
      __VERSION__: JSON.stringify(process.env.npm_package_version),
      __NEXT_NAME__: JSON.stringify(process.env.npm_package_name),
    },
  };
});

export default viteConfig;
