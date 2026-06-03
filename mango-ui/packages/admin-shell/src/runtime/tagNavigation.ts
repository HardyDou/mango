import type { RouteLocationRaw, RouteRecordRaw } from 'vue-router';
import { HOME_TAG_PATH, isHomeTag } from '@mango/common/utils/tagsView';
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

export function resolveClosedTagFallback(
  tags: RouteRecordRaw[],
  closedTag: Pick<RouteRecordRaw, 'path'>,
  activePath: string,
): RouteLocationRaw | undefined {
  if (!closedTag?.path || closedTag.path !== activePath) {
    return undefined;
  }
  const closedIndex = tags.findIndex(tag => tag.path === closedTag.path);
  const remaining = tags.filter(tag => tag.path !== closedTag.path);
  if (remaining.length === 0) {
    return { path: HOME_TAG_PATH };
  }
  const previous = findTag(remaining, Math.min(closedIndex - 1, remaining.length - 1), -1);
  if (previous) {
    return resolveTagLocation(previous);
  }
  const next = findTag(remaining, Math.max(closedIndex, 0), 1);
  return resolveTagLocation(next || remaining.find(isHomeTag) || remaining[0]);
}

function findTag(tags: RouteRecordRaw[], startIndex: number, step: 1 | -1) {
  for (let index = startIndex; index >= 0 && index < tags.length; index += step) {
    const tag = tags[index];
    if (tag?.path) {
      return tag;
    }
  }
  return undefined;
}
