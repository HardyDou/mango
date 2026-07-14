import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const testDir = path.dirname(fileURLToPath(import.meta.url));
const preflight = path.resolve(testDir, '../tools/pmo-preflight.mjs');

function git(cwd, ...args) {
  return execFileSync('git', args, { cwd, encoding: 'utf8' }).trim();
}

function createRepository() {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-pmo-preflight-'));
  git(root, 'init', '-b', 'main');
  git(root, 'config', 'user.email', 'pmo-test@example.com');
  git(root, 'config', 'user.name', 'PMO Test');
  fs.writeFileSync(path.join(root, 'README.md'), 'fixture\n');
  git(root, 'add', 'README.md');
  git(root, 'commit', '-m', 'fixture');
  return root;
}

function runPreflight(cwd, task, paths) {
  return runPreflightAs(cwd, 'dev', 'develop', task, paths);
}

function runPreflightAs(cwd, role, phase, task, paths) {
  const output = execFileSync(process.execPath, [
    preflight,
    '--role', role,
    '--phase', phase,
    '--task', task,
    '--paths', paths,
    '--json'
  ], { cwd, encoding: 'utf8' });
  return JSON.parse(output);
}

test('main branch recommends a task worktree for code changes and leaves M01 to human confirmation', () => {
  const root = createRepository();
  try {
    const result = runPreflight(root, '修复后端 API', 'mango/**');
    assert.equal(result.currentWorkspace.isMainBranch, true);
    assert.equal(result.classifiedWorkspacePolicy.mode, 'worktree-required');
    assert.equal(result.workspacePolicy.mode, 'worktree-required');
    assert.equal(result.assuranceRecommendation.measureId, 'M01');
    assert.equal(result.assuranceRecommendation.recommendedValue, 'CREATE');
    assert.equal(result.assuranceRecommendation.requiresHumanConfirmation, true);
    assert.match(result.assuranceRecommendation.reason, /path mango\/\*\*/);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('non-main branch recommends reusing the current workspace without creating another worktree', () => {
  const root = createRepository();
  try {
    git(root, 'switch', '-c', 'fix/api-contract');
    const result = runPreflight(root, '修复后端 API', 'mango/**');
    assert.equal(result.currentWorkspace.reusableTaskWorkspace, true);
    assert.equal(result.classifiedWorkspacePolicy.mode, 'worktree-required');
    assert.equal(result.workspacePolicy.mode, 'reuse-current-worktree');
    assert.match(result.workspacePolicy.summary, /建议复用.*不再创建/);
    assert.equal(result.assuranceRecommendation.recommendedValue, 'DO_NOT_CREATE');
    assert.equal(result.assuranceRecommendation.requiresHumanConfirmation, true);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('unclear scope still requires confirmation on a non-main branch', () => {
  const root = createRepository();
  try {
    git(root, 'switch', '-c', 'fix/unknown-scope');
    const result = runPreflight(root, '帮我看看并处理', 'unknown-area');
    assert.equal(result.classifiedWorkspacePolicy.mode, 'needs-human-check');
    assert.equal(result.workspacePolicy.mode, 'needs-human-check');
    assert.equal(result.assuranceRecommendation.recommendedValue, null);
    assert.equal(result.assuranceRecommendation.requiresHumanConfirmation, true);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('release phase loads artifact version synchronization instead of domain versioning', () => {
  const root = createRepository();
  try {
    git(root, 'switch', '-c', 'release/mango-batch');
    const result = runPreflightAs(
      root,
      'pmo',
      'release',
      '发布 Maven npm CLI starter PMO 批次',
      'mango/**,mango-ui/**,mango-pmo/**',
    );
    const mustRead = new Set(result.mustRead.map(item => item.path));
    assert.equal(mustRead.has('rules/10-release-artifacts.md'), true);
    assert.equal(mustRead.has('rules/backend/09-versioning.md'), false);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('CLI and Skill release paths load artifact version synchronization during governance', () => {
  const root = createRepository();
  try {
    const result = runPreflightAs(
      root,
      'pmo',
      'governance',
      '同步 CLI、Skill 与 PMO 发布矩阵',
      'mango-ui/packages/mango-cli/release-versions.json,mango-pmo/skills/**',
    );
    const mustRead = new Set(result.mustRead.map(item => item.path));
    assert.equal(mustRead.has('rules/10-release-artifacts.md'), true);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});
