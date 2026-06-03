import { expect, test } from '@playwright/test';
import { api as e2eApi } from '../support/api';

test.describe('登录机构选择 E2E', () => {
  test('账号密码校验后返回当前账号可进入机构，登录页下拉随账号权限收敛', async ({ page, request }) => {
    const optionsResponse = await request.post(e2eApi('/auth/login-institutions'), {
      data: {
        username: 'admin',
        password: 'admin123',
        realm: 'INTERNAL',
        appCode: 'internal-admin',
      },
    });
    expect(optionsResponse.status()).toBe(200);
    const optionsBody = await optionsResponse.json();
    expect(optionsBody.success || optionsBody.code === 200).toBeTruthy();
    expect(optionsBody.data.length).toBeGreaterThan(0);
    expect(optionsBody.data.every((item: any) => item.memberId)).toBeTruthy();
    expect(optionsBody.data.map((item: any) => item.tenantName)).toContain('芒果集团');

    const invalidResponse = await request.post(e2eApi('/auth/login-institutions'), {
      data: {
        username: 'admin',
        password: 'bad-password',
        realm: 'INTERNAL',
        appCode: 'internal-admin',
      },
    });
    expect(invalidResponse.status()).toBe(200);
    const invalidBody = await invalidResponse.json();
    expect(invalidBody.success).toBeFalsy();
    expect(invalidBody.code).toBe(1400);

    await page.goto('/#/login');
    await page.fill('input[placeholder="用户名"]', 'admin');
    await page.fill('input[placeholder="密码"]', 'admin123');
    const accountTenantsResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/auth/login-institutions') && response.status() === 200
    );
    await page.locator('input[placeholder="密码"]').blur();
    await accountTenantsResponsePromise;
    await page.locator('.tenant-select').click();
    await expect(page.getByRole('option', { name: /芒果集团/ })).toBeVisible({ timeout: 10000 });
  });
});
