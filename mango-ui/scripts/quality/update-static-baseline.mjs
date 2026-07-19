#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import {
  collectDiagnosticIdentities,
  compareStaticBaselines,
  METRICS,
  validateStaticBaseline,
} from './static-gate-lib.mjs';

const uiRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const repositoryRoot = path.resolve(uiRoot, '..');
const reportRoot = path.resolve(
  process.argv.find((value) => value.startsWith('--reports='))?.slice('--reports='.length) ||
    path.join(uiRoot, '../.runtime/frontend-quality/gate'),
);
const baselineFile = path.join(uiRoot, 'quality-baseline.json');
const baseRef =
  process.argv.find((value) => value.startsWith('--base-ref='))?.slice('--base-ref='.length) || 'origin/main';

if (!process.argv.includes('--write')) {
  throw new Error('baseline update requires explicit --write');
}

const baseline = JSON.parse(fs.readFileSync(baselineFile, 'utf8'));
baseline.schemaVersion = 2;
baseline.identities = {};
for (const tool of Object.keys(METRICS)) {
  const reportFile = path.join(reportRoot, `${tool}.json`);
  const report = JSON.parse(fs.readFileSync(reportFile, 'utf8'));
  if (!report.raw || !report.metrics) throw new Error(`${tool} report is missing raw diagnostics or metrics`);
  baseline.tools[tool] = Object.fromEntries(
    METRICS[tool].map((metric) => [metric, Number(report.metrics[metric] || 0)]),
  );
  baseline.identities[tool] = collectDiagnosticIdentities(tool, report.raw, uiRoot, fs.readFileSync);
}
const relative = path.relative(repositoryRoot, baselineFile).split(path.sep).join('/');
const commit = spawnSync('git', ['rev-parse', '--verify', `${baseRef}^{commit}`], {
  cwd: repositoryRoot,
  encoding: 'utf8',
});
if (commit.status !== 0) throw new Error(`Static quality base ref is unavailable: ${baseRef}`);
const baseResult = spawnSync('git', ['show', `${baseRef}:${relative}`], { cwd: repositoryRoot, encoding: 'utf8' });
let baseBaseline = baseResult.status === 0 ? JSON.parse(baseResult.stdout) : null;
if (!baseBaseline) {
  const anchor = spawnSync('git', ['log', '-1', '--format=%H', 'HEAD^', '--', relative], {
    cwd: repositoryRoot,
    encoding: 'utf8',
  });
  const anchorRef = anchor.status === 0 ? anchor.stdout.trim() : '';
  if (anchorRef) {
    const anchorResult = spawnSync('git', ['show', `${anchorRef}:${relative}`], {
      cwd: repositoryRoot,
      encoding: 'utf8',
    });
    if (anchorResult.status !== 0) throw new Error(`Static quality bootstrap anchor is unreadable: ${anchorRef}`);
    baseBaseline = JSON.parse(anchorResult.stdout);
  }
}
const failures = validateStaticBaseline(baseline);
if (baseBaseline) {
  failures.push(...validateStaticBaseline(baseBaseline));
  failures.push(...compareStaticBaselines(baseline, baseBaseline));
}
if (failures.length > 0) throw new Error(failures.join('\n'));
fs.writeFileSync(baselineFile, `${JSON.stringify(baseline, null, 2)}\n`);
process.stdout.write(`Static quality identity baseline updated from ${reportRoot}\n`);
