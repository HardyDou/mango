# Mango Changelog

## Unreleased

### Fixed

- Normalize date-only query range boundaries to `00:00:00` and `23:59:59` across shared, Job, Notice, System and Workflow frontend API clients while preserving explicit date-time values.
- Resolve packaged `FILE_ASSET` content from content-addressed `META-INF/mango/files.bundle/objects/{sha256}` resources during Resource Bootstrap instead of treating the packaged classpath object as a source file.

### Changed

- Advance the generated CLI compatibility matrix from Mango Maven `1.0.41` to `1.0.42`; PMO remains `1.4.2`, and the exact npm dependency closure is generated from the release plan.

## v2026.08.27-maven-1.0.42-date-resource-fixes - 2026-08-27

Status: `PENDING`. Publication, registry verification, Tag and GitHub Release remain deferred until the prepared Release PR is merged and the sealed candidate passes pure consume-registry verification.

### Pull Requests

- [PR #878](https://github.com/HardyDou/mango/pull/878) Fixed date-only query ranges across shared, Job, Notice, System and Workflow frontend clients and added compatible backend `LocalDateTime` date binding. Packages: Mango Maven `1.0.42`, `io.mango:mango-docs-bundle:1.0.42`, direct npm packages `@mango/common`, `@mango/job`, `@mango/notice`, `@mango/system`, `@mango/workflow`, their generated dependency closure and `@mango/cli@1.2.2`. Business Adaptation: date-only filters now cover the complete selected days; consumers that need narrower boundaries should continue sending explicit date-time values.
- [PR #880](https://github.com/HardyDou/mango/pull/880) Fixed Resource Bootstrap consumption of packaged `FILE_ASSET` content-addressed objects. Packages: Mango Maven `1.0.42`, `io.mango:mango-docs-bundle:1.0.42`, `@mango/cli@1.2.2`. Business Adaptation: aggregate consumers receive the correction automatically; custom declaration producers must preserve the canonical object-bundle layout and checksum.

### Fixed

- Normalize date-only start and end boundaries to `00:00:00` and `23:59:59` in the shared helper and the Job, Notice, System and Workflow API clients while preserving complete date-time input.
- Accept `yyyy-MM-dd` query parameters for backend `LocalDateTime` binding as the start of that day without changing existing full date-time formats.
- Resolve packaged `FILE_ASSET` bytes from `META-INF/mango/files.bundle/objects/{sha256}` during Resource Bootstrap and retain canonical checksum validation.

### Changed

- Advance the generated CLI Maven compatibility matrix to `1.0.42` and project the complete exact npm dependency closure from the five direct patch Changesets; PMO and its Skill bundle remain at `1.4.2`.

### Versions

- Mango Maven non-application reactor and `io.mango:mango-docs-bundle`: `1.0.41` to `1.0.42`.
- Direct npm packages: `@mango/common` `2.0.0` to `2.0.1`; `@mango/job` `1.0.30` to `1.0.31`; `@mango/notice` `1.0.42` to `1.0.43`; `@mango/system` `1.0.35` to `1.0.36`; `@mango/workflow` `1.0.43` to `1.0.44`.
- Generated dependency closure: `@mango/admin-extension` `1.0.2` to `1.0.3`; `@mango/ai` `1.1.0` to `1.1.1`; `@mango/auth` `1.0.29` to `1.0.30`; `@mango/file` `1.0.36` to `1.0.37`; `@mango/grid-layout` `1.0.19` to `1.0.20`; `@mango/grid-widgets` `1.0.25` to `1.0.26`; `@mango/home` `1.0.17` to `1.0.18`; `@mango/rbac` `1.0.27` to `1.0.28`; `@mango/site-shell` `1.0.15` to `1.0.16`; `@mango/admin-pages` `1.0.36` to `1.0.37`; `@mango/calendar` `1.0.37` to `1.0.38`; `@mango/cms` `1.0.26` to `1.0.27`; `@mango/link` `1.0.23` to `1.0.24`; `@mango/numgen` `1.0.38` to `1.0.39`; `@mango/payment` `1.0.29` to `1.0.30`; `@mango/template` `1.0.38` to `1.0.39`; `@mango/admin-shell` `1.0.64` to `1.0.65`; `@mango/workflow-business-example` `1.0.42` to `1.0.43`; `@mango/admin` `1.1.0` to `1.1.1`.
- `@mango/cli`: `1.2.1` to `1.2.2`. `@mango/pmo` remains `1.4.2` and is not republished.

### Published Packages

1. Mango Maven `--all-non-app` Catalog batch at `1.0.42`, followed by `io.mango:mango-docs-bundle:1.0.42`; application and capability-app fat JARs are excluded.
2. npm topology: `@mango/common@2.0.1` -> `@mango/admin-extension@1.0.3` -> `@mango/ai@1.1.1` -> `@mango/auth@1.0.30` -> `@mango/file@1.0.37` -> `@mango/grid-layout@1.0.20` -> `@mango/grid-widgets@1.0.26` -> `@mango/home@1.0.18` -> `@mango/rbac@1.0.28` -> `@mango/site-shell@1.0.16` -> `@mango/system@1.0.36` -> `@mango/admin-pages@1.0.37` -> `@mango/calendar@1.0.38` -> `@mango/cms@1.0.27` -> `@mango/job@1.0.31` -> `@mango/link@1.0.24` -> `@mango/notice@1.0.43` -> `@mango/numgen@1.0.39` -> `@mango/payment@1.0.30` -> `@mango/template@1.0.39` -> `@mango/workflow@1.0.44` -> `@mango/admin-shell@1.0.65` -> `@mango/workflow-business-example@1.0.43` -> `@mango/admin@1.1.1` -> `@mango/cli@1.2.2`.
3. Tag and GitHub Release `v2026.08.27-maven-1.0.42-date-resource-fixes` are created only after both registry roles and a clean pure-registry consumer verify the sealed candidate.

### Business Impact

- Date-only filters in shared and affected domain clients now include the whole selected start and end days. Existing explicit date-time values remain unchanged, and backend endpoints accept date-only `LocalDateTime` query parameters at the start of day.
- Resource Bootstrap can consume packaged file declarations produced by the build pipeline without trying to treat a classpath object as a local source file. Existing source-tree generation and checksum rules remain unchanged.
- Aggregate consumers receive the aligned Maven/npm/CLI tuple. Direct npm consumers must upgrade exact dependants together according to the generated topology.
- There is no database schema migration, application deployment, traffic change, permission grant, tenant-boundary change or application fat-JAR publication in this component release.

### Upgrade Estimate

- Audience: Mango platform maintainers, generated applications, direct consumers of the affected frontend packages, and custom Resource declaration producers.
- Engineering Effort: 30 to 90 minutes for generated aggregate consumers; 1 to 3 hours for direct consumers with custom date serialization or packaged Resource declarations.
- Execution Window: 1 to 3 hours including dependency upgrade, clean build, date-boundary regression and packaged Resource bootstrap verification.
- Service Downtime: no framework-mandated downtime; consuming applications may use their normal restart window to adopt the Maven and npm tuple.
- Rollback Effort: 15 to 45 minutes to restore Maven `1.0.41`, CLI `1.2.1` and the prior exact npm tuple; no database rollback is required by this batch.
- Assumptions: Node `22.23.1`, Java 21, clean dependency locks, configured publish and consume registries, representative date-range queries and a packaged Resource fixture.

### Upgrade Notes

1. Upgrade the Mango Maven BOM and non-application platform tuple to `1.0.42`; generated projects should use `@mango/cli@1.2.2`, which locks Maven `1.0.42`, PMO `1.4.2` and the generated npm tuple.
2. Upgrade direct npm consumers using the exact dependency closure in the publication topology. Date-only start/end values now cover full days; continue sending complete date-time strings when a narrower bound is required.
3. Custom packaged `FILE_ASSET` declaration producers must preserve `META-INF/mango/files.bundle/objects/{sha256}` and the declaration checksum. Source-tree inputs remain supported during manifest generation.
4. Run focused date-range tests at day boundaries and start a clean aggregate application containing a packaged file declaration; verify the file is materialized with the declared checksum.

### Verification

- Run the Catalog compiler, Git impact checker, release-plan checker, release-notes checker, capability documentation audit and repository local release checks under Node `22.23.1`.
- Prepare one sealed source archive, the complete non-application Maven/docs batch and the generated npm topology; reuse those exact hashes through publication and recovery without rebuilding.
- Require the Release PR checks on the exact prepared tree, then prove the merged `origin/main` tree equals the prepared tree before any registry write.
- Verify Maven POM/checksum evidence and npm integrity in both publish and consume roles, resolve the aggregate Maven consumer once, and run a clean pure consume-registry generated consumer.
- Consumer assertions cover date-only start/end normalization, explicit date-time preservation, backend date binding, packaged file object resolution, checksum validation and missing-object failure.

### Rollback

- Restore the consumer to Maven `1.0.41`, `@mango/cli@1.2.1` and the prior exact npm tuple; keep PMO at the compatible `1.4.2` unless restoring the entire previous tuple.
- No database or business data is changed by publication. Application rollback remains the consuming project's controlled dependency and restart procedure.
- Never overwrite immutable `1.0.42`, `1.2.2` or npm package coordinates. If publication becomes partial or ambiguous, retain the same prepared candidate and use `mango release status` followed by authorized `mango release repair`.

### Audit History

- PR #875 prepared the previous `1.0.41` batch and PR #876 closed it after immutable publication; both belong to the successful baseline and are not release-bearing changes in this batch.
- The previous Tag and Release `v2026.08.27-maven-1.0.41-workflow-resource-release` remain unchanged.

## v2026.08.27-maven-1.0.41-workflow-resource-release - 2026-08-27

Status: `PUBLISHED_AND_VERIFIED`. Canonical manifest SHA-256 `71b74be938f4b0b7c27c1dd0d4baae595d8ea402a43d868f6b75e698082919c6` for plan `748025ecf10314bb44cd42a2e51faec7cbebde1b1d51dcb890dced6ec266d92c` and prepared candidate `ae9058056f54ff6b46a828e25db7bbfbf58ee1213589bc96fe7680ef1fb7a8b5` is `COMPLETED`: all 192 Maven/docs coordinates and `@mango/cli@1.2.1` match the sealed candidate in both publish and consume registries, the pure consume-registry consumer passed, and Tag plus GitHub Release are `CREATED_AND_VERIFIED`.

### Pull Requests

- [PR #872](https://github.com/HardyDou/mango/pull/872) Fixed Workflow business-apply reads to reuse the declared business data-permission contract. Packages: Mango Maven `1.0.41`, `io.mango:mango-docs-bundle:1.0.41`, `@mango/cli@1.2.1`. Business Adaptation: declare the workflow code and provide a custom data-permission provider when the default creator/assignee/handled-user policy is insufficient.
- [PR #873](https://github.com/HardyDou/mango/pull/873) Fixed Resource cross-module menu bootstrap ordering by loading module dependencies from declaration envelopes. Packages: Mango Maven `1.0.41`, `io.mango:mango-docs-bundle:1.0.41`, `@mango/cli@1.2.1`. Business Adaptation: aggregate consumers receive the built-in menu dependencies automatically; direct Resource declarations may use `moduleDependencies` in JSON or `module-dependencies` in YAML.

### Fixed

- Apply the Workflow business data-permission contract consistently to business-apply detail reads and preserve workflow-code identity for reusable provider selection.
- Load and validate Resource declaration module dependencies before topological bootstrap, including consistency checks across multiple files in one module.

### Changed

- Declare cross-module menu prerequisites for the built-in platform Resource manifests.
- Advance the generated CLI Maven compatibility matrix to `1.0.41`; PMO and its Skill bundle remain at `1.4.2`.

### Versions

- Mango Maven non-application reactor and `io.mango:mango-docs-bundle`: `1.0.40` to `1.0.41`.
- `@mango/cli`: `1.2.0` to `1.2.1`.
- `@mango/pmo` remains `1.4.2`; all other npm coordinates remain at the successful `1.0.40` baseline versions and are not republished.

### Published Packages

1. Mango Maven `--all-non-app` Catalog batch at `1.0.41`, followed by `io.mango:mango-docs-bundle:1.0.41`; application and capability-app fat JARs are excluded.
2. `@mango/cli@1.2.1` after the Maven/docs batch.
3. Tag and GitHub Release `v2026.08.27-maven-1.0.41-workflow-resource-release` only after immutable registry and clean-consumer verification.

### Business Impact

- Workflow consumers receive tenant-scoped business-apply read authorization through the default creator/assignee/handled-user policy or a workflow-code-specific custom provider.
- Resource Bootstrap honors declaration-level module dependencies and fails before handler execution on missing, cyclic or conflicting dependencies.
- There is no database schema migration, application deployment, traffic change, automatic permission grant or application fat-JAR publication.

### Upgrade Estimate

- Audience: Mango platform maintainers, generated applications, and direct Workflow or Resource consumers.
- Engineering Effort: 30 to 90 minutes for generated aggregate consumers; 1 to 3 hours for direct consumers with custom permission providers or Resource declarations.
- Execution Window: 1 to 3 hours including dependency upgrade, clean build and focused authorization/bootstrap regression.
- Service Downtime: no framework-mandated downtime; applications may use their normal restart window.
- Rollback Effort: 15 to 45 minutes to restore Maven `1.0.40` and CLI `1.2.0`.
- Assumptions: Node `22.23.1`, Java 21, clean locks, configured registries and representative tenant users.

### Upgrade Notes

1. Upgrade the Maven BOM and non-application platform tuple to `1.0.41`; generated projects should use `@mango/cli@1.2.1` with PMO `1.4.2`.
2. Custom Workflow visibility must declare a stable workflow code and register a matching `WorkflowBusinessApplyDataPermissionProvider`.
3. Direct Resource declarations may add `moduleDependencies` in JSON or `module-dependencies` in YAML; repeated lists for one module must remain identical.
4. Verify allowed and denied tenant users plus successful cross-module menu finalization before application adoption.

### Verification

- Require Catalog, Git impact, release plan, release notes, capability documentation and local release checks under Node `22.23.1`.
- Seal the complete Maven/docs batch and CLI tarball once, require protected checks on the exact prepared tree, and prove the merged tree is identical before publication.
- Verify both registry roles, an aggregate Maven consumer and a clean pure consume-registry generated consumer before Tag or GitHub Release creation.

### Rollback

- Restore Maven `1.0.40` and `@mango/cli@1.2.0`; PMO can remain at compatible `1.4.2`.
- Never overwrite immutable coordinates. For partial or ambiguous publication, retain the same prepared candidate and use `status` then authorized `repair`.

### Audit History

- PR #868 and PR #869 belong to the completed `1.0.40` baseline and are not release-bearing changes in this batch.
- Initial local candidate `6b2e59d29dd3af06336413b38fbb7e9357d4251b4d835f34c26ac5c301ce7ab2` reached `READY` but failed the final governance suite before any remote write because the Workflow business guide duplicated long-term rule wording. The replacement source links the PMO rule source instead.
- The previous Tag and Release `v2026.08.26-maven-1.0.40-ai-resource-platform-release` remain unchanged.

## v2026.08.26-maven-1.0.40-ai-resource-platform-release - 2026-08-26

Status: `PUBLISHED_AND_VERIFIED`. Canonical manifest SHA-256 `7d2830aa837e81454e2f190c7ce68b63885f9e4c92a11a24a6b50d989a56d8a7` for plan `7900477b28fceb4a64efe877743eaf1f5a32c5f48fe62a6390b408be0725c8fa` and prepared candidate `a5c63488c40263eb3258550ce7686ddb0933d7d0cf30c63297eae9e6a23a14a7` is `COMPLETED`: all 192 Maven/docs coordinates and all 28 npm packages match the sealed candidate in both publish and consume registries, the pure consume-registry consumer passed, and Tag plus GitHub Release are `CREATED_AND_VERIFIED`.

### Pull Requests

- [PR #850](https://github.com/HardyDou/mango/pull/850) Fixed release baseline carry-forward and Node runtime validation. Packages: `@mango/cli@1.2.0`. Business Adaptation: release operators must use Node `22.23.1` and retain completed Maven evidence across npm-only closeout.
- [PR #852](https://github.com/HardyDou/mango/pull/852) Added build-time Resource manifests, content-addressed file bundles and environment-level module receipts. Packages: Mango Maven `1.0.40`, `io.mango:mango-docs-bundle:1.0.40`, `@mango/pmo@1.4.2`. Business Adaptation: generated and existing applications must run the Resource Bootstrap candidate/finalize lifecycle and preserve receipt storage across restarts.
- [PR #853](https://github.com/HardyDou/mango/pull/853) Fixed historical `AUTH_ROLE_DATA_SCOPE` FINALIZE cleanup by persisted target identity. Packages: Mango Maven `1.0.40`, `io.mango:mango-docs-bundle:1.0.40`. Business Adaptation: operators must repair inconsistent historical Registry target identities instead of relying on a business-key fallback.
- [PR #854](https://github.com/HardyDou/mango/pull/854) Added the Spring AI management platform and unified service workbench. Packages: Mango Maven `1.0.40`, `@mango/ai-api@1.1.0`, `@mango/ai@1.1.0`, `@mango/http-client@1.0.1`, `@mango/common@2.0.0`, `@mango/admin-shell@1.0.64`, `@mango/admin@1.1.0`, `@mango/cli@1.2.0`. Business Adaptation: configure an enabled AI provider/model, inject the required Chat stream provider, and verify authenticated chat, attachment and structured-service flows.
- [PR #855](https://github.com/HardyDou/mango/pull/855) Changed the release intent, public component contracts and consumer migration documentation for the AI batch. Packages: all 28 npm coordinates in this plan plus Mango Maven `1.0.40`. Business Adaptation: upgrade the complete generated tuple rather than selecting only the eight directly declared packages.
- [PR #858](https://github.com/HardyDou/mango/pull/858) Fixed AI service request IDs to use Mango's Web Crypto compatibility API. Packages: `@mango/ai@1.1.0`, `@mango/admin@1.1.0`, `@mango/cli@1.2.0`. Business Adaptation: none; consumers receive the browser compatibility correction through the complete tuple.
- [PR #860](https://github.com/HardyDou/mango/pull/860) Fixed deterministic formatting of the canonical PMO Codex plugin manifest. Packages: `@mango/pmo@1.4.2`, `@mango/cli@1.2.0`. Business Adaptation: none; plugin capabilities and configuration remain unchanged.
- [PR #861](https://github.com/HardyDou/mango/pull/861) Fixed the frontend standards recovery lock to cover the complete 32-package candidate set. Packages: release-governance configuration only. Business Adaptation: none; this restores release verification without changing consumer runtime behavior.
- [PR #863](https://github.com/HardyDou/mango/pull/863) Fixed successful-baseline anchoring after protected squash merges by resolving an exact first-parent tree match. Packages: release-planner source only. Business Adaptation: none; release operators receive fail-closed planning across squash-merged Release PRs.
- [PR #865](https://github.com/HardyDou/mango/pull/865) Fixed Release PR classification on clean runners by installing pnpm before deterministic PMO projection. Packages: release workflow and validation tests only. Business Adaptation: none; this restores the protected Release PR check on a clean Runner.
- [PR #867](https://github.com/HardyDou/mango/pull/867) Fixed Release PR classification by installing the frozen `mango-ui` dependency graph before Catalog projection. Packages: release workflow only. Business Adaptation: none; clean runners can import the declared TypeScript dependency during protected classification.

### Fixed

- Preserve completed Maven evidence when a prior npm-only closeout becomes the baseline for the next release plan.
- Disable historical Authorization role data scopes by the exact persisted Registry target and owning tenant, while failing closed on inconsistent identity fields.
- Keep browser streaming responses alive until the consumer finishes or cancels the stream.
- Use the browser-compatible UUID helper for AI service request correlation.
- Keep the PMO plugin manifest deterministic under the release formatter.
- Cover the complete npm candidate set in dependency recovery and anchor squash-merged successful baselines by exact tree identity.
- Install pnpm and the frozen frontend dependency graph before Release PR projection classification on a clean CI Runner.

### Added

- Add build-time Resource module manifests, content-addressed file bundles, incremental receipts and recoverable EXPAND/FINALIZE processing.
- Add Spring AI provider/model configuration, prompt and skill management, chat conversations, attachments, model switching and structured AI service invocation.
- Add the AI frontend packages and their Admin aggregation, CLI module projection and public component contracts.

### Changed

- Require `ChatProps.stream` consumers to provide an explicit `ChatStreamProvider`; this is the intentional `@mango/common@2.0.0` compatibility break.
- Make release commands reject unsupported Node runtimes before plan, lock or registry access.
- Correct PMO Maven impact classification so nested test-fixture POMs under source trees are not treated as Reactor modules.
- Publish the full non-application Maven platform batch because the successful baseline-to-candidate Git impact includes Maven production sources.

### Versions

- Mango Maven non-app reactor and `io.mango:mango-docs-bundle`: `1.0.39` to `1.0.40`.
- Direct npm packages: `@mango/ai-api` `1.0.0` to `1.1.0`; `@mango/http-client` `1.0.0` to `1.0.1`; `@mango/common` `1.0.27` to `2.0.0`; `@mango/ai` `1.0.0` to `1.1.0`; `@mango/pmo` `1.4.1` to `1.4.2`; `@mango/admin-shell` `1.0.63` to `1.0.64`; `@mango/admin` `1.0.69` to `1.1.0`; `@mango/cli` `1.1.1` to `1.2.0`.
- Dependency closure: `@mango/admin-extension` `1.0.1` to `1.0.2`; `@mango/auth` `1.0.28` to `1.0.29`; `@mango/file` `1.0.35` to `1.0.36`; `@mango/grid-layout` `1.0.18` to `1.0.19`; `@mango/grid-widgets` `1.0.24` to `1.0.25`; `@mango/home` `1.0.16` to `1.0.17`; `@mango/rbac` `1.0.26` to `1.0.27`; `@mango/site-shell` `1.0.14` to `1.0.15`; `@mango/system` `1.0.34` to `1.0.35`; `@mango/admin-pages` `1.0.35` to `1.0.36`; `@mango/calendar` `1.0.36` to `1.0.37`; `@mango/cms` `1.0.25` to `1.0.26`; `@mango/job` `1.0.29` to `1.0.30`; `@mango/link` `1.0.22` to `1.0.23`; `@mango/notice` `1.0.41` to `1.0.42`; `@mango/numgen` `1.0.37` to `1.0.38`; `@mango/payment` `1.0.28` to `1.0.29`; `@mango/template` `1.0.37` to `1.0.38`; `@mango/workflow` `1.0.42` to `1.0.43`; `@mango/workflow-business-example` `1.0.41` to `1.0.42`.

### Published Packages

1. Mango Maven `--all-non-app` Catalog batch: 191 non-application reactor coordinates at `1.0.40`, followed by `io.mango:mango-docs-bundle:1.0.40`; application and capability-app fat JARs are excluded.
2. npm topology: `@mango/ai-api@1.1.0` -> `@mango/http-client@1.0.1` -> `@mango/common@2.0.0` -> `@mango/admin-extension@1.0.2` -> `@mango/ai@1.1.0` -> `@mango/auth@1.0.29` -> `@mango/file@1.0.36` -> `@mango/grid-layout@1.0.19` -> `@mango/grid-widgets@1.0.25` -> `@mango/home@1.0.17` -> `@mango/pmo@1.4.2` -> `@mango/rbac@1.0.27` -> `@mango/site-shell@1.0.15` -> `@mango/system@1.0.35` -> `@mango/admin-pages@1.0.36` -> `@mango/calendar@1.0.37` -> `@mango/cms@1.0.26` -> `@mango/job@1.0.30` -> `@mango/link@1.0.23` -> `@mango/notice@1.0.42` -> `@mango/numgen@1.0.38` -> `@mango/payment@1.0.29` -> `@mango/template@1.0.38` -> `@mango/workflow@1.0.43` -> `@mango/admin-shell@1.0.64` -> `@mango/workflow-business-example@1.0.42` -> `@mango/admin@1.1.0` -> `@mango/cli@1.2.0`.
3. Tag and GitHub Release `v2026.08.26-maven-1.0.40-ai-resource-platform-release` are created only after both registry roles and a clean pure-registry consumer verify the sealed candidate.

### Business Impact

- AI consumers gain provider/model administration, prompt/skill management, authenticated streaming chat, attachment handling and structured service execution. Built-in providers remain disabled without valid configuration.
- `@mango/common` Chat consumers must provide a stream adapter; aggregate consumers receive the matching Admin and CLI projections in the same tuple.
- Resource Bootstrap persists module receipts and file-bundle identities. Existing application databases are upgraded through Flyway and the managed Bootstrap lifecycle; this release does not reset or delete business data.
- Authorization cleanup is stricter: stale or inconsistent Registry target identities stop explicitly and require data correction.
- There is no application deployment, traffic change, automatic role grant or application fat-JAR publication in this component release.

### Upgrade Estimate

- Audience: Mango platform maintainers and business applications consuming AI, Resource Bootstrap, Authorization, Admin, CLI or PMO artifacts.
- Engineering Effort: 1 to 3 hours for generated aggregate consumers; 3 to 8 hours for direct package consumers that must add a Chat stream provider and configure AI models.
- Execution Window: 2 to 6 hours including dependency upgrade, database migration in a non-production environment, focused regression and registry-only consumer verification.
- Service Downtime: no framework-mandated downtime; applications may choose a normal restart window for Maven and Flyway adoption.
- Rollback Effort: 30 to 90 minutes to restore the prior compatible tuple and application configuration; database rollback depends on the consumer's verified backup and migration policy.
- Assumptions: Node `22.23.1`, Java 21, clean dependency locks, configured publish/consume registries, a verified database backup for application adoption, and valid provider credentials supplied outside source control.

### Upgrade Notes

1. Upgrade Maven to `1.0.40` and the complete npm tuple in the published topology; do not combine `@mango/common@2.0.0` with older Admin or CLI projections.
2. Direct Chat consumers must implement and pass `ChatStreamProvider` through the required `stream` prop; verify success, cancellation and error completion.
3. Configure at least one enabled AI provider and compatible model before using chat or structured services. Keep provider secrets in the consumer's secret-management path.
4. Run Flyway and Resource Bootstrap against a non-production database first, then verify module receipts, file bundles and FINALIZE behavior. Correct stale Authorization Registry target identities if the fail-closed check reports a mismatch.
5. Upgrade PMO and CLI together to `@mango/pmo@1.4.2` and `@mango/cli@1.2.0`, then run the project PMO upgrade/check and generated-project verification entry points.

### Verification

- Recompute the Catalog-bound plan and require digest equality after release notes, package versions, lockfile, PMO projection and CLI release matrix are committed.
- Prepare must seal one source archive, all 192 Maven/docs coordinates and all 28 npm tarballs, then pass mixed local-artifact plus consume-registry validation without rebuilding.
- The Release PR must pass `frontend-pr-quality`, `pmo-doc-check` and `pr-contract-check` on the exact prepared tree.
- Publish must verify POM/checksum evidence for every Maven coordinate, integrity for every npm tarball in both roles, an aggregate Maven consumer and a clean pure consume-registry business consumer before Tag or GitHub Release creation.
- Representative consumer acceptance must cover Resource Bootstrap receipt reuse, Authorization historical data-scope cleanup, Admin AI configuration, authenticated streaming chat, attachments, structured service execution and production frontend build.

### Rollback

- Restore Maven `1.0.39`, `@mango/common@1.0.27`, `@mango/ai-api@1.0.0`, `@mango/ai@1.0.0`, `@mango/admin@1.0.69`, `@mango/admin-shell@1.0.63`, `@mango/pmo@1.4.1`, `@mango/cli@1.1.1` and the corresponding prior dependency closure; never overwrite immutable `1.0.40` or npm coordinates.
- Preserve and restore the consumer database only through its verified backup/migration procedure. The Mango artifact publisher does not roll back or delete application data.
- If publication state becomes partial or ambiguous, keep the same prepared candidate and use `mango release status` followed by authorized `mango release repair`; do not rebuild or republish verified coordinates.

### Audit History

- PR #849 closed the previous PMO/CLI publication and is not a release-bearing change in this batch.
- PR #866 was a superseded Release PR candidate that never merged and performed no registry, Tag or GitHub Release write.
- The first publication process dispatched all 192 Maven coordinates successfully, then stopped while recording command evidence because the result loop did not resolve each coordinate's publication record. Read-only status proved every dispatched Maven coordinate matched the sealed candidate in both registry roles; authorized repair reused the same candidate, skipped all verified Maven writes, published the untouched npm tuple and completed consumer, Tag and GitHub Release verification. The closeout fixes that result-record lookup for future releases.
- The previous immutable Tag and Release `v2026.08.22-pmo-1.4.1-cli-1.1.1-release` remain unchanged.

## v2026.08.22-pmo-1.4.1-cli-1.1.1-release - 2026-08-22

Status: `PUBLISHED_AND_VERIFIED`. Canonical manifest SHA-256 `3b4b695c86f9ff24763423696ab543dcf6483d25a3e5e5656593328902cc622f` for plan `a948d84b41cbdb2dcedad09e874440c46a94cc2b9c154ac182f65a08bcc99692` and prepared candidate `a89efa10c81dbd8c35a264d8a0f2da944f629fa8c29f92233a985ec8510d99b2` is `COMPLETED`: `@mango/pmo@1.4.1` and `@mango/cli@1.1.1` match their sealed SHA-256/SRI in both npm registry roles, the pure consume-registry consumer passed, and Tag plus GitHub Release are `CREATED_AND_VERIFIED`. Mango Maven and `io.mango:mango-docs-bundle` remain at `1.0.39`.

### Pull Requests

- [PR #847](https://github.com/HardyDou/mango/pull/847) Changed Mango release verification to use bounded parallel Maven publication, POM-first remote checks and one aggregate clean Maven consumer. Packages: `@mango/pmo@1.4.1`, `@mango/cli@1.1.1`. Business Adaptation: release operators use default `basic` Maven verification and opt into `full` only for a per-JAR audit.
- [PR #848](https://github.com/HardyDou/mango/pull/848) Published and verified the immutable PMO/CLI npm batch. Merge commit: `0492deb336ba38c939aae0f8a200d33ad46f74bb`.

### Fixed

- Keep Maven release evidence complete while removing redundant per-JAR remote downloads from the default verification path.

### Added

### Changed

- Bound Maven publication and consume-registry visibility concurrency at 16 by default, preserving per-coordinate journal and recovery evidence.
- Use one aggregate clean Maven consumer for the sealed batch.

### Versions

- `@mango/pmo`: `1.4.0` to `1.4.1`.
- `@mango/cli`: `1.1.0` to `1.1.1`.
- Maven remains `1.0.39`; no Maven coordinate is republished.

### Published Packages

1. `@mango/pmo@1.4.1`.
2. `@mango/cli@1.1.1`.
3. No Maven, application fat JAR or unchanged npm coordinate is published.

### Business Impact

- Release operators receive a shorter default Maven verification path with the same immutable journal and fail-closed recovery behavior.
- Runtime consumers have no API, database, permission, tenant or service configuration migration.

### Upgrade Estimate

- Audience: Mango maintainers and release operators; runtime consumers do not need an upgrade.
- Engineering Effort: 10 to 20 minutes for PMO/CLI upgrade and verification.
- Execution Window: 15 to 30 minutes including clean consumer and registry back-checks.
- Service Downtime: none.
- Rollback Effort: 5 to 15 minutes to restore PMO `1.4.0` and CLI `1.1.0`.
- Assumptions: configured npm registries, clean locks and Maven `1.0.39` consume access.

### Upgrade Notes

1. Upgrade the release operator tuple to `@mango/pmo@1.4.1` and `@mango/cli@1.1.1` together.
2. Keep Maven at `1.0.39` for this npm-only candidate.
3. Use `MANGO_RELEASE_MAVEN_VERIFY_MODE=full` only for an explicitly requested per-JAR audit.

### Verification

- `pnpm -C mango-ui release:test` passed `80/80`, including bounded Maven concurrency, verification modes and aggregate consumer generation.
- Release PR #848 passed `frontend-pr-quality`, `pmo-doc-check` and `pr-contract-check`; merge commit `0492deb336ba38c939aae0f8a200d33ad46f74bb` retained prepared source commit `8998c5dcf1a6a94d9f95007d5f04d20cb551ccb8` and tree `ee41a5f69998f9dba737eec14224dd4be20b5af8`.
- The completed manifest records `VERIFIED` publication journal entries for both sealed npm tarballs. Hosted and group registry reads match `@mango/pmo@1.4.1` SHA-256 `530492ab4be19d74a313b0cd08b94a22ae708b7605bf756311e4b3c16d47a1dd` and `@mango/cli@1.1.1` SHA-256 `d374f37aaf25bb0c2138632967e1140cf406596af3359934bcab1cabeb376be2`.
- A clean consumer resolved only the consume-registry tuple and passed typecheck plus production build before immutable publication completion.
- Tag [`v2026.08.22-pmo-1.4.1-cli-1.1.1-release`](https://github.com/HardyDou/mango/releases/tag/v2026.08.22-pmo-1.4.1-cli-1.1.1-release) points to the prepared source and the non-draft, non-prerelease GitHub Release is `CREATED_AND_VERIFIED`.
- Local audit evidence: `.runtime/mango-release/a948d84b41cbdb2dcedad09e874440c46a94cc2b9c154ac182f65a08bcc99692/manifest.json`.

### Rollback

- Restore PMO `1.4.0` and CLI `1.1.0`; never overwrite or republish immutable coordinates.

### Audit History

- This closeout updates the successful-release baseline and documentation only; it does not republish any coordinate or move the immutable Tag/Release.

## v2026.08.22-maven-1.0.39-platform-content-fileproc-release - 2026-08-22

Status: `PUBLISHED_AND_VERIFIED`. Canonical manifest SHA-256 `7848cf85826e9aac4e62835a1c355d567a71759dc96f4164c74f24e9cba2e47b` for plan `b0ffa4e09c67dcacb98e70f3c9e8f02600a988603393619796c1a78300ca8c7c` and prepared candidate `9e6023400991ee7b8007a8b5297b42b113071df4c00d984174d176bf8c40ef7f` is `COMPLETED`: all 192 sealed Mango Maven non-app/docs coordinates at `1.0.39` and all five sealed npm packages ending at `@mango/cli@1.1.0` match in their publish and consume registries, the pure consume-registry consumer passed, and GitHub Release `v2026.08.22-maven-1.0.39-platform-content-fileproc-release` is `CREATED_AND_VERIFIED`.

### Pull Requests

- [PR #839](https://github.com/HardyDou/mango/pull/839) Changed the PMO frontend-page baseline opt-out contract. Packages: `@mango/pmo@1.4.0`, `@mango/cli@1.1.0`. Business Adaptation: upgrade the tuple and use only the explicit boolean opt-out.
- [PR #840](https://github.com/HardyDou/mango/pull/840) Fixed Admin Shell confirmation cancellation and close handling. Packages: `@mango/admin-shell@1.0.63`, `@mango/admin@1.0.69`, `@mango/cli@1.1.0`. Business Adaptation: verify cancel/close behavior in shell-created applications.
- [PR #841](https://github.com/HardyDou/mango/pull/841) Changed the frontend-quality workflow pause contract. Packages: release tooling source only. Business Adaptation: none; maintainers follow the stable required-check policy.
- [PR #843](https://github.com/HardyDou/mango/pull/843) Added OFD conversion and document signing. Packages: Maven non-app reactor and `io.mango:mango-docs-bundle:1.0.39`, `@mango/template@1.0.37`. Business Adaptation: configure and test signing material in a non-production environment.
- [PR #844](https://github.com/HardyDou/mango/pull/844) Changed Catalog-bound release planning and sealed publication recovery. Packages: `@mango/pmo@1.4.0`, `@mango/admin@1.0.69`, `@mango/template@1.0.37`, `@mango/cli@1.1.0`. Business Adaptation: none for runtime consumers; release operators use the latest state machine.

### Added

- Add OFD conversion and document-signing APIs, providers, starter wiring and capability documentation.

### Fixed

- Treat confirmation cancellation and dialog close as expected Admin Shell interactions.

### Changed

- Make the PMO frontend-page baseline opt-out explicit and fail-closed.
- Make the Catalog and sealed candidate manifest the source of truth for platform publication and recovery.

### Versions

- Maven non-app reactor and docs bundle: `1.0.38` to `1.0.39`.
- `@mango/admin-shell`: `1.0.62` to `1.0.63`; `@mango/pmo`: `1.3.16` to `1.4.0`; `@mango/template`: `1.0.36` to `1.0.37`; `@mango/admin`: `1.0.68` to `1.0.69`; `@mango/cli`: `1.0.111` to `1.1.0`.

### Published Packages

1. Mango Maven `--all-non-app` reactor and `io.mango:mango-docs-bundle:1.0.39` — `PUBLISHED_AND_VERIFIED`; all 192 coordinates match through Maven releases/public.
2. `@mango/admin-shell@1.0.63` -> `@mango/pmo@1.4.0` -> `@mango/template@1.0.37` -> `@mango/admin@1.0.69` -> `@mango/cli@1.1.0` — `PUBLISHED_AND_VERIFIED`; npm hosted/group integrity matches every sealed tarball.
3. Tag and GitHub Release `v2026.08.22-maven-1.0.39-platform-content-fileproc-release` — `CREATED_AND_VERIFIED` after pure consume-registry verification.

### Business Impact

- Consumers gain OFD conversion/signing and corrected Admin Shell cancellation semantics. PMO consumers gain an explicit frontend baseline opt-out; no application fat JAR or production database operation is included.

### Upgrade Estimate

- Audience: consumers of fileproc/docsign, Admin Shell or PMO/CLI governance.
- Engineering Effort: 30 to 90 minutes for generated consumers; 2 to 4 hours for signing integrations.
- Execution Window: 1 to 3 hours for upgrade, focused tests and consume-registry verification.
- Service Downtime: none required by the framework artifacts.
- Rollback Effort: 15 to 60 minutes to restore the prior compatible tuple.
- Assumptions: registry access, clean locks, compatible JDK/PDF/OFD libraries and non-production signing fixtures.

### Upgrade Notes

1. Upgrade Maven to `1.0.39` and install `@mango/cli@1.1.0` or the exact managed npm matrix.
2. Keep the PMO baseline enabled unless an explicit boolean `false` is reviewed and intended.
3. Validate OFD conversion, signing, verification and Admin Shell cancel/close behavior before business deployment.

### Verification

- Release PR #845 passed all seven required checks; merge commit `922a8e7cff981b9fccf185afa6eeba1b3d964a71` retained sealed source tree `c024dbf3542922ff74c54468944f9311fa60c5df`.
- Publication journal records `VERIFIED` for all 192 Maven coordinates and all five npm packages; read-only `release status` confirmed the publish and consume registry copies.
- A clean consumer resolved the Maven and npm tuple only through Maven public and npm group before Tag or GitHub Release creation.
- Tag `v2026.08.22-maven-1.0.39-platform-content-fileproc-release` points to sealed source commit `8dfabade82fd8cf7de8ec1b267085b1e091367ce`; its tree matches `origin/main`, and the GitHub Release is non-draft and non-prerelease.

### Rollback

- Restore Maven `1.0.38`, CLI `1.0.111`, PMO `1.3.16`, Admin Shell `1.0.62`, Template `1.0.36` and Admin `1.0.68`; never overwrite immutable coordinates.

### Audit History

- Release PR #845 sealed and published the final candidate; this closeout updates the successful-release baseline and documentation only and does not move the release tag.

## v2026.08.19-maven-1.0.38-wecom-identity-notice-security-release - 2026-08-19

Status: `PUBLISHED_AND_VERIFIED`. Canonical manifest SHA-256 `b0eebb391ca1302d9f6e78016c34226b98bac36a01402477d704986e2bc89acf` for plan `5adf1a66c8b2a658903015f8a9c91a6041b66ade7dffc96283074650135a7a5f` is `COMPLETED`: all 188 sealed Mango Maven non-app/docs coordinates at `1.0.38` and all 17 sealed npm packages ending at `@mango/cli@1.0.111` match in their publish and consume registries, the pure consume-registry consumer passed, and GitHub Release `v2026.08.19-maven-1.0.38-wecom-identity-notice-security-release` is `CREATED_AND_VERIFIED`.

### Pull Requests

- [PR #827](https://github.com/HardyDou/mango/pull/827) Fixed Notice channel configuration so non-secret values round-trip while manually configured secrets are encrypted, masked by default, revealed only through an authorized single-field API, and audited. Packages: Mango Maven `1.0.38`, `@mango/notice@1.0.41`, exact runtime dependents, and `@mango/cli@1.0.111`. Business Adaptation: upgrade the aligned backend/frontend matrix, grant `notice:channel:secret:view` only to authorized administrators, configure Mango Crypto before saving manual secrets, and verify existing channels through the management page.
- [PR #828](https://github.com/HardyDou/mango/pull/828) Fixed the Notice JSON editor so configured secret fields retain the same mask and one-field reveal semantics as form mode. Packages: `@mango/notice@1.0.41`, exact runtime dependents, and `@mango/cli@1.0.111`. Business Adaptation: direct Notice consumers must upgrade the complete frontend tuple and verify both form and JSON channel editing modes.
- [PR #829](https://github.com/HardyDou/mango/pull/829) Changed WeCom identity ownership so profile display and delivery use the Identity binding snapshot, profile refresh updates only the current binding, and administrative synchronization maintains Identity bindings instead of a parallel Notice account source. Packages: Mango Maven `1.0.38`, `@mango/auth@1.0.28`, `@mango/rbac@1.0.26`, `@mango/notice@1.0.41`, exact runtime dependents, and `@mango/cli@1.0.111`. Business Adaptation: upgrade Maven and the complete npm tuple together, back up the database, let Flyway apply Identity V3/V4, configure WeCom member-read visibility and trusted IPs where refresh is enabled, and validate delivery against authorized test credentials.
- [PR #830](https://github.com/HardyDou/mango/pull/830) Changed the WeCom profile guide to state the actual runtime ownership boundary. Packages: `io.mango:mango-docs-bundle:1.0.38`. Business Adaptation: no code or configuration change; use the corrected guide for profile refresh validation.
- [PR #832](https://github.com/HardyDou/mango/pull/832) Changed the outer Release-only required check registry route; the next protected Release PR proved nested pnpm still used the internal registry. Packages: Mango release tooling source only; no standalone coordinate. Business Adaptation: none; business runtime behavior is unchanged.
- [PR #834](https://github.com/HardyDou/mango/pull/834) Fixed nested pnpm registry propagation with `PNPM_CONFIG_REGISTRY` and added a workflow regression contract. Packages: Mango release tooling source only; no standalone coordinate. Business Adaptation: none; published package contents and business runtime behavior are unchanged.

### Fixed

- Preserve non-sensitive Notice channel configuration values while preventing ordinary configuration reads from returning secret plaintext; encrypt manual secrets, mask them in form/JSON modes, and constrain one-field reveal by permission, tenant, whitelist, audit and `no-store`.
- Show complete WeCom identity nickname/avatar snapshots and refresh only the current binding without destroying a valid snapshot when provider lookup fails.
- Keep the protected Release-only frozen lockfile check runnable on GitHub-hosted runners by propagating the public registry to nested pnpm without weakening its blocking semantics.

### Changed

- Make Identity external bindings the single WeCom identity source for profile display, administrative synchronization and Notice delivery; apply bounded Identity V3/V4 migrations.
- Publish exact-version frontend dependents and CLI matrix generated by the release plan so immutable aggregators never point at the previous direct package versions.
- Include the corrected WeCom profile refresh guide in `io.mango:mango-docs-bundle:1.0.38`.

### Versions

- Mango Maven non-app reactor and docs bundle: `1.0.37` to `1.0.38`.
- Direct npm: Auth `1.0.27` to `1.0.28`; RBAC `1.0.25` to `1.0.26`; Notice `1.0.40` to `1.0.41`.
- Dependency closure: System `1.0.34`, Admin Pages `1.0.35`, Calendar `1.0.36`, CMS `1.0.25`, Job `1.0.29`, Link `1.0.22`, Numgen `1.0.37`, Payment `1.0.28`, Template `1.0.36`, Workflow `1.0.42`, Admin Shell `1.0.62`, Workflow Example `1.0.41`, Admin `1.0.68`.
- CLI: `1.0.110` to `1.0.111`; PMO remains `1.3.16` and absent npm coordinates remain unchanged.

### Published Packages

1. Mango Maven `1.0.38` complete non-app reactor and `io.mango:mango-docs-bundle:1.0.38`.
2. `@mango/auth@1.0.28`, `@mango/rbac@1.0.26`, `@mango/system@1.0.34`, `@mango/admin-pages@1.0.35`.
3. `@mango/calendar@1.0.36`, `@mango/cms@1.0.25`, `@mango/job@1.0.29`, `@mango/link@1.0.22`, `@mango/notice@1.0.41`, `@mango/numgen@1.0.37`, `@mango/payment@1.0.28`, `@mango/template@1.0.36`, `@mango/workflow@1.0.42`.
4. `@mango/admin-shell@1.0.62`, `@mango/workflow-business-example@1.0.41`, `@mango/admin@1.0.68`, `@mango/cli@1.0.111`.

### Business Impact

- Notice secrets are masked by default and temporary reveal requires explicit permission and audit. Existing manual plaintext migrates lazily when the affected channel is saved, used or revealed.
- WeCom profile and delivery identity now share Identity bindings. Other providers and non-WeCom notification channels retain existing behavior.
- Existing databases upgrade through bounded Identity V3/V4 migrations. Consumers upgrade Maven and the complete npm tuple together; no application fat JAR or business deployment is part of this release.

### Upgrade Estimate

- Audience: Mango consumers using Auth/Identity, RBAC, Notice channels, WeCom login/delivery or the full CLI matrix.
- Engineering Effort: 1 to 2 hours for generated consumers; 2 to 4 hours for custom dependency, permission, Crypto or WeCom integration configurations.
- Execution Window: 1 to 3 hours for backup, dependencies, Flyway, API/UI checks, build and authorized WeCom smoke test.
- Service Downtime: no framework-mandated downtime; use the application's normal migration window when Flyway is not run during rolling startup.
- Rollback Effort: 30 to 90 minutes for the prior tuple and build; database backup restore only if migration validation requires it.
- Assumptions: registry access, clean lock, configured Mango Crypto, database backup and authorized WeCom test configuration.

### Upgrade Notes

1. Back up the database and count/sample the bounded legacy WeCom `SELF` display-name rows.
2. Upgrade all backend Mango dependencies to `1.0.38`, then install CLI `1.0.111` or its complete exact npm matrix.
3. Apply Flyway Identity V3/V4 and verify unrelated providers/sources remain unchanged.
4. Verify Notice form/JSON masks, authorized audited reveal, and Resource/environment reference non-resolution.
5. Before business deployment, verify profile refresh and one real Notice delivery in an authorized WeCom test tenant.

### Verification

- Release PR #836 passed `frontend-pr-quality`, `pmo-doc-check` and `pr-contract-check`; merge commit `aac207e348e2eb25088dce6ee2d49e3821609c40` retained the sealed source tree `0242da120f4af9dafedc3f67b8b2f240927ff2e5`.
- All 188 Maven `1.0.38` coordinates and all 17 npm coordinates were read back from both publish and consume registries with the sealed content, then a clean consumer installed only the consume-registry tuple.
- Tag `v2026.08.19-maven-1.0.38-wecom-identity-notice-security-release` points to sealed source commit `df2a58d3a75c167f4bc80849b47dc959e68ee232`; the non-draft, non-prerelease GitHub Release body passed the same release-notes checker.
- Repository tests cover the Auth/Identity/Notice backend contracts and Auth/RBAC/Notice frontend behavior. Real provider acceptance remains required before business deployment because release verification does not use production WeCom credentials.

### Rollback

- Restore Maven `1.0.37`, CLI `1.0.110` and the prior exact npm matrix; restore the database backup only under the business deployment runbook if migration validation fails.
- Never overwrite immutable coordinates or move the release tag; correct defects with a new patch release.

### Audit History

- PR #822 and PR #823 are part of the completed Link Page batch and are excluded from this release.
- Release PR #831 and its r2 manifest were superseded after PR #832 changed the source tree; no registry coordinate, Tag, or GitHub Release was written from that candidate.
- Release PR #833 and its r3 manifest were stopped after the required check proved #832 did not propagate the public registry to nested pnpm; PR #834 fixed the gate, and r3 retained `remoteWrites=false` with no registry coordinate, Tag, or GitHub Release written.
- Release PR #836 sealed and published the final r4 candidate; this closeout changes documentation only and does not move the release tag.

## v2026.08.17-issue-817-link-page-tenant-id-release - 2026-08-17

Status: `PUBLISHED_AND_VERIFIED`. Canonical manifest `e9f06a730d474766a8ccf415045edb1be256efadb114ea19a836a7d00f63fc73` is `COMPLETED`: both sealed npm tarballs match in npm-hosted and npm-group, the pure consume-registry consumer passed, and GitHub Release `v2026.08.17-issue-817-link-page-tenant-id-release` is `CREATED_AND_VERIFIED`. Mango Maven remains `1.0.37`; every other npm coordinate remains unchanged.

### Pull Requests

- [PR #822](https://github.com/HardyDou/mango/pull/822) Fixed anonymous `@mango/link-page` public queries so an explicit `tenantId` is forwarded while authenticated queries retain the existing visible-link path. Packages: `@mango/link-page@1.0.8`, `@mango/cli@1.0.110` (matrix projection). Business Adaptation: consumers using public links may pass the tenant ID; no backend or database change.

### Fixed

- Add the typed `tenantId` prop to `@mango/link-page` and forward it only on anonymous public-link queries; authenticated queries continue to use `listVisibleLinks`.
- Document the public-link tenant contract and add a regression contract test.

### Versions

- `@mango/link-page`: `1.0.7` to `1.0.8`.
- `@mango/cli`: `1.0.109` to `1.0.110`, carrying the generated release matrix entry for link-page `1.0.8`.
- Mango Maven remains `1.0.37`; every other npm coordinate remains unchanged.

### Published Packages

1. `@mango/link-page@1.0.8`.
2. `@mango/cli@1.0.110` (required matrix projection).
3. No Maven or other npm package is published in this batch.

### Business Impact

- Public link-page consumers that serve multiple tenants can provide `tenantId` without changing backend APIs.
- Authenticated navigation behavior is unchanged. No database, permission, message, file or deployment migration is introduced.

### Upgrade Estimate

- Audience: Mango consumers importing `@mango/link-page` or the generated CLI matrix.
- Engineering Effort: 5 to 15 minutes for consumers that need tenant-scoped public links; zero code changes otherwise.
- Execution Window: 10 to 20 minutes for install, focused test and frontend build.
- Service Downtime: none.
- Rollback Effort: 5 to 10 minutes to restore the prior npm matrix and lockfile.
- Assumptions: consume-registry access, clean lockfile and no private fork of link-page.

### Upgrade Notes

1. Install `@mango/cli@1.0.110` or update the managed matrix, which resolves `@mango/link-page@1.0.8`.
2. For anonymous public links, pass `tenantId` to `LinkPage`; authenticated consumers do not need to change.
3. Run the consumer frozen install, focused link-page test and frontend build.

### Verification

- Link-page tenant contract tests, the package build, admin style gates, release-version lock, workspace layout checks, the full local release gate and all required PR checks passed before preparation.
- Candidate preparation sealed `@mango/link-page@1.0.8` as SHA-256 `d810c96c10ef6e3f85480b0ee4244d71d953defb41c90912dba4c44c07b2bdd0` and `@mango/cli@1.0.110` as SHA-256 `0707b0605892b0f8b3d130dee45190beada849be7ab3d6a53c48c60b2d688746` from source commit `da2bb1d4c6a4ec8754d823ba1e9eb25b90b85e56`.
- Both exact hashes were read back from npm-hosted and npm-group; the pure consume-registry consumer passed before the Tag and GitHub Release were created and verified.

### Rollback

- Restore the previous lockfile and `@mango/cli@1.0.109` / `@mango/link-page@1.0.7` tuple.
- Never overwrite or republish immutable coordinates; a release defect requires a new patch version.

### Audit History

- Issue #817 is fixed by PR #822. The prior successful release baseline is tag `v2026.08.17-issue-806-business-readme-audit-scope`.
- The first completion attempt created the Tag but received HTTP 503 from the GitHub Releases API. Same-manifest `repair` reverified both immutable npm coordinates without republishing, created the GitHub Release and advanced the canonical manifest to `COMPLETED`.

## v2026.08.17-issue-806-business-readme-audit-scope - 2026-08-17

Status: `PUBLISHED_AND_VERIFIED`. Canonical manifest `8c21dcba0576d6b0b01f1afa3bbdd2b7ba9032dea0e2b5f8a2ba47ae9139d699` is `COMPLETED`: both sealed npm tarballs match in npm-hosted and npm-group, the pure consume-registry consumer passed, and GitHub Release `v2026.08.17-issue-806-business-readme-audit-scope` is `CREATED_AND_VERIFIED`. Mango Maven `1.0.37` and all other npm coordinates remain unchanged.

### Pull Requests

- [PR #818](https://github.com/HardyDou/mango/pull/818) Fixed business-consumer README audit root and scope resolution. Packages: `@mango/pmo@1.3.16`, `@mango/cli@1.0.109`. Business Adaptation: upgrade the exact PMO/CLI tuple, rerun the locked PMO upgrade/check and both M08 README audits, and repair only genuine business-owned README findings.

### Fixed

- Resolve business audit roots from the actual project root and `mango.config.json.paths`, audit only capability-map-owned business documents, and fail closed on invalid roots, empty scope, path escape or symlink escape.
- Preserve the fixed Mango source README/source-facts gates and exclude `mango.config.json.paths` from runtime configuration facts.

### Versions

- `@mango/pmo` advances from `1.3.15` to `1.3.16`; exact dependent `@mango/cli` advances from `1.0.108` to `1.0.109`.
- Mango Maven remains `1.0.37`; all other npm coordinates remain unchanged.

### Published Packages

1. `@mango/pmo@1.3.16`.
2. `@mango/cli@1.0.109`.
3. No Maven or other npm package is published.

### Business Impact

- Packaged PMO checks now audit a business repository's configured backend, frontend and business documentation roots instead of requiring Mango source-only README paths.
- No API, database, menu, permission, tenant, message, file, frontend runtime or application configuration migration is introduced.

### Upgrade Estimate

- Audience: business repositories using Mango PMO README/source-facts audits.
- Engineering Effort: 15 to 30 minutes for a clean repository; up to 1 hour when genuine business README links need repair.
- Execution Window: 20 to 60 minutes for dry-run, upgrade, locked check and M08 audits.
- Service Downtime: none; this is development-governance tooling only.
- Rollback Effort: 10 to 20 minutes using the PMO backup and prior CLI/PMO tuple.
- Assumptions: company npm-group access, a clean business worktree and valid `mango.config.json.paths`.

### Upgrade Notes

1. Install `@mango/cli@1.0.109`, which locks `@mango/pmo@1.3.16`.
2. Run `mango pmo upgrade --project-dir . --to 1.3.16 --dry-run`, apply the reviewed upgrade, then run `mango pmo check --project-dir . --locked`.
3. Run both packaged M08 audits from the business project root and repair genuine business-owned README findings without adding Mango source directories.

### Verification

- PR #819 passed all required checks before merge; `origin/main` tree `f8c3e99fb4e3554aa678d9d19acc1c096696136f` matched the sealed candidate tree exactly.
- Candidate preparation sealed `@mango/pmo@1.3.16` as SHA-256 `064866af5eac9962895122f10ac977cfddf359d14fd587f3f7e6a47b77099b2c` and `@mango/cli@1.0.109` as SHA-256 `dd312175c6fe8ed1ba946e07cd773fa227c8a53eb6ce82774a38e44145a29503` after the mixed PMO upgrade/audit consumer passed.
- Both exact hashes were read back from npm-hosted and npm-group; the pure consume-registry typecheck and production build passed before the Tag and GitHub Release were created.

### Rollback

- Revert the business PMO upgrade commit, restore CLI `1.0.108` and PMO `1.3.15` using the validated PMO backup, then rerun the locked check.
- Never overwrite an immutable npm coordinate; correct any release defect with a new patch batch.

### Audit History

- PR #815 and tag `v2026.08.16-issue-805-release-consumer-fix` are the successful release baseline.
- PR #816 is prior-batch closeout evidence and is not counted as a release-bearing change here.
- PR #819 is the machine-projected Release PR; its merge tree equals the sealed candidate, and the release tag remains on source commit `876b4c45577ec8e802e2beeae360553d7407d0c4` rather than this documentation-only closeout.

## v2026.08.16-issue-805-release-consumer-fix - 2026-08-16

Status: `PUBLISHED_AND_VERIFIED`. Canonical manifest `127cdcdf8d08b95ae6dee13fed2edb6a0838d1b4c586258757d016c3bb4c30d8` is `COMPLETED`: all 24 sealed npm tarballs and 188 Maven coordinates match in their publish and consume registries, the pure consume-registry consumer passed, and the GitHub Release is `CREATED_AND_VERIFIED`.

### Pull Requests

- [PR #797](https://github.com/HardyDou/mango/pull/797) Changed immutable-write audit recovery and completed-batch closeout checks. Packages: Mango release tooling (source-only; no standalone coordinate). Business Adaptation: none; business dependency, API, configuration and deployment contracts are unchanged.
- [PR #808](https://github.com/HardyDou/mango/pull/808) Fixed the Notice inbound broadcast contract so events carry identifiers and status instead of message bodies or attachment lists. Packages: `io.mango:mango-notice-core:1.0.37`. Business Adaptation: event consumers query Notice detail by `messageId`, then retrieve attachments from File by `fileId`.
- [PR #809](https://github.com/HardyDou/mango/pull/809) Fixed the full-consumer package cycle, CMS/system-event routes, Notice settings route and affected frontend compatibility issues. Packages: `@mango/admin-extension@1.0.1`, `@mango/admin-pages@1.0.34`, `@mango/admin@1.0.67`, `@mango/common@1.0.27`, `@mango/file@1.0.35`, `@mango/link@1.0.21`, `@mango/notice@1.0.40`, `@mango/rbac@1.0.25`, `@mango/system@1.0.33`, `@mango/cli@1.0.108`. Business Adaptation: align the full CLI matrix, add Admin Extension for direct consumers, enable the Outbox where system events are exposed, and verify all authorized routes.
- [PR #810](https://github.com/HardyDou/mango/pull/810) Fixed mixed-plan Maven source retention and exact-coordinate registry preflight. Packages: `@mango/cli@1.0.108` and Mango release tooling. Business Adaptation: no runtime migration; release operators configure distinct publish/consume roles and probe the published BOM coordinate.
- [PR #811](https://github.com/HardyDou/mango/pull/811) Changed release planning to bind the candidate to an exact committed source snapshot. Packages: `@mango/cli@1.0.108` and Mango release tooling. Business Adaptation: none; business projects continue to consume the exact published tuple.
- [PR #812](https://github.com/HardyDou/mango/pull/812) Fixed machine projection of the public CLI README version. Packages: `@mango/cli@1.0.108`. Business Adaptation: install and query the exact CLI `1.0.108` coordinate shown in the published README.
- [PR #813](https://github.com/HardyDou/mango/pull/813) Fixed machine projection of the full frontend template CLI dependency. Packages: `@mango/cli@1.0.108`. Business Adaptation: newly generated full projects use the same CLI version as the release matrix.
- [PR #814](https://github.com/HardyDou/mango/pull/814) Fixed local Release-only checks so they match the trusted Runner classifier. Packages: `@mango/cli@1.0.108` and Mango release tooling. Business Adaptation: none; this changes Mango maintainer verification, not business PR or application-release behavior.

### Added

- Add `office.xlsx.web.buttons.enabled` with `KK_OFFICE_XLSX_WEB_BUTTONS_ENABLED` override to control the XLS/XLSX Web preview “跳转 HTML 预览” and “打印” entries. The compatibility default is `true`; disabling it removes the button region and lets Luckysheet fill the page from the top without changing other preview types, download behavior, or `@mango/file`.

### Release Notes

- This change is delivered by the Mango Maven `mango-file-preview-engine` artifact. Include it in the next Mango Maven release and upgrade backend consumers as one aligned Maven matrix. No standalone `@mango/file` npm release is required for this capability.

### Fixed

- Remove the Admin Pages/System/File package cycle by extracting the singleton page, feature and Notice provider contracts to FE1 `@mango/admin-extension`; keep Admin Pages compatibility re-exports for the migration window.
- Preserve `@mango/cms/admin-pages` as an external Admin dependency so all CMS registrars share the Shell registry, and enable the domain-event Outbox in newly generated full projects so the system-event menu matches its controller condition.
- Register the hidden Notice settings route, ship a real favicon, normalize numeric strings, migrate affected Element Plus props, and narrow Notice inbound broadcasts to identifiers/status while keeping message bodies in Notice and files in File.
- Preserve the published Maven source version when an in-progress plan is recalculated and validate both Maven registry roles with the same exact historical BOM coordinate.
- Bind the release plan to the exact committed source snapshot and reject source, tree or file-set drift while accepting machine-generated dependency-closure version projection.
- Project the CLI README current version, exact query and installation examples from the machine plan, then reject every other README change against the source snapshot.
- Project the unresolved full frontend template's exact CLI development dependency from the machine plan without parsing its placeholders, then reject every other template change against the source snapshot.
- Use the Runner's trusted Release-only classifier in `release:local-check`, selecting `release:pr-check` for machine projections and retaining ordinary Changeset/plan checks for mixed source changes.
- Retain high-volume Maven and npm output with an explicit release-command buffer so a healthy `mango release prepare` run is not terminated before its candidate manifest is written.
- Exclude the sealed local Maven staging repository from user-configured wildcard mirrors and resolve all 188 candidate coordinates through one generated consumer POM, preventing pre-publication lookups from being redirected to the consume registry while removing the per-coordinate Maven process loop.
- Validate Changeset intent for a mixed Release through the plan's committed source snapshot, leaving final dependency-closure version projections to the independent plan check.

### Added

- Add `@mango/admin-extension@1.0.1`, sealed candidate release contracts, and the configurable XLS/XLSX Web preview control flag.

### Changed

- Publish all generated exact-version dependents in machine-generated topological order; do not infer the batch from the latest commit or maintain a manual package list.
- New registrars import from `@mango/admin-extension`; old Admin Pages subpath imports resolve the same implementation until the documented next-major removal point.
- Persist `remoteWrites=true` before immutable writes and allow completed-batch repair to recover the audit flag only after read-only hosted/group, Tag and Release verification.
- Treat a non-browsable Maven hosted root as neutral and fail closed on the exact `io.mango:mango-bom:1.0.36` probe instead.

### Versions

| Component group                             | Previous                                                                                                                                               | Release                                                                                                                                                | Compatibility                                                 |
| ------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------- |
| Mango Maven non-app reactor and docs bundle | `1.0.36`                                                                                                                                               | `1.0.37`                                                                                                                                               | Notice event contract and pending File Preview configuration. |
| Foundation                                  | Common `1.0.26`; Admin Extension `1.0.0`; Auth `1.0.26`; File `1.0.34`; Grid Layout `1.0.17`; Grid Widgets `1.0.23`; Home `1.0.15`                     | Common `1.0.27`; Admin Extension `1.0.1`; Auth `1.0.27`; File `1.0.35`; Grid Layout `1.0.18`; Grid Widgets `1.0.24`; Home `1.0.16`                     | New FE1 contract and exact dependency closure.                |
| Platform core                               | RBAC `1.0.24`; Site Shell `1.0.13`; System `1.0.32`; Admin Pages `1.0.33`                                                                              | RBAC `1.0.25`; Site Shell `1.0.14`; System `1.0.33`; Admin Pages `1.0.34`                                                                              | Element Plus fixes and singleton compatibility exports.       |
| Domain packages                             | Calendar `1.0.34`; CMS `1.0.23`; Job `1.0.27`; Link `1.0.20`; Notice `1.0.39`; Numgen `1.0.35`; Payment `1.0.26`; Template `1.0.34`; Workflow `1.0.40` | Calendar `1.0.35`; CMS `1.0.24`; Job `1.0.28`; Link `1.0.21`; Notice `1.0.40`; Numgen `1.0.36`; Payment `1.0.27`; Template `1.0.35`; Workflow `1.0.41` | Generated exact dependency closure.                           |
| Aggregates and CLI                          | Admin Shell `1.0.60`; Workflow Example `1.0.39`; Admin `1.0.66`; CLI `1.0.107`                                                                         | Admin Shell `1.0.61`; Workflow Example `1.0.40`; Admin `1.0.67`; CLI `1.0.108`                                                                         | Full aggregate and generated-project tuple.                   |
| `@mango/pmo`                                | `1.3.15`                                                                                                                                               | unchanged                                                                                                                                              | No distributed PMO source change.                             |

### Published Packages

1. Mango Maven non-app reactor and `io.mango:mango-docs-bundle:1.0.37` — `PUBLISHED_AND_VERIFIED`; all 188 coordinates and 334 sealed repository files match through Maven releases/public.
2. `@mango/common@1.0.27` -> `@mango/admin-extension@1.0.1` -> `@mango/auth@1.0.27` -> `@mango/file@1.0.35` -> `@mango/grid-layout@1.0.18` -> `@mango/grid-widgets@1.0.24` -> `@mango/home@1.0.16` -> `@mango/rbac@1.0.25` -> `@mango/site-shell@1.0.14` -> `@mango/system@1.0.33` -> `@mango/admin-pages@1.0.34` -> `@mango/calendar@1.0.35` -> `@mango/cms@1.0.24` -> `@mango/job@1.0.28` -> `@mango/link@1.0.21` -> `@mango/notice@1.0.40` -> `@mango/numgen@1.0.36` -> `@mango/payment@1.0.27` -> `@mango/template@1.0.35` -> `@mango/workflow@1.0.41` -> `@mango/admin-shell@1.0.61` -> `@mango/workflow-business-example@1.0.40` -> `@mango/admin@1.0.67` -> `@mango/cli@1.0.108` — `PUBLISHED_AND_VERIFIED`; hosted/group content matches every sealed tarball.
3. `@mango/pmo@1.3.15` is unchanged and was not republished.
4. Tag and GitHub Release `v2026.08.16-issue-805-release-consumer-fix` — `CREATED_AND_VERIFIED` after pure consume-registry verification.

### Business Impact

- Full/aggregated consumers must upgrade the complete CLI matrix. Direct consumers add `@mango/admin-extension@1.0.1`; existing Admin Pages imports remain compatible but new code uses Admin Extension.
- Existing projects exposing system events set `mango.event.outbox.enabled=true`. No Issue #805 database migration is introduced.
- Notice event consumers migrate removed content/address/attachment fields to the authorized `messageId` detail lookup and File `fileId` retrieval.
- XLS/XLSX Web preview controls remain enabled by default; only consumers wanting the reduced toolbar add the new flag.
- PMO and business PR/application release processes are unchanged.

### Upgrade Estimate

- Audience: full/aggregated Mango Admin projects, direct frontend consumers, Notice event consumers, and Maven File Preview/Notice consumers.
- Engineering Effort: 0.5 to 1 person-day for standard aggregate consumers; 1 to 2 person-days for custom modular pins; add 0.5 to 1 person-day for custom Notice event consumers.
- Execution Window: 1 to 3 hours for alignment, configuration review, build and focused acceptance, plus the business repository's normal deployment pipeline.
- Service Downtime: no database window; normal backend restart and frontend redeployment only.
- Rollback Effort: 0.5 to 1 person-day to restore the prior tuple and lockfile; custom event consumers may require one additional person-day.
- Assumptions: company registry access, a clean lockfile, no private package forks or conflicting overrides, and repeatable build/deployment pipelines.

### Upgrade Notes

1. Keep PMO `1.3.15`; upgrade Maven/BOM to `1.0.37` and install CLI `1.0.108`.
2. Apply the entire `release-versions.json` matrix; do not upgrade Admin, Common or Notice alone. Direct consumers add Admin Extension `1.0.1`.
3. Use Admin Extension subpaths for new registrars; enable `mango.event.outbox.enabled=true` where the system-event menu is present.
4. Migrate Notice event consumers to detail-by-`messageId` and File-by-`fileId`; optionally configure XLS/XLSX Web buttons.
5. Run frozen frontend install/typecheck/tests/build, Maven `clean verify`, login, real CRUD and every actually authorized menu before business deployment.

### Verification

- The final Release PR tree `b5f8fd12555a1c858f7ba8a9a982975ac54373bf` matched the sealed candidate tree exactly; PR #815 passed all required checks before merge.
- Registry doctor successfully read `io.mango:mango-bom:1.0.36` through both Maven publish and consume roles and validated the four configured registry roles.
- Preparation sealed source archive SHA-256 `069515e72d4087466ff649ade132be996b376eb562a59246f0f38752504ff67e`, 24 npm tarballs and 188 Maven coordinates, then passed the mixed candidate consumer.
- Publication matched every sealed npm and Maven artifact through hosted/group and releases/public; no app fat JAR or PMO coordinate was published.
- The pure consume-registry consumer passed before tag and GitHub Release creation; manifest states `PUBLISHED`, `CONSUMER_VERIFIED` and `COMPLETED` all passed on 2026-08-16.

### Rollback

1. Revert the business upgrade/configuration commit, restore Maven `1.0.36`, CLI `1.0.107` and the previous exact npm matrix, then rebuild and redeploy normally.
2. No Issue #805 database migration requires reversal; handle any queued Outbox events according to the business runbook before disabling it.
3. Never delete, overwrite or republish immutable Maven/npm coordinates. Fix release defects with a new patch batch.

### Audit History

- PR #796 and its successful 2026-08-15 tag are the accumulated release baseline, not a new capability in this batch.
- PR #797 changes repository release audit behavior only and does not introduce a standalone business-consumable coordinate.

## v2026.08.15-pmo-1.3.15-job-1.0.27-admin-shell-1.0.60-admin-1.0.66-cli-1.0.107-local-first-release - 2026-08-15

Status: `PUBLISHED_AND_VERIFIED`. All five sealed npm tarballs match in npm-hosted and npm-group, the pure consume-registry consumer passed typecheck and production build, and the GitHub Release is `CREATED_AND_VERIFIED`. Canonical manifest `363855d6a488231eff129b0541e69e1ce916ea20d0c1c3bbf8e757f08100bce9` is `COMPLETED`. Mango Maven remains `1.0.36` and `@mango/common` remains `1.0.26`.

### Pull Requests

- [PR #792](https://github.com/HardyDou/mango/pull/792) Fixed the Admin release compatibility regression. Packages: `@mango/admin-shell@1.0.60`, `@mango/admin@1.0.66`. Business Adaptation: aggregated consumers upgrade Admin; direct consumers align Admin Shell while keeping Common `1.0.26`.
- [PR #795](https://github.com/HardyDou/mango/pull/795) Fixed Job management-list pagination and CLI cross-platform command handling. Packages: `@mango/job@1.0.27`, `@mango/cli@1.0.107`. Business Adaptation: Job consumers verify totals, page number, page size and navigation; CLI consumers use the project-pinned `1.0.107` path.
- [PR #796](https://github.com/HardyDou/mango/pull/796) Added the local-first Mango component release flow and shared PR submission flow. Packages: `@mango/pmo@1.3.15`, `@mango/cli@1.0.107`. Business Adaptation: business repositories upgrade PMO, use `mango-submit-pr`, remove the PMO-managed project copy of `mango-release`, and retain their own business release/deployment process.

### Fixed

- Fix [Issue #791](https://github.com/HardyDou/mango/issues/791): `@mango/admin-shell@1.0.59` imported `resolveActiveMenuPath` from the already-published `@mango/common@1.0.26`, whose tarball does not export that symbol, so a clean Admin production build failed.
- Keep active parent-menu selection inside Admin Shell, restore the Common menu-tree source to the exact `1.0.26` release baseline, and leave all existing immutable package versions unchanged.
- Add a release-candidate consumer matrix: unpublished candidate packages use local tarballs while unchanged packages and the published CLI resolve from the consume registry. After the CLI is published, the same gate verifies a pure consume-registry install, typecheck and production build.
- Replace the manual package list and seventeen-state adapter workflow with Changesets intent, Git impact reconciliation, dependency/CLI closure, a machine-generated plan and one set of SHA-256 sealed artifacts. Tag and GitHub Release now wait for pure consume-registry verification.
- Keep Maven in the same release owner: production-source impact requires an explicit Maven target, automatically advances the CLI matrix, deploys the non-app reactor once into a sealed local repository, and publishes or repairs only the recorded POM/JAR hashes.
- Add repository-only `mango-release` distribution isolation and the shared `mango-submit-pr` submission Skill. Every required Runner check must have a local equivalent; Push and PR creation wait for a clean final-head local pass, and Runner is only an independent verification surface.
- Preserve a local-only verified candidate as `superseded` audit evidence when the Release PR final HEAD changes, then rebuild the canonical candidate for the new commit/tree; candidates with remote writes remain immutable and cannot be replaced automatically.
- Include Issue #43 / PR #795: restore Job list pagination totals, page-size propagation and page controls, plus the associated CLI cross-platform command compatibility changes merged before this release candidate was sealed.

### Versions

| Component            |               Previous |   Release | Compatibility                                                                                               |
| -------------------- | ---------------------: | --------: | ----------------------------------------------------------------------------------------------------------- |
| `@mango/admin-shell` |               `1.0.59` |  `1.0.60` | Keeps active-menu resolution private to the Shell and remains compatible with Common `1.0.26`.              |
| `@mango/job`         |               `1.0.26` |  `1.0.27` | Restores complete pagination behavior on Job management lists.                                              |
| `@mango/admin`       |               `1.0.65` |  `1.0.66` | Aggregates Admin Shell `1.0.60`; public Admin API and styles remain unchanged.                              |
| `@mango/pmo`         |               `1.3.14` |  `1.3.15` | Delivers the canonical local-first release Skill and release artifact governance.                           |
| `@mango/cli`         |              `1.0.106` | `1.0.107` | Locks the corrected Admin tuple and PMO `1.3.15`, and exposes `release plan/prepare/publish/status/repair`. |
| `@mango/common`      |               `1.0.26` | unchanged | Not republished; source is restored to its released baseline.                                               |
| Mango Maven          |               `1.0.36` | unchanged | This is an npm-only batch; no Maven coordinate is published.                                                |
| Other npm packages   | current release matrix | unchanged | Not republished.                                                                                            |

### Published Packages

| Order | Target                           | Version                                                                                             | Status                                                                      |
| ----: | -------------------------------- | --------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------- |
|     1 | `@mango/job`                     | `1.0.27`                                                                                            | `PUBLISHED_AND_VERIFIED`; hosted/group SHA-256 matches the sealed tarball.  |
|     2 | `@mango/admin-shell`             | `1.0.60`                                                                                            | `PUBLISHED_AND_VERIFIED`; hosted/group SHA-256 matches the sealed tarball.  |
|     3 | `@mango/admin`                   | `1.0.66`                                                                                            | `PUBLISHED_AND_VERIFIED`; hosted/group SHA-256 matches the sealed tarball.  |
|     4 | `@mango/pmo`                     | `1.3.15`                                                                                            | `PUBLISHED_AND_VERIFIED`; hosted/group SHA-256 matches the sealed tarball.  |
|     5 | `@mango/cli`                     | `1.0.107`                                                                                           | `PUBLISHED_AND_VERIFIED`; hosted/group SHA-256 matches the sealed tarball.  |
|     6 | Immutable tag and GitHub Release | `v2026.08.15-pmo-1.3.15-job-1.0.27-admin-shell-1.0.60-admin-1.0.66-cli-1.0.107-local-first-release` | `CREATED_AND_VERIFIED` after the pure consume-registry consumer passed.     |
|     7 | Latest documentation             | Pages                                                                                               | `NOT_APPLICABLE`; npm-only release creates no Maven documentation snapshot. |

### Business Impact

- Aggregated Admin consumers upgrade `@mango/admin` to `1.0.66`; direct or modular consumers align the complete CLI matrix, especially Job `1.0.27`, Admin Shell `1.0.60` and Admin `1.0.66`.
- Job users regain correct totals, page number, page size and navigation on the definition, instance, Worker and alarm-rule lists. API URLs, permissions, tenants, menus, database schema and runtime configuration do not change.
- PMO upgrades install `mango-submit-pr` and remove any PMO-managed business-project copy of `mango-release`. Business PR, build, deployment and application-release processes remain separate from the Mango component release state machine.
- `@mango/common@1.0.26`, Mango Maven `1.0.36` and all other npm coordinates remain unchanged and are not republished.

### Upgrade Estimate

- Audience: owners of business repositories consuming Job, Admin/Admin Shell, or the PMO/CLI toolchain; repositories consuming none of these coordinates require no change.
- Engineering Effort: approximately 0.5 person-day for an aggregated Admin consumer, 0.5 to 1 person-day for a direct or modular consumer, and 0.25 to 0.5 person-day for a PMO-only repository.
- Execution Window: approximately 30 to 90 minutes for dependency and PMO changes plus 30 to 60 minutes for repository-specific typecheck, production build and focused acceptance; normal deployment-pipeline time is additional.
- Service Downtime: no Mango-mandated downtime or database maintenance window; only the business system's normal redeployment window applies.
- Rollback Effort: approximately 15 to 30 minutes to revert the business upgrade commit and lockfile, plus the repository's normal build and deployment-pipeline time.
- Assumptions: the business repository has a clean dependency lock, no private package patch, consume-registry access and a repeatable build/deployment pipeline; custom forks or unrelated dependency conflicts require a separate estimate.

### Upgrade Notes

1. Confirm `@mango/pmo@1.3.15` and `@mango/cli@1.0.107` resolve from the company npm group registry, then install the exact CLI `1.0.107`.
2. Run `mango pmo status --project-dir .`, `mango pmo upgrade --project-dir . --to 1.3.15 --dry-run`, `mango pmo upgrade --project-dir . --to 1.3.15 --sync-shell`, and `mango pmo check --project-dir . --locked`.
3. Aggregated Admin consumers upgrade `@mango/admin` to `1.0.66`; direct or modular consumers align their declared packages with the complete `release-versions.json` matrix.
4. Keep `@mango/common@1.0.26` and Mango Maven `1.0.36`; do not republish either unchanged coordinate. No API, database, menu, permission, tenant, or runtime configuration migration is required.

### Verification

- The old pure-registry tuple must reproduce the missing `resolveActiveMenuPath` export during the Admin Vite production build.
- Admin Shell unit tests must cover parent-route ownership and sibling-prefix rejection; Common source must match its `1.0.26` release baseline.
- Job API and view tests must cover backend totals, page/size forwarding, page-size changes and all affected management list pagination controls.
- Changeset/Git impact, machine plan, CLI release lock, Admin styles, package exports, sealed release-candidate matrix, post-publish pure-registry matrix, PMO package/document gates, Runner classification and required checks must pass before publication is complete.
- The single `pnpm -C mango-ui release:local-check -- --base=origin/main --head=HEAD` entry must reproduce the applicable PMO, CLI, frontend, release-plan, projection, workspace and capability-document Runner checks locally before the Release PR is pushed.

### Rollback

1. Revert the business repository dependency/PMO upgrade commit and lockfile, then rebuild and redeploy through that repository's normal pipeline.
2. For PMO state, run `mango pmo rollback --project-dir . --dry-run`, review the managed changes, then run the rollback and the locked PMO check.
3. Do not change the database or Maven version; this npm-only batch has no corresponding migration.
4. Never delete, overwrite or republish an immutable npm coordinate. An artifact defect requires a new patch version and release batch.

### Audit History

- PR #790 belongs to the earlier CLI `1.0.106` / PMO `1.3.14` release recovery and is not a new capability in this batch.
- PR #793 was the superseded Admin release configuration; PR #796 and the one-time reconciliation record own the final batch scope.

## v2026.08.14-pmo-1.3.14-cli-1.0.106-historical-document-compat-release - 2026-08-14

Status: `PENDING`. This npm-only governance patch publishes `@mango/pmo@1.3.14` and the exact dependent `@mango/cli@1.0.106`; Mango Maven `1.0.36` and all other npm coordinates remain unchanged.

### Fixed

- Fix [Issue #786](https://github.com/HardyDou/mango/issues/786): path/SHA-256/`pmoVersion`-pinned PMO 1.3.10 through 1.3.12 TDD/Plan documents retain the historical “参考资料与代码基线” section required by their original contract instead of being rejected by the current H2 whitelist.
- Keep historical compatibility exact and fail closed. Current documents, changed or unpinned historical documents, unknown sections, duplicate titles, invalid insertion points and malformed historical tables remain blocked.
- Preserve all existing metadata, structure, table, ID, approval, traceability, blocking, dependency, forbidden-content and upstream-summary checks.

### Versions

| Component          |               Previous |   Release | Compatibility                                                                                                                  |
| ------------------ | ---------------------: | --------: | ------------------------------------------------------------------------------------------------------------------------------ |
| `@mango/pmo`       |               `1.3.13` |  `1.3.14` | Adds exact historical TDD/Plan section variants and advances current lifecycle contracts without changing schema revision `1`. |
| `@mango/cli`       |              `1.0.105` | `1.0.106` | Exact dependency and business-project release lock for PMO `1.3.14`.                                                           |
| Mango Maven        |               `1.0.36` | unchanged | No Maven artifact is published by this npm-only patch.                                                                         |
| Other npm packages | current release matrix | unchanged | Notice, Admin, File and all runtime frontend packages are not republished.                                                     |

### Published Packages

| Order | Target                           | Version                                                                 | Status                                                                      |
| ----: | -------------------------------- | ----------------------------------------------------------------------- | --------------------------------------------------------------------------- |
|     1 | `@mango/pmo`                     | `1.3.14`                                                                | `PENDING` publication and hosted/group verification.                        |
|     2 | `@mango/cli`                     | `1.0.106`                                                               | `PENDING` after PMO hosted/group verification.                              |
|     3 | Immutable tag and GitHub Release | `v2026.08.14-pmo-1.3.14-cli-1.0.106-historical-document-compat-release` | `PENDING`.                                                                  |
|     4 | Latest documentation             | Pages                                                                   | `PENDING`; npm-only release does not create a Maven documentation snapshot. |

### Upgrade Notes

1. Wait until both `@mango/pmo@1.3.14` and `@mango/cli@1.0.106` resolve from the company npm group registry, then install CLI `1.0.106`.
2. Run `mango pmo upgrade --project-dir . --to 1.3.14 --dry-run` and review the managed PMO changes.
3. Run the upgrade without `--dry-run`, then execute `mango pmo check --project-dir . --locked`.
4. Do not delete historical sections, rewrite approved lifecycle documents, hand-edit `baseline.json`, or change `.mango-pmo-legacy-documents.json` hashes to force acceptance.

### Verification

- The document-contract suite must cover pinned 1.3.10/1.3.11/1.3.12 positive cases, a pinned 1.3.13 no-variant case, and current-version, unpinned, unknown-section and malformed-table negative cases.
- The real Baohan set of 82 lifecycle documents must pass without modifying its seven path/SHA/version-pinned historical documents.
- PMO and CLI package checks, Business Starter projection, release impact, README/capability audits, workspace layout, branch protection, required checks, hosted/group back-checks and a clean published Baohan consumer upgrade must pass before completion.

## v2026.08.14-cli-1.0.105-pmo-manifest-compat-release - 2026-08-14

Status: `PUBLISHED_AND_VERIFIED`. This npm-only patch published `@mango/cli@1.0.105` from source commit `d62f553699df77c900f230ae1b345564e142cdb5` (tree `31e35d530a8194b4a19b42a4931f0090b41689c0`); Mango Maven `1.0.36`, `@mango/pmo@1.3.13`, and all other npm coordinates remained unchanged. The completed canonical manifest is `.mango/releases/1.0.105/manifest.json`, SHA-256 `2d548eb0426cc4bb05765b2f0d1005f7290a7b05e886cb7642630d03f7cd357a`, with `completed=true`.

### Fixed

- Fix [Issue #784](https://github.com/HardyDou/mango/issues/784) by restoring compatibility with the PMO manifest schema v2 historical `code-template` file kind used by `@mango/pmo@1.3.11`.
- Preserve full path, hash, size, mode, duplicate-path and bundle-hash validation while allowing the atomic upgrade transaction to remove files that are absent from the target `@mango/pmo@1.3.13` manifest.
- Include the already merged CLI local-development cleanup fix so backend start and restart remove stale Reactor output before `compile + spring-boot:run`.

### Versions

| Component          |               Previous |   Release | Compatibility                                                              |
| ------------------ | ---------------------: | --------: | -------------------------------------------------------------------------- |
| `@mango/cli`       |              `1.0.104` | `1.0.105` | Upgrades PMO 1.3.11 business baselines to the unchanged PMO 1.3.13 bundle. |
| `@mango/pmo`       |               `1.3.13` | unchanged | No PMO rule, contract, Agent, Skill, or package content changes.           |
| Mango Maven        |               `1.0.36` | unchanged | No Maven artifact is published by this npm-only patch.                     |
| Other npm packages | current release matrix | unchanged | No Notice, Admin, File, or other npm package is republished.               |

### Published Packages

| Order | Target                           | Version                                               | Status                                                                                                                                                                                                 |
| ----: | -------------------------------- | ----------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
|     1 | `@mango/cli`                     | `1.0.105`                                             | `PUBLISHED_AND_VERIFIED`; hosted/group shasum `5f7b824dd583652402edbdaedade7935d69852e0`, integrity `sha512-Mfof0ucyXd120nES/2VszLq5b73YphoOIfqa5EGpVwn2d/cxllWB2MflfcbU72Y4a8i72qOfpYe4iEuz2VaPmw==`. |
|     2 | Immutable tag and GitHub Release | `v2026.08.14-cli-1.0.105-pmo-manifest-compat-release` | `CREATED_AND_VERIFIED`; annotated tag resolves to source commit `d62f553699df77c900f230ae1b345564e142cdb5`.                                                                                            |
|     3 | Latest documentation             | Pages                                                 | `PUBLISHED_AND_VERIFIED`; npm-only release did not create a Maven documentation snapshot.                                                                                                              |

### Upgrade Notes

1. Install `@mango/cli@1.0.105` from the company npm group registry; keep Mango Maven at `1.0.36` and `@mango/pmo` at `1.3.13`.
2. Run `mango pmo upgrade --project-dir . --to 1.3.13 --dry-run` and review the managed deletion of obsolete PMO files.
3. Run the same command without `--dry-run`, then execute `mango pmo check --project-dir . --locked`.
4. Do not manually create `business-pmo/mango-baseline/code-templates/README.md`, edit `baseline.json`, or bypass the transaction.

### Verification

- Regression test reproduces the exact `invalid @mango/pmo manifest file descriptor: code-templates/README.md` failure against a valid historical schema v2 descriptor, then proves the fixed transaction removes the obsolete file.
- Required CLI tests, packed and clean registry consumer verification, release lock, PMO/Business Starter projection, workspace layout, README/capability audits, admin style gates, branch protection read-back, required checks and the real Baohan PMO upgrade all passed.

## v2026.08.14-maven-1.0.36-notice-1.0.39-admin-1.0.65-cli-1.0.104-inbound-release - 2026-08-14

Status: `PUBLISHED_AND_VERIFIED`. Release source is the merged Notice inbound implementation from PR #772 at source commit `8843c0049de68747ba443ccc9a31e7010a43b4a8` (tree `27b42763b392a199dc7d43e994d27b7ea406b735`). The immutable tag, GitHub Release, Maven/npm artifacts, Latest documentation, versioned documentation snapshot, registry back-checks, and clean consumer verification all passed. The canonical publication manifest remains preserved as failed audit evidence; the completed read-only recovery manifest is recorded below.

### Fixed

- Add Notice inbound message storage and query support for email polling/interface push and anonymous WeCom webhook verification/message delivery.
- Persist message bodies separately from large payloads, send attachments to File service storage, and expose attachment preview metadata and processing state in the admin UI.
- Keep existing email, in-app message, SMS, and WeCom outbound delivery paths unchanged while adding the explicit channel capability mode (`仅发送`、`仅接收`、`收发一体`).

### Versions

| Component                                            |  Previous |   Release | Compatibility                                                            |
| ---------------------------------------------------- | --------: | --------: | ------------------------------------------------------------------------ |
| Mango Maven backend and `io.mango:mango-docs-bundle` |  `1.0.35` |  `1.0.36` | Inbound Notice API, persistence, File integration, and channel adapters. |
| `@mango/notice`                                      |  `1.0.38` |  `1.0.39` | Inbound message management UI and public Notice API exports.             |
| `@mango/admin-shell`                                 |  `1.0.58` |  `1.0.59` | Consumes the Notice inbound page registration.                           |
| `@mango/admin`                                       |  `1.0.64` |  `1.0.65` | Aggregates the matching Admin Shell and Notice versions.                 |
| `@mango/cli`                                         | `1.0.103` | `1.0.104` | Locks Maven `1.0.36` and the exact frontend matrix.                      |
| `@mango/pmo`                                         |  `1.3.13` | unchanged | No PMO rule, contract, Agent, or Skill source changed.                   |

### Published Packages

| Order | Target                                                        | Version                                                                           | Status                                                                                                                                                                                                 |
| ----: | ------------------------------------------------------------- | --------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
|     1 | Complete non-app Maven batch and `io.mango:mango-docs-bundle` | `1.0.36`                                                                          | `PUBLISHED_AND_VERIFIED`; Maven consume registry back-check passed.                                                                                                                                    |
|     2 | `@mango/notice`                                               | `1.0.39`                                                                          | `PUBLISHED_AND_VERIFIED`; hosted/group shasum `90fdcfedc6788236b8079a014720b0b5212777f0`, integrity `sha512-INOsJcIw8nuLHvn4uAOsQjJNTAdM9OxgAVpgDPLz6TyNDfUXnxZR6dwtwOIwxNw0d84YqMJibzDdSL+3JzdWbg==`. |
|     3 | `@mango/admin-shell`                                          | `1.0.59`                                                                          | `PUBLISHED_AND_VERIFIED`; hosted/group shasum `08b3b836a45cef47e79b71ef1e86da3dc052d3f1`, integrity `sha512-Snb6gliAX+3besVrhlReZMwCy9klukgy3lr1b2iD1C5cmvsAVBvHyCOJ2f45GkWaF51ysaV/qp7mU972oDZ3pw==`. |
|     4 | `@mango/admin`                                                | `1.0.65`                                                                          | `PUBLISHED_AND_VERIFIED`; hosted/group shasum `f38244a2984f7328baf3007a485bcf04de3dacfa`, integrity `sha512-D2GGe8NjDymA0ov2YJ0Ssl6yLVzbvh8GPnuuG1K9zn6IO+eokwHPamwKoRJG6f/iiLwgzu6n4ErCk9x4BnQMYw==`. |
|     5 | `@mango/cli`                                                  | `1.0.104`                                                                         | `PUBLISHED_AND_VERIFIED`; hosted/group shasum `933e202a7837b274d0c2df09d2a423282d02488d`, integrity `sha512-G01CtciMF0SK0xgqg8yBJL9aif/cJm6XWbWc2A8Tau+vNpXQZr2RAFoumVhxnjk93kGxACXDrmu98YDikBTMfg==`. |
|     6 | Immutable tag and GitHub Release                              | `v2026.08.14-maven-1.0.36-notice-1.0.39-admin-1.0.65-cli-1.0.104-inbound-release` | `CREATED_AND_VERIFIED`; tag resolves to source commit `8843c0049de68747ba443ccc9a31e7010a43b4a8`.                                                                                                      |
|     7 | Latest and versioned documentation                            | Pages                                                                             | `PUBLISHED_AND_VERIFIED`; Pages workflow `31754309218`, both URLs returned HTTP 200.                                                                                                                   |

### Upgrade Notes

1. Upgrade Maven consumers to `1.0.36` before installing `@mango/cli@1.0.104`.
2. Upgrade the frontend package closure together; do not install only Notice or only Admin Shell.
3. Configure email inbound polling/interface push and WeCom token/aesKey webhook settings. Keep the callback URI anonymously accessible so WeCom signature verification can run before authentication.
4. Configure File service storage for inbound attachments. Message body/HTML remains in the message record; large attachment content does not enter the message broadcast payload.
5. Keep `@mango/pmo@1.3.13` unchanged and run the locked PMO check after upgrading.

### Verification

- Required checks on PR #772 passed: Java gates, documentation gates, `pmo-doc-check`, `frontend-pr-quality`, preflight/scope, and `pr-contract-check`.
- Release gates: release-impact, CLI release-version lock, Business Starter PMO baseline projection, generated backend gate, admin styles, Notice/Admin Shell tests, Maven/npm registry doctor, and clean-consumer verification.
- Completed read-only recovery manifest: `.runtime/release-1.0.36-readonly/manifests/1.0.36/manifest.json`, SHA-256 `12812201c42d29a742998f2aaf40e6f4d0373f413b412c4fbfb43df38906f998`, `completed=true`. It verifies source, versions, release notes, README audit, tests, PR #780, tag, GitHub Release, Maven, all four npm packages from hosted/group, CLI lock, private-registry consume, Latest docs, versioned docs, post-verify, and cleanup without republishing an immutable coordinate.
- Preserved original failed publication manifest: `.mango/releases/1.0.36/manifest.json`, SHA-256 `43abe9197636a347e8d01d8b1a45abb071f07f765b31fdc254c3c3319c48d837`; it records only the superseded docs-snapshot verifier failure and remains unchanged for audit.

## v2026.08.09-pmo-1.3.13-cli-1.0.103-pmo-rollback-release - 2026-08-09

Status: `PUBLISHED_AND_VERIFIED`. 本批次从受保护 `main` 的 source commit `80bd147ab4525b581aa5711799d096e8f73ee06d`（tree `8827aa55e82f0d149cf123cefe3bb31f40a1ffeb`）发布，以新的不可变版本撤回 PR #737、#742、#745 的 PMO 部分及 #749-#752 引入的 PMO 升级。精确源码 archive 为 1,755,248,640 字节，SHA-256 为 `2ad9915f1a0914dd053746983b8aa30da960010880418ea8f93d17d75aab1a82`。`@mango/pmo@1.3.13`、`@mango/cli@1.0.103`、Tag、[GitHub Release](https://github.com/HardyDou/mango/releases/tag/v2026.08.09-pmo-1.3.13-cli-1.0.103-pmo-rollback-release) 与 Latest 文档均已发布并验证；Mango Maven `1.0.35` 未修改、未重新发布。旧 PMO/CLI 制品、Tag 和 GitHub Release 保留为不可变审计历史，不删除、不移动、不覆盖。

最终 canonical release manifest 位于 `.mango/releases/pmo-1.3.13-cli-1.0.103-pmo-rollback/manifest.json`，SHA-256 为 `857435a0bfaee49e37704e1e121028c17b2cd2a5f22180800f5030bb09974d73`，`completed=true`。同一 manifest 保留了两次只读验证器失败及恢复证据：第一次错误解析 `npm view` 的扁平字段，第二次错误假设 pnpm 会把 PMO 提升到顶层 `node_modules`；两次均发生在不可变发布成功之后，修正语义验证器后只通过 `mango release repair` 继续验证，没有重发 Tag、Release 或 npm 坐标。

### Changed

- 恢复 PR #737 合并前的 PMO 开发、文档与交付语义，移除 canonical code baseline、历史审批快照放宽、Issue 强制证据闭环、L0-L5 精简文档、中文批量选择器和 worktree 四阶段完整性门禁。
- CLI 恢复从自身 `templates/business-module` 生成业务模块，不再从已安装 PMO 包解析 code baseline。
- 业务模块元数据模板改用 `.template` 源文件并继续生成真实 `module.properties`，避免模板占位符被仓库 Java 架构门禁当作模块身份扫描，生成结果与运行时行为不变。
- PMO 合同 schema revision 保持 `1`，当前版本前移到 1.3.13；Mango Maven `1.0.35` 与全部无关业务 npm 坐标保持不变。

### Versions

| 对象         | 旧版本    | 新版本    | 说明                                                |
| ------------ | --------- | --------- | --------------------------------------------------- |
| `@mango/pmo` | `1.3.12`  | `1.3.13`  | 以新版本发布回退后的规则、合同、Skill 和 baseline。 |
| `@mango/cli` | `1.0.102` | `1.0.103` | 精确依赖 PMO 1.3.13，并恢复 CLI 自带业务模块模板。  |
| Mango Maven  | `1.0.35`  | unchanged | 本批次不修改、不重新发布 Maven 制品。               |

### Published Packages

| 顺序 | 对象                       | 版本                                                      | 状态                                                                                                                                                                                                  |
| ---: | -------------------------- | --------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
|    1 | `@mango/pmo`               | `1.3.13`                                                  | `PUBLISHED_AND_VERIFIED`; hosted/group integrity `sha512-L/ULGDRq0WVxLaJ5q9S6rdI1UX65DVbBx6QeZpjZyyiAILBwLGwk9cZRdsL13xcL4+1XOLTnIkFCjLKvaiC5fQ==`; shasum `a5d990227fe880c66f00b9e9a1749792687b00b8` |
|    2 | `@mango/cli`               | `1.0.103`                                                 | `PUBLISHED_AND_VERIFIED`; hosted/group integrity `sha512-6XYmMEZz+O1LaBe1lC/fMPgfCe4ZIVpCCmn2CyIppgc9Qg2fpOMeYOawULhnFepp2km6+ut9YYwoDTQs8YygIA==`; shasum `45d0c6a069bcf02ed8b8f02e5d47f5b637eb7274` |
|    3 | Git tag and GitHub Release | `v2026.08.09-pmo-1.3.13-cli-1.0.103-pmo-rollback-release` | `CREATED_AND_VERIFIED`; annotated Tag 与 Release 均绑定 source commit `80bd147ab4525b581aa5711799d096e8f73ee06d`                                                                                      |
|    4 | Latest documentation       | release source above                                      | `PUBLISHED_AND_VERIFIED`; [GitHub Pages run 31321858786](https://github.com/HardyDou/mango/actions/runs/31321858786)；npm-only 批次未创建 Maven 文档快照                                              |

### Upgrade Notes

1. 等待 `@mango/pmo@1.3.13` 和 `@mango/cli@1.0.103` 均从消费仓返回精确版本后再升级。
2. 安装 CLI 1.0.103，运行 `mango pmo upgrade --project-dir . --to 1.3.13 --dry-run`，确认将撤回的 PMO 文件后执行实际升级和 `mango pmo check --project-dir . --locked`。
3. 不降级业务代码、前端业务包或 Mango Maven；旧 PMO/CLI 版本只作为升级来源和审计记录保留。

### Verification

- PR #756 的 `frontend-pr-quality`、`pmo-doc-check`、`pr-contract-check` required checks 全绿；发布状态机再次执行 PMO package check、Business Starter 142 文件精确投影、CLI 全量测试、README 审计和 release-notes 门禁。
- hosted 与 group 对两个坐标返回完全一致的 version、integrity、shasum 和发布时间；CLI release lock 同时匹配 29 个 npm 坐标，Maven 保持 `1.0.35`。
- 干净消费者只从 npm group 安装 CLI 1.0.103 与其精确依赖的 PMO 1.3.13，完成 full 单体项目初始化、142 文件 locked check、业务模块生成、前端 typecheck、Vite production build 和后端八模块 Maven validate。
- annotated Tag 和 GitHub Release 指向 source commit `80bd147ab4525b581aa5711799d096e8f73ee06d`；Latest 文档页面展示 PMO 1.3.13 / CLI 1.0.103，并由同一 source commit 的 GitHub Pages 成功运行证明。

## v2026.08.08-pmo-1.3.12-cli-1.0.102-worktree-integrity-release - 2026-08-08

Status: `PUBLISHED_AND_VERIFIED`. 发布 PR #753 通过受保护分支 required checks，并以 source commit `695e87ea1e0339db25257d207fa4d5fccaa02e0b`（tree `9bb99388cc297618740a937d9e56ed337ddd8f98`）合并。精确源 bundle 为 447,764,912 字节，SHA-256 为 `a27db0ddd672d54c8213e573e4f26017f5f6946f6222eda4a84c324abf5cb9f2`。`@mango/pmo@1.3.12`、`@mango/cli@1.0.102`、Tag、[GitHub Release](https://github.com/HardyDou/mango/releases/tag/v2026.08.08-pmo-1.3.12-cli-1.0.102-worktree-integrity-release) 与 Latest 文档均已发布并验证；Mango Maven `1.0.35` 只验证、未重新发布。

首次发布 manifest SHA-256 为 `bff7d575e42eeb8dcb87eeef0cacd4041c0a03ddc6b1b19f8c6f2e53d181cf2f`，其中不可变状态均已成功，因 Latest 校验器错误要求页面展示内部脚本文件名而停在 `docs-latest`。第一次只读恢复 manifest SHA-256 为 `1bb4843bcd256f12c34f7feff714e407fe93e18bdd43e62ae14cb250d5eb267f`，它进一步暴露消费者脚本错误复用父仓库 Git 上下文。两份失败证据均保留，未重发任何不可变对象。修正语义校验和消费者隔离后，完整只读恢复 manifest SHA-256 为 `1c236c30d9cb81e5116602968f9139b100059a52d16c42f0c7f0a5d6a953b8c3`。

### Fixed

- 新增 worktree 交付完整性门禁，在任务开始、提交、Push/PR 和清理阶段阻断跨任务复用、漏暂存、未跟踪文件、未 Push 提交和未合并清理。
- 修复 full 业务项目在正常执行 Maven 与前端生产构建后遗留 `.flattened-pom.xml` 和 `frontend/build-reports/`，导致干净 worktree 被交付门禁阻断的问题。
- 为 CLI 生成项目验收增加验证产物忽略规则回归，避免模板后续丢失这两个模式。

### Changed

- 发布同步后的 PMO 规则、中文工程 Skill、业务 baseline、交付保障流程和检查工具；CLI 精确依赖 PMO `1.3.12`。
- 保持全部 27 个非目标 npm 坐标和 Mango Maven `1.0.35` 不变；本批次不包含 Java 制品、数据库 migration 或 Maven 文档快照。

### Versions

| Component           |                               Previous |   Release | Compatibility                                                               |
| ------------------- | -------------------------------------: | --------: | --------------------------------------------------------------------------- |
| `@mango/pmo`        |                               `1.3.11` |  `1.3.12` | 文档合同 schema revision 保持 `1`；新增 worktree 任务身份和提交完整性治理。 |
| `@mango/cli`        |                              `1.0.101` | `1.0.102` | 精确依赖 PMO `1.3.12`；full 模板新增两项构建产物忽略规则。                  |
| Mango Maven backend |                               `1.0.35` | unchanged | 已发布前置版本，仅验证，不重新发布。                                        |
| Other npm packages  | `release-versions.json` current matrix | unchanged | 不重新发布，继续使用 CLI 锁定的既有坐标。                                   |

### Published Packages

| Order | Target                     | Version                                                         | Status                                                                                                                                                                                                |
| ----: | -------------------------- | --------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
|     1 | `@mango/pmo`               | `1.3.12`                                                        | `PUBLISHED_AND_VERIFIED`; hosted/group integrity `sha512-j5FYZ1EJDw9+1Mnkl7X8j8uOS4scPZ134Yc8a+pxwZYwIzvHkzZsvHQsavmp8cbySDCdKYfjy2wgBrKjRtVY/w==`; shasum `53d6df3f29a1dea069427435048feba8822b95a1` |
|     2 | `@mango/cli`               | `1.0.102`                                                       | `PUBLISHED_AND_VERIFIED`; hosted/group integrity `sha512-GBb7lVHnd0XmTXrWi174xELH0FMutFnCiOFJb9YSvIH/QJpvnDSNf25iEWFhYfvTyHU6r84Hp8g1WM6o0iWKHA==`; shasum `7b6405e52ba900a5b0fcb7b247e10913a924c6b6` |
|     3 | Git tag and GitHub Release | `v2026.08.08-pmo-1.3.12-cli-1.0.102-worktree-integrity-release` | `CREATED_AND_VERIFIED`; 均指向 source commit `695e87ea1e0339db25257d207fa4d5fccaa02e0b`                                                                                                               |
|     4 | Latest documentation       | release source above                                            | `PUBLISHED_AND_VERIFIED`; [GitHub Pages run 31253215592](https://github.com/HardyDou/mango/actions/runs/31253215592)；npm-only 批次未创建 Maven 文档快照                                              |

### PMO Required Checks

- `check-worktree-delivery-integrity.mjs`
  - Migration: 已有业务项目升级到 PMO `1.3.12`，逐个处理 active worktree，并把 `.flattened-pom.xml` 与 `frontend/build-reports/` 加入项目根 `.gitignore`。
  - Exception: 只有用户明确确认的精确并行脏 worktree 可在 `start` 阶段保留；提交、交付和清理阶段不允许以例外隐藏当前任务遗漏。
  - Verify: 运行工具单测、CLI 69 项测试，并从本地 tarball 生成空白业务项目，验证未暂存阻断、完整暂存提交、前后端构建和构建后干净交付。

### Upgrade Notes

1. 等待 `@mango/pmo@1.3.12` 和 `@mango/cli@1.0.102` 均从 npm-group 返回精确版本后再升级。
2. 安装 CLI `1.0.102`，先执行 `mango pmo upgrade --project-dir . --to 1.3.12 --dry-run`，审阅后执行实际升级和 `mango pmo check --project-dir . --locked`。
3. 已有业务项目在根 `.gitignore` 增加 `.flattened-pom.xml` 与 `frontend/build-reports/`；新生成的 full 项目已自动包含。
4. 升级后按 `start -> commit -> deliver -> cleanup` 使用交付完整性门禁。`commit` 前必须让当前任务的修改和新文件全部完成提交、忽略或清理取舍。
5. Mango Maven 保持 `1.0.35`，其它前端包保持 CLI `release-versions.json` 中的现有版本。

### Verification

- `@mango/cli` 69 项测试、PMO package build/check、business starter 投影、admin 样式、workspace 布局和 9 次 Maven 生成后端正反向门禁均通过。
- 两轮真实业务项目验证均通过；第二轮从全新 tarball 消费目录生成 full 项目和四层业务模块，前端格式/静态检查/类型/4 项单测/生产构建与后端 8 模块 Maven verify 均成功。
- 实际构建生成 `.flattened-pom.xml` 和三份 `frontend/build-reports` 后，Git 正确忽略这些文件，`deliver` 门禁仍为 PASS。
- hosted 与 group 对两个 npm 坐标返回一致的 version、integrity、shasum 和发布时间；PMO 发布时间为 `2026-08-08T10:56:46.530Z`，CLI 为 `2026-08-08T10:56:56.505Z`。
- 仓库外全新消费者从 npm-group 安装 CLI `1.0.102` 与 PMO `1.3.12`，显式初始化 Git 后创建真实 task worktree；模块生成产生 43 个任务文件，未暂存门禁先阻断，完整暂存后通过并提交。
- 消费者前端格式、ESLint、Stylelint、类型检查、4 项单测和生产构建通过；后端 8 模块 Maven verify 通过；实际生成的 8 个 `.flattened-pom.xml` 与 `frontend/build-reports/frontend-build-report.json` 均被忽略，最终 `deliver` 与 `git status --porcelain` 为空。
- Tag、GitHub Release、Latest 文档和完整只读恢复 manifest 均已验证；发布 Tag 保持指向制品 source commit，不移动到本次 CHANGELOG 收尾提交。

## v2026.08.06-pmo-1.3.11-cli-1.0.101-profile-sections-release - 2026-08-06

Status: `PUBLISHED_AND_VERIFIED`. PR #745 passed the protected required checks and merged as source commit `4592e70ac31cb6e8c57709da027c963e50b9ab80` (tree `b75611a455f354093898b2961ada8ded25197f74`). The exact-source archive is 1,755,463,680 bytes with SHA-256 `2f708650d6d22b426efea1dfe904e2fefbdb9ff6261a6cd2622b4683615f11e8`. All 24 npm coordinates, the immutable tag, [GitHub Release](https://github.com/HardyDou/mango/releases/tag/v2026.08.06-pmo-1.3.11-cli-1.0.101-profile-sections-release) and Latest documentation are published and verified; Mango Maven `1.0.35` was verified and not republished.

The canonical publication manifest SHA-256 is `37de973286f36a3199425346e97a67eb599612db2c7061d8bf6be5726870d8a7`. Its failed post-verify attempt is preserved because the original harness incorrectly required every published package at a non-full admin consumer root after immutable publication had succeeded. No immutable state was retried. The completed read-only recovery manifest SHA-256 is `53e4b2ab50bcf3a4194a50636b04d659bb2de9676b04885a162c3b48755c224c`.

### Fixed

- Publish the actual Auth, Notice, System and Admin Shell runtime code required to aggregate “我的消息、系统公告、通知设置、登录日志” under `/profile`; the previous Nexus coordinates contained documentation but omitted part of this runtime chain.
- Treat path/SHA-256/version-pinned historical lifecycle documents as immutable approval snapshots, so a newer PMO chapter contract no longer produces retroactive failures. Duplicate document IDs, adjacent lifecycle stages and upstream SHA-256 digests remain enforced.
- Verify fixed entry files and Admin Shell's generated `featureRegistrars-*.js` chunk inside packed and published tarballs, preventing source-only fixes from passing release verification.

### Changed

- Publish the complete exact-dependency frontend closure selected by release impact, then publish PMO `1.3.11` and CLI `1.0.101`. Mango Maven remains `1.0.35`; this npm-only batch contains no Java artifact, database migration or versioned Maven documentation snapshot.
- Keep new module generation on the canonical code baseline and update its frontend dependency pins to the same release matrix.

### Versions

| Component                  |                                                              Previous |                                                               Release | Compatibility                                                                                                              |
| -------------------------- | --------------------------------------------------------------------: | --------------------------------------------------------------------: | -------------------------------------------------------------------------------------------------------------------------- |
| `@mango/pmo`               |                                                              `1.3.10` |                                                              `1.3.11` | Schema revision remains `1`; unchanged `1.3.10` lifecycle documents require the generated path/SHA/version baseline.       |
| `@mango/cli`               |                                                             `1.0.100` |                                                             `1.0.101` | Locks PMO `1.3.11`, Maven `1.0.35` and the complete frontend matrix below.                                                 |
| Profile runtime chain      | Auth `1.0.25`, Notice `1.0.37`, System `1.0.31`, Admin Shell `1.0.57` | Auth `1.0.26`, Notice `1.0.38`, System `1.0.32`, Admin Shell `1.0.58` | Patch-compatible public registrars and Shell aggregation; existing menu, permission and tenant contracts remain unchanged. |
| Remaining frontend closure |                                               last published versions |                                                  exact versions below | Patch bumps preserve exact internal dependency alignment; no backend or database change.                                   |
| Mango Maven backend        |                                                              `1.0.35` |                                                             unchanged | Verified prerequisite only; not republished.                                                                               |

### Published Packages

| Order | Target                                                                                                                                                 | Version                                                                                  | Status                                                                           |
| ----: | ------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------- |
|     1 | `@mango/common`                                                                                                                                        | `1.0.26`                                                                                 | `PUBLISHED_AND_VERIFIED`                                                         |
|     2 | `@mango/auth`, `@mango/rbac`, `@mango/grid-layout`, `@mango/home`, `@mango/site-shell`                                                                 | `1.0.26`, `1.0.24`, `1.0.17`, `1.0.15`, `1.0.13`                                         | `PUBLISHED_AND_VERIFIED`                                                         |
|     3 | `@mango/grid-widgets`                                                                                                                                  | `1.0.23`                                                                                 | `PUBLISHED_AND_VERIFIED`                                                         |
|     4 | Exact cyclic cohort: `@mango/admin-pages`, `@mango/file`, `@mango/system`                                                                              | `1.0.33`, `1.0.34`, `1.0.32`                                                             | `PUBLISHED_AND_VERIFIED`                                                         |
|     5 | `@mango/calendar`, `@mango/cms`, `@mango/job`, `@mango/link`, `@mango/notice`, `@mango/numgen`, `@mango/payment`, `@mango/template`, `@mango/workflow` | `1.0.34`, `1.0.23`, `1.0.26`, `1.0.20`, `1.0.38`, `1.0.35`, `1.0.26`, `1.0.34`, `1.0.40` | `PUBLISHED_AND_VERIFIED`                                                         |
|     6 | `@mango/workflow-business-example`                                                                                                                     | `1.0.39`                                                                                 | `PUBLISHED_AND_VERIFIED`                                                         |
|     7 | `@mango/admin-shell`                                                                                                                                   | `1.0.58`                                                                                 | `PUBLISHED_AND_VERIFIED`                                                         |
|     8 | `@mango/admin`                                                                                                                                         | `1.0.64`                                                                                 | `PUBLISHED_AND_VERIFIED`                                                         |
|     9 | `@mango/pmo`                                                                                                                                           | `1.3.11`                                                                                 | `PUBLISHED_AND_VERIFIED`                                                         |
|    10 | `@mango/cli`                                                                                                                                           | `1.0.101`                                                                                | `PUBLISHED_AND_VERIFIED`                                                         |
|    11 | Git tag and GitHub Release                                                                                                                             | `v2026.08.06-pmo-1.3.11-cli-1.0.101-profile-sections-release`                            | `CREATED_AND_VERIFIED`                                                           |
|    12 | Latest documentation                                                                                                                                   | release source above                                                                     | `PUBLISHED_AND_VERIFIED`; npm-only batch, versioned docs snapshot not applicable |

### Upgrade Notes

1. Wait until all listed npm coordinates resolve from the configured consume registry. Do not combine the new Notice or Shell package with the old exact-dependency matrix.
2. Install `@mango/cli@1.0.101`, run `mango pmo upgrade --project-dir . --to 1.3.11 --dry-run`, review the projected baseline, then perform the upgrade and run `mango pmo check --project-dir . --locked`.
3. Keep Mango Maven `1.0.35`. Reinstall the frontend with a clean/frozen lock, rebuild the business application, and do not rewrite approved historical documents merely to add a section introduced by PMO `1.3.10` or `1.3.11`.
4. Log in to the real business application and verify `/profile` exposes “我的消息、系统公告、通知设置、登录日志”; exercise each entry with the current account and confirm browser console/network contain no unexplained failures.

### Verification

- PR #745 passed `pr-contract-check`, `pmo-doc-check`, `frontend-pr-quality` and the underlying PMO, CLI, package, documentation and frontend gates. Live branch protection matched `.github/branch-protection-policy.json` before publication.
- All 24 coordinates resolve from npm-hosted and npm-group with identical versions, integrity, shasum and publication timestamps. Every tarball contract passed and each immutable coordinate was published exactly once. CLI release lock matches all 29 local package versions and keeps Maven `1.0.35`.
- Git tag and GitHub Release point to source commit `4592e70a...`; Pages run `31083471251` published and verified Latest documentation from the same source.
- A clean npm-group consumer installed CLI `1.0.101` and PMO `1.3.11`, passed locked PMO validation, generated a module from the published code baseline, passed `vue-tsc` and produced a production build. The real guarantee project also resolved Admin `1.0.64`, Admin Shell `1.0.58`, Notice `1.0.38` and System `1.0.32`, passed its 11/11 upgrade contract, PMO document-set regression, typecheck, production build and runtime health check against database `mango_dev_baohan_system_upgrade_latest_mango_006`.
- The guarantee application's authenticated `/profile` click-through screenshot and console/network capture remain a business UI acceptance follow-up because the verification session had no controllable browser instance. This does not weaken the completed immutable publication, dual-registry verification, package contracts or clean-consumer build evidence.

## v2026.08.06-cli-1.0.100-code-baseline-consumer-fix - 2026-08-06

Status: `PUBLISHED_AND_VERIFIED`. Source commit `d7cca37c49d70de531b21a33bab3719ddf0ee939` (tree `1a03266a0847b848bb343d48b1ea7b99baa43def`) published only `@mango/cli@1.0.100`, tag and GitHub Release `v2026.08.06-cli-1.0.100-code-baseline-consumer-fix`, and verified Latest documentation. The exact-source bundle SHA-256 is `f52c841f7078707710a3b200823c84294bebb5b68b0b7610a958674d2dada3af`. Existing `@mango/pmo@1.3.10` and Mango Maven `1.0.35` were verified and not republished.

### Fixed

- Resolve the installed `@mango/pmo` package through its public entry and locate its owning package root before reading `dist/baseline/code-templates/business-module`.
- Support npm, pnpm virtual-store and workspace-link layouts without assuming the source-only sibling directory name `mango-pmo`.
- Add a packed tarball consumer regression that installs PMO/CLI outside the monorepo, generates a project, runs `mango module add` with `moduleKebab=quality-center` and `aggregateKebab=review-record`, checks baseline convention evidence and rejects unresolved placeholders.

### Versions

| Component                   |                           Previous |   Release | Compatibility                                                                                                                                |
| --------------------------- | ---------------------------------: | --------: | -------------------------------------------------------------------------------------------------------------------------------------------- |
| `@mango/cli`                |                           `1.0.99` | `1.0.100` | Corrective patch for installed-package code-baseline resolution; command syntax, generated module contract and release matrix are unchanged. |
| `@mango/pmo`                |                           `1.3.10` | unchanged | Already published and verified; supplies the canonical code baseline consumed by CLI `1.0.100`.                                              |
| Mango Maven backend         |                           `1.0.35` | unchanged | No Maven artifact, runtime code or database migration is published.                                                                          |
| Other frontend npm packages | CLI `release-versions.json` matrix | unchanged | No runtime frontend package is republished.                                                                                                  |

### Published Packages

| Order | Target                             | Version                                              | Status                                                                                  |
| ----: | ---------------------------------- | ---------------------------------------------------- | --------------------------------------------------------------------------------------- |
|     1 | Existing prerequisite `@mango/pmo` | `1.3.10`                                             | `PUBLISHED_AND_VERIFIED`; not republished                                               |
|     2 | `@mango/cli`                       | `1.0.100`                                            | `PUBLISHED_AND_VERIFIED`                                                                |
|     3 | Git tag and GitHub Release         | `v2026.08.06-cli-1.0.100-code-baseline-consumer-fix` | `CREATED_AND_VERIFIED`                                                                  |
|     4 | Latest documentation               | release source above                                 | `PUBLISHED_AND_VERIFIED`; npm-only batch did not create a Maven versioned docs snapshot |

### Upgrade Notes

1. Do not use `@mango/cli@1.0.99` for `mango module add`; that immutable coordinate resolves PMO correctly for project initialization but uses a source-only path for module templates.
2. Install `@mango/cli@1.0.100` from the consume registry with `@mango/pmo@1.3.10`, then run `mango pmo check --project-dir . --locked`.
3. Existing generated projects and modules do not require rewriting. Retry only the failed `mango module add` command after upgrading the project-local CLI.
4. Keep Mango Maven `1.0.35` and all non-target npm coordinates at the versions in CLI `1.0.100`.

### Verification

- PR #742 passed the protected required checks. PMO/starter projection checks, CLI full tests, the offline packed-tarball regression, release-impact/version-lock checks, generated backend gate, admin styles and documentation audits passed from the protected release source.
- Before publication, both registries proved `@mango/cli@1.0.100` absent and returned identical existing PMO metadata. After publication, hosted/group returned CLI integrity `sha512-UWkwDCi0gpHmlldzByCp6Ws81ZBJ5FrelbhpPSAeFxTwvlSwmJT+ykJwY/y/8IIdIRazrg5ORoWPS1BYi5UmLQ==`, shasum `dd5ebe57b22028a85229d8be06809b390173ea86` and publication time `2026-08-06T04:29:52.947Z`.
- A clean isolated-cache `npm-group` consumer installed remote CLI `1.0.100` and PMO `1.3.10`, initialized a full project, generated `quality-center/review-record`, passed locked PMO validation, proved all 11 code-baseline conventions and found zero unresolved placeholders.
- The completed recovery manifest SHA-256 is `e5cef422ac6feeb423896e0d41ef962912b94951c6baff9217fbe2c89560ff4a`. The preserved pre-immutable failed manifest SHA-256 is `1299751530902e2b12a2a312d9f2c9a8297c4dd79d3dbde03ea47d94babab9e0`; it failed only on an invalid release-impact base before Tag, Release or npm mutation. The earlier `1.0.99` failed post-verify manifest remains `1501a6dbc47d05954aad61da3f779b13aa79def68a6fde1516c0c640cf47e89c`.

## v2026.08.06-pmo-1.3.10-cli-1.0.99-code-baseline-release - 2026-08-06

Status: `PUBLISHED_WITH_FAILED_POST_VERIFY`. Source commit `d22c5dde80c52c83f50b7b65f172dad38d6168c3` (tree `05167e2e1712fa42107758e8a6168f9bf91ae341`) published `@mango/pmo@1.3.10` and `@mango/cli@1.0.99`, tag and GitHub Release `v2026.08.06-pmo-1.3.10-cli-1.0.99-code-baseline-release`, and Latest docs. The exact-source bundle SHA-256 is `f9d4e17ba0abb1549ce10776c080de7899aae02b3efb7d84a22963fb53c28b5e`. Dual-registry package verification passed, but the clean consumer proved CLI `1.0.99` resolves module templates through a source-only path and fails `mango module add`; the failed manifest SHA-256 is `1501a6dbc47d05954aad61da3f779b13aa79def68a6fde1516c0c640cf47e89c`. Do not republish or declare this batch complete.

### Added

- Add the canonical `business-module` code baseline, covering the backend API/core/starter/starter-remote module and package structure, Flyway/resources, frontend API/page packages, public exports and focused tests.
- Add typed template inputs such as `moduleKebab`, derived names, manifest-declared outputs, rule coverage, template self-checks and generated-project checks.
- Bind concrete Mango conventions to template evidence, including `XxxCode implements BizCode`, `Require`, typed CRUD services, tenant entities, Mapper/API/resource/migration patterns, frontend registration and tests.
- Bind generated Java to Mango Checkstyle and architecture rule profiles so templates demonstrate each governed code rule through executable code and machine checks.

### Changed

- Make code baselines and target repository source the default development inputs; references are directed lookups instead of a mandatory bulk-reading stage.
- Require newly generated code to follow the selected baseline even when surrounding legacy code is non-standard, while limiting cleanup of touched legacy code to behavior-preserving, locally verifiable violations in the same symbol or code block.
- Package the canonical code templates, renderer/evaluator, development routing and synchronized business-starter projection in PMO `1.3.10`; CLI `1.0.99` consumes that exact PMO version for project generation and `mango module add`.

### Versions

| Component                                                                    |                               Previous |                            Release | Compatibility                                                                                                                                                                           |
| ---------------------------------------------------------------------------- | -------------------------------------: | ---------------------------------: | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `@mango/pmo`                                                                 |                                `1.3.9` |                           `1.3.10` | Patch-compatible baseline and workflow update; lifecycle contract schema remains revision `1`, with `1.3.9` admitted only through the existing controlled historical-document baseline. |
| `@mango/cli`                                                                 |                               `1.0.98` |                           `1.0.99` | Patch-compatible CLI/template release that exactly depends on PMO `1.3.10`; command names and existing generated project runtime behavior remain compatible.                            |
| Canonical business-module code baseline, starter/module templates and Skills |      PMO `1.3.9` / CLI `1.0.98` bundle | PMO `1.3.10` / CLI `1.0.99` bundle | New module generation uses the canonical template; existing business source is not rewritten automatically.                                                                             |
| Mango Maven backend                                                          |                               `1.0.35` |                          unchanged | No Java runtime, Maven artifact or database migration is published.                                                                                                                     |
| Other frontend npm packages                                                  | current `release-versions.json` matrix |                          unchanged | No runtime frontend package is republished.                                                                                                                                             |

### Published Packages

| Order | Target                     | Version                                                   | Status                                                           |
| ----: | -------------------------- | --------------------------------------------------------- | ---------------------------------------------------------------- |
|     1 | `@mango/pmo`               | `1.3.10`                                                  | `PUBLISHED_AND_VERIFIED`                                         |
|     2 | `@mango/cli`               | `1.0.99`                                                  | `PUBLISHED_BUT_CONSUMER_FAILED`; superseded by planned `1.0.100` |
|     3 | Git tag and GitHub Release | `v2026.08.06-pmo-1.3.10-cli-1.0.99-code-baseline-release` | `CREATED_AND_VERIFIED`                                           |
|     4 | Latest documentation       | release tag above                                         | `PUBLISHED_AND_VERIFIED`; no Maven versioned docs snapshot       |

### Upgrade Notes

1. Do not install `@mango/cli@1.0.99` for module generation; use the corrective CLI `1.0.100` with the already published PMO `1.3.10`.
2. In an existing business repository, run `mango pmo upgrade --project-dir . --to 1.3.10 --dry-run`, review the baseline, code-template, Agent and Skill changes, then run the upgrade with the project's normal shell-sync choice and finish with `mango pmo check --project-dir . --locked`.
3. Use `mango module add` for new modules so generation starts from the published canonical business-module baseline. Existing modules are not bulk-rewritten; clean only behavior-preserving violations in the exact legacy symbol or block touched by a future change.
4. Keep Mango Maven `1.0.35` and every non-target npm coordinate at the versions packaged in CLI `1.0.99`. No database rebuild or migration is required by this release.

### Verification

- PR #737 passed `pr-contract-check`, `pmo-doc-check`, `frontend-pr-quality` and the underlying PMO, CLI/JavaScript, Java and documentation gates before merge.
- PMO package build/pack verification, CLI tests, code-baseline tests, template projection checks, release-impact/version checks, generated backend gate, admin style checks and capability-document audits passed from the protected release commit.
- Both packages were absent before publication. Hosted/group returned matching metadata after publication: PMO integrity `sha512-MP6+bXOlWWlVC46mIX4zl5hQkWKZyZM0iZXmHVYMyHDCx6NXyk+5I2E85QPArfRqR3kAGV+Wh86cIxDidastCA==`, shasum `80554aa93dec2316160b2483cb2fa72be23c62ad`, published `2026-08-06T02:47:38.897Z`; CLI integrity `sha512-N0fpZjfLZuAS5vTmmLnBtSPNPscsLm43iXilg2AZ+mzEFJM/PSS1LDpK8uiu3I3Fc9Ka8XTysyUJrmjena0AwQ==`, shasum `ca4a6288c65e0860a8ac14b55064ac5587b8c8d5`, published `2026-08-06T02:47:54.892Z`.
- Clean `npm-group` installation and full project generation succeeded, but `mango module add quality-center --aggregate review-record` failed before rendering because CLI looked for `mango-pmo/code-templates/business-module` beside its pnpm store package instead of resolving installed `@mango/pmo`. This is a release-blocking consumer failure, not a verification exception.

## v2026.08.05-maven-1.0.35-numgen-1.0.34-admin-1.0.63-cli-1.0.98-release - 2026-08-05

Status: `PUBLISHED_AND_VERIFIED`. Published from protected `main` source commit `ce9262beccad859a43299d7df554747e1b108b0a` (tree `4ec8532b59a3f4c377715b4e387cf6c814ee7f1e`) after Issue #730, implementation PR #731 and release PR #733 passed their required checks. The exact-source bundle SHA-256 is `3e22057b0af7c3e13f1bedf733952ae7c210a8bd85481c06ade0a558c7f91d41`. Maven `1.0.35`, `@mango/numgen@1.0.34`, `@mango/admin@1.0.63` and `@mango/cli@1.0.98` are `PUBLISHED_AND_VERIFIED`; the immutable tag and [GitHub Release](https://github.com/HardyDou/mango/releases/tag/v2026.08.05-maven-1.0.35-numgen-1.0.34-admin-1.0.63-cli-1.0.98-release) are `CREATED_AND_VERIFIED`.

The completed read-only verification manifest SHA-256 is `d1021af285b1ed607db2ba210526e75a95ac544f26f6ca7b5cbf495cb2900c5a`. Audit evidence preserves the original failed publication manifest `603c16f59695e7544476f3ee8c7187257c6fc86e5f55dd1a74bbd5a87506e249`, artifact-recovery manifest `f9d8a1896107ff41e76c99717553241d2bbb7a6f8d67c555b8dee5dadc91d360` and final-recovery harness-failure manifest `f3b12104d9530d2b1c4c8b54e6cfc40ae85b3cb29810a925dd500cd184c55b9e`. The first Maven attempt's three partial Nexus components were deleted under receipt `d636161625fa55bd8d70ed4874389e79398723d7731f1b13b256e51db112d571` only after 668 publish/consume checks proved all 334 target paths absent (`9a52894b608442508e7bf03b49ae2549b07ad8949f7679c3a580bc893b299046`). The canonical Maven batch then ran once from the exact source; no passed immutable coordinate was republished during later recovery.

### Fixed

- Allow numgen DATE segments to use `MMdd` and arbitrary `DateTimeFormatter` patterns from the management page, with live preview and explicit validation errors.
- Support daily and yearly sequence grouping needed by the business guarantee-number formats `YYYY年分离式字第XXMMDDNNNN号` and `AAA（YYYY）年第XXNNNN号`.
- Keep the persisted frontend configuration, backend date validation and injected-clock tests aligned.

### Versions

| Component                                                    | Previous |   Release | Compatibility                                                                 |
| ------------------------------------------------------------ | -------: | --------: | ----------------------------------------------------------------------------- |
| Mango Maven non-app backend and `io.mango:mango-docs-bundle` | `1.0.34` |  `1.0.35` | Patch-compatible numgen rule validation and rendering; no database migration. |
| `@mango/numgen`                                              | `1.0.33` |  `1.0.34` | Patch-compatible management-page configuration and preview behavior.          |
| `@mango/admin`                                               | `1.0.62` |  `1.0.63` | Republished aggregate closure for the fixed numgen package.                   |
| `@mango/cli`                                                 | `1.0.97` |  `1.0.98` | Locks Maven `1.0.35` and the matching frontend matrix.                        |
| `@mango/pmo`                                                 |  `1.3.9` | unchanged | No PMO source or packaged baseline change.                                    |

### Published Packages

| Order | Target                                                          | Version                                                                  | Status                   |
| ----: | --------------------------------------------------------------- | ------------------------------------------------------------------------ | ------------------------ |
|     1 | Complete non-app Maven Reactor and `io.mango:mango-docs-bundle` | `1.0.35`                                                                 | `PUBLISHED_AND_VERIFIED` |
|     2 | `@mango/numgen`, `@mango/admin`, `@mango/cli`                   | `1.0.34`, `1.0.63`, `1.0.98`                                             | `PUBLISHED_AND_VERIFIED` |
|     3 | Git tag and GitHub Release                                      | `v2026.08.05-maven-1.0.35-numgen-1.0.34-admin-1.0.63-cli-1.0.98-release` | `CREATED_AND_VERIFIED`   |
|     4 | Latest docs and versioned docs snapshot                         | release tag above                                                        | `PUBLISHED_AND_VERIFIED` |

### Upgrade Notes

1. Upgrade the complete Mango backend matrix to Maven `1.0.35`; do not mix Maven `1.0.34` modules with this batch.
2. Install `@mango/cli@1.0.98` for new or upgraded projects so the packaged `release-versions.json` selects Maven `1.0.35`, `@mango/numgen@1.0.34` and `@mango/admin@1.0.63`.
3. Existing numgen rules remain valid. Configure a DATE segment with `MMdd` or another `DateTimeFormatter` pattern, and set DATE `sequenceScope=1` when the sequence should reset by the rendered date group.

### Verification

- Core Maven tests passed 26/26 with changed-only Java quality `newIssueCount=0`; numgen frontend unit tests passed 3/3 together with build, E2E persistence, page configuration and frontend baseline checks in PR #731.
- Maven `1.0.35` and `io.mango:mango-docs-bundle:1.0.35` resolve from both publish and consume repositories. All Maven build and consumer commands used release-specific repositories under `.runtime`, never the shared `~/.m2/repository`.
- Both npm registries returned and verified the published tarballs for `@mango/numgen@1.0.34`, `@mango/admin@1.0.63` and `@mango/cli@1.0.98`, including matching consume-registry versions and integrity values.
- A clean npm-group consumer installed CLI `1.0.98`, generated a full monolith project with Maven `1.0.35` / PMO `1.3.9` / numgen `1.0.34` / admin `1.0.63`, passed locked PMO validation (142 files and 33 Skill files with no drift), frontend typecheck and production build, and the complete backend `mvn verify` using isolated `.runtime/release-m2-business-1.0.35`.
- Pages run [31004400570](https://github.com/HardyDou/mango/actions/runs/31004400570) built and deployed from merged snapshot PR #735. The [versioned documentation snapshot](https://hardydou.github.io/mango/versions/v2026.08.05-maven-1.0.35-numgen-1.0.34-admin-1.0.63-cli-1.0.98-release/) returns HTTP 200.
- The completed read-only state machine records every applicable state as `passed`, including source, required checks, tag, GitHub Release, Maven/npm dual-registry verification, documentation, clean consumer verification and cleanup. The release tag remains pinned to source commit `ce9262be...`; this closeout is documentation-only.

## v2026.08.05-maven-1.0.34-workflow-withdraw-release - 2026-08-05

Status: `PUBLISHED_AND_VERIFIED`. Published from protected `main` commit `811cc642e04b3dacc5f12dd0255e81cf739afab1` (tree `6decfe773756d59c83b0b13c7d6f9ba27d573503`) after Issue #725 and its required checks passed. The exact-source bundle SHA-256 is `f31db99c094cb3810ab3f2e159608d910c35e2c7a8b90d82f95e671792bd717d`. The complete non-app Maven Reactor and matching `io.mango:mango-docs-bundle:1.0.34` are published and verified from both configured Maven repositories. CLI, PMO and frontend npm coordinates are unchanged and were not republished.

The completed recovery publication manifest SHA-256 is `6a13c07ebc6dd10b4d92031b42fdb457a1619ba99a613bf64d0f97e38f7498ee`; the preserved first-attempt manifest SHA-256 is `0c6df7bd3baca910daccd2a2c3cdd39b88c9edd72091f373e39970b1b927773a`. The first attempt's isolated `io.mango.tools:mango-architecture-verification:1.0.34` component was removed after publish/consume back-checks proved it was the only partial remote mutation; no immutable coordinate was overwritten or republished. The completed state machine recorded all applicable states as `passed`, including clean consumer resolution. The release tag remains pinned to `811cc642...`; the CHANGELOG closeout commit is documentation-only.

### Fixed

- Add the public `WorkflowProcessApi.withdraw()` contract and `POST /workflow/processes/withdraw` endpoint for an original applicant to withdraw a running business approval by application ID or process instance ID.
- Enforce tenant and applicant ownership, the `IN_APPROVAL` state boundary and the `workflow:process:withdraw` permission; repeated withdrawal is idempotent and other terminal states remain unchanged.
- Terminate the Flowable runtime instance and current tasks, persist the `WITHDRAWN` application state and withdrawal audit action, and publish `workflow.process.withdrawn` before `workflow.process.ended`.
- Keep local and remote starter contracts aligned and document the business-owned status-machine and event-consumption responsibilities.

### Versions

| Component                                                    |                 Previous |   Release | Compatibility                                                                                                       |
| ------------------------------------------------------------ | -----------------------: | --------: | ------------------------------------------------------------------------------------------------------------------- |
| Mango Maven non-app backend and `io.mango:mango-docs-bundle` |                 `1.0.33` |  `1.0.34` | Patch-compatible Workflow API, withdrawal behavior and remote adapter additions; no database migration is required. |
| `@mango/cli`                                                 |                 `1.0.97` | unchanged | This Maven-only batch does not republish CLI; generated-project defaults remain unchanged.                          |
| `@mango/pmo`                                                 |                  `1.3.9` | unchanged | No PMO rule, contract, Agent, Skill or packaged baseline changes.                                                   |
| Frontend npm packages                                        | current published matrix | unchanged | No frontend source or npm package is published.                                                                     |

### Published Packages

| Order | Target                                                                     | Version                                              | Status                   |
| ----: | -------------------------------------------------------------------------- | ---------------------------------------------------- | ------------------------ |
|     1 | Complete non-app `io.mango` Maven Reactor and `io.mango:mango-docs-bundle` | `1.0.34`                                             | `PUBLISHED_AND_VERIFIED` |
|     2 | Git tag, GitHub Release, Latest docs and versioned docs snapshot           | `v2026.08.05-maven-1.0.34-workflow-withdraw-release` | `CREATED_AND_VERIFIED`   |

### Upgrade Notes

1. Upgrade all `io.mango` backend dependencies together through `mango-parent` or `mango-bom` `1.0.34`; do not mix individual `1.0.33` and `1.0.34` modules.
2. Existing CLI `1.0.97` projects must set `mango.version=1.0.34` explicitly to consume this Maven-only patch. Keep `@mango/pmo@1.3.9` and the current frontend package matrix unchanged.
3. Business modules must decide whether a document is withdrawable in their own state machine, call the public Workflow API, and handle `workflow.process.withdrawn` idempotently; Workflow does not add a frontend withdrawal button or replace business-state ownership.
4. Consumers using the remote starter must update the complete Maven matrix together so the API, controller and Feign contracts resolve at the same version.

### Verification

- The Workflow API, core, starter and starter-remote gates must pass, including the withdrawal unit suite, H2 plus real Mapper state/audit/task-cleanup verification, HTTP/API surface contracts and Feign contracts.
- The complete non-app Maven batch, documentation bundle, release-note checker, capability documentation gates and repository source checks must pass from the protected release commit.
- Every Maven `1.0.34` coordinate and `io.mango:mango-docs-bundle:1.0.34` must resolve from both publish and consume registries with matching checksums. A clean isolated Maven consumer must resolve the published BOM, Workflow API, Workflow starter and docs bundle without using the source workspace or shared local release cache.
- Publication and consume back-checks covered 120 Maven HTTP verification targets, including the complete non-app Reactor, `io.mango:mango-bom:1.0.34`, `io.mango.tools:mango-architecture-verification:1.0.34` and `io.mango:mango-docs-bundle:1.0.34`; publish and consume repositories returned matching versions and checksums.
- A clean isolated Maven consumer used `.runtime/maven-consumer-1.0.34` and resolved `io.mango.platform.workflow:mango-workflow-api:1.0.34` (219,907 bytes), `io.mango.platform.workflow:mango-workflow-starter:1.0.34` (49,497 bytes) and `io.mango:mango-docs-bundle:1.0.34` (192,530,056 bytes) from `maven-public`.
- The immutable tag resolves to `811cc642e04b3dacc5f12dd0255e81cf739afab1`, and the [GitHub Release](https://github.com/HardyDou/mango/releases/tag/v2026.08.05-maven-1.0.34-workflow-withdraw-release) is non-draft and non-prerelease. Latest docs and the [versioned docs snapshot](https://hardydou.github.io/mango/versions/v2026.08.05-maven-1.0.34-workflow-withdraw-release/) return HTTP 200 and contain the withdrawal contract and integration guidance.

## v2026.08.03-maven-1.0.33-baseline-web-errors-release - 2026-08-03

Status: `PUBLISHED_AND_VERIFIED`. This Maven-only patch was published from protected `main` commit `19a9160a6f57e80b257418f4ca90a208c7cd958a` and tree `97df70447980438cd412ce0f869cc3c06f48f415`; the exact-source bundle SHA-256 is `edd3c2fb9a4e20e41ff6f91ec1ac288490047eb70a05815c8af32be100dbef2a`. The complete non-app Maven Reactor and `io.mango:mango-docs-bundle:1.0.33`, immutable tag, [GitHub Release](https://github.com/HardyDou/mango/releases/tag/v2026.08.03-maven-1.0.33-baseline-web-errors-release), both Maven registries and a clean isolated consumer are verified. The completed read-only verification manifest SHA-256 is `1cbc624b6d79e2664a894699a3ceb203ac21fde6122ec403e5fae1826ce02e97`; the publication manifest SHA-256 is `328c6095af718bd65a21e98043990286e2332b9856cec71bb056c5bd5f99c0cc` and is retained because its post-verifier exceeded Node's default output buffer while listing the 192 MB docs JAR after immutable publication had succeeded. No immutable artifact was republished. CLI `1.0.97`, PMO `1.3.9`, CLI's Maven `1.0.32` default and frontend npm coordinates remain unchanged.

### Fixed

- Make `mango:baseline-generate` create replay, determinism and verification schemas with the explicit default `utf8mb4 / utf8mb4_unicode_ci` pair, so BSQL no longer captures a build machine's `utf8mb4_0900_ai_ci` server default. Explicit safe overrides remain supported and are recorded in the baseline manifest and fingerprint.
- Map Jackson unknown-property, invalid-format and mismatched-input failures to safe full JSON paths such as `materials[0].materialCategoryCode`, stable target categories and a dedicated date-time message. Malformed JSON without a reliable path continues to return HTTP 400 with a safe top-level message.
- Keep request-body error responses free of submitted values, complete request bodies, exception stacks and Java class names; preserve the host application's existing `FAIL_ON_UNKNOWN_PROPERTIES` choice instead of weakening unknown-field validation.

### Versions

| Component                                                    |                 Previous |   Release | Compatibility                                                                                                                                                        |
| ------------------------------------------------------------ | -----------------------: | --------: | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Mango Maven non-app backend and `io.mango:mango-docs-bundle` |                 `1.0.32` |  `1.0.33` | Patch-compatible build determinism and HTTP error-diagnostic fixes; successful request contracts, database migrations and `R` response shape are unchanged.          |
| `@mango/cli`                                                 |                 `1.0.97` | unchanged | This Maven-only batch does not republish CLI; its generated-project default remains Maven `1.0.32`. Consumers opting into these fixes set Maven `1.0.33` explicitly. |
| `@mango/pmo`                                                 |                  `1.3.9` | unchanged | No PMO rule, contract, Agent, Skill, template or packaged baseline changes.                                                                                          |
| Frontend npm packages                                        | current published matrix | unchanged | No frontend code or npm package is published.                                                                                                                        |

### Published Packages

| Order | Target                                                                     | Version                                                | Status                   |
| ----: | -------------------------------------------------------------------------- | ------------------------------------------------------ | ------------------------ |
|     1 | Complete non-app `io.mango` Maven Reactor and `io.mango:mango-docs-bundle` | `1.0.33`                                               | `PUBLISHED_AND_VERIFIED` |
|     2 | Git tag, GitHub Release, Latest docs and versioned docs snapshot           | `v2026.08.03-maven-1.0.33-baseline-web-errors-release` | `CREATED_AND_VERIFIED`   |

### Upgrade Notes

1. Upgrade all `io.mango` backend dependencies together through `mango-parent` or `mango-bom` `1.0.33`; do not mix individual `1.0.32` and `1.0.33` modules.
2. Existing CLI `1.0.97` projects must set `mango.version=1.0.33` explicitly to consume this Maven-only patch. Keep `@mango/pmo@1.3.9` and the current frontend package matrix unchanged.
3. Rebuild BSQL with `mango:baseline-generate`. The default target is `utf8mb4 / utf8mb4_unicode_ci`; deployments that override either value must use a valid matching pair and create the target business database with the same semantics.
4. Applications that require unknown JSON fields to fail must retain their existing Jackson strict setting. No database migration, frontend change or PMO migration is required.

### Verification

- The Maven plugin unit and real-MySQL regression must prove replay, determinism and verification schemas use the configured charset/collation, manifest and fingerprint values match, and invalid combinations fail before schema creation.
- The Web Starter random-port Tomcat suite must cover unknown nested fields, nested list type mismatch, date-time format errors, malformed JSON and sensitive-value non-disclosure.
- The complete non-app Maven batch, documentation bundle, release-note checker, capability documentation gates and repository source checks must pass from the protected release commit.
- Every Maven `1.0.33` coordinate must resolve from both publish and consume registries with matching checksums. A clean isolated Maven consumer must resolve the published BOM, Maven plugin and Web Starter without using the source workspace or shared local release cache.
- Publication verification confirmed 188 coordinates and 334 POM/JAR primary files in both `maven-releases` and `maven-public` with matching SHA-1 values. The isolated consumer also resolved `io.mango:mango-docs-bundle:1.0.33` and confirmed `MySqlSchemaDefaults.class`, `GlobalExceptionHandler.class` and the matching versioned documentation snapshot are present.
- The immutable tag resolves to `19a9160a6f57e80b257418f4ca90a208c7cd958a`. The canonical verification and preserved publication manifests are recorded under `mango-docs/evidence/governance/`.

## v2026.08.03-cli-1.0.97-release-matrix-sync-release - 2026-08-03

Status: `PUBLISHED_AND_VERIFIED`. This CLI-only patch was published from protected `main` commit `ad19d298b1c2c9a13f61bb3875d783d554013f7f` and tree `ebc5b637f69ce99c39ecad48adae16dad5532917`; the exact-source bundle SHA-256 is `41677a9a26946ec4be6ab14365856437cdd9d6c38507cf8a09cbeda48074633b`. `@mango/cli@1.0.97`, the immutable tag, [GitHub Release](https://github.com/HardyDou/mango/releases/tag/v2026.08.03-cli-1.0.97-release-matrix-sync-release), both private registries and a clean generated-project consumer are verified. The completed read-only manifest SHA-256 is `a3754a5d454f86f9289dc36b4d055891d65df74ba704ae6765533266a95ff581`; the earlier publication manifest (`753dd5a29b3b81d2be616e3bd971446198156bb9c222eeb9f2a2664023d671a2`) is retained because its first consume verifier misread Nexus's flat `dist.integrity` keys after publication had already succeeded. No Maven, PMO or runtime frontend package was republished.

### Fixed

- Replace the stale Maven `1.0.31` lock packaged in CLI `1.0.96` with the published Maven `1.0.32` backend.
- Package the current frontend matrix headed by `@mango/admin@1.0.62` and `@mango/common@1.0.25` instead of letting consumers receive the older `1.0.61` / `1.0.24` tuple from CLI `1.0.96`.
- Treat `release-versions.json` changes as CLI release impact so a matrix update requires a new CLI package version.

### Versions

| Component                   |                                                      Previous |                                       Release | Compatibility                                                                                         |
| --------------------------- | ------------------------------------------------------------: | --------------------------------------------: | ----------------------------------------------------------------------------------------------------- |
| `@mango/cli`                |                                                      `1.0.96` |                                      `1.0.97` | Patch-compatible version-lock correction; CLI commands and generated project structure are unchanged. |
| Mango Maven backend         |                                          `1.0.31` in CLI lock |                                      `1.0.32` | References the already-published Maven release; no Maven artifact is republished.                     |
| Frontend aggregate / common | `@mango/admin@1.0.61`, `@mango/common@1.0.24` in CLI `1.0.96` | `@mango/admin@1.0.62`, `@mango/common@1.0.25` | References the already-published 22-package dependency closure.                                       |
| `@mango/pmo`                |                                                       `1.3.9` |                                     unchanged | No PMO source, baseline or package release.                                                           |

### Published Packages

| Order | Target                     | Version                                              | Status                   |
| ----: | -------------------------- | ---------------------------------------------------- | ------------------------ |
|     1 | `@mango/cli`               | `1.0.97`                                             | `PUBLISHED_AND_VERIFIED` |
|     2 | Git tag and GitHub Release | `v2026.08.03-cli-1.0.97-release-matrix-sync-release` | `CREATED_AND_VERIFIED`   |

### Upgrade Notes

1. Install `@mango/cli@1.0.97` from `npm-group`; do not use `1.0.96` as the version source for a newly generated or upgraded business project.
2. Align backend dependencies to Mango Maven `1.0.32` and frontend dependencies to the exact matrix in CLI `1.0.97`'s packaged `release-versions.json`.
3. Keep `@mango/pmo@1.3.9`. Existing databases upgrade in place; no database rebuild, PMO migration or business source change is required by this patch.

### Verification

- Release-impact validation must identify `@mango/cli` when `release-versions.json` changes and reject an unchanged CLI version.
- CLI tests, release-version checks, template projection checks and the Maven `1.0.32` generated-backend gate must pass.
- After publication, `@mango/cli@1.0.97` must resolve from both `npm-hosted` and `npm-group` with matching integrity and shasum.
- A clean consume-registry project must install the published CLI, verify its packaged matrix, generate a project locked to Maven `1.0.32` / PMO `1.3.9` / the current frontend tuple, and pass the applicable static consumer checks.
- `npm-hosted` and `npm-group` returned matching integrity `sha512-9+drk7/oyZUJGaApr7EvcZxclqu2SGvZzWqs1C7+F2Yeb6LgH5SVvbjgWgwQ1u99+sA5Ne4MAswNm3Z66n+OzA==`, shasum `c274fb35d8989271add262f085f1b4bf6581d5d6` and publication time `2026-08-03T08:45:24.851Z`.
- A clean `npm-group` consumer installed `@mango/cli@1.0.97`, verified the packaged Maven `1.0.32` / CLI `1.0.97` / Admin `1.0.62` / Common `1.0.25` / PMO `1.3.9` tuple, and generated a full project with the same backend and frontend locks.

## v2026.08.03-common-1.0.25-dialog-accessibility-release - 2026-08-03

Status: `PUBLISHED_AND_VERIFIED`. This npm-only patch release was published from protected `main` commit `66d825024e588353b387ef164cb1b485d086e449` and tree `727400929eed138842c72e4dcc0c6e2dab0d7be7`. The exact-source bundle SHA-256 is `81e5a90901c3d848334dc2514ff5dec8574a3a29c223e72e27aa110ea8465ff3`; all 22 npm coordinates, both private registries, the Git tag, [GitHub Release](https://github.com/HardyDou/mango/releases/tag/v2026.08.03-common-1.0.25-dialog-accessibility-release) and a clean runtime consumer are verified. The completed release manifest has SHA-256 `7cb0df4e075e1c953e36489e317b4491b41e0cc2b9cecc0e65e36d09b829995f`. Maven `1.0.32`, `@mango/pmo@1.3.9` and `@mango/cli@1.0.96` remain unchanged and were not republished.

### Fixed

- Give `MangoDialog` an accessible name automatically: a non-empty `title` prop becomes the dialog `aria-label`, while a title supplied only through `#title` is associated through `aria-labelledby`.
- Keep custom visual titles, fallback titles and caller-provided Element Plus attributes compatible while allowing role-based queries such as `getByRole('dialog', { name: '批量识别企业资质' })` to identify the dialog.
- Republish the complete fixed-version frontend dependency closure so downstream packages cannot mix `@mango/common@1.0.25` with stale exact workspace dependencies.

### Versions

| Component                                 |           Previous |              Release | Compatibility                                                                                                      |
| ----------------------------------------- | -----------------: | -------------------: | ------------------------------------------------------------------------------------------------------------------ |
| `@mango/common`                           |           `1.0.24` |             `1.0.25` | Patch-compatible accessibility correction; public component props, emits, slots and exposed methods are unchanged. |
| Fixed-version frontend dependency closure |    previous matrix | exact versions below | Patch republish updates only package versions and exact internal dependency pins.                                  |
| Mango Maven backend                       |           `1.0.32` |            unchanged | No Java, Maven or database artifact is published in this npm-only batch.                                           |
| `@mango/pmo` / `@mango/cli`               | `1.3.9` / `1.0.96` |            unchanged | No PMO, CLI, starter-generation behavior or packaged governance baseline is published.                             |

### Published Packages

| Order | Target                                                                                                                                           | Version                                                                        | Status                   |
| ----: | ------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------ | ------------------------ |
|     1 | `@mango/common`, `@mango/grid-layout`, `@mango/grid-widgets`, `@mango/rbac`, `@mango/auth`, `@mango/system`, `@mango/admin-pages`, `@mango/file` | `1.0.25`, `1.0.16`, `1.0.22`, `1.0.23`, `1.0.25`, `1.0.31`, `1.0.32`, `1.0.33` | `PUBLISHED_AND_VERIFIED` |
|     2 | `@mango/calendar`, `@mango/cms`, `@mango/home`, `@mango/job`, `@mango/link`, `@mango/notice`, `@mango/numgen`, `@mango/payment`                  | `1.0.33`, `1.0.22`, `1.0.14`, `1.0.25`, `1.0.19`, `1.0.37`, `1.0.33`, `1.0.25` | `PUBLISHED_AND_VERIFIED` |
|     3 | `@mango/site-shell`, `@mango/template`, `@mango/workflow`, `@mango/workflow-business-example`                                                    | `1.0.12`, `1.0.33`, `1.0.39`, `1.0.38`                                         | `PUBLISHED_AND_VERIFIED` |
|     4 | `@mango/admin-shell`, `@mango/admin`                                                                                                             | `1.0.57`, `1.0.62`                                                             | `PUBLISHED_AND_VERIFIED` |
|     5 | Git tag and GitHub Release                                                                                                                       | `v2026.08.03-common-1.0.25-dialog-accessibility-release`                       | `CREATED_AND_VERIFIED`   |

### Upgrade Notes

1. Aggregate consumers upgrade to `@mango/admin@1.0.62`; direct consumers update every installed affected package to the exact versions in `Published Packages` instead of mixing older fixed internal dependency pins.
2. Consumers that use `MangoDialog` do not need to add accessibility attributes for the normal `title` or `#title` cases. An explicit caller-provided accessible-name attribute remains available for specialized integrations.
3. No backend, database, PMO or CLI migration is required. Keep Mango Maven `1.0.32`, `@mango/pmo@1.3.9` and `@mango/cli@1.0.96`.

### Verification

- PR #708 merged the Issue #707 implementation and PR #709 locked the exact release matrix; all required checks passed before publication.
- Release-impact validation identified the exact 22-package npm dependency closure and checked all fixed workspace dependency versions, starter templates and the unchanged CLI release lock. The complete `mango-ui` build, package exports, 29 local-tarball consumer build and typecheck, and all 295 `@mango/common` tests passed.
- Every coordinate resolves from both `npm-hosted` and `npm-group` with the same version, integrity, shasum and publish timestamp:

| Package                                   | Integrity                                                                                         | Shasum                                     | Publish/consume timestamp  |
| ----------------------------------------- | ------------------------------------------------------------------------------------------------- | ------------------------------------------ | -------------------------- |
| `@mango/common@1.0.25`                    | `sha512-zkUSbetHg3KtAwjmlBU/If5TwSOPQ71eKcmuXbXpwoBRKcCXJGFkcdmkup4ogUWItWnussacKlenFMUVIgmuDg==` | `2c4d410ef4e0011c1d263583445c23023ba81dc1` | `2026-08-03T06:48:02.479Z` |
| `@mango/grid-layout@1.0.16`               | `sha512-O2dKr6Hpw0Qsd87j9OWKCOMYv6yFVt2jdZh/+mXvkwzKAPjHdCXPMJlz1E74LRKqpx8QxTiRBV6Rs5FagBnQGA==` | `48199904a9d24cf355a5e3ab4ef6f1468885135c` | `2026-08-03T06:48:06.937Z` |
| `@mango/grid-widgets@1.0.22`              | `sha512-taqGFVulIk2wypY2y5IJCrf6G4FLU0O7ZrA+A8StKJegDaUeZ2BiVnrovZmQesk3EaM6ZdKgXNkEW7lwrWl9lQ==` | `d032c43fe76539a0e466d73db8eedcc5b7b682bf` | `2026-08-03T06:48:11.440Z` |
| `@mango/rbac@1.0.23`                      | `sha512-mUISlNh5LevxhI/dK1bBku7b087EJOBcyJJRT+4K+evtq8HPRhQxkep9negxpwJbADnw/59rhMsJZ44du7jQ6A==` | `0c1800553cd8cfaaa8b92df039b1c05e66a2ec62` | `2026-08-03T06:48:20.643Z` |
| `@mango/auth@1.0.25`                      | `sha512-Y8eZAePfp5zLHFIICYXT3Cobx8cWBQKEdre92FurdkffHbUsto40VGf92TDDo7AKlDTjSyalS0V/9L+dYDIGJg==` | `6e848752da741d25b07d8406012c9cfd7d9b9fe2` | `2026-08-03T06:48:29.166Z` |
| `@mango/system@1.0.31`                    | `sha512-1ejak0MzYNE+0dNvdmVQBYjbqFe1AGxzUQKKvH68mbezTk/KxL9YCArY0vQnpmNxq0F8Yj1qMM+UTyCv73l5TQ==` | `afdde689f62447f34bf4c2116d4602d196be25a5` | `2026-08-03T06:48:33.991Z` |
| `@mango/admin-pages@1.0.32`               | `sha512-QIsTvBcqhBgFunEgbA74ffv8nHN/6FLCWptje636tKxDXP3sz9xjjBpMv+gJBxMGOOnlsoQ6RoLQF3zkM0t64g==` | `26adf9c9a00a5aab194ab479a7f65e9fe8e00dd7` | `2026-08-03T06:48:39.507Z` |
| `@mango/file@1.0.33`                      | `sha512-cebr67+lOoCpXGLDzq7dxjBxMpiWRd5kFg42I948vRGXa/kWNR37XEVQ8fTXC7l9Q24zty7AHp6j0h+z7JJlfQ==` | `f48d713ffbfcf0fdca5a2f783920d68cb38da908` | `2026-08-03T06:48:43.538Z` |
| `@mango/calendar@1.0.33`                  | `sha512-Y6rNFdLppCEa43GK8yzWIc4jE/iJ35TdY79JXNhhK0QG0CNmyMZ3JVY1EEi9tjUjHflSsQvIbF7JDzmD95Gthg==` | `bf032ebf3e17ff4b8d3bf93976bb5d47de0e2e46` | `2026-08-03T06:48:47.866Z` |
| `@mango/cms@1.0.22`                       | `sha512-c9xC+7RRl4vgUeg2ISJGFd9ja4AhrqzsVPKRqRNzBl4gb39wsEe4xppavsPE3N9X6HUM3VK7x8TXxIPvO3kAqg==` | `0ca8377ce6194945d1987d3cfaed33f78e897f17` | `2026-08-03T06:48:51.702Z` |
| `@mango/home@1.0.14`                      | `sha512-B2o+BLvdTjTnltZBzQJ3z3TDKrS6OAbu382KBMDypZqcOH0/gIWhK4iY2FWJejuhiUfi/GCxK0dKjRzY249MRw==` | `841c1c8c34db0000f2706f6af078c5fdd3a75918` | `2026-08-03T06:48:58.394Z` |
| `@mango/job@1.0.25`                       | `sha512-4vjZ6lHVOcz+/SQUfHaUuyNaHnUhXs/DPlN9neq/6cY/NzFPBUKGGM9UqWFPgoIl7YA/Tar5YRphSDk77ET3eA==` | `278920b8e6cd74994b7bc548eb5cc4e77c3a25ee` | `2026-08-03T06:49:02.287Z` |
| `@mango/link@1.0.19`                      | `sha512-k8UvlUYCxN/EpwbXo9OzifWFzCOcc+RNT0keWl1uUrBgypeISuoEPwG+LFYJr3oPSRiGpxLRwXGJ46PWSoA1Rw==` | `5ffb6aebbac48c58a10ce2950bb8ee92ef86db13` | `2026-08-03T06:49:06.952Z` |
| `@mango/notice@1.0.37`                    | `sha512-3LImBxB2xgND/On8m5IB1ySFn+OmZrC/+08KmW9sqyEJsp6C5D90ooMxZJQ/QQpG8wc+It9PwnXiGUcE+noMFA==` | `ec94517abdb8ae24a9c4b7881546bf99b45a17ce` | `2026-08-03T06:49:11.688Z` |
| `@mango/numgen@1.0.33`                    | `sha512-omodA8KlRSdPqDMtKtjK3yw6w9O512XEkmHxwG6ewFF36aDN0LPSirmjuYXbY2QYiU2Zy5pXy8npGDMNvi92AA==` | `e2dd2bd40b8f2e8c10c9b10821807359fd7b5791` | `2026-08-03T06:49:15.897Z` |
| `@mango/payment@1.0.25`                   | `sha512-F5pcEqRad0AUtEqUlhqe1t+6babM7AMUgZnNldMAjeuvgYEqY5zUDQYuC5vzx5F6REUo3MxHfsn2ya4f3jVdfw==` | `92f86e57eda829fb5dc562cc52524f2b152a8292` | `2026-08-03T06:49:20.681Z` |
| `@mango/site-shell@1.0.12`                | `sha512-5wcg/8x3KteAvvJ3fQYvyop3YSkejIHj7LNDfBt704WmlRkYoxxFfr77VqIbIvsiqEZupXJnH4ia4Mhg2qrpFg==` | `a7436bdb11555da12774945b5667894b6cc13e5a` | `2026-08-03T06:49:25.521Z` |
| `@mango/template@1.0.33`                  | `sha512-j/9ce/jnhunOG+tPHalkBe0ikiRn6aAP+E9Kf0e5STetPl/vN8d7LeEM3YpcqXrspzA2RiFeyAPH3jGduHdkKw==` | `5babcc706aec98869a18b91fcb52dc0f8448bd21` | `2026-08-03T06:49:35.637Z` |
| `@mango/workflow@1.0.39`                  | `sha512-RPw4LuQSnmFcWYt3MqFDGlTJ2ibnUofYK8X0dhpJmwT/lTMcj7iBPD4frgDjgMOkMZ5I0+xqMRcKcYXAk/lRIA==` | `3825b453f20f60e8814fbce838a9e06864276dff` | `2026-08-03T06:49:40.561Z` |
| `@mango/workflow-business-example@1.0.38` | `sha512-J2ztjqJ+4nZHc8uUqj16jWVYqJKyGAL5CdjYgk8PetijeU4QxnVdYyuttBkqtvLPyb4/F38thOW9yPfzki8ALQ==` | `cd07ba8dd1d118330e2508f2f69317b97f7b48bf` | `2026-08-03T06:49:45.662Z` |
| `@mango/admin-shell@1.0.57`               | `sha512-2nFZeTcbW6w/xtXZxudue/GUy51accSnvWWpWgHQ4mjj4SNSbKIyRS5S12IdXvj3Y8xR0fglc0C6xwVwjjyoVg==` | `ac288c5326592ddafc8aa8cfbd03a812db5ce8bd` | `2026-08-03T06:49:54.853Z` |
| `@mango/admin@1.0.62`                     | `sha512-qfXRdCaiMaUmpJ7O+8tFU2fhK6U1DCOz1xtxf61Yw7Als3FhVw5i2qbel3OUVumeIEeNm6mi+Bo8m4OYA5cxbQ==` | `d269afdad90504912394043e6b3dcd851dccf4e2` | `2026-08-03T06:51:19.714Z` |

- A clean project installed `@mango/common@1.0.25` and `@mango/admin@1.0.62` only from `npm-group`, imported the public API and `style.css`, registered Element Plus, verified runtime `role="dialog"` with `aria-label="批量识别企业资质"`, then passed TypeScript and a Vite production build.
- The first four post-publication attempts exposed defects in the temporary verifier itself: Node 22's read-only global `navigator`, missing Element Plus registration, a missing DOM constructor and an incompatible Vue `h()` type fixture. The semantic verifier was corrected to SHA-256 `0feb62a55598bb630773750f4e885c9e3adaa9269b8058e87c97e389978db9f4`; recovery retained all attempts in the same manifest and did not republish any immutable npm coordinate, tag or GitHub Release.

## v2026.08.03-maven-1.0.32-bootstrap-resource-fixes-release - 2026-08-03

Status: `PUBLISHED_AND_VERIFIED`. This Maven-only patch release was published from protected `main` commit `3aa36ccb526469aaeb76bdf0b1b9c3adef430488` and tree `8d380430c7c5384de5536d71d48849f1a963cb81`. The exact-source bundle SHA-256 is `a6c9d3e89f7bfe6125f8c93a328fbb7f1c6d84e86f3f995542de3aa4cda1b42b`; all 188 Maven coordinates, the publish and consume registries, the Git tag, GitHub Release, Latest docs, versioned docs snapshot and a clean Maven consumer are verified. The completed release manifest has SHA-256 `1e2b1bdc1efbb6317feb5701ec1a01e1739c852396e66ee5fa525680fce1d7f8`; CLI, PMO and frontend npm coordinates remain unchanged.

### Fixed

- Prevent a business starter that injects public `mango-workflow-api` contracts such as `WorkflowTaskRuntimeApi`, `WorkflowProcessApi` or `WorkflowBusinessApplyApi` from creating Workflow controllers, runtime services and Flowable during Bootstrap context refresh.
- Preserve the lifecycle boundary: Bootstrap dependency injection uses deferred public API proxies, the Resource step can still create Workflow handlers and Flowable after migration, and Runtime continues to use the original controllers directly.
- Keep `mango.workflow.enabled=false` unchanged and avoid the rejected workarounds of restoring `forceSync()`, pre-creating `ACT_GE_PROPERTY`, enabling Flowable automatic schema creation or injecting dummy services.
- Correct all 22 non-null CMS demo `DATETIME` values from the non-ISO `2026-07-14 09:53:24` form to `2026-07-14T09:53:24`, so the CMS Resource Handler can deserialize demo content during empty-database Bootstrap without changing the shared date-time contract.

### Versions

| Component                                                    |                        Previous |   Release | Compatibility                                                                                                                      |
| ------------------------------------------------------------ | ------------------------------: | --------: | ---------------------------------------------------------------------------------------------------------------------------------- |
| Mango Maven non-app backend and `io.mango:mango-docs-bundle` |                        `1.0.31` |  `1.0.32` | Patch-compatible public Java and HTTP contracts; changes only Bootstrap-time local API resolution.                                 |
| `@mango/cli`                                                 |                        `1.0.96` | unchanged | Existing projects explicitly set `mango.version` to `1.0.32`; generated defaults remain unchanged in this urgent Maven-only batch. |
| `@mango/pmo`                                                 |                         `1.3.9` | unchanged | No PMO source or packaged baseline changes.                                                                                        |
| Frontend npm packages                                        | current `1.0.31` release matrix | unchanged | No frontend source, route, page key or package lock changes.                                                                       |

### Published Packages

| Order | Target                                                           | Version                                                     | Status                   |
| ----: | ---------------------------------------------------------------- | ----------------------------------------------------------- | ------------------------ |
|     1 | Complete non-app `io.mango` Maven Reactor                        | `1.0.32`                                                    | `PUBLISHED_AND_VERIFIED` |
|     2 | `io.mango:mango-docs-bundle`                                     | `1.0.32`                                                    | `PUBLISHED_AND_VERIFIED` |
|     3 | Git tag, GitHub Release, Latest docs and versioned docs snapshot | `v2026.08.03-maven-1.0.32-bootstrap-resource-fixes-release` | `CREATED_AND_VERIFIED`   |

### Upgrade Notes

1. Upgrade all Mango backend dependencies together by setting the shared `mango.version` or imported `mango-bom` to `1.0.32`; do not mix individual `1.0.31` Workflow jars with the new starter.
2. Keep business modules on the public `mango-workflow-api` boundary. No business source adaptation, `forceSync()` compatibility, Flowable switch or manual database DDL is required.
3. Run the normal empty-database `bootstrap apply` before `runtime`. A Bootstrap context may inject public Workflow APIs, but it must not query `ACT_GE_PROPERTY` before Workflow migration.
4. CLI `1.0.96`, PMO `1.3.9` and the existing frontend package matrix remain compatible and are not republished in this Maven-only patch.

### Verification

- Workflow starter L1 unit tests pass: 22 tests, 0 failures, 0 errors and 0 skipped.
- CMS starter L1 unit tests pass: 16 tests, 0 failures, 0 errors and 0 skipped; the resource contract test now parses every non-null CMS demo `DATETIME` with `LocalDateTime.parse` instead of accepting the invalid space-separated format.
- The regression test proves that a Bootstrap business consumer can inject `WorkflowTaskRuntimeApi` without creating `WorkflowTaskController`; the first API invocation resolves the original controller, while Runtime and disabled-Workflow modes do not register the Bootstrap proxy.
- Backend test-double audit and deterministic test-quality checks pass with no block or warning.
- Module README and source-fact audits pass. A local complete non-app Reactor install of `1.0.32-local-SNAPSHOT` completed 186/186 modules, and the installed Workflow starter contains the new isolation auto-configuration.
- Per explicit user direction, no additional Mango-side local database, service-start, API, UI or browser test is part of the implementation gate; the business project may independently consume the local snapshot for empty-database verification, while protected-branch required checks and immutable release repository verification remain mandatory.
- Formal release verification passed for all 188 coordinates in both `maven-releases` and `maven-public`; SHA-1 values match for 334 primary artifacts and POM files. A clean isolated Maven repository resolved the BOM plus the Workflow and CMS starters transitively, then verified the Bootstrap isolation class and all 22 ISO CMS demo timestamps.
- The initial Maven adapter completed remote publication but exceeded the release runner's captured-output buffer. Both registries already contained 188/188 coordinates, so recovery used the same manifest's `verify-existing` adapter and did not republish any immutable artifact.

## v2026.08.02-maven-1.0.31-pmo-1.3.9-cli-1.0.96-platform-identity-bootstrap-release - 2026-08-02

Status: `PUBLISHED_AND_VERIFIED`. This indivisible mixed release was published from protected `main` commit `50ad66fc14234b696b88edcd12f31f0715544b5d` and tree `2001eb07e3a84ed7d0bfabb51939837cc1eb8d4c`. The exact-source bundle SHA-256 is `f90d8e07767e172ba915da8a291a53912d684918d971bf1c5ec472897395df56`; Maven/npm publish and consume registries, the Git tag, GitHub Release, Latest docs, versioned docs snapshot and a clean generated business consumer are verified. The completed read-only release manifest has SHA-256 `0f0bc0339a2e5f622bc075a1b549fc42e83aca631f69fa3a404172139fda9db6` and records all 17 release states as passed.

### Added

- Add configurable third-party identity providers, provider authorization/binding, and complete personal settings for avatar, real name, phone, email, password and theme.
- Add typed Resource declarations with Bootstrap-required and runtime-eventual reconciliation, stable Bootstrap receipts, generation fencing and explicit runtime ownership for generated and existing business applications.
- Add personal-settings entries for the message center, receive preferences and login logs while preserving registered page keys and authorization boundaries.

### Changed

- Split Bootstrap, Workflow/Flowable, Job and Notice initialization from runtime workers so migration, reconciliation and runtime activation have one deterministic lifecycle order.
- Package PMO `1.3.9` with terminal Gitea PR edit handling, typed full-page baseline exceptions and the synchronized business baseline; CLI `1.0.96` locks and installs that exact PMO version.
- Advance the complete generated-project matrix to Maven `1.0.31`, PMO `1.3.9`, CLI `1.0.96` and the exact frontend package versions below.

### Fixed

- Recover deduplicated file uploads by content hash without returning a deleted logical file record, including concurrent save and upload-session paths.
- Prevent database-dependent Flowable metadata, Resource handlers and runtime workers from starting before the Bootstrap-owned migration and reconciliation phases complete.
- Make CLI generation, module formatting, backend-gate dependency installation, BSQL validation and executable Boot JAR checks independent of workspace links and incidental local registry state.
- Keep repository and CI source-mode backend installs in one Maven reactor with both `:mango-bom` and the selected backend module. Maven `-am` does not automatically add an imported BOM, so the repository manifest checker now rejects an incomplete selector before startup.
- Make Resource declaration identity deterministic across non-Web Bootstrap and Web Runtime processes by isolating canonical fingerprint serialization from host Jackson customizations, including `Long`-as-string HTTP serializers.
- Keep delegated PMD, Checkstyle and SpotBugs Maven invocations on the outer build's resolved local repository, settings, profiles and CI-friendly version so isolated or custom-registry business builds do not fall back to a different repository context.
- Keep generated Gitea PMO checks valid when a merged or closed PR body is edited, without manufacturing an invalid base/head diff.

### Versions

| Component                                                    |              Previous |                                                                                                                                                                     Release | Compatibility                                                                                                                                                   |
| ------------------------------------------------------------ | --------------------: | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------: | --------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Mango Maven non-app backend and `io.mango:mango-docs-bundle` |              `1.0.30` |                                                                                                                                                                    `1.0.31` | Patch release; all `io.mango` dependencies must move together through `mango-parent` or `mango-bom`.                                                            |
| `@mango/pmo`                                                 |               `1.3.8` |                                                                                                                                                                     `1.3.9` | Current rules, contracts, tools, Agents, Skills and generated business baseline are one package; unchanged pinned `1.3.8` lifecycle documents remain supported. |
| `@mango/cli`                                                 |              `1.0.95` |                                                                                                                                                                    `1.0.96` | Depends exactly on PMO `1.3.9` and generates Maven/frontend locks from this batch.                                                                              |
| Direct frontend changes                                      |       previous matrix | `@mango/common@1.0.24`, `@mango/auth@1.0.24`, `@mango/admin-pages@1.0.31`, `@mango/admin-shell@1.0.56`, `@mango/cms@1.0.21`, `@mango/notice@1.0.36`, `@mango/system@1.0.30` | Public package contracts remain patch-compatible; identity/profile and personal-settings behavior is added through existing extension points.                   |
| Aggregate frontend                                           | `@mango/admin@1.0.60` |                                                                                                                                                       `@mango/admin@1.0.61` | Exact dependency closure for the frontend packages in this batch.                                                                                               |
| Dependency-propagated frontend packages                      |       previous matrix |                                                                                                                                                        exact versions below | Patch republish prevents fixed workspace dependencies from mixing old and new package tuples.                                                                   |

### Published Packages

| Order | Target                                                                                                                                                                                                                         | Version                                                                                                                | Status                   |
| ----: | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------- | ------------------------ |
|     1 | Complete non-app `io.mango` Maven Reactor and `io.mango:mango-docs-bundle`                                                                                                                                                     | `1.0.31`                                                                                                               | `PUBLISHED_AND_VERIFIED` |
|     2 | `@mango/pmo`                                                                                                                                                                                                                   | `1.3.9`                                                                                                                | `PUBLISHED_AND_VERIFIED` |
|     3 | `@mango/common`, `@mango/grid-layout`, `@mango/grid-widgets`, `@mango/rbac`, `@mango/auth`, `@mango/system`, `@mango/admin-pages`, `@mango/file`                                                                               | `1.0.24`, `1.0.15`, `1.0.21`, `1.0.22`, `1.0.24`, `1.0.30`, `1.0.31`, `1.0.32`                                         | `PUBLISHED_AND_VERIFIED` |
|     4 | `@mango/calendar`, `@mango/cms`, `@mango/home`, `@mango/job`, `@mango/link`, `@mango/notice`, `@mango/numgen`, `@mango/payment`, `@mango/site-shell`, `@mango/template`, `@mango/workflow`, `@mango/workflow-business-example` | `1.0.32`, `1.0.21`, `1.0.13`, `1.0.24`, `1.0.18`, `1.0.36`, `1.0.32`, `1.0.24`, `1.0.11`, `1.0.32`, `1.0.38`, `1.0.37` | `PUBLISHED_AND_VERIFIED` |
|     5 | `@mango/admin-shell`, `@mango/admin`                                                                                                                                                                                           | `1.0.56`, `1.0.61`                                                                                                     | `PUBLISHED_AND_VERIFIED` |
|     6 | `@mango/cli`                                                                                                                                                                                                                   | `1.0.96`                                                                                                               | `PUBLISHED_AND_VERIFIED` |
|     7 | Git tag, GitHub Release, Latest docs and versioned docs snapshot                                                                                                                                                               | `v2026.08.02-maven-1.0.31-pmo-1.3.9-cli-1.0.96-platform-identity-bootstrap-release`                                    | `CREATED_AND_VERIFIED`   |

Unchanged npm coordinates are `@mango/api-schema@1.0.3`, `@mango/app-runtime@1.0.6`, `@mango/http-client@1.0.0`, `@mango/link-openapi@1.0.4` and `@mango/link-page@1.0.7`; they are not republished.

### Upgrade Notes

1. Treat this release as one tuple. Do not combine Maven `1.0.30` or any other `1.0.3x` backend with CLI `1.0.96`, PMO `1.3.9` or only part of the frontend matrix.
2. Parent consumers set the shared `mango.version` to `1.0.31`; projects with another parent import `io.mango:mango-bom:1.0.31` and omit versions from BOM-managed Mango dependencies. Existing databases upgrade in place and must not be rebuilt.
3. Upgrade the frontend to the exact versions in `Published Packages`. Aggregate consumers use `@mango/admin@1.0.61`; direct consumers update every installed affected package rather than relying on transitive version mixing.
4. Install `@mango/pmo@1.3.9` before `@mango/cli@1.0.96`. Then run `mango pmo upgrade --project-dir . --to 1.3.9 --sync-shell`, review the PMO/workflow diff, and run `mango pmo check --project-dir . --locked`.
5. Re-run `mango workspace init`, then use the project-local CLI for `mango dev doctor` and the normal startup flow. Mango source-repository or derived CI manifests that install a selected backend with Maven `-pl ... -am` must include both `:mango-bom` and the backend module. Preserve `.mango`, the Bootstrap audit tables and application logs before upgrading an existing `1.0.30`/`1.0.3x` project.
6. Modules with an old monolithic `resource-manifest.json` migrate to typed `META-INF/mango/resources/*-common-*.json|yml|yaml` declarations. Keep formal built-in resources `BOOTSTRAP_REQUIRED`; use `RUNTIME_EVENTUAL` only for explicitly non-blocking reconciliation. Resource fingerprints in `1.0.31` are stable across Bootstrap/Runtime Jackson profiles; an older `BOOTSTRAP_FINGERPRINT_MISMATCH` is recovered by upgrading the complete tuple and planning a new generation, not by deleting the database or editing Bootstrap audit tables.
7. Projects with custom identity providers, login pages, personal settings, Notice routes or Shell composition must re-check provider secrets, callback URLs, permissions, tenant boundaries, page registration and package style imports after the tuple upgrade.

### Verification

- `pnpm -C mango-ui release:impact --base=4a959d2ac3906f1cfa14359eb3abf2cd652733b1 --head=HEAD` passes and identifies the exact 24-package npm publication closure.
- `node mango-pmo/tools/workspace-layout-check.mjs --root .` passes in the isolated release worktree.
- PRs #696 through #700 passed all required checks; final protected `main` Frontend Quality run `30769520118`, including the real-backend browser P0 job, and Pages run `30769520109` passed before publication.
- All 188 Maven coordinates resolve from publish and consume registries with matching checksums; `mango-bom:1.0.31` has SHA-1 `a7f81e02970485c6a9ee5d0f69942ff09dde1fc9` and `mango-docs-bundle:1.0.31` has SHA-1 `88b35c275647d28b9a435987c795d292e72eed9c`.
- All 24 npm coordinates resolve from publish and consume registries with matching versions, integrity and shasum. Publication timestamps span `2026-08-02T22:31:25.874Z` through `2026-08-02T22:34:51.163Z`.
- A clean consume-registry-only project installed `@mango/cli@1.0.96`, generated the Maven `1.0.31`/PMO `1.3.9` full monolith preset, passed locked PMO validation, frontend install/typecheck/production build and backend Maven `verify` with an isolated local repository.
- The final read-only manifest is `.runtime/releases-1.0.31-final-verify-3/1.0.31/manifest.json`; its durable release summary is `mango-docs/evidence/governance/release-v2026.08.02-maven-1.0.31-pmo-1.3.9-cli-1.0.96-platform-identity-bootstrap-release.json`.

## v2026.08.01-maven-1.0.30-cli-1.0.95-flyway-startup-order-release - 2026-08-01

Status: `PUBLISHED_AND_VERIFIED`. This mixed patch release was published from source commit `4a959d2ac3906f1cfa14359eb3abf2cd652733b1` and tree `adecfa0f1befd2aafb2a517e3e33285573a0bbb9`; the exact-source bundle SHA-256 is `29f2407b9927958c8ef37a264d0dcad83ca4aa45afde507f1dc7618dfb071783`. The complete non-app `io.mango` Maven Reactor, matching `io.mango:mango-docs-bundle:1.0.30`, and `@mango/cli@1.0.95` resolve from both publish and consume registries. The tag and GitHub Release are `CREATED_AND_VERIFIED` and remain attached to the release source commit. The completed exact-source read-only manifest has SHA-256 `bb156cb7e52867885af4985acbbb2e444e3238710b13191abf9c95a0a3499b9f` and records all 17 release states as passed. `@mango/pmo` remains `1.3.8`; frontend runtime packages are unchanged and were not republished. The durable release summary is `mango-docs/evidence/governance/release-v2026.08.01-maven-1.0.30-cli-1.0.95-flyway-startup-order-release.json`.

### Fixed

- Restore module EXPAND migration before database-dependent Workflow/Flowable metadata initialization for legacy business applications that call `SpringApplication.run` without configuring `mango.bootstrap.mode`.
- Exclude the Bootstrap lifecycle-owned schema from Persistence module discovery so compatibility startup does not claim or migrate Bootstrap control tables.
- Preserve the explicit lifecycle boundary: `bootstrap` performs migration and `runtime` does not execute Flyway.

### Versions

| Component                                                    |               Previous |   Release | Compatibility                                                                                                                                     |
| ------------------------------------------------------------ | ---------------------: | --------: | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| Mango Maven non-app backend and `io.mango:mango-docs-bundle` |               `1.0.29` |  `1.0.30` | Patch fix for direct-start database initialization ordering; public Java/HTTP contracts and explicit Bootstrap lifecycle semantics are unchanged. |
| `@mango/cli`                                                 |               `1.0.94` |  `1.0.95` | Locks generated and upgraded backends to Maven `1.0.30`; continues to depend exactly on `@mango/pmo@1.3.8`.                                       |
| `@mango/pmo`                                                 |                `1.3.8` | unchanged | No PMO source changed; the package is not republished.                                                                                            |
| Frontend runtime packages                                    | current release matrix | unchanged | No frontend package source or lock changed; packages are not republished.                                                                         |

### Published Packages

| Order | Target                                       | Version                                                            | Status                   |
| ----: | -------------------------------------------- | ------------------------------------------------------------------ | ------------------------ |
|     1 | Maven non-app Reactor including `io.mango:*` | `1.0.30`                                                           | `PUBLISHED_AND_VERIFIED` |
|     2 | `io.mango:mango-docs-bundle`                 | `1.0.30`                                                           | `PUBLISHED_AND_VERIFIED` |
|     3 | `@mango/cli`                                 | `1.0.95`                                                           | `PUBLISHED_AND_VERIFIED` |
|     4 | Git tag and GitHub Release                   | `v2026.08.01-maven-1.0.30-cli-1.0.95-flyway-startup-order-release` | `CREATED_AND_VERIFIED`   |

### Upgrade Notes

1. Use the published Maven `1.0.30` non-app Reactor, `io.mango:mango-docs-bundle:1.0.30`, and `@mango/cli@1.0.95` as one verified compatibility batch.
2. Existing business projects inheriting `mango-parent` update their shared `mango.version` to `1.0.30`; projects with another parent import `io.mango:mango-bom:1.0.30` and omit versions from BOM-managed Mango dependencies.
3. Install `@mango/cli@1.0.95` for newly generated or upgraded projects. Keep `@mango/pmo@1.3.8` and the current frontend runtime package matrix.
4. Existing databases upgrade in place. Do not rebuild or manually pre-create Flowable tables. Direct-start applications leave `mango.bootstrap.mode` unset; applications using the governed lifecycle continue to run `bootstrap apply` before `runtime`.

### Verification

- PR #675 passed all required checks and merged as `f3088aeb046d7936d5170cb3737828084c24831b` before release preparation.
- Persistence starter targeted tests and module `verify` cover compatible direct startup, explicit bootstrap/runtime modes, module discovery and reserved Bootstrap schema exclusion.
- A real MySQL empty-database Workflow consumer created the module and Flowable schema in the governed order before ProcessEngine creation.
- PR #676 prepared and locked Maven `1.0.30` and CLI `1.0.95`; PR #678 published the matching versioned documentation snapshot. Their required checks passed before the corresponding release step.
- All 188 Maven `1.0.30` coordinates resolve from the publish and consume repositories. Each main artifact has the same SHA-1 on both sides; the complete checksum matrix SHA-256 is `10932fa711a2519a6bf2d00b405f743553ad8b9b1455a2fd0d7da1eb43e75c5d`.
- `@mango/cli@1.0.95` resolves from both npm registries with integrity `sha512-kPf9yAYzVSVvRdiMqVq8NoZYuHPBxvBSnXDUXkA7tgh/V4vZAiAyV0JOe2yEvSy6EieAaCIsg/GP4trj41Qdsg==`; it was published at `2026-08-01T11:51:44.438Z`.
- A clean consumer installed CLI `1.0.95` from the consume registry without an explicit `--mango-version`, generated Maven `1.0.30` with PMO `1.3.8` and 23 matrix-aligned `@mango/*` dependencies, passed `mango pmo check --locked`, frontend typecheck, and production build. An isolated Maven local repository also consumed `io.mango:mango-bom:1.0.30` from the consume registry.
- Latest documentation and the tag-addressed snapshot at `https://hardydou.github.io/mango/versions/v2026.08.01-maven-1.0.30-cli-1.0.95-flyway-startup-order-release/` both return HTTP 200.
- The completed exact-source read-only manifest records all 17 release states as passed. Earlier failed manifests are preserved as audit evidence: the first captures Maven publication output-buffer exhaustion followed by verify-existing recovery and an over-strict optional CLI package assertion; the second captures the correct source guard after `main` advanced during the release. No passed immutable artifact was republished.

## v2026.08.01-pmo-1.3.8-cli-1.0.94-document-version-compat-release - 2026-08-01

Status: `PUBLISHED_AND_VERIFIED`. This npm-only release was published from merge commit `cbe184378344b3502058259c40a87fd41949b9c4` and tree `dc2fe69f27cb27b64d3164e0b1b069cf1f375ff6`. Both `@mango/pmo@1.3.8` and `@mango/cli@1.0.94` resolve from the configured publish and consume registries with matching integrity. The tag and GitHub Release are `CREATED_AND_VERIFIED` and point to that commit. The completed manifest has SHA-256 `d6276af0bfe1f73abd78abbc37db67f9658901062894f7aab8c97c0a04cf2db9` and records all applicable states as passed; Maven and the versioned Maven documentation snapshot were correctly not applicable. The durable release summary is `mango-docs/evidence/governance/release-v2026.08.01-pmo-1.3.8-cli-1.0.94-document-version-compat-release.json`.

### Fixed

- Preserve unchanged lifecycle documents created under PMO `1.3.6` or `1.3.7` when their current contracts remain schema-compatible, without allowing new documents to select an obsolete version.
- Make `mango pmo upgrade` create a path, SHA-256 and version baseline for eligible historical documents, so the documented upgrade path resolves the collection gate failure.

### Changed

- Require new or modified management list pages to use `MangoListPage`, `MangoSearchPanel`, `MangoListPanel` and `Pagination`; require independent detail and form pages to use their current Mango page shells, and standard dialogs to use `MangoDialog`.
- Add the incremental `check-frontend-page-baseline.mjs` checker to the PMO bundle and generated business-project baseline.
- Add the frontend page-baseline job to generated GitHub and Gitea `pmo-doc-check` workflows and include its result in the stable required-check aggregate.

### Versions

| Component    | Previous |  Release | Compatibility                                                                                                         |
| ------------ | -------: | -------: | --------------------------------------------------------------------------------------------------------------------- |
| `@mango/pmo` |  `1.3.7` |  `1.3.8` | Keeps schema revision `1`; contracts explicitly accept locked `1.3.6` and `1.3.7` historical documents.               |
| `@mango/cli` | `1.0.93` | `1.0.94` | Depends exactly on `@mango/pmo@1.3.8` and runs the controlled historical-document baseline step during `pmo upgrade`. |

### Published Packages

| Order | Target                     | Version                                                            | Status                   |
| ----: | -------------------------- | ------------------------------------------------------------------ | ------------------------ |
|     1 | `@mango/pmo`               | `1.3.8`                                                            | `PUBLISHED_AND_VERIFIED` |
|     2 | `@mango/cli`               | `1.0.94`                                                           | `PUBLISHED_AND_VERIFIED` |
|     3 | Git tag and GitHub Release | `v2026.08.01-pmo-1.3.8-cli-1.0.94-document-version-compat-release` | `CREATED_AND_VERIFIED`   |

### PMO Required Checks

- `check-frontend-page-baseline.mjs`
  - Migration: Before upgrading, inspect changed `views/**/*.vue` files and migrate management lists to the Mango list-page components, independent detail/form pages to their page shells, and standard dialogs to `MangoDialog`.
  - Exception: For a non-management table or a native component dependency that cannot use the standard shell, add a reviewable typed comment such as `<!-- mango-page-baseline-exception list: embedded comparison table is not a management list -->`; supported kinds are `list`, `detail`, `form` and `dialog`.
  - Verify: `node business-pmo/mango-baseline/tools/check-frontend-page-baseline.mjs --base <base-sha> --head <head-sha> --frontend-root <frontend-root>`.

### Upgrade Notes

1. Publish and install `@mango/pmo@1.3.8` before `@mango/cli@1.0.94`.
2. Before syncing the new PMO baseline, inspect open and planned frontend changes for the page patterns above; migrate affected pages or record a typed, reviewable exception reason.
3. In each existing business repository, run `mango pmo upgrade --project-dir . --to 1.3.8 --sync-shell`, review the planned or written `.mango-pmo-legacy-documents.json`, then run `mango pmo check --project-dir . --locked`.
4. Run the frontend page-baseline checker against the intended PR base/head before relying on the generated `pmo-doc-check` required check.
5. The generated historical-document baseline is accepted only for unchanged documents whose version is explicitly supported by the current contract. New or migrated lifecycle documents must use `1.3.8`.

### Verification

- `node --test mango-pmo/tests/document-contract/document-contract.test.mjs`
- `node --test mango-pmo/tests/frontend-page-baseline.test.mjs`
- `node mango-ui/packages/mango-pmo/scripts/build-package.mjs && node mango-ui/packages/mango-pmo/scripts/check-package.mjs`
- `node mango-ui/packages/mango-cli/scripts/check-cli.mjs`
- `node mango-business-starter/scripts/sync-pmo-baseline.mjs --check`

## v2026.08.01-maven-1.0.29-pmo-1.3.7-cli-1.0.93-platform-bootstrap-file-release - 2026-08-01

Status: `PUBLISHED_AND_VERIFIED`. This mixed release was published from source commit `840bd4de116de53d89de208146aef8de5d0cd2c9` and tree `8093ccc6d31ac10bc52cfb02cf570a0968619d4b`. The exact-source bundle SHA-256 is `d9e11790fcf1b66c771bc01f100d731660a7bd37c46b7bf9146da8b2d82ce11c`. All 188 Maven coordinates at `1.0.29` and all 24 npm packages below resolve from the configured publish and consume registries. The tag and GitHub Release are `CREATED_AND_VERIFIED` and point to the reviewed source commit; the versioned documentation snapshot is published at the same tag path. The completed read-only manifest at `.runtime/releases-final-verify/1.0.29/manifest.json` has SHA-256 `37c54aad85dc8ac2708892b7ade2e3c72ccbeb67a7a67e53fe82f41a0394f27a` and records all 17 release states as passed. The original and recovery manifests are preserved with SHA-256 values `c71d762d71b4bba6218b954525d791e608085b9b85fb3e63e389e42e0a9ee829`, `e863fd840cb7f7220fd0afec4f6e2df191f92b55f6c6ce652b541ed77136a774` and `c475dfe44e013d8fc768e3fd816dac5c42ac6e4fb5c5721c7c7f69c960cc7cde`; they retain the malformed initial tag adapter, Maven output-buffer/docs-bundle recovery, and the post-publication verifier that incorrectly required optional `home` and `site-shell` packages as direct full-preset dependencies. No passed immutable artifact was republished during recovery.

### Added

- Add structured Notice presentation across message details and realtime popups: the title is the user-facing message type, the body keeps only message type, content and time as `label: value`, and the footer keeps `关闭` plus one context-aware primary action.
- Add safe rich-text rendering for Notice content with an allowlist sanitizer, and remove scripts, event attributes, inline styles and unsafe link protocols before `v-html` or programmatic `innerHTML` rendering.
- Add exact unread Notice category statistics and paging filters for approval, system and business messages; the top bell keeps individual messages through 10 unread items and switches to non-empty category groups above 10.
- Add the public `MangoDialogExpose.bringToFront()` contract and automatic pointer-based dialog promotion across the header, content and footer.
- Add the optional `FilePreviewPanel.fitContainer` mode and make image preview use the embedded Element Plus viewer directly inside the preview stage.
- Add the Bootstrap lifecycle that separates initialization from Runtime with generation fencing, apply/finalize/abort phases, Flyway expand/finalize handling, Resource reconciliation and build-time cold baseline artifacts.
- Add File ZIP package size control with automatic proportional allocation or per-entry manual targets, preserving a single archive and returning achieved-size details when a target cannot be reached.

### Fixed

- Route Notice primary actions through the registered Shell target, preserve safe fallback navigation, and derive Workflow read-only targets from `customConfig.viewPath` when available.
- Keep Workflow completion, rejection and ended notifications navigable when a custom view path is missing or invalid by falling back to the corresponding generic task or application page.
- Keep managed File asset content and storage locations stable across bootstrap reentry, and fail closed when packaged resource content changes unexpectedly.

### Changed

- Advance the generated-project compatibility locks to Maven `1.0.29`, PMO `1.3.7`, CLI `1.0.93` and the exact frontend package matrix below.
- Isolate mutable Maven project outputs between worktrees by assigning each Mango workspace a stable revision qualifier while continuing to share immutable third-party dependencies.
- Tighten PMO engineering rules for rewrite semantics, current-rule precedence, Lombok boundaries and valid test ownership, and ship the synchronized business baseline in `@mango/pmo@1.3.7`.

### Versions

| Component                                   |                Previous |              Release | Compatibility                                                                                                                   |
| ------------------------------------------- | ----------------------: | -------------------: | ------------------------------------------------------------------------------------------------------------------------------- |
| Mango Maven non-app backend and docs bundle |                `1.0.28` |             `1.0.29` | Patch release adding Bootstrap lifecycle, cold baseline and File package controls; Parent and BOM consumers upgrade as one set. |
| `@mango/common`                             |                `1.0.22` |             `1.0.23` | Adds a typed dialog instance method; existing props and default behavior remain compatible.                                     |
| `@mango/file`                               |                `1.0.30` |             `1.0.31` | Adds opt-in container filling; embedded image viewing becomes the default preview interaction.                                  |
| `@mango/notice`                             |                `1.0.34` |             `1.0.35` | Adds structured/sanitized presentation, category grouping and unified navigation.                                               |
| `@mango/admin-shell`                        |                `1.0.54` |             `1.0.55` | Handles Notice category and primary-action navigation without changing menu contracts.                                          |
| `@mango/workflow`                           |                `1.0.36` |             `1.0.37` | Aligns the frontend dependency matrix with Workflow/Notice target metadata from Maven `1.0.29`.                                 |
| `@mango/pmo`                                |                 `1.3.6` |              `1.3.7` | Packages the current engineering rules, scope checks and synchronized generated-project baseline.                               |
| `@mango/cli`                                |                `1.0.92` |             `1.0.93` | Locks this batch and isolates each workspace's mutable Maven project revision.                                                  |
| Other affected frontend packages            | previous release matrix | exact versions below | Patch republish keeps fixed inter-package dependencies aligned.                                                                 |

### Published Packages

| Order | Target                                                                                                                                                                                                                                                                                                       | Version                                                                                                                                                        | Status                   |
| ----: | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------ |
|     1 | Maven non-app Reactor including `io.mango:*` and `io.mango:mango-docs-bundle`                                                                                                                                                                                                                                | `1.0.29`                                                                                                                                                       | `PUBLISHED_AND_VERIFIED` |
|     2 | `@mango/pmo`                                                                                                                                                                                                                                                                                                 | `1.3.7`                                                                                                                                                        | `PUBLISHED_AND_VERIFIED` |
|     3 | `@mango/common`, `@mango/file`, `@mango/notice`, `@mango/workflow`                                                                                                                                                                                                                                           | `1.0.23`, `1.0.31`, `1.0.35`, `1.0.37`                                                                                                                         | `PUBLISHED_AND_VERIFIED` |
|     4 | `@mango/admin-pages`, `@mango/auth`, `@mango/calendar`, `@mango/cms`, `@mango/grid-layout`, `@mango/grid-widgets`, `@mango/home`, `@mango/job`, `@mango/link`, `@mango/numgen`, `@mango/payment`, `@mango/rbac`, `@mango/site-shell`, `@mango/system`, `@mango/template`, `@mango/workflow-business-example` | `1.0.30`, `1.0.23`, `1.0.31`, `1.0.20`, `1.0.14`, `1.0.20`, `1.0.12`, `1.0.23`, `1.0.17`, `1.0.31`, `1.0.23`, `1.0.21`, `1.0.10`, `1.0.29`, `1.0.31`, `1.0.36` | `PUBLISHED_AND_VERIFIED` |
|     5 | `@mango/admin-shell`, `@mango/admin`                                                                                                                                                                                                                                                                         | `1.0.55`, `1.0.60`                                                                                                                                             | `PUBLISHED_AND_VERIFIED` |
|     6 | `@mango/cli`                                                                                                                                                                                                                                                                                                 | `1.0.93`                                                                                                                                                       | `PUBLISHED_AND_VERIFIED` |
|     7 | Git tag and GitHub Release                                                                                                                                                                                                                                                                                   | `v2026.08.01-maven-1.0.29-pmo-1.3.7-cli-1.0.93-platform-bootstrap-file-release`                                                                                | `CREATED_AND_VERIFIED`   |

### Upgrade Notes

1. Upgrade all Mango backend dependencies together. Parent consumers set `mango.version` to `1.0.29`; projects with another parent import `io.mango:mango-bom:1.0.29` and omit versions from BOM-managed Mango dependencies.
2. Upgrade the frontend packages to the exact versions in this release. Aggregate consumers use `@mango/admin@1.0.60`; direct consumers align their imported packages explicitly.
3. Publish and upgrade `@mango/pmo@1.3.7` before installing `@mango/cli@1.0.93`, then run `mango pmo upgrade --project-dir . --to 1.3.7 --sync-shell` and verify the locked baseline.
4. Existing databases upgrade in place. New installations and controlled upgrades use the Bootstrap plan/apply/finalize lifecycle; review module order, cold baseline availability, Resource modes and finalize/abort procedures before enabling it in an existing deployment.
5. File package size targets are best-effort results rather than implicit errors. Callers that adopt `packageFilesWithSizeControl` must inspect `packageTargetAchieved`, actual archive bytes and per-entry results; existing `packageFiles` callers are unchanged.
6. Notice categories are derived from existing business-type groups and require no schema migration. Verify the intended `bizGroup`, registered target route and Workflow `customConfig.viewPath` for business notifications.
7. `FilePreviewPanel.fitContainer` is opt-in, but image preview now opens as an embedded viewer by default; verify any custom viewer CSS after upgrading Element Plus. Existing `MangoDialog` callers are unchanged unless they use the new `bringToFront()` instance method.
8. Run `mango workspace init` in every existing worktree before the next local Maven install or backend start. The CLI preserves third-party cache reuse but assigns each workspace a distinct project revision; generated backends that still use fixed project versions must be upgraded to the CI-friendly `${revision}` template first.

### Verification

- Release preparation PR #665 passed `frontend-pr-quality`, `pmo-doc-check` and `pr-contract-check` on the exact source tree before publication.
- The complete 216-module Maven Reactor, Architecture Verification, static analysis with zero new issues, `pnpm -C mango-ui check:full`, package exports, release-note checks and registry doctor passed before publication.
- The completed read-only manifest verifies all 188 Maven coordinates and all 24 npm coordinates from both publish and consume repositories, plus the CLI release lock, tag and GitHub Release.
- A clean consumer installed `@mango/cli@1.0.93` from the consume registry, generated the Maven `1.0.29` / PMO `1.3.7` / frontend compatibility matrix, passed `mango pmo check --locked`, installed frontend dependencies from Nexus, and passed typecheck and production build.
- Documentation snapshot PR #666 passed all required checks; GitHub Pages deployed the tag-addressed snapshot and both Latest and versioned URLs return HTTP 200.
- The canonical final verification manifest at `.runtime/releases-final-verify/1.0.29/manifest.json` records all 17 release states as passed with no pending or failed required state.

## v2026.07.29-maven-1.0.28-cli-1.0.92-dialog-workflow-job-release - 2026-07-29

Status: `PUBLISHED_AND_VERIFIED`. This mixed release was published from source commit `7bfa481a4f769b2b348ed1786656d02961933708` and tree `e91b789edb66e3b246d85edf49419292db32ea16`. The exact-source bundle SHA-256 is `96f18067dfa63f84b254955d986a96c3859a2ef4a21f1beca2cd0ab127e0d562`. Maven `1.0.28`, all 23 npm packages and CLI `1.0.92` resolve from the configured publish and consume registries; `@mango/pmo` remains `1.3.6` and was not republished. The tag and GitHub Release are `CREATED_AND_VERIFIED` and point to the same source commit. The completed read-only recovery manifest at `.runtime/release-audit/1.0.28/read-only-v1/1.0.28/manifest.json` has SHA-256 `a426a00afb53f903cab7d70a7b2d88b20c0f3dd45b48b311c555878c2f62b035` and records all 17 release states as passed. The original publication manifest at `.runtime/releases/1.0.28/manifest.json` is preserved with SHA-256 `77db4972477244806987a239631b83f2ff635727aa26b20708d427f03a4616e5`; its only failure was a post-publication verifier that incorrectly required optional `home` and `site-shell` packages as direct full-preset dependencies, and no immutable artifact was republished during recovery.

### Added

- Add optional drag, eight-direction resize, maximize/restore, viewport constraints and resize-time boundary convergence to the public `MangoDialog` component while preserving its default behavior.

### Fixed

- Return the deployed runtime Workflow designer snapshot from process and task detail APIs so historical instances render the version that actually ran without requiring definition-management permission.
- Reduce native Job worker heartbeat log noise without changing registration, dispatch or execution behavior.

### Changed

- Advance the generated-project compatibility locks to Maven `1.0.28`, CLI `1.0.92` and the exact frontend package matrix below; `@mango/pmo` remains `1.3.6` and is not republished.

### Versions

| Component                                   |                Previous |              Release | Compatibility                                                                |
| ------------------------------------------- | ----------------------: | -------------------: | ---------------------------------------------------------------------------- |
| Mango Maven non-app backend and docs bundle |                `1.0.27` |             `1.0.28` | Patch release; Parent and BOM consumers upgrade as one set.                  |
| `@mango/common`                             |                `1.0.21` |             `1.0.22` | Adds opt-in window interactions; existing dialog defaults remain compatible. |
| `@mango/workflow`                           |                `1.0.35` |             `1.0.36` | Uses the runtime designer snapshot returned by Maven `1.0.28`.               |
| `@mango/cli`                                |                `1.0.91` |             `1.0.92` | Locks Maven `1.0.28` and all affected frontend packages in this batch.       |
| `@mango/pmo`                                |                 `1.3.6` |            unchanged | No PMO source changed; the package is not republished.                       |
| Other affected frontend packages            | previous release matrix | exact versions below | Patch republish keeps fixed inter-package dependencies aligned.              |

### Published Packages

| Order | Target                                                                                                                                                                                                                                                                                                                                       | Version                                                                                                                                                                           |
| ----: | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
|     1 | Maven non-app Reactor including `io.mango:*` and `io.mango:mango-docs-bundle`                                                                                                                                                                                                                                                                | `1.0.28`                                                                                                                                                                          |
|     2 | `@mango/common`, `@mango/workflow`                                                                                                                                                                                                                                                                                                           | `1.0.22`, `1.0.36`                                                                                                                                                                |
|     3 | `@mango/admin-pages`, `@mango/auth`, `@mango/calendar`, `@mango/cms`, `@mango/file`, `@mango/grid-layout`, `@mango/grid-widgets`, `@mango/home`, `@mango/job`, `@mango/link`, `@mango/notice`, `@mango/numgen`, `@mango/payment`, `@mango/rbac`, `@mango/site-shell`, `@mango/system`, `@mango/template`, `@mango/workflow-business-example` | `1.0.29`, `1.0.22`, `1.0.30`, `1.0.19`, `1.0.30`, `1.0.13`, `1.0.19`, `1.0.11`, `1.0.22`, `1.0.16`, `1.0.34`, `1.0.30`, `1.0.22`, `1.0.20`, `1.0.9`, `1.0.28`, `1.0.30`, `1.0.35` |
|     4 | `@mango/admin-shell`, `@mango/admin`                                                                                                                                                                                                                                                                                                         | `1.0.54`, `1.0.59`                                                                                                                                                                |
|     5 | `@mango/cli`                                                                                                                                                                                                                                                                                                                                 | `1.0.92`                                                                                                                                                                          |
|     6 | Git tag and GitHub Release                                                                                                                                                                                                                                                                                                                   | `v2026.07.29-maven-1.0.28-cli-1.0.92-dialog-workflow-job-release`                                                                                                                 |

### Upgrade Notes

1. Upgrade all Mango backend dependencies together. Parent consumers set `mango.version` to `1.0.28`; projects with another parent import `io.mango:mango-bom:1.0.28` and omit versions from BOM-managed Mango dependencies.
2. Upgrade the frontend packages to the exact versions in this release. Aggregate consumers use `@mango/admin@1.0.59`; direct consumers align their imported packages explicitly.
3. Install `@mango/cli@1.0.92`. Keep `@mango/pmo@1.3.6`; no PMO baseline migration is required.
4. Existing databases upgrade in place. Verify a historical Workflow instance renders its deployed diagram, and enable `MangoDialog` window interactions only where the host wants draggable, resizable or maximizable dialogs.

### Verification

- `pnpm -C mango-ui release:impact --base=v2026.07.26-maven-1.0.27-pmo-1.3.6-cli-1.0.91-file-notice-release --head=HEAD`
- `node mango-ui/packages/mango-cli/scripts/check-release-versions.mjs`
- `MANGO_BACKEND_GATE_VERSION=1.0.28 node mango-ui/packages/mango-cli/scripts/check-generated-backend-gate.mjs`
- `pnpm -C mango-ui admin:styles:check && pnpm -C mango-ui admin:module-styles:check`
- The MangoDialog component suite, Workflow API/integration suites, Job quality gate, full Maven Reactor and affected frontend builds passed before publication.
- Release preparation PR #653 passed all required checks on the exact source tree; live `main` branch protection matched the checked-in `single-owner` policy before immutable publication.
- The Maven non-app Reactor and docs bundle passed publish- and consume-repository HTTP verification. All 23 npm packages passed hosted and group registry tarball verification; `@mango/cli@1.0.92` was published at `2026-07-29T03:19:00.483Z` with integrity `sha512-gHRSAq7AL0aOjMoWWOlM545UhuSqjSNjk/0WZXj4htW1Pts/ThPYUPDXg7tMRTZuJHQsglwzxxEWJ1KQbt07FA==`.
- A clean consumer installed `@mango/cli@1.0.92` from the consume registry, generated a full monolith project, verified Maven `1.0.28`, PMO `1.3.6`, the complete frontend release lock and generated direct dependencies, passed locked PMO checks, installed dependencies, typechecked and completed a production build.
- The completed read-only recovery manifest verified all 17 states, including both registry sides, tag, GitHub Release, Latest docs and the versioned documentation snapshot without republishing immutable artifacts.

## v2026.07.26-maven-1.0.27-pmo-1.3.6-cli-1.0.91-file-notice-release - 2026-07-26

Status: `PUBLISHED_AND_VERIFIED`. This mixed release was published from source commit `34835c1a8091950689f766f4e8c4f4d786bca7e0` and tree `2457334c8e093261c68479882cdeab051bf25c23`. The exact-source bundle SHA-256 is `9c9d9876fd2343642f93713a8323bdbaae9395ff6a043aa5b2fc396b1982bbf5`. Maven `1.0.27`, PMO `1.3.6`, the affected frontend matrix and CLI `1.0.91` resolve from the configured publish and consume registries. The tag and GitHub Release are `CREATED_AND_VERIFIED` and point to the same source commit. The completed release manifest at `.runtime/releases/1.0.27/manifest.json` has SHA-256 `9c230734ce553e163537846bf36b1eb17f629e3293f9af36cdf6963702bd6b11` and records all 17 release states as passed.

### Added

- Add File HTTP multipart upload support with the corresponding backend and reusable frontend integration paths.
- Add Notice multi-mailbox delivery configuration, route selection, managed credentials and real attachment delivery through the configured mailbox.
- Add `Require.nonNull` as the canonical null-validation API so business validation remains consistent while SpotBugs receives an explicit non-null return value.

### Fixed

- Resolve Issues 641 and 642 by keeping mailbox selection, encrypted client authorization credentials, attachment resolution and delivery records aligned across Notice configuration and dispatch.
- Eliminate the `NP_NULL_PARAM_DEREF` false-positive pattern without disabling SpotBugs or replacing required `Require` validation with tool-driven control flow.

### Changed

- Document the `Require.nonNull` convention in PMO and project templates, and advance the generated-project compatibility locks to Maven `1.0.27`, PMO `1.3.6`, CLI `1.0.91` and the exact frontend package matrix below.

### Versions

| Component                                   |                Previous |        Release | Compatibility                                                                  |
| ------------------------------------------- | ----------------------: | -------------: | ------------------------------------------------------------------------------ |
| Mango Maven non-app backend and docs bundle |                `1.0.26` |       `1.0.27` | Patch release; Parent and BOM consumers upgrade as one set.                    |
| `@mango/pmo`                                |                 `1.3.5` |        `1.3.6` | Adds the current `Require`/SpotBugs governance and generated-project baseline. |
| `@mango/cli`                                |                `1.0.90` |       `1.0.91` | Locks Maven, PMO and all affected frontend packages in this batch.             |
| Frontend runtime packages                   | previous release matrix | versions below | Patch updates; public package identities and entry points remain compatible.   |

### Published Packages

| Order | Target                                                                                                                                                                                                                                           | Version                                                                                                                          | Release status           |
| ----: | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------- | ------------------------ |
|     1 | Maven non-app Reactor including `io.mango:*` and `io.mango:mango-docs-bundle`                                                                                                                                                                    | `1.0.27`                                                                                                                         | `PUBLISHED_AND_VERIFIED` |
|     2 | `@mango/pmo`                                                                                                                                                                                                                                     | `1.3.6`                                                                                                                          | `PUBLISHED_AND_VERIFIED` |
|     3 | `@mango/admin-pages`, `@mango/file`, `@mango/system`, `@mango/notice`, `@mango/calendar`, `@mango/cms`, `@mango/job`, `@mango/link`, `@mango/numgen`, `@mango/payment`, `@mango/template`, `@mango/workflow`, `@mango/workflow-business-example` | `1.0.28`, `1.0.29`, `1.0.27`, `1.0.33`, `1.0.29`, `1.0.18`, `1.0.21`, `1.0.15`, `1.0.29`, `1.0.21`, `1.0.29`, `1.0.35`, `1.0.34` | `PUBLISHED_AND_VERIFIED` |
|     4 | `@mango/admin-shell`, `@mango/admin`                                                                                                                                                                                                             | `1.0.53`, `1.0.58`                                                                                                               | `PUBLISHED_AND_VERIFIED` |
|     5 | `@mango/cli`                                                                                                                                                                                                                                     | `1.0.91`                                                                                                                         | `PUBLISHED_AND_VERIFIED` |
|     6 | Git tag and GitHub Release                                                                                                                                                                                                                       | `v2026.07.26-maven-1.0.27-pmo-1.3.6-cli-1.0.91-file-notice-release`                                                              | `CREATED_AND_VERIFIED`   |

### Upgrade Notes

1. Upgrade all Mango backend dependencies together. Parent consumers set `mango.version` to `1.0.27`; projects with another parent import `io.mango:mango-bom:1.0.27` and omit versions from BOM-managed Mango dependencies.
2. Upgrade the frontend packages to the exact versions in this release. Aggregate consumers use `@mango/admin@1.0.58`; direct consumers align their imported packages explicitly.
3. Install `@mango/cli@1.0.91`, then run `mango pmo upgrade --project-dir . --to 1.3.6 --sync-shell` and `mango pmo check --project-dir . --locked`.
4. Existing databases upgrade in place. Configure each Notice mailbox with its own client authorization credential, verify route selection, and send a real attachment through the intended mailbox before production use.

### Verification

- `pnpm -C mango-ui release:impact --base=v2026.07.23-maven-1.0.26-pmo-1.3.5-cli-1.0.90-platform-runtime-release --head=HEAD`
- `pnpm -C mango-ui --filter @mango/pmo build && pnpm -C mango-ui --filter @mango/pmo check`
- `node mango-business-starter/scripts/sync-pmo-baseline.mjs --check`
- `node mango-ui/packages/mango-cli/scripts/check-release-versions.mjs`
- `MANGO_BACKEND_GATE_VERSION=1.0.27 node mango-ui/packages/mango-cli/scripts/check-generated-backend-gate.mjs`
- `pnpm -C mango-ui admin:styles:check && pnpm -C mango-ui admin:module-styles:check`
- The File and Notice feature suites, full Maven Reactor quality gates, clean package consumers and the real attachment-mail path passed before closeout.
- Release preparation PR #646 passed all required checks on the exact source tree before immutable publication.
- The completed release state machine records Nexus publish/consume back-checks, clean published consumers, tag, GitHub Release and documentation evidence; an independent `release verify` run also passed all 17 states.

## v2026.07.23-maven-1.0.26-pmo-1.3.5-cli-1.0.90-platform-runtime-release - 2026-07-23

Status: `PUBLISHED_AND_VERIFIED`. This mixed release was published from source commit `e269b726a34440170555010c72e80f0c0c66e524` and tree `54225448d36961a15a473eee2d7f619ed51b9425`. The exact-source bundle SHA-256 is `e811a68934c41ea9594aef0c97ed7ae1d9c36d14e3d162d9731b97ef1f577af5`. Maven `1.0.26`, PMO `1.3.5`, the frontend runtime matrix and CLI `1.0.90` resolve from the configured publish and consume registries. The tag and GitHub Release point to the same source commit. The completed read-only recovery manifest at `.runtime/release-audit/1.0.26/read-only-v3/1.0.26/manifest.json` has SHA-256 `4bbdbc835ffd54264449d116cbd1d4b94de495697f1e331dcfc04d31307d6600` and records all 17 release states as passed; the original publication manifest is preserved with its post-publication harness failure and was not used to republish immutable artifacts.

### Added

- Add fail-closed module runtime diagnostics and controlled onboarding for legacy modules, with explicit architecture-debt and service-registration checks.
- Add managed rich-text asset upload, cleanup and rendering support across reusable frontend packages and the platform runtime.

### Fixed

- Initialize Workflow definition administrator data scope so clean databases expose the definition list to authorized administrators.
- Harden startup resource synchronization and preserve dependency-only Reactor jars required by published Maven consumers.
- Route Notice message cards to the canonical destination, display member nicknames consistently, and handle asynchronous web lifecycle failures without leaking framework warnings into business handling.
- Expose workspace database status in the CLI and move AI notifications to the realtime delivery API.

### Changed

- Rename platform capability management surfaces consistently without changing route identities or permission semantics.
- Require the frontend PR quality check and build affected frontend packages before consumer type checking.
- Advance the generated-project compatibility locks to Maven `1.0.26`, PMO `1.3.5`, CLI `1.0.90` and the exact frontend package matrix below.

### Versions

| Component                                   |                Previous |        Release | Compatibility                                                                |
| ------------------------------------------- | ----------------------: | -------------: | ---------------------------------------------------------------------------- |
| Mango Maven non-app backend and docs bundle |                `1.0.25` |       `1.0.26` | Patch release; Parent and BOM consumers upgrade as one set.                  |
| `@mango/pmo`                                |                 `1.3.4` |        `1.3.5` | Adds the current governance, onboarding and release baseline.                |
| `@mango/cli`                                |                `1.0.89` |       `1.0.90` | Locks Maven, PMO and all frontend packages in this batch.                    |
| Frontend runtime packages                   | previous release matrix | versions below | Patch updates; public package identities and entry points remain compatible. |

### Published Packages

| Order | Target                                                                                                                                                                                                                                                                                                                                                                           | Version                                                                                                                                                                                               | Release status           |
| ----: | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------ |
|     1 | Maven non-app Reactor including `io.mango:*` and `io.mango:mango-docs-bundle`                                                                                                                                                                                                                                                                                                    | `1.0.26`                                                                                                                                                                                              | `PUBLISHED_AND_VERIFIED` |
|     2 | `@mango/pmo`                                                                                                                                                                                                                                                                                                                                                                     | `1.3.5`                                                                                                                                                                                               | `PUBLISHED_AND_VERIFIED` |
|     3 | `@mango/admin-pages`, `@mango/auth`, `@mango/calendar`, `@mango/cms`, `@mango/common`, `@mango/file`, `@mango/grid-layout`, `@mango/grid-widgets`, `@mango/home`, `@mango/job`, `@mango/link`, `@mango/notice`, `@mango/numgen`, `@mango/payment`, `@mango/rbac`, `@mango/site-shell`, `@mango/system`, `@mango/template`, `@mango/workflow`, `@mango/workflow-business-example` | `1.0.27`, `1.0.21`, `1.0.28`, `1.0.17`, `1.0.21`, `1.0.28`, `1.0.12`, `1.0.18`, `1.0.10`, `1.0.20`, `1.0.14`, `1.0.32`, `1.0.28`, `1.0.20`, `1.0.19`, `1.0.8`, `1.0.26`, `1.0.28`, `1.0.34`, `1.0.33` | `PUBLISHED_AND_VERIFIED` |
|     4 | `@mango/admin-shell`, `@mango/admin`                                                                                                                                                                                                                                                                                                                                             | `1.0.52`, `1.0.57`                                                                                                                                                                                    | `PUBLISHED_AND_VERIFIED` |
|     5 | `@mango/cli`                                                                                                                                                                                                                                                                                                                                                                     | `1.0.90`                                                                                                                                                                                              | `PUBLISHED_AND_VERIFIED` |
|     6 | Git tag and GitHub Release                                                                                                                                                                                                                                                                                                                                                       | `v2026.07.23-maven-1.0.26-pmo-1.3.5-cli-1.0.90-platform-runtime-release`                                                                                                                              | `CREATED_AND_VERIFIED`   |

### Upgrade Notes

1. Upgrade all Mango backend dependencies together. Parent consumers set `mango.version` to `1.0.26`; projects with another parent import `io.mango:mango-bom:1.0.26` and omit versions from BOM-managed Mango dependencies.
2. Upgrade the frontend packages to the exact versions in this release. Aggregate consumers use `@mango/admin@1.0.57`; direct consumers align their imported packages explicitly.
3. Install `@mango/cli@1.0.90`, then run `mango pmo upgrade --project-dir . --to 1.3.5 --sync-shell` and `mango pmo check --project-dir . --locked`.
4. Existing databases upgrade in place. After startup resource reconciliation, verify Workflow definition visibility, renamed platform management labels, message-card navigation and managed rich-text assets with the intended tenant and administrator account.

### Verification

- `pnpm -C mango-ui release:impact --base=v2026.07.21-maven-1.0.25-cli-1.0.89-branding-workflow-bom-release --head=HEAD`
- `pnpm -C mango-ui --filter @mango/pmo build && pnpm -C mango-ui --filter @mango/pmo check`
- `node mango-business-starter/scripts/sync-pmo-baseline.mjs --check`
- `node mango-ui/packages/mango-cli/scripts/check-release-versions.mjs`
- `MANGO_BACKEND_GATE_VERSION=1.0.26 node mango-ui/packages/mango-cli/scripts/check-generated-backend-gate.mjs`
- `pnpm -C mango-ui admin:styles:check && pnpm -C mango-ui admin:module-styles:check`
- The release preparation PR must pass all required checks on the exact source tree before immutable publication.
- The release state machine will record Nexus publish/consume back-checks, clean published consumers, tag, GitHub Release, documentation and closeout evidence.

## v2026.07.21-maven-1.0.25-cli-1.0.89-branding-workflow-bom-release - 2026-07-21

Status: `PUBLISHED_AND_VERIFIED`. This mixed release was published from source commit `d77f5c1961edce33c8bf049105494b5f92fe6b9c` and tree `311affe083f05e9121bfc67209dbfbdeb37e79d1`. Maven `1.0.25`, the frontend patch matrix and `@mango/cli@1.0.89` resolve from their configured publish and consume registries; `@mango/pmo` remains `1.3.4` and was not republished. The immutable tag and GitHub Release point to the same source commit; the versioned documentation snapshot is included in this closeout and Pages verification remains recorded by the canonical release manifest.

### Added

- Add the independently importable `io.mango:mango-bom`, covering all published non-app Mango JARs and the verified third-party dependency versions used by the Reactor.

### Changed

- Make `mango-parent` import only the same-version `mango-bom` for dependency management while retaining Java and Maven plugin defaults in the parent.
- Extend Admin branding with separate expanded and collapsed logo assets and keep the runtime branding state consistent across the navigation shell.
- Advance the CLI compatibility lock to Maven `1.0.25` and the exact frontend patch matrix in this batch; CLI commands and PMO `1.3.4` remain compatible.

### Fixed

- Send Workflow completion notices to the original applicant, expand shared candidate recipients consistently, and preserve tenant, application and realm context through local and remote Notice delivery.
- Keep assigned parallel and multi-instance Workflow tasks mapped to their actual runtime assignees and skip notice dispatch when no valid recipient exists.

### Versions

| Component                                   |             Previous |              Release | Compatibility                                                                                                    |
| ------------------------------------------- | -------------------: | -------------------: | ---------------------------------------------------------------------------------------------------------------- |
| Mango Maven non-app backend and docs bundle |             `1.0.24` |             `1.0.25` | Adds the BOM consumption option; Parent consumers keep the same inheritance model and update one shared version. |
| `@mango/admin`                              |             `1.0.53` |             `1.0.55` | Branding aggregate update; existing public entry points remain compatible.                                       |
| `@mango/admin-shell`                        |             `1.0.48` |             `1.0.50` | Adds expanded/collapsed branding asset support without changing route contracts.                                 |
| `@mango/admin-pages`                        |             `1.0.25` |             `1.0.26` | Exact dependency-matrix alignment.                                                                               |
| `@mango/auth`                               |             `1.0.19` |             `1.0.20` | Aligns login branding behavior and exact dependencies.                                                           |
| `@mango/notice`                             |             `1.0.29` |             `1.0.30` | Aligns the Workflow/Notice patch matrix; public Notice APIs remain compatible.                                   |
| `@mango/system`                             |             `1.0.24` |             `1.0.25` | Adds collapsed-logo branding fields while preserving existing branding settings.                                 |
| `@mango/workflow`                           |             `1.0.32` |             `1.0.33` | Workflow recipient and context fixes; routes and approval APIs remain compatible.                                |
| Other republished frontend packages         | prior `1.0.x` matrix | exact versions below | Patch republish keeps exact inter-package dependency locks consistent.                                           |
| `@mango/cli`                                |             `1.0.88` |             `1.0.89` | Locks Maven `1.0.25` and this frontend matrix; command behavior remains compatible.                              |
| `@mango/pmo`                                |              `1.3.4` |            unchanged | Not republished.                                                                                                 |

### Published Packages

| Order | Target                                | Version                                                             | Release status           |
| ----: | ------------------------------------- | ------------------------------------------------------------------- | ------------------------ |
|     1 | Maven non-app backend and docs bundle | `io.mango:*:1.0.25`                                                 | `PUBLISHED_AND_VERIFIED` |
|     2 | npm Admin Pages                       | `@mango/admin-pages@1.0.26`                                         | `PUBLISHED_AND_VERIFIED` |
|     3 | npm Auth                              | `@mango/auth@1.0.20`                                                | `PUBLISHED_AND_VERIFIED` |
|     4 | npm Calendar                          | `@mango/calendar@1.0.27`                                            | `PUBLISHED_AND_VERIFIED` |
|     5 | npm CMS                               | `@mango/cms@1.0.16`                                                 | `PUBLISHED_AND_VERIFIED` |
|     6 | npm File                              | `@mango/file@1.0.27`                                                | `PUBLISHED_AND_VERIFIED` |
|     7 | npm Job                               | `@mango/job@1.0.19`                                                 | `PUBLISHED_AND_VERIFIED` |
|     8 | npm Link                              | `@mango/link@1.0.13`                                                | `PUBLISHED_AND_VERIFIED` |
|     9 | npm Notice                            | `@mango/notice@1.0.30`                                              | `PUBLISHED_AND_VERIFIED` |
|    10 | npm Numgen                            | `@mango/numgen@1.0.27`                                              | `PUBLISHED_AND_VERIFIED` |
|    11 | npm Payment                           | `@mango/payment@1.0.19`                                             | `PUBLISHED_AND_VERIFIED` |
|    12 | npm System                            | `@mango/system@1.0.25`                                              | `PUBLISHED_AND_VERIFIED` |
|    13 | npm Template                          | `@mango/template@1.0.27`                                            | `PUBLISHED_AND_VERIFIED` |
|    14 | npm Workflow                          | `@mango/workflow@1.0.33`                                            | `PUBLISHED_AND_VERIFIED` |
|    15 | npm Workflow Business Example         | `@mango/workflow-business-example@1.0.32`                           | `PUBLISHED_AND_VERIFIED` |
|    16 | npm Admin Shell                       | `@mango/admin-shell@1.0.50`                                         | `PUBLISHED_AND_VERIFIED` |
|    17 | npm Admin aggregate                   | `@mango/admin@1.0.55`                                               | `PUBLISHED_AND_VERIFIED` |
|    18 | npm CLI                               | `@mango/cli@1.0.89`                                                 | `PUBLISHED_AND_VERIFIED` |
|    19 | GitHub Release                        | `v2026.07.21-maven-1.0.25-cli-1.0.89-branding-workflow-bom-release` | `CREATED_AND_VERIFIED`   |

### Upgrade Notes

1. Upgrade all backend Mango dependencies to `1.0.25` together. Projects inheriting `mango-parent` update their shared `mango.version`; projects retaining another parent import `io.mango:mango-bom:1.0.25` and omit versions from BOM-managed dependencies.
2. Do not import another Spring Boot or Spring Cloud BOM and then silently override individual Mango-tested versions. Any intentional override requires a dependency-tree review and startup regression.
3. Upgrade the 16 frontend runtime packages to the exact versions above as one matrix. Aggregate consumers can install `@mango/admin@1.0.55`; direct consumers must keep exact package versions aligned.
4. Install `@mango/cli@1.0.89` only after Maven `1.0.25` and the frontend matrix resolve from the consume registries. Existing generated projects are not automatically converted between Parent inheritance and direct BOM import.
5. Existing databases upgrade in place. The Workflow and Notice changes preserve routes, permissions and stored preferences; verify process-completion and shared-candidate notifications in the target tenant.

### Verification

- Local BOM coverage tests, the 212-module Maven Reactor, exact-version local installation and an independent Spring Boot consumer passed before release preparation.
- Before publication, rerun the full Maven Reactor from the merged release source and run CLI tests, generated backend gates, release impact, package exports, Admin style checks, package builds, capability documentation audits and release-notes checks.
- The canonical `mango release` state machine must record the clean merged source, required PR checks, registry doctor, target-version absence, Maven/npm publication, publish/consume back-checks, docs snapshot, tag, GitHub Release and clean consumer evidence before this section is closed out.

## v2026.07.21-maven-1.0.24-pmo-1.3.4-cli-1.0.88-platform-notice-fixes-release - 2026-07-21

Status: `PUBLISHED_AND_VERIFIED`. This mixed Mango release was published from source commit `6bef98bab0bbe5ae50fe7d1ac35e858cac24a8af` and tree `905150b5de2db658cb339a01d3fdea150d97e968`. The exact-source bundle SHA-256 is `e8557ef5c2b645e9a85d60c4afe0cdcd26f088e8e07129e1a4fd89a3d91ebb23`. The completed read-only recovery manifest at `.runtime/releases/1.0.24/manifest.json` has SHA-256 `fe1cb55be3af6c0a2c16318fdf3e0a989984329dfb2314a4507fd6665644e776` and records all 17 release states as passed.

### Fixed

- Align Flyway 11.20.3 across Mango parent POMs and generated projects, remove MySQL integer display widths and UTF8MB3 defaults from fresh migrations, and exclude explicitly managed third-party schemas from Mango schema ownership checks.
- Move Notice receive settings into the user message center, keep personal Notice resources on the authenticated-user grant path, and remove the duplicate administration-menu placement.
- Preserve the current tenant when System operation logs are written so tenant-scoped audit queries return the expected records.
- Stabilize Workflow notice task routing and the matching menu/resource contracts while preserving existing runtime route identities.
- Add the canonical business pull-request template to the PMO delivery-assurance contract and fail closed on missing, outdated or duplicated managed Risk / Verification blocks.

### Changed

- `mango pmo init/sync/upgrade/check/rollback` now manages only the canonical PR template block and preserves project-owned PR description content.
- The CLI generated-project lock advances to the Maven `1.0.24`, PMO `1.3.4`, Notice `1.0.29`, Admin Shell `1.0.48` and Admin `1.0.53` compatibility matrix.

### Versions

| Component                                   |       Previous |   Release | Compatibility                                                                                                     |
| ------------------------------------------- | -------------: | --------: | ----------------------------------------------------------------------------------------------------------------- |
| Mango Maven non-app backend and docs bundle |       `1.0.23` |  `1.0.24` | Patch fixes; existing databases upgrade in place and fresh MySQL 8.4 schemas avoid the corrected warnings.        |
| `@mango/pmo`                                |        `1.3.3` |   `1.3.4` | Adds schema revision 5 and canonical PR template governance; existing project-owned template content is retained. |
| `@mango/notice`                             |       `1.0.28` |  `1.0.29` | Moves receive settings into the message center; public Notice APIs remain compatible.                             |
| `@mango/admin-shell`                        |       `1.0.47` |  `1.0.48` | Aligns the Notice dependency and verified route contracts; Shell public APIs remain compatible.                   |
| `@mango/admin`                              |       `1.0.52` |  `1.0.53` | Aggregate dependency alignment only; entries, exports and styles remain compatible.                               |
| `@mango/cli`                                |       `1.0.87` |  `1.0.88` | Adds Flyway alignment and PMO PR-template lifecycle support while preserving command compatibility.               |
| Other frontend runtime packages             | current matrix | unchanged | No other npm coordinate is republished in this batch.                                                             |

### Published Packages

| Order | Target                                | Version                                                                       | Release status           |
| ----: | ------------------------------------- | ----------------------------------------------------------------------------- | ------------------------ |
|     1 | Maven non-app backend and docs bundle | `io.mango:*:1.0.24`                                                           | `PUBLISHED_AND_VERIFIED` |
|     2 | npm PMO bundle and Skills             | `@mango/pmo@1.3.4`                                                            | `PUBLISHED_AND_VERIFIED` |
|     3 | npm Notice                            | `@mango/notice@1.0.29`                                                        | `PUBLISHED_AND_VERIFIED` |
|     4 | npm Admin Shell                       | `@mango/admin-shell@1.0.48`                                                   | `PUBLISHED_AND_VERIFIED` |
|     5 | npm Admin aggregate                   | `@mango/admin@1.0.53`                                                         | `PUBLISHED_AND_VERIFIED` |
|     6 | npm CLI                               | `@mango/cli@1.0.88`                                                           | `PUBLISHED_AND_VERIFIED` |
|     7 | GitHub Release                        | `v2026.07.21-maven-1.0.24-pmo-1.3.4-cli-1.0.88-platform-notice-fixes-release` | `CREATED_AND_VERIFIED`   |

### Upgrade Notes

1. Upgrade Mango backend dependencies as one Maven `1.0.24` set. Do not mix platform modules from `1.0.23` and `1.0.24`.
2. Existing databases do not require a reset. Workflow migration compatibility remains fail closed for unknown historical checksums, while fresh MySQL 8.4 databases use the corrected charset and type definitions.
3. Publish and verify `@mango/pmo@1.3.4` before installing `@mango/cli@1.0.88`. Business repositories should run `mango pmo upgrade --project-dir . --to 1.3.4 --sync-shell`, review the managed PR template block, and retain their project-owned text outside that block.
4. Frontend consumers using the aggregate admin package should upgrade to `@mango/admin@1.0.53`. Direct consumers should align `@mango/notice@1.0.29` and `@mango/admin-shell@1.0.48` together.
5. After backend resource reconciliation, Notice receive settings appear under the user message center rather than the administration menu. Existing Notice APIs, tenant data and user preferences remain in place.

### Verification

- `pnpm -C mango-ui release:impact --base=v2026.07.20-frontend-ui-fixes-npm-release --head=HEAD`
- `pnpm -C mango-ui --filter @mango/pmo build && pnpm -C mango-ui --filter @mango/pmo check`
- `node mango-business-starter/scripts/sync-pmo-baseline.mjs --check`
- `node mango-ui/packages/mango-cli/scripts/check-release-versions.mjs`
- `MANGO_BACKEND_GATE_VERSION=1.0.24 node mango-ui/packages/mango-cli/scripts/check-generated-backend-gate.mjs`
- `pnpm -C mango-ui admin:styles:check && pnpm -C mango-ui admin:module-styles:check`
- PR #609 and recovery PR #610 passed required PMO, frontend quality and P0 E2E checks before immutable publication.
- Maven `io.mango:*:1.0.24` and all five npm coordinates resolve from both Nexus publish and consume repositories with exact versions and package integrity evidence.
- A clean published `@mango/cli@1.0.88` consumer generated the Maven `1.0.24` / PMO `1.3.4` / Notice `1.0.29` / Admin Shell `1.0.48` / Admin `1.0.53` matrix, passed `mango pmo check --locked`, the nine-invocation generated backend gate, frontend typecheck and production build.
- The immutable tag points to source commit `6bef98bab0bbe5ae50fe7d1ac35e858cac24a8af`; the matching [GitHub Release](https://github.com/HardyDou/mango/releases/tag/v2026.07.21-maven-1.0.24-pmo-1.3.4-cli-1.0.88-platform-notice-fixes-release), documentation Latest and version snapshot were verified.
- Earlier failed manifests were retained as audit evidence. They stopped before remote mutation or failed only in local verification harness assumptions; no published Maven/npm/tag/Release state was replayed.

## v2026.07.20-frontend-ui-fixes-npm-release - 2026-07-20

Status: `PUBLISHED_AND_VERIFIED`. This npm-only Mango package batch was published from source commit `bd9e81db2683787456eb0260a04100efb08e4096`. All 24 npm coordinates resolve from both Nexus npm hosted and group, and the GitHub Release is published at the matching immutable tag. Mango Maven remains `1.0.23`, `@mango/cli` updates to `1.0.87`, and `@mango/pmo` updates to `1.3.3`.

### Fixed

- Make the admin theme settings panel apply the configured top-bar text color through the Shell theme tokens.
- Keep `MangoSearchPanel`'s bottom expand/collapse action row at a stable fixed height when no expansion is needed, while hiding only the expand/collapse buttons.
- Let PDF previews fill the file preview dialog content height and leave scrolling to the PDF surface.
- Always render the file preview "open in new window" action and disable it only when no usable preview URL exists.

### Added

- Add `Editor.toolbarKeys` so consumers can provide a compact WangEditor toolbar, such as bold, text color, ordered list, unordered list, and image upload only.
- Add `Editor.imageValueType` so uploaded rich-text images can write URL, file ID, or `mango-file:<id>` tokens into saved HTML.

### Versions

| Component                   |             Previous |      Release | Compatibility                                                                                                                                   |
| --------------------------- | -------------------: | -----------: | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| `@mango/common`             |             `1.0.19` |     `1.0.20` | Additive Editor props plus search-panel layout fix; existing props and events remain compatible.                                                |
| `@mango/file`               |             `1.0.25` |     `1.0.26` | Preview dialog layout and button visibility fix; upload, download, file ID persistence and APIs remain compatible.                              |
| `@mango/admin-shell`        |             `1.0.46` |     `1.0.47` | Theme token behavior fix and dependency matrix alignment; route, menu and login contracts remain compatible.                                    |
| `@mango/admin`              |             `1.0.51` |     `1.0.52` | Aggregate package alignment for the affected frontend matrix.                                                                                   |
| `@mango/pmo`                |              `1.3.2` |      `1.3.3` | PMO package-check compatibility fix for Windows release validation; PMO rules and baseline semantics remain compatible.                         |
| `@mango/cli`                |             `1.0.85` |     `1.0.87` | Business module template, release-version lock and Windows generated build-script compatibility alignment; command semantics remain compatible. |
| Downstream runtime packages | `v2026.07.19` matrix | patch bumped | Version-only republish to keep exact workspace dependency locks consistent with the new `@mango/common` and `@mango/file` releases.             |

### Published Packages

| Order | Target                        | Version                                     | Release status           |
| ----- | ----------------------------- | ------------------------------------------- | ------------------------ |
| 1     | npm PMO bundle and Skills     | `@mango/pmo@1.3.3`                          | `PUBLISHED_AND_VERIFIED` |
| 2     | npm common                    | `@mango/common@1.0.20`                      | `PUBLISHED_AND_VERIFIED` |
| 3     | npm admin pages               | `@mango/admin-pages@1.0.25`                 | `PUBLISHED_AND_VERIFIED` |
| 4     | npm auth                      | `@mango/auth@1.0.19`                        | `PUBLISHED_AND_VERIFIED` |
| 5     | npm grid layout               | `@mango/grid-layout@1.0.11`                 | `PUBLISHED_AND_VERIFIED` |
| 6     | npm grid widgets              | `@mango/grid-widgets@1.0.17`                | `PUBLISHED_AND_VERIFIED` |
| 7     | npm home                      | `@mango/home@1.0.9`                         | `PUBLISHED_AND_VERIFIED` |
| 8     | npm RBAC                      | `@mango/rbac@1.0.18`                        | `PUBLISHED_AND_VERIFIED` |
| 9     | npm site shell                | `@mango/site-shell@1.0.7`                   | `PUBLISHED_AND_VERIFIED` |
| 10    | npm job                       | `@mango/job@1.0.18`                         | `PUBLISHED_AND_VERIFIED` |
| 11    | npm link                      | `@mango/link@1.0.12`                        | `PUBLISHED_AND_VERIFIED` |
| 12    | npm file                      | `@mango/file@1.0.26`                        | `PUBLISHED_AND_VERIFIED` |
| 13    | npm notice                    | `@mango/notice@1.0.28`                      | `PUBLISHED_AND_VERIFIED` |
| 14    | npm calendar                  | `@mango/calendar@1.0.26`                    | `PUBLISHED_AND_VERIFIED` |
| 15    | npm numgen                    | `@mango/numgen@1.0.26`                      | `PUBLISHED_AND_VERIFIED` |
| 16    | npm payment                   | `@mango/payment@1.0.18`                     | `PUBLISHED_AND_VERIFIED` |
| 17    | npm template                  | `@mango/template@1.0.26`                    | `PUBLISHED_AND_VERIFIED` |
| 18    | npm workflow                  | `@mango/workflow@1.0.32`                    | `PUBLISHED_AND_VERIFIED` |
| 19    | npm workflow business example | `@mango/workflow-business-example@1.0.31`   | `PUBLISHED_AND_VERIFIED` |
| 20    | npm CMS                       | `@mango/cms@1.0.15`                         | `PUBLISHED_AND_VERIFIED` |
| 21    | npm system                    | `@mango/system@1.0.24`                      | `PUBLISHED_AND_VERIFIED` |
| 22    | npm admin shell               | `@mango/admin-shell@1.0.47`                 | `PUBLISHED_AND_VERIFIED` |
| 23    | npm admin aggregate           | `@mango/admin@1.0.52`                       | `PUBLISHED_AND_VERIFIED` |
| 24    | npm CLI                       | `@mango/cli@1.0.87`                         | `PUBLISHED_AND_VERIFIED` |
| 25    | GitHub Release                | `v2026.07.20-frontend-ui-fixes-npm-release` | `CREATED_AND_VERIFIED`   |

### Upgrade Notes

1. Upgrade the affected frontend runtime matrix together; do not mix `@mango/common@1.0.20` with packages still locked to the previous `@mango/common@1.0.19` matrix.
2. Consumption projects that use only the aggregate admin package should upgrade `@mango/admin` to `1.0.52` and refresh the lockfile.
3. Projects that install individual Mango frontend packages should upgrade the affected packages listed in `Published Packages` to the exact versions in this release.
4. Rich-text consumers can keep the default Editor behavior. Only pages that pass `toolbarKeys` or `imageValueType` opt into the new compact toolbar or file-ID/token image storage behavior.
5. File preview consumers no longer need a local override to show the new-window preview action; the action is rendered by default and disabled when no preview address is available.
6. Projects that use Mango CLI to generate business modules should upgrade the local CLI to `@mango/cli@1.0.87` after this npm batch is published.

### Verification

- `pnpm -C mango-ui admin:styles:check`
- `pnpm -C mango-ui admin:module-styles:check`
- `node mango-pmo/tools/workspace-layout-check.mjs --root .`
- `pnpm -C mango-ui release:impact --base=v2026.07.19-frontend-standards-npm-release --head=HEAD`
- `node mango-ui/packages/mango-cli/scripts/check-release-versions.mjs`
- Target package builds before publish for the 24 affected npm packages.
- Full post-publication back-check on 2026-07-21 confirmed every listed coordinate from both `npm-hosted` and `npm-group`; the GitHub Release is published and points to tag commit `bd9e81db2683787456eb0260a04100efb08e4096`.

## v2026.07.20-maven-1.0.23-cli-1.0.85-platform-fixes-release - 2026-07-20

Status: `PUBLISHED_AND_VERIFIED`. Maven `1.0.23` and CLI `1.0.85` were published from source commit `f91cf7d327dde373ae657a8bbbb3ba9749eec712`, with PMO `1.3.2` and the frontend matrix from CLI `1.0.84` unchanged. Private publication/consumer read-back, clean generated-business-project verification, GitHub Release, and documentation checks passed.

### Added

- Complete the Payment remote starter with typed OpenFeign adapters for all 27 public Payment API contracts, covering 127 methods and registering the adapters through Spring Boot auto-configuration.
- Add explicit Resource Registry synchronization lifecycle events and status so tenant provisioning reconciliation waits for required resource synchronization and can retry after an initial failure.

### Fixed

- Safely upgrade Workflow databases created by Maven `1.0.20` by repairing only the known historical V1 checksum and adding the seven missing audit columns through an idempotent V2 migration.
- Coordinate Resource Registry and System startup without depending on incidental runner order, while preserving retry behavior after synchronization failures.
- Generate stable project-relative quality fingerprints so the same `mango:check` baseline works across worktrees without normalizing paths or weakening rules.
- Keep Mango Checkstyle defaults aligned between the Maven plugin and generated business template, including the intended ternary-expression, line-length and complexity policies.
- Correct the File Preview application flow assertion so the API filename and engine-generated preview filename are validated independently.

### Versions

| Component                                   |                    Previous |   Release | Compatibility                                                                                         |
| ------------------------------------------- | --------------------------: | --------: | ----------------------------------------------------------------------------------------------------- |
| Mango Maven non-app backend and docs bundle |                    `1.0.22` |  `1.0.23` | Additive fixes; Workflow V2 is idempotent and narrowly repairs the known `1.0.20` checksum.           |
| `@mango/cli`                                |                    `1.0.84` |  `1.0.85` | Locks generated backends to Maven `1.0.23`; keeps current commands and frontend generation contracts. |
| `@mango/pmo`                                |                     `1.3.2` |   `1.3.2` | Unchanged.                                                                                            |
| Runtime frontend packages                   | CLI `1.0.84` release matrix | unchanged | No runtime frontend package is republished in this batch.                                             |

### Published Packages

| Order | Target                                | Version                                                      | Release status           |
| ----- | ------------------------------------- | ------------------------------------------------------------ | ------------------------ |
| 1     | Maven non-app backend and docs bundle | `io.mango:*:1.0.23`                                          | `PUBLISHED_AND_VERIFIED` |
| 2     | npm CLI                               | `@mango/cli@1.0.85`                                          | `PUBLISHED_AND_VERIFIED` |
| 3     | GitHub Release                        | `v2026.07.20-maven-1.0.23-cli-1.0.85-platform-fixes-release` | `CREATED_AND_VERIFIED`   |

### Upgrade Notes

1. Upgrade business backends from Mango Maven `1.0.22` to `1.0.23` as one consistent dependency set.
2. Keep `@mango/pmo@1.3.2` and the runtime frontend versions published with CLI `1.0.84`; this batch does not republish them.
3. Workflow databases initialized by Maven `1.0.20` receive the one known V1 checksum repair followed by the idempotent audit-column V2 migration. Unknown V1 checksums remain fail-closed.
4. Payment microservice consumers may replace hand-written Feign adapters with `mango-payment-starter-remote`; monolith consumers and existing Payment APIs require no source migration.
5. Upgrade the project-local CLI to `@mango/cli@1.0.85` only after Maven `1.0.23` is published and verified. No database reset or frontend runtime migration is required.

### Verification

- Required release PR checks passed on the exact source commit before tagging or publishing immutable artifacts.
- `pnpm -C mango-ui release:impact --base=v2026.07.19-frontend-standards-npm-release --head=HEAD`
- `pnpm -C mango-ui --filter @mango/cli test`
- `MANGO_BACKEND_GATE_VERSION=1.0.23 node mango-ui/packages/mango-cli/scripts/check-generated-backend-gate.mjs`
- `mvn -f mango/pom.xml -pl mango-app/microservice/mango-file-preview-app -am -Dtest=MangoFilePreviewAppFlowTest -Dsurefire.failIfNoSpecifiedTests=false test`
- The completed `1.0.23` release manifest records all 17 states as passed. It includes private publication and consumer read-back, a clean generated backend gate in 9 Maven invocations, installation and execution of the published CLI, generated project lock verification, GitHub Release verification, and docs Latest/snapshot verification.

## v2026.07.19-frontend-standards-npm-release - 2026-07-19

Status: `PUBLISHED_AND_VERIFIED`. The complete frontend standards npm matrix was published from source commit `13166d9910836c59e9eb9a4b635828bd58a0a931`. Private hosted/group resolution, clean business consumption, Vue type checking, Vite production build, GitHub Release, and docs Latest verification passed. Mango Maven remains unchanged at `1.0.22`.

### Published Packages

| Order | Target                        | Version                                      | Release status           |
| ----- | ----------------------------- | -------------------------------------------- | ------------------------ |
| 1     | npm API schema                | `@mango/api-schema@1.0.3`                    | `PUBLISHED_AND_VERIFIED` |
| 2     | npm Link OpenAPI              | `@mango/link-openapi@1.0.4`                  | `PUBLISHED_AND_VERIFIED` |
| 3     | npm PMO bundle and Skills     | `@mango/pmo@1.3.2`                           | `PUBLISHED_AND_VERIFIED` |
| 4     | npm application runtime       | `@mango/app-runtime@1.0.6`                   | `PUBLISHED_AND_VERIFIED` |
| 5     | npm HTTP client Axios adapter | `@mango/http-client@1.0.0`                   | `PUBLISHED_AND_VERIFIED` |
| 6     | npm Link page                 | `@mango/link-page@1.0.7`                     | `PUBLISHED_AND_VERIFIED` |
| 7     | npm common                    | `@mango/common@1.0.19`                       | `PUBLISHED_AND_VERIFIED` |
| 8     | npm auth                      | `@mango/auth@1.0.18`                         | `PUBLISHED_AND_VERIFIED` |
| 9     | npm grid layout               | `@mango/grid-layout@1.0.10`                  | `PUBLISHED_AND_VERIFIED` |
| 10    | npm home                      | `@mango/home@1.0.8`                          | `PUBLISHED_AND_VERIFIED` |
| 11    | npm RBAC                      | `@mango/rbac@1.0.17`                         | `PUBLISHED_AND_VERIFIED` |
| 12    | npm site shell                | `@mango/site-shell@1.0.6`                    | `PUBLISHED_AND_VERIFIED` |
| 13    | npm grid widgets              | `@mango/grid-widgets@1.0.16`                 | `PUBLISHED_AND_VERIFIED` |
| 14    | npm system                    | `@mango/system@1.0.23`                       | `PUBLISHED_AND_VERIFIED` |
| 15    | npm admin pages               | `@mango/admin-pages@1.0.24`                  | `PUBLISHED_AND_VERIFIED` |
| 16    | npm calendar                  | `@mango/calendar@1.0.25`                     | `PUBLISHED_AND_VERIFIED` |
| 17    | npm file                      | `@mango/file@1.0.25`                         | `PUBLISHED_AND_VERIFIED` |
| 18    | npm job                       | `@mango/job@1.0.17`                          | `PUBLISHED_AND_VERIFIED` |
| 19    | npm link                      | `@mango/link@1.0.11`                         | `PUBLISHED_AND_VERIFIED` |
| 20    | npm notice                    | `@mango/notice@1.0.27`                       | `PUBLISHED_AND_VERIFIED` |
| 21    | npm numgen                    | `@mango/numgen@1.0.25`                       | `PUBLISHED_AND_VERIFIED` |
| 22    | npm CMS                       | `@mango/cms@1.0.14`                          | `PUBLISHED_AND_VERIFIED` |
| 23    | npm payment                   | `@mango/payment@1.0.17`                      | `PUBLISHED_AND_VERIFIED` |
| 24    | npm template                  | `@mango/template@1.0.25`                     | `PUBLISHED_AND_VERIFIED` |
| 25    | npm workflow                  | `@mango/workflow@1.0.31`                     | `PUBLISHED_AND_VERIFIED` |
| 26    | npm admin shell               | `@mango/admin-shell@1.0.46`                  | `PUBLISHED_AND_VERIFIED` |
| 27    | npm workflow business example | `@mango/workflow-business-example@1.0.30`    | `PUBLISHED_AND_VERIFIED` |
| 28    | npm admin aggregate           | `@mango/admin@1.0.51`                        | `PUBLISHED_AND_VERIFIED` |
| 29    | npm CLI                       | `@mango/cli@1.0.84`                          | `PUBLISHED_AND_VERIFIED` |
| 30    | GitHub Release                | `v2026.07.19-frontend-standards-npm-release` | `CREATED_AND_VERIFIED`   |

### Upgrade Notes

1. Upgrade the complete npm matrix together; do not mix this release with older fixed Mango package versions.
2. New business APIs belong in `frontend/packages/<module>-api` and accept the vendor-neutral `HttpClient`. The host creates and provides one `@mango/http-client` instance per runtime context before mount; page registration remains client-free. Vue pages do not import Axios, create transports, or own base URLs.
3. Mango CMS pages now consume that injected client in both Shell local and Wujie modes. Custom runtime configs may set a stable `instanceId`; duplicate explicit identities fail validation. The historical `cmsApi` export remains deprecated compatibility only.
4. Business package CSS remains in its package style entry; page-private styles stay scoped in the page. Hosts import package style exports explicitly.
5. Existing `@mango/pmo@1.3.2` remains the governance baseline; publish the affected runtime packages before `@mango/cli@1.0.84`, which carries their exact locks.
6. Standards adoption uses `pilot`, `affected`, and `repository` stages. `mango-ui/frontend-standards-adoption.json` keeps the exact stable package matrix; the new HTTP client is removed when restoring a pre-client dependency batch.
7. The repository-local `mango-release` Skill now triggers only for actual immutable artifact/tag/GitHub Release work. Frontend standards adoption, static quality governance, and application traffic rollout remain separate; single-owner governance does not add a separate approving-review ceremony.
8. Playwright business specs use semantic locators through a centralized Element Plus adapter. Direct `.el-*`, positional `nth()`, fixed `waitForTimeout()`, and `force: true` usage is blocked; static-debt baselines cannot be raised by later commits on the task branch.

### Verification

- Vendor-neutral HTTP contracts, Axios adapter lifecycle/refresh/retry/abort behavior, generated business API factories, CLI module generation, release-version impact, and standards adoption coverage are machine checked.
- `frontend-standards-adoption.json` binds every one of the 29 release npm packages to its exact `origin/main` dependency recovery target; it does not represent a production traffic rollout.
- Release state `frontend-standards-npm-2026.07.19` completed all required states. Private hosted/group repositories resolve every exact package version, and the clean published consumer installed all 29 packages, typechecked 27 library packages, and completed a Vite production build.
- Full fixed-toolchain, sealed Business Lab, clean tarball consumer, browser/runtime, final quality, and sanitized publication results are recorded in `mango-docs/evidence/2026-07-19-frontend-production-candidate-evidence.md` and `mango-docs/evidence/governance/release-v2026.07.19-frontend-standards-npm-release.json`.

## v2026.07.18-pmo-1.3.1-cli-1.0.82-release-governance - 2026-07-18

### Changed

- Make the repository-local `mango-pmo/skills/mango-release` the exclusive Mango release workflow; user-level, plugin-level, bundled, and generic external release skills must not supplement or replace it.
- Add release readiness checks for GitHub Release headings and semantic adapter assertions, auditable recovery for failures before remote mutation, read-only verification recovery after a published artifact, mandatory CHANGELOG status backfill, and service/worktree/branch/database cleanup.
- Route Mango release tasks from the repository `AGENTS.md` directly to the governed local Skill and synchronize the same Skill into PMO and business baseline projections.

### Upgrade Notes

1. Publish `@mango/pmo@1.3.1` first, then publish `@mango/cli@1.0.82`, which locks the new PMO bundle.
2. Business projects run `mango pmo upgrade --project-dir . --to 1.3.1`, then update their project-local CLI to `@mango/cli@1.0.82`.
3. This governance-only release does not change Mango Maven `1.0.22`, runtime frontend packages, backend APIs, databases, menus, permissions, tenants, routes, or business behavior.

### Published Packages

| Order | Target                    | Version / destination                                 | Release status           |
| ----- | ------------------------- | ----------------------------------------------------- | ------------------------ |
| 1     | npm PMO bundle and Skills | `@mango/pmo@1.3.1` -> Nexus npm hosted                | `PUBLISHED_AND_VERIFIED` |
| 2     | npm CLI                   | `@mango/cli@1.0.82` -> Nexus npm hosted               | `PUBLISHED_AND_VERIFIED` |
| 3     | GitHub Release            | `v2026.07.18-pmo-1.3.1-cli-1.0.82-release-governance` | `CREATED_AND_VERIFIED`   |

### Verification

- Validate the Skill with the skill-creator quick validator and Mango Skill eval suite, including local-only routing, missing Release headings, immutable pre-write recovery, post-publish verification recovery, and closeout requirements.
- Build and check `@mango/pmo@1.3.1`, mechanically synchronize the business PMO baseline, and run CLI PMO install/upgrade/rollback/package contracts.
- Release state `pmo-1.3.1-cli-1.0.82` completed every required state. Nexus hosted/group resolve both exact packages with matching integrity; a clean consumer installed CLI 1.0.82, generated a full project, verified PMO lock 1.3.1 and the repository-local exclusive `mango-release` Skill, passed locked PMO check, and passed frontend typecheck.

## v2026.07.18-cli-1.0.81-pnpm11-generated-install-hotfix - 2026-07-18

### Fixed

- Generate `frontend/pnpm-workspace.yaml` with the pnpm 11 `allowBuilds` policy required by Mango frontend dependencies, so a newly generated business project can complete its first `pnpm install` without `ERR_PNPM_IGNORED_BUILDS`.
- Make the packed-package consumer regression require the generated install policy instead of injecting a replacement file that could hide a broken template.

### Upgrade Notes

1. Install `@mango/cli@1.0.81` before generating a new business project. Mango Maven remains `1.0.22`, PMO remains `1.3.0`, and every runtime frontend package lock remains unchanged from the preceding release.
2. Existing projects generated by an earlier CLI should add the same `allowBuilds` map from the current `frontend/pnpm-workspace.yaml` template before installing with pnpm 11; this patch does not overwrite business-owned frontend workspace configuration.
3. No backend API, database, menu, permission, tenant, route or runtime behavior changes in this npm-only hotfix.

### Published Packages

| Order | Target         | Version / destination                                    | Release status           |
| ----- | -------------- | -------------------------------------------------------- | ------------------------ |
| 1     | npm CLI        | `@mango/cli@1.0.81` -> Nexus npm hosted                  | `PUBLISHED_AND_VERIFIED` |
| 2     | GitHub Release | `v2026.07.18-cli-1.0.81-pnpm11-generated-install-hotfix` | `CREATED_AND_VERIFIED`   |

### Verification

- `pnpm -C mango-ui --filter @mango/cli test`
- `pnpm -C mango-ui package-consumer:typecheck -- --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- Fresh generated-project install with pnpm 11, without a verification-harness workspace-policy injection, followed by `pnpm run typecheck`.
- Nexus npm hosted and npm group resolve `@mango/cli@1.0.81` with matching integrity; read-only release manifest `cli-1.0.81-verified` completed every required state after installing the published package, generating a full project, completing pnpm 11 install, and passing frontend typecheck.

## v2026.07.18-maven-1.0.22-pmo-1.3.0-cli-1.0.80-regression-release - 2026-07-18

### Breaking Changes

- Mango Maven `1.0.22` removes `io.mango.resource.api.ResourceRegistryApi` and replaces the resource declaration contract with `ResourceDeclarationApi`. Business code that imports, injects, implements or references the old API (including Feign declarations) must migrate to `ResourceDeclarationApi`; this release intentionally does not retain a compatibility alias.
- Resource synchronization now uses `mango-resource-sync-starter`. Business applications with source-level references to the former resource target topology must switch to the sync starter and its current declaration contract before upgrading.
- Mango Maven `1.0.22` also removes or relocates public Java types from Identity, Access, Authorization, Calendar, CMS, Job, Org, System and Template. In particular, `IdentityUserInfo` becomes `IdentityUserInfoVO`, the monolithic `CmsAdminApi` is split by capability, and Job/Resource in-process extension points move from API artifacts to support artifacts. See the [complete 1.0.21 to 1.0.22 Java API upgrade guide](mango-docs/guides/business-integration/maven-1.0.21-to-1.0.22-java-api-upgrade.md) before changing the Maven version.
- Workflow consumers upgrading directly from Maven 1.0.20 can remove the Issue #511 `MethodValidationExcludeFilter` workaround after aligning all `mango-workflow-*` artifacts to 1.0.21 or 1.0.22; both versions contain the API-owned validation constraint fix and its metadata regression test.

### Added

- Add PMO `1.3.0` risk-based delivery modes (`SIMPLE`, `STANDARD` and `FULL`) and synchronize the same governed baseline into the business starter and CLI templates.
- Establish the post-refactor full-regression baseline: 189/189 browser scenarios passed with zero skips after rebuilding a clean database and loading required/demo data.

### Fixed

- Complete the cross-module technical-debt refactor while preserving menu ownership, permissions, routes, tenant boundaries and business behavior verified by the full browser regression.
- Fix file preview URL selection consistently across file types: directly preview browser-supported files and route complex formats through the preview service, while preserving the requesting frontend host through the proxy path.
- Restore required file storage configuration during clean-database initialization and change the default file access mode to proxy delivery.
- Correct Notice login/message-center flows, Payment public cross-tenant and cashier-preview flows, CMS remote adapter behavior, and the remaining admin regression gaps.

### Upgrade Notes

1. Publish and verify Mango Maven `1.0.22` first. Before changing a business application's Maven version, scan and migrate the deleted Java APIs using the [complete upgrade guide](mango-docs/guides/business-integration/maven-1.0.21-to-1.0.22-java-api-upgrade.md).
2. Migrate every `ResourceRegistryApi` reference to `ResourceDeclarationApi`, move local Resource extension points to `mango-resource-support`, and replace former resource-target deployment dependencies with `mango-resource-sync-starter` where applicable.
3. Compile and verify the complete business backend on one consistent Mango `1.0.22` dependency set before upgrading the PMO, frontend or CLI.
   The published `mango-maven-plugin:1.0.22` can misclassify absolute-path baselines from another worktree; until a later Maven version includes Issue #588, keep any temporary path normalization under the ignored `.runtime/` directory and never commit or relax the baseline.
4. Upgrade the PMO bundle with `mango pmo upgrade --project-dir . --to 1.3.0`; this synchronizes the new delivery-mode contracts and governed baseline.
5. Upgrade frontend consumers to the package versions below. Aggregate admin consumers should use `@mango/admin@1.0.49`.
6. Upgrade the project-local CLI to `@mango/cli@1.0.80` last so generated and upgraded projects receive the exact Maven, PMO and frontend locks from this batch.
7. Existing databases run `V2__default_file_access_mode_to_proxy.sql`, changing the default file access mode to proxy delivery. Review deployments that intentionally require direct-access URLs and keep an explicit configuration for that behavior.

### Published Packages

| Order | Target                                | Version / destination                                              | Release status           |
| ----- | ------------------------------------- | ------------------------------------------------------------------ | ------------------------ |
| 1     | Maven non-app backend and docs bundle | `io.mango:*:1.0.22` -> Nexus Maven hosted                          | `PUBLISHED_AND_VERIFIED` |
| 2     | npm common                            | `@mango/common@1.0.18` -> Nexus npm hosted                         | `PUBLISHED_AND_VERIFIED` |
| 3     | npm grid layout                       | `@mango/grid-layout@1.0.9` -> Nexus npm hosted                     | `PUBLISHED_AND_VERIFIED` |
| 4     | npm grid widgets                      | `@mango/grid-widgets@1.0.15` -> Nexus npm hosted                   | `PUBLISHED_AND_VERIFIED` |
| 5     | npm home                              | `@mango/home@1.0.7` -> Nexus npm hosted                            | `PUBLISHED_AND_VERIFIED` |
| 6     | npm link OpenAPI                      | `@mango/link-openapi@1.0.3` -> Nexus npm hosted                    | `PUBLISHED_AND_VERIFIED` |
| 7     | npm link page                         | `@mango/link-page@1.0.6` -> Nexus npm hosted                       | `PUBLISHED_AND_VERIFIED` |
| 8     | npm PMO bundle                        | `@mango/pmo@1.3.0` -> Nexus npm hosted                             | `PUBLISHED_AND_VERIFIED` |
| 9     | npm RBAC                              | `@mango/rbac@1.0.15` -> Nexus npm hosted                           | `PUBLISHED_AND_VERIFIED` |
| 10    | npm site shell                        | `@mango/site-shell@1.0.5` -> Nexus npm hosted                      | `PUBLISHED_AND_VERIFIED` |
| 11    | npm system                            | `@mango/system@1.0.21` -> Nexus npm hosted                         | `PUBLISHED_AND_VERIFIED` |
| 12    | npm auth                              | `@mango/auth@1.0.17` -> Nexus npm hosted                           | `PUBLISHED_AND_VERIFIED` |
| 13    | npm admin pages                       | `@mango/admin-pages@1.0.22` -> Nexus npm hosted                    | `PUBLISHED_AND_VERIFIED` |
| 14    | npm calendar                          | `@mango/calendar@1.0.23` -> Nexus npm hosted                       | `PUBLISHED_AND_VERIFIED` |
| 15    | npm file                              | `@mango/file@1.0.23` -> Nexus npm hosted                           | `PUBLISHED_AND_VERIFIED` |
| 16    | npm job                               | `@mango/job@1.0.15` -> Nexus npm hosted                            | `PUBLISHED_AND_VERIFIED` |
| 17    | npm link                              | `@mango/link@1.0.9` -> Nexus npm hosted                            | `PUBLISHED_AND_VERIFIED` |
| 18    | npm notice                            | `@mango/notice@1.0.25` -> Nexus npm hosted                         | `PUBLISHED_AND_VERIFIED` |
| 19    | npm numgen                            | `@mango/numgen@1.0.23` -> Nexus npm hosted                         | `PUBLISHED_AND_VERIFIED` |
| 20    | npm payment                           | `@mango/payment@1.0.15` -> Nexus npm hosted                        | `PUBLISHED_AND_VERIFIED` |
| 21    | npm template                          | `@mango/template@1.0.23` -> Nexus npm hosted                       | `PUBLISHED_AND_VERIFIED` |
| 22    | npm workflow                          | `@mango/workflow@1.0.29` -> Nexus npm hosted                       | `PUBLISHED_AND_VERIFIED` |
| 23    | npm workflow example                  | `@mango/workflow-business-example@1.0.28` -> Nexus npm hosted      | `PUBLISHED_AND_VERIFIED` |
| 24    | npm admin shell                       | `@mango/admin-shell@1.0.44` -> Nexus npm hosted                    | `PUBLISHED_AND_VERIFIED` |
| 25    | npm CMS                               | `@mango/cms@1.0.12` -> Nexus npm hosted                            | `PUBLISHED_AND_VERIFIED` |
| 26    | npm admin aggregate                   | `@mango/admin@1.0.49` -> Nexus npm hosted                          | `PUBLISHED_AND_VERIFIED` |
| 27    | npm CLI                               | `@mango/cli@1.0.80` -> Nexus npm hosted                            | `PUBLISHED_AND_VERIFIED` |
| 28    | GitHub Release                        | `v2026.07.18-maven-1.0.22-pmo-1.3.0-cli-1.0.80-regression-release` | `CREATED_AND_VERIFIED`   |

### Verification

- PR #581 required checks passed on the merged refactor, including the Java quality gate, frontend contract checks and targeted module suites.
- Release PR #582 required checks passed and merged before the tag and immutable artifacts were created.
- Clean-database browser regression: 189/189 passed, zero failed and zero skipped; Payment core regression: 262/262 passed.
- `node mango-pmo/tools/workspace-layout-check.mjs --root .`
- `node mango-business-starter/scripts/sync-pmo-baseline.mjs --check`
- `pnpm -C mango-ui release:impact --base=v2026.07.16-theme-search-file-npm-release --head=HEAD`
- `pnpm -C mango-ui --filter @mango/cli run check:release-versions`
- `MANGO_BACKEND_GATE_VERSION=1.0.22 node mango-ui/packages/mango-cli/scripts/check-generated-backend-gate.mjs`
- Release state `1.0.22` completed all 16 states; exact Maven/npm coordinates, GitHub Release, docs Latest/snapshot and clean-cache generated consumers were verified through Nexus public/group repositories.

## v2026.07.16-theme-search-file-npm-release - 2026-07-16

### Fixed

- Publish the merged admin theme color token fixes so the top-bar text color, shell layout surfaces and micro-frontend runtime theme propagation use the configured theme variables consistently.
- Publish the `MangoSearchPanel` bottom expand-row fix so panels that do not need expansion keep the fixed bottom row height while hiding only the expand/collapse button.
- Publish the file preview dialog fix so PDF preview fills the available dialog height and the new-window preview action is always rendered, disabled only when no preview URL is available.

### Upgrade Notes

1. Upgrade generated or existing business frontends to `@mango/cli@1.0.79` so the project release lock points at this npm batch.
2. Consumers using the Mango admin aggregate should upgrade to `@mango/admin@1.0.48`; independent consumers should upgrade the directly used packages listed below.
3. Continue importing package styles through the public `style.css` entries, for example `@mango/common/style.css`, `@mango/file/style.css` and `@mango/admin/style.css`.
4. No backend Maven, database, API, route, permission, tenant or storage migration is required for this frontend-only release batch.

### Published Packages

| Order | Target               | Version / destination                                         | Pre-release status |
| ----- | -------------------- | ------------------------------------------------------------- | ------------------ |
| 1     | npm common           | `@mango/common@1.0.17` -> Nexus npm hosted                    | `PENDING`          |
| 2     | npm app runtime      | `@mango/app-runtime@1.0.4` -> Nexus npm hosted                | `PENDING`          |
| 3     | npm admin pages      | `@mango/admin-pages@1.0.21` -> Nexus npm hosted               | `PENDING`          |
| 4     | npm auth             | `@mango/auth@1.0.16` -> Nexus npm hosted                      | `PENDING`          |
| 5     | npm grid layout      | `@mango/grid-layout@1.0.8` -> Nexus npm hosted                | `PENDING`          |
| 6     | npm grid widgets     | `@mango/grid-widgets@1.0.14` -> Nexus npm hosted              | `PENDING`          |
| 7     | npm home             | `@mango/home@1.0.6` -> Nexus npm hosted                       | `PENDING`          |
| 8     | npm rbac             | `@mango/rbac@1.0.14` -> Nexus npm hosted                      | `PENDING`          |
| 9     | npm system           | `@mango/system@1.0.20` -> Nexus npm hosted                    | `PENDING`          |
| 10    | npm file             | `@mango/file@1.0.22` -> Nexus npm hosted                      | `PENDING`          |
| 11    | npm job              | `@mango/job@1.0.14` -> Nexus npm hosted                       | `PENDING`          |
| 12    | npm link             | `@mango/link@1.0.8` -> Nexus npm hosted                       | `PENDING`          |
| 13    | npm cms              | `@mango/cms@1.0.11` -> Nexus npm hosted                       | `PENDING`          |
| 14    | npm calendar         | `@mango/calendar@1.0.22` -> Nexus npm hosted                  | `PENDING`          |
| 15    | npm notice           | `@mango/notice@1.0.24` -> Nexus npm hosted                    | `PENDING`          |
| 16    | npm numgen           | `@mango/numgen@1.0.22` -> Nexus npm hosted                    | `PENDING`          |
| 17    | npm payment          | `@mango/payment@1.0.14` -> Nexus npm hosted                   | `PENDING`          |
| 18    | npm template         | `@mango/template@1.0.22` -> Nexus npm hosted                  | `PENDING`          |
| 19    | npm workflow         | `@mango/workflow@1.0.28` -> Nexus npm hosted                  | `PENDING`          |
| 20    | npm workflow example | `@mango/workflow-business-example@1.0.27` -> Nexus npm hosted | `PENDING`          |
| 21    | npm site shell       | `@mango/site-shell@1.0.4` -> Nexus npm hosted                 | `PENDING`          |
| 22    | npm admin shell      | `@mango/admin-shell@1.0.43` -> Nexus npm hosted               | `PENDING`          |
| 23    | npm admin aggregate  | `@mango/admin@1.0.48` -> Nexus npm hosted                     | `PENDING`          |
| 24    | npm CLI              | `@mango/cli@1.0.79` -> Nexus npm hosted                       | `PENDING`          |
| 25    | GitHub Release       | `v2026.07.16-theme-search-file-npm-release`                   | `PENDING`          |

### Verification

- `pnpm -C mango-ui --filter @mango/common exec vitest run components/MangoListPage/__tests__/MangoPageLayout.spec.ts`
- `pnpm -C mango-ui --filter @mango/file test`
- `pnpm -C mango-ui --filter @mango/common build`
- `pnpm -C mango-ui --filter @mango/file build`
- `pnpm -C mango-ui admin:styles:check`
- `pnpm -C mango-ui admin:module-styles:check`
- `pnpm -C mango-ui release:impact --base=936c4b846 --head=HEAD`
- `pnpm -C mango-ui -r --filter ./packages/* --filter !@mango/cli --if-present run build`
- `pnpm -C mango-ui package-exports:check`
- Manual split of `package-consumer:typecheck` using packed tarballs under `.runtime/pct/mrndqy35`: generated consumer `pnpm install --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/` and `pnpm run typecheck` both passed. The wrapper command itself timed out on Windows after reaching the generated consumer install step and is recorded as a local script execution issue, not a consumer type failure.
- `pnpm -C mango-ui --filter @mango/cli run check:release-versions`
- After publishing: verify every package from `npm-hosted` and `npm-group`, then run `pnpm -C mango-ui release:verify-npm <package> --version=<version> --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`.

## v2026.07.14-maven-1.0.21-platform-debt-release - 2026-07-14

### Fixed

- Fix [Issue #507](https://github.com/HardyDou/mango/issues/507): initialize `.mango/m2/repository` as a directory link to `~/.m2/repository` during `mango workspace init`, so fresh worktrees reuse the user's Maven dependency cache instead of downloading everything again.
- Preserve a pre-existing real directory or link, keeping explicitly isolated Maven repositories opt-in and preventing destructive initialization.
- Complete the Payment, CMS, Workflow, and Notice historical-debt corrections from PR #502, including clean-database initialization, API validation ownership, notification integration, Payment demo resources, and current management-page contracts.
- Make the Java architecture gate recognize external API and Service interface stubs in partial Reactor scans while retaining full interface and method parity checks when the interface bytecode is available.
- Include the previously merged Notice architecture cleanup and PMO delivery-assurance update in the first unpublished package versions after the prior release batch.

### Upgrade Notes

1. Publish and verify Mango Maven `1.0.21`, then publish `@mango/pmo@1.2.6`, `@mango/notice@1.0.23`, `@mango/payment@1.0.13`, `@mango/admin-shell@1.0.41`, `@mango/admin@1.0.46`, and `@mango/cli@1.0.78` in dependency order.
2. Upgrade existing business backends to Mango Maven `1.0.21`; new databases receive the corrected V1 schemas and module resources automatically. Existing databases do not replay V1 and require no destructive migration for this fresh-database correction batch.
3. Upgrade frontend packages through `@mango/admin@1.0.46`, or update Notice and Payment packages directly when consumed independently. Run `mango pmo upgrade --project-dir . --to 1.2.6` for the current delivery-assurance baseline.
4. Upgrade the project-local CLI to `@mango/cli@1.0.78` and run `mango workspace init` once in every active worktree; no business `mango.dev.json` edit or manual filesystem workaround is required.

### Published Packages

| Order | Target                                | Version / destination                            | Pre-release status |
| ----- | ------------------------------------- | ------------------------------------------------ | ------------------ |
| 1     | Maven non-app backend and docs bundle | `io.mango:*:1.0.21` -> Nexus Maven hosted        | `PENDING`          |
| 2     | npm PMO bundle                        | `@mango/pmo@1.2.6` -> Nexus npm hosted           | `PENDING`          |
| 3     | npm Notice                            | `@mango/notice@1.0.23` -> Nexus npm hosted       | `PENDING`          |
| 4     | npm Payment                           | `@mango/payment@1.0.13` -> Nexus npm hosted      | `PENDING`          |
| 5     | npm Admin Shell                       | `@mango/admin-shell@1.0.41` -> Nexus npm hosted  | `PENDING`          |
| 6     | npm Admin                             | `@mango/admin@1.0.46` -> Nexus npm hosted        | `PENDING`          |
| 7     | npm CLI                               | `@mango/cli@1.0.78` -> Nexus npm hosted          | `PENDING`          |
| 8     | GitHub Release                        | `v2026.07.14-maven-1.0.21-platform-debt-release` | `PENDING`          |

### Verification

- PR #502: Payment 36/36 and Workflow 14/14 browser E2E, focused Payment/CMS/Workflow/Notice API and persistence checks, and the exact partial-Reactor Java gate all passed.
- `pnpm -C mango-ui release:impact --base=v2026.07.14-maven-1.0.20-cli-1.0.77-path-binding-release --head=HEAD`
- `pnpm -C mango-ui admin:styles:check && pnpm -C mango-ui admin:module-styles:check`
- `node mango-business-starter/scripts/sync-pmo-baseline.mjs --check`
- `node mango-ui/packages/mango-cli/scripts/check-release-versions.mjs`
- `MANGO_BACKEND_GATE_VERSION=1.0.21 node mango-ui/packages/mango-cli/scripts/check-generated-backend-gate.mjs`
- Internal Nexus publication is complete only after hosted/group repository back-check and clean consumer verification.
- `node mango-pmo/tools/workspace-layout-check.mjs --root .`

## v2026.07.14-maven-1.0.20-cli-1.0.77-path-binding-release - 2026-07-14

### Fixed

- Fix [Issue #496](https://github.com/HardyDou/mango/issues/496): change all configurable `architecture` goal path parameters from the non-instantiable `java.nio.file.Path` interface to Maven/Plexus-bindable `java.io.File`, converting to `Path` only inside the plugin.
- Cover `reportFile`, `rootDirectory`, `debtBaselineFile`, and `globalEntityManifest`; the latter is fixed proactively because it used the same unsupported descriptor type.
- Add a Maven plugin harness regression that loads a real plugin POM and binds all four parameters through Maven's component configurator.

### Upgrade Notes

1. Publish and verify Mango Maven `1.0.20`, then publish `@mango/cli@1.0.77`.
2. Business repositories blocked by `Cannot create instance of interface java.nio.file.Path` should upgrade their backend Mango version to `1.0.20` and rerun only the failed current-SHA required check once.
3. No PMO baseline, source-code, database, API, menu, permission, tenant, or runtime configuration migration is required.

### Published Packages

| Order | Target | Version / destination | Pre-release status |
|---|---|---|
| 1 | Maven non-app backend and docs bundle | `io.mango:*:1.0.20` -> Nexus Maven hosted | `PENDING` |
| 2 | npm CLI | `@mango/cli@1.0.77` -> Nexus npm hosted | `PENDING` |
| 3 | GitHub Release | `v2026.07.14-maven-1.0.20-cli-1.0.77-path-binding-release` | `PENDING` |

### Verification

- `mvn -pl mango-tools/mango-maven-plugin -am -DskipTests=false -Dtest=ArchitectureMojoTest,ArchitectureMojoBindingTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Generated plugin descriptor declares all four architecture path parameters as `java.io.File`.
- `pnpm --filter @mango/cli run check:release-versions`
- `node mango-pmo/tools/test-quality-check.mjs --base origin/main`

## v2026.07.14-link-page-1.0.5-cli-1.0.76-release - 2026-07-14

### Changed

- Publish `@mango/link-page@1.0.5` with the merged public link page visual polish from PR #487.
- Keep the existing component API and data contract unchanged while shipping the updated background, spacing, tag, and card styles through the package `style.css` entry.
- Publish `@mango/cli@1.0.76` so generated and upgraded business projects lock `@mango/link-page` to `1.0.5`.
- Add an npm release contract for `@mango/link-page` so future tarball verification checks the JavaScript entry, type entry, style entry, and link-page style marker.
- Normalize local tarball paths in the generated package-consumer typecheck so the shared npm publish gate works on Windows release worktrees.

### Upgrade Notes

1. Upgrade business projects that consume the public link page package to `@mango/link-page@1.0.5`.
2. Continue importing the package style through `@mango/link-page/style.css`; no prop, event, API, route, permission, or data migration is required.
3. Install `@mango/cli@1.0.76` before generating or upgrading projects that should receive the `@mango/link-page@1.0.5` release lock.

### Published Packages

| Order | Target                | Version / destination                            | Status                   |
| ----- | --------------------- | ------------------------------------------------ | ------------------------ |
| 1     | npm link page package | `@mango/link-page@1.0.5` -> Nexus npm hosted     | `PUBLISHED_AND_VERIFIED` |
| 2     | npm CLI package       | `@mango/cli@1.0.76` -> Nexus npm hosted          | `PUBLISHED_AND_VERIFIED` |
| 3     | GitHub Release        | `v2026.07.14-link-page-1.0.5-cli-1.0.76-release` | `PUBLISHED`              |

### Verification

- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`
- `pnpm --filter @mango/link-page build`
- `pnpm --filter @mango/cli run check:release-versions`
- `pnpm release:impact --base=origin/main --head=HEAD`
- `pnpm package-consumer:typecheck -- --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/ --keep-temp`
- `MANGO_SHARED_PUBLISH_GATES_PASSED=1 pnpm publish:pkg @mango/link-page --dry-run --skip-shared-gates --publish-registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/ --consume-registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- `MANGO_SHARED_PUBLISH_GATES_PASSED=1 pnpm publish:pkg @mango/cli --dry-run --skip-shared-gates --publish-registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/ --consume-registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/`
- After publishing: verify `@mango/link-page@1.0.5` and `@mango/cli@1.0.76` from both `npm-hosted` and `npm-group`, then run `pnpm release:verify-npm` for both packages against `npm-group`.

## v2026.07.14-maven-1.0.19-pmo-1.2.5-cli-1.0.75-gate-baseline-release - 2026-07-14

### Fixed

- Fix [Issue #491](https://github.com/HardyDou/mango/issues/491): aggregated PMD and Checkstyle now produce complete reports before Mango applies `no-new-violations`, so historical findings remain visible without terminating the changed-file gate before classification.
- Keep static-analysis execution errors blocking under `staticFailurePolicy=block`; the fix does not skip PMD, Checkstyle, SpotBugs, or downgrade the required gate.
- Add `.mango-pmo-legacy-documents.json` for lifecycle documents created before PMO contracts. Every exception is pinned to a repository-relative path and SHA-256; changed, stale, duplicate, or out-of-root entries fail closed, and new lifecycle documents cannot use the baseline.

### Upgrade Notes

1. Publish and verify Mango Maven `1.0.19` and `@mango/pmo@1.2.5` before installing `@mango/cli@1.0.75`.
2. Run `mango pmo upgrade --project-dir . --to 1.2.5`, upgrade the business backend's `<mango.version>` to `1.0.19`, and register only approved pre-contract documents in the hash baseline.
3. Run one final required check for the upgraded business commit; do not retry unchanged runs based on older artifacts.

### Published Packages

| Order | Target                                | Version / destination                                                 | Pre-release status |
| ----- | ------------------------------------- | --------------------------------------------------------------------- | ------------------ |
| 1     | Maven non-app backend and docs bundle | `io.mango:*:1.0.19` -> Nexus Maven hosted                             | `PENDING`          |
| 2     | npm PMO bundle                        | `@mango/pmo@1.2.5` -> Nexus npm hosted                                | `PENDING`          |
| 3     | npm CLI                               | `@mango/cli@1.0.75` -> Nexus npm hosted                               | `PENDING`          |
| 4     | GitHub Release                        | `v2026.07.14-maven-1.0.19-pmo-1.2.5-cli-1.0.75-gate-baseline-release` | `PENDING`          |

### Verification

- `mvn -f mango/pom.xml -pl mango-tools/mango-maven-plugin -Dtest=CheckMojoTest test`
- `node --test mango-pmo/tests/document-contract/document-contract.test.mjs`
- PMO package build/check, business baseline projection, CLI release lock, capability documentation, README source facts, and workspace layout all passed.

## v2026.07.14-pmo-1.2.4-cli-1.0.74-ci-fast-gates-release - 2026-07-14

### Changed

- Split the required PR gate into parallel PMO, CLI/JavaScript, Java, and documentation jobs behind the stable `pmo-doc-check` result.
- Add a trusted, two-minute `pr-contract-check` for PR-body risk and capability contracts; editing the PR body no longer starts Maven, Java, pnpm, or the heavy code gate.
- Cancel obsolete code-SHA runs, cache pnpm and Maven inputs, and classify generated-backend behavior independently so version, changelog, README, release-lock, and PMO-contract-only changes skip generated backend acceptance.
- Reduce generated-backend acceptance from 19 Maven invocations to 9 and remove all 13 `clean` executions while retaining positive, negative, fail-closed, static-analysis, architecture, schema, metadata, and affected-module coverage.
- Ship the same dependency-build separation and precise partial quality scope through the generated GitHub and Gitea workflows. No full Reactor PR gate and no human approval are introduced.
- Keep the Jenkins release workspace Git object database between builds, stop fetching unrelated tags, and bootstrap Maven from a fast mirror with the Apache official SHA-512 and official-source fallback.
- Force Jenkins Maven invocations to read the persisted `${JENKINS_HOME}/.m2/settings.xml`, matching the release credential contract when the container runs as root.

### Upgrade Notes

1. Install `@mango/cli@1.0.74` only after both it and `@mango/pmo@1.2.4` resolve from `npm-group`; Mango Maven remains `1.0.18`.
2. Run `mango pmo upgrade --project-dir . --to 1.2.4`, then synchronize the repository-host workflow.
3. On GitHub, require `pr-contract-check` and `pmo-doc-check`; a green result merges automatically without human Review.

### Published Packages

| Order | Target         | Version / destination                                    | Pre-release status       |
| ----- | -------------- | -------------------------------------------------------- | ------------------------ |
| 1     | npm PMO bundle | `@mango/pmo@1.2.4` -> Nexus npm hosted                   | `PUBLISHED_AND_VERIFIED` |
| 2     | npm CLI        | `@mango/cli@1.0.74` -> Nexus npm hosted                  | `PUBLISHED_AND_VERIFIED` |
| 3     | GitHub Release | `v2026.07.14-pmo-1.2.4-cli-1.0.74-ci-fast-gates-release` | `PUBLISHED`              |

### Verification

- `node --test mango-pmo/tests/pmo-check-scope.test.mjs mango-pmo/tests/branch-protection-policy.test.mjs`
- `node --test mango-ui/packages/mango-cli/tests/generated-backend-gate-contract.test.mjs`
- `MANGO_BACKEND_GATE_VERSION=1.0.18 node mango-ui/packages/mango-cli/scripts/check-generated-backend-gate.mjs`
- `node mango-ui/packages/mango-cli/scripts/check-cli.mjs`
- Workflow YAML parsing, PMO package build/check, baseline sync, governance intent, and live branch-protection read-back.
- PR contract checks completed in 6-7 seconds; release-pipeline-only PR #488 completed the aggregate code gate in about 42 seconds while unrelated Java and PMO jobs were skipped.
- `@mango/pmo@1.2.4` and `@mango/cli@1.0.74` were verified from both `npm-hosted` and `npm-group`, followed by a clean joint install from `npm-group`.

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

### Published Packages

| Order | Target                                | Version / destination                                   | Pre-release status       |
| ----- | ------------------------------------- | ------------------------------------------------------- | ------------------------ |
| 1     | Maven non-app backend and docs bundle | `io.mango:*:1.0.18` -> Nexus Maven hosted               | `PUBLISHED_AND_VERIFIED` |
| 2     | npm PMO bundle                        | `@mango/pmo@1.2.3` -> Nexus npm hosted                  | `PUBLISHED_AND_VERIFIED` |
| 3     | npm CLI                               | `@mango/cli@1.0.73` -> Nexus npm hosted                 | `PUBLISHED_AND_VERIFIED` |
| 4     | GitHub Release                        | `v2026.07.14-maven-1.0.18-pmo-1.2.3-cli-1.0.73-release` | `PUBLISHED`              |

### Verification

- `node --test mango-pmo/tests/pmo-check-scope.test.mjs`
- `mvn -q -f mango/pom.xml -pl mango-tools/mango-maven-plugin verify`
- Clean local Maven repository: prerequisite `-am install` followed by a direct-module partial quality gate completed with `BUILD SUCCESS`.
- GitHub implementation PR [#483](https://github.com/HardyDou/mango/pull/483) passed `PMO Documentation Checks / pmo-doc-check` before merge.
- GitHub Maven release run `29306733668` completed through Jenkins #13 in 2m47s; `mango-parent` and the 121 MB `mango-docs-bundle` resolve from `maven-public`.
- `@mango/pmo@1.2.3` and `@mango/cli@1.0.73` were verified from both `npm-hosted` and `npm-group`, followed by a clean joint install from `npm-group`.

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

| Order | Target         | Version / destination                                                                | Status                   |
| ----- | -------------- | ------------------------------------------------------------------------------------ | ------------------------ |
| 1     | npm PMO bundle | `@mango/pmo@1.2.2` -> `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`  | `PUBLISHED_AND_VERIFIED` |
| 2     | npm CLI        | `@mango/cli@1.0.72` -> `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/` | `PUBLISHED_AND_VERIFIED` |
| 3     | GitHub Release | `v2026.07.14-pmo-1.2.2-cli-1.0.72-release`                                           | `PUBLISHED`              |

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

| Order | Target         | Version / destination                                                                | Status                   |
| ----- | -------------- | ------------------------------------------------------------------------------------ | ------------------------ |
| 1     | npm PMO bundle | `@mango/pmo@1.2.1` -> `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`  | `PUBLISHED_AND_VERIFIED` |
| 2     | npm CLI        | `@mango/cli@1.0.71` -> `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/` | `PUBLISHED_AND_VERIFIED` |
| 3     | GitHub Release | `v2026.07.14-pmo-1.2.1-cli-1.0.71-release`                                           | `PUBLISHED`              |

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

| Order | Target                      | Version / destination                                                                | Status                            |
| ----- | --------------------------- | ------------------------------------------------------------------------------------ | --------------------------------- |
| 1     | Maven non-app backend batch | `io.mango:*:1.0.17` -> Nexus Maven hosted                                            | `PUBLISHED_AND_VERIFIED`          |
| 2     | npm PMO bundle              | `@mango/pmo@1.2.0` -> `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`  | `PUBLISHED; SUPERSEDED_BY_1.2.1`  |
| 3     | npm CLI                     | `@mango/cli@1.0.70` -> `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/` | `PUBLISHED; SUPERSEDED_BY_1.0.71` |
| 4     | GitHub Release              | `v2026.07.13-maven-1.0.17-pmo-1.2.0-cli-1.0.70-release`                              | `PUBLISHED`                       |

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

| Order | Target         | Version / destination                                                                | Status      |
| ----- | -------------- | ------------------------------------------------------------------------------------ | ----------- |
| 1     | npm PMO bundle | `@mango/pmo@1.1.1` -> `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`  | `PUBLISHED` |
| 2     | npm CLI        | `@mango/cli@1.0.69` -> `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/` | `PUBLISHED` |
| 3     | GitHub Release | `v2026.07.13-pmo-1.1.1-cli-1.0.69-release`                                           | `PUBLISHED` |

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

| Order | Target                                       | Version / destination                                                                | Status      |
| ----- | -------------------------------------------- | ------------------------------------------------------------------------------------ | ----------- |
| 1     | Complete Mango backend non-app Maven reactor | `1.0.16` -> `http://nexus.inner.yunxinbaokeji.com/repository/maven-releases/`        | `PUBLISHED` |
| 2     | npm PMO bundle                               | `@mango/pmo@1.1.0` -> `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/`  | `PUBLISHED` |
| 3     | npm CLI                                      | `@mango/cli@1.0.68` -> `http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/` | `PUBLISHED` |
| 4     | Versioned Mango Docs snapshot                | `v2026.07.13-maven-1.0.16-pmo-cli-release` -> GitHub Pages                           | `PUBLISHED` |
| 5     | GitHub Release                               | release notes containing this dependency order and upgrade procedure                 | `PUBLISHED` |

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
