import { expect, test } from '@playwright/test';

test.describe('用户菜单导航 E2E', () => {
  test('登录后使用后端用户菜单树渲染侧边导航', async ({ page }) => {
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
      '用户管理',
      '角色管理',
      '菜单管理',
    ]);

    await expect(page.getByText('系统管理').first()).toBeVisible();
    await page.getByText('系统管理').first().click();
    await expect(page.getByText('用户管理').first()).toBeVisible();
    await expect(page.getByText('角色管理').first()).toBeVisible();
    await expect(page.getByText('菜单管理').first()).toBeVisible();
  });
});
