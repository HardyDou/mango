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

async function loginPage(page: Page) {
  await page.goto('/#/login');
  await page.fill('input[placeholder="请输入用户名"]', 'admin');
  await page.fill('input[placeholder="请输入密码"]', 'admin123');
  const accountTenantsResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/api/auth/login-institutions') && response.status() === 200,
  );
  await page.locator('input[placeholder="请输入密码"]').blur();
  await accountTenantsResponsePromise;
  await expect(page.locator('.tenant-select')).toHaveCount(0);
  await page.locator('.login-btn').click();
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

async function getJson(request: APIRequestContext, token: string, path: string) {
  const response = await request.get(e2eApi(`${path}`), {
    headers: { Authorization: `Bearer ${token}` },
  });
  const body = await response.json();
  return { response, body };
}

async function expectNoAuthError(page: Page) {
  await expect(page.locator('.el-message--error')).toHaveCount(0);
  await expect(page.locator('text=/401|403|未授权|没有权限|拒绝访问|加载失败|登录已过期|请重新登录/')).toHaveCount(0);
}

async function expandMenuGroup(page: Page, name: string) {
  const group = page.getByRole('menuitem', { name: new RegExp(name) }).first();
  await expect(group).toBeVisible({ timeout: 10000 });
  const expanded = await group.getAttribute('aria-expanded');
  if (expanded !== 'true') {
    await group.click();
  }
}

async function openTopMenu(page: Page, name: string) {
  const button = page.getByRole('button', { name }).first();
  await expect(button).toBeVisible({ timeout: 10000 });
  await button.evaluate((element: HTMLElement) => element.click());
}

function sideMenu(page: Page) {
  return page.locator('.layout-sidebar, aside, [role="menubar"]').first();
}

test.describe('T3 默认单租户平台元数据复核', () => {
  test('登录选项仅包含默认租户，平台元数据不被租户 SQL 误过滤', async ({ request }) => {
    const platformToken = await loginToken(request, platformTenant);

    const loginOptions = await request.get(e2eApi('/system/tenant/login-options'));
    expect(loginOptions.status()).toBe(200);
    const loginOptionsBody = await loginOptions.json();
    expect(loginOptionsBody.data).toEqual([platformTenant]);

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
      '/system/dict/data/options?typeCode=sys_normal_disable',
      '/system/area/tree?level=1',
      '/authorization/menus/user?fmt=tree',
    ]) {
      const { response, body } = await getJson(request, platformToken, path);
      expect(response.status(), path).toBe(200);
      expect(body.success || body.code === 200, path).toBeTruthy();
      expect(body.data.length, path).toBeGreaterThan(0);
    }
  });

  test('默认租户可见平台管理入口但不展示租户管理', async ({ page }) => {
    await loginPage(page);
    await openTopMenu(page, '系统管理');
    await expect(page.getByText('权限管理').first()).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('应用管理').first()).toBeVisible();
    await expect(page.getByText('字典管理').first()).toBeVisible();
    await expect(page.getByText('行政区划').first()).toBeVisible();

    await expandMenuGroup(page, '权限管理');
    await expect(sideMenu(page).getByText('租户管理')).toHaveCount(0);
    await expect(page.getByText('菜单管理').first()).toBeVisible();
    await expect(page.getByText('套餐管理').first()).toBeVisible();

    await openTopMenu(page, '平台能力');
    await expect(page.getByText('审批管理').first()).toBeVisible();
    await expectNoAuthError(page);
  });
});
