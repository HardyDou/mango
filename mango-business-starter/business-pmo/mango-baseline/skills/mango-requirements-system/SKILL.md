---
name: mango-requirements-system
description: Create or review a Mango System Requirements Specification from an approved Business Requirements Document. Use for observable system capabilities, roles, information, states, constraints, and system acceptance behavior; do not use for business discovery, APIs, module classes, database schemas, technical design, implementation tasks, or coding.
---

# Mango System Requirements

## Resolve Sources

Set `PMO_ROOT` to the first available source: `<repo>/business-pmo/mango-baseline`, `<repo>/mango-pmo`, or `<plugin-root>/dist/baseline`. If none exists, `STOP`. Never use remembered or copied rules.

## Load

Before drafting, read these authoritative assets:

- `$PMO_ROOT/agents/system-requirements-agent.md`
- `$PMO_ROOT/rules/product/02-system-requirements.md`
- `$PMO_ROOT/contracts/system-requirements.json`
- `$PMO_ROOT/templates/system-requirements.md`
- `$PMO_ROOT/tools/check-system-requirements.mjs`
- `$PMO_ROOT/tools/check-lifecycle-handoff.mjs`
- `$PMO_ROOT/rules/09-test-case-automation-flow.md`
- `$PMO_ROOT/rules/product/05-document-lifecycle.md`

Run PMO preflight with role `pm` and phase `requirement`, then read every `Must read` file.

## Execute

1. Refine the L0-L3 impact assessment using observable system-entry, behavior, data and non-functional facts; do not lower the BRD level or assume implementation details. This four-document stage applies to L2/L3; for L0/L1, do not fabricate an SRS and route back to the approved lightweight lifecycle path. For L2/L3, locate the approved Business Requirements Document and verify it with its dedicated checker.
2. Choose one action:
   - `STOP`: the upstream document is absent, unapproved, invalid, or the request asks for implementation design.
   - `ASK`: a required externally observable system fact cannot be traced to approved business input or verified repository facts.
   - `WRITE`: inputs satisfy the rule; fill the official template and preserve upstream trace links without inventing business scope.
3. Set `pmoVersion` to the contract's exact `metadata.fixed.pmoVersion`, then run `node "$PMO_ROOT/tools/check-system-requirements.mjs" --document <document-path>`.
4. Fix failures without weakening the checker or adding technical implementation content.
5. Run the lifecycle checker with BRD and SRS through `--through srs`; require a valid upstream hash, full upstream trace coverage, `APPROVED/NEXT`, a human approver, approval evidence and no open blocker.
6. Return `NEXT: $mango-design-technical` only when the dedicated checker, staged lifecycle handoff, gate table and human approval all pass.

With an empty context or no approved upstream document, return `STOP` and identify the missing prerequisite.
