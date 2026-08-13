import assert from 'node:assert/strict';
import test from 'node:test';

import { classifyNpmBatchRecovery } from './npm-batch-recovery.mjs';

const exists = (version) => ({ status: 0, stdout: `${version}\n`, stderr: '' });
const absent = () => ({ status: 1, stdout: '', stderr: 'npm error code E404 No match found' });
const failed = () => ({ status: 1, stdout: '', stderr: 'npm error code E500 Server Error' });

test('verifies an immutable coordinate only when both registries contain it', () => {
  assert.equal(classifyNpmBatchRecovery(exists('1.2.3'), exists('1.2.3'), '1.2.3'), 'verify-existing');
});

test('publishes only when both registries prove the coordinate is absent', () => {
  assert.equal(classifyNpmBatchRecovery(absent(), absent(), '1.2.3'), 'publish-absent');
});

test('stops on partial propagation or an ambiguous registry failure', () => {
  assert.equal(classifyNpmBatchRecovery(exists('1.2.3'), absent(), '1.2.3'), 'stop');
  assert.equal(classifyNpmBatchRecovery(absent(), failed(), '1.2.3'), 'stop');
});
