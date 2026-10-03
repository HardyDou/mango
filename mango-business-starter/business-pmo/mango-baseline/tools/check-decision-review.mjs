#!/usr/bin/env node
import { existsSync, lstatSync, readFileSync } from 'node:fs';

const documentPath = valueAfter('--document');
if (!documentPath) fail('用法：node check-decision-review.mjs --document <decision-review.md>');
if (!existsSync(documentPath) || !lstatSync(documentPath).isFile() || lstatSync(documentPath).isSymbolicLink()) {
  fail(`决策评审文件不存在或不是普通文件：${documentPath}`);
}
const source = readFileSync(documentPath, 'utf8');
const failures = [];

if (source.includes('{{')) failures.push('不能包含未填写的模板占位符');
for (const heading of [
  '## 1. 决定摘要',
  '## 2. 第一轮：事实与用户视角',
  '## 3. 第二轮：技术与风险视角',
  '## 4. 第三轮：验证与交付视角',
  '## 5. 方案整理',
  '## 6. 同行评审',
  '## 7. 审批与后续动作',
]) {
  if (!source.includes(heading)) failures.push(`缺少章节：${heading}`);
}
for (const round of ['第一轮：事实与用户视角', '第二轮：技术与风险视角', '第三轮：验证与交付视角']) {
  const section = sectionAfter(source, round);
  if (!section || !hasDataRow(section)) failures.push(`${round} 必须填写独立意见和证据`);
}
const reviewer = firstDataRow(sectionAfter(source, '同行评审'));
if (!reviewer) {
  failures.push('同行评审必须填写评审人、阻断问题、建议和结论');
} else {
  if (/(?:ai|agent|codex|claude|gpt|机器人|模拟|dry[- ]?run|自评)/iu.test(reviewer)) {
    failures.push('同行评审必须由真实同行完成，不能填写 AI 或模拟角色');
  }
  if (!/\bPASS\b/u.test(reviewer)) failures.push('同行评审结论必须为 PASS');
}
const status = source.match(/^-\s*\*\*状态：\*\*\s*(\S+)/mu)?.[1];
if (!['REVIEWED', 'APPROVED'].includes(status)) failures.push('状态必须为 REVIEWED 或 APPROVED');
if (status === 'APPROVED') {
  const approval = source.match(/^-\s*\*\*审批人：\*\*\s*([^\n]+)/mu)?.[1]?.trim();
  if (!approval || /(?:ai|agent|codex|claude|gpt|机器人|模拟|dry[- ]?run|自评)/iu.test(approval)) {
    failures.push('APPROVED 必须填写真实人工审批人');
  }
}

if (failures.length) {
  console.error('[FAIL] decision review');
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}
console.log('[PASS] decision review: three independent rounds and peer review recorded');

function sectionAfter(text, title) {
  const match = text.match(new RegExp(`^##\\s+(?:\\d+\\.\\s*)?${escapeRegExp(title)}\\s*$`, 'mu'));
  if (!match || match.index === undefined) return '';
  const start = match.index;
  const next = text.indexOf('\n## ', start + match[0].length);
  return text.slice(start, next < 0 ? text.length : next);
}
function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
function hasDataRow(section) {
  return Boolean(firstDataRow(section));
}
function firstDataRow(section) {
  return section.split(/\r?\n/u).find(line => /^\|/u.test(line) && !/^\|\s*:?-{2,}/u.test(line) && !/^\|\s*(参与者\/角色|评审人)\s*\|/u.test(line));
}
function valueAfter(flag) {
  const index = process.argv.indexOf(flag);
  return index >= 0 ? process.argv[index + 1] : null;
}
function fail(message) {
  console.error(`[FAIL] ${message}`);
  process.exit(1);
}
