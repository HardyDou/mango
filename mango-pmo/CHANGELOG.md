# Mango PMO Changelog

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
