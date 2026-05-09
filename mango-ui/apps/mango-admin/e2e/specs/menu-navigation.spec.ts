import { expect, test } from '@playwright/test';

async function expandMenuGroup(page: import('@playwright/test').Page, name: string) {
  const group = page.getByRole('menuitem', { name: new RegExp(name) }).first();
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
  const menuItem = page.getByRole('menuitem', { name: new RegExp(name) }).first();
  await expect(menuItem).toBeVisible({ timeout: 10000 });
  await expect(menuItem.locator('.menu-icon svg').first()).toBeVisible({ timeout: 10000 });
}

test.describe('用户菜单导航 E2E', () => {
  test('芒果集团登录后使用后端用户菜单树渲染完整系统管理导航', async ({ page }) => {
    await page.goto('/#/login');

    const menuResponsePromise = page.waitForResponse((response) => {
      const url = response.url();
      return response.status() === 200
        && url.includes('/api/authorization/menus/user')
        && url.includes('fmt=tree');
    });

    await page.fill('input[placeholder="用户名"]', 'admin');
    await page.fill('input[placeholder="密码"]', 'admin123');
    await page.click('button:has-text("登 录")');
    await page.waitForURL('**/#/home', { timeout: 10000 });

    const menuResponse = await menuResponsePromise;
    const menuBody = await menuResponse.json();
    expect(menuBody.data).toHaveLength(1);
    expect(menuBody.data[0]).toMatchObject({
      menuName: '系统管理',
      path: '/system',
    });
    expect(menuBody.data[0].children.map((item: { menuName: string }) => item.menuName)).toEqual([
      '账号权限',
      '组织人事',
      '平台运营',
      '基础数据',
      '审计日志',
    ]);
    for (const menu of collectVisibleMenus(menuBody.data)) {
      expect(menu.icon, `${menu.menuName} 必须配置菜单图标`).toBeTruthy();
    }

    await expect(page.getByText('系统管理').first()).toBeVisible();
    await page.getByText('系统管理').first().click();
    await expectMenuIcon(page, '账号权限');
    await expectMenuIcon(page, '组织人事');
    await expectMenuIcon(page, '平台运营');
    await expectMenuIcon(page, '基础数据');
    await expectMenuIcon(page, '审计日志');

    await expandMenuGroup(page, '账号权限');
    await expectMenuIcon(page, '角色管理');

    await expandMenuGroup(page, '平台运营');
    await expectMenuIcon(page, '菜单管理');

    await expandMenuGroup(page, '基础数据');
    await expectMenuIcon(page, '字典管理');
    await expectMenuIcon(page, '行政区划');
  });

  test('A 公司登录后只渲染机构授权范围内的系统管理导航', async ({ page }) => {
    await page.goto('/#/login');

    const menuResponsePromise = page.waitForResponse((response) => {
      const url = response.url();
      return response.status() === 200
        && url.includes('/api/authorization/menus/user')
        && url.includes('fmt=tree');
    });

    await page.locator('.tenant-select').click();
    await page.getByRole('option', { name: /A公司/ }).click();
    await page.fill('input[placeholder="用户名"]', 'admin');
    await page.fill('input[placeholder="密码"]', 'admin123');
    await page.click('button:has-text("登 录")');
    await page.waitForURL('**/#/home', { timeout: 10000 });

    const menuResponse = await menuResponsePromise;
    const menuBody = await menuResponse.json();
    expect(menuBody.data).toHaveLength(1);
    expect(menuBody.data[0]).toMatchObject({
      menuName: '系统管理',
      path: '/system',
    });
    expect(menuBody.data[0].children.map((item: { menuName: string }) => item.menuName)).toEqual([
      '账号权限',
      '组织人事',
      '审计日志',
    ]);
    for (const menu of collectVisibleMenus(menuBody.data)) {
      expect(menu.icon, `${menu.menuName} 必须配置菜单图标`).toBeTruthy();
    }

    await expectMenuIcon(page, '账号权限');
    await expectMenuIcon(page, '组织人事');
    await expectMenuIcon(page, '审计日志');
    await expect(page.getByText('平台运营')).toHaveCount(0);
    await expect(page.getByText('基础数据')).toHaveCount(0);
  });
});
