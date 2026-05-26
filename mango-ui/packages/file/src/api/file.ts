/**
 * File API - 文件管理。
 */

import { del, get, post, put, request, type RequestConfig } from '@mango/common';
import type { ApiId } from '@mango/api-schema';

export type FileId = ApiId;
export type FileBizMeta = Record<string, unknown> | string;
export type FileReference = FileId | number | `mango-file:${string}` | FileRecord | FilePreview | null | undefined;

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
  objectId?: FileId;
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

export interface FilePreviewLink {
  fileId: FileId;
  fileName: string;
  previewUrl: string;
  previewToken?: string;
  expireSeconds?: number;
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
export interface FileUploadOptions extends Pick<RequestConfig, 'onUploadProgress'> {
  multipartThreshold?: number;
  chunkSize?: number;
}

export interface CreateFileUploadSessionCommand extends FileUploadParams {
  fileName: string;
  fileSize: number;
  fileHash: string;
  contentType?: string;
  chunkSize?: number;
  totalParts?: number;
}

export interface FileUploadInit {
  instant?: boolean;
  fileRecord?: FileRecord;
  sessionId?: FileId;
  uploadMode?: 'SERVER_CHUNK' | 'S3_MULTIPART';
  storageUploadId?: string;
  chunkSize?: number;
  totalParts?: number;
  expiresAt?: string;
}

export interface FileUploadPartSign {
  partNumber: number;
  uploadUrl: string;
  method: 'PUT' | string;
  expireSeconds?: number;
}

export interface CompleteFileUploadPartCommand {
  partNumber: number;
  etag: string;
  partSize?: number;
  partHash?: string;
}

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
  previewLink: (fileId: FileId) => get<FilePreviewLink>('/file-preview/files/preview-link', { params: { fileId } }).then((item: any) => ({
    ...item,
    fileId: normalizeId(item.fileId),
    previewUrl: normalizeApiUrl(item.previewUrl),
    expireSeconds: Number(item.expireSeconds ?? 0),
  })),
  upload: (file: File, params?: FileUploadParams, options?: FileUploadOptions) => uploadSmart(file, params, options),
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
  delete: (ids: FileId[]) => post<boolean>('/file/files/delete', { ids }),
  createUploadSession: (command: CreateFileUploadSessionCommand) => post<FileUploadInit>('/file/files/uploads', normalizeUploadSessionCommand(command) as any)
    .then(fromBackendUploadInit),
  createUploadPartSign: (sessionId: FileId, partNumber: number, partSize?: number) => post<FileUploadPartSign>(
    `/file/files/uploads/${encodeURIComponent(String(sessionId))}/parts/sign`,
    { partNumber, partSize },
  ),
  uploadServerPart: (sessionId: FileId, partNumber: number, chunk: Blob, fileName: string) => {
    const formData = new FormData();
    formData.append('partNumber', String(partNumber));
    formData.append('file', chunk, fileName);
    return post<boolean>(`/file/files/uploads/${encodeURIComponent(String(sessionId))}/parts`, formData as any, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  completeUploadPart: (sessionId: FileId, command: CompleteFileUploadPartCommand) => put<boolean>(
    `/file/files/uploads/${encodeURIComponent(String(sessionId))}/parts`,
    command,
  ),
  completeUploadSession: (sessionId: FileId) => post<FileRecord>(
    `/file/files/uploads/${encodeURIComponent(String(sessionId))}/complete`,
  ).then(fromBackendFileRecord),
  abortUploadSession: (sessionId: FileId) => del<boolean>(`/file/files/uploads/${encodeURIComponent(String(sessionId))}`),
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

export function normalizeFileId(value?: FileReference): FileId {
  if (!value) return '';
  if (typeof value === 'number') {
    return isValidFileId(value) ? String(value) : '';
  }
  if (typeof value !== 'string') {
    return normalizeFileId(value.id);
  }
  const text = value.trim();
  if (!text || isFileAccessUrl(text)) return '';
  const rawId = text.startsWith('mango-file:')
    ? text.slice('mango-file:'.length).trim()
    : text;
  return isValidFileId(rawId) ? rawId : '';
}

export function fileToken(id?: FileId): string {
  const normalized = normalizeFileId(id);
  return normalized ? `mango-file:${normalized}` : '';
}

export function isValidFileId(value?: unknown): value is FileId {
  if (value === undefined || value === null) return false;
  return /^[1-9]\d*$/.test(String(value).trim());
}

export function fileRuntimeUrl(record?: Partial<FileRecord | FilePreview> | null): string {
  if (!record) return '';
  const candidates = [
    record.directPreviewUrl,
    record.directDownloadUrl,
    'url' in record ? record.url : '',
    record.previewUrl,
    record.downloadUrl,
  ];
  return candidates.find(item => Boolean(item && !isProtectedApiUrl(item))) || '';
}

export function isFileAccessUrl(value?: string): boolean {
  if (!value) return false;
  return /^(https?:|data:|blob:)/i.test(value) || value.startsWith('/') || isProtectedApiUrl(value);
}

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

export const DEFAULT_MULTIPART_THRESHOLD = 20 * 1024 * 1024;
export const DEFAULT_CHUNK_SIZE = 10 * 1024 * 1024;

async function uploadSmart(file: File, params?: FileUploadParams, options?: FileUploadOptions): Promise<FileRecord> {
  const threshold = options?.multipartThreshold ?? DEFAULT_MULTIPART_THRESHOLD;
  if (file.size < threshold) {
    return uploadSimple(file, params, options);
  }
  return uploadMultipart(file, params, options);
}

function uploadSimple(file: File, params?: FileUploadParams, options?: FileUploadOptions) {
  const formData = new FormData();
  formData.append('file', file);
  appendOptional(formData, params);
  return post<FileRecord>('/file/files', formData as any, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: options?.onUploadProgress,
  }).then(fromBackendFileRecord);
}

async function uploadMultipart(file: File, params?: FileUploadParams, options?: FileUploadOptions): Promise<FileRecord> {
  const chunkSize = options?.chunkSize ?? DEFAULT_CHUNK_SIZE;
  const totalParts = Math.ceil(file.size / chunkSize);
  const fileHash = await sha256(file);
  const init = await fileApi.createUploadSession({
    ...params,
    fileName: file.name,
    fileSize: file.size,
    fileHash,
    contentType: file.type || undefined,
    chunkSize,
    totalParts,
  });
  if (init.instant && init.fileRecord) {
    options?.onUploadProgress?.({ loaded: file.size, total: file.size, progress: 1 } as any);
    return init.fileRecord;
  }
  if (!init.sessionId || !init.uploadMode) {
    throw new Error('文件上传会话创建失败');
  }
  const resolvedChunkSize = Number(init.chunkSize || chunkSize);
  const resolvedTotalParts = Number(init.totalParts || totalParts);
  try {
    for (let partNumber = 1; partNumber <= resolvedTotalParts; partNumber++) {
      const start = (partNumber - 1) * resolvedChunkSize;
      const end = Math.min(start + resolvedChunkSize, file.size);
      const chunk = file.slice(start, end);
      if (init.uploadMode === 'S3_MULTIPART') {
        await uploadS3MultipartPart(init.sessionId, partNumber, chunk);
      } else {
        await fileApi.uploadServerPart(init.sessionId, partNumber, chunk, file.name);
      }
      reportMultipartProgress(options, end, file.size);
    }
    return await fileApi.completeUploadSession(init.sessionId);
  } catch (error) {
    await fileApi.abortUploadSession(init.sessionId).catch(() => undefined);
    throw error;
  }
}

async function uploadS3MultipartPart(sessionId: FileId, partNumber: number, chunk: Blob) {
  const sign = await fileApi.createUploadPartSign(sessionId, partNumber, chunk.size);
  const response = await fetch(sign.uploadUrl, {
    method: sign.method || 'PUT',
    body: chunk,
  });
  if (!response.ok) {
    throw new Error(`分片上传失败：${response.status}`);
  }
  const etag = response.headers.get('ETag')?.replace(/^"|"$/g, '') || '';
  if (!etag) {
    throw new Error('对象存储未返回分片 ETag');
  }
  await fileApi.completeUploadPart(sessionId, {
    partNumber,
    etag,
    partSize: chunk.size,
  });
}

function reportMultipartProgress(options: FileUploadOptions | undefined, loaded: number, total: number) {
  options?.onUploadProgress?.({
    loaded,
    total,
    progress: total ? loaded / total : 0,
  } as any);
}

async function sha256(file: File): Promise<string> {
  const buffer = await file.arrayBuffer();
  const digest = await crypto.subtle.digest('SHA-256', buffer);
  return Array.from(new Uint8Array(digest))
    .map(item => item.toString(16).padStart(2, '0'))
    .join('');
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

function normalizeUploadSessionCommand(command: CreateFileUploadSessionCommand) {
  return {
    ...command,
    bizId: command.bizId ? String(command.bizId) : undefined,
    directoryId: command.directoryId ? String(command.directoryId) : undefined,
    bizMeta: normalizeBizMetaForRequest(command.bizMeta),
  };
}

function normalizeBizMetaForRequest(value?: FileBizMeta) {
  if (!value) return undefined;
  return typeof value === 'string' ? value : JSON.stringify(value);
}

function fromBackendUploadInit(item: any): FileUploadInit {
  return {
    ...item,
    instant: Boolean(item?.instant),
    fileRecord: item?.fileRecord ? fromBackendFileRecord(item.fileRecord) : undefined,
    sessionId: normalizeId(item?.sessionId),
    chunkSize: Number(item?.chunkSize ?? 0),
    totalParts: Number(item?.totalParts ?? 0),
    expiresAt: normalizeDateTime(item?.expiresAt),
  };
}

function fromBackendFileRecord(item: any): FileRecord {
  return {
    ...item,
    id: normalizeId(item.id),
    tenantId: normalizeId(item.tenantId),
    objectId: normalizeId(item.objectId),
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

function isProtectedApiUrl(value?: string) {
  if (!value) return false;
  if (value.startsWith('/api/')) return true;
  if (!/^https?:\/\//i.test(value)) return false;
  try {
    const url = new URL(value, window.location.origin);
    return url.pathname.startsWith('/api/');
  } catch {
    return false;
  }
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
