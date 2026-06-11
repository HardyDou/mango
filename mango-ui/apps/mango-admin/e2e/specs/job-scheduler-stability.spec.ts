import { expect, test, type APIRequestContext, type APIResponse } from '@playwright/test';
import { mkdir, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { api as e2eApi } from '../support/api';

test.describe.configure({ mode: 'serial' });

type ApiId = string;
type JobDefinitionStatus = 'DRAFT' | 'ENABLED' | 'DISABLED' | 'PAUSED';

interface BusinessResponse<T> {
  code?: number;
  success?: boolean;
  data?: T;
  msg?: string;
  message?: string;
}

interface PageData<T> {
  list?: T[];
  records?: T[];
  rows?: T[];
  data?: T[];
  total?: number;
}

interface JobDefinition {
  id: ApiId;
  jobCode: string;
  jobName: string;
  scheduleType?: string;
  scheduleExpression?: string;
  status?: JobDefinitionStatus;
  syncStatus?: string;
  syncError?: string;
  engineType?: string;
  engineJobId?: string;
}

interface JobInstance {
  id: ApiId;
  jobId: ApiId;
  jobCode?: string;
  jobName?: string;
  triggerType?: string;
  triggerBatchNo?: string;
  scheduledFireTime?: string;
  triggerTime?: string;
  actualFireTime?: string;
  startTime?: string;
  endTime?: string;
  status?: string;
  engineType?: string;
  workerAddress?: string;
  errorSummary?: string;
}

interface JobLogDetail {
  logSource?: string;
  nativeLogAvailable?: boolean;
  nativeLogContent?: string;
  engineResult?: string;
}

interface SaveJobDefinitionPayload {
  appCode: string;
  jobCode: string;
  jobName: string;
  jobType: string;
  scheduleType: string;
  scheduleExpression: string;
  handlerName: string;
  paramValue: string;
  misfireStrategy: string;
  concurrencyPolicy: string;
  timeoutSeconds: number;
  retryPolicy: string;
  engineType: string;
}

const terminalStatuses = new Set(['SUCCESS', 'FAILED', 'CANCELED', 'TIMEOUT']);
const evidenceDir = resolve(__dirname, '../../../../../mango-docs/evidence/2026-06-07-mango-native-job-e2e');
const handlerName = 'mangoJobRuntimeProbeHandler';

function api(path: string) {
  return e2eApi(path);
}

function stabilityMinutes() {
  const raw = Number(process.env.JOB_STABILITY_MINUTES || '6');
  if (!Number.isFinite(raw) || raw < 3) {
    return 6;
  }
  return Math.min(raw, 240);
}

function runKey(projectName: string) {
  return projectName.replace(/[^a-z0-9]+/gi, '_').replace(/^_+|_+$/g, '').toLowerCase() || 'chromium';
}

function headers(token: string) {
  return {
    Authorization: `Bearer ${token}`,
    'TENANT-ID': '1',
    'X-Mango-Tenant-Id': '1',
    'X-Tenant-Id': '1',
  };
}

async function expectBusinessOk<T>(response: APIResponse, context: string): Promise<T> {
  expect(response.status(), context).toBe(200);
  const body = await response.json() as BusinessResponse<T>;
  expect(body.success || body.code === 200, `${context}: ${body.msg || body.message || JSON.stringify(body)}`).toBeTruthy();
  return body.data as T;
}

async function loginToken(request: APIRequestContext) {
  const response = await request.post(api('/auth/login'), {
    data: {
      username: 'admin',
      password: 'admin123',
      tenantId: '1',
    },
  });
  const data = await expectBusinessOk<{ accessToken?: string; token?: string }>(response, '登录 Mango 管理后台');
  const token = data.accessToken || data.token || '';
  expect(token, '登录接口必须返回 accessToken').toBeTruthy();
  return token;
}

async function listDefinitions(request: APIRequestContext, token: string, keyword: string): Promise<JobDefinition[]> {
  const response = await request.get(api('/job/definitions/page'), {
    headers: headers(token),
    params: { keyword, pageNum: '1', pageSize: '50' },
  });
  const data = await expectBusinessOk<PageData<JobDefinition>>(response, '查询 Job 定义列表');
  return data.list || data.records || data.rows || data.data || [];
}

async function createDefinition(request: APIRequestContext, token: string, payload: SaveJobDefinitionPayload): Promise<ApiId> {
  return expectBusinessOk<ApiId>(await request.post(api('/job/definitions'), {
    headers: headers(token),
    data: payload,
  }), '创建每分钟稳定性任务');
}

async function detailDefinition(request: APIRequestContext, token: string, id: ApiId): Promise<JobDefinition> {
  const response = await request.get(api('/job/definitions/detail'), {
    headers: headers(token),
    params: { id: String(id) },
  });
  return expectBusinessOk<JobDefinition>(response, '查询 Job 定义详情');
}

async function updateDefinitionStatus(
  request: APIRequestContext,
  token: string,
  id: ApiId,
  status: JobDefinitionStatus,
) {
  await expectBusinessOk<boolean>(await request.put(api('/job/definitions/status'), {
    headers: headers(token),
    data: { id, status },
  }), `更新 Job 定义状态为 ${status}`);
}

async function deleteDefinition(request: APIRequestContext, token: string, id: ApiId) {
  await expectBusinessOk<boolean>(await request.delete(api('/job/definitions'), {
    headers: headers(token),
    params: { id: String(id) },
  }), '删除历史稳定性任务');
}

async function listInstances(request: APIRequestContext, token: string, jobId: ApiId): Promise<JobInstance[]> {
  const response = await request.get(api('/job/instances/page'), {
    headers: headers(token),
    params: {
      jobId: String(jobId),
      triggerType: 'SCHEDULED',
      pageNum: '1',
      pageSize: '100',
    },
  });
  const data = await expectBusinessOk<PageData<JobInstance>>(response, '查询每分钟调度实例');
  return data.list || data.records || data.rows || data.data || [];
}

async function detailInstanceLog(request: APIRequestContext, token: string, instanceId: ApiId): Promise<JobLogDetail> {
  return expectBusinessOk<JobLogDetail>(await request.get(api(`/job/instances/${instanceId}/logs`), {
    headers: headers(token),
  }), '查询每分钟任务执行日志');
}

async function moveDefinitionToDraft(request: APIRequestContext, token: string, definition: JobDefinition) {
  if (definition.status === 'ENABLED' || definition.status === 'PAUSED') {
    await updateDefinitionStatus(request, token, definition.id, 'DISABLED');
  }
  if (definition.status !== 'DRAFT') {
    await updateDefinitionStatus(request, token, definition.id, 'DRAFT');
  }
}

async function cleanupDefinitions(request: APIRequestContext, token: string, keyword: string) {
  const definitions = await listDefinitions(request, token, keyword);
  for (const definition of definitions.filter(item => item.jobCode.includes(keyword))) {
    await moveDefinitionToDraft(request, token, definition);
    await deleteDefinition(request, token, definition.id);
  }
}

async function waitForSyncedDefinition(request: APIRequestContext, token: string, id: ApiId): Promise<JobDefinition> {
  let latest: JobDefinition | undefined;
  await expect.poll(async () => {
    latest = await detailDefinition(request, token, id);
    return latest.syncStatus === 'SYNCED' && latest.engineType === 'MANGO_NATIVE' && Boolean(latest.engineJobId);
  }, {
    timeout: 30_000,
    intervals: [500, 1000, 2000],
    message: '每分钟稳定性任务必须同步到 Mango 原生引擎',
  }).toBeTruthy();
  expect(latest, '每分钟稳定性任务同步后必须能查询到定义详情').toBeDefined();
  return latest as JobDefinition;
}

function completedScheduledInstances(instances: JobInstance[]) {
  return instances
    .filter(item => item.triggerType === 'SCHEDULED' && terminalStatuses.has(item.status || ''))
    .sort((left, right) => String(left.scheduledFireTime || left.triggerTime || '')
      .localeCompare(String(right.scheduledFireTime || right.triggerTime || '')));
}

async function writeStabilityReport(report: string) {
  await mkdir(evidenceDir, { recursive: true });
  await writeFile(resolve(evidenceDir, 'job-scheduler-stability-local.md'), report, 'utf8');
}

test('每分钟 Cron 任务在连续观察窗口内稳定调度、无重复窗口、日志可读', async ({ request }, testInfo) => {
  test.skip(testInfo.project.name !== 'chromium', '稳定性观察只在 Chromium 项目执行，避免多浏览器并发创建调度任务');
  const minutes = stabilityMinutes();
  test.setTimeout((minutes * 60 + 180) * 1000);

  const token = await loginToken(request);
  const keyword = `mango_job_stability_${runKey(testInfo.project.name)}_`;
  const jobCode = `${keyword}${Date.now()}`;
  const jobName = `稳定性 每分钟任务 ${runKey(testInfo.project.name)}`;
  const startedAt = new Date();

  await cleanupDefinitions(request, token, keyword);
  const jobId = await createDefinition(request, token, {
    appCode: 'mango-job',
    jobCode,
    jobName,
    jobType: 'BUILTIN',
    scheduleType: 'CRON',
    scheduleExpression: '0 */1 * * * ?',
    handlerName,
    paramValue: JSON.stringify({ source: 'stability-e2e', minutes, jobCode }),
    concurrencyPolicy: 'SERIAL',
    misfireStrategy: 'IGNORE',
    timeoutSeconds: 120,
    retryPolicy: '{ "maxRetryTimes": 0 }',
    engineType: 'MANGO_NATIVE',
  });
  await updateDefinitionStatus(request, token, jobId, 'ENABLED');
  await waitForSyncedDefinition(request, token, jobId);

  const expectedMinimum = Math.max(2, Math.floor(minutes) - 2);
  const observedSnapshots: Array<{ at: string; completed: number; total: number }> = [];
  const deadline = Date.now() + minutes * 60_000;
  let latestInstances: JobInstance[] = [];

  while (Date.now() < deadline) {
    latestInstances = await listInstances(request, token, jobId);
    observedSnapshots.push({
      at: new Date().toISOString(),
      completed: completedScheduledInstances(latestInstances).length,
      total: latestInstances.length,
    });
    await new Promise(resolveWait => setTimeout(resolveWait, 15_000));
  }

  await expect.poll(async () => {
    latestInstances = await listInstances(request, token, jobId);
    return latestInstances.every(item => item.status !== 'WAITING' && item.status !== 'RUNNING');
  }, {
    timeout: 45_000,
    intervals: [3000, 5000],
    message: '观察窗口结束后不应存在长时间 WAITING/RUNNING 实例',
  }).toBeTruthy();

  await updateDefinitionStatus(request, token, jobId, 'DISABLED');

  const completed = completedScheduledInstances(latestInstances);
  const fireTimes = completed.map(item => item.scheduledFireTime || item.triggerTime || '');
  const duplicateFireTimes = fireTimes.filter((time, index) => time && fireTimes.indexOf(time) !== index);
  const failures = latestInstances.filter(item => item.status && item.status !== 'SUCCESS');
  const sampleInstance = completed[completed.length - 1];

  expect(completed.length, `连续 ${minutes} 分钟观察应至少产生 ${expectedMinimum} 个完成调度实例`).toBeGreaterThanOrEqual(expectedMinimum);
  expect(duplicateFireTimes, '同一计划触发窗口不能重复生成实例').toEqual([]);
  expect(failures.map(item => `${item.id}:${item.status}:${item.errorSummary || ''}`), '观察窗口内调度实例必须全部成功').toEqual([]);
  expect(sampleInstance?.workerAddress || '', '稳定性任务必须由真实 Worker 执行').toContain('embedded://');

  const logDetail = await detailInstanceLog(request, token, sampleInstance.id);
  expect(logDetail.logSource).toBe('MANGO_NATIVE');
  expect(logDetail.nativeLogAvailable).toBe(true);
  expect(logDetail.nativeLogContent || '').toContain('Mango Job runtime probe System.out');
  expect(logDetail.nativeLogContent || '').toContain('Mango Job runtime probe logger');
  expect(logDetail.nativeLogContent || '').toContain(jobCode);

  const endedAt = new Date();
  await writeStabilityReport([
    '# Mango Job 本地调度稳定性证据',
    '',
    `- jobCode: ${jobCode}`,
    `- jobName: ${jobName}`,
    `- jobId: ${jobId}`,
    `- schedule: 0 */1 * * * ?`,
    `- observeStartedAt: ${startedAt.toISOString()}`,
    `- observeEndedAt: ${endedAt.toISOString()}`,
    `- observeMinutes: ${minutes}`,
    `- expectedMinimumCompletedInstances: ${expectedMinimum}`,
    `- actualCompletedInstances: ${completed.length}`,
    `- totalInstances: ${latestInstances.length}`,
    `- duplicateScheduledFireTimes: ${duplicateFireTimes.length}`,
    `- nonSuccessInstances: ${failures.length}`,
    `- sampleInstanceId: ${sampleInstance.id}`,
    `- sampleWorkerAddress: ${sampleInstance.workerAddress || ''}`,
    `- sampleLogContainsSystemOut: ${String((logDetail.nativeLogContent || '').includes('Mango Job runtime probe System.out'))}`,
    `- sampleLogContainsLogger: ${String((logDetail.nativeLogContent || '').includes('Mango Job runtime probe logger'))}`,
    '',
    '## 观察快照',
    '',
    '| time | completed | total |',
    '|---|---:|---:|',
    ...observedSnapshots.map(item => `| ${item.at} | ${item.completed} | ${item.total} |`),
    '',
    '## 完成实例窗口',
    '',
    '| instanceId | scheduledFireTime | triggerTime | status | workerAddress |',
    '|---|---|---|---|---|',
    ...completed.map(item => `| ${item.id} | ${item.scheduledFireTime || ''} | ${item.triggerTime || ''} | ${item.status || ''} | ${item.workerAddress || ''} |`),
    '',
  ].join('\n'));
});
