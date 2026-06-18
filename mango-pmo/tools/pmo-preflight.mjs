#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const pmoRoot = path.resolve(__dirname, '..');
const indexPath = path.join(pmoRoot, 'rules', 'index.json');

function parseArgs(argv) {
  const args = {
    role: '',
    phase: '',
    task: '',
    paths: '',
    json: false
  };
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i];
    if (arg === '--json') {
      args.json = true;
    } else if (arg.startsWith('--')) {
      const key = arg.slice(2);
      args[key] = argv[i + 1] ?? '';
      i += 1;
    }
  }
  return args;
}

function readIndex() {
  if (!fs.existsSync(indexPath)) {
    throw new Error(`PMO index not found: ${indexPath}`);
  }
  return JSON.parse(fs.readFileSync(indexPath, 'utf8'));
}

function normalizeText(value) {
  return String(value || '').toLowerCase();
}

function splitPaths(value) {
  return String(value || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

const directMainPathPatterns = [
  'mango-pmo/**',
  'mango-docs/**',
  'AGENTS.md',
  'CLAUDE.md',
  'GEMINI.md'
];

const worktreeRequiredPathPatterns = [
  'mango/**',
  'mango-ui/**',
  'mango-business-starter/**',
  'scripts/**',
  '.github/**',
  'package.json',
  'pnpm-lock.yaml',
  'pom.xml',
  '**/package.json',
  '**/pom.xml',
  '**/src/**',
  '**/db/migration/**'
];

const directMainKeywords = [
  '规范',
  '规则',
  '流程治理',
  '规范治理',
  'agent 入口',
  'agent入口',
  '文档资产',
  '归档边界',
  '交付记录',
  '复盘'
];

const worktreeRequiredKeywords = [
  '代码',
  '接口',
  '数据库',
  'migration',
  '前端页面',
  '页面',
  '构建配置',
  '测试',
  '发布脚本',
  '启动脚本',
  '模板',
  'starter',
  'cli',
  'npm',
  'maven',
  'pom',
  'package.json'
];

function pathMatches(inputPath, pattern) {
  const normalizedPath = inputPath.replaceAll('\\', '/');
  const normalizedPattern = pattern.replaceAll('\\', '/');
  if (normalizedPattern.endsWith('/**') && !normalizedPattern.slice(0, -3).match(/[*?]/)) {
    const prefix = normalizedPattern.slice(0, -3);
    return normalizedPath === prefix || normalizedPath.startsWith(`${prefix}/`);
  }
  if (normalizedPattern.includes('*') || normalizedPattern.includes('?')) {
    const regex = globToRegExp(normalizedPattern);
    return regex.test(normalizedPath);
  }
  return normalizedPath === normalizedPattern || normalizedPath.startsWith(`${normalizedPattern}/`);
}

function anyKeywordMatches(task, keywords) {
  const normalizedTask = normalizeText(task);
  return keywords.some((keyword) => normalizedTask.includes(normalizeText(keyword)));
}

function classifyWorkspacePolicy(args) {
  const inputPaths = splitPaths(args.paths);
  const requiredHits = [];
  const directHits = [];

  if (args.role === 'pmo' || args.phase === 'governance') {
    directHits.push('role/phase is PMO governance');
  }
  if (anyKeywordMatches(args.task, directMainKeywords)) {
    directHits.push('task matches governance/document keywords');
  }
  for (const inputPath of inputPaths) {
    if (worktreeRequiredPathPatterns.some((pattern) => pathMatches(inputPath, pattern))) {
      requiredHits.push(`path ${inputPath}`);
    }
    if (directMainPathPatterns.some((pattern) => pathMatches(inputPath, pattern))) {
      directHits.push(`path ${inputPath}`);
    }
  }
  if (anyKeywordMatches(args.task, worktreeRequiredKeywords)) {
    requiredHits.push('task matches service/code/build keywords');
  }

  if (requiredHits.length > 0) {
    return {
      mode: 'worktree-required',
      summary: '必须使用任务专用 Git worktree 和任务分支。',
      reason: unique(requiredHits).join('; ')
    };
  }

  if (inputPaths.length > 0 && inputPaths.every((inputPath) => directMainPathPatterns.some((pattern) => pathMatches(inputPath, pattern)))) {
    return {
      mode: 'main-direct-allowed',
      summary: '可在主工作区直接修改并提交。',
      reason: `all paths are governance/document entry paths: ${inputPaths.join(', ')}`
    };
  }

  if (directHits.length > 0 && inputPaths.length === 0) {
    return {
      mode: 'main-direct-allowed',
      summary: '可在主工作区直接修改并提交；若实际影响服务代码、接口、数据库、测试、前端页面或构建配置，必须改用任务 worktree。',
      reason: unique(directHits).join('; ')
    };
  }

  return {
    mode: 'needs-human-check',
    summary: '影响范围不足，先确认路径；一旦涉及服务代码、接口、数据库、测试、前端页面或构建配置，必须使用任务 worktree。',
    reason: 'no decisive path or keyword match'
  };
}

function unique(items) {
  return [...new Set(items)];
}

function globToRegExp(pattern) {
  let source = '^';
  for (let i = 0; i < pattern.length; i += 1) {
    const char = pattern[i];
    const next = pattern[i + 1];
    if (char === '*' && next === '*') {
      const after = pattern[i + 2];
      if (after === '/') {
        source += '(?:.*/)?';
        i += 2;
      } else {
        source += '.*';
        i += 1;
      }
    } else if (char === '*') {
      source += '[^/]*';
    } else if (char === '?') {
      source += '[^/]';
    } else if ('\\^$+?.()|{}[]'.includes(char)) {
      source += `\\${char}`;
    } else {
      source += char;
    }
  }
  source += '$';
  return new RegExp(source);
}

function bundleMatches(bundle, args) {
  const task = normalizeText(args.task);
  const inputPaths = splitPaths(args.paths);
  if (Array.isArray(bundle.roles) && bundle.roles.length > 0 && !bundle.roles.includes(args.role)) {
    return false;
  }
  if (Array.isArray(bundle.phases) && bundle.phases.length > 0 && !bundle.phases.includes(args.phase)) {
    return false;
  }
  const keywordHit = (bundle.keywords || []).some((keyword) => task.includes(normalizeText(keyword)));
  const pathHit = inputPaths.some((inputPath) => (bundle.paths || []).some((pattern) => pathMatches(inputPath, pattern)));
  return keywordHit || pathHit;
}

const frontendAdminModuleStyleCheck = {
  id: 'frontend-admin-module-style-governance',
  commands: ['pnpm admin:styles:check', 'pnpm admin:module-styles:check'],
  reason: '前端官方模块、@mango/admin/full、CLI 模块清单或 admin 样式聚合可能变化'
};

const frontendAdminModuleStylePaths = [
  'mango-ui/packages/admin/**',
  'mango-ui/packages/mango-cli/**',
  'mango-ui/packages/*/style.css',
  'mango-ui/packages/*/package.json',
  'mango-ui/packages/*/src/**/admin-pages*',
  'mango-ui/scripts/generate-package-styles.mjs',
  'mango-ui/scripts/check-admin-module-style-governance.mjs',
  'mango-ui/apps/mango-admin/src/main.ts',
  'mango-ui/package.json'
];

const frontendAdminModuleStyleKeywords = [
  '@mango/admin/full',
  'admin full',
  'full preset',
  'style-full',
  'admin-packages',
  'generated-package-styles',
  'module-styles',
  '官方模块',
  '业务模块',
  '模块样式',
  '样式聚合',
  '样式丢失',
  'header 样式',
  'header样式',
  '微前端',
  '单体',
  'mango-cli',
  'cli 模块',
  'CLI 模块'
];

function collectRequiredChecks(args) {
  const inputPaths = splitPaths(args.paths);
  const task = normalizeText(args.task);
  const pathHit = inputPaths.some((inputPath) =>
    frontendAdminModuleStylePaths.some((pattern) => pathMatches(inputPath, pattern)),
  );
  const keywordHit = frontendAdminModuleStyleKeywords.some((keyword) => task.includes(normalizeText(keyword)));

  if (!pathHit && !keywordHit) {
    return [];
  }

  return [frontendAdminModuleStyleCheck];
}

function addRule(result, index, key, source) {
  const rule = index.rules[key];
  if (!rule) {
    result.errors.push(`Unknown PMO rule key "${key}" from ${source}`);
    return;
  }
  if (!result.seen.has(rule.path)) {
    result.seen.add(rule.path);
    result.mustRead.push({
      key,
      path: rule.path,
      reason: rule.reason || source
    });
  }
}

function buildResult(index, args) {
  const result = {
    role: args.role || 'auto',
    phase: args.phase || 'auto',
    task: args.task || '',
    paths: splitPaths(args.paths),
    workspacePolicy: classifyWorkspacePolicy(args),
    mustRead: [],
    requiredChecks: collectRequiredChecks(args),
    errors: [],
    seen: new Set()
  };

  for (const entry of index.always || []) {
    if (!result.seen.has(entry.path)) {
      result.seen.add(entry.path);
      result.mustRead.push({
        key: 'always',
        path: entry.path,
        reason: entry.reason || '全局必读'
      });
    }
  }

  for (const key of index.roles?.[args.role] || []) {
    addRule(result, index, key, `role:${args.role}`);
  }

  for (const key of index.phases?.[args.phase] || []) {
    addRule(result, index, key, `phase:${args.phase}`);
  }

  for (const [bundleName, bundle] of Object.entries(index.bundles || {})) {
    if (bundleMatches(bundle, args)) {
      for (const key of bundle.include || []) {
        addRule(result, index, key, `bundle:${bundleName}`);
      }
    }
  }

  for (const item of result.mustRead) {
    const filePath = path.join(pmoRoot, item.path);
    if (!fs.existsSync(filePath)) {
      result.errors.push(`Missing PMO file: ${item.path}`);
    }
  }

  delete result.seen;
  return result;
}

function printText(result) {
  console.log('PMO Preflight');
  console.log(`Role: ${result.role}`);
  console.log(`Phase: ${result.phase}`);
  if (result.task) {
    console.log(`Task: ${result.task}`);
  }
  if (result.paths.length > 0) {
    console.log(`Paths: ${result.paths.join(', ')}`);
  }
  console.log(`Workspace: ${result.workspacePolicy.mode} - ${result.workspacePolicy.summary}`);
  console.log(`Workspace reason: ${result.workspacePolicy.reason}`);
  console.log('');
  console.log('Must read:');
  result.mustRead.forEach((item, index) => {
    console.log(`${index + 1}. ${item.path} - ${item.reason}`);
  });
  if (result.requiredChecks.length > 0) {
    console.log('');
    console.log('Required checks:');
    result.requiredChecks.forEach((item) => {
      console.log(`- ${item.reason}`);
      item.commands.forEach((command) => console.log(`  ${command}`));
    });
  }
  if (result.errors.length > 0) {
    console.log('');
    console.log('Errors:');
    result.errors.forEach((error) => console.log(`- ${error}`));
  }
}

try {
  const args = parseArgs(process.argv.slice(2));
  const index = readIndex();
  const result = buildResult(index, args);
  if (args.json) {
    console.log(JSON.stringify(result, null, 2));
  } else {
    printText(result);
  }
  process.exit(result.errors.length > 0 ? 1 : 0);
} catch (error) {
  console.error(`PMO preflight failed: ${error.message}`);
  process.exit(1);
}
