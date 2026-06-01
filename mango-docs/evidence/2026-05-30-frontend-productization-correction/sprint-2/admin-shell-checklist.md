# Sprint 2 Admin Shell Checklist

Status: `ACCEPTED`

## 1. Single Source

- [x] `apps/mango-admin` does not keep a long-term independent main shell layout.
- [x] `packages/admin-shell` is the single implementation owner, or both app and package share the same source entry.
- [x] There are not two different implementations of layout, header, aside, nav bars, tags view, settings drawer or user menu.
- [x] `admin-shell` does not depend on `apps/mango-admin`.
- [x] `apps/mango-admin` consumes `admin-shell` through package APIs or shared package-owned source.

## 2. Original Mango Shell Parity

- [x] Login page matches the Sprint 0 baseline.
- [x] Home shell matches the Sprint 0 baseline.
- [x] Top bar style matches the Sprint 0 baseline.
- [x] Left menu style matches the Sprint 0 baseline.
- [x] Tags view style and behavior match the Sprint 0 baseline.
- [x] Notice bell exists and matches expected position and style.
- [x] User dropdown exists and matches expected menu items and style.
- [x] Settings drawer exists and exposes the expected layout controls.
- [x] No temporary CSS skin is added to fake visual parity.

## 3. Menu And Routes

- [x] Default full mode uses the real backend menu source.
- [x] Business menus are not silently added by Sprint 2.
- [x] `开发中心` is recorded as a Sprint 0 existing top-bar entry, not silently treated as a new Sprint 2 feature.
- [x] Backend first-level menu and browser first-level menu can be reconciled.
- [x] Menu clicks load routes successfully for sampled pages.
- [x] Micro frontend `#/home` renders the shell-owned home page instead of `404`.
- [x] Micro frontend `#/system/menu-package` renders through the same Mango shell and shows real package rows.
- [x] Micro frontend development-center child pages render package-owned pages instead of `404`.
- [x] 404, access-denied and route-load failure states remain explicit.

## 4. Auth And Context

- [x] Real backend login is used.
- [x] `admin / admin123` can log in.
- [x] Tenant `芒果集团` is displayed.
- [x] Current user information is displayed.
- [ ] Logout works.
- [x] Personal center and change password entries exist.

## 5. Styles And Dependencies

- [x] Element Plus styles are fully loaded.
- [x] Mango theme styles are fully loaded.
- [x] `admin-shell` styles are fully loaded.
- [x] Consumer does not need deep internal CSS imports for `mango-admin`.
- [x] Full package style facade work is deferred to Sprint 4.

## 6. Package Boundary

- [ ] `admin-shell` exposes a stable startup API such as `createMangoAdminApp`.
- [x] `admin-shell` exposes shell components or runtime exports needed by the app.
- [x] `admin-shell` exposes the shell-owned home view through a package subpath consumed by `apps/mango-admin`.
- [x] `admin-shell` owns and registers development-center demo page loaders used by the shell-generated dev menu.
- [x] Public components do not depend on app-private stores.
- [x] Shell runtime does not hardcode business-system menus.
- [x] No mock, fake, dummy or hardcoded data is used as acceptance proof.

## 7. Build Verification

- [x] `pnpm -F @mango/admin-shell build` passes.
- [x] `pnpm -F @mango/admin-shell test` passes.
- [x] `pnpm -F mango-admin build` passes.
- [x] `pnpm -F mango-admin-shell build` passes.
- [x] `mango-admin-shell` runtime composition E2E passes on 5176 with b/c micro apps.
- [x] TypeScript has no new errors from the checked builds.
- [x] Browser console has no blocking page errors.

## 8. E2E Screenshot Verification

- [x] Login screenshot is saved.
- [x] Home screenshot is saved.
- [x] User dropdown screenshot is saved.
- [x] Settings drawer screenshot is saved.
- [x] Notice entry screenshot is saved.
- [x] Every first-level menu is sampled with 1 to 3 child pages or functions.
- [x] Micro frontend home screenshot after the home-route fix is saved.
- [x] Micro frontend menu-package screenshot after the home-route fix is saved.
- [x] Micro frontend development-center screenshots are saved for `文件上传`, `验证码` and `实时通信`.
- [x] Screenshot review checks layout, colors, spacing, menu, top tools and Mango elements against Sprint 0.
- [x] Reports include absolute screenshot paths.

## 9. Sprint 2 Manual Acceptance

- [x] User confirms this is not a newly written Mango-like shell.
- [x] User confirms the main shell is the original Mango Admin experience.
- [x] User confirms notice bell, user menu, settings drawer and tags view are normal.
- [x] User confirms Sprint 3 menu contract may start.

## 10. Known Non-Blocking Notes

- `开发中心` appears in Sprint 0 baseline top menu and Sprint 2 screenshots. It is not introduced by Sprint 2. User confirmed the intended rule: configurable, visible by default in dev/test and hidden in prod. Sprint 3 must implement and verify the final menu contract.
- `接收设置` exists in the backend menu report but is not directly visible in the first six left menu items at 1440x960. Sprint 0 did not sample it either. Sprint 3 must reconcile full menu visibility and overflow/hidden behavior.
- Browser console captured two non-blocking meta/header warnings for `X-Frame-Options` and `frame-ancestors`; there were no page errors or network failures.
- The 5176 post-fix micro verification captured one realtime SSE probe abort. It did not block home, menu-package, menu API or runtime composition verification, but realtime transport itself is not accepted by Sprint 2.
