import vue from '@vitejs/plugin-vue';
import { defineConfig, loadEnv, type ConfigEnv, type Plugin } from 'vite';
import { readFileSync } from 'fs';
import { resolve } from 'path';
import { mangoMicroManualChunks } from '../../build-config/microChunks';
import { assertMangoPackageModeDist, createMangoWorkspaceAliases } from '../../build-config/mangoAliases';
import { createMangoApiProxy } from '../../build-config/apiProxy';

const ALLOWED_PROXY_HOSTS = ['127.0.0.1', 'localhost'];
const DEV_ALLOWED_HOSTS = [
  'localhost',
  '127.0.0.1',
  'a.mango.io',
  'b.mango.io',
  'c.mango.io',
  'd.mango.io',
  'e.mango.io',
];

function runtimeConfigDevPlugin(configFile?: string): Plugin {
  return {
    name: 'mango-runtime-config-dev',
    apply: 'serve',
    configureServer(server) {
      server.middlewares.use((request, response, next) => {
        if (!configFile || request.url?.split('?')[0] !== '/runtime-config.json') {
          next();
          return;
        }
        try {
          const content = readFileSync(resolve(configFile), 'utf8');
          JSON.parse(content);
          response.statusCode = 200;
          response.setHeader('content-type', 'application/json; charset=utf-8');
          response.setHeader('cache-control', 'no-store');
          response.end(content);
        } catch (error) {
          response.statusCode = 500;
          response.setHeader('content-type', 'application/json; charset=utf-8');
          response.end(JSON.stringify({ message: error instanceof Error ? error.message : String(error) }));
        }
      });
    },
  };
}

function validateProxyTarget(target: string): string {
  const url = new URL(target);
  if (!ALLOWED_PROXY_HOSTS.includes(url.hostname)) {
    throw new Error(`Proxy target hostname '${url.hostname}' not allowed`);
  }
  return target;
}

export default defineConfig((mode: ConfigEnv) => {
  assertMangoPackageModeDist(__dirname, { command: mode.command });
  const env = loadEnv(mode.mode, process.cwd());
  const proxyTarget = validateProxyTarget(
    process.env.VITE_ADMIN_PROXY_PATH || env.VITE_ADMIN_PROXY_PATH || 'http://127.0.0.1:18081',
  );

  return {
    base: env.VITE_PUBLIC_PATH || '/',
    plugins: [vue(), runtimeConfigDevPlugin(process.env.VITE_MANGO_RUNTIME_CONFIG_FILE)],
    resolve: {
      alias: createMangoWorkspaceAliases({
        appDir: __dirname,
        appSrcAlias: resolve(__dirname, './src'),
      }),
    },
    server: {
      host: env.VITE_HOST || '0.0.0.0',
      port: Number(env.VITE_PORT || 5176),
      allowedHosts: DEV_ALLOWED_HOSTS,
      proxy: {
        '/api': createMangoApiProxy(proxyTarget),
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
