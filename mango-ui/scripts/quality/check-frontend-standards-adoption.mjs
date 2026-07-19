#!/usr/bin/env node
import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { validateAdoptionContract } from './frontend-adoption-lib.mjs';

const uiRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const repoRoot = path.resolve(uiRoot, '..');
const contractPath = path.join(uiRoot, 'frontend-standards-adoption.json');
const contract = readJson(contractPath);
const candidateLock = readJson(path.join(uiRoot, contract.candidateLock));
const packageIndex = indexPackages();
const localVersions = Object.fromEntries([...packageIndex].map(([name, entry]) => [name, entry.manifest.version]));
const baseVersions = {};
for (const [name, entry] of packageIndex) {
  const relativeManifest = path.relative(repoRoot, entry.path).split('\\').join('/');
  const result = spawnSync('git', ['show', `${contract.baseRef}:${relativeManifest}`], {
    cwd: repoRoot,
    encoding: 'utf8',
  });
  if (result.status === 0) baseVersions[name] = JSON.parse(result.stdout).version;
}

const report = validateAdoptionContract(contract, {
  candidateVersions: candidateLock.npm ?? {},
  baseVersions,
  localVersions,
});
const outputPath = path.resolve(uiRoot, '../.runtime/frontend-quality/frontend-standards-adoption.json');
fs.mkdirSync(path.dirname(outputPath), { recursive: true });
fs.writeFileSync(
  outputPath,
  `${JSON.stringify({ schemaVersion: 1, baseRef: contract.baseRef, ...report }, null, 2)}\n`,
);

if (report.failures.length > 0) {
  process.stderr.write(
    `frontend standards adoption FAIL\n${report.failures.map((failure) => `- ${failure}`).join('\n')}\n`,
  );
  process.exit(1);
}
process.stdout.write(
  `frontend standards adoption PASS mode=${report.mode} packages=${report.packageCount} stages=${report.stageCount}\n`,
);

function indexPackages() {
  const result = new Map();
  for (const directory of fs.readdirSync(path.join(uiRoot, 'packages'))) {
    const manifestPath = path.join(uiRoot, 'packages', directory, 'package.json');
    if (!fs.existsSync(manifestPath)) continue;
    const manifest = readJson(manifestPath);
    if (manifest.name?.startsWith('@mango/')) result.set(manifest.name, { path: manifestPath, manifest });
  }
  return result;
}

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}
