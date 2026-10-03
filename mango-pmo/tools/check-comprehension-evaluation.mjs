#!/usr/bin/env node
import { existsSync, lstatSync, readFileSync } from 'node:fs';
import path from 'node:path';
import { splitTableRow } from './document-contract/markdown-ast.mjs';

const evaluationPath = valueAfter('--document');
if (!evaluationPath) fail('用法：node check-comprehension-evaluation.mjs --document <completed-packet.md>');
if (!existsSync(evaluationPath) || !lstatSync(evaluationPath).isFile() || lstatSync(evaluationPath).isSymbolicLink()) {
  fail(`评测包不存在或不是普通文件：${evaluationPath}`);
}
const source = readFileSync(evaluationPath, 'utf8');
const failures = [];
const participantRow = source.match(/^\| 参与者 \| 评测者 \| 日期 \| 版本\/提交 \|\n\|[^\n]+\|\n\|([^\n]+)\|$/mu);
if (!participantRow) {
  failures.push('缺少评测记录行');
} else {
  const cells = splitTableRow(`|${participantRow[1]}|`) ?? [];
  if (cells.length !== 4 || cells.some(cell => !cell.trim())) failures.push('参与者、评测者、日期和版本/提交必须填写');
  if (/(?:ai|agent|codex|claude|gpt|机器人|模拟|dry[- ]?run|self[- ]?evaluation|自评)/iu.test(cells.join(' '))) failures.push('参与者和评测者必须是真实人工，不能填写 AI 或模拟角色');
}

const cases = [...source.matchAll(/^## (COG-\d+)[^\n]*\n([\s\S]*?)(?=^## COG-|^## 汇总|(?![\s\S]))/gmu)];
if (cases.length !== 8) failures.push(`必须包含 8 个 COG 案例，实际 ${cases.length} 个`);
const scores = [];
for (const match of cases) {
  const id = match[1];
  const body = match[2];
  const rowScores = [...body.matchAll(/^\|\s*[1-6]\s*\|[^\n]*\|\s*([01])\s*\|[^\n]*\|$/gmu)].map(item => Number(item[1]));
  if (rowScores.length !== 6) failures.push(`${id}: 六问必须逐题填写 0/1 分，实际 ${rowScores.length} 题`);
  const score = body.match(/本题得分：\*{2}\s*([0-6])\/6/u)?.[1];
  const clarification = body.match(/澄清次数：\*{2}\s*(\d+)/u)?.[1];
  const firstSource = body.match(/参与者首次指出的源码入口：\*{2}\s*([^\n]+)/u)?.[1]?.trim();
  const unexplained = body.match(/未解释术语数量：\*{2}\s*(\d+)/u)?.[1];
  if (score === undefined) failures.push(`${id}: 缺少本题得分`);
  if (clarification === undefined) failures.push(`${id}: 缺少澄清次数`);
  if (!firstSource || firstSource === '-' || firstSource.includes('`')) failures.push(`${id}: 缺少首次源码入口`);
  if (unexplained === undefined) failures.push(`${id}: 缺少未解释术语数量`);
  if (score !== undefined) scores.push(Number(score));
}

const summary = source.match(/^\| 总分达标（每题至少 5\/6） \|\s*([^|]+)\|\n\| 首次源码命中率达标 \|\s*([^|]+)\|\n\| 澄清次数较基线下降 \|\s*([^|]+)\|\n\| 结论 \|\s*([^|]+)\|/mu);
if (!summary || summary.slice(1).some(value => !value.trim())) {
  failures.push('汇总表必须填写四项结论');
} else {
  const [scoreStatus, sourceStatus, clarificationStatus, conclusion] = summary.slice(1).map(value => value.trim());
  if (scores.length === 8 && scores.some(score => score < 5)) failures.push('每个案例必须达到至少 5/6');
  if (scoreStatus !== 'PASS' || sourceStatus !== 'PASS' || clarificationStatus !== 'PASS' || conclusion !== 'PASS') {
    failures.push('理解评测未达到投产阈值');
  }
}

if (failures.length > 0) {
  console.error('[FAIL] comprehension evaluation');
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}
const passed = scores.filter(score => score >= 5).length;
console.log(`[PASS] comprehension evaluation: ${passed}/${scores.length} cases reached 5/6`);

function valueAfter(flag) {
  const index = process.argv.indexOf(flag);
  return index >= 0 ? process.argv[index + 1] : null;
}
function fail(message) {
  console.error(`[FAIL] ${message}`);
  process.exit(1);
}
