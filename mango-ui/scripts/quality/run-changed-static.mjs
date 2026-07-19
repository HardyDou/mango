#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const uiRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const repositoryRoot = path.resolve(uiRoot, '..');
const repositoryPrefix = 'mango-ui/';
const baseRef = process.argv.find((value) => value.startsWith('--base-ref='))?.slice('--base-ref='.length);
const changedFiles = process.argv
  .slice(2)
  .filter((file) => !file.startsWith('--'))
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
  eslint: changedFiles.filter(
    (file) => /\.(?:[cm]?[jt]s|tsx|vue)$/u.test(file) && !file.startsWith('packages/mango-cli/templates/'),
  ),
  prettier: changedFiles.filter((file) => prettierExtensions.has(path.extname(file))),
  stylelint: changedFiles.filter((file) => /\.(?:css|scss|vue)$/u.test(file)),
};

function existedAtBase(file) {
  if (!baseRef) return false;
  return (
    spawnSync('git', ['cat-file', '-e', `${baseRef}:${repositoryPrefix}${file}`], {
      cwd: repositoryRoot,
      stdio: 'ignore',
    }).status === 0
  );
}

function runGate(tool, files, strict) {
  if (files.length === 0) return;
  const result = spawnSync(
    process.execPath,
    ['./scripts/quality/run-static-gate.mjs', tool, ...(strict ? ['--strict'] : [`--base-ref=${baseRef}`]), ...files],
    {
      cwd: uiRoot,
      stdio: 'inherit',
    },
  );
  if (result.error) throw result.error;
  if (result.signal || result.status !== 0) {
    throw new Error(
      `${tool} changed-file ${strict ? 'strict' : 'ratchet'} gate failed (${result.signal || result.status})`,
    );
  }
}

for (const [tool, files] of Object.entries(selections)) {
  const legacyFiles = files.filter(existedAtBase);
  const strictFiles = files.filter((file) => !existedAtBase(file));
  runGate(tool, legacyFiles, false);
  runGate(tool, strictFiles, true);
}

console.log(`changed-file static gate PASS ${changedFiles.length} existing files`);
