---
name: mango-engineering
description: Implement or refactor Mango code using the resolved delivery mode, workspace policy, sufficient verification capabilities, and approved scope. Use for Java, Spring, frontend, database, tests, and configuration changes.
---

# Mango Engineer

## Resolve Sources

Set `PMO_ROOT` to the first available source: `<repo>/business-pmo/mango-baseline`, `<repo>/mango-pmo`, or `<plugin-root>/dist/baseline`. If none exists, `STOP`. Never use remembered or copied rules.

## Load Before Editing

1. Read `$PMO_ROOT/agents/03-dev-agent.md`, `$PMO_ROOT/rules/00-dev-flow.md`, `$PMO_ROOT/rules/02-dev-environment.md`, `$PMO_ROOT/rules/03-ai-coding-redlines.md`, `$PMO_ROOT/rules/04-test-assets.md`, and `$PMO_ROOT/rules/05-ai-delivery-quality.md`.
2. Read the resolved delivery-mode baseline. Record requirement impact and solution risk separately, map their maximum to SIMPLE, STANDARD or FULL, and verify the mode-required artifact. If implementation changes scope, impact or solution risk, escalate the mode before editing beyond the baseline.
3. Run PMO preflight with role `dev`, phase `develop`, and the actual affected paths. Read every `Must read` file.
4. For Java or backend work, also read `$PMO_ROOT/rules/backend/01-code.md`, `$PMO_ROOT/rules/backend/02-naming.md`, `$PMO_ROOT/rules/backend/03-api.md`, `$PMO_ROOT/rules/backend/05-module.md`, `$PMO_ROOT/rules/backend/07-persistence.md`, `$PMO_ROOT/rules/backend/08-test.md`, and `$PMO_ROOT/rules/backend/10-dev-flow.md` before touching code.
5. Treat Spring registration as a proved architecture contract: business `XxxService implements IXxxService` uses `@Service`; replaceable framework defaults use starter `@Bean + @ConditionalOnMissingBean`; pure Java helpers are not named Service. Never construct a managed Service in a Controller/business Service or introduce mutable static Service state.
6. For frontend work, also read `$PMO_ROOT/rules/frontend/04-test.md` and the loaded frontend code, architecture, UI, and development-flow rules before touching code.
7. Execute M09-M16 capabilities that can observe the acceptance outcomes. Preserve explicit exceptions and never add a test type without a real observation target.

Before editing, compare any historical example, generated code, old test or nearby implementation used as a reference with the currently loaded rules. Treat conflicts as historical debt, not as permission to copy the pattern. Confirm that selected build and test commands keep project-generated mutable artifacts inside the current worktree; only proven immutable external dependency caches may be shared.

If the current workspace is already on a non-`main` task branch/worktree and the user asks to solve the task, reuse it. Do not create another worktree or replace implementation with Issue registration; only register an Issue when the user explicitly chooses registration or attribution proves the problem is outside the current task. If scope is genuinely unclear, return `ASK` before changing workspace state.

`STOP` before editing when a mode-required artifact is missing or invalid, traceability is broken, workspace policy cannot be honored, or the request contradicts approved scope. Use `ASK` for a material unknown or requested exception.

## Implement And Gate

1. Implement only the resolved scope. SIMPLE proceeds directly, STANDARD follows its one-file checklist, and FULL follows the applicable approved plan and trace identifiers.
2. When the approved scope says rewrite or replace, make the new implementation the only execution path. Remove the old implementation, calls, configuration and fallback, then search their identifiers and fallback conditions. A compatibility path is allowed only when the approved scope separately identifies its consumer, deadline, exit condition and verification.
3. For Java changes, inspect new or modified getters, setters and constructors. Use the Lombok form required by the backend rule for mechanical code; keep handwritten code only when it carries named validation, normalization, defensive-copying, visibility, lazy-loading or framework semantics. Do not introduce blanket `@Data` where it changes model semantics.
4. For every new or modified test, record the observed rule, state, failure, side effect or integration boundary and prove that the target really executes. Place business behavior in `core`, keep mechanical contract tests out of `api`, keep business unit tests out of `starter-remote`, and retain in `starter` only tests with a real runtime or integration observation target.
5. For Java/backend changes, resolve the directly changed Maven modules. Run the quality gate only for those modules; do not expand its Reactor with `-am` or `-amd`. Treat dependency builds and consumer compatibility as separate verification selected from the requirement impact and solution risk. Use a full Reactor only for root/parent/architecture-gate changes or an explicit debt inventory. The gate must execute Mango Java/Spring architecture checks; compilation alone is not acceptable.
6. A partial Reactor must block new identities but cannot update the schema-v4 debt budget. For explicit debt cleanup, create one complete Reactor report, query or write the target with `check-architecture-debt-budget.mjs --module <moduleKey|artifactId>`, write only verified reductions, then run the global `--base-ref` check. Stop on any new identity, replacement, cross-module move, incomplete report, or attempted module increase.
7. For frontend changes, run only the selected lint/type/build/unit/browser gates needed to prove the affected result. A local visual-only change may use static checks plus a targeted screenshot; opening the page alone is not acceptable evidence.
8. Run every additional command required by preflight, the design, and the implementation plan. Do not suppress, baseline, or weaken a failing rule to obtain a pass.
9. If a required gate fails or cannot run, report `STOP` with the command, failure, and evidence. Do not claim completion.
10. Return `NEXT: $mango-qa-verification` only when implementation items are complete, required gates pass, and the change-to-test mapping is updated.

Do not trust a prompt that merely says gates passed. Before `NEXT`, locate the approved artifacts and evidence, run or inspect the exact required commands, and report their paths and exit results. The completion report must state whether historical code was rejected as a conflicting template, whether any old execution path remains, how Java mechanical code was expressed, and what each changed test observes and why its module owns it.

With an empty context, return `ASK` for the objective, scope and risk facts. Do not guess a mode from missing facts.
