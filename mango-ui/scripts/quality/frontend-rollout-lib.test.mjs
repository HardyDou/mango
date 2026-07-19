import assert from 'node:assert/strict';
import test from 'node:test';
import { evaluateRolloutSample, validateRolloutContract } from './frontend-rollout-lib.mjs';

const thresholds = { errors: 0.5, whiteScreens: 0 };

test('rollout decision promotes a healthy sample and rolls back on one breach', () => {
  assert.equal(evaluateRolloutSample(thresholds, { errors: 0.1, whiteScreens: 0 }).decision, 'promote');
  const result = evaluateRolloutSample(thresholds, { errors: 0.6, whiteScreens: 0 });
  assert.equal(result.decision, 'rollback');
  assert.deepEqual(
    result.breaches.map((breach) => breach.signal),
    ['errors'],
  );
});

test('rollout contract requires exact stable pins and removal of a new package', () => {
  const contract = {
    schemaVersion: 1,
    owner: 'owner',
    approvalMode: 'single-owner',
    scope: 'release-lock',
    trafficStages: [0, 5, 25, 100].map((percent) => ({ percent, minimumObservationMinutes: 1 })),
    healthThresholds: thresholds,
    rollbackPolicy: {
      automaticOnAnyThresholdBreach: true,
      retainImmutableStableAssets: true,
      restoreStableRuntimeEntries: true,
      databaseRollbackRequired: false,
      apiCompatibility: 'backward-compatible',
    },
    rollbackPackages: {
      '@mango/existing': { version: '1.0.0', action: 'pin' },
      '@mango/new': { version: null, action: 'remove' },
    },
    exerciseSamples: {
      healthy: { errors: 0.1, whiteScreens: 0 },
      breach: { errors: 0.6, whiteScreens: 0 },
    },
  };
  const result = validateRolloutContract(contract, {
    candidateVersions: { '@mango/existing': '1.1.0', '@mango/new': '1.0.0' },
    baseVersions: { '@mango/existing': '1.0.0' },
    localVersions: { '@mango/existing': '1.1.0', '@mango/new': '1.0.0' },
  });
  assert.deepEqual(result.failures, []);
  assert.equal(result.mode, 'candidate');
});

test('rollout contract rejects a stale rollback pin', () => {
  const contract = {
    schemaVersion: 1,
    owner: 'owner',
    approvalMode: 'single-owner',
    scope: 'release-lock',
    trafficStages: [0, 5, 25, 100].map((percent) => ({ percent, minimumObservationMinutes: 1 })),
    healthThresholds: thresholds,
    rollbackPolicy: {
      automaticOnAnyThresholdBreach: true,
      retainImmutableStableAssets: true,
      restoreStableRuntimeEntries: true,
      databaseRollbackRequired: false,
      apiCompatibility: 'backward-compatible',
    },
    rollbackPackages: { '@mango/existing': { version: '0.9.0', action: 'pin' } },
    exerciseSamples: {
      healthy: { errors: 0.1, whiteScreens: 0 },
      breach: { errors: 0.6, whiteScreens: 0 },
    },
  };
  const result = validateRolloutContract(contract, {
    candidateVersions: { '@mango/existing': '1.1.0' },
    baseVersions: { '@mango/existing': '1.0.0' },
    localVersions: { '@mango/existing': '1.1.0' },
  });
  assert.match(result.failures.join('\n'), /rollback must pin 1\.0\.0/);
});

test('rollout contract rejects a local public package missing from the candidate lock', () => {
  const result = validateRolloutContract(
    {
      schemaVersion: 1,
      owner: 'owner',
      approvalMode: 'single-owner',
      scope: 'release-lock',
      trafficStages: [0, 5, 25, 100].map((percent) => ({ percent, minimumObservationMinutes: 1 })),
      healthThresholds: thresholds,
      rollbackPolicy: {
        automaticOnAnyThresholdBreach: true,
        retainImmutableStableAssets: true,
        restoreStableRuntimeEntries: true,
        databaseRollbackRequired: false,
        apiCompatibility: 'backward-compatible',
      },
      rollbackPackages: { '@mango/existing': { version: '1.0.0', action: 'pin' } },
      exerciseSamples: {
        healthy: { errors: 0.1, whiteScreens: 0 },
        breach: { errors: 0.6, whiteScreens: 0 },
      },
    },
    {
      candidateVersions: { '@mango/existing': '1.1.0' },
      baseVersions: { '@mango/existing': '1.0.0' },
      localVersions: { '@mango/existing': '1.1.0', '@mango/unlocked': '1.0.0' },
    },
  );
  assert.match(result.failures.join('\n'), /complete local public package set/);
});
