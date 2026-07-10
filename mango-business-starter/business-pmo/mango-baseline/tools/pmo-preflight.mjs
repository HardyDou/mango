#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const pmoRoot = path.resolve(__dirname, '..');
const indexPath = path.join(pmoRoot, 'rules', 'index.json');
const supportedOptions = new Set(['role', 'phase', 'task', 'paths']);

function parseArgs(argv) {
  const args = {
    role: '',
    phase: '',
    task: '',
    paths: '',
    json: false,
    parseErrors: []
  };
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i];
    if (arg === '--json') {
      args.json = true;
    } else if (arg.startsWith('--')) {
      const key = arg.slice(2);
      if (!supportedOptions.has(key)) {
        args.parseErrors.push(`Unknown option "${arg}"`);
        if (argv[i + 1] && !argv[i + 1].startsWith('--')) i += 1;
        continue;
      }
      if (argv[i + 1] === undefined || argv[i + 1].startsWith('--')) {
        args.parseErrors.push(`Missing value for option "${arg}"`);
        continue;
      }
      args[key] = argv[i + 1];
      i += 1;
    } else {
      args.parseErrors.push(`Unexpected positional argument "${arg}"`);
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
  'backend',
  'backend/**',
  'frontend',
  'frontend/**',
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

const highRiskKeywords = [
  '权限', '租户', '安全', '金额', '支付', '事务', '并发', '幂等', 'migration', '数据库结构',
  '公共 api', '公共api', 'schema', '跨服务', '跨模块', '发布', '回滚', '加密', '认证'
];

const behaviorKeywords = [
  '点击事件', '校验', '显隐', '状态', '表单字段', '路由', '菜单', '权限', '接口', 'api',
  '数据', '提交', '保存', '删除', '新增', '逻辑', '请求', '响应'
];

const microVisualKeywords = [
  '移动一点', '挪动一点', '微调', '像素', '间距', '对齐', '颜色', '字号', '按钮位置',
  '纯样式', '视觉调整', '文案调整', '文案修正', '错别字'
];

const smallChangeKeywords = [
  '小修复', '局部修复', '轻量修复', '提示文案', '错误提示', '单组件', '单文件'
];

function looksLikeFile(inputPath) {
  return /\.[a-z0-9-]+$/i.test(inputPath);
}

function isFrontendVisualFile(inputPath) {
  return /\.(?:vue|css|scss|less|sass)$/.test(inputPath.replaceAll('\\', '/'));
}

function classifyTaskProfile(args) {
  const task = normalizeText(args.task);
  const inputPaths = splitPaths(args.paths);
  const highHits = highRiskKeywords.filter((keyword) => task.includes(normalizeText(keyword)));
  const behaviorHits = behaviorKeywords.filter((keyword) => task.includes(normalizeText(keyword)));
  const microHits = microVisualKeywords.filter((keyword) => task.includes(normalizeText(keyword)));
  const smallHits = smallChangeKeywords.filter((keyword) => task.includes(normalizeText(keyword)));
  const codePaths = inputPaths.filter((inputPath) => /(?:mango|backend|frontend|src|scripts|\.github)(?:\/|$)/.test(inputPath.replaceAll('\\', '/')));
  const backendBoundary = inputPaths.some((inputPath) => /(?:Controller|Service|Mapper)\.java$|\/db\/migration\/|pom\.xml$/.test(inputPath));
  const uiAffected = inputPaths.some(isFrontendVisualFile) || /页面|按钮|表单|弹框|抽屉|ui|e2e/.test(task);
  const microEligible = microHits.length > 0
    && behaviorHits.length === 0
    && inputPaths.length > 0
    && inputPaths.length <= 2
    && inputPaths.every(isFrontendVisualFile);

  let level;
  const reasons = [];
  if (highHits.length > 0) {
    level = 'L3';
    reasons.push(`high-risk semantics: ${unique(highHits).join(', ')}`);
  } else if (microEligible) {
    level = 'L0';
    reasons.push(`visual-only micro change: ${unique(microHits).join(', ')}`);
  } else if (backendBoundary || codePaths.length > 3 || /完整流程|业务流程|表单提交|状态写入|controller|service|mapper/.test(task)) {
    level = 'L2';
    if (backendBoundary) reasons.push('backend/API/persistence boundary path');
    if (codePaths.length > 3) reasons.push(`estimated changed paths: ${codePaths.length}`);
    if (reasons.length === 0) reasons.push('multi-node or state-changing behavior');
  } else if (smallHits.length > 0 && inputPaths.length > 0 && inputPaths.length <= 3) {
    level = 'L1';
    reasons.push(`explicit contained change: ${unique(smallHits).join(', ')}`);
  } else if (codePaths.length > 0 || uiAffected) {
    level = 'L2';
    reasons.push('code/page change without enough evidence for lightweight classification');
  } else {
    level = 'L1';
    reasons.push('contained non-runtime or governance change');
  }

  const lightweightWorkspaceEligible = ['L0', 'L1'].includes(level)
    && inputPaths.length > 0
    && inputPaths.length <= 3
    && inputPaths.every(looksLikeFile);
  const obligations = {
    dedicatedWorktree: ['L2', 'L3'].includes(level),
    detailedDesign: level === 'L3',
    deliveryPlan: ['L2', 'L3'].includes(level),
    changedOnlyChecks: level !== 'L0',
    staticReview: true,
    unitTests: ['L2', 'L3'].includes(level) ? 'WHEN_KEY_LOGIC' : (level === 'L1' ? 'WHEN_KEY_LOGIC' : 'NOT_REQUIRED'),
    apiTests: ['L2', 'L3'].includes(level) ? 'WHEN_BACKEND_ENTRY_OR_DATA' : 'NOT_REQUIRED',
    uiVerification: uiAffected ? (level === 'L0' ? 'AFFECTED_PAGE_SMOKE' : 'AFFECTED_FLOW_ONLY') : 'NOT_REQUIRED',
    fullE2E: level === 'L3' && uiAffected,
    screenshot: ['L2', 'L3'].includes(level) && uiAffected,
    proofPath: level === 'L3',
    targetedMutation: level === 'L3',
    latestBaseline: ['L2', 'L3'].includes(level),
    formalDeliveryReport: level === 'L3',
    verificationSummary: level === 'L0' ? 'ONE_LINE' : (level === 'L1' ? 'SHORT' : 'STRUCTURED')
  };
  return {
    level,
    label: { L0: 'MICRO', L1: 'SMALL', L2: 'STANDARD', L3: 'HIGH' }[level],
    provisional: true,
    reasons,
    estimatedPathCount: inputPaths.length,
    lightweightWorkspaceEligible,
    upgradeRule: '交付前按真实 Git diff 复核；范围扩大、行为/数据风险出现或边界不确定时自动升级。',
    obligations
  };
}

function classifyWorkspacePolicy(args, taskProfile) {
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

  if (
    requiredHits.length > 0 &&
    inputPaths.length > 0 &&
    inputPaths.every((inputPath) => directMainPathPatterns.some((pattern) => pathMatches(inputPath, pattern))) &&
    requiredHits.every((hit) => hit === 'task matches service/code/build keywords')
  ) {
    return {
      mode: 'main-direct-allowed',
      summary: '可在主工作区直接修改并提交。',
      reason: `all paths are governance/document entry paths: ${inputPaths.join(', ')}`
    };
  }

  if (inputPaths.length > 0 && inputPaths.every((inputPath) => directMainPathPatterns.some((pattern) => pathMatches(inputPath, pattern)))) {
    return {
      mode: 'main-direct-allowed',
      summary: '可在主工作区直接修改并提交。',
      reason: `all paths are governance/document entry paths: ${inputPaths.join(', ')}`
    };
  }

  if (taskProfile.lightweightWorkspaceEligible) {
    return {
      mode: 'lightweight-branch-allowed',
      summary: '可复用当前干净任务分支/工作区，不强制创建专用 worktree；不得直接绕过仓库 PR 规则。',
      reason: `${taskProfile.level} ${taskProfile.label}: ${taskProfile.reasons.join('; ')}`
    };
  }

  if (requiredHits.length > 0) {
    return {
      mode: 'worktree-required',
      summary: '必须使用任务专用 Git worktree 和任务分支。',
      reason: unique(requiredHits).join('; ')
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

function rulesFingerprint(index) {
  const paths = unique([
    ...(index.always || []).map((entry) => entry.path),
    ...Object.values(index.rules || {}).map((entry) => entry.path)
  ]).sort();
  const hash = crypto.createHash('sha256');
  hash.update(fs.readFileSync(indexPath));
  for (const relativePath of paths) {
    const absolutePath = path.join(pmoRoot, relativePath);
    hash.update(`\n${relativePath}\n`);
    if (fs.existsSync(absolutePath)) hash.update(fs.readFileSync(absolutePath));
  }
  return `sha256:${hash.digest('hex')}`;
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
  const taskProfile = classifyTaskProfile(args);
  const result = {
    role: args.role,
    phase: args.phase,
    task: args.task || '',
    paths: splitPaths(args.paths),
    indexVersion: index.version,
    rulesFingerprint: rulesFingerprint(index),
    taskProfile,
    workspacePolicy: classifyWorkspacePolicy(args, taskProfile),
    mustRead: [],
    requiredChecks: collectRequiredChecks(args),
    errors: [...args.parseErrors],
    seen: new Set()
  };

  if (!args.role) {
    result.errors.push('Missing required option "--role"');
  } else if (!Object.hasOwn(index.roles || {}, args.role)) {
    result.errors.push(`Unknown PMO role "${args.role}"; expected one of: ${Object.keys(index.roles || {}).join(', ')}`);
  }
  if (!args.phase) {
    result.errors.push('Missing required option "--phase"');
  } else if (!Object.hasOwn(index.phases || {}, args.phase)) {
    result.errors.push(`Unknown PMO phase "${args.phase}"; expected one of: ${Object.keys(index.phases || {}).join(', ')}`);
  }
  if (!String(args.task || '').trim()) {
    result.errors.push('Missing required option "--task"');
  }

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
  console.log(`Task profile: ${result.taskProfile.level} ${result.taskProfile.label} (provisional)`);
  console.log(`Task profile reason: ${result.taskProfile.reasons.join('; ')}`);
  console.log('Workflow obligations:');
  for (const [key, value] of Object.entries(result.taskProfile.obligations)) {
    console.log(`- ${key}: ${value}`);
  }
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
