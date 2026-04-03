import { test, expect } from '@playwright/test';

test.describe('无障碍访问 (A11y) E2E 测试', () => {
  test('页面语言设置', async ({ page }) => {
    await page.goto('/#/login');
    await page.waitForLoadState('domcontentloaded');

    // html 应该有 lang 属性
    const lang = await page.getAttribute('html', 'lang');
    expect(lang).toBeTruthy();
  });

  test('登录表单可用', async ({ page }) => {
    await page.goto('/#/login');
    await page.waitForLoadState('domcontentloaded');

    // 检查登录表单存在且可用
    const usernameInput = page.locator('input[placeholder="用户名"]');
    const passwordInput = page.locator('input[placeholder="密码"]');
    const loginBtn = page.locator('button:has-text("登 录")');

    await expect(usernameInput).toBeVisible();
    await expect(passwordInput).toBeVisible();
    await expect(loginBtn).toBeVisible();
  });

  test('按钮可点击', async ({ page }) => {
    await page.goto('/#/login');
    await page.waitForLoadState('domcontentloaded');

    // 检查登录按钮存在
    const loginBtn = page.locator('button:has-text("登 录")');
    await expect(loginBtn).toBeVisible();

    // 按钮应该有文本
    const text = await loginBtn.textContent();
    expect(text?.trim()).toBeTruthy();
  });

  test('Tab 键导航', async ({ page }) => {
    await page.goto('/#/login');
    await page.waitForLoadState('domcontentloaded');

    // Tab 键应该能聚焦到可交互元素
    const focusableElements = page.locator(
      'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
    );

    const count = await focusableElements.count();
    expect(count).toBeGreaterThan(0);
  });

  test('输入框可聚焦', async ({ page }) => {
    await page.goto('/#/login');
    await page.waitForLoadState('domcontentloaded');

    // 用户名输入框应该可以聚焦
    const usernameInput = page.locator('input[placeholder="用户名"]');
    await usernameInput.focus();
    await expect(usernameInput).toBeFocused();
  });

  test('页面内容完整加载', async ({ page }) => {
    await page.goto('/#/login');
    await page.waitForLoadState('networkidle');

    // 页面标题
    const title = await page.title();
    expect(title).toBeTruthy();

    // 登录表单应该完整显示
    const loginBox = page.locator('.login-box, .login-form, form');
    await expect(loginBox.first()).toBeVisible();
  });
});
