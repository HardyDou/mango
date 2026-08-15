import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import {
  discoverTypecheckTargets,
  parseTypeScriptDiagnostics,
  resolveTypecheckCommand,
} from './typecheck-runner-lib.mjs';

test('resolves the platform-specific vue-tsc executable', () => {
  const root = path.resolve('/repo/mango-ui');
  assert.deepEqual(resolveTypecheckCommand(root, 'win32'), {
    executable: path.join(root, 'node_modules', '.bin', 'vue-tsc.cmd'),
    shell: true,
  });
  assert.deepEqual(resolveTypecheckCommand(root, 'linux'), {
    executable: path.join(root, 'node_modules', '.bin', 'vue-tsc'),
    shell: false,
  });
});

test('discovers workspace tsconfig files and reports missing configurations', () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-typecheck-targets-'));
  const ready = path.join(root, 'packages', 'ready');
  const missing = path.join(root, 'apps', 'missing');
  fs.mkdirSync(ready, { recursive: true });
  fs.mkdirSync(missing, { recursive: true });
  fs.writeFileSync(path.join(ready, 'package.json'), JSON.stringify({ name: '@fixture/ready' }));
  fs.writeFileSync(path.join(ready, 'tsconfig.json'), '{}');
  fs.writeFileSync(path.join(missing, 'package.json'), JSON.stringify({ name: '@fixture/missing' }));

  const result = discoverTypecheckTargets(root);
  assert.deepEqual(
    result.targets.map((item) => item.workspace),
    ['@fixture/ready'],
  );
  assert.deepEqual(result.skipped, [
    { workspace: '@fixture/missing', directory: 'apps/missing', reason: 'missing-tsconfig' },
  ]);
});

test('normalizes TypeScript diagnostics into stable identities', () => {
  const root = path.resolve('/repo/mango-ui');
  const diagnostics = parseTypeScriptDiagnostics(
    'packages/example/src/index.ts(4,9): error TS2322: Type string is not assignable.\n',
    root,
    '@fixture/example',
  );
  assert.deepEqual(diagnostics, [
    {
      workspace: '@fixture/example',
      file: 'packages/example/src/index.ts',
      line: 4,
      column: 9,
      severity: 'error',
      code: 'TS2322',
      message: 'Type string is not assignable.',
    },
  ]);
});
