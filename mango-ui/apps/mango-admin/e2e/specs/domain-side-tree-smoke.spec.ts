import { expect, test, type Page } from '@playwright/test';

type LoginTenant = {
  tenantId: string;
  tenantCode: string;
  tenantName: string;
};

const platformTenant: LoginTenant = {
  tenantId: '1',
  tenantCode: 'default',
  tenantName: '芒果集团',
};

async function loginPage(page: Page, tenant: LoginTenant) {
  await page.goto('/#/login');
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: new RegExp(tenant.tenantName) }).click();
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  await page.click('button:has-text("登 录")');
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

test.describe('业务域左侧树后台页面冒烟', () => {
  test('流程定义、流程模板和消息配置使用业务域侧栏', async ({ page }) => {
    const consoleErrors: string[] = [];
    page.on('console', message => {
      if (message.type() === 'error') {
        consoleErrors.push(message.text());
      }
    });

    await loginPage(page, platformTenant);

    await page.goto('/#/workflow/manage/definition');
    await expect(page.getByRole('heading', { name: '业务域' })).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('button', { name: '全部流程 ALL' })).toBeVisible();
    await expect(page.getByText('WORKFLOW').first()).toBeVisible();
    await expect(page.getByText('流程分类').first()).toBeVisible();

    await page.goto('/#/workflow/manage/template');
    await expect(page.getByRole('heading', { name: '业务域' })).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('button', { name: '全部模板 ALL' })).toBeVisible();
    await expect(page.getByText('WORKFLOW').first()).toBeVisible();
    await expect(page.getByText('模板分类').first()).toBeVisible();

    await page.goto('/#/notice/message-definition');
    await expect(page.getByRole('heading', { name: '业务域' })).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('button', { name: '全部消息 ALL' })).toBeVisible();
    await expect(page.getByText('NOTICE').first()).toBeVisible();

    expect(consoleErrors).toEqual([]);
  });
});
