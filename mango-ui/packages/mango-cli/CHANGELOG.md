# @mango/cli Changelog

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
