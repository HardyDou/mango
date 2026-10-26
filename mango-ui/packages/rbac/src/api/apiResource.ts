import { get } from '@mango/common/utils/request';
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

export const apiResourceApi = {
  page: (params: ApiResourceQuery) =>
    get<BackendPageResult<ApiResourceVO>>('/authorization/api-resources/page', {
      params: toBackendQuery(params),
    }).then((data) => toPageResult(data, params)),
  filterOptions: () => get<ApiResourceFilterOptionsVO>('/authorization/api-resources/filter-options'),
};

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
