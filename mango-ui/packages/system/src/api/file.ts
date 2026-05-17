/**
 * File API - 文件管理。
 */

import { del, get, post, request } from '@mango/common/utils/request';

export interface FileRecord {
  id: number;
  tenantId?: number;
  bizType?: string;
  bizId?: string;
  purpose?: string;
  accessLevel?: string;
  storageType?: string;
  bucketName?: string;
  objectName?: string;
  fileName: string;
  fileExt?: string;
  fileSize: number;
  contentType?: string;
  fileHash?: string;
  status?: number;
  archived?: number;
  createdBy?: number;
  createdTime?: string;
  updatedTime?: string;
}

export interface FilePreview {
  id: number;
  fileName: string;
  fileExt?: string;
  fileSize: number;
  contentType?: string;
  previewable: boolean;
  previewUrl: string;
  downloadUrl: string;
}

export interface FileQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  bizType?: string;
  bizId?: string;
  purpose?: string;
  accessLevel?: string;
  status?: number;
  includeArchived?: boolean;
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
  detail: (id: number) => get<FileRecord>('/file/files/detail', { params: { id } }).then(fromBackendFileRecord),
  preview: (id: number) => get<FilePreview>('/file/files/preview', { params: { id } }).then((item: any) => ({
    ...item,
    previewable: Boolean(item.previewable),
  })),
  upload: (file: File, params?: Partial<Pick<FileRecord, 'purpose' | 'accessLevel' | 'bizType' | 'bizId'>>) => {
    const formData = new FormData();
    formData.append('file', file);
    appendOptional(formData, params);
    return post<FileRecord>('/file/files', formData as any, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(fromBackendFileRecord);
  },
  archive: (id: number, reason?: string) => del<boolean>('/file/files', { params: { id, reason } }),
  downloadUrl: (id: number) => `/api/file/files/download?id=${id}`,
  download: async (id: number) => {
    const response = await request.get('/file/files/download', {
      params: { id },
      responseType: 'blob',
      rawResponse: true,
    } as any);
    return response as any;
  },
};

export async function downloadFileRecord(row: Pick<FileRecord, 'id' | 'fileName'>) {
  const response = await fileApi.download(row.id);
  const blob = new Blob([response.data], {
    type: response.headers?.['content-type'] || 'application/octet-stream',
  });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = parseDownloadFileName(response.headers?.['content-disposition']) || row.fileName || `file-${row.id}`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
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
    fileSize: Number(item.fileSize ?? 0),
    createdTime: normalizeDateTime(item.createdTime),
    updatedTime: normalizeDateTime(item.updatedTime),
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

function parseDownloadFileName(disposition?: string): string {
  if (!disposition) return '';
  const encoded = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
  if (encoded) {
    return decodeURIComponent(encoded);
  }
  const quoted = disposition.match(/filename="([^"]+)"/i)?.[1];
  return quoted ? decodeURIComponent(quoted) : '';
}
