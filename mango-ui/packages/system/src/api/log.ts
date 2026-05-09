/**
 * System Log API - 系统日志
 */

import { get, del } from '@mango/common';

// ==================== 登录日志 ====================

export interface SysLoginLog {
  id?: number;
  username: string;
  ip: string;
  location?: string;
  browser?: string;
  os?: string;
  userAgent?: string;
  loginTime: string;
  status: number;
  msg?: string;
  tenantId?: number;
}

export interface LoginLogQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  status?: number;
  startTime?: string;
  endTime?: string;
}

export interface LoginStatistics {
  totalCount: number;
  successCount: number;
  failCount: number;
  todayCount: number;
  weekCount?: number;
  monthCount?: number;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export const loginLogApi = {
  list: (params?: LoginLogQuery) => get<any>('/system/log/login/list', { params: toBackendPageParams(params) })
    .then((data) => fromBackendPageResult(data, fromBackendLoginLog, params)),
  detail: (id: number) => {
    return get<any>('/system/log/login/detail', { params: { id } }).then(fromBackendLoginLog);
  },
  clean: (retentionDays?: number) => {
    return del<boolean>('/system/log/login/clean', { params: { retentionDays } });
  },
  statistics: (params?: { startTime?: string; endTime?: string }) => {
    return get<any>('/system/log/login/statistics', { params }).then((data) => ({
      totalCount: data.totalCount ?? 0,
      successCount: data.successCount ?? 0,
      failCount: data.failCount ?? 0,
      todayCount: data.todayCount ?? 0,
      weekCount: data.weekCount,
      monthCount: data.monthCount,
    }));
  },
};

// ==================== 操作日志 ====================

export interface SysOperationLog {
  id?: number;
  username: string;
  module?: string;
  operation: string;
  requestMethod: string;
  handlerMethod?: string;
  requestUrl: string;
  requestParams?: string;
  requestBody?: string;
  responseResult?: string;
  operateTime: string;
  costTime: number;
  ip: string;
  location?: string;
  status: number;
  errorMsg?: string;
  tenantId?: number;
}

export interface OperationLogQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  username?: string;
  startTime?: string;
  endTime?: string;
}

export const operationLogApi = {
  list: (params?: OperationLogQuery) => get<any>('/system/log/operation/list', { params: toBackendPageParams(params) })
    .then((data) => fromBackendPageResult(data, fromBackendOperationLog, params)),
  detail: (id: number) => {
    return get<any>('/system/log/operation/detail', { params: { id } }).then(fromBackendOperationLog);
  },
  clean: (retentionDays?: number) => {
    return del<boolean>('/system/log/operation/clean', { params: { retentionDays } });
  },
};

function fromBackendLoginLog(item: any): SysLoginLog {
  return {
    ...item,
    loginTime: normalizeDateTime(item.loginTime),
    userAgent: item.userAgent ?? [item.browser, item.os].filter(Boolean).join(' / '),
  };
}

function fromBackendOperationLog(item: any): SysOperationLog {
  return {
    ...item,
    operateTime: normalizeDateTime(item.operateTime),
    requestMethod: item.requestMethod ?? item.method,
    handlerMethod: item.handlerMethod,
    requestUrl: item.requestUrl ?? item.url,
    requestParams: item.requestParams ?? item.params,
    responseResult: item.responseResult ?? item.result,
    costTime: item.costTime ?? item.duration ?? 0,
  };
}

function normalizeDateTime(value: any): string {
  if (!value) return '';
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value;
    return `${year}-${pad(month)}-${pad(day)} ${pad(hour)}:${pad(minute)}:${pad(second)}`;
  }
  return String(value).replace('T', ' ');
}

function pad(value: number): string {
  return String(value).padStart(2, '0');
}

function toBackendPageParams(params?: Record<string, any>) {
  if (!params) return params;
  const { pageNum, pageSize, ...rest } = params;
  return {
    ...rest,
    page: pageNum,
    size: pageSize,
  };
}

function fromBackendPageResult<T>(
  data: any,
  mapper: (item: any) => T,
  params?: { pageNum?: number; pageSize?: number },
): PageResult<T> {
  if (Array.isArray(data)) {
    return toPageResult(data.map(mapper), params);
  }
  const list = Array.isArray(data?.list) ? data.list.map(mapper) : [];
  return {
    list,
    total: Number(data?.total ?? list.length),
    pageNum: Number(data?.page ?? params?.pageNum ?? 1),
    pageSize: Number(data?.size ?? params?.pageSize ?? list.length ?? 10),
  };
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
