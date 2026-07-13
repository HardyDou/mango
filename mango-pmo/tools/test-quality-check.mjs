#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';

const IGNORED_DIRECTORIES = new Set([
  '.git', '.runtime', '.mango', 'node_modules', 'target', 'dist', 'build', 'coverage'
]);
const LITERAL = String.raw`(?:"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|-?\d+(?:\.\d+)?(?:[dDfFlL])?|true|false|null|undefined)`;

function parseArgs(argv) {
  const args = { base: '', paths: '', json: false };
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === '--json') {
      args.json = true;
    } else if (arg === '--base' || arg === '--paths') {
      args[arg.slice(2)] = argv[index + 1] || '';
      index += 1;
    } else {
      throw new Error(`unknown argument: ${arg}`);
    }
  }
  return args;
}

function runGit(root, args, allowFailure = false) {
  try {
    return execFileSync('git', args, {
      cwd: root,
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'ignore']
    }).trim();
  } catch (error) {
    if (allowFailure) {
      return '';
    }
    throw error;
  }
}

function repositoryRoot() {
  return runGit(process.cwd(), ['rev-parse', '--show-toplevel']);
}

function defaultBase(root) {
  for (const candidate of ['main', 'origin/main', 'HEAD^']) {
    if (runGit(root, ['rev-parse', '--verify', candidate], true)) {
      return candidate;
    }
  }
  return '';
}

function changedPaths(root, base) {
  const result = new Set();
  if (base) {
    for (const file of runGit(root, ['diff', '--name-only', '--diff-filter=ACMR', base], true).split(/\r?\n/)) {
      if (file) result.add(file);
    }
  }
  for (const file of runGit(root, ['ls-files', '--others', '--exclude-standard'], true).split(/\r?\n/)) {
    if (file) result.add(file);
  }
  return [...result];
}

function walk(inputPath) {
  if (!fs.existsSync(inputPath)) return [];
  const stat = fs.statSync(inputPath);
  if (stat.isFile()) return [inputPath];
  const files = [];
  for (const entry of fs.readdirSync(inputPath, { withFileTypes: true })) {
    if (entry.isDirectory() && IGNORED_DIRECTORIES.has(entry.name)) continue;
    files.push(...walk(path.join(inputPath, entry.name)));
  }
  return files;
}

function isTestFile(file) {
  const normalized = file.replaceAll('\\', '/');
  const name = path.basename(file);
  return normalized.includes('/src/test/')
    || normalized.includes('/__tests__/')
    || normalized.includes('/e2e/')
    || /(?:Test|Tests)\.java$/.test(name)
    || /\.(?:spec|test)\.[cm]?[jt]sx?$/.test(name);
}

function lineNumber(source, index) {
  return source.slice(0, index).split('\n').length;
}

function addMatches(issues, source, file, ruleId, message, regex) {
  for (const match of source.matchAll(regex)) {
    issues.push({ ruleId, file, line: lineNumber(source, match.index), message });
  }
}

function normalizedLiteral(value) {
  return value.replace(/\s+/g, '').replace(/[dDfFlL]$/, '');
}

function sutName(file) {
  const name = path.basename(file).replace(/\.java$/, '');
  return name.replace(/(?:Integration|Flow|E2E|Unit)?Tests?$/, '');
}

function inspectFile(root, absolutePath) {
  const source = fs.readFileSync(absolutePath, 'utf8');
  const file = path.relative(root, absolutePath).replaceAll('\\', '/');
  const issues = [];

  addMatches(issues, source, file, 'PMO-TEST-001', '恒真断言不能证明任何行为。', /\bassertTrue\s*\(\s*true\s*(?:,\s*[^)]*)?\)/g);
  addMatches(issues, source, file, 'PMO-TEST-001', '恒真断言不能证明任何行为。', /\bassertFalse\s*\(\s*false\s*(?:,\s*[^)]*)?\)/g);
  addMatches(issues, source, file, 'PMO-TEST-001', '恒真断言不能证明任何行为。', /\bexpect\s*\(\s*true\s*\)\s*\.toBeTruthy\s*\(\s*\)/g);
  addMatches(issues, source, file, 'PMO-TEST-001', '恒真断言不能证明任何行为。', /\bexpect\s*\(\s*false\s*\)\s*\.toBeFalsy\s*\(\s*\)/g);

  const javaSameValue = new RegExp(String.raw`\bassert(?:Equals|Same|StrictEqual)\s*\(\s*(${LITERAL})\s*,\s*(${LITERAL})`, 'g');
  for (const match of source.matchAll(javaSameValue)) {
    if (normalizedLiteral(match[1]) === normalizedLiteral(match[2])) {
      issues.push({
        ruleId: 'PMO-TEST-001',
        file,
        line: lineNumber(source, match.index),
        message: '同一个字面量与自身比较属于实现复述式测试。'
      });
    }
  }

  const jsSameValue = new RegExp(String.raw`\bexpect\s*\(\s*(${LITERAL})\s*\)\s*\.(?:toBe|toEqual|toStrictEqual)\s*\(\s*(${LITERAL})\s*\)`, 'g');
  for (const match of source.matchAll(jsSameValue)) {
    if (normalizedLiteral(match[1]) === normalizedLiteral(match[2])) {
      issues.push({
        ruleId: 'PMO-TEST-001',
        file,
        line: lineNumber(source, match.index),
        message: '同一个字面量与自身比较属于实现复述式测试。'
      });
    }
  }

  if (file.endsWith('.java')) {
    const sut = sutName(file);
    if (sut) {
      const escaped = sut.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      const patterns = [
        new RegExp(String.raw`@Mock\b[\s\S]{0,120}?\b${escaped}\s+[A-Za-z_$][\w$]*\s*;`, 'g'),
        new RegExp(String.raw`\bmock\s*\(\s*${escaped}\.class\s*\)`, 'g'),
        new RegExp(String.raw`\bspy\s*\(\s*new\s+${escaped}\b`, 'g')
      ];
      for (const regex of patterns) {
        addMatches(issues, source, file, 'PMO-TEST-002', `测试 ${sut} 时禁止 mock/spy 被测对象本身。`, regex);
      }
    }
  }

  return issues;
}

function collectFiles(root, args) {
  const inputs = args.paths
    ? args.paths.split(',').map((item) => item.trim()).filter(Boolean)
    : changedPaths(root, args.base || defaultBase(root));
  const files = new Set();
  for (const input of inputs) {
    const absolute = path.resolve(root, input);
    for (const file of walk(absolute)) {
      if (isTestFile(file)) files.add(file);
    }
  }
  return [...files].sort();
}

function printText(result) {
  if (result.passed) {
    console.log(`Test quality PASS: ${result.filesChecked} file(s)`);
    return;
  }
  console.error(`Test quality FAIL: ${result.issues.length} issue(s)`);
  for (const issue of result.issues) {
    console.error(`- ${issue.ruleId} ${issue.file}:${issue.line} ${issue.message}`);
  }
}

try {
  const args = parseArgs(process.argv.slice(2));
  const root = repositoryRoot();
  const files = collectFiles(root, args);
  const issues = files.flatMap((file) => inspectFile(root, file));
  const result = { passed: issues.length === 0, filesChecked: files.length, issues };
  if (args.json) {
    console.log(JSON.stringify(result, null, 2));
  } else {
    printText(result);
  }
  process.exit(result.passed ? 0 : 1);
} catch (error) {
  console.error(`Test quality check failed: ${error.message}`);
  process.exit(2);
}
