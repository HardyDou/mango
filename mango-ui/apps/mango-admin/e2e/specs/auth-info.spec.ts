import { expect, test } from '@playwright/test';
import { api as e2eApi } from '../support/api';

test.describe('当前用户信息 E2E', () => {
  test('登录后可通过真实后端获取当前用户信息', async ({ page, request }) => {
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
    const accessToken = loginBody?.data?.accessToken;
    expect(accessToken).toBeTruthy();

    const infoResponse = await request.get(e2eApi('/auth/info'), {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });
    expect(infoResponse.ok()).toBeTruthy();

    const infoBody = await infoResponse.json();
    expect(infoBody?.data).toMatchObject({
      userId: '1',
      username: 'admin',
      nickname: 'Administrator',
    });
    expect(infoBody.data.roles).toContain('ROLE_ADMIN');
    expect(infoBody.data.permissions).toContain('authorization:role:list');
    expect(infoBody.data.permissions).not.toContain('*:*');

    await page.goto('/#/login');
    await page.fill('input[placeholder="用户名"]', 'admin');
    await page.fill('input[placeholder="密码"]', 'admin123');
    await page.click('button:has-text("登 录")');
    await page.waitForURL('**/#/home', { timeout: 10000 });

    const sessionUserInfo = await page.evaluate(() => {
      const raw = window.sessionStorage.getItem('userInfo');
      return raw ? JSON.parse(raw) : null;
    });
    expect(sessionUserInfo).toMatchObject({
      userId: '1',
      username: 'admin',
      nickname: 'Administrator',
    });

    await page.locator('.layout-breadcrumb-user').click();
    await expect(page.locator('.institution-dropdown-item')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText(/当前租户：/)).toHaveCount(0);
    await expect(page.getByText(/当前机构：/)).toHaveCount(0);
    await expect(page.getByText(/当前上下文：/)).toHaveCount(0);
  });
});
