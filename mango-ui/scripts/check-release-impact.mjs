#!/usr/bin/env node
import { existsSync, readFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  collectPackageJsonFiles,
  indexWorkspacePackages,
  parsePackageJsonAllowingTemplates,
  readJson,
  readReleaseContracts,
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
if (args.includes('--self-test')) {
  runSelfTest();
  console.log('Release impact self-test passed.');
  process.exit(0);
}
const changedFiles = readChangedFiles(base, head);
const impactPackageNames = new Set();
const mismatches = [];

for (const file of changedFiles) {
  const normalizedFile = normalizeChangedFile(file);
  if (isPublishedPmoSourceFile(normalizedFile)) {
    impactPackageNames.add('@mango/pmo');
    continue;
  }
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

const releaseVersions = existsSync(releaseVersionsPath) ? readJson(releaseVersionsPath) : {};
const releaseContracts = readReleaseContracts(workspaceRoot);
const restoredPackages = [];

for (const packageName of impactPackageNames) {
  const workspacePackage = packageIndex.get(packageName);
  if (matchesPublishedSourceBaseline(workspacePackage, releaseContracts[packageName]?.sourceBaseline)) {
    impactPackageNames.delete(packageName);
    restoredPackages.push(packageName);
  }
}

const affectedPackages = resolveAffectedPackages(impactPackageNames);

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
    for (const [dependencyName, declaredVersion] of Object.entries(
      workspacePackage.packageJson[dependencyType] ?? {},
    )) {
      if (!affectedPackages.has(dependencyName) || declaredVersion === 'workspace:*') {
        continue;
      }
      const dependencyPackage = packageIndex.get(dependencyName);
      if (!matchesWorkspaceVersion(declaredVersion, dependencyPackage.packageJson.version)) {
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
      if (
        !affectedPackages.has(dependencyName) ||
        declaredVersion.includes('{{') ||
        declaredVersion.startsWith('workspace:')
      ) {
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
  console.error(
    `Release impact check failed for ${base}..${head}:\n${mismatches.map((item) => `- ${item}`).join('\n')}`,
  );
  process.exit(1);
}

if (affectedPackages.size === 0) {
  console.log(`No npm package release-impact changes detected for ${base}..${head}.`);
} else {
  console.log(`Release impact check passed for ${base}..${head}.`);
  console.log(`Affected npm packages: ${[...affectedPackages].join(', ')}`);
}
if (restoredPackages.length > 0) {
  console.log(`Restored published source baselines: ${restoredPackages.join(', ')}`);
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
    const untracked = run('git', ['ls-files', '--others', '--exclude-standard'], { capture: true });
    for (const output of [workingTreeDiff.stdout, stagedDiff.stdout, untracked.stdout]) {
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
  if (/^(?:src|utils|components|hooks|api|types|views)\//u.test(packageFile)) {
    if (/(?:^|\/)__tests__\//u.test(packageFile) || /(?:\.spec|\.test)\.[^/]+$/u.test(packageFile)) {
      return false;
    }
    return true;
  }
  if (
    packageFile === 'package.json' ||
    packageFile === 'release-versions.json' ||
    packageFile === 'vite.config.ts' ||
    packageFile === 'README.md'
  ) {
    return true;
  }
  if (packageFile.startsWith('scripts/') || packageFile.startsWith('style')) {
    return true;
  }
  return false;
}

function isPublishedPmoSourceFile(file) {
  return (
    /^mango-pmo\/(agents|rules|templates|contracts|tools|skills|plugin-src)\//.test(file) ||
    file === 'mango-pmo/README.md'
  );
}

function runSelfTest() {
  if (!isReleaseImpactFile('release-versions.json')) {
    throw new Error('release-versions.json changes must require an @mango/cli version bump');
  }
  if (!isReleaseImpactFile('utils/menuTree.ts') || isReleaseImpactFile('utils/__tests__/menuTree.spec.ts')) {
    throw new Error('published utility source must require impact while utility tests remain non-release changes');
  }
  const published = [
    'mango-pmo/rules/00-dev-flow.md',
    'mango-pmo/contracts/business-requirements.contract.json',
    'mango-pmo/skills/mango-requirements-business/SKILL.md',
    'mango-pmo/plugin-src/.codex-plugin/plugin.json',
    'mango-pmo/README.md',
  ];
  const ignored = ['mango-pmo/baselines/mango-check/no-new-violations-baseline.json', 'mango-docs/plans/example.md'];
  for (const file of published) {
    if (!isPublishedPmoSourceFile(file)) {
      throw new Error(`expected published PMO source impact: ${file}`);
    }
  }
  for (const file of ignored) {
    if (isPublishedPmoSourceFile(file)) {
      throw new Error(`expected non-package PMO source to be ignored: ${file}`);
    }
  }
  if (!matchesWorkspaceVersion('1.2.3', '1.2.3') || !matchesWorkspaceVersion('workspace:1.2.3', '1.2.3')) {
    throw new Error('exact and workspace-exact versions must match the local package');
  }
  if (matchesWorkspaceVersion('workspace:*', '1.2.3') || matchesWorkspaceVersion('^1.2.3', '1.2.3')) {
    throw new Error('floating workspace and semver ranges must not match an exact release version');
  }
  if (!hasFixedDependencyOnAffected({ dependencies: { '@mango/base': 'workspace:1.2.3' } }, new Set(['@mango/base']))) {
    throw new Error('workspace-exact dependencies must cascade release impact');
  }
  if (
    !isSourceBaselineDescriptor({ version: '1.2.3', gitCommit: 'a'.repeat(40) }) ||
    isSourceBaselineDescriptor({ version: '1.2.3', gitCommit: 'not-a-commit' })
  ) {
    throw new Error('published source baseline descriptors require an exact version and full Git commit');
  }
  if (!hasGitPathEntries('utils/new-source.ts\n') || hasGitPathEntries('\n')) {
    throw new Error('published source baseline comparison must detect untracked package files');
  }
}

function matchesPublishedSourceBaseline(workspacePackage, descriptor) {
  if (!isSourceBaselineDescriptor(descriptor) || descriptor.version !== workspacePackage.packageJson.version) {
    return false;
  }
  const packagePath = `mango-ui/packages/${workspacePackage.dir}`;
  const baselinePackageJson = run('git', ['show', `${descriptor.gitCommit}:${packagePath}/package.json`], {
    capture: true,
  });
  if (baselinePackageJson.status !== 0) {
    throw new Error(`${workspacePackage.packageJson.name}: cannot read source baseline ${descriptor.gitCommit}`);
  }
  const baselinePackage = JSON.parse(baselinePackageJson.stdout);
  if (
    baselinePackage.name !== workspacePackage.packageJson.name ||
    baselinePackage.version !== descriptor.version
  ) {
    throw new Error(
      `${workspacePackage.packageJson.name}: source baseline identity does not match ${descriptor.version}`,
    );
  }
  const untracked = run(
    'git',
    ['ls-files', '--others', '--exclude-standard', '--', packagePath],
    { capture: true },
  );
  if (untracked.status !== 0) {
    throw new Error(`${workspacePackage.packageJson.name}: cannot inspect untracked source baseline files`);
  }
  if (hasGitPathEntries(untracked.stdout)) {
    return false;
  }
  const diff = run('git', ['diff', '--quiet', descriptor.gitCommit, '--', packagePath], { capture: true });
  if (diff.status === 0) {
    return true;
  }
  if (diff.status === 1) {
    return false;
  }
  throw new Error(`${workspacePackage.packageJson.name}: cannot compare source baseline ${descriptor.gitCommit}`);
}

function hasGitPathEntries(output) {
  return output.split(/\r?\n/u).some((entry) => entry.trim().length > 0);
}

function isSourceBaselineDescriptor(descriptor) {
  return (
    descriptor &&
    typeof descriptor.version === 'string' &&
    /^\d+\.\d+\.\d+(?:[-+][0-9A-Za-z.-]+)?$/u.test(descriptor.version) &&
    typeof descriptor.gitCommit === 'string' &&
    /^[0-9a-f]{40}$/u.test(descriptor.gitCommit)
  );
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

function matchesWorkspaceVersion(declaredVersion, localVersion) {
  return declaredVersion === localVersion || declaredVersion === `workspace:${localVersion}`;
}

function readBasePackageJson(workspacePackage) {
  const path = `mango-ui/packages/${workspacePackage.dir}/package.json`;
  const result = run('git', ['show', `${base}:${path}`], { capture: true });
  if (result.status !== 0) {
    return null;
  }
  return JSON.parse(result.stdout);
}
