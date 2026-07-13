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

Read `$PMO_ROOT/agents/05-pmo-agent.md`, `$PMO_ROOT/rules/00-dev-flow.md`, `$PMO_ROOT/rules/09-test-case-automation-flow.md`, `$PMO_ROOT/rules/product/05-document-lifecycle.md`, `$PMO_ROOT/contracts/document-lifecycle.json`, and `$PMO_ROOT/tools/check-lifecycle-handoff.mjs`. Run PMO preflight for the current phase and read every `Must read` file it returns.

For every stage document, copy `pmoVersion` from that stage contract's `metadata.fixed.pmoVersion`. In a business project, also require `business-pmo/pmo-lock.json`, the installed baseline manifest, and the contract version to agree; otherwise `STOP` and repair or upgrade the PMO bundle.

## Route

1. Establish and record the L0-L3 risk level from repository facts and the canonical test rule, then inventory the applicable artifacts, approval state, checker results, unresolved decisions, and trace links.
2. Select exactly one action:
   - `STOP`: PMO assets are missing or inconsistent, an upstream stage is absent or unapproved, or the request attempts to skip a required stage.
   - `ASK`: the lifecycle objective, current stage, artifact location, or approval state cannot be established from the repository.
   - `NEXT`: the current artifact passes its dedicated checker, staged lifecycle handoff and human approval gate, with no blocking unresolved decision.
3. L0/L1 work follows its approved lightweight task and evidence path without fabricating BRD, SRS, TDD or Plan. L2/L3 work must follow the complete BRD -> SRS -> TDD -> Plan chain.
4. For L2/L3, run the lifecycle handoff checker with every artifact from BRD through the current stage and `--through brd|srs|tdd|plan` before returning `NEXT`. Do not require a future artifact before routing to the Skill that creates it.
5. Route to exactly one specialized Skill. Use the document chain for L2/L3 staged feature delivery, the approved lightweight engineering/QA path for L0/L1, or the matching high-frequency workflow Skill for an existing Issue, new Issue, new project, new module, defect fix, release, or PR review.
6. Do not draft a stage artifact inside this coordinator. The selected stage Skill owns writing and validation.
7. Do not declare lifecycle completion until the risk-appropriate QA evidence passes its dedicated checks and all required trace links are closed.

With an empty context, return `ASK` for the business objective and source material. Do not guess a current stage.
