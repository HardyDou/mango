import { post } from '../utils/request';

export interface UploadResult {
  url: string;
  fileName: string;
  fileSize: number;
}

export interface ExcelUploadResult {
  fileName: string;
  fileSize: number;
  data: Record<string, unknown>[];
}

export function uploadFile(file: File): Promise<UploadResult> {
  const formData = new FormData();
  formData.append('file', file);
  return post<UploadResult>('/admin/upload/file', formData as any, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
}

export function uploadImage(file: File): Promise<UploadResult> {
  const formData = new FormData();
  formData.append('file', file);
  return post<UploadResult>('/admin/upload/image', formData as any, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
}

export function uploadExcel(file: File): Promise<ExcelUploadResult> {
  const formData = new FormData();
  formData.append('file', file);
  return post<ExcelUploadResult>('/admin/upload/excel', formData as any, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
}

export function uploadMultiple(files: File[]): Promise<UploadResult[]> {
  const formData = new FormData();
  files.forEach((file) => {
    formData.append('files', file);
  });
  return post<UploadResult[]>('/admin/upload/multiple', formData as any, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
}
