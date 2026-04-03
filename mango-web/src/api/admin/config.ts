/**
 * System Config API - 系统配置
 */

import { get, post, put, del } from '@/utils/request';

export interface SysConfig {
  id?: number;
  configKey: string;
  configValue: string;
  configGroup: string;
  description?: string;
  status?: number;
  createTime?: string;
  updateTime?: string;
}

export interface SysConfigQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  configGroup?: string;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export const configApi = {
  list: (params?: SysConfigQuery) => {
    return get<PageResult<SysConfig>>('/system/config/list', { params });
  },
  detail: (id: number) => {
    return get<SysConfig>(`/system/config/${id}`);
  },
  create: (data: SysConfig) => {
    return post<SysConfig>('/system/config', data);
  },
  update: (data: SysConfig) => {
    return put<SysConfig>('/system/config', data);
  },
  delete: (id: number) => {
    return del<void>(`/system/config/${id}`);
  },
  group: (group: string) => {
    return get<SysConfig[]>(`/system/config/group/${group}`);
  },
  groups: () => {
    return get<string[]>('/system/config/groups');
  },
};
