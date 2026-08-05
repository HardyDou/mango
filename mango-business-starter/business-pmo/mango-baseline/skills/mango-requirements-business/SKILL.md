---
name: mango-requirements-business
description: Create or review a Mango Business Requirements Document from business goals, actors, objects, flows, rules, and acceptance outcomes. Use for business requirement discovery only; do not use for system behavior, UI specifications, APIs, database design, technical design, implementation tasks, or coding.
---

# Mango Business Requirements

## Resolve Sources

Set `PMO_ROOT` to the first available source: `<repo>/business-pmo/mango-baseline`, `<repo>/mango-pmo`, or `<plugin-root>/dist/baseline`. If none exists, `STOP`. Never use remembered or copied rules.

## Load

Before drafting, read these authoritative assets:

- `$PMO_ROOT/agents/business-requirements-agent.md`
- `$PMO_ROOT/rules/product/01-business-requirements.md`
- `$PMO_ROOT/contracts/business-requirements.json`
- `$PMO_ROOT/templates/business-requirements.md`
- `$PMO_ROOT/tools/check-business-requirements.mjs`
- `$PMO_ROOT/tools/check-lifecycle-handoff.mjs`
- `$PMO_ROOT/rules/09-test-case-automation-flow.md`
- `$PMO_ROOT/rules/product/05-document-lifecycle.md`

Run PMO preflight with role `pm` and phase `requirement`; consult individual references only when requirement ownership or a contract boundary remains unresolved.

## Execute

1. Read repository facts, user sources and the resolved delivery-mode baseline. Continue for a standalone BRD only in FULL product flow or when the user explicitly requests a higher-mode artifact. SIMPLE creates no BRD; STANDARD writes the business section in its single record.
2. Choose one action:
   - `STOP`: requested content crosses the stage boundary, authoritative assets disagree, or a required source is unavailable.
   - `ASK`: a required business fact cannot be established. Ask one focused question and do not insert placeholders or invented facts.
   - `WRITE`: inputs satisfy the rule; fill the official template with business content only and preserve required trace identifiers.
3. Set `pmoVersion` to the contract's exact `metadata.fixed.pmoVersion`, then run `node "$PMO_ROOT/tools/check-business-requirements.mjs" --document <document-path>`.
4. Fix failures without weakening the checker or moving forbidden downstream content into the document.
5. Run the lifecycle checker for the user-enabled BRD; disabled future documents are not required. The BRD must be `APPROVED/NEXT`, name a human approver, contain verifiable approval evidence, and have no open blocker.
6. Return `NEXT: $mango-pmo-lifecycle` only when the dedicated checker, applicable lifecycle handoff, gate table and human approval all pass. Let the coordinator choose the next enabled measure; do not assume SRS follows.

With an empty context, return `ASK` for the business problem, affected actors, expected outcome and M03 confirmation. Do not generate a generic document or infer BRD from L2/L3.
