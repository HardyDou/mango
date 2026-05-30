import { defineStore } from 'pinia';
import type { RouteRecordRaw } from 'vue-router';
import { Session } from '@mango/common/utils/storage';
import { normalizeTagsViewRoutes } from '@mango/common/utils/tagsView';

export const useTagsViewRoutes = defineStore('tagsViewRoutes', {
  state: (): {
    tagsViewRoutes: RouteRecordRaw[];
    isTagsViewCurrenFull: boolean;
    favoriteRoutes: RouteRecordRaw[];
  } => ({
    tagsViewRoutes: [],
    isTagsViewCurrenFull: false,
    favoriteRoutes: [],
  }),
  actions: {
    setTagsViewRoutes(data: RouteRecordRaw[]) {
      this.tagsViewRoutes = normalizeTagsViewRoutes(data);
    },
    setCurrenFullscreen(bool: boolean) {
      Session.set('isTagsViewCurrenFull', bool);
      this.isTagsViewCurrenFull = bool;
    },
    addFavoriteRoutes(item: RouteRecordRaw) {
      this.favoriteRoutes.unshift(item);
    },
    delFavoriteRoutes(item: RouteRecordRaw) {
      const idx = this.favoriteRoutes.indexOf(item);
      if (idx > -1) this.favoriteRoutes.splice(idx, 1);
    },
    clearTagsView() {
      this.tagsViewRoutes = normalizeTagsViewRoutes([]);
    },
  },
  persist: {
    enabled: true,
    strategies: [
      {
        key: 'mango-tags-view-routes',
        storage: localStorage,
      },
    ],
  },
});
