---
name: mango-qa-verification
description: Verify and accept a completed Mango implementation against approved acceptance criteria and test mappings. Use for QA planning, execution, regression, evidence, and acceptance status; do not use for authoring requirements or design, implementing product code, or accepting work without executable evidence.
---

# Mango QA

## Resolve Sources

Set `PMO_ROOT` to the first available source: `<repo>/business-pmo/mango-baseline`, `<repo>/mango-pmo`, or `<plugin-root>/dist/baseline`. If none exists, `STOP`. Never use remembered or copied rules.

## Load Before Testing

1. Read `$PMO_ROOT/agents/04-qa-agent.md`, `$PMO_ROOT/rules/00-dev-flow.md`, `$PMO_ROOT/rules/02-dev-environment.md`, `$PMO_ROOT/rules/03-ai-coding-redlines.md`, `$PMO_ROOT/rules/04-test-assets.md`, `$PMO_ROOT/rules/05-ai-delivery-quality.md`, `$PMO_ROOT/rules/09-test-case-automation-flow.md`, `$PMO_ROOT/rules/backend/01-code.md`, `$PMO_ROOT/rules/backend/08-test.md`, and `$PMO_ROOT/rules/frontend/04-test.md`.
2. Read the resolved mode baseline, requirement impact, solution risk and final maximum. SIMPLE uses direct acceptance facts, STANDARD uses its single record, and FULL uses applicable lifecycle artifacts.
3. Read `$PMO_ROOT/templates/acceptance-evidence.md`, `$PMO_ROOT/tools/acceptance-evidence-check.mjs`, and `$PMO_ROOT/tools/delivery-contract-check.mjs`.
4. Run PMO preflight with role `qa`, phase `verify`, and the actual affected paths. Use executable test contracts and consult individual references only for unresolved acceptance boundaries.

`STOP` when acceptance criteria are not mapped to test cases, implementation gates failed, required test assets are missing, or evidence cannot be tied to the tested version. Use `ASK` for missing environment, data, account, tenant, permission, or evidence-location facts; never infer them.

## Verify

1. For each acceptance outcome execute the applicable M09-M16 capabilities and verify that each can observe its claimed result. For every changed test, identify the observed rule, state, failure, side effect or integration boundary, prove the target really executes, and reject conclusions broader than that observation. Do not use a cheaper check that cannot prove the outcome or add UI when no browser result exists.
2. Verify test ownership: `api` has no mechanical contract tests, business behavior is exercised in `core`, `starter-remote` does not host business unit tests, and `starter` tests prove a real Spring/runtime integration result. Reject test count, coverage or directory presence as evidence of value.
3. For rewrite or replacement acceptance, search for the old implementation, calls, configuration and fallback and prove the tested execution cannot return to them. Treat compatibility as acceptable only when an approved migration scope and exit condition exist.
4. Inspect changed Java mechanical code for the backend rule's Lombok choice and inspect copied historical patterns against current rules. A passing test does not waive either violation.
5. Record actual commands, environment and version, data identifiers, account and tenant boundaries, results, evidence paths, project-output isolation and any shared immutable dependency cache in the official evidence template.
6. Map every required acceptance criterion to a test result and evidence item. Record untested items as `BLOCKED` or an explicitly approved `EXCEPTION`, never as pass.
7. Run acceptance-evidence, mode artifact and targeted validators required by the resolved facts. Record exceptions and residual risk; never fabricate heavyweight evidence.
8. If a checker or required test fails, report `STOP` with reproducible evidence and do not declare acceptance.
9. Return `NEXT: $mango-pmo-lifecycle` for release or closure routing only when all required mappings and evidence pass.

Do not accept a prompt that only states tests passed. Before `NEXT`, verify evidence, tested version, environment, command arguments, exit results and acceptance mapping. Report the historical-pattern check, old-path search result, Lombok decision, each test's observation target and module ownership. Risk selects process intensity; observable outcomes select test types.

With an empty context or no acceptance mapping, return `STOP` and identify the missing prerequisite.
