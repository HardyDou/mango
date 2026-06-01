import { spawn } from 'node:child_process';
import { readFileSync } from 'node:fs';

const apps = [
  ['mango-admin-shell', 'http://a.mango.io:5176'],
  ['mango-admin-rbac-app', 'http://b.mango.io:5181'],
  ['mango-admin-workflow-app', 'http://c.mango.io:5182'],
  ['mango-admin-template-app', 'http://d.mango.io:5183'],
];

const microAllowedOrigins = apps.map(([, url]) => new URL(url).origin).join(',');
const workspaceEnv = readWorkspaceEnv();
const backendPort = process.env.MANGO_BACKEND_PORT || workspaceEnv.MANGO_BACKEND_PORT || '18081';
const adminProxyPath = process.env.VITE_ADMIN_PROXY_PATH || `http://127.0.0.1:${backendPort}`;

const children = apps.map(([name]) => {
  const child = spawn('pnpm', ['--filter', name, 'dev'], {
    cwd: new URL('..', import.meta.url),
    env: {
      ...process.env,
      VITE_ADMIN_PROXY_PATH: adminProxyPath,
      VITE_MANGO_DEPLOY_ENV: process.env.VITE_MANGO_DEPLOY_ENV || 'dev',
      VITE_MANGO_ALLOWED_REMOTE_ORIGINS: process.env.VITE_MANGO_ALLOWED_REMOTE_ORIGINS || microAllowedOrigins,
      FORCE_COLOR: process.env.FORCE_COLOR || '1',
    },
    stdio: 'inherit',
  });
  child.on('exit', (code, signal) => {
    if (signal || code) {
      shutdown(signal || code || 1);
    }
  });
  return child;
});

console.log('\nMango micro frontend dev servers:');
for (const [name, url] of apps) {
  console.log(`- ${name}: ${url}`);
}
console.log(`\nProxy target: ${adminProxyPath}`);
console.log('\nRequired hosts: a.mango.io b.mango.io c.mango.io d.mango.io -> 127.0.0.1\n');

function readWorkspaceEnv() {
  const envUrl = new URL('../../.mango/dev-workspace.env', import.meta.url);
  try {
    return Object.fromEntries(
      readFileSync(envUrl, 'utf8')
        .split(/\r?\n/)
        .map((line) => line.trim())
        .filter((line) => line && !line.startsWith('#') && line.includes('='))
        .map((line) => {
          const index = line.indexOf('=');
          const key = line.slice(0, index);
          const value = line.slice(index + 1).replace(/^['"]|['"]$/g, '');
          return [key, value];
        }),
    );
  } catch {
    return {};
  }
}

function shutdown(reason) {
  for (const child of children) {
    if (!child.killed) {
      child.kill(typeof reason === 'string' ? reason : 'SIGTERM');
    }
  }
  if (typeof reason === 'number') {
    process.exit(reason);
  }
}

process.on('SIGINT', () => shutdown('SIGINT'));
process.on('SIGTERM', () => shutdown('SIGTERM'));
