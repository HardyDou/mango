import { test, expect } from '@playwright/test';

test.describe('登录页面 E2E 测试', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/#/login');
  });

  test('页面加载成功', async ({ page }) => {
    // 检查页面标题
    await expect(page).toHaveTitle(/Mango Admin/);

    // 检查登录表单元素存在
    await expect(page.locator('input[placeholder="用户名"]')).toBeVisible();
    await expect(page.locator('input[placeholder="密码"]')).toBeVisible();
    await expect(page.locator('button:has-text("登 录")')).toBeVisible();
  });

  test('登录表单验证', async ({ page }) => {
    // 点击登录按钮，触发验证
    await page.click('button:has-text("登 录")');

    // 应该显示验证错误
    await expect(page.locator('text=请输入用户名')).toBeVisible();
  });

  test('使用 Mock Token 登录成功', async ({ page }) => {
    // 填写表单
    await page.fill('input[placeholder="用户名"]', 'admin');
    await page.fill('input[placeholder="密码"]', 'admin123');

    // 点击登录
    await page.click('button:has-text("登 录")');

    // 应该跳转到首页
    await page.waitForURL('**/#/home', { timeout: 5000 });

    // 检查首页元素 - 首页应该有"首页"菜单
    await expect(page.locator('text=首页').first()).toBeVisible();
  });

  test('登录页面响应式布局', async ({ page }) => {
    // 移动端视图
    await page.setViewportSize({ width: 375, height: 667 });

    // 登录表单应该仍然可用
    await expect(page.locator('input[placeholder="用户名"]')).toBeVisible();
    await expect(page.locator('button:has-text("登 录")')).toBeVisible();
  });

  test('Loading 状态显示', async ({ page }) => {
    await page.route('**/api/auth/login', async (route) => {
      await new Promise((resolve) => setTimeout(resolve, 1000));
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          success: true,
          message: '登录成功',
          data: {
            token: 'mock-delayed-token',
            userInfo: {
              username: 'admin',
              nickname: 'Admin',
              roles: ['admin'],
              permissions: ['*:*:*'],
            },
          },
        }),
      });
    });

    // 填写表单
    await page.fill('input[placeholder="用户名"]', 'admin');
    await page.fill('input[placeholder="密码"]', 'admin123');

    // 点击登录
    await page.click('button:has-text("登 录")');

    // 按钮应该显示 loading 状态
    await expect(page.locator('.login-btn')).toHaveClass(/is-loading/);
    await page.waitForURL('**/#/home', { timeout: 10000 });
  });

  test('错误消息显示', async ({ page }) => {
    // 填写错误的登录信息
    await page.fill('input[placeholder="用户名"]', 'wronguser');
    await page.fill('input[placeholder="密码"]', 'wrongpassword');

    // 点击登录
    await page.click('button:has-text("登 录")');

    // 应该显示错误消息
    await expect(page.locator('text=/登录失败|用户名或密码错误/')).toBeVisible({ timeout: 3000 });
  });
});
