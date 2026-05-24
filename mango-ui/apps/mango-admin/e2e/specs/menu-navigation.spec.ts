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
  return menus.flatMap((menu) => [
    menu,
    ...collectVisibleMenus(menu.children || []),
  ]).filter((menu) => menu.menuType !== 3 && menu.visible !== 0);
}

async function expectMenuIcon(page: import('@playwright/test').Page, name: string) {
  const menuItem = page.locator(
    [
      '.el-menu-item',
      '.el-sub-menu__title',
      '.layout-top-system-item',
      '.layout-columns-aside li',
    ].join(', '),
    { hasText: name },
  ).first();
  await expect(menuItem, `${name} 菜单必须可见`).toBeVisible({ timeout: 10000 });
  await expect(menuItem.locator('.el-icon svg, img').first(), `${name} 必须渲染菜单图标`).toBeVisible();
}

async function openTopMenu(page: import('@playwright/test').Page, name: string) {
  await page.getByRole('button', { name }).evaluate((button: HTMLButtonElement) => button.click());
}

function childMenuNames(menu: { children?: Array<{ menuName: string }> }) {
  return (menu.children || []).map((item) => item.menuName);
}

async function loginPage(
  page: import('@playwright/test').Page,
  tenantName = '芒果集团',
) {
  await page.goto('/#/login');
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  const accountTenantsResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/auth/login-institutions') && response.status() === 200
  );
  await page.locator('input[placeholder="密码"]').blur();
  await accountTenantsResponsePromise;
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: new RegExp(tenantName) }).click();
  await page.locator('.login-btn').click();
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

test.describe('用户菜单导航 E2E', () => {
  test('芒果集团登录后使用后端用户菜单树渲染完整系统管理导航', async ({ page }) => {
    const menuResponsePromise = page.waitForResponse((response) => {
      const url = response.url();
      return response.status() === 200
        && url.includes('/api/authorization/menus/user')
        && url.includes('fmt=tree');
    });

    await loginPage(page);

    const menuResponse = await menuResponsePromise;
    const menuBody = await menuResponse.json();
    expect(menuBody.data).toHaveLength(4);
    expect(menuBody.data[0]).toMatchObject({
      menuName: '系统管理',
      path: '/system',
    });
    expect(childMenuNames(menuBody.data[0])).toEqual([
      '权限管理',
      '应用管理',
      '字典管理',
      '参数配置',
      '行政区划',
      '日志管理',
    ]);
    expect(menuBody.data[1]).toMatchObject({
      menuName: '审批中心',
      path: '/workflow',
    });
    expect(menuBody.data[2]).toMatchObject({
      menuName: '平台能力',
      path: '/data',
    });
    const platformMenus = childMenuNames(menuBody.data[2]);
    expect(platformMenus).toEqual(expect.arrayContaining([
      '日历管理',
      '编号规则',
      '文件管理',
    ]));
    expect(menuBody.data[3]).toMatchObject({
      menuName: '支付中心',
      path: '/payment',
    });
    expect(childMenuNames(menuBody.data[3])).toEqual([
      '支付管理',
      '租户收银台',
    ]);
    for (const menu of collectVisibleMenus(menuBody.data)) {
      expect(menu.icon, `${menu.menuName} 必须配置菜单图标`).toBeTruthy();
    }

    await openTopMenu(page, '系统管理');
    await expectMenuIcon(page, '权限管理');
    await expectMenuIcon(page, '应用管理');
    await expectMenuIcon(page, '字典管理');
    await expectMenuIcon(page, '参数配置');
    await expectMenuIcon(page, '行政区划');
    await expectMenuIcon(page, '日志管理');

    await expandMenuGroup(page, '权限管理');
    await expectMenuIcon(page, '套餐管理');
    await expectMenuIcon(page, '机构管理');
    await expectMenuIcon(page, '角色管理');
    await expectMenuIcon(page, '菜单管理');

    await openTopMenu(page, '审批中心');
    await expectMenuIcon(page, '流程办理');
    await expandMenuGroup(page, '流程办理');
    await expectMenuIcon(page, '发起流程');
    await expectMenuIcon(page, '我的待办');
    await expectMenuIcon(page, '我的申请');
    await expectMenuIcon(page, '我的已办');
    await expectMenuIcon(page, '抄送给我');
    await expectMenuIcon(page, '流程管理');
    await expandMenuGroup(page, '流程管理');
    await expectMenuIcon(page, '流程模板');
    await expectMenuIcon(page, '流程定义');
    await expectMenuIcon(page, '业务示例');

    await openTopMenu(page, '平台能力');
    await expectMenuIcon(page, '日历管理');
    await expectMenuIcon(page, '编号规则');
    await expectMenuIcon(page, '文件管理');
    await expandMenuGroup(page, '文件管理');
    await expectMenuIcon(page, '存储配置');
    await expectMenuIcon(page, '文件配置');
    if (platformMenus.includes('模板管理')) {
      await expectMenuIcon(page, '模板管理');
      await expandMenuGroup(page, '模板管理');
      await expectMenuIcon(page, '模板分类');
      await expectMenuIcon(page, '模板列表');
      await expectMenuIcon(page, '渲染记录');
    }

    await openTopMenu(page, '支付中心');
    await expectMenuIcon(page, '支付管理');
    await expectMenuIcon(page, '租户收银台');
  });

  test('A 公司登录后只渲染机构授权范围内的系统管理、审批中心与平台能力导航', async ({ page }) => {
    const menuResponsePromise = page.waitForResponse((response) => {
      const url = response.url();
      return response.status() === 200
        && url.includes('/api/authorization/menus/user')
        && url.includes('fmt=tree');
    });

    await loginPage(page, 'A公司');

    const menuResponse = await menuResponsePromise;
    const menuBody = await menuResponse.json();
    expect(menuBody.data).toHaveLength(3);
    expect(menuBody.data[0]).toMatchObject({
      menuName: '系统管理',
      path: '/system',
    });
    expect(menuBody.data[1]).toMatchObject({
      menuName: '审批中心',
      path: '/workflow',
    });
    expect(menuBody.data[2]).toMatchObject({
      menuName: '平台能力',
      path: '/data',
    });
    expect(childMenuNames(menuBody.data[0])).toEqual([
      '权限管理',
      '日志管理',
    ]);
    expect(childMenuNames(menuBody.data[1])).toEqual([
      '流程办理',
      '业务示例',
    ]);
    const companyPlatformMenus = childMenuNames(menuBody.data[2]);
    expect(companyPlatformMenus).toEqual(expect.arrayContaining([
      '日历管理',
      '编号规则',
    ]));
    for (const menu of collectVisibleMenus(menuBody.data)) {
      expect(menu.icon, `${menu.menuName} 必须配置菜单图标`).toBeTruthy();
    }

    await openTopMenu(page, '系统管理');
    await expectMenuIcon(page, '权限管理');
    await expectMenuIcon(page, '日志管理');

    await openTopMenu(page, '审批中心');
    await expectMenuIcon(page, '流程办理');
    await expectMenuIcon(page, '审批中心');

    await openTopMenu(page, '平台能力');
    await expectMenuIcon(page, '日历管理');
    await expectMenuIcon(page, '编号规则');
    if (companyPlatformMenus.includes('模板管理')) {
      await expectMenuIcon(page, '模板管理');
      await expandMenuGroup(page, '模板管理');
      await expectMenuIcon(page, '模板分类');
      await expectMenuIcon(page, '模板列表');
      await expectMenuIcon(page, '渲染记录');
    }

    await expect(page.getByText('应用管理')).toHaveCount(0);
    await expect(page.getByText('文件管理')).toHaveCount(0);
  });
});
