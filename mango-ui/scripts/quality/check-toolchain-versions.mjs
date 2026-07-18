#!/usr/bin/env node
import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const uiRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const manifest = JSON.parse(fs.readFileSync(path.join(uiRoot, 'package.json'), 'utf8'));
const governed = [
  'eslint', 'eslint-plugin-vue', 'prettier', 'stylelint', 'typescript', 'vite', 'vitest',
  'vue', 'vue-tsc', '@vitejs/plugin-vue', '@playwright/test', '@vue/compiler-sfc',
];
const result = spawnSync('corepack', ['pnpm@11.14.0', 'list', '-r', '--depth', '0', '--json'], {
  cwd: uiRoot,
  encoding: 'utf8',
  maxBuffer: 64 * 1024 * 1024,
});
if (result.status !== 0) {
  process.stderr.write(result.stderr || result.stdout);
  process.exit(2);
}
const projects = JSON.parse(result.stdout);
const failures = [];
for (const name of governed) {
  const expected = manifest.devDependencies[name];
  const observed = new Set();
  for (const project of projects) {
    for (const field of ['dependencies', 'devDependencies', 'optionalDependencies']) {
      const resolved = project[field]?.[name]?.version;
      if (resolved) observed.add(resolved);
    }
  }
  if (!expected || observed.size !== 1 || !observed.has(expected)) {
    failures.push({ name, expected: expected || null, observed: [...observed].sort() });
  }
}
const report = { schemaVersion: 1, governed, failures };
const output = path.resolve(uiRoot, '../.runtime/frontend-quality/toolchain-versions.json');
fs.mkdirSync(path.dirname(output), { recursive: true });
fs.writeFileSync(output, `${JSON.stringify(report, null, 2)}\n`);
process.stdout.write(`toolchain ${failures.length ? 'FAIL' : 'PASS'} (${governed.length} governed packages)\n`);
if (failures.length) {
  process.stderr.write(`${JSON.stringify(failures, null, 2)}\n`);
  process.exitCode = 1;
}

