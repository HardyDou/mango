import { del, get, post, put } from '@mango/common/utils/request';

export type ApiId = string | number;

export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export type NumgenSegmentType = 'TEXT' | 'DATE' | 'PARAM' | 'SEQ' | 'EXPR';
export type NumgenRuleStatus = 0 | 1 | 2 | 3;
export type NumgenPublishStatus = 0 | 1;
export type NumgenVersionState = 'DRAFT' | 'ACTIVE' | 'HISTORY';

export interface NumgenGenerator {
  id?: ApiId;
  genKey: string;
  genName: string;
  status?: number;
  currentRuleVersion?: number;
  currentPublishStatus?: NumgenPublishStatus;
  hasUnpublishedChanges?: boolean;
  createTime?: string;
  updateTime?: string;
}

export type NumgenGeneratorSavePayload = Pick<NumgenGenerator, 'id' | 'genKey' | 'genName' | 'status'>;

export interface NumgenGeneratorQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  status?: number | '';
}

export interface NumgenRule {
  id?: ApiId;
  genKey: string;
  ruleName: string;
  version?: number;
  status?: NumgenRuleStatus;
  publishStatus?: NumgenPublishStatus;
  versionState?: NumgenVersionState;
  createTime?: string;
  updateTime?: string;
}

export type NumgenVersion = NumgenRule;

export interface NumgenRuleQuery {
  pageNum?: number;
  pageSize?: number;
  genKey?: string;
  keyword?: string;
  status?: number | '';
  publishStatus?: NumgenPublishStatus | '';
}

export type NumgenVersionQuery = NumgenRuleQuery;

export interface NumgenRuleSegment {
  id?: ApiId;
  ruleId?: ApiId;
  sortOrder: number;
  segmentType: NumgenSegmentType;
  segmentName: string;
  literalValue?: string;
  variableKey?: string;
  dateFormat?: string;
  seqWidth?: number;
  padChar?: string;
}

export type NumgenSegment = NumgenRuleSegment;

export interface NumgenSegmentQuery {
  pageNum?: number;
  pageSize?: number;
  ruleId?: ApiId;
}

export interface NumgenHistory {
  id?: ApiId;
  genKey: string;
  ruleId?: ApiId;
  ruleVersion?: number;
  resultNo: string;
  bizKey?: string;
  inputDigest?: string;
  costMillis?: number;
  status?: number;
  errorMessage?: string;
  createTime?: string;
}

export interface NumgenHistoryQuery {
  pageNum?: number;
  pageSize?: number;
  genKey?: string;
  ruleVersion?: number | '';
  resultNo?: string;
  status?: number | '';
  bizKey?: string;
}

export interface NumgenNextRequest {
  genKey: string;
  params?: Record<string, unknown>;
}

export interface NumgenPreviewResponse {
  genKey: string;
  ruleVersion?: number;
  segments: Array<{
    sortOrder: number;
    segmentType: NumgenSegmentType;
    segmentName: string;
    value: string;
  }>;
  values: string[];
}

export interface NumgenBatchRequest {
  genKey: string;
  count: number;
  params?: Record<string, unknown>;
}

export interface NumgenPublishRequest {
  ruleId?: ApiId;
  genKey?: string;
}

export interface NumgenVersionPublishRequest {
  versionId: ApiId;
}

export interface NumgenPreviewRequest {
  genKey: string;
  count?: number;
  params?: Record<string, unknown>;
}

export const numgenApi = {
  pageGenerators: (params?: NumgenGeneratorQuery) => get<any>('/numgen/generators/page', { params: toBackendPageParams(params) })
    .then(data => fromBackendPageResult(data, fromBackendGenerator, params)),
  detailGenerator: (id: ApiId) => get<NumgenGenerator>('/numgen/generators/detail', { params: { id } }).then(fromBackendGenerator),
  createGenerator: (data: NumgenGeneratorSavePayload) => post<ApiId>('/numgen/generators', toBackendGeneratorPayload(data)),
  updateGenerator: (data: NumgenGeneratorSavePayload) => put<boolean>('/numgen/generators', toBackendGeneratorPayload(data)),
  updateGeneratorStatus: (id: ApiId, status: number) => put<boolean>('/numgen/generators/status', { id, status }),
  deleteGenerator: (id: ApiId) => del<boolean>('/numgen/generators', { params: { id } }),

  pageRules: (params?: NumgenRuleQuery) => get<any>('/numgen/rules/page', { params: toBackendPageParams(params) })
    .then(data => fromBackendPageResult(data, fromBackendRule, params)),
  detailRule: (id: ApiId) => get<NumgenRule>('/numgen/rules/detail', { params: { id } }).then(fromBackendRule),
  createRule: (data: NumgenRule) => post<ApiId>('/numgen/rules', toBackendRulePayload(data)),
  updateRule: (data: NumgenRule) => put<boolean>('/numgen/rules', toBackendRulePayload(data)),
  deleteRule: (id: ApiId) => del<boolean>('/numgen/rules', { params: { id } }),
  updateRuleStatus: (id: ApiId, status: NumgenRuleStatus) => put<boolean>('/numgen/rules/status', { id, status }),
  publishRule: (request: NumgenPublishRequest) => post<boolean>('/numgen/rules/publish', request),
  publishGenerator: (genKey: string) => numgenApi.publishRule({ genKey }),
  previewRule: (request: NumgenPreviewRequest) => post<NumgenPreviewResponse>('/numgen/rules/preview', request),

  pageVersions: (params?: NumgenVersionQuery) => numgenApi.pageRules(params),
  detailVersion: (id: ApiId) => numgenApi.detailRule(id),
  createVersion: (data: NumgenVersion) => numgenApi.createRule(data),
  updateVersion: (data: NumgenVersion) => numgenApi.updateRule(data),
  deleteVersion: (id: ApiId) => numgenApi.deleteRule(id),
  publishVersion: (request: NumgenVersionPublishRequest) => numgenApi.publishRule({ ruleId: request.versionId }),
  previewVersion: (request: NumgenPreviewRequest) => numgenApi.previewRule(request),

  pageSegments: (params?: NumgenSegmentQuery) => get<any>('/numgen/segments/page', { params: toBackendPageParams(params) })
    .then(data => fromBackendPageResult(data, fromBackendSegment, params)),
  createSegment: (data: NumgenRuleSegment) => post<ApiId>('/numgen/segments', toBackendSegmentPayload(data)),
  updateSegment: (data: NumgenRuleSegment) => put<boolean>('/numgen/segments', toBackendSegmentPayload(data)),
  deleteSegment: (id: ApiId) => del<boolean>('/numgen/segments', { params: { id } }),

  pageHistories: (params?: NumgenHistoryQuery) => get<any>('/numgen/histories/page', { params: toBackendPageParams(params) })
    .then(data => fromBackendPageResult(data, fromBackendHistory, params)),

  nextValue: (request: NumgenNextRequest) => post<string>('/numgen/next', request),
  batchValue: (request: NumgenBatchRequest) => post<string[]>('/numgen/batch', request),
};

function toBackendGeneratorPayload(data: NumgenGeneratorSavePayload): NumgenGeneratorSavePayload {
  return {
    id: data.id,
    genKey: data.genKey,
    genName: data.genName,
    status: data.status,
  };
}

function toBackendRulePayload(data: NumgenRule): NumgenRule {
  return {
    id: data.id,
    genKey: data.genKey,
    ruleName: data.ruleName,
    version: data.version,
    status: data.status,
    publishStatus: data.publishStatus,
  };
}

function toBackendSegmentPayload(data: NumgenRuleSegment): NumgenRuleSegment {
  return {
    id: data.id,
    ruleId: data.ruleId,
    sortOrder: data.sortOrder,
    segmentType: data.segmentType,
    segmentName: data.segmentName,
    literalValue: data.literalValue,
    variableKey: data.variableKey,
    dateFormat: data.dateFormat,
    seqWidth: data.seqWidth,
    padChar: data.padChar,
  };
}

function fromBackendGenerator(item: any): NumgenGenerator {
  return {
    ...item,
    id: normalizeId(item.id),
    createTime: normalizeDateTime(item.createTime),
    updateTime: normalizeDateTime(item.updateTime),
  };
}

function fromBackendRule(item: any): NumgenRule {
  return {
    ...item,
    id: normalizeId(item.id),
    createTime: normalizeDateTime(item.createTime),
    updateTime: normalizeDateTime(item.updateTime),
  };
}

function fromBackendSegment(item: any): NumgenRuleSegment {
  return {
    ...item,
    id: normalizeId(item.id),
  };
}

function fromBackendHistory(item: any): NumgenHistory {
  return {
    ...item,
    id: normalizeId(item.id),
    createTime: normalizeDateTime(item.createTime),
  };
}

function toBackendPageParams(params?: any) {
  if (!params) return params;
  const { pageNum, pageSize, status, publishStatus, ...rest } = params;
  return {
    ...rest,
    status: status === '' ? undefined : status,
    publishStatus: publishStatus === '' ? undefined : publishStatus,
    page: pageNum,
    size: pageSize,
  };
}

function fromBackendPageResult<T>(data: any, mapper: (item: any) => T, params?: { pageNum?: number; pageSize?: number }): PageResult<T> {
  const list = Array.isArray(data?.list) ? data.list.map(mapper) : [];
  return {
    list,
    total: Number(data?.total ?? list.length),
    pageNum: Number(data?.page ?? params?.pageNum ?? 1),
    pageSize: Number(data?.size ?? params?.pageSize ?? 10),
  };
}

function normalizeId(value: any): string {
  return value === undefined || value === null ? '' : String(value);
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
