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
    return get<any[]>('/system/config/list', { params: toBackendQuery(params) })
      .then((list) => toPageResult(list.map(fromConfig), params));
  },
  detail: (id: number) => {
    return get<any>('/system/config/detail', { params: { id } }).then(fromConfig);
  },
  create: (data: SysParam) => {
    return post<number>('/system/config', toConfig(data));
  },
  update: (data: SysParam) => {
    return put<boolean>('/system/config', toConfig(data));
  },
  delete: (id: number) => {
    return del<boolean>('/system/config', { params: { id } });
  },
  updateValue: (id: number, paramValue: string) => {
    return put<boolean>('/system/config/value', undefined, { params: { id, value: paramValue } });
  },
};

function toBackendQuery(params?: SysParamQuery) {
  return {
    type: params?.paramType ? toBackendType(params.paramType) : undefined,
  };
}

function toBackendType(paramType: number) {
  return paramType === 1 ? 'SYSTEM' : 'BUSINESS';
}

function fromConfig(item: any): SysParam {
  return {
    id: item.id,
    paramKey: item.configKey,
    paramValue: item.configValue,
    paramType: item.type === 'SYSTEM' ? 1 : 2,
    description: item.remark || item.configName,
    status: item.status,
    createTime: item.createTime,
    updateTime: item.updateTime,
  };
}

function toConfig(item: SysParam) {
  return {
    id: item.id,
    configKey: item.paramKey,
    configValue: item.paramValue,
    configName: item.paramKey,
    type: toBackendType(item.paramType),
    remark: item.description,
    status: item.status,
    sort: 0,
  };
}

function toPageResult<T>(list: T[] = [], params?: SysParamQuery): PageResult<T> {
  const pageNum = params?.pageNum || 1;
  const pageSize = params?.pageSize || list.length || 10;
  return {
    list,
    total: list.length,
    pageNum,
    pageSize,
  };
}
