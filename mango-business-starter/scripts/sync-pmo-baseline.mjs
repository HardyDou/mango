#!/usr/bin/env node
import { createHash } from 'node:crypto';
import { spawnSync } from 'node:child_process';
import {
  chmodSync,
  copyFileSync,
  existsSync,
  mkdirSync,
  readFileSync,
  readdirSync,
  rmSync,
  statSync,
} from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptRoot = dirname(fileURLToPath(import.meta.url));
const starterRoot = resolve(scriptRoot, '..');
const repoRoot = resolve(starterRoot, '..');
const packageRoot = join(repoRoot, 'mango-ui/packages/mango-pmo');
const packageBuildScript = join(packageRoot, 'scripts/build-package.mjs');
const packageCheckScript = join(packageRoot, 'scripts/check-package.mjs');
const expectedBaselineRoot = join(packageRoot, 'dist/baseline');
const expectedManifestPath = join(packageRoot, 'dist/baseline.json');
const targetBaselineRoot = join(starterRoot, 'business-pmo/mango-baseline');
const mode = process.argv.includes('--write') ? 'write' : 'check';

buildCanonicalPackage();
checkCanonicalPackage();

if (mode === 'write') {
  writeProjection();
}

const differences = compareProjection();
if (differences.length > 0) {
  console.error('Business PMO baseline projection differs from canonical mango-pmo:');
  for (const difference of differences) {
    console.error(`- ${difference}`);
  }
  console.error('Run: node mango-business-starter/scripts/sync-pmo-baseline.mjs --write');
  process.exit(1);
}

const manifest = JSON.parse(readFileSync(expectedManifestPath, 'utf8'));
console.log(
  `Business PMO baseline ${mode === 'write' ? 'synced' : 'checked'}: `
    + `${manifest.packageName}@${manifest.packageVersion}, ${manifest.files.length} managed files.`,
);

function buildCanonicalPackage() {
  const result = spawnSync(process.execPath, [packageBuildScript], {
    cwd: repoRoot,
    encoding: 'utf8',
  });
  if (result.status !== 0) {
    throw new Error(`canonical @mango/pmo build failed:\n${result.stdout}\n${result.stderr}`);
  }
}

function checkCanonicalPackage() {
  const result = spawnSync(process.execPath, [packageCheckScript], {
    cwd: repoRoot,
    encoding: 'utf8',
  });
  if (result.status !== 0) {
    throw new Error(`canonical @mango/pmo check failed:\n${result.stdout}\n${result.stderr}`);
  }
}

function writeProjection() {
  rmSync(targetBaselineRoot, { recursive: true, force: true });
  copyTree(expectedBaselineRoot, targetBaselineRoot);
  copyFileWithMode(expectedManifestPath, join(targetBaselineRoot, 'baseline.json'));
}

function compareProjection() {
  if (!existsSync(targetBaselineRoot)) {
    return ['target baseline directory is missing'];
  }

  const expectedFiles = new Map(
    walkFiles(expectedBaselineRoot).map(path => [toPosix(relative(expectedBaselineRoot, path)), path]),
  );
  expectedFiles.set('baseline.json', expectedManifestPath);
  const actualFiles = new Map(
    walkFiles(targetBaselineRoot).map(path => [toPosix(relative(targetBaselineRoot, path)), path]),
  );
  const differences = [];

  for (const path of expectedFiles.keys()) {
    if (!actualFiles.has(path)) {
      differences.push(`missing ${path}`);
    }
  }
  for (const path of actualFiles.keys()) {
    if (!expectedFiles.has(path)) {
      differences.push(`stale ${path}`);
    }
  }

  for (const [path, expectedFile] of expectedFiles) {
    const actualFile = actualFiles.get(path);
    if (!actualFile) {
      continue;
    }
    const expectedContent = normalizedContent(path, expectedFile);
    const actualContent = normalizedContent(path, actualFile);
    if (expectedContent.length !== actualContent.length
        || sha256(expectedContent) !== sha256(actualContent)) {
      differences.push(`content mismatch ${path}`);
    }
    if (normalizeMode(statSync(expectedFile).mode) !== normalizeMode(statSync(actualFile).mode)) {
      differences.push(`mode mismatch ${path}`);
    }
  }
  return differences.sort(compareText);
}

function normalizedContent(path, file) {
  const content = readFileSync(file);
  if (path !== 'baseline.json') {
    return content;
  }
  const manifest = JSON.parse(content.toString('utf8'));
  if (typeof manifest.sourceCommit !== 'string' || !/^[0-9a-f]{40}$/i.test(manifest.sourceCommit)) {
    throw new Error(`invalid sourceCommit in ${file}`);
  }
  // A checked-in projection necessarily precedes the commit that contains it.
  // Bundle identity is determined by package version, contracts, plugin, files, and bundleSha256.
  manifest.sourceCommit = '<projection-commit>';
  return Buffer.from(`${JSON.stringify(manifest, null, 2)}\n`, 'utf8');
}

function copyTree(source, target) {
  mkdirSync(target, { recursive: true });
  for (const entry of readdirSync(source, { withFileTypes: true }).sort(byName)) {
    const sourcePath = join(source, entry.name);
    const targetPath = join(target, entry.name);
    if (entry.isDirectory()) {
      copyTree(sourcePath, targetPath);
      continue;
    }
    if (!entry.isFile()) {
      throw new Error(`unsupported canonical baseline entry: ${sourcePath}`);
    }
    copyFileWithMode(sourcePath, targetPath);
  }
}

function copyFileWithMode(source, target) {
  mkdirSync(dirname(target), { recursive: true });
  copyFileSync(source, target);
  chmodSync(target, statSync(source).mode & 0o111 ? 0o755 : 0o644);
}

function walkFiles(root) {
  if (!existsSync(root)) {
    return [];
  }
  const result = [];
  for (const entry of readdirSync(root, { withFileTypes: true }).sort(byName)) {
    const path = join(root, entry.name);
    if (entry.isSymbolicLink()) {
      throw new Error(`symbolic links are forbidden in PMO baseline projection: ${path}`);
    }
    if (entry.isDirectory()) {
      result.push(...walkFiles(path));
    } else if (entry.isFile()) {
      result.push(path);
    } else {
      throw new Error(`unsupported PMO baseline projection entry: ${path}`);
    }
  }
  return result;
}

function sha256(content) {
  return createHash('sha256').update(content).digest('hex');
}

function normalizeMode(modeValue) {
  return modeValue & 0o111 ? '0755' : '0644';
}

function toPosix(path) {
  return path.split('\\').join('/');
}

function compareText(left, right) {
  return left < right ? -1 : left > right ? 1 : 0;
}

function byName(left, right) {
  return compareText(left.name, right.name);
}
