import assert from 'node:assert/strict';
import { mkdirSync, mkdtempSync, realpathSync, rmSync, symlinkSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import test from 'node:test';

import { toCanonicalRelativePath } from './consumer-tarball-paths.mjs';

test('resolves candidate tarballs against the canonical temporary directory', () => {
  const root = mkdtempSync(join(tmpdir(), 'mango-consumer-tarball-path-'));
  try {
    const canonicalFrontend = join(root, 'private', 'var', 'consumer', 'frontend');
    const aliasedFrontend = join(root, 'var', 'consumer', 'frontend');
    const tarball = join(root, 'Users', 'repo', 'candidate.tgz');
    mkdirSync(canonicalFrontend, { recursive: true });
    mkdirSync(dirname(tarball), { recursive: true });
    writeFileSync(tarball, 'candidate');
    symlinkSync(join(root, 'private', 'var'), join(root, 'var'), 'dir');

    const relativePath = toCanonicalRelativePath(aliasedFrontend, tarball);

    assert.equal(resolve(realpathSync(aliasedFrontend), relativePath), realpathSync(tarball));
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
});
