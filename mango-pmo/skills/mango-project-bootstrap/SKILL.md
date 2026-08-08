---
name: mango-project-bootstrap
description: Initialize and accept a new Mango-based business project, including project generation, PMO baseline, workspace initialization, startup, documentation entry points, and first-run evidence. Do not use to add a module to an existing project, implement a feature, upgrade an existing project, or publish Mango artifacts.
---

# Mango New Project

## Resolve And Load

Resolve `PMO_ROOT` from the installed business baseline, Mango source repository, or `<plugin-root>/dist/baseline`. Run PMO preflight with role `dev`, phase `develop`, and the actual project paths. Use returned code baselines as the implementation source; consult individual references only when a boundary is unresolved. Reuse the current non-`main` task worktree when one already exists.

## Execute

1. Return `ASK` until project identity, topology, target directory, package coordinates, required modules, and release versions are explicit.
2. 读取已解析的交付等级。新项目、新系统或新模块固定为 `L5`，要求业务需求、系统需求、技术设计、实施与验证计划及其直接追踪有效；只有事实证明任务是已有系统的有限扩展时，才按 `L2-L4` 对应单文档处理。
3. Use the released Mango CLI and its canonical project-generation entry; do not hand-copy starter files.
4. Run the PMO baseline, workspace, dependency, startup, documentation, acceptance, Maven architecture, and generated-backend gates required by the loaded rules and generated project.
5. Return `STOP` on version drift, baseline drift, workspace collision, failed startup, missing real data path, failed architecture verification, or failed acceptance evidence. Do not replace failed commands with a hand-built shortcut.
6. Return `NEXT` only when the generated project is reproducible, required services and entry points are verified, Maven/npm/CLI/PMO versions form a compatible released batch, and the evidence identifies the exact versions and workspace.

With an empty context, return `ASK` for the project identity and topology. Do not generate defaults that affect public coordinates or deployment shape.
