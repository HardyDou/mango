#!/usr/bin/env node
import { spawnSync } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const preflightPath = path.join(__dirname, 'pmo-preflight.mjs');

const cases = [
  {
    name: 'pmo governance can use main workspace',
    args: ['--role', 'pmo', '--phase', 'governance', '--task', '优化 preflight 工作区策略', '--paths', 'mango-pmo/tools,mango-pmo/rules'],
    mode: 'main-direct-allowed'
  },
  {
    name: 'backend code requires worktree',
    args: ['--role', 'dev', '--phase', 'develop', '--task', '修复后端代码', '--paths', 'mango/mango-platform/mango-job/mango-job-core/src/main/java'],
    mode: 'worktree-required',
    mustRead: ['rules/08-capability-docs.md']
  },
  {
    name: 'frontend page requires worktree',
    args: ['--role', 'dev', '--phase', 'develop', '--task', '修改前端页面', '--paths', 'mango-ui/packages/admin-shell/src/views/home/index.vue'],
    mode: 'worktree-required'
  },
  {
    name: 'mixed governance and release script requires worktree',
    args: ['--role', 'pmo', '--phase', 'governance', '--task', '更新规范并修改发布脚本', '--paths', 'mango-pmo/rules,mango-ui/scripts/publish-package.mjs'],
    mode: 'worktree-required'
  },
  {
    name: 'unknown scope needs human check',
    args: ['--role', 'dev', '--phase', 'develop', '--task', '处理问题', '--paths', 'unknown/path'],
    mode: 'needs-human-check'
  },
  {
    name: 'pr review loads delivery contract',
    args: ['--role', 'dev', '--phase', 'develop', '--task', '评审 PR #151 并提交 PR 修复', '--paths', 'mango-ui/packages/mango-cli'],
    mode: 'worktree-required',
    mustRead: ['rules/01-delivery-contract.md']
  },
  {
    name: 'module README change loads capability docs',
    args: ['--role', 'dev', '--phase', 'develop', '--task', '更新 Job 模块 README 能力说明', '--paths', 'mango/mango-platform/mango-job/README.md'],
    mode: 'worktree-required',
    mustRead: ['rules/08-capability-docs.md', 'rules/06-document-assets.md']
  },
  {
    name: 'capability map governance loads capability docs',
    args: ['--role', 'pmo', '--phase', 'governance', '--task', '优化 Mango 能力地图', '--paths', 'mango-docs/capabilities/README.md'],
    mode: 'main-direct-allowed',
    mustRead: ['rules/08-capability-docs.md', 'rules/06-document-assets.md']
  },
  {
    name: 'agent entry governance loads PMO docs',
    args: ['--role', 'pmo', '--phase', 'governance', '--task', '优化 Agent 入口 PMO 触发边界', '--paths', 'AGENTS.md'],
    mode: 'main-direct-allowed',
    mustRead: ['agents/05-pmo-agent.md', 'rules/06-document-assets.md']
  },
  {
    name: 'nested src glob loads capability docs',
    args: ['--role', 'dev', '--phase', 'develop', '--task', '调整模块实现', '--paths', 'mango/mango-platform/mango-job/mango-job-core/src/main/java/com/example/Job.java'],
    mode: 'worktree-required',
    mustRead: ['rules/08-capability-docs.md']
  }
];

const failures = [];

for (const item of cases) {
  const result = spawnSync('node', [preflightPath, ...item.args, '--json'], {
    encoding: 'utf8',
    stdio: 'pipe'
  });
  if (result.status !== 0) {
    failures.push(`${item.name}: preflight exited ${result.status}\n${result.stderr || result.stdout}`);
    continue;
  }
  const output = JSON.parse(result.stdout);
  if (output.workspacePolicy?.mode !== item.mode) {
    failures.push(`${item.name}: expected ${item.mode}, got ${output.workspacePolicy?.mode || '<missing>'}`);
  }
  for (const expectedPath of item.mustRead || []) {
    const hasPath = (output.mustRead || []).some((entry) => entry.path === expectedPath);
    if (!hasPath) {
      failures.push(`${item.name}: expected mustRead ${expectedPath}`);
    }
  }
}

if (failures.length > 0) {
  console.error(`PMO preflight check failed:\n${failures.map((failure) => `- ${failure}`).join('\n')}`);
  process.exit(1);
}

console.log(`PMO preflight workspace policy checks passed: ${cases.length}`);
