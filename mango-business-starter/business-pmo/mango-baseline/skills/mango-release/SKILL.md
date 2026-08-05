---
name: mango-release
description: The exclusive repository-local workflow for planning, publishing, recovering, verifying, and closing an actual Mango npm, Maven, CLI, starter, template, PMO, Skill, or documentation release. Use only when the user intends to version, publish, verify, or recover immutable Mango artifacts, tags, or a GitHub Release. Do not invoke an external or generic release skill. Do not use for frontend standards design, validation or adoption; lint, typecheck or directory governance; ordinary implementation; one-project dependency consumption; a non-release PR; PR review; or application traffic rollout.
---

# Mango Release

## Exclusive Entry

This repository Skill is the only workflow authority for Mango releases. If user-level, plugin-level, bundled, or generic release skills are also available, do not load or invoke them. They may not replace, wrap, or supplement this Skill. Repository PMO rules and release tools remain the execution authority.

## Classify The Boundary

Load this Skill only when the intended result changes or verifies an immutable Mango release coordinate, tag, or GitHub Release.

- Keep frontend standards governance on its own adoption states, such as `STANDARD_VALIDATED_LOCAL`, `STANDARD_READY_FOR_ADOPTION`, and `STANDARD_ENFORCED`. Do not translate those states into release completion.
- Treat `pilot -> affected -> repository` as code-standard adoption scope, not production traffic. Never add 5%/25% traffic, canary routing, fault injection, or deployment rollback to a standards task.
- Treat dependency recovery for a standards toolchain as restoration of reviewed exact versions, not as an application production rollback.
- Treat application deployment and traffic rollout as a separate, explicitly requested deployment contract. Do not invent rollout percentages or fault-injection steps for an artifact release.
- If no artifact coordinate, version, tag, registry publication, or GitHub Release is in scope, return to the task's implementation, governance, or verification workflow without invoking this Skill.

## Resolve And Load

Set `PMO_ROOT` to `<repo>/mango-pmo`; a business baseline alone is insufficient for publishing Mango artifacts. Read `$PMO_ROOT/rules/00-dev-flow.md`, `$PMO_ROOT/rules/05-ai-delivery-quality.md`, `$PMO_ROOT/rules/08-capability-docs.md`, `$PMO_ROOT/rules/10-release-artifacts.md`, and, for npm packages, `$PMO_ROOT/rules/frontend/03-component-development.md` plus `$PMO_ROOT/rules/frontend/06-monorepo-architecture.md`.

Run PMO preflight with `role=pmo`, `phase=release`, exact artifact paths, requested versions, registry roles, and release kind. Consult returned references only for release boundaries that are not already resolved by the release manifest, checker output, or current artifact metadata.

## Readiness

Return `ASK` until release scope, exact versions, dependency order, registry targets, upgrade audience, and authorization are explicit. Before any immutable action:

1. Prove the source commit is merged, clean, synchronized with `origin/main`, and covered by passing required checks. Record the full commit SHA, Git tree, and an exact-source archive or bundle checksum; reject evidence produced from a different tree or a dirty workspace.
2. Read `.github/branch-protection-policy.json` and compare it with the live base-branch protection. Stop on drift; never toggle protection for one release. In `single-owner` mode, approving-review count remains zero: required checks and the Owner's PR merge provide the authorization record, without a separate reviewer approval ceremony.
3. Produce one compatibility matrix for Maven, every npm package, CLI, starter/template, PMO, Skills, documentation, and consumers. A changed PMO/Skill bundle requires a new PMO version; an exact CLI dependency on PMO requires a corresponding CLI version.
4. Maintain the platform `CHANGELOG.md` and artifact changelogs first. The proposed GitHub Release body must contain `Fixed` or `Added/Changed`, `Versions`, `Published Packages`, `Upgrade Notes`, and `Verification` as applicable. Run the exact release-notes checker before the npm/Maven publish adapter; do not discover missing headings inside an immutable publish attempt.
5. Validate structured state adapters before publication. Assertions must check semantics, not incidental formatting such as YAML quote style. Use repository parsers/checkers where available.
6. Run `mango release registry doctor`, confirm every target version is absent before its first publish, and save registry/coordinate evidence without credentials.
7. Obtain explicit authorization in the current turn before tag, GitHub Release, registry publication, deployment, or other external mutation. Never persist authorization in configuration or infer it from delivery mode.

## Execute

Use `mango release status/verify/publish/repair --version <version>` as the only batch state owner. Configure stable adapters to invoke existing Maven/npm/docs scripts; do not duplicate their publication logic in the Skill.

1. Execute canonical shared gates once and per-artifact gates in dependency order. Bind the state manifest and every generated report to the recorded commit, Git tree, and source checksum.
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
2. A clean consumer resolves artifacts from the configured consume registry, not a workspace link, local publish cache, or source directory; it installs the published CLI/PMO/starter/template and executes the affected generated-project or upgrade flow. For reusable frontend packages, include a representative business project that imports the public API, styles, runtime configuration, and production build.
3. Tag and GitHub Release point to the intended release commit and carry the same versions and upgrade notes.
4. The platform `CHANGELOG.md` no longer says `PENDING`: published artifacts are `PUBLISHED_AND_VERIFIED`, GitHub Release is `CREATED_AND_VERIFIED`, and the recorded verification refers to the completed manifest. Submit and merge this closeout through a PR; do not move the release tag to the documentation-only closeout commit.
5. The canonical publication or read-only recovery manifest is complete with no pending/failed required state. Preserve any earlier failed manifest as audit evidence.
6. Stop services, release the task workspace and owned database, remove merged worktrees/local branches, delete obsolete remote task branches, prune refs, return to `main`, fast-forward, and prove local `main` equals `origin/main`. Preserve unrelated or dirty user worktrees.

Before `NEXT`, compare the exact Maven, npm, CLI, starter/template, PMO and Skill versions as one compatibility matrix. A release summary without artifact coordinates, registry back-checks, consumer evidence and authorization is `ASK` or `STOP`, never proof of completion.

With an empty context, return `ASK` for release scope and target versions. Do not infer a version bump.
