#!/usr/bin/env node
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { analyzeE2eSpecs } from './e2e-selector-governance-lib.mjs';

const uiRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const specRoot = path.join(uiRoot, 'apps/mango-admin-shell/e2e/specs');
const violations = analyzeE2eSpecs(specRoot);

if (violations.length > 0) {
  for (const violation of violations) {
    console.error(`${violation.file}:${violation.line} ${violation.rule}: ${violation.evidence}`);
  }
  process.exit(1);
}
console.log('E2E selector governance PASS: business specs contain no forbidden implementation selectors');
