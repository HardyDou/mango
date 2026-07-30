import type { RouteLocationRaw, Router } from 'vue-router';

const NOTICE_TARGET_PATHS: Readonly<Record<string, string>> = {
  'notice:receive-setting': '/message-center/receive-setting',
  'notice:announcement-user': '/message-center/announcement',
  'account:profile': '/profile',
  'account:password': '/password',
};

const NAVIGATION_META_KEYS = new Set([
  'fallbackTargetKey',
  'bizType',
  'bizGroup',
  'bizName',
  'messageScene',
  'messageId',
  'actionCode',
  'clientIp',
  'clientIP',
  'ipAddress',
  'accessToken',
  'refreshToken',
  'password',
  'token',
]);

export function resolveNoticeTargetPath(targetKey: string): string | undefined {
  return NOTICE_TARGET_PATHS[targetKey];
}

export function resolveNoticeTargetLocation(
  router: Router,
  targetKey: string,
  params?: Record<string, unknown>,
): RouteLocationRaw | undefined {
  const query = normalizeNoticeQuery(params);
  const mappedPath = resolveNoticeTargetPath(targetKey);
  if (mappedPath) return { path: mappedPath, query };
  if (isSafeNoticePath(targetKey)) {
    const resolved = router.resolve({ path: targetKey });
    const accessible = resolved.matched.some((record) => !record.path.includes(':pathMatch'));
    return accessible ? { path: targetKey, query } : undefined;
  }
  return router.hasRoute(targetKey) ? { name: targetKey, query } : undefined;
}

export function noticeFallbackTargetKey(params?: Record<string, unknown>) {
  const value = params?.fallbackTargetKey;
  return typeof value === 'string' && value.trim() ? value.trim() : undefined;
}

export function normalizeNoticeQuery(params?: Record<string, unknown>) {
  return Object.fromEntries(
    Object.entries(params || {})
      .filter(([key, value]) => !NAVIGATION_META_KEYS.has(key) && isQueryValue(value))
      .map(([key, value]) => [key, Array.isArray(value) ? value.map(String) : String(value)]),
  );
}

export function isSafeNoticePath(path: string) {
  return (
    path.startsWith('/') &&
    !path.startsWith('//') &&
    !path.includes('\\') &&
    !path.includes('?') &&
    !path.includes('#') &&
    !Array.from(path).some((character) => character.charCodeAt(0) < 32 || character.charCodeAt(0) === 127) &&
    !/^[a-z][a-z0-9+.-]*:/i.test(path)
  );
}

function isQueryValue(value: unknown): value is string | number | boolean | Array<string | number | boolean> {
  if (['string', 'number', 'boolean'].includes(typeof value)) return true;
  return Array.isArray(value) && value.every((item) => ['string', 'number', 'boolean'].includes(typeof item));
}
