#!/usr/bin/env node
import { existsSync, lstatSync, readFileSync } from 'node:fs';
import { parseMarkdown } from './document-contract/markdown-ast.mjs';

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

const reviewSections = parseMarkdown(source).sections.filter(section => section.logicalTitle === '评审选择与记录');
if (reviewSections.length !== 1) failures.push('评审选择与记录章节必须存在且唯一');
const reviewSection = reviewSections[0];
const reviewMode = reviewField(reviewSection, '评审选择');
if (!['AGENT_ONLY', 'EXTERNAL_PEER_REVIEW'].includes(reviewMode)) {
  failures.push('评审选择必须记录为 AGENT_ONLY 或 EXTERNAL_PEER_REVIEW');
}
const inquiry = reviewField(reviewSection, '询问记录');
if (isEmptyOrPlaceholder(inquiry)) failures.push('询问记录必须填写非空的询问与选择事实');

// Parse every row by its headers: mode and suggestions must never count as a conclusion.
const reviewTables = reviewSection?.tables ?? [];
if (reviewTables.length === 0 || reviewTables.every(table => table.rows.length === 0)) {
  failures.push('评审选择记录必须填写方式、参与者、阻断问题、建议和结论');
}
for (const table of reviewTables) {
  if (new Set(table.headers).size !== table.headers.length) failures.push('评审记录表头不能重复');
  for (const header of ['评审方式', '评审人/Agent 角色', '阻断问题', '结论']) {
    if (!table.headers.includes(header)) failures.push(`评审记录缺少列：${header}`);
  }
  if (table.malformedRows.length > 0) failures.push('评审记录行列数必须与表头一致');
  for (const { values: row, line } of table.rows) {
    if (row['评审方式'] !== reviewMode) failures.push(`第 ${line} 行评审方式必须与评审选择一致`);
    const reviewer = row['评审人/Agent 角色'];
    if (isEmptyOrPlaceholder(reviewer)) failures.push(`第 ${line} 行必须填写评审人/Agent 角色`);
    if (reviewMode === 'EXTERNAL_PEER_REVIEW'
      && /(?:ai|agent|codex|claude|gpt|机器人|模拟|dry[- ]?run|自评)/iu.test(reviewer ?? '')) {
      failures.push('EXTERNAL_PEER_REVIEW 必须填写真实同行，不能填写 AI 或模拟角色');
    }
    if (!/^(?:none|无|无阻断项|无阻断问题)$/iu.test(row['阻断问题'] ?? '')) {
      failures.push(`第 ${line} 行阻断问题必须明确为 None 或无；开放阻断不能通过复核`);
    }
    const conclusions = reviewMode === 'AGENT_ONLY' ? ['PASS', 'RECORDED'] : ['PASS'];
    if (!conclusions.includes(row['结论'])) {
      failures.push(`第 ${line} 行 ${reviewMode} 结论必须为 ${conclusions.join(' 或 ')}`);
    }
  }
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
function reviewField(section, label) {
  const pattern = new RegExp(`^-[ \\t]+\\*\\*${escapeRegExp(label)}：\\*\\*[ \\t]*(.*)$`, 'u');
  const matches = (section?.nodes ?? [])
    .filter(node => node.type === 'text')
    .map(node => pattern.exec(node.value.trim()))
    .filter(Boolean);
  if (matches.length !== 1) failures.push(`${label}必须在评审选择与记录章节填写且只能出现一次`);
  return matches.length === 1 ? matches[0][1].trim() : '';
}
function isEmptyOrPlaceholder(value) {
  return !value || /^(?:-|n\/?a|none|tbd|todo)$/iu.test(value);
}
function valueAfter(flag) {
  const index = process.argv.indexOf(flag);
  return index >= 0 ? process.argv[index + 1] : null;
}
function fail(message) {
  console.error(`[FAIL] ${message}`);
  process.exit(1);
}
