import { spawn } from 'node:child_process';

const shellOrigin = process.env.MANGO_MICRO_PREVIEW_SHELL_ORIGIN || 'http://a.mango.io:4176';

const apps = [
  ['mango-admin-shell', shellOrigin],
  ['mango-admin-rbac-app', 'http://b.mango.io:4181'],
  ['mango-admin-workflow-app', 'http://c.mango.io:4182'],
  ['mango-admin-template-app', 'http://d.mango.io:4183'],
  ['mango-admin-cms-app', 'http://e.mango.io:4184'],
];

const children = apps.map(([name]) => {
  const child = spawn('pnpm', ['--filter', name, 'preview'], {
    cwd: new URL('..', import.meta.url),
    env: {
      ...process.env,
      FORCE_COLOR: process.env.FORCE_COLOR || '1',
      VITE_MANGO_ALLOWED_ORIGINS: process.env.VITE_MANGO_ALLOWED_ORIGINS || shellOrigin,
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

console.log('\nMango micro frontend preview servers:');
for (const [name, url] of apps) {
  console.log(`- ${name}: ${url}`);
}
console.log('\nRequired hosts: a.mango.io b.mango.io c.mango.io d.mango.io e.mango.io -> 127.0.0.1\n');

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
