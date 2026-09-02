import { expect, test, type Page, type APIRequestContext } from '@playwright/test';
import { api as e2eApi } from '../support/api';
import { collectBrowserDiagnostics } from '../support/browser-diagnostics';
import { elementPlusTreeItemCheckbox } from '../support/element-plus';

async function loginDefaultTenant(page: Page) {
  await page.goto('/#/login');
  await page.getByPlaceholder('请输入用户名').fill('admin');
  await page.getByPlaceholder('请输入密码').fill('admin123');
  await page.getByPlaceholder('请输入密码').blur();
  await expect(page.locator('.tenant-select')).toHaveCount(0);
  await page.getByRole('button', { name: '登录' }).click();
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

async function loginTokenAsDefaultTenant(request: APIRequestContext) {
  const response = await request.post(e2eApi('/auth/login'), {
    data: {
      username: 'admin',
      password: 'admin123',
      tenantId: '1',
      tenantCode: 'default',
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

async function platformMenuId(request: APIRequestContext, menuCode: string) {
  const loginResponse = await request.post(e2eApi('/auth/login'), {
    data: {
      username: 'admin',
      password: 'admin123',
      tenantId: '1',
      tenantCode: 'default',
      realm: 'INTERNAL',
      actorType: 'INTERNAL_USER',
      partyType: 'INTERNAL_ORG',
      appCode: 'internal-admin',
    },
  });
  expect(loginResponse.ok()).toBeTruthy();
  const loginBody = await loginResponse.json();
  const response = await request.get(e2eApi('/authorization/menus'), {
    headers: { Authorization: `Bearer ${loginBody.data.accessToken}` },
    params: { appCode: 'internal-admin', fmt: 'list' },
  });
  expect(response.ok()).toBeTruthy();
  const body = await response.json();
  const menu = (body.data || []).find((item: any) => item.menuCode === menuCode);
  expect(menu, `未找到平台菜单 ${menuCode}`).toBeTruthy();
  return menu.menuId as string;
}

async function cleanupRole(request: APIRequestContext, token: string, roleCode: string) {
  const rolesResponse = await request.get(e2eApi('/authorization/roles'), {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!rolesResponse.ok()) return;
  const body = await rolesResponse.json();
  const roles = body.data || [];
  for (const role of roles.filter((item: any) => item.roleCode === roleCode)) {
    await request.delete(e2eApi(`/authorization/roles?id=${role.roleId}`), {
      headers: { Authorization: `Bearer ${token}` },
    });
  }
}

async function expectNoAuthError(page: Page) {
  await expect(page.locator('.el-message--error')).toHaveCount(0);
  await expect(page.locator('text=/401|403|未授权|没有权限|拒绝访问|加载失败|登录已过期|请重新登录/')).toHaveCount(0);
}

test.describe('T2 角色授权闭环', () => {
  test('角色菜单回显只选授权叶子并保存必要祖先 @p0 @rbac', async ({ page }, testInfo) => {
    const diagnostics = collectBrowserDiagnostics(page);
    let assignRequestBody: { roleId: string; menuIds: string[] } | undefined;
    const menuTree = [
      {
        menuId: '1',
        parentId: '0',
        menuType: 1,
        menuName: '权限管理',
        children: [
          {
            menuId: '10',
            parentId: '1',
            menuType: 2,
            menuName: '角色管理',
            children: [
              { menuId: '100', parentId: '10', menuType: 3, menuName: '查询角色' },
              { menuId: '101', parentId: '10', menuType: 3, menuName: '删除角色' },
            ],
          },
        ],
      },
    ];

    await page.route('**/api/authorization/roles/assignable-menus**', async (route) => {
      await route.fulfill({ json: { code: 200, success: true, data: menuTree } });
    });
    await page.route('**/api/authorization/roles/menus**', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({ json: { code: 200, success: true, data: ['1', '10', '100'] } });
        return;
      }
      assignRequestBody = route.request().postDataJSON();
      await route.fulfill({ json: { code: 200, success: true, data: true } });
    });

    await loginDefaultTenant(page);
    await page.goto('/#/system/role');
    const rolePage = page.locator('[data-page="role.management"]');
    await expect(rolePage).toBeVisible({ timeout: 10000 });
    const adminRole = rolePage.getByRole('row').filter({ hasText: 'ROLE_ADMIN' });
    await expect(adminRole).toHaveCount(1);
    await adminRole.getByRole('button', { name: '分配权限' }).click();

    const assignDialog = page.getByRole('dialog', { name: '分配角色权限' });
    const tree = assignDialog.locator('[data-surface="role.menu-assignment"]');
    await expect(tree).toBeVisible();

    await expect(elementPlusTreeItemCheckbox(tree, '查询角色')).toBeChecked();
    await expect(elementPlusTreeItemCheckbox(tree, '删除角色')).not.toBeChecked();
    await expect(elementPlusTreeItemCheckbox(tree, '角色管理')).toBeChecked({ indeterminate: true });
    await expect(elementPlusTreeItemCheckbox(tree, '权限管理')).toBeChecked({ indeterminate: true });
    await testInfo.attach('issue-918-role-menu-hydration', {
      body: await assignDialog.screenshot(),
      contentType: 'image/png',
    });

    await assignDialog.locator('[data-action="role.menu.save"]').click();
    await expect(page.getByText('分配成功')).toBeVisible({ timeout: 10000 });
    const submittedMenuIds = assignRequestBody?.menuIds ?? [];
    expect(submittedMenuIds).toHaveLength(3);
    expect(submittedMenuIds).toEqual(expect.arrayContaining(['100', '10', '1']));
    expect(diagnostics, diagnostics.join('\n')).toEqual([]);
  });

  test('默认租户可维护角色，且隐藏的租户管理不能被授权 @p0 @rbac', async ({ page, request }) => {
    const unique = Date.now();
    const roleCode = `E2E_ROLE_${unique}`;
    const roleName = `E2E角色${unique}`;
    const editedRoleName = `${roleName}-编辑`;
    const token = await loginTokenAsDefaultTenant(request);
    const tenantManagementMenuId = await platformMenuId(request, 'system:tenant');

    await cleanupRole(request, token, roleCode);

    await loginDefaultTenant(page);
    await expect(page.getByText('租户管理')).toHaveCount(0);

    const roleListResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/api/authorization/roles') &&
        response.request().method() === 'GET' &&
        response.status() === 200,
    );
    await page.goto('/#/system/role');
    await roleListResponsePromise;
    await expect(page.getByText('角色管理').first()).toBeVisible({ timeout: 10000 });
    await expectNoAuthError(page);

    await page.getByRole('button', { name: '新增角色' }).click();
    const createDialog = page.getByRole('dialog', { name: '新增角色' });
    await expect(createDialog).toBeVisible();
    await createDialog.getByLabel('角色名称').fill(roleName);
    await createDialog.getByLabel('角色编码').fill(roleCode);
    await createDialog.getByLabel('备注').fill('T2 E2E 临时角色');

    const createResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/api/authorization/roles') &&
        response.request().method() === 'POST' &&
        response.status() === 200,
    );
    await createDialog.getByRole('button', { name: '确定' }).click();
    await createResponsePromise;
    await expect(page.getByText('新增成功')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText(roleName)).toBeVisible({ timeout: 10000 });

    const row = page.locator('.el-table__row', { hasText: roleCode }).first();
    await row.getByRole('button', { name: '编辑' }).click();
    const editDialog = page.getByRole('dialog', { name: '编辑角色' });
    await expect(editDialog).toBeVisible();
    await editDialog.getByLabel('角色名称').fill(editedRoleName);

    const updateResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/api/authorization/roles') &&
        response.request().method() === 'PUT' &&
        response.status() === 200,
    );
    await editDialog.getByRole('button', { name: '确定' }).click();
    await updateResponsePromise;
    await expect(page.getByText('修改成功')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText(editedRoleName)).toBeVisible({ timeout: 10000 });

    const editedRow = page.locator('.el-table__row', { hasText: roleCode }).first();
    const assignableMenusResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/api/authorization/roles/assignable-menus') && response.status() === 200,
    );
    await editedRow.getByRole('button', { name: '分配权限' }).click();
    const assignDialog = page.getByRole('dialog', { name: '分配角色权限' });
    await expect(assignDialog).toBeVisible();
    await assignableMenusResponsePromise;
    await expect(assignDialog.getByText('角色管理')).toBeVisible({ timeout: 10000 });
    await expect(assignDialog.getByText('组织架构')).toBeVisible();
    await expect(assignDialog.getByText('岗位管理')).toBeVisible();
    await expect(assignDialog.getByText('租户管理')).toHaveCount(0);

    const roleManageNode = assignDialog.locator('.el-tree-node', { hasText: '角色管理' }).first();
    await roleManageNode.locator('.el-checkbox').first().click();
    const assignResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/api/authorization/roles/menus') &&
        response.request().method() === 'POST' &&
        response.status() === 200,
    );
    await assignDialog.getByRole('button', { name: '确定' }).click();
    const assignResponse = await assignResponsePromise;
    const assignBody = await assignResponse.json();
    expect(assignBody.success || assignBody.code === 200).toBeTruthy();
    await expect(page.getByText('分配成功')).toBeVisible({ timeout: 10000 });

    const latestRolesResponse = await request.get(e2eApi('/authorization/roles'), {
      headers: { Authorization: `Bearer ${token}` },
    });
    const latestRolesBody = await latestRolesResponse.json();
    const createdRole = latestRolesBody.data.find((item: any) => item.roleCode === roleCode);
    expect(createdRole).toBeTruthy();

    const invalidAssignResponse = await request.post(e2eApi('/authorization/roles/menus'), {
      headers: { Authorization: `Bearer ${token}` },
      data: { roleId: createdRole.roleId, menuIds: [tenantManagementMenuId] },
    });
    const invalidAssignBody = await invalidAssignResponse.json();
    expect(invalidAssignBody.success).toBeFalsy();
    expect(invalidAssignBody.code).toBe(403);

    const deleteRow = page.locator('.el-table__row', { hasText: roleCode }).first();
    await deleteRow.getByRole('button', { name: '删除' }).click();
    await expect(page.getByText(`确认删除角色「${editedRoleName}」?`)).toBeVisible();
    const deleteResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/api/authorization/roles') &&
        response.request().method() === 'DELETE' &&
        response.status() === 200,
    );
    const reloadAfterDeletePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/api/authorization/roles') &&
        response.request().method() === 'GET' &&
        response.status() === 200,
    );
    await page.getByRole('button', { name: '确定' }).click();
    await deleteResponsePromise;
    await reloadAfterDeletePromise;
    await expect(page.getByText('删除成功')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('.el-table__row', { hasText: roleCode })).toHaveCount(0);
    await expectNoAuthError(page);
  });
});
