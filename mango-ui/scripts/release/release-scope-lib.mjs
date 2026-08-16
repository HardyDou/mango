import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';

export const RELEASE_TYPES = new Set(['patch', 'minor', 'major']);
export const RUNTIME_DEPENDENCY_TYPES = ['dependencies', 'optionalDependencies', 'peerDependencies'];

export function indexPublishedPackages(workspaceRoot) {
  const packages = new Map();
  const packagesRoot = join(workspaceRoot, 'packages');
  for (const dir of readdirSync(packagesRoot)) {
    const packageJsonPath = join(packagesRoot, dir, 'package.json');
    if (!existsSync(packageJsonPath)) continue;
    const packageJson = JSON.parse(readFileSync(packageJsonPath, 'utf8'));
    if (!packageJson.name?.startsWith('@mango/') || packageJson.private === true) continue;
    packages.set(packageJson.name, { dir, packageJson, packageJsonPath });
  }
  return packages;
}

export function isReleaseImpactFile(packageFile) {
  if (/(?:^|\/)__tests__\//u.test(packageFile)) return false;
  if (/(?:\.spec|\.test)\.[^/]+$/u.test(packageFile)) return false;
  if (/^(?:coverage|test-results|playwright-report)\//u.test(packageFile)) return false;
  if (packageFile === 'CHANGELOG.md') return false;
  return true;
}

export function directPackageImpact(changedFiles, packageIndex) {
  const packageByDir = new Map([...packageIndex.values()].map((entry) => [entry.dir, entry.packageJson.name]));
  const direct = new Set();
  const classifiedFiles = [];
  for (const file of changedFiles) {
    const normalized = normalizeRepositoryPath(file);
    const packageMatch = normalized.match(/^mango-ui\/packages\/([^/]+)\/(.+)$/u);
    if (packageMatch) {
      const packageName = packageByDir.get(packageMatch[1]);
      if (packageName && isReleaseImpactFile(packageMatch[2])) {
        direct.add(packageName);
        classifiedFiles.push({ file: normalized, packageName, reason: 'published-package-path' });
      }
      continue;
    }
    if (isPublishedPmoSourceFile(normalized)) {
      direct.add('@mango/pmo');
      classifiedFiles.push({ file: normalized, packageName: '@mango/pmo', reason: 'published-pmo-source' });
      continue;
    }
    if (isPublishedCliProjectionFile(normalized)) {
      direct.add('@mango/cli');
      classifiedFiles.push({ file: normalized, packageName: '@mango/cli', reason: 'published-cli-projection' });
    }
  }
  return { direct, classifiedFiles };
}

export function resolveReleaseClosure(initialPackages, packageIndex, managedVersions = {}) {
  const releases = new Set(initialPackages);
  let changed = true;
  while (changed) {
    changed = false;
    for (const [packageName, entry] of packageIndex) {
      if (releases.has(packageName)) continue;
      if (hasExactRuntimeDependency(entry.packageJson, releases)) {
        releases.add(packageName);
        changed = true;
      }
    }
  }
  if (
    packageIndex.has('@mango/cli') &&
    [...releases].some((packageName) => packageName !== '@mango/cli' && managedVersions[packageName])
  ) {
    releases.add('@mango/cli');
  }
  return releases;
}

export function topologicalReleaseOrder(releases, packageIndex, managedVersions = {}) {
  const names = [...releases].sort();
  const incoming = new Map(names.map((name) => [name, new Set()]));
  const outgoing = new Map(names.map((name) => [name, new Set()]));
  for (const dependentName of names) {
    const packageJson = packageIndex.get(dependentName)?.packageJson ?? {};
    for (const dependencyType of RUNTIME_DEPENDENCY_TYPES) {
      for (const [dependencyName, declaredVersion] of Object.entries(packageJson[dependencyType] ?? {})) {
        if (!releases.has(dependencyName) || !isExactWorkspaceVersion(declaredVersion)) continue;
        addEdge(dependencyName, dependentName, incoming, outgoing);
      }
    }
  }
  if (releases.has('@mango/cli')) {
    for (const packageName of names) {
      if (packageName !== '@mango/cli' && managedVersions[packageName]) {
        addEdge(packageName, '@mango/cli', incoming, outgoing);
      }
    }
  }
  const ready = names.filter((name) => incoming.get(name).size === 0).sort();
  const order = [];
  while (ready.length > 0) {
    const current = ready.shift();
    order.push(current);
    for (const dependent of [...outgoing.get(current)].sort()) {
      incoming.get(dependent).delete(current);
      if (incoming.get(dependent).size === 0) insertSorted(ready, dependent);
    }
  }
  if (order.length !== names.length) {
    const blocked = names.filter((name) => !order.includes(name));
    throw new Error(`release dependency cycle detected: ${blocked.join(', ')}`);
  }
  return order;
}

export function parseChangeset(content, file = '<changeset>') {
  const match = content.match(/^---\s*\r?\n([\s\S]*?)\r?\n---\s*\r?\n([\s\S]*)$/u);
  if (!match) throw new Error(`${file}: Changeset must contain YAML frontmatter and a summary`);
  const releases = [];
  for (const rawLine of match[1].split(/\r?\n/u)) {
    const line = rawLine.trim();
    if (!line || line.startsWith('#')) continue;
    const entry = line.match(/^["']?(@mango\/[A-Za-z0-9._-]+)["']?\s*:\s*["']?(patch|minor|major)["']?$/u);
    if (!entry) throw new Error(`${file}: unsupported Changeset entry: ${line}`);
    releases.push({ name: entry[1], type: entry[2] });
  }
  if (releases.length === 0) throw new Error(`${file}: Changeset has no Mango package release`);
  const summary = match[2].trim();
  if (!summary) throw new Error(`${file}: Changeset summary is required`);
  return { releases, summary };
}

export function validateDeclaredReleaseSet({ direct, expected, declared }) {
  const errors = [];
  for (const packageName of direct) {
    if (!declared.has(packageName)) errors.push(`missing Changeset declaration for ${packageName}`);
  }
  for (const packageName of declared) {
    if (!expected.has(packageName)) errors.push(`Changeset declares unrelated package ${packageName}`);
  }
  return errors;
}

export function selectReleaseIntentHead({ head, planChanged, sourceCommit, sourceIsAncestor, projectionReleaseOnly }) {
  if (!planChanged) return head;
  if (!sourceCommit) throw new Error('changed release plan is missing its source commit');
  if (!sourceIsAncestor) throw new Error('release plan source commit is not an ancestor of the final head');
  if (!projectionReleaseOnly) {
    throw new Error('final head contains non-release changes after the release plan source commit');
  }
  return sourceCommit;
}

export function normalizeRepositoryPath(file) {
  const normalized = file.replaceAll('\\', '/').replace(/^\.\//u, '');
  if (normalized.startsWith('packages/')) return `mango-ui/${normalized}`;
  return normalized;
}

export function isPublishedPmoSourceFile(file) {
  return (
    /^mango-pmo\/(agents|rules|templates|contracts|tools|skills|plugin-src)\//u.test(file) ||
    file === 'mango-pmo/README.md'
  );
}

export function isPublishedCliProjectionFile(file) {
  return /^mango-business-starter\/frontend\//u.test(file) || file === 'mango-business-starter/README.md';
}

export function isExactWorkspaceVersion(value) {
  return typeof value === 'string' && value.startsWith('workspace:') && value !== 'workspace:*';
}

function hasExactRuntimeDependency(packageJson, releases) {
  for (const dependencyType of RUNTIME_DEPENDENCY_TYPES) {
    for (const [dependencyName, declaredVersion] of Object.entries(packageJson[dependencyType] ?? {})) {
      if (releases.has(dependencyName) && isExactWorkspaceVersion(declaredVersion)) return true;
    }
  }
  return false;
}

function addEdge(from, to, incoming, outgoing) {
  if (from === to || outgoing.get(from).has(to)) return;
  outgoing.get(from).add(to);
  incoming.get(to).add(from);
}

function insertSorted(values, value) {
  if (values.includes(value)) return;
  values.push(value);
  values.sort();
}
