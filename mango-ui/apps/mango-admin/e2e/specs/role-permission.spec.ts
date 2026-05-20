import { expect, test, type Page, type APIRequestContext } from '@playwright/test';

async function loginAsCompanyA(page: Page) {
  await page.goto('/#/login');
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: /A公司/ }).click();
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  await page.click('button:has-text("登 录")');
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

async function loginTokenAsCompanyA(request: APIRequestContext) {
  const response = await request.post('http://localhost:5555/auth/login', {
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

async function cleanupRole(request: APIRequestContext, token: string, roleCode: string) {
  const rolesResponse = await request.get('http://localhost:5555/authorization/roles', {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!rolesResponse.ok()) return;
  const body = await rolesResponse.json();
  const roles = body.data || [];
  for (const role of roles.filter((item: any) => item.roleCode === roleCode)) {
    await request.delete(`http://localhost:5555/authorization/roles?id=${role.roleId}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  }
}

async function expectNoAuthError(page: Page) {
  await expect(page.locator('.el-message--error')).toHaveCount(0);
  await expect(page.locator('text=/401|403|未授权|没有权限|拒绝访问|加载失败|登录已过期|请重新登录/')).toHaveCount(0);
}

test.describe('T2 角色授权闭环', () => {
  test('A 公司可维护本租户角色，且不能授权平台级菜单能力', async ({ page, request }) => {
    const unique = Date.now();
    const roleCode = `E2E_ROLE_${unique}`;
    const roleName = `E2E角色${unique}`;
    const editedRoleName = `${roleName}-编辑`;
    const token = await loginTokenAsCompanyA(request);

    await cleanupRole(request, token, roleCode);

    await loginAsCompanyA(page);
    await expect(page.getByText('审批中心')).toHaveCount(0);
    await expect(page.getByText('机构管理')).toHaveCount(0);
    await expect(page.getByText('应用管理')).toHaveCount(0);
    await expect(page.getByText('菜单管理')).toHaveCount(0);

    const roleListResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/authorization/roles') && response.request().method() === 'GET' && response.status() === 200
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

    const createResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/authorization/roles') && response.request().method() === 'POST' && response.status() === 200
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

    const updateResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/authorization/roles') && response.request().method() === 'PUT' && response.status() === 200
    );
    await editDialog.getByRole('button', { name: '确定' }).click();
    await updateResponsePromise;
    await expect(page.getByText('修改成功')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText(editedRoleName)).toBeVisible({ timeout: 10000 });

    const editedRow = page.locator('.el-table__row', { hasText: roleCode }).first();
    const assignableMenusResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/authorization/roles/assignable-menus') && response.status() === 200
    );
    await editedRow.getByRole('button', { name: '分配权限' }).click();
    const assignDialog = page.getByRole('dialog', { name: '分配角色权限' });
    await expect(assignDialog).toBeVisible();
    await assignableMenusResponsePromise;
    await expect(assignDialog.getByText('角色管理')).toBeVisible({ timeout: 10000 });
    await expect(assignDialog.getByText('组织架构')).toBeVisible();
    await expect(assignDialog.getByText('岗位管理')).toBeVisible();
    await expect(assignDialog.getByText('机构管理')).toHaveCount(0);
    await expect(assignDialog.getByText('应用管理')).toHaveCount(0);
    await expect(assignDialog.getByText('套餐管理')).toHaveCount(0);

    const roleManageNode = assignDialog.locator('.el-tree-node', { hasText: '角色管理' }).first();
    await roleManageNode.locator('.el-checkbox').first().click();
    const assignResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/authorization/roles/menus') && response.request().method() === 'POST' && response.status() === 200
    );
    await assignDialog.getByRole('button', { name: '确定' }).click();
    const assignResponse = await assignResponsePromise;
    const assignBody = await assignResponse.json();
    expect(assignBody.success || assignBody.code === 200).toBeTruthy();
    await expect(page.getByText('分配成功')).toBeVisible({ timeout: 10000 });

    const latestRolesResponse = await request.get('http://localhost:5555/authorization/roles', {
      headers: { Authorization: `Bearer ${token}` },
    });
    const latestRolesBody = await latestRolesResponse.json();
    const createdRole = latestRolesBody.data.find((item: any) => item.roleCode === roleCode);
    expect(createdRole).toBeTruthy();

    const invalidAssignResponse = await request.post('http://localhost:5555/authorization/roles/menus', {
      headers: { Authorization: `Bearer ${token}` },
      data: { roleId: createdRole.roleId, menuIds: [14] },
    });
    const invalidAssignBody = await invalidAssignResponse.json();
    expect(invalidAssignBody.success).toBeFalsy();
    expect(invalidAssignBody.code).toBe(403);

    const deleteRow = page.locator('.el-table__row', { hasText: roleCode }).first();
    await deleteRow.getByRole('button', { name: '删除' }).click();
    await expect(page.getByText(`确认删除角色「${editedRoleName}」?`)).toBeVisible();
    const deleteResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/authorization/roles') && response.request().method() === 'DELETE' && response.status() === 200
    );
    const reloadAfterDeletePromise = page.waitForResponse((response) =>
      response.url().includes('/api/authorization/roles') && response.request().method() === 'GET' && response.status() === 200
    );
    await page.getByRole('button', { name: '确定' }).click();
    await deleteResponsePromise;
    await reloadAfterDeletePromise;
    await expect(page.getByText('删除成功')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('.el-table__row', { hasText: roleCode })).toHaveCount(0);
    await expectNoAuthError(page);
  });
});
