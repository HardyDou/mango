#!/usr/bin/env node
import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { compareMetrics, METRICS } from './static-gate-lib.mjs';

const uiRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const tool = process.argv[2];
const strict = process.argv.includes('--strict');
const baselineFile = path.join(uiRoot, 'quality-baseline.json');
const reportDirectory = path.resolve(uiRoot, '../.runtime/frontend-quality/gate');
const bin = name => path.join(uiRoot, 'node_modules', '.bin', name);

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
  });
}

let raw;
let metrics;
if (tool === 'eslint') {
  const result = run(bin('eslint'), ['apps', 'packages', 'scripts', '--format', 'json']);
  raw = JSON.parse(result.stdout || '[]');
  metrics = raw.reduce((total, file) => ({
    fatal: total.fatal + file.fatalErrorCount,
    errors: total.errors + file.errorCount,
    warnings: total.warnings + file.warningCount,
  }), { fatal: 0, errors: 0, warnings: 0 });
} else if (tool === 'stylelint') {
  const result = run(bin('stylelint'), ['apps/**/*.{css,scss,vue}', 'packages/**/*.{css,scss,vue}', '--formatter', 'json']);
  raw = JSON.parse(result.stdout || result.stderr || '[]');
  metrics = raw.reduce((total, file) => {
    total.parseErrors += file.parseErrors?.length || 0;
    for (const issue of file.warnings || []) {
      if (issue.severity === 'error') total.errors += 1;
      else total.warnings += 1;
    }
    return total;
  }, { parseErrors: 0, errors: 0, warnings: 0 });
} else if (tool === 'prettier') {
  const result = run(bin('prettier'), ['--list-different', '.']);
  raw = (result.stdout || '').split(/\r?\n/u).filter(Boolean).sort();
  metrics = { files: raw.length };
} else {
  const outputFile = path.join(reportDirectory, 'typecheck-raw.json');
  const result = run(process.execPath, ['./scripts/quality/run-typecheck.mjs', `--out=${outputFile}`]);
  if (result.error || !fs.existsSync(outputFile)) {
    process.stderr.write(result.stderr || String(result.error || 'typecheck report missing'));
    process.exit(2);
  }
  raw = JSON.parse(fs.readFileSync(outputFile, 'utf8'));
  metrics = {
    failedWorkspaces: raw.summary.failedCount,
    diagnostics: raw.summary.diagnosticCount,
  };
}

const baseline = JSON.parse(fs.readFileSync(baselineFile, 'utf8'));
const failures = compareMetrics(tool, metrics, baseline.tools[tool], strict);
const report = {
  schemaVersion: 1,
  tool,
  mode: strict ? 'strict' : 'ratchet',
  metrics,
  allowed: strict ? Object.fromEntries(METRICS[tool].map(metric => [metric, 0])) : baseline.tools[tool],
  failures,
  raw,
};
fs.mkdirSync(reportDirectory, { recursive: true });
fs.writeFileSync(path.join(reportDirectory, `${tool}.json`), `${JSON.stringify(report, null, 2)}\n`);
process.stdout.write(`${tool} ${failures.length ? 'FAIL' : 'PASS'} ${JSON.stringify(metrics)} (${report.mode})\n`);
for (const failure of failures) {
  process.stderr.write(`${failure.metric}: ${failure.actual} exceeds ${failure.allowed}\n`);
}
if (failures.length) process.exitCode = 1;
