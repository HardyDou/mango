import axios, { AxiosHeaders, AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import type { ApiId } from '@mango/api-schema';
import { Session } from './storage';
import { mangoMessage } from './message';

// 环境变量（当前未使用，预留）
// const VITE_ADMIN_PROXY_PATH = import.meta.env.VITE_ADMIN_PROXY_PATH || 'http://127.0.0.1:5555';

export interface RequestConfig extends AxiosRequestConfig {
  /** 是否显示 Loading */
  loading?: boolean;
  /** 是否忽略 Token */
  ignoreToken?: boolean;
  /** 请求重试次数 */
  retry?: number;
  /** 是否返回原始响应，适用于文件下载等非业务包裹响应 */
  rawResponse?: boolean;
  /** 是否跳过 refresh token 续期 */
  skipRefreshToken?: boolean;
  /** 是否静默处理错误提示 */
  silentError?: boolean;
  /** 内部标记：当前请求是否已经重试过 */
  _retry?: boolean;
}

export interface ResponseResult<T = any> {
  code: number;
  data: T;
  message?: string;
  msg?: string;
  success: boolean;
}

export interface RequestError {
  code?: number;
  message: string;
  response?: AxiosResponse;
}

type UnauthorizedHandler = () => void | Promise<void>;
type JsonRecord = Record<string, unknown>;

// 创建 axios 实例
const service: AxiosInstance = axios.create({
  baseURL: resolveApiBaseUrl(),
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
  },
});

// 请求队列
let requestCount = 0;

// 401 重定向保护标志
let isRedirecting = false;
let unauthorizedHandler: UnauthorizedHandler | null = null;
let refreshPromise: Promise<string | null> | null = null;
const REFRESH_BEFORE_EXPIRE_MS = 5 * 60 * 1000;
const AUTH_EXPIRED_CODES = new Set([401, 1410, 1411]);

function resolveApiBaseUrl(): string {
  const wujieRuntime = typeof window !== 'undefined'
    ? (window as any).$wujie?.props?.mangoRuntime
    : undefined;
  return wujieRuntime?.apiBaseUrl || '/api';
}

export function registerUnauthorizedHandler(handler: UnauthorizedHandler): void {
  unauthorizedHandler = handler;
}

export function setRequestBaseUrl(baseURL: string): void {
  service.defaults.baseURL = baseURL;
}

async function redirectToLogin(): Promise<void> {
  if (unauthorizedHandler) {
    await unauthorizedHandler();
    return;
  }

  if (typeof window !== 'undefined') {
    window.location.hash = '/login';
  }
}

async function handleUnauthorized(message?: string): Promise<void> {
  if (isRedirecting) {
    return;
  }
  isRedirecting = true;
  Session.clearSession();
  mangoMessage.warning(message || '登录已过期，请重新登录');
  try {
    await redirectToLogin();
  } finally {
    isRedirecting = false;
  }
}

function shouldRefreshToken(config: RequestConfig): boolean {
  if (config.ignoreToken || config.skipRefreshToken) {
    return false;
  }
  const refreshToken = Session.getRefreshToken?.();
  if (!refreshToken) {
    return false;
  }
  const expiresAt = Session.getTokenExpiresAt?.();
  return !expiresAt || expiresAt - Date.now() <= REFRESH_BEFORE_EXPIRE_MS;
}

function persistLoginSession(data: any): string | null {
  const token = data?.accessToken || data?.token;
  if (!token) {
    return null;
  }
  Session.setToken(token, {
    refreshToken: data?.refreshToken || Session.getRefreshToken?.() || undefined,
    expiresIn: Number(data?.expiresIn) || undefined,
  });
  const userInfo = Session.get('userInfo') || {};
  const mergedUserInfo = {
    ...userInfo,
    ...data,
    tenantId: data?.tenantId ?? userInfo.tenantId,
    tenantCode: data?.tenantCode ?? userInfo.tenantCode,
    tenantName: data?.tenantName ?? userInfo.tenantName,
    realm: data?.realm ?? userInfo.realm,
    actorType: data?.actorType ?? userInfo.actorType,
    partyType: data?.partyType ?? userInfo.partyType,
    partyId: data?.partyId ?? userInfo.partyId,
    appCode: data?.appCode ?? userInfo.appCode,
  };
  Session.set('userInfo', mergedUserInfo);
  if (mergedUserInfo.tenantId) {
    Session.set('tenantId', mergedUserInfo.tenantId);
  }
  return token;
}

function refreshAccessToken(): Promise<string | null> {
  if (refreshPromise) {
    return refreshPromise;
  }
  const refreshToken = Session.getRefreshToken?.();
  if (!refreshToken) {
    return Promise.resolve(null);
  }
  refreshPromise = service.post('/auth/refresh', { refreshToken }, {
    ignoreToken: true,
    skipRefreshToken: true,
    silentError: true,
  } as RequestConfig)
    .then((data: any) => persistLoginSession(data))
    .catch(() => null)
    .finally(() => {
      refreshPromise = null;
    });
  return refreshPromise;
}

/**
 * 显示 Loading
 */
function showLoading(): void {
  requestCount++;
  if (requestCount === 1) {
    // 可在此添加全局 Loading
  }
}

/**
 * 隐藏 Loading
 */
function hideLoading(): void {
  requestCount--;
  if (requestCount <= 0) {
    requestCount = 0;
  }
}

/**
 * 处理 Token
 */
function ensureHeaders(config: InternalAxiosRequestConfig): AxiosHeaders {
  if (config.headers instanceof AxiosHeaders) {
    return config.headers;
  }
  config.headers = new AxiosHeaders(config.headers);
  return config.headers;
}

function handleToken(config: InternalAxiosRequestConfig): InternalAxiosRequestConfig {
  const token = Session.getToken();
  if (token) {
    ensureHeaders(config).set('Authorization', `Bearer ${token}`);
  }
  return config;
}

/**
 * 处理机构隔离 ID
 */
function handleTenantId(config: InternalAxiosRequestConfig): InternalAxiosRequestConfig {
  const userInfo = Session.get('userInfo');
  const tenantId = userInfo?.tenantId ?? Session.get('tenantId');
  if (tenantId !== undefined && tenantId !== null && tenantId !== '') {
    const headers = ensureHeaders(config);
    headers.set('X-Mango-Tenant-Id', String(tenantId));
    headers.set('TENANT-ID', String(tenantId));
  }
  return config;
}


/**
 * 请求拦截器
 */
service.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    const requestConfig = config as InternalAxiosRequestConfig & RequestConfig;
    showLoading();

    if (shouldRefreshToken(requestConfig)) {
      const refreshedToken = await refreshAccessToken();
      if (refreshedToken) {
        ensureHeaders(config).set('Authorization', `Bearer ${refreshedToken}`);
      }
    }

    // 添加 Token
    if (!requestConfig.ignoreToken) {
      handleToken(config);
    }

    // 添加机构隔离 ID
    handleTenantId(config);

    return config;
  },
  (error) => {
    hideLoading();
    return Promise.reject(error);
  }
);

/**
 * 响应拦截器
 */
service.interceptors.response.use(
  async (response: AxiosResponse<ResponseResult>) => {
    hideLoading();

    const config = response.config as RequestConfig;
    if (config.rawResponse) {
      return response as any;
    }

    const { code, data, success } = response.data;
    const message = response.data.message || response.data.msg;

    // 成功
    if (success || code === 200) {
      return normalizeApiPayload(data);
    }

    // token 过期
    if (isAuthExpiredCode(code)) {
      if (!config._retry && !config.skipRefreshToken && Session.getRefreshToken?.()) {
        config._retry = true;
        const refreshedToken = await refreshAccessToken();
        if (refreshedToken) {
          config.headers = config.headers || {};
          config.headers['Authorization'] = `Bearer ${refreshedToken}`;
          return service(config);
        }
      }
      if (!config.ignoreToken) {
        void handleUnauthorized(message || '登录已过期，请重新登录');
      }
      return Promise.reject(createRequestError(message || '登录已过期', code, response));
    }

    // 其他错误
    const errorMessage = resolveBusinessErrorMessage(code, message);
    if (!config.silentError) {
      mangoMessage.error(errorMessage);
    }
    return Promise.reject(createRequestError(errorMessage, code, response));
  },
  async (error) => {
    hideLoading();

    // 处理 HTTP 错误
    const config = (error.config || {}) as RequestConfig;
    const status = error.response?.status;
    const responseData = error.response?.data;
    const message = resolveHttpErrorMessage(status, responseData, error.message);

    if (status === 401) {
      if (!config._retry && !config.skipRefreshToken && Session.getRefreshToken?.()) {
        config._retry = true;
        const refreshedToken = await refreshAccessToken();
        if (refreshedToken) {
          config.headers = config.headers || {};
          config.headers['Authorization'] = `Bearer ${refreshedToken}`;
          return service(config);
        }
      }
      if (!config.ignoreToken) {
        void handleUnauthorized('登录已过期，请重新登录');
      }
    } else if (!config.silentError) {
      mangoMessage.error(message);
    }

    return Promise.reject(error);
  }
);

/**
 * 封装 GET 请求
 */
export function get<T = any>(url: string, config?: RequestConfig): Promise<T> {
  return service.get(url, config);
}

/**
 * 封装 POST 请求
 */
export function post<T = any>(url: string, data?: any, config?: RequestConfig): Promise<T> {
  return service.post(url, data, config);
}

/**
 * 封装 PUT 请求
 */
export function put<T = any>(url: string, data?: any, config?: RequestConfig): Promise<T> {
  return service.put(url, data, config);
}

/**
 * 封装 DELETE 请求
 */
export function del<T = any>(url: string, config?: RequestConfig): Promise<T> {
  return service.delete(url, config);
}

export const request = service;

function resolveBusinessErrorMessage(code: number, message?: string): string {
  if (message) {
    return message;
  }
  return errorCodeMessage[code] || '请求失败';
}

function isAuthExpiredCode(code?: number): boolean {
  return typeof code === 'number' && AUTH_EXPIRED_CODES.has(code);
}

export function resolveHttpErrorMessage(
  status?: number,
  responseData?: Record<string, any>,
  fallbackMessage?: string
): string {
  const responseMessage = responseData?.message
    || responseData?.msg
    || responseData?.error;
  if (responseMessage) {
    return String(responseMessage);
  }
  return (status ? errorCodeMessage[status] : undefined)
    || fallbackMessage
    || '网络错误';
}

function createRequestError(message: string, code?: number, response?: AxiosResponse): RequestError & Error {
  const error = new Error(message) as RequestError & Error;
  error.code = code;
  error.response = response;
  return error;
}

/**
 * 后端 Long ID 正常应由 mango-infra-web-starter 序列化为字符串。
 * 这里作为前端兜底，只处理明确的标识字段，避免影响页码、状态、金额、排序等真实数值。
 */
export function normalizeApiPayload<T>(payload: T): T {
  return normalizeValue(payload, '') as T;
}

function normalizeValue(value: unknown, key: string): unknown {
  if (Array.isArray(value)) {
    return value.map(item => normalizeValue(item, singularKey(key)));
  }
  if (value && typeof value === 'object') {
    const record = value as JsonRecord;
    const normalized: JsonRecord = {};
    Object.keys(record).forEach((childKey) => {
      normalized[childKey] = normalizeValue(record[childKey], childKey);
    });
    return normalized;
  }
  if (typeof value === 'number' && shouldTreatAsApiId(key)) {
    return String(value) as ApiId;
  }
  return value;
}

function shouldTreatAsApiId(key: string): boolean {
  if (!key) {
    return false;
  }
  return /(^id$|Id$|Ids$|By$)/.test(key);
}

function singularKey(key: string): string {
  return key.endsWith('Ids') ? `${key.slice(0, -1)}` : key;
}

/**
 * 错误码对照表
 */
const errorCodeMessage: Record<number, string> = {
  400: '请求参数错误',
  401: '未授权，请登录',
  403: '拒绝访问',
  404: '请求的资源不存在',
  405: '请求方法不允许',
  408: '请求超时',
  500: '服务器内部错误',
  501: '服务未实现',
  502: '网关错误',
  503: '服务不可用',
  504: '网关超时',
};

export default service;
