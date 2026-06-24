/**
 * System Config API - 系统配置
 */

import { del, get, post, put } from '@mango/common/utils/request';
import type { ApiId } from '@mango/api-schema';

export type ConfigValueType = 'BOOLEAN' | 'STRING' | 'NUMBER' | 'RADIO' | 'SELECT' | 'MULTI_SELECT' | 'DATE' | 'DATE_RANGE';
export type ConfigOptionSource = 'CUSTOM' | 'DICT';

export interface SysConfig {
  id?: ApiId;
  configKey: string;
  configValue: string;
  configGroup: string;
  configName?: string;
  type?: string;
  domainCode?: string;
  valueType?: ConfigValueType;
  groupCode?: string;
  groupName?: string;
  defaultValue?: string;
  options?: string;
  optionSource?: ConfigOptionSource;
  dictType?: string;
  editable?: boolean;
  editableReason?: string;
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
  updateValue: (id: ApiId, value: string) => {
    return put<boolean>('/system/config/value', undefined, { params: { id, value } });
  },
  group: (group: string) => {
    return get<any[]>('/system/config/type', { params: { type: toBackendType(group) } })
      .then((list) => list.map(fromBackend));
  },
  groups: () => {
    return get<string[]>('/system/config/groups');
  },
  valueTypes: () => {
    return get<ConfigValueType[]>('/system/config/value-types');
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
    valueType: normalizeValueType(item.valueType),
    groupCode: item.groupCode,
    groupName: item.groupName,
    defaultValue: item.defaultValue,
    options: item.options,
    optionSource: normalizeOptionSource(item.optionSource),
    dictType: item.dictType,
    editable: item.editable ?? true,
    editableReason: item.editableReason,
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
    valueType: item.valueType || 'STRING',
    groupCode: item.groupCode,
    groupName: item.groupName,
    defaultValue: item.defaultValue,
    options: item.options,
    optionSource: item.optionSource || 'CUSTOM',
    dictType: item.dictType,
    editable: item.editable ?? true,
    editableReason: item.editableReason,
    status: item.status,
    sort: 0,
    remark: item.remark || item.description,
  };
}

function normalizeValueType(value?: string): ConfigValueType {
  const normalized = (value || 'STRING').toUpperCase();
  if (
    normalized === 'BOOLEAN'
    || normalized === 'STRING'
    || normalized === 'NUMBER'
    || normalized === 'RADIO'
    || normalized === 'SELECT'
    || normalized === 'MULTI_SELECT'
    || normalized === 'DATE'
    || normalized === 'DATE_RANGE'
  ) {
    return normalized;
  }
  return 'STRING';
}

function normalizeOptionSource(value?: string): ConfigOptionSource {
  return value === 'DICT' ? 'DICT' : 'CUSTOM';
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
