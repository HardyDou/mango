# Mango Changelog

## v2026.07.14-pmo-1.2.4-cli-1.0.74-ci-fast-gates-release - 2026-07-14

### Changed

- Split the required PR gate into parallel PMO, CLI/JavaScript, Java, and documentation jobs behind the stable `pmo-doc-check` result.
- Add a trusted, two-minute `pr-contract-check` for PR-body risk and capability contracts; editing the PR body no longer starts Maven, Java, pnpm, or the heavy code gate.
- Cancel obsolete code-SHA runs, cache pnpm and Maven inputs, and classify generated-backend behavior independently so version, changelog, README, release-lock, and PMO-contract-only changes skip generated backend acceptance.
- Reduce generated-backend acceptance from 19 Maven invocations to 9 and remove all 13 `clean` executions while retaining positive, negative, fail-closed, static-analysis, architecture, schema, metadata, and affected-module coverage.
- Ship the same dependency-build separation and precise partial quality scope through the generated GitHub and Gitea workflows. No full Reactor PR gate and no human approval are introduced.

### Upgrade Notes

1. Install `@mango/cli@1.0.74` only after both it and `@mango/pmo@1.2.4` resolve from `npm-group`; Mango Maven remains `1.0.18`.
2. Run `mango pmo upgrade --project-dir . --to 1.2.4`, then synchronize the repository-host workflow.
3. On GitHub, require `pr-contract-check` and `pmo-doc-check`; a green result merges automatically without human Review.

### Release Batch

| Order | Target | Version / destination | Pre-release status |
|---|---|---|---|
| 1 | npm PMO bundle | `@mango/pmo@1.2.4` -> Nexus npm hosted | `READY_TO_PUBLISH` |
| 2 | npm CLI | `@mango/cli@1.0.74` -> Nexus npm hosted | `WAITING_FOR_PMO` |
| 3 | GitHub Release | `v2026.07.14-pmo-1.2.4-cli-1.0.74-ci-fast-gates-release` | `WAITING_FOR_ARTIFACTS` |

### Verification

- `node --test mango-pmo/tests/pmo-check-scope.test.mjs mango-pmo/tests/branch-protection-policy.test.mjs`
- `node --test mango-ui/packages/mango-cli/tests/generated-backend-gate-contract.test.mjs`
- `MANGO_BACKEND_GATE_VERSION=1.0.18 node mango-ui/packages/mango-cli/scripts/check-generated-backend-gate.mjs`
- `node mango-ui/packages/mango-cli/scripts/check-cli.mjs`
- Workflow YAML parsing, PMO package build/check, baseline sync, governance intent, and live branch-protection read-back.

## v2026.07.14-maven-1.0.18-pmo-1.2.3-cli-1.0.73-release - 2026-07-14

### Fixed

- Fix [Issue #480](https://github.com/HardyDou/mango/issues/480): a clean PR runner now installs the directly changed Maven modules' upstream SNAPSHOT prerequisites before the partial quality gate. The quality gate itself remains limited to directly changed modules and does not use `-am` or `-amd`.
- Fix [Issue #481](https://github.com/HardyDou/mango/issues/481): nested PMD, Checkstyle, and SpotBugs invocations from `mango:check` exclude the architecture-verification aggregator while the outer architecture gate remains enabled.
- Publish the matching scope classifier, governance guard, generated GitHub/Gitea workflow baseline, PMO bundle, and CLI release lock together so business repositories do not need local workarounds.

### Upgrade Notes

1. Publish and verify the non-app Mango Maven `1.0.18` batch first.
2. Install `@mango/cli@1.0.73` only after `@mango/pmo@1.2.3` and the CLI both resolve from `npm-group`.
3. Run `mango pmo upgrade --project-dir . --to 1.2.3`, synchronize the repository-host workflow, and upgrade the business backend's `<mango.version>` to `1.0.18`.
4. Existing PRs blocked by #480 or #481 should update to this release batch and rerun the required check once; unchanged failed runs must not be repeatedly retried.

### Release Batch

| Order | Target | Version / destination | Pre-release status |
|---|---|---|---|
| 1 | Maven non-app backend and docs bundle | `io.mango:*:1.0.18` -> Nexus Maven hosted | `READY_TO_PUBLISH` |
| 2 | npm PMO bundle | `@mango/pmo@1.2.3` -> Nexus npm hosted | `WAITING_FOR_MAVEN` |
| 3 | npm CLI | `@mango/cli@1.0.73` -> Nexus npm hosted | `WAITING_FOR_PMO` |
| 4 | GitHub Release | `v2026.07.14-maven-1.0.18-pmo-1.2.3-cli-1.0.73-release` | `WAITING_FOR_ARTIFACTS` |

### Verification

- `node --test mango-pmo/tests/pmo-check-scope.test.mjs`
- `mvn -q -f mango/pom.xml -pl mango-tools/mango-maven-plugin verify`
- Clean local Maven repository: prerequisite `-am install` followed by a direct-module partial quality gate completed with `BUILD SUCCESS`.
- GitHub implementation PR [#483](https://github.com/HardyDou/mango/pull/483) passed `PMO Documentation Checks / pmo-doc-check` before merge.

## v2026.07.14-pmo-1.2.2-cli-1.0.72-release - 2026-07-14

### Changed

- Fix [Issue #470](https://github.com/HardyDou/mango/issues/470): standard `pmo-doc-check` now reads repository-relative backend, frontend, and business-document roots from `mango.config.json.paths` instead of assuming `backend/`, `frontend/`, and `business-docs/`.
- Resolve the configured backend POM and directly changed Maven modules from the same path configuration; an invalid configured backend path now fails closed instead of producing a green check that skipped Java.
- Publish equivalent GitHub and Gitea workflow templates with the stable `PMO Documentation Checks / pmo-doc-check` identity. Partial backend checks remain limited to directly modified modules without `-am` or `-amd` and use the no-new-violations gate against the PR base.
- Reuse the existing explicit architecture-prerequisite build for distribution-only and governance checks so a clean GitHub runner has `mango-common` and the current gate plugin before generated-backend acceptance; no new workflow step or Reactor expansion is added.
- Make `scripts/publish-maven-batch.sh --all-non-app` publish the committed `mango-docs/**` tree as `io.mango:mango-docs-bundle:<version>` in the same Maven batch. The release no longer depends on a separate manual `deploy:deploy-file` step.

### Upgrade Notes

1. Install `@mango/cli@1.0.72` after both it and `@mango/pmo@1.2.2` resolve from `npm-group`; Mango Maven remains `1.0.17`.
2. In a historical business repository, set the real roots once, for example `"paths": { "backend": "baohan-backend", "frontend": "frontend", "businessDocs": "business-docs" }`.
3. Run `mango pmo upgrade --project-dir . --to 1.2.2`, then synchronize the standard `.gitea/workflows/pmo-doc-check.yml` or `.github/workflows/pmo-doc-check.yml` for the hosting platform.
4. Configure `PMO Documentation Checks / pmo-doc-check` as a required check in the business repository. This is repository-host configuration, not part of the Mango package release.

### Published Packages

| Order | Target | Version / destination | Status |
|---|---|---|---|
| 1 | npm PMO bundle | `@mango/pmo@1.2.2` -> `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/` | `PUBLISHED_AND_VERIFIED` |
| 2 | npm CLI | `@mango/cli@1.0.72` -> `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/` | `PUBLISHED_AND_VERIFIED` |
| 3 | GitHub Release | `v2026.07.14-pmo-1.2.2-cli-1.0.72-release` | `PUBLISHED` |

### Verification

- `node --test mango-pmo/tests/pmo-check-scope.test.mjs mango-pmo/tests/publish-maven-batch.test.mjs`
- `pnpm -C mango-ui --filter @mango/pmo build && pnpm -C mango-ui --filter @mango/pmo check`
- `node mango-ui/packages/mango-cli/scripts/check-release-versions.mjs`
- `node mango-ui/packages/mango-cli/scripts/check-cli.mjs`
- `node --test mango-ui/packages/mango-cli/tests/*.test.mjs`
- `node mango-business-starter/scripts/sync-pmo-baseline.mjs --check`
- Required implementation PR check `pmo-doc-check` passed in 7m07s; the release-record check passed in 1m00s.
- The release state manifest completed all 17 fixed states at `2026-07-14T02:41:08.818Z`; Maven and the versioned docs snapshot were explicitly not applicable for this npm-only release.
- Fresh `npm-group` reads and a clean install verified `@mango/pmo@1.2.2` and `@mango/cli@1.0.72`; immutable coordinates and checksums are recorded in `mango-docs/evidence/governance/release-v2026.07.14-pmo-1.2.2-cli-1.0.72-release.json`.

## v2026.07.14-pmo-1.2.1-cli-1.0.71-release - 2026-07-14

### Fixed

- Fix [Issue #464](https://github.com/HardyDou/mango/issues/464): `pnpm publish` normalized three PMO tool files to `0644` while the bundle manifest declared `0755`, causing business-project PMO upgrades to fail integrity verification.
- Declare every manifest-owned executable through `publishConfig.executableFiles` so the PMO tarball preserves executable modes without weakening the bundle contract.
- Run a real `pnpm pack` before publication and verify each packed baseline/plugin file's hash, size, and mode against the generated manifest.
- Extend post-publication verification to reject a downloaded PMO tarball whose file mode differs from its manifest, with a regression test covering the exact `0755` to `0644` failure.

### Upgrade Notes

1. Install `@mango/cli@1.0.71` from `npm-group` only after `@mango/pmo@1.2.1` and the CLI both resolve at their exact versions.
2. Run `mango pmo upgrade --project-dir . --to 1.2.1 --dry-run`, review the plan, then run the upgrade and `mango pmo check --project-dir . --locked`.
3. Do not use `@mango/pmo@1.2.0` / `@mango/cli@1.0.70` for business upgrades. Those immutable artifacts remain historical evidence of the packaging defect.
4. Mango Maven remains `1.0.17`; this patch does not require a backend dependency upgrade.

### Published Packages

| Order | Target | Version / destination | Status |
|---|---|---|---|
| 1 | npm PMO bundle | `@mango/pmo@1.2.1` -> `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/` | `PUBLISHED_AND_VERIFIED` |
| 2 | npm CLI | `@mango/cli@1.0.71` -> `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/` | `PUBLISHED_AND_VERIFIED` |
| 3 | GitHub Release | `v2026.07.14-pmo-1.2.1-cli-1.0.71-release` | `PUBLISHED` |

### Verification

- `pnpm -C mango-ui --filter @mango/pmo build`
- `pnpm -C mango-ui --filter @mango/pmo check`
- `node --test mango-ui/packages/mango-cli/tests/pmo-bundle.test.mjs`
- `node mango-ui/scripts/publish-package.mjs --verify-pmo-package-root=<extracted-package-root>` rejects the historical `@mango/pmo@1.2.0` tarball because its executable mode differs from the manifest.
- Business consumer dry-run against `/Users/hardy/Work/Yunxin/baohan-system-mango-pmo-1.2.0`: local `@mango/cli@1.0.71` / `@mango/pmo@1.2.1` tarballs produced `add: 99, update: 36, delete: 0, skip: 30, warn: 0`; the business worktree remained unchanged.
- Required PR check `pmo-doc-check` passed in 7m08s; the release state manifest completed all 17 fixed states at `2026-07-14T00:47:54.581Z`.
- A fresh `npm-group` download preserved `0755` for all three governed tools and passed package-root verification; a clean registry install repeated the business dry-run with `warn: 0` and no worktree changes.
- Immutable coordinates, checksums, recovery facts, and consumer evidence are recorded in `mango-docs/evidence/governance/release-v2026.07.14-pmo-1.2.1-cli-1.0.71-release.json`.

## v2026.07.13-maven-1.0.17-pmo-1.2.0-cli-1.0.70-release - 2026-07-13

### Changed

- Replace keyword/line-count risk grading with two explicit assessments: requirement impact and solution risk. The final L0-L3 level is their maximum; BRD/SRS hold impact pre-assessments, TDD fixes the final level, and Plan inherits it without downgrade.
- Add a required PR contract for the selected `STATIC/UNIT/API/UI` set, why it proves the acceptance outcome, and concrete reasons for every skipped type. A behavior-neutral button-position change can remain L0 with static review and a targeted screenshot; backend L3 work without a browser entry is not forced to add UI.
- Add machine gates and regression cases for risk maximum calculation, lifecycle risk escalation/no-downgrade, same requirement with different solution risks, one-line tenant changes, backend-only L3, and real transaction/tenant evidence.
- Keep `PMO Documentation Checks / pmo-doc-check` stable while classifying its internal work by changed paths. Frontend/docs/PMO-only changes skip Java; ordinary backend quality gates select only directly changed Maven modules and do not use `-am` or `-amd`. Dependency builds and consumer compatibility remain separate verification concerns.
- Keep architecture-tool build preparation separate from the quality gate: a clean runner explicitly installs the `mango-parent`, `mango-common`, and `mango-tools` prerequisite projects before testing the two gate modules, without expanding either build or quality scope through `-am` or `-amd`.
- Move the 212-module historical architecture inventory exclusively to the scheduled/manual `Mango Architecture Debt Inventory` workflow. Root/parent POM, architecture rule/plugin, debt baseline, or gate workflow changes use governance mode in PRs: current gate unit tests, baseline-only anti-increase validation, POM validation, and generated-business-backend acceptance replace a repeated full Reactor scan.
- Make generated business projects default to the HTTPS Nexus endpoint, while GitHub generated-backend acceptance uses only explicitly built local Mango artifacts and never connects to the internal Nexus.
- Adopt the PMO 1.2.0 / Mango Maven 1.0.17 rule expansion's 9,182 detected findings as an explicitly approved historical budget from a complete 212-module report. The schema-v4 budget records every module independently; subsequent budgets may only decrease and cannot move debt between modules. The base-bound acceptance remains inert audit evidence after merge and cannot authorize a later increase.
- Remove hidden `-am` expansion from the Maven quality plugin's delegated PMD, Checkstyle, and SpotBugs commands, and ship the same direct-module behavior through Mango Maven `1.0.17`, `@mango/pmo@1.2.0`, and `@mango/cli@1.0.70`.

### Upgrade Notes

1. Publish and verify the non-app Mango Maven `1.0.17` batch before publishing the npm packages; then install `@mango/cli@1.0.70` from `npm-group` after all target packages are visible.
2. Run `mango pmo upgrade --project-dir . --to 1.2.0`, then `mango pmo check --project-dir . --locked` in each business repository.
3. Fill the generated PR template's requirement impact, solution risk, final maximum, selected verification, sufficiency, and skipped-type reasons. Editing the PR body retriggers the required check.
4. Upgrade business backends to Mango Maven `1.0.17`; `1.0.16` still expands delegated static-analysis commands with `-am` and cannot provide the direct-module quality-gate contract.
5. Existing historical debt remains non-blocking for unrelated changes. Use the full inventory workflow before writing a global or per-module reduced debt budget.

### Published Packages

The release state machine completed the following target batch. Its 2026-07-13 verifier checked content hashes and sizes but omitted file modes; Issue #464 later invalidated the PMO/CLI upgrade pair, which is superseded by `@mango/pmo@1.2.1` / `@mango/cli@1.0.71`.

| Order | Target | Version / destination | Status |
|---|---|---|---|
| 1 | Maven non-app backend batch | `io.mango:*:1.0.17` -> Nexus Maven hosted | `PUBLISHED_AND_VERIFIED` |
| 2 | npm PMO bundle | `@mango/pmo@1.2.0` -> `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/` | `PUBLISHED; SUPERSEDED_BY_1.2.1` |
| 3 | npm CLI | `@mango/cli@1.0.70` -> `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/` | `PUBLISHED; SUPERSEDED_BY_1.0.71` |
| 4 | GitHub Release | `v2026.07.13-maven-1.0.17-pmo-1.2.0-cli-1.0.70-release` | `PUBLISHED` |

### Verification

- `node --test mango-pmo/tests/document-contract/document-contract.test.mjs mango-pmo/tests/pmo-check-scope.test.mjs mango-pmo/tests/risk-verification.test.mjs`
- `node mango-pmo/tests/skills/check-skill-evals.mjs`
- `node mango-pmo/tools/check-governance-intent.mjs`
- Direct-module Maven gate verification passed without `-am` or `-amd`: the Reactor contained only `mango-system-core` and `mango-architecture-verification`, completed in 18.286s, and delegated PMD, Checkstyle, and SpotBugs commands contained neither expansion flag.
- `node mango-ui/packages/mango-pmo/scripts/build-package.mjs`
- `node mango-ui/packages/mango-pmo/scripts/check-package.mjs`
- `node mango-ui/packages/mango-cli/scripts/check-release-versions.mjs`
- `node mango-ui/packages/mango-cli/scripts/check-cli.mjs`
- `node --test mango-ui/packages/mango-cli/tests/*.test.mjs`
- `node mango-business-starter/scripts/sync-pmo-baseline.mjs --check`
- Unified release manifest completed all 17 fixed states. The private Maven consume repository resolved 122 published coordinates, and the private npm consume repository resolved both exact package versions.
- Generated backend acceptance passed against Mango Maven `1.0.17`; a clean install of `@mango/cli@1.0.70` from `npm-group` exposed Mango Maven `1.0.17` as its default.
- Latest and versioned documentation endpoints returned HTTP 200. Detailed immutable-attempt and consumer evidence is recorded in `mango-docs/evidence/governance/release-v2026.07.13-maven-1.0.17-pmo-1.2.0-cli-1.0.70-release.json`.

## v2026.07.13-pmo-1.1.1-cli-1.0.69-release - 2026-07-13

### Changed

- Fix `mango release repair` so a pending immutable state with no prior attempt executes its first reviewed publish adapter instead of verify-only.
- Preserve immutable safety after an attempt: Maven, npm, tag, GitHub Release, and documentation snapshot recovery still requires `{ "kind": "verify-existing" }` and cannot republish an already attempted artifact.
- Fix `scripts/publish-maven-batch.sh --all-non-app` so `mango-architecture-verification` is flattened and deployed as a POM without running its full-Reactor verify phase inside the app-excluded release Reactor.
- Fix the architecture-debt base-ref reader so committed budgets larger than Node's default child-process buffer remain comparable in CI; the current 1.4 MiB, 9,038-item baseline now completes without `ENOBUFS`.
- Publish the matching PMO rule, Release Skill, boundary eval, CLI test, Maven dry-run test, README instructions, and release evidence together.

### Upgrade Notes

1. Install `@mango/cli@1.0.69` from `npm-group`.
2. Run `mango pmo upgrade --project-dir . --to 1.1.1`, then `mango pmo check --project-dir . --locked` in each business repository.
3. Keep the backend on Mango Maven `1.0.16`; this patch does not change Java runtime artifacts.
4. For an existing release manifest, use `repair` only after inspecting the failed state's `attempts`, `immutableAttempted`, evidence, and registry facts.

### Published Packages

The following repair batch was published once and re-resolved through the consume repository by the completed release manifest.

| Order | Target | Version / destination | Status |
|---|---|---|---|
| 1 | npm PMO bundle | `@mango/pmo@1.1.1` -> `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/` | `PUBLISHED` |
| 2 | npm CLI | `@mango/cli@1.0.69` -> `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/` | `PUBLISHED` |
| 3 | GitHub Release | `v2026.07.13-pmo-1.1.1-cli-1.0.69-release` | `PUBLISHED` |

### Verification

- `node --test mango-ui/packages/mango-cli/tests/release-command.test.mjs`
- `node --test mango-pmo/tests/publish-maven-batch.test.mjs`
- `node --test mango-pmo/tests/architecture-debt-budget.test.mjs`
- `node mango-pmo/tools/check-architecture-debt-budget.mjs --base-ref fb7473ea2b93d49073a9a43cf3118558d9ae358d`
- `node mango-pmo/tests/skills/check-skill-evals.mjs`
- `node mango-business-starter/scripts/sync-pmo-baseline.mjs --check`
- `node mango-ui/packages/mango-cli/scripts/check-release-versions.mjs`
- `pnpm -C mango-ui release:impact --base=origin/main --head=HEAD`
- Required PR check `pmo-doc-check`: PASS in 24m40s.
- Release manifest: complete at `2026-07-13T08:52:50.165Z`; all 17 states are `passed` or reasoned `not_applicable`.
- PMO/CLI hosted and consume-registry tarball verification: PASS; published integrities are recorded in `mango-docs/evidence/governance/release-v2026.07.13-pmo-1.1.1-cli-1.0.69-release.json`.

## v2026.07.13-maven-1.0.16-pmo-cli-release - 2026-07-13

### Changed

- Published the Java/Spring architecture enforcement and `MangoTypedCrudService` first as the complete non-app Mango Maven backend batch `1.0.16`; `1.0.15` already resolves from Nexus but cannot parse the new global Entity manifest contract and must not be reused.
- Published `@mango/pmo@1.1.0` second with the reproducible PMO bundle, document contracts, lifecycle checkers, dedicated agents, project Skills, and package-root Codex plugin projection.
- Published `@mango/cli@1.0.68` last, with exact dependencies on `@mango/pmo@1.1.0` and Mango Maven backend `1.0.16` so generated business modules never reference an incompatible Java contract.
- Use the new `mango release publish/status/verify/repair/registry doctor` state machine as the batch owner; registry roles are explicit, verification can run without republishing, and every state persists a non-empty auditable reason.
- Require schema-v2 release evidence for every applicable terminal state, revalidate required-state applicability, and restrict immutable repair to `{kind: verify-existing}` backed by the existing verify adapter; `not_applicable` records a reason and decision time without fabricating a command.
- Enforce Spring Service registration: business `XxxService implements IXxxService` uses `@Service`, replaceable framework defaults use starter `@Bean + @ConditionalOnMissingBean`, and direct construction or mutable static Service Locator patterns are rejected.
- Track the 9,038-item full-Reactor architecture inventory with schema-v2 module-aware reports and a schema-v4 budget across 212 Maven modules; Maven subtracts only base-SHA identities while CI compares base, PR, current and per-module aggregates so unchanged debt does not block new work, replacements and cross-module moves still fail, and each module can only ratchet downward.
- Support an executable `single-owner` branch-protection mode for repositories where the sole Owner authors PRs: zero impossible self-approval requirements, unchanged strict `pmo-doc-check`, resolved conversations, administrator enforcement, and force-push/deletion protection; retain `multi-maintainer` for independent Code Owner approval.
- Published the package, template, checker, workflow, capability documentation, Maven/npm artifacts, GitHub Release, Latest docs, and versioned snapshot as one verified batch.

### Upgrade Notes

1. Upgrade an existing business backend to `<mango.version>1.0.16</mango.version>` only after the complete Maven batch is available from `maven-releases` and a clean consumer can resolve it; do not use the already-published incompatible `1.0.15` plugin with this template batch.
2. Install or upgrade the CLI with `npm install -g @mango/cli@1.0.68 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/` only after both npm packages are visible through `npm-group`.
3. In each business repository, run `mango pmo upgrade --project-dir . --to 1.1.0`, then `mango pmo check --project-dir . --locked` and review the project-owned Skills under `.agents/skills`.
4. Migrate CRUD services to `MangoTypedCrudService<Entity, CreateCommand, UpdateCommand, PageQuery, VO, Long>` and keep the implementation on `MangoCrudServiceImpl<Mapper, Entity>`; migrate Controllers to explicit `XxxApi` adapters before running Maven `verify`.
5. `mango pmo sync` repairs the version recorded in `business-pmo/pmo-lock.json`; it does not select a newer PMO version. `rollback` only restores a verified local backup.
6. Project Skill synchronization does not install or modify a user-level Codex plugin. User-profile plugin installation remains a separate explicit operation against the published `@mango/pmo` package.
7. For historical architecture cleanup, run one complete Reactor scan, query with `check-architecture-debt-budget.mjs --module <moduleKey|artifactId>`, write only verified reductions with `--module ... --write`, then run the global `--base-ref` check before submission.

### Published Packages

The following targets were published and re-resolved through their consume repositories by the completed release manifest.

| Order | Target | Version / destination | Status |
|---|---|---|---|
| 1 | Complete Mango backend non-app Maven reactor | `1.0.16` -> `http://nexus.inner.yunxinbaokeji.com/repository/maven-releases/` | `PUBLISHED` |
| 2 | npm PMO bundle | `@mango/pmo@1.1.0` -> `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/` | `PUBLISHED` |
| 3 | npm CLI | `@mango/cli@1.0.68` -> `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/` | `PUBLISHED` |
| 4 | Versioned Mango Docs snapshot | `v2026.07.13-maven-1.0.16-pmo-cli-release` -> GitHub Pages | `PUBLISHED` |
| 5 | GitHub Release | release notes containing this dependency order and upgrade procedure | `PUBLISHED` |

### Verification

Release-candidate checks and final repository/consumer verification completed:

- `node mango-ui/packages/mango-pmo/scripts/build-package.mjs`
- `node mango-ui/packages/mango-pmo/scripts/check-package.mjs`
- `node --test mango-ui/packages/mango-cli/tests/pmo-bundle.test.mjs`
- `node mango-ui/scripts/check-release-impact.mjs --self-test`
- `node --test mango-ui/packages/mango-cli/tests/release-command.test.mjs`
- `node --test mango-pmo/tests/architecture-debt-budget.test.mjs`
- `node mango-pmo/tools/check-architecture-debt-budget.mjs --base-ref "$(git merge-base HEAD origin/main)"`
- `node mango-pmo/tools/check-architecture-debt-budget.mjs --module mango-platform/mango-system`
- `node mango-pmo/tools/check-architecture-debt-budget.mjs --module mango-system-core`
- `node --test mango-pmo/tests/branch-protection-policy.test.mjs`
- `node mango-pmo/tools/check-governance-intent.mjs`
- `mvn -f mango/pom.xml -pl :mango-maven-plugin -am test -DskipTests=false`
- `MANGO_BACKEND_GATE_VERSION=1.0.0-SNAPSHOT node mango-ui/packages/mango-cli/scripts/check-generated-backend-gate.mjs`
- `npm pack --dry-run --json` from `mango-ui/packages/mango-pmo`
- `npm --prefix mango-docs run docs:snapshot -- v2026.07.13-maven-1.0.16-pmo-cli-release`
- `npm --prefix mango-docs run docs:build`

The completed release manifest records all 17 states as `passed`, including the complete Maven architecture suite, PMO lifecycle/document checks, template projection checks, package export and release-impact gates, Maven/npm consume-repository lookups, clean generated backend verification, GitHub Release, Latest docs, and the versioned documentation snapshot.

## v2026.07.11-maven-1.0.14-cli-release - 2026-07-11

### Changed

- Published the current `main` backend implementation forward as the complete non-app Maven reactor batch `1.0.14`.
- Published `@mango/cli@1.0.67` with `maven.mangoBackend` locked to `1.0.14`; the certified frontend npm package lock remains unchanged.
- No backend or frontend runtime source was reverted or replaced with an older implementation.

### Upgrade Notes

- Business backends should set `<mango.version>1.0.14</mango.version>` and refresh Maven dependencies as one platform batch.
- Install or upgrade the CLI with `npm install -g @mango/cli@1.0.67 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- Frontend packages remain on the versions published by `v2026.07.11-npm-readme-forward-release`; no frontend package downgrade is required.

### Published Packages

- Maven: complete Mango backend non-app reactor artifacts at `1.0.14` to `http://nexus.inner.yunxinbaokeji.com/repository/maven-releases/`.
- npm: `@mango/cli@1.0.67` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- GitHub Release: `v2026.07.11-maven-1.0.14-cli-release`.

### Verification

- `scripts/publish-maven-batch.sh --all-non-app --release-version 1.0.14 --dry-run`
- `scripts/publish-maven-batch.sh --all-non-app --release-version 1.0.14`
- `pnpm -C mango-ui --filter @mango/cli test`
- `pnpm -C mango-ui publish:pkg cli --dry-run --release-tag=v2026.07.11-maven-1.0.14-cli-release`
- `pnpm -C mango-ui --filter @mango/cli run check:release-versions -- --check-registry --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`

## v2026.07.11-npm-readme-forward-release - 2026-07-11

### Changed

- Published a forward-only npm patch batch from the current `main` implementation. No runtime source was reverted.
- Included the corrected package READMEs for the development-center search panel entry, custom Admin Shell login route,
  file access baseline, responsive preview dialog, and `FilePreviewPanel.downloadPermission` migration.
- Advanced exact internal dependencies from `@mango/admin-pages@1.0.20` and `@mango/file@1.0.21` through the affected
  system, feature, Shell, admin aggregate, starter, and CLI release locks.

### Upgrade Notes

- Upgrade this batch together; do not mix the new `@mango/admin-pages` or `@mango/file` versions with aggregate packages
  from `v2026.07.11-npm-lock-sync-release`.
- Consumers of `FilePreviewPanel` must remove `downloadPermission`. Runtime behavior remains the current login-level file
  access model; file list, archive, delete, directory, storage, and settings management permissions remain unchanged.
- Install or upgrade the CLI with
  `npm install -g @mango/cli@1.0.66 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- Generated backend projects remain on Mango Maven backend `1.0.13`.

### Published Packages

- npm: `@mango/admin-pages@1.0.20`
- npm: `@mango/file@1.0.21`
- npm: `@mango/system@1.0.19`
- npm: `@mango/calendar@1.0.21`
- npm: `@mango/cms@1.0.10`
- npm: `@mango/job@1.0.13`
- npm: `@mango/link@1.0.7`
- npm: `@mango/notice@1.0.22`
- npm: `@mango/numgen@1.0.21`
- npm: `@mango/payment@1.0.12`
- npm: `@mango/template@1.0.21`
- npm: `@mango/workflow@1.0.27`
- npm: `@mango/workflow-business-example@1.0.26`
- npm: `@mango/admin-shell@1.0.40`
- npm: `@mango/admin@1.0.45`
- npm: `@mango/cli@1.0.66`
- Registry: `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`
- GitHub Release: `v2026.07.11-npm-readme-forward-release`

### Verification

- `git diff --check`
- `pnpm -C mango-ui --filter @mango/cli run check:release-versions`
- `pnpm -C mango-ui release:impact --base=origin/main --head=HEAD`
- `pnpm -C mango-ui admin:styles:check`
- `pnpm -C mango-ui admin:module-styles:check`
- `node mango-pmo/tools/audit-module-readmes.mjs`
- `node mango-pmo/tools/audit-readme-source-facts.mjs`
- `pnpm -C mango-ui run package-consumer:typecheck -- --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`

## v2026.07.11-npm-lock-sync-release - 2026-07-11

### Changed

- Synchronized the Mango frontend npm release lock to the latest certified package batch, so new generated business
  projects and package consumers resolve a coherent set of `@mango/*` versions.
- `@mango/admin-shell@1.0.39` adds `login.component` so a business application can replace the default `/login` route
  while reusing Mango login flow configuration, and registers a development-center search panel example route.
- `@mango/admin-pages@1.0.19` registers the `demo/components/SearchPanelView` development-center page descriptor used
  by the Shell example route.
- `@mango/file@1.0.20` aligns the file management UI with the login-level file access baseline: upload, preview, and
  download actions no longer use the removed `file:files:upload`, `file:files:query`, or `file:files:download` UI
  permission gates. It also makes the file preview dialog responsive and removes the public
  `FilePreviewPanel.downloadPermission` prop.
- Updated starter frontend package templates to consume `@mango/admin@1.0.44`, `@mango/admin-pages@1.0.19`, and
  `@mango/common@1.0.16`.
- Hardened the packed consumer type gate for pnpm 11 by writing supported workspace overrides for local tarballs and
  carrying the approved dependency build-script allowlist into the temporary consumer workspace.

### Upgrade Notes

- Business frontends should upgrade Mango frontend packages as a batch. Do not mix the new aggregate packages with
  older `@mango/auth`, `@mango/admin-pages`, `@mango/file`, `@mango/system`, or workflow package versions.
- Install or upgrade the CLI with
  `npm install -g @mango/cli@1.0.65 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- New generated business projects continue to use Mango Maven backend `1.0.13` and receive the npm lock batch below.
- Consumers of `FilePreviewPanel` must remove `downloadPermission`; download visibility and access are now determined
  by the component state and backend file access checks. File list, archive, delete, directory, storage, and settings
  management permissions remain unchanged.

### Published Packages

- npm: `@mango/admin@1.0.44`
- npm: `@mango/admin-pages@1.0.19`
- npm: `@mango/admin-shell@1.0.39`
- npm: `@mango/auth@1.0.15`
- npm: `@mango/calendar@1.0.20`
- npm: `@mango/cms@1.0.9`
- npm: `@mango/file@1.0.20`
- npm: `@mango/grid-layout@1.0.7`
- npm: `@mango/grid-widgets@1.0.13`
- npm: `@mango/home@1.0.5`
- npm: `@mango/job@1.0.12`
- npm: `@mango/link@1.0.6`
- npm: `@mango/notice@1.0.21`
- npm: `@mango/numgen@1.0.20`
- npm: `@mango/payment@1.0.11`
- npm: `@mango/rbac@1.0.13`
- npm: `@mango/site-shell@1.0.3`
- npm: `@mango/system@1.0.18`
- npm: `@mango/template@1.0.20`
- npm: `@mango/workflow@1.0.26`
- npm: `@mango/workflow-business-example@1.0.25`
- npm: `@mango/cli@1.0.65`
- Registry: `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`
- GitHub Release: `v2026.07.11-npm-lock-sync-release`

### Verification

- `git diff --check`
- `pnpm -C mango-ui --filter @mango/cli run check:release-versions`
- `pnpm -C mango-ui release:impact --base=origin/main --head=HEAD`
- `pnpm -C mango-ui admin:styles:check`
- `pnpm -C mango-ui admin:module-styles:check`
- `pnpm -C mango-ui --filter @mango/cli test`
- `pnpm -C mango-ui --filter @mango/file test`
- `pnpm -C mango-ui --filter @mango/workflow test`
- `pnpm -C mango-ui --filter @mango/admin-shell test`
- `pnpm -C mango-ui --filter @mango/workflow-business-example test`
- `pnpm -C mango-ui run package-consumer:typecheck -- --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`

## v2026.07.09-common-search-panel-form-layout-release - 2026-07-09

### Changed

- Released `@mango/common@1.0.16` with configurable `MangoSearchPanel` form layout defaults, including four columns per
  row, two collapsed rows, configurable label suffix, label alignment, form size, and bottom-centered icon-only expand
  or collapse control.
- Kept `MangoSearchPanel` shellless so business pages can place it inside their own card or page containers without
  duplicated background, border, shadow, or padding.

### Upgrade Notes

- Business frontends that use `MangoSearchPanel` should upgrade to `@mango/common@1.0.16`.
- Continue importing the component from `@mango/common` or `@mango/common/components/MangoSearchPanel/index.vue`.
- Continue importing styles through `@mango/common/style.css`.
- This is a single-package release for the common component package. It does not change backend API, database, menu,
  permission, tenant, Maven version, CLI templates, or admin package runtime behavior.

### Published Packages

- npm: `@mango/common@1.0.16` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- GitHub Release: `v2026.07.09-common-search-panel-form-layout-release`.

### Verification

- `git diff --check`
- `pnpm -C mango-ui admin:styles:check`
- `pnpm -C mango-ui admin:module-styles:check`
- `pnpm -C mango-ui --filter @mango/common build`
- `pnpm -C mango-ui --filter @mango/common exec vitest run components/MangoListPage/__tests__/MangoPageLayout.spec.ts`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/common --version=1.0.16 --tag=v2026.07.09-common-search-panel-form-layout-release`
- `pnpm -C mango-ui publish:pkg @mango/common --release-tag=v2026.07.09-common-search-panel-form-layout-release`
- `pnpm -C mango-ui release:verify-npm @mango/common --version=1.0.16`

## v2026.07.09-common-search-panel-shellless-release - 2026-07-09

### Changed

- Released `@mango/common@1.0.15` with the shellless `MangoSearchPanel` style so business pages can place the
  component inside their own card or page containers without duplicated background, border, shadow, or padding.
- Kept the fixed-column search layout, collapsed-field behavior, and bottom expand button from the current main branch.

### Upgrade Notes

- Business frontends that use `MangoSearchPanel` should upgrade to `@mango/common@1.0.15`.
- Continue importing the component from `@mango/common` or `@mango/common/components/MangoSearchPanel/index.vue`.
- Continue importing styles through `@mango/common/style.css`.
- This is a single-package release for the common component package. It does not change backend API, database, menu,
  permission, tenant, Maven version, CLI templates, or admin package runtime behavior.

### Published Packages

- npm: `@mango/common@1.0.15` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- GitHub Release: `v2026.07.09-common-search-panel-shellless-release`.

### Verification

- `git diff --check`
- `pnpm -C mango-ui admin:styles:check`
- `pnpm -C mango-ui admin:module-styles:check`
- `pnpm -C mango-ui --filter @mango/common build`
- `pnpm -C mango-ui --filter @mango/common exec vitest run components/MangoListPage/__tests__/MangoPageLayout.spec.ts`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/common --version=1.0.15 --tag=v2026.07.09-common-search-panel-shellless-release`
- `pnpm -C mango-ui publish:pkg @mango/common --release-tag=v2026.07.09-common-search-panel-shellless-release --skip-shared-gates`
- `pnpm -C mango-ui release:verify-npm @mango/common --version=1.0.15`

## v2026.07.09-auth-login-flow-1.0.14-release - 2026-07-09

### Changed

- 基于已合并的 `main` 分支发布 `@mango/auth@1.0.14`，方便业务系统从 Nexus 获取包含
  `useMangoLoginFlow` 的最新登录 Hook 包版本。

### Upgrade Notes

- 需要登录 Hook 的业务前端升级到 `@mango/auth@1.0.14`。
- Hook 引入方式保持为：`import { useMangoLoginFlow } from '@mango/auth';`。
- 使用包内登录页面时，样式仍通过 `@mango/auth/style.css` 引入。
- 本次为基于已合并主干的 auth 单包发布，不调整后端接口、数据库、菜单、权限、租户、Maven 版本、CLI 模板或 admin 包运行时行为。

### Published Packages

- npm: `@mango/auth@1.0.14` 发布到 `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`。
- GitHub Release: `v2026.07.09-auth-login-flow-1.0.14-release`。

### Verification

- `git diff --check`
- `pnpm -C mango-ui admin:styles:check`
- `pnpm -C mango-ui admin:module-styles:check`
- `pnpm -C mango-ui --filter @mango/auth build`
- `pnpm -C mango-ui package-consumer:typecheck -- --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/auth --version=1.0.14 --tag=v2026.07.09-auth-login-flow-1.0.14-release`
- `pnpm -C mango-ui publish:pkg @mango/auth --release-tag=v2026.07.09-auth-login-flow-1.0.14-release --skip-shared-gates`
- `pnpm -C mango-ui release:verify-npm @mango/auth --version=1.0.14`

## v2026.07.10-link-page-business-home-release - 2026-07-10

### Changed

- Published `@mango/link-page@1.0.4` with the business-home navigation page changes from the merged link-page work.
- Updated the generated project release lock so business frontends resolve `@mango/link-page@1.0.4` instead of the older
  `1.0.3` package.

### Upgrade Notes

- Business frontends that use `MangoLinkPage` should upgrade to `@mango/link-page@1.0.4`.
- Continue importing the page styles through `@mango/link-page/style.css`.
- This is a single-package frontend release. It does not change backend API, database, menu, permission, tenant,
  Maven version, or the workbench link-navigation widget.

### Published Packages

- npm: `@mango/link-page@1.0.4` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- GitHub Release: `v2026.07.10-link-page-business-home-release`.

### Verification

- `git diff --check`
- `pnpm -C mango-ui release:impact --base=origin/main --head=HEAD`
- `pnpm -C mango-ui --filter @mango/link-page build`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/link-page --version=1.0.4 --tag=v2026.07.10-link-page-business-home-release`
- `MANGO_SHARED_PUBLISH_GATES_PASSED=1 pnpm -C mango-ui publish:pkg @mango/link-page --release-tag=v2026.07.10-link-page-business-home-release --skip-shared-gates`
- `pnpm -C mango-ui release:verify-npm @mango/link-page --version=1.0.4`

## v2026.07.09-auth-login-flow-release - 2026-07-09

### New

- Published `@mango/auth@1.0.13` with the reusable `useMangoLoginFlow` hook for business systems that need to build
  custom login pages while reusing Mango login, tenant, WeCom login, forced password reset, token storage, user context,
  and safe redirect behavior.

### Changed

- Updated release tooling so npm and pnpm commands resolve correctly on Windows release machines.
- Added an npm tarball contract for `@mango/auth` to verify the published package exposes `useMangoLoginFlow`.

### Upgrade Notes

- Business frontends that need the login hook should upgrade to `@mango/auth@1.0.13`.
- Import the hook from `@mango/auth`: `import { useMangoLoginFlow } from '@mango/auth';`.
- Continue importing auth styles through `@mango/auth/style.css` when using package-provided pages.
- This is a single-package auth release. It does not change backend API, database, menu, permission, tenant, Maven
  version, CLI templates, or admin package runtime behavior.

### Published Packages

- npm: `@mango/auth@1.0.13` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- GitHub Release: `v2026.07.09-auth-login-flow-release`.

### Verification

- `git diff --check`
- `pnpm -C mango-ui admin:styles:check`
- `pnpm -C mango-ui admin:module-styles:check`
- `pnpm -C mango-ui --filter @mango/auth build`
- `pnpm -C mango-ui package-consumer:typecheck -- --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `node --check mango-ui/scripts/release-guard-utils.mjs`
- `node --check mango-ui/scripts/publish-package.mjs`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/auth --version=1.0.13 --tag=v2026.07.09-auth-login-flow-release`
- `pnpm -C mango-ui publish:pkg @mango/auth --release-tag=v2026.07.09-auth-login-flow-release --skip-shared-gates`
- `pnpm -C mango-ui release:verify-npm @mango/auth --version=1.0.13`

## v2026.07.08-admin-page-layout-release - 2026-07-08

### New

- Added `@mango/common` admin page skeleton components for business backend pages:
  `MangoListPage`, `MangoSearchPanel`, `MangoListPanel`, `MangoDetailPage`, `MangoFormPage`, and `MangoPageSection`.
- Added standard and compact admin page theme entries through `@mango/common/theme/admin-standard.css` and
  `@mango/common/theme/admin-compact.css`, so business projects can keep multiple admin page styles side by side.
- Added collapsible search panel behavior for generated list pages, including common-condition folding and right-aligned
  query/reset actions in the lower-right corner.

### Changed

- Updated generated business module list templates in `@mango/cli` and `mango-business-starter` to use the shared page
  skeleton instead of page-local custom layout classes.
- Updated frontend PMO rules so new list, detail, form, and search layouts are governed by shared framework components
  instead of business-specific page CSS.
- Advanced the certified frontend npm batch through `@mango/cli@1.0.64` while keeping the generated backend Maven lock
  on Mango Maven `1.0.13`.

### Upgrade Notes

- Business frontends should upgrade Mango npm packages as one batch using the versions listed below. Do not mix
  `@mango/common@1.0.13` with older admin packages that still depend on `@mango/common@1.0.12`.
- Generated or upgraded business projects should use `@mango/cli@1.0.64`; its release lock points to Mango Maven
  `1.0.13` and this npm package batch.
- Existing business pages can migrate incrementally by replacing custom page wrappers with `MangoListPage`,
  `MangoSearchPanel`, `MangoListPanel`, `MangoDetailPage`, and `MangoFormPage`. No backend API, database, menu,
  permission, tenant, or Maven dependency change is required for this release.

### Published Packages

- npm: `@mango/admin@1.0.43`, `@mango/admin-pages@1.0.18`, `@mango/admin-shell@1.0.38`,
  `@mango/auth@1.0.12`, `@mango/calendar@1.0.19`, `@mango/cms@1.0.8`, `@mango/common@1.0.13`,
  `@mango/file@1.0.19`, `@mango/grid-layout@1.0.6`, `@mango/grid-widgets@1.0.12`,
  `@mango/home@1.0.4`, `@mango/job@1.0.11`, `@mango/link@1.0.5`, `@mango/notice@1.0.20`,
  `@mango/numgen@1.0.19`, `@mango/payment@1.0.10`, `@mango/rbac@1.0.12`,
  `@mango/site-shell@1.0.2`, `@mango/system@1.0.17`, `@mango/template@1.0.19`,
  `@mango/workflow@1.0.25`, `@mango/workflow-business-example@1.0.24`, and `@mango/cli@1.0.64`
  to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- GitHub Release: `v2026.07.08-admin-page-layout-release`.

### Verification

- `git diff --check`
- `node mango-ui/scripts/check-release-impact.mjs --base=v2026.07.07-maven-1.0.13-menu-api-codes-release --head=HEAD`
- `cd mango-ui/packages/common && ../../node_modules/.bin/vite build && node ../../scripts/generate-package-types.mjs`
- `cd mango-ui/packages/common && ../../node_modules/.bin/vitest run components/MangoListPage/__tests__/MangoPageLayout.spec.ts`
- `node mango-ui/packages/mango-cli/scripts/check-cli.mjs`
- `node mango-ui/packages/mango-cli/scripts/check-release-versions.mjs`
- `node mango-ui/scripts/generate-package-styles.mjs --root mango-ui --manifest mango-ui/packages/admin/admin-modules.json --package mango-ui/packages/admin/package.json --out mango-ui/packages/admin/generated-package-styles.css --admin-manifest-out mango-ui/packages/admin/admin-packages.json --full-style-out mango-ui/packages/admin/style-full.css --full-entry-out mango-ui/packages/admin/src/full.ts --full-types-out mango-ui/packages/admin/src/full.d.ts --build-deps-script-out mango-ui/packages/admin/build-style-deps.mjs --check`
- `node mango-ui/scripts/check-admin-module-style-governance.mjs`
- `node mango-pmo/tools/audit-module-readmes.mjs`
- `node mango-pmo/tools/audit-readme-source-facts.mjs`
- `PR_BODY_FILE=.runtime/release-admin-page-layout-pr-body.md node mango-pmo/tools/check-capability-docs.mjs --base=v2026.07.07-maven-1.0.13-menu-api-codes-release --head=HEAD`
- `MANGO_SHARED_PUBLISH_GATES_PASSED=1 pnpm -C mango-ui publish:pkg <package> --release-tag=v2026.07.08-admin-page-layout-release --skip-shared-gates`
- `pnpm -C mango-ui release:verify-npm <package> --version=<version>`
- `gh release view v2026.07.08-admin-page-layout-release`

## v2026.07.07-maven-1.0.13-menu-api-codes-release - 2026-07-07

### Fixed

- Fixed menu/API permission coupling in Authorization. `menuCode` now controls menu visibility, while menu `apiCodes`
  provide the API permission codes granted with that menu.
- Fixed business approval authorization where workflow API permissions could expose workflow/risk menus to non-risk
  roles. Business menus can now carry required `workflow:*` API permissions without showing workflow parent menus.
- Fixed hidden baseline API permission grants for anonymous and logged-in users. File upload/preview/download and notice
  site-message baseline permissions are now injected through hidden `ROLE_ANONYMOUS` / `ROLE_LOGIN` menus without adding
  visible navigation.
- Fixed home/workflow widget permission states from the previous unreleased main changes so missing workflow page entries
  or API permissions show an inline access state instead of leading users into unavailable pages.

### Changed

- Added `authorization_menu.api_codes` and migration `V107__menu_api_codes.sql` to fold legacy menu/button permission
  nodes into parent menu API permissions and remove old button-menu role bindings.
- Updated `AUTH_MENU` resource usage: modules declare page-owned API permissions with `menus[].apiCodes` and no longer
  declare `permissions`, `permissionItems`, or `permissionCode` menu nodes.
- Updated RBAC menu management so operators edit menu API codes on the menu itself and role assignment only grants real
  menu nodes.
- Advanced generated business backend Maven lock to `1.0.13` through `@mango/cli@1.0.63`.

### Upgrade Notes

- Business backends should set `<mango.version>1.0.13</mango.version>` to consume the menu/API permission split and
  migration.
- Business modules should move any menu-owned API permissions from legacy `permissionItems`/button nodes to the owning
  menu `apiCodes` field.
- If a business page needs workflow, notice, file, or other platform API permissions without exposing that platform
  menu, put those API permission codes on the business menu's `apiCodes`.
- Anonymous and logged-in baseline APIs should be injected through hidden menus bound to `ROLE_ANONYMOUS` or
  `ROLE_LOGIN`; hidden menus do not render in the user menu tree.
- Generated or upgraded business projects should use `@mango/cli@1.0.63`; its release lock points to Mango Maven
  `1.0.13` and this npm package batch.

### Published Packages

- Maven: Mango backend platform non-app artifacts at `1.0.13` to
  `http://nexus.inner.yunxinbaokeji.com/repository/maven-releases/`.
- npm: `@mango/admin@1.0.42`, `@mango/admin-pages@1.0.17`, `@mango/admin-shell@1.0.37`,
  `@mango/calendar@1.0.18`, `@mango/cms@1.0.7`, `@mango/file@1.0.18`, `@mango/grid-widgets@1.0.11`,
  `@mango/home@1.0.3`, `@mango/job@1.0.10`, `@mango/link@1.0.4`, `@mango/link-openapi@1.0.2`,
  `@mango/link-page@1.0.3`, `@mango/notice@1.0.19`, `@mango/numgen@1.0.18`, `@mango/payment@1.0.9`,
  `@mango/rbac@1.0.11`, `@mango/system@1.0.16`, `@mango/template@1.0.18`, `@mango/workflow@1.0.24`,
  `@mango/workflow-business-example@1.0.23`, and `@mango/cli@1.0.63` to
  `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- GitHub Release: `v2026.07.07-maven-1.0.13-menu-api-codes-release`.

### Verification

- `git diff --check`
- `node mango-ui/scripts/check-release-impact.mjs --base=v2026.07.07-maven-1.0.12-flyway-bootstrap-release --head=HEAD`
- `mvn -pl mango-platform/mango-authorization/mango-authorization-core,mango-platform/mango-authorization/mango-authorization-starter,mango-platform/mango-authorization/mango-authorization-resource-access-starter,mango-platform/mango-file/mango-file-starter,mango-platform/mango-file-preview/mango-file-preview-starter,mango-platform/mango-notice/mango-notice-starter -am test -Dtest=AppModuleServiceImplIntegrationTest,SubjectAuthorityServiceImplIntegrationTest,MenuServiceImplIntegrationTest,MenuApiCodesBusinessScenarioIntegrationTest,RoleDataScopeServiceImplIntegrationTest,RoleServiceImplIntegrationTest,TenantMenuPackageBindingHandlerIntegrationTest,MenuBaselineTest,AuthMenuResourceHandlerTest,RolePermissionAuthorityContributorTest,ApiResourceAuthorizationManagerTest,FileControllerAccessModeTest,FilePreviewControllerAccessModeTest,NoticeControllerAccessModeTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false`
- `cd mango-ui/packages/common && ../../node_modules/.bin/vite build && node ../../scripts/generate-package-types.mjs`
- `cd mango-ui/packages/rbac && ../../node_modules/.bin/vite build && node ../../scripts/generate-package-types.mjs`
- `node mango-pmo/tools/audit-module-readmes.mjs`
- `node mango-pmo/tools/audit-readme-source-facts.mjs`
- `pnpm -C mango-ui --filter @mango/cli test`
- `pnpm -C mango-ui --filter @mango/cli run check:release-versions`
- `pnpm -C mango-ui -r --filter './packages/*' --filter '!@mango/cli' --if-present run build`
- `pnpm -C mango-ui package-exports:check`
- `scripts/publish-maven-batch.sh --all-non-app --release-version 1.0.13`
- `MANGO_SHARED_PUBLISH_GATES_PASSED=1 pnpm -C mango-ui publish:pkg <package> --release-tag=v2026.07.07-maven-1.0.13-menu-api-codes-release --skip-shared-gates`
- `pnpm -C mango-ui release:verify-npm <package> --version=<version>`
- `gh release view v2026.07.07-maven-1.0.13-menu-api-codes-release`
- `git diff --check`

## v2026.07.07-maven-1.0.12-flyway-bootstrap-release - 2026-07-07

### Fixed

- Fixed workflow clean-database bootstrap when `workflow_business_apply_current_task.claim_status`,
  `candidate_users`, and `candidate_groups` already exist from V1 and V4 is replayed by a new database.
- Fixed `mango-cms` Flyway migration V4 so standalone CMS migration no longer fails when authorization or resource
  tables are not present in the same schema.
- Fixed `mango-cms` demo media seed migrations so they skip file-backed cover/banner/ad delivery rows when
  `file_record` is absent, while still seeding CMS-owned site, category, navigation, and content records.
- Fixed generated full-preset business backend Flyway module declarations by adding the built-in modules included by
  `mango-admin-starter`: `resource`, `home`, `payment`, `link`, `grid-layout`, and `mango-cms`.

### Changed

- Advanced generated business backend Maven lock to `1.0.12` through `@mango/cli@1.0.62`.
- Kept Maven default release scope on non-app platform artifacts; `mango-app/**`, `app-*`, and `*-capability-app`
  deployment jars remain excluded unless explicitly requested with `--include-apps`.

### Upgrade Notes

- Business backends should set `<mango.version>1.0.12</mango.version>` to consume the workflow and CMS Flyway bootstrap
  fixes.
- Generated or upgraded business projects should use `@mango/cli@1.0.62`; its release lock points to Mango Maven
  `1.0.12`.
- Existing generated projects can update only `<mango.version>` to `1.0.12` if their frontend Mango package versions
  already match the current certified frontend batch.
- Existing databases with incomplete Flyway history still require environment-specific Flyway `repair`/baseline
  handling. This release fixes code-level clean bootstrap and cross-module optional table guards; it does not mutate
  historical `flyway_schema_history` state automatically.

### Published Packages

- Maven: Mango backend platform non-app artifacts at `1.0.12` to
  `http://nexus.inner.yunxinbaokeji.com/repository/maven-releases/`.
- npm: `@mango/cli@1.0.62` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- GitHub Release: `v2026.07.07-maven-1.0.12-flyway-bootstrap-release`.

### Verification

- MySQL clean `mango-cms` migrations V1-V10 without authorization, resource, or file tables.
- MySQL `mango-cms` V4 with minimal authorization/resource tables removes and disables only `cms:banner` menu entries.
- MySQL `mango-cms` V8/V9 with minimal `file_record` table seeds cover, `NEWS_HERO`, and about ad delivery rows.
- MySQL clean workflow migrations V1-V4.
- `mvn -f mango/pom.xml -pl mango-platform/mango-cms/mango-cms-core -am -DskipTests compile`
- `mvn -f mango/pom.xml -pl mango-platform/mango-workflow/mango-workflow-core -am -DskipTests compile`
- `pnpm -C mango-ui --filter @mango/cli test`
- `pnpm -C mango-ui --filter @mango/cli run check:release-versions`
- `scripts/publish-maven-batch.sh --all-non-app --release-version 1.0.12`
- `MANGO_SHARED_PUBLISH_GATES_PASSED=1 pnpm -C mango-ui publish:pkg @mango/cli --release-tag=v2026.07.07-maven-1.0.12-flyway-bootstrap-release --skip-shared-gates`
- `pnpm -C mango-ui release:verify-npm @mango/cli --version=1.0.62`
- `gh release view v2026.07.07-maven-1.0.12-flyway-bootstrap-release`
- `git diff --check`

## v2026.07.07-maven-1.0.10-resource-sync-release - 2026-07-07

### Fixed

- Fixed Resource Registry dependent-resource replay for AUTO declarations. When a resource type changes, handlers that
  depend on that type are replayed in the same sync round, so unchanged menu, subject-role, data-scope, org binding, and
  workflow declarations can reconcile against newly created dependencies.
- Fixed business menu role binding when `AUTH_MENU` declarations are processed before newly created `AUTH_ROLE`
  declarations in the same sync round. `AUTH_MENU` now declares its `AUTH_ROLE` dependency and participates in dependent
  replay.
- Fixed same-batch `ORG_UNIT` parent/child ordering for AUTO declarations, so child org units no longer run before a
  parent org unit declared in the same resource batch.

### Changed

- Updated Maven publishing scripts so the standard backend platform release uses a non-app reactor scope and blocks
  `mango-app/**` fat jar deployment artifacts unless `--include-apps` is explicitly requested.
- Documented the backend Maven release gate: use `scripts/publish-maven-batch.sh --all-non-app --release-version <version>`
  for full platform releases instead of raw full-reactor `mvn deploy`.

### Upgrade Notes

- Business backends should set `<mango.version>1.0.10</mango.version>` to consume the Resource Registry replay fix.
- Generated or upgraded business projects should use `@mango/cli@1.0.61`; its release lock points to Mango Maven
  `1.0.10` while keeping the certified frontend package batch from the `1.0.9` release.
- Existing environments that already bootstrapped with the old ordering bug may need one Resource Registry sync replay
  after upgrading if both the role and menu declarations are unchanged.
- Backend release operators must use `scripts/publish-maven-batch.sh --all-non-app --release-version 1.0.10` for the
  standard platform release. Do not run a raw full-reactor `mvn deploy`, because it publishes `app-*` fat jars.
- Bug-fix publishing should follow a release train by default. Only framework defects that block business bootstrap,
  permissions, data correctness, or production operation should be promoted as immediate hotfix releases.

### Published Packages

- Maven: Mango backend platform non-app artifacts at `1.0.10` to
  `http://nexus.inner.yunxinbaokeji.com/repository/maven-releases/`.
- npm: `@mango/cli@1.0.61` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- Docs: Mango Docs snapshot `v2026.07.07-maven-1.0.10-resource-sync-release` for GitHub Pages and Maven docs bundle
  `io.mango:mango-docs-bundle:1.0.10`.
- GitHub Release: `v2026.07.07-maven-1.0.10-resource-sync-release`.

### Verification

- `mvn -pl mango-platform/mango-resource/mango-resource-core,mango-platform/mango-authorization/mango-authorization-starter,mango-platform/mango-org/mango-org-starter -am -Dtest=ResourceRegistrySyncServiceIntegrationTest,AuthMenuResourceHandlerTest,OrgUnitResourceHandlerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `node mango-pmo/tools/audit-module-readmes.mjs`
- `node mango-pmo/tools/audit-readme-source-facts.mjs`
- `bash -n scripts/publish-maven-batch.sh && bash -n scripts/publish-maven-module.sh`
- `scripts/publish-maven-batch.sh --all-non-app --release-version 1.0.10 --skip-verify --dry-run`
- `scripts/publish-maven-batch.sh :mango-platform-app --release-version 1.0.10 --skip-verify --dry-run`
- `scripts/publish-maven-module.sh :mango-platform-app --release-version 1.0.10 --skip-verify --dry-run`
- `mvn -f mango/pom.xml -Drevision=1.0.10-SNAPSHOT -DskipTests -pl <all-non-app-module-paths> validate`
- `node mango-pmo/tools/check-pmo-preflight.mjs`
- `PR_BODY_FILE=/tmp/pr403-body.md node mango-pmo/tools/check-capability-docs.mjs --base origin/main --head HEAD`
- `pnpm -C mango-ui --filter @mango/cli test`
- `pnpm -C mango-ui --filter @mango/cli run check:release-versions`
- `npm --prefix mango-docs run docs:snapshot -- v2026.07.07-maven-1.0.10-resource-sync-release`
- `npm --prefix mango-docs run docs:build`
- `jar cf .runtime/mango-docs-bundle-1.0.10.jar -C .runtime/docs-bundle-1.0.10 .`
- `shasum -a 256 .runtime/mango-docs-bundle-1.0.10.jar`
- `scripts/publish-maven-batch.sh --all-non-app --release-version 1.0.10`
- `mvn deploy:deploy-file -DgroupId=io.mango -DartifactId=mango-docs-bundle -Dversion=1.0.10 -Dpackaging=jar -Dfile=.runtime/mango-docs-bundle-1.0.10.jar -Durl=http://nexus.inner.yunxinbaokeji.com/repository/maven-releases/ -DrepositoryId=maven-releases`
- `MANGO_SHARED_PUBLISH_GATES_PASSED=1 pnpm -C mango-ui publish:pkg @mango/cli --release-tag=v2026.07.07-maven-1.0.10-resource-sync-release --skip-shared-gates`
- `pnpm -C mango-ui release:verify-npm @mango/cli --version=1.0.61`
- `gh release view v2026.07.07-maven-1.0.10-resource-sync-release`
- `git diff --check`

## v2026.07.07-maven-1.0.9-api-contract-release - 2026-07-07

### New

- Exposed workflow business start, progress, task action, and event payload contracts through `mango-workflow-api`, so
  business modules can integrate workflow by depending on API beans instead of `mango-workflow-core` services.
- Added workflow task runtime API adapters for claim, complete, approve, reject, and current task/progress operations.
- Added workflow event API types and payload value objects so business code can subscribe to workflow events through the
  published API boundary.

### Changed

- Aligned platform controller and adapter dependencies across Auth, Authorization, Captcha, CMS, Payment, System, and
  Workflow so business-facing access goes through starter/API contracts rather than direct core service usage.
- Updated workflow frontend API typings and package metadata to match the new workflow business API contract batch.

### Upgrade Notes

- Business backends should set `<mango.version>1.0.9</mango.version>` to consume the API-boundary contract updates.
- Business workflow integrations must inject `WorkflowProcessApi` and `WorkflowTaskRuntimeApi`, and must stop depending
  on `io.mango.workflow.core.service.*` from business code.
- Workflow event consumers should use `WorkflowEventTypes` and `WorkflowEventPayloadVO` from `mango-workflow-api` rather
  than core event classes.
- Business frontends should upgrade Mango npm packages as one batch using the versions listed below. Do not mix
  `@mango/admin-shell`, `@mango/admin`, `@mango/workflow`, or `@mango/workflow-business-example` from this release with
  older admin packages.
- Generated or upgraded business projects should use `@mango/cli@1.0.60`; its release lock points to Mango Maven
  `1.0.9` and this npm package batch.
- Business developers can read version-matched docs from GitHub Pages at
  `/mango/versions/v2026.07.07-maven-1.0.9-api-contract-release/`, or run `mango docs pull` in a generated project to
  download `io.mango:mango-docs-bundle:1.0.9` into `.mango/docs/1.0.9`.

### Published Packages

- Maven: Mango backend platform artifacts at `1.0.9` to `http://nexus.inner.yunxinbaokeji.com/repository/maven-releases/`.
- npm: `@mango/admin-shell@1.0.36`, `@mango/admin@1.0.41`,
  `@mango/workflow-business-example@1.0.22`, `@mango/workflow@1.0.23`, and `@mango/cli@1.0.60` to
  `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- Docs: Mango Docs snapshot `v2026.07.07-maven-1.0.9-api-contract-release` for GitHub Pages and Maven docs bundle
  `io.mango:mango-docs-bundle:1.0.9`.
- GitHub Release: `v2026.07.07-maven-1.0.9-api-contract-release`.

### Verification

- `node mango-pmo/tools/pmo-preflight.mjs --role dev --phase release --task "发布最新版本 mango" --paths "mango,mango-ui,mango-pmo,mango-docs"`
- `pnpm -C mango-ui release:impact --base=v2026.07.04-maven-1.0.8-platform-release --head=HEAD`
- `mvn -f mango/pom.xml -pl mango-platform/mango-auth/mango-auth-starter,mango-platform/mango-authorization/mango-authorization-starter,mango-platform/mango-captcha/mango-captcha-starter,mango-platform/mango-cms/mango-cms-starter,mango-platform/mango-payment/mango-payment-starter,mango-platform/mango-system/mango-system-starter,mango-platform/mango-workflow/mango-workflow-starter -am -Dtest=AuthApiContractTest,AuthorizationApiContractTest,CaptchaControllerIntegrationTest,CmsControllerContractTest,PaymentDomainControllerContractTest,SystemApiContractTest,WorkflowApiControllerContractTest,WorkflowEventPublisherTest,WorkflowProcessServiceImplIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f mango/pom.xml -pl mango-platform/mango-workflow/mango-workflow-api,mango-platform/mango-workflow/mango-workflow-core,mango-platform/mango-workflow/mango-workflow-starter,mango-platform/mango-workflow/mango-workflow-starter-remote -am -DskipTests compile`
- `pnpm -C mango-ui --filter @mango/workflow test`
- `pnpm -C mango-ui --filter @mango/workflow build`
- `pnpm -C mango-ui --filter @mango/workflow-business-example test`
- `pnpm -C mango-ui --filter @mango/workflow-business-example build`
- `pnpm -C mango-ui --filter @mango/admin-shell build`
- `pnpm -C mango-ui --filter @mango/admin build`
- `pnpm -C mango-ui --filter @mango/cli test`
- `pnpm -C mango-ui --filter @mango/cli run check:release-versions`
- `pnpm -C mango-ui package-exports:check`
- `pnpm -C mango-ui admin:styles:check`
- `pnpm -C mango-ui package-consumer:typecheck -- --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `npm --prefix mango-docs run docs:snapshot -- v2026.07.07-maven-1.0.9-api-contract-release`
- `npm --prefix mango-docs run docs:build`
- `jar cf .runtime/mango-docs-bundle-1.0.9.jar -C .runtime/docs-bundle-1.0.9 .`
- `shasum -a 256 .runtime/mango-docs-bundle-1.0.9.jar`
- `PR_BODY_FILE=.runtime/release-pr-body.md node mango-pmo/tools/check-capability-docs.mjs --base v2026.07.04-maven-1.0.8-platform-release --head HEAD`
- `node mango-pmo/tools/check-business-guides.mjs`
- `node mango-pmo/tools/audit-module-readmes.mjs`
- `node mango-pmo/tools/audit-readme-source-facts.mjs`
- `mvn -f mango/pom.xml -Drevision=1.0.9 -DskipTests deploy`
- `mvn deploy:deploy-file -DgroupId=io.mango -DartifactId=mango-docs-bundle -Dversion=1.0.9 -Dpackaging=jar -Dfile=.runtime/mango-docs-bundle-1.0.9.jar -Durl=http://nexus.inner.yunxinbaokeji.com/repository/maven-releases/ -DrepositoryId=maven-releases`
- `MANGO_SHARED_PUBLISH_GATES_PASSED=1 pnpm -C mango-ui publish:pkg <package> --release-tag=v2026.07.07-maven-1.0.9-api-contract-release --skip-shared-gates`
- `pnpm -C mango-ui release:verify-npm <package> --version=<version>`
- `gh release view v2026.07.07-maven-1.0.9-api-contract-release`
- `git diff --check`

## v2026.07.04-maven-1.0.8-platform-release - 2026-07-04

### New

- Added file service PDF merge support for Issue #382. Backends can call `FileApi.mergeToPdf(...)` or
  `POST /file/files/merge-pdf` to combine existing file IDs into a new PDF file record in entry order.
- Added an ImageIO/PDFBox PNG/JPEG to PDF converter in `mango-infra-fileproc`, so mobile photo upload scenarios can
  generate mergeable PDF pages without depending on Aspose.Imaging reflection behavior on Java 21.
- Added structured site-message actions for Notice. Senders can attach domain action targets, the message center can
  execute or complete actions, and action requests are tracked through the Notice API and persistence model.
- Added homepage management follow-up APIs and admin UI support for batch selection, preview, edit, delete, user
  selector filtering, and editing published pages without changing the currently effective version until republished.

### Changed

- Simplified file upload/query `FileRecordVO` JSON responses. Business callers now use `previewUrl` for original file
  preview and `downloadUrl` for download; storage object fields, `url`, and storage public-access details are hidden
  from these record responses.
- Simplified frontend `@mango/file` `FileRecord` typing and file list UI. Storage type, bucket/object path, `url`, and
  `direct*` access fields are no longer part of the public file record contract.
- Updated Notice frontend message detail and message-center rendering to use structured action metadata instead of
  free-form action assumptions.
- Updated home management screens to use page-level editing and list operations instead of dialog-only editing for
  complex homepage content.

### Upgrade Notes

- Business backends should set `<mango.version>1.0.8</mango.version>` to consume the file PDF merge, Notice action, and
  homepage management follow-up APIs.
- Business frontends should upgrade Mango npm packages as one batch using the versions listed below. Do not mix
  `@mango/admin-shell`, `@mango/home`, `@mango/notice`, or `@mango/file` from this release with older admin packages.
- Generated or upgraded business projects should use `@mango/cli@1.0.59`; its release lock points to Mango Maven
  `1.0.8` and this npm package batch.
- The first PDF merge version only accepts `targetFormat=PDF`; Word output is intentionally out of scope.
- PDF merge source files must be current-tenant visible and completed. Supported source formats are PDF, JPG/JPEG, PNG,
  TIFF, DOC, and DOCX.
- `mango.fileproc.convert.image-to-pdf-enabled=true` is enabled by default. Set it to `false` only when a project
  intentionally wants image-to-PDF conversion to fall through to another custom provider.
- Business code should persist only file IDs. Do not depend on `url`, `directPreviewUrl`, `directDownloadUrl`, bucket,
  or object name fields from upload/page/detail responses. Office/document preview components should load preview
  metadata by file ID and use `documentPreviewUrl` instead of treating `FileRecordVO.previewUrl` as a document-preview
  service URL.
- Frontend callers using `FileRecord` should read `previewUrl` for preview actions and `downloadUrl` for download
  actions. Storage-layer diagnostics belong to storage configuration/admin APIs, not the business file record.
- Notice senders that need message-center actions should populate structured `actions`, `subjects`, and `targets` on
  `SendNoticeCommand` instead of encoding domain operations in display text.
- Homepage edits on published pages are draft changes until the page is published again. The currently effective page
  remains unchanged until republish.
- Business developers can read version-matched docs from GitHub Pages at
  `/mango/versions/v2026.07.04-maven-1.0.8-platform-release/`, or run `mango docs pull` in a generated project to
  download `io.mango:mango-docs-bundle:1.0.8` into `.mango/docs/1.0.8`.

### Published Packages

- Maven: Mango backend platform artifacts at `1.0.8` to `http://nexus.inner.yunxinbaokeji.com/repository/maven-releases/`.
- npm: `@mango/admin-pages@1.0.16`, `@mango/admin-shell@1.0.35`, `@mango/admin@1.0.40`,
  `@mango/calendar@1.0.17`, `@mango/cms@1.0.6`, `@mango/file@1.0.17`, `@mango/home@1.0.2`,
  `@mango/job@1.0.9`, `@mango/link@1.0.3`, `@mango/notice@1.0.18`, `@mango/numgen@1.0.17`,
  `@mango/payment@1.0.8`, `@mango/system@1.0.15`, `@mango/template@1.0.17`,
  `@mango/workflow-business-example@1.0.21`, `@mango/workflow@1.0.22`, and `@mango/cli@1.0.59` to
  `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- Docs: Mango Docs snapshot `v2026.07.04-maven-1.0.8-platform-release` for GitHub Pages and Maven docs bundle
  `io.mango:mango-docs-bundle:1.0.8`.
- GitHub Release: `v2026.07.04-maven-1.0.8-platform-release`.

### Verification

- `node mango-pmo/tools/pmo-preflight.mjs --role dev --phase release --task "发布当前 main 中已合并的短信/首页/通知变更" --paths "mango,mango-ui,mango-business-starter,mango-docs"`
- `pnpm -C mango-ui release:impact --base=v2026.07.03-maven-1.0.7-platform-release --head=HEAD`
- `mvn -f mango/pom.xml -pl mango-infra/mango-infra-fileproc/mango-infra-fileproc-core,mango-infra/mango-infra-fileproc/mango-infra-fileproc-starter,mango-platform/mango-file/mango-file-core,mango-platform/mango-file/mango-file-starter -am -Dtest=ImageToPdfConvertProviderTest,ConvertAutoConfigurationTest,FileServiceMergeToPdfTest,FileControllerMergeToPdfTest,FileControllerRecordSerializationTest,FileControllerDownloadResponseTest,FileControllerAccessModeTest,FileAccessUrlAssemblerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f mango/pom.xml -pl mango-platform/mango-file/mango-file-api,mango-platform/mango-file/mango-file-core,mango-platform/mango-file/mango-file-starter -am -DskipTests checkstyle:check`
- `mvn -f mango/pom.xml -pl mango-platform/mango-home/mango-home-core,mango-platform/mango-home/mango-home-starter,mango-platform/mango-notice/mango-notice-core,mango-platform/mango-notice/mango-notice-starter,mango-platform/mango-notice/mango-notice-channel-site,mango-platform/mango-notice/mango-notice-channel-sms,mango-platform/mango-notice/mango-notice-channel-email,mango-platform/mango-notice/mango-notice-channel-dingtalk,mango-platform/mango-notice/mango-notice-channel-wechat-official,mango-platform/mango-notice/mango-notice-channel-wecom -am -Dtest=HomePageServiceTest,NoticeOutboxDispatcherTest,NoticeChannelResourceHandlerIntegrationTest,NoticeMessageTemplateResourceHandlerIntegrationTest,NoticeAnnouncementServiceIntegrationTest,NoticeServiceIntegrationTest,NoticeAutoConfigurationTest,NoticeControllerAccessModeTest,NoticeControllerTest,SiteNoticeChannelSenderTest,SmsNoticeChannelSenderTest,EmailNoticeChannelSenderTest,DingtalkNoticeChannelSenderTest,WechatOfficialNoticeChannelSenderTest,WecomNoticeChannelSenderTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `pnpm -C mango-ui -F @mango/file test`
- `pnpm -C mango-ui -F @mango/file build`
- `pnpm -C mango-ui -F @mango/notice test`
- `pnpm -C mango-ui -F @mango/admin-shell build`
- `pnpm -C mango-ui -F @mango/admin build`
- `pnpm -C mango-ui --filter @mango/cli run check:release-versions`
- `pnpm -C mango-ui release:verify-npm <package> --version=<version>`
- `npm --prefix mango-docs run docs:snapshot -- v2026.07.04-maven-1.0.8-platform-release`
- `npm --prefix mango-docs run docs:build`
- `mvn -f mango/pom.xml -Drevision=1.0.8 -DskipTests deploy`
- `mvn deploy:deploy-file -DgroupId=io.mango -DartifactId=mango-docs-bundle -Dversion=1.0.8 -Dpackaging=jar -Dfile=.runtime/mango-docs-bundle-1.0.8.jar -Durl=http://nexus.inner.yunxinbaokeji.com/repository/maven-releases/ -DrepositoryId=maven-releases`
- `gh release view v2026.07.04-maven-1.0.8-platform-release`
- `git diff --check`

## v2026.07.03-maven-1.0.7-platform-release - 2026-07-03

### New

- Added Resource Registry resource-type dependency ordering for Issue #354. Resource handlers can now declare
  `dependsOnResourceTypes()`, and active resource sync batches are topologically ordered before target handlers run.
- Added the `mango-home` backend capability and `@mango/home` frontend package for user home pages, homepage templates,
  template authorization, final user homepage views, and homepage management pages.
- Added home management E2E coverage and evidence for template list, user view, and end-to-end homepage management flows.
- Added Aliyun SMS and Tencent Cloud SMS gateways for `mango-notice-channel-sms`, including provider-specific channel
  configuration fields, SMS template Code support, variable mapping, and sender tests.
- Added notice SMS setup documentation for backend and frontend business configuration pages.

### Fixed

- Fixed clean database Resource Registry bootstrap ordering for cross-type declarations such as `IDENTITY_USER` before
  `ORG_MEMBER_BINDING`, `AUTH_ROLE` before `AUTH_SUBJECT_ROLE`, and workflow categories/nodes before
  `WORKFLOW_DEFINITION`. Cyclic type dependencies now fail before any target handler is called.
- Isolated KV outbox worker claims by topic so notice, realtime, and domain-event workers no longer compete for each
  other's messages. Historical topic-less `notice.send` messages are inferred as notice messages.
- Added Flyway module diagnostics for undeclared classpath migration modules so startup fails with actionable module
  guidance instead of silently skipping migrations.
- Relaxed Mango frontend peer dependency ranges within the currently certified major versions and documented the
  unsupported cross-major ranges.
- Scoped the Maven `module-menu` check to changed files when `mango.check.changedOnly` is enabled, reporting historical
  out-of-scope findings as excluded issues.
- Preserved same-origin admin deep-link targets through login by carrying the `redirect` query from route guards to the
  auth login page and rejecting external redirect values.

### Upgrade Notes

- Business projects can keep Resource Registry declarations split across files and modules; file scan order is no longer
  the ordering contract for resource types that declare handler dependencies.
- Business backends should set `<mango.version>1.0.7</mango.version>` to consume the home, notice SMS, outbox topic,
  Resource Registry ordering, Flyway diagnostics, and Maven checker fixes.
- Business frontends should upgrade Mango npm packages as a single batch using the versions listed below. Do not mix the
  new `@mango/home` package and admin shell homepage pages with older `@mango/admin` or `@mango/admin-shell` versions.
- Generated or upgraded business projects should use `@mango/cli@1.0.58`; its release lock points to Mango Maven
  `1.0.7` and this npm package batch.
- To use homepage management, enable `mango-home-starter`, run the home Flyway migrations, synchronize the
  `home-common-menu.json` resources, and make sure admin frontends register the `@mango/home` pages through the current
  admin package batch.
- To use SMS delivery, include `mango-notice-channel-sms`, configure channel provider credentials in Notice channel
  management, and set the SMS template Code plus variable mapping in Notice business configuration.
- Business developers can read version-matched docs from GitHub Pages at
  `/mango/versions/v2026.07.03-maven-1.0.7-platform-release/`, or run `mango docs pull` in a generated project to
  download `io.mango:mango-docs-bundle:1.0.7` into `.mango/docs/1.0.7`.

### Published Packages

- Maven: Mango backend platform artifacts at `1.0.7` to `http://nexus.inner.yunxinbaokeji.com/repository/maven-releases/`.
- npm: `@mango/admin-pages@1.0.15`, `@mango/admin-shell@1.0.33`, `@mango/admin@1.0.38`,
  `@mango/app-runtime@1.0.3`, `@mango/auth@1.0.11`, `@mango/calendar@1.0.16`, `@mango/cms@1.0.5`,
  `@mango/common@1.0.12`, `@mango/file@1.0.16`, `@mango/grid-layout@1.0.5`,
  `@mango/grid-widgets@1.0.10`, `@mango/home@1.0.0`, `@mango/job@1.0.8`, `@mango/link-page@1.0.2`,
  `@mango/link@1.0.2`, `@mango/notice@1.0.17`, `@mango/numgen@1.0.16`, `@mango/payment@1.0.7`,
  `@mango/rbac@1.0.10`, `@mango/site-shell@1.0.1`, `@mango/system@1.0.14`, `@mango/template@1.0.16`,
  `@mango/workflow-business-example@1.0.20`, `@mango/workflow@1.0.21`, and `@mango/cli@1.0.58` to
  `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- Docs: Mango Docs snapshot `v2026.07.03-maven-1.0.7-platform-release` for GitHub Pages and Maven docs bundle
  `io.mango:mango-docs-bundle:1.0.7`.
- GitHub Release: `v2026.07.03-maven-1.0.7-platform-release`.

### Verification

- `node mango-pmo/tools/pmo-preflight.mjs --role dev --phase release --task "发布最新版本" --paths "CHANGELOG.md,mango/pom.xml,mango-ui/packages/mango-cli,mango-ui/packages/*/package.json,mango-docs"`
- `mvn -f mango/pom.xml -pl mango-platform/mango-resource/mango-resource-core -am -Dtest=ResourceRegistrySyncServiceIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f mango/pom.xml -pl mango-platform/mango-home/mango-home-core -am test`
- `mvn -f mango/pom.xml -pl mango-platform/mango-notice/mango-notice-channel-sms -am -DskipTests=false test`
- `mvn -f mango/pom.xml -pl mango-tools/mango-maven-plugin -am test`
- `mvn -f mango/pom.xml -pl mango-platform/mango-identity/mango-identity-starter,mango-platform/mango-authorization/mango-authorization-starter,mango-platform/mango-workflow/mango-workflow-core -am -DskipTests compile`
- `node mango-pmo/tools/audit-backend-test-mocks.mjs --report-only --changed-only --base origin/main`
- `node mango-pmo/tools/audit-module-readmes.mjs`
- `node mango-pmo/tools/audit-readme-source-facts.mjs`
- `node mango-pmo/tools/check-business-guides.mjs`
- `PR_BODY_FILE=.runtime/release-pr-body.md node mango-pmo/tools/check-capability-docs.mjs --base v2026.07.02-maven-1.0.6-home-widgets-cli-release --head HEAD`
- `pnpm -C mango-ui release:impact --base=v2026.07.02-maven-1.0.6-home-widgets-cli-release --head=HEAD`
- `pnpm -C mango-ui --filter @mango/cli test`
- `pnpm -C mango-ui --filter @mango/cli run check:release-versions`
- `pnpm -C mango-ui admin:styles:check`
- `pnpm -C mango-ui admin:module-styles:check`
- `pnpm -C mango-ui package-consumer:typecheck -- --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `npm --prefix mango-docs run docs:snapshot -- v2026.07.03-maven-1.0.7-platform-release`
- `npm --prefix mango-docs run docs:build`
- `mvn -f mango/pom.xml -Drevision=1.0.7 -DskipTests deploy`
- `mvn deploy:deploy-file -DgroupId=io.mango -DartifactId=mango-docs-bundle -Dversion=1.0.7 -Dpackaging=jar -Dfile=.runtime/mango-docs-bundle-1.0.7.jar -Durl=http://nexus.inner.yunxinbaokeji.com/repository/maven-releases/ -DrepositoryId=maven-releases`
- `mvn -U org.apache.maven.plugins:maven-dependency-plugin:3.8.1:get -Dmaven.repo.local=.runtime/maven-publish-verify-1.0.7 -Dartifact=io.mango:mango-admin-starter:1.0.7 -Dtransitive=false`
- `mvn -U org.apache.maven.plugins:maven-dependency-plugin:3.8.1:get -Dmaven.repo.local=.runtime/maven-publish-verify-1.0.7 -Dartifact=io.mango.platform.home:mango-home-starter:1.0.7 -Dtransitive=false`
- `mvn -U org.apache.maven.plugins:maven-dependency-plugin:3.8.1:get -Dmaven.repo.local=.runtime/maven-publish-verify-1.0.7 -Dartifact=io.mango:mango-docs-bundle:1.0.7 -Dtransitive=false`
- `MANGO_SHARED_PUBLISH_GATES_PASSED=1 pnpm -C mango-ui publish:pkg <package> --release-tag=v2026.07.03-maven-1.0.7-platform-release --skip-shared-gates`
- `pnpm -C mango-ui release:verify-npm <package> --version=<version>`
- `gh release view v2026.07.03-maven-1.0.7-platform-release`
- `git diff --check`

## v2026.07.02-maven-1.0.6-home-widgets-cli-release - 2026-07-02

### New

- Modularized home workbench widgets so concrete widgets now live in their owning business UI packages: `@mango/link`, `@mango/system`, `@mango/calendar`, `@mango/notice`, and `@mango/workflow`.
- Updated the admin home workbench UI by removing the welcome header, floating the round layout action at the bottom-right corner, and refining the widget library panel.
- Added the Link navigation home widget under `@mango/link`, including its package-owned styles and admin registrar integration.

### Changed

- Reduced `@mango/grid-widgets` to the shared widget registry/runtime/types boundary and moved business widget styles out to their owning packages.
- Updated the CLI generated project release lock to Mango backend `1.0.6` and the home-widget package batch.
- Removed the deleted `@mango/link-panel` compatibility package from generated project release locks.

### Fixed

- Added `mango-notice-starter` to custom generated backend baseline dependencies so `custom --modules none` projects that include `mango-auth-starter` can provide the required `NoticeApi` bean during Spring Boot startup.
- Added explicit `MangoAdminFeatureRegistrar[]` types to generated admin frontend feature registrar arrays so custom projects pass strict `vue-tsc` when no business modules have been added yet.

### Upgrade Notes

- Business backends should set `<mango.version>1.0.6</mango.version>` to consume this backend and generated-project baseline batch.
- Business frontends should stop depending on `@mango/link-panel`; use `@mango/link` for Link admin pages and the Link navigation home widget.
- Custom admin integrations must include the business UI packages that own the widgets they want to expose. `@mango/grid-widgets` no longer provides concrete business widget implementations.
- Admin package consumers should keep package styles synchronized through the generated admin style aggregation; do not rely on `@mango/grid-widgets/style.css` to carry Link, System, Calendar, Notice, or Workflow widget styles.
- Existing generated projects should upgrade to `@mango/cli@1.0.57` and run `mango pmo sync --project-dir . --sync-shell` or `mango pmo upgrade --project-dir . --sync-shell` when they need the refreshed PMO shell baseline.
- Business developers can read version-matched docs from GitHub Pages at `/mango/versions/v2026.07.02-maven-1.0.6-home-widgets-cli-release/`, or run `mango docs pull` in a generated project to download `io.mango:mango-docs-bundle:1.0.6` into `.mango/docs/1.0.6`.

### Published Packages

- Maven: Mango backend platform artifacts at `1.0.6` to `http://nexus.inner.yunxinbaokeji.com/repository/maven-releases/`.
- npm: `@mango/admin-shell@1.0.32`, `@mango/admin@1.0.37`, `@mango/calendar@1.0.15`, `@mango/grid-layout@1.0.4`, `@mango/grid-widgets@1.0.9`, `@mango/link-openapi@1.0.1`, `@mango/link-page@1.0.1`, `@mango/link@1.0.1`, `@mango/cli@1.0.57`, `@mango/notice@1.0.16`, `@mango/system@1.0.13`, `@mango/workflow@1.0.20`, `@mango/admin-pages@1.0.14`, `@mango/cms@1.0.4`, `@mango/file@1.0.15`, `@mango/job@1.0.7`, `@mango/numgen@1.0.15`, `@mango/payment@1.0.6`, `@mango/template@1.0.15`, and `@mango/workflow-business-example@1.0.19` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- Docs: Mango Docs snapshot `v2026.07.02-maven-1.0.6-home-widgets-cli-release` for GitHub Pages and Maven docs bundle `io.mango:mango-docs-bundle:1.0.6`.
- GitHub Release: `v2026.07.02-maven-1.0.6-home-widgets-cli-release`.

### Verification

- `node mango-pmo/tools/pmo-preflight.mjs --role dev --phase release --task "发布 PR 365 和 PR 367 合并后的最新版本" --paths "CHANGELOG.md,mango/pom.xml,mango-ui/packages/mango-cli/package.json,mango-docs,mango-ui/packages/*/package.json"`
- `node mango-pmo/tools/check-business-guides.mjs`
- `node mango-pmo/tools/audit-module-readmes.mjs`
- `node mango-pmo/tools/audit-readme-source-facts.mjs`
- `PR_BODY_FILE=.runtime/release-pr-body.md node mango-pmo/tools/check-capability-docs.mjs --base v2026.07.01-maven-1.0.5-data-governance-release --head HEAD`
- `pnpm -C mango-ui release:impact --base=v2026.07.01-maven-1.0.5-data-governance-release --head=HEAD`
- `pnpm -C mango-ui --filter @mango/cli run check:release-versions`
- `pnpm -C mango-ui --filter @mango/cli test`
- `pnpm -C mango-ui admin:styles:check`
- `pnpm -C mango-ui admin:module-styles:check`
- `pnpm -C mango-ui package-consumer:typecheck -- --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `npm --prefix mango-docs run docs:snapshot -- v2026.07.02-maven-1.0.6-home-widgets-cli-release`
- `npm --prefix mango-docs run docs:build`
- `mvn -f mango/pom.xml -Drevision=1.0.6 -DskipTests deploy`
- `mvn -U org.apache.maven.plugins:maven-dependency-plugin:3.8.1:get -Dmaven.repo.local=.runtime/maven-publish-verify-1.0.6 -Dartifact=io.mango:mango-admin-starter:1.0.6 -Dtransitive=false`
- `mvn -U org.apache.maven.plugins:maven-dependency-plugin:3.8.1:get -Dmaven.repo.local=.runtime/maven-publish-verify-1.0.6 -Dartifact=io.mango.platform.link:mango-link-starter:1.0.6 -Dtransitive=false`
- `mvn -U org.apache.maven.plugins:maven-dependency-plugin:3.8.1:get -Dmaven.repo.local=.runtime/maven-publish-verify-1.0.6 -Dartifact=io.mango:mango-docs-bundle:1.0.6 -Dtransitive=false`
- `MANGO_SHARED_PUBLISH_GATES_PASSED=1 pnpm -C mango-ui publish:pkg <package> --release-tag=v2026.07.02-maven-1.0.6-home-widgets-cli-release --skip-shared-gates`
- `pnpm -C mango-ui release:verify-npm <package> --version=<version>`
- `gh release view v2026.07.02-maven-1.0.6-home-widgets-cli-release`
- `git diff --check`

## v2026.07.01-maven-1.0.5-data-governance-release - 2026-07-01

### New

- Published Issue #184 data initialization governance. Resource Registry demo declarations are now isolated behind explicit demo resource locations instead of loading through the default runtime path.
- Added `INIT_ONLY` Resource Registry sync mode so built-in initialization data can create missing records without overwriting operator-maintained records.
- Added external Flyway module locations for persistence migrations, allowing controlled schema baseline and comparison packs outside the default classpath migration path.
- Published PMO governance that forbids default runtime seeds for sample/demo/test data and documents where explicit release materials should live.
- Published a versioned Mango Docs snapshot for this release, including the Resource Registry and persistence README updates plus the shortened docs version labels.

### Fixed

- Fixed `@mango/link-panel` so its package entry imports the package-owned `style.css` export instead of relying on the `@mango/link-page` style entry.

### Upgrade Notes

- Business backends should set `<mango.version>1.0.5</mango.version>` to consume the Resource Registry and persistence initialization governance changes.
- Demo/sample Resource Registry declarations are no longer part of the default production declaration path. Enable demo material only through explicit demo resource locations or test resources.
- Use `ResourceSyncMode.INIT_ONLY` for idempotent initial data that must not overwrite operator changes. Use the existing sync modes only when the target data is intentionally owned by Mango declarations.
- Persistence Flyway modules can now declare external `file:` or `classpath:` locations for controlled release or comparison packs; default runtime migrations should remain module-owned and production-safe.
- Existing business projects should upgrade to `@mango/cli@1.0.56` and run `mango pmo sync --project-dir . --sync-shell` or `mango pmo upgrade --project-dir . --sync-shell` to receive `@mango/pmo@1.0.6`.
- Frontend projects that consume the compatibility URL navigation package should upgrade `@mango/link-panel` to `1.0.1`.

### Published Packages

- Maven: full Mango backend platform reactor at `1.0.5` to `http://nexus.inner.yunxinbaokeji.com/repository/maven-releases/`.
- npm: `@mango/pmo@1.0.6`, `@mango/link-panel@1.0.1`, and `@mango/cli@1.0.56` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- Docs: Mango Docs snapshot `v2026.07.01-maven-1.0.5-data-governance-release` for GitHub Pages.
- GitHub Release: `v2026.07.01-maven-1.0.5-data-governance-release`.

### Verification

- `node mango-pmo/tools/pmo-preflight.mjs --role dev --phase release --task "发布 PR 363 合并后的最新版本" --paths "CHANGELOG.md,mango/pom.xml,mango-ui/packages/mango-cli/package.json,mango-docs,mango-business-starter"`
- `node mango-pmo/tools/check-business-guides.mjs`
- `node mango-pmo/tools/audit-module-readmes.mjs`
- `node mango-pmo/tools/audit-readme-source-facts.mjs`
- `PR_BODY_FILE=.runtime/release-pr-body.md node mango-pmo/tools/check-capability-docs.mjs --base v2026.07.01-maven-1.0.4-link-cli-docs-release --head HEAD`
- `mvn -f mango/pom.xml -pl mango-platform/mango-resource/mango-resource-api,mango-platform/mango-resource/mango-resource-core -am test`
- `mvn -f mango/pom.xml -pl mango-infra/mango-infra-persistence/mango-infra-persistence-starter -am test`
- `pnpm -C mango-ui release:impact --base=v2026.07.01-maven-1.0.4-link-cli-docs-release --head=HEAD`
- `pnpm -C mango-ui --filter @mango/pmo build`
- `pnpm -C mango-ui --filter @mango/pmo check`
- `pnpm -C mango-ui --filter @mango/cli run check:release-versions`
- `pnpm -C mango-ui admin:styles:check`
- `pnpm -C mango-ui admin:module-styles:check`
- `pnpm -C mango-ui package-consumer:typecheck -- --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `npm --prefix mango-docs run docs:snapshot -- v2026.07.01-maven-1.0.5-data-governance-release`
- `npm --prefix mango-docs run docs:build`
- `mvn -f mango/pom.xml -Drevision=1.0.5 -DskipTests deploy`
- `mvn -U org.apache.maven.plugins:maven-dependency-plugin:3.8.1:get -Dmaven.repo.local=.runtime/maven-publish-verify-1.0.5 -Dartifact=io.mango:mango-admin-starter:1.0.5 -Dtransitive=false`
- `mvn -U org.apache.maven.plugins:maven-dependency-plugin:3.8.1:get -Dmaven.repo.local=.runtime/maven-publish-verify-1.0.5 -Dartifact=io.mango.platform.resource:mango-resource-starter:1.0.5 -Dtransitive=false`
- `mvn -U org.apache.maven.plugins:maven-dependency-plugin:3.8.1:get -Dmaven.repo.local=.runtime/maven-publish-verify-1.0.5 -Dartifact=io.mango.infra:mango-infra-persistence-starter:1.0.5 -Dtransitive=false`
- `MANGO_SHARED_PUBLISH_GATES_PASSED=1 pnpm -C mango-ui publish:pkg <package> --release-tag=v2026.07.01-maven-1.0.5-data-governance-release --skip-shared-gates`
- `pnpm -C mango-ui release:verify-npm <package> --version=<version>`
- `gh release view v2026.07.01-maven-1.0.5-data-governance-release`
- `git diff --check`

## v2026.07.01-maven-1.0.4-link-cli-docs-release - 2026-07-01

### New

- Published the `mango-link` URL navigation capability. The backend adds link category, company link, personal link, favorites, jump tracking, menu resources, and Flyway migrations under `mango-link`.
- Published the Link frontend package set: `@mango/link@1.0.0`, `@mango/link-openapi@1.0.0`, `@mango/link-page@1.0.0`, and `@mango/link-panel@1.0.0`.
- Added Link module integration to the full admin package and CLI release lock through `@mango/admin@1.0.36`.
- Published `@mango/cli@1.0.55` with `mango dev restart`, which runs the existing stop and start flow for a selected app or group.
- Added a versioned Mango Docs snapshot for this release tag so business developers can read docs that match the Maven and npm artifacts.

### Upgrade Notes

- Business backends should set `<mango.version>1.0.4</mango.version>` to consume the new `mango-link` backend artifacts.
- Monolith deployments that need URL navigation should add `mango-link-starter`; remote or split deployments should use the matching `mango-link-starter-remote` dependency pattern.
- Business admin frontends should consume the npm package versions listed in this release as a batch. Do not mix `@mango/admin@1.0.36` with older CLI release locks when enabling Link pages.
- For standalone URL navigation pages, install `@mango/link-page@1.0.0` and import `@mango/link-page/style.css`; existing consumers of the compatibility package can use `@mango/link-panel@1.0.0`.
- Developers should install `@mango/cli@1.0.55` from the company npm group registry before relying on `mango dev restart`.
- Do not consume Mango Maven `1.0.3`; that attempted release was superseded before completion after verification found an invalid `mango-file-preview-engine` jar.

### Published Packages

- Maven: full Mango backend platform reactor at `1.0.4` to `http://nexus.inner.yunxinbaokeji.com/repository/maven-releases/`.
- npm: `@mango/link-openapi@1.0.0`, `@mango/link-page@1.0.0`, `@mango/link-panel@1.0.0`, `@mango/link@1.0.0`, `@mango/admin@1.0.36`, and `@mango/cli@1.0.55` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- Docs: Mango Docs snapshot `v2026.07.01-maven-1.0.4-link-cli-docs-release` for GitHub Pages.
- GitHub Release: `v2026.07.01-maven-1.0.4-link-cli-docs-release`.

### Verification

- `pnpm -C mango-ui release:impact --base=v2026.07.01-maven-1.0.2-workflow-runtime-release --head=HEAD`
- `pnpm --filter @mango/cli run check:release-versions`
- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`
- `npm --prefix mango-docs run docs:snapshot -- v2026.07.01-maven-1.0.4-link-cli-docs-release`
- `npm --prefix mango-docs run docs:build`
- `mvn -f mango/pom.xml -Drevision=1.0.4 -DskipTests deploy`
- `mvn -U org.apache.maven.plugins:maven-dependency-plugin:3.8.1:get -Dmaven.repo.local=.runtime/maven-publish-verify-1.0.4 -Dartifact=io.mango:mango-admin-starter:1.0.4 -Dtransitive=false`
- `mvn -U org.apache.maven.plugins:maven-dependency-plugin:3.8.1:get -Dmaven.repo.local=.runtime/maven-publish-verify-1.0.4 -Dartifact=io.mango.platform.link:mango-link-starter:1.0.4 -Dtransitive=false`
- `MANGO_SHARED_PUBLISH_GATES_PASSED=1 pnpm -C mango-ui publish:pkg <package> --release-tag=v2026.07.01-maven-1.0.4-link-cli-docs-release --skip-shared-gates`
- `pnpm -C mango-ui release:verify-npm <package> --version=<version>`
- `gh release view v2026.07.01-maven-1.0.4-link-cli-docs-release`
- `git diff --check`

## v2026.07.01-maven-1.0.2-workflow-runtime-release - 2026-07-01

### New

- Added `WORKFLOW_DEFINITION` Resource Registry support for Issue #344. Workflow declarations can now be consumed by `mango-workflow` through a target `ResourceHandler`, create or update definitions by `tenantId + definitionKey`, load inline or classpath JSON, and publish deployable Flowable process definitions.
- Added `WorkflowTaskRuntimeApi` for Issue #345 so business modules can call workflow task runtime operations through `mango-workflow-api` instead of depending on `mango-workflow-core`.
- Hardened Maven release tooling so backend publishes must pass an explicit `--release-version`/`--revision`; implicit `1.0.0-SNAPSHOT` publishes are blocked, and intentional snapshots require `--allow-snapshot`.

### Fixed

- Updated workflow definition ensure-publish behavior so changed built-in declarations update the existing ensured definition and create a new published version instead of being skipped when an older published definition exists.

### Upgrade Notes

- Business backends should set `<mango.version>1.0.2</mango.version>` when consuming the new workflow runtime API or workflow definition Resource Registry handler.
- Backend consumers that bootstrap workflow definitions through Resource Registry should refresh Mango workflow dependencies and declare `WORKFLOW_DEFINITION` resources under `META-INF/mango/resources`.
- Business modules should depend on `mango-workflow-api` for task runtime operations. Only the host application that provides the workflow runtime should include `mango-workflow-starter`.
- No database migration or frontend package change is required by this PR.

### Published Packages

- Maven: full Mango backend platform reactor at `1.0.2` to `http://nexus.inner.yunxinbaokeji.com/repository/maven-releases/`.
- GitHub Release: `v2026.07.01-maven-1.0.2-workflow-runtime-release`.

### Verification

- `mvn -f mango/pom.xml -pl mango-platform/mango-workflow/mango-workflow-core,mango-platform/mango-workflow/mango-workflow-starter -am test -DskipTests=false`
- `mvn -f mango/pom.xml -pl mango-platform/mango-workflow/mango-workflow-core,mango-platform/mango-workflow/mango-workflow-starter -am checkstyle:check`
- `mvn -f mango/pom.xml -pl mango-platform/mango-workflow/mango-workflow-core,mango-platform/mango-workflow/mango-workflow-starter -am pmd:check`
- `mvn -f mango/pom.xml -pl mango-platform/mango-workflow/mango-workflow-core,mango-platform/mango-workflow/mango-workflow-starter -am mango:check -Drule=api-contract` failed on existing repository-wide API contract debt; the current-task `WorkflowTaskController -> WorkflowTaskRuntimeApi` violation was removed.
- `mvn -f mango/pom.xml -pl mango-platform/mango-workflow/mango-workflow-core,mango-platform/mango-workflow/mango-workflow-starter -am mango:check -Drule=all` failed on existing repository-wide rule debt unrelated to the new workflow handler/API files.
- Real E2E smoke: monolith backend on isolated MySQL database, mango-admin frontend, workflow definition page, workflow todo page, `/resource/handler-specs`, and workflow definition/task APIs returned `200` with no browser console errors.
- `pnpm --filter @mango/cli run check:release-versions`
- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`
- `node mango-pmo/tools/audit-module-readmes.mjs`
- `node mango-pmo/tools/audit-readme-source-facts.mjs`
- `node mango-pmo/tools/check-business-guides.mjs`
- `PR_BODY_FILE=/tmp/release-maven-1.0.2-pr-body.md node mango-pmo/tools/check-capability-docs.mjs --base origin/main --head HEAD`
- `mvn -f mango/pom.xml -Drevision=1.0.2 -DskipTests deploy`
- `mvn -q dependency:get -Dartifact=io.mango:mango-admin-starter:1.0.2 -Dtransitive=false -DremoteRepositories=mango-public::default::http://nexus.inner.yunxinbaokeji.com/repository/maven-public/`
- `git diff --check`

## v2026.06.30-maven-1.0.1-admin-branding-cli-release - 2026-06-30

### New

- Started fixed backend Maven jar version management for business projects. Generated projects now lock Mango backend dependencies through `<mango.version>1.0.1</mango.version>` and `dependencyManagement` instead of defaulting to `1.0.0-SNAPSHOT`.
- Added admin branding configuration in the System module. The backend exposes branding configuration contracts and controller support, while the admin shell, login page, footer, and logo rendering can consume file-center-backed branding images and text.
- Polished workspace grid widgets and grid layout styling in the admin home page batch.

### Fixed

- Updated the File upload component image回显 behavior used by admin branding so file-center IDs can be resolved back to previewable image data instead of relying on persisted access URLs.
- Split file preview and download URL handling so preview panels no longer fall back to download endpoints for inline rendering.

### Published Packages

- Maven: full Mango backend platform reactor at `1.0.1` to `http://nexus.inner.yunxinbaokeji.com/repository/maven-releases/`.
- npm: `@mango/admin@1.0.35`, `@mango/admin-pages@1.0.13`, `@mango/admin-shell@1.0.31`, `@mango/auth@1.0.10`, `@mango/calendar@1.0.14`, `@mango/cms@1.0.3`, `@mango/file@1.0.14`, `@mango/grid-layout@1.0.3`, `@mango/grid-widgets@1.0.8`, `@mango/job@1.0.6`, `@mango/notice@1.0.15`, `@mango/numgen@1.0.14`, `@mango/payment@1.0.5`, `@mango/system@1.0.12`, `@mango/template@1.0.14`, `@mango/workflow@1.0.19`, `@mango/workflow-business-example@1.0.18`, and `@mango/cli@1.0.54` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- GitHub Release: `v2026.06.30-maven-1.0.1-admin-branding-cli-release`.

### Upgrade Notes

- Business backends should set `<mango.version>1.0.1</mango.version>` in the generated backend parent POM, or regenerate/upgrade with `@mango/cli@1.0.54` so the CLI writes the same Maven lock.
- Business frontend projects should consume the npm versions listed above as a batch. Do not mix the new admin shell/system/file branding packages with older `@mango/admin` or CLI release locks.
- After upgrading backend artifacts, start the application with the normal Resource Registry and configuration initialization path so the new System admin branding menu and default configuration resources are synchronized before assigning permissions.
- No manual database DDL is required by this release beyond normal Flyway execution.

### Verification

- `mvn -f mango/pom.xml -pl mango-platform/mango-system/mango-system-core -am -Drevision=1.0.1 -Dtest=AdminBrandingServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `pnpm --filter @mango/cli test`
- `pnpm --filter @mango/file test`
- `pnpm --filter @mango/cli run check:release-versions`
- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`
- `pnpm -C mango-ui release:impact --base=v2026.06.30-file-download-cli-pmo-release --head=HEAD`
- `node mango-pmo/tools/audit-module-readmes.mjs`
- `node mango-pmo/tools/audit-readme-source-facts.mjs`
- `node mango-pmo/tools/check-business-guides.mjs`
- `PR_BODY_FILE=/tmp/release-maven-1.0.1-pr-body.md node mango-pmo/tools/check-capability-docs.mjs --base origin/main --head HEAD`
- `mvn -f mango/pom.xml -Drevision=1.0.1 -DskipTests deploy`
- `MANGO_SHARED_PUBLISH_GATES_PASSED=1 pnpm -C mango-ui publish:pkg <package> --release-tag=v2026.06.30-maven-1.0.1-admin-branding-cli-release --skip-shared-gates`
- `pnpm -C mango-ui release:verify-npm <package> --version=<version>`
- `mvn -q dependency:get -Dartifact=io.mango:mango-admin-starter:1.0.1 -Dtransitive=false -DremoteRepositories=mango-public::default::http://nexus.inner.yunxinbaokeji.com/repository/maven-public/`
- `git diff --check`

## v2026.06.30-file-download-cli-pmo-release - 2026-06-30

### Fixed

- Published the file download filename fix from Issue #332 / PR #333. `Content-Disposition` filenames are no longer double-encoded, so Chinese names and `+` characters are presented correctly by browser downloads.
- Published the Mango CLI dev-workspace cleanup from PR #331. Generated and root compatibility scripts no longer keep the legacy `init` shim as an owning entry point; business projects should use Mango CLI workspace commands.

### New

- Published the PMO test case automation governance flow from PR #334. Mango delivery rules, templates, and checkers now require test case registration, automation layer decisions, result baselines, and business-developer-facing handoff output.

### Published Packages

- Maven: File backend batch `io.mango.platform.file:mango-file-api`, `mango-file-core`, `mango-file-starter`, and `mango-file-starter-remote` at `1.0.0-SNAPSHOT` to `http://nexus.inner.yunxinbaokeji.com/repository/maven-snapshots/`.
- npm: `@mango/pmo@1.0.5` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/cli@1.0.53` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- GitHub Release: `v2026.06.30-file-download-cli-pmo-release`.

### Upgrade Notes

- Backend consumers should refresh Mango `1.0.0-SNAPSHOT` file dependencies before relying on corrected browser download filenames for non-ASCII file names.
- Business developers should install `@mango/cli@1.0.53` with `npm install -g @mango/cli@1.0.53 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- Existing business projects should run `mango pmo sync --project-dir . --sync-shell` or `mango pmo upgrade --project-dir . --sync-shell` to receive `@mango/pmo@1.0.5`, updated PMO governance, and aligned compatibility scripts.
- No database migration, menu resource, button permission, tenant binding, frontend runtime package, or route change is required for this release.

### Verification

- `mvn -pl mango-platform/mango-file/mango-file-starter -am -Dtest=FileControllerDownloadResponseTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl mango-platform/mango-file/mango-file-starter -am test`
- `pnpm --filter @mango/pmo build`
- `pnpm --filter @mango/pmo check`
- `pnpm --filter @mango/cli test`
- `pnpm --filter @mango/cli run check:release-versions`
- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`
- `pnpm run package-consumer:typecheck -- --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `node mango-pmo/tools/check-pmo-preflight.mjs`
- `node mango-pmo/tools/check-governance-intent.mjs`
- `node mango-pmo/tools/audit-module-readmes.mjs`
- `node mango-pmo/tools/audit-readme-source-facts.mjs`
- `node mango-pmo/tools/check-business-guides.mjs`
- `PR_BODY_FILE=/tmp/release-pr-body.md node mango-pmo/tools/check-capability-docs.mjs --base origin/main --head HEAD`
- `scripts/publish-maven-batch.sh :mango-file-api :mango-file-core :mango-file-starter :mango-file-starter-remote --revision 1.0.0-SNAPSHOT`
- `MANGO_SHARED_PUBLISH_GATES_PASSED=1 pnpm -C mango-ui publish:pkg pmo --release-tag=v2026.06.30-file-download-cli-pmo-release --skip-shared-gates`
- `MANGO_SHARED_PUBLISH_GATES_PASSED=1 pnpm -C mango-ui publish:pkg cli --release-tag=v2026.06.30-file-download-cli-pmo-release --skip-shared-gates`
- `pnpm -C mango-ui release:verify-npm pmo --version=1.0.5`
- `pnpm -C mango-ui release:verify-npm cli --version=1.0.53`
- `git diff --check`

## v2026.06.29-file-compression-release - 2026-06-29

### New

- Published compressed file downloads from PR #329. `mango-infra-fileproc` now exposes `FileCompressApi` with image and rasterized PDF compression providers, and `mango-file` can apply compression to single downloads and ZIP package entries.
- `FileApi.packageFiles(FilePackageCommand)` and `POST /file/files/package` now support package-level `compression` / `perFileTargetSizeBytes` and entry-level `compression` / `targetSizeBytes`; unsupported file types remain unchanged in the ZIP.

### Published Packages

- Maven: Fileproc backend batch `io.mango.infra.fileproc:mango-infra-fileproc-api`, `mango-infra-fileproc-core`, and `mango-infra-fileproc-starter` at `1.0.0-SNAPSHOT` to `http://nexus.inner.yunxinbaokeji.com/repository/maven-snapshots/`.
- Maven: File backend batch `io.mango.platform.file:mango-file-api`, `mango-file-core`, `mango-file-starter`, and `mango-file-starter-remote` at `1.0.0-SNAPSHOT` to `http://nexus.inner.yunxinbaokeji.com/repository/maven-snapshots/`.
- GitHub Release: `v2026.06.29-file-compression-release`.

### Upgrade Notes

- Backend consumers should refresh Mango `1.0.0-SNAPSHOT` fileproc and file dependencies before using compressed file downloads or ZIP entry compression.
- `perFileTargetSizeBytes` and entry-level `targetSizeBytes` are single-file targets, not ZIP total-size targets.
- Office original-format image recompression is not implemented in this release. Word, PPT, and Excel entries remain original unless business code converts them before download.
- No database migration, menu resource, button permission, tenant binding, frontend package, or route change is required for this release.

### Verification

- `mvn -pl mango-infra/mango-infra-fileproc/mango-infra-fileproc-core -am -Dtest=FileCompressApiTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl mango-infra/mango-infra-fileproc/mango-infra-fileproc-core,mango-infra/mango-infra-fileproc/mango-infra-fileproc-starter,mango-platform/mango-file/mango-file-core,mango-platform/mango-file/mango-file-starter -am test`
- `node mango-pmo/tools/audit-backend-test-mocks.mjs --report-only --changed-only --base origin/main`
- `node mango-pmo/tools/audit-module-readmes.mjs`
- `node mango-pmo/tools/audit-readme-source-facts.mjs`
- `node mango-pmo/tools/check-business-guides.mjs`
- `PR_BODY_FILE=.release-pr-body.md node mango-pmo/tools/check-capability-docs.mjs --base origin/main --head HEAD`
- `scripts/publish-maven-batch.sh :mango-infra-fileproc-api :mango-infra-fileproc-core :mango-infra-fileproc-starter :mango-file-api :mango-file-core :mango-file-starter :mango-file-starter-remote --revision 1.0.0-SNAPSHOT`
- `git diff --check`

## v2026.06.29-auth-subject-role-release - 2026-06-29

### New

- Published the authorization Resource Registry `AUTH_SUBJECT_ROLE` stable subject binding support from PR #327. Member role baseline declarations can now resolve tenant members by `subjectId`, `subjectCode`, `memberNo`, or `username`, while keeping direct `subjectId` compatibility and ignoring members that have left the tenant.

### Published Packages

- Maven: Authorization backend batch `io.mango.platform.authorization:mango-authorization-api`, `mango-authorization-core`, `mango-authorization-support`, `mango-authorization-starter`, `mango-authorization-resource-sync-starter`, `mango-authorization-resource-access-starter`, and `mango-authorization-starter-remote` at `1.0.0-SNAPSHOT` to `http://nexus.inner.yunxinbaokeji.com/repository/maven-snapshots/`.
- GitHub Release: `v2026.06.29-auth-subject-role-release`.

### Upgrade Notes

- Backend consumers should refresh Mango `1.0.0-SNAPSHOT` authorization dependencies before using stable keys in `AUTH_SUBJECT_ROLE` declarations.
- Existing `subjectId` declarations remain compatible. New `subjectCode` and `memberNo` declarations resolve against active `tenant_member.member_no`; `username` declarations resolve `identity_user.username` and then the active tenant member for the target tenant.
- Environments that initialize roles through Resource Registry should rerun resource synchronization after upgrading so member-role binding baselines are applied with the new stable subject keys.
- No npm package version change, database migration, menu resource, button permission, tenant binding, or frontend route change is required for this release.

### Verification

- `mvn -f mango/pom.xml -pl mango-platform/mango-authorization/mango-authorization-starter -am -Dtest=AuthSubjectRoleResourceHandlerIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `node mango-pmo/tools/audit-module-readmes.mjs`
- `node mango-pmo/tools/audit-readme-source-facts.mjs`
- `scripts/publish-maven-batch.sh :mango-authorization-api :mango-authorization-core :mango-authorization-support :mango-authorization-starter :mango-authorization-resource-sync-starter :mango-authorization-resource-access-starter :mango-authorization-starter-remote --revision 1.0.0-SNAPSHOT`
- `git diff --check`

## v2026.06.29-file-package-cli-release - 2026-06-29

### New

- Published File ZIP packaging from Issue #316. `FileApi.packageFiles(FilePackageCommand)` and `POST /file/files/package` can create a ZIP from existing completed file records, preserve nested relative paths, replace `${fileName}` from source file records, save the generated ZIP through the existing file storage flow, and return a new `FileRecordVO`.

### Fixed

- Published the business project generator dependency management fix from Issue #315. Generated backend parent POMs now manage Mango public API artifacts consistently, so business core modules can depend on API artifacts such as `mango-file-api` without declaring versions or depending on runtime starters.

### Published Packages

- Maven: File backend batch `io.mango.platform.file:mango-file-api`, `mango-file-core`, `mango-file-starter`, and `mango-file-starter-remote` at `1.0.0-SNAPSHOT` to `http://nexus.inner.yunxinbaokeji.com/repository/maven-snapshots/`.
- npm: `@mango/cli@1.0.52` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- GitHub Release: `v2026.06.29-file-package-cli-release`.

### Upgrade Notes

- Backend consumers should refresh Mango `1.0.0-SNAPSHOT` dependencies before using `FileApi.packageFiles` or `POST /file/files/package`.
- File ZIP packaging does not require a database migration, new menu resource, frontend package, or tenant configuration change. Existing file visibility, download, and save rules still apply to source files and generated ZIP records.
- Business code should pass safe ZIP-relative `entries.path` values. The file service rejects empty paths, directory entries, absolute paths, path traversal, and duplicate ZIP paths.
- New or regenerated business projects should use `@mango/cli@1.0.52` so generated backend parent POMs include the current Mango API dependency management. Existing projects can manually align parent POM dependency management if needed.

### Verification

- `mvn -f mango/pom.xml -pl mango-platform/mango-file/mango-file-core -am -Dtest=FileServicePackageFilesTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f mango/pom.xml -pl mango-platform/mango-file/mango-file-api,mango-platform/mango-file/mango-file-core,mango-platform/mango-file/mango-file-starter,mango-platform/mango-file/mango-file-starter-remote -am test -DskipITs`
- `pnpm -C mango-ui --filter @mango/cli test`
- `pnpm -C mango-ui release:impact --base=v2026.06.29-payment-admin-check-release --head=HEAD`
- `node mango-pmo/tools/audit-module-readmes.mjs`
- `node mango-pmo/tools/audit-readme-source-facts.mjs`
- `node mango-pmo/tools/check-business-guides.mjs`
- `PR_BODY_FILE=.runtime/pr-320-body.md node mango-pmo/tools/check-capability-docs.mjs --base origin/main --head HEAD`
- `scripts/publish-maven-batch.sh :mango-file-api :mango-file-core :mango-file-starter :mango-file-starter-remote --revision 1.0.0-SNAPSHOT`
- `MANGO_SHARED_PUBLISH_GATES_PASSED=1 pnpm -C mango-ui publish:pkg cli --release-tag=v2026.06.29-file-package-cli-release --skip-shared-gates`
- `pnpm -C mango-ui release:verify-npm cli --version=1.0.52`
- `git diff --check`

## v2026.06.29-payment-admin-check-release - 2026-06-29

### Fixed

- Published the payment administration menu and refund workflow initialization from PR #310. `支付管理` is now included in the platform admin package under `平台能力`, and monolith startup enables the payment refund approval workflow resource.
- Published the authorization permission snapshot fix from PR #310. Subject permission aggregation now includes page-level menu permissions together with button permissions, while excluding directory menu permissions.
- Published the `mango:check` persistence schema migration folding fix from PR #309. The Maven plugin can now fold module-scoped persistence migrations into the schema checks without requiring developers to duplicate migration paths.
- Published the stale public documentation staging fix from PR #308 so deleted/deprecated `mango-seed` documentation entries no longer break GitHub Pages publishing.

### Published Packages

- Maven: `io.mango.tools.maven.plugin:mango-maven-plugin:1.0.0-SNAPSHOT` to `http://nexus.inner.yunxinbaokeji.com/repository/maven-snapshots/`.
- Maven: Authorization backend batch `io.mango.platform.authorization:mango-authorization-api`, `mango-authorization-core`, `mango-authorization-support`, `mango-authorization-starter`, `mango-authorization-resource-sync-starter`, `mango-authorization-resource-access-starter`, and `mango-authorization-starter-remote` at `1.0.0-SNAPSHOT` to `http://nexus.inner.yunxinbaokeji.com/repository/maven-snapshots/`.
- Maven: Payment backend batch `io.mango.platform.payment:mango-payment-api`, `mango-payment-core`, `mango-payment-starter`, and `mango-payment-starter-remote` at `1.0.0-SNAPSHOT` to `http://nexus.inner.yunxinbaokeji.com/repository/maven-snapshots/`.
- Docs: GitHub Pages Documentation run `28332162284` published the current developer docs for commit `cb2046cfa`.
- GitHub Release: `v2026.06.29-payment-admin-check-release`.

### Upgrade Notes

- Backend consumers should refresh Mango `1.0.0-SNAPSHOT` dependencies before using the payment management menu, refund workflow defaults, subject permission snapshots, or the updated `mango:check` persistence schema folding logic.
- Environments that use payment refund approval must rerun resource initialization/synchronization after upgrading so the payment menu resources and refund workflow resource are present.
- Admin roles must be granted the updated payment menu resources before users can see `平台能力 / 支付管理`.
- Projects using `mvn mango:check` should refresh `mango-maven-plugin:1.0.0-SNAPSHOT` so migration folding checks use the current persistence schema behavior.
- No npm package version change is required for this release.

### Verification

- `mvn -f mango/pom.xml -pl mango-platform/mango-authorization/mango-authorization-core -Dtest=SubjectAuthorityServiceImplIntegrationTest test`
- `mvn -f mango/pom.xml -pl mango-tools/mango-maven-plugin -Dtest=CheckMojoTest test`
- `node mango-pmo/tools/audit-module-readmes.mjs`
- `node mango-pmo/tools/audit-readme-source-facts.mjs`
- Payment menu JSON validation for `payment-common-menu.json`: valid JSON, 25 menu nodes and 68 button permissions are assigned to `platform_admin`; root `支付管理` remains under `data`.
- GitHub Pages Documentation run `28332162284` succeeded for commit `cb2046cfa`.
- `scripts/publish-maven-batch.sh :mango-maven-plugin :mango-authorization-api :mango-authorization-core :mango-authorization-support :mango-authorization-starter :mango-authorization-resource-sync-starter :mango-authorization-resource-access-starter :mango-authorization-starter-remote :mango-payment-api :mango-payment-core :mango-payment-starter :mango-payment-starter-remote --revision 1.0.0-SNAPSHOT`
- `git diff --check`

## v2026.06.29-workflow-return-cli-db-release - 2026-06-29

### New

- Published Mango Workflow task return support from Issue #296. `POST /workflow/tasks/return` can move an active approval task back to a historical user task, emit refreshed current task snapshots, record RETURN audit history, and expose the `workflow:task:return` permission.
- Published the matching `@mango/workflow@1.0.18` frontend action, task detail handling, designer action configuration, and business approval integration guidance for return-to-node flows.

### Fixed

- Published the Mango CLI local database auto-create fix from Issue #297. `mango dev start` now creates local `mango_dev_*` workspace databases before starting Spring Boot Maven apps when `MANGO_DB_AUTO_CREATE=true`, while Flyway remains responsible for schema and seed data.
- Published the CLI development workspace runtime ownership and manifest discovery updates accumulated since the previous release so generated and upgraded business projects use the current Mango CLI workspace/dev/frontend entry points.

### Breaking / Required Actions

- Mango local development is now CLI-owned. Mango source developers and business project developers should use `mango workspace init` and `mango dev start`; `scripts/dev-workspace.sh` remains a compatibility shim and should not be treated as the owner of workspace allocation, ports, frontend preparation, or process lifecycle.
- Existing business projects must upgrade to `@mango/cli@1.0.51`, sync the versioned PMO baseline with `mango pmo upgrade --project-dir . --sync-shell` or `mango pmo sync --project-dir . --sync-shell`, and then run `mango workspace init` in each active worktree so `.mango/workspace.json`, `.mango/dev-workspace.env`, and `mango.dev.json` follow the current runtime model.
- Local database names are workspace-owned and must stay under the `mango_dev_*` prefix when `MANGO_DB_AUTO_CREATE=true`. The CLI refuses to auto-create arbitrary database names; teams using custom local databases must create them manually or turn off auto-create.
- Workflow return actions add the `workflow:task:return` permission. Environments must rerun resource synchronization and grant this permission explicitly before users can return approval tasks.

### Published Packages

- Maven: Workflow backend batch `io.mango.platform.workflow:mango-workflow-api`, `mango-workflow-core`, `mango-workflow-starter`, and `mango-workflow-starter-remote` at `1.0.0-SNAPSHOT` to `http://nexus.inner.yunxinbaokeji.com/repository/maven-snapshots/`.
- npm: `@mango/pmo@1.0.4`, `@mango/workflow@1.0.18`, `@mango/grid-widgets@1.0.7`, `@mango/workflow-business-example@1.0.17`, `@mango/admin-shell@1.0.30`, `@mango/admin@1.0.34`, and `@mango/cli@1.0.51` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- GitHub Release: `v2026.06.29-workflow-return-cli-db-release`.

### Upgrade Notes

- Backend consumers should refresh Mango `1.0.0-SNAPSHOT` dependencies before using workflow task return. Existing databases do not need a new workflow schema migration for this release, but resource synchronization must run so `workflow:task:return` is available to roles.
- Workflow frontends should upgrade `@mango/workflow` to `1.0.18`. Dashboard/widget consumers should upgrade `@mango/grid-widgets@1.0.7`. Projects consuming the aggregate admin package should upgrade `@mango/admin@1.0.34`; shell consumers should upgrade `@mango/admin-shell@1.0.30`.
- New or regenerated business projects should upgrade `@mango/cli` to `1.0.51`, then run `mango pmo sync --project-dir . --sync-shell` or `mango pmo upgrade --project-dir . --sync-shell` and `mango workspace init` in active worktrees.
- Complex workflow definitions with parallel, multi-instance, or repeated user task nodes should configure `targetTaskDefinitionKey` explicitly for return actions instead of relying on the nearest-history default.

### Adoption Verification Plan

- Mango source developer path: install the released CLI, run `mango workspace init`, `mango dev plan`, and `mango dev start` in a clean Mango worktree, then confirm the allocated ports, `.mango/workspace.json`, `.mango/dev-workspace.env`, and source-mode frontend aliases match `mango-pmo/rules/02-dev-environment.md`.
- Existing business project path: upgrade to `@mango/cli@1.0.51`, run `mango pmo upgrade --project-dir . --sync-shell`, verify `mango pmo status --project-dir .` reports `@mango/pmo@1.0.4`, rerun `mango workspace init`, and confirm `mango dev plan` shows the discovered backend/frontend apps and a `mango_dev_*` database.
- Fresh business project path: generate a new project with `@mango/cli@1.0.51`, verify generated `README.md`, `AGENTS.md`, `business-pmo/mango-baseline`, `release-versions.json`, and `mango.dev.json` all reference the CLI-owned workflow, then run `mango workspace init` and `mango dev plan`.
- Permission/resource path: after syncing resources, verify roles can see or grant `workflow:task:return`; without that permission, return actions must remain inaccessible.

### Verification

- `pnpm -C mango-ui --filter @mango/pmo build`
- `pnpm -C mango-ui --filter @mango/pmo check`
- `mvn -q -f mango/pom.xml -pl mango-platform/mango-workflow/mango-workflow-core -am -Dtest=WorkflowNodeActionConfigResolverTest,WorkflowTaskRuntimeServiceIntegrationTest,WorkflowTaskRuntimeServiceImplIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -q -f mango/pom.xml -pl mango-platform/mango-workflow/mango-workflow-starter -am -DskipTests compile`
- `pnpm -C mango-ui --filter @mango/workflow build`
- `pnpm -C mango-ui --filter @mango/grid-widgets build`
- `pnpm -C mango-ui --filter @mango/workflow-business-example build`
- `pnpm -C mango-ui --filter @mango/admin-shell build`
- `pnpm -C mango-ui --filter @mango/admin build`
- `pnpm -C mango-ui --filter @mango/cli test`
- `pnpm -C mango-ui release:impact --base=v2026.06.27-notice-check-release --head=HEAD`
- `pnpm -C mango-ui package-consumer:typecheck -- --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `pnpm -C mango-ui admin:styles:check`
- `pnpm -C mango-ui admin:module-styles:check`
- `node mango-pmo/tools/audit-module-readmes.mjs`
- `node mango-pmo/tools/audit-readme-source-facts.mjs`
- `node mango-pmo/tools/check-business-guides.mjs`
- `scripts/publish-maven-batch.sh :mango-workflow-api :mango-workflow-core :mango-workflow-starter :mango-workflow-starter-remote --revision 1.0.0-SNAPSHOT`
- `MANGO_SHARED_PUBLISH_GATES_PASSED=1 pnpm -C mango-ui publish:pkg pmo --release-tag=v2026.06.29-workflow-return-cli-db-release --skip-shared-gates`
- `MANGO_SHARED_PUBLISH_GATES_PASSED=1 pnpm -C mango-ui publish:pkg workflow --release-tag=v2026.06.29-workflow-return-cli-db-release --skip-shared-gates`
- `MANGO_SHARED_PUBLISH_GATES_PASSED=1 pnpm -C mango-ui publish:pkg grid-widgets --release-tag=v2026.06.29-workflow-return-cli-db-release --skip-shared-gates`
- `MANGO_SHARED_PUBLISH_GATES_PASSED=1 pnpm -C mango-ui publish:pkg workflow-business-example --release-tag=v2026.06.29-workflow-return-cli-db-release --skip-shared-gates`
- `MANGO_SHARED_PUBLISH_GATES_PASSED=1 pnpm -C mango-ui publish:pkg admin-shell --release-tag=v2026.06.29-workflow-return-cli-db-release --skip-shared-gates`
- `MANGO_SHARED_PUBLISH_GATES_PASSED=1 pnpm -C mango-ui publish:pkg admin --release-tag=v2026.06.29-workflow-return-cli-db-release --skip-shared-gates`
- `MANGO_SHARED_PUBLISH_GATES_PASSED=1 pnpm -C mango-ui publish:pkg cli --release-tag=v2026.06.29-workflow-return-cli-db-release --skip-shared-gates`
- `git diff --check`

## v2026.06.28-cli-dev-manifest-discovery - 2026-06-28

### Fixed

- Published `@mango/cli@1.0.49` with development workspace manifest discovery for existing business projects. `mango pmo sync --sync-shell` and `mango init-dev` can now generate `mango.dev.json` from discovered Spring Boot Maven app POMs and Vite apps instead of assuming fixed `backend` and `frontend` folders.
- Skipped Maven aggregator POMs during CLI manifest generation and emitted confirmation warnings for skipped aggregators or multiple detected backend/frontend apps.
- Documented the discovery rules and troubleshooting path in the `@mango/cli` README.

### Published Packages

- npm: `@mango/cli@1.0.49` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- GitHub Release: `v2026.06.28-cli-dev-manifest-discovery`.

### Upgrade Notes

- Existing business projects with renamed backend or frontend folders should upgrade to `@mango/cli@1.0.49`, then run `mango pmo sync --project-dir . --sync-shell` or `mango init-dev` when no project-owned `mango.dev.json` exists.
- Projects with custom Spring Boot or frontend commands can keep their committed `mango.dev.json` or override local paths through `.mango/dev-workspace.local.json`.
- No backend Maven dependency, database migration, menu data, permission code, tenant configuration, or frontend runtime page change is required for this release.

### Verification

- `node --check mango-ui/packages/mango-cli/src/index.mjs`
- `node --check mango-ui/packages/mango-cli/scripts/check-cli.mjs`
- `pnpm -C mango-ui --filter @mango/cli test`
- `pnpm -C mango-ui release:impact --base=origin/main --head=HEAD`
- `pnpm -C mango-ui admin:styles:check`
- `pnpm -C mango-ui admin:module-styles:check`
- `node mango-pmo/tools/audit-module-readmes.mjs`
- `node mango-pmo/tools/audit-readme-source-facts.mjs`
- `PR_BODY_FILE=.runtime/pr/pr-294-body.md node mango-pmo/tools/check-capability-docs.mjs --base origin/main --head HEAD`
- `git diff --check`

## v2026.06.27-notice-check-release - 2026-06-27

### Fixed

- Published the `mango:check` no-new-violations baseline gate support from PR #287. The Maven plugin can now compare current findings against a JSON baseline and fail only on findings outside that baseline.
- Published the Notice announcement cleanup from PR #288. Announcement query/entity/service code now aligns with the low-risk check cleanup, and upgraded databases receive missing announcement organization/audit fields through `V16__notice_announcement_audit_fields.sql` without rewriting the released V15 migration.
- Updated the versioned PMO baseline and CLI release lock so business projects can sync the current Mango PMO rules, mango-check baseline, and generated release metadata from published npm packages.

### Published Packages

- Maven: `io.mango.tools.maven.plugin:mango-maven-plugin:1.0.0-SNAPSHOT` to `http://nexus.inner.yunxinbaokeji.com/repository/maven-snapshots/`.
- Maven: Notice backend batch `io.mango.platform.notice:mango-notice-api`, `mango-notice-core`, `mango-notice-starter`, and `mango-notice-starter-remote` at `1.0.0-SNAPSHOT` to `http://nexus.inner.yunxinbaokeji.com/repository/maven-snapshots/`.
- npm: `@mango/pmo@1.0.3` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/cli@1.0.48` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- GitHub Release: `v2026.06.27-notice-check-release`.

### Upgrade Notes

- Backend consumers should refresh Mango `1.0.0-SNAPSHOT` dependencies before using the Notice announcement cleanup. Existing databases must run Flyway so `V16__notice_announcement_audit_fields.sql` adds the missing organization and audit fields.
- Projects or CI jobs that use `mvn mango:check -Dmango.check.gate=no-new-violations` can now provide `-Dmango.check.baselineFile=<baseline.json>` to gate only newly introduced findings.
- Existing business projects should upgrade to `@mango/cli@1.0.48` and rerun `mango pmo sync --project-dir .` or `mango pmo upgrade --project-dir .` to receive `@mango/pmo@1.0.3` baseline content.
- No frontend runtime page, menu data, button permission, tenant binding, or business guide behavior changes are required for this release.

### Verification

- `mvn -pl mango-tools/mango-maven-plugin,mango-platform/mango-notice/mango-notice-core,mango-platform/mango-notice/mango-notice-api -am test -DskipTests=false`
- `mvn mango:check -Dmango.check.gate=no-new-violations -Dmango.check.baselineFile=../mango-pmo/baselines/mango-check/no-new-violations-baseline.json -Dmango.check.codeLevelExcludedModules=mango-platform/mango-file-preview -DreportFile=target/mango-check-report.json`
- `pnpm -C mango-ui --filter @mango/pmo build`
- `pnpm -C mango-ui --filter @mango/pmo check`
- `pnpm -C mango-ui --filter @mango/cli test`
- `pnpm -C mango-ui --filter @mango/cli run check:release-versions -- --check-registry --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/ --ignore-registry-package=@mango/pmo --ignore-registry-package=@mango/cli`
- `pnpm -C mango-ui package-consumer:typecheck -- --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `pnpm -C mango-ui admin:styles:check`
- `pnpm -C mango-ui admin:module-styles:check`
- `node mango-pmo/tools/audit-module-readmes.mjs`
- `node mango-pmo/tools/audit-readme-source-facts.mjs`
- `node mango-pmo/tools/check-business-guides.mjs`
- `PR_BODY_FILE=.release-pr-body.md node mango-pmo/tools/check-capability-docs.mjs --base origin/main --head HEAD`
- `scripts/publish-maven-batch.sh :mango-maven-plugin :mango-notice-api :mango-notice-core :mango-notice-starter :mango-notice-starter-remote --revision 1.0.0-SNAPSHOT`
- `MANGO_SHARED_PUBLISH_GATES_PASSED=1 pnpm -C mango-ui publish:pkg pmo --release-tag=v2026.06.27-notice-check-release --skip-shared-gates`
- `MANGO_SHARED_PUBLISH_GATES_PASSED=1 pnpm -C mango-ui publish:pkg cli --release-tag=v2026.06.27-notice-check-release --skip-shared-gates`
- `pnpm -C mango-ui release:verify-npm pmo --version=1.0.3`
- `pnpm -C mango-ui release:verify-npm cli --version=1.0.48`
- `git diff --check`

## v2026.06.27-pmo-cli-baseline-release - 2026-06-27

### Fixed

- Published the PMO baseline EOF blank-line fix from PR #285 so business projects consuming `@mango/pmo` or `@mango/cli` can sync a baseline without trailing blank lines.
- Fixed `publish:pkg cli` so CLI self-publish checks can validate all already-published release locks while excluding the CLI package version that is currently being published.

### Published Packages

- npm: `@mango/pmo@1.0.2` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/cli@1.0.47` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- GitHub Release: `v2026.06.27-pmo-cli-baseline-release`.

### Upgrade Notes

- Existing business projects that synced PMO baseline with an earlier CLI should upgrade to `@mango/cli@1.0.47` and rerun `mango pmo sync --project-dir .` or `mango pmo upgrade --project-dir .`.
- Business projects that consume `@mango/pmo` directly should upgrade to `@mango/pmo@1.0.2`.
- No backend Maven dependency, database migration, menu data, permission code, tenant configuration, or frontend runtime page change is required for this release.

### Verification

- `pnpm -C mango-ui --filter @mango/pmo build`
- `pnpm -C mango-ui --filter @mango/pmo check`
- `pnpm -C mango-ui --filter @mango/cli test`
- `pnpm -C mango-ui --filter @mango/cli run check:release-versions -- --check-registry --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/ --ignore-registry-package=@mango/pmo --ignore-registry-package=@mango/cli`
- `pnpm -C mango-ui publish:pkg cli --release-tag=v2026.06.27-pmo-cli-baseline-release --dry-run`
- `pnpm -C mango-ui package-consumer:typecheck -- --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `pnpm -C mango-ui admin:styles:check`
- `pnpm -C mango-ui admin:module-styles:check`
- `node mango-pmo/tools/audit-module-readmes.mjs`
- `node mango-pmo/tools/audit-readme-source-facts.mjs`
- `node mango-pmo/tools/check-business-guides.mjs`
- `PR_BODY_FILE=.release-pr-body.md node mango-pmo/tools/check-capability-docs.mjs --base origin/main --head HEAD`
- `pnpm -C mango-ui release:verify-npm pmo --version=1.0.2`
- `pnpm -C mango-ui release:verify-npm cli --version=1.0.47`
- `git diff --check`

## v2026.06.27-workflow-history-dialog-release - 2026-06-27

### Fixed

- Published the workflow history dialog title fix from PR #281. Business approval history dialogs now avoid showing a duplicate inner title when opened from the reusable workflow UI components.
- Updated the workflow package release batch and dependent aggregate packages so direct package consumers, admin shell consumers, grid widget consumers, workflow business examples, aggregate admin consumers, and newly generated business projects resolve the same workflow UI fix.

### Published Packages

- npm: `@mango/workflow@1.0.17` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/admin-shell@1.0.29`, `@mango/grid-widgets@1.0.6`, and `@mango/workflow-business-example@1.0.16` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/admin@1.0.33` and `@mango/cli@1.0.46` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- GitHub Release: `v2026.06.27-workflow-history-dialog-release`.

### Upgrade Notes

- Business frontends that consume workflow UI components directly should upgrade to `@mango/workflow@1.0.17`.
- Business frontends that consume admin shell, grid widgets, workflow business example, or the aggregate admin package should upgrade this release batch together: `@mango/admin-shell@1.0.29`, `@mango/grid-widgets@1.0.6`, `@mango/workflow-business-example@1.0.16`, and `@mango/admin@1.0.33`.
- New or regenerated business projects must use `@mango/cli@1.0.46` so generated frontend dependency locks include the same workflow UI release batch.
- No backend Maven dependency, database migration, menu data, permission code, or tenant configuration change is required for this release.

### Verification

- `pnpm -C mango-ui release:impact --base=c97c79be17a7cd9ecefff64e6c7dbbbdcc05b509 --head=HEAD`
- `pnpm -C mango-ui admin:styles:check`
- `pnpm -C mango-ui admin:module-styles:check`
- `pnpm -C mango-ui --filter @mango/workflow build`
- `pnpm -C mango-ui --filter @mango/admin-shell build`
- `pnpm -C mango-ui --filter @mango/grid-widgets build`
- `pnpm -C mango-ui --filter @mango/workflow-business-example build`
- `pnpm -C mango-ui --filter @mango/admin build`
- `pnpm -C mango-ui --filter @mango/cli test`
- `pnpm -C mango-ui --filter @mango/cli run check:release-versions`
- `pnpm -C mango-ui package-consumer:typecheck -- --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `pnpm -C mango-ui release:verify-npm workflow --version=1.0.17`
- `pnpm -C mango-ui release:verify-npm admin-shell --version=1.0.29`
- `pnpm -C mango-ui release:verify-npm grid-widgets --version=1.0.6`
- `pnpm -C mango-ui release:verify-npm workflow-business-example --version=1.0.16`
- `pnpm -C mango-ui release:verify-npm admin --version=1.0.33`
- `pnpm -C mango-ui release:verify-npm cli --version=1.0.46`
- `git diff --check`

## v2026.06.27-admin-shell-menu-redirect-release - 2026-06-27

### Fixed

- Published the Admin Shell directory menu redirect fix from issue #274. Directory menu redirects now only take effect when the target page is visible and runnable for the current user; otherwise Admin Shell falls back to the first accessible child page in the visible menu tree.
- Updated the aggregate admin package and CLI release locks so direct consumers, aggregate consumers, and newly generated business projects can receive the same Admin Shell fix.

### Published Packages

- npm: `@mango/admin-shell@1.0.28` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/admin@1.0.32` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/cli@1.0.45` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- GitHub Release: `v2026.06.27-admin-shell-menu-redirect-release`.

### Upgrade Notes

- Business frontends that consume the admin shell directly should upgrade to `@mango/admin-shell@1.0.28`.
- Business frontends that consume the aggregate admin package should upgrade to `@mango/admin@1.0.32`.
- New or regenerated business projects must use `@mango/cli@1.0.45` so generated frontend dependency locks include `@mango/admin-shell@1.0.28` and `@mango/admin@1.0.32`.
- No backend Maven dependency, database migration, menu data, permission code, or tenant configuration change is required for this release.

### Verification

- `pnpm install --lockfile-only`
- `pnpm --filter @mango/admin-shell test`
- `pnpm --filter @mango/admin-shell build`
- `pnpm --filter @mango/admin build`
- `pnpm --filter @mango/cli test`
- `pnpm --filter @mango/cli run check:release-versions`
- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`
- `pnpm package-exports:check`
- `pnpm package-consumer:typecheck -- --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `node mango-pmo/tools/audit-module-readmes.mjs`
- `node mango-pmo/tools/audit-readme-source-facts.mjs`
- `node mango-pmo/tools/check-business-guides.mjs`
- `PR_BODY_FILE=.release-pr-body.md node mango-pmo/tools/check-capability-docs.mjs --base origin/main --head HEAD`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/admin-shell --version=1.0.28 --tag=v2026.06.27-admin-shell-menu-redirect-release --check-github-release`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/admin --version=1.0.32 --tag=v2026.06.27-admin-shell-menu-redirect-release --check-github-release`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/cli --version=1.0.45 --tag=v2026.06.27-admin-shell-menu-redirect-release --check-github-release`
- `git diff --check`

## v2026.06.27-system-component-release - 2026-06-27

### Fixed

- Published the updated `@mango/system` component package so business frontends can consume the latest system UI build through npm-hosted.
- Updated the direct frontend packages that depend on `@mango/system`, plus the aggregate `@mango/admin` package and `@mango/cli` release lock, so new, regenerated, and upgraded business projects resolve a consistent system component version.

### Published Packages

- npm: `@mango/system@1.0.11` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/admin-pages@1.0.12`, `@mango/admin-shell@1.0.27`, `@mango/calendar@1.0.13`, `@mango/cms@1.0.2`, `@mango/file@1.0.13`, `@mango/grid-widgets@1.0.5`, `@mango/job@1.0.5`, `@mango/notice@1.0.14`, `@mango/numgen@1.0.13`, `@mango/payment@1.0.4`, `@mango/template@1.0.13`, `@mango/workflow@1.0.16`, and `@mango/workflow-business-example@1.0.15` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/admin@1.0.31` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/cli@1.0.44` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- GitHub Release: `v2026.06.27-system-component-release`.

### Upgrade Notes

- Business frontends that consume system pages directly should upgrade to `@mango/system@1.0.11`.
- Business frontends that consume the aggregate admin package should upgrade to `@mango/admin@1.0.31`.
- Business frontends that consume optional modules directly should upgrade this frontend package batch together, including `@mango/admin-pages@1.0.12`, `@mango/admin-shell@1.0.27`, `@mango/calendar@1.0.13`, `@mango/cms@1.0.2`, `@mango/file@1.0.13`, `@mango/grid-widgets@1.0.5`, `@mango/job@1.0.5`, `@mango/notice@1.0.14`, `@mango/numgen@1.0.13`, `@mango/payment@1.0.4`, `@mango/template@1.0.13`, `@mango/workflow@1.0.16`, and `@mango/workflow-business-example@1.0.15`.
- New or regenerated business projects must use `@mango/cli@1.0.44` so generated frontend dependency locks include this release batch.

### Verification

- `pnpm install --lockfile-only`
- `pnpm --filter @mango/system build`
- `pnpm --filter @mango/admin-pages build`
- `pnpm --filter @mango/admin-shell build`
- `pnpm --filter @mango/calendar build`
- `pnpm --filter @mango/cms build`
- `pnpm --filter @mango/file build`
- `pnpm --filter @mango/grid-widgets build`
- `pnpm --filter @mango/job build`
- `pnpm --filter @mango/notice build`
- `pnpm --filter @mango/numgen build`
- `pnpm --filter @mango/payment build`
- `pnpm --filter @mango/template build`
- `pnpm --filter @mango/workflow build`
- `pnpm --filter @mango/workflow-business-example build`
- `pnpm --filter @mango/admin build`
- `pnpm --filter @mango/cli test`
- `pnpm --filter @mango/cli run check:release-versions`
- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`
- `pnpm package-consumer:typecheck -- --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `node mango-pmo/tools/audit-module-readmes.mjs`
- `node mango-pmo/tools/audit-readme-source-facts.mjs`
- `node mango-pmo/tools/check-business-guides.mjs`
- `PR_BODY_FILE=.release-pr-body.md node mango-pmo/tools/check-capability-docs.mjs --base origin/main --head HEAD`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/system --version=1.0.11 --tag=v2026.06.27-system-component-release --check-github-release`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/admin-pages --version=1.0.12 --tag=v2026.06.27-system-component-release --check-github-release`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/admin-shell --version=1.0.27 --tag=v2026.06.27-system-component-release --check-github-release`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/calendar --version=1.0.13 --tag=v2026.06.27-system-component-release --check-github-release`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/cms --version=1.0.2 --tag=v2026.06.27-system-component-release --check-github-release`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/file --version=1.0.13 --tag=v2026.06.27-system-component-release --check-github-release`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/grid-widgets --version=1.0.5 --tag=v2026.06.27-system-component-release --check-github-release`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/job --version=1.0.5 --tag=v2026.06.27-system-component-release --check-github-release`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/notice --version=1.0.14 --tag=v2026.06.27-system-component-release --check-github-release`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/numgen --version=1.0.13 --tag=v2026.06.27-system-component-release --check-github-release`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/payment --version=1.0.4 --tag=v2026.06.27-system-component-release --check-github-release`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/template --version=1.0.13 --tag=v2026.06.27-system-component-release --check-github-release`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/workflow --version=1.0.16 --tag=v2026.06.27-system-component-release --check-github-release`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/workflow-business-example --version=1.0.15 --tag=v2026.06.27-system-component-release --check-github-release`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/admin --version=1.0.31 --tag=v2026.06.27-system-component-release --check-github-release`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/cli --version=1.0.44 --tag=v2026.06.27-system-component-release --check-github-release`
- `git diff --check`

## v2026.06.26-notice-workflow-release - 2026-06-26

### New

- Added Notice announcement management for admin publishing and user-side announcement reading/confirmation. Announcement targets support all users, organizations, roles, and selected users, with recipient snapshots scoped by tenant.
- Added reusable `@mango/workflow` business approval detail UI components: `WorkflowLayout`, `WorkflowSidebar`, instance summary/progress, definition graph dialog, and business application history dialog.

### Fixed

- Kept the workflow "My Applications" page compatible with both business application records and directly started process instances. Status-filtered views still use business application records, while the default list also includes direct process instances and deduplicates rows by process instance ID.
- Scoped the workflow task detail approval action bar to the left content column. The buttons are centered under the approval content and stay sticky at the bottom of that column when the content scrolls, without extending below the right workflow sidebar.
- Synchronized the previously published frontend package release lock from `v2026.06.26-frontend-release-missing-widgets-system` back into `main` before this release so source package versions no longer lag npm-hosted.

### Published Packages

- npm: `@mango/notice@1.0.13` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/workflow@1.0.15` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/workflow-business-example@1.0.14` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/grid-widgets@1.0.4`, `@mango/admin-shell@1.0.26`, `@mango/admin@1.0.30`, and `@mango/cli@1.0.43` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- Maven: `mango-notice-api`, `mango-notice-core`, and `mango-notice-starter` on the `1.0.0-SNAPSHOT` line to `http://nexus.inner.yunxinbaokeji.com/repository/maven-snapshots/`.
- GitHub Release: `v2026.06.26-notice-workflow-release`.

### Upgrade Notes

- Backend consumers should refresh Mango `1.0.0-SNAPSHOT` dependencies and rerun Flyway migrations to receive Notice announcement tables and starter endpoints.
- Business frontends that consume the aggregate admin package should upgrade to `@mango/admin@1.0.30`.
- Business frontends that consume the admin shell directly should upgrade to `@mango/admin-shell@1.0.26`.
- Business frontends that embed Notice, Workflow, Workflow Business Example, or Grid Widgets directly should upgrade to `@mango/notice@1.0.13`, `@mango/workflow@1.0.15`, `@mango/workflow-business-example@1.0.14`, and `@mango/grid-widgets@1.0.4` together.
- New or regenerated business projects must use `@mango/cli@1.0.43` so generated frontend dependency locks include this release batch.

### Verification

- `pnpm install --lockfile-only`
- `pnpm --filter @mango/notice build`
- `pnpm --filter @mango/workflow build`
- `pnpm --filter @mango/workflow-business-example build`
- `pnpm --filter @mango/grid-widgets build`
- `pnpm --filter @mango/admin-shell build`
- `pnpm --filter @mango/admin build`
- `pnpm --filter @mango/cli test`
- `pnpm --filter @mango/cli run check:release-versions`
- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`
- `pnpm package-consumer:typecheck -- --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `mvn -f mango/pom.xml -pl :mango-notice-starter -am test`
- `scripts/publish-maven-batch.sh :mango-notice-api :mango-notice-core :mango-notice-starter --revision 1.0.0-SNAPSHOT`
- `node mango-pmo/tools/audit-module-readmes.mjs`
- `node mango-pmo/tools/audit-readme-source-facts.mjs`
- `node mango-pmo/tools/check-business-guides.mjs`
- `PR_BODY_FILE=.release-pr-body.md node mango-pmo/tools/check-capability-docs.mjs --base origin/main --head HEAD`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/notice --version=1.0.13 --tag=v2026.06.26-notice-workflow-release --check-github-release`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/workflow --version=1.0.15 --tag=v2026.06.26-notice-workflow-release --check-github-release`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/workflow-business-example --version=1.0.14 --tag=v2026.06.26-notice-workflow-release --check-github-release`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/grid-widgets --version=1.0.4 --tag=v2026.06.26-notice-workflow-release --check-github-release`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/admin-shell --version=1.0.26 --tag=v2026.06.26-notice-workflow-release --check-github-release`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/admin --version=1.0.30 --tag=v2026.06.26-notice-workflow-release --check-github-release`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/cli --version=1.0.43 --tag=v2026.06.26-notice-workflow-release --check-github-release`
- `git diff --check`

## v2026.06.26-frontend-release-missing-widgets-system - 2026-06-26

### Fixed

- Published the already-implemented workbench calendar widget in `@mango/grid-widgets@1.0.3`; the previous `@mango/grid-widgets@1.0.2` tarball did not contain `dist/calendar.js` or `dist/system/calendar/**`.
- Published the updated system configuration page and `SystemConfigPanel` in `@mango/system@1.0.10`; the previous `@mango/system@1.0.9` tarball did not contain the panel component.
- Updated the dependent frontend release batch, `@mango/cli@1.0.42`, and the business starter lock so generated and upgraded business projects resolve the fixed frontend package set.

### Published Packages

- npm: `@mango/grid-widgets@1.0.3` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/system@1.0.10` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/admin-pages@1.0.11` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/admin-shell@1.0.25` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/admin@1.0.29` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/calendar@1.0.12`, `@mango/cms@1.0.1`, `@mango/file@1.0.12`, `@mango/job@1.0.4`, `@mango/notice@1.0.12`, `@mango/numgen@1.0.12`, `@mango/payment@1.0.3`, `@mango/template@1.0.12`, `@mango/workflow@1.0.14`, and `@mango/workflow-business-example@1.0.13` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/cli@1.0.42` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- GitHub Release: `v2026.06.26-frontend-release-missing-widgets-system`.

### Upgrade Notes

- Business frontends that consume the aggregate admin package should upgrade to `@mango/admin@1.0.29`.
- Business frontends that consume the admin shell directly should upgrade to `@mango/admin-shell@1.0.25`.
- Business frontends that consume grid widgets directly should upgrade to `@mango/grid-widgets@1.0.3` and keep importing `@mango/grid-widgets/style.css`.
- Business frontends that consume system pages directly should upgrade to `@mango/system@1.0.10`.
- Business frontends that consume optional admin feature packages should upgrade the dependent package set together: `@mango/calendar@1.0.12`, `@mango/cms@1.0.1`, `@mango/file@1.0.12`, `@mango/job@1.0.4`, `@mango/notice@1.0.12`, `@mango/numgen@1.0.12`, `@mango/payment@1.0.3`, `@mango/template@1.0.12`, `@mango/workflow@1.0.14`, and `@mango/workflow-business-example@1.0.13`.
- New or regenerated business projects must use `@mango/cli@1.0.42` so generated frontend dependency locks include this release batch.

### Verification

- `gh issue create --repo HardyDou/mango ...` created Issue #264.
- `pnpm install --lockfile-only`
- `pnpm --filter @mango/grid-widgets build`
- `pnpm --filter @mango/system build`
- `pnpm --filter @mango/admin-pages build`
- `pnpm --filter @mango/admin-shell build`
- `pnpm --filter @mango/admin build`
- `pnpm --filter @mango/cli test`
- `pnpm --filter @mango/cli run check:release-versions`
- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`
- `npm pack @mango/grid-widgets@1.0.3 --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `npm pack @mango/system@1.0.10 --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`

## v2026.06.26-cms-demo-identity-security - 2026-06-26

### New

- Added identity security policy baseline for first-login forced password change, password complexity hints and validation, login-failure lockout, timed unlock behavior, and admin-side user unlock/reset actions.
- Added default workbench layout data so clean environments can show the expected admin home widgets without manual layout setup.
- Added CMS demo data for the help, enterprise, and demo public sites, including site settings, domains, categories, navigation, articles, publish relations, and advertisements.
- Registered local frontend app ports in workspace configuration so each worktree can run its own admin shell, CMS admin app, and public site apps through environment-driven ports.

### Fixed

- Aligned failed-login lockout handling with the identity security policy, including persisted locking for existing users and KV-backed tracking for nonexistent usernames.
- Fixed CMS demo seed ownership so the default admin data scope can see seeded site and category records.

### Published Packages

- npm: `@mango/common@1.0.11` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/auth@1.0.9` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/rbac@1.0.9` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/workflow@1.0.13` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/admin-shell@1.0.24` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/admin@1.0.28` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/cli@1.0.41` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- Maven: backend artifacts remain on the existing `1.0.0-SNAPSHOT` line.
- GitHub Release: `v2026.06.26-cms-demo-identity-security`.

### Upgrade Notes

- Backend consumers should refresh Mango `1.0.0-SNAPSHOT` dependencies and rerun Flyway migrations to receive identity security policy columns and CMS demo seed data.
- Business frontends that consume the aggregate admin package should upgrade to `@mango/admin@1.0.28`.
- Business frontends that consume the admin shell directly should upgrade to `@mango/admin-shell@1.0.24`.
- Business frontends that embed auth, RBAC, workflow, or common UI packages directly should upgrade to `@mango/auth@1.0.9`, `@mango/rbac@1.0.9`, `@mango/workflow@1.0.13`, and `@mango/common@1.0.11` together.
- New projects should use `@mango/cli@1.0.41` so generated dependency locks include this release batch.
- Business projects using local worktrees should rerun `scripts/dev-workspace.sh init` only when a workspace has no existing `.mango/dev-workspace.env`; existing workspaces keep their current port assignments.
- Public CMS demo apps now rely on seeded CMS domains for `127.0.0.1:5191`, `127.0.0.1:5192`, and `127.0.0.1:5193` when using the default main-workspace port set.

### Verification

- `git diff --check`
- `PR_BODY_FILE=/tmp/pr-261-body-current.md node mango-pmo/tools/check-capability-docs.mjs --base origin/main^1 --head origin/main`
- `node mango-ui/packages/mango-cli/src/index.mjs validate`
- `bash -n scripts/dev-workspace.sh`
- `mvn -f mango/pom.xml -pl :mango-auth-starter -am test`
- `mvn -f mango/pom.xml -pl :mango-identity-core -am test`
- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`
- `pnpm package-consumer:typecheck -- --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `node scripts/check-release-notes.mjs --package=@mango/common --version=1.0.11 --tag=v2026.06.26-cms-demo-identity-security --check-github-release`
- `node scripts/check-release-notes.mjs --package=@mango/auth --version=1.0.9 --tag=v2026.06.26-cms-demo-identity-security --check-github-release`
- `node scripts/check-release-notes.mjs --package=@mango/rbac --version=1.0.9 --tag=v2026.06.26-cms-demo-identity-security --check-github-release`
- `node scripts/check-release-notes.mjs --package=@mango/workflow --version=1.0.13 --tag=v2026.06.26-cms-demo-identity-security --check-github-release`
- `node scripts/check-release-notes.mjs --package=@mango/admin-shell --version=1.0.24 --tag=v2026.06.26-cms-demo-identity-security --check-github-release`
- `node scripts/check-release-notes.mjs --package=@mango/admin --version=1.0.28 --tag=v2026.06.26-cms-demo-identity-security --check-github-release`
- `node scripts/check-release-notes.mjs --package=@mango/cli --version=1.0.41 --tag=v2026.06.26-cms-demo-identity-security --check-github-release`
- CMS V10 Flyway seed SQL was executed repeatably against the local main-workspace database during PR verification.

## v2026.06.26-resource-identity-auth-domain - 2026-06-26

### New

- Added Resource Registry baseline declarations for authorization roles, role data scopes, subject-role bindings, organization units, posts, identity users, and member org/post bindings. This lets clean deployments or demo/bootstrap projects initialize RBAC, organization, post, and demo account baseline data through resource declarations instead of manual SQL. User password security policy enforcement remains out of scope and is tracked separately by Issue #250.
- Added a workbench calendar widget to `@mango/grid-widgets@1.0.2` and registered it in the admin shell home view.
- Improved the workbench user profile widget layout while keeping the existing `@mango/grid-widgets@1.0.2` package line.
- Added Workflow start entry visibility for business-embedded processes. Workflow definitions can now be marked as hidden from the approval center start-process list while remaining startable through business-context Workflow APIs.

### Fixed

- Injected the `AUTH` business domain from `mango-auth-starter` resource declarations so notification business configuration can group and filter authentication events.
- Updated `@mango/notice@1.0.11` business-domain selectors and notice pages to load enabled domains, filter message definitions by domain, and keep receive settings compatible with the existing business-type API.

### Published Packages

- No npm package version bump in this release. Source changes remain on the existing package versions: `@mango/grid-widgets@1.0.2` and `@mango/notice@1.0.11`.
- Maven: backend artifacts remain on the existing `1.0.0-SNAPSHOT` line.
- GitHub Release: `v2026.06.26-resource-identity-auth-domain`.

### Upgrade Notes

- Backend consumers should refresh Mango `1.0.0-SNAPSHOT` dependencies to receive Resource Registry handlers for authorization, organization, identity, and the AUTH business domain resource.
- Business projects that consume the existing admin source line should rebuild with `@mango/grid-widgets@1.0.2` and `@mango/notice@1.0.11`; no npm package version upgrade is required for this source release.
- Resource baseline deployments can now declare roles, role data scopes, subject-role bindings, org units, posts, identity users, and member org/post bindings through Resource Registry YAML.
- AUTH notification business types can be grouped under the `AUTH` domain after the resource sync writes `biz_domain.domain_code=AUTH`.

### Fixed

- Added identity security policy baseline for first-login forced password change, password complexity hints, login-failure lockout, and timed unlock behavior.
- Extended the user management backend with password reset, forced password reset, and unlock actions for locked users.
- Updated auth and identity validation so weak password changes do not consume forced-change tickets before validation succeeds.

### Verification

- `mvn -f mango/pom.xml -pl mango-platform/mango-workflow/mango-workflow-api -am -DskipTests install`
- `mvn -f mango/pom.xml -pl mango-platform/mango-workflow/mango-workflow-core -Dtest=WorkflowDefinitionServiceImplTest test`
- `pnpm -F @mango/workflow build`
- `mvn -pl :mango-resource-api,:mango-authorization-api,:mango-authorization-starter,:mango-org-starter,:mango-identity-starter -am test`
- `mvn -f mango/pom.xml -pl :mango-auth-starter -am test`
- `pnpm --filter @mango/grid-widgets build`
- `pnpm --filter @mango/notice build`
- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`
- `git diff --check`
- `PR_BODY_FILE=.pr-body.md node mango-pmo/tools/check-capability-docs.mjs --base origin/main --head HEAD`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/grid-widgets --version=1.0.2`
- `node mango-ui/scripts/check-release-notes.mjs --package=@mango/notice --version=1.0.11`

## v2026.06.26-pmo-batch-release-rules - 2026-06-26

### Fixed

- Added a PMO multi-package release gate requiring shared batch checks to run once before per-package publish actions.
- Added guarded `--skip-shared-gates` support to `pnpm publish:pkg` for releases that already completed shared package-consumer validation.
- Updated the published PMO baseline lock used by `@mango/cli`.

### Published Packages

- npm: `@mango/pmo@1.0.1` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/cli@1.0.40` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- GitHub Release: `v2026.06.26-pmo-batch-release-rules`.

### Upgrade Notes

- Existing business projects should upgrade to `@mango/cli@1.0.40` and run `mango pmo sync --project-dir .` to receive the multi-package release gate.
- Mango release agents must run shared release gates once per release batch, then publish each package with per-package build, registry verification, and tarball verification.

### Verification

- `git diff --check`
- `pnpm --filter @mango/pmo build`
- `pnpm --filter @mango/pmo check`
- `pnpm --filter @mango/cli test`
- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`
- `node scripts/check-release-notes.mjs --package=@mango/pmo --version=1.0.1`
- `node scripts/check-release-notes.mjs --package=@mango/cli --version=1.0.40`
- `pnpm package-consumer:typecheck -- --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `MANGO_SHARED_PUBLISH_GATES_PASSED=1 pnpm publish:pkg pmo --release-tag=v2026.06.26-pmo-batch-release-rules --skip-shared-gates`
- `MANGO_SHARED_PUBLISH_GATES_PASSED=1 pnpm publish:pkg cli --release-tag=v2026.06.26-pmo-batch-release-rules --skip-shared-gates`
- `npm view @mango/pmo@1.0.1 version --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `npm view @mango/cli@1.0.40 version --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`

## v2026.06.26-grid-widgets-style-main-release - 2026-06-26

### Fixed

- Published `@mango/grid-widgets@1.0.2` from main so the `./style.css` export ships the real widget CSS artifact.
- Kept npm publish validation for exported `style.css` content so empty JavaScript placeholder style artifacts fail publication.
- Updated `@mango/admin-shell@1.0.23`, `@mango/admin@1.0.27`, and `@mango/cli@1.0.39` locks so business projects resolve `@mango/grid-widgets@1.0.2`.

### Published Packages

- npm: `@mango/grid-widgets@1.0.2` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/admin-shell@1.0.23` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/admin@1.0.27` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/cli@1.0.39` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- GitHub Release: `v2026.06.26-grid-widgets-style-main-release`.

### Upgrade Notes

- Business frontends that depend on `@mango/admin` should upgrade to `@mango/admin@1.0.27`.
- Business frontends that depend on `@mango/admin-shell` directly should upgrade to `@mango/admin-shell@1.0.23`.
- Business frontends that depend on `@mango/grid-widgets` directly should upgrade to `@mango/grid-widgets@1.0.2` and keep importing `@mango/grid-widgets/style.css`.
- New projects should use `@mango/cli@1.0.39` so generated dependency locks include the fixed widget package.

### Verification

- `git diff --check`
- `pnpm --filter @mango/grid-widgets build`
- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`
- `pnpm --filter @mango/admin-shell build`
- `pnpm --filter @mango/admin build`
- `pnpm --filter @mango/cli test`
- `node scripts/check-release-notes.mjs --package=@mango/grid-widgets --version=1.0.2`
- `node scripts/check-release-notes.mjs --package=@mango/admin-shell --version=1.0.23`
- `node scripts/check-release-notes.mjs --package=@mango/admin --version=1.0.27`
- `node scripts/check-release-notes.mjs --package=@mango/cli --version=1.0.39`
- `pnpm publish:pkg grid-widgets --release-tag=v2026.06.26-grid-widgets-style-main-release`
- `pnpm publish:pkg admin-shell --release-tag=v2026.06.26-grid-widgets-style-main-release`
- `pnpm publish:pkg admin --release-tag=v2026.06.26-grid-widgets-style-main-release`
- `pnpm publish:pkg cli --release-tag=v2026.06.26-grid-widgets-style-main-release`
- `npm view @mango/grid-widgets@1.0.2 version --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `npm view @mango/admin-shell@1.0.23 version --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `npm view @mango/admin@1.0.27 version --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `npm view @mango/cli@1.0.39 version --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`

## v2026.06.25-cms-platform - 2026-06-25

### New

- Released the Mango CMS platform module with backend API/core/starter artifacts, Flyway migrations, tenant-aware admin APIs, public site APIs, CMS menu resources, and file-center based media handling.
- Added the `@mango/cms` admin package with site, category, content, publishing, navigation, advertisement, and delivery management pages, plus the `@mango/cms/admin-pages` and `@mango/cms/style.css` public entries.
- Added the `@mango/site-shell` frontend package for public CMS site rendering, including site resolution, navigation, category, advertisement, content listing, content detail, SEO, and public media URL helpers.
- Integrated CMS into the aggregate admin package, the admin shell micro-frontend runtime configuration, and the CLI full/custom module metadata so generated business projects can include CMS by default.

### Fixed

- Aligned CMS frontend package publication metadata so `@mango/cms` and `@mango/site-shell` publish `dist` artifacts without leaking repository `src` files.
- Bumped the aggregate admin package and CLI release lock so business consumers can resolve the newly published CMS package set from Nexus.

### Published Packages

- npm: `@mango/cms@1.0.0` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/site-shell@1.0.0` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/admin@1.0.26` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/cli@1.0.38` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- Maven: Mango backend artifacts remain on `1.0.0-SNAPSHOT`; CMS artifacts are published under `io.mango.platform.cms` to `http://nexus.inner.yunxinbaokeji.com/repository/maven-snapshots/`.
- GitHub Release: `v2026.06.25-cms-platform`.

### Upgrade Notes

- Backend consumers should refresh Mango `1.0.0-SNAPSHOT` dependencies and add `io.mango.platform.cms:mango-cms-starter` for local CMS deployment or `io.mango.platform.cms:mango-cms-starter-remote` for remote deployment.
- New or regenerated business projects should use `@mango/cli@1.0.38` so generated backend module metadata, admin module metadata, and release locks include CMS.
- Business frontends that consume the aggregate admin package should upgrade to `@mango/admin@1.0.26`; this resolves `@mango/cms@1.0.0` and keeps the generated admin style aggregation aligned.
- Business frontends that embed CMS admin pages directly should install `@mango/cms@1.0.0` and import `@mango/cms/style.css`.
- Public site frontends should install `@mango/site-shell@1.0.0` and call the CMS public APIs through the site shell helpers instead of hardcoding open CMS endpoints.

### Verification

- `git diff --check`
- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`
- `pnpm package-exports:check`
- `pnpm --filter @mango/cms build`
- `pnpm --filter @mango/site-shell build`
- `pnpm --filter @mango/admin build`
- `pnpm --filter @mango/cli test`
- `mvn -f mango/pom.xml -pl :mango-cms-core,:mango-cms-starter -am test`
- `node scripts/check-release-notes.mjs --package=@mango/cms --version=1.0.0`
- `node scripts/check-release-notes.mjs --package=@mango/site-shell --version=1.0.0`
- `node scripts/check-release-notes.mjs --package=@mango/admin --version=1.0.26`
- `node scripts/check-release-notes.mjs --package=@mango/cli --version=1.0.38`
- `scripts/publish-maven-module.sh mango-platform/mango-cms --also-make`
- `pnpm publish:pkg cms --release-tag=v2026.06.25-cms-platform`
- `pnpm publish:pkg site-shell --release-tag=v2026.06.25-cms-platform`
- `pnpm publish:pkg admin --release-tag=v2026.06.25-cms-platform`
- `pnpm publish:pkg cli --release-tag=v2026.06.25-cms-platform`
- `npm view @mango/cms@1.0.0 version --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `npm view @mango/site-shell@1.0.0 version --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `npm view @mango/admin@1.0.26 version --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `npm view @mango/cli@1.0.38 version --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`

## v2026.06.25-grid-widgets-style-artifact - 2026-06-25

### Fixed

- Fixed `@mango/grid-widgets@1.0.1` so the published `./style.css` export contains the real grid widget CSS instead of the invalid 11-byte `export {};` artifact.
- Added npm publish tarball validation for exported `style.css` content so empty or JavaScript placeholder style artifacts fail publication.
- Updated `@mango/admin-shell@1.0.22`, `@mango/admin@1.0.25`, and `@mango/cli@1.0.37` locks so business projects resolve `@mango/grid-widgets@1.0.1`.

### Published Packages

- npm: `@mango/grid-widgets@1.0.1` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/admin-shell@1.0.22` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/admin@1.0.25` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/cli@1.0.37` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- GitHub Release: `v2026.06.25-grid-widgets-style-artifact`.

### Upgrade Notes

- Business frontends that depend on `@mango/admin` should upgrade to `@mango/admin@1.0.25`.
- Business frontends that depend on `@mango/admin-shell` directly should upgrade to `@mango/admin-shell@1.0.22`.
- Business frontends that depend on `@mango/grid-widgets` directly should upgrade to `@mango/grid-widgets@1.0.1` and keep importing `@mango/grid-widgets/style.css`.
- New projects should use `@mango/cli@1.0.37` so generated dependency locks include the fixed widget package.

### Verification

- `pnpm --filter @mango/grid-widgets build`
- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`
- `pnpm --filter @mango/admin-shell build`
- `pnpm --filter @mango/admin build`
- `pnpm --filter @mango/cli test`
- `node scripts/check-release-notes.mjs --package=@mango/grid-widgets --version=1.0.1`
- `node scripts/check-release-notes.mjs --package=@mango/admin-shell --version=1.0.22`
- `node scripts/check-release-notes.mjs --package=@mango/admin --version=1.0.25`
- `node scripts/check-release-notes.mjs --package=@mango/cli --version=1.0.37`
- `npm pack @mango/grid-widgets@1.0.1 --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`

## v2026.06.25-workflow-return-path - 2026-06-25

### Fixed

- Added safe business return-path support to the Workflow standard task detail page. Business modules can pass `returnPath=/guarantee/risk/reviews` and optional `returnQuery=scope%3DTODO` so the top-level return button goes back to the originating business workspace.
- Reused the same safe business return target after task actions complete, so approve/reject/claim/unclaim no longer force business users into Workflow todo/done lists when a valid `returnPath` is present.
- Hardened `returnPath` validation to allow only same-site absolute paths and reject external URLs, protocol-relative URLs, empty values, query/hash-in-path values, backslashes, and control characters.
- Updated task-detail unit coverage for business return paths, unsafe URL fallback, legacy `from=initiated/done/todo` fallback, and post-action navigation.

### Published Packages

- npm: `@mango/workflow@1.0.12` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/workflow-business-example@1.0.12` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/grid-widgets@1.0.1` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/admin-shell@1.0.22` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/admin@1.0.25` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/cli@1.0.37` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- Maven: no backend artifact changes. Mango backend artifacts remain on the existing `1.0.0-SNAPSHOT` line.
- GitHub Release: `v2026.06.25-workflow-return-path`.

### Upgrade Notes

- Business frontends that enter Workflow task detail from a business workspace should pass a same-site `returnPath`, for example `/guarantee/risk/reviews`, and optional `returnQuery` for business tab state.
- Business frontends that consume Workflow directly should upgrade to `@mango/workflow@1.0.12`.
- Business frontends that consume the Workflow business example package should upgrade to `@mango/workflow-business-example@1.0.12`.
- Business frontends that consume grid widgets should upgrade to `@mango/grid-widgets@1.0.1` so widget dependencies resolve the updated Workflow package.
- Business frontends that consume the aggregate admin package should upgrade to `@mango/admin@1.0.25` so its dependency lock resolves `@mango/admin-shell@1.0.22` and `@mango/workflow@1.0.12`.
- New or regenerated business projects should use `@mango/cli@1.0.37` so the generated release locks include the updated Workflow/Admin package set.
- No backend dependency or database migration changes are required for this release.

### Verification

- `git diff --check`
- `node_modules/.pnpm/node_modules/.bin/vitest run packages/workflow/src/views/task-detail/__tests__/taskDetail.spec.ts --config .runtime/vitest-workflow-task-detail.config.ts`
- `pnpm -F @mango/workflow build`
- `pnpm -F @mango/workflow-business-example build`
- `pnpm -F @mango/grid-widgets build`
- `pnpm -F @mango/admin-shell build`
- `pnpm -F @mango/admin build`
- `pnpm --filter @mango/cli test`
- `node scripts/check-release-notes.mjs --package=@mango/workflow --version=1.0.12`
- `node scripts/check-release-notes.mjs --package=@mango/workflow-business-example --version=1.0.12`
- `node scripts/check-release-notes.mjs --package=@mango/grid-widgets --version=1.0.1`
- `node scripts/check-release-notes.mjs --package=@mango/admin-shell --version=1.0.22`
- `node scripts/check-release-notes.mjs --package=@mango/admin --version=1.0.25`
- `node scripts/check-release-notes.mjs --package=@mango/cli --version=1.0.37`
- `pnpm publish:pkg workflow --release-tag=v2026.06.25-workflow-return-path`
- `pnpm publish:pkg workflow-business-example --release-tag=v2026.06.25-workflow-return-path`
- `pnpm publish:pkg grid-widgets --release-tag=v2026.06.25-workflow-return-path`
- `pnpm publish:pkg admin-shell --release-tag=v2026.06.25-workflow-return-path`
- `pnpm publish:pkg admin --release-tag=v2026.06.25-workflow-return-path`
- `pnpm publish:pkg cli --release-tag=v2026.06.25-workflow-return-path`
- `npm view @mango/workflow@1.0.12 version --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `npm view @mango/workflow-business-example@1.0.12 version --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `npm view @mango/grid-widgets@1.0.1 version --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `npm view @mango/admin-shell@1.0.22 version --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `npm view @mango/admin@1.0.25 version --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `npm view @mango/cli@1.0.37 version --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`

## v2026.06.25-cli-pmo-resolution - 2026-06-25

### Fixed

- Published `@mango/cli@1.0.36` to fix PMO baseline package resolution in pnpm business projects. CLI PMO commands now resolve `@mango/pmo` through Node package resolution before falling back to the bundled CLI template baseline.
- Added CLI regression coverage for the published pnpm layout where `@mango/pmo` is installed beside `@mango/cli` under `.pnpm/.../node_modules/@mango`.

### Published Packages

- npm: `@mango/cli@1.0.36` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- GitHub Release: `v2026.06.25-cli-pmo-resolution`.

### Upgrade Notes

- New machines should install the CLI globally with `npm install -g @mango/cli@1.0.36 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- Existing business projects that synced PMO baseline with `@mango/cli@1.0.35` should upgrade the CLI and rerun `mango pmo sync --project-dir .` so `business-pmo/mango-baseline` is compared against `@mango/pmo@1.0.0`.
- If `mango pmo check` reports only baseline README drift after this upgrade, rerun `mango pmo sync --project-dir .` to rewrite the baseline snapshot from `@mango/pmo`.

### Verification

- `pnpm --filter @mango/cli test`
- `pnpm --filter @mango/pmo check`
- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`
- `node scripts/check-release-notes.mjs --package=@mango/cli --version=1.0.36`
- `node mango-ui/packages/mango-cli/src/index.mjs pmo status --project-dir <business-project-root>`
- `pnpm publish:pkg cli --dry-run`

## v2026.06.24-admin-shell-footer-layout - 2026-06-24

### Fixed

- Fixed the Mango admin shell footer layout so edge footer modes align with shell layouts and the main content keeps the correct safe bottom spacing.
- Updated the Mango admin aggregate package dependency lock so business projects that consume `@mango/admin` receive the new admin shell package version.

### Published Packages

- npm: `@mango/admin-shell@1.0.21` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/admin@1.0.24` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- Maven: no backend artifact changes. Mango backend artifacts remain on the existing `1.0.0-SNAPSHOT` line.
- GitHub Release: `v2026.06.24-admin-shell-footer-layout`.

### Upgrade Notes

- Business frontends that depend on `@mango/admin-shell` directly should upgrade to `@mango/admin-shell@1.0.21`.
- Business frontends that consume the aggregate admin package should upgrade to `@mango/admin@1.0.24` so its dependency lock resolves `@mango/admin-shell@1.0.21`.
- No backend dependency or database migration changes are required for this release.

### Verification

- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`
- `pnpm --filter @mango/admin-shell build`
- `pnpm --filter @mango/admin build`
- `node scripts/check-release-notes.mjs --package=@mango/admin-shell --version=1.0.21`
- `node scripts/check-release-notes.mjs --package=@mango/admin --version=1.0.24`
- `npm view @mango/admin-shell@1.0.21 version --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `npm view @mango/admin@1.0.24 version --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`

## v2026.06.24-mango-governance-local-repo - 2026-06-24

### New

- Published the Mango governance baseline as `@mango/pmo@1.0.0` so business projects can consume PMO rules, agents, templates, and preflight tools from the internal npm registry.
- Published `@mango/cli@1.0.35` so project creation, historical project upgrades, and PMO baseline synchronization use the versioned `@mango/pmo` package.
- Published `@mango/grid-widgets@1.0.0` to complete the CLI release lock for business frontend project installation.
- Added release notes for the local repository publication flow that maps npm, Maven, and GitHub Release records to the same release tag.

### Published Packages

- npm: `@mango/pmo@1.0.0` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/cli@1.0.35` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/grid-widgets@1.0.0` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- Maven: Mango backend artifacts remain on `1.0.0-SNAPSHOT` and are published to `http://nexus.inner.yunxinbaokeji.com/repository/maven-snapshots/`.
- GitHub Release: `v2026.06.24-mango-governance-local-repo`.

### Upgrade Notes

- New machines should install the CLI globally with `npm install -g @mango/cli@1.0.35 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- Existing business projects should run `mango pmo upgrade --project-dir .` with the upgraded CLI, then run `cd frontend && pnpm install` so the project-local CLI and `@mango/pmo` dependency are locked.
- Daily business development should continue to use `scripts/dev-workspace.sh`; that script prefers the project-local CLI and only falls back to the global `mango` command before dependencies are installed.
- Backend consumers should refresh Mango `1.0.0-SNAPSHOT` dependencies from the internal Maven group repository after the Maven publication completes.

### Verification

- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`
- `pnpm --filter @mango/pmo build`
- `pnpm --filter @mango/pmo check`
- `pnpm --filter @mango/cli test`
- `node scripts/check-release-notes.mjs --package=@mango/pmo --version=1.0.0`
- `node scripts/check-release-notes.mjs --package=@mango/grid-widgets --version=1.0.0`
- `node scripts/check-release-notes.mjs --package=@mango/cli --version=1.0.35`
- `mvn -f mango/pom.xml -Drevision=1.0.0-SNAPSHOT -DskipTests deploy`
- `npm view @mango/pmo@1.0.0 version --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `npm view @mango/grid-widgets@1.0.0 version --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `npm view @mango/cli@1.0.35 version --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`

## Unreleased

## v2026.06.23-business-docs-export - 2026-06-23

### New

- Added public documentation site entry points for business-facing product document output,
  including the PRD template, detailed design template, delivery contract template, PRD template
  rules, detailed design template rules, and Sprint rules.
- Added a dedicated docs sidebar group for product document output so business developers can find
  PRD, design, and delivery contract assets without browsing internal PMO folders.
- Added the business docs export release plan and delivery ledger for this release.

### Fixed

- Exposed frontend runtime resource type constants from `mango-resource-api` via
  `ResourceTypes.FRONTEND_APP_REGISTRY` and
  `ResourceTypes.FRONTEND_MODULE_RUNTIME_STRATEGY`, and kept authorization
  resource type aliases aligned with the shared Resource Registry API constants.
- Fixed Mango frontend npm package boundaries so non-CLI `@mango/*` packages publish `dist`
  declarations and runtime artifacts instead of repository `src` or other source directories.
- Added package export and generated business consumer typecheck gates to prevent published
  frontend packages from leaking source files or missing exported declaration files.
- Aligned `@form-create/element-ui` usage in the workflow and system packages to `3.2.42`
  so workflow package consumption does not resolve conflicting form-create type versions.
- Exposed the detailed design template and detailed design template rules in the public docs staging
  whitelist so the docs build can publish both PRD and design assets together.

### Documentation

- Added AI-ready PRD and detailed design templates for business requirements, menu/page prototypes,
  business rules, PRD traceability, implementation mapping, interface/data/permission design, and
  acceptance mapping.
- Updated product documentation rules so PRD remains business-facing while detailed design carries
  technical decisions, interface contracts, data changes, permissions, state machines, and
  verification mapping.

### Published Packages

- No Maven artifact version changes. Backend artifacts remain on the Mango `1.0.0-SNAPSHOT` line.
- No npm package version changes. Frontend package versions remain unchanged.
- Published release object: Mango docs site source, platform changelog, annotated Git tag, and
  GitHub Release notes for `v2026.06.23-business-docs-export`.

### Upgrade Notes

- Business developers should use the Mango docs site “产品文档输出” entry to copy or reference:
  - `mango-pmo/templates/prd.md`
  - `mango-pmo/templates/detailed-design.md`
  - `mango-pmo/templates/delivery-contract.md`
  - `mango-pmo/rules/product/01-prd-template.md`
  - `mango-pmo/rules/product/03-detailed-design-template.md`
- Existing Mango runtime consumers do not need to change dependencies for this release.
- Existing published npm versions are immutable. Before publishing this fix, bump the affected
  `@mango/*` frontend package versions and publish new versions through `pnpm publish:pkg`.
- Business frontends should upgrade to the newly published Mango frontend package set after
  publication, then rerun their project typecheck.

### Verification

- `git diff --check`
- `npm --prefix mango-docs run docs:stage`
- `npm --prefix mango-docs run docs:build`
- `node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/plans/2026-06-23-business-docs-export-release-plan.md --ledger mango-docs/plans/2026-06-23-business-docs-export-release-ledger.md --mode verify`
- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`
- `pnpm -r --filter './packages/*' --filter '!@mango/cli' --if-present run build`
- `pnpm package-exports:check`
- `pnpm package-consumer:typecheck -- --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/ --keep-temp`
- `mvn -f mango/pom.xml -pl :mango-authorization-api,:mango-authorization-starter mango:check -Drule=dependency`
- `mvn -f mango/pom.xml -pl :mango-resource-api,:mango-authorization-api,:mango-authorization-starter -am test`
- `mvn -f mango/pom.xml -pl :mango-resource-api,:mango-authorization-api,:mango-authorization-starter -am -DskipTests package`

## v2026.06.21-frontend-runtime-resource-registry - 2026-06-21

### New

- Added Resource Registry handlers for authorization frontend runtime declarations:
  `FRONTEND_APP_REGISTRY` writes frontend runtime units to
  `authorization_frontend_app_registry`, and `FRONTEND_MODULE_RUNTIME_STRATEGY`
  writes module runtime routing rules to `authorization_frontend_module_runtime_strategy`.
- Added runtime descriptor support so authorization can return the current deploy profile,
  accessible frontend runtime units, and active module runtime strategies for the requesting
  subject.
- Added integration coverage for the full declaration flow from Resource Registry sync through
  authorization runtime tables and `runtimeDescriptor`.

### Fixed

- Rebased the pre-release authorization frontend runtime table names into the
  `authorization_*` namespace and marked the affected Flyway SQL files with
  `REBASE_REQUIRED(issue-204)`.
- Split authorization app metadata from frontend runtime configuration so `authorization_app`
  keeps authorization-domain fields while frontend runtime fields are read from the dedicated
  frontend runtime registry table.

### Upgrade Notes

- This is a breaking pre-1.0 database rebase. Development and test databases that already applied
  the previous local frontend runtime migrations must be rebuilt from a clean schema; do not use
  Flyway repair as a substitute for rebuilding those local databases.
- Frontend runtime declarations should use `FRONTEND_APP_REGISTRY` and
  `FRONTEND_MODULE_RUNTIME_STRATEGY` resources instead of seeding these runtime rows manually.

### Published Packages

- Backend Maven artifacts remain on the Mango `1.0.0-SNAPSHOT` line. Consumers should refresh the
  authorization API/core/starter artifacts and their required upstream SNAPSHOT dependencies after
  publication.

### Verification

- `git diff --check`
- `mvn -f mango/pom.xml -pl :mango-authorization-starter -am -Dtest=FrontendRuntimeResourceSyncIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f mango/pom.xml -pl :mango-authorization-starter -am test -DskipITs`
- `mvn -f mango/pom.xml -pl :mango-authorization-core -am test -DskipITs`
- `mvn -f mango/pom.xml -pl :mango-resource-core -am test -DskipITs`
- `mvn -f mango/pom.xml -pl :mango-resource-sync-starter -am test -DskipITs`

## v2026.06.21-resource-registry-runtime-baseline - 2026-06-21

### New

- Added the Issue #186 runtime validation baseline for Resource Registry, including monolith
  startup, capability-app Nacos configuration, Docker/Nacos helper assets, and admin E2E coverage
  for menus, permissions, notifications, workflow, tenant, system, template, realtime, and platform
  metadata isolation.
- Added Nacos-ready `application-nacos.yml` entries for microservice and platform capability apps so
  independent deployment can resolve service registration and remote Resource Registry wiring from
  environment variables.
- Added runtime evidence for Resource Registry synchronization, `AUTH_MENU` consumption, `API_RESOURCE`
  injection, clean-database rebuild, and menu/permission E2E acceptance.

### Fixed

- Fixed dynamic Feign target preservation so remote Resource Registry and module-based internal
  calls keep runtime target service resolution instead of losing the module target URI.
- Fixed `system:area:*` menu permission package inheritance by removing explicit empty
  `packageCodes`, allowing the permissions to inherit their parent menu package as documented.
- Fixed template preview failure handling so backend render errors are surfaced as a failed render
  result instead of leaving the page without an actionable error state.
- Aligned admin E2E tests with the Resource Registry menu baseline, current realtime protocol,
  current tenant provisioning contract, notification center flow, and platform metadata isolation.

### Published Packages

- Backend Maven artifacts remain on the Mango `1.0.0-SNAPSHOT` line. Business backends should
  refresh the SNAPSHOT dependencies for the updated Resource Registry runtime, authorization
  resource sync, gateway resource sync, infra Feign, admin starter, platform capability apps, and
  affected platform starters.
- No frontend npm package version was changed in this release. Consumers can keep the package set
  from `v2026.06.19-resource-registry` while applying the backend/runtime upgrade notes below.

### Upgrade Notes

- This is a breaking pre-1.0 upgrade for menu and default resource initialization. Development and
  test databases that contain Flyway-seeded menus must be backed up and rebuilt from a clean schema;
  do not repair menus, role-menu bindings, menu package items, or frontend menu runtime config with
  ad hoc SQL.
- Functional modules must publish menus and button permissions through
  `META-INF/mango/resources/{module}-common-menu.{json,yml,yaml}` as `AUTH_MENU` declarations.
  Flyway migration files may keep DDL and immutable base records, but must not seed menus, button
  permissions, menu package items, role-menu bindings, or frontend menu runtime config.
- Business monolith deployments should use `mango-admin-starter`, which includes the local Resource
  Registry runtime. Custom monolith aggregations must include `mango-resource-starter` and
  `mango-resource-sync-starter`.
- Microservice or capability-app deployments that only report declarations must include
  `mango-resource-starter-remote` and `mango-resource-sync-starter`; the Resource capability app
  hosts the registry and target dispatch.
- Menu resources are idempotent by `appCode + moduleCode + menuCode`. `packageCodes` and `roleCodes`
  inherit from the parent menu or declaration when omitted; an explicit empty array means no package
  or role binding.
- `DEPRECATED` resources remain readable and only update registry state; `DISABLED` disables target
  resources; `REMOVED` deletes when the target handler supports physical deletion.

### Verification

- `git diff --check`
- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`
- `pnpm -F @mango/template build`
- `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=false PLAYWRIGHT_BASE_URL=http://127.0.0.1:8510 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18820 pnpm exec playwright test ... --project=chromium --workers=1 --reporter=line --timeout=240000` (`26 passed`)
- `GET http://127.0.0.1:18820/actuator/health` returned `UP`
- Anonymous `GET /authorization/menus/user?fmt=tree&appCode=internal-admin` returned `401`
- Authenticated `/auth/info` included `system:area:add`, `system:area:delete`,
  `system:area:edit`, and `system:area:query`

## v2026.06.19-resource-registry - 2026-06-19

### New

- Added the Mango resource registry backend capability on the `1.0.0-SNAPSHOT` line, including
  resource API, support, core, starter, remote starter, sync starter, admin query endpoints,
  change logs, sync logs, file-based declaration loading, content hashing, force sync, and
  physical delete support.
- Migrated platform seed data to resource declarations for system dictionaries and config,
  domains, file storage settings, job definitions, notice channels and message templates, numgen
  sequence rules, payment rules, auth/identity/payment/job message templates, i18n messages, and
  API access resources.
- Added resource-backed notice and i18n registration so starters can publish reusable default
  platform resources through `META-INF/mango/resources`.
- Added button display rule support across backend authorization/auth contracts and frontend RBAC
  pages, including authorization snapshot output and RBAC role/menu UI integration (by
  @chengkuankuan).
- Added a frontend package consumer type gate with `pnpm package-consumer:typecheck` so published
  `@mango/*` packages are checked in a generated business consumer before npm publish.

### Fixed

- Fixed Mango Flyway upgrade compatibility for legacy business databases that already contain
  later-versioned module migrations.
- Tightened RBAC button display rule evaluation so hidden buttons are consistently filtered by the
  shared frontend authorization utility.
- Synchronized `@mango/cli` release locks with the current admin package set so newly generated
  projects consume the released frontend package versions.

### Documentation

- Added the resource registry design, delivery contract, module README coverage, capability map
  entry, and business integration impact notes.
- Updated business integration guides for permission button display rules, file upload forms,
  RBAC troubleshooting, tenant dict/config initialization, and workflow approval impacts.

### Published Packages

- Backend Maven artifacts remain on the Mango `1.0.0-SNAPSHOT` line, including the new resource
  modules and updated platform starters:
  - `io.mango.platform.resource:mango-resource-api`
  - `io.mango.platform.resource:mango-resource-support`
  - `io.mango.platform.resource:mango-resource-core`
  - `io.mango.platform.resource:mango-resource-starter`
  - `io.mango.platform.resource:mango-resource-starter-remote`
  - `io.mango.platform.resource:mango-resource-sync-starter`
  - `io.mango.platform.authorization:mango-authorization-resource-sync-starter`
  - Updated auth, authorization, system, domain, file, job, notice, numgen, payment, identity,
    workflow, template, and persistence modules on the same SNAPSHOT line.
- Frontend npm packages:
  - `@mango/admin@1.0.23`
  - `@mango/admin-pages@1.0.10`
  - `@mango/admin-shell@1.0.20`
  - `@mango/auth@1.0.8`
  - `@mango/calendar@1.0.11`
  - `@mango/common@1.0.10`
  - `@mango/file@1.0.11`
  - `@mango/grid-layout@1.0.2`
  - `@mango/job@1.0.3`
  - `@mango/notice@1.0.11`
  - `@mango/numgen@1.0.11`
  - `@mango/payment@1.0.2`
  - `@mango/rbac@1.0.8`
  - `@mango/system@1.0.9`
  - `@mango/template@1.0.11`
  - `@mango/workflow@1.0.11`
  - `@mango/workflow-business-example@1.0.11`
  - `@mango/cli@1.0.34`

### Upgrade Notes

- Business backends should refresh Mango backend `1.0.0-SNAPSHOT` dependencies and run the new
  Flyway migrations before starting applications that consume resource-backed default data.
- Applications that rely on platform default dictionaries, domains, file storage, jobs, notices,
  numgen, payment, auth templates, or i18n resources should keep the corresponding starters enabled
  so `META-INF/mango/resources` declarations can be synced.
- Existing databases keep their historical records; resource declarations become the managed
  source for default data and support sync/change logging through the resource registry tables.
- Frontend consumers should upgrade the published `@mango/*` package set together, especially
  `@mango/admin`, `@mango/admin-shell`, `@mango/common`, `@mango/auth`, and `@mango/rbac`.
- Upgrade `@mango/cli` to `1.0.34` before generating new business projects so generated dependency
  locks and backend resource sync configuration match this release.

### Verification

- `git diff --check`
- `node mango-pmo/tools/audit-module-readmes.mjs`
- `PR_BODY_FILE=.runtime/pr-193-body.md node mango-pmo/tools/check-capability-docs.mjs --base origin/main --head HEAD`
- `mvn -pl mango-platform/mango-resource/... -am test`
- `mvn -pl mango-platform/mango-system/mango-system-core,...,mango-workflow-core -am test`
- `mvn -f mango/pom.xml -pl :mango-infra-persistence-starter -am test`
- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`
- `pnpm package-consumer:typecheck`
- `pnpm --filter @mango/cli test`

## v2026.06.19-datascope-provider-autoconfig - 2026-06-19

### Fixed

- Fixed the authorization data-scope provider registration so `DataScopeProvider` is declared by
  `AuthorizationAutoConfiguration` as an explicit auto-configuration bean instead of relying on
  component scanning.
- Fixed the business startup failure where `mango-infra-persistence-starter` could evaluate
  `@ConditionalOnBean(DataScopeProvider.class)` before the authorization provider bean definition
  was visible, preventing `DataScopeApplier` from being created.
- Confirmed this is not caused by business MyBatis-Plus usage and does not require a business-side
  fallback `DataScopeApplier` bean.

### Published Packages

- Backend Maven artifacts remain on the Mango `1.0.0-SNAPSHOT` line, including:
  - `io.mango.platform.authorization:mango-authorization-core`
  - `io.mango.platform.authorization:mango-authorization-starter`
- No npm package publish is required for this backend-only hotfix.

### Upgrade Notes

- Business backends should refresh Mango backend `1.0.0-SNAPSHOT` dependencies after the release,
  especially `mango-authorization-starter`.
- No database migration, HTTP API change, frontend package upgrade, or business code workaround is
  required.

### Verification

- `mvn -f mango/pom.xml -pl :mango-authorization-starter -am test checkstyle:check`
- `mvn -f mango/pom.xml -pl :mango-infra-persistence-starter -am test checkstyle:check`

## v2026.06.18-data-scope-applier - 2026-06-18

### Fixed

- Fixed the startup failure where business applications that import both Mango persistence and authorization starters could not inject `DataScopeApplier`.
- Ordered persistence auto-configuration after the authorization starter without adding a direct module dependency, so authorization-provided `DataScopeProvider` beans are visible when the persistence starter creates `DataScopeApplier`.
- Kept `DataScopeApplier` conditional on an available `DataScopeProvider`, preserving applications that do not enable data-scope integration.
- Fixed the authorization app service generic CRUD contract so the authorization starter aggregation compiles with the typed Mango persistence API.

### Documentation

- Added the Issue 178 delivery contract, verification ledger, and business integration impact notes for permission button and RBAC menu troubleshooting guides.

### Published Packages

- Backend Maven artifacts remain on the Mango `1.0.0-SNAPSHOT` line, including:
  - `io.mango.infra.persistence:mango-infra-persistence-starter`
  - `io.mango.platform.authorization:mango-authorization-core`
  - `io.mango.platform.authorization:mango-authorization-starter`

### Upgrade Notes

- Refresh Mango backend `1.0.0-SNAPSHOT` dependencies after the release before starting business applications that combine persistence data-scope and authorization modules.
- No database migration, HTTP API, frontend package, menu, or permission-code change is required for this fix.

### Verification

- `node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/plans/2026-06-18-issue-178-data-scope-applier.md --ledger mango-docs/plans/2026-06-18-issue-178-data-scope-applier.md --mode verify`
- `git diff --check origin/main...HEAD`
- `mvn -f mango/pom.xml -pl :mango-infra-persistence-starter -am test checkstyle:check`
- `mvn -f mango/pom.xml -pl :mango-authorization-starter -am test`

## v2026.06.18-persistence-baseline-docs - 2026-06-18

### Fixed

- Enforced the Mango persistence baseline for generated business modules: generated services now extend typed `MangoCrudService<SealEntity>` and `MangoCrudServiceImpl<SealMapper, SealEntity>` instead of falling back to raw or MyBatis-Plus service contracts.
- Added Mango check coverage for common business persistence violations, including direct JDBC access, annotation SQL, raw MyBatis-Plus pagination, manual tenant assignment, and ad hoc data-scope conditions.
- Fixed the public `MangoCrudService` API contract to be entity-generic so generated business services compile against the published persistence API.

### Documentation

- Added the Persistence README examples for tenant isolation, data permission, standard pagination, and Mapper XML join queries.
- Added business module README templates that point developers to Mango capability docs, module README files, PMO baseline rules, and troubleshooting entries.
- Clarified that Maven runtime jars do not carry module README documentation; business teams should use the Mango docs site or a version-matched documentation snapshot. npm packages continue to include package-root README files.
- Updated Mango docs staging so package README files can be exposed through the documentation site.
- Tightened the capability documentation governance rule so PR authors must align template README links, PMO rule index updates, business integration impact notes, and PR body evidence before publishing a PR.

### Published Packages

- Backend Maven artifacts remain on the Mango `1.0.0-SNAPSHOT` line, including:
  - `io.mango.infra.persistence:mango-infra-persistence-api`
  - `io.mango.infra.persistence:mango-infra-persistence-starter`
  - `io.mango.infra.persistence:mango-infra-persistence-web-starter`
  - `io.mango.tools.maven.plugin:mango-maven-plugin`
- Frontend package metadata was prepared so published npm packages include `README.md`, including `@mango/admin`, `@mango/admin-pages`, `@mango/api-schema`, `@mango/app-runtime`, and existing module packages.
- `@mango/cli` templates were updated for generated business module README and persistence baseline checks.

### Upgrade Notes

- Refresh Mango backend `1.0.0-SNAPSHOT` dependencies before generating or compiling new business CRUD modules that use typed `MangoCrudService<E>`.
- Upgrade business starter or `@mango/cli` before creating new modules so generated migrations contain `tenant_id`, `org_id`, and audit fields, and generated services stay on the Mango CRUD baseline.
- Business developers should read the Mango capability map and module README before using persistence, authorization, admin pages, or frontend package capabilities. For offline development, distribute a documentation snapshot that matches the dependency version.

### Verification

- `mvn -f backend/pom.xml -pl modules/contract/contract-core -am -Dtest=ContractPersistenceRuntimeAcceptanceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f mango/pom.xml -pl mango-tools/mango-maven-plugin -Dtest=GenCrudMojoTest,CheckMojoTest test`
- `mvn -f mango/pom.xml -pl mango-infra/mango-infra-persistence/mango-infra-persistence-starter,mango-infra/mango-infra-persistence/mango-infra-persistence-web-starter -am test`
- `node mango-business-starter/scripts/check-template.mjs`
- `node mango-ui/packages/mango-cli/scripts/check-cli.mjs`

## v2026.06.18-admin-style-config-fix - 2026-06-18

### Fixed

- Fixed the `@mango/admin@1.0.21` regression where `@mango/payment/style.css` was pulled into the default `@mango/admin/style.css` aggregation and compressed non-payment search/select controls.
- Moved admin module style aggregation to `admin-modules.json` as the single source for default packages, full packages, registrars, and CLI governance checks.
- Regenerated `admin-packages.json`, `generated-package-styles.css`, `style-full.css`, and `@mango/admin/full` from the same module manifest.
- Scoped `@mango/payment/style.css` selectors so payment toolbar/table/form rules do not leak into unrelated admin pages.

### Published Packages

- `@mango/admin@1.0.22`

### Upgrade Notes

- Do not use `@mango/admin@1.0.21`; upgrade to `@mango/admin@1.0.22`.
- Consumers using the default admin preset should keep `import '@mango/admin/style.css'`; payment styles are no longer loaded unless using `@mango/admin/style-full.css` or importing `@mango/payment/style.css` explicitly.
- Full preset consumers should keep installing the optional full packages they enable, including `@mango/payment` when payment pages are registered.

### Verification

- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`
- `pnpm -F @mango/admin build`
- `pnpm -F @mango/payment build`
- `pnpm -F mango-admin build`
- Mango Admin browser verification on `http://127.0.0.1:7795/`

## v2026.06.18-admin-style-dependency-fix - 2026-06-18

### Fixed

- Fixed `@mango/admin/style.css` package consumption by moving the packages it imports by default from optional peers to direct dependencies.
- Prevented Vite/PostCSS failures where consumers without optional admin modules installed saw unresolved `@mango/grid-layout/style.css`, `@mango/job/style.css`, or `@mango/payment/style.css` imports.
- Superseded by `@mango/admin@1.0.22`; `1.0.21` must not be used because it loaded payment styles in the default admin preset and caused admin UI regressions.

### Published Packages

- `@mango/admin@1.0.21`

### Upgrade Notes

- Frontend consumers affected by `@mango/admin/style.css` resolution errors should upgrade `@mango/admin` to `1.0.21`.
- No API or import-path migration is required; continue using `import '@mango/admin/style.css'`.

### Verification

- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`
- `pnpm -F @mango/admin build`
- `.runtime/admin-style-consumer: pnpm install --lockfile=false --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/ && pnpm build`
- `pnpm publish:pkg admin --dry-run --release-tag=v2026.06.18-admin-style-dependency-fix`
- `npm pack @mango/admin@1.0.20 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/ --pack-destination .runtime/npm-pack-check`

## v2026.06.18-role-data-scope - 2026-06-18

### New

- Added role data scope support across Authorization, Persistence, RBAC, and Workflow, including role data scope APIs, persistence `DataScopeApplier`, Flyway migrations, role-page configuration, and workflow definition list integration.
- Added role authorization button-node visibility in the RBAC authorization dialog so operators can verify assignable button permissions from the role page.
- Added the shared `MangoDialog` component in `@mango/common` and migrated the app management dialog to the shared shell.
- Updated business integration guides and capability docs with role data scope impact notes and acceptance evidence.

### Fixed

- Compacted the role data scope selector interaction on the RBAC role page.
- Tightened worktree reuse guidance for PR gate and CI rework.

### Published Packages

- `@mango/common@1.0.9`
- `@mango/rbac@1.0.7`
- `@mango/admin-shell@1.0.19`
- `@mango/admin@1.0.20`
- `@mango/admin-pages@1.0.9`
- `@mango/auth@1.0.7`
- `@mango/calendar@1.0.10`
- `@mango/file@1.0.10`
- `@mango/grid-layout@1.0.1`
- `@mango/job@1.0.2`
- `@mango/notice@1.0.10`
- `@mango/numgen@1.0.10`
- `@mango/payment@1.0.1`
- `@mango/system@1.0.8`
- `@mango/template@1.0.10`
- `@mango/workflow@1.0.10`
- `@mango/workflow-business-example@1.0.10`
- `@mango/cli@1.0.33`
- Backend Maven artifacts remain on the Mango `1.0.0-SNAPSHOT` line, including:
  - `io.mango.infra.persistence:mango-infra-persistence-api`
  - `io.mango.infra.persistence:mango-infra-persistence-starter`
  - `io.mango.platform.authorization:mango-authorization-api`
  - `io.mango.platform.authorization:mango-authorization-core`
  - `io.mango.platform.authorization:mango-authorization-starter`
  - `io.mango.platform.workflow:mango-workflow-core`

### Upgrade Notes

- Existing business projects should refresh backend Mango `1.0.0-SNAPSHOT` dependencies from the Maven repository and run the new authorization, domain, and job Flyway migrations before enabling role data scope.
- Frontend consumers should upgrade `@mango/admin@1.0.20`, `@mango/admin-shell@1.0.19`, `@mango/common@1.0.9`, `@mango/rbac@1.0.7`, and the dependent `@mango/*` packages listed in Published Packages together.
- Upgrade `@mango/cli` to `1.0.33` before creating new business projects so generated dependency locks include the role data scope package set.
- Business queries only receive data scope filtering after they explicitly integrate `DataScopeApplier`; XML, JOIN, and statistical SQL paths should pass alias-aware field mappings and keep fail-fast validation.

### Verification

- `node mango-pmo/tools/acceptance-evidence-check.mjs --evidence mango-docs/evidence/2026-06-17-role-data-scope/acceptance-evidence.md`
- `node mango-pmo/tools/audit-module-readmes.mjs`
- `node mango-pmo/tools/audit-readme-source-facts.mjs`
- `node mango-pmo/tools/check-capability-docs.mjs --base origin/main --head HEAD`
- `mvn -pl mango-platform/mango-authorization/mango-authorization-api,mango-platform/mango-authorization/mango-authorization-core,mango-platform/mango-authorization/mango-authorization-starter -am -Dtest=RoleDataScopeServiceImplTest,AuthorizationDataScopeProviderTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl mango-infra/mango-infra-persistence/mango-infra-persistence-starter -Dtest=MybatisPlusDataScopeApplierTest test`
- `mvn -pl mango-platform/mango-workflow/mango-workflow-core -Dtest=WorkflowDefinitionServiceImplTest test`
- `pnpm -F @mango/common exec vitest run components/MangoDialog/__tests__/MangoDialog.spec.ts`
- `pnpm -F @mango/common build`
- `pnpm -F @mango/rbac build`
- `pnpm -F @mango/admin-shell build`
- `pnpm -F @mango/admin build`
- `pnpm -F @mango/cli test`
- `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true pnpm -F mango-admin exec playwright test --config playwright.config.ts e2e/specs/role-data-scope.spec.ts --project=chromium --reporter=list`

## v2026.06.17-grid-layout-workbench - 2026-06-17

### New

- Added custom Workbench grid layout support, including edit mode, widget removal, save, reset to default, refresh persistence, and per-user layout APIs.
- Added the `@mango/grid-layout@1.0.0` frontend package with reusable grid layout components, designer APIs, styles, and usage documentation.
- Added backend Grid Layout Maven modules on the Mango `1.0.0-SNAPSHOT` line for personal layout persistence.
- Updated generated admin projects to lock and install `@mango/grid-layout@1.0.0` with the refreshed admin package set.

### Fixed

- Completed `@mango/admin-shell` public README contract coverage for feature registrars, runtime modules, menu contract, theme, i18n, directives, migration guidance, and compatibility.
- Bumped admin package versions so the new workbench layout dependency can be published without overwriting existing npm versions.

### Published Packages

- `@mango/grid-layout@1.0.0`
- `@mango/admin-shell@1.0.18`
- `@mango/admin@1.0.19`
- `@mango/cli@1.0.32`
- Backend Maven artifacts remain on the Mango `1.0.0-SNAPSHOT` line, including:
  - `io.mango.platform.gridlayout:mango-grid-layout`
  - `io.mango.platform.gridlayout:mango-grid-layout-api`
  - `io.mango.platform.gridlayout:mango-grid-layout-core`
  - `io.mango.platform.gridlayout:mango-grid-layout-starter`

### Upgrade Notes

- Existing business projects should refresh backend Mango `1.0.0-SNAPSHOT` dependencies from the Maven repository.
- Frontend consumers using Mango Admin should upgrade to `@mango/admin@1.0.19` and `@mango/admin-shell@1.0.18`.
- Generated or manually maintained admin projects should include `@mango/grid-layout@1.0.0` and import the admin style entry that includes grid layout styles.
- Upgrade `@mango/cli` to `1.0.32` before creating new business projects so generated frontend dependencies include the grid layout package lock.

### Verification

- `pnpm -F @mango/grid-layout build`
- `pnpm -F @mango/admin-shell test`
- `pnpm -F @mango/admin-shell build`
- `pnpm -F @mango/admin build`
- `pnpm -F @mango/cli test`
- `mvn -f mango/pom.xml -pl mango-platform/mango-grid-layout/mango-grid-layout-core -am test`
- `node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/designs/mango-grid-layout-workbench-design.md --ledger mango-docs/plans/2026-06-15-grid-layout-workbench-delivery-ledger.md --mode verify`

## v2026.06.13-payment-platform - 2026-06-13

### New

- Added the Payment platform module on the backend `1.0.0-SNAPSHOT` line, including payment applications, cashier configuration, payment orders, refunds, refund approvals, reconciliations, differences, settlement summaries, operation audit, notifications, offline collections/refunds, and channel contract management.
- Added Fuiou payment channel support, including scan-pay/gateway flow, callback handling, refund query, channel bill fetching, and test callback development host support.
- Added the `@mango/payment@1.0.0` frontend package with payment admin pages, cashier UI, payment APIs, package styles, and admin feature registration.
- Added payment authorization menus, permissions, numgen seeds, workflow integration, and delivery evidence for the payment sprint.

### Fixed

- Closed PR #149 payment review blockers around channel callback consistency, transaction boundaries, Flyway migration ordering, refund workflow startup compensation, synchronous workflow completion, and fixed `bizRefundNo` recovery after workflow startup failure.
- Kept payment callback `allowedHosts` support for test callback scenarios.
- Kept backend Maven artifacts on the Mango `1.0.0-SNAPSHOT` line and added payment modules to the reactor.

### Published Packages

- `@mango/payment@1.0.0`
- Backend Maven artifacts remain on the Mango `1.0.0-SNAPSHOT` line, including:
  - `io.mango.platform.payment:mango-payment`
  - `io.mango.platform.payment:mango-payment-api`
  - `io.mango.platform.payment:mango-payment-core`
  - `io.mango.platform.payment:mango-payment-starter`
  - `io.mango.platform.payment:mango-payment-starter-remote`

### Upgrade Notes

- Existing business projects should refresh backend Mango `1.0.0-SNAPSHOT` dependencies from the Maven repository.
- Frontend consumers that need the payment center should install `@mango/payment@1.0.0` and import `@mango/payment/style.css`.
- Admin applications should register `registerMangoPaymentAdminPages` from `@mango/payment/admin-pages` when enabling the payment center.
- Run payment Flyway migrations in order before enabling payment menus or payment APIs.
- Configure real payment channel credentials, callback domains, and sensitive values per environment; the included Fuiou values are for confirmed test callback scenarios.

### Verification

- `git diff --check origin/main...HEAD`
- Payment and authorization Flyway duplicate version check
- `node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/plans/2026-05-25-payment-sprint-01.md --ledger mango-docs/plans/2026-05-25-payment-delivery-ledger.md --mode verify`
- `node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/plans/2026-05-25-payment-sprint-01.md --ledger mango-docs/plans/2026-05-25-payment-app-cashier-boundary-ledger.md --mode verify`
- `mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentRefundApprovalServiceTest,PaymentRefundApprovalMapperContractTest,PaymentTenantIsolationContractTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter -am test -DskipTests=false`
- `mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am checkstyle:check -DskipTests`
- `mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am pmd:check -DskipTests`

## v2026.06.12-mango-platform-release - 2026-06-12

### New

- Added System Event management to generated admin projects through:
  - `@mango/system@1.0.7`
  - `@mango/admin-pages@1.0.8`
  - `@mango/admin@1.0.18`
- Added reliable transparent domain event delivery in the backend `1.0.0-SNAPSHOT` line, including Redis Stream transport, pending message recovery, restart recovery, and Outbox reconsume support.
- Added `mango.dev.json` based development workspace commands in `@mango/cli@1.0.31`:
  - `mango init-dev`
  - `mango validate`
  - `mango doctor`
  - `mango plan [group|app...]`
  - `mango start [group|app...]`
  - `mango stop [app...]`
  - `mango status`
  - `mango logs <app>`
- New generated projects include `mango.dev.json` as the committed app startup manifest.
- `scripts/dev-workspace.sh` is now a compatibility shim; the real startup runner lives in Mango CLI.
- `mango pmo sync --sync-shell` now installs `mango.dev.json` when missing and does not overwrite a business-owned manifest.

### Fixed

- Backend development startup now uses the explicit Spring Boot Maven plugin coordinate from `mango.dev.json`, avoiding Maven prefix resolution failures.
- App stop, status and logs now use `.mango/run/pids` and `.mango/run/logs` instead of killing by port.
- Published package verification now checks exported `style.css` paths.
- Business PMO now requires Mango framework issues found during business development to be filed back to Mango instead of being silently patched in the business project.
- Business persistence checks now reject direct JDBC, mapper annotation SQL, and non-standard business persistence styles.

### Published Packages

- `@mango/admin@1.0.18`
- `@mango/admin-pages@1.0.8`
- `@mango/admin-shell@1.0.17`
- `@mango/app-runtime@1.0.2`
- `@mango/auth@1.0.6`
- `@mango/calendar@1.0.9`
- `@mango/common@1.0.8`
- `@mango/file@1.0.9`
- `@mango/job@1.0.1`
- `@mango/notice@1.0.9`
- `@mango/numgen@1.0.9`
- `@mango/rbac@1.0.6`
- `@mango/system@1.0.7`
- `@mango/template@1.0.9`
- `@mango/workflow@1.0.9`
- `@mango/workflow-business-example@1.0.9`
- `@mango/cli@1.0.31`
- Backend Maven artifacts remain on the Mango `1.0.0-SNAPSHOT` line.

### Upgrade Notes

- Upgrade `@mango/cli` first, then run `mango changelog` to view CLI-level new features and verification steps.
- Existing business projects should upgrade frontend `@mango/*` packages to the versions listed above.
- Existing business projects should refresh backend Mango `1.0.0-SNAPSHOT` dependencies from the Maven repository.
- Existing business projects should run `mango pmo sync --project-dir <project> --sync-shell`.
- Keep project-specific app names, folders, groups and extra apps in `mango.dev.json`.
- Keep local ports, database settings and secrets in `.mango/dev-workspace.env`.

### Verification

- `mango validate`
- `mango plan`
- `mango pmo sync --project-dir <dir> --sync-shell --dry-run`
- `pnpm --filter @mango/cli test`
- `scripts/check-business-persistence-style.sh`
- `mvn -pl mango-infra/mango-infra-test -am -Dtest=DomainEventOutboxAutoConfigurationTest,OutboxAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl mango-infra/mango-infra-test -am -Dtest=RedisStreamDomainEventTransportIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test`
