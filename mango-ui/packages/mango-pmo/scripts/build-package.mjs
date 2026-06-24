#!/usr/bin/env node
import { createHash } from 'node:crypto';
import { copyFileSync, existsSync, mkdirSync, readdirSync, readFileSync, rmSync, statSync, writeFileSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const packageRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const repoRoot = resolve(packageRoot, '../../..');
const sourceRoot = join(repoRoot, 'mango-pmo');
const distRoot = join(packageRoot, 'dist');
const baselineRoot = join(distRoot, 'baseline');
const packageJson = JSON.parse(readFileSync(join(packageRoot, 'package.json'), 'utf8'));
const copiedRoots = ['agents', 'rules', 'templates', 'tools'];

if (!existsSync(sourceRoot)) {
  throw new Error(`mango-pmo source not found: ${sourceRoot}`);
}

rmSync(distRoot, { recursive: true, force: true });
mkdirSync(baselineRoot, { recursive: true });

for (const root of copiedRoots) {
  copyTree(join(sourceRoot, root), join(baselineRoot, root));
}
copyFileSync(join(sourceRoot, 'README.md'), join(baselineRoot, 'README.md'));

const files = walkFiles(baselineRoot)
  .map(file => {
    const content = readFileSync(file);
    return {
      path: toPosix(relative(baselineRoot, file)),
      sha256: createHash('sha256').update(content).digest('hex'),
      size: content.length,
    };
  })
  .sort((left, right) => left.path.localeCompare(right.path));

const manifest = {
  packageName: packageJson.name,
  packageVersion: packageJson.version,
  schemaVersion: 1,
  source: 'mango-pmo',
  generatedAt: new Date().toISOString(),
  files,
};

writeFileSync(join(distRoot, 'baseline.json'), `${JSON.stringify(manifest, null, 2)}\n`);
process.stdout.write(`Built ${packageJson.name}@${packageJson.version} baseline with ${files.length} files.\n`);

function copyTree(source, target) {
  if (!existsSync(source)) {
    throw new Error(`required PMO source directory missing: ${source}`);
  }
  mkdirSync(target, { recursive: true });
  for (const entry of readdirSync(source)) {
    const sourcePath = join(source, entry);
    const targetPath = join(target, entry);
    if (statSync(sourcePath).isDirectory()) {
      copyTree(sourcePath, targetPath);
    } else {
      copyFileSync(sourcePath, targetPath);
    }
  }
}

function walkFiles(root) {
  const result = [];
  for (const entry of readdirSync(root)) {
    const fullPath = join(root, entry);
    if (statSync(fullPath).isDirectory()) {
      result.push(...walkFiles(fullPath));
    } else {
      result.push(fullPath);
    }
  }
  return result;
}

function toPosix(path) {
  return path.split('\\').join('/');
}
