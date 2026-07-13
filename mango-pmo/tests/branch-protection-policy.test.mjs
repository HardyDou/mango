import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import {
  normalizeBranchProtectionEvidence,
  validateBranchProtectionPolicy
} from '../tools/branch-protection-policy.mjs';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const currentPolicy = JSON.parse(fs.readFileSync(path.join(root, '.github/branch-protection-policy.json'), 'utf8'));
const currentEvidence = JSON.parse(fs.readFileSync(
  path.join(root, 'mango-docs/evidence/governance/main-branch-protection-2026-07-13.json'),
  'utf8',
));

test('current single-owner policy preserves machine and history protections', () => {
  assert.deepEqual(validateBranchProtectionPolicy(currentPolicy), []);
  assert.deepEqual(normalizeBranchProtectionEvidence(currentEvidence), currentPolicy);
});

test('single-owner policy cannot disable the required PMO check', () => {
  const policy = structuredClone(currentPolicy);
  policy.requiredStatusChecks.contexts = [];
  assert.match(validateBranchProtectionPolicy(policy).join('\n'), /pmo-doc-check/);
});

test('single-owner policy cannot require an impossible self-approval', () => {
  const policy = structuredClone(currentPolicy);
  policy.pullRequestReviews.requireCodeOwnerReviews = true;
  policy.pullRequestReviews.requiredApprovingReviewCount = 1;
  assert.match(validateBranchProtectionPolicy(policy).join('\n'), /must not require an approval/);
});

test('multi-maintainer policy requires an independent Code Owner approval', () => {
  const policy = structuredClone(currentPolicy);
  policy.governanceMode = 'multi-maintainer';
  policy.pullRequestReviews.requireCodeOwnerReviews = true;
  policy.pullRequestReviews.requiredApprovingReviewCount = 1;
  assert.deepEqual(validateBranchProtectionPolicy(policy), []);
});
