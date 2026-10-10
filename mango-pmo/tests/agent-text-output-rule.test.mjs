import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const index = JSON.parse(readFileSync(path.join(root, 'rules/index.json'), 'utf8'));
const textRule = readFileSync(path.join(root, 'rules/13-agent-text-output.md'), 'utf8');
const decisionRule = readFileSync(path.join(root, 'rules/14-decision-expert-review.md'), 'utf8');

test('text output and decision review rules are canonical always-loaded sources', () => {
  assert.equal(index.version, 20);
  assert.deepEqual(index.always.map(entry => entry.path).slice(-2), [
    'rules/13-agent-text-output.md',
    'rules/14-decision-expert-review.md',
  ]);
  for (const phrase of ['结论、影响或下一步', '事实', '成功条件', '失败条件', '代码注释']) {
    assert.match(textRule, new RegExp(phrase));
  }
  for (const phrase of ['三视角', '事实与用户视角', '技术与风险视角', '验证与交付视角', '评审选择']) {
    assert.match(decisionRule, new RegExp(phrase));
  }
});
