/**
 * Dictionary Management API - 字典管理
 *
 * Backend API prefix: /dict
 * Response: {code, msg, data, success}
 */

import { get, post, put, del } from '@/utils/request';

// ==================== 类型定义 ====================

/** 字典类型 */
export interface DictType {
  id?: number;
  name: string;
  code: string;
  description?: string;
  sort?: number;
  status?: number;
  createTime?: string;
  updateTime?: string;
}

/** 字典数据 */
export interface DictData {
  id?: number;
  typeId: number;
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
}

/** 字典数据查询参数 */
export interface DictDataQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  typeId?: number;
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
    return get<PageResult<DictType>>('/system/dict/type/list', { params });
  },

  /**
   * 获取字典类型详情
   */
  detail: (id: number) => {
    return get<DictType>(`/system/dict/type/${id}`);
  },

  /**
   * 新增字典类型
   */
  create: (data: DictType) => {
    return post<DictType>('/system/dict/type', data);
  },

  /**
   * 修改字典类型
   */
  update: (data: DictType) => {
    return put<DictType>('/system/dict/type', data);
  },

  /**
   * 删除字典类型
   */
  delete: (id: number) => {
    return del<void>(`/system/dict/type/${id}`);
  },
};

// ==================== 字典数据 API ====================

export const dictDataApi = {
  /**
   * 分页查询字典数据
   */
  list: (params?: DictDataQuery) => {
    return get<PageResult<DictData>>('/system/dict/data/list', { params });
  },

  /**
   * 获取字典数据详情
   */
  detail: (id: number) => {
    return get<DictData>(`/system/dict/data/${id}`);
  },

  /**
   * 新增字典数据
   */
  create: (data: DictData) => {
    return post<DictData>('/system/dict/data', data);
  },

  /**
   * 修改字典数据
   */
  update: (data: DictData) => {
    return put<DictData>('/system/dict/data', data);
  },

  /**
   * 删除字典数据
   */
  delete: (id: number) => {
    return del<void>(`/system/dict/data/${id}`);
  },

  /**
   * 获取指定类型的字典数据（用于下拉选项）
   * @param typeCode 字典类型编码
   */
  options: (typeCode: string) => {
    return get<DictData[]>('/system/dict/data/options', { params: { typeCode } });
  },
};
