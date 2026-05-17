/**
 * File Storage API - 文件存储配置。
 */

import { del, get, post, put } from '@mango/common';
import type { ApiId } from '@mango/api-schema';

export type FileStorageType = 'LOCAL' | 'S3' | 'MINIO' | 'AWS_S3' | 'ALIYUN_OSS' | 'TENCENT_COS' | 'QINIU_KODO';

export interface FileStorageConfig {
  id?: ApiId;
  configName: string;
  storageType: FileStorageType;
  endpoint?: string;
  publicEndpoint?: string;
  region?: string;
  bucketName: string;
  storagePath?: string;
  accessKey?: string;
  secretKey?: string;
  secretConfigured?: boolean;
  pathStyleAccess?: boolean;
  sslEnabled?: boolean;
  active?: boolean;
  status?: number;
  remark?: string;
  createdTime?: string;
  updatedTime?: string;
}

export interface FileStorageConfigQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  storageType?: FileStorageType | '';
  active?: boolean | '';
  status?: number;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export interface TestStorageConfigCommand {
  id?: ApiId;
  config?: FileStorageConfig;
}

export interface TestStorageConfigResult {
  success: boolean;
  message: string;
}

export const fileStorageApi = {
  page: (params?: FileStorageConfigQuery) => get<any>('/file/storage-configs/page', { params: toBackendPageParams(params) })
    .then((data) => fromBackendPageResult(data, fromBackendStorageConfig, params)),
  detail: (id: ApiId) => get<FileStorageConfig>('/file/storage-configs/detail', { params: { id } }).then(fromBackendStorageConfig),
  create: (data: FileStorageConfig) => post<ApiId>('/file/storage-configs', toBackendStorageConfig(data)),
  update: (data: FileStorageConfig) => put<boolean>('/file/storage-configs', toBackendStorageConfig(data)),
  delete: (id: ApiId) => del<boolean>('/file/storage-configs', { params: { id } }),
  activate: (id: ApiId) => put<boolean>('/file/storage-configs/active', undefined, { params: { id } }),
  test: (command: TestStorageConfigCommand) => post<TestStorageConfigResult>('/file/storage-configs/test', command),
};

export const storageTypeOptions: Array<{ label: string; value: FileStorageType }> = [
  { label: '本地存储', value: 'LOCAL' },
  { label: 'S3 兼容', value: 'S3' },
  { label: 'MinIO', value: 'MINIO' },
  { label: 'AWS S3', value: 'AWS_S3' },
  { label: '阿里云 OSS', value: 'ALIYUN_OSS' },
  { label: '腾讯云 COS', value: 'TENCENT_COS' },
  { label: '七牛云 Kodo', value: 'QINIU_KODO' },
];

export function storageTypeLabel(value?: string) {
  return storageTypeOptions.find(item => item.value === value)?.label || value || '-';
}

function toBackendStorageConfig(data: FileStorageConfig) {
  return {
    ...data,
    secretKey: data.secretKey || undefined,
    status: data.status ?? 1,
    pathStyleAccess: Boolean(data.pathStyleAccess),
    sslEnabled: Boolean(data.sslEnabled),
    active: Boolean(data.active),
  };
}

function fromBackendStorageConfig(item: any): FileStorageConfig {
  return {
    ...item,
    id: normalizeId(item.id),
    pathStyleAccess: Boolean(item.pathStyleAccess),
    sslEnabled: Boolean(item.sslEnabled),
    active: Boolean(item.active),
    secretConfigured: Boolean(item.secretConfigured),
    createdTime: normalizeDateTime(item.createdTime),
    updatedTime: normalizeDateTime(item.updatedTime),
  };
}

function normalizeId(value: any): ApiId | undefined {
  return value === undefined || value === null ? undefined : String(value);
}

function toBackendPageParams(params?: FileStorageConfigQuery) {
  if (!params) return params;
  const { pageNum, pageSize, active, ...rest } = params;
  return {
    ...rest,
    active: active === '' ? undefined : active,
    page: pageNum,
    size: pageSize,
  };
}

function fromBackendPageResult<T>(
  data: any,
  mapper: (item: any) => T,
  params?: { pageNum?: number; pageSize?: number },
): PageResult<T> {
  const list = Array.isArray(data?.list) ? data.list.map(mapper) : [];
  return {
    list,
    total: Number(data?.total ?? list.length),
    pageNum: Number(data?.page ?? params?.pageNum ?? 1),
    pageSize: Number(data?.size ?? params?.pageSize ?? list.length ?? 10),
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
