# Mango PMO Changelog

## 1.3.10 - 2026-08-06

### Added

- Add the canonical `business-module` code baseline with backend API/core/starter/starter-remote modules, frontend API/page packages, Flyway/resources, public exports and focused tests.
- Add manifest-driven rendering and evaluation through `code-baseline.mjs` and `evaluate-code-baseline.mjs`, including input/derived-variable validation, path safety, unresolved-placeholder rejection and projection hash checks.
- Add explicit rule coverage, Mango Checkstyle and architecture quality profiles, and concrete template conventions for `XxxCode`, `Require`, typed CRUD, tenant persistence, Mapper/API/resource/migration and frontend patterns.

### Changed

- Route development toward selected code baselines and target repository source instead of mandatory bulk reference reading.
- Require generated code to retain the canonical baseline shape even in legacy repositories, while limiting touched-file cleanup to behavior-preserving violations in the same symbol or local block.
- Advance lifecycle document contracts to PMO `1.3.10` without changing schema revision `1`; unchanged PMO `1.3.9` documents remain supported only through the existing path/SHA/version historical baseline.
- Ship the mechanically synchronized business-starter baseline, package-root Codex plugin and project Skills in `@mango/pmo@1.3.10`.

### Upgrade Notes

1. Publish and verify `@mango/pmo@1.3.10` before installing `@mango/cli@1.0.99`.
2. Run `mango pmo upgrade --project-dir . --to 1.3.10 --dry-run`, review the projected rules, code templates, tools, Agents and Skills, then perform the upgrade and run `mango pmo check --project-dir . --locked`.
3. Use the published `business-module` baseline for new module work. Existing modules are not automatically rewritten and should only receive locally verifiable cleanup when their code is otherwise being changed.
4. Preserve historical PMO `1.3.9` lifecycle documents only when the upgrade-generated path, SHA-256 and version baseline remains valid; new or modified lifecycle documents use `1.3.10`.

### Verification

- `node --test mango-pmo/tests/code-baseline.test.mjs mango-pmo/tests/pmo-preflight.test.mjs mango-pmo/tests/document-contract/document-contract.test.mjs`
- `node mango-ui/packages/mango-pmo/scripts/build-package.mjs && node mango-ui/packages/mango-pmo/scripts/check-package.mjs`
- `node mango-business-starter/scripts/sync-pmo-baseline.mjs --check`
- `node mango-ui/packages/mango-cli/scripts/check-business-module-template.mjs`
- `node mango-ui/packages/mango-cli/scripts/check-cli.mjs`

## 1.3.9 - 2026-08-02

### Fixed

- Resolve terminal Gitea pull-request body edits without constructing an invalid base/head diff after the PR is merged or closed; open PR edits continue to run the full trusted-diff gate.
- Accept typed full-page baseline exceptions for genuinely inapplicable list, detail, form or dialog shells while continuing to reject untyped or explanation-free bypasses.
- Keep generated business PMO scope classification, governance-intent checks and baseline manifests mechanically aligned with the current source bundle.
- Advance lifecycle contracts to PMO `1.3.9` while accepting unchanged, upgrade-pinned PMO `1.3.8` documents as controlled historical inputs.

### Changed

- Ship the current Gitea event-mode resolver, frontend page-baseline behavior and synchronized generated-project workflow/tool projection in `@mango/pmo@1.3.9`.
- Preserve stable `pr-contract-check` and `pmo-doc-check` identities; this release adds no new required-check filename beyond the PMO `1.3.8` set.

### Upgrade Notes

1. Install `@mango/pmo@1.3.9` before `@mango/cli@1.0.96`.
2. Run `mango pmo upgrade --project-dir . --to 1.3.9 --sync-shell`, then review changes to GitHub/Gitea workflows, PMO tools, rules and the generated baseline manifest.
3. Run `mango pmo check --project-dir . --locked`; for Gitea, verify both an open-PR body edit and a terminal merged/closed-PR body edit.
4. Existing typed frontend page-baseline exceptions remain valid when their page kind and concrete reason still match the detected page; do not replace them with generic suppression text.
5. Existing PMO `1.3.8` lifecycle documents are preserved only when the upgrade-generated path/SHA-256/version baseline still matches; new or modified lifecycle documents use `1.3.9`.

### Verification

- `node --test mango-pmo/tests/pmo-check-event-mode.test.mjs mango-pmo/tests/frontend-page-baseline.test.mjs mango-pmo/tests/pmo-check-scope.test.mjs`
- `node mango-ui/packages/mango-pmo/scripts/build-package.mjs && node mango-ui/packages/mango-pmo/scripts/check-package.mjs`
- `node mango-business-starter/scripts/sync-pmo-baseline.mjs --check`
- `node mango-ui/packages/mango-cli/scripts/check-cli.mjs`

## 1.3.8 - 2026-08-01

### Fixed

- Preserve unchanged lifecycle documents created under PMO `1.3.6` or `1.3.7` when their schema-compatible contracts remain supported, using a path, SHA-256 and PMO-version baseline created by `mango pmo upgrade`.

### Changed

- Require new or modified management list pages to use `MangoListPage`, `MangoSearchPanel`, `MangoListPanel` and `Pagination`.
- Require new or modified independent detail/form pages to use the current Mango page shells and standard dialogs to use `MangoDialog`.
- Add the incremental frontend page-baseline checker to the PMO bundle and generated business baseline.
- Add frontend page-baseline execution to generated GitHub and Gitea workflows and aggregate it into the stable `pmo-doc-check` required check.

### PMO Required Checks

- `check-frontend-page-baseline.mjs`
  - Migration: Before upgrading, inspect changed `views/**/*.vue` files and migrate management lists, independent detail/form pages and standard dialogs to their current Mango components.
  - Exception: Use a reviewable typed comment such as `<!-- mango-page-baseline-exception form: embedded vendor form cannot use the independent page shell -->` only when the detected pattern is not the governed page type; supported kinds are `list`, `detail`, `form` and `dialog`.
  - Verify: `node business-pmo/mango-baseline/tools/check-frontend-page-baseline.mjs --base <base-sha> --head <head-sha> --frontend-root <frontend-root>`.

### Upgrade Notes

1. Inspect open and planned frontend changes before syncing PMO 1.3.8; migrate affected pages or add a typed exception with a concrete reason.
2. Run `mango pmo upgrade --project-dir . --to 1.3.8 --sync-shell` and review the PMO rule, tool and GitHub/Gitea workflow diff.
3. Review `.mango-pmo-legacy-documents.json`, run `mango pmo check --project-dir . --locked`, then run the frontend page-baseline checker against the intended PR base/head.
4. New or migrated lifecycle documents must use PMO `1.3.8`; only unchanged documents on contract-declared historical versions can use the generated compatibility baseline.

### Verification

- `node --test mango-pmo/tests/document-contract/document-contract.test.mjs`
- `node --test mango-pmo/tests/frontend-page-baseline.test.mjs`
- `node mango-ui/packages/mango-pmo/scripts/build-package.mjs && node mango-ui/packages/mango-pmo/scripts/check-package.mjs`
- `node mango-ui/packages/mango-cli/scripts/check-cli.mjs`
- `node mango-business-starter/scripts/sync-pmo-baseline.mjs --check`
