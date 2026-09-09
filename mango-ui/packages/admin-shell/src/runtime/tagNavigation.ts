import type { RouteLocationRaw, RouteRecordRaw } from 'vue-router';
import { HOME_TAG_PATH, getTagKey, isHomeTag, isSameTag, type MangoTagRouteInput } from '@mango/common/utils/tagsView';
import { isRunnableMenu, type ShellRouteMenu } from './menuHost';

export function normalizeRouteLocation(location: RouteLocationRaw): RouteLocationRaw {
  if (typeof location === 'string') {
    return location;
  }
  return {
    ...location,
    params: location.params || {},
    query: location.query || {},
  } as RouteLocationRaw;
}

export function resolveTagLocation(tag: MangoTagRouteInput, replace = false): RouteLocationRaw {
  const hasParams = Boolean(tag.params && Object.keys(tag.params).length > 0);
  if (tag.name && hasParams) {
    return normalizeRouteLocation({ name: tag.name, params: tag.params, query: tag.query, hash: tag.hash, replace });
  }
  return normalizeRouteLocation({ path: tag.path, params: tag.params, query: tag.query, hash: tag.hash, replace });
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
  tags: Array<RouteRecordRaw | MangoTagRouteInput>,
  closedTag: MangoTagRouteInput,
  activeRoute: MangoTagRouteInput | string,
): RouteLocationRaw | undefined {
  if (!closedTag?.path || !isActiveTag(closedTag, activeRoute)) {
    return undefined;
  }
  const closedKey = getTagKey(closedTag);
  const closedIndex = tags.findIndex((tag) => getTagKey(tag as MangoTagRouteInput) === closedKey);
  const remaining = tags.filter((tag) => getTagKey(tag as MangoTagRouteInput) !== closedKey);
  if (remaining.length === 0) {
    return normalizeRouteLocation({ path: HOME_TAG_PATH });
  }
  const previous = findTag(remaining, Math.min(closedIndex - 1, remaining.length - 1), -1);
  if (previous) {
    return resolveTagLocation(previous);
  }
  const next = findTag(remaining, Math.max(closedIndex, 0), 1);
  return resolveTagLocation(next || remaining.find(isHomeTag) || remaining[0]);
}

function isActiveTag(closedTag: MangoTagRouteInput, activeRoute: MangoTagRouteInput | string) {
  return typeof activeRoute === 'string' ? closedTag.path === activeRoute : isSameTag(closedTag, activeRoute);
}

function findTag(tags: Array<RouteRecordRaw | MangoTagRouteInput>, startIndex: number, step: 1 | -1) {
  for (let index = startIndex; index >= 0 && index < tags.length; index += step) {
    const tag = tags[index];
    if (tag?.path) {
      return tag;
    }
  }
  return undefined;
}
