import { defineStore } from 'pinia';
import type { PiniaPluginContext } from 'pinia';
import type { RouteRecordRaw } from 'vue-router';
import { Session } from '@mango/common/utils/storage';
import {
  createTagSnapshot,
  getTagKey,
  normalizeTagsViewRoutes,
  type MangoTagRoute,
  type MangoTagRouteInput,
} from '@mango/common/utils/tagsView';
import { resetPersistedTagsView } from '../runtime/tagsViewSession';

export const useTagsViewRoutes = defineStore('tagsViewRoutes', {
  state: (): {
    tagsViewRoutes: MangoTagRoute[];
    isTagsViewCurrenFull: boolean;
    favoriteRoutes: MangoTagRoute[];
  } => ({
    tagsViewRoutes: [],
    isTagsViewCurrenFull: false,
    favoriteRoutes: [],
  }),
  actions: {
    setTagsViewRoutes(data: Array<RouteRecordRaw | MangoTagRoute>) {
      this.tagsViewRoutes = normalizeTagsViewRoutes(data);
    },
    upsertTag(data: MangoTagRouteInput) {
      const tag = createTagSnapshot(data);
      const index = this.tagsViewRoutes.findIndex((item) => getTagKey(item) === tag.tabKey);
      if (index < 0) {
        this.tagsViewRoutes = normalizeTagsViewRoutes([...this.tagsViewRoutes, tag]);
        return tag;
      }
      const current = this.tagsViewRoutes[index];
      this.tagsViewRoutes.splice(index, 1, createTagSnapshot({ ...current, ...tag, tabKey: current.tabKey }));
      return this.tagsViewRoutes[index];
    },
    updateTagTitle(data: Pick<MangoTagRoute, 'tabKey' | 'meta' | 'name'>) {
      const index = this.tagsViewRoutes.findIndex((item) => item.tabKey === data.tabKey);
      if (index < 0) {
        return;
      }
      const current = this.tagsViewRoutes[index];
      this.tagsViewRoutes.splice(
        index,
        1,
        createTagSnapshot({
          ...current,
          name: data.name || current.name,
          meta: { ...current.meta, ...data.meta },
          tabKey: current.tabKey,
        }),
      );
    },
    setCurrenFullscreen(bool: boolean) {
      Session.set('isTagsViewCurrenFull', bool);
      this.isTagsViewCurrenFull = bool;
    },
    addFavoriteRoutes(item: RouteRecordRaw) {
      this.favoriteRoutes.unshift(createTagSnapshot(item as MangoTagRouteInput));
    },
    delFavoriteRoutes(item: MangoTagRouteInput) {
      const itemKey = getTagKey(item);
      const idx = this.favoriteRoutes.findIndex((favorite) => getTagKey(favorite) === itemKey);
      if (idx > -1) this.favoriteRoutes.splice(idx, 1);
    },
    clearTagsView() {
      this.tagsViewRoutes = normalizeTagsViewRoutes([]);
      this.isTagsViewCurrenFull = false;
      resetPersistedTagsView();
    },
  },
  persist: {
    enabled: true,
    afterRestore: ({ store }: PiniaPluginContext) => {
      store.tagsViewRoutes = normalizeTagsViewRoutes(store.tagsViewRoutes as RouteRecordRaw[]);
    },
    strategies: [
      {
        key: 'mango-tags-view-routes',
        storage: localStorage,
      },
    ],
  },
});
