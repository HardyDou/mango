import { expect, test } from '@playwright/test';

test.use({ storageState: { cookies: [], origins: [] } });

test.describe('认证会话 E2E @p0 @mango-auth', () => {
  test('真实登录、刷新保持与退出撤销形成完整浏览器会话', async ({ page, context }) => {
    const consoleErrors: string[] = [];
    const pageErrors: string[] = [];
    const failedApiResponses: string[] = [];

    page.on('console', (message) => {
      if (message.type() === 'error') {
        consoleErrors.push(message.text());
      }
    });
    page.on('pageerror', (error) => pageErrors.push(error.message));
    page.on('response', (response) => {
      if (response.url().includes('/api/') && response.status() >= 400) {
        failedApiResponses.push(`${response.status()} ${response.request().method()} ${response.url()}`);
      }
    });

    await page.goto('/#/login');
    await page.getByPlaceholder('用户名').fill(process.env.MANGO_E2E_ADMIN_USERNAME || 'admin');
    await page.getByPlaceholder('密码').fill(process.env.MANGO_E2E_ADMIN_PASSWORD || 'admin123');

    const loginResponsePromise = page.waitForResponse((response) =>
      new URL(response.url()).pathname === '/api/auth/login' && response.request().method() === 'POST'
    );
    await page.getByRole('button', { name: '登 录' }).click();
    const loginResponse = await loginResponsePromise;

    expect(loginResponse.status()).toBe(200);
    await expect(page).toHaveURL(/#\/home$/);
    await expect(page.getByRole('button', { name: '系统管理', exact: true })).toBeVisible();
    await expect.poll(async () => {
      return page.evaluate(() => JSON.parse(sessionStorage.getItem('userInfo') || 'null')?.username);
    }).toBe('admin');

    const accessCookie = (await context.cookies()).find((cookie) => cookie.name === 'MANGO_TOKEN');
    expect(accessCookie).toMatchObject({ httpOnly: true, sameSite: 'Lax', path: '/' });

    const menuResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/authorization/menus/user') && response.status() === 200
    );
    await page.reload();
    await menuResponsePromise;
    await expect(page).toHaveURL(/#\/home$/);
    await expect(page.getByRole('button', { name: '系统管理', exact: true })).toBeVisible();
    await page.waitForLoadState('networkidle');

    await page.getByRole('button', { name: /admin 芒果集团/ }).click();
    await page.locator('[data-action="auth.logout"]').click();
    await expect(page.getByRole('dialog', { name: '提示' })).toBeVisible();

    const logoutResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/auth/logout') && response.request().method() === 'POST'
    );
    await page.getByRole('button', { name: '确定', exact: true }).click();
    const logoutResponse = await logoutResponsePromise;

    expect(logoutResponse.status()).toBe(200);
    await expect(page).toHaveURL(/#\/login/);
    await expect(page.getByPlaceholder('用户名')).toBeVisible();
    await expect(page.getByRole('dialog', { name: '提示' })).toBeHidden();
    expect((await context.cookies()).some((cookie) => cookie.name === 'MANGO_TOKEN')).toBe(false);
    expect(await page.evaluate(() => sessionStorage.getItem('userInfo'))).toBeNull();
    expect(consoleErrors, consoleErrors.join('\n')).toEqual([]);
    expect(pageErrors, pageErrors.join('\n')).toEqual([]);
    expect(failedApiResponses, failedApiResponses.join('\n')).toEqual([]);
  });
});
