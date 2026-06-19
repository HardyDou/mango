import { expect, test } from '@playwright/test';
import { api as e2eApi } from '../support/api';

async function loginByUi(page: import('@playwright/test').Page) {
  await page.goto('/#/login');
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  await page.click('button:has-text("登 录")');
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

test.describe('验证码组件 E2E', () => {
  test('验证码接口和页面组件使用真实后端生成验证码', async ({ page, request }) => {
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
    const loginBody = await loginResponse.json();
    const accessToken = loginBody?.data?.accessToken;
    expect(accessToken).toBeTruthy();

    const typesResponse = await request.get(e2eApi('/captcha/types'), {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(typesResponse.ok()).toBeTruthy();
    const typesBody = await typesResponse.json();
    expect(typesBody.data.types).toEqual(expect.arrayContaining(['ARITHMETIC', 'BLOCK_PUZZLE']));

    await loginByUi(page);

    const arithmeticResponseOnLoad = page.waitForResponse((response) => {
      return response.status() === 200 && response.url().includes('/api/captcha/arithmetic');
    });
    await page.goto('/#/components/captcha');
    await arithmeticResponseOnLoad;
    await expect(page.getByRole('heading', { name: '验证码组件' })).toBeVisible();
    const arithmeticPanel = page.locator('section#arithmetic');
    await expect(arithmeticPanel.getByRole('heading', { name: '算术验证码' })).toBeVisible();
    await expect(arithmeticPanel.locator('img[alt="Arithmetic captcha"]')).toBeVisible();

    const arithmeticResponsePromise = page.waitForResponse((response) => {
      return response.status() === 200 && response.url().includes('/api/captcha/arithmetic');
    });
    await arithmeticPanel.getByRole('button', { name: '刷新' }).click();
    const arithmeticResponse = await arithmeticResponsePromise;
    const arithmeticBody = await arithmeticResponse.json();
    expect(arithmeticBody.data).toMatchObject({
      type: 'ARITHMETIC',
      expireTime: '300',
    });
    expect(arithmeticBody.data.key).toBeTruthy();
    expect(arithmeticBody.data.image).toContain('data:image/png;base64,');
    await expect(arithmeticPanel.locator('img[alt="Arithmetic captcha"]')).toBeVisible();

    const blockPuzzlePanel = page.locator('section#block');
    await expect(blockPuzzlePanel.getByRole('heading', { name: '图片滑块验证码' })).toBeVisible();
    await expect(blockPuzzlePanel.getByText('拖动滑块完成拼图')).toBeVisible();

    const blockPuzzleResponsePromise = page.waitForResponse((response) => {
      return response.status() === 200 && response.url().includes('/api/captcha/block-puzzle');
    });
    await blockPuzzlePanel.getByRole('button', { name: '刷新' }).click();
    const blockPuzzleResponse = await blockPuzzleResponsePromise;
    const blockPuzzleBody = await blockPuzzleResponse.json();
    expect(blockPuzzleBody.data).toMatchObject({
      type: 'BLOCK_PUZZLE',
      expireTime: '300',
    });
    expect(blockPuzzleBody.data.key).toBeTruthy();
    await expect(blockPuzzlePanel.getByText('拖动滑块完成拼图')).toBeVisible();
  });
});
