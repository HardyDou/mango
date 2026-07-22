import assert from 'node:assert/strict';
import test from 'node:test';
import { resolveHealthPollIntervalMs } from '../src/dev-health-policy.mjs';

test('health polling defaults to 500 milliseconds', () => {
  assert.equal(resolveHealthPollIntervalMs(undefined), 500);
  assert.equal(resolveHealthPollIntervalMs('invalid'), 500);
  assert.equal(resolveHealthPollIntervalMs(0), 500);
});

test('health polling accepts explicit values and enforces a safe minimum', () => {
  assert.equal(resolveHealthPollIntervalMs(1000), 1000);
  assert.equal(resolveHealthPollIntervalMs('250'), 250);
  assert.equal(resolveHealthPollIntervalMs(10), 100);
});
