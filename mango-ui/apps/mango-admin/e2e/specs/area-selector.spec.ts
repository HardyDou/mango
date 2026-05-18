import { expect, test } from '@playwright/test';

async function loginByUi(page: import('@playwright/test').Page) {
  await page.goto('/#/login');
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  await page.click('button:has-text("登 录")');
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

test.describe('行政区划选择器 E2E', () => {
  test('按级别懒加载省市区并完成选择', async ({ page, request }) => {
    const loginResponse = await request.post('/api/auth/login', {
      data: {
        username: 'admin',
        password: 'admin123',
      },
    });
    const loginBody = await loginResponse.json();
    const accessToken = loginBody?.data?.accessToken;
    expect(accessToken).toBeTruthy();

    const rootResponse = await request.get('/api/system/area/tree?type=1', {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(rootResponse.ok()).toBeTruthy();
    const rootBody = await rootResponse.json();
    expect(rootBody.data[0]).toMatchObject({ id: 1, name: '北京市' });

    const cityResponse = await request.get('/api/system/area/children?parentId=1', {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(cityResponse.ok()).toBeTruthy();
    const cityBody = await cityResponse.json();
    expect(cityBody.data[0]).toMatchObject({ id: 1101, name: '北京市' });

    await loginByUi(page);
    await page.goto('/#/components/china-area');
    await expect(page.getByText('省市区选择器').first()).toBeVisible();

    await page.getByPlaceholder('请选择省市区').first().click();

    const provinceNode = page.locator('.el-cascader-node', { hasText: '北京市' }).first();
    await expect(provinceNode).toBeVisible();

    const provinceChildrenResponsePromise = page.waitForResponse((response) => {
      return response.status() === 200 && response.url().includes('/api/system/area/children?parentId=1');
    });
    await provinceNode.hover();
    await provinceChildrenResponsePromise;

    const cityNode = page.locator('.el-cascader-node', { hasText: '北京市' }).nth(1);
    await expect(cityNode).toBeVisible();

    const cityChildrenResponsePromise = page.waitForResponse((response) => {
      return response.status() === 200 && response.url().includes('/api/system/area/children?parentId=1101');
    });
    await cityNode.hover();
    await cityChildrenResponsePromise;

    await page.locator('.el-cascader-node', { hasText: '东城区' }).click();
    await expect(page.getByText('省市区: 1,1101,110101')).toBeVisible();
  });
});
