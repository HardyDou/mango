import { defineStore } from 'pinia';
import type { RouteRecordRaw } from 'vue-router';

export const useRoutesList = defineStore('routesList', {
  state: (): { routesList: RouteRecordRaw[]; isColumnsMenuHover: boolean; isColumnsNavHover: boolean } => ({
    routesList: [],
    isColumnsMenuHover: false,
    isColumnsNavHover: false,
  }),
  actions: {
    setRoutesList(data: RouteRecordRaw[]) {
      this.routesList = data;
    },
    setColumnsMenuHover(bool: boolean) {
      this.isColumnsMenuHover = bool;
    },
    setColumnsNavHover(bool: boolean) {
      this.isColumnsNavHover = bool;
    },
  },
});
