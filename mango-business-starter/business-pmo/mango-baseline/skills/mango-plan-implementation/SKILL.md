---
name: mango-plan-implementation
description: Create or review a Mango Implementation Plan from an approved Technical Design Document. Use for ordered work items, dependencies, ownership, affected paths, verification commands, rollout, and completion criteria; do not use for requirements discovery, API or database design, coding, or QA execution.
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

1. Establish the L0-L3 risk level. This four-document stage applies to L2/L3; for L0/L1, do not fabricate a Plan and route back to the approved lightweight lifecycle path. For L2/L3, locate and verify the approved Technical Design Document and its upstream trace chain.
2. Choose one action:
   - `STOP`: the design is absent, unapproved, invalid, or the request asks the plan to decide unresolved requirements or architecture.
   - `ASK`: ownership, dependency, sequencing, target path, verification, or rollout facts cannot be established.
   - `WRITE`: inputs satisfy the rule; fill the official template with executable, traceable work items only.
3. Set `pmoVersion` to the contract's exact `metadata.fixed.pmoVersion`, then run `node "$PMO_ROOT/tools/check-implementation-plan.mjs" --document <document-path>`.
4. Fix failures without weakening the checker or silently redesigning the approved solution. Any new endpoint, schema, technology choice or architecture decision requires a TDD revision and renewed approval.
5. Run the complete lifecycle checker with BRD, SRS, TDD and Plan through `--through plan`; require valid hashes and trace coverage, `APPROVED/NEXT`, a human approver, approval evidence and no open blocker.
6. Return `NEXT: $mango-engineering` only when the dedicated checker, complete lifecycle handoff, dependency graph, gate table and human approval all pass.

With an empty context or no approved design, return `STOP` and identify the missing prerequisite.
