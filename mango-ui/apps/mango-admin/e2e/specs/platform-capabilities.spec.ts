import { expect, test, type Page } from '@playwright/test';

async function login(page: Page) {
  await page.goto('/#/login');
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  const accountTenantsResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/auth/login-institutions') && response.status() === 200
  );
  await page.locator('input[placeholder="密码"]').blur();
  await accountTenantsResponsePromise;
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: /芒果集团/ }).click();
  await page.locator('.login-btn').click();
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

async function clickMenu(page: Page, name: string) {
  const menu = page
    .locator(
      [
        '.layout-top-system-item',
        '.layout-columns-aside li',
        '.el-sub-menu__title',
        '.el-menu-item',
      ].join(', '),
      { hasText: name },
    )
    .first();

  await expect(menu, `${name} 菜单必须可见`).toBeVisible({ timeout: 10000 });
  await menu.click();
}

async function expectMenuVisible(page: Page, name: string) {
  await expect(
    page.locator('.layout-top-system-item, .el-sub-menu__title, .el-menu-item', { hasText: name }).first(),
    `${name} 菜单必须可见`,
  ).toBeVisible({ timeout: 10000 });
}

test.describe('平台能力介绍 E2E', () => {
  test('可从开发中心菜单打开模板服务说明页', async ({ page }) => {
    await login(page);

    await clickMenu(page, '开发中心');
    await expectMenuVisible(page, '平台能力介绍');
    await page.goto('/#/debug/capabilities/template');

    await expect(page).toHaveURL(/#\/debug\/capabilities\/template$/);
    await expect(page.getByRole('heading', { name: '模板服务', level: 1 })).toBeVisible({
      timeout: 10000,
    });

    for (const text of ['能力边界', '支持格式', '支持语法', '使用示例', '如何扩展']) {
      await expect(page.getByRole('heading', { name: text })).toBeVisible();
    }

    for (const text of ['TEXT', 'DOCX', 'PDF/OFD', 'FreeMarker', 'poi-tl', 'mango-infra-tools-doc']) {
      await expect(page.getByText(text).first()).toBeVisible();
    }

    await expect(page.locator('.el-message--error')).toHaveCount(0);
    await expect(page.locator('text=/401|403|未授权|拒绝访问|路由加载失败|加载失败/')).toHaveCount(0);
  });
});
