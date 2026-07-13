#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';

const FORBIDDEN_DIRECTORIES = [
  '.runtime/worktrees',
  '.mango/worktrees',
  '.claude/worktrees',
  '.claude/.runtime',
  'mango-pmo/tmp',
  'tmp',
  'temp'
];

function parseArgs(argv) {
  const args = { root: process.cwd(), json: false };
  for (let index = 0; index < argv.length; index += 1) {
    if (argv[index] === '--json') {
      args.json = true;
    } else if (argv[index] === '--root') {
      args.root = argv[index + 1] || '';
      index += 1;
    } else {
      throw new Error(`unknown argument: ${argv[index]}`);
    }
  }
  return args;
}

function normalize(value) {
  return value.replaceAll('\\', '/');
}

function runGit(root, args) {
  return execFileSync('git', args, {
    cwd: root,
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'ignore']
  }).trim();
}

function registeredWorktrees(root) {
  try {
    return runGit(root, ['worktree', 'list', '--porcelain'])
      .split(/\r?\n/)
      .filter((line) => line.startsWith('worktree '))
      .map((line) => path.resolve(line.slice('worktree '.length)));
  } catch {
    return [];
  }
}

function hasIgnoreRule(content, name) {
  const accepted = new Set([name, `${name}/`, `/${name}`, `/${name}/`]);
  return content
    .split(/\r?\n/)
    .map((line) => line.trim())
    .some((line) => accepted.has(line));
}

function inspect(rootValue) {
  const root = path.resolve(rootValue);
  if (!fs.existsSync(root) || !fs.statSync(root).isDirectory()) {
    throw new Error(`workspace root is not a directory: ${root}`);
  }
  const issues = [];

  for (const relativePath of FORBIDDEN_DIRECTORIES) {
    const candidate = path.join(root, relativePath);
    if (fs.existsSync(candidate) && fs.statSync(candidate).isDirectory()) {
      issues.push({
        ruleId: 'PMO-WORKSPACE-002',
        path: relativePath,
        message: '运行时目录或 worktree 目录位于禁止位置；临时数据应进入 .runtime 的约定子目录，Git worktree 必须位于仓库外。'
      });
    }
  }

  for (const worktreePath of registeredWorktrees(root)) {
    if (worktreePath !== root && worktreePath.startsWith(`${root}${path.sep}`)) {
      issues.push({
        ruleId: 'PMO-WORKSPACE-001',
        path: normalize(path.relative(root, worktreePath)),
        message: 'Git worktree 禁止创建在仓库目录内部。'
      });
    }
  }

  const gitignorePath = path.join(root, '.gitignore');
  const gitignore = fs.existsSync(gitignorePath) ? fs.readFileSync(gitignorePath, 'utf8') : '';
  for (const required of ['.runtime', '.mango']) {
    if (!hasIgnoreRule(gitignore, required)) {
      issues.push({
        ruleId: 'PMO-WORKSPACE-003',
        path: '.gitignore',
        message: `缺少 ${required}/ 本机工作区忽略规则。`
      });
    }
  }

  return { root, passed: issues.length === 0, issues };
}

function printText(result) {
  if (result.passed) {
    console.log(`Workspace layout PASS: ${result.root}`);
    return;
  }
  console.error(`Workspace layout FAIL: ${result.root}`);
  for (const issue of result.issues) {
    console.error(`- ${issue.ruleId} ${issue.path}: ${issue.message}`);
  }
}

try {
  const args = parseArgs(process.argv.slice(2));
  const result = inspect(args.root);
  if (args.json) {
    console.log(JSON.stringify(result, null, 2));
  } else {
    printText(result);
  }
  process.exit(result.passed ? 0 : 1);
} catch (error) {
  console.error(`Workspace layout check failed: ${error.message}`);
  process.exit(2);
}
