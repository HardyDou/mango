import { defineStore } from 'pinia';
import { Session } from '@/utils/storage';

export const useTagsViewRoutes = defineStore('tagsViewRoutes', {
  state: (): {
    tagsViewRoutes: any[];
    isTagsViewCurrenFull: boolean;
    favoriteRoutes: any[];
  } => ({
    tagsViewRoutes: [],
    isTagsViewCurrenFull: false,
    favoriteRoutes: [],
  }),
  actions: {
    setTagsViewRoutes(data: any[]) {
      this.tagsViewRoutes = data;
    },
    setCurrenFullscreen(bool: boolean) {
      Session.set('isTagsViewCurrenFull', bool);
      this.isTagsViewCurrenFull = bool;
    },
    addFavoriteRoutes(item: any) {
      this.favoriteRoutes.unshift(item);
    },
    delFavoriteRoutes(item: any) {
      const idx = this.favoriteRoutes.indexOf(item);
      if (idx > -1) this.favoriteRoutes.splice(idx, 1);
    },
    clearTagsView() {
      this.tagsViewRoutes = [];
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
