#!/usr/bin/env node
import { existsSync, lstatSync, readFileSync } from 'node:fs';
import path from 'node:path';
import { parseMarkdown } from './document-contract/markdown-ast.mjs';

const documents = process.argv
  .flatMap((argument, index, argumentsList) => argument === '--document' ? [argumentsList[index + 1]] : [])
  .filter(Boolean);
if (documents.length === 0) fail('用法：node check-document-reading-entry.mjs --document <path> [--document <path> ...]');

const formalTypes = new Set(['business-requirements', 'system-requirements', 'technical-design', 'implementation-plan']);
const labels = ['一句话目标', '真实场景', '关键输入 / 输出', '成功与失败', '明确不做', '阅读顺序'];
const findings = [];
for (const documentPath of documents) {
  if (!existsSync(documentPath) || !lstatSync(documentPath).isFile() || lstatSync(documentPath).isSymbolicLink()) {
    findings.push(`${documentPath}: 不存在或不是普通文件`);
    continue;
  }
  const source = readFileSync(documentPath, 'utf8');
  const ast = parseMarkdown(source);
  if (!formalTypes.has(ast.frontmatter.values.documentType)) {
    findings.push(`${documentPath}: 不是 BRD/SRS/TDD/Plan，跳过阅读入口检查`);
    continue;
  }
  const firstH2 = ast.headings.find(heading => heading.level === 2);
  const prelude = ast.lines.slice(ast.frontmatter.endLine + 1, (firstH2?.line ?? ast.lines.length + 1) - 1).join('\n');
  for (const label of labels) {
    if (!prelude.includes(`**${label}：**`)) findings.push(`${documentPath}: 人类摘要缺少字段 ${label}`);
  }
  if (prelude.includes('{{')) findings.push(`${documentPath}: 人类摘要包含未填写占位符`);
  if (!prelude.includes('先读：这份文档要回答什么')) {
    findings.push(`${documentPath}: 缺少“先读”标记`);
  }
}

if (findings.length > 0) {
  console.error('[FAIL] document reading entry');
  for (const finding of findings) console.error(`- ${finding}`);
  process.exit(1);
}
console.log(`[PASS] document reading entry: ${documents.length} file(s)`);

function fail(message) {
  console.error(`[FAIL] ${message}`);
  process.exit(1);
}
