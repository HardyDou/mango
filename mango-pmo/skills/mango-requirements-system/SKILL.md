---
name: mango-requirements-system
description: Create or review a user-enabled Mango System Requirements Specification from approved business input or a confirmed assurance baseline. Use for observable system capabilities, roles, information, states, constraints, and system acceptance behavior; do not use for selecting safeguards, business discovery, APIs, module classes, database schemas, technical design, implementation tasks, or coding.
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

1. Read the resolved delivery-mode baseline. Continue for a standalone SRS only in FULL product flow or an explicit higher-mode request. SIMPLE creates no SRS; STANDARD writes observable requirements in its single record.
2. Choose one action:
   - `STOP`: the upstream document is absent, unapproved, invalid, or the request asks for implementation design.
   - `ASK`: a required externally observable system fact cannot be traced to approved business input or verified repository facts.
   - `WRITE`: inputs satisfy the rule; fill the official template and preserve upstream trace links without inventing business scope.
3. Set `pmoVersion` to the contract's exact `metadata.fixed.pmoVersion`, then run `node "$PMO_ROOT/tools/check-system-requirements.mjs" --document <document-path>`.
4. Fix failures without weakening the checker or adding technical implementation content.
5. Run the lifecycle checker for the user-enabled SRS and applicable upstream; require the valid hash and trace coverage that apply, `APPROVED/NEXT`, a human approver, approval evidence and no open blocker.
6. Return `NEXT: $mango-pmo-lifecycle` only when the dedicated checker, applicable lifecycle handoff, gate table and human approval all pass. Let the coordinator choose the next enabled measure; do not assume TDD follows.

With an empty context, return `ASK` for M04 confirmation and the observable system source. When a confirmed SRS lacks an applicable approved source, return `STOP` and identify it. Do not infer documents from L2/L3.
