/**
 * Tenant API - 机构管理。
 * 后端路径和字段暂保留 tenant 命名，前端展示统一使用“机构”。
 */

import { get, post, put, del } from '@mango/common';

export interface SysTenant {
  id?: number;
  tenantName: string;
  tenantCode: string;
  contactName?: string;
  contactPhone?: string;
  contactEmail?: string;
  expireTime?: string;
  status?: number;
  createTime?: string;
  updateTime?: string;
}

export interface TenantQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  status?: number;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export const tenantApi = {
  list: (params?: TenantQuery) => {
    return get<SysTenant[]>('/system/tenant/list', { params }).then((list) => toPageResult(list, params));
  },
  detail: (id: number) => {
    return get<SysTenant>('/system/tenant/detail', { params: { id } });
  },
  create: (data: SysTenant) => {
    return post<number>('/system/tenant', data);
  },
  update: (data: SysTenant) => {
    return put<boolean>('/system/tenant', data);
  },
  delete: (id: number) => {
    return del<boolean>('/system/tenant', { params: { id } });
  },
  updateStatus: (id: number, params: { status: number }) => {
    return put<boolean>('/system/tenant/status', undefined, { params: { id, status: params.status } });
  },
};

function toPageResult<T>(list: T[] = [], params?: TenantQuery): PageResult<T> {
  const pageNum = params?.pageNum || 1;
  const pageSize = params?.pageSize || list.length || 10;
  return {
    list,
    total: list.length,
    pageNum,
    pageSize,
  };
}
