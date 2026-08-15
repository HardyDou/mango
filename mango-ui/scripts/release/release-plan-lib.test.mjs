import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';
import { assertReleasePlanShape, buildReleasePlan, bumpVersion, readPendingChangesets } from './release-plan-lib.mjs';

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

test('plan combines direct intent with dependency and CLI closure', () => {
  const plan = buildReleasePlan({
    packageIndex: packages(),
    managedVersions: { '@mango/base': '1.2.3', '@mango/app': '2.0.0' },
    changedFiles: ['mango-ui/packages/base/src/index.ts'],
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
        changedFiles: ['mango/mango-common/src/main/java/io/mango/A.java'],
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
    changedFiles: ['mango/mango-common/src/main/java/io/mango/A.java'],
    changesets: [],
    baseline: { kind: 'successful-release', commit: 'base', tree: 'tree' },
    release: { tag: 'v-test', title: 'test', notesFile: '.changeset/release-notes.txt', notesSha256: 'b'.repeat(64) },
    generatedAt: '2026-08-15T00:00:00.000Z',
  });
  assert.equal(plan.releaseKind, 'mixed');
  assert.equal(plan.maven.targetVersion, '1.0.37');
  assert.deepEqual(plan.order, ['@mango/cli']);
});
