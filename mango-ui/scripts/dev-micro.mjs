import { spawn } from 'node:child_process';

const apps = [
  ['mango-admin-shell', 'http://a.mango.io:5176'],
  ['mango-admin-rbac-app', 'http://b.mango.io:5181'],
  ['mango-admin-workflow-app', 'http://c.mango.io:5182'],
  ['mango-admin-template-app', 'http://d.mango.io:5183'],
];

const children = apps.map(([name]) => {
  const child = spawn('pnpm', ['--filter', name, 'dev'], {
    cwd: new URL('..', import.meta.url),
    env: {
      ...process.env,
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
console.log('\nRequired hosts: a.mango.io b.mango.io c.mango.io d.mango.io -> 127.0.0.1\n');

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
