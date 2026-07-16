---
name: mango-qa-verification
description: Verify and accept a completed Mango implementation against approved acceptance criteria and test mappings. Use for QA planning, execution, regression, evidence, and acceptance status; do not use for authoring requirements or design, implementing product code, or accepting work without executable evidence.
---

# Mango QA

## Resolve Sources

Set `PMO_ROOT` to the first available source: `<repo>/business-pmo/mango-baseline`, `<repo>/mango-pmo`, or `<plugin-root>/dist/baseline`. If none exists, `STOP`. Never use remembered or copied rules.

## Load Before Testing

1. Read `$PMO_ROOT/agents/04-qa-agent.md`, `$PMO_ROOT/rules/04-test-assets.md`, `$PMO_ROOT/rules/05-ai-delivery-quality.md`, `$PMO_ROOT/rules/09-test-case-automation-flow.md`, `$PMO_ROOT/rules/backend/08-test.md`, and `$PMO_ROOT/rules/frontend/04-test.md`.
2. Read the resolved mode baseline, requirement impact, solution risk and final maximum. SIMPLE uses direct acceptance facts, STANDARD uses its single record, and FULL uses applicable lifecycle artifacts.
3. Read `$PMO_ROOT/templates/acceptance-evidence.md`, `$PMO_ROOT/tools/acceptance-evidence-check.mjs`, and `$PMO_ROOT/tools/delivery-contract-check.mjs`.
4. Run PMO preflight with role `qa`, phase `verify`, and the actual affected paths. Read every `Must read` file.

`STOP` when acceptance criteria are not mapped to test cases, implementation gates failed, required test assets are missing, or evidence cannot be tied to the tested version. Use `ASK` for missing environment, data, account, tenant, permission, or evidence-location facts; never infer them.

## Verify

1. For each acceptance outcome execute the applicable M09-M16 capabilities and verify that each can observe its claimed result. Do not use a cheaper check that cannot prove the outcome or add UI when no browser result exists.
2. Record actual commands, environment and version, data identifiers, account and tenant boundaries, results, and evidence paths in the official evidence template.
3. Map every required acceptance criterion to a test result and evidence item. Record untested items as `BLOCKED` or an explicitly approved `EXCEPTION`, never as pass.
4. Run acceptance-evidence, mode artifact and targeted validators required by the resolved facts. Record exceptions and residual risk; never fabricate heavyweight evidence.
5. If a checker or required test fails, report `STOP` with reproducible evidence and do not declare acceptance.
6. Return `NEXT: $mango-pmo-lifecycle` for release or closure routing only when all required mappings and evidence pass.

Do not accept a prompt that only states tests passed. Before `NEXT`, verify evidence, tested version, environment, command arguments, exit results and acceptance mapping. Risk selects process intensity; observable outcomes select test types.

With an empty context or no acceptance mapping, return `STOP` and identify the missing prerequisite.
