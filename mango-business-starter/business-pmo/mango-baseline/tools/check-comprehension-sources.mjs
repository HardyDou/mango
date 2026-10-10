#!/usr/bin/env node
import { existsSync, lstatSync, readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const pmoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const repoRoot = path.resolve(pmoRoot, '..');
const datasetPath = path.join(pmoRoot, 'tests/skills/comprehension-evals.json');
const dataset = JSON.parse(readFileSync(datasetPath, 'utf8'));
const failures = [];

for (const item of dataset.cases ?? []) {
  for (const reference of [...(item.sourceRefs ?? []), item.nextSource].filter(Boolean)) {
    const [rawPath, rawAnchor] = String(reference).split('#', 2);
    const file = resolveRepositoryFile(rawPath);
    if (!file) {
      failures.push(`${item.id}: source path is outside repository: ${rawPath}`);
      continue;
    }
    if (!existsSync(file) || !lstatSync(file).isFile() || lstatSync(file).isSymbolicLink()) {
      failures.push(`${item.id}: source file does not exist as a regular file: ${rawPath}`);
      continue;
    }
    if (rawAnchor && !hasEvidence(file, rawAnchor)) {
      failures.push(`${item.id}: source heading or symbol not found: ${reference}`);
    }
  }
}

if (failures.length > 0) {
  console.error('[FAIL] comprehension source references');
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}
console.log(`[PASS] comprehension source references: ${dataset.cases.length} cases`);

function resolveRepositoryFile(rawPath) {
  const file = path.resolve(repoRoot, rawPath);
  const relative = path.relative(repoRoot, file);
  return relative && !relative.startsWith('..') && !path.isAbsolute(relative) ? file : null;
}
function hasEvidence(file, rawAnchor) {
  const wanted = comparable(rawAnchor);
  const lines = readFileSync(file, 'utf8').split(/\r?\n/u);
  return lines.some(line => /^#{1,6}\s+/u.test(line) && comparable(line.replace(/^#{1,6}\s+/u, '')) === wanted)
    || lines.some(line => comparable(line).includes(wanted));
}
function comparable(value) {
  return String(value)
    .replaceAll(/[`*_~\s\p{P}\p{S}]/gu, '')
    .replace(/^\d+(?:\.\d+)*\.?/u, '')
    .toLocaleLowerCase('en-US');
}
