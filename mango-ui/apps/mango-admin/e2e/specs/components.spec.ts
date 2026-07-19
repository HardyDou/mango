import { test, expect } from '@playwright/test';

test.describe('前端组件库 E2E 测试', () => {
  test.beforeEach(async ({ page }) => {
    // 登录
    await page.goto('/#/login');
    await expect(page.locator('input[placeholder="用户名"]')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('input[placeholder="密码"]')).toBeVisible({ timeout: 10000 });
    await page.fill('input[placeholder="用户名"]', 'admin');
    await page.fill('input[placeholder="密码"]', 'admin123');
    await page.click('button:has-text("登 录")');

    // 等待登录完成并跳转
    await page.waitForURL('**/#/home', { timeout: 10000 });
    await expect(page.locator('.layout-main, .home-container').first()).toBeVisible({ timeout: 10000 });
  });

  test('@p1 @components 文件上传组件演示页面可访问', async ({ page }) => {
    await page.goto('/#/components/upload');

    await expect(page).toHaveURL(/#\/components\/upload$/);
    await expect(page.locator('.upload-view')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'MUpload 文件上传' })).toBeVisible();
    await expect(page.getByTestId('mixed-upload-panel')).toBeVisible();
  });

  test('@p1 @components 首页组件展示', async ({ page }) => {
    // 在首页检查是否有组件相关内容
    await expect(page.locator('.layout-main, .home-container').first()).toBeVisible({ timeout: 5000 });
  });

  test('@p1 @components ECharts 组件存在', async ({ page }) => {
    await page.goto('/#/components/charts');

    await expect(page).toHaveURL(/#\/components\/charts$/);
    await expect(page.locator('.charts-view')).toBeVisible();
    await expect(page.getByRole('heading', { name: '数据图表' })).toBeVisible();
    await expect(page.locator('.echarts-container').first()).toBeVisible();
  });
});
