#!/usr/bin/env node
import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs';
import { dirname, extname, join, relative, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const DEFAULT_ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const DEFAULT_SCAN_ROOTS = ['apps', 'build-config', 'packages', 'scripts'];
const DEFAULT_ALLOWED_FILES = ['apps/mango-admin/public/mockServiceWorker.js', 'packages/common/utils/webCrypto.ts'];
const SOURCE_EXTENSIONS = new Set(['.cjs', '.js', '.mjs', '.ts', '.tsx', '.vue']);
const SKIPPED_DIRECTORIES = new Set(['dist', 'node_modules']);
const DIRECT_CALL_PATTERN = /\.randomUUID\s*\(/gu;

export function findDisallowedRandomUUIDCalls({
  root = DEFAULT_ROOT,
  scanRoots = DEFAULT_SCAN_ROOTS,
  allowedFiles = DEFAULT_ALLOWED_FILES,
} = {}) {
  const allowed = new Set(allowedFiles);
  const violations = [];
  for (const scanRoot of scanRoots) {
    const absoluteScanRoot = resolve(root, scanRoot);
    if (!existsSync(absoluteScanRoot)) {
      continue;
    }
    for (const file of listSourceFiles(absoluteScanRoot)) {
      const relativeFile = relative(root, file).split('\\').join('/');
      if (allowed.has(relativeFile)) {
        continue;
      }
      const source = readFileSync(file, 'utf8');
      for (const match of source.matchAll(DIRECT_CALL_PATTERN)) {
        const line = source.slice(0, match.index).split('\n').length;
        violations.push(`${relativeFile}:${line}`);
      }
    }
  }
  return violations;
}

function listSourceFiles(directory) {
  return readdirSync(directory).flatMap((name) => {
    const path = join(directory, name);
    const stat = statSync(path);
    if (stat.isDirectory()) {
      return SKIPPED_DIRECTORIES.has(name) ? [] : listSourceFiles(path);
    }
    return SOURCE_EXTENSIONS.has(extname(name)) ? [path] : [];
  });
}

function main() {
  const violations = findDisallowedRandomUUIDCalls();
  if (violations.length > 0) {
    process.stderr.write(`Direct randomUUID calls must use the Mango compatibility API:\n${violations.join('\n')}\n`);
    process.exitCode = 1;
    return;
  }
  process.stdout.write('randomUUID compatibility usage check passed.\n');
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main();
}
