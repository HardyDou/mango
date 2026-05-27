import { defineStore } from 'pinia';

export const useKeepAliveNames = defineStore('keepAliveNames', {
  state: (): {
    keepAliveNames: string[];
  } => ({
    keepAliveNames: [],
  }),
  actions: {
    setKeepAliveNames(names: string[]) {
      this.keepAliveNames = names;
    },
    addKeepAliveName(name: string) {
      if (!this.keepAliveNames.includes(name)) {
        this.keepAliveNames.push(name);
      }
    },
    removeKeepAliveName(name: string) {
      this.keepAliveNames = this.keepAliveNames.filter((item) => item !== name);
    },
  },
});
