import { expect, test, type APIRequestContext, type Page } from '@playwright/test';

const apiBaseURL = process.env.PLAYWRIGHT_API_BASE_URL || 'http://localhost:5555';

function api(path: string) {
  return `${apiBaseURL}${path}`;
}

function expectApiSuccess(body: any, message: string) {
  expect(body.success || body.code === 200, `${message}: ${JSON.stringify(body)}`).toBeTruthy();
}

async function loginToken(request: APIRequestContext) {
  const response = await request.post(api('/auth/login'), {
    data: {
      username: 'admin',
      password: 'admin123',
      tenantId: '1',
    },
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  expectApiSuccess(body, '登录失败');
  return body.data.accessToken as string;
}

function authHeaders(token: string) {
  return { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1' };
}

async function login(page: Page) {
  await page.goto('/#/login');
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  const accountTenantsResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/auth/login-institutions') && response.status() === 200
  );
  await page.locator('input[placeholder="密码"]').blur();
  await accountTenantsResponsePromise;
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: /芒果集团/ }).click();
  await page.locator('.login-btn').click();
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

async function openTopMenu(page: Page, name: string) {
  await page.getByRole('button', { name }).evaluate((button: HTMLButtonElement) => button.click());
}

async function openTemplatePage(page: Page, path: string, marker: string) {
  await page.goto(`/#${path}`);
  await expect(page).toHaveURL(new RegExp(`#${path}$`));
  await expect(page.getByText(marker).first()).toBeVisible({ timeout: 10000 });
}

async function cleanupTemplateData(request: APIRequestContext, token: string, keyword: string) {
  const headers = authHeaders(token);
  const templatesResponse = await request.get(api(`/template/templates/page?page=1&size=50&keyword=${keyword}`), { headers });
  if (templatesResponse.ok()) {
    const body = await templatesResponse.json();
    for (const item of body.data?.list || []) {
      await request.delete(api(`/template/templates?id=${item.id}`), { headers }).catch(() => undefined);
    }
  }

  const categoriesResponse = await request.get(api(`/template/categories/page?page=1&size=50&keyword=${keyword}`), { headers });
  if (categoriesResponse.ok()) {
    const body = await categoriesResponse.json();
    for (const item of body.data?.list || []) {
      await request.delete(api(`/template/categories?id=${item.id}`), { headers }).catch(() => undefined);
    }
  }
}

async function setCodeMirrorValue(page: Page, root: ReturnType<Page['locator']>, value: string) {
  const editor = root.locator('.CodeMirror').first();
  await expect(editor).toBeVisible({ timeout: 10000 });
  await editor.click();
  await page.keyboard.press(process.platform === 'darwin' ? 'Meta+A' : 'Control+A');
  await page.keyboard.insertText(value);
  await expect(editor.locator('.CodeMirror-code')).toContainText(value.split('\n')[0]);
}

test.describe('模板管理 E2E', () => {
  test('分类、模板列表、版本发布/设为生效和渲染记录流程可用', async ({ page, request }) => {
    const unique = Date.now();
    const keyword = `E2E_TPL_${unique}`;
    const categoryName = `E2E 分类 ${unique}`;
    const categoryCode = `E2E_CAT_${unique}`;
    const templateName = `E2E 模板 ${unique}`;
    const templateCode = `E2E_TPL_${unique}`;
    const v1Content = 'V1 客户：${customer.name}，金额：${amount}';
    const v2Content = 'V2 客户：${customer.name}，金额：${amount}';

    const token = await loginToken(request);
    await cleanupTemplateData(request, token, keyword);

    await login(page);
    await openTopMenu(page, '平台能力');
    await expect(page.getByText('模板管理', { exact: true })).toBeVisible({ timeout: 10000 });

    await openTemplatePage(page, '/template/categories', '分类编码');
    await page.locator('.action-toolbar').getByRole('button', { name: '新增分类' }).click();
    const categoryDialog = page.locator('.el-dialog').filter({ hasText: '新增分类' });
    await categoryDialog.getByPlaceholder('如 CONTRACT').fill(categoryCode);
    await categoryDialog.getByPlaceholder('如 合同文书').fill(categoryName);
    await categoryDialog.getByRole('button', { name: '保存' }).click();
    await expect(page.getByText('分类已创建')).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('cell', { name: categoryCode })).toBeVisible({ timeout: 10000 });

    await openTemplatePage(page, '/template/templates', '模板格式');
    await expect(page.locator('.action-toolbar').getByRole('button', { name: '批量删除' })).toBeVisible();
    await page.locator('.action-toolbar').getByRole('button', { name: '新增模板' }).click();
    const templatePage = page.locator('.page-card').filter({ hasText: '模板维护' });
    await templatePage.getByPlaceholder('如 CONTRACT_NOTICE').fill(templateCode);
    await templatePage.getByPlaceholder('如 合同到期提醒').fill(templateName);
    await templatePage.locator('.el-select').first().click();
    await page.getByRole('option', { name: categoryName }).click();
    await templatePage.getByPlaceholder('记录适用场景、调用方或维护说明').fill('E2E 只按模板编码渲染');
    await templatePage.getByRole('button', { name: '保存', exact: true }).click();
    await expect(page.getByText('模板已创建，发布后生效')).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('row', { name: new RegExp(templateCode) })).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('row', { name: new RegExp(templateCode) })).toContainText('未同步');
    await expect(page.getByText('业务KEY')).toHaveCount(0);

    await page.getByRole('row', { name: new RegExp(templateCode) }).getByRole('button', { name: '编辑' }).click();
    await expect(templatePage).toBeVisible({ timeout: 10000 });
    await expect(templatePage.getByPlaceholder('如 合同到期提醒')).toHaveValue(templateName, { timeout: 10000 });
    await setCodeMirrorValue(page, templatePage, v1Content);
    await templatePage.getByRole('button', { name: '提取变量' }).click();
    await expect(templatePage.getByRole('row', { name: /customer\.name/ })).toBeVisible({ timeout: 10000 });
    await expect(templatePage.getByRole('row', { name: /amount/ })).toBeVisible({ timeout: 10000 });
    await templatePage.getByRole('button', { name: '发布', exact: true }).click();
    await expect(page.getByText('发布成功')).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('row', { name: new RegExp(templateCode) })).toContainText('V1', { timeout: 10000 });
    await expect(page.getByRole('row', { name: new RegExp(templateCode) })).toContainText('已同步', { timeout: 10000 });

    await page.getByRole('row', { name: new RegExp(templateCode) }).getByRole('button', { name: '编辑' }).click();
    await expect(templatePage.getByText('历史版本')).toHaveCount(0);
    await setCodeMirrorValue(page, templatePage, v2Content);
    await templatePage.getByRole('button', { name: '保存', exact: true }).click();
    await expect(page.getByText('保存成功，修改未同步')).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('row', { name: new RegExp(templateCode) })).toContainText('未同步', { timeout: 10000 });
    await page.getByRole('row', { name: new RegExp(templateCode) }).getByRole('button', { name: '编辑' }).click();
    await templatePage.getByRole('button', { name: '发布', exact: true }).click();
    await expect(page.getByText('发布成功')).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('row', { name: new RegExp(templateCode) })).toContainText('V2', { timeout: 10000 });
    await expect(page.getByRole('row', { name: new RegExp(templateCode) })).toContainText('已同步', { timeout: 10000 });

    const listRow = page.getByRole('row', { name: new RegExp(templateCode) });
    await expect(listRow.getByRole('button', { name: '详情' })).toHaveCount(0);
    await listRow.getByRole('button', { name: '历史版本' }).click();
    const versionsPage = page.locator('.page-card').filter({ hasText: '历史版本' });
    await expect(versionsPage.getByRole('cell', { name: 'V1', exact: true })).toBeVisible({ timeout: 10000 });
    await expect(versionsPage.getByRole('cell', { name: 'V2', exact: true })).toBeVisible({ timeout: 10000 });
    await versionsPage.locator('button:has-text("设为生效"):not(.is-disabled)').click();
    await page.locator('.el-message-box').getByRole('button', { name: /确定|OK/ }).click();
    await expect(page.getByText('V1 已设为生效版本')).toBeVisible({ timeout: 10000 });
    await versionsPage.getByRole('button', { name: '预览' }).first().click();

    const previewPage = page.locator('.page-card').filter({ hasText: '模板预览' });
    await expect(previewPage).toBeVisible({ timeout: 10000 });
    await expect(previewPage.getByText('参数输入')).toBeVisible();
    await previewPage.locator('.render-variable-item').filter({ hasText: 'customer.name' }).getByRole('textbox').fill('李四');
    await previewPage.locator('.render-variable-item').filter({ hasText: 'amount' }).getByRole('textbox').fill('1000');
    await previewPage.getByRole('tab', { name: 'JSON 输入' }).click();
    await expect(previewPage.locator('textarea')).toHaveValue(/"customer"/);
    await previewPage.getByRole('tab', { name: '表单填写' }).click();
    const renderResponsePromise = page.waitForResponse((response) => {
      const postData = response.request().postData() || '';
      return response.url().includes('/api/template/templates/render')
        && response.request().method() === 'POST'
        && postData.includes('"templateCode"')
        && postData.includes(templateCode)
        && !postData.includes('"businessKey"');
    });
    await previewPage.getByRole('button', { name: '生成预览' }).click();
    await renderResponsePromise;
    await expect(page.getByText('渲染完成')).toBeVisible({ timeout: 10000 });
    await expect(previewPage.getByText('V2 客户：李四，金额：1000')).toBeVisible({ timeout: 10000 });

    await openTemplatePage(page, '/template/render-records', '输出格式');
    await page.getByPlaceholder('输入模板编码').fill(templateCode);
    await page.getByRole('button', { name: '查询' }).click();
    await expect(page.getByRole('row', { name: new RegExp(templateCode) })).toContainText('成功', { timeout: 10000 });
    await page.getByRole('row', { name: new RegExp(templateCode) }).getByRole('button', { name: '详情' }).click();
    const recordDrawer = page.locator('.el-drawer').filter({ hasText: '渲染详情' });
    await expect(recordDrawer.getByText('V2 客户：李四，金额：1000')).toBeVisible({ timeout: 10000 });

    await openTemplatePage(page, '/template/templates', '模板格式');
    const row = page.getByRole('row', { name: new RegExp(templateCode) });
    await expect(row.getByRole('button', { name: '删除' })).toBeVisible();
    await row.locator('.el-checkbox').click();
    await expect(page.locator('.action-toolbar').getByRole('button', { name: '批量删除' })).toBeEnabled();
    await cleanupTemplateData(request, token, keyword);
  });
});
