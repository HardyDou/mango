import { del, get, post, put } from '@mango/common/utils/request';
import type {
  MangoDeployMode,
  MangoFrontendAppType,
  MangoMenuPageType,
  MangoModuleRuntimeStrategy,
  MangoStyleIsolation,
} from '@mango/app-runtime';
import type { ApiId } from '@mango/api-schema';

export interface AppLoginContext {
  contextId?: ApiId;
  appId?: ApiId;
  appCode?: string;
  realm: string;
  actorType: string;
  defaultFlag: number;
  status: number;
  sort?: number;
  createTime?: string;
  updateTime?: string;
}

export interface AuthorizationApp {
  appId?: ApiId;
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
  createTime?: string;
  updateTime?: string;
}

export interface AppModuleBinding {
  bindingId?: ApiId;
  appCode: string;
  moduleCode: string;
  moduleName?: string;
  status: number;
  sort?: number;
  createTime?: string;
  updateTime?: string;
}

export interface AppModuleRuntimeStrategy extends MangoModuleRuntimeStrategy {
  strategyId?: ApiId;
}

interface AppLoginContextPayload {
  contextId?: ApiId;
  realm: string;
  actorType: string;
  defaultFlag: number;
  status: number;
  sort?: number;
}

interface AuthorizationAppPayload {
  appId?: ApiId;
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
  detail: (appId: ApiId) => get<AuthorizationApp>('/authorization/apps/detail', { params: { appId } }),
  create: (data: AuthorizationApp) => post<ApiId>('/authorization/apps', toBackend(data)),
  update: (data: AuthorizationApp) => put<boolean>('/authorization/apps', toBackend(data)),
  delete: (appId: ApiId) => del<boolean>('/authorization/apps', { params: { appId } }),
};

export const appModuleApi = {
  list: (params: { appCode?: string; status?: number } = {}) =>
    get<AppModuleBinding[]>('/authorization/app-modules', { params }),
  save: (data: AppModuleBinding) => post<ApiId>('/authorization/app-modules', data),
  disable: (appCode: string, moduleCode: string) =>
    del<boolean>('/authorization/app-modules', { params: { appCode, moduleCode } }),
  syncMenus: (appCode: string, moduleCode: string) =>
    post<ApiId>('/authorization/app-modules/sync-menus', undefined, { params: { appCode, moduleCode } }),
  listRuntimeStrategies: (params: { appCode?: string; deployProfile?: string } = {}) =>
    get<AppModuleRuntimeStrategy[]>('/authorization/app-modules/runtime-strategies', { params }),
  saveRuntimeStrategy: (data: AppModuleRuntimeStrategy) =>
    post<ApiId>('/authorization/app-modules/runtime-strategies', {
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
