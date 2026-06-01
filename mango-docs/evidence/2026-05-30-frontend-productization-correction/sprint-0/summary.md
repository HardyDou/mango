# Sprint 0 Baseline Summary

## Scope

- Worktree: `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0`
- Branch: `worktree/frontend-productization-correction-sprint-0`
- Frontend: `http://127.0.0.1:8490`
- Backend: `http://127.0.0.1:18800`
- Database: real development MySQL through the local backend.

Sprint 0 only locks the baseline. It does not implement package extraction, menu fixes, shell extraction or starter changes.

## Verification Commands

```bash
git status --short --branch
pnpm install --frozen-lockfile
pnpm -F mango-admin build
curl -s http://127.0.0.1:18800/actuator/health
curl -s -I http://127.0.0.1:8490/
MANGO_FRONTEND_URL=http://127.0.0.1:8490 MANGO_BACKEND_URL=http://127.0.0.1:18800 node mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-0/sprint0-baseline-e2e.mjs
```

## Results

- `pnpm install --frozen-lockfile`: passed.
- `pnpm -F mango-admin build`: passed.
- Backend health: HTTP 200, status `UP`, MySQL `UP`.
- Frontend root: HTTP 200.
- Real login: `admin / admin123`, tenant `芒果集团`, login API success with access token.
- Backend menu API: HTTP 200, success, 4 first-level menus, 218 flattened rows.
- E2E page errors: 0.
- E2E network failures: 0.
- Browser console errors: 2 CSP/X-Frame-Options meta warnings.

## Menu Baseline

Backend first-level menus:

- `系统管理`
- `审批中心`
- `平台能力`
- `通知中心`

Visible top navigation in browser:

- `首页`
- `系统管理`
- `审批中心`
- `平台能力`
- `通知中心`
- `开发中心`

Observation: browser top navigation contains `开发中心`, but the backend menu API baseline report contains 4 first-level menus and does not include `开发中心`. This is a Sprint 0 baseline finding and must be resolved or explicitly accepted in a later menu-contract Sprint. It is not silently treated as correct full-mode behavior.

## Screenshot Review

Visual review completed from saved screenshots:

- Login page shows original Mango split login card, blue-purple background, `Mango Admin`, `Mango 管理平台`, tenant selector and login fields.
- Home page shows original blue top navigation, left side menu, tags view, notice bell, settings button and user/tenant area.
- User dropdown shows tenant `芒果集团`, personal center, change password and logout entries.
- Settings drawer shows `布局配置` drawer with layout/theme/menu/interface controls.
- Menu sampling screenshots are not covered by drawer overlays after the final run.
- Sampled pages show original Mango admin layout with left menu, top tabs, search areas, tables/cards/pagination where applicable.

## Sampled Menus

Every backend first-level menu was sampled with 1 to 3 child pages:

- `系统管理`: `权限管理`, `应用管理`, `字典管理`
- `审批中心`: `流程办理`, `流程管理`, `业务示例`
- `平台能力`: `日历管理`, `编号规则`, `文件管理`
- `通知中心`: `我的消息`, `消息配置`, `发送任务`

All sampled pages:

- page title or target text visible.
- no visible 401, 403, 404, access denied or route-load failure.

## Screenshot Paths

- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-0/screenshots/login-1440x960.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-0/screenshots/home-1440x960.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-0/screenshots/user-dropdown-1440x960.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-0/screenshots/settings-drawer-1440x960.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-0/screenshots/sample-系统管理-权限管理.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-0/screenshots/sample-系统管理-应用管理.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-0/screenshots/sample-系统管理-字典管理.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-0/screenshots/sample-审批中心-流程办理.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-0/screenshots/sample-审批中心-流程管理.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-0/screenshots/sample-审批中心-业务示例.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-0/screenshots/sample-平台能力-日历管理.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-0/screenshots/sample-平台能力-编号规则.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-0/screenshots/sample-平台能力-文件管理.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-0/screenshots/sample-通知中心-我的消息.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-0/screenshots/sample-通知中心-消息配置.png`
- `/Users/hardy/Work/mango/.mango/worktrees/frontend-productization-correction-sprint-0/mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-0/screenshots/sample-通知中心-发送任务.png`

## Risks And Open Items

- Browser top navigation has `开发中心` while backend menu report has no `开发中心`; this must be handled in Sprint 3 menu contract, not ignored.
- Console contains 2 warnings about CSP/X-Frame-Options delivered via meta. No page crash or network failure was observed.
- Sprint 0 evidence proves baseline capture only. It does not prove package reuse, starter generation or single-source shell extraction.
