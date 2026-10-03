import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const tool = path.join(root, 'tools/check-readable-module-readmes.mjs');

test('representative module README pilot has a scenario-first reading entry', () => {
  const result = spawnSync(process.execPath, [tool], { cwd: root, encoding: 'utf8' });
  assert.equal(result.status, 0, result.stderr);
  assert.match(result.stdout, /6 files/);
});

test('readability checker reports an unoptimized existing component README', () => {
  const result = spawnSync(process.execPath, [tool, '--files', 'mango-ui/packages/common/components/MangoDialog/README.md'], {
    cwd: root,
    encoding: 'utf8',
  });
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /missing .* entry/);
});
