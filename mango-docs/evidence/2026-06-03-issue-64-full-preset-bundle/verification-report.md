# Issue 64 Full Preset Dependency and Bundle Verification

## Scope

- `@mango/cli` full preset frontend dependency versions.
- Generated full preset frontend Vite production build chunking.
- Generated full preset build warning capture.

## Root Cause

The CLI rendered generated frontend dependency versions from Mango workspace package versions. When a workspace package version was bumped before its npm package was published, the generated enterprise project could reference a version that was not available from the configured registry.

Observed blocker before the fix:

- `@mango/admin` rendered as `1.0.12`.
- Internal npm group registry latest `@mango/admin` was `1.0.11`.
- `npm install` failed with `No matching version found for @mango/admin@1.0.12`.

## Changes Verified

- Added `mango-ui/packages/mango-cli/release-versions.json` as the CLI release-material version source.
- Generated frontend now uses the published versions from the release version manifest.
- Generated full preset Vite config uses `manualChunks` for Mango modules and major vendor groups.
- Generated frontend `npm run build` records build logs and warning summaries under `frontend/build-reports/`.

## Commands

```bash
npm --prefix mango-ui/packages/mango-cli test
rm -rf issue64-full
node mango-ui/packages/mango-cli/src/index.mjs init issue64-full --preset full --package com.example.issue64 --group-id com.example --topology monolith --force
npm --prefix issue64-full/frontend install 2>&1 | tee issue64-full/frontend/install.log
npm --prefix issue64-full/frontend run build 2>&1 | tee issue64-full/frontend/build.log
```

## Results

- CLI full/custom/add/module checks: PASS.
- Generated `@mango/admin`: `1.0.11`, available in the internal npm group registry.
- Generated `@mango/admin-shell`: `1.0.10`, available in the internal npm group registry.
- `npm install`: PASS.
- `npm run build`: PASS.
- Build warning report: `warningCount = 0`.
- Vite large chunk warning: not present after chunking.

Largest generated JS chunks:

| Chunk | Size | Gzip |
|---|---:|---:|
| `form-create-designer` | 1,055.97 kB | 328.39 kB |
| `element-plus` | 834.53 kB | 257.80 kB |
| `richtext-vendor` | 811.52 kB | 281.06 kB |
| `mango-platform` | 411.07 kB | 121.76 kB |
| `mango-template` | 409.48 kB | 132.03 kB |

## Residual Warnings

`npm install` still reports transitive deprecated dependency warnings:

- `inflight@1.0.6`
- `lodash.isequal@4.5.0`
- `glob@7.2.3`
- `vue-i18n@9.2.2`
- `codemirror@6.65.7`

These warnings are captured in `install.log`. They were not changed in this task because replacing them requires dependency-specific upgrade work, including `vue-i18n` major-version migration and editor/designer dependency review.

## Evidence

- `install.log`
- `build.log`
- `frontend-build-report.json`
- `frontend-build-warnings.log`
