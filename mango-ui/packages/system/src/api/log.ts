/**
 * System Log API - 系统日志
 */

import { get, post, put, del } from '@mango/common';

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
  list: (params?: LoginLogQuery) => {
    return get<any[]>('/system/log/login/list', { params })
      .then((list) => toPageResult(filterLoginLogs(list.map(fromBackendLoginLog), params), params));
  },
  detail: (id: number) => {
    return get<any>('/system/log/login/detail', { params: { id } }).then(fromBackendLoginLog);
  },
  clean: (_days?: number) => {
    return del<boolean>('/system/log/login/clean');
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
  list: (params?: OperationLogQuery) => {
    return get<any[]>('/system/log/operation/list', { params })
      .then((list) => toPageResult(filterOperationLogs(list.map(fromBackendOperationLog), params), params));
  },
  detail: (id: number) => {
    return get<any>('/system/log/operation/detail', { params: { id } }).then(fromBackendOperationLog);
  },
  clean: (_days?: number) => {
    return del<boolean>('/system/log/operation/clean');
  },
};

function fromBackendLoginLog(item: any): SysLoginLog {
  return {
    ...item,
    userAgent: item.userAgent ?? [item.browser, item.os].filter(Boolean).join(' / '),
  };
}

function fromBackendOperationLog(item: any): SysOperationLog {
  return {
    ...item,
    requestMethod: item.requestMethod ?? item.method,
    requestUrl: item.requestUrl ?? item.url,
    requestParams: item.requestParams ?? item.params,
    responseResult: item.responseResult ?? item.result,
    costTime: item.costTime ?? item.duration ?? 0,
  };
}

function filterLoginLogs(list: SysLoginLog[], params?: LoginLogQuery): SysLoginLog[] {
  return list.filter((item) => {
    const keyword = params?.keyword?.trim().toLowerCase();
    const matchesKeyword = !keyword || [item.username, item.ip].filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword));
    const matchesStatus = params?.status === undefined || item.status === params.status;
    return matchesKeyword && matchesStatus;
  });
}

function filterOperationLogs(list: SysOperationLog[], params?: OperationLogQuery): SysOperationLog[] {
  return list.filter((item) => {
    const keyword = params?.keyword?.trim().toLowerCase();
    const matchesKeyword = !keyword || [item.username, item.operation, item.requestUrl].filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword));
    const matchesUsername = !params?.username || item.username?.includes(params.username);
    return matchesKeyword && matchesUsername;
  });
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
