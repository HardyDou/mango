import assert from 'node:assert/strict';
import test from 'node:test';
import { selectAffectedWorkspaces } from './affected-selector-lib.mjs';

const records = [
  {
    name: '@mango/contracts',
    relativeRoot: 'mango-ui/packages/contracts',
    private: false,
    scripts: {},
    dependencies: {},
  },
  {
    name: '@mango/domain',
    relativeRoot: 'mango-ui/packages/domain',
    private: false,
    scripts: {},
    dependencies: { '@mango/contracts': 'dependencies' },
  },
  {
    name: 'admin-app',
    relativeRoot: 'mango-ui/apps/admin',
    private: true,
    scripts: {},
    dependencies: { '@mango/domain': 'dependencies' },
  },
];

test('selects a changed workspace and all transitive consumers', () => {
  const result = selectAffectedWorkspaces(records, ['mango-ui/packages/contracts/src/index.ts']);
  assert.equal(result.mode, 'affected');
  assert.deepEqual(result.selected, ['@mango/contracts', '@mango/domain', 'admin-app']);
  assert.deepEqual(result.publishableChanged, ['@mango/contracts']);
});

test('handles additions, deletions, renames, exports and style entries through workspace ownership', () => {
  for (const paths of [
    ['mango-ui/packages/domain/src/new.ts'],
    ['mango-ui/packages/domain/src/deleted.ts'],
    ['mango-ui/packages/domain/src/old.ts', 'mango-ui/packages/domain/src/new.ts'],
    ['mango-ui/packages/domain/package.json'],
    ['mango-ui/packages/domain/style.css'],
    ['mango-ui/packages/domain/src/admin-pages.ts'],
  ]) {
    const result = selectAffectedWorkspaces(records, paths);
    assert.deepEqual(result.selected, ['@mango/domain', 'admin-app']);
  }
});

test('upgrades lockfile, shared config, frontend workflow and frontend governance changes to full', () => {
  for (const file of [
    'mango-ui/pnpm-lock.yaml',
    'mango-ui/tsconfig.base.json',
    '.github/workflows/frontend-quality.yml',
    'mango-catalog/modules/system.json',
    'mango-business-starter/frontend/apps/example/package.json',
    'mango-ui/scripts/quality/affected-selector-lib.mjs',
    'mango-ui/scripts/catalog/catalog-lib.mjs',
    'mango-pmo/rules/frontend/01-vue-code.md',
  ])
    assert.equal(selectAffectedWorkspaces(records, [file]).mode, 'full', file);
});

test('uses no-op only for classified non-frontend changes', () => {
  assert.equal(selectAffectedWorkspaces(records, ['mango-docs/designs/example.md']).mode, 'none');
  assert.equal(selectAffectedWorkspaces(records, ['mango/pom.xml']).mode, 'none');
  assert.equal(selectAffectedWorkspaces(records, ['.github/workflows/pmo-doc-check.yml']).mode, 'none');
  assert.equal(selectAffectedWorkspaces(records, ['mango-business-starter/backend/pom.xml']).mode, 'none');
  assert.equal(selectAffectedWorkspaces(records, ['CHANGELOG.md']).mode, 'none');
});

test('fails closed for unknown scope, empty changes and unowned paths', () => {
  assert.equal(selectAffectedWorkspaces(records, [], { scopeKnown: false }).mode, 'full');
  assert.equal(selectAffectedWorkspaces(records, []).mode, 'full');
  assert.equal(selectAffectedWorkspaces(records, ['mango-ui/packages/missing/src/index.ts']).mode, 'full');
  assert.equal(selectAffectedWorkspaces(records, ['unknown/file.txt']).mode, 'full');
});
