import { del, get, post, put } from '@mango/common/utils/request';

export type ApiId = string;

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
  current?: string | number;
  pageSize?: string | number;
  size?: string | number;
}

export type JobDefinitionStatus = 'DRAFT' | 'ENABLED' | 'DISABLED' | 'PAUSED';
export type JobEngineType = 'MANGO_NATIVE';
export type JobType = 'BUILTIN';
export type JobScheduleType = 'CRON' | 'FIXED_RATE' | 'ONE_TIME' | 'MANUAL';
export type JobSyncStatus = 'PENDING' | 'SYNCED' | 'FAILED';
export type JobInstanceStatus =
  | 'CREATED'
  | 'WAITING'
  | 'DISPATCHED'
  | 'RUNNING'
  | 'RETRY_WAITING'
  | 'SUCCESS'
  | 'FAILED'
  | 'TIMEOUT'
  | 'CANCELED';
export type JobTriggerType = 'SCHEDULED' | 'MANUAL' | 'RETRY' | 'API';
export type JobAlarmType = 'INSTANCE_FAILED';
export type JobWorkerStatus =
  | 'REGISTERED'
  | 'ONLINE'
  | 'DRAINING'
  | 'OFFLINE'
  | 'EXPIRED'
  | 'DISABLED'
  | 'UNKNOWN';

export interface JobDefinition {
  id?: ApiId;
  tenantId?: string;
  appCode: string;
  jobCode: string;
  jobName: string;
  jobType: JobType;
  scheduleType: JobScheduleType;
  scheduleExpression?: string;
  handlerName?: string;
  paramSchema?: string;
  paramValue?: string;
  misfireStrategy?: string;
  concurrencyPolicy?: string;
  timeoutSeconds?: number;
  retryPolicy?: string;
  status?: JobDefinitionStatus;
  engineType: JobEngineType;
  engineAppId?: string;
  engineJobId?: string;
  syncStatus?: JobSyncStatus;
  syncError?: string;
  createdBy?: ApiId;
  createdAt?: string;
  updatedBy?: ApiId;
  updatedAt?: string;
}

export interface JobDefinitionQuery {
  pageNum?: number;
  pageSize?: number;
  appCode?: string;
  status?: JobDefinitionStatus | '';
  jobType?: JobType | '';
  scheduleType?: JobScheduleType | '';
  engineType?: JobEngineType | '';
  keyword?: string;
}

export interface JobInstance {
  id?: ApiId;
  tenantId?: string;
  jobId?: ApiId;
  jobCode?: string;
  jobName?: string;
  triggerType?: JobTriggerType;
  triggerUserId?: ApiId;
  triggerTime?: string;
  scheduledFireTime?: string;
  actualFireTime?: string;
  startTime?: string;
  endTime?: string;
  status?: JobInstanceStatus;
  durationMillis?: number;
  attemptCount?: number;
  resultSummary?: string;
  workerAddress?: string;
  engineType?: JobEngineType;
  engineInstanceId?: string;
  errorSummary?: string;
  traceId?: string;
  triggerBatchNo?: string;
}

export interface JobInstanceQuery {
  pageNum?: number;
  pageSize?: number;
  jobId?: ApiId | '';
  status?: JobInstanceStatus | '';
  triggerType?: JobTriggerType | '';
  triggerBatchNo?: string;
  triggerTimeStart?: string;
  triggerTimeEnd?: string;
}

export interface SyncJobInstancePayload {
  jobId?: ApiId | '';
  triggerTimeStart?: string;
  triggerTimeEnd?: string;
  size?: number;
}

export interface JobLogIndex {
  id?: ApiId;
  tenantId?: string;
  jobId?: ApiId;
  instanceId?: ApiId;
  engineType?: JobEngineType;
  engineInstanceId?: string;
  logLocation?: string;
  readOffset?: number;
  errorSummary?: string;
  lastFetchedAt?: string;
  createdAt?: string;
}

export interface JobLogDetail extends JobLogIndex {
  jobCode?: string;
  jobName?: string;
  instanceStatus?: JobInstanceStatus;
  triggerBatchNo?: string;
  logSource?: string;
  nativeLogAvailable?: boolean;
  logFetchStatus?: string;
  nativeLogContent?: string;
  content?: string;
  engineResult?: string;
}

export interface JobLogQuery {
  pageNum?: number;
  pageSize?: number;
  jobId?: ApiId | '';
  instanceId?: ApiId | '';
  engineType?: JobEngineType | '';
}

export interface JobWorkerSnapshot {
  id?: ApiId;
  tenantId?: string;
  appCode?: string;
  workerAddress?: string;
  engineType?: JobEngineType;
  engineWorkerId?: string;
  lastHeartbeatAt?: string;
  status?: JobWorkerStatus;
}

export interface JobWorkerHandlerPayload {
  appCode?: string;
  handlerName: string;
  jobType?: JobType;
  paramSchema?: string;
  concurrent?: boolean;
  timeoutSeconds?: number;
  retryPolicy?: string;
}

export interface CreateJobWorkerPayload {
  appCode: string;
  workerAddress: string;
  transportType: 'HTTP_INTERNAL';
  workerInstanceId?: string;
  handlers: JobWorkerHandlerPayload[];
}

export interface UpdateJobWorkerStatusPayload {
  id: ApiId;
  status: JobWorkerStatus;
}

export interface JobWorkerQuery {
  pageNum?: number;
  pageSize?: number;
  appCode?: string;
  status?: JobWorkerStatus | '';
  engineType?: JobEngineType | '';
  keyword?: string;
}

export interface JobHandler {
  appCode?: string;
  handlerName?: string;
  jobType?: JobType;
  paramSchema?: string;
  concurrent?: boolean;
  timeoutSeconds?: number;
  retryPolicy?: string;
}

export interface JobEngineStatus {
  engineType?: JobEngineType;
  pendingCount?: number;
  failedCount?: number;
  syncedCount?: number;
  lastUpdatedAt?: string;
}

export interface JobAlarmRule {
  id?: ApiId;
  tenantId?: string;
  jobId?: ApiId;
  jobCode?: string;
  jobName?: string;
  appCode: string;
  ruleName: string;
  alarmType: JobAlarmType;
  triggerCondition?: string;
  noticeSceneCode: string;
  noticeTemplateCode: string;
  noticeParams?: string;
  enabled?: boolean;
  createdBy?: ApiId;
  createdAt?: string;
  updatedBy?: ApiId;
  updatedAt?: string;
}

export interface JobAlarmRuleQuery {
  pageNum?: number;
  pageSize?: number;
  appCode?: string;
  jobId?: ApiId | '';
  alarmType?: JobAlarmType | '';
  enabled?: boolean | '';
  keyword?: string;
}

export interface TriggerJobPayload {
  jobId: ApiId;
  triggerBatchNo?: string;
  paramValue?: string;
}

export type SaveJobDefinitionPayload = Pick<
  JobDefinition,
  | 'id'
  | 'appCode'
  | 'jobCode'
  | 'jobName'
  | 'jobType'
  | 'scheduleType'
  | 'scheduleExpression'
  | 'handlerName'
  | 'paramSchema'
  | 'paramValue'
  | 'misfireStrategy'
  | 'concurrencyPolicy'
  | 'timeoutSeconds'
  | 'retryPolicy'
  | 'engineType'
>;

export type SaveJobAlarmRulePayload = Pick<
  JobAlarmRule,
  | 'id'
  | 'jobId'
  | 'appCode'
  | 'ruleName'
  | 'alarmType'
  | 'triggerCondition'
  | 'noticeSceneCode'
  | 'noticeTemplateCode'
  | 'noticeParams'
  | 'enabled'
>;

export interface UpdateJobAlarmRuleStatusPayload {
  id: ApiId;
  enabled: boolean;
}

export const jobApi = {
  pageDefinitions: (params?: JobDefinitionQuery) =>
    get<BackendPageResult<JobDefinition>>('/job/definitions/page', { params: normalizeParams(params) })
      .then(data => fromBackendPageResult<JobDefinition>(data, params)),
  detailDefinition: (id: ApiId) => get<JobDefinition>('/job/definitions/detail', { params: { id } }),
  createDefinition: (data: SaveJobDefinitionPayload) => post<ApiId>('/job/definitions', normalizeParams(data)),
  updateDefinition: (data: SaveJobDefinitionPayload) => put<boolean>('/job/definitions', normalizeParams(data)),
  updateDefinitionStatus: (id: ApiId, status: JobDefinitionStatus) =>
    put<boolean>('/job/definitions/status', { id, status }),
  deleteDefinition: (id: ApiId) => del<boolean>('/job/definitions', { params: { id } }),
  triggerDefinition: (data: TriggerJobPayload) => post<ApiId>('/job/definitions/trigger', normalizeParams(data)),

  pageInstances: (params?: JobInstanceQuery) =>
    get<BackendPageResult<JobInstance>>('/job/instances/page', { params: normalizeParams(params) })
      .then(data => fromBackendPageResult<JobInstance>(data, params)),
  syncInstances: (params?: SyncJobInstancePayload) => post<boolean>('/job/instances/sync', normalizeParams(params)),
  detailInstanceLog: (instanceId: ApiId) => get<JobLogDetail>(`/job/instances/${instanceId}/logs`),
  pageLogs: (params?: JobLogQuery) =>
    get<BackendPageResult<JobLogIndex>>('/job/logs/page', { params: normalizeParams(params) })
      .then(data => fromBackendPageResult<JobLogIndex>(data, params)),
  detailLog: (id: ApiId) => get<JobLogDetail>('/job/logs/detail', { params: { id } }),
  pageWorkers: (params?: JobWorkerQuery) =>
    get<BackendPageResult<JobWorkerSnapshot>>('/job/workers/page', { params: normalizeParams(params) })
      .then(data => fromBackendPageResult<JobWorkerSnapshot>(data, params)),
  createWorker: (data: CreateJobWorkerPayload) => post<ApiId>('/job/workers', normalizeParams(data)),
  updateWorkerStatus: (data: UpdateJobWorkerStatusPayload) => put<boolean>('/job/workers/status', normalizeParams(data)),
  listHandlers: () => get<JobHandler[]>('/job/handlers'),
  pageAlarmRules: (params?: JobAlarmRuleQuery) =>
    get<BackendPageResult<JobAlarmRule>>('/job/alarm-rules/page', { params: normalizeParams(params) })
      .then(data => fromBackendPageResult<JobAlarmRule>(data, params)),
  detailAlarmRule: (id: ApiId) => get<JobAlarmRule>('/job/alarm-rules/detail', { params: { id } }),
  createAlarmRule: (data: SaveJobAlarmRulePayload) => post<ApiId>('/job/alarm-rules', normalizeParams(data)),
  updateAlarmRule: (data: SaveJobAlarmRulePayload) => put<boolean>('/job/alarm-rules', normalizeParams(data)),
  updateAlarmRuleStatus: (data: UpdateJobAlarmRuleStatusPayload) =>
    put<boolean>('/job/alarm-rules/status', normalizeParams(data)),
  deleteAlarmRule: (id: ApiId) => del<boolean>('/job/alarm-rules', { params: { id } }),
  listEngineStatus: () => get<JobEngineStatus[]>('/job/engines/status'),
};

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

function fromBackendPageResult<T>(
  data: BackendPageResult<T> | unknown,
  params?: { pageNum?: number; pageSize?: number }
): PageResult<T> {
  const page = isRecord(data) ? (data as BackendPageResult<T>) : {};
  const records = page.list ?? page.records ?? page.rows ?? page.data ?? [];
  return {
    list: Array.isArray(records) ? records : [],
    total: Number(page.total ?? page.totalCount ?? 0),
    pageNum: Number(page.pageNum ?? page.current ?? params?.pageNum ?? 1),
    pageSize: Number(page.pageSize ?? page.size ?? params?.pageSize ?? 10),
  };
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
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

export const jobDefinitionStatusOptions = [
  { label: '草稿', value: 'DRAFT', type: 'info' },
  { label: '已启用', value: 'ENABLED', type: 'success' },
  { label: '已暂停', value: 'PAUSED', type: 'warning' },
  { label: '已禁用', value: 'DISABLED', type: 'danger' },
] as const;

export const jobTypeOptions = [
  { label: '内置处理器', value: 'BUILTIN' },
] as const;

export const scheduleTypeOptions = [
  { label: 'Cron', value: 'CRON' },
  { label: '固定频率', value: 'FIXED_RATE' },
  { label: '一次性', value: 'ONE_TIME' },
  { label: '手动', value: 'MANUAL' },
] as const;

export const engineTypeOptions = [
  { label: 'Mango 原生', value: 'MANGO_NATIVE' },
] as const;

export const syncStatusOptions = [
  { label: '待同步', value: 'PENDING', type: 'warning' },
  { label: '已同步', value: 'SYNCED', type: 'success' },
  { label: '同步失败', value: 'FAILED', type: 'danger' },
] as const;

export const instanceStatusOptions = [
  { label: '已创建', value: 'CREATED', type: 'info' },
  { label: '等待执行', value: 'WAITING', type: 'info' },
  { label: '已分发', value: 'DISPATCHED', type: 'warning' },
  { label: '执行中', value: 'RUNNING', type: 'warning' },
  { label: '等待重试', value: 'RETRY_WAITING', type: 'warning' },
  { label: '成功', value: 'SUCCESS', type: 'success' },
  { label: '失败', value: 'FAILED', type: 'danger' },
  { label: '超时', value: 'TIMEOUT', type: 'danger' },
  { label: '已取消', value: 'CANCELED', type: 'info' },
] as const;

export const triggerTypeOptions = [
  { label: '调度触发', value: 'SCHEDULED' },
  { label: '手动触发', value: 'MANUAL' },
  { label: '重试触发', value: 'RETRY' },
  { label: 'API触发', value: 'API' },
] as const;

export const workerStatusOptions = [
  { label: '已注册', value: 'REGISTERED', type: 'info' },
  { label: '在线', value: 'ONLINE', type: 'success' },
  { label: '排空中', value: 'DRAINING', type: 'warning' },
  { label: '离线', value: 'OFFLINE', type: 'danger' },
  { label: '已过期', value: 'EXPIRED', type: 'danger' },
  { label: '已禁用', value: 'DISABLED', type: 'info' },
  { label: '未知', value: 'UNKNOWN', type: 'info' },
] as const;

export const alarmTypeOptions = [
  { label: '实例失败', value: 'INSTANCE_FAILED', type: 'danger' },
] as const;

export const enabledOptions = [
  { label: '启用', value: true, type: 'success' },
  { label: '停用', value: false, type: 'info' },
] as const;

export function optionLabel(options: readonly { label: string; value: string }[], value?: string) {
  return options.find(item => item.value === value)?.label || value || '-';
}

export function booleanOptionLabel(options: readonly { label: string; value: boolean }[], value?: boolean) {
  return options.find(item => item.value === value)?.label || (value === undefined ? '-' : String(value));
}

export function optionTagType(
  options: readonly { value: string; type?: string }[],
  value?: string,
): '' | 'success' | 'info' | 'warning' | 'danger' {
  return (options.find(item => item.value === value)?.type || '') as '' | 'success' | 'info' | 'warning' | 'danger';
}

export function booleanOptionTagType(
  options: readonly { value: boolean; type?: string }[],
  value?: boolean,
): '' | 'success' | 'info' | 'warning' | 'danger' {
  return (options.find(item => item.value === value)?.type || '') as '' | 'success' | 'info' | 'warning' | 'danger';
}
