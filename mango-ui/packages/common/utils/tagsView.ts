import type { LocationParams, LocationQuery, LocationQueryRaw, RouteMeta, RouteRecordRaw } from 'vue-router';

export const HOME_TAG_PATH = '/home';

export interface MangoTagRoute {
  path: string;
  name?: string | symbol;
  query?: LocationQuery | LocationQueryRaw;
  params?: LocationParams;
  hash?: string;
  meta?: RouteMeta;
  tabKey: string;
}

export type MangoTagRouteInput = {
  path: string;
  name?: string | symbol;
  query?: LocationQuery | LocationQueryRaw;
  params?: LocationParams;
  hash?: string;
  meta?: RouteMeta;
  tabKey?: string;
};

export function createHomeTag(): RouteRecordRaw {
  return createTagSnapshot({
    path: HOME_TAG_PATH,
    name: 'Home',
    meta: {
      title: '首页',
      icon: 'HomeFilled',
      isAffix: true,
    },
  });
}

export function isHomeTag(tag: { path?: string } | undefined): boolean {
  return tag?.path === HOME_TAG_PATH;
}

export function createTabKey(route: Pick<MangoTagRouteInput, 'path' | 'name' | 'query' | 'params' | 'hash'>): string {
  const routeIdentity = {
    path: route.path || '',
    query: route.query || {},
    params: route.params || {},
    hash: route.hash || '',
  };
  return `${routeIdentity.path}::${stableSerialize(routeIdentity)}`;
}

export function createTagSnapshot(route: MangoTagRouteInput): MangoTagRoute {
  const snapshot = {
    path: route.path,
    name: route.name,
    query: route.query ? { ...route.query } : undefined,
    params: route.params ? { ...route.params } : undefined,
    hash: route.hash,
    meta: route.meta ? { ...route.meta } : undefined,
  } as MangoTagRoute;
  snapshot.tabKey = route.tabKey || createTabKey(snapshot);
  return snapshot;
}

export function getTagKey(
  tag: Pick<MangoTagRouteInput, 'path' | 'name' | 'query' | 'params' | 'hash' | 'tabKey'>,
): string {
  return tag.tabKey || createTabKey(tag);
}

export function isSameTag(
  left: Pick<MangoTagRouteInput, 'path' | 'name' | 'query' | 'params' | 'hash' | 'tabKey'> | undefined,
  right: Pick<MangoTagRouteInput, 'path' | 'name' | 'query' | 'params' | 'hash' | 'tabKey'> | undefined,
): boolean {
  return Boolean(left && right && getTagKey(left) === getTagKey(right));
}

export function normalizeTagsViewRoutes(tags: Array<RouteRecordRaw | MangoTagRoute> = []): MangoTagRoute[] {
  const source = Array.isArray(tags) ? tags : [];
  const persistedHome = source.find(isHomeTag);
  const homeTag = createTagSnapshot({
    ...createHomeTag(),
    ...(persistedHome || {}),
    tabKey: undefined,
    path: HOME_TAG_PATH,
    meta: {
      ...createHomeTag().meta,
      ...(persistedHome?.meta || {}),
      isAffix: true,
    },
  });

  const deduped = new Map<string, MangoTagRoute>();
  source
    .filter((tag) => tag.path && !isHomeTag(tag))
    .forEach((tag) => {
      const normalized = createTagSnapshot({ ...(tag as MangoTagRouteInput), tabKey: undefined });
      if (!deduped.has(normalized.tabKey)) {
        deduped.set(normalized.tabKey, normalized);
      }
    });

  return [homeTag, ...deduped.values()];
}

function stableSerialize(value: unknown): string {
  if (value === undefined) {
    return 'undefined';
  }
  if (value === null || typeof value !== 'object') {
    return JSON.stringify(value);
  }
  if (Array.isArray(value)) {
    return `[${value.map(stableSerialize).join(',')}]`;
  }
  const entries = Object.entries(value as Record<string, unknown>)
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([key, item]) => `${JSON.stringify(key)}:${stableSerialize(item)}`);
  return `{${entries.join(',')}}`;
}
