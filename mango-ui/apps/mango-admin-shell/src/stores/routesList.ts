import { defineStore } from 'pinia';
import type { RouteRecordRaw } from 'vue-router';

export const useRoutesList = defineStore('routesList', {
  state: (): {
    routesList: RouteRecordRaw[];
    activeTopRoutePath: string;
  } => ({
    routesList: [],
    activeTopRoutePath: '',
  }),
  actions: {
    setRoutesList(data: RouteRecordRaw[]) {
      this.routesList = data;
    },
    setActiveTopRoutePath(path: string) {
      this.activeTopRoutePath = path;
    },
    resetRoutesList() {
      this.routesList = [];
      this.activeTopRoutePath = '';
    },
  },
});
