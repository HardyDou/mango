import { expect, test } from '@playwright/test';

async function login(page: import('@playwright/test').Page) {
  await page.goto('/#/login');
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  await page.click('button:has-text("登 录")');
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

function namedDialog(page: import('@playwright/test').Page, title: string) {
  return page.locator('.el-dialog').filter({ hasText: title }).last();
}

test.describe('应用管理保存联调', () => {
  test('编辑应用保存使用真实接口并保持登录态', async ({ page }) => {
    await login(page);

    const appListResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/authorization/apps') && response.request().method() === 'GET' && response.status() === 200
    );
    await page.goto('/#/system/app');
    const appListResponse = await appListResponsePromise;
    const appListBody = await appListResponse.json();
    expect(appListBody.success || appListBody.code === 200).toBeTruthy();
    expect(typeof appListBody.data?.[0]?.appId).toBe('string');
    expect(typeof appListBody.data?.[0]?.loginContexts?.[0]?.contextId).toBe('string');

    await expect(page.getByText('应用管理').first()).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('内部管理后台').first()).toBeVisible({ timeout: 10000 });

    const row = page.locator('.el-table__body-wrapper tr', { hasText: 'internal-admin' }).first();
    await row.getByRole('button', { name: '编辑' }).click();
    const editDialog = namedDialog(page, '编辑应用');
    await expect(editDialog).toBeVisible();

    const updateResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/authorization/apps') && response.request().method() === 'PUT'
    );
    await editDialog.getByRole('button', { name: '确定' }).click();
    const updateResponse = await updateResponsePromise;
    const requestBody = updateResponse.request().postDataJSON();
    const updateBody = await updateResponse.json();

    expect(updateResponse.status()).toBe(200);
    expect(updateBody.success || updateBody.code === 200).toBeTruthy();
    expect(updateBody.data).toBe(true);
    expect(typeof requestBody.appId).toBe('string');
    expect(typeof requestBody.loginContexts?.[0]?.contextId).toBe('string');
    expect(requestBody.createTime).toBeUndefined();
    expect(requestBody.updateTime).toBeUndefined();
    expect(requestBody.loginContexts?.[0]?.createTime).toBeUndefined();
    expect(requestBody.loginContexts?.[0]?.updateTime).toBeUndefined();

    await expect(page.getByText('修改成功')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('.el-message--error')).toHaveCount(0);
    await expect(page.locator('text=/401|未登录|未授权|登录已过期|请重新登录|应用入口不存在/')).toHaveCount(0);
  });

  test('新增、选择图标、编辑、删除应用主流程正常', async ({ page }) => {
    await login(page);

    const suffix = Date.now();
    const appCode = `e2e-app-${suffix}`;
    const appName = `E2E临时应用${suffix}`;
    const updatedName = `${appName}-已编辑`;

    await page.goto('/#/system/app');
    await expect(page.getByText('应用管理').first()).toBeVisible({ timeout: 10000 });

    await page.getByRole('button', { name: '新增应用' }).click();
    const createDialog = namedDialog(page, '新增应用');
    await expect(createDialog).toBeVisible();
    await createDialog.locator('input').nth(0).fill(appName);
    await createDialog.locator('input').nth(1).fill(appCode);

    await createDialog.getByRole('button', { name: '选择' }).click();
    const iconDialog = page.getByRole('dialog', { name: '选择图标' });
    await expect(iconDialog).toBeVisible();
    await iconDialog.getByPlaceholder('搜索图标').fill('Home');
    await iconDialog.locator('.icon-item', { hasText: 'Home' }).first().click();
    await expect(iconDialog).toBeHidden();

    const createResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/authorization/apps') && response.request().method() === 'POST'
    );
    await createDialog.getByRole('button', { name: '确定' }).click();
    const createResponse = await createResponsePromise;
    const createRequestBody = createResponse.request().postDataJSON();
    const createBody = await createResponse.json();
    expect(createResponse.status()).toBe(200);
    expect(createBody.success || createBody.code === 200).toBeTruthy();
    expect(createRequestBody.appCode).toBe(appCode);
    expect(createRequestBody.icon).toBe('Home');
    expect(createRequestBody.loginContexts?.[0]?.realm).toBe('INTERNAL');
    expect(createRequestBody.loginContexts?.[0]?.actorType).toBe('INTERNAL_USER');
    await expect(page.getByText('新增成功')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText(appName).first()).toBeVisible({ timeout: 10000 });

    const row = page.locator('.el-table__body-wrapper tr', { hasText: appCode }).first();
    await row.getByRole('button', { name: '编辑' }).click();
    const editDialog = namedDialog(page, '编辑应用');
    await expect(editDialog).toBeVisible();
    await editDialog.locator('input').nth(0).fill(updatedName);

    const updateResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/authorization/apps') && response.request().method() === 'PUT'
    );
    await editDialog.getByRole('button', { name: '确定' }).click();
    const updateResponse = await updateResponsePromise;
    const updateRequestBody = updateResponse.request().postDataJSON();
    const updateBody = await updateResponse.json();
    expect(updateResponse.status()).toBe(200);
    expect(updateBody.success || updateBody.code === 200).toBeTruthy();
    expect(updateRequestBody.appCode).toBe(appCode);
    expect(updateRequestBody.appName).toBe(updatedName);
    expect(typeof updateRequestBody.appId).toBe('string');
    expect(updateRequestBody.createTime).toBeUndefined();
    expect(updateRequestBody.updateTime).toBeUndefined();
    await expect(page.getByText('修改成功')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText(updatedName).first()).toBeVisible({ timeout: 10000 });

    const updatedRow = page.locator('.el-table__body-wrapper tr', { hasText: appCode }).first();
    await updatedRow.getByRole('button', { name: '删除' }).click();
    await expect(page.getByText(`确认删除应用「${updatedName}」?`)).toBeVisible();
    const deleteResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/authorization/apps') && response.request().method() === 'DELETE'
    );
    await page.getByRole('button', { name: '确定' }).last().click();
    const deleteResponse = await deleteResponsePromise;
    const deleteBody = await deleteResponse.json();
    expect(deleteResponse.status()).toBe(200);
    expect(deleteBody.success || deleteBody.code === 200).toBeTruthy();
    await expect(page.getByText('删除成功')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText(appCode)).toHaveCount(0);

    await expect(page.locator('.el-message--error')).toHaveCount(0);
    await expect(page.locator('text=/401|未登录|未授权|登录已过期|请重新登录|应用入口不存在/')).toHaveCount(0);
  });
});
