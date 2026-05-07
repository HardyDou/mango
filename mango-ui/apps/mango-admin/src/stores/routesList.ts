import { defineStore } from 'pinia';
import type { RouteRecordRaw } from 'vue-router';

export const useRoutesList = defineStore('routesList', {
  state: (): {
    routesList: RouteRecordRaw[];
    activeTopRoutePath: string;
    isColumnsMenuHover: boolean;
    isColumnsNavHover: boolean;
  } => ({
    routesList: [],
    activeTopRoutePath: '',
    isColumnsMenuHover: false,
    isColumnsNavHover: false,
  }),
  actions: {
    setRoutesList(data: RouteRecordRaw[]) {
      this.routesList = data;
    },
    setActiveTopRoutePath(path: string) {
      this.activeTopRoutePath = path;
    },
    setColumnsMenuHover(bool: boolean) {
      this.isColumnsMenuHover = bool;
    },
    setColumnsNavHover(bool: boolean) {
      this.isColumnsNavHover = bool;
    },
  },
});
