import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const tool = path.join(root, 'tools/render-implementation-plan-graph.mjs');
const fixture = path.join(root, 'tests/document-contract/fixtures/valid/implementation-plan.md');

test('implementation plan graph renders a Mermaid task graph', () => {
  const result = spawnSync(process.execPath, [tool, '--document', fixture, '--format', 'mermaid'], { encoding: 'utf8' });
  assert.equal(result.status, 0, result.stderr);
  assert.match(result.stdout, /^flowchart LR/m);
  assert.match(result.stdout, /TASK-001/);
});

test('implementation plan graph exposes task metadata as JSON', () => {
  const result = spawnSync(process.execPath, [tool, '--document', fixture, '--format', 'json'], { encoding: 'utf8' });
  assert.equal(result.status, 0, result.stderr);
  const graph = JSON.parse(result.stdout);
  assert.ok(Array.isArray(graph.tasks));
  assert.ok(graph.tasks.length > 0);
  assert.equal(graph.tasks[0].id, 'TASK-001');
});
