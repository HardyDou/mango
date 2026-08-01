#!/usr/bin/env node
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const currentFile = fileURLToPath(import.meta.url);
const toolDirectory = path.dirname(currentFile);

function resolveRepositoryRoot(start = toolDirectory) {
  try {
    return execFileSync('git', ['rev-parse', '--show-toplevel'], { cwd: start, encoding: 'utf8' }).trim();
  } catch {
    return path.resolve(start, '../..');
  }
}

function normalizeRepositoryPath(value, label) {
  const normalized = value.replaceAll('\\', '/').replace(/^\.\//u, '').replace(/\/$/u, '');
  if (!normalized || path.posix.isAbsolute(normalized) || normalized.split('/').includes('..')) {
    throw new Error(`${label} must be a repository-relative path`);
  }
  return normalized;
}

function parseArgs(argv) {
  const result = { base: '', head: '', frontendRoot: '' };
  for (let index = 0; index < argv.length; index += 1) {
    const option = argv[index];
    if (!['--base', '--head', '--frontend-root'].includes(option)) {
      throw new Error(`unknown option: ${option}`);
    }
    const value = argv[index + 1];
    if (!value || value.startsWith('--')) throw new Error(`missing value for ${option}`);
    result[option === '--frontend-root' ? 'frontendRoot' : option.slice(2)] = value;
    index += 1;
  }
  if (!result.base || !result.head) throw new Error('Use --base <git-ref> --head <git-ref>.');
  return result;
}

function readConfiguredFrontendRoot(repositoryRoot) {
  const configPath = path.join(repositoryRoot, 'mango.config.json');
  if (!fs.existsSync(configPath)) return 'frontend';
  const config = JSON.parse(fs.readFileSync(configPath, 'utf8'));
  const configured = config.paths?.frontend;
  return typeof configured === 'string' && configured.trim() ? configured.trim() : 'frontend';
}

function changedVueViews(repositoryRoot, base, head, frontendRoot) {
  const output = execFileSync(
    'git',
    ['diff', '--name-status', '--diff-filter=ACMR', '-z', `${base}...${head}`],
    { cwd: repositoryRoot, encoding: 'utf8' },
  );
  const parts = output.split('\0').filter(Boolean);
  const records = [];
  for (let index = 0; index < parts.length; index += 2) {
    const status = parts[index];
    const file = parts[index + 1];
    if (!file) throw new Error(`Cannot parse changed file after status ${status}`);
    if (status.startsWith('R')) {
      const renamedFile = parts[index + 2];
      if (!renamedFile) throw new Error(`Cannot parse renamed file after ${file}`);
      records.push({ status: 'R', file: renamedFile });
      index += 1;
      continue;
    }
    records.push({ status: status[0], file });
  }
  return records.filter(({ file }) =>
    file.startsWith(`${frontendRoot}/`) && file.endsWith('.vue') && file.includes('/views/'));
}

function hasException(content, kind) {
  const pattern = new RegExp(
    `<!--\\s*mango-page-baseline-exception\\s+${kind}:\\s*([^\\n-][^\\n]{9,})\\s*-->`,
    'iu',
  );
  return pattern.test(content);
}

function hasAny(content, patterns) {
  return patterns.some(pattern => pattern.test(content));
}

export function evaluateVuePageBaseline(file, content, status = 'M') {
  const failures = [];
  const isListPage = hasAny(content, [/<el-table\b/iu, /<ElTable\b/u]);
  if (isListPage && !hasException(content, 'list')) {
    for (const component of ['MangoListPage', 'MangoSearchPanel', 'MangoListPanel', 'Pagination']) {
      if (!content.includes(component)) failures.push(`${file}: list page must use ${component}`);
    }
  }

  if (hasAny(content, [/<el-dialog\b/iu, /<ElDialog\b/u]) && !hasException(content, 'dialog')) {
    failures.push(`${file}: standard dialog must use MangoDialog`);
  }

  const detailPath = /(?:^|\/)(?:detail|details)(?:\/|\.|$)|Detail(?:Page|View)?\.vue$/u.test(file);
  const formPath = /(?:^|\/)(?:form|create|edit)(?:\/|\.|$)|Form(?:Page|View)?\.vue$/u.test(file);
  if (detailPath && hasAny(content, [/<el-descriptions\b/iu, /<ElDescriptions\b/u]) && !hasException(content, 'detail')) {
    for (const component of ['MangoDetailPage', 'MangoPageSection']) {
      if (!content.includes(component)) failures.push(`${file}: independent detail page must use ${component}`);
    }
  }
  if (formPath && hasAny(content, [/<el-form\b/iu, /<ElForm\b/u]) && !hasException(content, 'form')) {
    for (const component of ['MangoFormPage', 'MangoPageSection']) {
      if (!content.includes(component)) failures.push(`${file}: independent form page must use ${component}`);
    }
  }
  return failures;
}

export function checkChangedFrontendPages({ repositoryRoot, base, head, frontendRoot }) {
  const normalizedRoot = normalizeRepositoryPath(frontendRoot, 'frontend root');
  const absoluteRoot = path.resolve(repositoryRoot, normalizedRoot);
  if (!absoluteRoot.startsWith(`${path.resolve(repositoryRoot)}${path.sep}`) || !fs.existsSync(absoluteRoot)) {
    throw new Error(`frontend root does not exist inside repository: ${normalizedRoot}`);
  }
  const records = changedVueViews(repositoryRoot, base, head, normalizedRoot);
  const failures = [];
  for (const record of records) {
    const absoluteFile = path.join(repositoryRoot, record.file);
    if (!fs.existsSync(absoluteFile)) continue;
    failures.push(...evaluateVuePageBaseline(record.file, fs.readFileSync(absoluteFile, 'utf8'), record.status));
  }
  return { records, failures };
}

export function runFrontendPageBaselineCli(argv = process.argv.slice(2)) {
  try {
    const args = parseArgs(argv);
    const repositoryRoot = resolveRepositoryRoot();
    const frontendRoot = args.frontendRoot || readConfiguredFrontendRoot(repositoryRoot);
    const result = checkChangedFrontendPages({ repositoryRoot, ...args, frontendRoot });
    if (result.failures.length > 0) {
      process.stderr.write(`Frontend page baseline FAIL:\n${result.failures.map(item => `- ${item}`).join('\n')}\n`);
      return 1;
    }
    process.stdout.write(`Frontend page baseline PASS: ${result.records.length} changed view files checked.\n`);
    return 0;
  } catch (error) {
    process.stderr.write(`Frontend page baseline FAIL: ${error.message}\n`);
    return 1;
  }
}

if (process.argv[1] && path.resolve(process.argv[1]) === currentFile) {
  process.exitCode = runFrontendPageBaselineCli();
}
