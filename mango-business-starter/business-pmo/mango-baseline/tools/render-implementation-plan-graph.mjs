#!/usr/bin/env node
import { existsSync, lstatSync, readFileSync } from 'node:fs';
import path from 'node:path';
import { parseMarkdown, tableKey } from './document-contract/markdown-ast.mjs';

const documentPath = valueAfter('--document');
const format = valueAfter('--format') ?? 'mermaid';
if (!documentPath) fail('用法：node render-implementation-plan-graph.mjs --document <path> [--format mermaid|json|markdown]');
if (!['mermaid', 'json', 'markdown'].includes(format)) fail(`不支持的格式：${format}`);
if (!existsSync(documentPath) || !lstatSync(documentPath).isFile() || lstatSync(documentPath).isSymbolicLink()) {
  fail(`实施计划不存在或不是普通文件：${documentPath}`);
}

const ast = parseMarkdown(readFileSync(documentPath, 'utf8'));
if (ast.frontmatter.values.documentType !== 'implementation-plan') {
  fail('文档 documentType 必须为 implementation-plan');
}
const section = ast.sections.find(item => item.logicalTitle === '工作分解');
const headers = ['任务ID', '技术设计ID', '交付物ID', '责任角色', '路径或模块', '前置任务', '具体动作', '完成标准', '验证ID', '实施批次', '状态'];
const table = section?.tables.find(item => tableKey(item.headers) === tableKey(headers));
if (!table) fail('缺少“工作分解”任务表');

const tasks = table.rows.map(row => ({
  id: row.values['任务ID'],
  dependencyText: row.values['前置任务'],
  dependsOn: row.values['前置任务'] === 'NONE' ? [] : identifiers(row.values['前置任务']),
  owner: row.values['责任角色'],
  path: row.values['路径或模块'],
  action: row.values['具体动作'],
  status: row.values['状态'],
}));
const ids = new Set(tasks.map(task => task.id));
const errors = [];
for (const task of tasks) {
  if (!/^TASK-[A-Z0-9][A-Z0-9._-]*$/u.test(task.id)) errors.push(`非法任务 ID：${task.id}`);
  if (task.dependencyText !== 'NONE' && task.dependsOn.length === 0) {
    errors.push(`${task.id} 的前置任务必须引用 TASK-... 或为 NONE`);
  }
  for (const dependency of task.dependsOn) {
    if (!ids.has(dependency)) errors.push(`${task.id} 依赖不存在的任务：${dependency}`);
    if (dependency === task.id) errors.push(`${task.id} 不能依赖自身`);
  }
}
if (errors.length > 0) {
  console.error('[FAIL] implementation plan graph');
  for (const error of errors) console.error(`- ${error}`);
  process.exit(1);
}

if (format === 'json') {
  console.log(JSON.stringify({ document: path.resolve(documentPath), tasks }, null, 2));
} else if (format === 'markdown') {
  console.log(`# 任务图\n\n来源：\`${documentPath}\`\n\n\`\`\`mermaid\n${renderMermaid(tasks)}\n\`\`\`\n`);
} else {
  console.log(renderMermaid(tasks));
}

function renderMermaid(items) {
  const lines = ['flowchart LR'];
  for (const task of items) {
    const label = `${task.id}<br/>${task.status}`.replaceAll('"', '\\"');
    lines.push(`  ${safeId(task.id)}["${label}"]`);
  }
  for (const task of items) {
    for (const dependency of task.dependsOn) lines.push(`  ${safeId(dependency)} --> ${safeId(task.id)}`);
  }
  return lines.join('\n');
}
function safeId(value) {
  return value.replaceAll(/[^A-Za-z0-9_]/gu, '_');
}
function identifiers(value) {
  return [...String(value).matchAll(/TASK-[A-Z0-9][A-Z0-9._-]*/gu)].map(match => match[0]);
}
function valueAfter(flag) {
  const index = process.argv.indexOf(flag);
  return index >= 0 ? process.argv[index + 1] : null;
}
function fail(message) {
  console.error(`[FAIL] ${message}`);
  process.exit(1);
}
