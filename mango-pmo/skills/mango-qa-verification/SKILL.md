---
name: mango-qa-verification
description: Verify and accept a completed Mango implementation against approved acceptance criteria and test mappings. Use for QA planning, execution, regression, evidence, and acceptance status; do not use for authoring requirements or design, implementing product code, or accepting work without executable evidence.
---

# Mango QA

## Resolve Sources

Set `PMO_ROOT` to the first available source: `<repo>/business-pmo/mango-baseline`, `<repo>/mango-pmo`, or `<plugin-root>/dist/baseline`. If none exists, `STOP`. Never use remembered or copied rules.

## Load Before Testing

1. Read `$PMO_ROOT/agents/04-qa-agent.md`, `$PMO_ROOT/rules/04-test-assets.md`, `$PMO_ROOT/rules/05-ai-delivery-quality.md`, `$PMO_ROOT/rules/09-test-case-automation-flow.md`, `$PMO_ROOT/rules/backend/08-test.md`, and `$PMO_ROOT/rules/frontend/04-test.md`.
2. Establish the L0-L3 level. For L2/L3, read the approved requirement, design, implementation plan, delivery contract, acceptance mapping, and implementation verification results. For L0/L1, read the approved lightweight scope, acceptance statement, risk evidence and implementation verification results without fabricating the four-stage chain.
3. Read `$PMO_ROOT/templates/acceptance-evidence.md`, `$PMO_ROOT/tools/acceptance-evidence-check.mjs`, and `$PMO_ROOT/tools/delivery-contract-check.mjs`.
4. Run PMO preflight with role `qa`, phase `verify`, and the actual affected paths. Read every `Must read` file.

`STOP` when acceptance criteria are not mapped to test cases, implementation gates failed, required test assets are missing, or evidence cannot be tied to the tested version. Use `ASK` for missing environment, data, account, tenant, permission, or evidence-location facts; never infer them.

## Verify

1. Select and record the required L0-L3 verification level from the canonical rules, then execute the mapped tests at the required levels and priorities.
2. Record actual commands, environment and version, data identifiers, account and tenant boundaries, results, and evidence paths in the official evidence template.
3. Map every required acceptance criterion to a test result and evidence item. Record untested items as `BLOCKED` or an explicitly approved `EXCEPTION`, never as pass.
4. For L2/L3, run the acceptance-evidence and delivery-contract checkers with the arguments required by those tools. For L0/L1, run the risk-appropriate targeted validator and record why the full delivery contract or environment fields are not applicable; never fabricate heavyweight evidence to satisfy a checker.
5. If a checker or required test fails, report `STOP` with reproducible evidence and do not declare acceptance.
6. Return `NEXT: $mango-pmo-lifecycle` for release or closure routing only when all required mappings and evidence pass.

Do not accept a prompt that only states tests or checkers passed. Before `NEXT`, verify the risk-appropriate evidence file, tested version, environment or explicit non-applicability, command arguments, exit results and every acceptance mapping from the workspace; L2/L3 also requires the full delivery contract and both dedicated checkers.

With an empty context or no acceptance mapping, return `STOP` and identify the missing prerequisite.
