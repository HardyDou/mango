#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const args = process.argv.slice(2);
const failOnBlock = args.includes('--fail-on-block');
const reportOnly = !failOnBlock;
const includeAll = args.includes('--all');
const changedOnly = args.includes('--changed-only');
const baseIndex = args.indexOf('--base');
const baseRef = baseIndex >= 0 && args[baseIndex + 1] ? args[baseIndex + 1] : 'origin/main';

const scanRoots = args
  .filter((arg, index) => !arg.startsWith('--') && args[index - 1] !== '--base')
  .map((arg) => arg.replace(/\/$/, ''));

const defaultRoots = [
  'mango/mango-platform/mango-auth',
  'mango/mango-platform/mango-identity',
  'mango/mango-platform/mango-authorization',
  'mango/mango-platform/mango-workflow',
  'mango/mango-platform/mango-payment',
  'mango/mango-platform/mango-notice'
];

const roots = scanRoots.length > 0 ? scanRoots : defaultRoots;
const ignoredDirs = new Set(['.git', 'target', 'node_modules', 'dist', 'coverage']);

function walk(dir, results = []) {
  if (!fs.existsSync(dir)) return results;
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (ignoredDirs.has(entry.name)) continue;
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      walk(fullPath, results);
    } else if (fullPath.endsWith('.java') && fullPath.includes(`${path.sep}src${path.sep}test${path.sep}`)) {
      results.push(fullPath);
    }
  }
  return results;
}

function relative(file) {
  return path.relative(root, file).split(path.sep).join('/');
}

function lineNumber(text, index) {
  return text.slice(0, index).split('\n').length;
}

function moduleName(relativePath) {
  const match = relativePath.match(/^mango\/mango-platform\/([^/]+)\/([^/]+)/);
  return match ? `${match[1]}/${match[2]}` : 'unknown';
}

function hasMockito(text) {
  return /\bMockito\b|\bmock\s*\(|\bspy\s*\(|@Mock\b|@MockBean\b|@Spy\b|@SpyBean\b|mockito|\bdoReturn\s*\(|\bgiven\s*\(/i.test(text);
}

function detectIssues(file, text) {
  const issues = [];
  const relativePath = relative(file);
  const className = path.basename(file, '.java');
  const testedName = className
    .replace(/IntegrationTest$/, '')
    .replace(/E2ETest$/, '')
    .replace(/UnitTest$/, '')
    .replace(/Test$/, '');

  const selfMockPattern = new RegExp(
    `(?:mock\\s*\\(\\s*(?:[\\w.]+\\.)?${testedName}\\.class\\s*\\)|spy\\s*\\(\\s*new\\s+${testedName}\\s*\\(|@(Mock|MockBean|Spy|SpyBean)(?:\\s*\\([^)]*\\))?\\s+(?:private\\s+|protected\\s+|public\\s+)?${testedName}\\b)`,
    'm'
  );
  const selfMockMatch = text.match(selfMockPattern);
  if (testedName && selfMockMatch) {
    issues.push({
      severity: 'BLOCK',
      rule: 'mock-tested-target',
      line: lineNumber(text, selfMockMatch.index ?? 0),
      reason: `测试类 ${className} mock 了被测目标 ${testedName}`
    });
  }

  const mapperMockPattern = /(?:mock\s*\(\s*(?:[\w.]+\.)?\w*Mapper\.class\s*\)|@(Mock|MockBean|Spy|SpyBean)(?:\s*\([^)]*\))?\s+(?:private\s+|protected\s+|public\s+)?[\w.]*\w*Mapper\b)/g;
  for (const match of text.matchAll(mapperMockPattern)) {
    const pathRisk = /\/service\/impl\/|\/resource\//.test(relativePath);
    issues.push({
      severity: pathRisk ? 'BLOCK' : 'WARN',
      rule: 'mock-mapper-in-core-path',
      line: lineNumber(text, match.index ?? 0),
      reason: pathRisk
        ? 'service/resource 测试 mock Mapper，不能作为持久化或资源落库验收'
        : '测试 mock Mapper，需确认不声称覆盖真实持久化链路'
    });
  }

  const mockBeanMatch = text.match(/@MockBean\b/);
  if (mockBeanMatch) {
    issues.push({
      severity: 'WARN',
      rule: 'spring-mockbean',
      line: lineNumber(text, mockBeanMatch.index ?? 0),
      reason: '@MockBean 会替换 Spring 容器协作者，需确认没有替换被测链路'
    });
  }

  const spyMatch = text.match(/\bspy\s*\(|@Spy\b|@SpyBean\b|\bdoReturn\s*\([\s\S]{0,120}\)\.when\s*\(|\bgiven\s*\(/);
  if (spyMatch) {
    issues.push({
      severity: 'WARN',
      rule: 'partial-mock-or-bdd-stub',
      line: lineNumber(text, spyMatch.index ?? 0),
      reason: 'partial mock 或 BDD stub 需要确认没有覆盖被测目标核心行为'
    });
  }

  const verifyOnly = /\bverify\s*\(/.test(text) && !/\bassert(?:That|Equals|Null|Throws|True|False)\b/.test(text);
  if (verifyOnly) {
    issues.push({
      severity: 'WARN',
      rule: 'verify-only',
      line: 1,
      reason: '测试主要验证 mock 调用，不能作为业务行为正确的验收依据'
    });
  }

  return issues;
}

const rows = [];
let candidateFiles = [];
if (changedOnly) {
  candidateFiles = changedTestFiles(baseRef);
} else {
  for (const scanRoot of roots) {
    candidateFiles.push(...walk(path.join(root, scanRoot)));
  }
}

for (const file of unique(candidateFiles)) {
  const text = fs.readFileSync(file, 'utf8');
  if (!hasMockito(text) && !includeAll) continue;
  const issues = detectIssues(file, text);
  if (issues.length === 0 && includeAll) {
    rows.push({
      severity: 'OK',
      module: moduleName(relative(file)),
      file: relative(file),
      line: '-',
      rule: 'none',
      reason: 'No high-risk mock pattern detected'
    });
  }
  for (const issue of issues) {
    rows.push({
      severity: issue.severity,
      module: moduleName(relative(file)),
      file: relative(file),
      line: issue.line,
      rule: issue.rule,
      reason: issue.reason
    });
  }
}

rows.sort((a, b) => `${a.severity}:${a.module}:${a.file}:${a.line}`.localeCompare(`${b.severity}:${b.module}:${b.file}:${b.line}`));

console.log('| Severity | Module | File | Line | Rule | Reason |');
console.log('|---|---|---|---:|---|---|');
for (const row of rows) {
  console.log(`| ${row.severity} | ${row.module} | \`${row.file}\` | ${row.line} | ${row.rule} | ${row.reason} |`);
}

const blockCount = rows.filter((row) => row.severity === 'BLOCK').length;
const warnCount = rows.filter((row) => row.severity === 'WARN').length;
console.log(`\nSummary: block=${blockCount}, warn=${warnCount}, reportOnly=${reportOnly}, changedOnly=${changedOnly}, base=${baseRef}`);

if (blockCount > 0 && failOnBlock) {
  process.exit(1);
}

function changedTestFiles(base) {
  const output = [runGitDiff(base), runGitWorkingTreeDiff(), runGitUntracked()].join('\n');
  return output
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .filter((file) => file.endsWith('.java') && file.includes('/src/test/'))
    .filter((file) => roots.some((scanRoot) => file.startsWith(scanRoot)))
    .map((file) => path.join(root, file))
    .filter((file) => fs.existsSync(file));
}

function runGitDiff(base) {
  try {
    return execFileSync('git', ['diff', '--name-only', `${base}...HEAD`, '--'], {
      cwd: root,
      encoding: 'utf8'
    });
  } catch {
    return execFileSync('git', ['diff', '--name-only', 'HEAD', '--'], {
      cwd: root,
      encoding: 'utf8'
    });
  }
}

function runGitWorkingTreeDiff() {
  return execFileSync('git', ['diff', '--name-only', 'HEAD', '--'], {
    cwd: root,
    encoding: 'utf8'
  });
}

function runGitUntracked() {
  return execFileSync('git', ['ls-files', '--others', '--exclude-standard'], {
    cwd: root,
    encoding: 'utf8'
  });
}

function unique(values) {
  return [...new Set(values)];
}
