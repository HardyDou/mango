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

async function listMenus(request: APIRequestContext, token: string, menuName: string) {
  const response = await request.get(
    `http://localhost:5555/authorization/menus?appCode=internal-admin&fmt=list&menuName=${encodeURIComponent(menuName)}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.success || body.code === 200).toBeTruthy();
  return body.data || [];
}

async function cleanupMenus(request: APIRequestContext, token: string, menuName: string) {
  const menus = await listMenus(request, token, menuName);
  const sorted = menus
    .filter((item: any) => item.menuName === menuName)
    .sort((a: any, b: any) => Number(b.menuId) - Number(a.menuId));
  for (const menu of sorted) {
    await request.delete(`http://localhost:5555/authorization/menus?menuId=${menu.menuId}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  }
}

async function expectNoAuthError(page: Page) {
  await expect(page.locator('.el-message--error')).toHaveCount(0);
  await expect(page.locator('text=/401|403|未授权|没有权限|拒绝访问|加载失败|登录已过期|请重新登录/')).toHaveCount(0);
}

test.describe('T9 菜单管理页面真实接口闭环', () => {
  test('平台管理员可新增、编辑、删除菜单，列表按树形展示', async ({ page, request }) => {
    const unique = Date.now();
    const menuName = `E2E菜单${unique}`;
    const editedMenuName = `${menuName}-编辑`;
    const menuCode = `e2e:menu:${unique}`;
    const menuPath = `/system/e2e-menu-${unique}`;
    const platformToken = await loginToken(request, platformTenant);

    try {
      await cleanupMenus(request, platformToken, menuName);
      await cleanupMenus(request, platformToken, editedMenuName);
      await loginPage(page, platformTenant);

      const listResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/authorization/menus') &&
        response.url().includes('fmt=list') &&
        response.status() === 200
      );
      await page.goto('/#/system/menu');
      await listResponsePromise;
      await expect(page.getByText('菜单管理').first()).toBeVisible({ timeout: 10000 });
      await expect(page.getByText('平台运营').first()).toBeVisible({ timeout: 10000 });
      await expect(page.getByText('应用管理').first()).toBeVisible({ timeout: 10000 });

      await page.getByRole('button', { name: '新增菜单' }).click();
      const createDialog = page.getByRole('dialog', { name: '新增菜单' });
      await expect(createDialog).toBeVisible();
      await createDialog.getByLabel('菜单名称').fill(menuName);
      await createDialog.getByLabel('菜单编码').fill(menuCode);
      await createDialog.getByLabel('路由路径').fill(menuPath);
      await createDialog.getByLabel('组件路径').fill('views/error/404');
      await createDialog.getByLabel('图标').fill('Menu');

      const createResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/authorization/menus') &&
        response.request().method() === 'POST'
      );
      await createDialog.getByRole('button', { name: '确定' }).click();
      const createResponse = await createResponsePromise;
      const createBody = await createResponse.json();
      expect(createResponse.status()).toBe(200);
      expect(createBody.success || createBody.code === 200).toBeTruthy();
      await expect(page.getByText('新增成功')).toBeVisible({ timeout: 10000 });

      await page.getByLabel('关键词').fill(menuName);
      const searchResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/authorization/menus') &&
        response.url().includes('fmt=list') &&
        response.status() === 200
      );
      await page.getByRole('button', { name: '查询' }).click();
      await searchResponsePromise;
      const row = page.locator('.el-table__row', { hasText: menuName }).first();
      await expect(row).toBeVisible({ timeout: 10000 });
      await expect(row.getByText(menuPath, { exact: true })).toBeVisible();

      await row.getByRole('button', { name: '编辑' }).click();
      const editDialog = page.getByRole('dialog', { name: '编辑菜单' });
      await expect(editDialog).toBeVisible();
      await editDialog.getByLabel('菜单名称').fill(editedMenuName);

      const updateResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/authorization/menus') &&
        response.request().method() === 'PUT'
      );
      await editDialog.getByRole('button', { name: '确定' }).click();
      const updateResponse = await updateResponsePromise;
      const updateBody = await updateResponse.json();
      expect(updateResponse.status()).toBe(200);
      expect(updateBody.success || updateBody.code === 200).toBeTruthy();
      await expect(page.getByText('修改成功')).toBeVisible({ timeout: 10000 });
      await expect(page.locator('.el-table__row', { hasText: editedMenuName })).toBeVisible({ timeout: 10000 });

      const editedRow = page.locator('.el-table__row', { hasText: editedMenuName }).first();
      await editedRow.getByRole('button', { name: '删除' }).click();
      await expect(page.getByText('确认删除该菜单?')).toBeVisible();
      const deleteResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/authorization/menus') &&
        response.request().method() === 'DELETE'
      );
      await page.getByRole('button', { name: '确定' }).click();
      const deleteResponse = await deleteResponsePromise;
      const deleteBody = await deleteResponse.json();
      expect(deleteResponse.status()).toBe(200);
      expect(deleteBody.success || deleteBody.code === 200).toBeTruthy();
      await expect(page.getByText('删除成功')).toBeVisible({ timeout: 10000 });
      await expect(page.locator('.el-table__row', { hasText: editedMenuName })).toHaveCount(0);
      await expectNoAuthError(page);
    } finally {
      await cleanupMenus(request, platformToken, menuName).catch(() => undefined);
      await cleanupMenus(request, platformToken, editedMenuName).catch(() => undefined);
    }
  });

  test('A 公司不可见菜单管理入口，维护接口返回 403', async ({ page, request }) => {
    const companyToken = await loginToken(request, companyATenant);

    const listResponse = await request.get('http://localhost:5555/authorization/menus?appCode=internal-admin&fmt=list', {
      headers: { Authorization: `Bearer ${companyToken}` },
    });
    expect(listResponse.status()).toBe(403);

    const createResponse = await request.post('http://localhost:5555/authorization/menus', {
      headers: { Authorization: `Bearer ${companyToken}` },
      data: {
        appCode: 'internal-admin',
        parentId: 1,
        menuType: 2,
        menuName: `无权菜单${Date.now()}`,
        path: '/system/deny-menu',
        component: 'views/error/404',
        sort: 0,
        status: 1,
        visible: 1,
      },
    });
    expect(createResponse.status()).toBe(403);

    await loginPage(page, companyATenant);
    await expect(page.getByText('账号权限').first()).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('平台运营')).toHaveCount(0);
    await expect(page.getByText('菜单管理')).toHaveCount(0);
    await expectNoAuthError(page);
  });
});
