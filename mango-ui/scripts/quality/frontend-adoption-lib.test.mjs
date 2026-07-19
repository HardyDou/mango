import assert from 'node:assert/strict';
import test from 'node:test';
import { validateAdoptionContract } from './frontend-adoption-lib.mjs';

function adoptionContract(dependencyRecovery) {
  return {
    schemaVersion: 1,
    owner: 'owner',
    approvalMode: 'single-owner',
    scope: 'standards-adoption',
    adoptionStages: [
      { name: 'pilot', requiredChecks: ['cms-runtime'] },
      { name: 'affected', requiredChecks: ['affected-workspaces'] },
      { name: 'repository', requiredChecks: ['architecture-ratchet'] },
    ],
    enforcement: {
      newAndChangedCode: 'blocking',
      historicalDebt: 'exact-identities-only-decrease',
    },
    dependencyRecovery,
  };
}

test('adoption contract requires exact stable pins and removal of a new package', () => {
  const result = validateAdoptionContract(
    adoptionContract({
      '@mango/existing': { version: '1.0.0', action: 'pin' },
      '@mango/new': { version: null, action: 'remove' },
    }),
    {
      candidateVersions: { '@mango/existing': '1.1.0', '@mango/new': '1.0.0' },
      baseVersions: { '@mango/existing': '1.0.0' },
      localVersions: { '@mango/existing': '1.1.0', '@mango/new': '1.0.0' },
    },
  );
  assert.deepEqual(result.failures, []);
  assert.equal(result.mode, 'candidate');
  assert.equal(result.stageCount, 3);
});

test('adoption contract rejects traffic-style stages and permissive enforcement', () => {
  const contract = adoptionContract({ '@mango/existing': { version: '1.0.0', action: 'pin' } });
  contract.adoptionStages = [{ name: 'canary', requiredChecks: ['traffic'] }];
  contract.enforcement.newAndChangedCode = 'warning';
  const result = validateAdoptionContract(contract, {
    candidateVersions: { '@mango/existing': '1.1.0' },
    baseVersions: { '@mango/existing': '1.0.0' },
    localVersions: { '@mango/existing': '1.1.0' },
  });
  assert.match(result.failures.join('\n'), /pilot, affected, repository/);
  assert.match(result.failures.join('\n'), /must be blocking/);
});

test('adoption contract rejects a stale dependency recovery pin', () => {
  const result = validateAdoptionContract(
    adoptionContract({ '@mango/existing': { version: '0.9.0', action: 'pin' } }),
    {
      candidateVersions: { '@mango/existing': '1.1.0' },
      baseVersions: { '@mango/existing': '1.0.0' },
      localVersions: { '@mango/existing': '1.1.0' },
    },
  );
  assert.match(result.failures.join('\n'), /recovery must pin 1\.0\.0/);
});

test('adoption contract rejects a local public package missing from the candidate lock', () => {
  const result = validateAdoptionContract(
    adoptionContract({ '@mango/existing': { version: '1.0.0', action: 'pin' } }),
    {
      candidateVersions: { '@mango/existing': '1.1.0' },
      baseVersions: { '@mango/existing': '1.0.0' },
      localVersions: { '@mango/existing': '1.1.0', '@mango/unlocked': '1.0.0' },
    },
  );
  assert.match(result.failures.join('\n'), /complete local public package set/);
});
