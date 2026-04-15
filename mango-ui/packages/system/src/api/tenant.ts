/**
 * Tenant API - 租户管理
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
    return get<PageResult<SysTenant>>('/tenant/list', { params });
  },
  detail: (id: number) => {
    return get<SysTenant>(`/tenant/${id}`);
  },
  create: (data: SysTenant) => {
    return post<SysTenant>('/tenant', data);
  },
  update: (data: SysTenant) => {
    return put<SysTenant>('/tenant', data);
  },
  delete: (id: number) => {
    return del<void>(`/tenant/${id}`);
  },
  updateStatus: (id: number, params: { status: number }) => {
    return put<void>(`/tenant/status/${id}`, params);
  },
};
