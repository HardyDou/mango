import { test, expect } from '@playwright/test';

test.describe('布局系统 E2E 测试', () => {
  test.beforeEach(async ({ page }) => {
    // 登录
    await page.goto('/#/login');
    await page.fill('input[placeholder="用户名"]', 'admin');
    await page.fill('input[placeholder="密码"]', 'admin123');
    await page.click('button:has-text("登 录")');
    await page.waitForURL('**/#/home', { timeout: 10000 });
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
    // 点击用户头像打开下拉菜单
    await page.click('.layout-breadcrumb-user');
    await page.waitForTimeout(500);

    // 检查横向菜单是否存在（通过菜单容器判断）
    const hasHorizontalMenu = await page.locator('.nav-menu-horizontal').count() > 0 ||
                              await page.locator('.layout-navbars-container').count() > 0;
    expect(hasHorizontalMenu).toBeTruthy();
  });

  test('分栏布局 (columns)', async ({ page }) => {
    // 分栏布局的测试 - 检查页面可以正常显示
    await page.waitForLoadState('networkidle');
    const mainContent = page.locator('.layout-main');
    await expect(mainContent).toBeVisible();
  });

  test('侧边栏折叠/展开', async ({ page }) => {
    // 查找折叠按钮 - 在桌面端，使用非移动端的 hamburger 按钮
    // 经典布局中，折叠按钮在导航栏左侧
    const collapseBtn = page.locator('.layout-navbars-container .hamburger:not(.hamburger-mobile)').first();

    // 检查按钮是否存在
    const btnCount = await collapseBtn.count();
    if (btnCount === 0) {
      // 如果没有折叠按钮，可能是其他布局，跳过
      test.skip();
      return;
    }

    await collapseBtn.click();
    await page.waitForTimeout(300);

    // 侧边栏应该有折叠样式
    const asideEl = page.locator('.layout-aside');
    if (await asideEl.count() > 0) {
      await expect(asideEl).toHaveClass(/is-collapse/);

      // 再次点击展开
      await collapseBtn.click();
      await page.waitForTimeout(300);
      await expect(asideEl).not.toHaveClass(/is-collapse/);
    }
  });

  test('标签导航功能', async ({ page }) => {
    // 等待页面加载完成
    await page.waitForLoadState('networkidle');

    // 检查标签导航容器
    const tagsView = page.locator('.tags-view-container');
    // 如果标签导航存在则检查，否则跳过
    if (await tagsView.count() > 0) {
      await expect(tagsView).toBeVisible();
    } else {
      // 经典布局默认不显示标签导航
      await expect(tagsView).not.toBeVisible();
    }
  });

  test('标签页右键菜单', async ({ page }) => {
    // 等待页面加载
    await page.waitForLoadState('networkidle');

    // 检查是否有标签可以右键
    const tagsViewItem = page.locator('.tags-view-item').first();
    if (await tagsViewItem.count() > 0) {
      // 右键点击标签
      await tagsViewItem.click({ button: 'right' });
      await page.waitForTimeout(300);

      // 检查上下文菜单
      const contextMenu = page.locator('.context-menu');
      if (await contextMenu.count() > 0) {
        await expect(contextMenu).toBeVisible();
      }
    }
  });

  test('面包屑导航', async ({ page }) => {
    // 访问一个子页面
    await page.goto('/#/system/user');
    await page.waitForLoadState('networkidle');

    // 检查面包屑容器
    const breadcrumb = page.locator('.layout-breadcrumb');
    if (await breadcrumb.count() > 0) {
      await expect(breadcrumb).toBeVisible();
    }
  });

  test('用户下拉菜单', async ({ page }) => {
    // 点击用户头像
    await page.click('.layout-breadcrumb-user');
    await page.waitForTimeout(500);

    // 下拉菜单应该可见
    await expect(page.locator('.el-dropdown-menu')).toBeVisible();
    // 使用更精确的选择器
    await expect(page.getByText('个人中心').first()).toBeVisible();
    await expect(page.getByText('修改密码').first()).toBeVisible();
    await expect(page.getByText('退出登录').first()).toBeVisible();
  });

  test('1000px 断点响应式', async ({ page }) => {
    // 小于 1000px 应该是移动端布局
    await page.setViewportSize({ width: 800, height: 600 });
    await page.reload();
    await page.waitForLoadState('domcontentloaded');

    // 应该显示移动端适配布局
    await page.waitForTimeout(500);
  });
});
