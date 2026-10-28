import { get, post, put } from '@mango/common/utils/request';
import type { ApiId } from '@mango/api-schema';

/** 接口资源登记来源。 */
export type ApiResourceSource = 'FIXED_DECLARATION' | 'CONTROLLER_SCAN' | 'MANUAL';

/** 接口访问模式。 */
export type ApiResourceAccessMode = 'PUBLIC' | 'LOGIN' | 'PERMISSION' | 'INTERNAL';

/** 权限标识登记明细。 */
export interface ApiResourceVO {
  id: ApiId;
  moduleName: string;
  domain: string;
  source: ApiResourceSource;
  /** 是否被人工接管：true 时访问模式来自人工配置。 */
  manualOverride?: boolean;
  /** 接管前的访问模式，未接管时为空。 */
  originAccessMode?: ApiResourceAccessMode;
  httpMethod: string;
  pathPattern: string;
  resourceCode?: string;
  permissionCode?: string;
  accessMode: ApiResourceAccessMode;
  handlerClass?: string;
  handlerMethod?: string;
  description?: string;
  status: number;
  createTime?: string;
  updateTime?: string;
}

/** 权限标识筛选维度。 */
export interface ApiResourceFilterOptionsVO {
  modules: string[];
  domains: string[];
}

/** 权限标识查询条件。 */
export interface ApiResourceQuery {
  pageNum: number;
  pageSize: number;
  moduleName?: string;
  domain?: string;
  source?: ApiResourceSource;
  accessMode?: ApiResourceAccessMode;
  status?: number;
  keyword?: string;
}

/** 权限标识分页结果。 */
export interface ApiResourcePageResult {
  list: ApiResourceVO[];
  total: number;
  pageNum: number;
  pageSize: number;
}

interface BackendPageResult<T> {
  list?: T[];
  records?: T[];
  total?: number | string;
  page?: number | string;
  size?: number | string;
  current?: number | string;
}

/** 人工接管命令：接管后该路由由人工配置绝对控制。 */
export interface ApiResourceManualCommand {
  moduleName: string;
  httpMethod: string;
  pathPattern: string;
  accessMode: ApiResourceAccessMode;
  permissionCode?: string;
  description?: string;
  /** 当前是否为人工接管（仅用于页面状态展示，不提交）。 */
  manualOverride?: boolean;
}

/** 释放人工接管的定位条件。 */
export interface ApiResourceManualQuery {
  moduleName: string;
  httpMethod: string;
  pathPattern: string;
}

export const apiResourceApi = {
  page: (params: ApiResourceQuery) =>
    get<BackendPageResult<ApiResourceVO>>('/authorization/api-resources/page', {
      params: toBackendQuery(params),
    }).then((data) => toPageResult(data, params)),
  filterOptions: () => get<ApiResourceFilterOptionsVO>('/authorization/api-resources/filter-options'),
  saveManual: (command: ApiResourceManualCommand) =>
    post<void>('/authorization/api-resources/manual', toManualCommand(command)),
  releaseManual: (query: ApiResourceManualQuery) =>
    put<void>('/authorization/api-resources/manual/release', undefined, { params: query }),
};

function toManualCommand(command: ApiResourceManualCommand) {
  return {
    moduleName: command.moduleName,
    httpMethod: command.httpMethod,
    pathPattern: command.pathPattern,
    accessMode: command.accessMode,
    permissionCode: command.accessMode === 'PERMISSION' ? command.permissionCode : command.permissionCode || undefined,
    description: command.description || undefined,
  };
}

function toBackendQuery(params: ApiResourceQuery) {
  return {
    page: params.pageNum,
    size: params.pageSize,
    moduleName: params.moduleName || undefined,
    domain: params.domain || undefined,
    source: params.source || undefined,
    accessMode: params.accessMode || undefined,
    status: params.status,
    keyword: params.keyword || undefined,
  };
}

function toPageResult(
  data: BackendPageResult<ApiResourceVO> | undefined,
  params: ApiResourceQuery,
): ApiResourcePageResult {
  const list = data?.list || data?.records || [];
  return {
    list,
    total: Number(data?.total ?? list.length),
    pageNum: Number(data?.page ?? data?.current ?? params.pageNum),
    pageSize: Number(data?.size ?? params.pageSize),
  };
}
