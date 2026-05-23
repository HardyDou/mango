import { expect, test, type APIResponse, type Page } from '@playwright/test';

test.setTimeout(90 * 1000);

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

async function apiHeaders(page: Page) {
  return page.evaluate(() => {
    const token = sessionStorage.getItem('MANGO_TOKEN') || '';
    const userInfo = JSON.parse(sessionStorage.getItem('userInfo') || '{}');
    const tenantId = String(userInfo?.tenantId || '1');
    return {
      Authorization: token ? `Bearer ${token}` : '',
      'TENANT-ID': tenantId,
      'X-Mango-Tenant-Id': tenantId,
    };
  });
}

async function expectBusinessOk(response: APIResponse) {
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.success || body.code === 200).toBeTruthy();
  return body;
}

async function createRuleVersion(page: Page, headers: Record<string, string>, genKey: string, version: number, prefix: string) {
  const ruleResponse = await page.request.post('/api/numgen/rules', {
    headers,
    data: {
      genKey,
      ruleName: '默认规则',
      version,
      status: 1,
    },
  });
  const ruleBody = await expectBusinessOk(ruleResponse);
  const ruleId = ruleBody.data;

  for (const segment of [
    { sortOrder: 1, segmentType: 'TEXT', segmentName: '固定前缀', literalValue: prefix },
    { sortOrder: 2, segmentType: 'SEQ', segmentName: '流水', seqWidth: 4, padChar: '0' },
  ]) {
    const segmentResponse = await page.request.post('/api/numgen/segments', {
      headers,
      data: { ...segment, ruleId },
    });
    await expectBusinessOk(segmentResponse);
  }
  return ruleId;
}

async function prepareHistoryData(page: Page, headers: Record<string, string>, genKey: string, genName: string) {
  const generatorResponse = await page.request.post('/api/numgen/generators', {
    headers,
    data: {
      genKey,
      genName,
      status: 1,
    },
  });
  await expectBusinessOk(generatorResponse);

  const v1 = await createRuleVersion(page, headers, genKey, 1, 'OLD');
  const publishV1Response = await page.request.post('/api/numgen/rules/publish', {
    headers,
    data: { ruleId: v1 },
  });
  await expectBusinessOk(publishV1Response);

  const v2 = await createRuleVersion(page, headers, genKey, 2, 'NEW');
  const publishV2Response = await page.request.post('/api/numgen/rules/publish', {
    headers,
    data: { ruleId: v2 },
  });
  await expectBusinessOk(publishV2Response);
}

async function generatorDetail(page: Page, headers: Record<string, string>, genKey: string) {
  const response = await page.request.get('/api/numgen/generators/page', {
    headers,
    params: { keyword: genKey, page: '1', size: '20' },
  });
  const body = await expectBusinessOk(response);
  return (body.data?.list || []).find((item: any) => item.genKey === genKey);
}

async function versions(page: Page, headers: Record<string, string>, genKey: string) {
  const response = await page.request.get('/api/numgen/rules/page', {
    headers,
    params: { genKey, page: '1', size: '20' },
  });
  const body = await expectBusinessOk(response);
  return body.data?.list || [];
}

test.describe('编号规则管理 E2E', () => {
  test('历史版本列表可查看并切换为新的生效版本', async ({ page }) => {
    await login(page);
    const headers = await apiHeaders(page);
    const stamp = Date.now();
    const genKey = `E2E_HISTORY_${stamp}`;
    const genName = `E2E 历史版本 ${stamp}`;

    await prepareHistoryData(page, headers, genKey, genName);

    await page.goto('/#/data/numgen');
    await expect(page.getByRole('heading', { name: '编号规则' })).toBeVisible({ timeout: 10000 });
    await page.getByPlaceholder('业务Key / 名称').fill(genKey);
    const listResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/numgen/generators/page') && response.status() === 200
    );
    await page.getByRole('button', { name: '查询' }).click();
    await listResponsePromise;

    const generatorRow = page.locator('.generator-table .el-table__row', { hasText: genKey }).first();
    await expect(generatorRow).toBeVisible({ timeout: 10000 });
    await expect(generatorRow).toContainText('V2');
    await expect(generatorRow).toContainText('已同步');

    await generatorRow.getByRole('button', { name: '历史版本' }).click();
    await expect(page.getByRole('heading', { name: '历史版本' })).toBeVisible({ timeout: 10000 });
    const v1Row = page.locator('.history-table .el-table__row', { hasText: 'V1' }).first();
    const v2Row = page.locator('.history-table .el-table__row', { hasText: 'V2' }).first();
    await expect(v1Row).toContainText('历史');
    await expect(v1Row).toContainText('OLD');
    await expect(v2Row).toContainText('生效中');

    const switchResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/numgen/rules/publish') &&
      response.request().method() === 'POST' &&
      response.status() === 200
    );
    await v1Row.getByRole('button', { name: '切换版本' }).click();
    await page.locator('.el-message-box', { hasText: '切换历史版本' }).getByRole('button', { name: /^(OK|确认)$/ }).click();
    await switchResponsePromise;
    await expect(page.locator('.el-message__content', { hasText: '历史版本已切换' }).last()).toBeVisible({ timeout: 10000 });

    const updatedGenerator = await generatorDetail(page, headers, genKey);
    expect(updatedGenerator.currentRuleVersion).toBe(3);
    expect(updatedGenerator.hasUnpublishedChanges).toBeFalsy();

    const updatedVersions = await versions(page, headers, genKey);
    expect(updatedVersions).toEqual(expect.arrayContaining([
      expect.objectContaining({ version: 1, versionState: 'HISTORY' }),
      expect.objectContaining({ version: 2, versionState: 'HISTORY' }),
      expect.objectContaining({ version: 3, versionState: 'ACTIVE' }),
    ]));
    await expect(page.locator('.history-table .el-table__row', { hasText: 'V3' }).first()).toContainText('生效中', { timeout: 10000 });
  });
});
