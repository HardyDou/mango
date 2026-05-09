/**
 * Tenant API - 机构管理。
 * 后端路径和字段暂保留 tenant 命名，前端展示统一使用“机构”。
 */

import { get, post, put, del } from '@mango/common';

export interface SysTenant {
  id?: number;
  tenantName: string;
  tenantCode: string;
  institutionType?: string;
  capabilityCodes?: string;
  capabilityCodeList?: string[];
  contactName?: string;
  contactPhone?: string;
  contactEmail?: string;
  contact?: string;
  mobile?: string;
  email?: string;
  status?: number;
  createTime?: string;
  updateTime?: string;
}

interface SysTenantPayload {
  id?: number;
  tenantName: string;
  tenantCode: string;
  institutionType?: string;
  capabilityCodes?: string;
  status?: number;
  contact?: string;
  mobile?: string;
  email?: string;
  remark?: string;
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
    return get<SysTenant[]>('/system/tenant/list', { params })
      .then((list) => toPageResult(list.map(fromBackend), params));
  },
  detail: (id: number) => {
    return get<SysTenant>('/system/tenant/detail', { params: { id } }).then(fromBackend);
  },
  create: (data: SysTenant) => {
    return post<number>('/system/tenant', toBackend(data));
  },
  update: (data: SysTenant) => {
    return put<boolean>('/system/tenant', toBackend(data));
  },
  delete: (id: number) => {
    return del<boolean>('/system/tenant', { params: { id } });
  },
  updateStatus: (id: number, params: { status: number }) => {
    return put<boolean>('/system/tenant/status', undefined, { params: { id, status: params.status } });
  },
};

function fromBackend(item: SysTenant): SysTenant {
  return {
    ...item,
    capabilityCodeList: item.capabilityCodeList
      ?? (item.capabilityCodes ? item.capabilityCodes.split(',').filter(Boolean) : []),
    contactName: item.contactName ?? item.contact,
    contactPhone: item.contactPhone ?? item.mobile,
    contactEmail: item.contactEmail ?? item.email,
  };
}

function toBackend(item: SysTenant): SysTenantPayload {
  return {
    id: item.id,
    tenantName: item.tenantName,
    tenantCode: item.tenantCode,
    institutionType: item.institutionType,
    capabilityCodes: item.capabilityCodeList?.length
      ? item.capabilityCodeList.join(',')
      : item.capabilityCodes,
    status: item.status,
    contact: item.contactName ?? item.contact,
    mobile: item.contactPhone ?? item.mobile,
    email: item.contactEmail ?? item.email,
  };
}

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
