/**
 * Dictionary Management API - 字典管理
 *
 * Backend API prefix: /system/dict
 * Response: {code, msg, data, success}
 */

import { del, get, post, put } from '@mango/common/utils/request';
import type { ApiId } from '@mango/api-schema';

// ==================== 类型定义 ====================

/** 字典类型 */
export interface DictType {
  id?: ApiId;
  name: string;
  code: string;
  domainCode?: string;
  description?: string;
  sort?: number;
  status?: number;
  createTime?: string;
  updateTime?: string;
}

/** 字典数据 */
export interface DictData {
  id?: ApiId;
  typeId: ApiId;
  dictType?: string;
  label: string;
  value: string;
  sort?: number;
  status?: number;
  extra?: string;
  createTime?: string;
  updateTime?: string;
}

/** 字典类型查询参数 */
export interface DictTypeQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  domainCode?: string;
}

/** 字典数据查询参数 */
export interface DictDataQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  typeId?: ApiId;
}

/** 分页结果 */
export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

// ==================== 字典类型 API ====================

export const dictTypeApi = {
  /**
   * 分页查询字典类型
   */
  list: (params?: DictTypeQuery) => {
    return get<any[]>('/system/dict/type/list', { params })
      .then((list) => toPageResult(filterByKeyword(list.map(fromBackendType), params?.keyword), params));
  },

  /**
   * 获取字典类型详情
   */
  detail: (id: ApiId) => {
    return get<any>('/system/dict/type/detail', { params: { id } }).then(fromBackendType);
  },

  /**
   * 新增字典类型
   */
  create: (data: DictType) => {
    return post<ApiId>('/system/dict/type', toBackendType(data));
  },

  /**
   * 修改字典类型
   */
  update: (data: DictType) => {
    return put<boolean>('/system/dict/type', toBackendType(data));
  },

  /**
   * 删除字典类型
   */
  delete: (id: ApiId) => {
    return del<boolean>('/system/dict/type', { params: { id } });
  },
};

// ==================== 字典数据 API ====================

export const dictDataApi = {
  /**
   * 分页查询字典数据
   */
  list: (params?: DictDataQuery) => {
    return get<any[]>('/system/dict/data/list', { params: { typeId: params?.typeId } })
      .then((list) => toPageResult(filterByKeyword(list.map((item) => fromBackendData(item, params?.typeId)), params?.keyword), params));
  },

  /**
   * 获取字典数据详情
   */
  detail: (id: ApiId) => {
    return get<any>('/system/dict/data/detail', { params: { id } }).then(fromBackendData);
  },

  /**
   * 新增字典数据
   */
  create: (data: DictData) => {
    return post<ApiId>('/system/dict/data', toBackendData(data));
  },

  /**
   * 修改字典数据
   */
  update: (data: DictData) => {
    return put<boolean>('/system/dict/data', toBackendData(data));
  },

  /**
   * 删除字典数据
   */
  delete: (id: ApiId) => {
    return del<boolean>('/system/dict/data', { params: { id } });
  },

  /**
   * 获取指定类型的字典数据（用于下拉选项）
   * @param typeCode 字典类型编码
   */
  options: (typeCode: string) => {
    return get<any[]>('/system/dict/data/options', { params: { typeCode } })
      .then((list) => list.map(fromBackendOption));
  },
};

function fromBackendType(item: any): DictType {
    return {
    id: item.id,
    name: item.dictName ?? item.name,
    code: item.dictType ?? item.code,
    domainCode: item.domainCode ?? 'COMMON',
    description: item.remark ?? item.description,
    sort: item.sort ?? 0,
    status: item.status ?? 1,
    createTime: item.createTime,
    updateTime: item.updateTime,
  };
}

function toBackendType(item: DictType) {
  return {
    id: item.id,
    dictName: item.name,
    dictType: item.code,
    domainCode: item.domainCode || 'COMMON',
    status: item.status,
    remark: item.description,
  };
}

function fromBackendData(item: any, typeId?: ApiId): DictData {
  return {
    id: item.id,
    typeId: item.typeId ?? typeId ?? '0',
    dictType: item.dictType,
    label: item.dictLabel ?? item.label,
    value: item.dictValue ?? item.value,
    sort: item.sort ?? 0,
    status: item.status ?? 1,
    extra: item.extra,
    createTime: item.createTime,
    updateTime: item.updateTime,
  };
}

function fromBackendOption(item: any): DictData {
  return {
    id: item.id,
    typeId: '0',
    label: item.label ?? item.dictLabel,
    value: item.value ?? item.dictValue,
    sort: item.sort ?? 0,
    status: item.status ?? 1,
  };
}

function toBackendData(item: DictData) {
  return {
    id: item.id,
    dictLabel: item.label,
    dictValue: item.value,
    dictType: item.dictType,
    sort: item.sort,
    status: item.status,
  };
}

function filterByKeyword<T extends { name?: string; code?: string; label?: string; value?: string }>(list: T[], keyword?: string): T[] {
  const normalized = keyword?.trim().toLowerCase();
  if (!normalized) {
    return list;
  }
  return list.filter((item) =>
    [item.name, item.code, item.label, item.value]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(normalized))
  );
}

function toPageResult<T>(list: T[] = [], params?: { pageNum?: number; pageSize?: number }): PageResult<T> {
  const pageNum = params?.pageNum || 1;
  const pageSize = params?.pageSize || list.length || 10;
  const start = (pageNum - 1) * pageSize;
  return {
    list: list.slice(start, start + pageSize),
    total: list.length,
    pageNum,
    pageSize,
  };
}
