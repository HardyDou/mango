#!/usr/bin/env node
import { existsSync, lstatSync, readFileSync } from 'node:fs';
import path from 'node:path';
import { parseMarkdown } from './document-contract/markdown-ast.mjs';

const documentPath = valueAfter('--document');
if (!documentPath) fail('用法：node check-retrospective.mjs --document <path>');
if (!existsSync(documentPath) || !lstatSync(documentPath).isFile() || lstatSync(documentPath).isSymbolicLink()) {
  fail(`复盘文档不存在或不是普通文件：${documentPath}`);
}

const source = readFileSync(documentPath, 'utf8');
const ast = parseMarkdown(source);
const findings = [];
const values = ast.frontmatter.values;
for (const key of ['retrospectiveId', 'documentType', 'status', 'owner', 'reviewedCommitOrVersion', 'sourceDeliveryRecord']) {
  if (!String(values[key] ?? '').trim() || String(values[key]).includes('{{')) {
    findings.push(`frontmatter 缺少有效值：${key}`);
  }
}
if (values.documentType !== 'delivery-retrospective') findings.push('documentType 必须为 delivery-retrospective');
if (!/^RETRO-[A-Z0-9][A-Z0-9._-]*$/u.test(String(values.retrospectiveId ?? ''))) {
  findings.push('retrospectiveId 必须是 RETRO-... 格式');
}

const requiredSections = ['保留', '改变', '停止', '理解与路由复盘', '后续动作', '证据索引'];
for (const title of requiredSections) {
  if (!ast.sections.some(section => section.logicalTitle === title)) findings.push(`缺少章节：${title}`);
}

const firstH2 = ast.headings.find(heading => heading.level === 2);
const prelude = ast.lines.slice(ast.frontmatter.endLine + 1, (firstH2?.line ?? ast.lines.length + 1) - 1).join('\n');
for (const label of ['一句话结论', '真实场景', '关键证据', '成功与失败', '明确不做', '阅读顺序']) {
  if (!prelude.includes(`**${label}：**`)) findings.push(`人类摘要缺少字段：${label}`);
}
if (prelude.includes('{{')) findings.push('人类摘要仍包含未填写占位符');

for (const section of ast.sections) {
  if (!requiredSections.includes(section.logicalTitle)) continue;
  if (section.tables.length === 0 || section.tables.every(table => table.rows.length === 0)) {
    findings.push(`章节“${section.logicalTitle}”必须包含至少一行事实表格`);
  }
}
if (source.includes('{{')) findings.push('文档仍包含未填写占位符');

if (findings.length > 0) {
  console.error(`[FAIL] ${path.resolve(documentPath)}`);
  for (const finding of findings) console.error(`- ${finding}`);
  process.exit(1);
}
console.log(`[PASS] retrospective ${path.resolve(documentPath)}`);

function valueAfter(flag) {
  const index = process.argv.indexOf(flag);
  return index >= 0 ? process.argv[index + 1] : null;
}
function fail(message) {
  console.error(`[FAIL] ${message}`);
  process.exit(1);
}
