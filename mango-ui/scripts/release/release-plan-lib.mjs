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
import { buildReleasePlanIdentity, digestRelease, releasePlanDigest } from './release-manifest-lib.mjs';

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
  catalogDigest = '0'.repeat(64),
  mavenInventory = [],
  releaseArtifacts = [],
  previousBaselineDigest = '',
  versionPolicyDigest = digestRelease({ schemaVersion: 1, policy: 'changeset-runtime-closure-v1' }),
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
    mavenInventory,
    releaseArtifacts,
  });

  const order = topologicalReleaseOrder(expected, packageIndex, managedVersions);
  const baselineDigest = previousBaselineDigest || digestRelease(baseline ?? {});
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
    publicationOrder: [...(maven?.order ?? []), ...order],
    maven,
    catalogDigest,
    previousBaselineDigest: baselineDigest,
    versionPolicyDigest,
  };
  const releasePlanIdentity = buildReleasePlanIdentity({
    sourceCommitOid: source.commit,
    sourceTreeOid: source.tree,
    catalogDigest,
    previousBaselineDigest: baselineDigest,
    changesetsDigest: digestRelease(changesets.map(({ id, file, sha256: hash }) => ({ id, file, sha256: hash }))),
    releaseNotesDigest: release?.notesSha256 || '',
    versionPolicyDigest,
    resolvedTuple: completeResolvedTuple(baseline, packages, maven),
    releaseClosure: unsigned.publicationOrder,
    publishOrder: unsigned.publicationOrder,
  });
  const planDigest = releasePlanDigest(releasePlanIdentity);
  return { ...unsigned, releasePlanIdentity, planDigest };
}

function completeResolvedTuple(baseline, packages, maven) {
  const tuple = new Map(
    Object.entries(baseline?.packages || {}).map(([name, entry]) => [
      `npm:${name}`,
      { kind: 'npm', name, version: typeof entry === 'string' ? entry : entry.version },
    ]),
  );
  for (const entry of packages) {
    tuple.set(`npm:${entry.name}`, { kind: 'npm', name: entry.name, version: entry.targetVersion });
  }
  if (maven) {
    for (const entry of maven.coordinates) {
      tuple.set(`maven:${entry.coordinate}`, {
        kind: 'maven',
        name: entry.coordinate,
        version: maven.targetVersion,
      });
    }
  } else {
    for (const coordinate of baselineCoordinates(baseline?.maven)) {
      tuple.set(`maven:${coordinate}`, coordinateTuple('maven', coordinate));
    }
    for (const coordinate of baselineCoordinates(baseline?.docs)) {
      tuple.set(`maven:${coordinate}`, coordinateTuple('maven', coordinate));
    }
  }
  return [...tuple.values()].sort((left, right) =>
    compareUtf8(`${left.kind}:${left.name}`, `${right.kind}:${right.name}`),
  );
}

function baselineCoordinates(value) {
  if (!value || typeof value !== 'object') return [];
  if (Array.isArray(value.coordinates)) {
    return value.coordinates.map((entry) => entry.coordinate || entry);
  }
  return Object.keys(value).filter((key) => key.split(':').length === 3);
}

function coordinateTuple(kind, coordinate) {
  return { kind, name: coordinate, version: coordinate.split(':').at(-1) };
}

function resolveMavenRelease({ mavenImpact, sourceVersion, targetVersion, mavenInventory, releaseArtifacts }) {
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
  if (!Array.isArray(mavenInventory) || mavenInventory.length === 0) {
    throw new Error('Maven source impact requires the Catalog Maven coordinate inventory');
  }
  const coordinates = mavenInventory.map((entry) => ({
    coordinate: `${entry.groupId}:${entry.artifactId}:${targetVersion}`,
    groupId: entry.groupId,
    artifactId: entry.artifactId,
    packaging: entry.packaging,
    extension: entry.extension,
    classifier: entry.classifier,
    dependencies: (entry.dependencies || [])
      .filter(isMavenPublicationDependency)
      .map((dependency) => `${dependency.groupId}:${dependency.artifactId}`),
  }));
  const platformArtifacts = (releaseArtifacts || []).filter((entry) => entry.versionSource === 'maven-platform');
  if (platformArtifacts.length === 0) {
    throw new Error('Maven source impact requires the Catalog platform release artifacts');
  }
  for (const entry of platformArtifacts) {
    coordinates.push({
      coordinate: `${entry.groupId}:${entry.artifactId}:${targetVersion}`,
      groupId: entry.groupId,
      artifactId: entry.artifactId,
      packaging: entry.packaging,
      extension: entry.extension,
      classifier: entry.classifier,
      dependencies: [],
    });
  }
  const coordinateSet = new Set(coordinates.map((entry) => entry.coordinate));
  if (coordinateSet.size !== coordinates.length)
    throw new Error('Maven release coordinate inventory contains duplicates');
  const order = topologicalMavenOrder(coordinates);
  return {
    sourceVersion,
    targetVersion,
    scope: 'all-non-app',
    docsBundle: true,
    coordinates: order.map((coordinate) => coordinates.find((entry) => entry.coordinate === coordinate)),
    order,
    reason: 'Git impact includes Mango Maven production sources',
  };
}

function isMavenPublicationDependency(dependency) {
  const scope = dependency.scope || 'compile';
  const type = dependency.type || 'jar';
  return ['compile', 'runtime'].includes(scope) && ['jar', 'pom'].includes(type);
}

function topologicalMavenOrder(coordinates) {
  const byGa = new Map(coordinates.map((entry) => [`${entry.groupId}:${entry.artifactId}`, entry]));
  const result = [];
  const visiting = new Set();
  const visited = new Set();
  for (const entry of [...coordinates].sort((left, right) => compareUtf8(left.coordinate, right.coordinate)))
    visit(entry);
  return result;

  function visit(entry) {
    if (visited.has(entry.coordinate)) return;
    if (visiting.has(entry.coordinate)) throw new Error(`Maven release dependency cycle includes ${entry.coordinate}`);
    visiting.add(entry.coordinate);
    for (const dependency of [...new Set(entry.dependencies)].sort(compareUtf8)) {
      const target = byGa.get(dependency);
      if (target) visit(target);
    }
    visiting.delete(entry.coordinate);
    visited.add(entry.coordinate);
    result.push(entry.coordinate);
  }
}

export function assertReleasePlanShape(plan) {
  if (plan?.schemaVersion !== 1) throw new Error('release plan schemaVersion must be 1');
  if (!/^[0-9a-f]{64}$/u.test(plan.planDigest ?? '')) throw new Error('release plan digest is invalid');
  const actual = plan.releasePlanIdentity
    ? releasePlanDigest(plan.releasePlanIdentity)
    : digestJson({ ...plan, generatedAt: undefined, planDigest: undefined });
  if (actual !== plan.planDigest) throw new Error(`release plan digest mismatch: expected ${actual}`);
  if (plan.releasePlanIdentity) {
    if (!Array.isArray(plan.publicationOrder)) throw new Error('release plan publication order is missing');
    const publicationOrder = plan.publicationOrder;
    if (JSON.stringify(plan.releasePlanIdentity.publishOrder) !== JSON.stringify(publicationOrder)) {
      throw new Error('release plan identity publish order differs from the plan');
    }
    if (JSON.stringify(plan.releasePlanIdentity.releaseClosure) !== JSON.stringify(publicationOrder)) {
      throw new Error('release plan identity closure differs from the plan');
    }
    if (plan.releasePlanIdentity.sourceCommitOid !== plan.source?.commit) {
      throw new Error('release plan identity source commit differs from the plan');
    }
    if (plan.releasePlanIdentity.sourceTreeOid !== plan.source?.tree) {
      throw new Error('release plan identity source tree differs from the plan');
    }
    if (plan.releasePlanIdentity.catalogDigest !== plan.catalogDigest) {
      throw new Error('release plan identity Catalog digest differs from the plan');
    }
    if (plan.releasePlanIdentity.previousBaselineDigest !== plan.previousBaselineDigest) {
      throw new Error('release plan identity baseline digest differs from the plan');
    }
    if (plan.releasePlanIdentity.changesetsDigest !== digestRelease(plan.changesets ?? [])) {
      throw new Error('release plan identity Changeset digest differs from the plan');
    }
    if (plan.releasePlanIdentity.releaseNotesDigest !== (plan.release?.notesSha256 || '')) {
      throw new Error('release plan identity release notes digest differs from the plan');
    }
    if (plan.releasePlanIdentity.versionPolicyDigest !== plan.versionPolicyDigest) {
      throw new Error('release plan identity version policy digest differs from the plan');
    }
    if (
      JSON.stringify(plan.releasePlanIdentity.resolvedTuple) !==
      JSON.stringify(completeResolvedTuple(plan.baseline, plan.packages ?? [], plan.maven))
    ) {
      throw new Error('release plan identity resolved tuple differs from the plan');
    }
  }
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
    if (
      (plan.maven.coordinates || plan.maven.order) &&
      (!Array.isArray(plan.maven.coordinates) ||
        plan.maven.coordinates.length === 0 ||
        JSON.stringify(plan.maven.coordinates.map((entry) => entry.coordinate)) !== JSON.stringify(plan.maven.order))
    ) {
      throw new Error('release plan Maven coordinate order is invalid');
    }
  }
  if (plan.releasePlanIdentity) {
    const expectedPublicationOrder = [...(plan.maven?.order ?? []), ...plan.order];
    if (JSON.stringify(plan.publicationOrder) !== JSON.stringify(expectedPublicationOrder)) {
      throw new Error('release plan publication order differs from Maven and npm topological order');
    }
    if (new Set(plan.publicationOrder).size !== plan.publicationOrder.length) {
      throw new Error('release plan publication order contains duplicates');
    }
  }
}

function compareUtf8(left, right) {
  return Buffer.compare(Buffer.from(left, 'utf8'), Buffer.from(right, 'utf8'));
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

  for (const entry of plan.packages) {
    const baselineEntry = baseline.packages?.[entry.name];
    const actualVersion = typeof baselineEntry === 'string' ? baselineEntry : baselineEntry?.version;
    if (actualVersion !== entry.targetVersion) {
      throw new Error(`release baseline package ${entry.name} differs from the plan`);
    }
  }
  const actualMaven =
    typeof baseline.maven === 'object' && baseline.maven ? baseline.maven.version : (baseline.maven ?? null);
  if (plan.maven) {
    if (actualMaven !== plan.maven.targetVersion) {
      throw new Error('release baseline Maven version differs from the plan');
    }
  } else if (digestJson(baseline.maven ?? null) !== digestJson(plan.baseline?.maven ?? null)) {
    throw new Error('release baseline carried Maven evidence differs from the previous successful baseline');
  }
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
