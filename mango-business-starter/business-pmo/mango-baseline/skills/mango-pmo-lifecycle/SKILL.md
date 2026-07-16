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

1. Establish the objective, success conditions, scope and repository facts. Resolve workspace policy automatically: CREATE on main/primary, REUSE in the same task worktree, and ask only for a requested MAIN_EXCEPTION.
2. Record requirement impact and solution risk separately, calculate their maximum, and map L0/L1 to SIMPLE, L2 to STANDARD, and L3 to FULL. Resolve solution risk before editing.
3. Select exactly one action:
   - `STOP`: PMO assets are missing or inconsistent, an upstream stage is absent or unapproved, or the request attempts to skip a required stage.
   - `ASK`: the objective, material fact, requested exception, current stage, artifact location, or approval state cannot be established.
   - `NEXT`: the current artifact passes its dedicated checker, staged lifecycle handoff and human approval gate, with no blocking unresolved decision.
4. SIMPLE routes directly to engineering with no delivery document. STANDARD routes through one standard delivery record and its checker. FULL routes through the applicable complete product or specialist lifecycle; do not fabricate product documents for governance-only work.
5. Select M09-M16 by observable acceptance facts, not by level alone. Route to exactly one specialized Skill; release stays separate.
6. Do not draft an enabled artifact inside this coordinator. The selected specialized Skill owns writing and validation, then returns here for the next enabled measure.
7. Do not declare completion until the mode-required artifacts pass, selected verification proves each acceptance outcome, trace links close, and every exception has explicit evidence and residual risk.

With an empty context, return `ASK` for the business objective and source material. Do not guess a current stage.
