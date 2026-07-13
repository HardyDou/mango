import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const testDir = path.dirname(fileURLToPath(import.meta.url));
const checker = path.resolve(testDir, '../tools/workspace-layout-check.mjs');

function createRoot(gitignore = '.runtime/\n.mango/\n') {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-workspace-layout-'));
  fs.writeFileSync(path.join(root, '.gitignore'), gitignore);
  return root;
}

function run(root) {
  const result = spawnSync(process.execPath, [checker, '--root', root, '--json'], {
    encoding: 'utf8'
  });
  return {
    status: result.status,
    report: JSON.parse(result.stdout)
  };
}

test('accepts the canonical runtime layout', () => {
  const root = createRoot();
  try {
    fs.mkdirSync(path.join(root, '.runtime', 'projects'), { recursive: true });
    const result = run(root);
    assert.equal(result.status, 0);
    assert.equal(result.report.passed, true);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('rejects repository-internal worktree locations', () => {
  const root = createRoot();
  try {
    fs.mkdirSync(path.join(root, '.runtime', 'worktrees'), { recursive: true });
    fs.mkdirSync(path.join(root, '.claude', 'worktrees'), { recursive: true });
    const result = run(root);
    assert.equal(result.status, 1);
    assert.equal(result.report.passed, false);
    assert.deepEqual(
      result.report.issues.map((issue) => issue.ruleId),
      ['PMO-WORKSPACE-002', 'PMO-WORKSPACE-002']
    );
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('requires runtime and workspace state to be gitignored', () => {
  const root = createRoot('node_modules/\n');
  try {
    const result = run(root);
    assert.equal(result.status, 1);
    assert.deepEqual(
      result.report.issues.map((issue) => issue.message),
      [
        '缺少 .runtime/ 本机工作区忽略规则。',
        '缺少 .mango/ 本机工作区忽略规则。'
      ]
    );
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});
