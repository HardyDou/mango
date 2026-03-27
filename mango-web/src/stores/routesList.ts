import { defineStore } from 'pinia';

export const useRoutesList = defineStore('routesList', {
  state: (): { routesList: any[]; isColumnsMenuHover: boolean; isColumnsNavHover: boolean } => ({
    routesList: [],
    isColumnsMenuHover: false,
    isColumnsNavHover: false,
  }),
  actions: {
    setRoutesList(data: any[]) {
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
