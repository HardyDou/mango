import assert from 'node:assert/strict';
import test from 'node:test';
import { decideRegistryAction } from './release-publication-lib.mjs';

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
