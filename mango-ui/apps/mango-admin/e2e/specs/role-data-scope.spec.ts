import { expect, test, type APIRequestContext, type Page } from '@playwright/test';
import { api as e2eApi } from '../support/api';

type RoleVO = {
  roleId: string;
  roleCode: string;
  roleName: string;
};

type SysMenuVO = {
  menuId: string | number;
  menuName: string;
  permissions?: string;
  children?: SysMenuVO[];
};

const DATA_SCOPE_RESOURCE_CODE = 'authorization:role:list';
const DATA_SCOPE_RESOURCE_NAME = '角色列表';

async function loginTokenAsCompanyA(request: APIRequestContext) {
  const response = await request.post(e2eApi('/auth/login'), {
    data: {
      username: 'admin',
      password: 'admin123',
      tenantId: '2',
      tenantCode: 'company_a',
      realm: 'INTERNAL',
      actorType: 'INTERNAL_USER',
      partyType: 'INTERNAL_ORG',
      appCode: 'internal-admin',
    },
  });
  expect(response.ok()).toBeTruthy();
  const body = await response.json();
  return body.data.accessToken as string;
}

async function loginAsCompanyA(page: Page) {
  await page.goto('/#/login');
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: /A公司/ }).click();
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  await page.click('button:has-text("登 录")');
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

async function findRole(request: APIRequestContext, token: string, roleCode: string): Promise<RoleVO | undefined> {
  const rolesResponse = await request.get(e2eApi('/authorization/roles'), {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(rolesResponse.ok()).toBeTruthy();
  const body = await rolesResponse.json();
  return (body.data || []).find((item: RoleVO) => item.roleCode === roleCode);
}

async function cleanupRole(request: APIRequestContext, token: string, roleCode: string) {
  const role = await findRole(request, token, roleCode);
  if (!role?.roleId) return;
  await request.delete(e2eApi('/authorization/data-scopes/roles'), {
    headers: { Authorization: `Bearer ${token}` },
    params: {
      roleId: role.roleId,
      resourceCode: DATA_SCOPE_RESOURCE_CODE,
    },
  });
  await request.delete(e2eApi('/authorization/roles'), {
    headers: { Authorization: `Bearer ${token}` },
    params: { id: role.roleId },
  });
}

async function createRole(request: APIRequestContext, token: string, roleCode: string, roleName: string) {
  const response = await request.post(e2eApi('/authorization/roles'), {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      appCode: 'internal-admin',
      realm: 'INTERNAL',
      actorType: 'INTERNAL_USER',
      roleCode,
      roleName,
      roleType: 1,
      status: 1,
      sort: 0,
      remark: '角色数据权限 E2E 临时角色',
    },
  });
  expect(response.ok()).toBeTruthy();
  const body = await response.json();
  expect(body.success || body.code === 200).toBeTruthy();
  const role = await findRole(request, token, roleCode);
  expect(role, `未找到临时角色 ${roleCode}`).toBeTruthy();
  return role!;
}

async function grantRoleResource(request: APIRequestContext, token: string, roleId: string, resourceCode: string) {
  const menusResponse = await request.get(e2eApi('/authorization/roles/assignable-menus'), {
    headers: { Authorization: `Bearer ${token}` },
    params: { appCode: 'internal-admin' },
  });
  expect(menusResponse.ok()).toBeTruthy();
  const menusBody = await menusResponse.json();
  const menuIds = findMenuPathByPermission(menusBody.data || [], resourceCode);
  expect(menuIds, `未找到资源 ${resourceCode} 对应的可授权菜单`).toBeTruthy();

  const assignResponse = await request.post(e2eApi('/authorization/roles/menus'), {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      roleId,
      menuIds,
    },
  });
  expect(assignResponse.ok()).toBeTruthy();
  const assignBody = await assignResponse.json();
  expect(assignBody.success || assignBody.code === 200).toBeTruthy();
}

function findMenuPathByPermission(menus: SysMenuVO[], resourceCode: string, ancestors: string[] = []): string[] | undefined {
  for (const menu of menus) {
    const currentPath = [...ancestors, String(menu.menuId)];
    const permissions = (menu.permissions || '')
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean);
    if (permissions.includes(resourceCode)) {
      return currentPath;
    }
    const childPath = findMenuPathByPermission(menu.children || [], resourceCode, currentPath);
    if (childPath) {
      return childPath;
    }
  }
  return undefined;
}

async function expectNoAuthError(page: Page) {
  await expect(page.locator('.el-message--error')).toHaveCount(0);
  await expect(page.locator('text=/401|403|未授权|没有权限|拒绝访问|加载失败|登录已过期|请重新登录/')).toHaveCount(0);
}

test.describe('角色数据权限管理', () => {
  test('A 公司可在角色页选择资源并保存、回显、删除数据权限配置', async ({ page, request }, testInfo) => {
    test.setTimeout(120000);

    const unexpectedConsoleErrors: string[] = [];
    const pageErrors: string[] = [];
    const failedRequests: string[] = [];
    const badResponses: string[] = [];

    page.on('console', (message) => {
      if (message.type() === 'error') {
        const location = message.location();
        const locationText = location.url ? ` (${location.url}:${location.lineNumber})` : '';
        unexpectedConsoleErrors.push(`${message.text()}${locationText}`);
      }
    });
    page.on('pageerror', (error) => {
      pageErrors.push(error.message);
    });
    page.on('requestfailed', (requestItem) => {
      failedRequests.push(`${requestItem.method()} ${requestItem.url()} ${requestItem.failure()?.errorText || ''}`);
    });
    page.on('response', (response) => {
      if (response.status() >= 400) {
        badResponses.push(`${response.status()} ${response.request().method()} ${response.url()}`);
      }
    });

    const unique = Date.now();
    const roleCode = `E2E_DATA_SCOPE_${unique}`;
    const roleName = `E2E数据权限角色${unique}`;
    const token = await loginTokenAsCompanyA(request);

    await cleanupRole(request, token, roleCode);
    const role = await createRole(request, token, roleCode, roleName);
    await grantRoleResource(request, token, role.roleId, DATA_SCOPE_RESOURCE_CODE);

    try {
      await loginAsCompanyA(page);

      const roleListResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/authorization/roles') &&
        response.request().method() === 'GET' &&
        response.status() === 200
      );
      await page.goto('/#/system/role');
      await roleListResponsePromise;
      await expect(page.getByText('角色管理').first()).toBeVisible({ timeout: 10000 });
      await expect(page.locator('.el-table__row', { hasText: roleCode })).toBeVisible({ timeout: 10000 });

      const row = page.locator('.el-table__row', { hasText: roleCode }).first();
      const initialScopesResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/authorization/data-scopes/roles') &&
        response.request().method() === 'GET' &&
        response.status() === 200
      );
      const assignableMenusResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/authorization/roles/assignable-menus') &&
        response.request().method() === 'GET' &&
        response.status() === 200
      );
      await row.getByRole('button', { name: '数据权限' }).click();
      await initialScopesResponsePromise;
      await assignableMenusResponsePromise;

      const dialog = page.getByRole('dialog', { name: '角色数据权限' });
      await expect(dialog).toBeVisible();
      await expect(dialog.getByText(roleName)).toBeVisible();
      await expect(dialog.getByRole('button', { name: '新增数据权限' })).toBeVisible();
      await dialog.getByRole('button', { name: '新增数据权限' }).click();
      await expect(dialog.locator('[data-test="data-scope-resource-tree"]')).toBeVisible();
      await expect(dialog.getByRole('button', { name: '保存' })).toBeVisible();

      await dialog.locator('[data-test="data-scope-resource-tree"]').click();
      await page
        .locator('.el-select-dropdown .el-tree-node__content', {
          hasText: new RegExp(`${DATA_SCOPE_RESOURCE_NAME}.*${DATA_SCOPE_RESOURCE_CODE}`),
        })
        .last()
        .click();
      await page.keyboard.press('Escape');
      await dialog.locator('.el-segmented__item').filter({ hasText: /^本人部门$/ }).click();
      const editingSaveButton = dialog
        .locator('[data-test="data-scope-row-save"]:visible')
        .first();
      await expect(editingSaveButton).toBeVisible();

      const saveResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/authorization/data-scopes/roles') &&
        response.request().method() === 'POST' &&
        response.status() === 200
      );
      const reloadAfterSavePromise = page.waitForResponse((response) =>
        response.url().includes('/api/authorization/data-scopes/roles') &&
        response.request().method() === 'GET' &&
        response.status() === 200
      );
      await editingSaveButton.click();
      const saveResponse = await saveResponsePromise;
      await reloadAfterSavePromise;
      const saveBody = await saveResponse.json();
      expect(saveBody.success || saveBody.code === 200).toBeTruthy();
      await expect(page.getByText('保存成功')).toBeVisible({ timeout: 10000 });

      await expect(dialog.locator('.el-table__row', { hasText: DATA_SCOPE_RESOURCE_CODE })).toBeVisible({ timeout: 10000 });
      await expect(dialog.locator('.el-table__row', { hasText: DATA_SCOPE_RESOURCE_CODE }).getByText(DATA_SCOPE_RESOURCE_NAME)).toBeVisible();
      await expect(dialog.locator('.el-table__row', { hasText: DATA_SCOPE_RESOURCE_CODE }).getByText('本人部门')).toBeVisible();
      await expect(dialog.locator('.el-table__row', { hasText: DATA_SCOPE_RESOURCE_CODE }).getByText('-')).toBeVisible();
      await expect(page.getByText('保存成功')).toHaveCount(0, { timeout: 10000 });

      const dataScopeRow = dialog.locator('.el-table__row', { hasText: DATA_SCOPE_RESOURCE_CODE }).first();
      await dataScopeRow.getByRole('button', { name: '编辑' }).click();
      await expect(dataScopeRow.getByRole('button', { name: '保存' })).toBeVisible();
      await expect(dataScopeRow.locator('.el-segmented__item').filter({ hasText: /^本人部门$/ })).toBeVisible();

      const editSaveResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/authorization/data-scopes/roles') &&
        response.request().method() === 'POST' &&
        response.status() === 200
      );
      const reloadAfterEditPromise = page.waitForResponse((response) =>
        response.url().includes('/api/authorization/data-scopes/roles') &&
        response.request().method() === 'GET' &&
        response.status() === 200
      );
      await dataScopeRow.locator('[data-test="data-scope-row-save"]:visible').click();
      const editSaveResponse = await editSaveResponsePromise;
      await reloadAfterEditPromise;
      const editSaveBody = await editSaveResponse.json();
      expect(editSaveBody.success || editSaveBody.code === 200).toBeTruthy();
      await expect(page.getByText('保存成功')).toHaveCount(0, { timeout: 10000 });

      const listedScopesResponse = await request.get(e2eApi('/authorization/data-scopes/roles'), {
        headers: { Authorization: `Bearer ${token}` },
        params: { roleId: role.roleId },
      });
      expect(listedScopesResponse.ok()).toBeTruthy();
      const listedScopesBody = await listedScopesResponse.json();
      expect(listedScopesBody.data).toEqual(
        expect.arrayContaining([
          expect.objectContaining({
            resourceCode: DATA_SCOPE_RESOURCE_CODE,
            scopeMode: 'SELF_ORG',
            status: 1,
          }),
        ]),
      );

      const screenshotPath = testInfo.outputPath('role-data-scope-dialog.png');
      await dialog.screenshot({ path: screenshotPath });
      await testInfo.attach('role-data-scope-dialog', {
        path: screenshotPath,
        contentType: 'image/png',
      });

      await dialog.locator('.el-table__row', { hasText: DATA_SCOPE_RESOURCE_CODE }).getByRole('button', { name: '删除' }).click();
      await expect(page.getByText(`确认删除资源「${DATA_SCOPE_RESOURCE_CODE}」的数据权限配置?`)).toBeVisible();

      const deleteResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/authorization/data-scopes/roles') &&
        response.request().method() === 'DELETE' &&
        response.status() === 200
      );
      const reloadAfterDeletePromise = page.waitForResponse((response) =>
        response.url().includes('/api/authorization/data-scopes/roles') &&
        response.request().method() === 'GET' &&
        response.status() === 200
      );
      await page.getByRole('button', { name: '确定' }).click();
      const deleteResponse = await deleteResponsePromise;
      await reloadAfterDeletePromise;
      const deleteBody = await deleteResponse.json();
      expect(deleteBody.success || deleteBody.code === 200).toBeTruthy();
      await expect(page.getByText('删除成功')).toBeVisible({ timeout: 10000 });
      await expect(dialog.locator('.el-table__row', { hasText: DATA_SCOPE_RESOURCE_CODE })).toHaveCount(0);

      await expectNoAuthError(page);
      expect(pageErrors, `Uncaught page errors:\n${pageErrors.join('\n')}`).toHaveLength(0);
      expect(unexpectedConsoleErrors, `Console errors:\n${unexpectedConsoleErrors.join('\n')}`).toHaveLength(0);
      expect(failedRequests, `Failed requests:\n${failedRequests.join('\n')}`).toHaveLength(0);
      expect(badResponses, `HTTP 4xx/5xx responses:\n${badResponses.join('\n')}`).toHaveLength(0);
    } finally {
      await cleanupRole(request, token, roleCode).catch(() => {});
    }
  });
});
