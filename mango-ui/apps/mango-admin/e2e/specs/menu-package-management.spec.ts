import { expect, test, type APIRequestContext, type Locator, type Page } from '@playwright/test';

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
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  const accountTenantsResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/auth/login-institutions') && response.status() === 200
  );
  await page.locator('input[placeholder="密码"]').blur();
  await accountTenantsResponsePromise;
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: new RegExp(tenant.tenantName) }).click();
  await page.locator('.login-btn').click();
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

async function listPackages(request: APIRequestContext, token: string, keyword?: string) {
  const response = await request.get('http://localhost:5555/authorization/menu-packages', {
    headers: { Authorization: `Bearer ${token}` },
    params: {
      appCode: 'internal-admin',
      keyword,
    },
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.success || body.code === 200).toBeTruthy();
  return body.data || [];
}

async function getPackageDetail(request: APIRequestContext, token: string, packageId: number) {
  const response = await request.get('http://localhost:5555/authorization/menu-packages/detail', {
    headers: { Authorization: `Bearer ${token}` },
    params: { packageId },
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.success || body.code === 200).toBeTruthy();
  return body.data;
}

async function cleanupPackages(request: APIRequestContext, token: string, packageCode: string) {
  const packages = await listPackages(request, token, packageCode);
  for (const item of packages.filter((row: any) => row.packageCode === packageCode)) {
    await request.delete(`http://localhost:5555/authorization/menu-packages?packageId=${item.packageId}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  }
}

async function checkPackageMenuNode(dialog: Locator, name: string) {
  const treeNode = dialog
    .locator('.package-tree .el-tree-node__content')
    .filter({ hasText: new RegExp(`^${name}$`) })
    .first();
  await expect(treeNode).toBeVisible({ timeout: 10000 });
  await treeNode.locator('.el-checkbox').click();
}

test.describe('套餐管理页面真实接口闭环', () => {
  test('平台管理员可新增、编辑、删除套餐并预览菜单树', async ({ page, request }) => {
    const unique = Date.now();
    const packageName = `E2E套餐${unique}`;
    const editedPackageName = `${packageName}-编辑`;
    const packageCode = `e2e_package_${unique}`;
    const token = await loginToken(request, platformTenant);

    try {
      await cleanupPackages(request, token, packageCode);
      await loginPage(page, platformTenant);

      const listResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/authorization/menu-packages') &&
        response.request().method() === 'GET' &&
        response.status() === 200
      );
      await page.goto('/#/system/menu-package');
      await listResponsePromise;
      await expect(page.getByText('套餐管理').first()).toBeVisible({ timeout: 10000 });

      await page.getByRole('button', { name: '新增套餐' }).click();
      const createDialog = page.getByRole('dialog', { name: '新增套餐' });
      await expect(createDialog).toBeVisible();
      await createDialog.getByLabel('套餐名称').fill(packageName);
      await createDialog.getByLabel('套餐编码').fill(packageCode);
      await checkPackageMenuNode(createDialog, '角色管理');
      await checkPackageMenuNode(createDialog, '日志管理');

      const createResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/authorization/menu-packages') &&
        response.request().method() === 'POST' &&
        response.status() === 200
      );
      await createDialog.getByRole('button', { name: '确定' }).click();
      await createResponsePromise;
      await expect(page.getByText('新增成功')).toBeVisible({ timeout: 10000 });

      await page.getByLabel('关键词').fill(packageCode);
      await page.getByRole('button', { name: '查询' }).click();
      const row = page.locator('.el-table__row', { hasText: packageCode }).first();
      await expect(row).toBeVisible({ timeout: 10000 });

      await row.getByRole('button', { name: '预览' }).click();
      await expect(page.getByText('套餐菜单预览').first()).toBeVisible({ timeout: 10000 });
      await expect(page.locator('.el-drawer').getByText('角色管理').first()).toBeVisible();
      await page.keyboard.press('Escape');

      const createdPackage = (await listPackages(request, token, packageCode)).find((item: any) => item.packageCode === packageCode);
      expect(createdPackage?.packageId).toBeTruthy();
      const packageDetail = await getPackageDetail(request, token, createdPackage.packageId);
      const updateResponse = await request.put('http://localhost:5555/authorization/menu-packages', {
        headers: { Authorization: `Bearer ${token}` },
        data: {
          packageId: packageDetail.packageId,
          packageName: editedPackageName,
          packageCode: packageDetail.packageCode,
          appCode: packageDetail.appCode,
          status: packageDetail.status,
          sort: packageDetail.sort,
          remark: packageDetail.remark,
          menuIds: packageDetail.menuIds,
        },
      });
      expect(updateResponse.status()).toBe(200);
      const updateBody = await updateResponse.json();
      expect(updateBody.success || updateBody.code === 200).toBeTruthy();

      const refreshAfterUpdatePromise = page.waitForResponse((response) =>
        response.url().includes('/api/authorization/menu-packages') &&
        response.request().method() === 'GET' &&
        response.status() === 200
      );
      await page.getByRole('button', { name: '查询' }).click();
      await refreshAfterUpdatePromise;
      await expect(page.locator('.el-table__row', { hasText: editedPackageName })).toBeVisible({ timeout: 10000 });

      const editedRow = page.locator('.el-table__row', { hasText: packageCode }).first();
      const deleteResponse = await request.delete(`http://localhost:5555/authorization/menu-packages?packageId=${createdPackage.packageId}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      expect(deleteResponse.status()).toBe(200);
      const deleteBody = await deleteResponse.json();
      expect(deleteBody.success || deleteBody.code === 200).toBeTruthy();

      const refreshAfterDeletePromise = page.waitForResponse((response) =>
        response.url().includes('/api/authorization/menu-packages') &&
        response.request().method() === 'GET' &&
        response.status() === 200
      );
      await page.getByRole('button', { name: '查询' }).click();
      await refreshAfterDeletePromise;
      await expect(editedRow).toHaveCount(0);
    } finally {
      await cleanupPackages(request, token, packageCode).catch(() => undefined);
    }
  });

  test('A 公司不可见套餐管理入口，维护接口返回 403', async ({ page, request }) => {
    const token = await loginToken(request, companyATenant);
    const listResponse = await request.get('http://localhost:5555/authorization/menu-packages', {
      headers: { Authorization: `Bearer ${token}` },
      params: { appCode: 'internal-admin' },
    });
    expect(listResponse.status()).toBe(403);

    await loginPage(page, companyATenant);
    await expect(page.getByText('权限管理').first()).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('套餐管理')).toHaveCount(0);
  });
});
