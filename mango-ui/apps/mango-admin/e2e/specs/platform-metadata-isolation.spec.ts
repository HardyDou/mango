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

async function getJson(request: APIRequestContext, token: string, path: string) {
  const response = await request.get(`http://localhost:5555${path}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const body = await response.json();
  return { response, body };
}

async function expectNoAuthError(page: Page) {
  await expect(page.locator('.el-message--error')).toHaveCount(0);
  await expect(page.locator('text=/401|403|未授权|没有权限|拒绝访问|加载失败|登录已过期|请重新登录/')).toHaveCount(0);
}

async function clearAuthState(page: Page) {
  await page.context().clearCookies();
  await page.evaluate(() => {
    localStorage.clear();
    sessionStorage.clear();
  });
}

async function expandMenuGroup(page: Page, name: string) {
  const group = page.getByRole('menuitem', { name: new RegExp(name) }).first();
  await expect(group).toBeVisible({ timeout: 10000 });
  const expanded = await group.getAttribute('aria-expanded');
  if (expanded !== 'true') {
    await group.click();
  }
}

test.describe('T3 平台元数据隔离复核', () => {
  test('平台元数据不被机构 SQL 误过滤，普通机构无维护权限', async ({ request }) => {
    const platformToken = await loginToken(request, platformTenant);
    const companyAToken = await loginToken(request, companyATenant);

    const loginOptions = await request.get('http://localhost:5555/system/tenant/login-options');
    expect(loginOptions.status()).toBe(200);
    const loginOptionsBody = await loginOptions.json();
    const tenantNames = loginOptionsBody.data.map((item: LoginTenant) => item.tenantName);
    expect(tenantNames).toEqual(expect.arrayContaining(['芒果集团', 'A公司', 'B公司', 'C公司']));

    for (const path of [
      '/authorization/apps',
      '/authorization/menus?fmt=tree',
      '/system/dict/type/list',
      '/system/dict/data/list',
    ]) {
      const { response, body } = await getJson(request, platformToken, path);
      expect(response.status(), path).toBe(200);
      expect(body.success || body.code === 200, path).toBeTruthy();
      expect(body.data.length, path).toBeGreaterThan(0);
    }

    for (const path of [
      '/authorization/apps',
      '/authorization/menus?fmt=tree',
      '/system/dict/type/list',
      '/system/dict/data/list',
    ]) {
      const { response, body } = await getJson(request, companyAToken, path);
      expect(response.status(), path).toBe(403);
      expect(body.code, path).toBe(403);
    }

    for (const path of [
      '/system/dict/data/options?typeCode=sys_normal_disable',
      '/system/area/tree?level=1',
      '/authorization/menus/user?fmt=tree',
    ]) {
      const { response, body } = await getJson(request, companyAToken, path);
      expect(response.status(), path).toBe(200);
      expect(body.success || body.code === 200, path).toBeTruthy();
      expect(body.data.length, path).toBeGreaterThan(0);
    }
  });

  test('平台机构可见系统与审批中心入口，A 公司不可见平台维护入口', async ({ page }) => {
    await loginPage(page, platformTenant);
    await expect(page.getByText('权限管理').first()).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('审批中心').first()).toBeVisible();
    await expect(page.getByText('应用管理').first()).toBeVisible();
    await expect(page.getByText('字典管理').first()).toBeVisible();
    await expect(page.getByText('行政区划').first()).toBeVisible();

    await expandMenuGroup(page, '权限管理');
    await expect(page.getByText('机构管理').first()).toBeVisible();
    await expect(page.getByText('菜单管理').first()).toBeVisible();
    await expect(page.getByText('套餐管理').first()).toBeVisible();
    await expectNoAuthError(page);

    await clearAuthState(page);
    await loginPage(page, companyATenant);
    await expect(page.getByText('权限管理').first()).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('日志管理').first()).toBeVisible();
    await expect(page.getByText('应用管理')).toHaveCount(0);
    await expect(page.getByText('机构管理')).toHaveCount(0);
    await expect(page.getByText('菜单管理')).toHaveCount(0);
    await expect(page.getByText('套餐管理')).toHaveCount(0);
    await expect(page.getByText('字典管理')).toHaveCount(0);
    await expect(page.getByText('行政区划')).toHaveCount(0);
    await expectNoAuthError(page);
  });
});
