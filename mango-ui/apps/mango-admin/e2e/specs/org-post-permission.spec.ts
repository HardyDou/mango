import { expect, test, type Page } from '@playwright/test';

async function loginDefaultTenant(page: Page) {
  await page.goto('/#/login');
  await page.getByPlaceholder('请输入用户名').fill('admin');
  await page.getByPlaceholder('请输入密码').fill('admin123');
  await page.getByPlaceholder('请输入密码').blur();
  await expect(page.locator('.tenant-select')).toHaveCount(0);
  await page.getByRole('button', { name: '登录' }).click();
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

async function expectNoAuthError(page: Page) {
  await expect(page.locator('.el-message--error')).toHaveCount(0);
  await expect(page.locator('text=/401|403|未授权|没有权限|拒绝访问|加载失败|登录已过期|请重新登录/')).toHaveCount(0);
}

test.describe('T1 组织岗位权限闭环', () => {
  test('默认租户可访问两级组织和岗位，岗位新增编辑删除可用', async ({ page }) => {
    const unique = Date.now();
    const postName = `E2E岗位${unique}`;
    const editedPostName = `${postName}-编辑`;
    const postCode = `E2E_POST_${unique}`;

    await loginDefaultTenant(page);

    await expect(page.getByText('租户管理')).toHaveCount(0);

    const orgResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/api/org/tree') && response.status() === 200,
    );
    await page.goto('/#/system/org');
    await orgResponsePromise;
    await expect(page.getByText('组织架构').first()).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('组织详情').first()).toBeVisible();
    await expect(page.getByText('请选择左侧组织')).toBeVisible();
    await expect(page.getByText('A公司').first()).toBeVisible();
    await expect(page.getByText('B公司').first()).toBeVisible();
    await expectNoAuthError(page);

    const postPageResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/api/post/page') && response.status() === 200,
    );
    await page.goto('/#/system/post');
    await postPageResponsePromise;
    await expect(page.getByText('岗位管理').first()).toBeVisible({ timeout: 10000 });
    await expectNoAuthError(page);

    await page.getByRole('button', { name: '新增岗位' }).click();
    const createDialog = page.getByRole('dialog', { name: '新增岗位' });
    await expect(createDialog).toBeVisible();
    await createDialog.getByLabel('岗位名称').fill(postName);
    await createDialog.getByLabel('岗位编码').fill(postCode);
    await createDialog.getByLabel('备注').fill('T1 E2E 临时岗位');

    const createResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/api/post') && response.request().method() === 'POST' && response.status() === 200,
    );
    await page.getByRole('button', { name: '确定' }).click();
    await createResponsePromise;
    await expect(page.getByText('新增成功')).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('dialog', { name: '新增岗位' })).toHaveCount(0);
    await expect(page.getByText(postName)).toBeVisible({ timeout: 10000 });
    await expectNoAuthError(page);

    const row = page.locator('.el-table__row', { hasText: postCode }).first();
    await row.getByRole('button', { name: '编辑' }).click();
    const editDialog = page.getByRole('dialog', { name: '编辑岗位' });
    await expect(editDialog).toBeVisible();
    await editDialog.getByLabel('岗位名称').fill(editedPostName);

    const updateResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/api/post') && response.request().method() === 'PUT' && response.status() === 200,
    );
    await page.getByRole('button', { name: '确定' }).click();
    await updateResponsePromise;
    await expect(page.getByText('修改成功')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText(editedPostName)).toBeVisible({ timeout: 10000 });
    await expectNoAuthError(page);

    const editedRow = page.locator('.el-table__row', { hasText: postCode }).first();
    await editedRow.getByRole('button', { name: '删除' }).click();
    await expect(page.getByText(`确认删除岗位「${editedPostName}」?`)).toBeVisible();

    const deleteResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/api/post') && response.request().method() === 'DELETE' && response.status() === 200,
    );
    await page.getByRole('button', { name: '确定' }).click();
    await deleteResponsePromise;
    await expect(page.getByText('删除成功')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText(postCode)).toHaveCount(0);
    await expectNoAuthError(page);
  });
});
