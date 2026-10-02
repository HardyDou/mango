#!/usr/bin/env node
import { existsSync, lstatSync, readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const pmoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const ruleIndexPath = path.join(pmoRoot, 'rules/index.json');
const ruleId = valueAfter('--id');
const explicitRule = valueAfter('--rule');
if (!ruleId && !explicitRule) fail('用法：node render-rule-action-card.mjs --id <rule-id> 或 --rule <path>');

const rulePath = explicitRule ? resolveRulePath(explicitRule) : resolveRuleId(ruleId);
if (!existsSync(rulePath) || !lstatSync(rulePath).isFile() || lstatSync(rulePath).isSymbolicLink()) {
  fail(`规范源不存在或不是普通文件：${rulePath}`);
}
const source = readFileSync(rulePath, 'utf8');
const lines = source.split(/\r?\n/u);
const title = lines.find(line => /^#\s+/u.test(line))?.replace(/^#\s+/u, '').trim() ?? path.basename(rulePath);
const fields = new Map([
  ['目的', '先判断'],
  ['正向要求', '执行'],
  ['禁止项', '停止或升级'],
  ['机器判定', '证据'],
]);
const card = [];
for (const [label, outputLabel] of fields) {
  const value = firstLabeledValue(lines, label) ?? fallbackSectionValue(lines, outputLabel);
  if (value) card.push(`- **${outputLabel}：** ${value}`);
}
if (card.length === 0) {
  const actionable = lines
    .filter(line => /^[-*]\s+/u.test(line) && line.trim().length > 4)
    .slice(0, 4)
    .map(line => `- ${line.replace(/^[-*]\s+/u, '')}`);
  card.push('- **先判断：** 当前规则没有结构化执行字段，请回到规范源阅读完整上下文。');
  card.push(...actionable);
}

console.log(`# 执行卡片：${title}`);
console.log('');
console.log(`- **规范源：** \`${toRepoRelative(rulePath)}\``);
console.log('- **使用方式：** 本卡片只压缩当前规范的执行入口；遇到冲突、例外或缺少上下文时，以规范源和 preflight 为准。');
console.log(card.join('\n'));

function firstLabeledValue(sourceLines, label) {
  const pattern = new RegExp(`^[-*]\\s+\\*\\*${escapeRegExp(label)}\\*\\*\\s*[:：]\\s*(.+)$`, 'u');
  return sourceLines.find(line => pattern.test(line))?.match(pattern)?.[1]?.trim() ?? null;
}
function fallbackSectionValue(sourceLines, outputLabel) {
  const headingPatterns = {
    '先判断': /定位|目标|决策与询问边界/iu,
    '执行': /标准流程|交付模式|执行能力|开发|验证/iu,
    '停止或升级': /禁止事项|禁止|红线/iu,
    '证据': /机器检查|验证|决策基线|证据/iu,
  };
  const headingPattern = headingPatterns[outputLabel];
  if (!headingPattern) return null;
  for (let index = 0; index < sourceLines.length; index += 1) {
    if (!/^##\s+/u.test(sourceLines[index]) || !headingPattern.test(sourceLines[index])) continue;
    for (let cursor = index + 1; cursor < sourceLines.length; cursor += 1) {
      const line = sourceLines[cursor].trim();
      if (/^##\s+/u.test(line)) break;
      if (line && !line.startsWith('#') && !line.startsWith('---')) return line.replace(/^[-*]\s+/u, '').trim();
    }
  }
  return null;
}
function resolveRuleId(id) {
  const index = JSON.parse(readFileSync(ruleIndexPath, 'utf8'));
  const entry = index.rules?.[id];
  if (!entry?.path) fail(`rules/index.json 中不存在规则：${id}`);
  return path.join(pmoRoot, entry.path.replace(/^rules\//u, 'rules/'));
}
function resolveRulePath(value) {
  const normalized = value.replaceAll('\\', '/');
  return path.isAbsolute(normalized) ? normalized : path.resolve(pmoRoot, normalized.replace(/^mango-pmo\//u, ''));
}
function toRepoRelative(file) {
  const relative = path.relative(path.resolve(pmoRoot, '..'), file).replaceAll('\\', '/');
  return relative || file;
}
function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/gu, '\\$&');
}
function valueAfter(flag) {
  const index = process.argv.indexOf(flag);
  return index >= 0 ? process.argv[index + 1] : null;
}
function fail(message) {
  console.error(`[FAIL] ${message}`);
  process.exit(1);
}
