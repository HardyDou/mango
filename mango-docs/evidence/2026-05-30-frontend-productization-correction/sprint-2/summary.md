# Sprint 2 Summary: Single-Source Admin Shell Extraction

## Status

`ACCEPTED`

Sprint 2 code, build verification, real-backend E2E screenshot capture, screenshot review and user manual acceptance are complete.

Latest correction note:

- `http://a.mango.io:5176/#/home` previously rendered `404` because the shell-generated home route uses `moduleCode: mango-shell` and `component: home/index`, but the runtime only registered the default `404` page.
- The shell-owned home page is now registered by `@mango/admin-shell`, and `apps/mango-admin` consumes the same home component through the package subpath export.
- `开发中心` previously showed in the shell top menu in dev mode while its component demo pages still lived under `apps/mango-admin/src/views/demo/components`; `mango-admin-shell` could not register those app-private page loaders, so its child pages rendered `404`.
- The development-center component demo pages now live under `@mango/admin-shell`, and both `mango-admin` and `mango-admin-shell` register them through the package.
- Micro frontend runtime regression has been rerun on the real shell at `http://a.mango.io:5176`.

## Goal

Make `@mango/admin-shell` the single source for the original Mango Admin main shell, and make `apps/mango-admin` consume that package instead of keeping its own long-term shell copy.

## Implemented

- Moved original Mango Admin layout ownership into `mango-ui/packages/admin-shell/src/layout`.
- Exported `MangoAdminLayout` and `MangoAdminParentView` from `@mango/admin-shell`.
- Updated `apps/mango-admin` route and menu config to use `@mango/admin-shell`.
- Converted app-side shell stores to thin re-exports from `@mango/admin-shell`.
- Removed app-side duplicate `src/layout/**`.
- Added `@mango/notice` as an `admin-shell` dependency so the original notice bell remains available.
- Updated E2E tests to use real tenant-aware login and Sprint 0 menu baseline.
- Added shell-owned home page registration for `mango-shell/home/index` so the runtime outlet does not fall through to `404`.
- Updated `apps/mango-admin` home view to consume the package-owned home component instead of keeping a separate source copy.
- Moved development-center component demo pages from `apps/mango-admin` to `packages/admin-shell`.
- Added package-owned development page registration so shell-generated development menus and page loaders have the same owner.

## Verification Commands

```bash
pnpm install --frozen-lockfile
pnpm -F @mango/admin-shell build
pnpm -F @mango/admin-shell test
pnpm -F mango-admin build
pnpm -F mango-admin-shell build
PLAYWRIGHT_BASE_URL=http://127.0.0.1:8490 PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true pnpm exec playwright test --config playwright.config.ts --project=chromium e2e/specs/layout.spec.ts e2e/specs/menu-navigation.spec.ts e2e/specs/theme.spec.ts
MANGO_FRONTEND_URL=http://127.0.0.1:8490 MANGO_BACKEND_URL=http://127.0.0.1:18800 node mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-2/sprint2-admin-shell-e2e.mjs
MANGO_MICRO_FRONTEND_URL=http://a.mango.io:5176 node mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-2/micro-frontend-verification/verify-after-home-fix.mjs
MANGO_MICRO_FRONTEND_URL=http://a.mango.io:5176 node mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-2/micro-frontend-verification/verify-dev-center.mjs
PLAYWRIGHT_BASE_URL=http://a.mango.io:5176 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18800 PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_RBAC_ENTRY=http://b.mango.io:5181/ PLAYWRIGHT_WORKFLOW_ENTRY=http://c.mango.io:5182/ pnpm -F mango-admin-shell test:e2e -- --config playwright.config.ts --project=chromium e2e/specs/runtime-composition.spec.ts
node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/plans/2026-05-30-mango-frontend-productization-correction-plan.md --ledger mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-2/delivery-ledger.md --mode verify --require "Single Source Admin Shell,Verification Contract"
```

## Verification Results

- `pnpm install --frozen-lockfile`: passed.
- `pnpm -F @mango/admin-shell build`: passed.
- `pnpm -F @mango/admin-shell test`: passed, `3 passed`.
- `pnpm -F mango-admin build`: passed.
- `pnpm -F mango-admin-shell build`: passed.
- Real backend health: `UP`.
- Real backend E2E regression: `18 passed / 2 skipped`.
- Micro frontend home/menu-package E2E after home fix: passed; screenshots and JSON report saved.
- Micro frontend development-center E2E: passed; sampled `文件上传`, `验证码`, `实时通信`; screenshots and JSON report saved.
- Micro frontend runtime composition regression: passed, `6 passed`.
- Screenshot capture: passed, 17 screenshots saved.
- Browser page errors: `0`.
- Network failures in 8490 single-app regression: `0`.
- Network failures in 5176 post-fix micro check: one realtime SSE probe returned `net::ERR_ABORTED`; page rendering, menu API and runtime checks still passed. This remains a non-blocking risk to inspect outside the Sprint 2 shell extraction scope.
- Console warnings: 2 non-blocking meta/header warnings.
- Sprint 2 delivery ledger check: passed, `8` rows, `5 DONE`, `3 EXCEPTION`, `0` incomplete.

## Screenshot Review

Manual screenshot review was performed against Sprint 0 baseline.

- Login page keeps the Mango Admin split card and blue/purple background.
- Home page keeps the original blue top bar, white left menu, tags view and card-based dashboard.
- Top-right search, fullscreen, notice bell, settings and user/tenant area are present.
- Notice bell popover renders expected empty state and links.
- User dropdown renders tenant, personal center, change password and logout.
- Settings drawer renders original layout/theme controls.
- Micro frontend shell at `http://a.mango.io:5176` renders the same original Mango blue top bar, left menu, TagsView and top-right tools.
- Micro frontend home page no longer renders `404`.
- Micro frontend `系统管理 / 权限管理 / 套餐管理` page renders real table rows such as `平台管理套餐` and `机构协同套餐`.
- Micro frontend `开发中心` child pages no longer render `404`; sampled pages render package-owned development demo pages inside the original Mango shell.
- Sampled first-level menus render inside the same Mango shell:
  - `系统管理`: `权限管理`, `应用管理`, `字典管理`
  - `审批中心`: `流程办理`, `流程管理`, `业务示例`
  - `平台能力`: `日历管理`, `编号规则`, `文件管理`
  - `通知中心`: `我的消息`, `消息配置`, `发送任务`

## Screenshot Paths

- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-2/screenshots/login-1440x960.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-2/screenshots/home-1440x960.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-2/screenshots/notice-bell-1440x960.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-2/screenshots/user-dropdown-1440x960.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-2/screenshots/settings-drawer-1440x960.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-2/screenshots/sample-系统管理-权限管理.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-2/screenshots/sample-系统管理-应用管理.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-2/screenshots/sample-系统管理-字典管理.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-2/screenshots/sample-审批中心-流程办理.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-2/screenshots/sample-审批中心-流程管理.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-2/screenshots/sample-审批中心-业务示例.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-2/screenshots/sample-平台能力-日历管理.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-2/screenshots/sample-平台能力-编号规则.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-2/screenshots/sample-平台能力-文件管理.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-2/screenshots/sample-通知中心-我的消息.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-2/screenshots/sample-通知中心-消息配置.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-2/screenshots/sample-通知中心-发送任务.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-2/micro-frontend-verification/screenshots/micro-home-after-home-fix-1440x960.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-2/micro-frontend-verification/screenshots/micro-rbac-menu-package-after-home-fix-1440x960.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-2/micro-frontend-verification/screenshots/micro-dev-center-upload-1440x960.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-2/micro-frontend-verification/screenshots/micro-dev-center-captcha-1440x960.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-2/micro-frontend-verification/screenshots/micro-dev-center-realtime-1440x960.png`

## Known Notes

- `开发中心` appears in Sprint 0 baseline and Sprint 2 screenshots. It was not introduced by Sprint 2. User confirmed the intended rule: configurable, visible by default in dev/test and hidden in prod. Sprint 3 must implement and verify the final menu contract.
- `接收设置` exists in backend menu data but is not directly visible in the first visible left-menu set at 1440x960. Sprint 3 must reconcile menu visibility and hidden/overflow behavior.
- `createMangoAdminApp` exists and is used by `apps/mango-admin-shell`; Sprint 4 still needs to stabilize the broader package export/style contract for npm consumers.
- The 5176 post-fix report captured one realtime SSE probe abort. It did not block page/menu/runtime verification, but it is not counted as a completed realtime transport validation.

## Delivery Ledger

- `mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-2/delivery-ledger.md`
