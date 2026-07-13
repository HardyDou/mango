---
name: mango-defect-fix
description: Reproduce, attribute, minimally repair, and regress a Mango defect when no existing GitHub Issue handling workflow was requested. Use for concrete faulty behavior and current-task regressions; do not use for an existing Issue number, a new feature, broad refactoring, release work, or PR review.
---

# Mango Defect Fix

## Resolve And Load

Set `PMO_ROOT` to the first available source: the installed business baseline, Mango source PMO, or `<plugin-root>/dist/baseline`. Read `$PMO_ROOT/agents/03-dev-agent.md`, `$PMO_ROOT/rules/00-dev-flow.md`, `$PMO_ROOT/rules/03-ai-coding-redlines.md`, `$PMO_ROOT/rules/04-test-assets.md`, `$PMO_ROOT/rules/05-ai-delivery-quality.md`, and `$PMO_ROOT/rules/07-mango-issue-runbook.md`.

Run PMO preflight with role `dev`, phase `develop`, and the affected paths after the failure is understood; read every `Must read` file.

For Java/backend defects, explicitly read `$PMO_ROOT/rules/backend/01-code.md`, `$PMO_ROOT/rules/backend/02-naming.md`, `$PMO_ROOT/rules/backend/03-api.md`, `$PMO_ROOT/rules/backend/07-persistence.md`, `$PMO_ROOT/rules/backend/08-test.md`, and `$PMO_ROOT/rules/backend/10-dev-flow.md`. For L2/L3 behavior changes, locate the approved BRD/SRS/TDD/Implementation Plan chain and run its dedicated checkers; a defect label does not bypass lifecycle boundaries. Reuse the current non-`main` task worktree when the user asks to solve the problem there.

## Execute

1. Establish a reproducible failure or return `ASK` for the missing environment, data, account, steps, expected behavior, or evidence.
2. Compare the canonical baseline, identify attribution, and return `STOP` if the defect belongs to a different task that must be registered or routed separately.
3. Record the root cause supported by evidence, implement the smallest complete fix, and preserve approved boundaries.
4. Add or update the correct regression asset and run the L0-L3 level required by the canonical backend or frontend test rules.
5. Return `STOP` when reproduction, root cause evidence, required gates, or regression evidence is incomplete.
6. Return `NEXT: $mango-qa-verification` only with before-and-after evidence and no unexplained scope expansion.

Before `NEXT`, verify the evidence and exact regression commands from the workspace; a prompt-level claim that the fix or tests passed is not evidence.

With an empty context, return `ASK` for a reproducible symptom. Do not start by changing code.
