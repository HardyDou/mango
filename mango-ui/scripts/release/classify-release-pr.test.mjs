import assert from 'node:assert/strict';
import test from 'node:test';
import { classifyReleasePullRequest } from './classify-release-pr.mjs';

test('machine-generated version projection is release-only', () => {
  const result = classifyReleasePullRequest([
    'mango-ui/.changeset/release-plan.json',
    'mango-ui/.changeset/release-notes.txt',
    'mango-ui/packages/admin/package.json',
    'mango-ui/packages/admin/CHANGELOG.md',
    'mango-ui/packages/mango-cli/release-versions.json',
    'mango-ui/pnpm-lock.yaml',
  ]);
  assert.equal(result.releaseOnly, true);
  assert.deepEqual(result.disallowed, []);
});

test('source mixed into a release PR falls back to normal gates', () => {
  const result = classifyReleasePullRequest([
    'mango-ui/.changeset/release-plan.json',
    'mango-ui/packages/admin/src/index.ts',
  ]);
  assert.equal(result.releaseOnly, false);
  assert.deepEqual(result.disallowed, ['mango-ui/packages/admin/src/index.ts']);
});

test('release notes template governance uses normal gates', () => {
  const result = classifyReleasePullRequest([
    'mango-ui/.changeset/release-plan.json',
    'mango-ui/.changeset/release-notes-template.md',
  ]);
  assert.equal(result.releaseOnly, false);
  assert.deepEqual(result.disallowed, ['mango-ui/.changeset/release-notes-template.md']);
});

test('version-shaped changes without a machine plan are not release-only', () => {
  const result = classifyReleasePullRequest(['mango-ui/packages/admin/package.json']);
  assert.equal(result.releaseOnly, false);
  assert.equal(result.hasPlan, false);
});
