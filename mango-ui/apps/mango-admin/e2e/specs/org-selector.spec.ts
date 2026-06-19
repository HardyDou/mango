import { expect, test } from '@playwright/test';
import { api as e2eApi } from '../support/api';

async function loginByUi(page: import('@playwright/test').Page) {
  await page.goto('/#/login');
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  await page.click('button:has-text("登 录")');
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

test.describe('组织选择器 E2E', () => {
  test('加载完整组织树并支持单选、多选', async ({ page, request }) => {
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

    const treeResponse = await request.get(e2eApi('/org/tree?parentId=0'), {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(treeResponse.ok()).toBeTruthy();
    const treeBody = await treeResponse.json();
    expect(treeBody.data[0]).toMatchObject({ id: '1', orgName: '芒果集团' });
    expect(treeBody.data[0].children.map((item: { orgName: string }) => item.orgName)).toEqual([
      'A公司',
      'B公司',
      'C公司',
    ]);

    await loginByUi(page);
    await page.goto('/#/components/org-selector');
    await expect(page.getByText('组织架构选择器').first()).toBeVisible();

    const selects = page.locator('.org-selector .el-select');

    await selects.nth(0).click();
    const singleDialog = page.getByRole('dialog', { name: '选择组织' });
    await expect(singleDialog).toBeVisible();
    await expect(singleDialog.getByText('芒果集团')).toBeVisible();
    await expect(singleDialog.getByText('A公司')).toBeVisible();
    await singleDialog.locator('.el-tree-node__content', { hasText: 'A公司' }).locator('.el-checkbox').click();
    await page.getByRole('button', { name: '确定' }).click();
    await expect(page.getByText('当前值：2')).toBeVisible();
    await expect(page.locator('.org-selected-tags .el-tag', { hasText: 'A公司' })).toBeVisible();

    await selects.nth(1).click();
    const multipleDialog = page.getByRole('dialog', { name: '选择组织' });
    await expect(multipleDialog).toBeVisible();
    await multipleDialog.locator('.el-tree-node__content', { hasText: 'B公司' }).locator('.el-checkbox').click();
    await multipleDialog.locator('.el-tree-node__content', { hasText: 'C公司' }).locator('.el-checkbox').click();
    await page.getByRole('button', { name: '确定' }).click();
    await expect(page.getByText('2 请选择组织')).toBeVisible();
    await expect(page.locator('.result-tags .el-tag', { hasText: '3' })).toBeVisible();
    await expect(page.locator('.result-tags .el-tag', { hasText: '4' })).toBeVisible();
  });
});
