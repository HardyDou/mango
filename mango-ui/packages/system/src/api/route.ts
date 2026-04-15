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
    return get<PageResult<SysRoute>>('/route/list', { params });
  },
  tree: () => {
    return get<SysRoute[]>('/route/tree');
  },
  detail: (id: number) => {
    return get<SysRoute>(`/route/${id}`);
  },
  create: (data: SysRoute) => {
    return post<SysRoute>('/route', data);
  },
  update: (data: SysRoute) => {
    return put<SysRoute>('/route', data);
  },
  delete: (id: number) => {
    return del<void>(`/route/${id}`);
  },
  sort: (data: { id: number; sort: number }[]) => {
    return put<void>('/route/sort', data);
  },
};
