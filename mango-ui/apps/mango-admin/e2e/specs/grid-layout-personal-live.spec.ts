import { expect, test, type APIRequestContext, type Page } from '@playwright/test';
import { resolve } from 'node:path';
import { api as e2eApi } from '../support/api';

type ApiResult<T> = {
  code: number | string;
  data: T;
  msg?: string;
  success?: boolean;
};

type LoginTenant = {
  tenantCode: string;
  tenantId: string;
};

type PersonalLayout = {
  id: string;
  layoutJson: string;
  pageCode: string;
  schemaVersion: number;
  tenantId: string;
  userId: string;
};

type HomePage = {
  id: string;
  name: string;
};

const platformTenant: LoginTenant = { tenantId: '1', tenantCode: 'default' };
const companyATenant: LoginTenant = { tenantId: '2', tenantCode: 'company_a' };
const gridUiEvidence = resolve(
  __dirname,
  '../../../../../mango-docs/evidence/baselines/grid-layout/latest/grid-layout-ui-success.png',
);

async function loginToken(request: APIRequestContext, tenant: LoginTenant): Promise<string> {
  const response = await request.post(e2eApi('/auth/login'), {
    data: {
      username: 'admin',
      password: 'admin123',
      tenantId: tenant.tenantId,
      tenantCode: tenant.tenantCode,
      realm: 'INTERNAL',
      actorType: 'INTERNAL_USER',
      partyType: 'INTERNAL_ORG',
      appCode: 'internal-admin',
    },
  });
  expect(response.status()).toBe(200);
  const body = await response.json() as ApiResult<{ accessToken: string }>;
  expect(body.code).toBe(200);
  expect(body.data.accessToken).toBeTruthy();
  return body.data.accessToken;
}

function authHeaders(token: string) {
  return { Authorization: `Bearer ${token}` };
}

async function readPersonal(request: APIRequestContext, token: string, pageCode: string) {
  const response = await request.get(e2eApi('/grid-layout/personal'), {
    headers: authHeaders(token),
    params: { pageCode },
  });
  expect(response.status()).toBe(200);
  return await response.json() as ApiResult<PersonalLayout | null>;
}

async function deletePersonal(request: APIRequestContext, token: string, pageCode: string) {
  const response = await request.delete(e2eApi('/grid-layout/personal'), {
    headers: authHeaders(token),
    params: { pageCode },
  });
  expect(response.status()).toBe(200);
  return await response.json() as ApiResult<boolean>;
}

async function installBrowserSession(page: Page) {
  await page.goto('/#/login');
  await page.evaluate(async () => {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: 'admin',
        password: 'admin123',
        tenantId: '1',
        tenantCode: 'default',
        realm: 'INTERNAL',
        actorType: 'INTERNAL_USER',
        partyType: 'INTERNAL_ORG',
        appCode: 'internal-admin',
      }),
    });
    const body = await response.json();
    if (!response.ok || !(body.success || body.code === 200) || !body.data?.accessToken) {
      throw new Error(`浏览器登录失败：${response.status} ${JSON.stringify(body)}`);
    }
    const userInfo = body.data;
    sessionStorage.setItem('MANGO_TOKEN', userInfo.accessToken);
    sessionStorage.setItem('MANGO_REFRESH_TOKEN', userInfo.refreshToken || '');
    sessionStorage.setItem(
      'MANGO_TOKEN_EXPIRES_AT',
      String(Date.now() + Number(userInfo.expiresIn || 7200) * 1000),
    );
    sessionStorage.setItem('userInfo', JSON.stringify(userInfo));
    sessionStorage.setItem('tenantId', String(userInfo.tenantId || '1'));
    document.cookie = `MANGO_TOKEN=${encodeURIComponent(userInfo.accessToken)}; path=/; SameSite=Lax`;
  });
}

test.describe('个人栅格布局真实服务入口 @p0 @grid-layout @api', () => {
  test('CRUD、稳定主键、租户隔离及 API 继承校验均正确', async ({ request }) => {
    const platformToken = await loginToken(request, platformTenant);
    const companyToken = await loginToken(request, companyATenant);
    const pageCode = `grid-layout-api-${Date.now()}-${test.info().workerIndex}`;
    const layoutOne = JSON.stringify({
      schemaVersion: 1,
      pageCode,
      items: [{ id: 'welcome', widgetType: 'system.welcome', layout: { x: 0, y: 0, w: 6, h: 2 } }],
    });
    const layoutTwo = JSON.stringify({
      schemaVersion: 1,
      pageCode,
      items: [{
        id: 'welcome',
        widgetType: 'system.welcome',
        layout: { x: 1, y: 0, w: 5, h: 2 },
        props: { title: 'updated' },
      }],
    });

    await deletePersonal(request, platformToken, pageCode);
    try {
      expect((await readPersonal(request, platformToken, pageCode)).data).toBeNull();

      const savedResponse = await request.put(e2eApi('/grid-layout/personal'), {
        headers: authHeaders(platformToken),
        data: { pageCode, layoutJson: layoutOne },
      });
      expect(savedResponse.status()).toBe(200);
      const saved = await savedResponse.json() as ApiResult<PersonalLayout>;
      expect(saved.data).toMatchObject({
        tenantId: '1',
        pageCode,
        schemaVersion: 1,
        layoutJson: layoutOne,
      });
      expect(saved.data.id).toBeTruthy();

      const updatedResponse = await request.put(e2eApi('/grid-layout/personal'), {
        headers: authHeaders(platformToken),
        data: { pageCode, layoutJson: layoutTwo },
      });
      expect(updatedResponse.status()).toBe(200);
      const updated = await updatedResponse.json() as ApiResult<PersonalLayout>;
      expect(updated.data.id).toBe(saved.data.id);
      expect(updated.data.layoutJson).toBe(layoutTwo);

      expect((await readPersonal(request, companyToken, pageCode)).data).toBeNull();

      const validationResponse = await request.get(e2eApi('/grid-layout/personal'), {
        headers: authHeaders(platformToken),
        params: { pageCode: '' },
      });
      expect(validationResponse.status()).toBe(400);
      const validationBody = await validationResponse.json() as ApiResult<null>;
      expect(validationBody.msg).toContain('pageCode不能为空');
      expect(JSON.stringify(validationBody)).not.toContain('ConstraintDeclarationException');
    } finally {
      const deleted = await deletePersonal(request, platformToken, pageCode);
      expect(deleted.code).toBe(200);
      expect((await readPersonal(request, platformToken, pageCode)).data).toBeNull();
    }
  });
});

test.describe('栅格设计器真实消费链 @p0 @grid-layout @ui', () => {
  test.setTimeout(60 * 1000);

  test('首页实际消费 Grid 组件时可恢复默认并保存，浏览器无异常', async ({ page, request }, testInfo) => {
    const platformToken = await loginToken(request, platformTenant);
    const unique = `${Date.now()}-${test.info().workerIndex}`;
    const homeName = `栅格布局E2E-${unique}`;
    const layoutJson = JSON.stringify({
      schemaVersion: 1,
      pageCode: 'admin-home-workbench',
      items: [{ id: 'welcome', widgetType: 'system.welcome', layout: { x: 0, y: 0, w: 6, h: 2 } }],
    });
    let homeId = '';

    const consoleErrors: string[] = [];
    const pageErrors: string[] = [];
    const requestFailures: string[] = [];
    const serverErrors: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') consoleErrors.push(message.text());
    });
    page.on('pageerror', error => pageErrors.push(error.message));
    page.on('requestfailed', requestFailure => requestFailures.push(
      `${requestFailure.method()} ${requestFailure.url()} ${requestFailure.failure()?.errorText || ''}`,
    ));
    page.on('response', (response) => {
      if (response.status() >= 500) serverErrors.push(`${response.status()} ${response.url()}`);
    });

    try {
      const createHomeResponse = await request.post(e2eApi('/home/pages'), {
        headers: authHeaders(platformToken),
        data: { name: homeName, layoutJson, setDefault: false },
      });
      expect(createHomeResponse.status()).toBe(200);
      const createdHome = await createHomeResponse.json() as ApiResult<HomePage>;
      homeId = String(createdHome.data.id);
      expect(homeId).toBeTruthy();

      await installBrowserSession(page);
      await page.goto(`/#/home/${homeId}`);
      await expect(page.locator('[data-page="home.workbench"]')).toHaveAttribute('data-state', 'ready', { timeout: 15_000 });
      await expect(page.getByRole('button', { name: '编辑布局' })).toBeEnabled();
      await page.getByRole('button', { name: '编辑布局' }).click();
      await expect(page.locator('[data-surface="grid-designer"]')).toBeVisible();
      await expect(page.locator('[data-surface="grid-designer.widget-library"]')).toBeVisible();

      await page.getByRole('button', { name: '恢复默认' }).click();
      const resetConfirmation = page.getByRole('tooltip').filter({
        hasText: '确认恢复默认布局？当前编辑内容会被默认布局替换。',
      });
      await expect(resetConfirmation).toBeVisible();
      await resetConfirmation.getByRole('button', { name: '恢复默认', exact: true }).click();

      const saveResponse = page.waitForResponse(response => (
        response.url().includes('/api/home/pages/layout')
          && response.request().method() === 'PUT'
      ));
      await page.getByRole('button', { name: '保存布局' }).click();
      expect((await saveResponse).status()).toBe(200);
      await expect(page.getByRole('button', { name: '编辑布局' })).toBeVisible();
      await page.screenshot({ path: gridUiEvidence, fullPage: true });
      await testInfo.attach('grid-layout-ui-success.png', {
        path: gridUiEvidence,
        contentType: 'image/png',
      });
    } finally {
      if (homeId) {
        const cleanupHome = await request.delete(e2eApi('/home/pages'), {
          headers: authHeaders(platformToken),
          data: { id: homeId },
        });
        expect(cleanupHome.status()).toBe(200);
      }
    }

    expect(consoleErrors, `浏览器控制台错误：\n${consoleErrors.join('\n')}`).toHaveLength(0);
    expect(pageErrors, `页面异常：\n${pageErrors.join('\n')}`).toHaveLength(0);
    expect(requestFailures, `请求失败：\n${requestFailures.join('\n')}`).toHaveLength(0);
    expect(serverErrors, `服务端 5xx：\n${serverErrors.join('\n')}`).toHaveLength(0);
  });
});
