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

async function listTenants(request: APIRequestContext, token: string) {
  const response = await request.get('http://localhost:5555/system/tenant/list', {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.success || body.code === 200).toBeTruthy();
  return body.data || [];
}

async function cleanupTenant(request: APIRequestContext, token: string, tenantCode: string) {
  const tenants = await listTenants(request, token);
  for (const tenant of tenants.filter((item: any) => item.tenantCode === tenantCode)) {
    await request.put(`http://localhost:5555/system/tenant/status?id=${tenant.id}&status=0`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  }
}

async function expectLoginOption(request: APIRequestContext, tenantName: string, visible: boolean) {
  const response = await request.get('http://localhost:5555/system/tenant/login-options');
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.success || body.code === 200).toBeTruthy();
  const names = (body.data || []).map((item: any) => item.tenantName);
  if (visible) {
    expect(names).toContain(tenantName);
  } else {
    expect(names).not.toContain(tenantName);
  }
}

async function expectLoginDenied(request: APIRequestContext, tenantCode: string) {
  const response = await request.post('http://localhost:5555/auth/login', {
    data: {
      username: 'admin',
      password: 'admin123',
      tenantCode,
      realm: 'INTERNAL',
      actorType: 'INTERNAL_USER',
      partyType: 'INTERNAL_ORG',
      appCode: 'internal-admin',
    },
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.success).toBeFalsy();
  expect(body.code).toBe(1403);
  expect(body.msg || body.message).toContain('机构不存在');
}

async function expectNoAuthError(page: Page) {
  await expect(page.locator('.el-message--error')).toHaveCount(0);
  await expect(page.locator('text=/401|403|未授权|没有权限|拒绝访问|加载失败|登录已过期|请重新登录/')).toHaveCount(0);
}

test.describe('T7 机构管理页面真实接口闭环', () => {
  test('机构类型与开通能力可持久化并在页面显示中文标签', async ({ page, request }) => {
    const unique = Date.now();
    const tenantName = `E2E担保机构${unique}`;
    const tenantCode = `e2e_guarantee_${unique}`;
    const platformToken = await loginToken(request, platformTenant);

    try {
      await cleanupTenant(request, platformToken, tenantCode);
      const createResponse = await request.post('http://localhost:5555/system/tenant', {
        headers: { Authorization: `Bearer ${platformToken}` },
        data: {
          tenantName,
          tenantCode,
          institutionType: 'GUARANTEE',
          capabilityCodes: 'GUARANTEE_BUSINESS,WORKFLOW',
          status: 1,
          contact: 'E2E能力管理员',
          mobile: '13900000003',
          email: `${tenantCode}@example.com`,
        },
      });
      expect(createResponse.status()).toBe(200);
      const createBody = await createResponse.json();
      expect(createBody.success || createBody.code === 200).toBeTruthy();

      const tenants = await listTenants(request, platformToken);
      const created = tenants.find((item: any) => item.tenantCode === tenantCode);
      expect(created).toBeTruthy();
      expect(created.institutionType).toBe('GUARANTEE');
      expect(created.capabilityCodes).toBe('GUARANTEE_BUSINESS,WORKFLOW');

      await loginPage(page, platformTenant);
      const listResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/system/tenant/list') && response.status() === 200
      );
      await page.goto('/#/system/tenant');
      await listResponsePromise;
      await page.getByLabel('关键词').fill(tenantName);
      await page.getByRole('button', { name: '查询' }).click();

      const row = page.locator('.el-table__row', { hasText: tenantName }).first();
      await expect(row).toBeVisible({ timeout: 10000 });
      await expect(row.getByText('担保机构', { exact: true })).toBeVisible();
      await expect(row.getByText('保函业务', { exact: true })).toBeVisible();
      await expect(row.getByText('流程管理', { exact: true })).toBeVisible();
      await expectNoAuthError(page);
    } finally {
      await cleanupTenant(request, platformToken, tenantCode);
    }
  });

  test('平台管理员可新增、编辑、禁用、启用机构，初始化后删除被依赖检查阻断', async ({ page, request }) => {
    const unique = Date.now();
    const tenantName = `E2E机构管理${unique}`;
    const tenantCode = `e2e_manage_${unique}`;
    const editedContact = `E2E联系人${unique}`;
    const platformToken = await loginToken(request, platformTenant);

    try {
      await cleanupTenant(request, platformToken, tenantCode);
      await loginPage(page, platformTenant);

      const listResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/system/tenant/list') && response.status() === 200
      );
      await page.goto('/#/system/tenant');
      await listResponsePromise;
      await expect(page.getByText('机构管理').first()).toBeVisible({ timeout: 10000 });
      await expectNoAuthError(page);

      await page.getByRole('button', { name: '新增机构' }).click();
      const createDialog = page.getByRole('dialog', { name: '新增机构' });
      await expect(createDialog).toBeVisible();
      await createDialog.getByLabel('机构名称').fill(tenantName);
      await createDialog.getByLabel('机构编码').fill(tenantCode);
      await createDialog.getByLabel('联系人').fill('E2E管理员');
      await createDialog.getByLabel('联系电话').fill('13900000001');
      await createDialog.getByLabel('联系邮箱').fill(`${tenantCode}@example.com`);

      const createResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/system/tenant') &&
        response.request().method() === 'POST' &&
        response.status() === 200
      );
      await createDialog.getByRole('button', { name: '确定' }).click();
      await createResponsePromise;
      await expect(page.getByText('新增成功')).toBeVisible({ timeout: 10000 });

      await page.getByLabel('关键词').fill(tenantName);
      const searchResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/system/tenant/list') && response.status() === 200
      );
      await page.getByRole('button', { name: '查询' }).click();
      await searchResponsePromise;
      const row = page.locator('.el-table__row', { hasText: tenantName }).first();
      await expect(row).toBeVisible({ timeout: 10000 });
      await expect(row.getByText(tenantCode, { exact: true })).toBeVisible();
      await expect(row.getByText('E2E管理员', { exact: true })).toBeVisible();
      await expect(row.getByText('13900000001', { exact: true })).toBeVisible();

      await row.getByRole('button', { name: '编辑' }).click();
      const editDialog = page.getByRole('dialog', { name: '编辑机构' });
      await expect(editDialog).toBeVisible();
      await editDialog.getByLabel('联系人').fill(editedContact);
      await editDialog.getByLabel('联系电话').fill('13900000002');
      await editDialog.getByLabel('联系邮箱').fill(`${tenantCode}-edit@example.com`);

      const updateResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/system/tenant') &&
        response.request().method() === 'PUT' &&
        response.status() === 200
      );
      await editDialog.getByRole('button', { name: '确定' }).click();
      await updateResponsePromise;
      await expect(page.getByText('修改成功')).toBeVisible({ timeout: 10000 });
      await expect(page.locator('.el-table__row', { hasText: editedContact })).toBeVisible({ timeout: 10000 });

      const editedRow = page.locator('.el-table__row', { hasText: tenantName }).first();
      const disableResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/system/tenant/status') &&
        response.request().method() === 'PUT' &&
        response.status() === 200
      );
      await editedRow.getByRole('button', { name: '禁用' }).click();
      await disableResponsePromise;
      await expect(page.getByText('禁用成功')).toBeVisible({ timeout: 10000 });
      await expectLoginOption(request, tenantName, false);
      await expectLoginDenied(request, tenantCode);

      const disabledRow = page.locator('.el-table__row', { hasText: tenantName }).first();
      const enableResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/system/tenant/status') &&
        response.request().method() === 'PUT' &&
        response.status() === 200
      );
      await disabledRow.getByRole('button', { name: '启用' }).click();
      await enableResponsePromise;
      await expect(page.getByText('启用成功')).toBeVisible({ timeout: 10000 });
      await expectLoginOption(request, tenantName, true);

      const enabledRow = page.locator('.el-table__row', { hasText: tenantName }).first();
      await enabledRow.getByRole('button', { name: '删除' }).click();
      await expect(page.getByText('确认删除该机构?')).toBeVisible();
      const deleteResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/system/tenant') &&
        response.request().method() === 'DELETE' &&
        response.status() === 200
      );
      await page.getByRole('button', { name: '确定' }).click();
      const deleteResponse = await deleteResponsePromise;
      const deleteBody = await deleteResponse.json();
      expect(deleteBody.success).toBeFalsy();
      expect(deleteBody.code).toBe(2409);
      expect(deleteBody.msg || deleteBody.message).toContain('不能直接删除');
      await expect(page.getByText(/不能直接删除|已有组织架构数据|已有成员数据|有关联角色/)).toBeVisible({ timeout: 10000 });
      await expect(page.locator('.el-table__row', { hasText: tenantName })).toBeVisible();
      await expectLoginOption(request, tenantName, true);
      await expectNoAuthError(page);
    } finally {
      await cleanupTenant(request, platformToken, tenantCode);
    }
  });

  test('A 公司不可见机构管理入口，维护接口返回 403', async ({ page, request }) => {
    const companyToken = await loginToken(request, companyATenant);

    for (const operation of [
      request.get('http://localhost:5555/system/tenant/list', {
        headers: { Authorization: `Bearer ${companyToken}` },
      }),
      request.post('http://localhost:5555/system/tenant', {
        headers: { Authorization: `Bearer ${companyToken}` },
        data: {
          tenantName: '非法机构',
          tenantCode: `deny_${Date.now()}`,
          status: 1,
          contact: '无权用户',
        },
      }),
    ]) {
      const response = await operation;
      expect(response.status()).toBe(403);
      const body = await response.json();
      expect(body.code).toBe(403);
    }

    await loginPage(page, companyATenant);
    await expect(page.getByText('账号权限').first()).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('平台运营')).toHaveCount(0);
    await expect(page.getByText('机构管理')).toHaveCount(0);
    await expectNoAuthError(page);
  });
});
