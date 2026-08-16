import assert from 'node:assert/strict';
import { dirname, resolve } from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import {
  directPackageImpact,
  indexPublishedPackages,
  isExactWorkspaceVersion,
  isReleaseImpactFile,
  parseChangeset,
  resolveReleaseClosure,
  selectReleaseIntentHead,
  topologicalReleaseOrder,
  validateDeclaredReleaseSet,
} from './release-scope-lib.mjs';

const workspaceRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../..');

function packageIndex() {
  return new Map([
    ['@mango/base', { dir: 'base', packageJson: { name: '@mango/base', version: '1.0.0' } }],
    [
      '@mango/shell',
      {
        dir: 'shell',
        packageJson: {
          name: '@mango/shell',
          version: '1.0.0',
          dependencies: { '@mango/base': 'workspace:1.0.0' },
        },
      },
    ],
    [
      '@mango/floating',
      {
        dir: 'floating',
        packageJson: {
          name: '@mango/floating',
          version: '1.0.0',
          dependencies: { '@mango/base': 'workspace:*' },
        },
      },
    ],
    ['@mango/cli', { dir: 'mango-cli', packageJson: { name: '@mango/cli', version: '1.0.0' } }],
  ]);
}

test('release-impact classification ignores tests but includes all other package files', () => {
  assert.equal(isReleaseImpactFile('src/index.ts'), true);
  assert.equal(isReleaseImpactFile('README.md'), true);
  assert.equal(isReleaseImpactFile('src/__tests__/index.spec.ts'), false);
  assert.equal(isReleaseImpactFile('src/index.test.ts'), false);
  assert.equal(isReleaseImpactFile('CHANGELOG.md'), false);
});

test('direct package impact maps package and PMO source paths', () => {
  const impact = directPackageImpact(
    [
      'mango-ui/packages/base/src/index.ts',
      'mango-ui/packages/base/src/index.test.ts',
      'mango-pmo/skills/mango-release/SKILL.md',
    ],
    packageIndex(),
  );
  assert.deepEqual([...impact.direct].sort(), ['@mango/base', '@mango/pmo']);
});

test('closure includes exact runtime dependents and managed CLI but not floating or dev dependents', () => {
  const packages = packageIndex();
  packages.get('@mango/floating').packageJson.devDependencies = { '@mango/shell': 'workspace:1.0.0' };
  const closure = resolveReleaseClosure(new Set(['@mango/base']), packages, {
    '@mango/base': '1.0.0',
    '@mango/shell': '1.0.0',
  });
  assert.deepEqual([...closure].sort(), ['@mango/base', '@mango/cli', '@mango/shell']);
  assert.deepEqual(topologicalReleaseOrder(closure, packages, { '@mango/base': '1.0.0', '@mango/shell': '1.0.0' }), [
    '@mango/base',
    '@mango/shell',
    '@mango/cli',
  ]);
});

test('exact workspace version excludes workspace star', () => {
  assert.equal(isExactWorkspaceVersion('workspace:1.2.3'), true);
  assert.equal(isExactWorkspaceVersion('workspace:*'), false);
  assert.equal(isExactWorkspaceVersion('1.2.3'), false);
});

test('Changeset parser requires explicit Mango package entries and summary', () => {
  const parsed = parseChangeset('---\n"@mango/base": patch\n---\n\nFix base.\n', 'base.md');
  assert.deepEqual(parsed.releases, [{ name: '@mango/base', type: 'patch' }]);
  assert.equal(parsed.summary, 'Fix base.');
  assert.throws(() => parseChangeset('---\n"@mango/base": invalid\n---\nBad\n'), /unsupported Changeset entry/u);
});

test('declaration validation rejects both missing and unrelated packages', () => {
  const errors = validateDeclaredReleaseSet({
    direct: new Set(['@mango/base']),
    expected: new Set(['@mango/base', '@mango/shell']),
    declared: new Set(['@mango/other']),
  });
  assert.deepEqual(errors, [
    'missing Changeset declaration for @mango/base',
    'Changeset declares unrelated package @mango/other',
  ]);
});

test('direct declarations do not have to duplicate graph-generated dependents', () => {
  const errors = validateDeclaredReleaseSet({
    direct: new Set(['@mango/base']),
    expected: new Set(['@mango/base', '@mango/shell', '@mango/cli']),
    declared: new Set(['@mango/base']),
  });
  assert.deepEqual(errors, []);
});

test('mixed release intent stops at the plan source before machine projections', () => {
  assert.equal(
    selectReleaseIntentHead({
      head: 'final-head',
      planChanged: true,
      sourceCommit: 'source-head',
      sourceIsAncestor: true,
      projectionReleaseOnly: true,
    }),
    'source-head',
  );
  assert.equal(
    selectReleaseIntentHead({
      head: 'ordinary-head',
      planChanged: false,
      sourceCommit: '',
      sourceIsAncestor: false,
      projectionReleaseOnly: false,
    }),
    'ordinary-head',
  );
  assert.throws(
    () =>
      selectReleaseIntentHead({
        head: 'final-head',
        planChanged: true,
        sourceCommit: 'source-head',
        sourceIsAncestor: false,
        projectionReleaseOnly: true,
      }),
    /not an ancestor/u,
  );
  assert.throws(
    () =>
      selectReleaseIntentHead({
        head: 'final-head',
        planChanged: true,
        sourceCommit: 'source-head',
        sourceIsAncestor: true,
        projectionReleaseOnly: false,
      }),
    /non-release changes/u,
  );
});

test('the complete published Mango package graph has a deterministic release order', () => {
  const packages = indexPublishedPackages(workspaceRoot);
  const releases = new Set(packages.keys());
  const managedVersions = Object.fromEntries([...packages].map(([name, entry]) => [name, entry.packageJson.version]));

  const order = topologicalReleaseOrder(releases, packages, managedVersions);

  assert.equal(order.length, releases.size);
  assert.deepEqual([...order].sort(), [...releases].sort());
});
