import vue from '@vitejs/plugin-vue';
import { defineConfig, loadEnv, type ConfigEnv } from 'vite';
import { resolve } from 'path';
import { createAllowedOrigins, createPreviewCorsHeaders } from './vite.cors';
import { mangoMicroManualChunks } from '../../build-config/microChunks';
import {
  assertMangoPackageModeDist,
  createMangoWorkspaceAliases,
} from '../../build-config/mangoAliases';

export default defineConfig((mode: ConfigEnv) => {
  assertMangoPackageModeDist(__dirname, { command: mode.command });
  const env = loadEnv(mode.mode, process.cwd());
  const proxyTarget = env.VITE_ADMIN_PROXY_PATH || 'http://127.0.0.1:18081';
  const allowedHosts = ['localhost', '127.0.0.1', 'a.mango.io', 'b.mango.io', 'c.mango.io'];
  const allowedOrigins = createAllowedOrigins(env.VITE_MANGO_ALLOWED_ORIGINS);

  return {
    base: env.VITE_PUBLIC_PATH || '/',
    plugins: [vue()],
    define: {
      'process.env.NODE_ENV': JSON.stringify(mode.mode === 'production' ? 'production' : 'development'),
      'process.env': {},
    },
    resolve: {
      alias: createMangoWorkspaceAliases({
        appDir: __dirname,
        appSrcAlias: resolve(__dirname, './src'),
      }),
    },
    server: {
      host: env.VITE_HOST || '0.0.0.0',
      port: Number(env.VITE_PORT || 5181),
      allowedHosts,
      cors: {
        origin: allowedOrigins,
        credentials: true,
      },
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, ''),
        },
      },
    },
    preview: {
      host: env.VITE_HOST || '0.0.0.0',
      port: Number(env.VITE_PREVIEW_PORT || 4181),
      allowedHosts,
      headers: createPreviewCorsHeaders(allowedOrigins),
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
