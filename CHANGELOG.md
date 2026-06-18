# Mango Changelog

## v2026.06.18-admin-style-dependency-fix - 2026-06-18

### Fixed

- Fixed `@mango/admin/style.css` package consumption by moving the packages it imports by default from optional peers to direct dependencies.
- Prevented Vite/PostCSS failures where consumers without optional admin modules installed saw unresolved `@mango/grid-layout/style.css`, `@mango/job/style.css`, or `@mango/payment/style.css` imports.

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
