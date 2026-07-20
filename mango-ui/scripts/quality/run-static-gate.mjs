#!/usr/bin/env node
import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  assertToolExecution,
  collectDiagnosticIdentities,
  compareIdentityMultisets,
  compareMetrics,
  compareStaticBaselines,
  METRICS,
  validateStaticBaseline,
} from './static-gate-lib.mjs';

const uiRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const repositoryRoot = path.resolve(uiRoot, '..');
const tool = process.argv[2];
const strict = process.argv.includes('--strict');
const baseRef =
  process.argv.find((value) => value.startsWith('--base-ref='))?.slice('--base-ref='.length) || 'origin/main';
const requestedPaths = process.argv.slice(3).filter((value) => value !== '--strict' && !value.startsWith('--'));
const baselineFile = path.join(uiRoot, 'quality-baseline.json');
const reportDirectory = path.resolve(uiRoot, '../.runtime/frontend-quality/gate');
const bin = (name) => path.join(uiRoot, 'node_modules', '.bin', process.platform === 'win32' ? `${name}.cmd` : name);

if (!METRICS[tool]) {
  process.stderr.write(`Usage: run-static-gate.mjs <${Object.keys(METRICS).join('|')}> [--strict]\n`);
  process.exit(2);
}

function run(command, args) {
  return spawnSync(command, args, {
    cwd: uiRoot,
    encoding: 'utf8',
    env: { ...process.env, FORCE_COLOR: '0' },
    maxBuffer: 128 * 1024 * 1024,
    shell: process.platform === 'win32' && /\.cmd$/iu.test(command),
  });
}

function runGit(args) {
  return spawnSync('git', args, {
    cwd: repositoryRoot,
    encoding: 'utf8',
    env: { ...process.env, FORCE_COLOR: '0' },
  });
}

function readBaseBaseline() {
  const relative = path.relative(repositoryRoot, baselineFile).split(path.sep).join('/');
  const commit = runGit(['rev-parse', '--verify', `${baseRef}^{commit}`]);
  if (commit.status !== 0) throw new Error(`Static quality base ref is unavailable: ${baseRef}`);
  const result = runGit(['show', `${baseRef}:${relative}`]);
  if (result.status === 0) return { baseline: JSON.parse(result.stdout), source: baseRef };
  const anchor = runGit(['log', '-1', '--format=%H', 'HEAD^', '--', relative]);
  const anchorRef = anchor.status === 0 ? anchor.stdout.trim() : '';
  if (!anchorRef) return null;
  const anchorResult = runGit(['show', `${anchorRef}:${relative}`]);
  if (anchorResult.status !== 0) throw new Error(`Static quality bootstrap anchor is unreadable: ${anchorRef}`);
  return { baseline: JSON.parse(anchorResult.stdout), source: anchorRef };
}

function parseArrayResult(result, allowedStatuses, name) {
  assertToolExecution(result, allowedStatuses, name);
  const output = result.stdout || result.stderr;
  if (!output.trim()) throw new Error(`${name} returned no machine-readable output`);
  const parsed = JSON.parse(output);
  if (!Array.isArray(parsed) || parsed.length === 0) throw new Error(`${name} scanned zero inputs`);
  return parsed;
}

let raw;
let metrics;
if (tool === 'eslint') {
  const result = run(bin('eslint'), [
    ...(requestedPaths.length ? requestedPaths : ['apps', 'packages', 'scripts']),
    '--format',
    'json',
  ]);
  raw = parseArrayResult(result, [0, 1], 'eslint');
  metrics = raw.reduce(
    (total, file) => ({
      fatal: total.fatal + file.fatalErrorCount,
      errors: total.errors + file.errorCount,
      warnings: total.warnings + file.warningCount,
    }),
    { fatal: 0, errors: 0, warnings: 0 },
  );
} else if (tool === 'stylelint') {
  const result = run(bin('stylelint'), [
    ...(requestedPaths.length ? requestedPaths : ['apps/**/*.{css,scss,vue}', 'packages/**/*.{css,scss,vue}']),
    '--formatter',
    'json',
  ]);
  raw = parseArrayResult(result, [0, 2], 'stylelint');
  metrics = raw.reduce(
    (total, file) => {
      total.parseErrors += file.parseErrors?.length || 0;
      for (const issue of file.warnings || []) {
        if (issue.severity === 'error') total.errors += 1;
        else total.warnings += 1;
      }
      return total;
    },
    { parseErrors: 0, errors: 0, warnings: 0 },
  );
} else if (tool === 'prettier') {
  const result = run(bin('prettier'), ['--list-different', ...(requestedPaths.length ? requestedPaths : ['.'])]);
  assertToolExecution(result, [0, 1], 'prettier');
  if (!fs.existsSync(path.join(uiRoot, 'package.json'))) throw new Error('prettier scanned zero inputs');
  raw = (result.stdout || '').split(/\r?\n/u).filter(Boolean).sort();
  metrics = { files: raw.length };
} else {
  const outputFile = path.join(reportDirectory, 'typecheck-raw.json');
  fs.rmSync(outputFile, { force: true });
  const result = run(process.execPath, ['./scripts/quality/run-typecheck.mjs', `--out=${outputFile}`]);
  assertToolExecution(result, [0], 'typecheck runner');
  if (!fs.existsSync(outputFile)) throw new Error('typecheck report missing');
  raw = JSON.parse(fs.readFileSync(outputFile, 'utf8'));
  if (!raw.summary || raw.summary.targetCount < 1) throw new Error('typecheck scanned zero inputs');
  metrics = {
    failedWorkspaces: raw.summary.failedCount,
    diagnostics: raw.summary.diagnosticCount,
  };
}

const baseline = JSON.parse(fs.readFileSync(baselineFile, 'utf8'));
const baselineFailures = validateStaticBaseline(baseline);
const baseBaselineRecord = readBaseBaseline();
if (baseBaselineRecord) {
  baselineFailures.push(...validateStaticBaseline(baseBaselineRecord.baseline));
  baselineFailures.push(...compareStaticBaselines(baseline, baseBaselineRecord.baseline));
  if (baseBaselineRecord.source !== baseRef) {
    process.stdout.write(`static quality ratchet uses branch bootstrap anchor ${baseBaselineRecord.source}\n`);
  }
} else {
  process.stdout.write(`static quality ratchet BOOTSTRAP baseline is not present at ${baseRef}\n`);
}
const metricFailures = compareMetrics(tool, metrics, baseline.tools[tool], strict);
const identities = collectDiagnosticIdentities(tool, raw, uiRoot, fs.readFileSync);
const baselineIdentities = baseline.identities?.[tool];
if (!strict && !Array.isArray(baselineIdentities)) {
  throw new Error(`quality baseline is missing ${tool} diagnostic identities`);
}
const identityAdditions = compareIdentityMultisets(identities, baselineIdentities, strict);
const failures = [
  ...baselineFailures.map((message) => ({ metric: 'baseline', message })),
  ...metricFailures,
  ...identityAdditions.map((diagnosticIdentity) => ({ metric: 'diagnosticIdentity', diagnosticIdentity })),
];
const report = {
  schemaVersion: 2,
  tool,
  mode: strict ? 'strict' : 'ratchet',
  metrics,
  allowed: strict ? Object.fromEntries(METRICS[tool].map((metric) => [metric, 0])) : baseline.tools[tool],
  failures,
  identities,
  raw,
};
fs.mkdirSync(reportDirectory, { recursive: true });
fs.writeFileSync(path.join(reportDirectory, `${tool}.json`), `${JSON.stringify(report, null, 2)}\n`);
process.stdout.write(`${tool} ${failures.length ? 'FAIL' : 'PASS'} ${JSON.stringify(metrics)} (${report.mode})\n`);
for (const failure of failures) {
  if (failure.metric === 'diagnosticIdentity') {
    process.stderr.write(`new diagnostic identity: ${failure.diagnosticIdentity}\n`);
  } else if (failure.metric === 'baseline') {
    process.stderr.write(`${failure.message}\n`);
  } else {
    process.stderr.write(`${failure.metric}: ${failure.actual} exceeds ${failure.allowed}\n`);
  }
}
if (failures.length) process.exitCode = 1;
