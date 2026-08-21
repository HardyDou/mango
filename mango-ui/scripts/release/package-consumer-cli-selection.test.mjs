import assert from 'node:assert/strict';
import test from 'node:test';
import { selectConsumerCliMode } from '../package-consumer-cli-selection.mjs';

test('uses source CLI while ordinary consumer candidates are packed locally', () => {
  assert.equal(
    selectConsumerCliMode({
      candidateDirectory: '',
      candidatePackageNames: new Set(['@mango/cli']),
      releaseCandidateMatrix: false,
    }),
    'source',
  );
});

test('uses candidate CLI only for a sealed candidate directory containing the CLI', () => {
  assert.equal(
    selectConsumerCliMode({
      candidateDirectory: '/tmp/candidate',
      candidatePackageNames: new Set(['@mango/cli']),
      releaseCandidateMatrix: false,
    }),
    'candidate',
  );
});

test('uses published CLI for a registry matrix without a candidate CLI tarball', () => {
  assert.equal(
    selectConsumerCliMode({
      candidateDirectory: '',
      candidatePackageNames: new Set(),
      releaseCandidateMatrix: true,
    }),
    'published',
  );
});
