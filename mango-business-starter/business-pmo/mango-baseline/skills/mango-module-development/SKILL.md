---
name: mango-module-development
description: Add and accept a new module inside an existing Mango or Mango-based project from approved requirements and design. Use for module boundaries, generated module structure, persistence and initialization, menu and permissions, documentation, capability mapping, and tests; do not use for a new project, an ordinary feature inside an existing module, or an unresolved architecture proposal.
---

# Mango New Module

## Resolve And Load

Set `PMO_ROOT` to the first available source: the installed business baseline, Mango source PMO, or `<plugin-root>/dist/baseline`. Read `$PMO_ROOT/rules/00-dev-flow.md`, `$PMO_ROOT/rules/backend/01-code.md`, `$PMO_ROOT/rules/backend/02-naming.md`, `$PMO_ROOT/rules/backend/03-api.md`, `$PMO_ROOT/rules/backend/04-db.md`, `$PMO_ROOT/rules/backend/05-module.md`, `$PMO_ROOT/rules/backend/07-persistence.md`, `$PMO_ROOT/rules/backend/08-test.md`, `$PMO_ROOT/rules/backend/10-dev-flow.md`, `$PMO_ROOT/rules/backend/11-module-menu.md`, `$PMO_ROOT/rules/frontend/06-monorepo-architecture.md`, `$PMO_ROOT/rules/08-capability-docs.md`, and `$PMO_ROOT/templates/module-readme.md`.

Run PMO preflight with role `dev`, phase `develop`, and all affected module paths; read every `Must read` file. Read the delivery-mode contract and resolved baseline. Follow automatic M01 CREATE/REUSE policy. Read the STANDARD record or FULL design/plan required by the selected mode and run its checker.

## Execute

1. Return `STOP` when the module boundary, ownership, public contract, dependency direction, or a user-enabled approved design is absent or inconsistent.
2. Return `ASK` for one unresolved module identity, aggregate, dependency, persistence, menu, permission, initialization, or consumer fact.
3. Use the canonical Mango module-generation entry when one exists; do not create a parallel structure by hand. Audit generated and neighboring historical code against the currently loaded rules before reusing it; current rules override generator or repository precedent.
4. Before implementation, map production and test responsibilities across `api`, `core`, `starter` and `starter-remote`. Keep protocols and mechanical contracts in `api`, business behavior and its tests in `core`, remote-boundary verification outside `starter-remote` unit tests, and only observable Spring/runtime integration tests in `starter`.
5. For Java types, use the rule-selected Lombok annotations for mechanical getters, setters and constructors. Require a named semantic reason for every handwritten accessor or constructor, and do not default to `@Data`.
6. If the module replaces an existing implementation, include removal of its old implementation, calls, configuration and fallback; compatibility requires an independently approved migration boundary and exit condition.
7. Implement the resolved scope, then execute M08-M16 capabilities selected by observable facts. Every test item must identify the rule, state, failure, side effect or integration boundary it observes and the real target that participates.
8. Return `NEXT: $mango-qa-verification` only when mode-required artifacts and every active capability have canonical evidence.

Before `NEXT`, report the historical-pattern audit, old-path removal result when applicable, Lombok decisions, test observation targets and `api/core/starter/starter-remote` ownership mapping.

With an empty context, return `ASK` for the objective and risk facts. Do not guess a delivery mode from “new module”.
