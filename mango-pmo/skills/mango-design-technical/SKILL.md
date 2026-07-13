---
name: mango-design-technical
description: Create or review a Mango Technical Design Document from an approved System Requirements Specification. Use for architecture, module boundaries, API contracts, persistence, security, migration, and verification design; do not use for business discovery, system requirement authoring, implementation scheduling, coding, or acceptance execution.
---

# Mango Technical Design

## Resolve Sources

Set `PMO_ROOT` to the first available source: `<repo>/business-pmo/mango-baseline`, `<repo>/mango-pmo`, or `<plugin-root>/dist/baseline`. If none exists, `STOP`. Never use remembered or copied rules.

## Load

Before drafting, read these authoritative assets:

- `$PMO_ROOT/agents/technical-design-agent.md`
- `$PMO_ROOT/rules/product/03-technical-design.md`
- `$PMO_ROOT/contracts/technical-design.json`
- `$PMO_ROOT/templates/technical-design.md`
- `$PMO_ROOT/tools/check-technical-design.mjs`
- `$PMO_ROOT/tools/check-lifecycle-handoff.mjs`
- `$PMO_ROOT/rules/backend/03-api.md`
- `$PMO_ROOT/rules/backend/05-module.md`
- `$PMO_ROOT/rules/09-test-case-automation-flow.md`
- `$PMO_ROOT/rules/product/05-document-lifecycle.md`

Run PMO preflight with role `tech-lead` and phase `design`, then read every `Must read` file.

## Execute

1. Establish the L0-L3 risk level. This four-document stage applies to L2/L3; for L0/L1, do not fabricate a TDD and route back to the approved lightweight lifecycle path. For L2/L3, locate the approved System Requirements Specification, verify it, and inspect affected Mango source and public contracts.
2. Choose one action:
   - `STOP`: upstream requirements are absent, unapproved, invalid, or the request asks this stage to invent business scope.
   - `ASK`: a design decision cannot be derived from approved requirements, repository facts, or loaded Mango rules.
   - `WRITE`: inputs satisfy the rule; fill the official template and trace every design decision to upstream requirements.
3. Set `pmoVersion` to the contract's exact `metadata.fixed.pmoVersion`, then run `node "$PMO_ROOT/tools/check-technical-design.mjs" --document <document-path>`.
4. Validate every API design against the loaded backend API/module rules, including the path-variable ban, protocol-model boundary and api/core/starter/starter-remote ownership.
5. Run the lifecycle checker with BRD, SRS and TDD through `--through tdd`; require valid hashes and trace coverage, `APPROVED/NEXT`, a human approver, approval evidence and no open blocker.
6. Return `NEXT: $mango-plan-implementation` only when the dedicated checker, staged lifecycle handoff, specialized design checks, gate table and human approval all pass.

With an empty context or no approved upstream document, return `STOP` and identify the missing prerequisite.
