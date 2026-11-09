import { expect, test } from '@playwright/test';

async function login(page: import('@playwright/test').Page) {
  await page.goto('/#/login');
  const tenantSelect = page.locator('.tenant-select');
  if (await tenantSelect.count()) {
    await tenantSelect.click();
    await page.getByRole('option', { name: /芒果集团/ }).click();
  }
  await page.locator('input[placeholder="用户名"], input[placeholder="请输入用户名"]').first().fill('admin');
  await page.locator('input[placeholder="密码"], input[placeholder="请输入密码"]').first().fill('admin123');
  await page.getByRole('button', { name: /登\s*录/ }).click();
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

test.describe('集成应用连接器模型', () => {
  test('@p0 @authorization 应用定义只读清单展示规范与能力', async ({ page }) => {
    const failedResponses: string[] = [];
    page.on('response', (response) => {
      if (response.status() >= 400) failedResponses.push(`${response.status()} ${response.url()}`);
    });

    await login(page);
    await page.goto('/#/integration/definition');
    await expect(page.locator('[data-page="integration.definition"]')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('[data-surface="search"]')).toBeVisible();
    await expect(page.locator('.el-table__body-wrapper tr', { hasText: 'wecom-message' }).first()).toBeVisible();
    await expect(page.getByRole('button', { name: '新增连接器' })).toBeVisible();
    await expect(page.getByRole('button', { name: '新增连接器' })).toBeVisible();

    await page
      .locator('.el-table__body-wrapper tr', { hasText: 'wecom-message' })
      .first()
      .locator('[data-action="connector.view"]')
      .click();
    const dialog = page.locator('.el-dialog').filter({ hasText: '连接器定义' }).last();
    await expect(dialog).toBeVisible();
    await expect(dialog).toContainText('配置规范');
    await expect(dialog).toContainText('认证规范');
    await expect(dialog).toContainText('corpSecret');
    await dialog.getByRole('button', { name: '关闭' }).click();

    expect(failedResponses).toEqual([]);
  });

  test('@p1 @authorization 连接器可通过可视化表格新增并直接保存', async ({ page }) => {
    const failedResponses: string[] = [];
    page.on('response', (response) => {
      if (response.status() >= 400) failedResponses.push(`${response.status()} ${response.url()}`);
    });

    await login(page);
    await page.goto('/#/integration/definition');
    await expect(page.locator('[data-page="integration.definition"]')).toBeVisible({ timeout: 10000 });

    const connectorCode = `demo-connector-${Date.now()}`;
    await page.getByRole('button', { name: '新增连接器' }).click();
    const dialog = page.locator('.el-dialog').filter({ hasText: '新增连接器' }).last();
    await dialog.locator('[data-field="connector.code"]').fill(connectorCode);
    await dialog.locator('[data-field="connector.name"]').fill('E2E 连接器');

    await dialog.getByRole('tab', { name: '配置规范' }).click();
    await dialog.getByRole('button', { name: '新增字段' }).first().click();
    const configRow = dialog.locator('.el-table__body-wrapper tr').first();
    await configRow.locator('input[placeholder="如 接口地址"]').fill('接口地址');
    await configRow.locator('input[placeholder="如 apiBaseUrl"]').fill('endpoint');
    await configRow.locator('td').nth(4).locator('input').fill('https://example.com');

    const saveResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/api/integration/connectors') &&
        response.request().method() === 'POST' &&
        !response.url().includes('/detail') &&
        !response.url().includes('/page'),
    );
    await dialog.locator('[data-action="connector.submit"]').click();
    const saveResponse = await saveResponsePromise;
    expect(saveResponse.status()).toBe(200);
    expect((await saveResponse.json()).data).toBe(true);

    const row = page.locator('.el-table__body-wrapper tr', { hasText: connectorCode }).first();
    await expect(row).toBeVisible({ timeout: 10000 });
    await expect(row).toContainText('启用');
    await expect(row.getByRole('button', { name: '编辑' })).toBeEnabled();

    expect(failedResponses).toEqual([]);
  });

  test('@p0 @authorization 按连接器动态渲染表单创建实例并加密保存凭证', async ({ page }) => {
    const consoleErrors: string[] = [];
    const failedResponses: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') consoleErrors.push(message.text());
    });
    page.on('response', (response) => {
      if (response.status() >= 400) failedResponses.push(`${response.status()} ${response.url()}`);
    });

    await login(page);
    await page.goto('/#/integration/application');
    await expect(page.locator('[data-page="integration.application"]')).toBeVisible({ timeout: 10000 });

    const instanceCode = `wecom-e2e-${Date.now()}`;
    await page.getByRole('button', { name: '新增集成应用' }).click();
    const dialog = page.locator('.el-dialog').filter({ hasText: '新增集成应用' }).last();
    await expect(dialog).toBeVisible();

    await dialog.locator('[data-field="integration-instance.instance-code"]').fill(instanceCode);
    await dialog.locator('[data-field="integration-instance.instance-name"]').fill('E2E 企业微信应用');
    await dialog.locator('[data-field="integration-instance.connector"]').click();
    await page.locator('.el-select-dropdown__item:visible').filter({ hasText: '企业微信应用消息' }).first().click();

    await expect(dialog).toContainText('连接器配置');
    await expect(dialog).toContainText('凭证');
    await expect(dialog.locator('[data-field="integration-instance.credential.corpId"]')).toBeVisible();
    await expect(dialog.locator('[data-field="integration-instance.credential.corpSecret"]')).toBeVisible();

    await dialog.locator('[data-field="integration-instance.credential.corpId"]').fill('wx-e2e-corp');
    await dialog.locator('[data-field="integration-instance.credential.corpSecret"]').fill('e2e-corp-secret');

    const instanceResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/api/integration/instances') && response.request().method() === 'POST',
    );
    await dialog.locator('[data-action="integration-instance.submit"]').click();
    const instanceResponse = await instanceResponsePromise;
    expect(instanceResponse.status()).toBe(200);
    expect((await instanceResponse.json()).data).toBe(true);

    const row = page.locator('.el-table__body-wrapper tr', { hasText: instanceCode }).first();
    await expect(row).toBeVisible({ timeout: 10000 });
    await expect(row).toContainText('企业微信应用消息');
    await expect(row).toContainText('已配置');
    await expect(row).not.toContainText('e2e-corp-secret');

    await row.locator('[data-action="integration-instance.view"]').click();
    const detail = page.locator('.el-dialog').filter({ hasText: '集成应用详情' }).last();
    await expect(detail).toBeVisible();
    await expect(detail).toContainText(`/integration/callback/wecom-message/${instanceCode}`);
    await expect(detail).toContainText('corpSecret');
    await expect(detail).not.toContainText('e2e-corp-secret');
    await detail.getByRole('button', { name: '关闭' }).click();

    expect(consoleErrors).toEqual([]);
    expect(failedResponses).toEqual([]);
  });
});
