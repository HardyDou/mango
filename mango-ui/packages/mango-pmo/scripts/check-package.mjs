#!/usr/bin/env node
import { createHash } from 'node:crypto';
import { existsSync, readFileSync, statSync } from 'node:fs';
import { join, resolve } from 'node:path';
import { spawnSync } from 'node:child_process';

const packageRoot = resolve(new URL('..', import.meta.url).pathname);
const distRoot = join(packageRoot, 'dist');
const baselineRoot = join(distRoot, 'baseline');
const manifestPath = join(distRoot, 'baseline.json');
const requiredFiles = [
  'rules/00-dev-flow.md',
  'README.md',
  'rules/03-ai-coding-redlines.md',
  'rules/10-executable-quality-contract.md',
  'rules/index.json',
  'schemas/quality-contract.schema.json',
  'fixtures/executable-quality/cases.json',
  'agents/03-dev-agent.md',
  'agents/05-pmo-agent.md',
  'tools/pmo-preflight.mjs',
  'tools/quality-gate.mjs',
  'tools/eval-executable-quality.mjs',
  'tools/delivery-contract-check.mjs',
  'tools/acceptance-evidence-check.mjs',
  'templates/delivery-contract.md',
  'templates/acceptance-evidence.md',
];

if (!existsSync(manifestPath)) {
  throw new Error('dist/baseline.json not found. Run pnpm -F @mango/pmo build first.');
}

const manifest = JSON.parse(readFileSync(manifestPath, 'utf8'));
const manifestFiles = new Map((manifest.files || []).map(file => [file.path, file]));

for (const file of requiredFiles) {
  const path = join(baselineRoot, file);
  if (!existsSync(path)) {
    throw new Error(`required baseline file missing: ${file}`);
  }
  if (!manifestFiles.has(file)) {
    throw new Error(`baseline manifest missing required file: ${file}`);
  }
}

for (const file of manifest.files || []) {
  const path = join(baselineRoot, file.path);
  if (!existsSync(path) || !statSync(path).isFile()) {
    throw new Error(`manifest points to missing file: ${file.path}`);
  }
  const content = readFileSync(path);
  if (content.toString('utf8').endsWith('\n\n')) {
    throw new Error(`baseline file has trailing blank line at EOF: ${file.path}`);
  }
  const actual = createHash('sha256').update(content).digest('hex');
  if (actual !== file.sha256) {
    throw new Error(`manifest hash mismatch: ${file.path}`);
  }
}

const preflight = spawnSync(process.execPath, [
  'tools/pmo-preflight.mjs',
  '--role',
  'dev',
  '--phase',
  'develop',
  '--task',
  '验证 @mango/pmo baseline package',
  '--paths',
  'backend,frontend',
], {
  cwd: baselineRoot,
  encoding: 'utf8',
});

if (preflight.status !== 0) {
  throw new Error(`packaged baseline preflight failed:\n${preflight.stdout}\n${preflight.stderr}`);
}
if (!preflight.stdout.includes('rules/00-dev-flow.md') || !preflight.stdout.includes('rules/03-ai-coding-redlines.md')) {
  throw new Error(`packaged baseline preflight did not load baseline rules:\n${preflight.stdout}`);
}

const qualityGate = spawnSync(process.execPath, ['tools/quality-gate.mjs', '--self-test'], {
  cwd: baselineRoot,
  encoding: 'utf8',
});
if (qualityGate.status !== 0) {
  throw new Error(`packaged baseline quality gate self-test failed:\n${qualityGate.stdout}\n${qualityGate.stderr}`);
}

const qualityBaseline = spawnSync(process.execPath, ['tools/quality-baseline.mjs', 'self-test'], {
  cwd: baselineRoot,
  encoding: 'utf8',
});
if (qualityBaseline.status !== 0) {
  throw new Error(`packaged baseline quality baseline self-test failed:\n${qualityBaseline.stdout}\n${qualityBaseline.stderr}`);
}

process.stdout.write(`Checked ${manifest.packageName}@${manifest.packageVersion} baseline package.\n`);
