import { createHash } from 'node:crypto';
import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';
import {
  directPackageImpact,
  parseChangeset,
  resolveReleaseClosure,
  topologicalReleaseOrder,
  validateDeclaredReleaseSet,
} from './release-scope-lib.mjs';
import { hasMavenReleaseImpact } from './release-maven-lib.mjs';

const BUMP_WEIGHT = { patch: 1, minor: 2, major: 3 };

export function sha256(value) {
  return createHash('sha256').update(value).digest('hex');
}

export function canonicalJson(value) {
  if (Array.isArray(value)) return value.map(canonicalJson);
  if (!value || typeof value !== 'object') return value;
  return Object.fromEntries(
    Object.keys(value)
      .sort()
      .map((key) => [key, canonicalJson(value[key])]),
  );
}

export function digestJson(value) {
  return sha256(Buffer.from(JSON.stringify(canonicalJson(value)), 'utf8'));
}

export function bumpVersion(version, type) {
  const match = String(version).match(/^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$/u);
  if (!match) throw new Error(`unsupported release version: ${version}`);
  let [, major, minor, patch] = match.map(Number);
  if (type === 'major') {
    major += 1;
    minor = 0;
    patch = 0;
  } else if (type === 'minor') {
    minor += 1;
    patch = 0;
  } else if (type === 'patch') {
    patch += 1;
  } else {
    throw new Error(`unsupported release type: ${type}`);
  }
  return `${major}.${minor}.${patch}`;
}

export function resolveReleaseMavenSourceVersion(previousPlan, managedVersion = '') {
  return previousPlan?.maven?.sourceVersion || managedVersion;
}

export function readPendingChangesets(workspaceRoot) {
  const root = join(workspaceRoot, '.changeset');
  if (!existsSync(root)) return [];
  return readdirSync(root)
    .filter(isPendingChangesetFile)
    .sort()
    .map((file) => {
      const content = readFileSync(join(root, file), 'utf8');
      return {
        id: file.slice(0, -3),
        file: `.changeset/${file}`,
        sha256: sha256(Buffer.from(content, 'utf8')),
        ...parseChangeset(content, `.changeset/${file}`),
      };
    });
}

export function isPendingChangesetFile(file) {
  return file.endsWith('.md') && !['README.md', 'release-notes-template.md'].includes(file);
}

export function buildReleasePlan({
  packageIndex,
  managedVersions,
  mavenSourceVersion = '',
  mavenTargetVersion = '',
  source,
  sourceFiles,
  changesets = [],
  legacy = null,
  restoredPublishedBaselines = [],
  ignoredDirectPackages = [],
  previousPlan = null,
  baseline,
  release = null,
  generatedAt = new Date().toISOString(),
}) {
  assertReleasePlanSourceShape({ source, sourceFiles });
  const impact = directPackageImpact(sourceFiles, packageIndex);
  const mavenImpact = hasMavenReleaseImpact(sourceFiles);
  for (const name of ignoredDirectPackages) impact.direct.delete(name);
  const releaseDeclarations = legacy
    ? legacy.releases.map((entry) => ({
        name: entry.name,
        type: entry.type,
        summary: entry.reason,
        source: `legacy:${legacy.id}`,
        fromVersion: entry.fromVersion,
        toVersion: entry.toVersion,
      }))
    : changesets.flatMap((changeset) =>
        changeset.releases.map((release) => ({
          ...release,
          summary: changeset.summary,
          source: `changeset:${changeset.id}`,
        })),
      );
  const declared = new Set(releaseDeclarations.map((entry) => entry.name));
  const expected = resolveReleaseClosure(impact.direct, packageIndex, managedVersions);
  if (mavenImpact && packageIndex.has('@mango/cli')) expected.add('@mango/cli');
  const errors = validateDeclaredReleaseSet({ direct: impact.direct, expected, declared });
  if (legacy) {
    for (const name of expected) {
      if (!declared.has(name)) errors.push(`legacy reconciliation is missing generated release ${name}`);
    }
  }
  for (const name of declared) {
    if (!packageIndex.has(name)) errors.push(`unknown published Mango package: ${name}`);
  }
  if (errors.length > 0) throw new Error(errors.join('\n'));
  const maven = resolveMavenRelease({
    mavenImpact,
    sourceVersion: mavenSourceVersion,
    targetVersion: mavenTargetVersion,
  });

  const order = topologicalReleaseOrder(expected, packageIndex, managedVersions);
  const previousByName = new Map((previousPlan?.packages ?? []).map((entry) => [entry.name, entry]));
  const declarationsByName = groupDeclarations(releaseDeclarations);
  const packages = order.map((name) => {
    const workspacePackage = packageIndex.get(name);
    const declarations = declarationsByName.get(name) ?? [];
    const legacyDeclaration = legacy?.releases.find((entry) => entry.name === name);
    const type = strongestType(declarations.map((entry) => entry.type)) || 'patch';
    const previous = previousByName.get(name);
    const sourceVersion =
      legacyDeclaration?.fromVersion || previous?.sourceVersion || workspacePackage.packageJson.version;
    const targetVersion = legacyDeclaration?.toVersion || previous?.targetVersion || bumpVersion(sourceVersion, type);
    if (bumpVersion(sourceVersion, type) !== targetVersion) {
      throw new Error(`${name}: ${sourceVersion} + ${type} must produce ${targetVersion}`);
    }
    return {
      name,
      directory: `packages/${workspacePackage.dir}`,
      sourceVersion,
      targetVersion,
      type,
      direct: impact.direct.has(name),
      generated: !declared.has(name),
      reasons:
        declarations.length > 0
          ? declarations.map((entry) => ({ source: entry.source, summary: entry.summary }))
          : [{ source: 'dependency-closure', summary: generatedReason(name, expected, packageIndex, managedVersions) }],
      releaseDependencies: releaseDependencies(name, expected, packageIndex, managedVersions),
    };
  });

  const unsigned = {
    schemaVersion: 1,
    generatedAt,
    releaseKind: maven ? (packages.length > 0 ? 'mixed' : 'maven-only') : 'npm-only',
    release,
    baseline,
    source,
    sourceFiles,
    changesets: changesets.map(({ id, file, sha256: hash }) => ({ id, file, sha256: hash })),
    legacyReconciliation: legacy
      ? { id: legacy.id, oneTime: legacy.oneTime === true, file: '.changeset/legacy-reconciliation.json' }
      : null,
    restoredPublishedBaselines,
    directPackages: [...impact.direct].sort(),
    classifiedFiles: impact.classifiedFiles.sort((a, b) => a.file.localeCompare(b.file)),
    packages,
    order,
    maven,
  };
  const planDigest = digestJson({ ...unsigned, generatedAt: undefined });
  return { ...unsigned, planDigest };
}

function resolveMavenRelease({ mavenImpact, sourceVersion, targetVersion }) {
  if (!mavenImpact && targetVersion) throw new Error('Maven target version was supplied without Maven source impact');
  if (!mavenImpact) return null;
  if (!sourceVersion) throw new Error('Maven source impact requires the current release matrix version');
  if (!targetVersion) throw new Error('Maven source impact requires --maven-version');
  const releasePattern = /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:[-+][0-9A-Za-z][0-9A-Za-z.-]*)?$/u;
  if (!releasePattern.test(targetVersion) || targetVersion.toUpperCase().endsWith('-SNAPSHOT')) {
    throw new Error(`unsupported Maven release version: ${targetVersion}`);
  }
  if (sourceVersion === targetVersion)
    throw new Error('Maven target version must differ from the published source version');
  return {
    sourceVersion,
    targetVersion,
    scope: 'all-non-app',
    docsBundle: true,
    reason: 'Git impact includes Mango Maven production sources',
  };
}

export function assertReleasePlanShape(plan) {
  if (plan?.schemaVersion !== 1) throw new Error('release plan schemaVersion must be 1');
  if (!/^[0-9a-f]{64}$/u.test(plan.planDigest ?? '')) throw new Error('release plan digest is invalid');
  const actual = digestJson({ ...plan, generatedAt: undefined, planDigest: undefined });
  if (actual !== plan.planDigest) throw new Error(`release plan digest mismatch: expected ${actual}`);
  if (!Array.isArray(plan.packages) || !Array.isArray(plan.order))
    throw new Error('release plan package list is missing');
  if (new Set(plan.order).size !== plan.order.length) throw new Error('release plan order contains duplicates');
  if (plan.packages.map((entry) => entry.name).join('\n') !== plan.order.join('\n')) {
    throw new Error('release plan packages must follow the generated topological order');
  }
  if (plan.maven !== null && plan.maven !== undefined) {
    if (plan.maven.scope !== 'all-non-app' || plan.maven.docsBundle !== true || !plan.maven.targetVersion) {
      throw new Error('release plan Maven descriptor is invalid');
    }
    if (!plan.order.includes('@mango/cli')) throw new Error('a Maven release must include the managed CLI matrix');
  }
}

export function assertReleasePlanSourceShape(plan) {
  if (!/^[0-9a-f]{40}$/u.test(plan?.source?.commit ?? '')) {
    throw new Error('release plan source commit is missing or invalid');
  }
  if (!/^[0-9a-f]{40}$/u.test(plan?.source?.tree ?? '')) {
    throw new Error('release plan source tree is missing or invalid');
  }
  if (!Array.isArray(plan.sourceFiles) || plan.sourceFiles.some((file) => typeof file !== 'string' || !file)) {
    throw new Error('release plan sourceFiles is missing or invalid');
  }
  const normalized = [...new Set(plan.sourceFiles)].sort();
  if (JSON.stringify(normalized) !== JSON.stringify(plan.sourceFiles)) {
    throw new Error('release plan sourceFiles must be unique and sorted');
  }
}

export function assertCompletedReleaseBaseline(plan, baseline) {
  assertReleasePlanShape(plan);
  if (baseline?.schemaVersion !== 1) throw new Error('release baseline schemaVersion must be 1');
  if (baseline.planDigest !== plan.planDigest) throw new Error('release baseline plan digest differs from the plan');
  if (baseline.tag !== plan.release?.tag) throw new Error('release baseline tag differs from the plan');
  if (!/^[0-9a-f]{40}$/u.test(baseline.commit ?? '')) throw new Error('release baseline commit is invalid');
  if (!/^[0-9a-f]{40}$/u.test(baseline.tree ?? '')) throw new Error('release baseline tree is invalid');

  const expectedPackages = Object.fromEntries(plan.packages.map((entry) => [entry.name, entry.targetVersion]));
  if (JSON.stringify(canonicalJson(baseline.packages ?? {})) !== JSON.stringify(canonicalJson(expectedPackages))) {
    throw new Error('release baseline package tuple differs from the plan');
  }
  const expectedMaven = plan.maven?.targetVersion ?? null;
  if ((baseline.maven ?? null) !== expectedMaven)
    throw new Error('release baseline Maven version differs from the plan');
}

function groupDeclarations(entries) {
  const grouped = new Map();
  for (const entry of entries) {
    if (!grouped.has(entry.name)) grouped.set(entry.name, []);
    grouped.get(entry.name).push(entry);
  }
  return grouped;
}

function strongestType(types) {
  return [...types].sort((left, right) => BUMP_WEIGHT[right] - BUMP_WEIGHT[left])[0] || '';
}

function releaseDependencies(name, releases, packageIndex, managedVersions) {
  const dependencies = [];
  const packageJson = packageIndex.get(name)?.packageJson ?? {};
  for (const section of ['dependencies', 'optionalDependencies', 'peerDependencies']) {
    for (const [dependency, value] of Object.entries(packageJson[section] ?? {})) {
      if (
        releases.has(dependency) &&
        typeof value === 'string' &&
        value.startsWith('workspace:') &&
        value !== 'workspace:*'
      ) {
        dependencies.push(dependency);
      }
    }
  }
  if (name === '@mango/cli') {
    for (const dependency of releases) {
      if (dependency !== name && managedVersions[dependency]) dependencies.push(dependency);
    }
  }
  return [...new Set(dependencies)].sort();
}

function generatedReason(name, releases, packageIndex, managedVersions) {
  if (name === '@mango/cli') return 'CLI release matrix follows the managed package tuple';
  const dependencies = releaseDependencies(name, releases, packageIndex, managedVersions);
  return `exact runtime dependency follows ${dependencies.join(', ')}`;
}
