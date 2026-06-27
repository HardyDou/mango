# Mango Changelog

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
