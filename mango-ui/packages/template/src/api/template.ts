import { del, get, post, put } from '@mango/common';

export type TemplateSourceFormat = 'TEXT' | 'HTML' | 'DOCX' | 'XLSX';
export type TemplateOutputFormat = 'TEXT' | 'HTML' | 'DOCX' | 'XLSX' | 'PDF' | 'OFD';
export type TemplateRenderStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED';
export type JsonValue = string | number | boolean | null | JsonValue[] | { [key: string]: JsonValue };
export type JsonObject = { [key: string]: JsonValue };

export interface TemplateItem {
  id: number;
  tenantId?: number;
  templateCode: string;
  templateName: string;
  categoryCode?: string;
  categoryName?: string;
  /** @deprecated 前端不再使用，业务侧统一按 templateCode 渲染。 */
  businessKey?: string;
  sourceFormat?: TemplateSourceFormat;
  status: number;
  currentVersionNo: number;
  publishedVersionNo?: number;
  hasUnpublishedChanges?: boolean;
  unpublishedChangeReasons?: string[];
  draftSourceFormat?: TemplateSourceFormat;
  remark?: string;
  createdTime?: string;
  updatedTime?: string;
}

export interface TemplateCategory {
  id: number;
  tenantId?: number;
  categoryCode: string;
  categoryName: string;
  sort?: number;
  status: number;
  remark?: string;
  createdTime?: string;
  updatedTime?: string;
}

export interface TemplateVariableDefinition {
  id?: string;
  name: string;
  label?: string;
  type?: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'OBJECT' | 'ARRAY' | 'DATE';
  required?: boolean;
  example?: string;
  description?: string;
  children?: TemplateVariableDefinition[];
}

export interface TemplateVersion {
  id: number;
  templateId: number;
  versionNo: number;
  sourceFormat?: TemplateSourceFormat;
  content?: string;
  sourceFileId?: number;
  variableSchema?: string;
  variables?: TemplateVariableDefinition[];
  currentPublished?: number;
  versionRemark?: string;
  createdTime?: string;
}

export interface TemplateDetail extends TemplateItem {
  versions: TemplateVersion[];
  draftContent?: string;
  draftSourceFileId?: number;
  draftVariables?: TemplateVariableDefinition[];
}

export interface TemplateQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  categoryCode?: string;
  sourceFormat?: string;
  status?: number;
}

export interface TemplateCategoryQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  status?: number;
}

export interface SaveTemplateCategoryPayload {
  id?: number;
  categoryCode: string;
  categoryName: string;
  sort?: number;
  status?: number;
  remark?: string;
}

export interface SaveTemplatePayload {
  id?: number;
  templateCode: string;
  templateName: string;
  categoryCode?: string;
  categoryName?: string;
  sourceFormat?: TemplateSourceFormat;
  draftContent?: string;
  draftSourceFileId?: number;
  draftVariables?: TemplateVariableDefinition[];
  remark?: string;
}

export interface PublishTemplateVersionPayload {
  templateId: number;
  sourceFormat: TemplateSourceFormat;
  content?: string;
  sourceFileId?: number;
  versionRemark?: string;
  variables?: TemplateVariableDefinition[];
}

export interface ActivateTemplateVersionPayload {
  templateId: number;
  versionNo: number;
}

export interface TemplateRenderPayload {
  templateCode?: string;
  /** @deprecated 前端不再使用，保留给历史调用兼容。 */
  businessKey?: string;
  versionNo?: number;
  outputFormat: TemplateOutputFormat;
  variables: JsonObject;
  async?: boolean;
  bizType?: string;
  bizId?: string;
}

export interface TemplateRenderResult {
  recordId: number;
  status: TemplateRenderStatus;
  content?: string;
  fileId?: number;
  fileName?: string;
  contentType?: string;
  errorMessage?: string;
}

export interface TemplateRenderRecord {
  id: number;
  templateId: number;
  templateCode: string;
  versionNo: number;
  outputFormat: TemplateOutputFormat;
  status: TemplateRenderStatus;
  outputFileId?: number;
  outputContent?: string;
  errorMessage?: string;
  bizType?: string;
  bizId?: string;
  createdTime?: string;
  updatedTime?: string;
}

export interface TemplateRenderRecordQuery {
  pageNum?: number;
  pageSize?: number;
  templateCode?: string;
  status?: string;
  bizType?: string;
  bizId?: string;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export const templateApi = {
  page: (params?: TemplateQuery) => get<BackendPageResult<TemplateItem>>('/template/templates/page', { params: toBackendPageParams(params) })
    .then((data) => fromBackendPageResult(data, normalizeTemplate, params)),
  detail: (id: number) => get<TemplateDetail>('/template/templates/detail', { params: { id } }),
  create: (data: SaveTemplatePayload) => post<number>('/template/templates', data),
  update: (data: SaveTemplatePayload) => put<boolean>('/template/templates', data),
  delete: (id: number) => del<boolean>('/template/templates', { params: { id } }),
  updateStatus: (id: number, status: number) => put<boolean>('/template/templates/status', { id, status }),
  publishVersion: (data: PublishTemplateVersionPayload) => post<number>('/template/templates/versions', data),
  activateVersion: (data: ActivateTemplateVersionPayload) => put<boolean>('/template/templates/versions/current', data),
  extractVariables: (data: { sourceFormat: TemplateSourceFormat; content?: string; sourceFileId?: number }) =>
    post<string[]>('/template/templates/variables/extract', data),
  render: (data: TemplateRenderPayload) => post<TemplateRenderResult>('/template/templates/render', data),
  renderAsync: (data: TemplateRenderPayload) => post<TemplateRenderResult>('/template/templates/render/async', data),
  renderRecord: (id: number) => get<TemplateRenderRecord>('/template/templates/render-records/detail', { params: { id } }),
  renderRecordPage: (params?: TemplateRenderRecordQuery) => get<BackendPageResult<TemplateRenderRecord>>('/template/templates/render-records/page', { params: toBackendPageParams(params) })
    .then((data) => fromBackendPageResult(data, (item) => item, params)),
};

export const templateCategoryApi = {
  page: (params?: TemplateCategoryQuery) =>
    get<BackendPageResult<TemplateCategory>>('/template/categories/page', { params: toBackendPageParams(params) })
      .then((data) => fromBackendPageResult(data, normalizeCategory, params)),
  list: (params?: TemplateCategoryQuery) =>
    get<TemplateCategory[]>('/template/categories/list', { params: toBackendPageParams(params) })
      .then((list) => (Array.isArray(list) ? list.map(normalizeCategory) : [])),
  detail: (id: number) => get<TemplateCategory>('/template/categories/detail', { params: { id } }).then(normalizeCategory),
  create: (data: SaveTemplateCategoryPayload) => post<number>('/template/categories', data),
  update: (data: SaveTemplateCategoryPayload) => put<boolean>('/template/categories', data),
  updateStatus: (id: number, status: number) => put<boolean>('/template/categories/status', { id, status }),
  delete: (id: number) => del<boolean>('/template/categories', { params: { id } }),
};

interface BackendPageResult<T> {
  list?: T[];
  total?: number;
  page?: number;
  size?: number;
}

function normalizeTemplate(item: TemplateItem): TemplateItem {
  return {
    ...item,
    status: Number(item.status ?? 0),
    currentVersionNo: Number(item.currentVersionNo ?? 0),
    publishedVersionNo: Number(item.publishedVersionNo ?? item.currentVersionNo ?? 0),
    hasUnpublishedChanges: Boolean(item.hasUnpublishedChanges),
    unpublishedChangeReasons: Array.isArray(item.unpublishedChangeReasons) ? item.unpublishedChangeReasons : [],
  };
}

function normalizeCategory(item: TemplateCategory): TemplateCategory {
  return {
    ...item,
    sort: Number(item.sort ?? 0),
    status: Number(item.status ?? 0),
  };
}

function toBackendPageParams(params?: { pageNum?: number; pageSize?: number }) {
  if (!params) return params;
  const { pageNum, pageSize, ...rest } = params;
  return {
    ...rest,
    page: pageNum,
    size: pageSize,
  };
}

function fromBackendPageResult<T>(
  data: BackendPageResult<T>,
  mapper: (item: T) => T,
  params?: { pageNum?: number; pageSize?: number },
): PageResult<T> {
  const list = Array.isArray(data?.list) ? data.list.map(mapper) : [];
  return {
    list,
    total: Number(data?.total ?? list.length),
    pageNum: Number(data?.page ?? params?.pageNum ?? 1),
    pageSize: Number(data?.size ?? params?.pageSize ?? 10),
  };
}
