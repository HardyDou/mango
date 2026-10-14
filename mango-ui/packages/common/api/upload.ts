import request, { get, post } from '../utils/request';
import type { ApiId, HttpClient, HttpProgress } from '@mango/api-schema';

/**
 * FormData 请求必须显式声明 multipart，避免宿主默认的 application/json 头让 axios 把 FormData 序列化成 JSON。
 */
const MULTIPART_HEADERS = { 'Content-Type': 'multipart/form-data' } as const;

export type FileId = ApiId;

export interface UploadResult {
  id?: FileId;
  url: string;
  previewUrl?: string;
  downloadUrl?: string;
  fileName: string;
  fileSize: number;
  contentType?: string;
  objectName?: string;
  directPreviewUrl?: string;
  directDownloadUrl?: string;
}

export interface BinaryDownloadResponse {
  data: Blob;
  headers?: Record<string, string | undefined>;
}

interface UploadApiRecord {
  id?: unknown;
  url?: string;
  previewUrl?: string;
  downloadUrl?: string;
  fileName: string;
  fileSize?: unknown;
  contentType?: string;
  objectName?: string;
  directPreviewUrl?: string;
  directDownloadUrl?: string;
}

export interface ExcelUploadResult {
  id?: FileId;
  url: string;
  previewUrl?: string;
  downloadUrl?: string;
  fileName: string;
  fileSize: number;
  contentType?: string;
  objectName?: string;
  directPreviewUrl?: string;
  directDownloadUrl?: string;
  data: Record<string, unknown>[];
}

export interface UploadRequestOptions {
  timeoutMs?: number;
  inactivityTimeoutMs?: number;
  onUploadProgress?: (progress: HttpProgress) => void;
}

export interface UploadApi {
  uploadFile(file: File, options?: UploadRequestOptions): Promise<UploadResult>;
  uploadImage(file: File, options?: UploadRequestOptions): Promise<UploadResult>;
  uploadExcel(file: File, options?: UploadRequestOptions): Promise<ExcelUploadResult>;
  uploadMultiple(files: File[], options?: UploadRequestOptions): Promise<UploadResult[]>;
  getUploadedFileDetail(id: FileId, options?: UploadRequestOptions): Promise<UploadResult>;
  importRemoteImage(input: ImportRemoteImageInput, options?: UploadRequestOptions): Promise<UploadResult>;
  downloadUploadedFile(id: FileId, options?: UploadRequestOptions): Promise<BinaryDownloadResponse>;
  createUploadedFileObjectUrl(id: FileId): Promise<string>;
}

export interface ImportRemoteImageInput {
  sourceUrl: string;
  bizType?: string;
  bizId?: string;
  bizMeta?: string;
  directoryId?: FileId;
}

export function uploadFile(file: File): Promise<UploadResult> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('purpose', 'attachment');
  formData.append('accessLevel', 'PRIVATE');
  return post<any>('/file/files', formData as any, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    timeout: DEFAULT_UPLOAD_TIMEOUT_MS,
  }).then(toUploadResult);
}

export function uploadImage(file: File): Promise<UploadResult> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('purpose', 'image');
  formData.append('accessLevel', 'PRIVATE');
  return post<any>('/file/files', formData as any, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    timeout: DEFAULT_UPLOAD_TIMEOUT_MS,
  }).then(toUploadResult);
}

export function uploadExcel(file: File): Promise<ExcelUploadResult> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('purpose', 'excel');
  formData.append('accessLevel', 'PRIVATE');
  return post<any>('/file/files', formData as any, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    timeout: DEFAULT_UPLOAD_TIMEOUT_MS,
  }).then((record) => ({ ...toUploadResult(record), data: [] }));
}

export function uploadMultiple(files: File[]): Promise<UploadResult[]> {
  const formData = new FormData();
  files.forEach((file) => {
    formData.append('files', file);
  });
  formData.append('purpose', 'attachment');
  formData.append('accessLevel', 'PRIVATE');
  return post<any[]>('/file/files/batch', formData as any, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    timeout: DEFAULT_UPLOAD_TIMEOUT_MS,
  }).then((records) => records.map(toUploadResult));
}

export function getUploadedFileDetail(id: FileId): Promise<UploadResult> {
  return get<UploadApiRecord>('/file/files/detail', { params: { id } }).then(toUploadedFileDetail);
}

export function importRemoteImage(input: ImportRemoteImageInput): Promise<UploadResult> {
  return post<UploadApiRecord>('/file/files/import-image', input, { timeout: DEFAULT_UPLOAD_TIMEOUT_MS }).then(
    toUploadResult,
  );
}

export async function downloadUploadedFile(id: FileId): Promise<BinaryDownloadResponse> {
  const response = await request.get('/file/files/download', {
    params: { id },
    responseType: 'blob',
    rawResponse: true,
    timeout: DEFAULT_UPLOAD_TIMEOUT_MS,
  } as any);
  await assertBinaryDownloadResponse(response as any);
  return response as BinaryDownloadResponse;
}

export async function createUploadedFileObjectUrl(id: FileId): Promise<string> {
  return `/api/file/files/download?id=${encodeURIComponent(String(id))}`;
}

function toUploadResult(record: UploadApiRecord): UploadResult {
  const id = normalizeId(record.id);
  const previewUrl = firstFileUrl(record.previewUrl, record.directPreviewUrl, record.url);
  const downloadUrl = firstFileUrl(record.downloadUrl, record.directDownloadUrl);
  return {
    id,
    url: previewUrl || downloadUrl || fileToken(id),
    previewUrl: previewUrl || undefined,
    downloadUrl: downloadUrl || undefined,
    fileName: record.fileName,
    fileSize: Number(record.fileSize ?? 0),
    contentType: record.contentType,
    objectName: record.objectName,
    directPreviewUrl: normalizeFileAccessUrl(record.directPreviewUrl) || undefined,
    directDownloadUrl: normalizeFileAccessUrl(record.directDownloadUrl) || undefined,
  };
}

function toUploadedFileDetail(record: UploadApiRecord): UploadResult {
  return toUploadResult(record);
}

const legacyUploadApi: UploadApi = {
  uploadFile,
  uploadImage,
  uploadExcel,
  uploadMultiple,
  getUploadedFileDetail,
  importRemoteImage,
  downloadUploadedFile,
  createUploadedFileObjectUrl,
};

export function createUploadApi(httpClient?: HttpClient): UploadApi {
  if (!httpClient) return legacyUploadApi;
  const upload = (file: File, purpose: string, options?: UploadRequestOptions) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('purpose', purpose);
    formData.append('accessLevel', 'PRIVATE');
    return httpClient
      .request<UploadApiRecord>({
        method: 'POST',
        url: '/file/files',
        body: formData,
        headers: MULTIPART_HEADERS,
        ...uploadRequestOptions(options),
      })
      .then(toUploadResult);
  };
  return {
    uploadFile: (file, options) => upload(file, 'attachment', options),
    uploadImage: (file, options) => upload(file, 'image', options),
    uploadExcel: (file, options) => upload(file, 'excel', options).then((record) => ({ ...record, data: [] })),
    uploadMultiple: (files, options) => {
      const formData = new FormData();
      files.forEach((file) => formData.append('files', file));
      formData.append('purpose', 'attachment');
      formData.append('accessLevel', 'PRIVATE');
      return httpClient
        .request<UploadApiRecord[]>({
          method: 'POST',
          url: '/file/files/batch',
          body: formData,
          headers: MULTIPART_HEADERS,
          ...uploadRequestOptions(options),
        })
        .then((records) => records.map(toUploadResult));
    },
    getUploadedFileDetail: (id) =>
      httpClient
        .request<UploadApiRecord>({ method: 'GET', url: '/file/files/detail', query: { id } })
        .then(toUploadedFileDetail),
    importRemoteImage: (input) =>
      httpClient
        .request<UploadApiRecord, ImportRemoteImageInput>({
          method: 'POST',
          url: '/file/files/import-image',
          body: input,
        })
        .then(toUploadResult),
    downloadUploadedFile: async (id, options) => {
      const data = await httpClient.request<Blob>({
        method: 'GET',
        url: '/file/files/download',
        query: { id },
        responseType: 'blob',
        ...uploadRequestOptions(options),
      });
      await assertBinaryBlob(data);
      return { data };
    },
    createUploadedFileObjectUrl: async (id) => `/api/file/files/download?id=${encodeURIComponent(String(id))}`,
  };
}

const DEFAULT_UPLOAD_TIMEOUT_MS = 10 * 60 * 1000;
const DEFAULT_UPLOAD_INACTIVITY_TIMEOUT_MS = 60 * 1000;

function uploadRequestOptions(options?: UploadRequestOptions) {
  return {
    timeoutMs: options?.timeoutMs ?? DEFAULT_UPLOAD_TIMEOUT_MS,
    inactivityTimeoutMs: options?.inactivityTimeoutMs ?? DEFAULT_UPLOAD_INACTIVITY_TIMEOUT_MS,
    onUploadProgress: options?.onUploadProgress,
  };
}

async function assertBinaryBlob(blob: Blob) {
  if (!blob.type.toLowerCase().includes('application/json')) return;
  try {
    const body = JSON.parse(await blob.text()) as { msg?: string; message?: string };
    throw new Error(body.msg || body.message || '文件下载失败');
  } catch (error) {
    if (error instanceof SyntaxError) throw new Error('文件下载失败', { cause: error });
    throw error;
  }
}

export function fileToken(id?: FileId): string {
  return id ? `mango-file:${id}` : '';
}

function firstFileUrl(...values: Array<string | undefined>) {
  for (const value of values) {
    const normalized = normalizeFileAccessUrl(value);
    if (normalized) return normalized;
  }
  return '';
}

export function normalizeFileAccessUrl(value?: string): string {
  const normalized = String(value || '').trim();
  if (!normalized) return '';
  if (/^(https?:)?\/\//i.test(normalized) || /^(blob|data):/i.test(normalized)) return normalized;
  if (normalized.startsWith('/api/')) return normalized;
  if (normalized.startsWith('/file/')) return `/api${normalized}`;
  if (normalized.startsWith('file/')) return `/api/${normalized}`;
  return '';
}

function normalizeId(value: any): FileId | undefined {
  return value === undefined || value === null || value === '' ? undefined : String(value);
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
      throw new Error('文件下载失败', { cause: error });
    }
    throw error;
  }
}
