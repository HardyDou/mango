/**
 * Route API - 路由管理
 */

import { get, post, put, del } from '@mango/common';

export interface SysRoute {
  id?: number | string;
  routeName: string;
  routePath: string;
  routeType: number;
  sort?: number;
  status?: number;
  description?: string;
  createTime?: string;
  updateTime?: string;
}

export interface RouteQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  routeType?: number;
  status?: number;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export const routeApi = {
  list: (params?: RouteQuery) => {
    return get<any[]>('/system/route/list', { params: toBackendQuery(params) })
      .then((list) => toPageResult(list.map(fromBackend), params));
  },
  tree: () => {
    return get<any[]>('/system/route/tree').then((list) => list.map(fromBackend));
  },
  detail: (id: number) => {
    return get<any>('/system/route/detail', { params: { id } }).then(fromBackend);
  },
  create: (data: SysRoute) => {
    return post<number>('/system/route', toBackend(data));
  },
  update: (data: SysRoute) => {
    return put<boolean>('/system/route', toBackend(data));
  },
  delete: (id: number) => {
    return del<boolean>('/system/route', { params: { id } });
  },
  sort: (data: { id: number | string; sort: number }[]) => {
    return put<boolean>('/system/route/sort', { ids: data.map((item) => item.id) });
  },
};

function toBackendQuery(params?: RouteQuery) {
  return {
    routeName: params?.keyword || undefined,
    routePath: params?.keyword || undefined,
    routeType: params?.routeType,
    status: params?.status,
  };
}

function fromBackend(item: any): SysRoute {
  return {
    ...item,
    parentId: item.parentId ?? 0,
    description: item.description ?? item.routeDesc,
  };
}

function toBackend(item: SysRoute) {
  return {
    id: item.id,
    routeName: item.routeName,
    routePath: item.routePath,
    routeType: item.routeType,
    routeDesc: item.description,
    sort: item.sort,
    status: item.status,
  };
}

function toPageResult<T>(list: T[] = [], params?: RouteQuery): PageResult<T> {
  const pageNum = params?.pageNum || 1;
  const pageSize = params?.pageSize || list.length || 10;
  return {
    list,
    total: list.length,
    pageNum,
    pageSize,
  };
}
