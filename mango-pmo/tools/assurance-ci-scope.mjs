#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { validateRiskVerification } from './risk-verification.mjs';

export function assuranceCiScope(markdown) {
  const result = validateRiskVerification(markdown);
  if (result.failures.length > 0) return { failures: result.failures, outputs: null };
  const selections = result.assessment?.assuranceSelections ?? {};
  const outputs = {};
  for (let number = 1; number <= 16; number += 1) {
    const id = `M${String(number).padStart(2, '0')}`;
    outputs[id.toLowerCase()] = ['CREATE', 'REUSE', 'REBUILD', 'ENABLE'].includes(selections[id]);
  }
  return { failures: [], outputs };
}

export function runAssuranceCiScopeCli(argv = process.argv.slice(2)) {
  const bodyIndex = argv.indexOf('--body');
  const bodyPath = bodyIndex >= 0 ? argv[bodyIndex + 1] : process.env.PR_BODY_FILE || '.pr-body.md';
  if (!bodyPath || !fs.existsSync(path.resolve(bodyPath))) {
    process.stderr.write(`PR body file does not exist: ${bodyPath || '<missing>'}\n`);
    return 1;
  }
  const result = assuranceCiScope(fs.readFileSync(path.resolve(bodyPath), 'utf8'));
  if (result.failures.length > 0) {
    for (const failure of result.failures) process.stderr.write(`[FAIL] ${failure}\n`);
    return 1;
  }
  for (const [key, enabled] of Object.entries(result.outputs)) {
    process.stdout.write(`${key}=${enabled}\n`);
  }
  return 0;
}

const invokedPath = process.argv[1] ? path.resolve(process.argv[1]) : '';
if (invokedPath === fileURLToPath(import.meta.url)) {
  process.exitCode = runAssuranceCiScopeCli();
}
