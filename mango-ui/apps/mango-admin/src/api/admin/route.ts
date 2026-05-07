/**
 * Route API - 路由管理
 */

import { get, post, put, del } from '@mango/common';

export interface SysRoute {
  id?: number;
  parentId?: number;
  routeName: string;
  routePath: string;
  routeType: number;
  component?: string;
  redirect?: string;
  icon?: string;
  isCache?: number;
  isAffix?: number;
  isVisible?: number;
  sort?: number;
  status?: number;
  permission?: string;
  description?: string;
  createTime?: string;
  updateTime?: string;
  children?: SysRoute[];
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
    return get<any[]>('/system/route/list', { params }).then((list) => toPageResult(list.map(fromBackend), params));
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
  sort: (data: { id: number; sort: number }[]) => {
    return put<boolean>('/system/route/sort', data.map((item) => item.id));
  },
};

function fromBackend(item: any): SysRoute {
  return {
    ...item,
    parentId: item.parentId ?? 0,
    description: item.description ?? item.routeDesc,
  };
}

function toBackend(item: SysRoute) {
  return {
    ...item,
    routeDesc: item.description,
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
