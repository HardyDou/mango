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
  'vue-i18n': 'vue-i18n/dist/vue-i18n.cjs.js',
};

const viteConfig = defineConfig((mode: ConfigEnv) => {
  const env = loadEnv(mode.mode, process.cwd());
  const isDev = mode.command === 'serve';
  const enableCompression = env.VITE_ENABLE_COMPRESSION === 'true';
  const proxyTarget = validateProxyTarget(env.VITE_ADMIN_PROXY_PATH || 'http://127.0.0.1:18081');

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
