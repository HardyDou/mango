import { expect, test, type APIRequestContext, type Page } from '@playwright/test';
import { api as e2eApi } from '../support/api';

type LoginData = {
  accessToken: string;
  refreshToken?: string;
  expiresIn?: number | string;
  tenantId?: string | number;
  [key: string]: unknown;
};

type ApiResponse<T> = {
  code?: number;
  success?: boolean;
  data?: T;
};

type PageResult<T> = {
  list?: T[];
  records?: T[];
  rows?: T[];
  data?: T[];
};

type RegistryRecord = {
  resourceType?: string;
};

type SyncRecord = {
  result?: string;
};

type RuntimeErrors = {
  badResponses: string[];
  consoleErrors: string[];
  failedRequests: string[];
  pageErrors: string[];
  vueWarnings: string[];
};

async function login(request: APIRequestContext): Promise<LoginData> {
  const response = await request.post(e2eApi('/auth/login'), {
    data: {
      username: 'admin',
      password: 'admin123',
      tenantId: 1,
      tenantCode: 'default',
      realm: 'INTERNAL',
      actorType: 'INTERNAL_USER',
      partyType: 'INTERNAL_ORG',
      appCode: 'internal-admin',
    },
  });
  expect(response.status()).toBe(200);
  const body = await response.json() as ApiResponse<LoginData>;
  expect(body.success || body.code === 200).toBeTruthy();
  expect(body.data?.accessToken).toBeTruthy();
  return body.data as LoginData;
}

async function installSession(page: Page, loginData: LoginData) {
  await page.goto('/#/login');
  await page.evaluate((data) => {
    sessionStorage.setItem('MANGO_TOKEN', data.accessToken);
    sessionStorage.setItem('MANGO_REFRESH_TOKEN', data.refreshToken || '');
    sessionStorage.setItem(
      'MANGO_TOKEN_EXPIRES_AT',
      String(Date.now() + Number(data.expiresIn || 7200) * 1000),
    );
    sessionStorage.setItem('tenantId', String(data.tenantId || 1));
    sessionStorage.setItem('userInfo', JSON.stringify(data));
    document.cookie = `MANGO_TOKEN=${encodeURIComponent(data.accessToken)}; path=/; SameSite=Lax`;
  }, loginData);
}

function recordsOf<T>(body: ApiResponse<PageResult<T>>): T[] {
  return body.data?.records ?? body.data?.list ?? body.data?.rows ?? body.data?.data ?? [];
}

function captureRuntimeErrors(page: Page): RuntimeErrors {
  const errors: RuntimeErrors = {
    badResponses: [],
    consoleErrors: [],
    failedRequests: [],
    pageErrors: [],
    vueWarnings: [],
  };
  page.on('console', (message) => {
    if (message.type() === 'error') {
      errors.consoleErrors.push(message.text());
    }
    if (message.type() === 'warning' && message.text().includes('[Vue warn]')) {
      errors.vueWarnings.push(message.text());
    }
  });
  page.on('pageerror', error => errors.pageErrors.push(error.message));
  page.on('response', (response) => {
    if (response.status() >= 400) {
      errors.badResponses.push(
        `${response.status()} ${response.request().method()} ${response.url()}`,
      );
    }
  });
  page.on('requestfailed', request => errors.failedRequests.push(
    `${request.method()} ${request.url()} ${request.failure()?.errorText || ''}`,
  ));
  return errors;
}

test('@p0 @infra-kv JDBC KV 支撑资源注册同步，并通过真实管理接口和页面验收', async ({ page, request }, testInfo) => {
  const runtimeErrors = captureRuntimeErrors(page);
  const loginData = await login(request);
  const headers = { Authorization: `Bearer ${loginData.accessToken}` };
  await installSession(page, loginData);

  const menuResponsePromise = page.waitForResponse(response =>
    response.url().includes('/api/authorization/menus')
      && response.url().includes('fmt=list')
      && response.request().method() === 'GET',
  );
  await page.goto('/#/system/menu');
  const menuResponse = await menuResponsePromise;
  expect(menuResponse.status()).toBe(200);
  await expect(page.getByText('菜单管理').first()).toBeVisible({ timeout: 10_000 });

  const registryResponse = await request.get(
    e2eApi('/resource/registries/page?pageNum=1&pageSize=20&resourceType=API_RESOURCE'),
    { headers },
  );
  const registryBody = await registryResponse.json() as ApiResponse<PageResult<RegistryRecord>>;
  expect(registryResponse.status()).toBe(200);
  expect(registryBody.success || registryBody.code === 200).toBeTruthy();
  const registryRecords = recordsOf(registryBody);
  expect(registryRecords.length).toBeGreaterThan(0);
  expect(registryRecords.every(record => record.resourceType === 'API_RESOURCE')).toBeTruthy();

  const syncResponse = await request.get(
    e2eApi('/resource/sync-logs/page?pageNum=1&pageSize=20'),
    { headers },
  );
  const syncBody = await syncResponse.json() as ApiResponse<PageResult<SyncRecord>>;
  expect(syncResponse.status()).toBe(200);
  expect(syncBody.success || syncBody.code === 200).toBeTruthy();
  expect(recordsOf(syncBody).some(record => record.result === 'SUCCESS')).toBeTruthy();

  const handlerResponse = await request.get(e2eApi('/resource/handler-specs'), { headers });
  const handlerBody = await handlerResponse.json() as ApiResponse<unknown[]>;
  expect(handlerResponse.status()).toBe(200);
  expect(handlerBody.success || handlerBody.code === 200).toBeTruthy();
  expect(handlerBody.data?.length).toBeGreaterThan(0);

  await testInfo.attach('resource-menu', {
    body: await page.screenshot({ fullPage: true }),
    contentType: 'image/png',
  });
  expect(runtimeErrors.consoleErrors).toEqual([]);
  expect(runtimeErrors.vueWarnings).toEqual([]);
  expect(runtimeErrors.pageErrors).toEqual([]);
  expect(runtimeErrors.badResponses).toEqual([]);
  expect(runtimeErrors.failedRequests).toEqual([]);
});
