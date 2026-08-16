import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';
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
  assert.throws(() => assertReleasePlanShape({ ...plan, order: [...plan.order].reverse() }), /digest mismatch/u);
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
    source,
    sourceFiles: ['mango/mango-common/src/main/java/io/mango/A.java'],
    changesets: [],
    baseline: { kind: 'successful-release', commit: 'base', tree: 'tree' },
    release: { tag: 'v-test', title: 'test', notesFile: '.changeset/release-notes.txt', notesSha256: 'b'.repeat(64) },
    generatedAt: '2026-08-15T00:00:00.000Z',
  });
  assert.equal(plan.releaseKind, 'mixed');
  assert.equal(plan.maven.targetVersion, '1.0.37');
  assert.deepEqual(plan.order, ['@mango/cli']);
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

  assert.doesNotThrow(() => assertCompletedReleaseBaseline(plan, baseline));
  assert.throws(
    () =>
      assertCompletedReleaseBaseline(plan, {
        ...baseline,
        packages: { ...baseline.packages, '@mango/base': '9.9.9' },
      }),
    /package tuple differs/u,
  );
  assert.throws(() => assertCompletedReleaseBaseline(plan, { ...baseline, tag: 'v-other' }), /tag differs/u);
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
