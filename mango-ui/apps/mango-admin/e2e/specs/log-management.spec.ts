import { expect, test, type APIRequestContext, type Page } from '@playwright/test';
import { api as e2eApi } from '../support/api';

type LoginTenant = {
  tenantId: string;
  tenantCode: string;
  tenantName: string;
};

const platformTenant: LoginTenant = {
  tenantId: '1',
  tenantCode: 'default',
  tenantName: '芒果集团',
};

const companyATenant: LoginTenant = {
  tenantId: '2',
  tenantCode: 'company_a',
  tenantName: 'A公司',
};

test.describe.configure({ mode: 'serial' });

async function loginToken(request: APIRequestContext, tenant: LoginTenant) {
  const response = await request.post(e2eApi('/auth/login'), {
    headers: { 'X-Forwarded-For': '8.8.8.8' },
    data: {
      username: 'admin',
      password: 'admin123',
      tenantId: tenant.tenantId,
      tenantCode: tenant.tenantCode,
      realm: 'INTERNAL',
      actorType: 'INTERNAL_USER',
      partyType: 'INTERNAL_ORG',
      appCode: 'internal-admin',
    },
  });
  expect(response.ok()).toBeTruthy();
  const body = await response.json();
  expect(body.success || body.code === 200).toBeTruthy();
  return body.data.accessToken as string;
}

async function loginPage(page: Page, tenant: LoginTenant) {
  await page.goto('/#/login');
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: new RegExp(tenant.tenantName) }).click();
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  await page.click('button:has-text("登 录")');
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

async function getJson(request: APIRequestContext, token: string, path: string) {
  const response = await request.get(e2eApi(`${path}`), {
    headers: { Authorization: `Bearer ${token}` },
  });
  const body = await response.json();
  return { response, body };
}

async function listConfigs(request: APIRequestContext, token: string) {
  const response = await request.get(e2eApi('/system/config/list'), {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  return body.data || [];
}

async function cleanupConfig(request: APIRequestContext, token: string, configKey: string) {
  const configs = await listConfigs(request, token);
  for (const config of configs.filter((item: any) => item.configKey === configKey)) {
    await request.delete(e2eApi(`/system/config?id=${config.id}`), {
      headers: { Authorization: `Bearer ${token}` },
    });
  }
}

async function createConfig(request: APIRequestContext, token: string, configKey: string) {
  const response = await request.post(e2eApi('/system/config'), {
    headers: { Authorization: `Bearer ${token}`, 'X-Forwarded-For': '8.8.4.4' },
    data: {
      configKey,
      configValue: 'enabled',
      configName: 'E2E 审计配置',
      type: 'SYSTEM',
      sort: 999,
      status: 1,
      remark: 'E2E 审计日志触发配置',
    },
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.success || body.code === 200).toBeTruthy();
  return body.data as number | string;
}

async function deleteConfig(request: APIRequestContext, token: string, configId: number | string) {
  const response = await request.delete(e2eApi(`/system/config?id=${configId}`), {
    headers: { Authorization: `Bearer ${token}`, 'X-Forwarded-For': '8.8.4.4' },
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.success || body.code === 200).toBeTruthy();
}

async function expectNoAuthError(page: Page) {
  await expect(page.locator('.el-message--error')).toHaveCount(0);
  await expect(page.locator('text=/401|403|未授权|没有权限|拒绝访问|加载失败|登录已过期|请重新登录/')).toHaveCount(0);
}

test.describe('T14 审计日志真实写入与查询闭环', () => {
  test('登录日志写入、分页查询、统计和页面加载正常', async ({ page, request }) => {
    const platformToken = await loginToken(request, platformTenant);

    const { response: listResponse, body: listBody } = await getJson(
      request,
      platformToken,
      '/system/log/login/list?page=1&size=5',
    );
    expect(listResponse.status()).toBe(200);
    expect(listBody.success || listBody.code === 200).toBeTruthy();
    expect(Number(listBody.data.total)).toBeGreaterThan(0);
    expect(listBody.data.list.some((item: any) => item.username === 'admin' && item.tenantId === '1')).toBeTruthy();
    expect(listBody.data.list[0].location).toBeTruthy();

    const { response: statResponse, body: statBody } = await getJson(
      request,
      platformToken,
      '/system/log/login/statistics',
    );
    expect(statResponse.status()).toBe(200);
    expect(statBody.success || statBody.code === 200).toBeTruthy();
    expect(Number(statBody.data.totalCount)).toBeGreaterThan(0);
    expect(Number(statBody.data.successCount)).toBeGreaterThan(0);

    await loginPage(page, platformTenant);
    const listResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/system/log/login/list') &&
      response.status() === 200
    );
    const statResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/system/log/login/statistics') &&
      response.status() === 200
    );
    await page.goto('/#/system/login-log');
    const pageListResponse = await listResponsePromise;
    const pageStatResponse = await statResponsePromise;
    const pageListBody = await pageListResponse.json();
    const pageStatBody = await pageStatResponse.json();
    expect(Number(pageListBody.data.total)).toBeGreaterThan(0);
    expect(Number(pageStatBody.data.totalCount)).toBeGreaterThan(0);
    await expect(page.getByText('登录日志').first()).toBeVisible({ timeout: 10000 });
    await expect(page.locator('.el-table__row', { hasText: 'admin' }).first()).toBeVisible({ timeout: 10000 });
    await expectNoAuthError(page);
  });

  test('维护接口写入操作日志，操作日志页面使用后端分页查询', async ({ page, request }) => {
    const configKey = `e2e.audit.config.${Date.now()}`;
    const platformToken = await loginToken(request, platformTenant);
    let configId: number | string | undefined;

    try {
      await cleanupConfig(request, platformToken, configKey);
      configId = await createConfig(request, platformToken, configKey);
      await deleteConfig(request, platformToken, configId);
      configId = undefined;

      const { response: operationResponse, body: operationBody } = await getJson(
        request,
        platformToken,
        '/system/log/operation/list?page=1&size=10&keyword=%E7%B3%BB%E7%BB%9F%E9%85%8D%E7%BD%AE',
      );
      expect(operationResponse.status()).toBe(200);
      expect(operationBody.success || operationBody.code === 200).toBeTruthy();
      const createLog = operationBody.data.list.find((item: any) => item.operation === '新增系统配置');
      const deleteLog = operationBody.data.list.find((item: any) => item.operation === '删除系统配置');
      expect(createLog).toBeTruthy();
      expect(deleteLog).toBeTruthy();
      expect(createLog.method).toBe('POST');
      expect(createLog.handlerMethod).toContain('SysConfigController.create');
      expect(createLog.location).toBeTruthy();
      expect(deleteLog.method).toBe('DELETE');
      expect(deleteLog.handlerMethod).toContain('SysConfigController.delete');
      expect(deleteLog.location).toBeTruthy();

      await loginPage(page, platformTenant);
      const listResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/system/log/operation/list') &&
        response.status() === 200
      );
      await page.goto('/#/system/operation-log');
      await listResponsePromise;
      await expect(page.getByText('操作日志').first()).toBeVisible({ timeout: 10000 });

      await page.getByLabel('关键词').fill('系统配置');
      const searchResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/system/log/operation/list') &&
        response.url().includes('%E7%B3%BB%E7%BB%9F%E9%85%8D%E7%BD%AE') &&
        response.status() === 200
      );
      await page.getByRole('button', { name: '查询' }).click();
      const searchResponse = await searchResponsePromise;
      const searchBody = await searchResponse.json();
      expect(Number(searchBody.data.total)).toBeGreaterThan(0);
      await expect(page.locator('.el-table__row', { hasText: '系统配置' }).first()).toBeVisible({ timeout: 10000 });
      await expectNoAuthError(page);
    } finally {
      if (configId !== undefined) {
        await deleteConfig(request, platformToken, configId).catch(() => undefined);
      }
      await cleanupConfig(request, platformToken, configKey).catch(() => undefined);
    }
  });

  test('A 公司日志查询按机构隔离，不能看到平台机构日志', async ({ request }) => {
    const platformToken = await loginToken(request, platformTenant);
    const companyToken = await loginToken(request, companyATenant);

    const { body: platformLoginBody } = await getJson(
      request,
      platformToken,
      '/system/log/login/list?page=1&size=20',
    );
    expect(platformLoginBody.data.list.some((item: any) => item.tenantId === '1')).toBeTruthy();

    const { response: companyLoginResponse, body: companyLoginBody } = await getJson(
      request,
      companyToken,
      '/system/log/login/list?page=1&size=20',
    );
    expect(companyLoginResponse.status()).toBe(200);
    expect(companyLoginBody.success || companyLoginBody.code === 200).toBeTruthy();
    expect(companyLoginBody.data.list.every((item: any) => item.tenantId === '2')).toBeTruthy();

    const { response: companyOperationResponse, body: companyOperationBody } = await getJson(
      request,
      companyToken,
      '/system/log/operation/list?page=1&size=20',
    );
    expect(companyOperationResponse.status()).toBe(200);
    expect(companyOperationBody.success || companyOperationBody.code === 200).toBeTruthy();
    expect(companyOperationBody.data.list.every((item: any) => item.tenantId === '2')).toBeTruthy();
  });
});
