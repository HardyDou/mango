import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const tool = path.join(root, 'tools/check-document-reading-entry.mjs');

function run(document) {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-reading-entry-'));
  const documentPath = path.join(directory, 'document.md');
  fs.writeFileSync(documentPath, document);
  return spawnSync(process.execPath, [tool, '--document', documentPath], { encoding: 'utf8' });
}

const valid = `---\ndocumentType: business-requirements\n---\n\n# Example\n\n> **先读：这份文档要回答什么**\n> - **一句话目标：** 让员工提交申请\n> - **真实场景：** 员工在门户提交申请\n> - **关键输入 / 输出：** 表单输入 / 申请状态\n> - **成功与失败：** 成功显示编号，失败说明原因\n> - **明确不做：** 不处理审批\n> - **阅读顺序：** 先看摘要，再看表格\n\n## 1. 业务背景与问题\n`;

test('human reading entry passes when the six fields are present', () => {
  const result = run(valid);
  assert.equal(result.status, 0, result.stderr);
});

test('human reading entry fails when the scenario is missing', () => {
  const result = run(valid.replace('> - **真实场景：** 员工在门户提交申请\n', ''));
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /真实场景/);
});
