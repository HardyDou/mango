import { del, get, post, put } from '@mango/common';

type BackendId = string | number;

export interface AppLoginContext {
  contextId?: BackendId;
  appId?: BackendId;
  appCode?: string;
  realm: string;
  actorType: string;
  defaultFlag: number;
  status: number;
  sort?: number;
  createTime?: string | number[];
  updateTime?: string | number[];
}

export interface AuthorizationApp {
  appId?: BackendId;
  appCode: string;
  appName: string;
  loginContexts?: AppLoginContext[];
  icon?: string;
  sort?: number;
  status: number;
  remark?: string;
  createTime?: string | number[];
  updateTime?: string | number[];
}

interface AppLoginContextPayload {
  contextId?: BackendId;
  realm: string;
  actorType: string;
  defaultFlag: number;
  status: number;
  sort?: number;
}

interface AuthorizationAppPayload {
  appId?: BackendId;
  appCode: string;
  appName: string;
  loginContexts: AppLoginContextPayload[];
  icon?: string;
  sort?: number;
  status: number;
  remark?: string;
}

export const appApi = {
  list: () => get<AuthorizationApp[]>('/authorization/apps'),
  detail: (appId: BackendId) => get<AuthorizationApp>('/authorization/apps/detail', { params: { appId } }),
  create: (data: AuthorizationApp) => post<number>('/authorization/apps', toBackend(data)),
  update: (data: AuthorizationApp) => put<boolean>('/authorization/apps', toBackend(data)),
  delete: (appId: BackendId) => del<boolean>('/authorization/apps', { params: { appId } }),
};

function toBackend(data: AuthorizationApp): AuthorizationAppPayload {
  return {
    appId: data.appId,
    appCode: data.appCode,
    appName: data.appName,
    loginContexts: (data.loginContexts || []).map(toBackendLoginContext),
    icon: data.icon,
    sort: data.sort,
    status: data.status,
    remark: data.remark,
  };
}

function toBackendLoginContext(data: AppLoginContext): AppLoginContextPayload {
  return {
    contextId: data.contextId,
    realm: data.realm,
    actorType: data.actorType,
    defaultFlag: data.defaultFlag,
    status: data.status,
    sort: data.sort,
  };
}
