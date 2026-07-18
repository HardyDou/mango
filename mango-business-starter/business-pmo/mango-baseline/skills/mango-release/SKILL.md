---
name: mango-release
description: The exclusive repository-local workflow for planning, publishing, recovering, verifying, and closing a Mango npm, Maven, CLI, starter, template, PMO, Skill, or documentation release. Use this Skill for every Mango release task and do not invoke an external or generic release skill. Do not use for ordinary implementation, one-project dependency consumption, creating a non-release PR, or reviewing a PR.
---

# Mango Release

## Exclusive Entry

This repository Skill is the only workflow authority for Mango releases. If user-level, plugin-level, bundled, or generic release skills are also available, do not load or invoke them. They may not replace, wrap, or supplement this Skill. Repository PMO rules and release tools remain the execution authority.

## Resolve And Load

Set `PMO_ROOT` to `<repo>/mango-pmo`; a business baseline alone is insufficient for publishing Mango artifacts. Read `$PMO_ROOT/rules/00-dev-flow.md`, `$PMO_ROOT/rules/05-ai-delivery-quality.md`, `$PMO_ROOT/rules/08-capability-docs.md`, `$PMO_ROOT/rules/10-release-artifacts.md`, and, for npm packages, `$PMO_ROOT/rules/frontend/03-component-development.md` plus `$PMO_ROOT/rules/frontend/06-monorepo-architecture.md`.

Run PMO preflight with `role=pmo`, `phase=release`, exact artifact paths, requested versions, registry roles, and release kind; read every `Must read` file. Read the current capability map and the README for each changed public artifact.

## Readiness

Return `ASK` until release scope, exact versions, dependency order, registry targets, upgrade audience, and authorization are explicit. Before any immutable action:

1. Prove the source commit is merged, clean, synchronized with `origin/main`, and covered by passing required checks.
2. Read `.github/branch-protection-policy.json` and compare it with the live base-branch protection. Stop on drift; never toggle protection for one release.
3. Produce one compatibility matrix for Maven, every npm package, CLI, starter/template, PMO, Skills, documentation, and consumers. A changed PMO/Skill bundle requires a new PMO version; an exact CLI dependency on PMO requires a corresponding CLI version.
4. Maintain the platform `CHANGELOG.md` and artifact changelogs first. The proposed GitHub Release body must contain `Fixed` or `Added/Changed`, `Versions`, `Published Packages`, `Upgrade Notes`, and `Verification` as applicable. Run the exact release-notes checker before the npm/Maven publish adapter; do not discover missing headings inside an immutable publish attempt.
5. Validate structured state adapters before publication. Assertions must check semantics, not incidental formatting such as YAML quote style. Use repository parsers/checkers where available.
6. Run `mango release registry doctor`, confirm every target version is absent before its first publish, and save registry/coordinate evidence without credentials.

## Execute

Use `mango release status/verify/publish/repair --version <version>` as the only batch state owner. Configure stable adapters to invoke existing Maven/npm/docs scripts; do not duplicate their publication logic in the Skill.

1. Execute canonical shared gates once and per-artifact gates in dependency order.
2. Every fixed state must be `passed`, `failed`, `pending`, or reasoned `not_applicable`. Every attempted state retains command, working directory, start/completion time, integer exit code, and non-empty redacted output.
3. Never substitute a broad publish command for the canonical batch entry or publish excluded deployment artifacts without explicit approval.
4. Stop immediately on dirty or inconsistent source, version drift, missing notes, failed gates, failed publication, failed repository back-check, missing consumer evidence, registry doctor failure, or a required pending/failed state.

## Recover

1. Never republish a passed immutable Maven/npm/tag/Release/docs-snapshot state. `repair` skips it and uses the exact `{kind: verify-existing}` strategy for an attempted immutable state whose remote object may exist.
2. A pending immutable state with `attempts=0` must run its reviewed publish adapter; verify-only cannot manufacture publication evidence.
3. If evidence proves a publish adapter failed before any remote mutation, first back-check both publish and consume registries and prove the exact version/tag does not exist. Correct only the failed precondition, then allow one exact-state retry through the same state machine while retaining both attempts. Do not run a raw or broad publisher.
4. If remote mutation is possible or existence is ambiguous, return `STOP` and use verify-existing/manual repository investigation; never guess and never overwrite an immutable coordinate.
5. A verification-harness error after successful publication does not authorize republishing. Preserve the failed manifest, correct the semantic verifier, and create a read-only `mango release verify` manifest for the same immutable coordinates.

## Close

Return `NEXT` only after all of the following are true:

1. Published artifacts resolve from publish and consume repositories with matching versions, checksums/integrity, and timestamps.
2. A clean consumer installs the published CLI/PMO/starter/template and executes the affected generated-project or upgrade flow.
3. Tag and GitHub Release point to the intended release commit and carry the same versions and upgrade notes.
4. The platform `CHANGELOG.md` no longer says `PENDING`: published artifacts are `PUBLISHED_AND_VERIFIED`, GitHub Release is `CREATED_AND_VERIFIED`, and the recorded verification refers to the completed manifest. Submit and merge this closeout through a PR; do not move the release tag to the documentation-only closeout commit.
5. The canonical publication or read-only recovery manifest is complete with no pending/failed required state. Preserve any earlier failed manifest as audit evidence.
6. Stop services, release the task workspace and owned database, remove merged worktrees/local branches, delete obsolete remote task branches, prune refs, return to `main`, fast-forward, and prove local `main` equals `origin/main`. Preserve unrelated or dirty user worktrees.

Before `NEXT`, compare the exact Maven, npm, CLI, starter/template, PMO and Skill versions as one compatibility matrix. A release summary without artifact coordinates, registry back-checks, consumer evidence and authorization is `ASK` or `STOP`, never proof of completion.

With an empty context, return `ASK` for release scope and target versions. Do not infer a version bump.
