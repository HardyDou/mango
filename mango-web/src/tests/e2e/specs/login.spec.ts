import { test, expect } from '@playwright/test';

test.describe('登录功能', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
  });

  test('登录页正确渲染', async ({ page }) => {
    await expect(page.locator('h1')).toContainText('Mango Admin');
    await expect(page.locator('.form-title')).toBeVisible();
    await expect(page.locator('input[placeholder*="用户名"]')).toBeVisible();
    await expect(page.locator('input[placeholder*="密码"]')).toBeVisible();
    await expect(page.locator('button:has-text("登录")')).toBeVisible();
  });

  test('空表单提交失败', async ({ page }) => {
    const loginBtn = page.locator('button:has-text("登录")');
    await loginBtn.click();
    // 表单验证应阻止提交
    await expect(page.locator('.el-form-item__error')).toBeVisible();
  });

  test('登录按钮 loading 状态', async ({ page }) => {
    const loginBtn = page.locator('button:has-text("登录")');
    await page.fill('input[placeholder*="用户名"]', 'admin');
    await page.fill('input[placeholder*="密码"]', 'anypassword');

    // 点击后应有 loading 状态
    await loginBtn.click();
    await expect(loginBtn.locator('.el-icon-loading')).toBeVisible();
  });

  test('登录成功跳转首页', async ({ page }) => {
    // 使用 Mock 账号登录
    await page.fill('input[placeholder*="用户名"]', 'admin');
    await page.fill('input[placeholder*="密码"]', 'admin123');
    await page.locator('button:has-text("登录")').click();

    // 等待跳转
    await page.waitForURL('**/home**', { timeout: 5000 });
    await expect(page.locator('.el-message--success')).toContainText('登录成功');
  });

  test('登录失败显示错误提示', async ({ page }) => {
    await page.fill('input[placeholder*="用户名"]', 'wronguser');
    await page.fill('input[placeholder*="密码"]', 'wrongpassword');
    await page.locator('button:has-text("登录")').click();

    // 应显示错误提示，3秒后自动消失
    const errorMsg = page.locator('.el-alert');
    await expect(errorMsg).toBeVisible({ timeout: 3000 });
    await expect(errorMsg).toContainText('登录失败');
  });
});
