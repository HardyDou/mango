# Issue 32 Admin Shell API and Documentation Verification

## Purpose

Improve `@mango/admin-shell` as a product package for enterprise admin applications. The package should expose stable entrypoints, document extension contracts and remain consumable from published npm material.

## Review Findings

- `@mango/admin-shell` had no package README.
- Several package subpath exports pointed directly to source `.ts` or `.vue` files.
- `@mango/common` dependency was behind the current released CLI material version.
- Existing boundary tests covered workspace ranges and app-private aliases, but not product subpath exports or documentation coverage.

## Changes Verified

- Added `README.md` covering:
  - `createMangoAdminApp` setup.
  - configuration options.
  - feature registrars.
  - runtime modules.
  - menu contract.
  - theme, i18n and permission directives.
  - migration from app-local shell code.
  - compatibility matrix and verification commands.
- Changed published subpath exports to use `dist/*.js` imports with source type entries.
- Added Vite multi-entry build outputs for the exported subpaths.
- Aligned `@mango/common` dependency with `1.0.7`.
- Kept `pnpm-lock.yaml` scoped to the `packages/admin-shell` importer change only.
- Extended boundary tests for subpath exports, documentation coverage and dependency alignment.

## Commands

```bash
pnpm install
pnpm -F @mango/admin-shell test
pnpm -F @mango/admin-shell build
pnpm -F @mango/admin build
npm --prefix mango-ui/packages/mango-cli test
npm pack --dry-run --json
git diff --check
```

`npm pack --dry-run --json` was executed from `mango-ui/packages/admin-shell`.

## Results

- `@mango/admin-shell` tests: PASS, 23 tests.
- `@mango/admin-shell` build: PASS.
- `@mango/admin` build: PASS.
- `mango-cli` checks: PASS.
- `@mango/admin-shell` pack dry-run: PASS.
- Pack contents include `README.md`, `dist/index.js`, `dist/runtime.js`, `dist/menu.js`, `dist/stores.js`, `dist/router.js`, `dist/home.js`, `dist/dev-pages.js`, `dist/dev-base-pages.js`, `dist/dev-upload-page.js` and `dist/dev-workflow-page.js`.
- `git diff --check`: PASS.

## Residual Risk

- This task does not change shell runtime behavior or UI layout.
- Dev-center subpath entrypoints remain published for Mango development and verification, but the README marks them as development-center entrypoints rather than normal enterprise business APIs.
- Other frontend packages still declare older `@mango/common` versions. That wider version alignment was not changed in this issue to avoid mixing unrelated dependency governance into the admin-shell API work.
