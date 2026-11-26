import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { existsSync, mkdtempSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import net from 'node:net';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const cliEntry = join(dirname(fileURLToPath(import.meta.url)), '..', 'src', 'index.mjs');
const skip = process.platform === 'win32' ? 'needs a POSIX shell and lsof' : false;

function freePort() {
  return new Promise((resolvePromise) => {
    const server = net.createServer();
    server.listen(0, '127.0.0.1', () => {
      const { port } = server.address();
      server.close(() => resolvePromise(port));
    });
  });
}

function listenOn(port) {
  return new Promise((resolvePromise, reject) => {
    const server = net.createServer();
    server.once('error', reject);
    server.listen(port, '127.0.0.1', () => resolvePromise(server));
  });
}

function runCli(cwd, home, args) {
  return spawnSync(process.execPath, [cliEntry, 'dev', ...args], {
    cwd,
    encoding: 'utf8',
    env: { ...process.env, HOME: home, USERPROFILE: home },
    timeout: 120_000,
  });
}

test('dev start <group> skips a dependency that already serves its port', { skip }, async () => {
  const root = mkdtempSync(join(tmpdir(), 'mango-dev-port-skip-'));
  const home = join(root, 'home');
  mkdirSync(home, { recursive: true });
  const backendPort = await freePort();
  const frontendPort = await freePort();
  writeFileSync(
    join(root, 'mango.dev.json'),
    `${JSON.stringify(
      {
        version: 1,
        apps: {
          backend: {
            type: 'command',
            cwd: '.',
            command: process.execPath,
            args: ['-e', 'setInterval(() => {}, 1000)'],
            port: backendPort,
          },
          frontend: {
            type: 'command',
            cwd: '.',
            command: process.execPath,
            args: ['-e', 'setInterval(() => {}, 1000)'],
            port: frontendPort,
            dependsOn: ['backend'],
          },
        },
        groups: { default: ['backend', 'frontend'], frontend: ['frontend'] },
      },
      null,
      2,
    )}\n`,
  );
  const occupant = await listenOn(backendPort);
  try {
    const started = runCli(root, home, ['start', 'frontend']);
    assert.equal(started.status, 0, `${started.stdout}\n${started.stderr}`);
    assert.match(started.stdout, new RegExp(`backend: skipped, port ${backendPort} is already served`, 'u'));
    assert.match(started.stdout, /treated as already running/u);
    assert.match(started.stdout, /frontend: started/u);
    assert.ok(existsSync(join(root, '.mango', 'run', 'pids', 'frontend.json')));
    assert.equal(existsSync(join(root, '.mango', 'run', 'pids', 'backend.json')), false);

    const explicit = runCli(root, home, ['start', 'backend']);
    assert.notEqual(explicit.status, 0);
    assert.match(explicit.stdout + explicit.stderr, new RegExp(`backend port ${backendPort} is already in use`, 'u'));
  } finally {
    runCli(root, home, ['stop']);
    await new Promise((resolvePromise) => occupant.close(resolvePromise));
    rmSync(root, { recursive: true, force: true });
  }
});
