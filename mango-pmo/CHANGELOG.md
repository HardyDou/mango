# Mango PMO Changelog

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
