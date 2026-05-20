/**
 * File API - 文件管理。
 */

import { del, get, post, request, type RequestConfig } from '@mango/common';
import type { ApiId } from '@mango/api-schema';

export type FileId = ApiId;
export type FileBizMeta = Record<string, unknown> | string;

export interface FileRecord {
  id: FileId;
  tenantId?: FileId;
  bizType?: string;
  bizId?: string;
  purpose?: string;
  bizMeta?: FileBizMeta;
  directoryId?: FileId;
  directoryName?: string;
  accessLevel?: string;
  storageType?: string;
  storageConfigId?: FileId;
  bucketName?: string;
  objectName?: string;
  fileName: string;
  fileExt?: string;
  fileSize: number;
  contentType?: string;
  fileHash?: string;
  status?: number;
  archived?: number;
  createdBy?: FileId;
  createdTime?: string;
  updatedTime?: string;
  url?: string;
  previewUrl?: string;
  downloadUrl?: string;
  directAccess?: boolean;
  directPreviewUrl?: string;
  directDownloadUrl?: string;
  directPreviewExpireSeconds?: number;
  directDownloadExpireSeconds?: number;
}

export interface FilePreview {
  id: FileId;
  fileName: string;
  fileExt?: string;
  fileSize: number;
  contentType?: string;
  previewable: boolean;
  previewUrl: string;
  downloadUrl: string;
  directAccess?: boolean;
  directPreviewUrl?: string;
  directDownloadUrl?: string;
  directPreviewExpireSeconds?: number;
  directDownloadExpireSeconds?: number;
}

export interface FileQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  bizType?: string;
  bizId?: string;
  purpose?: string;
  accessLevel?: string;
  directoryId?: FileId;
  status?: number;
  includeArchived?: boolean;
}

export type FileUploadParams = Partial<Pick<FileRecord, 'purpose' | 'accessLevel' | 'bizType' | 'bizId' | 'bizMeta' | 'directoryId'>>;
export type FileUploadOptions = Pick<RequestConfig, 'onUploadProgress'>;

export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export const fileApi = {
  page: (params?: FileQuery) => get<any>('/file/files/page', { params: toBackendPageParams(params) })
    .then((data) => fromBackendPageResult(data, fromBackendFileRecord, params)),
  detail: (id: FileId) => get<FileRecord>('/file/files/detail', { params: { id } }).then(fromBackendFileRecord),
  preview: (id: FileId) => get<FilePreview>('/file/files/preview', { params: { id } }).then((item: any) => ({
    ...item,
    id: normalizeId(item.id),
    previewable: Boolean(item.previewable),
    previewUrl: normalizeApiUrl(item.previewUrl),
    downloadUrl: normalizeApiUrl(item.downloadUrl),
    directAccess: Boolean(item.directAccess),
    directPreviewUrl: normalizeApiUrl(item.directPreviewUrl),
    directDownloadUrl: normalizeApiUrl(item.directDownloadUrl),
    directPreviewExpireSeconds: Number(item.directPreviewExpireSeconds ?? 0),
    directDownloadExpireSeconds: Number(item.directDownloadExpireSeconds ?? 0),
  })),
  upload: (file: File, params?: FileUploadParams, options?: FileUploadOptions) => {
    const formData = new FormData();
    formData.append('file', file);
    appendOptional(formData, params);
    return post<FileRecord>('/file/files', formData as any, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: options?.onUploadProgress,
    }).then(fromBackendFileRecord);
  },
  uploadBatch: (files: File[], params?: FileUploadParams, options?: FileUploadOptions) => {
    const formData = new FormData();
    files.forEach(file => formData.append('files', file));
    appendOptional(formData, params);
    return post<FileRecord[]>('/file/files/batch', formData as any, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: options?.onUploadProgress,
    }).then(items => (items || []).map(fromBackendFileRecord));
  },
  archive: (id: FileId, reason?: string) => del<boolean>('/file/files', { params: { id, reason } }),
  downloadUrl: (id: FileId) => `/api/file/files/download?id=${encodeURIComponent(String(id))}`,
  download: async (id: FileId) => {
    const response = await request.get('/file/files/download', {
      params: { id },
      responseType: 'blob',
      rawResponse: true,
    } as any);
    await assertBinaryDownloadResponse(response as any);
    return response as any;
  },
};

export async function downloadFileRecord(row: Pick<FileRecord, 'id' | 'fileName'> & Partial<Pick<FilePreview, 'directDownloadUrl'>>) {
  if (row.directDownloadUrl) {
    openDirectDownload(row.directDownloadUrl, row.fileName || `file-${row.id}`);
    return;
  }
  const preview = await fileApi.preview(row.id);
  if (preview.directDownloadUrl) {
    openDirectDownload(preview.directDownloadUrl, preview.fileName || row.fileName || `file-${row.id}`);
    return;
  }
  openDirectDownload(fileApi.downloadUrl(row.id), row.fileName || `file-${row.id}`);
}

function openDirectDownload(url: string, fileName: string) {
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.rel = 'noopener noreferrer';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

function appendOptional(formData: FormData, params?: Record<string, any>) {
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      formData.append(key, String(value));
    }
  });
}

function fromBackendFileRecord(item: any): FileRecord {
  return {
    ...item,
    id: normalizeId(item.id),
    tenantId: normalizeId(item.tenantId),
    storageConfigId: normalizeId(item.storageConfigId),
    createdBy: normalizeId(item.createdBy),
    directoryId: normalizeId(item.directoryId ?? 0),
    bizMeta: parseBizMeta(item.bizMeta),
    fileSize: Number(item.fileSize ?? 0),
    createdTime: normalizeDateTime(item.createdTime),
    updatedTime: normalizeDateTime(item.updatedTime),
    url: normalizeApiUrl(item.url),
    previewUrl: normalizeApiUrl(item.previewUrl),
    downloadUrl: normalizeApiUrl(item.downloadUrl),
    directAccess: Boolean(item.directAccess),
    directPreviewUrl: normalizeApiUrl(item.directPreviewUrl),
    directDownloadUrl: normalizeApiUrl(item.directDownloadUrl),
    directPreviewExpireSeconds: Number(item.directPreviewExpireSeconds ?? 0),
    directDownloadExpireSeconds: Number(item.directDownloadExpireSeconds ?? 0),
  };
}

function normalizeId(value: any): FileId {
  return value === undefined || value === null ? '' : String(value);
}

function parseBizMeta(value: any): FileBizMeta | undefined {
  if (!value) return undefined;
  if (typeof value !== 'string') return value;
  try {
    return JSON.parse(value);
  } catch {
    return value;
  }
}

async function assertBinaryDownloadResponse(response: any) {
  const contentType = String(response.headers?.['content-type'] || '').toLowerCase();
  if (!contentType.includes('application/json')) return;
  const data = response.data;
  const text = data instanceof Blob ? await data.text() : String(data || '');
  try {
    const body = JSON.parse(text);
    throw new Error(body?.msg || body?.message || '文件下载失败');
  } catch (error) {
    if (error instanceof SyntaxError) {
      throw new Error('文件下载失败');
    }
    throw error;
  }
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

function toBackendPageParams(params?: FileQuery) {
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
  const list = Array.isArray(data?.list) ? data.list.map(mapper) : [];
  return {
    list,
    total: Number(data?.total ?? list.length),
    pageNum: Number(data?.page ?? params?.pageNum ?? 1),
    pageSize: Number(data?.size ?? params?.pageSize ?? list.length ?? 10),
  };
}

function normalizeApiUrl(value?: string): string {
  if (!value) return '';
  if (/^https?:\/\//i.test(value) || value.startsWith('/api/')) return value;
  return value.startsWith('/') ? `/api${value}` : value;
}
