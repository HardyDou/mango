#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';

const REPORT_DIR = path.join('target', 'surefire-reports');
const TEST_SOURCE_DIR = path.join('src', 'test', 'java');

function parseArgs(argv) {
  const args = { modules: '', root: '', changedSince: '', allowEmpty: false, json: false, listModules: false };
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === '--json') {
      args.json = true;
    } else if (arg === '--allow-empty') {
      args.allowEmpty = true;
    } else if (arg === '--list-modules') {
      args.listModules = true;
    } else if (arg === '--modules' || arg === '--root' || arg === '--changed-since') {
      const key = arg === '--changed-since' ? 'changedSince' : arg.slice(2);
      args[key] = argv[index + 1] || '';
      index += 1;
    } else {
      throw new Error(`unknown argument: ${arg}`);
    }
  }
  return args;
}

function changedModuleDirectories(root, baseRef) {
  const output = execFileSync('git', ['diff', '--name-only', `${baseRef}...HEAD`], {
    cwd: root,
    encoding: 'utf8'
  });
  // git 输出相对仓库根，模块根可能在其子目录（如 mango/），因此先解析仓库根再定位最近的 pom。
  const repositoryRoot = execFileSync('git', ['rev-parse', '--show-toplevel'], {
    cwd: root,
    encoding: 'utf8'
  }).trim();
  const directories = new Set();
  for (const file of output.split('\n').map((item) => item.trim()).filter(Boolean)) {
    let directory = path.dirname(path.resolve(repositoryRoot, file));
    while (directory.startsWith(root)) {
      if (directory !== root && fs.existsSync(path.join(directory, 'pom.xml'))) {
        directories.add(directory);
        break;
      }
      if (directory === root) {
        break;
      }
      directory = path.dirname(directory);
    }
  }
  return [...directories].sort();
}

function listModules(root, modules) {
  if (modules) {
    return modules
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)
      .map((item) => path.resolve(root, item));
  }
  const found = [];
  const walk = (directory) => {
    let entries;
    try {
      entries = fs.readdirSync(directory, { withFileTypes: true });
    } catch {
      return;
    }
    if (entries.some((entry) => entry.isFile() && entry.name === 'pom.xml')) {
      found.push(directory);
    }
    for (const entry of entries) {
      if (!entry.isDirectory()) {
        continue;
      }
      if (entry.name === '.git' || entry.name === 'node_modules' || entry.name === 'target') {
        continue;
      }
      walk(path.join(directory, entry.name));
    }
  };
  walk(root);
  return found;
}

const TEST_FILE_PATTERN = [/^Test.*\.java$/, /.*Tests\.java$/, /.*TestCase\.java$/, /.*Test\.java$/];

// 只统计 Surefire 默认包含规则能识别的测试文件，避免把测试辅助类所在模块误判为「必须执行用例」。
function hasTestSources(moduleDirectory) {
  const sourceDirectory = path.join(moduleDirectory, TEST_SOURCE_DIR);
  if (!fs.existsSync(sourceDirectory)) {
    return false;
  }
  const stack = [sourceDirectory];
  while (stack.length > 0) {
    const current = stack.pop();
    for (const entry of fs.readdirSync(current, { withFileTypes: true })) {
      if (entry.isDirectory()) {
        stack.push(path.join(current, entry.name));
      } else if (TEST_FILE_PATTERN.some((pattern) => pattern.test(entry.name))) {
        return true;
      }
    }
  }
  return false;
}

function sumExecutedTests(moduleDirectory) {
  const reportDirectory = path.join(moduleDirectory, REPORT_DIR);
  if (!fs.existsSync(reportDirectory)) {
    return null;
  }
  let total = 0;
  let reports = 0;
  for (const entry of fs.readdirSync(reportDirectory, { withFileTypes: true })) {
    if (!entry.isFile() || !entry.name.startsWith('TEST-') || !entry.name.endsWith('.xml')) {
      continue;
    }
    const content = fs.readFileSync(path.join(reportDirectory, entry.name), 'utf8');
    const match = content.match(/<testsuite\b[^>]*\btests="(\d+)"/);
    if (match) {
      total += Number.parseInt(match[1], 10);
      reports += 1;
    }
  }
  return { total, reports };
}

function inspect(root, args) {
  const modules = args.changedSince
    ? changedModuleDirectories(root, args.changedSince)
    : listModules(root, args.modules);
  if (args.listModules) {
    for (const moduleDirectory of modules) {
      process.stdout.write(`${path.relative(root, moduleDirectory) || '.'}\n`);
    }
    return null;
  }
  const issues = [];
  const summary = [];
  const warnings = [];
  let inspected = 0;
  for (const moduleDirectory of modules) {
    const relative = path.relative(root, moduleDirectory) || '.';
    if (!hasTestSources(moduleDirectory)) {
      continue;
    }
    inspected += 1;
    const executed = sumExecutedTests(moduleDirectory);
    if (executed === null) {
      issues.push({
        module: relative,
        ruleId: 'SUREFIRE_REPORT_MISSING',
        message: '模块存在 src/test/java，但没有 target/surefire-reports，测试没有被执行'
      });
      continue;
    }
    if (executed.total === 0) {
      issues.push({
        module: relative,
        ruleId: 'SUREFIRE_ZERO_TESTS',
        message: '测试报告存在但执行用例数为 0，通常是 Surefire 版本过低无法发现 JUnit 5 用例'
      });
      continue;
    }
    summary.push({ module: relative, tests: executed.total, reports: executed.reports });
  }
  if (inspected === 0) {
    // 改动没有落在任何含可执行测试的模块上（例如仅改文档），不构成失败，但要留痕。
    warnings.push('没有模块包含可执行测试，本次未校验用例执行数');
  }
  return { passed: issues.length === 0, modules: summary, issues, warnings };
}

function printText(result) {
  for (const warning of result.warnings || []) {
    console.log(`warning: ${warning}`);
  }
  for (const item of result.modules) {
    console.log(`${item.module}: ${item.tests} test(s) in ${item.reports} report(s)`);
  }
  if (result.passed) {
    console.log(`Surefire execution PASS: ${result.modules.length} module(s)`);
    return;
  }
  console.error(`Surefire execution FAIL: ${result.issues.length} issue(s)`);
  for (const issue of result.issues) {
    console.error(`- ${issue.ruleId} ${issue.module} ${issue.message}`);
  }
}

try {
  const args = parseArgs(process.argv.slice(2));
  const root = path.resolve(args.root || process.cwd());
  const result = inspect(root, args);
  if (result === null) {
    process.exit(0);
  }
  if (args.json) {
    console.log(JSON.stringify(result, null, 2));
  } else {
    printText(result);
  }
  process.exit(result.passed ? 0 : 1);
} catch (error) {
  console.error(`Surefire execution check failed: ${error.message}`);
  process.exit(2);
}
