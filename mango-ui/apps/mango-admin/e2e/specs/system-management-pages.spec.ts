import { expect, test } from '@playwright/test';

async function login(page: import('@playwright/test').Page) {
  await page.goto('/#/login');
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  await page.click('button:has-text("登 录")');
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

const pages = [
  { path: '/system/user', title: '成员管理', api: '/api/identity/users/page', labels: ['新增成员', '用户名'] },
  { path: '/system/role', title: '角色管理', api: '/api/authorization/roles', labels: ['系统角色', '启用'] },
  { path: '/system/org', title: '组织架构', api: '/api/org/tree', labels: ['组织详情', '芒果集团'] },
  { path: '/system/post', title: '岗位管理', api: '/api/post/page', labels: ['新增岗位', '岗位名称'] },
  { path: '/system/tenant', title: '机构管理', api: '/api/system/tenant/list', labels: ['芒果集团', '启用'] },
  { path: '/system/app', title: '应用管理', api: '/api/authorization/apps', labels: ['内部管理后台', 'internal-admin', '启用'] },
  { path: '/system/menu', title: '菜单管理', api: '/api/authorization/menus', labels: ['目录', '菜单'] },
  { path: '/system/menu-package', title: '套餐管理', api: '/api/authorization/menu-packages', labels: ['套餐名称', '授权菜单数'] },
  { path: '/system/dict', title: '字典类型', api: '/api/system/dict/type/list', labels: ['用户性别', '启用'] },
  { path: '/system/config', title: '系统配置', api: '/api/system/config/list', labels: ['系统参数'] },
  { path: '/system/area', title: '行政区划', api: '/api/system/area/children', labels: ['当前层级', '北京市'] },
  { path: '/system/route', title: '路由管理', api: '/api/system/route/list', labels: ['新增路由', '路由路径'] },
  { path: '/system/login-log', title: '登录日志', api: '/api/system/log/login/list', labels: [] },
  { path: '/system/operation-log', title: '操作日志', api: '/api/system/log/operation/list', labels: [] },
];

test.describe('系统管理首批页面联调', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  for (const item of pages) {
    test(`${item.title} 可访问并使用真实接口`, async ({ page }) => {
      const apiResponsePromise = page.waitForResponse((response) =>
        response.url().includes(item.api) && response.status() === 200
      );

      await page.goto(`/#${item.path}`);
      const apiResponse = await apiResponsePromise;
      const body = await apiResponse.json();
      expect(body.success || body.code === 200).toBeTruthy();

      await expect(page.getByText(item.title).first()).toBeVisible({ timeout: 10000 });
      await expect(page.locator('.el-message--error')).toHaveCount(0);
      await expect(page.locator('text=/401|403|未授权|拒绝访问|路由加载失败|加载失败/')).toHaveCount(0);

      for (const label of item.labels) {
        await expect(page.getByText(label).first()).toBeVisible({ timeout: 10000 });
      }
    });
  }

  test('系统配置 Tab 加载配置数据并显示字典标签', async ({ page }) => {
    await page.goto('/#/system/config');
    await expect(page.getByText('系统配置').first()).toBeVisible({ timeout: 10000 });

    const configResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/system/config/list') && response.status() === 200
    );

    await page.getByRole('tab', { name: '系统配置' }).click();
    const configResponse = await configResponsePromise;
    const body = await configResponse.json();
    expect(body.success || body.code === 200).toBeTruthy();

    const configRow = page
      .locator('.el-table__body-wrapper:visible')
      .getByRole('row', { name: /sys\.index\.skinName.*系统.*启用/ });
    await expect(configRow).toBeVisible({ timeout: 10000 });
  });
});
