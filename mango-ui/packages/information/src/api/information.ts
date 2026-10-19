import type { HttpClient, HttpQuery } from '@mango/api-schema';

export type InformationId = string;

export type InformationFormStatus = 'DRAFT' | 'PUBLISHED' | 'DISABLED';
export type InformationSubmissionStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'CLOSED';

export interface InformationForm {
  id?: InformationId;
  formCode?: string;
  formName?: string;
  description?: string;
  status?: InformationFormStatus;
  fieldCount?: number;
  submissionCount?: number;
  hostedPath?: string;
  createdByName?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface InformationFormDetail extends InformationForm {
  definitionJson?: string;
  successMessage?: string;
  serverSecret?: string;
}

export interface InformationSubmission {
  id?: InformationId;
  formId?: InformationId;
  formCode?: string;
  formName?: string;
  definitionJson?: string;
  valuesJson?: string;
  sourcePage?: string;
  status?: InformationSubmissionStatus;
  processRemark?: string;
  submittedAt?: string;
  updatedAt?: string;
}

export interface InformationPageQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  status?: string;
  formId?: InformationId | '';
  /** 提交时间下界，ISO 字符串，含边界 */
  startTime?: string;
  /** 提交时间上界，ISO 字符串，含边界 */
  endTime?: string;
}

export interface InformationPageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
  pages?: number;
}

export interface SaveInformationFormPayload {
  id?: InformationId;
  formCode: string;
  formName: string;
  description?: string;
  definitionJson: string;
  successMessage?: string;
  status?: InformationFormStatus;
}

export interface PublicInformationForm {
  formCode: string;
  formName: string;
  description?: string;
  definitionJson: string;
  successMessage?: string;
}

export interface SubmitInformationPayload {
  formCode: string;
  values: Record<string, unknown>;
  sourcePage?: string;
  idempotencyKey?: string;
  serverSecret?: string;
}

export interface InformationSubmissionResult {
  /** 提交记录主键，可用于查询详情。 */
  id: InformationId;
  message: string;
}

type BackendPageResult<T> = { list?: T[]; total?: number; page?: number; size?: number; pages?: number };

function pageParams(params: InformationPageQuery) {
  const next: Record<string, unknown> = {};
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return;
    }
    next[key === 'pageNum' ? 'page' : key === 'pageSize' ? 'size' : key] = value;
  });
  return next;
}

function normalizePage<T>(result: BackendPageResult<T>): InformationPageResult<T> {
  return {
    list: result.list || [],
    total: Number(result.total || 0),
    pageNum: Number(result.page || 1),
    pageSize: Number(result.size || 10),
    pages: Number(result.pages || 0),
  };
}

export function createInformationApi(httpClient: HttpClient) {
  const get = <T>(url: string, params?: object) =>
    httpClient.request<T>({ method: 'GET', url, query: params as HttpQuery | undefined });
  const post = <T>(url: string, body?: unknown, params?: object) =>
    httpClient.request<T>({ method: 'POST', url, body, query: params as HttpQuery | undefined });
  const put = <T>(url: string, body?: unknown) => httpClient.request<T>({ method: 'PUT', url, body });
  const del = <T>(url: string, params?: object) =>
    httpClient.request<T>({ method: 'DELETE', url, query: params as HttpQuery | undefined });

  return {
    pageForms: (params: InformationPageQuery) =>
      get<BackendPageResult<InformationForm>>('/information/forms/page', pageParams(params)).then(normalizePage),
    detailForm: (id: InformationId) => get<InformationFormDetail>('/information/forms/detail', { id }),
    saveForm: (data: SaveInformationFormPayload) => post<InformationId>('/information/forms/save', data),
    updateFormStatus: (id: InformationId, status: InformationFormStatus) =>
      put<boolean>('/information/forms/status', { id, status }),
    regenerateFormSecret: (id: InformationId) =>
      post<string>('/information/forms/secret/regenerate', undefined, { id }),
    deleteForm: (id: InformationId) => del<boolean>('/information/forms', { id }),

    pageSubmissions: (params: InformationPageQuery) =>
      get<BackendPageResult<InformationSubmission>>('/information/submissions/page', pageParams(params)).then(
        normalizePage,
      ),
    detailSubmission: (id: InformationId) => get<InformationSubmission>('/information/submissions/detail', { id }),
    exportSubmissions: (params: InformationPageQuery) =>
      get<string>('/information/submissions/export', pageParams(params)),
    processSubmission: (data: { id: InformationId; status: InformationSubmissionStatus; processRemark?: string }) =>
      put<boolean>('/information/submissions/process', data),

    publicForm: (formCode: string) => get<PublicInformationForm>('/information/open/forms/detail', { formCode }),
    submitPublic: (data: SubmitInformationPayload) =>
      post<InformationSubmissionResult>('/information/open/forms/submit', data),
  };
}

export type InformationApi = ReturnType<typeof createInformationApi>;
