import assert from 'node:assert/strict';
import test from 'node:test';
import { compareMetrics } from './static-gate-lib.mjs';

test('ratchet accepts equal or reduced debt and rejects regression', () => {
  assert.deepEqual(compareMetrics('eslint', { fatal: 0, errors: 3, warnings: 8 }, { fatal: 0, errors: 4, warnings: 8 }), []);
  assert.deepEqual(compareMetrics('eslint', { fatal: 0, errors: 5, warnings: 8 }, { fatal: 0, errors: 4, warnings: 8 }), [
    { metric: 'errors', actual: 5, allowed: 4 },
  ]);
});

test('strict mode requires every metric to be zero', () => {
  assert.deepEqual(compareMetrics('prettier', { files: 1 }, { files: 10 }, true), [
    { metric: 'files', actual: 1, allowed: 0 },
  ]);
});

