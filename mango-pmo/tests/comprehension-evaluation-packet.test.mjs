import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const tool = path.join(root, 'tools/create-comprehension-evaluation-packet.mjs');

test('evaluation packet is blind by default and contains all eight cases', () => {
  const result = spawnSync(process.execPath, [tool], { encoding: 'utf8' });
  assert.equal(result.status, 0, result.stderr);
  assert.equal((result.stdout.match(/^## COG-/gmu) ?? []).length, 8);
  assert.match(result.stdout, /真实参与者/);
  assert.match(result.stdout, /推荐表达形式/);
  assert.doesNotMatch(result.stdout, /RUNTIME_EVENTUAL runs in a runtime background worker/);
});

test('evaluation packet can expose the rubric for post-answer scoring', () => {
  const result = spawnSync(process.execPath, [tool, '--with-rubric'], { encoding: 'utf8' });
  assert.equal(result.status, 0, result.stderr);
  assert.match(result.stdout, /RUNTIME_EVENTUAL runs in a runtime background worker/);
});
