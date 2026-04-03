import { test, expect } from '@playwright/test';

test.describe('布局系统', () => {
  test.beforeEach(async ({ page }) => {
    // 先登录
    await page.goto('/login');
    await page.fill('input[placeholder*="用户名"]', 'admin');
    await page.fill('input[placeholder*="密码"]', 'admin123');
    await page.locator('button:has-text("登录")').click();
    await page.waitForURL('**/home**', { timeout: 5000 });
  });

  test('默认布局正确显示', async ({ page }) => {
    // 左侧菜单应可见
    await expect(page.locator('.aside')).toBeVisible();
    // 头部应可见
    await expect(page.locator('.header')).toBeVisible();
    // 主体内容区应可见
    await expect(page.locator('.main')).toBeVisible();
  });

  test('四种布局切换', async ({ page }) => {
    // 打开设置面板
    await page.locator('.header-setting').click();

    // 切换到经典布局
    await page.locator('text=经典布局').click();
    await expect(page.locator('.layout-classic')).toBeVisible();

    // 切换到横向布局
    await page.locator('.header-setting').click();
    await page.locator('text=横向布局').click();
    await expect(page.locator('.layout-transverse')).toBeVisible();

    // 切换到分栏布局
    await page.locator('.header-setting').click();
    await page.locator('text=分栏布局').click();
    await expect(page.locator('.layout-columns')).toBeVisible();
  });

  test('标签页功能', async ({ page }) => {
    // 点击菜单项应打开新标签页
    const menuItem = page.locator('.menu-list li').first();
    await menuItem.click();

    // 标签栏应显示多个标签
    const tags = page.locator('.tags-view li');
    await expect(tags).toHaveCount({ length: 2 });

    // 关闭标签
    await page.locator('.tags-view li').first().locator('.close-icon').click();
    await expect(tags).toHaveCount({ length: 1 });
  });

  test('面包屑导航', async ({ page }) => {
    // 点击菜单进入页面
    await page.locator('.menu-list li').first().click();

    // 面包屑应显示当前路径
    const breadcrumb = page.locator('.breadcrumb');
    await expect(breadcrumb).toBeVisible();
    // 应至少有一个面包屑项
    const items = breadcrumb.locator('span');
    await expect(items.first()).toBeVisible();
  });

  test('侧边菜单展开/收起', async ({ page }) => {
    const menu = page.locator('.aside');

    // 收起菜单
    const collapseBtn = menu.locator('.collapse-btn');
    await collapseBtn.click();
    await expect(menu).toHaveClass(/is-collapse/);

    // 展开菜单
    await collapseBtn.click();
    await expect(menu).not.toHaveClass(/is-collapse/);
  });
});
