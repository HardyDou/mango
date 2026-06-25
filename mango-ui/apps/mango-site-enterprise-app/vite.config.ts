import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';
import { readFileSync } from 'node:fs';
import { defineConfig, loadEnv, type ConfigEnv } from 'vite';
import { createMangoWorkspaceAliases } from '../../build-config/mangoAliases';

export default defineConfig((mode: ConfigEnv) => {
  const env = loadEnv(mode.mode, process.cwd());
  const workspaceEnv = readWorkspaceEnv();
  const backendPort = process.env.MANGO_BACKEND_PORT || workspaceEnv.MANGO_BACKEND_PORT || '18081';
  const proxyTarget = env.VITE_ADMIN_PROXY_PATH || `http://127.0.0.1:${backendPort}`;
  return {
    plugins: [vue()],
    resolve: {
      alias: createMangoWorkspaceAliases({
        appDir: __dirname,
        appSrcAlias: resolve(__dirname, './src'),
      }),
    },
    server: {
      host: env.VITE_HOST || '0.0.0.0',
      port: Number(env.VITE_PORT || 5191),
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
          rewrite: path => path.replace(/^\/api/, ''),
        },
      },
    },
  };
});

function readWorkspaceEnv() {
  try {
    return Object.fromEntries(
      readFileSync(resolve(__dirname, '../../../.mango/dev-workspace.env'), 'utf8')
        .split(/\r?\n/)
        .map(line => line.trim())
        .filter(line => line && !line.startsWith('#') && line.includes('='))
        .map(line => {
          const index = line.indexOf('=');
          return [line.slice(0, index), line.slice(index + 1).replace(/^['"]|['"]$/g, '')];
        }),
    ) as Record<string, string>;
  } catch {
    return {};
  }
}
