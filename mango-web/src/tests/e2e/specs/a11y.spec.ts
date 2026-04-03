import { test, expect } from '@playwright/test';

test.describe('无障碍测试', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
  });

  test('登录页键盘导航', async ({ page }) => {
    // Tab 应能顺序聚焦到用户名输入框
    await page.keyboard.press('Tab');
    const usernameInput = page.locator('input[placeholder*="用户名"]');
    await expect(usernameInput).toBeFocused();

    // 继续 Tab 应聚焦到密码输入框
    await page.keyboard.press('Tab');
    const passwordInput = page.locator('input[placeholder*="密码"]');
    await expect(passwordInput).toBeFocused();

    // 继续 Tab 应聚焦到登录按钮
    await page.keyboard.press('Tab');
    const loginBtn = page.locator('button:has-text("登录")');
    await expect(loginBtn).toBeFocused();
  });

  test('登录按钮可通过回车触发', async ({ page }) => {
    await page.fill('input[placeholder*="用户名"]', 'admin');
    await page.fill('input[placeholder*="密码"]', 'admin123');

    // 聚焦登录按钮并按回车
    await page.locator('button:has-text("登录")').focus();
    await page.keyboard.press('Enter');

    // 应触发登录
    await page.waitForURL('**/home**', { timeout: 5000 });
  });

  test('ARIA 属性存在性检查', async ({ page }) => {
    // 按钮应有 aria-label（无文本图标按钮）
    const buttons = page.locator('button');
    const count = await buttons.count();

    for (let i = 0; i < count; i++) {
      const btn = buttons.nth(i);
      const text = await btn.textContent();
      const ariaLabel = await btn.getAttribute('aria-label');

      // 无文本的按钮必须有 aria-label
      if (!text || !text.trim()) {
        expect(ariaLabel).toBeTruthy();
      }
    }
  });

  test('表单输入框关联标签', async ({ page }) => {
    const usernameInput = page.locator('input[placeholder*="用户名"]');
    const passwordInput = page.locator('input[placeholder*="密码"]');

    // 输入框应有 aria-label 或关联的 label
    const usernameAriaLabel = await usernameInput.getAttribute('aria-label');
    const usernameId = await usernameInput.getAttribute('id');
    expect(usernameAriaLabel || usernameId).toBeTruthy();

    const passwordAriaLabel = await passwordInput.getAttribute('aria-label');
    const passwordId = await passwordInput.getAttribute('id');
    expect(passwordAriaLabel || passwordId).toBeTruthy();
  });

  test('错误提示区域可被屏幕阅读器访问', async ({ page }) => {
    // 提交空表单触发错误
    await page.locator('button:has-text("登录")').click();

    // 错误信息应有 aria-live 属性
    const errorEl = page.locator('.el-form-item__error, [aria-live]');
    const ariaLive = await errorEl.getAttribute('aria-live');
    expect(ariaLive).toBeTruthy();
  });

  test('登录成功后页面可读性', async ({ page }) => {
    await page.fill('input[placeholder*="用户名"]', 'admin');
    await page.fill('input[placeholder*="密码"]', 'admin123');
    await page.locator('button:has-text("登录")').click();
    await page.waitForURL('**/home**', { timeout: 5000 });

    // 页面标题应存在
    const title = await page.title();
    expect(title).toBeTruthy();

    // 主要内容区域应可见
    await expect(page.locator('.main')).toBeVisible();
  });
});
