#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import {
  analyzeFrontendBoundaries,
  compareFrontendBoundaryBaselines,
  compareFrontendBoundaryReport,
  createFrontendBoundaryBaseline,
  validateFrontendBoundaryBaseline,
} from './frontend-boundaries-lib.mjs';

const uiRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const repositoryRoot = path.resolve(uiRoot, '..');
const baselineFile = path.join(uiRoot, 'frontend-boundary-baseline.json');
const outputFile = path.join(repositoryRoot, '.runtime/frontend-quality/frontend-boundaries.json');
const baseRef =
  process.argv.find((value) => value.startsWith('--base-ref='))?.slice('--base-ref='.length) || 'origin/main';
const writeBaseline = process.argv.includes('--write-baseline');

function readBaseBaseline() {
  const relative = path.relative(repositoryRoot, baselineFile).split(path.sep).join('/');
  const commit = spawnSync('git', ['rev-parse', '--verify', `${baseRef}^{commit}`], {
    cwd: repositoryRoot,
    encoding: 'utf8',
  });
  if (commit.status !== 0) throw new Error(`Frontend boundary base ref is unavailable: ${baseRef}`);
  const result = spawnSync('git', ['show', `${baseRef}:${relative}`], { cwd: repositoryRoot, encoding: 'utf8' });
  return result.status === 0 ? JSON.parse(result.stdout) : null;
}

try {
  const report = analyzeFrontendBoundaries(uiRoot);
  fs.mkdirSync(path.dirname(outputFile), { recursive: true });
  fs.writeFileSync(outputFile, `${JSON.stringify(report, null, 2)}\n`);
  if (writeBaseline) {
    fs.writeFileSync(baselineFile, `${JSON.stringify(createFrontendBoundaryBaseline(report), null, 2)}\n`);
    console.log(`frontend boundaries BASELINE WRITTEN ${JSON.stringify(report.summary)}`);
    process.exit(0);
  }
  if (!fs.existsSync(baselineFile)) throw new Error(`Frontend boundary baseline is missing: ${baselineFile}`);
  const baseline = JSON.parse(fs.readFileSync(baselineFile, 'utf8'));
  const failures = validateFrontendBoundaryBaseline(baseline);
  failures.push(...compareFrontendBoundaryReport(report, baseline));
  const baseBaseline = readBaseBaseline();
  if (baseBaseline) failures.push(...compareFrontendBoundaryBaselines(baseline, baseBaseline));
  else console.log(`frontend boundary ratchet BOOTSTRAP baseline is not present at ${baseRef}`);
  if (failures.length > 0) throw new Error(failures.join('\n'));
  console.log(`frontend boundaries PASS ${JSON.stringify(report.summary)} sha256=${report.reportSha256}`);
} catch (error) {
  console.error(error instanceof Error ? error.message : error);
  process.exit(1);
}
