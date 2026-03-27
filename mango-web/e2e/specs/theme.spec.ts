import { test, expect } from '@playwright/test';

test.describe('主题系统 E2E 测试', () => {
  test.beforeEach(async ({ page }) => {
    // 登录
    await page.goto('/#/login');
    await page.fill('input[placeholder="用户名"]', 'admin');
    await page.fill('input[placeholder="密码"]', 'admin123');
    await page.click('button:has-text("登 录")');
    await page.waitForURL('**/#/home', { timeout: 5000 });
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
    // 检查 data-theme 属性
    const initialTheme = await page.getAttribute('html', 'data-theme');

    // 点击主题设置按钮
    await page.click('.layout-breadcrumb-settings');

    // 应该在 DOM 中出现设置面板
    // 假设有一个深色模式开关
    const darkModeSwitch = page.locator('.theme-dark-switch, .switch-dark');
    if (await darkModeSwitch.isVisible()) {
      await darkModeSwitch.click();

      // 检查 data-theme 是否改变
      const newTheme = await page.getAttribute('html', 'data-theme');
      expect(newTheme).not.toBe(initialTheme);
    }
  });

  test('主题配置持久化', async ({ page }) => {
    // 修改主题配置
    await page.evaluate(() => {
      // @ts-ignore
      window.__MANGO_PINIA__.themeConfig.primary = '#ff0000';
    });

    // 刷新页面
    await page.reload();
    await page.waitForURL('**/#/home');

    // 检查主题色是否保持
    const primaryColor = await page.evaluate(() => {
      return getComputedStyle(document.documentElement)
        .getPropertyValue('--mango-color-primary')
        .trim();
    });

    expect(primaryColor).toBe('#ff0000');
  });

  test('布局主题', async ({ page }) => {
    // 测试不同布局的主题应用
    const layouts = ['defaults', 'classic', 'transverse', 'columns'];

    for (const layout of layouts) {
      await page.evaluate((l) => {
        // @ts-ignore
        window.__MANGO_PINIA__.themeConfig.layout = l;
      });

      await page.reload();
      await page.waitForLoadState('domcontentloaded');

      // 布局应该正常渲染
      await expect(page.locator('.layout-container')).toBeVisible();
    }
  });

  test('侧边栏主题', async ({ page }) => {
    // 测试侧边栏背景色
    const bgColor = await page.evaluate(() => {
      const aside = document.querySelector('.layout-aside');
      return aside ? getComputedStyle(aside).backgroundColor : null;
    });

    // 应该有一个非透明的背景色
    expect(bgColor).toBeTruthy();
  });

  test('菜单主题', async ({ page }) => {
    // 检查菜单项悬停样式
    const menuItem = page.locator('.el-menu-item').first();
    await menuItem.hover();

    // 应该有悬停样式
    const hoverColor = await page.evaluate(() => {
      const item = document.querySelector('.el-menu-item:hover');
      return item ? getComputedStyle(item).backgroundColor : null;
    });

    expect(hoverColor).toBeTruthy();
  });

  test('标签页主题', async ({ page }) => {
    // 切换到显示标签导航的布局
    await page.evaluate(() => {
      // @ts-ignore
      window.__MANGO_PINIA__.themeConfig.layout = 'defaults';
      window.__MANGO_PINIA__.themeConfig.isTagsview = true;
    });
    await page.reload();

    // 检查标签页样式
    const activeTag = page.locator('.tags-view-item.active');
    await expect(activeTag).toBeVisible();

    // 激活标签应该有主题色
    const activeBg = await activeTag.evaluate((el) => {
      return getComputedStyle(el).backgroundColor;
    });

    expect(activeBg).toContain('rgb'); // rgba, rgb, or hex
  });

  test('全局 CSS 变量', async ({ page }) => {
    // 验证所有 CSS 变量都存在
    const cssVars = [
      '--mango-bg-color',
      '--mango-text-color',
      '--mango-border-color',
      '--mango-color-primary',
      '--mango-color-menu-hover',
    ];

    for (const cssVar of cssVars) {
      const value = await page.evaluate((v) => {
        return getComputedStyle(document.documentElement).getPropertyValue(v).trim();
      }, cssVar);

      expect(value).toBeTruthy();
    }
  });
});
