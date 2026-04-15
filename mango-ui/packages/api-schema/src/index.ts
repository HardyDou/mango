/**
 * 统一响应结构
 */
export interface R<T = any> {
  code: number;
  data: T;
  msg: string;
  success: boolean;
}

/**
 * 分页请求参数
 */
export interface PageQuery {
  page: number;
  size: number;
  [key: string]: any;
}

/**
 * 分页返回结果
 */
export interface PageResult<T = any> {
  list: T[];
  total: number;
  page: number;
  size: number;
}

/**
 * 基础实体类型
 */
export interface BaseEntity {
  id?: number | string;
  createTime?: string;
  updateTime?: string;
  createBy?: string;
  updateBy?: string;
}
