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
    name: 'pmo test process governance can use main workspace and loads test automation',
    args: ['--role', 'pmo', '--phase', 'governance', '--task', '完善测试用例自动化测试流程规范', '--paths', 'mango-pmo/rules/09-test-case-automation-flow.md,mango-pmo/templates/delivery-contract.md'],
    mode: 'main-direct-allowed',
    mustRead: ['rules/09-test-case-automation-flow.md']
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
    mode: 'worktree-required',
    level: 'L2'
  },
  {
    name: 'visual-only button move uses micro workflow',
    args: ['--role', 'dev', '--phase', 'develop', '--task', '按钮位置移动一点，只调整 4px 间距', '--paths', 'mango-ui/packages/payment/src/views/channel/index.vue'],
    mode: 'lightweight-branch-allowed',
    level: 'L0',
    obligations: {
      dedicatedWorktree: false,
      detailedDesign: false,
      deliveryPlan: false,
      fullE2E: false,
      screenshot: false,
      formalDeliveryReport: false,
      uiVerification: 'AFFECTED_PAGE_SMOKE'
    }
  },
  {
    name: 'contained single-file fix uses small workflow',
    args: ['--role', 'dev', '--phase', 'develop', '--task', '单文件局部修复错误提示', '--paths', 'mango-ui/packages/payment/src/views/channel/index.vue'],
    mode: 'lightweight-branch-allowed',
    level: 'L1',
    obligations: {
      dedicatedWorktree: false,
      fullE2E: false,
      formalDeliveryReport: false
    }
  },
  {
    name: 'one-line permission change is still high risk',
    args: ['--role', 'dev', '--phase', 'develop', '--task', '按钮权限显隐微调一行', '--paths', 'mango-ui/packages/payment/src/views/channel/index.vue'],
    mode: 'worktree-required',
    level: 'L3',
    obligations: {
      dedicatedWorktree: true,
      proofPath: true,
      targetedMutation: true
    }
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
  },
  {
    name: 'frontend admin module style changes require style governance checks',
    args: ['--role', 'dev', '--phase', 'develop', '--task', '修复 Payment 支付中心 header 样式丢失并接入 admin full', '--paths', 'mango-ui/packages/admin/src/full.ts,mango-ui/packages/payment/style.css,mango-ui/packages/mango-cli/src/index.mjs'],
    mode: 'worktree-required',
    requiredChecks: ['pnpm admin:styles:check', 'pnpm admin:module-styles:check']
  },
  {
    name: 'design phase loads test case automation flow',
    args: ['--role', 'tech-lead', '--phase', 'design', '--task', '设计订单管理', '--paths', ''],
    mode: 'needs-human-check',
    mustRead: ['rules/09-test-case-automation-flow.md']
  },
  {
    name: 'current plans path loads delivery contract',
    args: ['--role', 'dev', '--phase', 'develop', '--task', '按 Sprint 计划开发', '--paths', 'mango-docs/plans/2026-07-03-plan.md'],
    mode: 'main-direct-allowed',
    mustRead: ['rules/01-delivery-contract.md']
  },
  {
    name: 'current evidence path loads delivery contract',
    args: ['--role', 'dev', '--phase', 'develop', '--task', '按交付记录验证', '--paths', 'mango-docs/evidence/2026-07-03-issue-372-home-management/report.md'],
    mode: 'main-direct-allowed',
    mustRead: ['rules/01-delivery-contract.md']
  },
  {
    name: 'frontend e2e task loads test case automation flow',
    args: ['--role', 'qa', '--phase', 'verify', '--task', '验证 Playwright E2E 自动化测试', '--paths', 'mango-ui/apps/mango-admin/e2e/specs/menu-management.spec.ts'],
    mode: 'worktree-required',
    mustRead: ['rules/09-test-case-automation-flow.md']
  },
  {
    name: 'unknown role fails closed',
    args: ['--role', 'developer', '--phase', 'develop', '--task', '修改代码', '--paths', 'mango/demo'],
    exit: 1,
    error: 'Unknown PMO role'
  },
  {
    name: 'unknown phase fails closed',
    args: ['--role', 'dev', '--phase', 'coding', '--task', '修改代码', '--paths', 'mango/demo'],
    exit: 1,
    error: 'Unknown PMO phase'
  },
  {
    name: 'unknown option fails closed',
    args: ['--role', 'dev', '--phase', 'develop', '--task', '修改代码', '--paths', 'mango/demo', '--skip-quality', 'true'],
    exit: 1,
    error: 'Unknown option'
  },
  {
    name: 'missing task fails closed',
    args: ['--role', 'dev', '--phase', 'develop', '--paths', 'mango/demo'],
    exit: 1,
    error: 'Missing required option "--task"'
  }
];

const failures = [];

for (const item of cases) {
  const result = spawnSync('node', [preflightPath, ...item.args, '--json'], {
    encoding: 'utf8',
    stdio: 'pipe'
  });
  if (item.exit && item.exit !== 0) {
    if (result.status === 0) {
      failures.push(`${item.name}: expected non-zero exit`);
      continue;
    }
    let output;
    try {
      output = JSON.parse(result.stdout);
    } catch {
      failures.push(`${item.name}: expected JSON error output\n${result.stderr || result.stdout}`);
      continue;
    }
    if (!(output.errors || []).some((error) => error.includes(item.error))) {
      failures.push(`${item.name}: expected error containing ${item.error}, got ${(output.errors || []).join('; ')}`);
    }
    continue;
  }
  if (result.status !== 0) {
    failures.push(`${item.name}: preflight exited ${result.status}\n${result.stderr || result.stdout}`);
    continue;
  }
  const output = JSON.parse(result.stdout);
  if (!/^sha256:[0-9a-f]{64}$/.test(output.rulesFingerprint || '')) {
    failures.push(`${item.name}: missing stable rules fingerprint`);
  }
  if (output.workspacePolicy?.mode !== item.mode) {
    failures.push(`${item.name}: expected ${item.mode}, got ${output.workspacePolicy?.mode || '<missing>'}`);
  }
  if (item.level && output.taskProfile?.level !== item.level) {
    failures.push(`${item.name}: expected task level ${item.level}, got ${output.taskProfile?.level || '<missing>'}`);
  }
  for (const [key, expectedValue] of Object.entries(item.obligations || {})) {
    const actualValue = output.taskProfile?.obligations?.[key];
    if (actualValue !== expectedValue) {
      failures.push(`${item.name}: expected obligation ${key}=${expectedValue}, got ${actualValue}`);
    }
  }
  for (const expectedPath of item.mustRead || []) {
    const hasPath = (output.mustRead || []).some((entry) => entry.path === expectedPath);
    if (!hasPath) {
      failures.push(`${item.name}: expected mustRead ${expectedPath}`);
    }
  }
  for (const expectedCommand of item.requiredChecks || []) {
    const hasCommand = (output.requiredChecks || []).some((entry) => (entry.commands || []).includes(expectedCommand));
    if (!hasCommand) {
      failures.push(`${item.name}: expected required check ${expectedCommand}`);
    }
  }
}

if (failures.length > 0) {
  console.error(`PMO preflight check failed:\n${failures.map((failure) => `- ${failure}`).join('\n')}`);
  process.exit(1);
}

console.log(`PMO preflight workspace policy checks passed: ${cases.length}`);
