import { expect, test, type APIResponse, type Locator, type Page } from '@playwright/test';
import { mkdir } from 'node:fs/promises';
import { resolve } from 'node:path';

test.setTimeout(240 * 1000);
test.describe.configure({ mode: 'serial' });

type ApiId = string;
type JobDefinitionStatus = 'DRAFT' | 'ENABLED' | 'DISABLED' | 'PAUSED';

interface LoginHeaders {
  Authorization: string;
  'TENANT-ID': string;
  'X-Mango-Tenant-Id': string;
}

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
  appCode: string;
  jobCode: string;
  jobName: string;
  jobType: string;
  scheduleType: string;
  scheduleExpression?: string;
  handlerName?: string;
  status?: JobDefinitionStatus;
  engineType: string;
  engineAppId?: string;
  engineJobId?: string;
  syncStatus?: string;
  syncError?: string;
}

interface JobInstance {
  id: ApiId;
  jobId: ApiId;
  jobCode?: string;
  jobName?: string;
  triggerType?: string;
  triggerBatchNo?: string;
  status?: string;
  engineType?: string;
  engineInstanceId?: string;
  workerAddress?: string;
  errorSummary?: string;
}

interface JobLogDetail {
  id?: ApiId;
  jobId?: ApiId;
  instanceId?: ApiId;
  engineType?: string;
  engineInstanceId?: string;
  logLocation?: string;
  jobCode?: string;
  jobName?: string;
  logSource?: string;
  nativeLogAvailable?: boolean;
  logFetchStatus?: string;
  nativeLogContent?: string;
  content?: string;
  engineResult?: string;
}

interface JobWorkerSnapshot {
  id: ApiId;
  appCode?: string;
  workerAddress?: string;
  engineType?: string;
  engineWorkerId?: string;
  status?: string;
}

interface JobAlarmRule {
  id: ApiId;
  jobId?: ApiId;
  jobCode?: string;
  jobName?: string;
  appCode: string;
  ruleName: string;
  alarmType: string;
  noticeSceneCode: string;
  noticeTemplateCode: string;
  noticeParams?: string;
  enabled?: boolean;
}

interface UpdateJobWorkerStatusPayload {
  id: ApiId;
  status: 'ONLINE' | 'DRAINING' | 'OFFLINE' | 'DISABLED';
}

function expectDefined<T>(value: T | undefined, message: string): T {
  expect(value, message).toBeDefined();
  return value as T;
}

const runtimeProbeHandler = 'mangoJobRuntimeProbeHandler';

interface SaveJobDefinitionPayload {
  id?: ApiId;
  appCode: string;
  jobCode: string;
  jobName: string;
  jobType: string;
  scheduleType: string;
  scheduleExpression?: string;
  handlerName?: string;
  paramSchema?: string;
  paramValue?: string;
  misfireStrategy?: string;
  concurrencyPolicy?: string;
  timeoutSeconds?: number;
  retryPolicy?: string;
  engineType: string;
}

const baseExampleJobs: SaveJobDefinitionPayload[] = [
  {
    appCode: 'mango-job',
    jobCode: 'mango_job_example_manual_builtin',
    jobName: '示例 手动内置任务',
    jobType: 'BUILTIN',
    scheduleType: 'MANUAL',
    handlerName: runtimeProbeHandler,
    paramSchema: JSON.stringify({
      type: 'object',
      properties: {
        source: { type: 'string', title: '来源' },
        assert: { type: 'string', title: '断言标记' },
      },
      required: ['source'],
    }),
    paramValue: '{ "source": "acceptance", "kind": "manual" }',
    concurrencyPolicy: 'SERIAL',
    misfireStrategy: 'IGNORE',
    timeoutSeconds: 300,
    retryPolicy: '{ "maxRetryTimes": 1 }',
    engineType: 'MANGO_NATIVE',
  },
  {
    appCode: 'mango-job',
    jobCode: 'mango_job_example_cron_native',
    jobName: '示例 Cron Mango 原生任务',
    jobType: 'BUILTIN',
    scheduleType: 'CRON',
    scheduleExpression: '0 */5 * * * ?',
    handlerName: runtimeProbeHandler,
    paramValue: '{ "source": "acceptance", "kind": "cron" }',
    concurrencyPolicy: 'SERIAL',
    misfireStrategy: 'FIRE_ONCE',
    timeoutSeconds: 600,
    retryPolicy: '{ "maxRetryTimes": 2 }',
    engineType: 'MANGO_NATIVE',
  },
  {
    appCode: 'mango-job',
    jobCode: 'mango_job_example_fixed_rate_probe',
    jobName: '示例 固定频率任务',
    jobType: 'BUILTIN',
    scheduleType: 'FIXED_RATE',
    scheduleExpression: '60000',
    handlerName: runtimeProbeHandler,
    paramValue: '{ "source": "acceptance", "kind": "fixed-rate" }',
    concurrencyPolicy: 'DISCARD',
    misfireStrategy: 'IGNORE',
    timeoutSeconds: 120,
    retryPolicy: '{ "maxRetryTimes": 0 }',
    engineType: 'MANGO_NATIVE',
  },
  {
    appCode: 'mango-job',
    jobCode: 'mango_job_example_every_minute_cron_probe',
    jobName: '示例 每分钟执行一次任务',
    jobType: 'BUILTIN',
    scheduleType: 'CRON',
    scheduleExpression: '0 */1 * * * ?',
    handlerName: runtimeProbeHandler,
    paramValue: '{ "source": "acceptance", "kind": "every-minute-cron" }',
    concurrencyPolicy: 'SERIAL',
    misfireStrategy: 'IGNORE',
    timeoutSeconds: 120,
    retryPolicy: '{ "maxRetryTimes": 0 }',
    engineType: 'MANGO_NATIVE',
  },
];

const evidenceDir = resolve(__dirname, '../../../../../mango-docs/evidence/2026-06-07-mango-native-job-e2e');

function projectRunKey(projectName: string) {
  return projectName.replace(/[^a-z0-9]+/gi, '_').replace(/^_+|_+$/g, '').toLowerCase() || 'default';
}

function exampleJobKeyword(runKey: string) {
  return `mango_job_example_${runKey}_`;
}

function buildExampleJobs(runKey: string): SaveJobDefinitionPayload[] {
  return baseExampleJobs.map((payload) => ({
    ...payload,
    jobCode: payload.jobCode.replace('mango_job_example_', exampleJobKeyword(runKey)),
    jobName: `${payload.jobName} ${runKey}`,
  }));
}

async function saveEvidenceScreenshot(page: Page, filename: string, locator?: Locator) {
  await mkdir(evidenceDir, { recursive: true });
  const target = locator || page;
  await target.screenshot({ path: resolve(evidenceDir, filename) });
}

async function login(page: Page) {
  await page.goto('/#/login');
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  const accountTenantsResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/auth/login-institutions') && response.status() === 200
  );
  await page.locator('input[placeholder="密码"]').blur();
  await accountTenantsResponsePromise;
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: /芒果集团/ }).click();
  await page.locator('.login-btn').click();
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

async function apiHeaders(page: Page): Promise<LoginHeaders> {
  return page.evaluate(() => {
    const token = sessionStorage.getItem('MANGO_TOKEN') || '';
    const userInfo = JSON.parse(sessionStorage.getItem('userInfo') || '{}') as { tenantId?: string | number };
    const tenantId = String(userInfo.tenantId || '1');
    return {
      Authorization: token ? `Bearer ${token}` : '',
      'TENANT-ID': tenantId,
      'X-Mango-Tenant-Id': tenantId,
    };
  });
}

async function expectBusinessOk<T>(response: APIResponse): Promise<T> {
  expect(response.status()).toBe(200);
  const body = await response.json() as BusinessResponse<T>;
  expect(body.success || body.code === 200, body.msg || body.message || '接口应返回业务成功').toBeTruthy();
  return body.data as T;
}

async function listDefinitions(page: Page, headers: LoginHeaders, keyword: string): Promise<JobDefinition[]> {
  const response = await page.request.get('/api/job/definitions/page', {
    headers,
    params: { keyword, pageNum: '1', pageSize: '50' },
  });
  const data = await expectBusinessOk<PageData<JobDefinition>>(response);
  return data.list || data.records || data.rows || data.data || [];
}

async function listInstances(page: Page, headers: LoginHeaders, jobId: ApiId): Promise<JobInstance[]> {
  const response = await page.request.get('/api/job/instances/page', {
    headers,
    params: { jobId: String(jobId), pageNum: '1', pageSize: '20' },
  });
  const data = await expectBusinessOk<PageData<JobInstance>>(response);
  return data.list || data.records || data.rows || data.data || [];
}

async function detailInstanceLog(page: Page, headers: LoginHeaders, instanceId: ApiId): Promise<JobLogDetail> {
  const response = await page.request.get(`/api/job/instances/${instanceId}/logs`, {
    headers,
  });
  return expectBusinessOk<JobLogDetail>(response);
}

async function listWorkers(page: Page, headers: LoginHeaders, keyword?: string): Promise<JobWorkerSnapshot[]> {
  const response = await page.request.get('/api/job/workers/page', {
    headers,
    params: { appCode: 'mango-job', engineType: 'MANGO_NATIVE', keyword: keyword || '', pageNum: '1', pageSize: '20' },
  });
  const data = await expectBusinessOk<PageData<JobWorkerSnapshot>>(response);
  return data.list || data.records || data.rows || data.data || [];
}

async function listAlarmRules(page: Page, headers: LoginHeaders, keyword: string): Promise<JobAlarmRule[]> {
  const response = await page.request.get('/api/job/alarm-rules/page', {
    headers,
    params: { keyword, pageNum: '1', pageSize: '20' },
  });
  const data = await expectBusinessOk<PageData<JobAlarmRule>>(response);
  return data.list || data.records || data.rows || data.data || [];
}

async function deleteAlarmRule(page: Page, headers: LoginHeaders, id: ApiId) {
  await expectBusinessOk<boolean>(await page.request.delete('/api/job/alarm-rules', {
    headers,
    params: { id: String(id) },
  }));
}

async function deleteAlarmRulesByKeyword(page: Page, headers: LoginHeaders, keyword: string) {
  const alarmRules = await listAlarmRules(page, headers, keyword);
  for (const alarmRule of alarmRules.filter(item => item.ruleName.includes(keyword))) {
    await deleteAlarmRule(page, headers, alarmRule.id);
  }
}

async function updateWorkerStatus(page: Page, headers: LoginHeaders, payload: UpdateJobWorkerStatusPayload) {
  await expectBusinessOk<boolean>(await page.request.put('/api/job/workers/status', { headers, data: payload }));
}

async function resetWorkersByKeyword(page: Page, headers: LoginHeaders, keyword: string) {
  const workers = await listWorkers(page, headers, keyword);
  for (const worker of workers.filter(item => item.workerAddress?.includes(keyword))) {
    await updateWorkerStatus(page, headers, { id: worker.id, status: 'DISABLED' });
  }
}

async function createDefinition(page: Page, headers: LoginHeaders, payload: SaveJobDefinitionPayload): Promise<ApiId> {
  return expectBusinessOk<ApiId>(await page.request.post('/api/job/definitions', { headers, data: payload }));
}

async function detailDefinition(page: Page, headers: LoginHeaders, id: ApiId): Promise<JobDefinition> {
  const response = await page.request.get('/api/job/definitions/detail', {
    headers,
    params: { id: String(id) },
  });
  return expectBusinessOk<JobDefinition>(response);
}

async function updateDefinitionStatus(page: Page, headers: LoginHeaders, id: ApiId, status: JobDefinitionStatus) {
  await expectBusinessOk<boolean>(await page.request.put('/api/job/definitions/status', {
    headers,
    data: { id, status },
  }));
}

async function deleteDefinition(page: Page, headers: LoginHeaders, id: ApiId) {
  await expectBusinessOk<boolean>(await page.request.delete('/api/job/definitions', {
    headers,
    params: { id: String(id) },
  }));
}

async function moveDefinitionToDraft(page: Page, headers: LoginHeaders, definition: JobDefinition) {
  if (definition.status === 'DRAFT') {
    return;
  }
  if (definition.status === 'ENABLED' || definition.status === 'PAUSED') {
    await updateDefinitionStatus(page, headers, definition.id, 'DISABLED');
  }
  await updateDefinitionStatus(page, headers, definition.id, 'DRAFT');
}

async function deleteDefinitionsByKeyword(page: Page, headers: LoginHeaders, keyword: string) {
  const definitions = await listDefinitions(page, headers, keyword);
  for (const definition of definitions.filter(item => item.jobCode.includes(keyword))) {
    await moveDefinitionToDraft(page, headers, definition);
    await deleteDefinition(page, headers, definition.id);
  }
}

async function prepareExampleJobs(
  page: Page,
  headers: LoginHeaders,
  jobs: SaveJobDefinitionPayload[],
  keyword: string,
) {
  await deleteDefinitionsByKeyword(page, headers, keyword);
  for (const payload of jobs) {
    const id = await createDefinition(page, headers, payload);
    await updateDefinitionStatus(page, headers, id, 'ENABLED');
    const synced = await waitForSyncedDefinition(page, headers, id);
    expect(synced.engineType).toBe('MANGO_NATIVE');
    expect(synced.syncStatus, synced.syncError || '任务定义必须同步到 Mango Native').toBe('SYNCED');
    expect(synced.engineAppId).toBeTruthy();
    expect(synced.engineJobId).toBeTruthy();
  }
}

async function waitForSyncedDefinition(page: Page, headers: LoginHeaders, id: ApiId): Promise<JobDefinition> {
  let latest: JobDefinition | undefined;
  await expect.poll(async () => {
    latest = await detailDefinition(page, headers, id);
    return latest.syncStatus === 'SYNCED' && Boolean(latest.engineJobId);
  }, {
    timeout: 30_000,
    intervals: [500, 1000, 2000],
    message: '任务定义必须获得真实 Mango Native engineJobId',
  }).toBeTruthy();
  return expectDefined(latest, '任务定义同步后必须可读取详情');
}

async function waitForTriggeredInstance(
  page: Page,
  headers: LoginHeaders,
  jobId: ApiId,
  triggerBatchNo: string,
): Promise<JobInstance> {
  let latest: JobInstance | undefined;
  await expect.poll(async () => {
    const instances = await listInstances(page, headers, jobId);
    latest = instances.find(item => item.triggerBatchNo === triggerBatchNo && item.triggerType === 'MANUAL');
    return Boolean(latest?.id) && latest?.status !== 'WAITING' && latest?.status !== 'RUNNING';
  }, {
    timeout: 60_000,
    intervals: [1000, 2000, 3000],
    message: '手动触发必须生成真实 Mango Native 执行实例并完成执行',
  }).toBeTruthy();
  const completed = expectDefined(latest, '手动触发执行实例必须存在');
  expect(completed.engineType).toBe('MANGO_NATIVE');
  expect(completed.status, completed.errorSummary || '示例任务应执行成功').toBe('SUCCESS');
  return completed;
}

async function waitForScheduledInstance(
  page: Page,
  headers: LoginHeaders,
  jobId: ApiId,
): Promise<JobInstance[]> {
  let latest: JobInstance[] = [];
  await expect.poll(async () => {
    const instances = await listInstances(page, headers, jobId);
    latest = instances.filter(item =>
      item.triggerType === 'SCHEDULED'
      && Boolean(item.id)
      && item.status !== 'WAITING'
      && item.status !== 'RUNNING'
    );
    return new Set(latest.map(item => item.id)).size >= 2;
  }, {
    timeout: 180_000,
    intervals: [5000, 10_000, 15_000],
    message: '每分钟示例任务必须产生至少两次可见的 Mango Native 调度执行实例',
  }).toBeTruthy();
  expect(latest[0].engineType).toBe('MANGO_NATIVE');
  expect(latest[0].status, latest[0].errorSummary || '每分钟示例任务应执行成功').toBe('SUCCESS');
  return latest;
}

async function waitForExecutionLogDetail(
  page: Page,
  headers: LoginHeaders,
  instanceId: ApiId,
): Promise<JobLogDetail> {
  let latest: JobLogDetail | undefined;
  const startedAt = Date.now();
  const timeoutMs = 120_000;
  while (Date.now() - startedAt < timeoutMs) {
    latest = await detailInstanceLog(page, headers, instanceId);
    const content = latest.nativeLogContent || '';
    if (latest.logSource === 'MANGO_NATIVE'
      && latest.nativeLogAvailable === true
      && content.includes('Mango Job runtime probe System.out')
      && content.includes('Mango Job runtime probe logger')
      && content.includes('handlerResult=Mango Job runtime probe executed')) {
      return latest;
    }
    await page.waitForTimeout(3_000);
  }

  throw new Error([
    '执行日志必须归档并可通过 Mango 详情接口读取',
    `instanceId=${instanceId}`,
    `logSource=${latest?.logSource || ''}`,
    `nativeLogAvailable=${String(latest?.nativeLogAvailable)}`,
    `logFetchStatus=${latest?.logFetchStatus || ''}`,
    `engineInstanceId=${String(latest?.engineInstanceId || '')}`,
    `engineResult=${(latest?.engineResult || '').slice(0, 240)}`,
    `nativeLogContent=${(latest?.nativeLogContent || '').slice(0, 240)}`,
    `content=${(latest?.content || '').slice(0, 240)}`,
  ].join('\n'));
}

async function expectVisibleExecutionLog(drawer: Locator, snippets: string[]) {
  await expect(drawer).toBeVisible();
  const logOutput = drawer.locator('.job-log-output', { hasText: '执行日志' }).last();
  await expect(logOutput).toBeVisible();
  for (const snippet of snippets) {
    await expect(logOutput).toContainText(snippet, { timeout: 70_000 });
  }
}

async function clickMenu(page: Page, name: string) {
  const menu = page.locator(
    [
      '.layout-top-system-item',
      '.layout-columns-aside li',
      '.el-sub-menu__title',
      '.el-menu-item',
    ].join(', '),
    { hasText: name },
  ).first();

  await expect(menu, `${name} 菜单必须可见`).toBeVisible({ timeout: 10000 });
  await menu.click();
}

async function openJobDefinitionPage(page: Page) {
  await clickMenu(page, '平台能力');
  await clickMenu(page, '任务管理');
  const responsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/job/definitions/page') && response.status() === 200
  );
  await clickMenu(page, '任务定义');
  await responsePromise;
  await expect(page).toHaveURL(/#\/job\/definition$/);
  await expect(page.getByRole('heading', { name: '任务定义' })).toBeVisible();
}

async function selectFormOption(page: Page, dialog: ReturnType<Page['getByRole']>, label: string, option: string) {
  const field = dialog.locator('.el-form-item', { hasText: label }).first();
  await field.locator('.el-select').click();
  await page.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: option }).first().click();
}

function paramEditor(dialog: Locator) {
  return dialog.locator('.job-param-editor').first();
}

async function fillParamTextField(editor: Locator, label: string, value: string) {
  const field = editor.locator('.el-form-item', { hasText: label }).first();
  await expect(field).toBeVisible();
  await field.getByRole('textbox').fill(value);
}

async function fillParamNumberField(editor: Locator, label: string, value: string) {
  const field = editor.locator('.el-form-item', { hasText: label }).first();
  await expect(field).toBeVisible();
  await field.locator('input').fill(value);
}

async function createDraftByUi(page: Page, jobCode: string) {
  await page.getByRole('button', { name: '新增任务' }).click();
  const dialog = page.getByRole('dialog', { name: '新增任务' });
  await expect(dialog).toBeVisible();
  await expect(dialog.getByText('基本信息')).toBeVisible();
  await expect(dialog.getByText('执行配置')).toBeVisible();
  await expect(dialog.getByText('参数配置')).toBeVisible();

  await dialog.getByLabel('所属应用').fill('mango-job');
  await dialog.getByLabel('任务编码').fill(jobCode);
  await dialog.getByLabel('任务名称').fill('E2E 验收草稿任务');
  await selectFormOption(page, dialog, '底层引擎', 'Mango 原生');
  await selectFormOption(page, dialog, '任务类型', '内置处理器');
  await dialog.getByLabel('处理器').fill(runtimeProbeHandler);
  await selectFormOption(page, dialog, '调度类型', '手动');
  await dialog.getByLabel('超时秒数').fill('180');
  await dialog.getByLabel('并发策略').fill('SERIAL');
  await dialog.getByLabel('错过策略').fill('IGNORE');
  await dialog.getByLabel('参数 Schema').fill(JSON.stringify({
    type: 'object',
    properties: {
      source: { type: 'string', title: '来源', description: '任务来源标记' },
      batchSize: { type: 'integer', title: '批处理数量', default: 100, minimum: 1, maximum: 500 },
      dryRun: { type: 'boolean', title: '试运行', default: false },
    },
    required: ['source'],
  }, null, 2));
  const editor = paramEditor(dialog);
  await expect(editor.getByText('按参数 Schema 生成表单')).toBeVisible();
  await fillParamTextField(editor, '来源', 'e2e');
  await fillParamNumberField(editor, '批处理数量', '128');
  await expect(editor.locator('.el-form-item', { hasText: '试运行' }).first()).toBeVisible();
  await dialog.getByLabel('重试策略').fill('{ "maxRetryTimes": 1 }');
  await saveEvidenceScreenshot(page, '00-definition-create-manual-schedule.png', dialog);

  const responsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/job/definitions') &&
    response.request().method() === 'POST' &&
    response.status() === 200
  );
  await dialog.getByRole('button', { name: '保存' }).click();
  await responsePromise;
  await expect(page.locator('.el-message__content', { hasText: '任务已创建' }).last()).toBeVisible({ timeout: 10000 });
}

async function searchDefinition(page: Page, keyword: string) {
  await page.getByPlaceholder('编码/名称/处理器').fill(keyword);
  const responsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/job/definitions/page') && response.status() === 200
  );
  await page.getByRole('button', { name: '查询' }).click();
  await responsePromise;
}

async function selectInstanceTaskFilter(page: Page, keyword: string) {
  const taskField = page.locator('.job-search .el-form-item', { hasText: '任务' }).first();
  const taskCombobox = taskField.getByRole('combobox', { name: '任务' });
  await taskField.locator('.el-select').click();
  const responsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/job/definitions/page') && response.status() === 200
  );
  await taskCombobox.fill(keyword);
  await responsePromise;
  await page.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: keyword }).first().click();
}

async function openDefinitionEditor(page: Page, jobCode: string) {
  await searchDefinition(page, jobCode);
  await definitionRow(page, jobCode).getByRole('button', { name: '编辑' }).click();
  const dialog = page.getByRole('dialog', { name: '编辑任务' });
  await expect(dialog).toBeVisible();
  return dialog;
}

function definitionRow(page: Page, jobCode: string) {
  return page.locator('.el-table__row', { hasText: jobCode }).first();
}

async function expectCompactJobToolbar(page: Page) {
  const toolbarHead = page.locator('.job-toolbar-head');
  const addButton = toolbarHead.getByRole('button', { name: '新增任务' });
  await expect(toolbarHead).toHaveCSS('display', 'flex');
  await expect(toolbarHead).toHaveCSS('justify-content', 'space-between');
  await expect(page.locator('.job-search')).toHaveCSS('display', 'flex');
  await expect(page.locator('.job-search')).toHaveCSS('flex-wrap', 'wrap');
  const headBox = await toolbarHead.boundingBox();
  const buttonBox = await addButton.boundingBox();
  expect(buttonBox?.x || 0).toBeGreaterThan((headBox?.x || 0) + ((headBox?.width || 0) / 2));
}

async function expectCompactSearchPage(page: Page, toolbar: Locator) {
  await expect(page.locator('.job-search')).toHaveCSS('display', 'flex');
  await expect(page.locator('.job-search')).toHaveCSS('flex-wrap', 'wrap');
  const toolbarBox = await toolbar.boundingBox();
  expect(toolbarBox?.height || 0).toBeLessThan(190);
}

async function openWorkerPage(page: Page) {
  const responsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/job/workers/page') && response.status() === 200
  );
  await clickMenu(page, 'Worker 节点');
  await responsePromise;
  await expect(page).toHaveURL(/#\/job\/worker$/);
  await expect(page.getByRole('heading', { name: 'Worker 节点' })).toBeVisible();
}

async function searchWorker(page: Page, keyword: string) {
  await page.getByPlaceholder('Worker 地址/实例标识').fill(keyword);
  const responsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/job/workers/page') && response.status() === 200
  );
  await page.getByRole('button', { name: '查询' }).click();
  await responsePromise;
}

function workerRow(page: Page, keyword: string) {
  return page.locator('.el-table__row', { hasText: keyword }).first();
}

async function openAlarmPage(page: Page) {
  const responsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/job/alarm-rules/page') && response.status() === 200
  );
  await clickMenu(page, '告警规则');
  await responsePromise;
  await expect(page).toHaveURL(/#\/job\/alarm$/);
  await expect(page.getByRole('heading', { name: '告警规则' })).toBeVisible();
}

async function searchAlarmRule(page: Page, keyword: string) {
  await page.getByPlaceholder('规则/场景/模板').fill(keyword);
  const responsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/job/alarm-rules/page') && response.status() === 200
  );
  await page.getByRole('button', { name: '查询' }).click();
  await responsePromise;
}

function alarmRuleRow(page: Page, keyword: string) {
  return page.locator('.el-table__row', { hasText: keyword }).first();
}

test.describe('Job 管理 E2E', () => {
  test('任务定义完成新增、查询、编辑、状态、触发和删除，并保留典型示例任务', async ({ page }, testInfo) => {
    const consoleErrors: string[] = [];
    const failedRequests: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        consoleErrors.push(message.text());
      }
    });
    page.on('response', (response) => {
      const url = response.url();
      if ((url.includes('/api/job/') || url.includes('/assets/')) && response.status() >= 400) {
        failedRequests.push(`${response.status()} ${url}`);
      }
    });

    await login(page);
    const headers = await apiHeaders(page);
    const runKey = projectRunKey(testInfo.project.name);
    const exampleJobs = buildExampleJobs(runKey);
    const exampleKeyword = exampleJobKeyword(runKey);
    await prepareExampleJobs(page, headers, exampleJobs, exampleKeyword);
    await deleteDefinitionsByKeyword(page, headers, 'mango_job_e2e_draft_');

    await openJobDefinitionPage(page);
    await expect(page.locator('.job-search')).toBeVisible();
    await expectCompactJobToolbar(page);
    const toolbarBox = await page.locator('.job-toolbar').boundingBox();
    const panelBox = await page.locator('.job-panel').boundingBox();
    expect(toolbarBox?.height || 0).toBeLessThan(170);
    expect(panelBox?.y || 0).toBeLessThan(260);

    await searchDefinition(page, exampleKeyword);
    for (const payload of exampleJobs) {
      await expect(definitionRow(page, payload.jobCode)).toBeVisible({ timeout: 10000 });
    }
    await saveEvidenceScreenshot(page, '01-definition-list-frequency.png');

    await page.getByRole('button', { name: /更多筛选/ }).click();
    await expect(page.locator('.job-search-more')).toBeVisible();
    await saveEvidenceScreenshot(page, '02-definition-more-filters.png');

    const cronDialog = await openDefinitionEditor(page, exampleJobs[1].jobCode);
    await expect(cronDialog.getByLabel('调度表达式')).toHaveValue('0 */5 * * * ?');
    await saveEvidenceScreenshot(page, '03-definition-edit-cron-schedule.png', cronDialog);
    await cronDialog.getByRole('button', { name: '取消' }).click();

    const fixedRateDialog = await openDefinitionEditor(page, exampleJobs[2].jobCode);
    await expect(fixedRateDialog.getByLabel('调度表达式')).toHaveValue('60000');
    await saveEvidenceScreenshot(page, '04-definition-edit-fixed-rate-schedule.png', fixedRateDialog);
    await fixedRateDialog.getByRole('button', { name: '取消' }).click();

    const draftCode = `mango_job_e2e_draft_${Date.now()}`;
    await createDraftByUi(page, draftCode);
    await searchDefinition(page, draftCode);
    const draftRow = definitionRow(page, draftCode);
    await expect(draftRow).toContainText('E2E 验收草稿任务');
    await expect(draftRow).toContainText('草稿');

    await draftRow.getByRole('button', { name: '编辑' }).click();
    const editDialog = page.getByRole('dialog', { name: '编辑任务' });
    await expect(editDialog).toBeVisible();
    await editDialog.getByLabel('任务名称').fill('E2E 验收草稿任务已编辑');
    const updateResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/job/definitions') &&
      response.request().method() === 'PUT' &&
      response.status() === 200
    );
    await editDialog.getByRole('button', { name: '保存' }).click();
    await updateResponsePromise;
    await expect(page.locator('.el-message__content', { hasText: '任务已更新' }).last()).toBeVisible({ timeout: 10000 });
    await expect(definitionRow(page, draftCode)).toContainText('E2E 验收草稿任务已编辑');

    const manualCode = exampleJobs[0].jobCode;
    await searchDefinition(page, manualCode);
    const manualRow = definitionRow(page, manualCode);
    await expect(manualRow).toContainText('已启用');
    await manualRow.getByRole('button', { name: '暂停' }).click();
    await page.locator('.el-message-box', { hasText: '调整状态' }).getByRole('button', { name: /^(OK|确认)$/ }).click();
    await expect(page.locator('.el-message__content', { hasText: '状态已更新' }).last()).toBeVisible({ timeout: 10000 });
    await expect(definitionRow(page, manualCode)).toContainText('已暂停');

    await definitionRow(page, manualCode).getByRole('button', { name: '启用' }).click();
    await page.locator('.el-message-box', { hasText: '调整状态' }).getByRole('button', { name: /^(OK|确认)$/ }).click();
    await expect(page.locator('.el-message__content', { hasText: '状态已更新' }).last()).toBeVisible({ timeout: 10000 });
    await expect(definitionRow(page, manualCode)).toContainText('已启用');
    await saveEvidenceScreenshot(page, '05-definition-status-enabled.png');

    const manualDefinition = (await listDefinitions(page, headers, manualCode)).find(item => item.jobCode === manualCode);
    const syncedManualDefinition = expectDefined(manualDefinition, '手动示例任务必须存在');
    expect(syncedManualDefinition.syncStatus).toBe('SYNCED');
    expect(syncedManualDefinition.engineJobId).toBeTruthy();
    const triggerBatchNo = `e2e-job-batch-${Date.now()}`;
    const triggerResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/job/definitions/trigger') &&
      response.request().method() === 'POST' &&
      response.status() === 200
    );
    await definitionRow(page, manualCode).getByRole('button', { name: '触发' }).click();
    const triggerDialog = page.getByRole('dialog', { name: '手动触发' });
    await expect(triggerDialog).toBeVisible();
    await triggerDialog.getByLabel('批次号').fill(triggerBatchNo);
    const triggerParamEditor = paramEditor(triggerDialog);
    await expect(triggerParamEditor.getByText('按参数 Schema 生成表单')).toBeVisible();
    await fillParamTextField(triggerParamEditor, '来源', 'e2e-trigger');
    await fillParamTextField(triggerParamEditor, '断言标记', 'mango-native-runtime');
    await saveEvidenceScreenshot(page, '06-trigger-dialog-frequency-manual.png', triggerDialog);
    await triggerDialog.getByRole('button', { name: '触发' }).click();
    await triggerResponsePromise;
    await expect(page.locator('.el-message__content', { hasText: '已触发' }).last()).toBeVisible({ timeout: 10000 });

    const triggeredInstance = await waitForTriggeredInstance(page, headers, syncedManualDefinition.id, triggerBatchNo);

    const apiLogDetail = await waitForExecutionLogDetail(page, headers, triggeredInstance.id);
    expect(apiLogDetail.logSource).toBe('MANGO_NATIVE');
    expect(apiLogDetail.nativeLogContent || '').toContain('Mango Job runtime probe executed');
    expect(apiLogDetail.nativeLogContent || '').toContain('Mango Job runtime probe System.out');
    expect(apiLogDetail.nativeLogContent || '').toContain('Mango Job runtime probe logger');
    expect(apiLogDetail.nativeLogContent || '').toContain('mango-native-runtime');
    expect(apiLogDetail.engineResult || '').toContain('Mango Job runtime probe executed');

    const everyMinuteCode = exampleJobs[3].jobCode;
    const everyMinuteDefinition = (await listDefinitions(page, headers, everyMinuteCode))
      .find(item => item.jobCode === everyMinuteCode);
    const syncedEveryMinuteDefinition = expectDefined(everyMinuteDefinition, '每分钟示例任务必须存在');
    expect(syncedEveryMinuteDefinition.scheduleType).toBe('CRON');
    expect(syncedEveryMinuteDefinition.scheduleExpression).toBe('0 */1 * * * ?');
    const scheduledInstances = await waitForScheduledInstance(page, headers, syncedEveryMinuteDefinition.id);
    const scheduledInstance = scheduledInstances[0];

    const instanceResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/job/instances/page') && response.status() === 200
    );
    await clickMenu(page, '执行实例');
    await instanceResponsePromise;
    await selectInstanceTaskFilter(page, manualCode);
    await page.getByPlaceholder('triggerBatchNo').fill(triggerBatchNo);
    const filteredInstanceResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/job/instances/page') && response.status() === 200
    );
    await page.getByRole('button', { name: '查询' }).click();
    await filteredInstanceResponsePromise;
    const instanceRow = page.locator('.el-table__row', { hasText: triggerBatchNo }).first();
    await expect(instanceRow).toBeVisible();
    await expect(instanceRow).toContainText('示例 手动内置任务');
    await expect(instanceRow).toContainText(manualCode);
    await expect(instanceRow).toContainText('embedded://');
    await expect(instanceRow).toContainText('成功');
    await saveEvidenceScreenshot(page, '07-instance-filtered-trigger-batch.png');
    await saveEvidenceScreenshot(page, '08-execution-instance-log-entry.png', instanceRow);

    const detailResponsePromise = page.waitForResponse((response) =>
      response.url().includes(`/api/job/instances/${triggeredInstance.id}/logs`) && response.status() === 200
    );
    await instanceRow.getByRole('button', { name: '日志' }).click();
    const detailResponse = await detailResponsePromise;
    const logDetail = await expectBusinessOk<JobLogDetail>(detailResponse);
    expect(logDetail.logSource).toBe('MANGO_NATIVE');
    expect(logDetail.nativeLogAvailable).toBe(true);
    expect(logDetail.nativeLogContent || '').toContain('Mango Job runtime probe executed');
    expect(logDetail.nativeLogContent || '').toContain('Mango Job runtime probe System.out');
    expect(logDetail.nativeLogContent || '').toContain('Mango Job runtime probe logger');
    expect(logDetail.nativeLogContent || '').toContain('mango-native-runtime');
    expect(logDetail.engineResult || '').toContain('Mango Job runtime probe executed');
    const logDetailDrawer = page.getByRole('dialog', { name: '执行日志详情' });
    await expect(logDetailDrawer).toBeVisible();
    await expect(logDetailDrawer).toContainText('示例 手动内置任务');
    await expect(logDetailDrawer).toContainText(manualCode);
    await expectVisibleExecutionLog(logDetailDrawer, [
      'Mango Job runtime probe executed',
      'Mango Job runtime probe System.out',
      'Mango Job runtime probe logger',
      'handlerResult=Mango Job runtime probe executed',
      'mango-native-runtime',
    ]);
    await saveEvidenceScreenshot(page, '08b-execution-log-detail.png', logDetailDrawer);
    await page.keyboard.press('Escape');

    await selectInstanceTaskFilter(page, everyMinuteCode);
    await page.getByPlaceholder('triggerBatchNo').clear();
    const scheduledResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/job/instances/page') && response.status() === 200
    );
    await page.getByRole('button', { name: '查询' }).click();
    await scheduledResponsePromise;
    const scheduledRow = page.locator('.el-table__row', { hasText: everyMinuteCode }).first();
    await expect(scheduledRow).toBeVisible();
    await expect(scheduledRow).toContainText('示例 每分钟执行一次任务');
    await expect(scheduledRow).toContainText(everyMinuteCode);
    await expect(scheduledRow).toContainText('调度');
    await expect(scheduledRow).toContainText('成功');
    await saveEvidenceScreenshot(page, '08c-scheduled-every-minute-instance.png');

    const scheduledDetailResponsePromise = page.waitForResponse((response) =>
      response.url().includes(`/api/job/instances/${scheduledInstance.id}/logs`) && response.status() === 200
    );
    await scheduledRow.getByRole('button', { name: '日志' }).click();
    const scheduledDetailResponse = await scheduledDetailResponsePromise;
    const scheduledLogDetail = await expectBusinessOk<JobLogDetail>(scheduledDetailResponse);
    expect(scheduledLogDetail.logSource).toBe('MANGO_NATIVE');
    expect(scheduledLogDetail.nativeLogAvailable).toBe(true);
    expect(scheduledLogDetail.nativeLogContent || '').toContain('Mango Job runtime probe executed');
    expect(scheduledLogDetail.nativeLogContent || '').toContain('Mango Job runtime probe System.out');
    expect(scheduledLogDetail.nativeLogContent || '').toContain('Mango Job runtime probe logger');
    const scheduledLogDrawer = page.getByRole('dialog', { name: '执行日志详情' });
    await expect(scheduledLogDrawer).toContainText('示例 每分钟执行一次任务');
    await expect(scheduledLogDrawer).toContainText(everyMinuteCode);
    await expectVisibleExecutionLog(scheduledLogDrawer, [
      'Mango Job runtime probe executed',
      'Mango Job runtime probe System.out',
      'Mango Job runtime probe logger',
      'handlerResult=Mango Job runtime probe executed',
    ]);
    await saveEvidenceScreenshot(page, '08d-scheduled-every-minute-log-detail.png', scheduledLogDrawer);
    await page.keyboard.press('Escape');

    const workers = await listWorkers(page, headers);
    expect(
      workers.some(item =>
        item.appCode === 'mango-job'
        && item.engineType === 'MANGO_NATIVE'
        && item.status === 'ONLINE'
        && Boolean(item.workerAddress?.includes('embedded://'))
      ),
      'Worker 快照必须来自真实 Mango Native Worker 注册',
    ).toBeTruthy();

    await openJobDefinitionPage(page);

    await searchDefinition(page, draftCode);
    await definitionRow(page, draftCode).getByRole('button', { name: '删除' }).click();
    await expect(page.locator('.el-message-box', { hasText: '删除任务' })).toBeVisible();
    await saveEvidenceScreenshot(page, '09-definition-delete-confirm.png', page.locator('.el-message-box', { hasText: '删除任务' }));
    await page.locator('.el-message-box', { hasText: '删除任务' }).getByRole('button', { name: /^(OK|确认)$/ }).click();
    await expect(page.locator('.el-message__content', { hasText: '任务已删除' }).last()).toBeVisible({ timeout: 10000 });
    await expect(definitionRow(page, draftCode)).toHaveCount(0);

    await searchDefinition(page, exampleKeyword);
    for (const payload of exampleJobs) {
      await expect(definitionRow(page, payload.jobCode)).toBeVisible();
    }

    await testInfo.attach('job-definition-layout', {
      body: await page.screenshot({ fullPage: true }),
      contentType: 'image/png',
    });

    expect(consoleErrors).toEqual([]);
    expect(failedRequests).toEqual([]);
  });

  test('任务管理各列表页均使用真实 Job API 并保持可用布局', async ({ page }, testInfo) => {
    const consoleErrors: string[] = [];
    const failedRequests: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        consoleErrors.push(message.text());
      }
    });
    page.on('response', (response) => {
      const url = response.url();
      if (url.includes('/api/job/') && response.status() >= 400) {
        failedRequests.push(`${response.status()} ${url}`);
      }
    });

    await login(page);
    const headers = await apiHeaders(page);
    await openJobDefinitionPage(page);
    await expect(page.locator(
      '.layout-columns-aside .el-menu-item, .layout-columns-aside .el-sub-menu__title',
      { hasText: /^\s*执行日志\s*$/ },
    )).toHaveCount(0);

    const pages = [
      { menu: '执行实例', path: '/job/instance', api: '/api/job/instances/page', heading: '执行实例', search: true },
      { menu: 'Worker 节点', path: '/job/worker', api: '/api/job/workers/page', heading: 'Worker 节点', search: true },
      { menu: '运行状态', path: '/job/engine', api: '/api/job/engines/status', heading: '运行状态', search: false },
    ];

    for (const item of pages) {
      const responsePromise = page.waitForResponse((response) =>
        response.url().includes(item.api) && response.status() === 200
      );
      await clickMenu(page, item.menu);
      await responsePromise;
      await expect(page).toHaveURL(new RegExp(`#${item.path}$`));
      await expect(page.getByRole('heading', { name: item.heading })).toBeVisible();
      await expect(page.locator('.job-panel')).toBeVisible();
      if (item.search) {
        await expect(page.locator('.job-search')).toBeVisible();
        await expectCompactSearchPage(page, page.locator('.job-toolbar'));
      }
      if (item.path === '/job/worker') {
        const workers = await listWorkers(page, headers);
        expect(workers.some(worker => Boolean(worker.workerAddress?.includes('embedded://')))).toBeTruthy();
        await expect(page.locator('.el-table')).toContainText('embedded://');
        await expect(page.locator('.el-table')).toContainText('在线');
      }
      await expect(page.locator('.el-message--error')).toHaveCount(0);
      await expect(page.locator('.job-error')).toHaveCount(0);
      await expect(page.locator('.error-container')).toHaveCount(0);
      await saveEvidenceScreenshot(page, `10-${item.path.replace('/job/', 'job-')}.png`);
    }

    const alarmKeyword = `E2E 失败告警 ${projectRunKey(testInfo.project.name)}`;
    await deleteAlarmRulesByKeyword(page, headers, alarmKeyword);
    const alarmRuleName = `${alarmKeyword} ${Date.now()}`;
    const definitions = await listDefinitions(page, headers, exampleJobKeyword(projectRunKey(testInfo.project.name)));
    const alarmTarget = definitions.find(item => item.jobCode.includes('manual_builtin'));
    const resolvedAlarmTarget = expectDefined(alarmTarget, '告警规则必须能选择手动示例任务');

    await openAlarmPage(page);
    await expect(page.locator('.job-search')).toBeVisible();
    await expectCompactSearchPage(page, page.locator('.job-toolbar'));
    await page.getByRole('button', { name: '新增规则' }).click();
    const alarmDialog = page.getByRole('dialog', { name: '新增告警规则' });
    await expect(alarmDialog).toBeVisible();
    await alarmDialog.getByLabel('所属应用').fill('mango-job');
    const alarmTaskField = alarmDialog.locator('.el-form-item', { hasText: '作用任务' }).first();
    await alarmTaskField.locator('.el-select').click();
    await alarmTaskField.getByRole('combobox', { name: '作用任务' }).fill(resolvedAlarmTarget.jobCode);
    await page.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: resolvedAlarmTarget.jobCode }).first().click();
    await alarmDialog.getByLabel('规则名称').fill(alarmRuleName);
    await expect(alarmDialog.getByLabel('通知场景')).toHaveValue('job.instance.failed');
    await alarmDialog.getByLabel('消息模板').fill('MANGO_JOB_FAILED_TEMPLATE_E2E');
    await alarmDialog.getByLabel('收件规则').fill('jobDutyE2E');
    await alarmDialog.getByLabel('单个用户').fill('1');
    await alarmDialog.getByLabel('多个用户').fill('1\n2');
    await saveEvidenceScreenshot(page, '15-alarm-rule-create-dialog.png', alarmDialog);
    const createAlarmResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/job/alarm-rules') &&
      response.request().method() === 'POST' &&
      response.status() === 200
    );
    await alarmDialog.getByRole('button', { name: '保存' }).click();
    await createAlarmResponsePromise;
    await expect(page.locator('.el-message__content', { hasText: '告警规则已创建' }).last()).toBeVisible({ timeout: 10000 });

    await searchAlarmRule(page, alarmRuleName);
    const createdAlarmRow = alarmRuleRow(page, alarmRuleName);
    await expect(createdAlarmRow).toBeVisible();
    await expect(createdAlarmRow).toContainText('job.instance.failed');
    await expect(createdAlarmRow).toContainText('MANGO_JOB_FAILED_TEMPLATE_E2E');
    await expect(createdAlarmRow).toContainText(resolvedAlarmTarget.jobCode);
    await expect(createdAlarmRow).toContainText('启用');
    await saveEvidenceScreenshot(page, '16-alarm-rule-created.png');
    let alarmRules = await listAlarmRules(page, headers, alarmRuleName);
    let createdAlarmRule = alarmRules.find(item => item.ruleName === alarmRuleName);
    const resolvedCreatedAlarmRule = expectDefined(createdAlarmRule, '新建告警规则必须可通过 API 查询');
    expect(resolvedCreatedAlarmRule.jobId).toBe(resolvedAlarmTarget.id);
    expect(resolvedCreatedAlarmRule.enabled).toBe(true);
    expect(resolvedCreatedAlarmRule.noticeParams || '').toContain('jobDutyE2E');

    const editAlarmResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/job/alarm-rules/detail') && response.status() === 200
    );
    await createdAlarmRow.getByRole('button', { name: '编辑' }).click();
    await editAlarmResponsePromise;
    const editAlarmDialog = page.getByRole('dialog', { name: '编辑告警规则' });
    await expect(editAlarmDialog).toBeVisible();
    await editAlarmDialog.getByLabel('消息模板').fill('MANGO_JOB_FAILED_TEMPLATE_E2E_V2');
    await editAlarmDialog.getByLabel('收件规则').fill('jobDutyE2EUpdated');
    await saveEvidenceScreenshot(page, '17-alarm-rule-edit-dialog.png', editAlarmDialog);
    const updateAlarmResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/job/alarm-rules') &&
      response.request().method() === 'PUT' &&
      response.status() === 200
    );
    await editAlarmDialog.getByRole('button', { name: '保存' }).click();
    await updateAlarmResponsePromise;
    await expect(page.locator('.el-message__content', { hasText: '告警规则已更新' }).last()).toBeVisible({ timeout: 10000 });
    await searchAlarmRule(page, alarmRuleName);
    await expect(alarmRuleRow(page, alarmRuleName)).toContainText('MANGO_JOB_FAILED_TEMPLATE_E2E_V2');
    alarmRules = await listAlarmRules(page, headers, alarmRuleName);
    createdAlarmRule = alarmRules.find(item => item.ruleName === alarmRuleName);
    expect(createdAlarmRule?.noticeParams || '').toContain('jobDutyE2EUpdated');

    const disableAlarmResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/job/alarm-rules/status') &&
      response.request().method() === 'PUT' &&
      response.status() === 200
    );
    await alarmRuleRow(page, alarmRuleName).getByRole('button', { name: '停用' }).click();
    await page.locator('.el-message-box', { hasText: '更新告警规则状态' }).getByRole('button', { name: /^(OK|确认)$/ }).click();
    await disableAlarmResponsePromise;
    await expect(page.locator('.el-message__content', { hasText: '告警规则已停用' }).last()).toBeVisible({ timeout: 10000 });
    await searchAlarmRule(page, alarmRuleName);
    await expect(alarmRuleRow(page, alarmRuleName)).toContainText('停用');
    await saveEvidenceScreenshot(page, '18-alarm-rule-disabled.png');
    alarmRules = await listAlarmRules(page, headers, alarmRuleName);
    expect(alarmRules.find(item => item.ruleName === alarmRuleName)?.enabled).toBe(false);

    const enableAlarmResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/job/alarm-rules/status') &&
      response.request().method() === 'PUT' &&
      response.status() === 200
    );
    await alarmRuleRow(page, alarmRuleName).getByRole('button', { name: '启用' }).click();
    await page.locator('.el-message-box', { hasText: '更新告警规则状态' }).getByRole('button', { name: /^(OK|确认)$/ }).click();
    await enableAlarmResponsePromise;
    await expect(page.locator('.el-message__content', { hasText: '告警规则已启用' }).last()).toBeVisible({ timeout: 10000 });
    await searchAlarmRule(page, alarmRuleName);
    await expect(alarmRuleRow(page, alarmRuleName)).toContainText('启用');
    await saveEvidenceScreenshot(page, '19-alarm-rule-enabled.png');

    const deleteAlarmResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/job/alarm-rules') &&
      response.request().method() === 'DELETE' &&
      response.status() === 200
    );
    await alarmRuleRow(page, alarmRuleName).getByRole('button', { name: '删除' }).click();
    await page.locator('.el-message-box', { hasText: '删除告警规则' }).getByRole('button', { name: /^(OK|确认)$/ }).click();
    await deleteAlarmResponsePromise;
    await expect(page.locator('.el-message__content', { hasText: '告警规则已删除' }).last()).toBeVisible({ timeout: 10000 });
    await searchAlarmRule(page, alarmRuleName);
    await expect(alarmRuleRow(page, alarmRuleName)).toHaveCount(0);
    await saveEvidenceScreenshot(page, '20-alarm-rule-deleted.png');

    await testInfo.attach('job-runtime-pages', {
      body: await page.screenshot({ fullPage: true }),
      contentType: 'image/png',
    });

    expect(consoleErrors).toEqual([]);
    expect(failedRequests).toEqual([]);
  });

  test('Worker 节点支持手动登记、禁用和恢复治理', async ({ page }, testInfo) => {
    const consoleErrors: string[] = [];
    const failedRequests: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        consoleErrors.push(message.text());
      }
    });
    page.on('response', (response) => {
      const url = response.url();
      if (url.includes('/api/job/') && response.status() >= 400) {
        failedRequests.push(`${response.status()} ${url}`);
      }
    });

    await login(page);
    const headers = await apiHeaders(page);
    const workerKey = `e2e-worker-${projectRunKey(testInfo.project.name)}-${Date.now()}`;
    const workerAddress = `http://127.0.0.1:18658/${workerKey}`;
    await resetWorkersByKeyword(page, headers, workerKey);

    await openJobDefinitionPage(page);
    await openWorkerPage(page);
    await expect(page.locator('.job-search')).toBeVisible();
    await expectCompactSearchPage(page, page.locator('.job-toolbar'));

    await page.getByRole('button', { name: '登记 Worker' }).click();
    const dialog = page.getByRole('dialog', { name: '登记 Worker' });
    await expect(dialog).toBeVisible();
    await dialog.getByLabel('所属应用').fill('mango-job');
    await dialog.getByLabel('Worker 地址').fill(workerAddress);
    await dialog.getByLabel('实例标识').fill(workerKey);
    await dialog.getByLabel('处理器').fill(runtimeProbeHandler);
    await dialog.getByLabel('参数 Schema').fill('{ "type": "object" }');
    await saveEvidenceScreenshot(page, '11-worker-create-dialog.png', dialog);

    const createResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/job/workers') &&
      response.request().method() === 'POST'
    );
    await dialog.getByRole('button', { name: '保存' }).click();
    await expectBusinessOk<ApiId>(await createResponsePromise);
    await expect(page.locator('.el-message__content', { hasText: 'Worker 已登记' }).last()).toBeVisible({ timeout: 10000 });

    await searchWorker(page, workerKey);
    const createdRow = workerRow(page, workerKey);
    await expect(createdRow).toBeVisible();
    await expect(createdRow).toContainText(workerAddress);
    await expect(createdRow).toContainText('MANGO_NATIVE');
    await expect(createdRow).toContainText('在线');
    await saveEvidenceScreenshot(page, '12-worker-created-online.png');

    let workers = await listWorkers(page, headers, workerKey);
    const createdWorker = workers.find(item => item.workerAddress === workerAddress);
    const resolvedCreatedWorker = expectDefined(createdWorker, '手动登记 Worker 必须可通过 API 查询');
    expect(resolvedCreatedWorker.status).toBe('ONLINE');

    const disableResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/job/workers/status') &&
      response.request().method() === 'PUT' &&
      response.status() === 200
    );
    await createdRow.getByRole('button', { name: '禁用' }).click();
    await page.locator('.el-message-box', { hasText: '调整 Worker 状态' }).getByRole('button', { name: /^(OK|确认)$/ }).click();
    await disableResponsePromise;
    await expect(page.locator('.el-message__content', { hasText: 'Worker 状态已更新' }).last()).toBeVisible({ timeout: 10000 });
    await searchWorker(page, workerKey);
    await expect(workerRow(page, workerKey)).toContainText('已禁用');
    await saveEvidenceScreenshot(page, '13-worker-disabled.png');

    workers = await listWorkers(page, headers, workerKey);
    expect(workers.find(item => item.workerAddress === workerAddress)?.status).toBe('DISABLED');

    const restoreResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/job/workers/status') &&
      response.request().method() === 'PUT' &&
      response.status() === 200
    );
    await workerRow(page, workerKey).getByRole('button', { name: '恢复' }).click();
    await page.locator('.el-message-box', { hasText: '调整 Worker 状态' }).getByRole('button', { name: /^(OK|确认)$/ }).click();
    await restoreResponsePromise;
    await expect(page.locator('.el-message__content', { hasText: 'Worker 状态已更新' }).last()).toBeVisible({ timeout: 10000 });
    await searchWorker(page, workerKey);
    await expect(workerRow(page, workerKey)).toContainText('在线');
    await saveEvidenceScreenshot(page, '14-worker-restored-online.png');

    workers = await listWorkers(page, headers, workerKey);
    expect(workers.find(item => item.workerAddress === workerAddress)?.status).toBe('ONLINE');

    await testInfo.attach('job-worker-governance', {
      body: await page.screenshot({ fullPage: true }),
      contentType: 'image/png',
    });

    expect(consoleErrors).toEqual([]);
    expect(failedRequests).toEqual([]);
  });
});
