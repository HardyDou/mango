import { test, expect } from '@playwright/test';

test.describe('布局系统 E2E 测试', () => {
  test.beforeEach(async ({ page }) => {
    // 登录
    await page.goto('/#/login');
    await page.fill('input[placeholder="用户名"]', 'admin');
    await page.fill('input[placeholder="密码"]', 'admin123');
    await page.click('button:has-text("登 录")');
    await page.waitForURL('**/#/home', { timeout: 5000 });
  });

  test('默认布局结构', async ({ page }) => {
    // 检查顶部导航栏
    await expect(page.locator('.layout-navbars-container')).toBeVisible();

    // 检查侧边栏
    await expect(page.locator('.layout-aside')).toBeVisible();

    // 检查主内容区
    await expect(page.locator('.layout-main')).toBeVisible();
  });

  test('经典布局 (classic)', async ({ page }) => {
    // 检查标签导航是否隐藏（经典布局）
    await expect(page.locator('.tags-view-container')).not.toBeVisible();
  });

  test('横向布局 (transverse)', async ({ page }) => {
    // 切换到横向布局（通过修改 Pinia store）
    await page.evaluate(() => {
      // @ts-ignore
      window.__MANGO_PINIA__.themeConfig.layout = 'transverse';
    });
    await page.reload();

    // 检查横向菜单
    await expect(page.locator('.nav-menu-horizontal')).toBeVisible();
  });

  test('分栏布局 (columns)', async ({ page }) => {
    // 切换到分栏布局
    await page.evaluate(() => {
      // @ts-ignore
      window.__MANGO_PINIA__.themeConfig.layout = 'columns';
    });
    await page.reload();

    // 检查分栏侧边栏
    await expect(page.locator('.layout-columns-aside')).toBeVisible();
  });

  test('侧边栏折叠/展开', async ({ page }) => {
    // 点击折叠按钮
    const collapseBtn = page.locator('.layout-logo .collapse-icon').first();
    await collapseBtn.click();

    // 侧边栏应该有折叠样式
    await expect(page.locator('.layout-aside')).toHaveClass(/is-collapse/);

    // 再次点击展开
    await collapseBtn.click();
    await expect(page.locator('.layout-aside')).not.toHaveClass(/is-collapse/);
  });

  test('标签导航功能', async ({ page }) => {
    // 切换到非经典布局以显示标签导航
    await page.evaluate(() => {
      // @ts-ignore
      window.__MANGO_PINIA__.themeConfig.layout = 'defaults';
      window.__MANGO_PINIA__.themeConfig.isTagsview = true;
    });
    await page.reload();

    // 检查标签导航
    await expect(page.locator('.tags-view-container')).toBeVisible();

    // 检查首页标签
    await expect(page.locator('.tags-view-item:has-text("首页")')).toBeVisible();
  });

  test('标签页右键菜单', async ({ page }) => {
    // 切换到非经典布局
    await page.evaluate(() => {
      // @ts-ignore
      window.__MANGO_PINIA__.themeConfig.layout = 'defaults';
      window.__MANGO_PINIA__.themeConfig.isTagsview = true;
    });
    await page.reload();

    // 右键点击标签
    await page.click('.tags-view-item', { button: 'right' });

    // 上下文菜单应该可见
    await expect(page.locator('.context-menu')).toBeVisible();
    await expect(page.locator('.context-menu-item:has-text("关闭")')).toBeVisible();
    await expect(page.locator('.context-menu-item:has-text("刷新")')).toBeVisible();
  });

  test('面包屑导航', async ({ page }) => {
    // 访问一个子页面
    await page.goto('/#/system/user');
    await page.waitForLoadState('networkidle');

    // 检查面包屑
    await expect(page.locator('.layout-breadcrumb')).toBeVisible();
  });

  test('用户下拉菜单', async ({ page }) => {
    // 点击用户头像
    await page.click('.layout-breadcrumb-user');

    // 下拉菜单应该可见
    await expect(page.locator('.el-dropdown-menu')).toBeVisible();
    await expect(page.locator('text=个人中心')).toBeVisible();
    await expect(page.locator('text=修改密码')).toBeVisible();
    await expect(page.locator('text=退出登录')).toBeVisible();
  });

  test('1000px 断点响应式', async ({ page }) => {
    // 小于 1000px 应该是移动端布局
    await page.setViewportSize({ width: 800, height: 600 });
    await page.reload();

    // 应该显示移动端提示或适配布局
    // 具体行为取决于实现
    await page.waitForLoadState('domcontentloaded');
  });
});
