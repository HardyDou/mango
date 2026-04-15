import { defineStore } from 'pinia';

export const useDict = defineStore('dict', {
  state: (): { dict: Record<string, any[]> } => ({
    dict: {},
  }),
  actions: {
    setDict(key: string, data: any[]) {
      this.dict[key] = data;
    },
    getDict(key: string): any[] {
      return this.dict[key] || [];
    },
    clearDict() {
      this.dict = {};
    },
  },
});
