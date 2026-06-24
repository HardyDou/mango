# Mango Changelog

## v2026.06.24-mango-governance-local-repo - 2026-06-24

### New

- Published the Mango governance baseline as `@mango/pmo@1.0.0` so business projects can consume PMO rules, agents, templates, and preflight tools from the internal npm registry.
- Published `@mango/cli@1.0.35` so project creation, historical project upgrades, and PMO baseline synchronization use the versioned `@mango/pmo` package.
- Added release notes for the local repository publication flow that maps npm, Maven, and GitHub Release records to the same release tag.

### Published Packages

- npm: `@mango/pmo@1.0.0` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
- npm: `@mango/cli@1.0.35` to `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`.
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
- `node scripts/check-release-notes.mjs --package=@mango/cli --version=1.0.35`
- `mvn -f mango/pom.xml -Drevision=1.0.0-SNAPSHOT -DskipTests deploy`
- `npm view @mango/pmo@1.0.0 version --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
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
