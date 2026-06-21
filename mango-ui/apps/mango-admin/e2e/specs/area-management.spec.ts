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

async function createArea(request: APIRequestContext, token: string, name: string, adcode: number) {
  const response = await request.post(e2eApi('/system/area'), {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      pid: 0,
      name,
      letter: 'E',
      adcode,
      location: '116.000000,39.000000',
      areaSort: 999,
      areaStatus: '1',
      areaType: '5',
      hot: '0',
      cityCode: String(adcode),
      tenantId: 1,
    },
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.success || body.code === 200).toBeTruthy();
}

async function findArea(request: APIRequestContext, token: string, name: string) {
  const response = await request.get(e2eApi('/system/area/children?parentId=0'), {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  return (body.data || []).find((item: any) => item.name === name);
}

async function cleanupArea(request: APIRequestContext, token: string, name: string) {
  const area = await findArea(request, token, name);
  if (!area?.id) return;
  await request.delete(e2eApi(`/system/area?id=${area.id}`), {
    headers: { Authorization: `Bearer ${token}` },
  });
}

async function expectLatestMessage(page: Page, message: string) {
  await expect(page.locator('.el-message__content', { hasText: message }).last()).toBeVisible({ timeout: 10000 });
}

async function expectNoAuthError(page: Page) {
  await expect(page.locator('.el-message--error')).toHaveCount(0);
  await expect(page.locator('text=/401|403|未授权|没有权限|拒绝访问|加载失败|登录已过期|请重新登录/')).toHaveCount(0);
}

test.describe('T11 行政区划管理页面真实接口闭环', () => {
  test('平台管理员按级别加载并可维护自定义区划', async ({ page, request }) => {
    const unique = Date.now();
    const areaName = `E2E区划${unique}`;
    const editedAreaName = `${areaName}-编辑`;
    const adcode = Number(String(unique).slice(-8));
    const platformToken = await loginToken(request, platformTenant);

    try {
      await cleanupArea(request, platformToken, areaName);
      await cleanupArea(request, platformToken, editedAreaName);
      await createArea(request, platformToken, areaName, adcode);
      await loginPage(page, platformTenant);

      const listResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/system/area/children?parentId=0') &&
        response.status() === 200
      );
      await page.goto('/#/system/area');
      await listResponsePromise;
      await expect(page.getByText('行政区划').first()).toBeVisible({ timeout: 10000 });
      await expect(page.locator('.el-table__row', { hasText: '北京市' }).first()).toBeVisible({ timeout: 10000 });

      const row = page.locator('.el-table__row', { hasText: areaName }).first();
      await expect(row).toBeVisible({ timeout: 10000 });
      await expect(row.getByText('自定义区域')).toBeVisible();

      await row.getByRole('button', { name: '编辑' }).click();
      const editDialog = page.getByRole('dialog', { name: '编辑区划' });
      await expect(editDialog).toBeVisible();
      await editDialog.getByLabel('区划名称').fill(editedAreaName);

      const updateResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/system/area') &&
        response.request().method() === 'PUT'
      );
      await editDialog.getByRole('button', { name: '确定' }).click();
      const updateResponse = await updateResponsePromise;
      const updateBody = await updateResponse.json();
      expect(updateResponse.status()).toBe(200);
      expect(updateBody.success || updateBody.code === 200).toBeTruthy();
      await expectLatestMessage(page, '修改成功');
      await expect(page.locator('.el-table__row', { hasText: editedAreaName })).toBeVisible({ timeout: 10000 });

      const editedRow = page.locator('.el-table__row', { hasText: editedAreaName }).first();
      await editedRow.getByRole('button', { name: '删除' }).click();
      await expect(page.getByText(`确认删除区划「${editedAreaName}」?`)).toBeVisible();
      const deleteResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/system/area') &&
        response.request().method() === 'DELETE'
      );
      await page.getByRole('button', { name: '确定' }).last().click();
      const deleteResponse = await deleteResponsePromise;
      const deleteBody = await deleteResponse.json();
      expect(deleteResponse.status()).toBe(200);
      expect(deleteBody.success || deleteBody.code === 200).toBeTruthy();
      await expectLatestMessage(page, '删除成功');
      await expect(page.locator('.el-table__row', { hasText: editedAreaName })).toHaveCount(0);
      await expectNoAuthError(page);
    } finally {
      await cleanupArea(request, platformToken, areaName).catch(() => undefined);
      await cleanupArea(request, platformToken, editedAreaName).catch(() => undefined);
    }
  });

  test('A 公司不可见行政区划管理入口，维护接口返回 403，选择器读取可用', async ({ page, request }) => {
    const companyToken = await loginToken(request, companyATenant);

    const treeResponse = await request.get(e2eApi('/system/area/tree?type=1'), {
      headers: { Authorization: `Bearer ${companyToken}` },
    });
    expect(treeResponse.status()).toBe(200);
    const treeBody = await treeResponse.json();
    expect(treeBody.data[0]).toMatchObject({ id: '1', name: '北京市' });

    const childrenResponse = await request.get(e2eApi('/system/area/children?parentId=1'), {
      headers: { Authorization: `Bearer ${companyToken}` },
    });
    expect(childrenResponse.status()).toBe(200);
    const childrenBody = await childrenResponse.json();
    expect(childrenBody.data[0]).toMatchObject({ id: '1101', name: '北京市' });

    const createResponse = await request.post(e2eApi('/system/area'), {
      headers: { Authorization: `Bearer ${companyToken}` },
      data: {
        pid: 0,
        name: `无权区划${Date.now()}`,
        adcode: Date.now(),
        areaType: '5',
        areaStatus: '1',
      },
    });
    expect(createResponse.status()).toBe(403);

    await loginPage(page, companyATenant);
    await expect(page.getByRole('button', { name: '系统管理' })).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('基础数据')).toHaveCount(0);
    await expect(page.getByText('行政区划')).toHaveCount(0);
    await expectNoAuthError(page);
  });
});
