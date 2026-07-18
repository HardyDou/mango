import assert from 'node:assert/strict';
import test from 'node:test';
import { compareArchitectureBaselines, compareWildcardSourceExports } from './architecture-ratchet-lib.mjs';

const base = {
  exceptions: [
    {
      from: '@mango/a',
      to: '@mango/b',
      reason: 'legacy',
      ownerRole: 'Owner',
      adr: 'ADR-1',
      decisionEvidence: 'decision.md',
      expiresAt: '2027-01-01',
    },
  ],
  legacySccs: [
    {
      id: 'legacy',
      graphKind: 'manifest',
      ownerRole: 'Owner',
      adr: 'ADR-1',
      targetPhase: 2,
      members: ['@mango/a', '@mango/b'],
      edges: [{ from: '@mango/a', to: '@mango/b', kind: 'dependencies' }],
    },
  ],
};

test('allows architecture debt to stay equal or shrink', () => {
  assert.deepEqual(compareArchitectureBaselines(base, base), []);
  assert.deepEqual(compareArchitectureBaselines({ exceptions: [], legacySccs: [] }, base), []);
});

test('rejects new exceptions, SCC baselines, members, and edges', () => {
  const current = {
    exceptions: [...base.exceptions, { from: '@mango/b', to: '@mango/a' }],
    legacySccs: [
      {
        ...base.legacySccs[0],
        members: [...base.legacySccs[0].members, '@mango/c'],
        edges: [...base.legacySccs[0].edges, { from: '@mango/b', to: '@mango/a', kind: 'dependencies' }],
      },
      { id: 'new-cycle', graphKind: 'combined', members: [], edges: [] },
    ],
  };
  assert.deepEqual(compareArchitectureBaselines(current, base), [
    'new architecture exception is not allowed: @mango/b->@mango/a',
    'legacy SCC gained member: legacy:@mango/c',
    'legacy SCC gained edge: legacy:@mango/b->@mango/a:dependencies',
    'new legacy SCC baseline is not allowed: new-cycle',
  ]);
});

test('rejects exception evidence changes, expiry extensions, and SCC delays', () => {
  const current = structuredClone(base);
  current.exceptions[0].decisionEvidence = 'replacement.md';
  current.exceptions[0].expiresAt = '2028-01-01';
  current.legacySccs[0].targetPhase = 3;
  assert.deepEqual(compareArchitectureBaselines(current, base), [
    'architecture exception decisionEvidence changed: @mango/a->@mango/b',
    'architecture exception expiry was extended: @mango/a->@mango/b',
    'legacy SCC target phase was delayed: legacy',
  ]);
});

test('allows only pre-existing wildcard source exports and rejects later expansion', () => {
  const baseManifest = {
    name: '@mango/common',
    exports: { './legacy/*': { import: './dist/legacy/*.js' } },
  };
  const currentManifest = {
    name: '@mango/common',
    exports: baseManifest.exports,
    mangoArchitecture: {
      sourceExports: {
        './legacy/*': {
          sourcePattern: './legacy/*.ts',
          expiresAt: '2027-01-18',
        },
      },
    },
  };
  assert.deepEqual(compareWildcardSourceExports([currentManifest], [baseManifest]), []);

  currentManifest.mangoArchitecture.sourceExports['./new/*'] = {
    sourcePattern: './new/*.ts',
    expiresAt: '2099-01-01',
  };
  assert.deepEqual(compareWildcardSourceExports([currentManifest], [baseManifest]), [
    'new wildcard source export is not allowed: @mango/common:./new/*',
  ]);
});

test('rejects wildcard source pattern changes and expiry extensions', () => {
  const baseManifest = {
    name: '@mango/common',
    exports: { './legacy/*': { import: './dist/legacy/*.js' } },
    mangoArchitecture: {
      sourceExports: {
        './legacy/*': {
          sourcePattern: './legacy/*.ts',
          expiresAt: '2027-01-18',
        },
      },
    },
  };
  const currentManifest = structuredClone(baseManifest);
  currentManifest.mangoArchitecture.sourceExports['./legacy/*'].sourcePattern = './expanded/**/*.ts';
  currentManifest.mangoArchitecture.sourceExports['./legacy/*'].expiresAt = '2028-01-18';
  assert.deepEqual(compareWildcardSourceExports([currentManifest], [baseManifest]), [
    'wildcard source pattern changed: @mango/common:./legacy/*',
    'wildcard source export expiry was extended: @mango/common:./legacy/*',
  ]);
});
