---
name: mango-plan-implementation
description: Create or review a user-enabled Mango Implementation Plan from approved design input or a confirmed assurance baseline. Use for ordered work items, dependencies, ownership, affected paths, verification commands, rollout, and completion criteria; do not use for selecting safeguards, requirements discovery, API or database design, coding, or QA execution.
---

# Mango Implementation Plan

## Resolve Sources

Set `PMO_ROOT` to the first available source: `<repo>/business-pmo/mango-baseline`, `<repo>/mango-pmo`, or `<plugin-root>/dist/baseline`. If none exists, `STOP`. Never use remembered or copied rules.

## Load

Before drafting, read these authoritative assets:

- `$PMO_ROOT/agents/implementation-plan-agent.md`
- `$PMO_ROOT/rules/product/04-implementation-plan.md`
- `$PMO_ROOT/contracts/implementation-plan.json`
- `$PMO_ROOT/templates/implementation-plan.md`
- `$PMO_ROOT/tools/check-implementation-plan.mjs`
- `$PMO_ROOT/tools/check-lifecycle-handoff.mjs`
- `$PMO_ROOT/rules/09-test-case-automation-flow.md`
- `$PMO_ROOT/rules/product/05-document-lifecycle.md`

Run PMO preflight with role `tech-lead` and phase `design`, then read every `Must read` file.

## Execute

1. Read `$PMO_ROOT/rules/11-delivery-assurance.md` and the assurance baseline. Continue only when the user confirmed M06 Implementation Plan=`ENABLE`; otherwise return `ASK` to `$mango-design-delivery-assurance`. Inherit the applicable approved design's final L0-L3 and evidence without reassessing or lowering it. Risk level does not enable Plan or any other document.
2. Choose one action:
   - `STOP`: the design is absent, unapproved, invalid, or the request asks the plan to decide unresolved requirements or architecture.
   - `ASK`: ownership, dependency, sequencing, target path, verification, or rollout facts cannot be established.
   - `WRITE`: inputs satisfy the rule; fill the official template with executable, traceable work items only.
3. Set `pmoVersion` to the contract's exact `metadata.fixed.pmoVersion`, then run `node "$PMO_ROOT/tools/check-implementation-plan.mjs" --document <document-path>`.
4. Turn only user-enabled M09-M16 measures into executable commands or steps. Do not add omitted measures or demand skip reasons for untriggered measures. Fix failures without weakening the checker or silently redesigning the approved solution. Any new endpoint, schema, technology choice or architecture decision requires a TDD revision and renewed approval when M05 is enabled; otherwise reconfirm the affected assurance measures.
5. Run the lifecycle checker for the user-enabled Plan and applicable upstream; require valid hashes and trace coverage, `APPROVED/NEXT`, a human approver, approval evidence and no open blocker.
6. Return `NEXT: $mango-pmo-lifecycle` only when the dedicated checker, applicable lifecycle handoff, dependency graph, gate table and human approval all pass. Let the coordinator choose the next enabled measure or engineering.

With an empty context, return `ASK` for M06 confirmation and the implementation source. When a confirmed Plan lacks an applicable approved design, return `STOP` and identify it. Do not infer Plan from L2/L3.
