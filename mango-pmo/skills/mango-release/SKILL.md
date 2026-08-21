---
name: mango-release
description: The Mango source repository's exclusive workflow for planning, publishing, recovering, verifying, and closing immutable Mango platform npm, Maven, CLI, starter, template, PMO, Skill, documentation, tag, and GitHub Release artifacts. Use only inside the Mango source repository for an actual Mango component release. Never use for a Mango development PR, a business-project PR, a business application release or deployment, or one-project dependency consumption.
distribution: repository-only
---

# Mango Release

## Exclusive Entry

This repository Skill is the only workflow authority for Mango platform component releases from the Mango source repository. It is repository-only and must not be packaged into `@mango/pmo`, projected into Business Starter, or synchronized to a business project's `.agents/skills`. If user-level, plugin-level, bundled, or generic release skills are also available, do not load or invoke them. They may not replace, wrap, or supplement this Skill. Repository PMO rules and release tools remain the execution authority.

## Classify The Boundary

Load this Skill only when the intended result changes or verifies an immutable Mango release coordinate, tag, or GitHub Release.

- A Mango development, defect, governance, review, submission, or merge PR remains in its ordinary PMO and PR workflow even when it carries a Changeset. A Changeset records future release intent; it does not activate this Skill or the release state machine.
- A business-project PR, build, package, deployment, application release, traffic change, rollback, or dependency upgrade never uses this Skill. Follow that business repository's own delivery and deployment runbook.
- If the current repository is not the Mango source repository containing `mango-pmo/skills/mango-release` and the repository-local Mango release tools, do not invoke this Skill.

- Keep frontend standards governance on its own adoption states, such as `STANDARD_VALIDATED_LOCAL`, `STANDARD_READY_FOR_ADOPTION`, and `STANDARD_ENFORCED`. Do not translate those states into release completion.
- Treat `pilot -> affected -> repository` as code-standard adoption scope, not production traffic. Never add 5%/25% traffic, canary routing, fault injection, or deployment rollback to a standards task.
- Treat dependency recovery for a standards toolchain as restoration of reviewed exact versions, not as an application production rollback.
- Treat application deployment and traffic rollout as a separate, explicitly requested deployment contract. Do not invent rollout percentages or fault-injection steps for an artifact release.
- If no artifact coordinate, version, tag, registry publication, or GitHub Release is in scope, return to the task's implementation, governance, or verification workflow without invoking this Skill.

## Resolve And Load

Set `PMO_ROOT` to `<repo>/mango-pmo`; a business baseline alone is insufficient for publishing Mango artifacts. Read `$PMO_ROOT/rules/00-dev-flow.md`, `$PMO_ROOT/rules/05-ai-delivery-quality.md`, `$PMO_ROOT/rules/08-capability-docs.md`, `$PMO_ROOT/rules/10-release-artifacts.md`, and, for npm packages, `$PMO_ROOT/rules/frontend/03-component-development.md` plus `$PMO_ROOT/rules/frontend/06-monorepo-architecture.md`.

Run PMO preflight with `role=pmo`, `phase=release`, exact artifact paths, requested versions, registry roles, and release kind; read every `Must read` file. Read the current capability map and the README for each changed public artifact.

## Readiness

Return `ASK` until release scope, exact versions, dependency order, registry targets, upgrade audience, and authorization are explicit. Before any immutable action:

1. Require a Changeset for each directly changed published package. Run the Catalog compiler and Git impact checker to reject drift, duplicate ownership, missing, unrelated, or unknown declarations; let the fixed runtime dependency graph and CLI version matrix add dependent packages automatically.
2. Run `mango release plan` from the accumulated successful-release baseline, not from the latest commit. Review the Catalog-bound complete tuple, machine-generated release closure, target versions, topology, release notes hash, and one-time reconciliation marker when present. Never hand-maintain a batch package list.
3. Prove the prepared source is clean and covered by passing required checks. `mango release prepare` must record the full commit SHA, Git tree, exact-source archive checksum, release-plan digest, and SHA-256 for every sealed artifact.
4. Read `.github/branch-protection-policy.json` and compare it with the live base-branch protection. Stop on drift; never toggle protection for one release. In `single-owner` mode, approving-review count remains zero: required checks and the Owner's PR merge provide the authorization record, without a separate reviewer approval ceremony.
5. Produce one compatibility matrix for Maven, every npm package, CLI, starter/template, PMO, Skills, documentation, and consumers. A changed PMO/Skill bundle requires a new PMO version; an exact CLI dependency on PMO requires a corresponding CLI version.
6. Maintain the platform `CHANGELOG.md` and artifact changelogs first. The proposed GitHub Release body must contain the complete successful-release-baseline-to-candidate `Pull Requests` list; map every actual PR to `Fixed`, `Added` or `Changed`, exact packages and business adaptation; and include `Versions`, `Published Packages`, `Business Impact`, `Upgrade Estimate`, `Upgrade Notes`, `Verification`, and `Rollback`. The estimate states audience, engineering effort, execution window, service downtime, rollback effort and assumptions. Run the exact release-notes checker before publication; do not infer missing business impact or estimates from a generated package plan.
7. Run `mango release registry doctor`, save credential-free registry evidence, and obtain explicit authorization in the current turn before registry publication, tag, GitHub Release, deployment, or any other external mutation.

## Execute

Use the repository-local sequence below as the only batch owner:

```text
mango release plan
mango release prepare
handoff Release PR submission to $mango-submit-pr and run the lightweight policy check
mango release publish --authorize
mango release status | mango release repair --authorize
```

1. `prepare` builds once, packs once, seals the exact tarball/JAR/docs/source archive files under one `preparedCandidateId`, and runs one mixed consumer using those files plus unchanged consume-registry coordinates. `READY`, publication and recovery must reuse the recorded hashes and must not rebuild.
2. After the Release PR merges, `publish` requires the merged `origin/main` Git tree to equal the prepared tree and completes a full npm/Maven/docs dual-registry preflight before the first write. It then publishes the sealed artifacts in topology order, verifies publish and consume content, and runs one pure consume-registry consumer.
3. The states are `VALIDATED`, `PREPARED`, `READY`, `PUBLISHING`, `PARTIAL`, `AMBIGUOUS`, `REPAIR`, and `COMPLETED`. Every attempted command retains timestamps, exit code, redacted output, coordinate, registry role, checksum, and publication-journal transition.
4. Create the immutable Tag and GitHub Release only after the consumer verification recorded within `PUBLISHING`. Never create them as a pre-publication checkpoint.
5. Stop immediately on dirty or inconsistent source, plan drift, artifact hash drift, missing notes, failed gates, unknown registry state, failed publication, missing consumer evidence, or registry doctor failure.

## Recover

1. Never republish a coordinate whose publish copy exists and matches the sealed SHA-256. When consume visibility is delayed, enter `PARTIAL`, wait read-only for a bounded interval, and continue with `repair`.
2. `status` validates the sealed candidate and reads both registry roles without mutating the manifest. `publish`, `status`, and `repair` all use the Git common-dir release lock; an existing lock is never reclaimed automatically. `repair` repeats the complete preflight, skips journal-owned verified coordinates, and resumes only coordinates whose journal is `NOT_ATTEMPTED` and whose two registry roles both prove absence.
3. `INTENT_RECORDED` and `REQUEST_DISPATCHED` mean the request may have left the process. If both registry roles still report absent, enter `AMBIGUOUS` and stop instead of retrying. Remote content without this candidate's journal ownership, unknown state, partial Maven content, or digest mismatch is also `STOP`.
4. `PARTIAL` and `AMBIGUOUS` retain every earlier attempt and artifact checksum. Recovery uses the same `preparedCandidateId` and prepared directory and never rebuilds, broad-publishes, changes versions, edits the plan, automatically reclaims a stale lock, or adopts another candidate's coordinate.
5. A verifier defect after a successful remote write does not authorize republishing. Correct the verifier and resume read-only through the same manifest.

## Close

Return `NEXT` only after all of the following are true:

1. Published artifacts resolve from publish and consume repositories with matching versions, checksums/integrity, and timestamps.
2. A clean consumer resolves artifacts from the configured consume registry, not a workspace link, local publish cache, or source directory; it installs the published CLI/PMO/starter/template and executes the affected generated-project or upgrade flow. For reusable frontend packages, include a representative business project that imports the public API, styles, runtime configuration, and production build.
3. Tag and GitHub Release point to the intended release commit and carry the same versions and upgrade notes.
4. The platform `CHANGELOG.md` no longer says `PENDING`: published artifacts are `PUBLISHED_AND_VERIFIED`, GitHub Release is `CREATED_AND_VERIFIED`, and the recorded verification refers to the completed manifest. Submit the closeout PR through `$mango-submit-pr`; merge remains a separate repository-governance action. Do not move the release tag to the documentation-only closeout commit.
5. The canonical prepared-batch manifest is `COMPLETED` with no pending or failed package. Preserve earlier attempts as audit evidence.
6. Stop services, release the task workspace and owned database, remove merged worktrees/local branches, delete obsolete remote task branches, prune refs, return to `main`, fast-forward, and prove local `main` equals `origin/main`. Preserve unrelated or dirty user worktrees.

Before `NEXT`, compare the exact Maven, npm, CLI, starter/template, PMO and Skill versions as one compatibility matrix. A release summary without artifact coordinates, registry back-checks, consumer evidence and authorization is `ASK` or `STOP`, never proof of completion.

With an empty context, return `ASK` for release scope and target versions. Do not infer a version bump.
