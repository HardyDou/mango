import assert from 'node:assert/strict';
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import { compareProjectionTrees } from '../scripts/check-business-module-template.mjs';

const testRoot = dirname(fileURLToPath(import.meta.url));
const cliRoot = resolve(testRoot, '..');
const repoRoot = resolve(cliRoot, '../../..');
const canonicalRoot = join(repoRoot, 'mango-business-starter/backend/modules/{{moduleKebab}}');
const projectionRoot = join(cliRoot, 'templates/business-module/backend/modules/{{moduleKebab}}');

test('CLI business module backend is an exact canonical projection', () => {
  const comparison = compareProjectionTrees(canonicalRoot, projectionRoot);
  assert.equal(comparison.equal, true, JSON.stringify(comparison, null, 2));
  assert.equal(comparison.fileCount > 0, true);
  assert.deepEqual(comparison.missing, []);
  assert.deepEqual(comparison.extra, []);
  assert.deepEqual(comparison.changed, []);
});

test('projection comparison reports changed, missing, and extra files', () => {
  const tempRoot = mkdtempSync(join(tmpdir(), 'mango-business-module-projection-'));
  const sourceRoot = join(tempRoot, 'source');
  const targetRoot = join(tempRoot, 'target');
  try {
    mkdirSync(join(sourceRoot, 'nested'), { recursive: true });
    mkdirSync(join(targetRoot, 'nested'), { recursive: true });
    writeFileSync(join(sourceRoot, 'same.txt'), 'same\n');
    writeFileSync(join(targetRoot, 'same.txt'), 'same\n');
    writeFileSync(join(sourceRoot, 'changed.txt'), 'canonical\n');
    writeFileSync(join(targetRoot, 'changed.txt'), 'stale\n');
    writeFileSync(join(sourceRoot, 'nested/missing.txt'), 'required\n');
    writeFileSync(join(targetRoot, 'extra.txt'), 'unexpected\n');

    const comparison = compareProjectionTrees(sourceRoot, targetRoot);
    assert.equal(comparison.equal, false);
    assert.deepEqual(comparison.changed, ['changed.txt']);
    assert.deepEqual(comparison.missing, ['nested/missing.txt']);
    assert.deepEqual(comparison.extra, ['extra.txt']);
    assert.notEqual(comparison.sourceSha256, comparison.projectionSha256);
  } finally {
    rmSync(tempRoot, { recursive: true, force: true });
  }
});
