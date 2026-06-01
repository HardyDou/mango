# Sprint 7 Summary

## Goal

Support optional Mango module selection in `mango-cli` and upgrade PMO page regression rules so page acceptance checks visible details instead of only checking route reachability.

## Completed

- Added `mango init --preset custom --modules ...`.
- Kept `full` preset on `@mango/admin/full`, `@mango/admin/style-full.css` and backend `mango-admin-starter`.
- Added optional module catalog for file, template, notice, numgen, calendar, workflow and workflow-example.
- Added `mango add <module...> --project-dir <project>`.
- Made `workflow-example` automatically include `workflow`.
- Generated custom projects with required authorization/system group plus selected optional modules only.
- Limited `mango add` rewriting to CLI-managed integration files.
- Updated generated PMO baseline with page detail regression requirements.

## Verification

- `node mango-ui/packages/mango-cli/scripts/check-cli.mjs`
- `pnpm --dir mango-ui -F mango-cli test`
- `git diff --check`

## Page Regression Rule Update

PMO frontend test rules now require screenshots, visible business content checks, shell/menu/topbar checks, style detail checks, critical interaction checks, console/network error review and single/micro frontend shape coverage when the change affects UI runtime.

## Not Verified

- This Sprint changed CLI generation and PMO rules. It did not start generated frontend/backend services or perform browser screenshots for a generated project.
- Future UI/runtime Sprints must follow the upgraded PMO rule and attach screenshots in evidence.
