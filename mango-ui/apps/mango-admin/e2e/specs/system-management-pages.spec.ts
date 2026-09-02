import { expect, test } from '@playwright/test';

async function login(page: import('@playwright/test').Page) {
  await page.goto('/#/login');
  const accountTenantsResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/api/auth/login-institutions') && response.status() === 200,
  );
  await page.fill('input[placeholder="请输入用户名"]', 'admin');
  await page.fill('input[placeholder="请输入密码"]', 'admin123');
  await page.locator('input[placeholder="请输入密码"]').blur();
  await accountTenantsResponsePromise;
  await expect(page.locator('.tenant-select')).toHaveCount(0);

  const menuResponsePromise = page.waitForResponse((response) => {
    const url = response.url();
    return response.status() === 200 && url.includes('/api/authorization/menus/user') && url.includes('fmt=tree');
  });
  await page.locator('.login-btn').click();
  await page.waitForURL('**/#/home', { timeout: 10000 });
  await menuResponsePromise;
  await expect(page.getByRole('button', { name: '系统管理' })).toBeVisible({ timeout: 10000 });
}

const pages = [
  { path: '/system/user', title: '成员管理', api: '/api/identity/users/page', labels: ['新增成员', '用户名'] },
  { path: '/system/role', title: '角色管理', api: '/api/authorization/roles', labels: ['系统角色', '启用'] },
  { path: '/system/org', title: '组织架构', api: '/api/org/tree', labels: ['组织详情', '芒果集团'] },
  { path: '/system/post', title: '岗位管理', api: '/api/post/page', labels: ['新增岗位', '岗位名称'] },
  {
    path: '/system/app',
    title: '应用管理',
    api: '/api/authorization/apps',
    labels: ['内部管理后台', 'internal-admin', '启用'],
  },
  { path: '/system/menu', title: '菜单管理', api: '/api/authorization/menus', labels: ['目录', '菜单'] },
  {
    path: '/system/menu-package',
    title: '套餐管理',
    api: '/api/authorization/menu-packages',
    labels: ['套餐名称', '授权菜单数'],
  },
  { path: '/system/dict', title: '新增类型', api: '/api/system/dict/type/list', labels: ['用户性别', '启用'] },
  {
    path: '/system/config',
    title: '参数配置',
    expectedText: '业务域',
    api: '/api/system/config/list',
    labels: ['系统参数'],
  },
  { path: '/system/area', title: '行政区划', api: '/api/system/area/children', labels: ['当前层级', '北京市'] },
  { path: '/system/login-log', title: '登录日志', api: '/api/system/log/login/list', labels: [] },
  { path: '/system/operation-log', title: '操作日志', api: '/api/system/log/operation/list', labels: [] },
];

test.describe('系统管理首批页面联调', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  for (const item of pages) {
    test(`${item.title} 可访问并使用真实接口`, async ({ page }) => {
      const apiResponsePromise = page.waitForResponse(
        (response) => response.url().includes(item.api) && response.status() === 200,
      );

      await page.goto(`/#${item.path}`);
      const apiResponse = await apiResponsePromise;
      const body = await apiResponse.json();
      expect(body.success || body.code === 200).toBeTruthy();

      await expect(page.getByText(item.expectedText || item.title).first()).toBeVisible({ timeout: 10000 });
      await expect(page.locator('.el-message--error')).toHaveCount(0);
      await expect(page.locator('text=/401|403|未授权|拒绝访问|路由加载失败|加载失败/')).toHaveCount(0);

      for (const label of item.labels) {
        await expect(page.getByText(label).first()).toBeVisible({ timeout: 10000 });
      }
    });
  }

  test('参数配置加载配置数据并显示业务域和展示类型', async ({ page }) => {
    const configResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/api/system/config/list') && response.status() === 200,
    );
    await page.goto('/#/system/config');
    const configResponse = await configResponsePromise;
    const body = await configResponse.json();
    expect(body.success || body.code === 200).toBeTruthy();
    await expect(page.getByText('业务域').first()).toBeVisible({ timeout: 10000 });

    const configRow = page
      .locator('.el-table__body-wrapper:visible')
      .getByRole('row')
      .filter({ hasText: 'sys.index.skinName' });
    await expect(configRow).toBeVisible({ timeout: 10000 });
    await expect(configRow).toContainText('皮肤名称');
    await expect(configRow).toContainText('通用域');
    await expect(configRow).toContainText('文本');
  });
});
