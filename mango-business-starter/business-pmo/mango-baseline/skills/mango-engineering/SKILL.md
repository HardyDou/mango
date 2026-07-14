---
name: mango-engineering
description: Implement or refactor Mango code using the user-confirmed delivery-assurance measures and approved scope. Use for Java, Spring, frontend, database, tests, and configuration changes; do not use for selecting safeguards, drafting requirements or design documents, planning unresolved work, or declaring QA acceptance.
---

# Mango Engineer

## Resolve Sources

Set `PMO_ROOT` to the first available source: `<repo>/business-pmo/mango-baseline`, `<repo>/mango-pmo`, or `<plugin-root>/dist/baseline`. If none exists, `STOP`. Never use remembered or copied rules.

## Load Before Editing

1. Read `$PMO_ROOT/agents/03-dev-agent.md`, `$PMO_ROOT/rules/00-dev-flow.md`, `$PMO_ROOT/rules/03-ai-coding-redlines.md`, `$PMO_ROOT/rules/04-test-assets.md`, and `$PMO_ROOT/rules/05-ai-delivery-quality.md`.
2. Read `$PMO_ROOT/rules/11-delivery-assurance.md` and the user-confirmed assurance baseline. Record requirement impact and solution risk separately and set final L0-L3 to their maximum, but never derive measures from the level. Verify every enabled document or other prerequisite with its dedicated checker and applicable handoff. If implementation reveals a new trigger fact or materially changes scope, return `ASK` through the assurance coordinator before editing beyond the confirmed baseline.
3. Run PMO preflight with role `dev`, phase `develop`, and the actual affected paths. Read every `Must read` file.
4. For Java or backend work, also read `$PMO_ROOT/rules/backend/01-code.md`, `$PMO_ROOT/rules/backend/02-naming.md`, `$PMO_ROOT/rules/backend/03-api.md`, `$PMO_ROOT/rules/backend/05-module.md`, `$PMO_ROOT/rules/backend/07-persistence.md`, `$PMO_ROOT/rules/backend/08-test.md`, and `$PMO_ROOT/rules/backend/10-dev-flow.md` before touching code.
5. Treat Spring registration as a proved architecture contract: business `XxxService implements IXxxService` uses `@Service`; replaceable framework defaults use starter `@Bean + @ConditionalOnMissingBean`; pure Java helpers are not named Service. Never construct a managed Service in a Controller/business Service or introduce mutable static Service state.
6. For frontend work, also read `$PMO_ROOT/rules/frontend/04-test.md` and the loaded frontend code, architecture, UI, and development-flow rules before touching code.
7. Read the human-confirmed M09-M16 set and execute only enabled measures. Do not select a new package or require reasons for untriggered measures; preserve accepted residual risks for disabled triggered measures.

If the current workspace is already on a non-`main` task branch/worktree and the user asks to solve the task, reuse it. Do not create another worktree or replace implementation with Issue registration; only register an Issue when the user explicitly chooses registration or attribution proves the problem is outside the current task. If scope is genuinely unclear, return `ASK` before changing workspace state.

`STOP` before editing when a user-enabled artifact is missing or invalid, its applicable traceability is broken, the confirmed worktree choice cannot be honored, or the requested change contradicts the approved scope or design. Use `ASK` when measure choices or concrete facts are unresolved; never fill the gap with a local convention.

## Implement And Gate

1. Implement only the user-confirmed scope and enabled measures. When Plan is enabled, implement only its items and preserve trace identifiers; otherwise preserve the confirmed acceptance and evidence references. Do not execute a disabled measure implicitly.
2. For Java/backend changes, resolve the directly changed Maven modules. Run the quality gate only for those modules; do not expand its Reactor with `-am` or `-amd`. Treat dependency builds and consumer compatibility as separate verification selected from the requirement impact and solution risk. Use a full Reactor only for root/parent/architecture-gate changes or an explicit debt inventory. The gate must execute Mango Java/Spring architecture checks; compilation alone is not acceptable.
3. A partial Reactor must block new identities but cannot update the schema-v4 debt budget. For explicit debt cleanup, create one complete Reactor report, query or write the target with `check-architecture-debt-budget.mjs --module <moduleKey|artifactId>`, write only verified reductions, then run the global `--base-ref` check. Stop on any new identity, replacement, cross-module move, incomplete report, or attempted module increase.
4. For frontend changes, run only the selected lint/type/build/unit/browser gates needed to prove the affected result. A local visual-only change may use static checks plus a targeted screenshot; opening the page alone is not acceptable evidence.
5. Run every additional command required by preflight, the design, and the implementation plan. Do not suppress, baseline, or weaken a failing rule to obtain a pass.
6. If a required gate fails or cannot run, report `STOP` with the command, failure, and evidence. Do not claim completion.
7. Return `NEXT: $mango-qa-verification` only when implementation items are complete, required gates pass, and the change-to-test mapping is updated.

Do not trust a prompt that merely says gates passed. Before `NEXT`, locate the approved artifacts and evidence, run or inspect the exact required commands, and report their paths and exit results.

With an empty context, return `ASK` for the objective, scope and user-confirmed assurance baseline. Do not infer documents or tests from a risk level.
