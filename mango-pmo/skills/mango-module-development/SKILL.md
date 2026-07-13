---
name: mango-module-development
description: Add and accept a new module inside an existing Mango or Mango-based project from approved requirements and design. Use for module boundaries, generated module structure, persistence and initialization, menu and permissions, documentation, capability mapping, and tests; do not use for a new project, an ordinary feature inside an existing module, or an unresolved architecture proposal.
---

# Mango New Module

## Resolve And Load

Set `PMO_ROOT` to the first available source: the installed business baseline, Mango source PMO, or `<plugin-root>/dist/baseline`. Read `$PMO_ROOT/rules/00-dev-flow.md`, `$PMO_ROOT/rules/backend/01-code.md`, `$PMO_ROOT/rules/backend/02-naming.md`, `$PMO_ROOT/rules/backend/03-api.md`, `$PMO_ROOT/rules/backend/04-db.md`, `$PMO_ROOT/rules/backend/05-module.md`, `$PMO_ROOT/rules/backend/07-persistence.md`, `$PMO_ROOT/rules/backend/08-test.md`, `$PMO_ROOT/rules/backend/10-dev-flow.md`, `$PMO_ROOT/rules/backend/11-module-menu.md`, `$PMO_ROOT/rules/frontend/06-monorepo-architecture.md`, `$PMO_ROOT/rules/08-capability-docs.md`, and `$PMO_ROOT/templates/module-readme.md`.

Run PMO preflight with role `dev`, phase `develop`, and all affected module paths; read every `Must read` file. Read the approved Technical Design Document and Implementation Plan, then run their dedicated checkers and staged lifecycle handoff before editing. Reuse an existing non-`main` task worktree; never create another worktree for the same user-requested fix.

## Execute

1. Return `STOP` when the module boundary, ownership, public contract, dependency direction, or approved design is absent or inconsistent.
2. Return `ASK` for one unresolved module identity, aggregate, dependency, persistence, menu, permission, initialization, or consumer fact.
3. Use the canonical Mango module-generation entry when one exists; do not create a parallel structure by hand.
4. Implement only approved module plan items, then run the backend, frontend, documentation, capability, and acceptance gates selected by preflight and the loaded rules. Backend verification must execute the authoritative Maven architecture gate for API/Controller/Feign/Service/Mapper/Entity, `R<T>`, `Require + BizCode`, typed CRUD, forbidden `@PathVariable`, Spring Service registration, direct construction, cross-cutting annotation ownership, and static Service Locator contracts.
5. Return `NEXT: $mango-qa-verification` only when structure, public exports, initialization, documentation, capability mapping, tests, and traceability all pass their canonical checks.

With an empty context, return `STOP` for missing approved design and plan.
