import { expect, test } from '@playwright/test';

async function expandMenuGroup(page: import('@playwright/test').Page, name: string) {
  const group = page.getByText(name, { exact: true }).first();
  await expect(group).toBeVisible({ timeout: 10000 });
  const expanded = await group.getAttribute('aria-expanded');
  if (expanded !== 'true') {
    await group.click();
  }
}

function collectVisibleMenus(menus: any[]): any[] {
  return menus
    .flatMap((menu) => [menu, ...collectVisibleMenus(menu.children || [])])
    .filter((menu) => menu.menuType !== 3 && menu.visible !== 0);
}

async function expectMenuIcon(page: import('@playwright/test').Page, name: string) {
  const visibleMenuSelectors = [
    '.el-menu-item',
    '.el-sub-menu__title',
    '.layout-top-system-item',
    '.layout-columns-aside li',
  ]
    .map((selector) => `${selector}:visible`)
    .join(', ');
  const menuItem = page.locator(visibleMenuSelectors, { hasText: name }).first();
  await expect(menuItem, `${name} 菜单必须可见`).toBeVisible({ timeout: 10000 });
  await expect(menuItem.locator('.el-icon svg:visible, img:visible').first(), `${name} 必须渲染菜单图标`).toBeVisible();
}

async function expectNoticeBell(page: import('@playwright/test').Page) {
  await expect(page.locator('.notice-bell'), '右上角我的消息入口必须可见').toBeVisible({ timeout: 10000 });
}

async function openTopMenu(page: import('@playwright/test').Page, name: string) {
  await page.getByRole('button', { name }).evaluate((button: HTMLButtonElement) => button.click());
}

async function loginPage(page: import('@playwright/test').Page) {
  await page.goto('/#/login');
  await page.fill('input[placeholder="请输入用户名"]', 'admin');
  await page.fill('input[placeholder="请输入密码"]', 'admin123');
  const accountTenantsResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/api/auth/login-institutions') && response.status() === 200,
  );
  await page.locator('input[placeholder="请输入密码"]').blur();
  await accountTenantsResponsePromise;
  await expect(page.locator('.tenant-select')).toHaveCount(0);
  await page.locator('.login-btn').click();
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

test.describe('用户菜单导航 E2E', () => {
  test('@p0 @menu 通知管理和审批管理归入平台能力且保留原路由', async ({ page }, testInfo) => {
    const menuResponsePromise = page.waitForResponse((response) => {
      const url = response.url();
      return response.status() === 200 && url.includes('/api/authorization/menus/user') && url.includes('fmt=tree');
    });

    await loginPage(page);

    const menuResponse = await menuResponsePromise;
    const menuBody = await menuResponse.json();
    const topLevelCodes = menuBody.data.map((item: { menuCode: string }) => item.menuCode);
    expect(topLevelCodes).not.toContain('notice');
    expect(topLevelCodes).not.toContain('workflow');

    const platformCapabilities = menuBody.data.find((item: { menuCode: string }) => item.menuCode === 'data');
    expect(platformCapabilities).toBeDefined();
    expect(
      platformCapabilities.children.find((item: { menuCode: string }) => item.menuCode === 'notice'),
    ).toMatchObject({ menuName: '通知管理', path: '/notice' });
    expect(
      platformCapabilities.children.find((item: { menuCode: string }) => item.menuCode === 'workflow'),
    ).toMatchObject({ menuName: '审批管理', path: '/workflow' });

    await openTopMenu(page, '平台能力');
    await expectMenuIcon(page, '通知管理');
    await expectMenuIcon(page, '审批管理');
    await testInfo.attach('platform-capability-menu', {
      body: await page.screenshot(),
      contentType: 'image/png',
    });
  });

  test('@p0 @system 网站配置菜单打开对应配置页面', async ({ page }, testInfo) => {
    const consoleErrors: string[] = [];
    const pageErrors: string[] = [];
    const failedResponses: string[] = [];
    let inspectRuntimeErrors = false;
    page.on('console', (message) => {
      if (inspectRuntimeErrors && message.type() === 'error') {
        consoleErrors.push(message.text());
      }
    });
    page.on('pageerror', (error) => {
      if (inspectRuntimeErrors) {
        pageErrors.push(error.message);
      }
    });
    page.on('response', (response) => {
      if (inspectRuntimeErrors && response.status() >= 400) {
        failedResponses.push(`${response.status()} ${response.url()}`);
      }
    });

    const menuResponsePromise = page.waitForResponse((response) => {
      const url = response.url();
      return response.status() === 200 && url.includes('/api/authorization/menus/user') && url.includes('fmt=tree');
    });
    await loginPage(page);

    const menuBody = await (await menuResponsePromise).json();
    const systemMenu = menuBody.data.find((item: { menuCode: string }) => item.menuCode === 'system');
    expect(
      systemMenu.children.find((item: { menuCode: string }) => item.menuCode === 'system:admin-branding'),
    ).toMatchObject({
      menuName: '网站配置',
      path: '/system/admin-branding',
    });

    inspectRuntimeErrors = true;
    await openTopMenu(page, '系统管理');
    await page.getByText('网站配置', { exact: true }).first().click();
    await page.waitForURL('**/#/system/admin-branding');
    const brandingPage = page.locator('[data-page="system.admin-branding"]');
    await expect(brandingPage).toBeVisible();
    await expect(brandingPage).toHaveAttribute('data-state', 'ready');
    await expect(brandingPage).toContainText('网站配置');
    await expect(brandingPage.getByRole('button', { name: '保存配置' })).toBeEnabled();
    await expect(page.getByText('后台品牌配置', { exact: true })).toHaveCount(0);
    expect(consoleErrors, '网站配置页面不应产生 console error').toEqual([]);
    expect(pageErrors, '网站配置页面不应产生 page error').toEqual([]);
    expect(failedResponses, '网站配置页面不应产生 4xx/5xx').toEqual([]);
    const screenshotPath = testInfo.outputPath('website-config-page.png');
    await page.screenshot({ path: screenshotPath });
    await testInfo.attach('website-config-page', { path: screenshotPath, contentType: 'image/png' });
  });

  test('芒果集团登录后使用后端用户菜单树渲染完整管理导航', async ({ page }) => {
    const menuResponsePromise = page.waitForResponse((response) => {
      const url = response.url();
      return response.status() === 200 && url.includes('/api/authorization/menus/user') && url.includes('fmt=tree');
    });

    await loginPage(page);
    await expectNoticeBell(page);

    const menuResponse = await menuResponsePromise;
    const menuBody = await menuResponse.json();
    expect(menuBody.data).toHaveLength(4);
    expect(menuBody.data[0]).toMatchObject({
      menuName: '系统管理',
      path: '/system',
    });
    expect(menuBody.data[0].children.map((item: { menuName: string }) => item.menuName)).toEqual([
      '权限管理',
      '应用管理',
      '字典管理',
      '参数配置',
      '行政区划',
      '网站配置',
      '日志管理',
      '系统维护',
      '业务域',
      '第三方登录',
    ]);
    expect(menuBody.data[1]).toMatchObject({
      menuName: '平台能力',
      path: '/data',
    });
    expect(menuBody.data[1].children.map((item: { menuName: string }) => item.menuName)).toEqual([
      '日历管理',
      '编号管理',
      '文件管理',
      '模板管理',
      '审批管理',
      '网址管理',
      '通知管理',
      '支付管理',
      '任务管理',
      '内容运营',
      '首页管理',
      'AI 管理',
    ]);
    const approvalCenter = menuBody.data[1].children.find((item: { menuCode: string }) => item.menuCode === 'workflow');
    expect(approvalCenter).toMatchObject({ path: '/workflow' });
    const noticeCenter = menuBody.data[1].children.find((item: { menuCode: string }) => item.menuCode === 'notice');
    expect(noticeCenter).toMatchObject({ path: '/notice' });
    expect(
      noticeCenter.children
        .filter((item: { visible: number }) => item.visible !== 0)
        .map((item: { menuName: string }) => item.menuName),
    ).toEqual(['公告管理', '消息配置', '发送任务', '渠道配置', '接收消息', '发送记录', '失败重试']);
    expect(menuBody.data[2]).toMatchObject({
      menuName: '网址导航',
      path: '/link',
    });
    expect(menuBody.data[2].children.map((item: { menuName: string }) => item.menuName)).toEqual([
      '我的收藏',
      '我的分类',
      '我的网址',
    ]);
    expect(menuBody.data[3]).toMatchObject({
      menuName: '消息中心',
      path: '/message-center',
    });
    expect(menuBody.data[3].children.map((item: { menuName: string }) => item.menuName)).toEqual([
      '我的消息',
      '系统公告',
      '通知设置',
    ]);
    for (const menu of collectVisibleMenus(menuBody.data)) {
      expect(menu.icon, `${menu.menuName} 必须配置菜单图标`).toBeTruthy();
    }

    await openTopMenu(page, '系统管理');
    await expectMenuIcon(page, '权限管理');
    await expectMenuIcon(page, '应用管理');
    await expectMenuIcon(page, '字典管理');
    await expectMenuIcon(page, '参数配置');
    await expectMenuIcon(page, '网站配置');
    await expectMenuIcon(page, '行政区划');
    await expectMenuIcon(page, '日志管理');
    await expectMenuIcon(page, '系统维护');
    await expectMenuIcon(page, '业务域');
    await expectMenuIcon(page, '第三方登录');

    await expandMenuGroup(page, '权限管理');
    await expectMenuIcon(page, '套餐管理');
    await expect(page.getByText('租户管理')).toHaveCount(0);
    await expectMenuIcon(page, '角色管理');
    await expectMenuIcon(page, '菜单管理');

    await openTopMenu(page, '平台能力');
    await expectMenuIcon(page, '审批管理');
    await expandMenuGroup(page, '审批管理');
    await expectMenuIcon(page, '流程办理');
    await expectMenuIcon(page, '流程管理');
    await expectMenuIcon(page, '业务示例');

    await expectMenuIcon(page, '日历管理');
    await expectMenuIcon(page, '编号管理');
    await expectMenuIcon(page, '文件管理');
    await expandMenuGroup(page, '文件管理');
    await expectMenuIcon(page, '存储配置');
    await expectMenuIcon(page, '文件配置');
    await expectMenuIcon(page, '模板管理');
    await expandMenuGroup(page, '模板管理');
    await expectMenuIcon(page, '模板分类');
    await expectMenuIcon(page, '模板列表');
    await expectMenuIcon(page, '渲染记录');
    await expectMenuIcon(page, '任务管理');
    await expandMenuGroup(page, '任务管理');
    await expectMenuIcon(page, '任务定义');
    await expectMenuIcon(page, '执行实例');
    await expectMenuIcon(page, 'Worker 节点');
    await expectMenuIcon(page, '告警规则');
    await expectMenuIcon(page, '运行状态');
    await expectMenuIcon(page, 'AI 管理');

    await openTopMenu(page, '网址导航');
    await expectMenuIcon(page, '我的收藏');
    await expectMenuIcon(page, '我的分类');
    await expectMenuIcon(page, '我的网址');

    await openTopMenu(page, '平台能力');
    await expectMenuIcon(page, '通知管理');
    await expandMenuGroup(page, '通知管理');
    await expectMenuIcon(page, '公告管理');
    const noticeTasksResponsePromise = page.waitForResponse((response) => {
      return response.url().includes('/api/notice/tasks') && response.request().method() === 'GET';
    });
    await page
      .locator('.el-menu-item, .el-sub-menu__title, .layout-columns-aside li', { hasText: '发送任务' })
      .first()
      .click();
    const noticeTasksResponse = await noticeTasksResponsePromise;
    expect(noticeTasksResponse.status(), '发送任务页面查询接口必须有权限').toBe(200);
    await expect(page.getByRole('heading', { name: '发送任务' })).toBeVisible();
    await expectMenuIcon(page, '消息配置');
    await expectMenuIcon(page, '发送任务');
    await expectMenuIcon(page, '渠道配置');
    await expectMenuIcon(page, '接收消息');
    await expectMenuIcon(page, '发送记录');
    await expectMenuIcon(page, '失败重试');
  });
});
