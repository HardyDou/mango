import assert from 'node:assert/strict';
import test from 'node:test';
import { releaseScopeCheckCommands } from './local-release-checks-lib.mjs';

test('release-only local verification uses the same plan gate as the Runner', () => {
  assert.deepEqual(releaseScopeCheckCommands({ releaseOnly: true, base: 'base-sha', head: 'head-sha' }), [
    { executable: 'pnpm', args: ['release:pr-check'] },
  ]);
});

test('ordinary source verification retains Changeset intent and plan checks', () => {
  assert.deepEqual(releaseScopeCheckCommands({ releaseOnly: false, base: 'base-sha', head: 'head-sha' }), [
    {
      executable: 'pnpm',
      args: ['release:change-check', '--', '--base=base-sha', '--head=head-sha'],
    },
    { executable: 'pnpm', args: ['release:plan:check'] },
  ]);
});
