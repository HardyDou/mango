# @mango/cli Changelog

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
