import { expect, test, type APIRequestContext, type Page } from '@playwright/test';

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

async function cleanupUser(request: APIRequestContext, token: string, username: string) {
  const pageResponse = await request.get(
    `http://localhost:5555/identity/users/page?page=1&size=20&username=${encodeURIComponent(username)}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  if (!pageResponse.ok()) return;

  const body = await pageResponse.json();
  const records = body.data?.records || body.data?.list || [];
  for (const user of records.filter((item: any) => item.username === username)) {
    await request.delete(`http://localhost:5555/identity/users?userId=${user.userId}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  }
}

async function expectNoAuthError(page: Page) {
  await expect(page.locator('.el-message--error')).toHaveCount(0);
  await expect(page.locator('text=/401|403|未授权|没有权限|拒绝访问|加载失败|登录已过期|请重新登录/')).toHaveCount(0);
}

test.describe('T5 成员管理能力闭环', () => {
  test('A 公司可维护机构成员并分配角色', async ({ page, request }) => {
    const unique = Date.now();
    const username = `E2E_USER_${unique}`;
    const nickname = `E2E用户${unique}`;
    const editedNickname = `${nickname}-编辑`;
    const password = `E2E@${unique}`;
    const resetPassword = `RESET@${unique}`;
    const token = await loginTokenAsCompanyA(request);

    await cleanupUser(request, token, username);

    await loginAsCompanyA(page);

    const pageResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/identity/users/page') && response.status() === 200
    );
    await page.goto('/#/system/user');
    await pageResponsePromise;
    await expect(page.getByText('成员管理').first()).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('admin').first()).toBeVisible({ timeout: 10000 });
    await expectNoAuthError(page);

    await page.getByRole('button', { name: '新增成员' }).click();
    const createDialog = page.getByRole('dialog', { name: '新增成员' });
    await expect(createDialog).toBeVisible();
    await createDialog.getByLabel('用户名').fill(username);
    await createDialog.getByLabel('初始密码').fill(password);
    await createDialog.getByLabel('昵称').fill(nickname);
    await createDialog.getByLabel('手机号').fill('13800000001');
    await createDialog.getByLabel('邮箱').fill(`${username.toLowerCase()}@example.com`);
    await createDialog.getByLabel('备注').fill('T5 E2E 临时用户');

    const createResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/identity/users') && response.request().method() === 'POST' && response.status() === 200
    );
    await createDialog.getByRole('button', { name: '确定' }).click();
    await createResponsePromise;
    await expect(page.getByText('新增成功')).toBeVisible({ timeout: 10000 });

    const searchForm = page.locator('.search-form');
    await searchForm.getByLabel('用户名').fill(username);
    const searchResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/identity/users/page') && response.status() === 200
    );
    await searchForm.getByRole('button', { name: '查询' }).click();
    await searchResponsePromise;
    await expect(page.locator('.el-table__row', { hasText: username })).toBeVisible({ timeout: 10000 });
    await expect(page.getByText(nickname)).toBeVisible();

    const row = page.locator('.el-table__row', { hasText: username }).first();
    await row.getByRole('button', { name: '编辑' }).click();
    const editDialog = page.getByRole('dialog', { name: '编辑成员' });
    await expect(editDialog).toBeVisible();
    await editDialog.getByLabel('昵称').fill(editedNickname);
    await editDialog.getByLabel('手机号').fill('13800000002');

    const updateResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/identity/users') && response.request().method() === 'PUT' && response.status() === 200
    );
    await editDialog.getByRole('button', { name: '确定' }).click();
    await updateResponsePromise;
    await expect(page.getByText('修改成功')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText(editedNickname)).toBeVisible({ timeout: 10000 });

    const editedRow = page.locator('.el-table__row', { hasText: username }).first();
    await editedRow.getByRole('button', { name: '禁用' }).click();
    await expect(page.getByText(`确认禁用成员「${username}」?`)).toBeVisible();
    const disableResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/identity/users/status') && response.request().method() === 'PUT' && response.status() === 200
    );
    await page.getByRole('button', { name: '确定' }).click();
    await disableResponsePromise;
    await expect(page.getByText('禁用成功')).toBeVisible({ timeout: 10000 });

    const disabledRow = page.locator('.el-table__row', { hasText: username }).first();
    await disabledRow.getByRole('button', { name: '启用' }).click();
    await expect(page.getByText(`确认启用成员「${username}」?`)).toBeVisible();
    const enableResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/identity/users/status') && response.request().method() === 'PUT' && response.status() === 200
    );
    await page.getByRole('button', { name: '确定' }).click();
    await enableResponsePromise;
    await expect(page.getByText('启用成功')).toBeVisible({ timeout: 10000 });

    const enabledRow = page.locator('.el-table__row', { hasText: username }).first();
    await enabledRow.getByRole('button', { name: '重置密码' }).click();
    await expect(page.getByRole('dialog', { name: '重置密码' })).toBeVisible();
    await page.getByRole('dialog', { name: '重置密码' }).locator('input').fill(resetPassword);
    const resetResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/identity/users/password/reset') && response.request().method() === 'PUT' && response.status() === 200
    );
    await page.getByRole('button', { name: '确定' }).click();
    await resetResponsePromise;
    await expect(page.getByText('重置成功')).toBeVisible({ timeout: 10000 });

    const latestRow = page.locator('.el-table__row', { hasText: username }).first();
    await latestRow.getByRole('button', { name: '分配角色' }).click();
    const assignDialog = page.getByRole('dialog', { name: '分配成员角色' });
    await expect(assignDialog).toBeVisible();
    await expect(assignDialog.getByText('超级管理员（ROLE_ADMIN）')).toBeVisible({ timeout: 10000 });
    await assignDialog.locator('.el-checkbox').first().click();
    const assignResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/authorization/roles/subjects') &&
      response.request().method() === 'POST' &&
      response.status() === 200
    );
    await assignDialog.getByRole('button', { name: '确定' }).click();
    await assignResponsePromise;
    await expect(page.getByText('分配成功')).toBeVisible({ timeout: 10000 });

    const deleteRow = page.locator('.el-table__row', { hasText: username }).first();
    await deleteRow.getByRole('button', { name: '删除' }).click();
    await expect(page.getByText(`确认移除成员「${username}」?`)).toBeVisible();
    const deleteResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/identity/users') && response.request().method() === 'DELETE' && response.status() === 200
    );
    await page.getByRole('button', { name: '确定' }).click();
    await deleteResponsePromise;
    await expect(page.getByText('移除成功')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('.el-table__row', { hasText: username })).toHaveCount(0);
    await expectNoAuthError(page);

    await cleanupUser(request, token, username);
  });
});
