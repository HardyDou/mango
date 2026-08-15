import assert from 'node:assert/strict';
import test from 'node:test';
import { decideRegistryAction, markRemoteWriteIntent, recoverRemoteWriteAudit } from './release-publication-lib.mjs';

const hash = 'a'.repeat(64);
const absent = { state: 'absent' };
const present = { state: 'present', sha256: hash };

test('both absent roles permit the exact first publish', () => {
  assert.equal(decideRegistryAction({ hosted: absent, consume: absent, expectedSha256: hash }).action, 'PUBLISH');
});

test('hosted present and group absent is read-only verify pending', () => {
  assert.equal(
    decideRegistryAction({ hosted: present, consume: absent, expectedSha256: hash }).action,
    'VERIFY_PENDING',
  );
});

test('matching hosted and group copies skip immutable publication', () => {
  assert.equal(decideRegistryAction({ hosted: present, consume: present, expectedSha256: hash }).action, 'VERIFIED');
});

test('unknown, reversed and hash-mismatched registry states stop', () => {
  assert.equal(
    decideRegistryAction({ hosted: { state: 'unknown' }, consume: absent, expectedSha256: hash }).action,
    'STOP',
  );
  assert.equal(decideRegistryAction({ hosted: absent, consume: present, expectedSha256: hash }).action, 'STOP');
  assert.equal(
    decideRegistryAction({
      hosted: { state: 'present', sha256: 'b'.repeat(64) },
      consume: absent,
      expectedSha256: hash,
    }).action,
    'STOP',
  );
});

test('remote write intent is persisted before an immutable command runs', () => {
  const manifest = { remoteWrites: false };
  markRemoteWriteIntent(manifest, {
    kind: 'npm-publish',
    target: '@mango/example@1.0.0',
    recordedAt: '2026-08-15T00:00:00.000Z',
  });
  assert.equal(manifest.remoteWrites, true);
  assert.deepEqual(manifest.remoteWriteAudit, [
    {
      kind: 'npm-publish',
      target: '@mango/example@1.0.0',
      recordedAt: '2026-08-15T00:00:00.000Z',
    },
  ]);
});

test('published attempt evidence repairs a missing remote write audit flag', () => {
  const manifest = {
    remoteWrites: false,
    packagePublications: {
      '@mango/example': { attempts: [{ exitCode: 0 }] },
    },
  };
  assert.equal(recoverRemoteWriteAudit(manifest, { recordedAt: '2026-08-15T00:01:00.000Z' }), true);
  assert.equal(manifest.remoteWrites, true);
  assert.equal(manifest.remoteWriteAudit[0].kind, 'recovered-publication-audit');
  assert.equal(recoverRemoteWriteAudit(manifest), false);
});

test('audit recovery remains read-only without publication attempt evidence', () => {
  const manifest = { remoteWrites: false, packagePublications: { '@mango/example': { attempts: [] } } };
  assert.equal(recoverRemoteWriteAudit(manifest), false);
  assert.equal(manifest.remoteWrites, false);
  assert.equal(manifest.remoteWriteAudit, undefined);
});
