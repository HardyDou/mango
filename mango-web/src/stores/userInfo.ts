import { defineStore } from 'pinia';
import { Session } from '@/utils/storage';

export const useUserInfo = defineStore('userInfo', {
  state: (): { userInfos: UserInfosState['userInfos'] } => ({
    userInfos: {
      username: '',
      nickname: '',
      photo: '',
      time: 0,
      roles: [],
      permissions: [],
      authBtnList: [],
      tenantId: '',
      tenantName: '',
    },
  }),
  actions: {
    setUserInfos(data: Partial<UserInfosState['userInfos']>) {
      this.userInfos = {
        ...this.userInfos,
        ...data,
        time: new Date().getTime(),
      };
      // SECURITY: userInfo including permissions is stored in sessionStorage.
      // This is a defense-in-depth measure since tokens are now in sessionStorage.
      // TODO (P2): For production, consider:
      // 1. Storing permissions only in memory (refetch from API on refresh)
      // 2. Using httpOnly cookies for all sensitive data
      Session.set('userInfo', this.userInfos);
    },
    updateTenantInfo(tenantId: string, tenantName: string) {
      this.userInfos.tenantId = tenantId;
      this.userInfos.tenantName = tenantName;
      Session.set('tenantId', tenantId);
    },
    clearUserInfo() {
      this.userInfos = {
        username: '',
        nickname: '',
        photo: '',
        time: 0,
        roles: [],
        permissions: [],
        authBtnList: [],
        tenantId: '',
        tenantName: '',
      };
      Session.clear();
    },
  },
});

export interface UserInfosState {
  userInfos: {
    username: string;
    nickname: string;
    photo: string;
    time: number;
    roles: string[];
    permissions: string[];
    authBtnList: string[];
    tenantId: string;
    tenantName: string;
  };
}
