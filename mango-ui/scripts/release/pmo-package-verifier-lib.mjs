import { createHash } from 'node:crypto';
import { existsSync, readFileSync, statSync } from 'node:fs';
import { join } from 'node:path';

export function verifyPmoPackageRoot(packageRoot) {
  const manifestPath = join(packageRoot, 'dist/baseline.json');
  const baselineRoot = join(packageRoot, 'dist/baseline');
  if (!existsSync(manifestPath)) throw new Error('PMO package is missing dist/baseline.json');
  const manifest = readJson(manifestPath);
  const packageJson = readJson(join(packageRoot, 'package.json'));
  if (manifest.packageName !== '@mango/pmo' || manifest.packageVersion !== packageJson.version) {
    throw new Error('PMO package manifest identity does not match package.json');
  }
  if (
    manifest.schemaVersion !== 2 ||
    !Array.isArray(manifest.files) ||
    !Array.isArray(manifest.contracts) ||
    !isSha256(manifest.bundleSha256)
  ) {
    throw new Error('PMO package baseline manifest has an unsupported structure');
  }

  const baselineFiles = uniqueEntries(manifest.files, 'baseline manifest');
  for (const path of ['rules/index.json', 'tools/pmo-preflight.mjs', 'agents/03-dev-agent.md']) {
    if (!baselineFiles.has(path) || !existsSync(join(baselineRoot, path))) {
      throw new Error(`PMO package is missing baseline file: ${path}`);
    }
  }
  for (const entry of baselineFiles.values()) {
    verifyManifestFile(packageRoot, `dist/baseline/${entry.path}`, entry, 'baseline');
  }

  const plugin = manifest.plugin;
  if (plugin?.path !== 'package-root' || !Array.isArray(plugin.files) || plugin.files.length === 0) {
    throw new Error('PMO package manifest is missing its package-root Codex plugin projection');
  }
  const pluginFiles = uniqueEntries(plugin.files, 'plugin projection');
  for (const entry of pluginFiles.values()) {
    if (!entry.path.startsWith('.codex-plugin/') && !entry.path.startsWith('skills/')) {
      throw new Error(`PMO package plugin projection contains an out-of-scope path: ${entry.path}`);
    }
  }
  for (const path of [
    '.codex-plugin/plugin.json',
    'skills/mango-pmo-lifecycle/SKILL.md',
    'skills/mango-requirements-business/SKILL.md',
    'skills/mango-requirements-system/SKILL.md',
    'skills/mango-design-technical/SKILL.md',
    'skills/mango-plan-implementation/SKILL.md',
  ]) {
    if (!pluginFiles.has(path) || !existsSync(join(packageRoot, path))) {
      throw new Error(`PMO package is missing package-root plugin file: ${path}`);
    }
  }
  for (const entry of pluginFiles.values()) verifyManifestFile(packageRoot, entry.path, entry, 'plugin');
  for (const [path, baselineEntry] of baselineFiles) {
    if (!path.startsWith('skills/')) continue;
    const projectedEntry = pluginFiles.get(path);
    if (
      !projectedEntry ||
      projectedEntry.sha256 !== baselineEntry.sha256 ||
      projectedEntry.size !== baselineEntry.size ||
      projectedEntry.mode !== baselineEntry.mode
    ) {
      throw new Error(`PMO package project Skill differs from its baseline source: ${path}`);
    }
  }

  const pluginSha256 = sha256(Buffer.from(JSON.stringify(plugin.files), 'utf8'));
  if (plugin.sha256 !== pluginSha256) {
    throw new Error(`PMO package plugin aggregate hash mismatch: expected ${pluginSha256}, got ${plugin.sha256}`);
  }
  const bundleSha256 = sha256(
    Buffer.from(
      JSON.stringify({ files: manifest.files, contracts: manifest.contracts, plugin: manifest.plugin }),
      'utf8',
    ),
  );
  if (manifest.bundleSha256 !== bundleSha256) {
    throw new Error(`PMO package bundle hash mismatch: expected ${bundleSha256}, got ${manifest.bundleSha256}`);
  }

  const pluginManifest = readJson(join(packageRoot, '.codex-plugin/plugin.json'));
  if (pluginManifest.version !== packageJson.version || pluginManifest.skills !== './skills/') {
    throw new Error('PMO package plugin metadata does not match the package version or skills projection');
  }
  return { packageName: packageJson.name, packageVersion: packageJson.version, bundleSha256 };
}

function uniqueEntries(entries, label) {
  const result = new Map(entries.map((entry) => [entry.path, entry]));
  if (result.size !== entries.length) throw new Error(`PMO package ${label} contains duplicate file paths`);
  return result;
}

function verifyManifestFile(packageRoot, packageRelativePath, entry, kind) {
  if (
    !isSafePackagePath(packageRelativePath) ||
    !isSha256(entry?.sha256) ||
    !Number.isInteger(entry.size) ||
    entry.size < 0 ||
    !['0644', '0755'].includes(entry.mode)
  ) {
    throw new Error(`PMO package ${kind} manifest contains an invalid file descriptor: ${packageRelativePath}`);
  }
  const filePath = join(packageRoot, packageRelativePath);
  if (!existsSync(filePath)) throw new Error(`PMO package is missing ${kind} file: ${packageRelativePath}`);
  const content = readFileSync(filePath);
  const actualMode = process.platform === 'win32' ? entry.mode : statSync(filePath).mode & 0o111 ? '0755' : '0644';
  if (content.length !== entry.size || sha256(content) !== entry.sha256 || actualMode !== entry.mode) {
    throw new Error(`PMO package ${kind} file differs from its manifest: ${packageRelativePath}`);
  }
}

function isSafePackagePath(value) {
  return (
    typeof value === 'string' &&
    value.length > 0 &&
    !value.startsWith('/') &&
    !value.includes('\\') &&
    value.split('/').every((segment) => segment && segment !== '.' && segment !== '..')
  );
}

function isSha256(value) {
  return typeof value === 'string' && /^[0-9a-f]{64}$/iu.test(value);
}

function readJson(path) {
  if (!existsSync(path)) throw new Error(`PMO package is missing ${path}`);
  return JSON.parse(readFileSync(path, 'utf8'));
}

function sha256(content) {
  return createHash('sha256').update(content).digest('hex');
}
