import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { spawnSync } from 'node:child_process';
import test from 'node:test';

import { commandForPlatform, shouldUseShellForCommand } from '../platform-command.mjs';

const uiRoot = resolve(import.meta.dirname, '../..');

function run(command, args, options = {}) {
  const platformCommand = commandForPlatform(command);
  return spawnSync(platformCommand, args, {
    encoding: 'utf8',
    shell: shouldUseShellForCommand(platformCommand),
    ...options,
  });
}

test('candidate consumer rejects an Admin tarball that violates its release contract before installation', (t) => {
  const root = mkdtempSync(join(tmpdir(), 'mango-admin-candidate-contract-'));
  const packageRoot = join(root, 'package-source');
  const candidateRoot = join(root, 'candidate');
  t.after(() => rmSync(root, { recursive: true, force: true }));
  mkdirSync(join(packageRoot, 'dist'), { recursive: true });
  mkdirSync(candidateRoot, { recursive: true });
  writeFileSync(
    join(packageRoot, 'package.json'),
    `${JSON.stringify(
      {
        name: '@mango/admin',
        version: '0.0.0-contract-test',
        files: ['dist'],
        exports: { './full': { import: './dist/full.js', types: './dist/full.d.ts' } },
      },
      null,
      2,
    )}\n`,
  );
  writeFileSync(join(packageRoot, 'dist/full.js'), 'export const brokenRegistrar = true;\n');
  writeFileSync(join(packageRoot, 'dist/full.d.ts'), 'export declare const brokenRegistrar: boolean;\n');

  const pack = run('npm', ['pack', '--pack-destination', candidateRoot], { cwd: packageRoot });
  assert.equal(pack.status, 0, pack.stderr);

  const check = run(
    process.execPath,
    [
      join(uiRoot, 'scripts/check-package-consumer-typecheck.mjs'),
      '--release-candidate-matrix',
      '--reuse-build',
      `--candidate-dir=${candidateRoot}`,
    ],
    { cwd: uiRoot },
  );

  assert.notEqual(check.status, 0);
  assert.match(check.stderr, /does not contain required text: @mango\/cms\/admin-pages/u);
  assert.doesNotMatch(`${check.stdout}\n${check.stderr}`, /Generating temporary Mango business frontend consumer/u);
});
