import assert from 'node:assert/strict';
import test from 'node:test';
import { advisoryRecord, isBlockingPrCommand } from './pr-quality-policy.mjs';

test('phase-one PR policy blocks affected workspace build failures', () => {
  assert.equal(isBlockingPrCommand('pnpm', ['--filter', 'admin-app', '-r', 'build']), true);
});

test('phase-one PR policy reports non-runtime checks as advisory', () => {
  for (const command of [
    ['pnpm', ['lint']],
    ['pnpm', ['typecheck']],
    ['pnpm', ['--filter', 'admin-app', '-r', 'test']],
    ['pnpm', ['component-contracts:check']],
  ]) {
    assert.equal(isBlockingPrCommand(...command), false);
  }
  assert.deepEqual(advisoryRecord('pnpm', ['lint'], 1), { command: 'pnpm lint', status: 1 });
});
