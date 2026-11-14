import { defineStore } from 'pinia';
import type { ShellRouteMenu } from '../runtime/menuHost';

export const useRoutesList = defineStore('routesList', {
  state: (): {
    routesList: ShellRouteMenu[];
    activeTopRoutePath: string;
  } => ({
    routesList: [],
    activeTopRoutePath: '',
  }),
  actions: {
    setRoutesList(data: ShellRouteMenu[]) {
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
