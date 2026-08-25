import assert from 'node:assert/strict';
import { existsSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';
import { acquireReleaseLock } from './release-lock-lib.mjs';

test('release lock is exclusive and never reclaims an existing lock', (t) => {
  const root = mkdtempSync(join(tmpdir(), 'mango-release-lock-'));
  t.after(() => rmSync(root, { recursive: true, force: true }));
  const lock = join(root, 'mango-release/release.lock');
  const release = acquireReleaseLock(lock, { releasePlanDigest: 'a'.repeat(64) });
  assert.equal(existsSync(lock), true);
  assert.match(readFileSync(lock, 'utf8'), /releasePlanDigest/u);
  assert.throws(() => acquireReleaseLock(lock, { releasePlanDigest: 'b'.repeat(64) }), /already exists/u);
  release();
  assert.equal(existsSync(lock), false);
});

test('release callback never removes a lock that no longer belongs to its process', (t) => {
  const root = mkdtempSync(join(tmpdir(), 'mango-release-lock-owner-'));
  t.after(() => rmSync(root, { recursive: true, force: true }));
  const lock = join(root, 'mango-release/release.lock');
  const release = acquireReleaseLock(lock, { releasePlanDigest: 'a'.repeat(64) });
  writeFileSync(lock, '{"lockId":"replacement"}\n');
  release();
  assert.equal(existsSync(lock), true);
});
