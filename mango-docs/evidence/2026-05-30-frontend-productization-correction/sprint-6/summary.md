# Sprint 6 Summary

## Status

`DONE`

Sprint 6 delivered the first usable `mango-cli` full preset and verified the path from Nexus-published CLI to an independently generated Mango full project.

## Completed

- Added `mango-ui/packages/mango-cli` as a publishable npm package.
- Added CLI bins:
  - `mango init <project> --preset full --topology monolith`
  - `mango-cli init <project> --preset full --topology monolith`
- Generated full project includes:
  - `frontend/` consuming `@mango/admin/full` and `@mango/admin/style-full.css`.
  - `backend/` consuming `io.mango:mango-admin-starter`.
  - `business-pmo/mango-baseline`.
  - topology docs for monolith and microservice.
- Fixed npm package publishing behavior for generated `.npmrc`:
  - Direct template `.npmrc` was ignored by npm packaging.
  - Replaced it with `npmrc.template`; CLI renders it to `frontend/.npmrc`.
- Published and verified `mango-cli@1.0.6` from Nexus group registry.
- Published `@mango/admin-shell@1.0.4` to fix `@mango/file` optional peer metadata as a boolean, and published `@mango/admin@1.0.7` to consume the corrected shell package.
- Verified generated project frontend install and production build from Nexus package dependencies.
- Verified generated backend health and real login/menu APIs against local MySQL.
- Captured screenshot evidence for login page and logged-in home/menu.

## Current Result

- `mango-cli@1.0.6` is the current usable version.
- `mango-cli@1.0.2` was published but generated projects missed `.npmrc` after package install.
- `mango-cli@1.0.3` fixed `.npmrc` generation but still had stale fallback versions for optional packages when running outside the monorepo.
- `mango-cli@1.0.4` fixed both `.npmrc` and fallback version issues.
- `mango-cli@1.0.5` additionally fixed generated `.gitignore` packaging.
- `mango-cli@1.0.6` uses `@mango/admin@1.0.7` and `@mango/admin-shell@1.0.4`, keeping published npm metadata and generated project dependencies aligned.

## Acceptance Addresses

- Frontend: `http://127.0.0.1:5186/`
- Backend health: `http://127.0.0.1:5555/actuator/health`
- Running frontend directory during final verification: `/tmp/mango-cli-nexus-verify-106/nexus-generated-full/frontend`
- Evidence project directory: `mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-6/generated-full-app`

## Evidence

- `mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-6/generated-full-app`
- `mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-6/screenshots/nexus-full-app-login-verified.png`
- `mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-6/screenshots/nexus-full-app-home-verified.png`
- `mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-6/layout-report.json`
- `mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-6/api-report.json`
- `mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-6/commands.log`

## Risks

- First page load before login still attempts to initialize shell menu and receives a 401 from `/authorization/menus/user`; after real login, menu API returns `系统管理`, `审批中心`, `平台能力`, `通知中心`.
- Generated frontend production build has large chunk warnings. This is not a Sprint 6 blocker, but it is a performance follow-up.
- Backend startup has existing schema validation warnings from broader platform tables. Health and verified APIs are `UP`, but schema cleanup remains separate work.

## Next

Sprint 7 should extend `mango-cli` from full preset to optional module selection and business extension commands, without weakening the current one-command full preset path.
