---
name: mango-project-bootstrap
description: Initialize and accept a new Mango-based business project, including project generation, PMO baseline, workspace initialization, startup, documentation entry points, and first-run evidence. Do not use to add a module to an existing project, implement a feature, upgrade an existing project, or publish Mango artifacts.
---

# Mango New Project

## Resolve And Load

Resolve `PMO_ROOT` from the installed business baseline, Mango source repository, or `<plugin-root>/dist/baseline`. Run PMO preflight with role `dev`, phase `develop`, and the actual project paths. Use returned code baselines as the implementation source; consult individual references only when a boundary is unresolved. Reuse the current non-`main` task worktree when one already exists.

## Execute

1. Return `ASK` until project identity, topology, target directory, package coordinates, required modules, and release versions are explicit.
2. Read the resolved delivery-mode baseline. A new project is normally FULL; require its applicable approved requirements, design and plan artifacts and lifecycle handoff. Use STANDARD only when facts prove a bounded L2 extension rather than project bootstrap.
3. Use the released Mango CLI and its canonical project-generation entry; do not hand-copy starter files.
4. Run the PMO baseline, workspace, dependency, startup, documentation, acceptance, Maven architecture, and generated-backend gates required by the loaded rules and generated project.
5. Return `STOP` on version drift, baseline drift, workspace collision, failed startup, missing real data path, failed architecture verification, or failed acceptance evidence. Do not replace failed commands with a hand-built shortcut.
6. Return `NEXT` only when the generated project is reproducible, required services and entry points are verified, Maven/npm/CLI/PMO versions form a compatible released batch, and the evidence identifies the exact versions and workspace.

With an empty context, return `ASK` for the project identity and topology. Do not generate defaults that affect public coordinates or deployment shape.
