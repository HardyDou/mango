#!/usr/bin/env node
import { existsSync, lstatSync, readFileSync } from 'node:fs';

const documentPath = valueAfter('--document');
if (!documentPath) fail('用法：node check-decision-review.mjs --document <decision-review.md>');
if (!existsSync(documentPath) || !lstatSync(documentPath).isFile() || lstatSync(documentPath).isSymbolicLink()) {
  fail(`决策复核文件不存在或不是普通文件：${documentPath}`);
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
  '## 6. 评审选择与记录',
  '## 7. 审批与后续动作',
]) {
  if (!source.includes(heading)) failures.push(`缺少章节：${heading}`);
}
for (const round of ['第一轮：事实与用户视角', '第二轮：技术与风险视角', '第三轮：验证与交付视角']) {
  const section = sectionAfter(source, round);
  if (!section || !hasDataRow(section)) failures.push(`${round} 必须填写独立意见和证据`);
}

const reviewMode = source.match(/^-[ \t]+\*\*评审选择：\*\*[ \t]*(AGENT_ONLY|EXTERNAL_PEER_REVIEW)\b/mu)?.[1];
if (!reviewMode) {
  failures.push('评审选择必须记录为 AGENT_ONLY 或 EXTERNAL_PEER_REVIEW');
}
const reviewSection = sectionAfter(source, '评审选择与记录');
const reviewRecord = firstDataRow(reviewSection);
if (!reviewRecord) {
  failures.push('评审选择记录必须填写方式、参与者、阻断问题、建议和结论');
} else if (reviewMode === 'EXTERNAL_PEER_REVIEW') {
  if (/(?:ai|agent|codex|claude|gpt|机器人|模拟|dry[- ]?run|自评)/iu.test(reviewRecord)) {
    failures.push('EXTERNAL_PEER_REVIEW 必须填写真实同行，不能填写 AI 或模拟角色');
  }
  if (!/\bPASS\b/u.test(reviewRecord)) failures.push('EXTERNAL_PEER_REVIEW 结论必须为 PASS');
} else if (!/(?:用户确认|用户选择|用户决定|询问记录)/u.test(source)) {
  failures.push('AGENT_ONLY 必须记录用户询问和选择');
} else if (!/\b(?:PASS|RECORDED|AGENT_ONLY)\b/u.test(reviewRecord)) {
  failures.push('AGENT_ONLY 必须记录 Agent 分析结论');
}

const status = source.match(/^-[ \t]*\*\*状态：\*\*[ \t]*(\S+)/mu)?.[1];
if (!['REVIEWED', 'APPROVED'].includes(status)) failures.push('状态必须为 REVIEWED 或 APPROVED');
if (status === 'APPROVED') {
  const approval = source.match(/^-[ \t]*\*\*审批人：\*\*[ \t]*([^\n]+)/mu)?.[1]?.trim();
  if (!approval || /(?:ai|agent|codex|claude|gpt|机器人|模拟|dry[- ]?run|自评)/iu.test(approval)) {
    failures.push('APPROVED 必须填写真实人工审批人');
  }
}

if (failures.length) {
  console.error('[FAIL] decision review');
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}
console.log(`[PASS] decision review: three perspectives and ${reviewMode} choice recorded`);

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
  return section.split(/\r?\n/u).find(line =>
    /^\|/u.test(line)
    && !/^\|\s*:?-{2,}/u.test(line)
    && !/^\|\s*(参与者\/角色|评审人|评审方式)/u.test(line),
  );
}
function valueAfter(flag) {
  const index = process.argv.indexOf(flag);
  return index >= 0 ? process.argv[index + 1] : null;
}
function fail(message) {
  console.error(`[FAIL] ${message}`);
  process.exit(1);
}
