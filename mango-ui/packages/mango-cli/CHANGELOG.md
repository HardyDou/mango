# @mango/cli Changelog

## 1.0.94 - 2026-08-01

### Fixed

- Upgrade historical, schema-compatible lifecycle documents by writing their path, SHA-256 and PMO-version baseline before enforcing the document-set gate.

### Upgrade Notes

- Publish and verify `@mango/pmo@1.3.8` before installing `@mango/cli@1.0.94`.
- Existing business projects run `mango pmo upgrade --project-dir . --to 1.3.8 --sync-shell`, review `.mango-pmo-legacy-documents.json`, then run `mango pmo check --project-dir . --locked`.
- New lifecycle documents remain on PMO `1.3.8`; the upgrade baseline only admits unchanged documents on contract-declared historical versions.

### Verification

- `node mango-ui/packages/mango-cli/scripts/check-cli.mjs`
- `node mango-ui/packages/mango-pmo/scripts/build-package.mjs && node mango-ui/packages/mango-pmo/scripts/check-package.mjs`

## 1.0.93 - 2026-08-01

### Changed

- Lock generated and upgraded business backends to Mango Maven `1.0.29` and the PMO baseline at `1.3.7`.
- Lock the exact frontend patch matrix published with structured Notice presentation and grouping, Workflow target metadata, `FilePreviewPanel.fitContainer` and `MangoDialogExpose.bringToFront()`.
- Assign every initialized worktree a stable Maven revision qualifier, use that qualifier consistently for local install and Spring Boot startup, and keep immutable third-party dependencies in the shared Maven cache.
- Generate business backend POMs with a CI-friendly `${revision}` project version so mutable project artifacts from parallel worktrees resolve to distinct GAVs.
- Preserve existing CLI command behavior and generated-project entry points.

### Upgrade Notes

- Publish and verify Maven `1.0.29`, `@mango/pmo@1.3.7` and the frontend package matrix before installing `@mango/cli@1.0.93`.
- Projects inheriting `mango-parent` update their shared `mango.version` to `1.0.29`; projects with another parent import `io.mango:mango-bom:1.0.29`.
- Run `mango pmo upgrade --project-dir . --to 1.3.7 --sync-shell`, review the managed baseline changes, then run `mango pmo check --project-dir . --locked`.
- Run `mango workspace init` in each existing worktree before local Maven commands. Upgrade generated backends that still use a fixed project version before relying on parallel worktree isolation.

### Verification

- `pnpm -C mango-ui --filter @mango/cli test`
- CLI tests cover stable qualifier allocation, existing workspace upgrades, reactor revision injection and fixed-version fail-closed behavior.
- `node mango-ui/packages/mango-cli/scripts/check-release-versions.mjs`
- `MANGO_BACKEND_GATE_VERSION=1.0.29 node mango-ui/packages/mango-cli/scripts/check-generated-backend-gate.mjs`
- Clean published Maven, CLI and generated-project consumers resolve only the exact release coordinates.

## 1.0.92 - 2026-07-29

### Changed

- Lock generated and upgraded business backends to Mango Maven `1.0.28` while keeping the PMO baseline at `1.3.6`.
- Lock the exact frontend patch matrix published with the Workflow runtime designer snapshot and `MangoDialog` drag, resize, maximize and viewport-boundary interactions.
- Preserve existing CLI command behavior and generated-project entry points.

### Upgrade Notes

- Publish and verify Maven `1.0.28` and the frontend package matrix before installing `@mango/cli@1.0.92`.
- Projects inheriting `mango-parent` update their shared `mango.version` to `1.0.28`; projects with another parent import `io.mango:mango-bom:1.0.28`.
- Keep `@mango/pmo@1.3.6`; no PMO baseline upgrade is required for this batch.

### Verification

- `pnpm -C mango-ui --filter @mango/cli test`
- `node mango-ui/packages/mango-cli/scripts/check-release-versions.mjs`
- `MANGO_BACKEND_GATE_VERSION=1.0.28 node mango-ui/packages/mango-cli/scripts/check-generated-backend-gate.mjs`
- Clean published Maven, CLI and generated-project consumers resolve only the exact release coordinates.

## 1.0.91 - 2026-07-26

### Changed

- Lock generated and upgraded business backends to Mango Maven `1.0.27` and PMO baseline `1.3.6`.
- Lock the exact frontend patch matrix published with File multipart upload, Notice multi-mailbox attachment delivery and the `Require.nonNull` quality convention.
- Preserve existing CLI command behavior and public generated-project entry points.

### Upgrade Notes

- Publish and verify Maven `1.0.27`, `@mango/pmo@1.3.6` and the frontend package matrix before installing `@mango/cli@1.0.91`.
- Projects inheriting `mango-parent` update their shared `mango.version` to `1.0.27`; projects with another parent import `io.mango:mango-bom:1.0.27`.
- Run `mango pmo upgrade --project-dir . --to 1.3.6 --sync-shell`, review the managed baseline changes, then run `mango pmo check --project-dir . --locked`.

### Verification

- `pnpm -C mango-ui --filter @mango/cli test`
- `node mango-ui/packages/mango-cli/scripts/check-release-versions.mjs`
- `MANGO_BACKEND_GATE_VERSION=1.0.27 node mango-ui/packages/mango-cli/scripts/check-generated-backend-gate.mjs`
- Clean published Maven, PMO, CLI and generated-project consumers resolve only the exact release coordinates.

## 1.0.90 - 2026-07-23

### Changed

- Lock generated and upgraded business backends to Mango Maven `1.0.26` and PMO baseline `1.3.5`.
- Lock the exact frontend patch matrix published with the module diagnostics, controlled onboarding, resource synchronization, Workflow scope, message routing and managed rich-text changes.
- Expose the current workspace database status and preserve existing CLI command compatibility.

### Upgrade Notes

- Publish and verify Maven `1.0.26`, `@mango/pmo@1.3.5` and the frontend package matrix before installing `@mango/cli@1.0.90`.
- Projects inheriting `mango-parent` update their shared `mango.version` to `1.0.26`; projects with another parent import `io.mango:mango-bom:1.0.26`.
- Run `mango pmo upgrade --project-dir . --to 1.3.5 --sync-shell`, review the managed baseline changes, then run `mango pmo check --project-dir . --locked`.

### Verification

- `pnpm -C mango-ui --filter @mango/cli test`
- `node mango-ui/packages/mango-cli/scripts/check-release-versions.mjs`
- `MANGO_BACKEND_GATE_VERSION=1.0.26 node mango-ui/packages/mango-cli/scripts/check-generated-backend-gate.mjs`
- Clean published Maven, PMO, CLI and generated-project consumers resolve only the exact release coordinates.

## 1.0.89 - 2026-07-21

### Changed

- Lock generated and upgraded business backends to Mango Maven `1.0.25`, whose published dependency contract adds the independently importable `io.mango:mango-bom` and makes `mango-parent` import that same-version BOM.
- Lock the complete branding and Workflow/Notice frontend patch matrix headed by `@mango/admin@1.0.55`, `@mango/admin-shell@1.0.50`, `@mango/notice@1.0.30`, `@mango/system@1.0.25` and `@mango/workflow@1.0.33`.
- Keep `@mango/pmo@1.3.4` and existing CLI command behavior unchanged.

### Upgrade Notes

- Publish and verify Maven `1.0.25` and the locked frontend package matrix before installing `@mango/cli@1.0.89`.
- Business backends that inherit `mango-parent` only need to update their shared `mango.version`; projects with another parent can import `io.mango:mango-bom:1.0.25` and omit versions for BOM-managed dependencies.
- Existing projects are not automatically converted from parent inheritance to direct BOM import. Choose one Maven integration style and do not mix Mango versions from different release batches.

### Verification

- `pnpm -C mango-ui --filter @mango/cli test`
- `node mango-ui/packages/mango-cli/scripts/check-release-versions.mjs`
- `MANGO_BACKEND_GATE_VERSION=1.0.25 node mango-ui/packages/mango-cli/scripts/check-generated-backend-gate.mjs`
- Clean Maven BOM and generated-project consumers resolve only the exact combined-release coordinates.

## 1.0.88 - 2026-07-20

### Fixed

- Generate backend parent POMs that explicitly align `flyway-core` and `flyway-mysql` on 11.20.3, avoiding the MySQL 8.4 support warning emitted by the Spring Boot 3.5.14 Flyway baseline.
- Synchronize, validate and roll back the canonical business pull-request template's managed Risk / Verification block without overwriting project-owned content.
- Lock generated projects to the Maven `1.0.24`, PMO `1.3.4`, Notice `1.0.29`, Admin Shell `1.0.48` and Admin `1.0.53` compatibility matrix.

### Upgrade Notes

- Use `@mango/cli@1.0.88` for newly generated projects that run on MySQL 8.4.
- Publish and verify Maven `1.0.24` and `@mango/pmo@1.3.4` before installing this CLI version.
- Existing generated projects can apply the same two Flyway dependency-management entries, then run `mango pmo upgrade --to 1.3.4 --sync-shell` to receive the managed PR template contract.

### Verification

- `pnpm -C mango-ui --filter @mango/cli test`
- Generated backend POM contract verifies the Flyway version and both MySQL artifacts.
- PMO project-template tests cover create, managed-block update, drift detection and rollback.

## 1.0.87 - 2026-07-20

### Fixed

- Run Mango CLI self checks, generated frontend build reports, and PMO package checks correctly on Windows when commands resolve to `.cmd` shims.
- Keep the frontend UI fixes npm matrix aligned while locking generated projects to `@mango/pmo@1.3.3`.

### Upgrade Notes

- Upgrade business projects to `@mango/cli@1.0.87` after the full npm batch is published and verified.
- No backend Maven upgrade or CLI command migration is required.

## 1.0.86 - 2026-07-20

### Changed

- Align the generated business-module frontend dependency locks with the frontend UI fixes npm batch.
- Update the packaged `release-versions.json` to reference `@mango/admin-pages@1.0.25`, `@mango/common@1.0.20`, and the matching frontend package matrix.
- Keep command semantics, Mango Maven `1.0.23`, and `@mango/pmo@1.3.2` unchanged.

### Upgrade Notes

- Upgrade business projects to `@mango/cli@1.0.86` after the full frontend UI fixes npm batch is published and verified.
- No database reset, route migration, or backend Maven upgrade is required beyond the already published Maven `1.0.23` baseline.

### Verification

- Release impact, release-version lock, package builds, generated consumer typecheck/build, and Nexus target-version prechecks passed as part of the frontend UI fixes npm release batch.

## 1.0.85 - 2026-07-20

### Changed

- Lock generated and upgraded business backends to Mango Maven `1.0.23`.
- Retain `@mango/pmo@1.3.2` and the complete frontend package matrix published with CLI `1.0.84`.
- Ship the current generated Checkstyle policy together with the Maven `1.0.23` quality-gate behavior.

### Upgrade Notes

- Publish and verify Mango Maven `1.0.23` before installing `@mango/cli@1.0.85`.
- Upgrade business projects as one compatible set: Maven `1.0.23`, CLI `1.0.85`, PMO `1.3.2`, and the existing CLI `1.0.84` frontend locks.
- Workflow databases created by Maven `1.0.20` receive the narrowly scoped checksum repair and idempotent audit-column V2 migration; `1.0.21` and `1.0.22` databases already contain those columns.

### Verification

- `pnpm --filter @mango/cli test`
- `MANGO_BACKEND_GATE_VERSION=1.0.23 node mango-ui/packages/mango-cli/scripts/check-generated-backend-gate.mjs`
- Clean generated-project dependency and backend gate verification passed against the published Maven `1.0.23` and CLI `1.0.85` coordinates.

## 1.0.84 - 2026-07-19

### Changed

- Lock the frontend matrix that routes real CMS admin requests through the host-provided instance `HttpClient`.
- Carry stable Wujie route-slot `instanceId` values so one app can be mounted more than once without sharing request or destroy identity.
- Keep `@mango/pmo@1.3.2` and lock the exact reverse-dependency patch batch.

### Upgrade Notes

- Upgrade the complete locked npm batch together; do not mix runtime 1.0.84 locks with the preceding local candidate.
- Custom runtime configs may set `instanceId`; duplicate explicit values fail validation.
- Existing `@mango/cms` consumers can keep using deprecated `cmsApi` temporarily, while new code uses `createCmsApi(httpClient)`.

### Verification

- CMS real Axios token/tenant isolation and AbortSignal tests.
- App-runtime identity validation and Admin Shell exact-instance selection tests.
- Fixed Node 22/pnpm 11 production-candidate container and sealed business consumer.

## 1.0.83 - 2026-07-19

### Changed

- Generate business API packages as `createXxxApi(HttpClient)` factories instead of importing the legacy global request singleton.
- Add `@mango/http-client` to generated frontend dependencies and inject one host-owned client into generated business page registrars.
- Keep business pages transport-neutral, pass `AbortSignal` through generated CRUD APIs, and abort page-owned requests on unmount.
- Lock the complete frontend standards production-candidate matrix, including `@mango/pmo@1.3.2` and `@mango/http-client@1.0.0`.

### Fixed

- Keep `mango dev status`, `stop`, and `restart` usable in minimal containers that do not install `ps`: a successful kernel PID probe is now accepted when the optional zombie-state probe is unavailable.
- Add a generated-project regression that starts, restarts, and stops a real child process with a `PATH` that intentionally contains no `ps` command.

### Verification

- `node mango-ui/packages/mango-cli/scripts/check-cli.mjs`
- `pnpm --filter @mango/cli test`

## 1.0.82 - 2026-07-18

### Changed

- Lock `@mango/pmo@1.3.1`, whose `mango-release` Skill is the exclusive repository-local Mango release workflow.
- Generated and upgraded projects receive the hardened release readiness, recovery, CHANGELOG backfill, verification, and cleanup rules without using external generic release skills.

### Upgrade Notes

- Publish `@mango/pmo@1.3.1` before installing `@mango/cli@1.0.82`.
- Run `mango pmo upgrade --project-dir . --to 1.3.1`, then use the project-local CLI for subsequent work.
- Mango Maven and runtime frontend packages remain unchanged.

### Verification

- `pnpm --filter @mango/pmo build && pnpm --filter @mango/pmo check`
- `node mango-business-starter/scripts/sync-pmo-baseline.mjs --check`
- `pnpm --filter @mango/cli test`

## 1.0.81 - 2026-07-18

### Fixed

- Generate `frontend/pnpm-workspace.yaml` with the pnpm 11 `allowBuilds` policy required by the generated Mango frontend dependency set.
- Require the real generated workspace policy in CLI and packed-package consumer regressions instead of injecting an independent allowlist during verification.

### Upgrade Notes

- Install `@mango/cli@1.0.81` before generating new projects that use pnpm 11.
- Existing generated projects can copy the `allowBuilds` map from the current full template into their business-owned `frontend/pnpm-workspace.yaml`; the CLI does not overwrite existing workspace policy.
- Mango Maven remains `1.0.22`, PMO remains `1.3.0`, and runtime frontend package versions remain unchanged.

### Verification

- `pnpm --filter @mango/cli test`
- `pnpm package-consumer:typecheck -- --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- Clean pnpm 11 install and `pnpm run typecheck` from a newly generated project.

## 1.0.80 - 2026-07-18

### Changed

- Lock generated and upgraded business projects to Mango Maven `1.0.22`, `@mango/pmo@1.3.0` and the frontend package batch headed by `@mango/admin@1.0.49`.
- Synchronize the PMO risk-based delivery modes (`SIMPLE`, `STANDARD` and `FULL`) into generated projects.

### Breaking Changes

- Mango Maven `1.0.22` replaces `ResourceRegistryApi` with `ResourceDeclarationApi` and intentionally provides no compatibility alias. Existing business source, dependency injection and Feign references must migrate before upgrading.
- Business projects that reference the former resource target topology must use `mango-resource-sync-starter` and the current declaration API.

### Upgrade Notes

- Publish Mango Maven `1.0.22` and the npm dependency batch before installing `@mango/cli@1.0.80`.
- Run `mango pmo upgrade --project-dir . --to 1.3.0`, migrate resource API references, then update the project-local CLI.
- Existing databases apply the File V2 migration that defaults file access to proxy delivery; deployments requiring direct access must retain an explicit override.

### Verification

- `node mango-ui/packages/mango-cli/scripts/check-cli.mjs`
- `node --test mango-ui/packages/mango-cli/tests/*.test.mjs`
- `pnpm --filter @mango/cli run check:release-versions`
- `MANGO_BACKEND_GATE_VERSION=1.0.22 node mango-ui/packages/mango-cli/scripts/check-generated-backend-gate.mjs`

## 1.0.78 - 2026-07-14

### Fixed

- Fix [Issue #507](https://github.com/HardyDou/mango/issues/507): make `mango workspace init` create `.mango/m2/repository` as a directory link to the user's `~/.m2/repository`, avoiding repeated Maven dependency downloads in fresh worktrees.
- Keep initialization idempotent and preserve any existing real directory or link that represents an explicitly isolated Maven repository.
- Lock generated and upgraded business projects to Mango Maven `1.0.21`, `@mango/pmo@1.2.6`, `@mango/notice@1.0.23`, `@mango/payment@1.0.13`, `@mango/admin-shell@1.0.41`, and `@mango/admin@1.0.46`.

### Upgrade Notes

- Upgrade the project-local CLI to `@mango/cli@1.0.78`, then run `mango workspace init` once in each active worktree.
- Upgrade existing business backends to Mango Maven `1.0.21` and run `mango pmo upgrade --project-dir . --to 1.2.6`; no business `mango.dev.json` edit is required.

### Verification

- `node mango-ui/packages/mango-cli/scripts/check-cli.mjs`
- `node --test mango-ui/packages/mango-cli/tests/*.test.mjs`
- `pnpm --filter @mango/cli run check:release-versions`

## 1.0.77 - 2026-07-14

### Fixed

- Lock generated and upgraded business backends to Mango Maven `1.0.20`, where the `architecture` goal exposes Maven/Plexus-bindable `java.io.File` parameters instead of the unsupported `java.nio.file.Path` interface.
- Add a Maven plugin harness regression that configures all architecture path parameters through a real plugin POM.

### Upgrade Notes

- Publish Mango Maven `1.0.20` before installing `@mango/cli@1.0.77`.
- Existing business projects blocked by `Cannot create instance of interface java.nio.file.Path` can directly upgrade their Mango backend version to `1.0.20`; no PMO, source-code, database, API, menu, permission, or runtime configuration migration is required.

### Verification

- `mvn -pl mango-tools/mango-maven-plugin -am -DskipTests=false -Dtest=ArchitectureMojoTest,ArchitectureMojoBindingTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `pnpm --filter @mango/cli run check:release-versions`
- `node mango-pmo/tools/test-quality-check.mjs --base origin/main`

## 1.0.76 - 2026-07-14

### Changed

- Lock generated and upgraded business frontend projects to `@mango/link-page@1.0.5` so they consume the published public link page visual polish.

### Fixed

- Normalize Windows local tarball paths in package consumer typecheck, keeping local packed-tarball verification usable from release worktrees.

### Upgrade Notes

- Publish `@mango/link-page@1.0.5` before installing `@mango/cli@1.0.76`.
- Existing business projects that use the public link page package can upgrade `@mango/link-page` directly; new generated projects receive the updated lock from this CLI version.

### Verification

- `pnpm --filter @mango/cli run check:release-versions`
- `pnpm package-consumer:typecheck -- --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/ --keep-temp`
- `pnpm release:impact --base=origin/main --head=HEAD`

## 1.0.75 - 2026-07-14

### Fixed

- Make aggregated PMD and Checkstyle execution produce reports first so Mango can apply `no-new-violations` to changed files instead of failing on historical findings before classification.
- Add a path- and SHA-256-pinned baseline for lifecycle documents created before PMO contracts, while continuing to reject changed legacy documents and all ungoverned new documents.
- Lock the CLI to `@mango/pmo@1.2.5` and Mango Maven `1.0.19`.

### Upgrade Notes

- Publish Mango Maven `1.0.19` and `@mango/pmo@1.2.5` before installing `@mango/cli@1.0.75`.
- Run `mango pmo upgrade --project-dir . --to 1.2.5`, update the business backend to Mango Maven `1.0.19`, add an approved legacy-document hash baseline where required, and run the final required check once.

### Verification

- `mvn -f mango/pom.xml -pl mango-tools/mango-maven-plugin -Dtest=CheckMojoTest test`
- `node --test mango-pmo/tests/document-contract/document-contract.test.mjs`
- `node mango-business-starter/scripts/sync-pmo-baseline.mjs --check`

## 1.0.74 - 2026-07-14

### Changed

- Split GitHub code-SHA checks into parallel PMO, docs, CLI, and Java jobs while preserving the stable `pmo-doc-check` result.
- Add the lightweight `pr-contract-check` path for PR description edits, cancel obsolete SHA runs, and keep generated GitHub/Gitea partial Maven quality checks limited to directly changed modules.
- Run generated-backend acceptance only for behavior-changing generators, templates, Java gates, or governance inputs; reduce its Maven invocations from 19 to 9 with no `clean` calls.
- Lock the CLI to `@mango/pmo@1.2.4`; Mango Maven remains `1.0.18`.

### Upgrade Notes

- Publish `@mango/pmo@1.2.4` before installing `@mango/cli@1.0.74`.
- Run `mango pmo upgrade --project-dir . --to 1.2.4`, synchronize the GitHub/Gitea workflow, and require `pr-contract-check` plus `pmo-doc-check` on GitHub.

### Verification

- `node mango-ui/packages/mango-cli/scripts/check-cli.mjs`
- `node --test mango-ui/packages/mango-cli/tests/*.test.mjs`
- `node --test mango-pmo/tests/pmo-check-scope.test.mjs mango-pmo/tests/branch-protection-policy.test.mjs`
- Full generated-backend acceptance with exactly 9 Maven invocations.

## 1.0.73 - 2026-07-14

### Fixed

- Lock generated and upgraded business backends to Mango Maven `1.0.18`, which prepares upstream SNAPSHOT prerequisites on clean PR runners without expanding the actual partial quality gate.
- Lock the CLI to `@mango/pmo@1.2.3`, carrying the matching scope classifier, governance guard, and GitHub/Gitea workflow baseline.
- Keep nested static-analysis invocations away from the architecture-verification aggregator while preserving the outer architecture gate.

### Upgrade Notes

- Publish Mango Maven `1.0.18` and `@mango/pmo@1.2.3` before installing `@mango/cli@1.0.73`.
- Run `mango pmo upgrade --project-dir . --to 1.2.3`, then update the business backend to Mango Maven `1.0.18` and rerun the required PR check once.

### Verification

- `node mango-ui/packages/mango-cli/scripts/check-release-versions.mjs`
- `node mango-ui/packages/mango-cli/scripts/check-generated-backend-gate.mjs`
- `node mango-business-starter/scripts/sync-pmo-baseline.mjs --check`

## 1.0.72 - 2026-07-14

### Fixed

- Read backend, frontend, and business-document roots from `mango.config.json.paths`; a repository using `baohan-backend/` is no longer silently classified as having no backend changes.
- Fail closed when the configured backend POM is missing, and feed the resolved backend POM and business-document root to the standard workflow.
- Ship equivalent GitHub and Gitea `pmo-doc-check` templates. Direct-module checks continue without `-am` or `-amd` and compare no-new violations against the PR base.
- Include `io.mango:mango-docs-bundle:<version>` in the existing `publish-maven-batch.sh --all-non-app` release step so a Maven release cannot omit its version-matched CLI documentation source.

### Upgrade Notes

- Install `@mango/cli@1.0.72`, configure non-default repository roots under `mango.config.json.paths`, and run `mango pmo upgrade --project-dir . --to 1.2.2`.
- Synchronize the standard workflow for the repository host, then configure `PMO Documentation Checks / pmo-doc-check` as a required check. Mango Maven remains `1.0.17`.

### Verification

- `node --test mango-pmo/tests/pmo-check-scope.test.mjs mango-pmo/tests/publish-maven-batch.test.mjs`
- `node mango-ui/packages/mango-cli/scripts/check-release-versions.mjs`
- `node mango-ui/packages/mango-cli/scripts/check-cli.mjs`
- `node --test mango-ui/packages/mango-cli/tests/*.test.mjs`

## 1.0.71 - 2026-07-14

### Fixed

- Updated the exact PMO dependency to `@mango/pmo@1.2.1`, whose published tarball preserves all manifest-declared executable modes.
- Added real `pnpm pack` verification before publication and downloaded-tarball mode verification after publication so a mode mismatch cannot be reported as a successful PMO release.

### Upgrade Notes

- Install `@mango/cli@1.0.71`, run `mango pmo upgrade --project-dir . --to 1.2.1 --dry-run`, then perform the upgrade and run `mango pmo check --project-dir . --locked`.
- Do not use `@mango/pmo@1.2.0` / `@mango/cli@1.0.70` for business upgrades. Mango Maven remains `1.0.17`; no backend dependency change is required.

### Verification

- `pnpm -C mango-ui --filter @mango/pmo build`
- `pnpm -C mango-ui --filter @mango/pmo check`
- `node --test mango-ui/packages/mango-cli/tests/pmo-bundle.test.mjs`
- `node mango-ui/scripts/publish-package.mjs --verify-pmo-package-root=<extracted-package-root>`

## 1.0.70 - 2026-07-13

### Changed

- Updated the exact PMO dependency to `@mango/pmo@1.2.0` and generated the matching PR risk/verification contract.
- Generated `pmo-doc-check` now classifies changed paths before Java setup. Normal backend quality gates verify only directly changed Maven modules without `-am` or `-amd`; dependency builds and consumer compatibility remain separate risk-based verification steps. PMO sync, docs, and frontend-only changes skip the backend Reactor.
- Route root backend POM, architecture-verification, global architecture inputs, and workflow changes to governance validation; ordinary PR quality checks remain limited to directly changed modules, while complete Reactor inventory runs only on schedule or manual request.
- Use the HTTPS Nexus endpoint by default for generated business projects; generated-backend gate acceptance keeps only the four business layers and consumes explicitly built local Mango artifacts without internal Nexus access.

### Upgrade Notes

- Install `@mango/cli@1.0.70`, run `mango pmo upgrade --project-dir . --to 1.2.0`, and then run `mango pmo check --project-dir . --locked`.
- Fill the generated PR template's requirement impact, solution risk, final maximum, selected `STATIC/UNIT/API/UI`, sufficiency, and skipped-type reasons.
- Publish Mango Maven `1.0.17` before this CLI, then upgrade generated and existing business backends to that version. It removes hidden dependency expansion from delegated static-analysis commands.

### Verification

- `node mango-ui/packages/mango-cli/scripts/check-release-versions.mjs`
- `node mango-ui/packages/mango-cli/scripts/check-cli.mjs`
- `node --test mango-ui/packages/mango-cli/tests/*.test.mjs`
- `node mango-business-starter/scripts/sync-pmo-baseline.mjs --check`

## 1.0.69 - 2026-07-13

### Fixed

- Fixed `mango release repair` so a pending immutable state with `attempts=0` executes its first reviewed publish adapter instead of verify-only.
- Kept immutable recovery fail-closed: after any publish attempt, repair still requires the exact `{ "kind": "verify-existing" }` strategy and never republishes a passed or attempted immutable artifact.
- Updated the PMO lock to `@mango/pmo@1.1.1`, which carries the matching release rule, Release Skill, and boundary evaluation.

### Upgrade Notes

- Install `@mango/cli@1.0.69` and run `mango pmo upgrade --project-dir . --to 1.1.1` before using release repair automation.
- Mango Maven remains `1.0.16`; no backend dependency change is required for this CLI/PMO patch.

### Verification

- `node --test mango-ui/packages/mango-cli/tests/release-command.test.mjs`
- `node mango-pmo/tests/skills/check-skill-evals.mjs`
- `node mango-ui/packages/mango-cli/scripts/check-release-versions.mjs`
- `pnpm -C mango-ui release:impact --base=origin/main --head=HEAD`

## 1.0.68 - 2026-07-12

### Added

- Locked the CLI to `@mango/pmo@1.1.0`, whose reproducible bundle includes the canonical PMO rules, lifecycle templates,
  document contracts, checkers, dedicated agents, project Skills, and package-root Codex plugin projection.
- Added project PMO locks at `business-pmo/pmo-lock.json` and synchronized the bundle-owned Skills into
  `.agents/skills` without modifying user-level Codex plugin configuration.
- Added `mango pmo check --locked`, exact-version `mango pmo upgrade --to <version>`, and verified
  `mango pmo rollback [--to <version>]` recovery from local bundle backups.
- Added a deterministic canonical-to-CLI business-module projection gate over relative paths, file sizes, SHA-256
  hashes, and executable modes.
- Added a final `architecture-verification` Maven reactor module and generated GitHub workflow. A normal backend
  `mvn verify` now runs the full Mango architecture engine, Mango project checks, and blocking P3C/PMD, Checkstyle,
  and SpotBugs checks.
- Added `mango release publish/status/verify/repair` and `mango release registry doctor` with a persisted state
  manifest, explicit authorization, configuration precedence, output redaction, and immutable-artifact resume safety.

### Changed

- Advanced the generated backend lock to Mango Maven `1.0.16`. That backend batch must be published first because it
  provides `MangoTypedCrudService` and the architecture gate consumed by this CLI template.
- Made PMO installation and upgrade transactional: baseline files, the project lock, and project Skills are staged,
  verified by hash, atomically switched, and cleaned of stale bundle-owned files.
- Kept `mango pmo sync` pinned to the project lock. Moving to another PMO bundle now requires the explicit upgrade
  command, while rollback only selects an already verified local backup.
- Synchronized the published business-module backend template with the canonical starter so `module add` generates a
  pure Api contract, matching Controller and Feign adapters, `MangoTypedCrudService`, module `BizCode`, and
  `Require`-based service validation without `PathVariable` endpoints.
- Removed implicit registry selection from the unified Maven/npm release adapters and added verify-only repository
  back-checks so repair can validate already published artifacts without republishing them.
- Upgraded release manifests to schema v2. Applicable terminal states now require complete command, working-directory,
  timestamp, integer exit-code, and redacted-output evidence; required-state applicability is revalidated, and
  immutable repair accepts only `{ "kind": "verify-existing" }` backed by the state's verify adapter.

### Upgrade Notes

- Publish and verify the full non-app Mango Maven `1.0.16` batch before publishing this CLI. Existing business
  backends must move `<mango.version>` to `1.0.16` before adopting generated typed CRUD modules. Maven `1.0.15`
  already exists but cannot parse this release candidate's global Entity manifest contract and is not compatible.
- Install or upgrade the global CLI with `npm install -g @mango/cli@1.0.68 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- In each business project, run `mango pmo upgrade --project-dir . --to 1.1.0`, then
  `mango pmo check --project-dir . --locked` and review the generated project-level Skills under `.agents/skills`.
- Project Skill synchronization does not install the package-root Codex plugin into a user profile. Plugin installation
  remains a separate, explicit Codex operation against the published `@mango/pmo` package.

### Verification

- `pnpm -C mango-ui --filter @mango/pmo build`
- `pnpm -C mango-ui --filter @mango/pmo check`
- `pnpm -C mango-ui --filter @mango/cli run check:release-versions`
- `scripts/publish-maven-batch.sh --all-non-app --release-version 1.0.16 --dry-run`
- `pnpm -C mango-ui --filter @mango/cli run check:business-module-template`
- `MANGO_BACKEND_GATE_VERSION=1.0.16 node mango-ui/packages/mango-cli/scripts/check-generated-backend-gate.mjs`
- `pnpm -C mango-ui --filter @mango/cli test`
- Generated custom project: `mvn -f backend/pom.xml verify` passed with the architecture and static-analysis reports.
- Generated project with `@PathVariable`: `mvn -f backend/pom.xml verify` failed with `MANGO-ARCH-PATH-001/002`.
- Generated project with a generic Java style violation: `mvn -f backend/pom.xml verify` failed in Checkstyle; attempts
  to switch architecture/static checks to skip, partial reactor, changed, static-only, narrowed baseDir, no-new-only,
  report-only, changed-files-only, or code-level module-exclusion modes failed in the governed Maven plugin configuration.
- Generated project violations for direct MyBatis `ServiceImpl`, missing tenant schema, invalid module metadata, and a
  missing global Entity exception manifest were each rejected by their owning architecture or Mango project gate.
- Global Entity E2E accepted an approved exact Entity/table with no tenant columns, rejected the same Entity when
  unregistered, and rejected a manifest table that differed from `@TableName` and migration.
- Generated four-layer business module: `mvn -f backend/pom.xml verify` passed for all eight reactor projects.
- `node --test mango-ui/packages/mango-cli/tests/business-module-template.test.mjs`
- `node --test mango-ui/packages/mango-cli/tests/pmo-bundle.test.mjs`
- `node --test mango-ui/packages/mango-cli/tests/release-command.test.mjs`
- `node mango-ui/scripts/check-package-exports.mjs --package=@mango/pmo`
- `npm pack --dry-run --json` from `mango-ui/packages/mango-pmo`
- `node mango-ui/scripts/publish-package.mjs --verify-pmo-package-root=<extracted-package-root>`
- `pnpm -C mango-ui release:impact --base=origin/main --head=HEAD`

## 1.0.67 - 2026-07-11

### Changed

- Advanced generated business backend projects from Mango Maven `1.0.13` to the forward-only `1.0.14` backend batch.
- Kept the certified frontend npm package lock from `v2026.07.11-npm-readme-forward-release` unchanged.
- Preserved the current backend and frontend runtime implementation; this release does not revert source changes.

### Upgrade Notes

- Install or upgrade the global CLI with `npm install -g @mango/cli@1.0.67 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- Existing business backends should update `<mango.version>` to `1.0.14`; new generated projects receive that version automatically.

### Verification

- `pnpm -C mango-ui --filter @mango/cli test`
- `pnpm -C mango-ui --filter @mango/cli run check:release-versions`
- `scripts/publish-maven-batch.sh --all-non-app --release-version 1.0.14 --dry-run`

## 1.0.66 - 2026-07-11

### Changed

- Locked generated business frontend projects to the forward-only documentation release batch
  `v2026.07.11-npm-readme-forward-release`.
- Preserved the runtime implementation published in the preceding batch while advancing the package versions that
  carry corrected module READMEs and exact internal dependencies.
- Kept generated backend projects on Mango Maven backend `1.0.13`.

### Upgrade Notes

- Install or upgrade the global CLI with `npm install -g @mango/cli@1.0.66 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- Existing business frontends should upgrade the package batch together, especially `@mango/admin-pages`,
  `@mango/file`, `@mango/system`, `@mango/admin-shell`, and `@mango/admin`.

### Verification

- `pnpm -C mango-ui --filter @mango/cli test`
- `pnpm -C mango-ui --filter @mango/cli run check:release-versions`
- `pnpm -C mango-ui run package-consumer:typecheck -- --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`

## 1.0.65 - 2026-07-11

### Changed

- Locked generated business frontend projects to the `v2026.07.11-npm-lock-sync-release` npm batch:
  - `@mango/admin@1.0.44`
  - `@mango/admin-pages@1.0.19`
  - `@mango/admin-shell@1.0.39`
  - `@mango/auth@1.0.15`
  - `@mango/calendar@1.0.20`
  - `@mango/cms@1.0.9`
  - `@mango/common@1.0.16`
  - `@mango/file@1.0.20`
  - `@mango/grid-layout@1.0.7`
  - `@mango/grid-widgets@1.0.13`
  - `@mango/home@1.0.5`
  - `@mango/job@1.0.12`
  - `@mango/link@1.0.6`
  - `@mango/notice@1.0.21`
  - `@mango/numgen@1.0.20`
  - `@mango/payment@1.0.11`
  - `@mango/rbac@1.0.13`
  - `@mango/site-shell@1.0.3`
  - `@mango/system@1.0.18`
  - `@mango/template@1.0.20`
  - `@mango/workflow-business-example@1.0.25`
  - `@mango/workflow@1.0.26`
  - `@mango/cli@1.0.65`
- Kept generated backend projects on Mango Maven backend `1.0.13`.
- Updated the packed consumer type gate so pnpm 11 verifies unpublished local tarball batches with supported workspace
  overrides and approved dependency build scripts.

### Upgrade Notes

- Install or upgrade the global CLI with `npm install -g @mango/cli@1.0.65 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- Existing generated frontend projects should upgrade the listed Mango npm packages as a batch.

### Verification

- `pnpm -C mango-ui --filter @mango/cli test`
- `pnpm -C mango-ui --filter @mango/cli run check:release-versions`
- `pnpm -C mango-ui run package-consumer:typecheck -- --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`

## 1.0.64 - 2026-07-08

### Changed

- Updated generated business list pages to use `@mango/common` admin page skeleton components:
  `MangoListPage`, `MangoSearchPanel`, and `MangoListPanel`.
- Locked generated business frontend projects to the `v2026.07.08-admin-page-layout-release` npm batch:
  - `@mango/admin@1.0.43`
  - `@mango/admin-pages@1.0.18`
  - `@mango/admin-shell@1.0.38`
  - `@mango/auth@1.0.12`
  - `@mango/calendar@1.0.19`
  - `@mango/cms@1.0.8`
  - `@mango/common@1.0.13`
  - `@mango/file@1.0.19`
  - `@mango/grid-layout@1.0.6`
  - `@mango/grid-widgets@1.0.12`
  - `@mango/home@1.0.4`
  - `@mango/job@1.0.11`
  - `@mango/link@1.0.5`
  - `@mango/notice@1.0.20`
  - `@mango/numgen@1.0.19`
  - `@mango/payment@1.0.10`
  - `@mango/rbac@1.0.12`
  - `@mango/site-shell@1.0.2`
  - `@mango/system@1.0.17`
  - `@mango/template@1.0.19`
  - `@mango/workflow-business-example@1.0.24`
  - `@mango/workflow@1.0.25`
  - `@mango/cli@1.0.64`

### Upgrade Notes

- Install or upgrade the global CLI with `npm install -g @mango/cli@1.0.64 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- New generated business projects keep `<mango.version>1.0.13</mango.version>` and receive the admin page layout package batch.
- Existing generated projects can upgrade frontend Mango packages as a batch to adopt the shared list/detail/form page skeletons.

### Verification

- `node mango-ui/packages/mango-cli/scripts/check-cli.mjs`
- `node mango-ui/packages/mango-cli/scripts/check-release-versions.mjs`

## 1.0.62 - 2026-07-07

### Fixed

- Added the missing built-in backend Flyway module declarations generated by the full preset: `resource`, `home`,
  `payment`, `link`, `grid-layout`, and `mango-cms`.

### Changed

- Locked generated business backend projects to Mango Maven backend `1.0.12`.
- Kept the certified frontend package batch from the `1.0.9` release while advancing the backend hotfix lock:
  - `@mango/admin-shell@1.0.36`
  - `@mango/admin@1.0.41`
  - `@mango/workflow-business-example@1.0.22`
  - `@mango/workflow@1.0.23`
  - `@mango/cli@1.0.62`

### Upgrade Notes

- Install or upgrade the global CLI with `npm install -g @mango/cli@1.0.62 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- New generated business projects receive `<mango.version>1.0.12</mango.version>` and the current Flyway bootstrap fixes.
- Existing generated projects can update only `<mango.version>` to `1.0.12` if their frontend Mango package versions
  already match the `1.0.9` release batch.

### Verification

- `pnpm --filter @mango/cli test`
- `pnpm --filter @mango/cli run check:release-versions`

## 1.0.61 - 2026-07-07

### Changed

- Locked generated business backend projects to Mango Maven backend `1.0.10`.
- Kept the certified frontend package batch from the `1.0.9` release while advancing the CLI release lock:
  - `@mango/admin-shell@1.0.36`
  - `@mango/admin@1.0.41`
  - `@mango/workflow-business-example@1.0.22`
  - `@mango/workflow@1.0.23`
  - `@mango/cli@1.0.61`

### Upgrade Notes

- Install or upgrade the global CLI with `npm install -g @mango/cli@1.0.61 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- New generated business projects receive `<mango.version>1.0.10</mango.version>` and the current Resource Registry
  replay fix.
- Existing generated projects can update only `<mango.version>` to `1.0.10` if their frontend Mango package versions
  already match the `1.0.9` release batch.

### Verification

- `pnpm --filter @mango/cli test`
- `pnpm --filter @mango/cli run check:release-versions`

## 1.0.60 - 2026-07-07

### Changed

- Locked generated business backend projects to Mango Maven backend `1.0.9`.
- Updated generated project release locks for the API contract boundary batch:
  - `@mango/admin-shell@1.0.36`
  - `@mango/admin@1.0.41`
  - `@mango/workflow-business-example@1.0.22`
  - `@mango/workflow@1.0.23`
  - `@mango/cli@1.0.60`

### Upgrade Notes

- Install or upgrade the global CLI with `npm install -g @mango/cli@1.0.60 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- New generated business projects receive `<mango.version>1.0.9</mango.version>` and the current API contract package batch.
- Existing workflow business integrations should depend on `WorkflowProcessApi`, `WorkflowTaskRuntimeApi`, and workflow
  event API types instead of importing `mango-workflow-core` services.

### Verification

- `pnpm --filter @mango/cli test`
- `pnpm --filter @mango/cli run check:release-versions`

## 1.0.58 - 2026-07-03

### Changed

- Locked generated business backend projects to Mango Maven backend `1.0.7`.
- Updated release version validation to accept the certified Element Plus peer range while keeping generated project
  templates on fixed dependency locks.
- Updated generated project release locks for the platform batch:
  - `@mango/admin-pages@1.0.15`
  - `@mango/admin-shell@1.0.33`
  - `@mango/admin@1.0.38`
  - `@mango/app-runtime@1.0.3`
  - `@mango/auth@1.0.11`
  - `@mango/calendar@1.0.16`
  - `@mango/cms@1.0.5`
  - `@mango/common@1.0.12`
  - `@mango/file@1.0.16`
  - `@mango/grid-layout@1.0.5`
  - `@mango/grid-widgets@1.0.10`
  - `@mango/home@1.0.0`
  - `@mango/job@1.0.8`
  - `@mango/link-page@1.0.2`
  - `@mango/link@1.0.2`
  - `@mango/notice@1.0.17`
  - `@mango/numgen@1.0.16`
  - `@mango/payment@1.0.7`
  - `@mango/rbac@1.0.10`
  - `@mango/site-shell@1.0.1`
  - `@mango/system@1.0.14`
  - `@mango/template@1.0.16`
  - `@mango/workflow-business-example@1.0.20`
  - `@mango/workflow@1.0.21`
  - `@mango/cli@1.0.58`

### Upgrade Notes

- Install or upgrade the global CLI with `npm install -g @mango/cli@1.0.58 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- New generated business projects receive `<mango.version>1.0.7</mango.version>` and the current platform npm package batch.
- Existing business projects should upgrade backend Maven, frontend Mango packages, and project-local `@mango/cli` as one batch.

### Verification

- `pnpm --filter @mango/cli test`
- `pnpm --filter @mango/cli run check:release-versions`

## 1.0.57 - 2026-07-02

### Changed

- Locked generated business backend projects to Mango Maven backend `1.0.6`.
- Updated generated project release locks for the home-widget modularization batch:
  - `@mango/admin-shell@1.0.32`
  - `@mango/admin@1.0.37`
  - `@mango/calendar@1.0.15`
  - `@mango/grid-layout@1.0.4`
  - `@mango/grid-widgets@1.0.9`
  - `@mango/link-openapi@1.0.1`
  - `@mango/link-page@1.0.1`
  - `@mango/link@1.0.1`
  - `@mango/notice@1.0.16`
  - `@mango/system@1.0.13`
  - `@mango/workflow@1.0.20`
  - `@mango/admin-pages@1.0.14`
  - `@mango/cms@1.0.4`
  - `@mango/file@1.0.15`
  - `@mango/job@1.0.7`
  - `@mango/numgen@1.0.15`
  - `@mango/payment@1.0.6`
  - `@mango/template@1.0.15`
  - `@mango/workflow-business-example@1.0.19`
  - `@mango/cli@1.0.57`

### Fixed

- Added `mango-notice-starter` to custom generated backend baseline dependencies so `custom --modules none` projects that include `mango-auth-starter` can provide the required `NoticeApi` bean during Spring Boot startup.
- Added explicit `MangoAdminFeatureRegistrar[]` types to generated admin frontend feature registrar arrays so custom projects pass strict `vue-tsc` when no business modules have been added yet.

### Upgrade Notes

- Install or upgrade the global CLI with `npm install -g @mango/cli@1.0.57 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- New generated business projects receive `<mango.version>1.0.6</mango.version>` and the current home-widget package versions.
- Existing business projects should remove any direct `@mango/link-panel` dependency and use `@mango/link` for Link admin pages and the Link navigation home widget.

### Verification

- `pnpm --filter @mango/cli test`
- `pnpm --filter @mango/cli run check:release-versions`

## 1.0.56 - 2026-07-01

### Changed

- Locked generated business backend projects to Mango Maven backend `1.0.5`.
- Updated generated project release locks for the data initialization governance batch:
  - `@mango/pmo@1.0.6`
  - `@mango/link-panel@1.0.1`
  - `@mango/cli@1.0.56`

### Upgrade Notes

- Install or upgrade the global CLI with `npm install -g @mango/cli@1.0.56 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- Existing business projects should run `mango pmo sync --project-dir . --sync-shell` or `mango pmo upgrade --project-dir . --sync-shell` to receive the updated PMO baseline.
- Business backends should set `<mango.version>1.0.5</mango.version>` when consuming the Resource Registry and persistence initialization governance changes.

### Verification

- `pnpm --filter @mango/cli test`
- `pnpm --filter @mango/cli run check:release-versions`

## 1.0.54 - 2026-06-30

### Changed

- Locked generated business backend projects to Mango Maven backend `1.0.1` instead of the previous `1.0.0-SNAPSHOT` default.
- Added release-version validation so formal CLI release locks reject `maven.mangoBackend` snapshot values.
- Documented how generated projects choose the CLI-owned Maven jar version and how business projects override it with `--mango-version`.
- Updated generated frontend release locks for the admin branding batch:
  - `@mango/admin@1.0.35`
  - `@mango/admin-pages@1.0.13`
  - `@mango/admin-shell@1.0.31`
  - `@mango/auth@1.0.10`
  - `@mango/file@1.0.14`
  - `@mango/grid-layout@1.0.3`
  - `@mango/grid-widgets@1.0.8`
  - `@mango/system@1.0.12`

### Upgrade Notes

- Install or upgrade the global CLI with `npm install -g @mango/cli@1.0.54 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- New business projects generated with this CLI receive `<mango.version>1.0.1</mango.version>` in `backend/pom.xml`.
- Existing business projects can align manually by setting their backend parent POM `<mango.version>` to `1.0.1` after the matching Mango Maven artifacts are published.
- Existing business frontends should refresh Mango npm packages as a batch using the versions from this CLI release lock.

### Verification

- `pnpm --filter @mango/cli test`
- `pnpm --filter @mango/cli run check:release-versions`

## 1.0.53 - 2026-06-30

### Fixed

- Removed the legacy generated `scripts/dev-workspace.sh init` shim so business projects use Mango CLI workspace commands as the owning development entry point.
- Updated generated project release locks to consume `@mango/pmo@1.0.5` and `@mango/cli@1.0.53`.

### Upgrade Notes

- Install or upgrade the global CLI with `npm install -g @mango/cli@1.0.53 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- Existing business projects should run `mango pmo sync --project-dir . --sync-shell` or `mango pmo upgrade --project-dir . --sync-shell` so generated compatibility scripts and PMO baseline content stay aligned.

### Verification

- `pnpm --filter @mango/cli test`
- `pnpm --filter @mango/cli run check:release-versions`

## 1.0.52 - 2026-06-29

### Fixed

- Added Mango backend public API artifacts to generated business backend parent POM `dependencyManagement`, including File, File Preview, Template, Workflow, Notice, Job, Numgen, System, Resource, Authorization, Identity, Org, Calendar, Domain, Captcha, Payment, CMS, Grid Layout, and Access API artifacts.
- Kept generated app POM direct dependencies on starters only, so business core modules can depend on API artifacts without depending on runtime starter assembly.

### Upgrade Notes

- Install or upgrade the global CLI with `npm install -g @mango/cli@1.0.52 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- New or regenerated business projects receive the updated backend `dependencyManagement` automatically.
- Existing business projects can regenerate from the current starter or manually align their parent POM dependency management if business core modules need direct API dependencies such as `mango-file-api`.

### Verification

- `pnpm --filter @mango/cli test`
- `pnpm --filter @mango/cli run check:release-versions`

## 1.0.51 - 2026-06-29

### Fixed

- Created local `mango_dev_*` workspace databases before starting Spring Boot Maven apps when `MANGO_DB_AUTO_CREATE=true`; Flyway still owns schema and seed migrations.
- Refused automatic creation of database names outside the `mango_dev_*` local workspace prefix.
- Updated generated project release locks for the current Mango release batch:
  - `@mango/pmo@1.0.4`
  - `@mango/workflow@1.0.18`
  - `@mango/grid-widgets@1.0.7`
  - `@mango/workflow-business-example@1.0.17`
  - `@mango/admin-shell@1.0.30`
  - `@mango/admin@1.0.34`

### Breaking / Required Actions

- Existing business projects should upgrade the global CLI with `npm install -g @mango/cli@1.0.51 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- After upgrading, run `mango pmo upgrade --project-dir . --sync-shell` or `mango pmo sync --project-dir . --sync-shell` so the project receives `@mango/pmo@1.0.4`, the current generated startup guidance, and the CLI-owned development entry points.
- Run `mango workspace init` in every active worktree before `mango dev start`; this writes `.mango/workspace.json`, backfills `.mango/dev-workspace.env`, and creates `mango.dev.json` when a discovered project is missing one.
- Local database auto-create requires a reachable MySQL client and a database name under `mango_dev_*`. Custom database names must be created manually or used with `MANGO_DB_AUTO_CREATE=false`.

### Adoption Verification Plan

- Existing business project: run `mango pmo status --project-dir .` after upgrade and confirm it reports `@mango/pmo@1.0.4`, then run `mango dev plan` and confirm discovered apps, ports, and database name match the generated workspace files.
- Fresh business project: generate a new project with this CLI, verify `release-versions.json`, `README.md`, `AGENTS.md`, and `business-pmo/mango-baseline` describe the CLI-owned workflow, then run `mango workspace init` and `mango dev plan`.

### Verification

- `pnpm --filter @mango/pmo build`
- `pnpm --filter @mango/pmo check`
- `pnpm --filter @mango/cli test`
- `pnpm --filter @mango/cli run check:release-versions`
- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`

## 1.0.50 - 2026-06-28

### Breaking

- Formal local development entry points moved from `scripts/dev-workspace.sh` to Mango CLI namespaces:
  - `mango workspace init|status|list|doctor|release`
  - `mango dev start|stop|status|doctor|plan|logs|backend|frontend`
  - `mango frontend prepare|doctor`
- Local workspace allocation now writes `.mango/workspace.json` and uses `~/.mango/workspaces.json` as the registry. The legacy `~/.mango/workspaces.tsv` registry is read only as migration input and is no longer the source of truth.
- Generated Vite app `dev` scripts no longer hardcode ports. Mango CLI injects the main and child app ports from the workspace slot.
- `scripts/dev-workspace.sh` is now a compatibility shim only. It no longer owns port allocation, frontend preparation, or process ownership rules.

### Fixed

- Added stable workspace slot allocation for backend port, frontend port, child frontend app ports, and local database name so new worktrees do not accidentally reuse another worktree's service or database.
- Added port owner diagnostics that point to the registered worktree when a port is occupied.
- Added `mango frontend prepare` to generate source-mode frontend style artifacts before starting Vite apps.
- Fixed Mango source-mode aliases so `@mango/app-runtime/vue-micro` and package style imports do not require stale `dist` artifacts during local source startup.
- Updated generated project, PMO baseline, hook, and compatibility script guidance to use Mango CLI commands first.

### Upgrade Notes

- Install or upgrade the global CLI with `npm install -g @mango/cli@1.0.50 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- Existing business projects should run `mango pmo sync --project-dir . --sync-shell` or `mango pmo upgrade --project-dir . --sync-shell` to refresh compatibility scripts and PMO guidance.
- After upgrading, run `mango workspace init` in every active worktree. This creates `.mango/workspace.json` and backfills missing values in `.mango/dev-workspace.env`.
- If a worktree was started with old scripts, stop it before starting through the new CLI. The new CLI fails on occupied ports and reports the registered owner instead of silently choosing another port.
- If local tooling depended on hardcoded Vite ports, switch it to read `MANGO_FRONTEND_PORT` or the relevant child app env such as `MANGO_ADMIN_RBAC_APP_PORT`.

### Verification

- `pnpm --filter @mango/cli test`
- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`

## 1.0.49 - 2026-06-28

### Fixed

- Generated `mango.dev.json` for existing business projects from discovered Spring Boot Maven apps and Vite apps instead of assuming fixed `backend` and `frontend` folders.
- Skipped Maven aggregator POMs during development workspace discovery and emitted confirmation warnings when multiple backend or frontend apps are detected.

### Upgrade Notes

- Install or upgrade the global CLI with `npm install -g @mango/cli@1.0.49 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- Existing business projects with renamed backend or frontend folders can run `mango pmo sync --project-dir . --sync-shell` or `mango init-dev` to generate a discovered `mango.dev.json` when no project-owned manifest exists.

### Verification

- `pnpm --filter @mango/cli test`
- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`
- `pnpm release:impact --base=origin/main --head=HEAD`

## 1.0.48 - 2026-06-27

### Fixed

- Updated generated PMO release locks to `@mango/pmo@1.0.3`.
- Published the latest PMO baseline and mango-check no-new-violations baseline metadata so generated and upgraded business projects can sync the same release governance content as Mango main.

### Upgrade Notes

- Install or upgrade the global CLI with `npm install -g @mango/cli@1.0.48 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- Existing business projects should run `mango pmo sync --project-dir .` or `mango pmo upgrade --project-dir .` after upgrading to receive the updated PMO baseline package.

### Verification

- `pnpm --filter @mango/pmo build`
- `pnpm --filter @mango/pmo check`
- `pnpm --filter @mango/cli test`
- `pnpm --filter @mango/cli run check:release-versions`

## 1.0.45 - 2026-06-27

### Fixed

- Updated generated project release locks to consume the Admin Shell directory menu redirect fix:
  - `@mango/admin-shell@1.0.28`
  - `@mango/admin@1.0.32`

### Upgrade Notes

- Install or upgrade the global CLI with `npm install -g @mango/cli@1.0.45 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- Existing business projects should upgrade `@mango/admin-shell` to `1.0.28` when consuming the shell directly, or `@mango/admin` to `1.0.32` when consuming the aggregate package.

### Verification

- `pnpm --filter @mango/cli test`
- `pnpm --filter @mango/cli run check:release-versions`

## 1.0.42 - 2026-06-26

### Fixed

- Updated generated frontend release locks to `@mango/admin@1.0.29`, `@mango/admin-shell@1.0.25`, `@mango/admin-pages@1.0.11`, `@mango/grid-widgets@1.0.3`, and `@mango/system@1.0.10`.
- Ensured new business projects receive the published workbench calendar widget and updated system configuration page through npm packages instead of relying on Mango source-tree rebuilds.

### Upgrade Notes

- Install or upgrade the global CLI with `npm install -g @mango/cli@1.0.42 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- Existing business projects should upgrade frontend dependencies to the same release batch, or regenerate/sync their frontend package locks from this CLI version.

### Verification

- `pnpm --filter @mango/cli test`
- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`

## 1.0.40 - 2026-06-26

### Fixed

- Updated generated PMO release locks to `@mango/pmo@1.0.1`.
- Published the PMO baseline rule requiring multi-package release batches to run shared gates once before per-package publish verification.

### Upgrade Notes

- Install or upgrade the global CLI with `npm install -g @mango/cli@1.0.40 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- Existing business projects should run `mango pmo sync --project-dir .` after upgrading to receive the new release gate.

### Verification

- `pnpm --filter @mango/pmo build`
- `pnpm --filter @mango/pmo check`
- `pnpm --filter @mango/cli test`

## 1.0.36 - 2026-06-25

### Fixed

- Fixed PMO baseline package resolution for pnpm-installed business projects. `mango pmo status/check/sync/upgrade` now resolves `@mango/pmo` through Node package resolution before falling back to the CLI template baseline.
- Added a regression check for the published pnpm layout where `@mango/pmo` is installed beside `@mango/cli` under `.pnpm/.../node_modules/@mango`.

### Upgrade Notes

- Install or upgrade the global CLI with `npm install -g @mango/cli@1.0.36 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- Existing business projects that synced PMO baseline with `@mango/cli@1.0.35` should run `mango pmo sync --project-dir .` again after upgrading, so `business-pmo/mango-baseline` is compared against `@mango/pmo@1.0.0` instead of the CLI template fallback.

### Verification

- `pnpm --filter @mango/cli test`
- `pnpm --filter @mango/pmo check`
- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`

## 1.0.35 - 2026-06-24

### New

- Published the versioned PMO baseline through `@mango/pmo@1.0.0` and made CLI PMO commands consume that package instead of relying on copied source-tree rules.
- Updated generated business projects so PMO baseline checks, upgrades, and compatibility scripts are tied to a published package version.
- Clarified global CLI installation for project creation, historical project upgrades, and cross-repository diagnostics.

### Upgrade Notes

- Install or upgrade the global CLI with `npm install -g @mango/cli@1.0.35 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.
- Existing business projects should run `mango pmo upgrade --project-dir .` from the project root, then reinstall frontend dependencies so the project-local CLI lock is refreshed.
- Generated projects still prefer the project-local CLI through `scripts/dev-workspace.sh`; the global CLI is the bootstrap and upgrade entry.

### Verification

- `pnpm --filter @mango/pmo build`
- `pnpm --filter @mango/pmo check`
- `pnpm --filter @mango/cli test`

## 1.0.34 - 2026-06-19

### New

- Updated generated admin project release locks for the resource registry release:
  - @mango/admin@1.0.23
  - @mango/admin-pages@1.0.10
  - @mango/admin-shell@1.0.20
  - @mango/auth@1.0.8
  - @mango/calendar@1.0.11
  - @mango/common@1.0.10
  - @mango/file@1.0.11
  - @mango/grid-layout@1.0.2
  - @mango/job@1.0.3
  - @mango/notice@1.0.11
  - @mango/numgen@1.0.11
  - @mango/payment@1.0.2
  - @mango/rbac@1.0.8
  - @mango/system@1.0.9
  - @mango/template@1.0.11
  - @mango/workflow@1.0.11
  - @mango/workflow-business-example@1.0.11
- Generated full backend projects now include resource sync starter configuration from the current
  Mango backend SNAPSHOT line.

### Upgrade Notes

- Upgrade CLI before creating new business admin projects so generated dependency locks include
  the resource registry frontend package set.
- Existing business projects should refresh backend Mango SNAPSHOT dependencies and upgrade the
  listed frontend packages together.

### Verification

- `pnpm --filter @mango/cli test`

## 1.0.33 - 2026-06-18

### New

- Updated generated admin project release locks for the role data scope release:
  - @mango/admin@1.0.20
  - @mango/admin-pages@1.0.9
  - @mango/admin-shell@1.0.19
  - @mango/auth@1.0.7
  - @mango/calendar@1.0.10
  - @mango/common@1.0.9
  - @mango/file@1.0.10
  - @mango/grid-layout@1.0.1
  - @mango/job@1.0.2
  - @mango/notice@1.0.10
  - @mango/numgen@1.0.10
  - @mango/rbac@1.0.7
  - @mango/system@1.0.8
  - @mango/template@1.0.10
  - @mango/workflow@1.0.10
  - @mango/workflow-business-example@1.0.10
- New generated admin projects now consume the role data scope RBAC UI and the shared `MangoDialog` package update.

### Upgrade Notes

- Upgrade CLI before creating new business admin projects so generated dependency locks include role data scope support.
- Existing business projects should upgrade `@mango/admin`, `@mango/admin-shell`, `@mango/common`, and `@mango/rbac` together.

### Verification

- `pnpm --filter @mango/cli test`

## 1.0.32 - 2026-06-17

### New

- Updated generated admin project release locks for the Workbench custom grid layout release:
  - @mango/admin@1.0.19
  - @mango/admin-shell@1.0.18
  - @mango/grid-layout@1.0.0
- New generated admin projects now include `@mango/grid-layout` as a core frontend dependency.

### Upgrade Notes

- Upgrade CLI before creating new business admin projects so generated dependency locks include the workbench grid layout package.
- Existing business projects should add or upgrade `@mango/grid-layout@1.0.0` together with `@mango/admin@1.0.19` and `@mango/admin-shell@1.0.18`.

### Verification

- `pnpm --filter @mango/cli test`

## 1.0.31 - 2026-06-12

### New

- Added System Event management to generated admin projects through the updated release locks:
  - @mango/system@1.0.7
  - @mango/admin-pages@1.0.8
  - @mango/admin@1.0.18
- Added reliable transparent domain event delivery support in the backend SNAPSHOT line, including Redis Stream transport, pending message recovery, restart recovery, and Outbox reconsume support.
- Added `mango.dev.json` based development workspace commands:
  - `mango init-dev`
  - `mango validate`
  - `mango doctor`
  - `mango plan [group|app...]`
  - `mango start [group|app...]`
  - `mango stop [app...]`
  - `mango status`
  - `mango logs <app>`
- New generated projects include `mango.dev.json` as the committed app startup manifest.
- `scripts/dev-workspace.sh` is now a compatibility shim; the real startup runner lives in the Mango CLI.
- `mango pmo sync --sync-shell` now installs `mango.dev.json` when missing and does not overwrite a business-owned manifest.

### Fixed

- Backend development startup now uses the explicit Spring Boot Maven plugin coordinate from `mango.dev.json`, avoiding Maven prefix resolution failures.
- App stop/status/logs now use `.mango/run/pids` and `.mango/run/logs` instead of killing by port, reducing the risk of stopping unrelated local services.

### Upgrade Notes

- Upgrade CLI first, then run `mango changelog`.
- Existing business projects should upgrade frontend packages to the versions listed above and refresh backend Mango SNAPSHOT dependencies from the Maven repository.
- Existing business projects should run `mango pmo sync --project-dir <project> --sync-shell`.
- Keep project-specific app names, folders, groups and extra apps in `mango.dev.json`.
- Keep local ports, database settings and secrets in `.mango/dev-workspace.env`.

### Verification

- `mango validate`
- `mango plan`
- `mango pmo sync --project-dir <dir> --sync-shell --dry-run`
- `pnpm --filter @mango/cli test`
- `mvn -pl mango-infra/mango-infra-test -am -Dtest=DomainEventOutboxAutoConfigurationTest,OutboxAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl mango-infra/mango-infra-test -am -Dtest=RedisStreamDomainEventTransportIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test`

## 1.0.30 - 2026-06-11

### Fixed

- Updated generated project release locks to consume the published menu indentation fix:
  - @mango/admin@1.0.17
  - @mango/admin-shell@1.0.16

### Verification

- npm pack --dry-run --json
- npm publish
- npm pack @mango/cli@1.0.30 --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/

## 1.0.29 - 2026-06-11

### New

- Added `mango pmo sync --sync-shell` for existing business projects to sync generated startup shell scripts.
- `pmo sync` keeps shell scripts unchanged by default; shell updates require the explicit `--sync-shell` flag.

### Verification

- `mango pmo sync --project-dir <dir> --sync-shell --dry-run`
- `mango pmo sync --project-dir <dir> --sync-shell`
- `pnpm --filter @mango/cli test`

## 1.0.28 - 2026-06-11

### New

- Added `mango changelog` so business teams can review new features, upgrade notes, and verification steps immediately after upgrading the CLI.
- Added upgrade guidance to `mango --help`.
- Added package-level `CHANGELOG.md` to the published CLI tarball.

### Governance

- Added release readiness checks for npm packages, CLI release locks, generated project template dependencies, admin style aggregation, and Maven artifacts.
- Added PMO release rules requiring all new features to be verified as `PASS` before publishing a new version.
- Release reports must now include published objects, versions, new features, upgrade notes, registry verification, and business consumption verification.

### Verification

- `mango changelog`
- `mango --help`
- `mango pmo sync --project-dir <dir> --sync-shell --dry-run`
- `scripts/check-release-readiness.sh --check-registry`
- `pnpm --filter @mango/cli test`
- `npm pack --dry-run --json`

## 1.0.27 - 2026-06-11

### New

- Added a unified generated project startup entry: `mango dev start`.
- Generated projects now start backend first, wait for backend health, then start frontend with `VITE_ADMIN_PROXY_PATH` pointing to the configured backend port.
- Generated backend startup now uses the explicit Spring Boot Maven plugin goal `org.springframework.boot:spring-boot-maven-plugin:3.5.14:run`, avoiding Maven plugin prefix resolution failures.
- Generated project guidance in `README.md` and `AGENTS.md` now points to `mango dev start`.

### Fixed

- Aligned CLI release locks with published frontend packages:
  - `@mango/admin@1.0.16`
  - `@mango/admin-shell@1.0.15`

### Upgrade Notes

- New projects should use `@mango/cli@1.0.27` or later.
- Existing generated projects are not changed automatically. Regenerate the project or copy the updated generated startup scripts and docs from a project created by `@mango/cli@1.0.27`.
- Existing projects should remove local workarounds that call `spring-boot:run` by prefix and use the explicit plugin coordinate instead.

### Verification

- `mango dev start`
- `mango dev backend`
- `mango dev frontend`
- `bash -n scripts/dev-workspace.sh scripts/backend-dev.sh`
