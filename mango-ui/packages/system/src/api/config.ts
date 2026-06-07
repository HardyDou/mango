/**
 * System Config API - 系统配置
 */

import { del, get, post, put } from '@mango/common/utils/request';
import type { ApiId } from '@mango/api-schema';

export interface SysConfig {
  id?: ApiId;
  configKey: string;
  configValue: string;
  configGroup: string;
  configName?: string;
  type?: string;
  domainCode?: string;
  description?: string;
  remark?: string;
  status?: number;
  createTime?: string;
  updateTime?: string;
}

export interface SysConfigQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  configGroup?: string;
  domainCode?: string;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export const configApi = {
  list: (params?: SysConfigQuery) => {
    return get<any[]>('/system/config/list', { params: toBackendQuery(params) })
      .then((list) => toPageResult(list.map(fromBackend), params));
  },
  detail: (id: ApiId) => {
    return get<any>('/system/config/detail', { params: { id } }).then(fromBackend);
  },
  create: (data: SysConfig) => {
    return post<ApiId>('/system/config', toBackend(data));
  },
  update: (data: SysConfig) => {
    return put<boolean>('/system/config', toBackend(data));
  },
  delete: (id: ApiId) => {
    return del<boolean>('/system/config', { params: { id } });
  },
  group: (group: string) => {
    return get<any[]>('/system/config/type', { params: { type: toBackendType(group) } })
      .then((list) => list.map(fromBackend));
  },
  groups: () => {
    return get<string[]>('/system/config/groups');
  },
};

function toBackendQuery(params?: SysConfigQuery) {
  return {
    type: params?.configGroup ? toBackendType(params.configGroup) : undefined,
    domainCode: params?.domainCode || undefined,
  };
}

function toBackendType(group?: string): string {
  const value = (group || 'system').toUpperCase();
  if (value === 'UPLOAD' || value === 'EMAIL' || value === 'SMS') {
    return 'BUSINESS';
  }
  return value;
}

function fromBackend(item: any): SysConfig {
  const group = (item.type || 'SYSTEM').toLowerCase();
  return {
    ...item,
    configGroup: group,
    domainCode: item.domainCode ?? 'COMMON',
    description: item.description ?? item.remark,
  };
}

function toBackend(item: SysConfig) {
  return {
    id: item.id,
    configKey: item.configKey,
    configValue: item.configValue,
    configName: item.configName || item.configKey,
    type: item.type || toBackendType(item.configGroup),
    domainCode: item.domainCode || 'COMMON',
    status: item.status,
    sort: 0,
    remark: item.remark || item.description,
  };
}

function toPageResult<T>(list: T[] = [], params?: SysConfigQuery): PageResult<T> {
  const pageNum = params?.pageNum || 1;
  const pageSize = params?.pageSize || list.length || 10;
  return {
    list,
    total: list.length,
    pageNum,
    pageSize,
  };
}
