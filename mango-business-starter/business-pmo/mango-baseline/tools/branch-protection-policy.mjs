const REQUIRED_CONTEXT = 'pmo-doc-check';
const GOVERNANCE_MODES = new Set(['single-owner', 'multi-maintainer']);

export function validateBranchProtectionPolicy(policy) {
  const failures = [];

  if (policy?.schemaVersion !== 1) {
    failures.push('schemaVersion must be 1');
  }
  if (!policy?.repository || !policy?.branch) {
    failures.push('repository and branch are required');
  }
  if (!GOVERNANCE_MODES.has(policy?.governanceMode)) {
    failures.push('governanceMode must be single-owner or multi-maintainer');
  }
  if (policy?.requiredStatusChecks?.strict !== true) {
    failures.push('required status checks must be strict');
  }
  if (!policy?.requiredStatusChecks?.contexts?.includes(REQUIRED_CONTEXT)) {
    failures.push(`required status checks must include ${REQUIRED_CONTEXT}`);
  }
  if (policy?.requiredConversationResolution !== true) {
    failures.push('conversation resolution must remain required');
  }
  if (policy?.enforceAdmins !== true) {
    failures.push('branch protection must apply to administrators');
  }
  if (policy?.allowForcePushes !== false) {
    failures.push('force pushes must remain disabled');
  }
  if (policy?.allowDeletions !== false) {
    failures.push('branch deletion must remain disabled');
  }

  const reviews = policy?.pullRequestReviews;
  if (typeof reviews?.dismissStaleReviews !== 'boolean' || typeof reviews?.requireLastPushApproval !== 'boolean') {
    failures.push('review policy must declare stale-review and last-push behavior');
  }
  if (policy?.governanceMode === 'single-owner') {
    if (reviews?.requireCodeOwnerReviews !== false || reviews?.requiredApprovingReviewCount !== 0) {
      failures.push('single-owner mode must not require an approval the PR author cannot provide');
    }
  }
  if (policy?.governanceMode === 'multi-maintainer') {
    if (reviews?.requireCodeOwnerReviews !== true || !Number.isInteger(reviews?.requiredApprovingReviewCount) || reviews.requiredApprovingReviewCount < 1) {
      failures.push('multi-maintainer mode must require at least one Code Owner approval');
    }
  }

  return failures;
}

export function normalizeBranchProtectionEvidence(evidence) {
  return {
    schemaVersion: evidence?.schemaVersion,
    repository: evidence?.repository,
    branch: evidence?.branch,
    governanceMode: evidence?.governanceMode,
    requiredStatusChecks: {
      strict: evidence?.requiredStatusChecks?.strict,
      contexts: evidence?.requiredStatusChecks?.checks?.map(check => check.context)
    },
    pullRequestReviews: {
      requireCodeOwnerReviews: evidence?.pullRequestReviews?.requireCodeOwnerReviews,
      requiredApprovingReviewCount: evidence?.pullRequestReviews?.requiredApprovingReviewCount,
      dismissStaleReviews: evidence?.pullRequestReviews?.dismissStaleReviews,
      requireLastPushApproval: evidence?.pullRequestReviews?.requireLastPushApproval
    },
    requiredConversationResolution: evidence?.requiredConversationResolution,
    enforceAdmins: evidence?.enforceAdmins,
    allowForcePushes: evidence?.allowForcePushes,
    allowDeletions: evidence?.allowDeletions
  };
}
