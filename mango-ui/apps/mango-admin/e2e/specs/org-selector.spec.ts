import { expect, test } from '@playwright/test';

async function loginByUi(page: import('@playwright/test').Page) {
  await page.goto('/#/login');
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  await page.click('button:has-text("登 录")');
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

test.describe('组织选择器 E2E', () => {
  test('加载完整组织树并支持单选、多选', async ({ page, request }) => {
    const loginResponse = await request.post('/api/auth/login', {
      data: {
        username: 'admin',
        password: 'admin123',
      },
    });
    const loginBody = await loginResponse.json();
    const accessToken = loginBody?.data?.accessToken;
    expect(accessToken).toBeTruthy();

    const treeResponse = await request.get('/api/org/tree?parentId=0', {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(treeResponse.ok()).toBeTruthy();
    const treeBody = await treeResponse.json();
    expect(treeBody.data[0]).toMatchObject({ id: 1, orgName: '芒果集团' });
    expect(treeBody.data[0].children.map((item: { orgName: string }) => item.orgName)).toEqual([
      '芒果科技',
      '芒果金融',
      '芒果地产',
    ]);

    await loginByUi(page);
    await page.goto('/#/demo/org-selector');
    await expect(page.getByText('组织架构选择器').first()).toBeVisible();

    const selects = page.locator('.org-selector .el-select');

    await selects.nth(0).click();
    await expect(page.getByRole('dialog', { name: '选择组织' })).toBeVisible();
    await expect(page.getByText('芒果集团')).toBeVisible();
    await expect(page.getByText('芒果科技')).toBeVisible();
    await page.locator('.el-tree-node__content', { hasText: '芒果科技' }).locator('.el-checkbox').click();
    await page.getByRole('button', { name: '确定' }).click();
    await expect(page.getByText('单选: 2')).toBeVisible();
    await expect(page.locator('.org-selected-tags .el-tag', { hasText: '芒果科技' })).toBeVisible();

    await selects.nth(1).click();
    await expect(page.getByRole('dialog', { name: '选择组织' })).toBeVisible();
    await page.locator('.el-tree-node__content', { hasText: '芒果金融' }).locator('.el-checkbox').click();
    await page.locator('.el-tree-node__content', { hasText: '芒果地产' }).locator('.el-checkbox').click();
    await page.getByRole('button', { name: '确定' }).click();
    await expect(page.getByText('2 请选择组织')).toBeVisible();
    await expect(page.locator('.result .el-tag', { hasText: '3' })).toBeVisible();
    await expect(page.locator('.result .el-tag', { hasText: '4' })).toBeVisible();
  });
});
