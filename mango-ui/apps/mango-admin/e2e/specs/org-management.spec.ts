import { expect, test, type APIRequestContext, type Page } from '@playwright/test';
import { api as e2eApi } from '../support/api';

async function loginTokenAsCompanyA(request: APIRequestContext) {
  const response = await request.post(e2eApi('/auth/login'), {
    data: {
      username: 'admin',
      password: 'admin123',
      tenantId: '2',
      tenantCode: 'company_a',
      realm: 'INTERNAL',
      actorType: 'INTERNAL_USER',
      partyType: 'INTERNAL_ORG',
      appCode: 'internal-admin',
    },
  });
  expect(response.ok()).toBeTruthy();
  const body = await response.json();
  expect(body.success || body.code === 200).toBeTruthy();
  return body.data.accessToken as string;
}

async function loginAsCompanyA(page: Page) {
  await page.goto('/#/login');
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: /A公司/ }).click();
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  await page.click('button:has-text("登 录")');
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

async function expectNoAuthError(page: Page) {
  await expect(page.locator('.el-message--error')).toHaveCount(0);
  await expect(page.locator('text=/401|403|未授权|没有权限|拒绝访问|加载失败|登录已过期|请重新登录/')).toHaveCount(0);
}

async function rootOrg(request: APIRequestContext, token: string) {
  const response = await request.get(e2eApi('/org/tree?parentId=0&includeDisabled=true'), {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.success || body.code === 200).toBeTruthy();
  expect(body.data?.[0]?.orgName).toBe('A公司');
  return body.data[0];
}

async function cleanupOrg(request: APIRequestContext, token: string, orgCode: string) {
  const root = await rootOrg(request, token);
  const treeResponse = await request.get(e2eApi(`/org/tree?parentId=${root.id}&includeDisabled=true`), {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!treeResponse.ok()) return;

  const body = await treeResponse.json();
  const stack = [...(body.data || [])];
  while (stack.length > 0) {
    const item = stack.pop();
    stack.push(...(item.children || []));
    if (item.orgCode === orgCode) {
      await request.delete(e2eApi(`/org?id=${item.id}`), {
        headers: { Authorization: `Bearer ${token}` },
      });
    }
  }
}

test.describe('T15 组织架构管理闭环', () => {
  test('A 公司可新增、编辑、删除下级组织，根组织受保护', async ({ page, request }) => {
    const unique = Date.now();
    const orgName = `E2E组织${unique}`;
    const editedOrgName = `${orgName}-编辑`;
    const orgCode = `E2E_ORG_${unique}`;
    const token = await loginTokenAsCompanyA(request);

    await cleanupOrg(request, token, orgCode);

    const root = await rootOrg(request, token);
    const illegalRootResponse = await request.post(e2eApi('/org'), {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        pid: 0,
        orgName: `非法根组织${unique}`,
        orgCode: `BAD_ROOT_${unique}`,
        orgType: 2,
        orgSort: 0,
        orgStatus: '1',
      },
    });
    expect(illegalRootResponse.status()).toBe(200);
    const illegalRootBody = await illegalRootResponse.json();
    expect(illegalRootBody.success).toBeFalsy();
    expect(illegalRootBody.code).toBe(400);
    expect(illegalRootBody.msg).toContain('根组织由机构初始化创建');

    await loginAsCompanyA(page);

    const treeResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/org/tree') && response.status() === 200
    );
    await page.goto('/#/system/org');
    await treeResponsePromise;
    await expect(page.getByText('组织架构').first()).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('A公司').first()).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('button', { name: '新增根组织' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: '删除' }).first()).toBeDisabled();
    await expectNoAuthError(page);

    await page.getByRole('button', { name: '新增下级' }).first().click();
    const createDialog = page.getByRole('dialog', { name: '新增组织' });
    await expect(createDialog).toBeVisible();
    await createDialog.getByLabel('组织名称').fill(orgName);
    await createDialog.getByLabel('组织编码').fill(orgCode);
    await expect(createDialog.getByText('A公司')).toBeVisible();

    const createResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/org') &&
      response.request().method() === 'POST' &&
      response.status() === 200
    );
    await createDialog.getByRole('button', { name: '确定' }).click();
    await createResponsePromise;
    await expect(page.getByText('新增成功')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText(orgName).first()).toBeVisible({ timeout: 10000 });

    const childRow = page.locator('.el-table__row', { hasText: orgCode }).first();
    await expect(childRow).toBeVisible({ timeout: 10000 });
    await childRow.getByRole('button', { name: '编辑' }).click();
    const editDialog = page.getByRole('dialog', { name: '编辑组织' });
    await expect(editDialog).toBeVisible();
    await editDialog.getByLabel('组织名称').fill(editedOrgName);

    const updateResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/org') &&
      response.request().method() === 'PUT' &&
      response.status() === 200
    );
    await editDialog.getByRole('button', { name: '确定' }).click();
    await updateResponsePromise;
    await expect(page.getByText('修改成功')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText(editedOrgName).first()).toBeVisible({ timeout: 10000 });

    const editedRow = page.locator('.el-table__row', { hasText: orgCode }).first();
    await editedRow.getByRole('button', { name: '删除' }).click();
    await expect(page.getByText(`确认删除组织「${editedOrgName}」?`)).toBeVisible();

    const deleteResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/org') &&
      response.request().method() === 'DELETE' &&
      response.status() === 200
    );
    await page.getByRole('button', { name: '确定' }).click();
    await deleteResponsePromise;
    await expect(page.getByText('删除成功')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('.el-table__row', { hasText: orgCode })).toHaveCount(0);
    await expectNoAuthError(page);

    await cleanupOrg(request, token, orgCode);
  });
});
