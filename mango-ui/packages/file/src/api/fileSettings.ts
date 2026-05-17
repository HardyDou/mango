/**
 * File Settings API - 文件中心运行时配置。
 */

import { get, put } from '@mango/common';
import type { ApiId } from '@mango/api-schema';

export interface FileSettings {
  id?: ApiId;
  tenantId?: ApiId;
  maxSize: number;
  allowedExtensions: string[];
  blockedExtensions: string[];
  defaultAccessLevel: 'PRIVATE' | 'PUBLIC_READ' | 'INTERNAL';
  duplicateNameStrategy: 'REJECT' | 'AUTO_RENAME' | 'ALLOW';
  duplicateCheckDirectoryScoped: boolean;
  objectNameStrategy: 'DATE_UUID' | 'HASH' | 'ORIGINAL';
  instantUploadEnabled: boolean;
  instantUploadScope: 'TENANT' | 'GLOBAL';
  contentTypeCheckEnabled: boolean;
  allowedContentTypes: string[];
  blockedContentTypes: string[];
  directUploadEnabled: boolean;
  directUploadExpireSeconds: number;
  accessTokenEnabled: boolean;
  publicReadRequiresToken: boolean;
  accessMode: 'PROXY' | 'DIRECT';
  accessTokenExpireSeconds: number;
  previewProviderUrl?: string;
  previewExpireSeconds: number;
  previewExternalExtensions: string[];
  archiveRetainEnabled: boolean;
  archiveRetainDays: number;
  archiveRestoreEnabled: boolean;
  physicalDeleteEnabled: boolean;
  defaultConfig?: boolean;
  updatedTime?: string;
}

export const fileSettingsApi = {
  get: () => get<FileSettings>('/file/settings').then(fromBackendSettings),
  save: (data: FileSettings) => put<boolean>('/file/settings', toBackendSettings(data)),
};

export const defaultFileSettings: FileSettings = {
  maxSize: 100 * 1024 * 1024,
  allowedExtensions: [],
  blockedExtensions: ['exe', 'bat', 'cmd', 'sh', 'jar'],
  defaultAccessLevel: 'PRIVATE',
  duplicateNameStrategy: 'REJECT',
  duplicateCheckDirectoryScoped: true,
  objectNameStrategy: 'DATE_UUID',
  instantUploadEnabled: true,
  instantUploadScope: 'TENANT',
  contentTypeCheckEnabled: true,
  allowedContentTypes: [],
  blockedContentTypes: ['application/x-msdownload', 'application/x-sh'],
  directUploadEnabled: false,
  directUploadExpireSeconds: 900,
  accessTokenEnabled: false,
  publicReadRequiresToken: false,
  accessMode: 'PROXY',
  accessTokenExpireSeconds: 600,
  previewProviderUrl: '',
  previewExpireSeconds: 600,
  previewExternalExtensions: [
    'doc', 'docx', 'xls', 'xlsx', 'xlsm', 'ppt', 'pptx',
    'odt', 'ods', 'odp', 'ofd', 'wps', 'et', 'dps',
    'csv', 'txt', 'zip', 'rar', '7z', 'eml', 'msg',
  ],
  archiveRetainEnabled: true,
  archiveRetainDays: 180,
  archiveRestoreEnabled: false,
  physicalDeleteEnabled: false,
};

export function parseExtensions(value: string): string[] {
  return value
    .split(/[,，\s\n]+/)
    .map(item => item.trim().replace(/^\./, '').toLowerCase())
    .filter(Boolean)
    .filter((item, index, array) => array.indexOf(item) === index);
}

export function stringifyExtensions(values?: string[]): string {
  return (values || []).join(', ');
}

export function parseTextList(value: string): string[] {
  return value
    .split(/[,，\s\n]+/)
    .map(item => item.trim())
    .filter(Boolean)
    .filter((item, index, array) => array.indexOf(item) === index);
}

export function stringifyTextList(values?: string[]): string {
  return (values || []).join(', ');
}

export function formatBytes(value?: number): string {
  const size = Number(value || 0);
  if (size >= 1024 * 1024 * 1024) return `${(size / 1024 / 1024 / 1024).toFixed(2)} GB`;
  if (size >= 1024 * 1024) return `${(size / 1024 / 1024).toFixed(2)} MB`;
  if (size >= 1024) return `${(size / 1024).toFixed(2)} KB`;
  return `${size} B`;
}

function fromBackendSettings(item: any): FileSettings {
  return {
    ...defaultFileSettings,
    ...item,
    id: normalizeId(item?.id),
    tenantId: normalizeId(item?.tenantId),
    maxSize: Number(item?.maxSize ?? defaultFileSettings.maxSize),
    allowedExtensions: normalizeArray(item?.allowedExtensions),
    blockedExtensions: normalizeArray(item?.blockedExtensions),
    defaultAccessLevel: item?.defaultAccessLevel || defaultFileSettings.defaultAccessLevel,
    duplicateNameStrategy: item?.duplicateNameStrategy || defaultFileSettings.duplicateNameStrategy,
    duplicateCheckDirectoryScoped: item?.duplicateCheckDirectoryScoped !== false,
    objectNameStrategy: item?.objectNameStrategy || defaultFileSettings.objectNameStrategy,
    instantUploadEnabled: item?.instantUploadEnabled !== false,
    instantUploadScope: item?.instantUploadScope || defaultFileSettings.instantUploadScope,
    contentTypeCheckEnabled: item?.contentTypeCheckEnabled !== false,
    allowedContentTypes: normalizeTextArray(item?.allowedContentTypes),
    blockedContentTypes: normalizeTextArray(item?.blockedContentTypes),
    directUploadEnabled: Boolean(item?.directUploadEnabled),
    directUploadExpireSeconds: Number(item?.directUploadExpireSeconds ?? defaultFileSettings.directUploadExpireSeconds),
    accessTokenEnabled: Boolean(item?.accessTokenEnabled),
    publicReadRequiresToken: Boolean(item?.publicReadRequiresToken),
    accessMode: item?.accessMode === 'DIRECT' ? 'DIRECT' : 'PROXY',
    accessTokenExpireSeconds: Number(item?.accessTokenExpireSeconds ?? defaultFileSettings.accessTokenExpireSeconds),
    previewProviderUrl: item?.previewProviderUrl || '',
    previewExpireSeconds: Number(item?.previewExpireSeconds ?? defaultFileSettings.previewExpireSeconds),
    previewExternalExtensions: normalizeArray(item?.previewExternalExtensions),
    archiveRetainEnabled: item?.archiveRetainEnabled !== false,
    archiveRetainDays: Number(item?.archiveRetainDays ?? defaultFileSettings.archiveRetainDays),
    archiveRestoreEnabled: Boolean(item?.archiveRestoreEnabled),
    physicalDeleteEnabled: Boolean(item?.physicalDeleteEnabled),
    defaultConfig: Boolean(item?.defaultConfig),
    updatedTime: normalizeDateTime(item?.updatedTime),
  };
}

function toBackendSettings(data: FileSettings) {
  return {
    maxSize: Number(data.maxSize),
    allowedExtensions: data.allowedExtensions || [],
    blockedExtensions: data.blockedExtensions || [],
    defaultAccessLevel: data.defaultAccessLevel,
    duplicateNameStrategy: data.duplicateNameStrategy,
    duplicateCheckDirectoryScoped: Boolean(data.duplicateCheckDirectoryScoped),
    objectNameStrategy: data.objectNameStrategy,
    instantUploadEnabled: Boolean(data.instantUploadEnabled),
    instantUploadScope: data.instantUploadScope,
    contentTypeCheckEnabled: Boolean(data.contentTypeCheckEnabled),
    allowedContentTypes: data.allowedContentTypes || [],
    blockedContentTypes: data.blockedContentTypes || [],
    directUploadEnabled: Boolean(data.directUploadEnabled),
    directUploadExpireSeconds: Number(data.directUploadExpireSeconds),
    accessTokenEnabled: Boolean(data.accessTokenEnabled),
    publicReadRequiresToken: Boolean(data.publicReadRequiresToken),
    accessMode: data.accessMode || 'PROXY',
    accessTokenExpireSeconds: Number(data.accessTokenExpireSeconds),
    previewProviderUrl: data.previewProviderUrl || undefined,
    previewExpireSeconds: Number(data.previewExpireSeconds),
    previewExternalExtensions: data.previewExternalExtensions || [],
    archiveRetainEnabled: Boolean(data.archiveRetainEnabled),
    archiveRetainDays: Number(data.archiveRetainDays),
    archiveRestoreEnabled: false,
    physicalDeleteEnabled: Boolean(data.physicalDeleteEnabled),
  };
}

function normalizeId(value: any): ApiId | undefined {
  return value === undefined || value === null ? undefined : String(value);
}

function normalizeArray(value: any): string[] {
  if (Array.isArray(value)) return value.map(String).filter(Boolean);
  if (typeof value === 'string') return parseExtensions(value);
  return [];
}

function normalizeTextArray(value: any): string[] {
  if (Array.isArray(value)) return value.map(String).filter(Boolean);
  if (typeof value === 'string') return parseTextList(value);
  return [];
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
