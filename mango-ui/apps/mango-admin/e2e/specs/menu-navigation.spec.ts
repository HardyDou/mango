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
    expect(menuBody.data).toHaveLength(2);
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
      '日志管理',
    ]);
    expect(menuBody.data[1]).toMatchObject({
      menuName: '协同办公',
      path: '/workflow',
    });
    for (const menu of collectVisibleMenus(menuBody.data)) {
      expect(menu.icon, `${menu.menuName} 必须配置菜单图标`).toBeTruthy();
    }

    await expect(page.getByText('系统管理', { exact: true }).first()).toBeVisible();
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

    await page.getByText('协同办公', { exact: true }).click();
    await expectMenuIcon(page, '任务管理');
    await expandMenuGroup(page, '任务管理');
    await expectMenuIcon(page, '我的待办');
    await expectMenuIcon(page, '我的发起');
    await expectMenuIcon(page, '我的已办');
    await expectMenuIcon(page, '抄送给我');
    await expectMenuIcon(page, '发起流程');
    await expectMenuIcon(page, '流程管理');
    await expectMenuIcon(page, '业务示例');
  });

  test('A 公司登录后只渲染机构授权范围内的系统管理与协同办公导航', async ({ page }) => {
    const menuResponsePromise = page.waitForResponse((response) => {
      const url = response.url();
      return response.status() === 200
        && url.includes('/api/authorization/menus/user')
        && url.includes('fmt=tree');
    });

    await loginPage(page, 'A公司');

    const menuResponse = await menuResponsePromise;
    const menuBody = await menuResponse.json();
    expect(menuBody.data).toHaveLength(2);
    expect(menuBody.data[0]).toMatchObject({
      menuName: '系统管理',
      path: '/system',
    });
    expect(menuBody.data[1]).toMatchObject({
      menuName: '协同办公',
      path: '/workflow',
    });
    expect(menuBody.data[0].children.map((item: { menuName: string }) => item.menuName)).toEqual([
      '权限管理',
      '日志管理',
    ]);
    expect(menuBody.data[1].children.map((item: { menuName: string }) => item.menuName)).toEqual([
      '任务管理',
      '发起流程',
      '流程管理',
      '业务示例',
    ]);
    for (const menu of collectVisibleMenus(menuBody.data)) {
      expect(menu.icon, `${menu.menuName} 必须配置菜单图标`).toBeTruthy();
    }

    await expectMenuIcon(page, '权限管理');
    await expectMenuIcon(page, '日志管理');
    await expectMenuIcon(page, '协同办公');
    await expect(page.getByText('应用管理')).toHaveCount(0);
  });
});
