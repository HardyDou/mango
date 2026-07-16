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

async function loginPage(page: Page) {
  await page.goto('/#/login');
  await expect(page.locator('.tenant-select')).toContainText('芒果集团');
  await page.getByPlaceholder('用户名').fill('admin');
  await page.getByPlaceholder('密码').fill('admin123');
  await page.locator('.login-btn').click();
  await page.waitForURL('**/#/home', { timeout: 10_000 });
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
  test.setTimeout(90_000);
  const runtimeErrors = captureRuntimeErrors(page);
  const loginData = await login(request);
  const headers = { Authorization: `Bearer ${loginData.accessToken}` };
  await loginPage(page);

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

  const invalidDeleteResponse = await request.delete(
    e2eApi('/resource/registries?physical=false'),
    { headers },
  );
  const invalidDeleteBody = await invalidDeleteResponse.json() as ApiResponse<unknown>;
  expect(invalidDeleteResponse.status()).toBe(400);
  expect(invalidDeleteBody.success).toBeFalsy();
  expect(invalidDeleteBody.code).toBe(400);

  const forceSyncResponse = await request.post(e2eApi('/resource/sync/force'), { headers });
  const forceSyncBody = await forceSyncResponse.json() as ApiResponse<boolean>;
  expect(forceSyncResponse.status()).toBe(200);
  expect(forceSyncBody.success || forceSyncBody.code === 200).toBeTruthy();
  expect(forceSyncBody.data).toBe(true);

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
