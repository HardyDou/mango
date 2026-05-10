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
}

export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export const loginLogApi = {
  list: (params?: LoginLogQuery) => {
    return get<PageResult<SysLoginLog>>('/system/log/login/list', { params });
  },
  detail: (id: number) => {
    return get<SysLoginLog>('/system/log/login/detail', { params: { id } });
  },
  clean: (days: number) => {
    return del<void>('/system/log/login/clean', { params: { days } });
  },
  statistics: (params?: { startTime?: string; endTime?: string }) => {
    return get<LoginStatistics>('/system/log/login/statistics', { params });
  },
};

// ==================== 操作日志 ====================

export interface SysOperationLog {
  id?: number;
  username: string;
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
    return get<PageResult<SysOperationLog>>('/system/log/operation/list', { params });
  },
  detail: (id: number) => {
    return get<SysOperationLog>('/system/log/operation/detail', { params: { id } });
  },
  clean: (days: number) => {
    return del<void>('/system/log/operation/clean', { params: { days } });
  },
  export: (params?: { startTime?: string; endTime?: string }) => {
    return get<void>('/system/log/operation/export', { params });
  },
};
