---
name: mango-engineering
description: Implement or refactor Mango code at the governed L0-L3 level, requiring an approved Technical Design Document and Implementation Plan for L2/L3. Use for Java, Spring, frontend, database, tests, and configuration changes; do not use for drafting requirements or design documents, planning unresolved work, or declaring QA acceptance.
---

# Mango Engineer

## Resolve Sources

Set `PMO_ROOT` to the first available source: `<repo>/business-pmo/mango-baseline`, `<repo>/mango-pmo`, or `<plugin-root>/dist/baseline`. If none exists, `STOP`. Never use remembered or copied rules.

## Load Before Editing

1. Read `$PMO_ROOT/agents/03-dev-agent.md`, `$PMO_ROOT/rules/00-dev-flow.md`, `$PMO_ROOT/rules/03-ai-coding-redlines.md`, `$PMO_ROOT/rules/04-test-assets.md`, and `$PMO_ROOT/rules/05-ai-delivery-quality.md`.
2. Establish and record the L0-L3 level from repository facts and the canonical test rule. L2/L3 requires the approved BRD/SRS/TDD/Plan chain: locate the TDD and Plan, run both dedicated document checkers and verify the staged lifecycle handoff. L0/L1 requires an approved lightweight task scope, acceptance statement and risk-appropriate verification record; do not fabricate four stage documents.
3. Run PMO preflight with role `dev`, phase `develop`, and the actual affected paths. Read every `Must read` file.
4. For Java or backend work, also read `$PMO_ROOT/rules/backend/01-code.md`, `$PMO_ROOT/rules/backend/02-naming.md`, `$PMO_ROOT/rules/backend/03-api.md`, `$PMO_ROOT/rules/backend/05-module.md`, `$PMO_ROOT/rules/backend/07-persistence.md`, `$PMO_ROOT/rules/backend/08-test.md`, and `$PMO_ROOT/rules/backend/10-dev-flow.md` before touching code.
5. Treat Spring registration as a proved architecture contract: business `XxxService implements IXxxService` uses `@Service`; replaceable framework defaults use starter `@Bean + @ConditionalOnMissingBean`; pure Java helpers are not named Service. Never construct a managed Service in a Controller/business Service or introduce mutable static Service state.
6. For frontend work, also read `$PMO_ROOT/rules/frontend/04-test.md` and the loaded frontend code, architecture, UI, and development-flow rules before touching code.
7. Use the L0-L3 verification level defined by those canonical test and delivery rules; record the selected level and required evidence before implementation.

If the current workspace is already on a non-`main` task branch/worktree and the user asks to solve the task, reuse it. Do not create another worktree or replace implementation with Issue registration; only register an Issue when the user explicitly chooses registration or attribution proves the problem is outside the current task. If scope is genuinely unclear, return `ASK` before changing workspace state.

`STOP` before editing when required risk-appropriate artifacts are missing or invalid, L2/L3 traceability is broken, the worktree policy is not satisfied, or the requested change contradicts the approved scope or design. Use `ASK` only for a concrete unresolved fact; never fill the gap with a local convention.

## Implement And Gate

1. Implement only the approved risk-appropriate scope. For L2/L3, implement only Plan items and preserve their trace identifiers; for L0/L1, implement only the approved lightweight task and preserve its acceptance/evidence reference.
2. For any Java or backend change, run the affected tests and Maven `verify` from the correct reactor with required dependencies. This gate must execute the Mango Java/Spring architecture checks; compilation alone is not acceptable.
3. For any frontend change, run the lint, type/build, test, and affected browser gates required by the loaded frontend rules. Opening the page alone is not acceptable.
4. Run every additional command required by preflight, the design, and the implementation plan. Do not suppress, baseline, or weaken a failing rule to obtain a pass.
5. If a required gate fails or cannot run, report `STOP` with the command, failure, and evidence. Do not claim completion.
6. Return `NEXT: $mango-qa-verification` only when implementation items are complete, required gates pass, and the change-to-test mapping is updated.

Do not trust a prompt that merely says gates passed. Before `NEXT`, locate the approved artifacts and evidence, run or inspect the exact required commands, and report their paths and exit results.

With an empty context, return `STOP` and request the risk level and its required approved artifacts. Missing TDD or Plan is always blocking for L2/L3.
