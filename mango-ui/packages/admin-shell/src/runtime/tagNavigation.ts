import type { RouteLocationRaw, RouteRecordRaw } from 'vue-router';
import { isRunnableMenu, type ShellRouteMenu } from './menuHost';

export function resolveTagLocation(tag: any, replace = false): RouteLocationRaw {
  return {
    path: tag.path,
    query: tag.query,
    hash: tag.hash,
    replace,
  } as RouteLocationRaw;
}

export function resolveFallbackLocation(routes: RouteRecordRaw[], excludePath?: string): string {
  const first = resolveFirstVisibleRoute(routes, excludePath);
  return first?.path || '/home';
}

export function resolveFirstVisibleRoute(routes: RouteRecordRaw[], excludePath?: string): RouteRecordRaw | undefined {
  for (const item of routes || []) {
    if (item.meta?.isHide || item.path === excludePath) {
      continue;
    }
    if (isRunnableMenu(item as ShellRouteMenu)) {
      return item;
    }
    const child = resolveFirstVisibleRoute((item.children || []) as RouteRecordRaw[], excludePath);
    if (child) {
      return child;
    }
  }
  return undefined;
}
