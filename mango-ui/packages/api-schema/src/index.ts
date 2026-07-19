/**
 * 后端 Long、雪花主键、业务主键类 ID 到前端统一按字符串处理。
 *
 * JavaScript number 无法安全表示超过 Number.MAX_SAFE_INTEGER 的 Long，
 * API 类型、路由参数、表格行数据和组件 model 中的 ID 不允许用 Number(id) 归一化。
 */
export type ApiId = string;

export type HttpMethod = 'DELETE' | 'GET' | 'HEAD' | 'OPTIONS' | 'PATCH' | 'POST' | 'PUT';

export type HttpResponseType = 'arrayBuffer' | 'blob' | 'json' | 'stream' | 'text';

export type HttpHeaderValue = string | readonly string[] | undefined;

export type HttpHeaders = Readonly<Record<string, HttpHeaderValue>>;

export type HttpQueryValue = string | number | boolean | null | undefined | readonly (string | number | boolean)[];

export type HttpQuery = Readonly<Record<string, HttpQueryValue>>;

export interface HttpProgress {
  loaded: number;
  total?: number;
  progress?: number;
  bytesPerSecond?: number;
}

export interface HttpRequest<TBody = unknown> {
  method: HttpMethod;
  /** Relative endpoint owned by the business API. Base URL belongs to the host provider. */
  url: string;
  body?: TBody;
  query?: HttpQuery;
  headers?: HttpHeaders;
  signal?: AbortSignal;
  timeoutMs?: number;
  responseType?: HttpResponseType;
  idempotencyKey?: string;
  onUploadProgress?: (progress: HttpProgress) => void;
  onDownloadProgress?: (progress: HttpProgress) => void;
  metadata?: Readonly<Record<string, unknown>>;
}

export interface HttpClient {
  request<TResponse = unknown, TBody = unknown>(request: HttpRequest<TBody>): Promise<TResponse>;
}

export type HttpFailureKind =
  'aborted' | 'configuration' | 'network' | 'protocol' | 'timeout' | 'unauthorized' | 'unknown';

export interface HttpError extends Error {
  readonly name: 'HttpError';
  readonly kind: HttpFailureKind;
  readonly status?: number;
  readonly code?: string;
  readonly retryable: boolean;
  readonly requestId?: string;
  readonly details?: unknown;
}

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
