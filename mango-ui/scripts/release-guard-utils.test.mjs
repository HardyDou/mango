import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';

import { verifyPackageTree } from './release-guard-utils.mjs';

function withPackage(t, files) {
  const root = mkdtempSync(join(tmpdir(), 'mango-release-contract-'));
  t.after(() => rmSync(root, { recursive: true, force: true }));
  for (const [relativePath, content] of Object.entries(files)) {
    const target = join(root, relativePath);
    mkdirSync(join(target, '..'), { recursive: true });
    writeFileSync(target, content);
  }
  return root;
}

test('release contract accepts required runtime code in a matching generated chunk', (t) => {
  const packageRoot = withPackage(t, {
    'dist/featureRegistrars-a1b2c3.js': 'registerMangoAuthProfileSections(registration.profileSections || []);',
  });

  assert.doesNotThrow(() =>
    verifyPackageTree(
      '@mango/admin-shell',
      packageRoot,
      {},
      {
        contract: {
          containsMatching: [{ pattern: 'dist/featureRegistrars-*.js', text: 'registerMangoAuthProfileSections' }],
        },
      },
    ),
  );
});

test('release contract rejects a missing generated chunk', (t) => {
  const packageRoot = withPackage(t, { 'dist/index.js': 'export {};\n' });

  assert.throws(
    () =>
      verifyPackageTree(
        '@mango/admin-shell',
        packageRoot,
        {},
        {
          contract: {
            containsMatching: [{ pattern: 'dist/featureRegistrars-*.js', text: 'registerMangoAuthProfileSections' }],
          },
        },
      ),
    /does not contain a file matching release contract pattern/u,
  );
});

test('release contract rejects a matching chunk without the required runtime code', (t) => {
  const packageRoot = withPackage(t, { 'dist/featureRegistrars-deadbeef.js': 'export {};\n' });

  assert.throws(
    () =>
      verifyPackageTree(
        '@mango/admin-shell',
        packageRoot,
        {},
        {
          contract: {
            containsMatching: [{ pattern: 'dist/featureRegistrars-*.js', text: 'registerMangoAuthProfileSections' }],
          },
        },
      ),
    /do not contain required text/u,
  );
});
