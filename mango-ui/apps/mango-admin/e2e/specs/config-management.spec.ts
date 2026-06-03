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

async function loginToken(request: APIRequestContext, tenant: LoginTenant) {
  const response = await request.post(e2eApi('/auth/login'), {
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

async function listConfigs(request: APIRequestContext, token: string, type?: string) {
  const url = type
    ? e2eApi(`/system/config/list?type=${type}`)
    : e2eApi('/system/config/list');
  const response = await request.get(url, {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  return body.data || [];
}

async function cleanupConfig(request: APIRequestContext, token: string, key: string) {
  const configs = await listConfigs(request, token);
  for (const item of configs.filter((config: any) => config.configKey === key)) {
    await request.delete(e2eApi(`/system/config?id=${item.id}`), {
      headers: { Authorization: `Bearer ${token}` },
    });
  }
}

async function expectLatestMessage(page: Page, message: string) {
  await expect(page.locator('.el-message__content', { hasText: message }).last()).toBeVisible({ timeout: 10000 });
}

async function expectNoAuthError(page: Page) {
  await expect(page.locator('.el-message--error')).toHaveCount(0);
  await expect(page.locator('text=/401|403|未授权|没有权限|拒绝访问|加载失败|登录已过期|请重新登录/')).toHaveCount(0);
}

function currentTabRows(page: Page, text: string) {
  return page.locator('.el-tab-pane:not([aria-hidden="true"]) .el-table__row', { hasText: text });
}

test.describe('T12 系统配置页面真实接口闭环', () => {
  test('平台管理员可新增、编辑、删除系统参数和系统配置', async ({ page, request }) => {
    const unique = Date.now();
    const paramKey = `e2e.param.${unique}`;
    const configKey = `e2e.config.${unique}`;
    const platformToken = await loginToken(request, platformTenant);

    try {
      await cleanupConfig(request, platformToken, paramKey);
      await cleanupConfig(request, platformToken, configKey);
      await loginPage(page, platformTenant);

      const listResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/system/config/list') &&
        response.status() === 200
      );
      await page.goto('/#/system/config');
      await listResponsePromise;
      await expect(page.getByText('系统配置').first()).toBeVisible({ timeout: 10000 });

      await page.getByRole('button', { name: '新增参数' }).click();
      const createParamDialog = page.getByRole('dialog', { name: '新增参数' });
      await expect(createParamDialog).toBeVisible();
      await createParamDialog.getByLabel('参数键').fill(paramKey);
      await createParamDialog.getByLabel('参数值').fill('value-1');
      await createParamDialog.getByLabel('描述').fill(`E2E参数${unique}`);

      const createParamResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/system/config') &&
        response.request().method() === 'POST'
      );
      await createParamDialog.getByRole('button', { name: '确定' }).click();
      const createParamResponse = await createParamResponsePromise;
      const createParamBody = await createParamResponse.json();
      expect(createParamResponse.status()).toBe(200);
      expect(createParamBody.success || createParamBody.code === 200).toBeTruthy();
      await expectLatestMessage(page, '新增成功');
      await expect(currentTabRows(page, paramKey)).toBeVisible({ timeout: 10000 });

      const paramRow = currentTabRows(page, paramKey).first();
      await paramRow.getByRole('button', { name: '编辑' }).click();
      const editParamDialog = page.getByRole('dialog', { name: '编辑参数' });
      await editParamDialog.getByLabel('参数值').fill('value-2');
      const updateParamResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/system/config') &&
        response.request().method() === 'PUT'
      );
      await editParamDialog.getByRole('button', { name: '确定' }).click();
      const updateParamResponse = await updateParamResponsePromise;
      const updateParamBody = await updateParamResponse.json();
      expect(updateParamResponse.status()).toBe(200);
      expect(updateParamBody.success || updateParamBody.code === 200).toBeTruthy();
      await expectLatestMessage(page, '修改成功');
      await expect(currentTabRows(page, paramKey).getByText('value-2')).toBeVisible({ timeout: 10000 });

      await page.getByRole('tab', { name: '系统配置' }).click();
      await page.getByRole('button', { name: '新增配置' }).click();
      const createConfigDialog = page.getByRole('dialog', { name: '新增配置' });
      await expect(createConfigDialog).toBeVisible();
      await createConfigDialog.getByLabel('配置键').fill(configKey);
      await createConfigDialog.getByLabel('配置值').fill('enabled');
      await createConfigDialog.getByLabel('描述').fill(`E2E配置${unique}`);

      const createConfigResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/system/config') &&
        response.request().method() === 'POST'
      );
      await createConfigDialog.getByRole('button', { name: '确定' }).click();
      const createConfigResponse = await createConfigResponsePromise;
      const createConfigBody = await createConfigResponse.json();
      expect(createConfigResponse.status()).toBe(200);
      expect(createConfigBody.success || createConfigBody.code === 200).toBeTruthy();
      await expectLatestMessage(page, '新增成功');
      await expect(currentTabRows(page, configKey)).toBeVisible({ timeout: 10000 });

      const configRow = currentTabRows(page, configKey).first();
      await configRow.getByRole('button', { name: '编辑' }).click();
      const editConfigDialog = page.getByRole('dialog', { name: '编辑配置' });
      await editConfigDialog.getByLabel('配置值').fill('disabled');
      const updateConfigResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/system/config') &&
        response.request().method() === 'PUT'
      );
      await editConfigDialog.getByRole('button', { name: '确定' }).click();
      const updateConfigResponse = await updateConfigResponsePromise;
      const updateConfigBody = await updateConfigResponse.json();
      expect(updateConfigResponse.status()).toBe(200);
      expect(updateConfigBody.success || updateConfigBody.code === 200).toBeTruthy();
      await expectLatestMessage(page, '修改成功');
      await expect(currentTabRows(page, configKey).getByText('disabled')).toBeVisible({ timeout: 10000 });

      for (const key of [configKey, paramKey]) {
        if (key === paramKey) {
          await page.getByRole('tab', { name: '系统参数' }).click();
        }
        const row = currentTabRows(page, key).first();
        await row.getByRole('button', { name: '删除' }).click();
        await expect(page.getByText('确认删除该')).toBeVisible();
        const deleteResponsePromise = page.waitForResponse((response) =>
          response.url().includes('/api/system/config') &&
          response.request().method() === 'DELETE'
        );
        await page.getByRole('button', { name: '确定' }).last().click();
        const deleteResponse = await deleteResponsePromise;
        const deleteBody = await deleteResponse.json();
        expect(deleteResponse.status()).toBe(200);
        expect(deleteBody.success || deleteBody.code === 200).toBeTruthy();
        await expectLatestMessage(page, '删除成功');
        await expect(currentTabRows(page, key)).toHaveCount(0);
      }

      await expectNoAuthError(page);
    } finally {
      await cleanupConfig(request, platformToken, paramKey).catch(() => undefined);
      await cleanupConfig(request, platformToken, configKey).catch(() => undefined);
    }
  });

  test('A 公司不可见参数配置入口，维护接口返回 403', async ({ page, request }) => {
    const companyToken = await loginToken(request, companyATenant);

    const listResponse = await request.get(e2eApi('/system/config/list'), {
      headers: { Authorization: `Bearer ${companyToken}` },
    });
    expect(listResponse.status()).toBe(403);

    const createResponse = await request.post(e2eApi('/system/config'), {
      headers: { Authorization: `Bearer ${companyToken}` },
      data: {
        configKey: `deny.config.${Date.now()}`,
        configValue: 'deny',
        configName: '无权配置',
        type: 'SYSTEM',
        sort: 0,
        status: 1,
      },
    });
    expect(createResponse.status()).toBe(403);

    await loginPage(page, companyATenant);
    await expect(page.getByText('权限管理').first()).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('基础数据')).toHaveCount(0);
    await expect(page.getByText('参数配置')).toHaveCount(0);
    await expectNoAuthError(page);
  });
});
