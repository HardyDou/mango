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
  errorSummary?: string;
}

interface JobLogIndex {
  id: ApiId;
  jobId: ApiId;
  instanceId: ApiId;
  engineType?: string;
  engineInstanceId?: string;
  logLocation?: string;
}

interface JobLogDetail extends JobLogIndex {
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

const exampleJobs: SaveJobDefinitionPayload[] = [
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
    engineType: 'POWERJOB',
  },
  {
    appCode: 'mango-job',
    jobCode: 'mango_job_example_cron_powerjob',
    jobName: '示例 Cron PowerJob 任务',
    jobType: 'BUILTIN',
    scheduleType: 'CRON',
    scheduleExpression: '0 */5 * * * ?',
    handlerName: runtimeProbeHandler,
    paramValue: '{ "source": "acceptance", "kind": "cron" }',
    concurrencyPolicy: 'SERIAL',
    misfireStrategy: 'FIRE_ONCE',
    timeoutSeconds: 600,
    retryPolicy: '{ "maxRetryTimes": 2 }',
    engineType: 'POWERJOB',
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
    engineType: 'POWERJOB',
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
    engineType: 'POWERJOB',
  },
];

const evidenceDir = resolve(__dirname, '../../../../../mango-docs/evidence/2026-06-05-mango-job-ui-e2e');

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

async function syncInstances(page: Page, headers: LoginHeaders, jobId: ApiId) {
  await expectBusinessOk<boolean>(await page.request.post('/api/job/instances/sync', {
    headers,
    data: { jobId, size: 50 },
  }));
}

async function listLogs(page: Page, headers: LoginHeaders, jobId: ApiId): Promise<JobLogIndex[]> {
  const response = await page.request.get('/api/job/logs/page', {
    headers,
    params: { jobId: String(jobId), pageNum: '1', pageSize: '20' },
  });
  const data = await expectBusinessOk<PageData<JobLogIndex>>(response);
  return data.list || data.records || data.rows || data.data || [];
}

async function detailLog(page: Page, headers: LoginHeaders, logId: ApiId): Promise<JobLogDetail> {
  const response = await page.request.get('/api/job/logs/detail', {
    headers,
    params: { id: String(logId) },
  });
  return expectBusinessOk<JobLogDetail>(response);
}

async function listWorkers(page: Page, headers: LoginHeaders): Promise<JobWorkerSnapshot[]> {
  const response = await page.request.get('/api/job/workers/page', {
    headers,
    params: { appCode: 'mango-job', engineType: 'POWERJOB', pageNum: '1', pageSize: '20' },
  });
  const data = await expectBusinessOk<PageData<JobWorkerSnapshot>>(response);
  return data.list || data.records || data.rows || data.data || [];
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

async function prepareExampleJobs(page: Page, headers: LoginHeaders) {
  await deleteDefinitionsByKeyword(page, headers, 'mango_job_example_');
  for (const payload of exampleJobs) {
    const id = await createDefinition(page, headers, payload);
    await updateDefinitionStatus(page, headers, id, 'ENABLED');
    const synced = await waitForSyncedDefinition(page, headers, id);
    expect(synced.engineType).toBe('POWERJOB');
    expect(synced.syncStatus, synced.syncError || '任务定义必须同步到 PowerJob').toBe('SYNCED');
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
    message: '任务定义必须获得真实 PowerJob engineJobId',
  }).toBeTruthy();
  return latest!;
}

async function waitForTriggeredInstance(
  page: Page,
  headers: LoginHeaders,
  jobId: ApiId,
  triggerBatchNo: string,
): Promise<JobInstance> {
  let latest: JobInstance | undefined;
  await expect.poll(async () => {
    await syncInstances(page, headers, jobId);
    const instances = await listInstances(page, headers, jobId);
    latest = instances.find(item => item.triggerBatchNo === triggerBatchNo && item.triggerType === 'MANUAL');
    return Boolean(latest?.engineInstanceId) && latest?.status !== 'WAITING' && latest?.status !== 'RUNNING';
  }, {
    timeout: 60_000,
    intervals: [1000, 2000, 3000],
    message: '手动触发必须获得真实 PowerJob engineInstanceId 并完成执行',
  }).toBeTruthy();
  expect(latest!.engineType).toBe('POWERJOB');
  expect(latest!.engineInstanceId).toBeTruthy();
  expect(latest!.status, latest!.errorSummary || '示例任务应执行成功').toBe('SUCCESS');
  return latest!;
}

async function waitForScheduledInstance(
  page: Page,
  headers: LoginHeaders,
  jobId: ApiId,
): Promise<JobInstance[]> {
  let latest: JobInstance[] = [];
  await expect.poll(async () => {
    await syncInstances(page, headers, jobId);
    const instances = await listInstances(page, headers, jobId);
    latest = instances.filter(item =>
      item.triggerType === 'SCHEDULED'
      && Boolean(item.engineInstanceId)
      && item.status !== 'WAITING'
      && item.status !== 'RUNNING'
    );
    return new Set(latest.map(item => item.engineInstanceId)).size >= 2;
  }, {
    timeout: 180_000,
    intervals: [5000, 10_000, 15_000],
    message: '每分钟示例任务必须产生至少两次可见的 PowerJob 调度执行实例',
  }).toBeTruthy();
  expect(latest[0].engineType).toBe('POWERJOB');
  expect(latest[0].engineInstanceId).toBeTruthy();
  expect(latest[0].status, latest[0].errorSummary || '每分钟示例任务应执行成功').toBe('SUCCESS');
  return latest;
}

async function waitForExecutionLogDetail(
  page: Page,
  headers: LoginHeaders,
  logId: ApiId,
): Promise<JobLogDetail> {
  let latest: JobLogDetail | undefined;
  const startedAt = Date.now();
  const timeoutMs = 120_000;
  while (Date.now() - startedAt < timeoutMs) {
    latest = await detailLog(page, headers, logId);
    const content = latest.nativeLogContent || '';
    if (latest.logSource === 'POWERJOB_NATIVE_LOG'
      && latest.nativeLogAvailable === true
      && content.includes('Mango Job handler output')
      && content.includes('Mango Job runtime probe System.out')
      && content.includes('Mango Job runtime probe logger')) {
      return latest;
    }
    await page.waitForTimeout(3_000);
  }

  throw new Error([
    '执行日志必须归档并可通过 Mango 详情接口读取',
    `logId=${logId}`,
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
  await dialog.getByLabel('任务名称').fill('E2E 临时任务');
  await selectFormOption(page, dialog, '底层引擎', 'PowerJob');
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
    await prepareExampleJobs(page, headers);
    await deleteDefinitionsByKeyword(page, headers, 'mango_job_e2e_tmp_');

    await openJobDefinitionPage(page);
    await expect(page.locator('.job-search')).toBeVisible();
    await expectCompactJobToolbar(page);
    const toolbarBox = await page.locator('.job-toolbar').boundingBox();
    const panelBox = await page.locator('.job-panel').boundingBox();
    expect(toolbarBox?.height || 0).toBeLessThan(170);
    expect(panelBox?.y || 0).toBeLessThan(260);

    await searchDefinition(page, 'mango_job_example_');
    for (const payload of exampleJobs) {
      await expect(definitionRow(page, payload.jobCode)).toBeVisible({ timeout: 10000 });
    }
    await saveEvidenceScreenshot(page, '01-definition-list-frequency.png');

    await page.getByRole('button', { name: /更多筛选/ }).click();
    await expect(page.locator('.job-search-more')).toBeVisible();
    await saveEvidenceScreenshot(page, '02-definition-more-filters.png');

    const cronDialog = await openDefinitionEditor(page, 'mango_job_example_cron_powerjob');
    await expect(cronDialog.getByLabel('调度表达式')).toHaveValue('0 */5 * * * ?');
    await saveEvidenceScreenshot(page, '03-definition-edit-cron-schedule.png', cronDialog);
    await cronDialog.getByRole('button', { name: '取消' }).click();

    const fixedRateDialog = await openDefinitionEditor(page, 'mango_job_example_fixed_rate_probe');
    await expect(fixedRateDialog.getByLabel('调度表达式')).toHaveValue('60000');
    await saveEvidenceScreenshot(page, '04-definition-edit-fixed-rate-schedule.png', fixedRateDialog);
    await fixedRateDialog.getByRole('button', { name: '取消' }).click();

    const tempCode = `mango_job_e2e_tmp_${Date.now()}`;
    await createDraftByUi(page, tempCode);
    await searchDefinition(page, tempCode);
    const tempRow = definitionRow(page, tempCode);
    await expect(tempRow).toContainText('E2E 临时任务');
    await expect(tempRow).toContainText('草稿');

    await tempRow.getByRole('button', { name: '编辑' }).click();
    const editDialog = page.getByRole('dialog', { name: '编辑任务' });
    await expect(editDialog).toBeVisible();
    await editDialog.getByLabel('任务名称').fill('E2E 临时任务已编辑');
    const updateResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/job/definitions') &&
      response.request().method() === 'PUT' &&
      response.status() === 200
    );
    await editDialog.getByRole('button', { name: '保存' }).click();
    await updateResponsePromise;
    await expect(page.locator('.el-message__content', { hasText: '任务已更新' }).last()).toBeVisible({ timeout: 10000 });
    await expect(definitionRow(page, tempCode)).toContainText('E2E 临时任务已编辑');

    const manualCode = 'mango_job_example_manual_builtin';
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
    expect(manualDefinition).toBeDefined();
    expect(manualDefinition!.syncStatus).toBe('SYNCED');
    expect(manualDefinition!.engineJobId).toBeTruthy();
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
    await fillParamTextField(triggerParamEditor, '断言标记', 'powerjob-runtime');
    await saveEvidenceScreenshot(page, '06-trigger-dialog-frequency-manual.png', triggerDialog);
    await triggerDialog.getByRole('button', { name: '触发' }).click();
    await triggerResponsePromise;
    await expect(page.locator('.el-message__content', { hasText: '已触发' }).last()).toBeVisible({ timeout: 10000 });

    const triggeredInstance = await waitForTriggeredInstance(page, headers, manualDefinition!.id, triggerBatchNo);

    const logs = await listLogs(page, headers, manualDefinition!.id);
    const executionLog = logs.find(item =>
      String(item.jobId) === String(manualDefinition!.id)
      && String(item.instanceId) === String(triggeredInstance!.id)
      && item.engineType === 'POWERJOB'
      && item.engineInstanceId
    );
    expect(executionLog).toBeDefined();
    const apiLogDetail = await waitForExecutionLogDetail(page, headers, executionLog!.id);
    expect(apiLogDetail.nativeLogContent || '').toContain('Mango Job runtime probe executed');
    expect(apiLogDetail.nativeLogContent || '').toContain('Mango Job runtime probe System.out');
    expect(apiLogDetail.nativeLogContent || '').toContain('Mango Job runtime probe logger');
    expect(apiLogDetail.nativeLogContent || '').toContain('powerjob-runtime');
    expect(apiLogDetail.engineResult || '').toContain('Mango Job runtime probe executed');

    const everyMinuteCode = 'mango_job_example_every_minute_cron_probe';
    const everyMinuteDefinition = (await listDefinitions(page, headers, everyMinuteCode))
      .find(item => item.jobCode === everyMinuteCode);
    expect(everyMinuteDefinition).toBeDefined();
    expect(everyMinuteDefinition!.scheduleType).toBe('CRON');
    expect(everyMinuteDefinition!.scheduleExpression).toBe('0 */1 * * * ?');
    const scheduledInstances = await waitForScheduledInstance(page, headers, everyMinuteDefinition!.id);
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
    await expect(instanceRow).toContainText(String(triggeredInstance.engineInstanceId));
    await expect(instanceRow).toContainText('成功');
    await saveEvidenceScreenshot(page, '07-instance-filtered-trigger-batch.png');
    await saveEvidenceScreenshot(page, '08-execution-log-index.png', instanceRow);

    const instanceLogResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/job/logs/page') && response.status() === 200
    );
    const detailResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/job/logs/detail') && response.status() === 200
    );
    await instanceRow.getByRole('button', { name: '日志' }).click();
    await instanceLogResponsePromise;
    const detailResponse = await detailResponsePromise;
    const logDetail = await expectBusinessOk<JobLogDetail>(detailResponse);
    expect(logDetail.logSource).toBe('POWERJOB_NATIVE_LOG');
    expect(logDetail.nativeLogAvailable).toBe(true);
    expect(logDetail.nativeLogContent || '').toContain('Mango Job handler output');
    expect(logDetail.nativeLogContent || '').toContain('Mango Job runtime probe executed');
    expect(logDetail.nativeLogContent || '').toContain('Mango Job runtime probe System.out');
    expect(logDetail.nativeLogContent || '').toContain('Mango Job runtime probe logger');
    expect(logDetail.nativeLogContent || '').toContain('powerjob-runtime');
    expect(logDetail.engineResult || '').toContain('Mango Job runtime probe executed');
    const logDetailDrawer = page.getByRole('dialog', { name: '执行日志详情' });
    await expect(logDetailDrawer).toBeVisible();
    await expect(logDetailDrawer).toContainText('示例 手动内置任务');
    await expect(logDetailDrawer).toContainText(manualCode);
    await expectVisibleExecutionLog(logDetailDrawer, [
      'Mango Job handler output',
      'Mango Job runtime probe executed',
      'Mango Job runtime probe System.out',
      'Mango Job runtime probe logger',
      'powerjob-runtime',
    ]);
    await saveEvidenceScreenshot(page, '08b-execution-log-detail.png', logDetailDrawer);
    await page.keyboard.press('Escape');

    await selectInstanceTaskFilter(page, everyMinuteCode);
    await page.getByPlaceholder('triggerBatchNo').clear();
    await syncInstances(page, headers, everyMinuteDefinition!.id);
    const scheduledResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/job/instances/page') && response.status() === 200
    );
    await page.getByRole('button', { name: '查询' }).click();
    await scheduledResponsePromise;
    const scheduledRow = page.locator('.el-table__row', { hasText: String(scheduledInstance.engineInstanceId) }).first();
    await expect(scheduledRow).toBeVisible();
    await expect(scheduledRow).toContainText('示例 每分钟执行一次任务');
    await expect(scheduledRow).toContainText(everyMinuteCode);
    await expect(scheduledRow).toContainText('调度');
    await expect(scheduledRow).toContainText('成功');
    await saveEvidenceScreenshot(page, '08c-scheduled-every-minute-instance.png');

    const scheduledLogResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/job/logs/page') && response.status() === 200
    );
    await scheduledRow.getByRole('button', { name: '日志' }).click();
    const scheduledLogResponse = await scheduledLogResponsePromise;
    const scheduledLogPage = await expectBusinessOk<PageData<JobLogIndex>>(scheduledLogResponse);
    const scheduledLogIndex = (scheduledLogPage.list || scheduledLogPage.records || scheduledLogPage.rows || scheduledLogPage.data || [])[0];
    expect(scheduledLogIndex).toBeDefined();
    const scheduledLogDetail = await waitForExecutionLogDetail(page, headers, scheduledLogIndex.id);
    expect(scheduledLogDetail.logSource).toBe('POWERJOB_NATIVE_LOG');
    expect(scheduledLogDetail.nativeLogAvailable).toBe(true);
    expect(scheduledLogDetail.nativeLogContent || '').toContain('Mango Job runtime probe executed');
    expect(scheduledLogDetail.nativeLogContent || '').toContain('Mango Job runtime probe System.out');
    expect(scheduledLogDetail.nativeLogContent || '').toContain('Mango Job runtime probe logger');
    const scheduledLogDrawer = page.getByRole('dialog', { name: '执行日志详情' });
    await expect(scheduledLogDrawer).toContainText('示例 每分钟执行一次任务');
    await expect(scheduledLogDrawer).toContainText(everyMinuteCode);
    await expectVisibleExecutionLog(scheduledLogDrawer, [
      'Mango Job handler output',
      'Mango Job runtime probe executed',
      'Mango Job runtime probe System.out',
      'Mango Job runtime probe logger',
    ]);
    await saveEvidenceScreenshot(page, '08d-scheduled-every-minute-log-detail.png', scheduledLogDrawer);
    await page.keyboard.press('Escape');

    const workers = await listWorkers(page, headers);
    expect(
      workers.some(item =>
        item.appCode === 'mango-job'
        && item.engineType === 'POWERJOB'
        && item.status === 'ONLINE'
        && Boolean(item.workerAddress?.includes(':27777'))
      ),
      'Worker 快照必须来自真实 PowerJob taskTrackerAddress',
    ).toBeTruthy();

    await openJobDefinitionPage(page);

    await searchDefinition(page, tempCode);
    await definitionRow(page, tempCode).getByRole('button', { name: '删除' }).click();
    await expect(page.locator('.el-message-box', { hasText: '删除任务' })).toBeVisible();
    await saveEvidenceScreenshot(page, '09-definition-delete-confirm.png', page.locator('.el-message-box', { hasText: '删除任务' }));
    await page.locator('.el-message-box', { hasText: '删除任务' }).getByRole('button', { name: /^(OK|确认)$/ }).click();
    await expect(page.locator('.el-message__content', { hasText: '任务已删除' }).last()).toBeVisible({ timeout: 10000 });
    await expect(definitionRow(page, tempCode)).toHaveCount(0);

    await searchDefinition(page, 'mango_job_example_');
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
      { menu: 'Worker', path: '/job/worker', api: '/api/job/workers/page', heading: 'Worker', search: true },
      { menu: '引擎状态', path: '/job/engine', api: '/api/job/engines/status', heading: '引擎状态', search: false },
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
        expect(workers.some(worker => Boolean(worker.workerAddress?.includes(':27777')))).toBeTruthy();
        await expect(page.locator('.el-table')).toContainText(':27777');
        await expect(page.locator('.el-table')).toContainText('在线');
      }
      await expect(page.locator('.el-message--error')).toHaveCount(0);
      await expect(page.locator('.job-error')).toHaveCount(0);
      await expect(page.locator('.error-container')).toHaveCount(0);
      await saveEvidenceScreenshot(page, `10-${item.path.replace('/job/', 'job-')}.png`);
    }

    await testInfo.attach('job-runtime-pages', {
      body: await page.screenshot({ fullPage: true }),
      contentType: 'image/png',
    });

    expect(consoleErrors).toEqual([]);
    expect(failedRequests).toEqual([]);
  });
});
