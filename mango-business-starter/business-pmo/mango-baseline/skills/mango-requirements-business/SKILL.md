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

Run PMO preflight with role `pm` and phase `requirement`, then read every `Must read` file.

## Execute

1. Read repository facts and user-provided sources before asking questions, then assess only the L0-L3 requirement impact from affected actors, business outcomes, boundaries, failure consequences and recovery facts. Do not assume a technical solution. This four-document stage applies when the governed path is L2/L3; for L0/L1, do not fabricate a BRD and route back to the approved lightweight lifecycle path.
2. Choose one action:
   - `STOP`: requested content crosses the stage boundary, authoritative assets disagree, or a required source is unavailable.
   - `ASK`: a required business fact cannot be established. Ask one focused question and do not insert placeholders or invented facts.
   - `WRITE`: inputs satisfy the rule; fill the official template with business content only and preserve required trace identifiers.
3. Set `pmoVersion` to the contract's exact `metadata.fixed.pmoVersion`, then run `node "$PMO_ROOT/tools/check-business-requirements.mjs" --document <document-path>`.
4. Fix failures without weakening the checker or moving forbidden downstream content into the document.
5. Run the lifecycle checker through BRD with `--through brd`; future documents are not required, but the BRD must be `APPROVED/NEXT`, name a human approver, contain verifiable approval evidence, and have no open blocker.
6. Return `NEXT: $mango-requirements-system` only when the dedicated checker, staged lifecycle handoff, gate table and human approval all pass.

With an empty context, return `ASK` for the business problem, affected actors, and expected outcome. Do not generate a generic document.
