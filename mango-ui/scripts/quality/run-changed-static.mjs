#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const uiRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const repositoryPrefix = 'mango-ui/';
const changedFiles = process.argv
  .slice(2)
  .filter((file) => file.startsWith(repositoryPrefix))
  .map((file) => file.slice(repositoryPrefix.length))
  .filter((file) => fs.existsSync(path.join(uiRoot, file)) && fs.statSync(path.join(uiRoot, file)).isFile());

const prettierExtensions = new Set([
  '.cjs',
  '.css',
  '.graphql',
  '.html',
  '.js',
  '.json',
  '.jsx',
  '.less',
  '.md',
  '.mdx',
  '.mjs',
  '.mts',
  '.scss',
  '.ts',
  '.tsx',
  '.vue',
  '.yaml',
  '.yml',
]);
const selections = {
  eslint: changedFiles.filter((file) => /\.(?:[cm]?[jt]s|tsx|vue)$/u.test(file)),
  prettier: changedFiles.filter((file) => prettierExtensions.has(path.extname(file))),
  stylelint: changedFiles.filter((file) => /\.(?:css|scss|vue)$/u.test(file)),
};

for (const [tool, files] of Object.entries(selections)) {
  if (files.length === 0) continue;
  const result = spawnSync(process.execPath, ['./scripts/quality/run-static-gate.mjs', tool, '--strict', ...files], {
    cwd: uiRoot,
    stdio: 'inherit',
  });
  if (result.error) throw result.error;
  if (result.signal || result.status !== 0) {
    throw new Error(`${tool} changed-file gate failed (${result.signal || result.status})`);
  }
}

console.log(`changed-file static gate PASS ${changedFiles.length} existing files`);
