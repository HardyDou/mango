import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';
import { ElMessage } from 'element-plus';
import { Session } from './storage';

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

function resolveApiBaseUrl(): string {
  const wujieRuntime = typeof window !== 'undefined'
    ? (window as any).$wujie?.props?.mangoRuntime
    : undefined;
  return wujieRuntime?.apiBaseUrl || '/api';
}

export function registerUnauthorizedHandler(handler: UnauthorizedHandler): void {
  unauthorizedHandler = handler;
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
  ElMessage.warning(message || '登录已过期，请重新登录');
  try {
    await redirectToLogin();
  } finally {
    isRedirecting = false;
  }
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
function handleToken(config: AxiosRequestConfig): AxiosRequestConfig {
  const token = Session.getToken();
  if (token) {
    config.headers = config.headers || {};
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
}

/**
 * 处理机构隔离 ID
 */
function handleTenantId(config: AxiosRequestConfig): AxiosRequestConfig {
  const userInfo = Session.get('userInfo');
  const tenantId = userInfo?.tenantId;
  if (tenantId !== undefined && tenantId !== null && tenantId !== '') {
    config.headers = config.headers || {};
    config.headers['X-Mango-Tenant-Id'] = String(tenantId);
    config.headers['TENANT-ID'] = String(tenantId);
  }
  return config;
}


/**
 * 请求拦截器
 */
service.interceptors.request.use(
  async (config: RequestConfig) => {
    showLoading();

    // 添加 Token
    if (!config.ignoreToken) {
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
  (response: AxiosResponse<ResponseResult>) => {
    hideLoading();

    const config = response.config as RequestConfig;
    if (config.rawResponse) {
      return response as any;
    }

    const { code, data, success } = response.data;
    const message = response.data.message || response.data.msg;

    // 成功
    if (success || code === 200) {
      return data;
    }

    // token 过期
    if (code === 401) {
      void handleUnauthorized(message || '登录已过期，请重新登录');
      return Promise.reject(new Error(message || '登录已过期'));
    }

    // 其他错误
    ElMessage.error(message || '请求失败');
    return Promise.reject(new Error(message || '请求失败'));
  },
  (error) => {
    hideLoading();

    // 处理 HTTP 错误
    const status = error.response?.status;
    const responseData = error.response?.data;
    const message = responseData?.message || responseData?.msg || responseData?.error || errorCodeMessage[status] || error.message || '网络错误';

    if (status === 401) {
      void handleUnauthorized('登录已过期，请重新登录');
    } else if (status === 403) {
      ElMessage.error('没有权限访问该资源');
    } else if (status === 500) {
      ElMessage.error('服务器错误');
    } else if (status === 502) {
      ElMessage.error('网关错误');
    } else {
      ElMessage.error(message);
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
