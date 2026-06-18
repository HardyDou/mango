import { defineStore } from 'pinia';
import type { ButtonDisplayRule } from '@mango/common';
import { Session } from '@mango/common/utils/storage';

export const useUserInfo = defineStore('userInfo', {
  state: (): { userInfos: UserInfosState['userInfos'] } => ({
    userInfos: {
      username: '',
      nickname: '',
      photo: '',
      time: 0,
      roles: [],
      permissions: [],
      buttonRules: [],
      authBtnList: [],
      tenantId: '',
      tenantCode: '',
      tenantName: '',
      realm: '',
      actorType: '',
      partyType: '',
      partyId: '',
      appCode: '',
    },
  }),
  actions: {
    setUserInfos(data: Partial<UserInfosState['userInfos']>) {
      this.userInfos = {
        ...this.userInfos,
        ...data,
        time: new Date().getTime(),
      };
      // userInfo is non-sensitive display data, stored in sessionStorage
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
        buttonRules: [],
        authBtnList: [],
        tenantId: '',
        tenantCode: '',
        tenantName: '',
        realm: '',
        actorType: '',
        partyType: '',
        partyId: '',
        appCode: '',
      };
      Session.clearSession();
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
    buttonRules: ButtonDisplayRule[];
    authBtnList: string[];
    tenantId: string;
    tenantCode: string;
    tenantName: string;
    realm: string;
    actorType: string;
    partyType: string;
    partyId: string | number;
    appCode: string;
  };
}
