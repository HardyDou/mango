#!/usr/bin/env node
import { existsSync, readFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  collectPackageJsonFiles,
  indexWorkspacePackages,
  parsePackageJsonAllowingTemplates,
  readJson,
  relativePath,
  run,
} from './release-guard-utils.mjs';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const workspaceRoot = resolve(scriptDir, '..');
const repoRoot = resolve(workspaceRoot, '..');
const packagesRoot = join(workspaceRoot, 'packages');
const businessStarterRoot = join(repoRoot, 'mango-business-starter');
const releaseVersionsPath = join(workspaceRoot, 'packages/mango-cli/release-versions.json');
const packageIndex = indexWorkspacePackages(workspaceRoot);
const packageByDir = new Map([...packageIndex.values()].map((entry) => [entry.dir, entry]));
const args = process.argv.slice(2);
const base = readArg('--base') || 'origin/main';
const head = readArg('--head') || 'HEAD';
const includeWorkingTree = !args.includes('--committed-only');
const changedFiles = readChangedFiles(base, head);
const impactPackageNames = new Set();
const mismatches = [];

for (const file of changedFiles) {
  const normalizedFile = normalizeChangedFile(file);
  const match = normalizedFile.match(/^mango-ui\/packages\/([^/]+)\/(.+)$/);
  if (!match) {
    continue;
  }
  const [, packageDir, packageFile] = match;
  const workspacePackage = packageByDir.get(packageDir);
  if (!workspacePackage || !isReleaseImpactFile(packageFile)) {
    continue;
  }
  impactPackageNames.add(workspacePackage.packageJson.name);
}

const affectedPackages = resolveAffectedPackages(impactPackageNames);
const releaseVersions = existsSync(releaseVersionsPath) ? readJson(releaseVersionsPath) : {};

for (const packageName of affectedPackages) {
  const workspacePackage = packageIndex.get(packageName);
  const previousPackageJson = readBasePackageJson(workspacePackage);
  if (!previousPackageJson) {
    continue;
  }
  if (previousPackageJson.version === workspacePackage.packageJson.version) {
    mismatches.push(
      `${packageName}: release-impact changes require package.json version bump (${workspacePackage.packageJson.version})`,
    );
  }
  const lockedVersion = releaseVersions.npm?.[packageName];
  if (lockedVersion !== workspacePackage.packageJson.version) {
    mismatches.push(
      `${packageName}: packages/mango-cli/release-versions.json ${lockedVersion || '<missing>'} != local package ${workspacePackage.packageJson.version}`,
    );
  }
}

for (const packageName of affectedPackages) {
  const workspacePackage = packageIndex.get(packageName);
  for (const dependencyType of ['dependencies', 'peerDependencies', 'devDependencies']) {
    for (const [dependencyName, declaredVersion] of Object.entries(workspacePackage.packageJson[dependencyType] ?? {})) {
      if (!affectedPackages.has(dependencyName) || declaredVersion === 'workspace:*') {
        continue;
      }
      const dependencyPackage = packageIndex.get(dependencyName);
      if (declaredVersion !== dependencyPackage.packageJson.version) {
        mismatches.push(
          `${packageName}: ${dependencyType}.${dependencyName} ${declaredVersion} != affected package ${dependencyPackage.packageJson.version}`,
        );
      }
    }
  }
}

for (const packageJsonPath of collectPackageJsonFiles([
  businessStarterRoot,
  join(workspaceRoot, 'packages/mango-cli/templates'),
])) {
  const packageJson = parsePackageJsonAllowingTemplates(packageJsonPath);
  for (const dependencyType of ['dependencies', 'peerDependencies', 'devDependencies']) {
    for (const [dependencyName, declaredVersion] of Object.entries(packageJson[dependencyType] ?? {})) {
      if (!affectedPackages.has(dependencyName) || declaredVersion.includes('{{') || declaredVersion.startsWith('workspace:')) {
        continue;
      }
      const dependencyPackage = packageIndex.get(dependencyName);
      if (declaredVersion !== dependencyPackage.packageJson.version) {
        mismatches.push(
          `${relativePath(repoRoot, packageJsonPath)}: ${dependencyType}.${dependencyName} ${declaredVersion} != affected package ${dependencyPackage.packageJson.version}`,
        );
      }
    }
  }
}

if (mismatches.length > 0) {
  console.error(`Release impact check failed for ${base}..${head}:\n${mismatches.map((item) => `- ${item}`).join('\n')}`);
  process.exit(1);
}

if (affectedPackages.size === 0) {
  console.log(`No npm package release-impact changes detected for ${base}..${head}.`);
} else {
  console.log(`Release impact check passed for ${base}..${head}.`);
  console.log(`Affected npm packages: ${[...affectedPackages].join(', ')}`);
}

function readArg(name) {
  const arg = args.find((item) => item.startsWith(`${name}=`));
  return arg?.slice(name.length + 1);
}

function readChangedFiles(baseRef, headRef) {
  const mergeBase = run('git', ['merge-base', baseRef, headRef], { capture: true });
  if (mergeBase.status !== 0) {
    console.error(`Cannot find merge-base for ${baseRef} and ${headRef}.`);
    process.exit(1);
  }
  const diff = run('git', ['diff', '--name-only', `${mergeBase.stdout.trim()}..${headRef}`], { capture: true });
  if (diff.status !== 0) {
    console.error(`Cannot read changed files for ${baseRef}..${headRef}.`);
    process.exit(1);
  }
  const files = new Set(diff.stdout.split(/\r?\n/).filter(Boolean));
  if (includeWorkingTree) {
    const workingTreeDiff = run('git', ['diff', '--name-only'], { capture: true });
    const stagedDiff = run('git', ['diff', '--cached', '--name-only'], { capture: true });
    for (const output of [workingTreeDiff.stdout, stagedDiff.stdout]) {
      for (const file of output.split(/\r?\n/).filter(Boolean)) {
        files.add(file);
      }
    }
  }
  return [...files];
}

function normalizeChangedFile(file) {
  if (file.startsWith('mango-ui/')) {
    return file;
  }
  if (file.startsWith('packages/')) {
    return `mango-ui/${file}`;
  }
  return file;
}

function isReleaseImpactFile(packageFile) {
  if (packageFile.startsWith('src/')) {
    return true;
  }
  if (packageFile === 'package.json' || packageFile === 'vite.config.ts' || packageFile === 'README.md') {
    return true;
  }
  if (packageFile.startsWith('scripts/') || packageFile.startsWith('style')) {
    return true;
  }
  return false;
}

function resolveAffectedPackages(initialPackageNames) {
  const affected = new Set(initialPackageNames);
  let changed = true;
  while (changed) {
    changed = false;
    for (const [packageName, workspacePackage] of packageIndex) {
      if (affected.has(packageName)) {
        continue;
      }
      if (hasFixedDependencyOnAffected(workspacePackage.packageJson, affected)) {
        affected.add(packageName);
        changed = true;
      }
    }
  }
  return affected;
}

function hasFixedDependencyOnAffected(packageJson, affected) {
  for (const dependencyType of ['dependencies', 'peerDependencies', 'devDependencies']) {
    for (const [dependencyName, declaredVersion] of Object.entries(packageJson[dependencyType] ?? {})) {
      if (affected.has(dependencyName) && declaredVersion !== 'workspace:*') {
        return true;
      }
    }
  }
  return false;
}

function readBasePackageJson(workspacePackage) {
  const path = `mango-ui/packages/${workspacePackage.dir}/package.json`;
  const result = run('git', ['show', `${base}:${path}`], { capture: true });
  if (result.status !== 0) {
    return null;
  }
  return JSON.parse(result.stdout);
}
