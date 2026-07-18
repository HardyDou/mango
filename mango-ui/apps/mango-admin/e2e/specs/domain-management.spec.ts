import { expect, test, type APIRequestContext, type Page } from '@playwright/test';
import { api as e2eApi } from '../support/api';
import {
  confirmLatestMessageBox,
  domainRow,
  domainTable,
  expectLatestMessage,
  selectLoginTenant,
} from '../support/domain-management';

type LoginTenant = {
  tenantId: string;
  tenantCode: string;
  tenantName: string;
};

type ApiResponse<T> = {
  code?: number;
  success?: boolean;
  data: T;
};

type DomainItem = {
  id: string;
  domainCode: string;
  domainShortCode: string;
  domainName: string;
  status: number;
  children?: DomainItem[];
};

const platformTenant: LoginTenant = {
  tenantId: '1',
  tenantCode: 'default',
  tenantName: '芒果集团',
};

async function loginToken(request: APIRequestContext, tenant: LoginTenant) {
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
  expect(response.ok()).toBeTruthy();
  const body = await response.json() as ApiResponse<{ accessToken: string }>;
  expect(body.success || body.code === 200).toBeTruthy();
  return body.data.accessToken;
}

async function loginPage(page: Page, tenant: LoginTenant, diagnostics: string[]) {
  await page.goto('/#/login');
  await selectLoginTenant(page, new RegExp(tenant.tenantName));
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  const loginResponsePromise = page.waitForResponse((response) =>
    new URL(response.url()).pathname === '/api/auth/login' &&
    response.request().method() === 'POST'
  );
  await page.click('button:has-text("登 录")');
  const loginResponse = await loginResponsePromise;
  expect(loginResponse.status()).toBe(200);
  const loginBody = await loginResponse.json() as ApiResponse<{ accessToken?: string }>;
  expect(loginBody.success || loginBody.code === 200).toBeTruthy();
  expect(loginBody.data?.accessToken).toBeTruthy();
  try {
    await expect(page).toHaveURL(/\/#\/home$/, { timeout: 10000 });
  } catch (error) {
    const storageState = await page.evaluate(() => ({
      hasToken: Boolean(sessionStorage.getItem('MANGO_TOKEN')),
      hasUserInfo: Boolean(sessionStorage.getItem('userInfo')),
    }));
    throw new Error(`登录后的浏览器异常：\n${diagnostics.join('\n')}\n存储状态：${JSON.stringify(storageState)}\n${String(error)}`);
  }
}

async function authJson<T>(request: APIRequestContext, token: string, path: string) {
  const response = await request.get(e2eApi(path), {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(response.status()).toBe(200);
  const body = await response.json() as ApiResponse<T>;
  expect(body.success || body.code === 200).toBeTruthy();
  return body.data;
}

async function findDomain(request: APIRequestContext, token: string, domainCode: string) {
  const response = await request.get(e2eApi(`/domain/domains/code?domainCode=${domainCode}`), {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (response.status() !== 200) {
    return undefined;
  }
  const body = await response.json() as ApiResponse<DomainItem>;
  if (!(body.success || body.code === 200)) {
    return undefined;
  }
  return body.data;
}

async function deleteDomain(request: APIRequestContext, token: string, domain?: DomainItem) {
  if (!domain?.id) return;
  await request.delete(e2eApi(`/domain/domains?id=${domain.id}`), {
    headers: { Authorization: `Bearer ${token}` },
  });
}

async function cleanupDomain(request: APIRequestContext, token: string, parentCode: string, childCode: string) {
  await deleteDomain(request, token, await findDomain(request, token, childCode));
  await deleteDomain(request, token, await findDomain(request, token, parentCode));
}

test.describe('业务域管理页面真实接口闭环 @p0 @domain', () => {
  test('平台管理员可维护业务域并在下拉接口中只看到启用业务域', async ({ page, request }, testInfo) => {
    test.setTimeout(90_000);

    const consoleErrors: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        consoleErrors.push(message.text());
      }
    });
    page.on('pageerror', (error) => consoleErrors.push(`pageerror: ${error.message}`));
    page.on('requestfailed', (request) => {
      consoleErrors.push(`requestfailed: ${request.method()} ${request.url()} ${request.failure()?.errorText || ''}`);
    });
    page.on('response', (response) => {
      if (response.status() >= 400) {
        consoleErrors.push(`response: ${response.status()} ${response.request().method()} ${response.url()}`);
      }
    });

    const unique = Date.now();
    const parentCode = `E2E_DOMAIN_${unique}`;
    const childCode = `${parentCode}_PAY`;
    const parentShortCode = `E2EP${String(unique).slice(-6)}`;
    const childShortCode = `E2EC${String(unique).slice(-6)}`;
    const parentName = `E2E业务域${unique}`;
    const editedParentName = `${parentName}-编辑`;
    const childName = `E2E支付域${unique}`;
    const token = await loginToken(request, platformTenant);

    try {
      await cleanupDomain(request, token, parentCode, childCode);
      await loginPage(page, platformTenant, consoleErrors);

      const treeResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/domain/domains/tree') &&
        response.status() === 200
      );
      await page.goto('/#/system/domain');
      await treeResponsePromise;
      await expect(page.getByRole('columnheader', { name: '业务域' })).toBeVisible({ timeout: 10000 });
      await expect(page.getByRole('button', { name: '新增业务域' })).toBeVisible();
      await expect(domainTable(page).getByText('通用域')).toBeVisible({ timeout: 10000 });

      const enabledBefore = await authJson<DomainItem[]>(request, token, '/domain/domains/enabled-tree');
      expect(enabledBefore.some((item) => item.domainCode === 'COMMON')).toBeTruthy();

      await page.getByRole('button', { name: '新增业务域' }).click();
      const createDialog = page.getByRole('dialog', { name: '新增业务域' });
      await expect(createDialog).toBeVisible();
      await createDialog.getByLabel('本层编码').fill(parentCode);
      await createDialog.getByLabel('编码简写').fill(parentShortCode);
      await createDialog.getByLabel('业务域名称').fill(parentName);
      await createDialog.getByLabel('备注').fill('E2E创建顶级业务域');

      const createResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/domain/domains') &&
        response.request().method() === 'POST'
      );
      await createDialog.getByRole('button', { name: '保存' }).click();
      const createResponse = await createResponsePromise;
      expect(createResponse.status()).toBe(200);
      await expectLatestMessage(page, '保存成功');
      await expect(domainRow(page, parentCode)).toBeVisible({ timeout: 10000 });

      const parentRow = domainRow(page, parentCode);
      await parentRow.getByRole('button', { name: '新增下级' }).click();
      const createChildDialog = page.getByRole('dialog', { name: '新增业务域' });
      await expect(createChildDialog).toBeVisible();
      await createChildDialog.getByLabel('本层编码').fill('PAY');
      await expect(createChildDialog.getByLabel('最终编码')).toHaveValue(childCode);
      await createChildDialog.getByLabel('编码简写').fill(childShortCode);
      await createChildDialog.getByLabel('业务域名称').fill(childName);
      await createChildDialog.getByRole('button', { name: '保存' }).click();
      await expectLatestMessage(page, '保存成功');
      await expect(domainRow(page, childCode)).toBeVisible({ timeout: 10000 });

      await domainRow(page, parentCode).getByRole('button', { name: '编辑' }).click();
      const editDialog = page.getByRole('dialog', { name: '编辑业务域' });
      await expect(editDialog).toBeVisible();
      await expect(editDialog.getByLabel('本层编码')).toBeDisabled();
      await editDialog.getByLabel('业务域名称').fill(editedParentName);
      const updateResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/domain/domains') &&
        response.request().method() === 'PUT'
      );
      await editDialog.getByRole('button', { name: '保存' }).click();
      const updateResponse = await updateResponsePromise;
      expect(updateResponse.status()).toBe(200);
      await expectLatestMessage(page, '保存成功');
      await expect(domainRow(page, editedParentName)).toBeVisible({ timeout: 10000 });

      await domainRow(page, editedParentName).getByRole('button', { name: '停用' }).click();
      await expectLatestMessage(page, '已停用');
      const enabledAfterDisable = await authJson<DomainItem[]>(request, token, '/domain/domains/enabled-tree');
      expect(enabledAfterDisable.some((item) => item.domainCode === parentCode)).toBeFalsy();

      await domainRow(page, editedParentName).getByRole('button', { name: '启用' }).click();
      await expectLatestMessage(page, '已启用');
      const enabledAfterEnable = await authJson<DomainItem[]>(request, token, '/domain/domains/enabled-tree');
      expect(enabledAfterEnable.some((item) => item.domainCode === parentCode)).toBeTruthy();

      await domainRow(page, childCode).getByRole('button', { name: '删除' }).click();
      await expect(page.getByText(`确认删除业务域“${childName}”？`)).toBeVisible();
      await confirmLatestMessageBox(page);
      await expectLatestMessage(page, '删除成功');
      await expect(page.getByText(childCode)).toHaveCount(0);

      await domainRow(page, editedParentName).getByRole('button', { name: '删除' }).click();
      await expect(page.getByText(`确认删除业务域“${editedParentName}”？`)).toBeVisible();
      await confirmLatestMessageBox(page);
      await expectLatestMessage(page, '删除成功');
      await expect(page.getByText(parentCode)).toHaveCount(0);

      await expect(page.locator('text=/401|403|404|500|未授权|没有权限|拒绝访问|加载失败|登录已过期|请重新登录/')).toHaveCount(0);
      expect(consoleErrors).toEqual([]);
      await page.screenshot({
        path: testInfo.outputPath('domain-management-final.png'),
        fullPage: true,
      });
    } finally {
      await cleanupDomain(request, token, parentCode, childCode).catch(() => undefined);
    }
  });
});
