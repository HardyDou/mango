# Mango Changelog

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
