import { test, expect } from '@playwright/test';

test.describe('主题系统 E2E 测试', () => {
  test.beforeEach(async ({ page }) => {
    // 登录
    await page.goto('/#/login');
    await page.getByPlaceholder('请输入用户名').fill('admin');
    await page.getByPlaceholder('请输入密码').fill('admin123');
    const accountTenantsResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/api/auth/login-institutions') && response.status() === 200,
    );
    await page.getByPlaceholder('请输入密码').blur();
    await accountTenantsResponsePromise;
    await page.locator('.tenant-select').click();
    await page.getByRole('option', { name: /芒果集团/ }).click();
    await page.getByRole('button', { name: '登录', exact: true }).click();
    await page.waitForURL('**/#/home', { timeout: 10000 });
  });

  test('默认主题色', async ({ page }) => {
    // 检查默认主题色变量
    const primaryColor = await page.evaluate(() => {
      return getComputedStyle(document.documentElement).getPropertyValue('--mango-color-primary').trim();
    });

    // 默认应该是 #2E5CF6
    expect(primaryColor).toBe('#2E5CF6');
  });

  test('头像菜单进入主题设置', async ({ page }) => {
    await page.locator('[data-action="user-menu.open"]').click();
    await page.locator('[data-action="profile.theme"]').click();

    await expect(page).toHaveURL(/\/profile\?tab=theme/);
    await expect(page.locator('[data-surface="profile.theme"]')).toBeVisible();
  });

  test('主题设置不再提供顶栏独立按钮', async ({ page }) => {
    await page.locator('[data-action="user-menu.open"]').click();
    await page.locator('[data-action="profile.theme"]').click();

    await expect(page.locator('[data-surface="profile.theme"]')).toBeVisible();
    await expect(page.getByRole('button', { name: '主题设置' })).toHaveCount(0);
  });

  test('布局主题', async ({ page }) => {
    // 布局应该正常渲染
    await page.waitForLoadState('networkidle');
    const container = page.locator('.layout-container, .layout-main');
    await expect(container.first()).toBeVisible();
  });

  test('@p1 @theme 侧边栏主题', async ({ page }) => {
    const asideEl = page.locator('.layout-aside, .layout-columns-aside');
    await expect(asideEl.first()).toBeVisible();

    const bgColor = await asideEl.first().evaluate((element) => getComputedStyle(element).backgroundColor);
    expect(bgColor).toBeTruthy();
  });

  test('菜单主题', async ({ page }) => {
    // 检查菜单项是否存在
    const menuItem = page.locator('.el-menu-item').first();
    if ((await menuItem.count()) > 0) {
      // 菜单项应该可以交互
      await expect(menuItem).toBeVisible();
    }
  });

  test('标签页主题', async ({ page }) => {
    // 标签导航应该正常渲染或隐藏
    await page.waitForLoadState('networkidle');

    // 如果有标签导航
    const tagsView = page.locator('.tags-view-container');
    const activeTag = page.locator('.tags-view-item.active, .tags-view-item');

    if ((await tagsView.count()) > 0 && (await activeTag.count()) > 0) {
      await expect(activeTag.first()).toBeVisible();
    }
  });

  test('全局 CSS 变量', async ({ page }) => {
    // 验证关键 CSS 变量都存在
    const cssVars = ['--mango-color-primary', '--mango-bg-top-bar', '--mango-bg-menu-bar'];

    for (const cssVar of cssVars) {
      const value = await page.evaluate((v) => {
        return getComputedStyle(document.documentElement).getPropertyValue(v).trim();
      }, cssVar);

      expect(value).toBeTruthy();
    }
  });
});
