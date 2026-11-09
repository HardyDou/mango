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

test.describe('微应用总览', () => {
  test('@p1 @authorization 使用真实运行单元注册表展示微应用并保持只读', async ({ page }) => {
    const consoleErrors: string[] = [];
    const failedResponses: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') consoleErrors.push(message.text());
    });
    page.on('response', (response) => {
      if (response.status() >= 400) failedResponses.push(`${response.status()} ${response.url()}`);
    });

    await login(page);
    await page.goto('/#/system/micro-application');
    await expect(page.locator('[data-page="system.micro-application"]')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('[data-surface="search"]')).toBeVisible();
    await expect(page.locator('[data-surface="list"]')).toBeVisible();

    await expect(page.locator('text=微应用由部署资源声明登记')).toBeVisible();
    await expect(page.getByRole('button', { name: /新增/ })).toHaveCount(0);

    const row = page.locator('.el-table__body-wrapper tr', { hasText: 'mango-admin-rbac-app' }).first();
    await expect(row).toBeVisible({ timeout: 10000 });
    await expect(row).toContainText('权限管理微应用');
    await expect(row).toContainText('1.0.0');
    await expect(row).toContainText('1 个');

    const healthResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/authorization/frontend-apps/health-check') && response.request().method() === 'POST',
    );
    await row.locator('[data-action="micro-application.check-health"]').click();
    const healthResponse = await healthResponsePromise;
    const healthBody = await healthResponse.json();
    expect(healthResponse.status()).toBe(200);
    expect(healthBody.data?.success).toBe(true);
    expect(healthBody.data?.statusCode).toBe(200);
    await expect(row).toContainText('可达');

    await row.locator('[data-action="micro-application.references"]').click();
    await expect(page.locator('.reference-list')).toContainText('internal-admin / mango-authorization');

    await page.locator('[data-field="micro-application.keyword"]').fill('not-exists-code');
    await page.getByRole('button', { name: '查询' }).click();
    await page.waitForTimeout(300);
    await expect(page.locator('.el-table__body-wrapper tr')).toHaveCount(0);

    await page.getByRole('button', { name: '重置' }).click();
    await page.waitForTimeout(300);
    await expect(page.locator('.el-table__body-wrapper tr', { hasText: 'mango-admin-rbac-app' })).toHaveCount(1);

    expect(consoleErrors).toEqual([]);
    expect(failedResponses).toEqual([]);
  });
});
