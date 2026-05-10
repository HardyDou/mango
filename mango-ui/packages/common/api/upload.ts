import request, { post } from '../utils/request';

export interface UploadResult {
  id?: number;
  url: string;
  fileName: string;
  fileSize: number;
  contentType?: string;
  objectName?: string;
}

export interface ExcelUploadResult {
  id?: number;
  url: string;
  fileName: string;
  fileSize: number;
  contentType?: string;
  objectName?: string;
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
    url: record.id ? `mango-file:${record.id}` : '',
    fileName: record.fileName,
    fileSize: Number(record.fileSize ?? 0),
    contentType: record.contentType,
    objectName: record.objectName,
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

export async function downloadUploadedFile(id: number) {
  const response = await request.get('/file/files/download', {
    params: { id },
    responseType: 'blob',
    rawResponse: true,
  } as any);
  return response as any;
}

export async function createUploadedFileObjectUrl(id: number): Promise<string> {
  const response = await downloadUploadedFile(id);
  const blob = new Blob([response.data], {
    type: response.headers?.['content-type'] || 'application/octet-stream',
  });
  return URL.createObjectURL(blob);
}

function toUploadResult(record: any): UploadResult {
  return {
    id: record.id,
    url: record.id ? `mango-file:${record.id}` : '',
    fileName: record.fileName,
    fileSize: Number(record.fileSize ?? 0),
    contentType: record.contentType,
    objectName: record.objectName,
  };
}
