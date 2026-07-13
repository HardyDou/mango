---
name: mango-release
description: Plan, execute, and verify a Mango release or upgrade batch for npm, Maven, CLI, starter, template, PMO, or documentation artifacts. Do not use for ordinary implementation, dependency consumption inside one business project, creating a PR, or reviewing a PR.
---

# Mango Release

## Resolve And Load

Set `PMO_ROOT` to `<repo>/mango-pmo`; a business baseline alone is insufficient for publishing Mango artifacts. Read `$PMO_ROOT/rules/00-dev-flow.md`, `$PMO_ROOT/rules/05-ai-delivery-quality.md`, `$PMO_ROOT/rules/08-capability-docs.md`, `$PMO_ROOT/rules/10-release-artifacts.md`, and, for npm packages, `$PMO_ROOT/rules/frontend/03-component-development.md` plus `$PMO_ROOT/rules/frontend/06-monorepo-architecture.md`.

Run PMO preflight with the release role and phase, exact artifact paths, and requested versions; read every `Must read` file.

## Execute

1. Return `ASK` until release scope, exact versions, dependency order, registry targets, upgrade audience, and authorization are explicit.
2. Read `.github/branch-protection-policy.json` and compare it with the live base-branch protection. Stop on drift; never toggle review requirements for one release. Then run `mango release registry doctor` and use `mango release status/verify/publish/repair --version <version>` as the only batch state owner. Configure stable adapters to invoke existing Maven/npm/docs scripts; do not duplicate their publish logic in the Skill.
3. Build the release batch and execute the canonical shared and per-artifact gates selected by the loaded rules and repository release tools. Every fixed state must have `passed`, `failed`, `pending`, or `not_applicable` plus a non-empty auditable reason. Every applicable attempted state must also retain command, working directory, start/completion time, integer exit code, and non-empty redacted output; `not_applicable` records only its reason and decision time because no command ran.
4. Return `STOP` on dirty or inconsistent source state, version drift, missing upgrade notes, failed build, failed publish, failed repository back-check, missing consumer verification, missing release evidence, registry doctor failure, or any required state left pending/failed.
5. Never substitute a broad publish command for the canonical batch entry or publish an excluded deployment artifact without explicit approval. `repair` must skip passed immutable states. After a failed immutable attempt, repair must use the exact `{kind: verify-existing}` strategy, which invokes the same state's reviewed verify adapter; never configure an independent repair command or empty adapter list.
6. Return `NEXT` only when published artifacts, repository resolution, consumer entry points, changelog, upgrade instructions, documentation, GitHub Release, and the persisted release manifest all identify the same complete release batch.

Before `NEXT`, compare the exact Maven, npm, CLI, starter/template, PMO and Skill versions as one compatibility matrix. A release summary without artifact coordinates, registry back-checks, consumer evidence and authorization is `ASK` or `STOP`, never proof of completion.

With an empty context, return `ASK` for release scope and target versions. Do not infer a version bump.
