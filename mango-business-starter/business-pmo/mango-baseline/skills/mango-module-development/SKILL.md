---
name: mango-module-development
description: Add and accept a new module inside an existing Mango or Mango-based project from approved requirements and design. Use for module boundaries, generated module structure, persistence and initialization, menu and permissions, documentation, capability mapping, and tests; do not use for a new project, an ordinary feature inside an existing module, or an unresolved architecture proposal.
---

# Mango New Module

## Resolve And Load

Set `PMO_ROOT` to the first available source: the installed business baseline, Mango source PMO, or `<plugin-root>/dist/baseline`. Read `$PMO_ROOT/rules/00-dev-flow.md`, `$PMO_ROOT/rules/backend/01-code.md`, `$PMO_ROOT/rules/backend/02-naming.md`, `$PMO_ROOT/rules/backend/03-api.md`, `$PMO_ROOT/rules/backend/04-db.md`, `$PMO_ROOT/rules/backend/05-module.md`, `$PMO_ROOT/rules/backend/07-persistence.md`, `$PMO_ROOT/rules/backend/08-test.md`, `$PMO_ROOT/rules/backend/10-dev-flow.md`, `$PMO_ROOT/rules/backend/11-module-menu.md`, `$PMO_ROOT/rules/frontend/06-monorepo-architecture.md`, `$PMO_ROOT/rules/08-capability-docs.md`, and `$PMO_ROOT/templates/module-readme.md`.

Run PMO preflight with role `dev`, phase `develop`, and all affected module paths; read every `Must read` file. Read `$PMO_ROOT/rules/11-delivery-assurance.md`, its contract, and the human-confirmed assurance baseline. Only when M05 or M06 is enabled, read the approved TDD or Implementation Plan and run its checker and applicable lifecycle handoff. Follow the confirmed M01 value; an existing task worktree makes `DO_NOT_CREATE` the normal recommendation, not an implicit decision.

## Execute

1. Return `STOP` when the module boundary, ownership, public contract, dependency direction, or a user-enabled approved design is absent or inconsistent.
2. Return `ASK` for one unresolved module identity, aggregate, dependency, persistence, menu, permission, initialization, or consumer fact.
3. Use the canonical Mango module-generation entry when one exists; do not create a parallel structure by hand.
4. Implement the confirmed scope, then execute only enabled M08-M16 measures. Domain rules define how an enabled check is run; they cannot enable documentation, capability, test, review, readback, or acceptance measures themselves.
5. Return `NEXT: $mango-qa-verification` only when every enabled measure has canonical evidence and every disabled triggered measure retains its user-accepted residual risk.

With an empty context, return `ASK` for the objective and assurance confirmation. Do not infer TDD, Plan, capability docs, or a verification package from “new module”.
