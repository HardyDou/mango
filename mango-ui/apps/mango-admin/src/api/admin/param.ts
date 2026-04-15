/**
 * System Param API - 系统参数
 */

import { get, post, put, del } from '@mango/common';

export interface SysParam {
  id?: number;
  paramKey: string;
  paramValue: string;
  paramType: number;
  description?: string;
  status?: number;
  createTime?: string;
  updateTime?: string;
}

export interface SysParamQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  paramType?: number;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export const paramApi = {
  list: (params?: SysParamQuery) => {
    return get<PageResult<SysParam>>('/system/param/list', { params });
  },
  detail: (id: number) => {
    return get<SysParam>(`/system/param/${id}`);
  },
  create: (data: SysParam) => {
    return post<SysParam>('/system/param', data);
  },
  update: (data: SysParam) => {
    return put<SysParam>('/system/param', data);
  },
  delete: (id: number) => {
    return del<void>(`/system/param/${id}`);
  },
  updateValue: (id: number, paramValue: string) => {
    return put<void>(`/system/param/value/${id}`, { paramValue });
  },
};
