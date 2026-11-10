import { getMangoNoticeNavigationProvider } from '@mango/admin-pages/notice';
import type { RouteLocationRaw, Router } from 'vue-router';

const NOTICE_TARGET_PATHS: Readonly<Record<string, string>> = {
  'notice:receive-setting': '/message-center/receive-setting',
  'notice:site-message': '/profile',
  'notice:announcement-user': '/profile',
  'account:profile': '/profile',
  'account:password': '/password',
};

/** 静态目标自带的固定查询参数；消息参数只能补充，不能覆盖。 */
const NOTICE_TARGET_FIXED_QUERY: Readonly<Record<string, Record<string, string>>> = {
  'notice:site-message': { tab: 'notice-site-message' },
  'notice:announcement-user': { tab: 'notice-announcement-user' },
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
  const fixedQuery = NOTICE_TARGET_FIXED_QUERY[targetKey];
  if (mappedPath) return { path: mappedPath, query: { ...query, ...fixedQuery } };
  if (isSafeNoticePath(targetKey)) {
    const provider = getMangoNoticeNavigationProvider();
    if (provider) {
      const resolvedPath = provider.resolveTargetPath(targetKey);
      return resolvedPath ? { path: resolvedPath, query } : undefined;
    }
    const resolved = router.resolve({ path: targetKey });
    const accessible = resolved.matched.some((record) => !record.path.includes(':pathMatch'));
    return accessible ? { path: targetKey, query } : undefined;
  }
  const provider = getMangoNoticeNavigationProvider();
  const resolvedPath = provider?.resolveTargetPath(targetKey);
  if (resolvedPath) return { path: resolvedPath, query };
  return router.hasRoute(targetKey) ? { name: targetKey, query } : undefined;
}

export function noticeFallbackTargetKey(params?: Record<string, unknown>) {
  const value = params?.fallbackTargetKey;
  return typeof value === 'string' && value.trim() ? value.trim() : undefined;
}

/** 消息中心的固定目标键；平台兜底使用，不依赖菜单授权。 */
export const NOTICE_MESSAGE_CENTER_TARGET_KEY = 'notice:site-message';

export type NoticeTargetResolution = 'primary' | 'fallback' | 'message-center' | 'none';

/**
 * 按主目标 → `fallbackTargetKey` → 消息中心 的顺序解析并打开消息目标。
 * 解析不到任何可用页面时返回 `none`，由调用方提示原因，不再直接死路。
 */
export async function openNoticeTarget(
  router: Router,
  targetKey: string,
  params?: Record<string, unknown>,
): Promise<NoticeTargetResolution> {
  const fallbackTargetKey = noticeFallbackTargetKey(params);
  const primary = resolveNoticeTargetLocation(router, targetKey, params);
  if (primary) {
    try {
      await router.push(primary);
      return 'primary';
    } catch (error) {
      console.warn('Notice target navigation failed', targetKey, error);
    }
  }
  const fallback =
    fallbackTargetKey && fallbackTargetKey !== targetKey
      ? resolveNoticeTargetLocation(router, fallbackTargetKey, params)
      : undefined;
  if (fallback) {
    try {
      await router.push(fallback);
      return primary ? 'primary' : 'fallback';
    } catch (error) {
      console.warn('Notice fallback navigation failed', fallbackTargetKey, error);
    }
  }
  const center =
    targetKey === NOTICE_MESSAGE_CENTER_TARGET_KEY
      ? undefined
      : resolveNoticeTargetLocation(router, NOTICE_MESSAGE_CENTER_TARGET_KEY, params);
  if (center) {
    const centerPath = typeof center === 'object' && 'path' in center ? center.path : undefined;
    if (centerPath && centerPath === router.currentRoute.value.path) {
      return 'none';
    }
    try {
      await router.push(center);
      return 'message-center';
    } catch (error) {
      console.warn('Notice message center navigation failed', error);
    }
  }
  return 'none';
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
