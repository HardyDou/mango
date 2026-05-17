/**
 * 后端 Long、雪花主键、业务主键类 ID 到前端统一按字符串处理。
 *
 * JavaScript number 无法安全表示超过 Number.MAX_SAFE_INTEGER 的 Long，
 * API 类型、路由参数、表格行数据和组件 model 中的 ID 不允许用 Number(id) 归一化。
 */
export type ApiId = string;

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
  id?: ApiId;
  createTime?: string;
  updateTime?: string;
  createBy?: string;
  updateBy?: string;
}
