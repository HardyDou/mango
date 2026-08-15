import assert from 'node:assert/strict';
import { resolve } from 'node:path';
import test from 'node:test';
import { resolveRepositoryInputPath } from './release-repository-lib.mjs';

test('resolves command input paths from the repository root', () => {
  const repoRoot = resolve('/tmp/mango-release-repository');

  assert.equal(
    resolveRepositoryInputPath(repoRoot, '.runtime/pr-body.md', '.runtime/mango-release/pr-body.md'),
    resolve(repoRoot, '.runtime/pr-body.md'),
  );
  assert.equal(
    resolveRepositoryInputPath(repoRoot, '', '.runtime/mango-release/pr-body.md'),
    resolve(repoRoot, '.runtime/mango-release/pr-body.md'),
  );
});

test('preserves absolute command input paths', () => {
  const repoRoot = resolve('/tmp/mango-release-repository');
  const absolute = resolve('/tmp/mango-release-pr-body.md');

  assert.equal(resolveRepositoryInputPath(repoRoot, absolute, '.runtime/mango-release/pr-body.md'), absolute);
});
