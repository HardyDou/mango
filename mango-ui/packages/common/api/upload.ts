import request, { get, post } from '../utils/request';
import type { ApiId } from '@mango/api-schema';

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

export function uploadFile(file: File): Promise<UploadResult> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('purpose', 'attachment');
  formData.append('accessLevel', 'PRIVATE');
  return post<any>('/file/files', formData as any, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
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
  }).then((record) => ({
    id: record.id,
    url: uploadAccessUrl(record),
    previewUrl: record.previewUrl || record.url,
    downloadUrl: record.downloadUrl,
    fileName: record.fileName,
    fileSize: Number(record.fileSize ?? 0),
    contentType: record.contentType,
    objectName: record.objectName,
    directPreviewUrl: record.directPreviewUrl,
    directDownloadUrl: record.directDownloadUrl,
    data: [],
  }));
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
  }).then(records => records.map(toUploadResult));
}

export function getUploadedFileDetail(id: FileId): Promise<UploadResult> {
  return get<any>('/file/files/detail', { params: { id } }).then(toUploadResult);
}

export async function downloadUploadedFile(id: FileId) {
  const response = await request.get('/file/files/download', {
    params: { id },
    responseType: 'blob',
    rawResponse: true,
  } as any);
  await assertBinaryDownloadResponse(response as any);
  return response as any;
}

export async function createUploadedFileObjectUrl(id: FileId): Promise<string> {
  return `/api/file/files/download?id=${encodeURIComponent(String(id))}`;
}

function toUploadResult(record: any): UploadResult {
  const id = normalizeId(record.id);
  return {
    id,
    url: uploadAccessUrl(record),
    previewUrl: record.previewUrl || record.url,
    downloadUrl: record.downloadUrl,
    fileName: record.fileName,
    fileSize: Number(record.fileSize ?? 0),
    contentType: record.contentType,
    objectName: record.objectName,
    directPreviewUrl: record.directPreviewUrl,
    directDownloadUrl: record.directDownloadUrl,
  };
}

export function fileToken(id?: FileId): string {
  return id ? `mango-file:${id}` : '';
}

function uploadAccessUrl(record: any): string {
  const id = normalizeId(record?.id);
  return directUrl(record?.directPreviewUrl)
    || directUrl(record?.url)
    || directUrl(record?.directDownloadUrl)
    || directUrl(record?.downloadUrl)
    || fileToken(id);
}

function directUrl(value?: string): string {
  if (!value) return '';
  if (/^(https?:)?\/\//i.test(value) || /^(blob|data):/i.test(value)) return value;
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
      throw new Error('文件下载失败');
    }
    throw error;
  }
}
