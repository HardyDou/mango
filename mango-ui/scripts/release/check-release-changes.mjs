#!/usr/bin/env node
import { existsSync, readFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';
import {
  directPackageImpact,
  indexPublishedPackages,
  parseChangeset,
  resolveReleaseClosure,
  topologicalReleaseOrder,
  validateDeclaredReleaseSet,
} from './release-scope-lib.mjs';

const scriptRoot = dirname(fileURLToPath(import.meta.url));
const workspaceRoot = resolve(scriptRoot, '../..');
const repoRoot = resolve(workspaceRoot, '..');
const args = process.argv.slice(2);
const base = readArg('--base') || 'origin/main';
const head = readArg('--head') || 'HEAD';
const includeWorkingTree = args.includes('--include-working-tree');
const packageIndex = indexPublishedPackages(workspaceRoot);
const managedVersions = readJson(join(workspaceRoot, 'packages/mango-cli/release-versions.json')).npm ?? {};
const legacyPath = join(workspaceRoot, '.changeset/legacy-reconciliation.json');
const legacyChanged = changedPathStatus(base, head, 'mango-ui/.changeset/legacy-reconciliation.json', includeWorkingTree);
const legacy = legacyChanged && existsSync(legacyPath) ? readJson(legacyPath) : null;
const impactBase = legacy?.from?.commit || base;
const changedFiles = gitChangedFiles(impactBase, head, includeWorkingTree);
const impact = directPackageImpact(changedFiles, packageIndex);
const restored = removeRestoredPublishedBaselines(impact.direct, packageIndex, legacy, head, includeWorkingTree);
const expected = resolveReleaseClosure(impact.direct, packageIndex, managedVersions);
const declared = legacy ? new Set(legacy.releases.map((entry) => entry.name)) : readChangedChangesets(base, head, includeWorkingTree);
const errors = validateDeclaredReleaseSet({ direct: impact.direct, expected, declared });
if (legacy) {
  for (const packageName of expected) {
    if (!declared.has(packageName)) {
      errors.push(`legacy reconciliation is missing generated release ${packageName}`);
    }
  }
}
for (const packageName of declared) {
  if (!packageIndex.has(packageName)) errors.push(`unknown published Mango package: ${packageName}`);
}
if (legacy && legacy.oneTime !== true) errors.push('legacy reconciliation must be explicitly oneTime=true');
if (errors.length > 0) {
  console.error(`Release change check failed:\n${errors.map((error) => `- ${error}`).join('\n')}`);
  process.exit(1);
}
const order = topologicalReleaseOrder(expected, packageIndex, managedVersions);
console.log('Release change check PASS');
console.log(`Direct packages: ${[...impact.direct].sort().join(', ') || '<none>'}`);
console.log(`Release closure: ${order.join(' -> ') || '<none>'}`);
if (restored.length > 0) console.log(`Restored published baselines: ${restored.join(', ')}`);
if (legacy) console.log(`Legacy reconciliation: ${legacy.id}`);

function readArg(name) {
  const inline = args.find((arg) => arg.startsWith(`${name}=`));
  return inline?.slice(name.length + 1) || '';
}

function readJson(path) {
  return JSON.parse(readFileSync(path, 'utf8'));
}

function runGit(gitArgs, allowFailure = false) {
  const result = spawnSync('git', gitArgs, { cwd: repoRoot, encoding: 'utf8' });
  if (!allowFailure && result.status !== 0) {
    throw new Error(`git ${gitArgs.join(' ')} failed: ${(result.stderr || result.stdout).trim()}`);
  }
  return result;
}

function gitChangedFiles(baseRef, headRef, includeWorking) {
  const mergeBase = runGit(['merge-base', baseRef, headRef]).stdout.trim();
  const files = new Set(runGit(['diff', '--name-only', `${mergeBase}..${headRef}`]).stdout.split(/\r?\n/u).filter(Boolean));
  if (includeWorking) {
    for (const command of [
      ['diff', '--name-only'],
      ['diff', '--cached', '--name-only'],
      ['ls-files', '--others', '--exclude-standard'],
    ]) {
      for (const file of runGit(command).stdout.split(/\r?\n/u).filter(Boolean)) files.add(file);
    }
  }
  return [...files];
}

function changedPathStatus(baseRef, headRef, path, includeWorking) {
  const tracked = runGit(['diff', '--name-only', `${baseRef}..${headRef}`, '--', path], true).stdout.trim();
  if (tracked) return true;
  if (!includeWorking) return false;
  return runGit(['status', '--porcelain', '--', path]).stdout.trim().length > 0;
}

function readChangedChangesets(baseRef, headRef, includeWorking) {
  const files = new Set(
    runGit(['diff', '--name-only', '--diff-filter=AM', `${baseRef}..${headRef}`, '--', 'mango-ui/.changeset/*.md'], true)
      .stdout.split(/\r?\n/u).filter(Boolean),
  );
  if (includeWorking) {
    for (const file of runGit(['status', '--porcelain', '--', 'mango-ui/.changeset']).stdout.split(/\r?\n/u)) {
      const path = file.slice(3).trim();
      if (path.endsWith('.md') && !path.endsWith('/README.md')) files.add(path);
    }
  }
  const declared = new Set();
  for (const file of files) {
    if (file.endsWith('/README.md')) continue;
    const absolutePath = join(repoRoot, file);
    if (!existsSync(absolutePath)) continue;
    for (const release of parseChangeset(readFileSync(absolutePath, 'utf8'), file).releases) declared.add(release.name);
  }
  return declared;
}

function removeRestoredPublishedBaselines(direct, packages, legacyRecord, headRef, includeWorking) {
  const restored = [];
  for (const descriptor of legacyRecord?.restoredPublishedBaselines ?? []) {
    if (!direct.has(descriptor.name)) continue;
    const entry = packages.get(descriptor.name);
    if (!entry || entry.packageJson.version !== descriptor.version) continue;
    const packagePath = `mango-ui/packages/${entry.dir}`;
    const diff = runGit(['diff', '--quiet', descriptor.gitCommit, headRef, '--', packagePath], true);
    const workingDiff = includeWorking ? runGit(['diff', '--quiet', '--', packagePath], true) : { status: 0 };
    const untracked = includeWorking ? runGit(['ls-files', '--others', '--exclude-standard', '--', packagePath]).stdout.trim() : '';
    if (diff.status === 0 && workingDiff.status === 0 && !untracked) {
      direct.delete(descriptor.name);
      restored.push(descriptor.name);
    }
  }
  return restored;
}
