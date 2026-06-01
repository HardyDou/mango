# Sprint 5 Summary

## Status

`DONE`

Sprint 5 proved that Mango frontend packages can be consumed by an independent business app and that optional capability packages are only enabled when the app explicitly installs and registers them.

## Completed

- Audited frontend package metadata for public entry, type entry, style entry, peer dependencies and workspace dependency leakage.
- Verified clean consumer install/build paths with packed packages and Nexus packages.
- Published and verified `@mango/admin@1.0.3` from Nexus after fixing its public type contract.
- Added package-level feature registration so optional modules do not leak into apps that only use core admin.
- Added package hidden route registration for local package pages that are not visible backend menus.
- Moved workflow custom apply route ownership into `@mango/workflow`; it now registers `/workflow/custom-apply` as a hidden `LOCAL_ROUTE`.
- Kept workflow business example forms in `@mango/workflow-business-example`; consumer apps must explicitly import `registerMangoWorkflowBusinessExampleAdminPages`.
- Fixed local package route mounting so a runtime-config load failure only blocks actual micro routes, not local package pages.
- Hardened feature-selection E2E with `--strictPort`, dev-server shutdown waits and screenshot readiness waits.
- Verified three consumer modes with screenshots:
  - core: `@mango/admin` only.
  - workflow-only: `@mango/admin` + `@mango/workflow` + `@mango/workflow-business-example`.
  - full: `@mango/admin/full` aggregation.

## Current Result

- Core consumer menus: `首页`, `系统管理`, `开发中心`.
- Workflow-only consumer menus: `首页`, `系统管理`, `审批中心`, `开发中心`.
- Workflow-only consumer does not show `平台能力` or `通知中心`.
- Full consumer menus: `首页`, `系统管理`, `审批中心`, `平台能力`, `通知中心`, `开发中心`.
- Workflow custom apply page can open `费用报销申请` and activate the `申请报销` example form from `@mango/workflow-business-example`.
- Screenshot review confirmed the latest evidence has no login toast遮挡 and no global runtime-config error toast.

## Evidence

- `mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-5/packed-consumer-report.json`
- `mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-5/nexus-consumer-report.json`
- `mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-5/nexus-runtime-e2e-report.json`
- `mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-5/feature-selection-report.json`
- `mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-5/screenshots/s5-feature-core.png`
- `mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-5/screenshots/s5-feature-workflow-only.png`
- `mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-5/screenshots/s5-feature-full.png`

## Next

1. Sprint 6 can start `mango-cli init` on top of the verified package-consumption path.
2. CLI must consume the same package contract: required core/system modules by default, optional modules by explicit selection.
3. Keep credential storage outside the repository: user-level npm config or CI Secret only.
