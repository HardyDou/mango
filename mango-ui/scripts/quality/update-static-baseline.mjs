#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { collectDiagnosticIdentities, METRICS } from './static-gate-lib.mjs';

const uiRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const reportRoot = path.resolve(
  process.argv.find((value) => value.startsWith('--reports='))?.slice('--reports='.length) ||
    path.join(uiRoot, '../.runtime/frontend-quality/gate'),
);
const baselineFile = path.join(uiRoot, 'quality-baseline.json');

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
fs.writeFileSync(baselineFile, `${JSON.stringify(baseline, null, 2)}\n`);
process.stdout.write(`Static quality identity baseline updated from ${reportRoot}\n`);
