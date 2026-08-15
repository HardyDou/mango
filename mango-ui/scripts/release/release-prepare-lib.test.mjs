import assert from 'node:assert/strict';
import { existsSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';
import { archiveFailedPrepare, isRetryablePrepareFailure } from './release-prepare-lib.mjs';

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
