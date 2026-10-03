import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const tool = path.join(root, 'tools/render-rule-action-card.mjs');

test('rule action card resolves a canonical rule by index id', () => {
  const result = spawnSync(process.execPath, [tool, '--id', 'product.documentLifecycle'], { encoding: 'utf8' });
  assert.equal(result.status, 0, result.stderr);
  assert.match(result.stdout, /执行卡片/);
  assert.match(result.stdout, /规范源：.*rules\/product\/05-document-lifecycle\.md/u);
  assert.match(result.stdout, /先判断/);
  assert.match(result.stdout, /证据/);
});

test('rule action card supports an explicit canonical rule path', () => {
  const result = spawnSync(process.execPath, [tool, '--rule', 'mango-pmo/rules/11-delivery-assurance.md'], { encoding: 'utf8' });
  assert.equal(result.status, 0, result.stderr);
  assert.match(result.stdout, /交付模式/);
  assert.match(result.stdout, /停止或升级/);
});
