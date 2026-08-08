import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

import { validateStandardDeliveryRecord } from '../tools/check-standard-delivery-record.mjs';

const pmoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const lifecycle = JSON.parse(fs.readFileSync(path.join(pmoRoot, 'contracts/document-lifecycle.json'), 'utf8'));
const template = fs.readFileSync(path.join(pmoRoot, 'templates/standard-delivery-record.md'), 'utf8');
const historicalRecord = template
  .replace('- 任务 ID：', '- 任务 ID：TASK-17')
  .replace('- 需求影响：L2 -', '- 需求影响：L2 - API failure semantics change')
  .replace('- 方案风险：L2 -', '- 方案风险：L2 - shared persistence update with reversible migration')
  .replace('- 工作区决策：CREATE / REUSE / MAIN_EXCEPTION', '- 工作区决策：CREATE');

test('历史 STANDARD 记录仍可由旧检查器读取', () => {
  assert.deepEqual(validateStandardDeliveryRecord(historicalRecord).failures, []);
  assert.equal(lifecycle.stagesAreLegacy, true);
});

test('当前 L2 路由到精简一页模板而不是历史 STANDARD 模板', () => {
  assert.deepEqual(lifecycle.currentArtifacts.L2, ['mango-pmo/templates/delivery-l2.md']);
  assert.equal(lifecycle.currentArtifacts.L2.includes('mango-pmo/templates/standard-delivery-record.md'), false);
});
