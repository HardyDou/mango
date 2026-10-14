/**
 * File API - 文件管理。
 *
 * The API is transport agnostic. Hosts create an instance-scoped HttpClient and
 * provide it through @mango/app-runtime; callers use createFileApi or useFileApi.
 */

import type { ApiId, HttpClient, HttpError, HttpFailureKind, HttpProgress } from '@mango/api-schema';
import { createFileSettingsApi, defaultFileSettings, type FileSettingsApi } from './fileSettings';
import { getLegacyHttpClient } from './legacyHttpClient';
import { sha256IfSupported, shouldUseMultipart } from './uploadSupport';

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
  previewUrl?: string;
  downloadUrl?: string;
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
  documentPreviewUrl?: string;
  directAccess?: boolean;
  directPreviewUrl?: string;
  directDownloadUrl?: string;
  directPreviewExpireSeconds?: number;
  directDownloadExpireSeconds?: number;
}

type FileRuntimeAccess = Partial<
  Pick<
    FilePreview,
    | 'directAccess'
    | 'directPreviewUrl'
    | 'directDownloadUrl'
    | 'directPreviewExpireSeconds'
    | 'directDownloadExpireSeconds'
  >
> & {
  url?: string;
};

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

export type FileUploadParams = Partial<
  Pick<FileRecord, 'purpose' | 'accessLevel' | 'bizType' | 'bizId' | 'bizMeta' | 'directoryId'>
>;

export interface LegacyUploadProgress extends HttpProgress {
  bytes: number;
  lengthComputable: boolean;
}

export type FileUploadProgressHandler = ((progress: HttpProgress) => void) | ((progress: LegacyUploadProgress) => void);

export interface FileUploadOptions {
  /** Accepts both the new vendor-neutral progress shape and the legacy Axios-compatible shape. */
  onUploadProgress?: FileUploadProgressHandler;
  /** Maximum total duration for each upload request. */
  timeoutMs?: number;
  /** Maximum duration without transport progress for each upload request. */
  inactivityTimeoutMs?: number;
  multipartEnabled?: boolean;
  multipartThreshold?: number;
  chunkSize?: number;
}

export interface FileUploadPolicy {
  multipartEnabled: boolean;
  multipartThreshold: number;
}

/** 文件下载参数。 */
export interface FileDownloadOptions {
  /** 压缩档位，取值为 none、low、standard、medium、high、extreme；缺省表示不压缩。 */
  compression?: string;
  /** 单文件目标大小，单位字节；设置后服务端会按目标体积提升压缩强度。 */
  perFileTargetSizeBytes?: number;
}

export interface CreateFileUploadSessionCommand extends FileUploadParams {
  fileName: string;
  fileSize: number;
  fileHash?: string;
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

interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export interface BinaryDownloadResponse {
  data: Blob;
  headers?: Record<string, string | undefined>;
}

export interface FileApi {
  page(params?: FileQuery): Promise<PageResult<FileRecord>>;
  detail(id: FileId): Promise<FileRecord>;
  preview(id: FileId): Promise<FilePreview>;
  previewLink(fileId: FileId): Promise<FilePreviewLink>;
  previewContent(id: FileId): Promise<BinaryDownloadResponse>;
  upload(file: File, params?: FileUploadParams, options?: FileUploadOptions): Promise<FileRecord>;
  uploadPolicy(options?: FileUploadOptions): Promise<FileUploadPolicy>;
  uploadBatch(files: File[], params?: FileUploadParams, options?: FileUploadOptions): Promise<FileRecord[]>;
  archive(id: FileId, reason?: string): Promise<boolean>;
  delete(ids: FileId[]): Promise<boolean>;
  createUploadSession(command: CreateFileUploadSessionCommand, options?: FileUploadOptions): Promise<FileUploadInit>;
  createUploadPartSign(
    sessionId: FileId,
    partNumber: number,
    partSize?: number,
    options?: FileUploadOptions,
  ): Promise<FileUploadPartSign>;
  uploadServerPart(
    sessionId: FileId,
    partNumber: number,
    chunk: Blob,
    fileName: string,
    options?: FileUploadOptions,
  ): Promise<boolean>;
  completeUploadPart(
    sessionId: FileId,
    command: CompleteFileUploadPartCommand,
    options?: FileUploadOptions,
  ): Promise<boolean>;
  completeUploadSession(sessionId: FileId, options?: FileUploadOptions): Promise<FileRecord>;
  abortUploadSession(sessionId: FileId, options?: FileUploadOptions): Promise<boolean>;
  downloadUrl(id: FileId, options?: FileDownloadOptions): string;
  download(id: FileId, options?: FileDownloadOptions): Promise<BinaryDownloadResponse>;
}

export class FileHttpError extends Error implements HttpError {
  readonly name = 'HttpError' as const;
  readonly kind: HttpFailureKind;
  readonly status?: number;
  readonly code?: string;
  readonly retryable: boolean;
  readonly requestId?: string;
  readonly details?: unknown;

  constructor(message: string, options: { kind: HttpFailureKind; status?: number; code?: string; details?: unknown }) {
    super(message);
    this.kind = options.kind;
    this.status = options.status;
    this.code = options.code;
    this.retryable = false;
    this.details = options.details;
  }
}

export const DEFAULT_MULTIPART_THRESHOLD = 20 * 1024 * 1024;
export const DEFAULT_CHUNK_SIZE = 10 * 1024 * 1024;
export const DEFAULT_UPLOAD_TIMEOUT_MS = 10 * 60 * 1000;
export const DEFAULT_UPLOAD_INACTIVITY_TIMEOUT_MS = 60 * 1000;

export function createFileApi(httpClient?: HttpClient): FileApi {
  const transport = httpClient || getLegacyHttpClient();
  const settingsApi = createFileSettingsApi(transport);
  const api: FileApi = {
    page: (params) =>
      transport
        .request<unknown>({ method: 'GET', url: '/file/files/page', query: toBackendPageParams(params) })
        .then((data) => fromBackendPageResult(data, fromBackendFileRecord, params)),
    detail: (id) =>
      transport
        .request<unknown>({ method: 'GET', url: '/file/files/detail', query: { id } })
        .then(fromBackendFileRecord),
    preview: (id) =>
      transport.request<unknown>({ method: 'GET', url: '/file/files/preview', query: { id } }).then((item) => {
        const record = asRecord(item);
        return {
          id: normalizeId(record.id),
          fileName: stringValue(record.fileName) || '',
          fileExt: stringValue(record.fileExt),
          fileSize: Number(record.fileSize ?? 0),
          contentType: stringValue(record.contentType),
          previewable: Boolean(record.previewable),
          previewUrl: normalizeApiUrl(stringValue(record.previewUrl)),
          downloadUrl: normalizeApiUrl(stringValue(record.downloadUrl)),
          documentPreviewUrl: normalizeApiUrl(stringValue(record.documentPreviewUrl)),
          directAccess: Boolean(record.directAccess),
          directPreviewUrl: normalizeApiUrl(stringValue(record.directPreviewUrl)),
          directDownloadUrl: normalizeApiUrl(stringValue(record.directDownloadUrl)),
          directPreviewExpireSeconds: Number(record.directPreviewExpireSeconds ?? 0),
          directDownloadExpireSeconds: Number(record.directDownloadExpireSeconds ?? 0),
        } satisfies FilePreview;
      }),
    previewLink: (fileId) =>
      transport
        .request<unknown>({ method: 'GET', url: '/file-preview/files/preview-link', query: { fileId } })
        .then((item) => {
          const record = asRecord(item);
          return {
            ...record,
            fileId: normalizeId(record.fileId),
            previewUrl: normalizeApiUrl(stringValue(record.previewUrl)),
            expireSeconds: Number(record.expireSeconds ?? 0),
          } as FilePreviewLink;
        }),
    previewContent: async (id) => {
      const response = await transport.request<Blob | BinaryDownloadResponse>({
        method: 'GET',
        url: '/file/files/preview-content',
        query: { id },
        responseType: 'blob',
      });
      const result = toBinaryDownloadResponse(response);
      await assertBinaryBlob(result.data, '文件预览失败');
      return result;
    },
    upload: (file, params, options) => uploadSmart(api, settingsApi, transport, file, params, options),
    uploadPolicy: (options) => resolveUploadPolicy(settingsApi, options),
    uploadBatch: async (files, params, options) => {
      const formData = new FormData();
      files.forEach((file) => formData.append('files', file));
      appendOptional(formData, params);
      const items = await transport.request<unknown>({
        method: 'POST',
        url: '/file/files/batch',
        body: formData,
        ...uploadRequestOptions(options),
      });
      return Array.isArray(items) ? items.map(fromBackendFileRecord) : [];
    },
    archive: (id, reason) =>
      transport.request<boolean>({ method: 'DELETE', url: '/file/files', query: { id, reason } }),
    delete: (ids) =>
      transport.request<boolean, { ids: FileId[] }>({ method: 'POST', url: '/file/files/delete', body: { ids } }),
    createUploadSession: (command, options) =>
      transport
        .request<unknown, ReturnType<typeof normalizeUploadSessionCommand>>({
          method: 'POST',
          url: '/file/files/uploads',
          body: normalizeUploadSessionCommand(command),
          ...uploadRequestOptions(options),
        })
        .then(fromBackendUploadInit),
    createUploadPartSign: (sessionId, partNumber, partSize, options) =>
      transport.request<FileUploadPartSign, { partNumber: number; partSize?: number }>({
        method: 'POST',
        url: `/file/files/uploads/${encodeURIComponent(String(sessionId))}/parts/sign`,
        body: { partNumber, partSize },
        ...uploadRequestOptions(options),
      }),
    uploadServerPart: async (sessionId, partNumber, chunk, fileName, options) => {
      const formData = new FormData();
      formData.append('partNumber', String(partNumber));
      formData.append('file', chunk, fileName);
      return transport.request<boolean>({
        method: 'POST',
        url: `/file/files/uploads/${encodeURIComponent(String(sessionId))}/parts`,
        body: formData,
        ...uploadRequestOptions(options),
      });
    },
    completeUploadPart: (sessionId, command, options) =>
      transport.request<boolean, CompleteFileUploadPartCommand>({
        method: 'PUT',
        url: `/file/files/uploads/${encodeURIComponent(String(sessionId))}/parts`,
        body: command,
        ...uploadRequestOptions(options),
      }),
    completeUploadSession: (sessionId, options) =>
      transport
        .request<unknown>({
          method: 'POST',
          url: `/file/files/uploads/${encodeURIComponent(String(sessionId))}/complete`,
          ...uploadRequestOptions(options),
        })
        .then(fromBackendFileRecord),
    abortUploadSession: (sessionId, options) =>
      transport.request<boolean>({
        method: 'DELETE',
        url: `/file/files/uploads/${encodeURIComponent(String(sessionId))}`,
        ...uploadRequestOptions(options),
      }),
    downloadUrl: (id, options) => downloadUrl(id, options),
    download: async (id, options) => {
      const response = await transport.request<Blob | BinaryDownloadResponse>({
        method: 'GET',
        url: '/file/files/download',
        query: downloadQuery(id, options),
        responseType: 'blob',
      });
      const result = toBinaryDownloadResponse(response);
      await assertBinaryBlob(result.data, '文件下载失败');
      return result;
    },
  };
  return api;
}

/**
 * Optional host configuration for new consumers. It returns an instance and
 * does not mutate the legacy `fileApi` singleton, avoiding cross-app leakage.
 */
export function configureFileApi(httpClient: HttpClient): FileApi {
  return createFileApi(httpClient);
}

/** Legacy entry point: no HttpClient is required and the original request path is retained. */
export const fileApi = createFileApi();

export function normalizeFileId(value?: FileReference) {
  if (!value) return '';
  if (typeof value === 'number') return isValidFileId(value) ? String(value) : '';
  if (typeof value !== 'string') return normalizeFileId(value.id);
  const text = value.trim();
  if (!text || isFileAccessUrl(text)) return '';
  const rawId = text.startsWith('mango-file:') ? text.slice('mango-file:'.length).trim() : text;
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
  const runtime = record as Partial<FileRecord | FilePreview> & FileRuntimeAccess;
  const candidates = [
    runtime.directPreviewUrl,
    runtime.directDownloadUrl,
    runtime.url,
    record.previewUrl,
    record.downloadUrl,
  ];
  return candidates.find((item) => Boolean(item && !isProtectedApiUrl(item))) || '';
}

export function isFileDisplayUrl(value?: string): boolean {
  if (!value) return false;
  return /^(https?:|data:|blob:)/i.test(value) || value.startsWith('/');
}

export function isFileAccessUrl(value?: string): boolean {
  if (!value) return false;
  return /^(https?:|data:|blob:)/i.test(value) || value.startsWith('/') || isProtectedApiUrl(value);
}

export function downloadQuery(id: FileId, options?: FileDownloadOptions) {
  return {
    id,
    ...(options?.compression ? { compression: options.compression } : {}),
    ...(typeof options?.perFileTargetSizeBytes === 'number'
      ? { perFileTargetSizeBytes: options.perFileTargetSizeBytes }
      : {}),
  };
}

export function downloadUrl(id: FileId, options?: FileDownloadOptions): string {
  const query = new URLSearchParams({ id: String(id) });
  if (options?.compression) {
    query.set('compression', options.compression);
  }
  if (typeof options?.perFileTargetSizeBytes === 'number') {
    query.set('perFileTargetSizeBytes', String(options.perFileTargetSizeBytes));
  }
  return `/api/file/files/download?${query.toString()}`;
}

export async function downloadFileRecord(
  apiOrRow: FileApi | (Pick<FileRecord, 'id' | 'fileName'> & Partial<Pick<FilePreview, 'directDownloadUrl'>>),
  maybeRow?: Pick<FileRecord, 'id' | 'fileName'> & Partial<Pick<FilePreview, 'directDownloadUrl'>>,
) {
  const api = maybeRow ? (apiOrRow as FileApi) : fileApi;
  const row =
    maybeRow || (apiOrRow as Pick<FileRecord, 'id' | 'fileName'> & Partial<Pick<FilePreview, 'directDownloadUrl'>>);
  if (row.directDownloadUrl && !isProtectedApiUrl(row.directDownloadUrl)) {
    openDirectDownload(row.directDownloadUrl, row.fileName || `file-${row.id}`);
    return;
  }
  const preview = row.directDownloadUrl ? undefined : await api.preview(row.id);
  if (preview?.directDownloadUrl && !isProtectedApiUrl(preview.directDownloadUrl)) {
    openDirectDownload(preview.directDownloadUrl, preview.fileName || row.fileName || `file-${row.id}`);
    return;
  }
  const response = await api.download(row.id);
  const objectUrl = URL.createObjectURL(response.data);
  openDirectDownload(objectUrl, preview?.fileName || row.fileName || `file-${row.id}`);
  setTimeout(() => URL.revokeObjectURL(objectUrl), 0);
}

async function uploadSmart(
  api: FileApi,
  settingsApi: FileSettingsApi,
  httpClient: HttpClient,
  file: File,
  params?: FileUploadParams,
  options?: FileUploadOptions,
): Promise<FileRecord> {
  const policy = await resolveUploadPolicy(settingsApi, options);
  if (!shouldUseMultipart(file.size, policy)) return uploadSimple(httpClient, file, params, options);
  return uploadMultipart(api, file, params, options);
}

async function resolveUploadPolicy(
  settingsApi: FileSettingsApi,
  options?: FileUploadOptions,
): Promise<FileUploadPolicy> {
  if (options?.multipartEnabled !== undefined && options.multipartThreshold !== undefined) {
    return {
      multipartEnabled: options.multipartEnabled,
      multipartThreshold: positiveNumber(options.multipartThreshold, DEFAULT_MULTIPART_THRESHOLD),
    };
  }
  const settings = await settingsApi.get(uploadRequestOptions(options)).catch(() => defaultFileSettings);
  return {
    multipartEnabled: options?.multipartEnabled ?? settings.multipartEnabled,
    multipartThreshold: positiveNumber(
      options?.multipartThreshold ?? settings.multipartThreshold,
      DEFAULT_MULTIPART_THRESHOLD,
    ),
  };
}

function uploadRequestOptions(options?: FileUploadOptions) {
  return {
    timeoutMs: options?.timeoutMs ?? DEFAULT_UPLOAD_TIMEOUT_MS,
    inactivityTimeoutMs: options?.inactivityTimeoutMs ?? DEFAULT_UPLOAD_INACTIVITY_TIMEOUT_MS,
    onUploadProgress: options?.onUploadProgress
      ? (progress: HttpProgress) => (options.onUploadProgress as (value: HttpProgress) => void)(progress)
      : undefined,
  };
}

async function uploadSimple(
  httpClient: HttpClient,
  file: File,
  params?: FileUploadParams,
  options?: FileUploadOptions,
): Promise<FileRecord> {
  const formData = new FormData();
  formData.append('file', file);
  appendOptional(formData, params);
  const item = await httpClient.request<unknown>({
    method: 'POST',
    url: '/file/files',
    body: formData,
    ...uploadRequestOptions(options),
  });
  return fromBackendFileRecord(item);
}

async function uploadMultipart(
  api: FileApi,
  file: File,
  params?: FileUploadParams,
  options?: FileUploadOptions,
): Promise<FileRecord> {
  const chunkSize = options?.chunkSize ?? DEFAULT_CHUNK_SIZE;
  const totalParts = Math.ceil(file.size / chunkSize);
  const fileHash = await sha256IfSupported(file);
  const controlOptions = withoutProgress(options);
  const init = await api.createUploadSession(
    {
      ...params,
      fileName: file.name,
      fileSize: file.size,
      fileHash,
      contentType: file.type || undefined,
      chunkSize,
      totalParts,
    },
    controlOptions,
  );
  if (init.instant && init.fileRecord) {
    emitUploadProgress(options, { loaded: file.size, total: file.size, progress: 1 });
    return init.fileRecord;
  }
  if (!init.sessionId || !init.uploadMode) throw new Error('文件上传会话创建失败');
  const resolvedChunkSize = Number(init.chunkSize || chunkSize);
  const resolvedTotalParts = Number(init.totalParts || totalParts);
  try {
    for (let partNumber = 1; partNumber <= resolvedTotalParts; partNumber++) {
      const start = (partNumber - 1) * resolvedChunkSize;
      const end = Math.min(start + resolvedChunkSize, file.size);
      const chunk = file.slice(start, end);
      const partOptions = withPartProgress(options, start, file.size);
      if (init.uploadMode === 'S3_MULTIPART') {
        await uploadS3MultipartPart(api, init.sessionId, partNumber, chunk, partOptions);
      } else {
        await api.uploadServerPart(init.sessionId, partNumber, chunk, file.name, partOptions);
      }
      reportMultipartProgress(options, end, file.size);
    }
    return await api.completeUploadSession(init.sessionId, controlOptions);
  } catch (error) {
    await api.abortUploadSession(init.sessionId, withoutProgress(options)).catch(() => undefined);
    throw error;
  }
}

async function uploadS3MultipartPart(
  api: FileApi,
  sessionId: FileId,
  partNumber: number,
  chunk: Blob,
  options?: FileUploadOptions,
) {
  const requestOptions = withoutProgress(options);
  const sign = await api.createUploadPartSign(sessionId, partNumber, chunk.size, requestOptions);
  const etag = await uploadSignedPart(sign, chunk, options);
  await api.completeUploadPart(sessionId, { partNumber, etag, partSize: chunk.size }, requestOptions);
}

async function uploadSignedPart(sign: FileUploadPartSign, chunk: Blob, options?: FileUploadOptions): Promise<string> {
  if (typeof XMLHttpRequest !== 'undefined') {
    return uploadSignedPartWithXhr(sign, chunk, options);
  }
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), options?.timeoutMs ?? DEFAULT_UPLOAD_TIMEOUT_MS);
  try {
    const response = await fetch(sign.uploadUrl, {
      method: sign.method || 'PUT',
      body: chunk,
      signal: controller.signal,
    });
    if (!response.ok) {
      throw new FileHttpError(`分片上传失败：${response.status}`, { kind: 'protocol', status: response.status });
    }
    const etag = response.headers.get('ETag')?.replace(/^"|"$/g, '') || '';
    if (!etag) throw new FileHttpError('对象存储未返回分片 ETag', { kind: 'protocol' });
    return etag;
  } catch (error) {
    if (controller.signal.aborted) throw new FileHttpError('分片上传超时', { kind: 'timeout' });
    throw error;
  } finally {
    clearTimeout(timer);
  }
}

function uploadSignedPartWithXhr(sign: FileUploadPartSign, chunk: Blob, options?: FileUploadOptions): Promise<string> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    const inactivityTimeoutMs = options?.inactivityTimeoutMs ?? DEFAULT_UPLOAD_INACTIVITY_TIMEOUT_MS;
    let inactivityTimer: ReturnType<typeof setTimeout> | undefined;
    let settled = false;
    const cleanup = () => {
      if (inactivityTimer) clearTimeout(inactivityTimer);
    };
    const fail = (error: Error) => {
      if (settled) return;
      settled = true;
      cleanup();
      xhr.abort();
      reject(error);
    };
    const resetInactivityTimer = () => {
      cleanup();
      inactivityTimer = setTimeout(
        () => fail(new FileHttpError('分片上传无进度超时', { kind: 'timeout' })),
        inactivityTimeoutMs,
      );
    };
    xhr.open(sign.method || 'PUT', sign.uploadUrl, true);
    xhr.timeout = options?.timeoutMs ?? DEFAULT_UPLOAD_TIMEOUT_MS;
    xhr.upload.onprogress = (event) => {
      resetInactivityTimer();
      emitUploadProgress(options, {
        loaded: event.loaded,
        total: event.total || chunk.size,
        progress: event.total ? event.loaded / event.total : undefined,
      });
    };
    xhr.onload = () => {
      if (settled) return;
      if (xhr.status < 200 || xhr.status >= 300) {
        fail(new FileHttpError(`分片上传失败：${xhr.status}`, { kind: 'protocol', status: xhr.status }));
        return;
      }
      const etag = xhr.getResponseHeader('ETag')?.replace(/^"|"$/g, '') || '';
      if (!etag) {
        fail(new FileHttpError('对象存储未返回分片 ETag', { kind: 'protocol' }));
        return;
      }
      settled = true;
      cleanup();
      resolve(etag);
    };
    xhr.onerror = () => fail(new FileHttpError('分片上传网络失败', { kind: 'network' }));
    xhr.ontimeout = () => fail(new FileHttpError('分片上传超时', { kind: 'timeout' }));
    xhr.onabort = () => {
      if (!settled) fail(new FileHttpError('分片上传已取消', { kind: 'aborted' }));
    };
    resetInactivityTimer();
    xhr.send(chunk);
  });
}

function withoutProgress(options?: FileUploadOptions): FileUploadOptions | undefined {
  return options ? { ...options, onUploadProgress: undefined } : undefined;
}

function withPartProgress(options: FileUploadOptions | undefined, offset: number, total: number) {
  if (!options?.onUploadProgress) return options;
  return {
    ...options,
    onUploadProgress: (progress: HttpProgress) => {
      const loaded = Math.min(offset + progress.loaded, total);
      emitUploadProgress(options, {
        loaded,
        total,
        progress: total ? loaded / total : 0,
        bytesPerSecond: progress.bytesPerSecond,
      });
    },
  };
}

function reportMultipartProgress(options: FileUploadOptions | undefined, loaded: number, total: number) {
  emitUploadProgress(options, { loaded, total, progress: total ? loaded / total : 0 });
}

function emitUploadProgress(options: FileUploadOptions | undefined, progress: HttpProgress) {
  const callback = options?.onUploadProgress as ((value: HttpProgress) => void) | undefined;
  callback?.({
    ...progress,
    bytes: progress.bytes ?? progress.loaded,
    lengthComputable: progress.lengthComputable ?? progress.total !== undefined,
  });
}

function positiveNumber(value: unknown, fallback: number): number {
  const number = Number(value);
  return Number.isFinite(number) && number > 0 ? number : fallback;
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

function appendOptional(formData: FormData, params?: Record<string, unknown>) {
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') formData.append(key, String(value));
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

function fromBackendUploadInit(item: unknown): FileUploadInit {
  const record = asRecord(item);
  return {
    instant: Boolean(record.instant),
    fileRecord: record.fileRecord ? fromBackendFileRecord(record.fileRecord) : undefined,
    sessionId: normalizeId(record.sessionId),
    uploadMode:
      record.uploadMode === 'S3_MULTIPART' || record.uploadMode === 'SERVER_CHUNK' ? record.uploadMode : undefined,
    storageUploadId: typeof record.storageUploadId === 'string' ? record.storageUploadId : undefined,
    chunkSize: Number(record.chunkSize ?? 0),
    totalParts: Number(record.totalParts ?? 0),
    expiresAt: normalizeDateTime(record.expiresAt),
  };
}

function fromBackendFileRecord(item: unknown): FileRecord {
  const record = asRecord(item);
  return {
    id: normalizeId(record.id),
    tenantId: normalizeId(record.tenantId),
    bizType: stringValue(record.bizType),
    bizId: stringValue(record.bizId),
    purpose: stringValue(record.purpose),
    bizMeta: parseBizMeta(record.bizMeta),
    directoryId: normalizeId(record.directoryId ?? 0),
    directoryName: stringValue(record.directoryName),
    accessLevel: stringValue(record.accessLevel),
    fileName: stringValue(record.fileName) || '',
    fileExt: stringValue(record.fileExt),
    fileSize: Number(record.fileSize ?? 0),
    contentType: stringValue(record.contentType),
    fileHash: stringValue(record.fileHash),
    status: numberValue(record.status),
    archived: numberValue(record.archived),
    createdBy: normalizeId(record.createdBy),
    createdTime: normalizeDateTime(record.createdTime),
    updatedTime: normalizeDateTime(record.updatedTime),
    previewUrl: normalizeApiUrl(stringValue(record.previewUrl)),
    downloadUrl: normalizeApiUrl(stringValue(record.downloadUrl)),
  };
}

function normalizeId(value: unknown): FileId {
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

function parseBizMeta(value: unknown): FileBizMeta | undefined {
  if (!value) return undefined;
  if (typeof value !== 'string') return value as Record<string, unknown>;
  try {
    return JSON.parse(value) as FileBizMeta;
  } catch {
    return value;
  }
}

function toBinaryDownloadResponse(response: Blob | BinaryDownloadResponse): BinaryDownloadResponse {
  if (response instanceof Blob) return { data: response };
  return response;
}

async function assertBinaryBlob(blob: Blob, fallbackMessage: string) {
  if (!blob.type.toLowerCase().includes('application/json')) return;
  try {
    const body = JSON.parse(await blob.text()) as { msg?: string; message?: string };
    throw new FileHttpError(body.msg || body.message || fallbackMessage, { kind: 'protocol', details: body });
  } catch (error) {
    if (error instanceof SyntaxError) {
      throw new FileHttpError(fallbackMessage, { kind: 'protocol', details: error });
    }
    throw error;
  }
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

function toBackendPageParams(params?: FileQuery) {
  if (!params) return undefined;
  const { pageNum, pageSize, ...rest } = params;
  return { ...rest, page: pageNum, size: pageSize };
}

function fromBackendPageResult(
  data: unknown,
  mapper: (item: unknown) => FileRecord,
  params?: { pageNum?: number; pageSize?: number },
): PageResult<FileRecord> {
  const record = asRecord(data);
  const list = Array.isArray(record.list) ? record.list.map(mapper) : [];
  return {
    list,
    total: Number(record.total ?? list.length),
    pageNum: Number(record.page ?? params?.pageNum ?? 1),
    pageSize: Number(record.size ?? params?.pageSize ?? (list.length || 10)),
  };
}

function normalizeApiUrl(value?: string): string {
  if (!value) return '';
  if (/^https?:\/\//i.test(value) || value.startsWith('/api/')) return value;
  return value.startsWith('/') ? `/api${value}` : value;
}

function asRecord(value: unknown): Record<string, unknown> {
  return value && typeof value === 'object' ? (value as Record<string, unknown>) : {};
}

function stringValue(value: unknown): string | undefined {
  return value === undefined || value === null ? undefined : String(value);
}

function numberValue(value: unknown): number | undefined {
  return value === undefined || value === null ? undefined : Number(value);
}
