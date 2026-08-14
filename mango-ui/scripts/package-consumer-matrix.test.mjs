import assert from 'node:assert/strict';
import test from 'node:test';

import { classifyRegistryVersionResult } from './package-consumer-matrix.mjs';

test('classifies an exact registry version as published', () => {
  assert.equal(
    classifyRegistryVersionResult({ status: 0, stdout: '1.0.59\n', stderr: '' }, '1.0.59'),
    'published',
  );
});

test('classifies only an explicit registry 404 as an unpublished candidate', () => {
  assert.equal(
    classifyRegistryVersionResult(
      { status: 1, stdout: '', stderr: 'npm error code E404\nnpm error 404 Not Found' },
      '1.0.60',
    ),
    'candidate',
  );
});

test('rejects registry outages instead of treating them as unpublished candidates', () => {
  assert.throws(
    () =>
      classifyRegistryVersionResult(
        { status: 1, stdout: '', stderr: 'npm error code E500\nnpm error 500 server error' },
        '1.0.60',
      ),
    /registry lookup failed/u,
  );
});

test('rejects a registry response for a different version', () => {
  assert.throws(
    () => classifyRegistryVersionResult({ status: 0, stdout: '1.0.58\n', stderr: '' }, '1.0.59'),
    /expected 1\.0\.59/u,
  );
});
