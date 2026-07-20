#!/usr/bin/env node
import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import {
  chmodSync,
  copyFileSync,
  existsSync,
  mkdirSync,
  readFileSync,
  readdirSync,
  rmSync,
  statSync,
  writeFileSync,
} from 'node:fs';
import { basename, dirname, extname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const packageRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const repoRoot = resolve(packageRoot, '../../..');
const sourceRoot = join(repoRoot, 'mango-pmo');
const distRoot = join(packageRoot, 'dist');
const baselineRoot = join(distRoot, 'baseline');
const pluginSourceRoot = join(sourceRoot, 'plugin-src');
const packagePluginManifestRoot = join(packageRoot, '.codex-plugin');
const packageSkillsRoot = join(packageRoot, 'skills');
const packageJson = JSON.parse(readFileSync(join(packageRoot, 'package.json'), 'utf8'));
const sourceRoots = [
  { path: 'agents', required: true },
  { path: 'rules', required: true },
  { path: 'templates', required: true },
  { path: 'contracts', required: true },
  { path: 'tools', required: true },
  { path: 'skills', required: true },
];

if (!existsSync(sourceRoot)) {
  throw new Error(`mango-pmo source not found: ${sourceRoot}`);
}

rmSync(distRoot, { recursive: true, force: true });
rmSync(packagePluginManifestRoot, { recursive: true, force: true });
rmSync(packageSkillsRoot, { recursive: true, force: true });
mkdirSync(baselineRoot, { recursive: true });

for (const root of sourceRoots) {
  const source = join(sourceRoot, root.path);
  if (!existsSync(source)) {
    if (root.required) {
      throw new Error(`required PMO source directory missing: ${source}`);
    }
    continue;
  }
  copyTree(source, join(baselineRoot, root.path));
}
copyRegularFile(join(sourceRoot, 'README.md'), join(baselineRoot, 'README.md'));

if (!existsSync(pluginSourceRoot)) {
  throw new Error(`required PMO plugin source directory missing: ${pluginSourceRoot}`);
}
if (!existsSync(join(pluginSourceRoot, '.codex-plugin/plugin.json'))) {
  throw new Error('mango-pmo/plugin-src must provide .codex-plugin/plugin.json');
}
copyTree(join(pluginSourceRoot, '.codex-plugin'), packagePluginManifestRoot);
copyTree(join(baselineRoot, 'skills'), packageSkillsRoot);
const pluginManifestPath = join(packagePluginManifestRoot, 'plugin.json');
const pluginManifest = JSON.parse(readFileSync(pluginManifestPath, 'utf8'));
if (pluginManifest.version !== packageJson.version) {
  throw new Error(
    `PMO plugin source version ${pluginManifest.version} must equal package version ${packageJson.version}`,
  );
}
const pluginFiles = [
  ...describeFiles(packagePluginManifestRoot, 'plugin').map((file) => ({
    ...file,
    path: `.codex-plugin/${file.path}`,
  })),
  ...describeFiles(packageSkillsRoot, 'plugin').map((file) => ({ ...file, path: `skills/${file.path}` })),
].sort((left, right) => compareText(left.path, right.path));
const plugin = {
  path: 'package-root',
  sha256: sha256(Buffer.from(JSON.stringify(pluginFiles), 'utf8')),
  files: pluginFiles,
};

const files = describeFiles(baselineRoot);

const contracts = readContracts(files, baselineRoot);
const bundleSha256 = sha256(Buffer.from(JSON.stringify({ files, contracts, plugin }), 'utf8'));
const manifest = {
  packageName: packageJson.name,
  packageVersion: packageJson.version,
  schemaVersion: 2,
  source: 'mango-pmo',
  sourceCommit: readSourceCommit(),
  bundleSha256,
  contracts,
  plugin,
  files,
};

writeFileSync(join(distRoot, 'baseline.json'), `${JSON.stringify(manifest, null, 2)}\n`);
process.stdout.write(
  `Built ${packageJson.name}@${packageJson.version} bundle ${bundleSha256.slice(0, 12)} with ${files.length} files.\n`,
);

function copyTree(source, target) {
  mkdirSync(target, { recursive: true });
  const entries = readdirSync(source, { withFileTypes: true }).sort((left, right) =>
    compareText(left.name, right.name),
  );
  for (const entry of entries) {
    const sourcePath = join(source, entry.name);
    const targetPath = join(target, entry.name);
    if (entry.isSymbolicLink()) {
      throw new Error(`symbolic links are not allowed in PMO package sources: ${sourcePath}`);
    }
    if (entry.isDirectory()) {
      copyTree(sourcePath, targetPath);
      continue;
    }
    if (!entry.isFile()) {
      throw new Error(`unsupported PMO package source entry: ${sourcePath}`);
    }
    copyRegularFile(sourcePath, targetPath);
  }
}

function copyRegularFile(source, target) {
  const sourceStat = statSync(source);
  if (!sourceStat.isFile()) {
    throw new Error(`PMO package source must be a regular file: ${source}`);
  }
  mkdirSync(dirname(target), { recursive: true });
  copyFileSync(source, target);
  chmodSync(target, sourceStat.mode & 0o111 ? 0o755 : 0o644);
}

function walkFiles(root) {
  const result = [];
  const entries = readdirSync(root, { withFileTypes: true }).sort((left, right) => compareText(left.name, right.name));
  for (const entry of entries) {
    const fullPath = join(root, entry.name);
    if (entry.isSymbolicLink()) {
      throw new Error(`symbolic links are not allowed in built PMO bundles: ${fullPath}`);
    }
    if (entry.isDirectory()) {
      result.push(...walkFiles(fullPath));
    } else if (entry.isFile()) {
      result.push(fullPath);
    } else {
      throw new Error(`unsupported built PMO bundle entry: ${fullPath}`);
    }
  }
  return result;
}

function describeFiles(root, kindOverride = '') {
  return walkFiles(root)
    .map((file) => {
      const content = readFileSync(file);
      const path = toPosix(relative(root, file));
      return {
        path,
        sha256: sha256(content),
        size: content.length,
        kind: kindOverride || classifyFile(path),
        mode: normalizeMode(statSync(file).mode),
      };
    })
    .sort((left, right) => compareText(left.path, right.path));
}

function readContracts(files, root) {
  const contracts = [];
  for (const file of files.filter((entry) => entry.kind === 'contract' && extname(entry.path) === '.json')) {
    const document = JSON.parse(readFileSync(join(root, file.path), 'utf8'));
    const entries = Array.isArray(document.contracts) ? document.contracts : [document];
    for (const entry of entries) {
      const contractId = entry.contractId || entry.id;
      const schemaRevision = entry.schemaRevision ?? entry.revision;
      if (!contractId || !Number.isInteger(schemaRevision) || schemaRevision < 1) {
        throw new Error(`contract JSON must define contractId and positive integer schemaRevision: ${file.path}`);
      }
      const fixedPmoVersion = entry.metadata?.fixed?.pmoVersion;
      if (fixedPmoVersion && fixedPmoVersion !== packageJson.version) {
        throw new Error(
          `contract ${contractId} pmoVersion ${fixedPmoVersion} must equal package version ${packageJson.version}`,
        );
      }
      contracts.push({
        contractId,
        schemaRevision,
        path: file.path,
      });
    }
  }
  contracts.sort((left, right) => compareText(left.contractId, right.contractId));
  const ids = new Set();
  for (const contract of contracts) {
    if (ids.has(contract.contractId)) {
      throw new Error(`duplicate PMO contractId: ${contract.contractId}`);
    }
    ids.add(contract.contractId);
  }
  return contracts;
}

function classifyFile(path) {
  const root = path.split('/')[0];
  return (
    {
      agents: 'agent',
      rules: 'rule',
      templates: 'template',
      contracts: 'contract',
      tools: 'tool',
      skills: 'skill',
    }[root] || (basename(path) === 'README.md' ? 'documentation' : 'asset')
  );
}

function normalizeMode(mode) {
  return mode & 0o111 ? '0755' : '0644';
}

function readSourceCommit() {
  if (process.env.MANGO_PMO_SOURCE_COMMIT) {
    return process.env.MANGO_PMO_SOURCE_COMMIT.trim();
  }
  return execFileSync('git', ['rev-parse', 'HEAD'], {
    cwd: repoRoot,
    encoding: 'utf8',
  }).trim();
}

function sha256(content) {
  return createHash('sha256').update(content).digest('hex');
}

function compareText(left, right) {
  return left < right ? -1 : left > right ? 1 : 0;
}

function toPosix(path) {
  return path.split('\\').join('/');
}
