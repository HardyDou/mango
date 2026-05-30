import type { RouteRecordRaw } from 'vue-router';

export const HOME_TAG_PATH = '/home';

export function createHomeTag(): RouteRecordRaw {
  return {
    path: HOME_TAG_PATH,
    name: 'Home',
    meta: {
      title: '首页',
      icon: 'HomeFilled',
      isAffix: true,
    },
  } as unknown as RouteRecordRaw;
}

export function isHomeTag(tag: Pick<RouteRecordRaw, 'path'> | undefined): boolean {
  return tag?.path === HOME_TAG_PATH;
}

export function normalizeTagsViewRoutes(tags: RouteRecordRaw[]): RouteRecordRaw[] {
  const source = Array.isArray(tags) ? tags : [];
  const homeTag = {
    ...createHomeTag(),
    ...(source.find(isHomeTag) || {}),
    path: HOME_TAG_PATH,
    meta: {
      ...createHomeTag().meta,
      ...(source.find(isHomeTag)?.meta || {}),
      isAffix: true,
    },
  } as RouteRecordRaw;

  const deduped = new Map<string, RouteRecordRaw>();
  source
    .filter(tag => tag.path && !isHomeTag(tag))
    .forEach(tag => {
      if (!deduped.has(tag.path)) {
        deduped.set(tag.path, tag);
      }
    });

  return [homeTag, ...deduped.values()];
}
