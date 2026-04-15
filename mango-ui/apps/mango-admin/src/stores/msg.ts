import { defineStore } from 'pinia';

export const useMsg = defineStore('msg', {
  state: (): { msg: any[] } => ({
    msg: [],
  }),
  actions: {
    setMsg(data: any[]) {
      this.msg = data;
    },
    clearMsg() {
      this.msg = [];
    },
  },
});
