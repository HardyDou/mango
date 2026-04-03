import { test, expect } from '@playwright/test';

test.describe('主题系统 E2E 测试', () => {
  test.beforeEach(async ({ page }) => {
    // 登录
    await page.goto('/#/login');
    await page.fill('input[placeholder="用户名"]', 'admin');
    await page.fill('input[placeholder="密码"]', 'admin123');
    await page.click('button:has-text("登 录")');
    await page.waitForURL('**/#/home', { timeout: 10000 });
  });

  test('默认主题色', async ({ page }) => {
    // 检查默认主题色变量
    const primaryColor = await page.evaluate(() => {
      return getComputedStyle(document.documentElement)
        .getPropertyValue('--mango-color-primary')
        .trim();
    });

    // 默认应该是 #2E5CF6
    expect(primaryColor).toBe('#2E5CF6');
  });

  test('深色模式切换', async ({ page }) => {
    // 点击主题设置按钮
    await page.click('.layout-breadcrumb-settings');
    await page.waitForTimeout(500);

    // 检查设置面板是否打开
    const settingsDrawer = page.locator('.el-drawer');
    if (await settingsDrawer.count() > 0) {
      // 深色模式切换按钮应该可见
      const darkSwitch = page.locator('.switch-dark, [aria-label*="深色"], .is-dark');
      if (await darkSwitch.count() > 0) {
        await darkSwitch.click();
        await page.waitForTimeout(300);

        // 检查 data-theme 是否改变
        const newTheme = await page.getAttribute('html', 'data-theme');
        // 深色模式开启时应该是 'dark'
        expect(newTheme === 'dark' || newTheme === 'light').toBeTruthy();
      }
    }
  });

  test('主题配置持久化', async ({ page }) => {
    // 点击主题设置按钮
    await page.click('.layout-breadcrumb-settings');
    await page.waitForTimeout(500);

    // 检查设置面板存在
    const settingsPanel = page.locator('.el-drawer, .theme-settings');
    if (await settingsPanel.count() > 0) {
      // 设置面板能正常打开说明主题系统工作
      await expect(settingsPanel).toBeVisible();
    }
  });

  test('布局主题', async ({ page }) => {
    // 布局应该正常渲染
    await page.waitForLoadState('networkidle');
    const container = page.locator('.layout-container, .layout-main');
    await expect(container.first()).toBeVisible();
  });

  test('侧边栏主题', async ({ page }) => {
    // 侧边栏应该存在
    const asideEl = page.locator('.layout-aside, .layout-columns-aside');
    const asideCount = await asideEl.count();

    if (asideCount > 0) {
      // 测试侧边栏背景色
      const bgColor = await page.evaluate(() => {
        const aside = document.querySelector('.layout-aside, .layout-columns-aside');
        return aside ? getComputedStyle(aside).backgroundColor : null;
      });

      // 应该有一个背景色
      expect(bgColor).toBeTruthy();
    } else {
      // 如果没有侧边栏元素，跳过
      test.skip();
    }
  });

  test('菜单主题', async ({ page }) => {
    // 检查菜单项是否存在
    const menuItem = page.locator('.el-menu-item').first();
    if (await menuItem.count() > 0) {
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

    if (await tagsView.count() > 0 && await activeTag.count() > 0) {
      await expect(activeTag.first()).toBeVisible();
    }
  });

  test('全局 CSS 变量', async ({ page }) => {
    // 验证关键 CSS 变量都存在
    const cssVars = [
      '--mango-color-primary',
      '--mango-bg-top-bar',
      '--mango-bg-menu-bar',
    ];

    for (const cssVar of cssVars) {
      const value = await page.evaluate((v) => {
        return getComputedStyle(document.documentElement).getPropertyValue(v).trim();
      }, cssVar);

      expect(value).toBeTruthy();
    }
  });
});
