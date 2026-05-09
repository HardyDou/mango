import { expect, test, type APIRequestContext, type Page } from '@playwright/test';

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
  const response = await request.post('http://localhost:5555/auth/login', {
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

async function listRoutes(request: APIRequestContext, token: string) {
  const response = await request.get('http://localhost:5555/system/route/list', {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  return body.data || [];
}

async function cleanupRoute(request: APIRequestContext, token: string, routeName: string) {
  const routes = await listRoutes(request, token);
  for (const route of routes.filter((item: any) => item.routeName === routeName)) {
    await request.delete(`http://localhost:5555/system/route?id=${route.id}`, {
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

test.describe('T13 路由管理页面真实接口闭环', () => {
  test('平台管理员可维护平台运行路由配置', async ({ page, request }) => {
    const unique = Date.now();
    const routeName = `E2E路由${unique}`;
    const editedRouteName = `${routeName}-编辑`;
    const routePath = `/e2e/route/${unique}`;
    const platformToken = await loginToken(request, platformTenant);

    try {
      await cleanupRoute(request, platformToken, routeName);
      await cleanupRoute(request, platformToken, editedRouteName);
      await loginPage(page, platformTenant);

      const listResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/system/route/list') &&
        response.status() === 200
      );
      await page.goto('/#/system/route');
      await listResponsePromise;
      await expect(page.getByText('路由管理').first()).toBeVisible({ timeout: 10000 });

      await page.getByRole('button', { name: '新增路由' }).click();
      const createDialog = page.getByRole('dialog', { name: '新增路由' });
      await expect(createDialog).toBeVisible();
      await createDialog.getByLabel('路由名称').fill(routeName);
      await createDialog.getByLabel('路由路径').fill(routePath);
      await createDialog.getByLabel('描述').fill(`E2E路由配置${unique}`);

      const createResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/system/route') &&
        response.request().method() === 'POST'
      );
      await createDialog.getByRole('button', { name: '确定' }).click();
      const createResponse = await createResponsePromise;
      const createBody = await createResponse.json();
      expect(createResponse.status()).toBe(200);
      expect(createBody.success || createBody.code === 200).toBeTruthy();
      await expectLatestMessage(page, '新增成功');
      await expect(page.locator('.el-table__row', { hasText: routeName })).toBeVisible({ timeout: 10000 });

      const row = page.locator('.el-table__row', { hasText: routeName }).first();
      await row.getByRole('button', { name: '编辑' }).click();
      const editDialog = page.getByRole('dialog', { name: '编辑路由' });
      await editDialog.getByLabel('路由名称').fill(editedRouteName);
      const updateResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/system/route') &&
        response.request().method() === 'PUT'
      );
      await editDialog.getByRole('button', { name: '确定' }).click();
      const updateResponse = await updateResponsePromise;
      const updateBody = await updateResponse.json();
      expect(updateResponse.status()).toBe(200);
      expect(updateBody.success || updateBody.code === 200).toBeTruthy();
      await expectLatestMessage(page, '修改成功');
      await expect(page.locator('.el-table__row', { hasText: editedRouteName })).toBeVisible({ timeout: 10000 });

      const editedRow = page.locator('.el-table__row', { hasText: editedRouteName }).first();
      await editedRow.getByRole('button', { name: '删除' }).click();
      await expect(page.getByText('确认删除该路由?')).toBeVisible();
      const deleteResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/system/route') &&
        response.request().method() === 'DELETE'
      );
      await page.getByRole('button', { name: '确定' }).last().click();
      const deleteResponse = await deleteResponsePromise;
      const deleteBody = await deleteResponse.json();
      expect(deleteResponse.status()).toBe(200);
      expect(deleteBody.success || deleteBody.code === 200).toBeTruthy();
      await expectLatestMessage(page, '删除成功');
      await expect(page.locator('.el-table__row', { hasText: editedRouteName })).toHaveCount(0);
      await expectNoAuthError(page);
    } finally {
      await cleanupRoute(request, platformToken, routeName).catch(() => undefined);
      await cleanupRoute(request, platformToken, editedRouteName).catch(() => undefined);
    }
  });

  test('A 公司不可见路由管理入口，维护接口返回 403', async ({ page, request }) => {
    const companyToken = await loginToken(request, companyATenant);

    const listResponse = await request.get('http://localhost:5555/system/route/list', {
      headers: { Authorization: `Bearer ${companyToken}` },
    });
    expect(listResponse.status()).toBe(403);

    const createResponse = await request.post('http://localhost:5555/system/route', {
      headers: { Authorization: `Bearer ${companyToken}` },
      data: {
        routeName: `无权路由${Date.now()}`,
        routeType: 1,
        routePath: '/deny/route',
        sort: 0,
        status: 1,
      },
    });
    expect(createResponse.status()).toBe(403);

    await loginPage(page, companyATenant);
    await expect(page.getByText('账号权限').first()).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('路由管理')).toHaveCount(0);
    await expectNoAuthError(page);
  });
});
