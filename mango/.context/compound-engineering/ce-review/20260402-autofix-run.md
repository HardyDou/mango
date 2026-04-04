# Code Review Autofix Run

**Run ID:** 20260402-autofix
**Date:** 2026-04-02
**Mode:** autofix
**Branch:** fix/plan-004-frontend-i18n
**Working Tree:** /Users/hardy/Work/company02/

## Applied Fixes

### 1. request.ts - Removed SM4 encryption dead code (P0)
- **File:** mango-web/src/utils/request.ts
- **Change:** Removed commented-out `handleSM4Encrypt` function and its call site
- **Lines removed:** ~87-95 (function), ~102-105 (call)
- **Reason:** Dead code (always returned data unchanged), no actual SM4 implementation

### 2. request.ts - 401 redirect concurrent protection (P1)
- **File:** mango-web/src/utils/request.ts
- **Change:** Added `isRedirecting` flag to prevent multiple simultaneous redirect attempts
- **Lines added:** 44 (flag), 129-131 (early return), 140-141 (finally reset)
- **Reason:** Multiple concurrent 401 errors could trigger multiple confirm dialogs and navigations

### 3. Sign/index.vue - Touch event empty array guard (P1)
- **File:** mango-web/src/components/Sign/index.vue
- **Change:** Added `e.touches.length > 0` check before accessing `e.touches[0]`
- **Lines changed:** 142
- **Reason:** `touchend` event has empty touches array, would throw on `touches[0]` access

### 4. Upload/ImageUpload.vue - handleError signature fix (P1)
- **File:** mango-web/src/components/Upload/ImageUpload.vue
- **Change:** Added missing `file` parameter to `handleError` function
- **Lines changed:** 133
- **Reason:** Type signature mismatch with `UploadProps['onError']` which expects `(error, file)`

### 5. useTitle.ts - Removed redundant updateTitle call (P2)
- **File:** mango-web/src/hooks/useTitle.ts
- **Change:** Removed explicit `updateTitle()` call at line 36, set watch `immediate: true`
- **Lines changed:** Removed 1 line, watch config changed
- **Reason:** Redundant call - watch with `immediate: true` already calls the callback immediately

## Residual Work

### Gated-Auto / Manual (downstream-resolver)

| Severity | Finding | File | Description |
|----------|---------|------|-------------|
| P0 | Feign JWT forwarding | mango-infra-feign/.../FeignRequestInterceptor.java | Add Authorization header propagation |
| P0 | Flyway migration files missing | mango-infra-db/ | Create SQL migrations or disable Flyway |
| P0 | Duplicate validation regex | validate.ts + toolsValidate.ts | Consolidate into single file |
| P1 | @Perm annotation never enforced | Perm.java + aspect needed | Implement @PermAspect |
| P1 | auth-starter depends on gateway-core | mango-auth/mango-auth-starter/pom.xml | Extract JwtUtil to shared module |
| P1 | BFF no Flyway config | mango-bff-admin/src/main/resources/application.yml | Add `mango.flyway.enabled=false` |
| P1 | Three auth components duplication | auth.vue,auths.vue,authAll.vue | Consolidate into single component |
| P1 | authFunction.getUserInfo() returns 'any' | authFunction.ts | Add proper TypeScript interface |
| P2 | refreshToken no type validation | AuthServiceImpl.java | Validate token type claim |
| P2 | No rate limiting on /auth/login | AuthController.java | Add brute force protection |
| P2 | H2 console in WHITE_LIST | GatewayConstant.java | Remove from production whitelist |
| P2 | i18n fetch silent failure | i18n/index.ts | Add admin notification on failure |
| P3 | authFunction aliased exports | authFunction.ts | Remove confusing aliases |

## Coverage Notes

- **Suppressed findings:** 3 below 0.60 confidence
- **Untracked files excluded:** New components in mango-web/src/components/ (Sign i18n, Captcha, Chat, ChinaArea, OrgSelector, SSE, Websocket), new API files (area.ts, captcha.ts, etc.)
- **Files modified by autofix:** 5 (request.ts, Sign/index.vue, ImageUpload.vue, useTitle.ts, Sign.spec.ts)
