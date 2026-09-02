import { expect, test } from '@playwright/test';
import { api as e2eApi } from '../support/api';

const defaultTenant = {
  tenantId: '1',
  tenantCode: 'default',
  tenantName: '芒果集团',
};

test.describe('T7 默认单租户产品态', () => {
  test('@p0 @tenant 登录选项仅包含默认租户', async ({ request }) => {
    const response = await request.get(e2eApi('/system/tenant/login-options'));
    expect(response.status()).toBe(200);

    const body = await response.json();
    expect(body.success || body.code === 200).toBeTruthy();
    expect(body.data).toEqual([defaultTenant]);
  });

  test('@p0 @tenant 登录页隐藏唯一租户选择且后台不展示租户管理', async ({ page }) => {
    const loginOptionsResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/api/system/tenant/login-options') && response.status() === 200,
    );
    await page.goto('/#/login');

    const loginOptionsResponse = await loginOptionsResponsePromise;
    const loginOptionsBody = await loginOptionsResponse.json();
    expect(loginOptionsBody.data).toEqual([defaultTenant]);
    await expect(page.locator('.tenant-select')).toHaveCount(0);

    await page.fill('input[placeholder="请输入用户名"]', 'admin');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');
    const accountTenantsResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/api/auth/login-institutions') && response.status() === 200,
    );
    await page.locator('input[placeholder="请输入密码"]').blur();
    const accountTenantsResponse = await accountTenantsResponsePromise;
    const accountTenantsBody = await accountTenantsResponse.json();
    expect(accountTenantsBody.data).toEqual([expect.objectContaining(defaultTenant)]);
    await expect(page.locator('.tenant-select')).toHaveCount(0);

    await page.locator('.login-btn').click();
    await page.waitForURL('**/#/home', { timeout: 10000 });
    await expect(page.getByRole('button', { name: '系统管理' })).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('租户管理')).toHaveCount(0);
  });
});
