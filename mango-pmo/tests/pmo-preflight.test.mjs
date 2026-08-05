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

function runPreflight(cwd, task, paths, extraArgs = []) {
  return runPreflightAs(cwd, 'dev', 'develop', task, paths, extraArgs);
}

function runPreflightAs(cwd, role, phase, task, paths, extraArgs = []) {
  const output = execFileSync(process.execPath, [
    preflight,
    '--role', role,
    '--phase', phase,
    '--task', task,
    '--paths', paths,
    '--json',
    ...extraArgs,
  ], { cwd, encoding: 'utf8' });
  return JSON.parse(output);
}

test('main branch automatically selects a task worktree for tracked changes', () => {
  const root = createRepository();
  try {
    const result = runPreflight(root, '修复后端 API', 'mango/**', ['--reuseCurrentTask', 'true']);
    assert.equal(result.currentWorkspace.isMainBranch, true);
    assert.equal(result.classifiedWorkspacePolicy.mode, 'worktree-required');
    assert.equal(result.workspacePolicy.mode, 'worktree-required');
    assert.equal(result.assuranceRecommendation.measureId, 'M01');
    assert.equal(result.assuranceRecommendation.recommendedValue, 'CREATE');
    assert.equal(result.assuranceRecommendation.requiresHumanConfirmation, false);
    assert.equal(result.assuranceRecommendation.decisionSource, 'policy');
    assert.match(result.assuranceRecommendation.reason, /path mango\/\*\*/);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('non-main worktree is not reused without an explicit same-task fact', () => {
  const root = createRepository();
  try {
    git(root, 'switch', '-c', 'fix/previous-task');
    const result = runPreflight(root, '实现新的独立 API', 'mango/**');
    assert.equal(result.currentWorkspace.reusableTaskWorkspace, true);
    assert.equal(result.workspacePolicy.mode, 'worktree-required');
    assert.equal(result.assuranceRecommendation.recommendedValue, 'CREATE');
    assert.match(result.workspacePolicy.reason, /same-task reuse not declared/);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('non-main branch automatically reuses the current task workspace', () => {
  const root = createRepository();
  try {
    git(root, 'switch', '-c', 'fix/api-contract');
    const result = runPreflight(root, '修复后端 API', 'mango/**', ['--reuseCurrentTask', 'true']);
    assert.equal(result.currentWorkspace.reusableTaskWorkspace, true);
    assert.equal(result.classifiedWorkspacePolicy.mode, 'worktree-required');
    assert.equal(result.workspacePolicy.mode, 'reuse-current-worktree');
    assert.match(result.workspacePolicy.summary, /建议复用.*不再创建/);
    assert.equal(result.assuranceRecommendation.recommendedValue, 'REUSE');
    assert.equal(result.assuranceRecommendation.requiresHumanConfirmation, false);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('declared unknown tracked path still defaults to worktree isolation', () => {
  const root = createRepository();
  try {
    git(root, 'switch', '-c', 'fix/unknown-scope');
    const result = runPreflight(root, '帮我看看并处理', 'unknown-area', ['--reuseCurrentTask', 'true']);
    assert.equal(result.classifiedWorkspacePolicy.mode, 'worktree-required');
    assert.equal(result.workspacePolicy.mode, 'reuse-current-worktree');
    assert.equal(result.assuranceRecommendation.recommendedValue, 'REUSE');
    assert.equal(result.assuranceRecommendation.requiresHumanConfirmation, false);
    assert.equal(result.assuranceRecommendation.requiresScopeClarification, false);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('governance files no longer receive an implicit main-worktree exception', () => {
  const root = createRepository();
  try {
    const result = runPreflightAs(root, 'pmo', 'governance', '调整 PMO 规范', 'mango-pmo/rules/00-dev-flow.md');
    assert.equal(result.workspacePolicy.mode, 'worktree-required');
    assert.equal(result.assuranceRecommendation.recommendedValue, 'CREATE');
    assert.equal(result.assuranceRecommendation.requiresHumanConfirmation, false);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('preflight returns advisory references without a mandatory reading field', () => {
  const root = createRepository();
  try {
    const result = runPreflightAs(root, 'pmo', 'governance', '调整 Agent 入口', 'AGENTS.md');
    assert.equal(Array.isArray(result.referenceDocs), true);
    assert.equal(Object.hasOwn(result, 'mustRead'), false);
    assert.equal(result.referenceDocs.length <= 3, true, JSON.stringify(result.referenceDocs, null, 2));
    const references = new Set(result.referenceDocs.map(item => item.path));
    assert.equal(references.has('rules/product/05-document-lifecycle.md'), false);
    assert.equal(references.has('rules/08-capability-docs.md'), false);
    assert.equal(references.has('rules/07-mango-issue-runbook.md'), false);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('business module development resolves the canonical code baseline', () => {
  const root = createRepository();
  try {
    const result = runPreflight(
      root,
      '新增订单业务模块',
      'backend/modules/order,frontend/packages/order',
    );
    assert.deepEqual(result.codeBaselines.map(item => item.id), ['business-module']);
    assert.equal(result.codeBaselines[0].root, 'code-templates/business-module');
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('main exception is accepted only through an explicit confirmed argument', () => {
  const root = createRepository();
  try {
    const output = execFileSync(process.execPath, [
      preflight,
      '--role', 'dev',
      '--phase', 'develop',
      '--task', '按用户确认直接在 main 修复',
      '--paths', 'mango/**',
      '--mainExceptionConfirmed', 'true',
      '--json',
    ], { cwd: root, encoding: 'utf8' });
    const result = JSON.parse(output);
    assert.equal(result.workspacePolicy.mode, 'main-exception-authorized');
    assert.equal(result.assuranceRecommendation.recommendedValue, 'MAIN_EXCEPTION');
    assert.equal(result.assuranceRecommendation.requiresHumanConfirmation, true);
    assert.equal(result.assuranceRecommendation.decisionSource, 'human-exception');
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
    const references = new Set(result.referenceDocs.map(item => item.path));
    assert.equal(references.has('rules/10-release-artifacts.md'), true);
    assert.equal(references.has('rules/backend/09-versioning.md'), false);
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
    const references = new Set(result.referenceDocs.map(item => item.path));
    assert.equal(references.has('rules/10-release-artifacts.md'), true);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('negative unchanged facts do not load unrelated domain bundles', () => {
  const root = createRepository();
  try {
    const result = runPreflightAs(
      root,
      'pm',
      'requirement',
      '只修改内部页面按钮文案，行为、API、数据库、数据、菜单、权限都不变',
      '',
    );
    const references = new Set(result.referenceDocs.map(item => item.path));
    assert.equal(references.has('rules/11-delivery-assurance.md'), true);
    assert.equal(references.has('rules/backend/04-db.md'), false);
    assert.equal(references.has('rules/backend/07-persistence.md'), false);
    assert.equal(references.has('rules/backend/11-module-menu.md'), false);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('an API change before a comma is not negated by later unchanged facts', () => {
  const root = createRepository();
  try {
    const result = runPreflightAs(root, 'dev', 'develop', '修改 API，数据和权限不变', '');
    const references = new Set(result.referenceDocs.map(item => item.path));
    assert.equal(references.has('rules/backend/03-api.md'), true);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});
