import assert from 'node:assert/strict';
import { mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';

import { assertReleaseNodeVersion } from '../src/release-runtime.mjs';

test('release runtime accepts only the governed Node engine range', (t) => {
  const root = mkdtempSync(join(tmpdir(), 'mango-release-runtime-'));
  t.after(() => rmSync(root, { recursive: true, force: true }));
  const manifestPath = join(root, 'package.json');
  writeFileSync(manifestPath, JSON.stringify({ engines: { node: '>=22.23.1 <23' } }));

  assert.deepEqual(assertReleaseNodeVersion({ manifestPath, nodeVersion: '22.23.1' }), {
    expectedRange: '>=22.23.1 <23',
    nodeVersion: '22.23.1',
  });
  assert.doesNotThrow(() => assertReleaseNodeVersion({ manifestPath, nodeVersion: '22.99.0' }));
  assert.throws(
    () => assertReleaseNodeVersion({ manifestPath, nodeVersion: '22.22.1' }),
    /requires Node >=22\.23\.1 <23/u,
  );
  assert.throws(
    () => assertReleaseNodeVersion({ manifestPath, nodeVersion: '24.13.0' }),
    /requires Node >=22\.23\.1 <23/u,
  );
});

test('release runtime fails closed when the governed range is malformed', (t) => {
  const root = mkdtempSync(join(tmpdir(), 'mango-release-runtime-'));
  t.after(() => rmSync(root, { recursive: true, force: true }));
  const manifestPath = join(root, 'package.json');
  writeFileSync(manifestPath, JSON.stringify({ engines: { node: '^22.23.1' } }));

  assert.throws(() => assertReleaseNodeVersion({ manifestPath, nodeVersion: '22.23.1' }), /must use the governed/u);
});
