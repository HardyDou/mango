---
name: mango-design-technical
description: Create or review a user-enabled Mango Technical Design Document from approved system input or a confirmed assurance baseline. Use for architecture, module boundaries, API contracts, persistence, security, migration, and verification design; do not use for selecting safeguards, business discovery, system requirement authoring, implementation scheduling, coding, or acceptance execution.
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

1. Read the resolved delivery-mode baseline. Continue for a standalone TDD only in FULL flow or an explicit higher-mode request. SIMPLE keeps design in the implementation reasoning; STANDARD writes technical decisions in its single record. Reassess solution risk and escalate mode when needed.
2. Choose one action:
   - `STOP`: upstream requirements are absent, unapproved, invalid, or the request asks this stage to invent business scope.
   - `ASK`: a design decision cannot be derived from approved requirements, repository facts, or loaded Mango rules.
   - `WRITE`: inputs satisfy the rule; fill the official template and trace every design decision to upstream requirements.
3. Set `pmoVersion` to the contract's exact `metadata.fixed.pmoVersion`, then run `node "$PMO_ROOT/tools/check-technical-design.mjs" --document <document-path>`.
4. Validate every API design against the loaded backend API/module rules, including the path-variable ban, protocol-model boundary and api/core/starter/starter-remote ownership.
5. Map acceptance outcomes only to user-enabled verification measures, record why each selected type proves the result and preserve the residual risk of disabled triggered measures. Run the lifecycle checker for the enabled TDD and applicable upstream; require valid hashes, applicable risk relations, trace coverage, `APPROVED/NEXT`, a human approver, approval evidence and no open blocker.
6. Return `NEXT: $mango-pmo-lifecycle` only when the dedicated checker, applicable lifecycle handoff, specialized design checks, gate table and human approval all pass. Let the coordinator choose the next enabled measure; do not assume Plan follows.

With an empty context, return `ASK` for M05 confirmation and the technical decision source. When a confirmed TDD lacks an applicable approved source, return `STOP` and identify it. Do not infer documents from L2/L3.
