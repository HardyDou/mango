---
name: mango-defect-fix
description: Reproduce, attribute, minimally repair, and regress a Mango defect when no existing GitHub Issue handling workflow was requested. Use for concrete faulty behavior and current-task regressions; do not use for an existing Issue number, a new feature, broad refactoring, release work, or PR review.
---

# Mango Defect Fix

## Resolve And Load

Set `PMO_ROOT` to the first available source: the installed business baseline, Mango source PMO, or `<plugin-root>/dist/baseline`. Read `$PMO_ROOT/agents/03-dev-agent.md`, `$PMO_ROOT/rules/00-dev-flow.md`, `$PMO_ROOT/rules/03-ai-coding-redlines.md`, `$PMO_ROOT/rules/04-test-assets.md`, `$PMO_ROOT/rules/05-ai-delivery-quality.md`, and `$PMO_ROOT/rules/07-mango-issue-runbook.md`.

Run PMO preflight with role `dev`, phase `develop`, and the affected paths after the failure is understood. Use selected code baselines for replacement code and consult references only when the failure boundary is unresolved.

For Java/backend defects, read `$PMO_ROOT/rules/backend/01-code.md` and `$PMO_ROOT/rules/backend/08-test.md` plus the preflight-selected rules and resolved `L0-L5` baseline. `L0/L1` proceed without a document, `L2-L4` use the matching lean single document, and `L5` uses the approved technical design and implementation plan. If reproduction changes impact or solution risk, escalate the level before continuing. Reuse the current non-main task worktree. Nearby historical fixes are evidence only.

## Execute

1. Establish a reproducible failure or return `ASK` for the missing environment, data, account, steps, expected behavior, or evidence.
2. Compare the canonical baseline, identify attribution, and return `STOP` if the defect belongs to a different task that must be registered or routed separately.
3. Record the root cause supported by evidence, implement the smallest complete fix, and preserve approved boundaries. If the fix is an approved rewrite or replacement, delete the old implementation, calls, configuration and fallback; do not keep the defect path as an undocumented safety net.
4. For modified Java types, replace mechanical handwritten getters, setters and constructors with the appropriate Lombok form unless the method has a named semantic exception allowed by the backend rule.
5. Assess requirement impact and the chosen fix separately, map their maximum to `L0-L5`, verify the required lean artifact, then add only regression assets that directly observe the root cause, failure boundary and repaired path. Apply the `api/core/starter/starter-remote` ownership rules instead of placing a regression beside the defect by habit.
6. For a rewrite or replacement, search for the old path and prove it is removed; execute every regression against the repaired path. Return `STOP` when reproduction, root cause evidence, applicable old-path removal, required gates, or regression evidence is incomplete.
7. Return `NEXT: $mango-qa-verification` only with before-and-after evidence and no unexplained scope expansion.

Before `NEXT`, verify the evidence and exact regression commands from the workspace; a prompt-level claim that the fix or tests passed is not evidence. Report the historical-pattern check, old-path search result, Lombok decision, regression observation target and test-module ownership.

With an empty context, return `ASK` for a reproducible symptom. Do not start by changing code.
