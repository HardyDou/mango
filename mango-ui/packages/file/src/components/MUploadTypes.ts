import type { FileRecord } from '../api/file';

export type UploadDisplay = 'list' | 'thumbnail' | 'table' | 'drag';
export type UploadValueType = 'id' | 'token' | 'record';
export type UploadColumnKey = keyof FileRecord | `meta.${string}` | string;

export interface UploadSizeRules {
  image?: string | number;
  video?: string | number;
  audio?: string | number;
  document?: string | number;
  archive?: string | number;
  other?: string | number;
  [key: string]: string | number | undefined;
}

export interface UploadColumn {
  key: UploadColumnKey;
  label: string;
  width?: number | string;
  minWidth?: number | string;
}
