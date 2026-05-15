import { expect, test, type APIRequestContext, type Page } from '@playwright/test';

async function loginTokenAsPlatform(request: APIRequestContext) {
  const response = await request.post('http://localhost:5555/auth/login', {
    data: {
      username: 'admin',
      password: 'admin123',
      tenantCode: 'default',
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

async function loginToken(request: APIRequestContext, tenantCode: string) {
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
  expect(response.ok()).toBeTruthy();
  const body = await response.json();
  expect(body.success || body.code === 200).toBeTruthy();
  return body.data;
}

async function createTenant(request: APIRequestContext, token: string) {
  const unique = Date.now();
  const tenantCode = `e2e_tenant_${unique}`;
  const tenantName = `E2E机构${unique}`;
  const response = await request.post('http://localhost:5555/system/tenant', {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      tenantName,
      tenantCode,
      status: 1,
      contact: 'E2E管理员',
      mobile: '13900000001',
      email: `tenant-${unique}@example.com`,
    },
  });
  expect(response.ok()).toBeTruthy();
  const body = await response.json();
  expect(body.success || body.code === 200).toBeTruthy();
  expect(body.data).toBeTruthy();
  return { tenantId: String(body.data), tenantCode, tenantName };
}

async function loginByUi(page: Page, tenantName: string) {
  await page.goto('/#/login');
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: new RegExp(tenantName) }).click();
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  await page.click('button:has-text("登 录")');
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

async function expectNoAuthError(page: Page) {
  await expect(page.locator('.el-message--error')).toHaveCount(0);
  await expect(page.locator('text=/401|403|未授权|没有权限|拒绝访问|加载失败|登录已过期|请重新登录/')).toHaveCount(0);
}

test.describe('T6 新机构初始化闭环', () => {
  test('平台新增机构后创建者可直接登录并加载菜单', async ({ page, request }) => {
    const platformToken = await loginTokenAsPlatform(request);
    const tenant = await createTenant(request, platformToken);

    const loginData = await loginToken(request, tenant.tenantCode);
    expect(String(loginData.tenantId)).toBe(tenant.tenantId);
    expect(loginData.tenantCode).toBe(tenant.tenantCode);
    expect(String(loginData.partyId)).toBe(tenant.tenantId);
    expect(loginData.roles).toContain('ROLE_ADMIN');
    expect(loginData.permissions.length).toBeGreaterThan(0);

    const menusResponse = await request.get(
      'http://localhost:5555/authorization/menus/user?appCode=internal-admin&fmt=tree',
      { headers: { Authorization: `Bearer ${loginData.accessToken}` } }
    );
    expect(menusResponse.ok()).toBeTruthy();
    const menusBody = await menusResponse.json();
    expect(menusBody.success || menusBody.code === 200).toBeTruthy();
    expect(menusBody.data?.length).toBeGreaterThan(0);

    await loginByUi(page, tenant.tenantName);
    await expect(page.getByText('系统管理').first()).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('权限管理').first()).toBeVisible({ timeout: 10000 });
    await expectNoAuthError(page);
  });
});
