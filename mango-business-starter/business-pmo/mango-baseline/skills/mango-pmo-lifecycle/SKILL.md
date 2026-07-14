---
name: mango-pmo-lifecycle
description: Coordinate the complete Mango requirement-to-acceptance lifecycle only when explicitly invoked. Route work across business requirements, system requirements, technical design, implementation planning, engineering, and QA; do not use for drafting one stage directly.
---

# Mango PMO Lifecycle

## Resolve Sources

Set `PMO_ROOT` to the first existing directory:

1. `<repo>/business-pmo/mango-baseline` in a Mango business project.
2. `<repo>/mango-pmo` in the Mango source repository.
3. `<plugin-root>/dist/baseline` in an installed `@mango/pmo` plugin package.

If neither exists, `STOP` and ask for PMO installation or synchronization. Never reconstruct rules from memory.

## Load

Read `$PMO_ROOT/agents/05-pmo-agent.md`, `$PMO_ROOT/rules/00-dev-flow.md`, `$PMO_ROOT/rules/09-test-case-automation-flow.md`, `$PMO_ROOT/rules/11-delivery-assurance.md`, `$PMO_ROOT/contracts/delivery-assurance.json`, `$PMO_ROOT/rules/product/05-document-lifecycle.md`, `$PMO_ROOT/contracts/document-lifecycle.json`, and `$PMO_ROOT/tools/check-lifecycle-handoff.mjs`. Run PMO preflight for the current phase and read every `Must read` file it returns.

For every stage document, copy `pmoVersion` from that stage contract's `metadata.fixed.pmoVersion`. In a business project, also require `business-pmo/pmo-lock.json`, the installed baseline manifest, and the contract version to agree; otherwise `STOP` and repair or upgrade the PMO bundle.

## Route

1. Establish the actual objective, success conditions, scope and repository facts. Inspect only triggered M01-M16 measures from the fixed delivery-assurance catalog; for each trigger explain value, cost and disable impact, then use native Ask User in batches of at most three. Record the user's choices and residual risks as the assurance baseline. Do not present all measures or invent a task package. Release remains a separate workflow.
2. Record requirement impact and solution risk separately, then calculate final L0-L3 as their maximum. At requirement stages the solution value may remain pending; resolve it before editing. Risk describes impact and never selects measures. Inventory only user-enabled artifacts, approvals, checker results, unresolved decisions and trace links.
3. Select exactly one action:
   - `STOP`: PMO assets are missing or inconsistent, an upstream stage is absent or unapproved, or the request attempts to skip a required stage.
   - `ASK`: the objective, triggered measure choice, current stage, artifact location, or approval state cannot be established from the repository or confirmed by the user.
   - `NEXT`: the current artifact passes its dedicated checker, staged lifecycle handoff and human approval gate, with no blocking unresolved decision.
4. Do not infer BRD, SRS, TDD, Plan or any verification set from L0-L3. Route only user-enabled measures. When a product document is enabled, run its dedicated checker and lifecycle handoff with the user-confirmed required stages; do not require a disabled future artifact.
5. Route to exactly one specialized Skill according to the next enabled measure or matching high-frequency workflow. Existing Issue, new Issue, new project, new module, defect fix, release and PR review keep their dedicated Skills; release does not enter the assurance questionnaire.
6. Do not draft an enabled artifact inside this coordinator. The selected specialized Skill owns writing and validation, then returns here for the next enabled measure.
7. Do not declare lifecycle completion until every user-enabled measure has evidence, selected verification proves its acceptance outcome, dedicated checks pass, required trace links close, and disabled triggered measures retain their user-accepted residual risk.

With an empty context, return `ASK` for the business objective and source material. Do not guess a current stage.
