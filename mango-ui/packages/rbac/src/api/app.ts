import { del, get, post, put } from '@mango/common';
import type {
  MangoDeployMode,
  MangoFrontendAppType,
  MangoMenuPageType,
  MangoModuleRuntimeStrategy,
  MangoStyleIsolation,
} from '@mango/app-runtime';

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
  appType?: MangoFrontendAppType;
  deployMode?: MangoDeployMode;
  entryUrl?: string;
  mountPath?: string;
  activeRule?: string;
  framework?: string;
  version?: string;
  healthCheckUrl?: string;
  sandboxEnabled?: boolean;
  styleIsolation?: MangoStyleIsolation;
  loginContexts?: AppLoginContext[];
  icon?: string;
  sort?: number;
  status: number;
  remark?: string;
  createTime?: string | number[];
  updateTime?: string | number[];
}

export interface AppModuleBinding {
  bindingId?: BackendId;
  appCode: string;
  moduleCode: string;
  moduleName?: string;
  status: number;
  sort?: number;
  createTime?: string | number[];
  updateTime?: string | number[];
}

export interface AppModuleRuntimeStrategy extends MangoModuleRuntimeStrategy {
  strategyId?: BackendId;
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
  appType?: MangoFrontendAppType;
  deployMode?: MangoDeployMode;
  entryUrl?: string;
  mountPath?: string;
  activeRule?: string;
  framework?: string;
  version?: string;
  healthCheckUrl?: string;
  sandboxEnabled?: boolean;
  styleIsolation?: MangoStyleIsolation;
  loginContexts: AppLoginContextPayload[];
  icon?: string;
  sort?: number;
  status: number;
  remark?: string;
}

export const appApi = {
  list: () => get<AuthorizationApp[]>('/authorization/apps'),
  runtime: () => get<AuthorizationApp[]>('/authorization/apps/runtime'),
  runtimeDetail: (appCode: string) => get<AuthorizationApp>(`/authorization/apps/runtime/detail/${appCode}`),
  detail: (appId: BackendId) => get<AuthorizationApp>('/authorization/apps/detail', { params: { appId } }),
  create: (data: AuthorizationApp) => post<number>('/authorization/apps', toBackend(data)),
  update: (data: AuthorizationApp) => put<boolean>('/authorization/apps', toBackend(data)),
  delete: (appId: BackendId) => del<boolean>('/authorization/apps', { params: { appId } }),
};

export const appModuleApi = {
  list: (params: { appCode?: string; status?: number } = {}) =>
    get<AppModuleBinding[]>('/authorization/app-modules', { params }),
  save: (data: AppModuleBinding) => post<number>('/authorization/app-modules', data),
  disable: (appCode: string, moduleCode: string) =>
    del<boolean>('/authorization/app-modules', { params: { appCode, moduleCode } }),
  syncMenus: (appCode: string, moduleCode: string) =>
    post<number>('/authorization/app-modules/sync-menus', undefined, { params: { appCode, moduleCode } }),
  listRuntimeStrategies: (params: { appCode?: string; deployProfile?: string } = {}) =>
    get<AppModuleRuntimeStrategy[]>('/authorization/app-modules/runtime-strategies', { params }),
  saveRuntimeStrategy: (data: AppModuleRuntimeStrategy) =>
    post<number>('/authorization/app-modules/runtime-strategies', {
      ...data,
      pageType: data.pageType as MangoMenuPageType,
    }),
};

function toBackend(data: AuthorizationApp): AuthorizationAppPayload {
  return {
    appId: data.appId,
    appCode: data.appCode,
    appName: data.appName,
    appType: data.appType || 'LOCAL',
    deployMode: data.deployMode || 'EMBEDDED',
    entryUrl: data.entryUrl,
    mountPath: data.mountPath,
    activeRule: data.activeRule,
    framework: data.framework,
    version: data.version,
    healthCheckUrl: data.healthCheckUrl,
    sandboxEnabled: data.sandboxEnabled ?? false,
    styleIsolation: data.styleIsolation || 'NONE',
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
