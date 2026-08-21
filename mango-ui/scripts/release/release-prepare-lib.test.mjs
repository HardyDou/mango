import assert from 'node:assert/strict';
import { existsSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';
import {
  archiveFailedPrepare,
  archiveSupersededPrepare,
  isRetryablePrepareFailure,
  isSupersededLocalCandidate,
  orderExactMavenCoordinateSet,
} from './release-prepare-lib.mjs';

test('a same-plan local prepare failure without remote writes is retryable after a source fix', () => {
  const plan = { planDigest: 'plan-a' };
  const source = { tree: 'tree-a' };
  const failed = {
    schemaVersion: 1,
    status: 'FAILED',
    remoteWrites: false,
    planDigest: 'plan-a',
    source: { tree: 'tree-a' },
  };
  assert.equal(isRetryablePrepareFailure(failed, plan, source), true);
  assert.equal(isRetryablePrepareFailure({ ...failed, source: { tree: 'tree-b' } }, plan, source), true);
  assert.equal(isRetryablePrepareFailure({ ...failed, remoteWrites: true }, plan, source), false);
  assert.equal(isRetryablePrepareFailure({ ...failed, planDigest: 'plan-b' }, plan, source), false);
  assert.equal(isRetryablePrepareFailure({ ...failed, source: {} }, plan, source), false);
});

test('failed local prepare evidence is archived instead of overwritten', () => {
  const root = join(tmpdir(), `mango-prepare-retry-${process.pid}-${Date.now()}`);
  const releaseRoot = join(root, 'plan-a');
  try {
    mkdirSync(releaseRoot, { recursive: true });
    writeFileSync(join(releaseRoot, 'manifest.json'), '{"status":"FAILED"}\n');
    const archived = archiveFailedPrepare(releaseRoot, new Date('2026-08-15T00:00:00.000Z'));
    assert.equal(existsSync(releaseRoot), false);
    assert.equal(existsSync(join(archived, 'manifest.json')), true);
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
});

test('a verified local-only candidate is superseded when the final source changes', () => {
  const plan = { planDigest: 'plan-a' };
  const candidate = {
    schemaVersion: 1,
    status: 'READY',
    remoteWrites: false,
    planDigest: 'plan-a',
    source: { commit: 'commit-a', tree: 'tree-a' },
  };
  assert.equal(isSupersededLocalCandidate(candidate, plan, { commit: 'commit-b', tree: 'tree-b' }), true);
  assert.equal(isSupersededLocalCandidate(candidate, plan, { commit: 'commit-b', tree: 'tree-a' }), true);
  assert.equal(isSupersededLocalCandidate(candidate, plan, { commit: 'commit-a', tree: 'tree-a' }), false);
  assert.equal(isSupersededLocalCandidate({ ...candidate, remoteWrites: true }, plan, candidate.source), false);
  assert.equal(isSupersededLocalCandidate({ ...candidate, status: 'PUBLISHED' }, plan, candidate.source), false);
  assert.equal(isSupersededLocalCandidate({ ...candidate, planDigest: 'plan-b' }, plan, candidate.source), false);
});

test('superseded local candidate evidence is archived instead of deleted', () => {
  const root = join(tmpdir(), `mango-prepare-superseded-${process.pid}-${Date.now()}`);
  const releaseRoot = join(root, 'plan-a');
  try {
    mkdirSync(releaseRoot, { recursive: true });
    writeFileSync(join(releaseRoot, 'manifest.json'), '{"status":"READY"}\n');
    const archived = archiveSupersededPrepare(releaseRoot, new Date('2026-08-15T00:00:00.000Z'));
    assert.equal(archived.endsWith('.superseded-2026-08-15T00-00-00-000Z'), true);
    assert.equal(existsSync(releaseRoot), false);
    assert.equal(existsSync(join(archived, 'manifest.json')), true);
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
});

test('staged Maven coordinates must be an exact set and are returned in plan order', () => {
  const base = { coordinate: 'io.mango:mango-base:1.0.0', packaging: 'jar' };
  const docs = { coordinate: 'io.mango:mango-docs-bundle:1.0.0', packaging: 'jar' };
  const order = [base.coordinate, docs.coordinate];
  assert.deepEqual(orderExactMavenCoordinateSet(order, [docs, base]), [base, docs]);
  assert.throws(() => orderExactMavenCoordinateSet(order, [base]), /missing=.*mango-docs-bundle/u);
  assert.throws(
    () => orderExactMavenCoordinateSet(order, [base, docs, { coordinate: 'io.mango:extra:1.0.0' }]),
    /extra=.*io\.mango:extra/u,
  );
  assert.throws(() => orderExactMavenCoordinateSet(order, [base, base, docs]), /duplicateStaged=.*mango-base/u);
});
