import { defineStore } from 'pinia';

export const useKeepAliveNames = defineStore('keepAliveNames', {
  state: (): { keepAliveNames: string[] } => ({
    keepAliveNames: [],
  }),
  actions: {
    setKeepAliveNames(data: string[]) {
      this.keepAliveNames = data;
    },
    addKeepAliveNames(name: string) {
      if (!this.keepAliveNames.includes(name)) {
        this.keepAliveNames.push(name);
      }
    },
    removeKeepAliveNames(name: string) {
      const idx = this.keepAliveNames.indexOf(name);
      if (idx > -1) this.keepAliveNames.splice(idx, 1);
    },
    clearKeepAliveNames() {
      this.keepAliveNames = [];
    },
  },
});
