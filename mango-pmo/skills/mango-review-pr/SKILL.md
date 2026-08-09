---
name: mango-review-pr
description: Review an existing Mango pull request for defects, regressions, governance violations, missing tests, and merge blockers using repository evidence and relevant expert perspectives. Do not use to implement requested changes, create a PR, review an uncommitted local diff, or publish a release.
---

# Mango PR Review

## Resolve And Load

Set `PMO_ROOT` to the first available source: the installed business baseline, Mango source PMO, or `<plugin-root>/dist/baseline`. Read `$PMO_ROOT/rules/05-ai-delivery-quality.md`, `$PMO_ROOT/rules/01-delivery-contract.md`, and the QA, backend, frontend, security, documentation, or release rules selected by the PR diff.

## Execute

1. Require a PR number or URL. With none, return `ASK` and do not review a guessed branch.
2. Read the PR base, head, latest commit, full changed-file list, diff, checks, review state, linked Issue, delivery artifacts, and `.github/branch-protection-policy.json` when present before forming findings.
3. Run PMO preflight with phase `verify` and the actual changed paths; read every `Must read` file.
4. Select the expert perspectives required by the canonical PR gate, then review behavior, contracts, boundaries, tests, evidence, compatibility, and release impact from the diff. For Java architecture or debt-budget changes, require one complete Reactor report, exact module ownership, non-increasing module identities, and the global base -> PR -> current check; unchanged total count cannot hide a cross-module move.
5. Lead with findings ordered by severity and grounded in file and line references. Separate blockers, non-blocking findings, open questions, and residual test risk.
6. Apply the declared repository governance mode. `single-owner` requires the machine gates, resolved conversations, Owner authorization, and zero impossible self-approval requirements; `multi-maintainer` additionally requires the configured independent approval. Return `STOP` when the policy and live protection differ, a merge blocker exists, or a required gate is unverified. Return `NEXT` only when the merge conditions defined by the loaded rules are met.

Do not accept a prompt-level claim that review and checks passed. Require the PR number/URL and inspect the actual base, head, diff, checks and expert findings before `NEXT`.

Do not modify code during review. Route requested fixes back to the existing task worktree and branch.
