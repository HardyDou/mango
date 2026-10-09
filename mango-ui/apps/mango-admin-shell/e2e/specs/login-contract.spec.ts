import { expect, test, type Page } from '@playwright/test';
import { login } from '../support/login';

// These browser contract cases test the login helper and the real LoginView.
// Only institution discovery is controlled in the marked cases; they are not
// evidence of multi-institution authorization or backend error handling.
function countLoginSubmissions(page: Page) {
  const requests: string[] = [];
  page.on('request', (request) => {
    if (request.method() === 'POST' && new URL(request.url()).pathname === '/api/auth/login') {
      requests.push(request.url());
    }
  });
  return requests;
}

test('@p1 @auth-contract 单机构真实登录自动选中并建立目标机构会话', async ({ page }, testInfo) => {
  const submissions = countLoginSubmissions(page);
  const discovery = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' && new URL(response.url()).pathname === '/api/auth/login-institutions',
  );
  const institution = await login(page);
  expect((await (await discovery).json()).data).toHaveLength(1);
  expect(institution).toMatchObject({ tenantCode: 'default', tenantId: '1' });
  expect(submissions).toHaveLength(1);
  await page.screenshot({ path: testInfo.outputPath('single-institution.png') });
});

test('@p1 @auth-contract 多机构受控选项按机构编码选择，不依赖列表顺序', async ({ page }, testInfo) => {
  await page.route('**/api/auth/login-institutions', async (route) => {
    const response = await route.fetch();
    const body = await response.json();
    expect(body.code).toBe(200);
    expect(body.success).toBe(true);
    expect(body.data).toHaveLength(1);
    await route.fulfill({
      response,
      json: {
        ...body,
        data: [
          { tenantId: '9007199254740993', tenantCode: 'contract-option', tenantName: '合同测试机构选项' },
          ...body.data,
        ],
      },
    });
  });
  const institution = await login(page);
  expect(institution.tenantCode).toBe('default');
  await page.screenshot({ path: testInfo.outputPath('multiple-options-login.png') });
});

for (const scenario of [
  { name: '空机构列表', status: 200, body: { code: 200, success: true, data: [] }, error: /账号没有可登录机构/ },
  {
    name: 'HTTP 200 业务失败',
    status: 200,
    body: { code: 1400, success: false, data: [] },
    error: /查询账号可登录机构未成功/,
  },
  { name: 'HTTP 错误', status: 503, body: { code: 503, success: false }, error: /查询账号可登录机构 HTTP 状态/ },
]) {
  test(`@p1 @auth-contract ${scenario.name}受控响应应明确失败且不提交登录`, async ({ page }) => {
    const submissions = countLoginSubmissions(page);
    await page.route('**/api/auth/login-institutions', (route) =>
      route.fulfill({ status: scenario.status, json: scenario.body }),
    );
    await expect(login(page)).rejects.toThrow(scenario.error);
    expect(submissions).toHaveLength(0);
    await expect(page).toHaveURL(/#\/login$/);
  });
}

for (const scenario of [
  { name: '登录 HTTP 错误', status: 401, body: { code: 401, success: false }, error: /登录 HTTP 状态/ },
  { name: '登录业务失败', status: 200, body: { code: 1400, success: false }, error: /登录未成功/ },
]) {
  test(`@p1 @auth-contract ${scenario.name}受控响应不能冒充登录成功`, async ({ page }) => {
    await page.route('**/api/auth/login', (route) => route.fulfill({ status: scenario.status, json: scenario.body }));
    await expect(login(page)).rejects.toThrow(scenario.error);
    await expect(page).toHaveURL(/#\/login$/);
    expect(await page.evaluate(() => sessionStorage.getItem('MANGO_TOKEN'))).toBeNull();
  });
}
