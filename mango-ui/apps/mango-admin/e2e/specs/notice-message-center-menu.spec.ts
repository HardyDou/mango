import { expect, test, type Page, type Route } from '@playwright/test';

function ok(data: unknown) {
  return JSON.stringify({ code: 200, success: true, data });
}

async function fulfillJson(route: Route, data: unknown) {
  await route.fulfill({ status: 200, contentType: 'application/json', body: ok(data) });
}

async function setupRoutes(page: Page) {
  await page.route('**/api/**', (route) => fulfillJson(route, []));
  await page.route('**/api/system/tenant/login-options**', (route) =>
    fulfillJson(route, [{ tenantId: '1', tenantCode: 'mango', tenantName: '芒果集团' }]),
  );
  await page.route('**/api/auth/login-institutions**', (route) =>
    fulfillJson(route, [{ tenantId: '1', tenantCode: 'mango', tenantName: '芒果集团' }]),
  );
  await page.route('**/api/auth/login', (route) =>
    fulfillJson(route, {
      token: 'notice-message-center-menu-token',
      userInfo: {
        userId: '1',
        username: 'admin',
        nickname: 'Admin',
        tenantId: '1',
        tenantCode: 'mango',
        tenantName: '芒果集团',
        appCode: 'internal-admin',
        roles: ['admin'],
        permissions: ['notice:business:view', 'notice:receive-setting:view', 'notice:receive-setting:edit'],
      },
    }),
  );
  await page.route('**/api/authorization/menus/user**', (route) =>
    fulfillJson(route, [
      {
        menuId: '2920',
        parentId: '0',
        menuType: 1,
        menuName: '消息中心',
        menuCode: 'message-center',
        path: '/message-center',
        icon: 'Message',
        redirect: '/message-center/site-message',
        moduleCode: 'mango-notice',
        pageType: 'LOCAL_ROUTE',
        visible: 1,
        status: 1,
        children: [
          {
            menuId: '2921',
            parentId: '2920',
            menuType: 2,
            menuName: '我的消息',
            menuCode: 'notice:site-message',
            path: '/message-center/site-message',
            icon: 'Message',
            component: 'notice/site-message/index',
            moduleCode: 'mango-notice',
            pageType: 'LOCAL_ROUTE',
            visible: 1,
            status: 1,
            children: [],
          },
          {
            menuId: '2922',
            parentId: '2920',
            menuType: 2,
            menuName: '系统公告',
            menuCode: 'notice:announcement-user',
            path: '/message-center/announcement',
            icon: 'Reading',
            component: 'notice/announcement-user/index',
            moduleCode: 'mango-notice',
            pageType: 'LOCAL_ROUTE',
            visible: 1,
            status: 1,
            children: [],
          },
          {
            menuId: '2923',
            parentId: '2920',
            menuType: 2,
            menuName: '接收配置',
            menuCode: 'notice:receive-setting',
            path: '/message-center/receive-setting',
            icon: 'Switch',
            component: 'notice/receive-setting/index',
            moduleCode: 'mango-notice',
            pageType: 'LOCAL_ROUTE',
            visible: 1,
            status: 1,
            children: [],
          },
        ],
      },
    ]),
  );
  await page.route('**/api/notice/business-types**', (route) =>
    fulfillJson(route, { list: [], total: 0, pageNum: 1, pageSize: 200 }),
  );
  await page.route('**/api/notice/recipient-accounts**', (route) => fulfillJson(route, []));
  await page.route('**/api/notice/receive-preferences**', (route) => fulfillJson(route, []));
  await page.route('**/api/domain/domains/enabled-tree**', (route) => fulfillJson(route, []));
  await page.route('**/api/system/personal-configs/value**', (route) => fulfillJson(route, null));
}

async function login(page: Page) {
  await page.goto('/#/login');
  await page.getByPlaceholder('用户名').fill('admin');
  await page.getByPlaceholder('密码').fill('admin123');
  await page.getByPlaceholder('密码').blur();
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: /芒果集团/ }).click();
  await page.getByRole('button', { name: '登 录' }).click();
  await page.waitForURL('**/#/home');
}

test('@p1 @notice 消息中心展示新菜单名称并兼容接收配置旧路径', async ({ page }, testInfo) => {
  const consoleErrors: string[] = [];
  const failedRequests: string[] = [];
  page.on('console', (message) => {
    if (message.type() === 'error') consoleErrors.push(message.text());
  });
  page.on('requestfailed', (request) => failedRequests.push(request.url()));
  page.on('response', (response) => {
    if (response.status() >= 400) failedRequests.push(`${response.status()} ${response.url()}`);
  });

  await setupRoutes(page);
  await login(page);

  await page.getByRole('button', { name: '消息中心' }).click();
  await expect(page.getByRole('menuitem', { name: '我的消息' })).toBeVisible();
  await expect(page.getByRole('menuitem', { name: '系统公告' })).toBeVisible();
  await expect(page.getByRole('menuitem', { name: '接收配置' })).toBeVisible();

  await page.getByRole('menuitem', { name: '接收配置' }).click();
  await expect(page).toHaveURL(/#\/message-center\/receive-setting$/);
  await expect(page.locator('[data-page="notice.receive-setting"]')).toBeVisible();
  await expect(page.getByRole('tab', { name: '提醒设置' })).toBeVisible();
  await page.screenshot({ path: testInfo.outputPath('message-center-receive-setting.png'), fullPage: true });

  await page.goto('/#/notice/receive-setting');
  await expect(page).toHaveURL(/#\/notice\/receive-setting$/);
  await expect(page.locator('[data-page="notice.receive-setting"]')).toBeVisible();

  await page.getByRole('button', { name: '消息中心' }).click();
  await page.getByRole('menuitem', { name: '系统公告' }).click();
  await expect(page).toHaveURL(/#\/message-center\/announcement$/);
  await expect(page.getByRole('heading', { name: '公告' })).toBeVisible();
  expect(consoleErrors).toEqual([]);
  expect(failedRequests).toEqual([]);
});
