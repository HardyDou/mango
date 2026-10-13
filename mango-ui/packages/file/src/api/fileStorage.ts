/**
 * File Storage API - 文件存储配置。
 */

import type { ApiId, HttpClient } from '@mango/api-schema';
import { getLegacyHttpClient } from './legacyHttpClient';

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

export type FileStoragePageResult = PageResult<FileStorageConfig>;

export interface TestStorageConfigCommand {
  id?: ApiId;
  config?: FileStorageConfig;
}

export interface TestStorageConfigResult {
  success: boolean;
  message: string;
}

export interface FileStorageApi {
  page(params?: FileStorageConfigQuery): Promise<FileStoragePageResult>;
  detail(id: ApiId): Promise<FileStorageConfig>;
  create(data: FileStorageConfig): Promise<ApiId>;
  update(data: FileStorageConfig): Promise<boolean>;
  delete(id: ApiId): Promise<boolean>;
  activate(id: ApiId): Promise<boolean>;
  test(command: TestStorageConfigCommand): Promise<TestStorageConfigResult>;
}

export function createFileStorageApi(httpClient?: HttpClient): FileStorageApi {
  const transport = httpClient || getLegacyHttpClient();
  return {
    page: (params) =>
      transport
        .request<unknown>({ method: 'GET', url: '/file/storage-configs/page', query: toBackendPageParams(params) })
        .then((data) => fromBackendPageResult(data, params)),
    detail: (id) =>
      transport
        .request<unknown>({ method: 'GET', url: '/file/storage-configs/detail', query: { id } })
        .then(fromBackendStorageConfig),
    create: (data) =>
      transport.request<ApiId, ReturnType<typeof toBackendStorageConfig>>({
        method: 'POST',
        url: '/file/storage-configs',
        body: toBackendStorageConfig(data),
      }),
    update: (data) =>
      transport.request<boolean, ReturnType<typeof toBackendStorageConfig>>({
        method: 'PUT',
        url: '/file/storage-configs',
        body: toBackendStorageConfig(data),
      }),
    delete: (id) => transport.request<boolean>({ method: 'DELETE', url: '/file/storage-configs', query: { id } }),
    activate: (id) => transport.request<boolean>({ method: 'PUT', url: '/file/storage-configs/active', query: { id } }),
    test: (command) =>
      transport.request<TestStorageConfigResult, TestStorageConfigCommand>({
        method: 'POST',
        url: '/file/storage-configs/test',
        body: command,
      }),
  };
}

export function configureFileStorageApi(httpClient: HttpClient): FileStorageApi {
  return createFileStorageApi(httpClient);
}

/** Legacy entry point: no HttpClient is required and the original request path is retained. */
export const fileStorageApi = createFileStorageApi();

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
  return storageTypeOptions.find((item) => item.value === value)?.label || value || '-';
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

function fromBackendStorageConfig(item: unknown): FileStorageConfig {
  const record = item && typeof item === 'object' ? (item as Record<string, unknown>) : {};
  return {
    ...record,
    id: normalizeId(record.id),
    configName: String(record.configName || ''),
    storageType: (record.storageType || 'LOCAL') as FileStorageType,
    bucketName: String(record.bucketName || ''),
    pathStyleAccess: Boolean(record.pathStyleAccess),
    sslEnabled: Boolean(record.sslEnabled),
    active: Boolean(record.active),
    secretConfigured: Boolean(record.secretConfigured),
    createdTime: normalizeDateTime(record.createdTime),
    updatedTime: normalizeDateTime(record.updatedTime),
  } as FileStorageConfig;
}

function normalizeId(value: unknown): ApiId | undefined {
  return value === undefined || value === null ? undefined : String(value);
}

function toBackendPageParams(params?: FileStorageConfigQuery) {
  if (!params) return undefined;
  const { pageNum, pageSize, active, ...rest } = params;
  return { ...rest, active: active === '' ? undefined : active, page: pageNum, size: pageSize };
}

function fromBackendPageResult(data: unknown, params?: { pageNum?: number; pageSize?: number }): FileStoragePageResult {
  const record = data && typeof data === 'object' ? (data as Record<string, unknown>) : {};
  const list = Array.isArray(record.list) ? record.list.map(fromBackendStorageConfig) : [];
  return {
    list,
    total: Number(record.total ?? list.length),
    pageNum: Number(record.page ?? params?.pageNum ?? 1),
    pageSize: Number(record.size ?? params?.pageSize ?? (list.length || 10)),
  };
}

function normalizeDateTime(value: unknown): string {
  if (!value) return '';
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value;
    return `${year}-${pad(Number(month))}-${pad(Number(day))} ${pad(Number(hour))}:${pad(Number(minute))}:${pad(Number(second))}`;
  }
  return String(value).replace('T', ' ');
}

function pad(value: number): string {
  return String(value).padStart(2, '0');
}
