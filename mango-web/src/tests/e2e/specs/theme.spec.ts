import { test, expect } from '@playwright/test';

test.describe('主题系统', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[placeholder*="用户名"]', 'admin');
    await page.fill('input[placeholder*="密码"]', 'admin123');
    await page.locator('button:has-text("登录")').click();
    await page.waitForURL('**/home**', { timeout: 5000 });
  });

  test('深色模式切换', async ({ page }) => {
    // 打开设置面板
    await page.locator('.header-setting').click();

    // 切换到深色模式
    await page.locator('text=深色模式').click();

    // html 标签应添加 dark class
    await expect(page.locator('html')).toHaveClass(/dark/);

    // 切换回浅色模式
    await page.locator('.header-setting').click();
    await page.locator('text=浅色模式').click();
    await expect(page.locator('html')).not.toHaveClass(/dark/);
  });

  test('主题配置持久化', async ({ page }) => {
    // 设置主题色
    await page.locator('.header-setting').click();
    await page.locator('.color-primary').click();
    await page.locator('text=科技蓝').click();

    // 刷新页面
    await page.reload();
    await page.waitForURL('**/home**');

    // 主题色应保持
    await page.locator('.header-setting').click();
    await expect(page.locator('text=科技蓝')).toHaveClass(/is-active/);
  });

  test('主题配置面板显示', async ({ page }) => {
    // 打开设置面板
    await page.locator('.header-setting').click();
    const panel = page.locator('.theme-panel');

    // 面板应显示主题配置选项
    await expect(panel.locator('text=主题色')).toBeVisible();
    await expect(panel.locator('text=深色模式')).toBeVisible();
    await expect(panel.locator('text=灰色模式')).toBeVisible();
  });

  test('灰色模式切换', async ({ page }) => {
    await page.locator('.header-setting').click();

    // 开启灰色模式
    await page.locator('text=灰色模式').click();
    await expect(page.locator('html')).toHaveClass(/grey/);

    // 关闭灰色模式
    await page.locator('text=灰色模式').click();
    await expect(page.locator('html')).not.toHaveClass(/grey/);
  });

  test('色弱模式切换', async ({ page }) => {
    await page.locator('.header-setting').click();

    // 开启色弱模式
    await page.locator('text=色弱模式').click();
    await expect(page.locator('html')).toHaveClass(/valentine/);

    // 关闭色弱模式
    await page.locator('text=色弱模式').click();
    await expect(page.locator('html')).not.toHaveClass(/valentine/);
  });
});
