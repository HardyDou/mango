import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const index = JSON.parse(readFileSync(path.join(root, 'rules/index.json'), 'utf8'));
const textRule = readFileSync(path.join(root, 'rules/13-agent-text-output.md'), 'utf8');
const decisionRule = readFileSync(path.join(root, 'rules/14-decision-expert-review.md'), 'utf8');
const redlineRule = readFileSync(path.join(root, 'rules/03-ai-coding-redlines.md'), 'utf8');
const REUSE_FIRST_CLAUSES = [
  'REDLINE-REUSE-010',
  'REDLINE-MINIMAL-020',
  'REDLINE-TERMS-030',
  'REDLINE-HIST-040',
  'REDLINE-QUEST-050',
  'REDLINE-OVERDESIGN-060',
];

function definedReuseFirstClauses(source) {
  return [...source.matchAll(/^### (REDLINE-[A-Z]+-\d{3})\b/gmu)].map(match => match[1]);
}

test('rules index version follows the declared reuse-first clause contract', () => {
  // Version 21 is the contract bump that publishes the reuse-first clauses. Adding
  // or removing a clause without bumping the index version or updating this list
  // fails here instead of drifting silently.
  assert.deepEqual(definedReuseFirstClauses(redlineRule), REUSE_FIRST_CLAUSES);
  assert.equal(index.version, 21);
});

test('reuse-first clauses are defined once and advertised by the always-loaded index', () => {
  for (const clause of REUSE_FIRST_CLAUSES) {
    assert.match(redlineRule, new RegExp(`^### ${clause}\\b`, 'mu'));
  }
  const alwaysEntry = index.always.find(entry => entry.path === 'rules/03-ai-coding-redlines.md');
  assert.ok(alwaysEntry, 'the AI coding redlines rule must stay always-loaded');
  assert.match(alwaysEntry.reason, /复用优先/u);
});

test('text output and decision review rules are canonical always-loaded sources', () => {
  assert.equal(index.version, 21);
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
