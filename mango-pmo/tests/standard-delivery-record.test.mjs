import assert from 'node:assert/strict';
import fs from 'node:fs';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

import { validateStandardDeliveryRecord } from '../tools/check-standard-delivery-record.mjs';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const template = fs.readFileSync(path.join(root, 'templates/standard-delivery-record.md'), 'utf8');
const valid = template
  .replace('- 任务 ID：', '- 任务 ID：TASK-17')
  .replace('- 需求影响：L2 -', '- 需求影响：L2 - API failure semantics change')
  .replace('- 方案风险：L2 -', '- 方案风险：L2 - shared persistence update with reversible migration')
  .replace('- 工作区决策：CREATE / REUSE / MAIN_EXCEPTION', '- 工作区决策：CREATE');

test('complete STANDARD record passes', () => {
  assert.deepEqual(validateStandardDeliveryRecord(valid).failures, []);
});

test('missing observable requirements and evidence mapping fail', () => {
  const invalid = valid
    .replace('## 3. 可观察系统要求', '## removed')
    .replace('| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |', '| removed |');
  const failures = validateStandardDeliveryRecord(invalid).failures.join('\n');
  assert.match(failures, /可观察系统要求/);
  assert.match(failures, /acceptance mapping/);
});
