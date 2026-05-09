import { expect, test, type APIRequestContext, type Page } from '@playwright/test';

type LoginTenant = {
  tenantId: string;
  tenantCode: string;
  tenantName: string;
};

const platformTenant: LoginTenant = {
  tenantId: '1',
  tenantCode: 'default',
  tenantName: '芒果集团',
};

const companyATenant: LoginTenant = {
  tenantId: '2',
  tenantCode: 'company_a',
  tenantName: 'A公司',
};

async function loginToken(request: APIRequestContext, tenant: LoginTenant) {
  const response = await request.post('http://localhost:5555/auth/login', {
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
  const body = await response.json();
  expect(body.success || body.code === 200).toBeTruthy();
  return body.data.accessToken as string;
}

async function loginPage(page: Page, tenant: LoginTenant) {
  await page.goto('/#/login');
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: new RegExp(tenant.tenantName) }).click();
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  await page.click('button:has-text("登 录")');
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

async function findDictType(request: APIRequestContext, token: string, dictType: string) {
  const response = await request.get('http://localhost:5555/system/dict/type/list', {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  return (body.data || []).find((item: any) => item.dictType === dictType);
}

async function cleanupDict(request: APIRequestContext, token: string, dictType: string) {
  const type = await findDictType(request, token, dictType);
  if (!type) return;

  const dataResponse = await request.get(`http://localhost:5555/system/dict/data/list?typeId=${type.id}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (dataResponse.status() === 200) {
    const dataBody = await dataResponse.json();
    for (const item of dataBody.data || []) {
      await request.delete(`http://localhost:5555/system/dict/data?id=${item.id}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
    }
  }
  await request.delete(`http://localhost:5555/system/dict/type?id=${type.id}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
}

async function expectNoAuthError(page: Page) {
  await expect(page.locator('.el-message--error')).toHaveCount(0);
  await expect(page.locator('text=/401|403|未授权|没有权限|拒绝访问|加载失败|登录已过期|请重新登录/')).toHaveCount(0);
}

async function expectLatestMessage(page: Page, message: string) {
  await expect(page.locator('.el-message__content', { hasText: message }).last()).toBeVisible({ timeout: 10000 });
}

test.describe('T10 字典管理页面真实接口闭环', () => {
  test('平台管理员可新增、编辑、删除字典类型和字典数据', async ({ page, request }) => {
    const unique = Date.now();
    const dictCode = `e2e_dict_${unique}`;
    const dictName = `E2E字典${unique}`;
    const editedDictName = `${dictName}-编辑`;
    const optionLabel = `选项${unique}`;
    const editedOptionLabel = `${optionLabel}-编辑`;
    const optionValue = `value_${unique}`;
    const platformToken = await loginToken(request, platformTenant);

    try {
      await cleanupDict(request, platformToken, dictCode);
      await loginPage(page, platformTenant);

      const typeListResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/system/dict/type/list') &&
        response.status() === 200
      );
      await page.goto('/#/system/dict');
      await typeListResponsePromise;
      await expect(page.getByText('字典管理').first()).toBeVisible({ timeout: 10000 });

      await page.getByRole('button', { name: '新增类型' }).click();
      const createTypeDialog = page.getByRole('dialog', { name: '新增类型' });
      await expect(createTypeDialog).toBeVisible();
      await createTypeDialog.getByLabel('类型名称').fill(dictName);
      await createTypeDialog.getByLabel('类型编码').fill(dictCode);

      const createTypeResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/system/dict/type') &&
        response.request().method() === 'POST'
      );
      await createTypeDialog.getByRole('button', { name: '确定' }).click();
      const createTypeResponse = await createTypeResponsePromise;
      const createTypeBody = await createTypeResponse.json();
      expect(createTypeResponse.status()).toBe(200);
      expect(createTypeBody.success || createTypeBody.code === 200).toBeTruthy();
      await expectLatestMessage(page, '新增成功');
      await expect(page.getByText(dictName).first()).toBeVisible({ timeout: 10000 });

      await page.getByText(dictName).click();
      await page.getByRole('button', { name: '新增数据' }).click();
      const createDataDialog = page.getByRole('dialog', { name: '新增数据' });
      await expect(createDataDialog).toBeVisible();
      await createDataDialog.getByLabel('标签').fill(optionLabel);
      await createDataDialog.getByLabel('值').fill(optionValue);

      const createDataResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/system/dict/data') &&
        response.request().method() === 'POST'
      );
      await createDataDialog.getByRole('button', { name: '确定' }).click();
      const createDataResponse = await createDataResponsePromise;
      const createDataBody = await createDataResponse.json();
      expect(createDataResponse.status()).toBe(200);
      expect(createDataBody.success || createDataBody.code === 200).toBeTruthy();
      await expectLatestMessage(page, '新增成功');
      await expect(page.locator('.el-table__row', { hasText: optionLabel })).toBeVisible({ timeout: 10000 });

      const optionsResponse = await request.get(`http://localhost:5555/system/dict/data/options?typeCode=${dictCode}`, {
        headers: { Authorization: `Bearer ${platformToken}` },
      });
      expect(optionsResponse.status()).toBe(200);
      const optionsBody = await optionsResponse.json();
      expect(optionsBody.data).toEqual([{ label: optionLabel, value: optionValue }]);

      const typeItem = page.locator('.type-item', { hasText: dictCode }).first();
      await typeItem.getByRole('button', { name: '编辑' }).click();
      const editTypeDialog = page.getByRole('dialog', { name: '编辑类型' });
      await expect(editTypeDialog).toBeVisible();
      await editTypeDialog.getByLabel('类型名称').fill(editedDictName);
      const updateTypeResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/system/dict/type') &&
        response.request().method() === 'PUT'
      );
      await editTypeDialog.getByRole('button', { name: '确定' }).click();
      const updateTypeResponse = await updateTypeResponsePromise;
      const updateTypeBody = await updateTypeResponse.json();
      expect(updateTypeResponse.status()).toBe(200);
      expect(updateTypeBody.success || updateTypeBody.code === 200).toBeTruthy();
      await expectLatestMessage(page, '修改成功');
      await expect(page.getByText(editedDictName).first()).toBeVisible({ timeout: 10000 });

      const dataRow = page.locator('.el-table__row', { hasText: optionValue }).first();
      await dataRow.getByRole('button', { name: '编辑' }).click();
      const editDataDialog = page.getByRole('dialog', { name: '编辑数据' });
      await expect(editDataDialog).toBeVisible();
      await editDataDialog.getByLabel('标签').fill(editedOptionLabel);
      const updateDataResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/system/dict/data') &&
        response.request().method() === 'PUT'
      );
      await editDataDialog.getByRole('button', { name: '确定' }).click();
      const updateDataResponse = await updateDataResponsePromise;
      const updateDataBody = await updateDataResponse.json();
      expect(updateDataResponse.status()).toBe(200);
      expect(updateDataBody.success || updateDataBody.code === 200).toBeTruthy();
      await expectLatestMessage(page, '修改成功');
      await expect(page.locator('.el-table__row', { hasText: editedOptionLabel })).toBeVisible({ timeout: 10000 });

      const editedDataRow = page.locator('.el-table__row', { hasText: optionValue }).first();
      await editedDataRow.getByRole('button', { name: '删除' }).click();
      await expect(page.getByText('确认删除该字典数据?')).toBeVisible();
      const deleteDataResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/system/dict/data') &&
        response.request().method() === 'DELETE'
      );
      await page.getByRole('button', { name: '确定' }).last().click();
      const deleteDataResponse = await deleteDataResponsePromise;
      const deleteDataBody = await deleteDataResponse.json();
      expect(deleteDataResponse.status()).toBe(200);
      expect(deleteDataBody.success || deleteDataBody.code === 200).toBeTruthy();
      await expectLatestMessage(page, '删除成功');

      const editedTypeItem = page.locator('.type-item', { hasText: dictCode }).first();
      await editedTypeItem.getByRole('button', { name: '删除' }).click();
      await expect(page.getByText(`确认删除字典类型「${editedDictName}」?`)).toBeVisible();
      const deleteTypeResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/system/dict/type') &&
        response.request().method() === 'DELETE'
      );
      await page.getByRole('button', { name: '确定' }).last().click();
      const deleteTypeResponse = await deleteTypeResponsePromise;
      const deleteTypeBody = await deleteTypeResponse.json();
      expect(deleteTypeResponse.status()).toBe(200);
      expect(deleteTypeBody.success || deleteTypeBody.code === 200).toBeTruthy();
      await expectLatestMessage(page, '删除成功');
      await expect(page.getByText(dictCode)).toHaveCount(0);
      await expectNoAuthError(page);
    } finally {
      await cleanupDict(request, platformToken, dictCode).catch(() => undefined);
    }
  });

  test('A 公司不可见字典管理入口，维护接口返回 403，选项接口可读', async ({ page, request }) => {
    const companyToken = await loginToken(request, companyATenant);

    const listResponse = await request.get('http://localhost:5555/system/dict/type/list', {
      headers: { Authorization: `Bearer ${companyToken}` },
    });
    expect(listResponse.status()).toBe(403);

    const createResponse = await request.post('http://localhost:5555/system/dict/type', {
      headers: { Authorization: `Bearer ${companyToken}` },
      data: {
        dictName: `无权字典${Date.now()}`,
        dictType: `deny_dict_${Date.now()}`,
        status: 1,
      },
    });
    expect(createResponse.status()).toBe(403);

    const optionsResponse = await request.get('http://localhost:5555/system/dict/data/options?typeCode=sys_normal_disable', {
      headers: { Authorization: `Bearer ${companyToken}` },
    });
    expect(optionsResponse.status()).toBe(200);
    const optionsBody = await optionsResponse.json();
    expect(optionsBody.data).toEqual([
      { label: '启用', value: '1' },
      { label: '禁用', value: '0' },
    ]);

    await loginPage(page, companyATenant);
    await expect(page.getByText('账号权限').first()).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('基础数据')).toHaveCount(0);
    await expect(page.getByText('字典管理')).toHaveCount(0);
    await expectNoAuthError(page);
  });
});
