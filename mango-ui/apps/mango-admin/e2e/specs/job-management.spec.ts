import { expect, test, type APIResponse, type Locator, type Page } from '@playwright/test';
import { mkdir } from 'node:fs/promises';
import { resolve } from 'node:path';

test.setTimeout(90 * 1000);

type ApiId = string | number;
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
}

interface JobInstance {
  id: ApiId;
  jobId: ApiId;
  triggerType?: string;
  triggerBatchNo?: string;
  status?: string;
}

interface JobLogIndex {
  id: ApiId;
  jobId: ApiId;
  instanceId: ApiId;
  engineType?: string;
  logLocation?: string;
}

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
    handlerName: 'sampleManualHandler',
    paramSchema: '{ "type": "object", "properties": { "source": { "type": "string" } } }',
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
    handlerName: 'sampleCronHandler',
    paramValue: '{ "source": "acceptance", "kind": "cron" }',
    concurrencyPolicy: 'SERIAL',
    misfireStrategy: 'FIRE_ONCE',
    timeoutSeconds: 600,
    retryPolicy: '{ "maxRetryTimes": 2 }',
    engineType: 'POWERJOB',
  },
  {
    appCode: 'mango-job',
    jobCode: 'mango_job_example_http_callback',
    jobName: '示例 HTTP 回调任务',
    jobType: 'HTTP',
    scheduleType: 'FIXED_RATE',
    scheduleExpression: '300',
    handlerName: 'https://example.invalid/mango/job/callback',
    paramValue: '{ "source": "acceptance", "kind": "http" }',
    concurrencyPolicy: 'DISCARD',
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

async function listLogs(page: Page, headers: LoginHeaders, jobId: ApiId): Promise<JobLogIndex[]> {
  const response = await page.request.get('/api/job/logs/page', {
    headers,
    params: { jobId: String(jobId), pageNum: '1', pageSize: '20' },
  });
  const data = await expectBusinessOk<PageData<JobLogIndex>>(response);
  return data.list || data.records || data.rows || data.data || [];
}

async function createDefinition(page: Page, headers: LoginHeaders, payload: SaveJobDefinitionPayload): Promise<ApiId> {
  return expectBusinessOk<ApiId>(await page.request.post('/api/job/definitions', { headers, data: payload }));
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
    if (payload.jobCode === 'mango_job_example_http_callback') {
      await updateDefinitionStatus(page, headers, id, 'PAUSED');
    }
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
  await dialog.getByLabel('处理器').fill('e2eTemporaryHandler');
  await selectFormOption(page, dialog, '调度类型', '手动');
  await dialog.getByLabel('超时秒数').fill('180');
  await dialog.getByLabel('并发策略').fill('SERIAL');
  await dialog.getByLabel('错过策略').fill('IGNORE');
  await dialog.getByLabel('参数 Schema').fill('{ "type": "object" }');
  await dialog.getByLabel('默认参数').fill('{ "source": "e2e" }');
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
  expect(toolbarBox?.height || 0).toBeLessThan(150);
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

    const fixedRateDialog = await openDefinitionEditor(page, 'mango_job_example_http_callback');
    await expect(fixedRateDialog.getByLabel('调度表达式')).toHaveValue('300');
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
    await triggerDialog.getByLabel('触发参数').fill('{ "source": "e2e-trigger" }');
    await saveEvidenceScreenshot(page, '06-trigger-dialog-frequency-manual.png', triggerDialog);
    await triggerDialog.getByRole('button', { name: '触发' }).click();
    await triggerResponsePromise;
    await expect(page.locator('.el-message__content', { hasText: '已触发' }).last()).toBeVisible({ timeout: 10000 });

    const instances = await listInstances(page, headers, manualDefinition!.id);
    const triggeredInstance = instances.find(item => item.triggerBatchNo === triggerBatchNo && item.triggerType === 'MANUAL');
    expect(triggeredInstance).toBeDefined();

    const logs = await listLogs(page, headers, manualDefinition!.id);
    const executionLog = logs.find(item =>
      String(item.jobId) === String(manualDefinition!.id)
      && String(item.instanceId) === String(triggeredInstance!.id)
      && item.engineType === 'POWERJOB'
    );
    expect(executionLog).toBeDefined();

    const instanceResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/job/instances/page') && response.status() === 200
    );
    await clickMenu(page, '执行实例');
    await instanceResponsePromise;
    await page.getByPlaceholder('jobId').fill(String(manualDefinition!.id));
    await page.getByPlaceholder('triggerBatchNo').fill(triggerBatchNo);
    const filteredInstanceResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/job/instances/page') && response.status() === 200
    );
    await page.getByRole('button', { name: '查询' }).click();
    await filteredInstanceResponsePromise;
    await expect(page.locator('.el-table__row', { hasText: triggerBatchNo }).first()).toBeVisible();
    await saveEvidenceScreenshot(page, '07-instance-filtered-trigger-batch.png');

    const logResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/job/logs/page') && response.status() === 200
    );
    await clickMenu(page, '执行日志');
    await logResponsePromise;
    await page.getByPlaceholder('jobId').fill(String(manualDefinition!.id));
    await page.getByPlaceholder('instanceId').fill(String(triggeredInstance!.id));
    const filteredLogResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/job/logs/page') && response.status() === 200
    );
    await page.getByRole('button', { name: '查询' }).click();
    await filteredLogResponsePromise;
    await expect(page.locator('.el-table__row', { hasText: String(triggeredInstance!.id) }).first()).toBeVisible();
    await saveEvidenceScreenshot(page, '08-execution-log-index.png');

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
    await openJobDefinitionPage(page);

    const pages = [
      { menu: '执行实例', path: '/job/instance', api: '/api/job/instances/page', heading: '执行实例', search: true },
      { menu: '执行日志', path: '/job/log', api: '/api/job/logs/page', heading: '执行日志', search: true },
      { menu: 'Worker', path: '/job/worker', api: '/api/job/workers/page', heading: 'Worker', search: true },
      { menu: '处理器', path: '/job/handler', api: '/api/job/handlers', heading: '处理器', search: false },
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
      await expect(page.locator('text=/401|403|未授权|拒绝访问|路由加载失败|加载失败/')).toHaveCount(0);
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
