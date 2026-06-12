import { get, post } from '@mango/common/utils/request';

export type SystemEventStatus = 'PENDING' | 'PROCESSING' | 'SUCCESS' | 'FAILED';

export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

interface BackendPageResult<T> {
  list?: T[];
  records?: T[];
  rows?: T[];
  data?: T[];
  total?: string | number;
  totalCount?: string | number;
  pageNum?: string | number;
  page?: string | number;
  current?: string | number;
  pageSize?: string | number;
  size?: string | number;
}

export interface SystemEvent {
  messageId: string;
  eventType?: string;
  businessType?: string;
  businessKey?: string;
  aggregateId?: string;
  occurredAt?: string;
  status?: SystemEventStatus;
  attemptCount?: number;
  nextAttemptAt?: string;
  lockedAt?: string;
  lockedBy?: string;
  errorMessage?: string;
  payload?: Record<string, unknown>;
  headers?: Record<string, string>;
}

export interface SystemEventQuery {
  pageNum?: number;
  pageSize?: number;
  status?: SystemEventStatus | '';
  eventType?: string;
  businessType?: string;
  businessKey?: string;
  keyword?: string;
  abnormalOnly?: boolean;
}

export const systemEventApi = {
  page: (params?: SystemEventQuery) =>
    get<BackendPageResult<SystemEvent>>('/system/events', { params: normalizeParams(params) })
      .then(data => fromBackendPageResult(data, params)),
  detail: (messageId: string) =>
    get<SystemEvent>('/system/events/detail', { params: { messageId } }).then(normalizeEvent),
  reconsume: (messageId: string) =>
    post<boolean>('/system/events/reconsume', { messageId }),
};

export const systemEventStatusOptions = [
  { label: '待投递', value: 'PENDING', type: 'warning' },
  { label: '处理中', value: 'PROCESSING', type: 'warning' },
  { label: '成功', value: 'SUCCESS', type: 'success' },
  { label: '失败', value: 'FAILED', type: 'danger' },
] as const;

export function systemEventStatusLabel(value?: SystemEventStatus | string) {
  return systemEventStatusOptions.find(item => item.value === value)?.label || value || '-';
}

export function systemEventStatusTagType(value?: SystemEventStatus | string): '' | 'success' | 'info' | 'warning' | 'danger' {
  return (systemEventStatusOptions.find(item => item.value === value)?.type || 'info') as '' | 'success' | 'info' | 'warning' | 'danger';
}

export function requestErrorMessage(error: unknown, fallback: string) {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  if (isRecord(error) && typeof error.message === 'string' && error.message) {
    return error.message;
  }
  return fallback;
}

function normalizeParams<T extends object | undefined>(params: T): T {
  if (!params) {
    return params;
  }
  const normalized: Record<string, unknown> = {};
  Object.entries(params as Record<string, unknown>).forEach(([key, value]) => {
    if (value !== '' && value !== undefined && value !== null) {
      normalized[key] = value;
    }
  });
  return normalized as T;
}

function fromBackendPageResult(
  data: BackendPageResult<SystemEvent> | unknown,
  params?: { pageNum?: number; pageSize?: number },
): PageResult<SystemEvent> {
  const page = isRecord(data) ? data as BackendPageResult<SystemEvent> : {};
  const records = page.list ?? page.records ?? page.rows ?? page.data ?? [];
  const list = Array.isArray(records) ? records.map(normalizeEvent) : [];
  return {
    list,
    total: Number(page.total ?? page.totalCount ?? 0),
    pageNum: Number(page.pageNum ?? page.page ?? page.current ?? params?.pageNum ?? 1),
    pageSize: Number(page.pageSize ?? page.size ?? params?.pageSize ?? 20),
  };
}

function normalizeEvent(item: SystemEvent | Record<string, unknown>): SystemEvent {
  const record = isRecord(item) ? item : {};
  return {
    ...record,
    messageId: String(record.messageId ?? ''),
    occurredAt: normalizeDateTime(record.occurredAt),
    nextAttemptAt: normalizeDateTime(record.nextAttemptAt),
    lockedAt: normalizeDateTime(record.lockedAt),
  } as SystemEvent;
}

function normalizeDateTime(value: unknown): string {
  if (!value) {
    return '';
  }
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value;
    return `${year}-${pad(month)}-${pad(day)} ${pad(hour)}:${pad(minute)}:${pad(second)}`;
  }
  return String(value).replace('T', ' ').replace('Z', '');
}

function pad(value: number): string {
  return String(value).padStart(2, '0');
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}
