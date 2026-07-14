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
test('current single-owner policy preserves fast contract, heavy PMO, and history protections', () => {
  assert.deepEqual(validateBranchProtectionPolicy(currentPolicy), []);
});

for (const context of ['pr-contract-check', 'pmo-doc-check']) {
  test(`single-owner policy cannot disable the required ${context} check`, () => {
    const policy = structuredClone(currentPolicy);
    policy.requiredStatusChecks.contexts = policy.requiredStatusChecks.contexts.filter(
      candidate => candidate !== context,
    );
    assert.match(validateBranchProtectionPolicy(policy).join('\n'), new RegExp(context));
  });
}

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

test('GitHub branch protection evidence normalizes check records to stable contexts', () => {
  const evidence = {
    ...structuredClone(currentPolicy),
    requiredStatusChecks: {
      strict: true,
      checks: currentPolicy.requiredStatusChecks.contexts.map(context => ({ context, appId: 15368 })),
    },
  };
  assert.deepEqual(normalizeBranchProtectionEvidence(evidence), currentPolicy);
});

test('PR body edits run only the trusted lightweight contract gate', () => {
  const workflow = fs.readFileSync(path.join(root, '.github/workflows/pr-contract-check.yml'), 'utf8');
  const capabilityChecker = fs.readFileSync(
    path.join(root, 'mango-pmo/tools/check-capability-docs.mjs'),
    'utf8',
  );
  assert.match(workflow, /types: \[opened, edited, reopened, synchronize, ready_for_review\]/);
  assert.match(workflow, /ref: \$\{\{ github\.event\.pull_request\.base\.sha \}\}/);
  assert.match(workflow, /sparse-checkout: \|[\s\S]*?check-capability-docs\.mjs[\s\S]*?risk-verification\.mjs/);
  assert.match(workflow, /git fetch --no-tags origin "\$BASE_SHA" "\$HEAD_SHA"/);
  assert.match(workflow, /node mango-pmo\/tools\/risk-verification\.mjs/);
  assert.match(workflow, /node mango-pmo\/tools\/check-capability-docs\.mjs[\s\S]*?--base \$\{\{ github\.event\.pull_request\.base\.sha \}\}[\s\S]*?--head \$\{\{ github\.event\.pull_request\.head\.sha \}\}/);
  assert.match(workflow, /group: pr-contract-check-\$\{\{ github\.event\.pull_request\.number \}\}/);
  assert.match(workflow, /cancel-in-progress: true/);
  assert.doesNotMatch(workflow, /setup-node|pnpm|setup-java|\bmvn\b/u);
  assert.match(capabilityChecker, /const shouldValidatePrBody = Boolean\(prBodyPath\);/);
  assert.doesNotMatch(
    capabilityChecker,
    /shouldValidatePrBody\s*=\s*Boolean\(prBodyPath\)\s*\|\|\s*Boolean\(process\.env\.GITHUB_ACTIONS\)/,
  );
});
