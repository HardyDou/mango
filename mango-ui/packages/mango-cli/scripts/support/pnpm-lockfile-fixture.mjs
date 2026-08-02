import { chmodSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { delimiter, join } from 'node:path';

export function createPnpmLockfileFixture(root, importers) {
  const binDir = join(root, 'pnpm-lockfile-fixture-bin');
  const runnerPath = join(binDir, 'pnpm-lockfile-fixture.mjs');
  const logPath = join(binDir, 'invocations.jsonl');
  const normalizedImporters = [...new Set(importers)].sort();
  mkdirSync(binDir, { recursive: true });
  writeFileSync(
    runnerPath,
    `#!/usr/bin/env node
import { appendFileSync, existsSync, readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

const args = process.argv.slice(2);
const importers = ${JSON.stringify(normalizedImporters)};
const logPath = ${JSON.stringify(logPath)};
const lockfilePath = join(process.cwd(), 'pnpm-lock.yaml');
appendFileSync(logPath, JSON.stringify({ args, cwd: process.cwd() }) + '\\n');
if (args[0] !== 'install' || !args.includes('--lockfile-only')) {
  process.stderr.write('pnpm lockfile fixture only accepts install --lockfile-only\\n');
  process.exit(2);
}
if (args.includes('--frozen-lockfile')) {
  const lockfile = existsSync(lockfilePath) ? readFileSync(lockfilePath, 'utf8') : '';
  if (importers.some((name) => !lockfile.includes('  ' + name + ':'))) {
    process.stderr.write('pnpm lockfile fixture frozen verification found missing importer\\n');
    process.exit(1);
  }
  process.exit(0);
}
const importerBlock = importers.map((name) => '  ' + name + ': {}').join('\\n');
writeFileSync(
  lockfilePath,
  "lockfileVersion: '9.0'\\n\\nsettings:\\n  autoInstallPeers: true\\n  excludeLinksFromLockfile: false\\n\\nimporters:\\n" + importerBlock + '\\n',
);
`,
  );
  chmodSync(runnerPath, 0o755);
  if (process.platform === 'win32') {
    writeFileSync(join(binDir, 'pnpm.cmd'), `@"${process.execPath}" "%~dp0\\pnpm-lockfile-fixture.mjs" %*\r\n`);
  } else {
    writeFileSync(
      join(binDir, 'pnpm'),
      `#!/bin/sh\nexec ${shellQuote(process.execPath)} ${shellQuote(runnerPath)} "$@"\n`,
    );
    chmodSync(join(binDir, 'pnpm'), 0o755);
  }
  return {
    env: { ...process.env, PATH: `${binDir}${delimiter}${process.env.PATH || ''}` },
    logPath,
  };
}

export function assertPnpmLockfileFixtureInvocations(logPath) {
  const invocations = readFileSync(logPath, 'utf8')
    .trim()
    .split('\n')
    .filter(Boolean)
    .map((line) => JSON.parse(line));
  const expected = [
    ['install', '--lockfile-only', '--ignore-scripts'],
    ['install', '--frozen-lockfile', '--lockfile-only', '--ignore-scripts'],
  ];
  if (JSON.stringify(invocations.map(({ args }) => args)) !== JSON.stringify(expected)) {
    throw new Error(`unexpected pnpm lockfile fixture invocations: ${JSON.stringify(invocations)}`);
  }
}

function shellQuote(value) {
  return `'${String(value).replaceAll("'", "'\\''")}'`;
}
