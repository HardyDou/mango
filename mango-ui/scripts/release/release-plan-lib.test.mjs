import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';
import {
  buildNormalReleaseBaseline,
  buildSealedArtifactManifest,
  preparedCandidateId,
} from './release-manifest-lib.mjs';
import {
  assertCompletedReleaseBaseline,
  assertReleasePlanShape,
  buildReleasePlan,
  bumpVersion,
  readPendingChangesets,
  resolveReleaseMavenSourceVersion,
} from './release-plan-lib.mjs';

function packages() {
  return new Map([
    ['@mango/base', { dir: 'base', packageJson: { name: '@mango/base', version: '1.2.3' } }],
    [
      '@mango/app',
      {
        dir: 'app',
        packageJson: { name: '@mango/app', version: '2.0.0', dependencies: { '@mango/base': 'workspace:1.2.3' } },
      },
    ],
    ['@mango/cli', { dir: 'mango-cli', packageJson: { name: '@mango/cli', version: '3.0.0' } }],
  ]);
}

const source = { commit: '1'.repeat(40), tree: '2'.repeat(40) };
const mavenInventory = [
  {
    groupId: 'io.mango',
    artifactId: 'mango-base',
    packaging: 'jar',
    extension: 'jar',
    classifier: null,
    dependencies: [],
  },
];
const releaseArtifacts = [
  {
    groupId: 'io.mango',
    artifactId: 'mango-docs-bundle',
    packaging: 'jar',
    extension: 'jar',
    classifier: null,
    versionSource: 'maven-platform',
  },
];

test('plan combines direct intent with dependency and CLI closure', () => {
  const plan = buildReleasePlan({
    packageIndex: packages(),
    managedVersions: { '@mango/base': '1.2.3', '@mango/app': '2.0.0' },
    source,
    sourceFiles: ['mango-ui/packages/base/src/index.ts'],
    changesets: [
      {
        id: 'base-fix',
        file: '.changeset/base-fix.md',
        sha256: 'a'.repeat(64),
        summary: 'Fix base.',
        releases: [{ name: '@mango/base', type: 'minor' }],
      },
    ],
    baseline: { kind: 'successful-release', commit: 'base', tree: 'tree' },
    release: { tag: 'v-test', title: 'test', notesFile: '.changeset/release-notes.txt', notesSha256: 'b'.repeat(64) },
    generatedAt: '2026-08-15T00:00:00.000Z',
  });
  assert.deepEqual(plan.order, ['@mango/base', '@mango/app', '@mango/cli']);
  assert.deepEqual(
    plan.packages.map((entry) => [entry.name, entry.targetVersion, entry.generated]),
    [
      ['@mango/base', '1.3.0', false],
      ['@mango/app', '2.0.1', true],
      ['@mango/cli', '3.0.1', true],
    ],
  );
  assertReleasePlanShape(plan);
  assert.throws(
    () => assertReleasePlanShape({ ...plan, order: [...plan.order].reverse() }),
    /digest mismatch|publish order differs|topological order/u,
  );
});

test('semver bump rejects non-release versions', () => {
  assert.equal(bumpVersion('1.2.3', 'patch'), '1.2.4');
  assert.equal(bumpVersion('1.2.3', 'major'), '2.0.0');
  assert.throws(() => bumpVersion('1.2.3-SNAPSHOT', 'patch'), /unsupported release version/u);
});

test('release notes template is reserved documentation rather than a Changeset', (t) => {
  const workspaceRoot = mkdtempSync(join(tmpdir(), 'mango-release-plan-'));
  t.after(() => rmSync(workspaceRoot, { recursive: true, force: true }));
  const changesetRoot = join(workspaceRoot, '.changeset');
  mkdirSync(changesetRoot, { recursive: true });
  writeFileSync(join(changesetRoot, 'README.md'), '# Changesets\n');
  writeFileSync(join(changesetRoot, 'release-notes-template.md'), '# Release Notes Template\n');
  writeFileSync(join(changesetRoot, 'base-fix.md'), '---\n"@mango/base": patch\n---\n\nFix base.\n');

  const changesets = readPendingChangesets(workspaceRoot);
  assert.deepEqual(
    changesets.map((entry) => entry.file),
    ['.changeset/base-fix.md'],
  );
});

test('Maven source impact requires an explicit version and adds the managed CLI', () => {
  assert.throws(
    () =>
      buildReleasePlan({
        packageIndex: packages(),
        managedVersions: {},
        mavenSourceVersion: '1.0.36',
        mavenInventory,
        releaseArtifacts,
        source,
        sourceFiles: ['mango/mango-common/src/main/java/io/mango/A.java'],
        changesets: [],
        baseline: { kind: 'successful-release', commit: 'base', tree: 'tree' },
        release: {
          tag: 'v-test',
          title: 'test',
          notesFile: '.changeset/release-notes.txt',
          notesSha256: 'b'.repeat(64),
        },
      }),
    /requires --maven-version/u,
  );
  const plan = buildReleasePlan({
    packageIndex: packages(),
    managedVersions: {},
    mavenSourceVersion: '1.0.36',
    mavenTargetVersion: '1.0.37',
    mavenInventory,
    releaseArtifacts,
    source,
    sourceFiles: ['mango/mango-common/src/main/java/io/mango/A.java'],
    changesets: [],
    baseline: { kind: 'successful-release', commit: 'base', tree: 'tree' },
    release: { tag: 'v-test', title: 'test', notesFile: '.changeset/release-notes.txt', notesSha256: 'b'.repeat(64) },
    generatedAt: '2026-08-15T00:00:00.000Z',
  });
  assert.equal(plan.releaseKind, 'mixed');
  assert.equal(plan.maven.targetVersion, '1.0.37');
  assert.deepEqual(plan.maven.order, ['io.mango:mango-base:1.0.37', 'io.mango:mango-docs-bundle:1.0.37']);
  assert.deepEqual(plan.publicationOrder, [...plan.maven.order, '@mango/cli']);
  assert.deepEqual(plan.order, ['@mango/cli']);
});

test('Maven publication order follows runtime dependencies, excludes tooling edges, and includes docs', () => {
  const dependentInventory = [
    {
      groupId: 'io.mango',
      artifactId: 'mango-consumer',
      packaging: 'jar',
      extension: 'jar',
      classifier: null,
      dependencies: [
        {
          groupId: 'io.mango',
          artifactId: 'mango-runtime',
          scope: 'runtime',
          optional: true,
          type: 'jar',
        },
        { groupId: 'io.mango', artifactId: 'mango-test-only', scope: 'test', optional: false, type: 'jar' },
      ],
    },
    {
      groupId: 'io.mango',
      artifactId: 'mango-runtime',
      packaging: 'jar',
      extension: 'jar',
      classifier: null,
      dependencies: [],
    },
    {
      groupId: 'io.mango',
      artifactId: 'mango-test-only',
      packaging: 'jar',
      extension: 'jar',
      classifier: null,
      dependencies: [
        { groupId: 'io.mango', artifactId: 'mango-consumer', scope: 'provided', optional: false, type: 'jar' },
      ],
    },
  ];
  const plan = buildReleasePlan({
    packageIndex: packages(),
    managedVersions: {},
    mavenSourceVersion: '1.0.36',
    mavenTargetVersion: '1.0.37',
    mavenInventory: dependentInventory,
    releaseArtifacts,
    source,
    sourceFiles: ['mango/mango-common/src/main/java/io/mango/A.java'],
    changesets: [],
    baseline: { kind: 'successful-release', commit: 'base', tree: 'tree' },
    release: { tag: 'v-test', title: 'test', notesFile: '.changeset/release-notes.txt', notesSha256: 'b'.repeat(64) },
    generatedAt: '2026-08-15T00:00:00.000Z',
  });

  assert.ok(
    plan.maven.order.indexOf('io.mango:mango-runtime:1.0.37') <
      plan.maven.order.indexOf('io.mango:mango-consumer:1.0.37'),
  );
  assert.ok(plan.maven.order.includes('io.mango:mango-docs-bundle:1.0.37'));
  assert.deepEqual(plan.publicationOrder, [...plan.maven.order, ...plan.order]);
  assertReleasePlanShape(plan);
});

test('Maven coordinate inventory and plan projections are bound to the plan identity', () => {
  const input = {
    packageIndex: packages(),
    managedVersions: {},
    mavenSourceVersion: '1.0.36',
    mavenTargetVersion: '1.0.37',
    releaseArtifacts,
    source,
    sourceFiles: ['mango/mango-common/src/main/java/io/mango/A.java'],
    changesets: [],
    baseline: { kind: 'successful-release', commit: 'base', tree: 'tree' },
    release: { tag: 'v-test', title: 'test', notesFile: '.changeset/release-notes.txt', notesSha256: 'b'.repeat(64) },
    generatedAt: '2026-08-15T00:00:00.000Z',
  };
  const first = buildReleasePlan({ ...input, mavenInventory });
  const changed = buildReleasePlan({
    ...input,
    mavenInventory: [{ ...mavenInventory[0], artifactId: 'mango-base-renamed' }],
  });
  assert.notEqual(first.planDigest, changed.planDigest);
  assert.throws(
    () => assertReleasePlanShape({ ...first, publicationOrder: [...first.publicationOrder].reverse() }),
    /publish order differs|publication order differs/u,
  );
  assert.throws(
    () =>
      assertReleasePlanShape({
        ...first,
        packages: first.packages.map((entry) =>
          entry.name === '@mango/cli' ? { ...entry, targetVersion: '9.9.9' } : entry,
        ),
      }),
    /resolved tuple differs/u,
  );
});

test('bootstrap Maven and docs coordinates remain in the next complete resolved tuple', () => {
  const baseline = {
    baselineKind: 'bootstrap',
    packages: { '@mango/base': { version: '1.2.3' } },
    maven: {
      'io.mango:mango-base:1.0.36': { files: [{ path: 'base.jar', sha256: 'a'.repeat(64) }] },
    },
    docs: {
      'io.mango:mango-docs-bundle:1.0.36': { files: [{ path: 'docs.jar', sha256: 'b'.repeat(64) }] },
    },
  };
  const plan = buildReleasePlan({
    packageIndex: packages(),
    managedVersions: { '@mango/base': '1.2.3' },
    source,
    sourceFiles: [],
    changesets: [],
    baseline,
    release: { tag: 'v-test', title: 'test', notesFile: '.changeset/release-notes.txt', notesSha256: 'b'.repeat(64) },
    generatedAt: '2026-08-15T00:00:00.000Z',
  });

  assert.deepEqual(
    plan.releasePlanIdentity.resolvedTuple.filter((entry) => entry.kind === 'maven'),
    [
      { kind: 'maven', name: 'io.mango:mango-base:1.0.36', version: '1.0.36' },
      { kind: 'maven', name: 'io.mango:mango-docs-bundle:1.0.36', version: '1.0.36' },
    ],
  );
  assertReleasePlanShape(plan);
});

test('an in-progress Maven plan keeps its published source after the CLI matrix is projected', () => {
  const previousPlan = {
    maven: {
      sourceVersion: '1.0.36',
      targetVersion: '1.0.37',
    },
  };

  assert.equal(resolveReleaseMavenSourceVersion(previousPlan, '1.0.37'), '1.0.36');
  assert.equal(resolveReleaseMavenSourceVersion(null, '1.0.36'), '1.0.36');
});

test('completed release baseline is bound to the immutable plan tuple', () => {
  const plan = buildReleasePlan({
    packageIndex: packages(),
    managedVersions: { '@mango/base': '1.2.3', '@mango/app': '2.0.0' },
    source,
    sourceFiles: ['mango-ui/packages/base/src/index.ts'],
    changesets: [
      {
        id: 'base-fix',
        file: '.changeset/base-fix.md',
        sha256: 'a'.repeat(64),
        summary: 'Fix base.',
        releases: [{ name: '@mango/base', type: 'patch' }],
      },
    ],
    baseline: { kind: 'successful-release', commit: 'base', tree: 'tree' },
    release: { tag: 'v-test', title: 'test', notesFile: '.changeset/release-notes.txt', notesSha256: 'b'.repeat(64) },
    generatedAt: '2026-08-15T00:00:00.000Z',
  });
  const baseline = {
    schemaVersion: 1,
    tag: 'v-test',
    commit: '1'.repeat(40),
    tree: '2'.repeat(40),
    planDigest: plan.planDigest,
    packages: Object.fromEntries(plan.packages.map((entry) => [entry.name, entry.targetVersion])),
    maven: null,
  };
  baseline.packages['@mango/untouched'] = {
    version: '1.0.0',
    tarballSha256: 'a'.repeat(64),
    sri: `sha512-${'x'.repeat(88)}`,
    publishedRanges: [],
  };

  assert.doesNotThrow(() => assertCompletedReleaseBaseline(plan, baseline));
  assert.throws(
    () =>
      assertCompletedReleaseBaseline(plan, {
        ...baseline,
        packages: { ...baseline.packages, '@mango/base': '9.9.9' },
      }),
    /package @mango\/base differs/u,
  );
  assert.throws(() => assertCompletedReleaseBaseline(plan, { ...baseline, tag: 'v-other' }), /tag differs/u);
});

test('Maven evidence survives npm-only completion and remains available to the next plan', () => {
  const previousMaven = {
    scope: 'all-non-app',
    version: '1.0.39',
    coordinates: [
      {
        coordinate: 'io.mango:mango-bom:1.0.39',
        files: [{ path: 'mango-bom-1.0.39.pom', size: 10, sha256: 'c'.repeat(64) }],
      },
    ],
  };
  const plan = buildReleasePlan({
    packageIndex: packages(),
    managedVersions: { '@mango/base': '1.2.3', '@mango/app': '2.0.0' },
    source,
    sourceFiles: ['mango-ui/packages/base/src/index.ts'],
    changesets: [
      {
        id: 'base-fix',
        file: '.changeset/base-fix.md',
        sha256: 'a'.repeat(64),
        summary: 'Fix base.',
        releases: [{ name: '@mango/base', type: 'patch' }],
      },
    ],
    baseline: {
      kind: 'successful-release',
      commit: 'base',
      tree: 'tree',
      maven: previousMaven,
    },
    release: { tag: 'v-test', title: 'test', notesFile: '.changeset/release-notes.txt', notesSha256: 'b'.repeat(64) },
    generatedAt: '2026-08-15T00:00:00.000Z',
  });
  const sealed = buildSealedArtifactManifest({
    releasePlanDigest: plan.planDigest,
    source,
    npmArtifacts: plan.packages.map((entry, index) => ({
      name: entry.name,
      version: entry.targetVersion,
      file: `artifacts/package-${index}.tgz`,
      size: 10 + index,
      sha256: String(index + 3).repeat(64),
      sri: `sha512-${String.fromCharCode(120 + index).repeat(88)}`,
      publishedRanges: [],
    })),
  });
  const baseline = buildNormalReleaseBaseline({
    plan,
    manifest: {
      preparedCandidateId: preparedCandidateId(sealed),
      source: { ...source, mergedCommit: '3'.repeat(40) },
      sealedArtifactManifest: sealed,
    },
  });

  assert.equal(plan.releaseKind, 'npm-only');
  assert.doesNotThrow(() => assertCompletedReleaseBaseline(plan, baseline));
  assert.deepEqual(baseline.maven, previousMaven);
  assert.throws(
    () =>
      assertCompletedReleaseBaseline(plan, {
        ...baseline,
        maven: {
          ...baseline.maven,
          coordinates: [
            {
              ...baseline.maven.coordinates[0],
              files: [{ ...baseline.maven.coordinates[0].files[0], sha256: 'd'.repeat(64) }],
            },
          ],
        },
      }),
    /carried Maven evidence differs/u,
  );

  const nextPlan = buildReleasePlan({
    packageIndex: packages(),
    managedVersions: { '@mango/base': '1.2.4', '@mango/app': '2.0.1' },
    source: { commit: '4'.repeat(40), tree: '5'.repeat(40) },
    sourceFiles: ['mango-ui/packages/base/src/index.ts'],
    changesets: [
      {
        id: 'next-base-fix',
        file: '.changeset/next-base-fix.md',
        sha256: 'e'.repeat(64),
        summary: 'Fix base again.',
        releases: [{ name: '@mango/base', type: 'patch' }],
      },
    ],
    baseline,
    release: {
      tag: 'v-next',
      title: 'next',
      notesFile: '.changeset/release-notes.txt',
      notesSha256: 'f'.repeat(64),
    },
    generatedAt: '2026-08-22T00:00:00.000Z',
  });
  assert.deepEqual(nextPlan.baseline.maven, previousMaven);
  assert.match(JSON.stringify(nextPlan.releasePlanIdentity.resolvedTuple), /io\.mango:mango-bom:1\.0\.39/u);
});

test('plan recheck keeps dependency-closure packages generated after version projection', () => {
  const changesets = [
    {
      id: 'base-fix',
      file: '.changeset/base-fix.md',
      sha256: 'a'.repeat(64),
      summary: 'Fix base.',
      releases: [{ name: '@mango/base', type: 'patch' }],
    },
  ];
  const input = {
    managedVersions: { '@mango/base': '1.2.3', '@mango/app': '2.0.0' },
    source,
    sourceFiles: ['mango-ui/packages/base/src/index.ts'],
    changesets,
    baseline: { kind: 'successful-release', commit: 'base', tree: 'tree' },
    release: { tag: 'v-test', title: 'test', notesFile: '.changeset/release-notes.txt', notesSha256: 'b'.repeat(64) },
    generatedAt: '2026-08-15T00:00:00.000Z',
  };
  const original = buildReleasePlan({ packageIndex: packages(), ...input });
  const projected = packages();
  projected.get('@mango/base').packageJson.version = '1.2.4';
  projected.get('@mango/app').packageJson.version = '2.0.1';
  projected.get('@mango/app').packageJson.dependencies['@mango/base'] = 'workspace:1.2.4';
  projected.get('@mango/cli').packageJson.version = '3.0.1';

  const checked = buildReleasePlan({
    packageIndex: projected,
    ...input,
    managedVersions: { '@mango/base': '1.2.4', '@mango/app': '2.0.1' },
    previousPlan: original,
  });

  assert.equal(checked.planDigest, original.planDigest);
  assert.deepEqual(checked.directPackages, ['@mango/base']);
  assert.equal(checked.packages.find((entry) => entry.name === '@mango/app').generated, true);
});
