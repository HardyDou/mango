# @mango/cli Changelog

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

- Added a unified generated project startup entry: `scripts/dev-workspace.sh start`.
- Generated projects now start backend first, wait for backend health, then start frontend with `VITE_ADMIN_PROXY_PATH` pointing to the configured backend port.
- Generated backend startup now uses the explicit Spring Boot Maven plugin goal `org.springframework.boot:spring-boot-maven-plugin:3.5.14:run`, avoiding Maven plugin prefix resolution failures.
- Generated project guidance in `README.md` and `AGENTS.md` now points to `scripts/dev-workspace.sh start`.

### Fixed

- Aligned CLI release locks with published frontend packages:
  - `@mango/admin@1.0.16`
  - `@mango/admin-shell@1.0.15`

### Upgrade Notes

- New projects should use `@mango/cli@1.0.27` or later.
- Existing generated projects are not changed automatically. Regenerate the project or copy the updated generated startup scripts and docs from a project created by `@mango/cli@1.0.27`.
- Existing projects should remove local workarounds that call `spring-boot:run` by prefix and use the explicit plugin coordinate instead.

### Verification

- `scripts/dev-workspace.sh start`
- `scripts/dev-workspace.sh backend`
- `scripts/dev-workspace.sh frontend`
- `bash -n scripts/dev-workspace.sh scripts/backend-dev.sh`
