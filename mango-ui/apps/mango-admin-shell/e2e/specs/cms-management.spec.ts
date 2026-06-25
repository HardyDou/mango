import { expect, test, type APIRequestContext, type Locator, type Page } from '@playwright/test';
import { api as e2eApi } from '../support/api';

type ApiBody<T> = {
  code?: number;
  success?: boolean;
  msg?: string;
  data?: T;
};

type PageResult<T> = {
  list?: T[];
  total?: number;
};

type CmsRecord = Record<string, unknown> & { id?: string | number };

const tenantHeaders = { 'X-Tenant-Id': '1' };

function api(path: string) {
  return e2eApi(path);
}

function expectApiSuccess<T>(body: ApiBody<T>, message: string): T {
  expect(body.success || body.code === 200, `${message}: ${JSON.stringify(body)}`).toBeTruthy();
  return body.data as T;
}

function asId(value: unknown) {
  expect(value, `接口未返回有效 ID: ${String(value)}`).toBeTruthy();
  return String(value);
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
  const body = await response.json() as ApiBody<{ accessToken: string }>;
  return expectApiSuccess(body, '登录失败').accessToken;
}

function authHeaders(token: string) {
  return { ...tenantHeaders, Authorization: `Bearer ${token}` };
}

async function apiGet<T>(request: APIRequestContext, token: string, path: string, params?: Record<string, string | number | boolean | undefined>) {
  const response = await request.get(api(path), { headers: authHeaders(token), params });
  expect(response.status(), `${path} HTTP 状态错误`).toBe(200);
  return expectApiSuccess((await response.json()) as ApiBody<T>, `${path} 调用失败`);
}

async function apiPost<T>(request: APIRequestContext, token: string, path: string, data: Record<string, unknown>) {
  const response = await request.post(api(path), { headers: authHeaders(token), data });
  expect(response.status(), `${path} HTTP 状态错误`).toBe(200);
  return expectApiSuccess((await response.json()) as ApiBody<T>, `${path} 调用失败`);
}

async function apiPut<T>(request: APIRequestContext, token: string, path: string, data: Record<string, unknown>) {
  const response = await request.put(api(path), { headers: authHeaders(token), data });
  expect(response.status(), `${path} HTTP 状态错误`).toBe(200);
  return expectApiSuccess((await response.json()) as ApiBody<T>, `${path} 调用失败`);
}

async function apiDelete(request: APIRequestContext, token: string, path: string, id: string) {
  const response = await request.delete(api(path), { headers: authHeaders(token), params: { id } });
  if (response.status() === 200) {
    const body = await response.json() as ApiBody<boolean>;
    expect(body.success || body.code === 200, `${path} 删除失败: ${JSON.stringify(body)}`).toBeTruthy();
  }
}

async function login(page: Page) {
  await page.goto('/#/login');
  await page.getByPlaceholder('用户名').fill('admin');
  await page.getByPlaceholder('密码').fill('admin123');
  const accountTenantsResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/auth/login-institutions') && response.status() === 200
  );
  await page.getByPlaceholder('密码').blur();
  await accountTenantsResponsePromise;
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: /芒果集团/ }).click();
  await page.getByRole('button', { name: /^登\s*录$/ }).click();
  await page.waitForURL('**/#/home', { timeout: 10000 });
  await expect(page.locator('.shell-runtime-content')).toBeVisible();
}

async function openCmsPage(page: Page, path: string, title: string) {
  await page.goto(`/#${path}`);
  await page.waitForURL(`**/#${path}`, { timeout: 10000 });
  await expect(page.locator('main')).toContainText(title);
  await expect(page.locator('.cms-panel')).toBeVisible({ timeout: 10000 });
}

async function searchKeyword(page: Page, keyword: string) {
  await page.getByPlaceholder('名称/编码').fill(keyword);
  await page.getByRole('button', { name: '查询' }).click();
}

async function selectSearchSite(page: Page, siteName: string) {
  const toolbar = page.locator('.cms-toolbar');
  await toolbar.locator('.el-form-item', { hasText: '站点' }).locator('.el-select').click();
  await page.getByRole('option', { name: siteName }).last().click();
}

function rowByText(page: Page, text: string | RegExp) {
  return page.getByRole('row', { name: text });
}

function firstDialog(page: Page) {
  return page.locator('.el-dialog').last();
}

async function fillInput(dialog: Locator, label: string, value: string) {
  await dialog.getByLabel(label, { exact: true }).fill(value);
}

async function fillTextarea(dialog: Locator, label: string, value: string) {
  await dialog.getByLabel(label, { exact: true }).fill(value);
}

async function fillRichText(dialog: Locator, label: string, value: string) {
  const editor = formItem(dialog, label).locator('[contenteditable="true"]').first();
  await expect(editor).toBeVisible({ timeout: 10000 });
  await editor.click();
  await dialog.page().keyboard.press(process.platform === 'darwin' ? 'Meta+A' : 'Control+A');
  await dialog.page().keyboard.type(value);
  await expect(editor).toContainText(value.slice(0, 24));
}

async function selectValue(dialog: Locator, label: string, option: string | RegExp) {
  await formItem(dialog, label).locator('.el-select').click();
  await dialog.page().getByRole('option', { name: option }).last().click();
}

async function selectValues(dialog: Locator, label: string, options: Array<string | RegExp>) {
  await formItem(dialog, label).locator('.el-select').click();
  for (const option of options) {
    await dialog.page().getByRole('option', { name: option }).last().click();
  }
  await dialog.page().keyboard.press('Escape');
}

async function checkButton(dialog: Locator, label: string, option: string) {
  const button = formItem(dialog, label)
    .locator('.el-checkbox-button')
    .filter({ hasText: new RegExp(`^\\s*${escapeRegExp(option)}\\s*$`) })
    .first();
  await expect(button).toBeVisible({ timeout: 10000 });
  if (!(await button.evaluate(element => element.classList.contains('is-checked')))) {
    await button.click();
  }
}

function formItem(scope: Locator, label: string) {
  return scope.locator('.el-form-item').filter({
    has: scope.page().locator('.el-form-item__label').filter({ hasText: new RegExp(`^\\*?\\s*${escapeRegExp(label)}$`) }),
  });
}

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

async function createByDialog(page: Page, expectedText: string, fill: (dialog: Locator) => Promise<void>) {
  await page.getByRole('button', { name: '新增' }).click();
  const dialog = firstDialog(page);
  await expect(dialog).toBeVisible();
  await fill(dialog);
  await dialog.getByRole('button', { name: '保存' }).click();
  await expect(page.getByText('保存成功')).toBeVisible({ timeout: 10000 });
  await expect(dialog).toBeHidden({ timeout: 10000 });
  await expect(rowByText(page, expectedText)).toBeVisible({ timeout: 10000 });
}

async function editRow(page: Page, rowText: string | RegExp, fill: (dialog: Locator) => Promise<void>) {
  const row = rowByText(page, rowText);
  await expect(row).toBeVisible({ timeout: 10000 });
  await row.getByRole('button', { name: '编辑' }).click();
  const dialog = firstDialog(page);
  await expect(dialog).toBeVisible();
  await fill(dialog);
  await dialog.getByRole('button', { name: '保存' }).click();
  await expect(page.getByText('保存成功')).toBeVisible({ timeout: 10000 });
  await expect(dialog).toBeHidden({ timeout: 10000 });
}

async function cleanupCmsData(request: APIRequestContext, token: string, keyword: string) {
  const deletes = [
    { list: '/cms/ad-deliveries/page', remove: '/cms/ad-deliveries' },
    { list: '/cms/advertisements/page', remove: '/cms/advertisements' },
    { list: '/cms/navigations/page', remove: '/cms/navigations' },
    { list: '/cms/content-publishes/page', offline: '/cms/content-publishes/offline' },
    { list: '/cms/contents/page', offline: '/cms/contents/offline', remove: '/cms/contents' },
    { list: '/cms/site-categories/tree', remove: '/cms/site-categories', tree: true },
    { list: '/cms/content-tags/page', remove: '/cms/content-tags' },
    { list: '/cms/content-categories/page', remove: '/cms/content-categories' },
    { list: '/cms/sites/page', remove: '/cms/sites' },
  ];

  for (const item of deletes) {
    const records = item.tree
      ? await apiGet<CmsRecord[]>(request, token, item.list, {}).catch(() => [])
      : (await apiGet<PageResult<CmsRecord>>(request, token, item.list, { page: 1, size: 100, keyword }).catch(() => ({ list: [] }))).list || [];
    for (const record of flattenRecords(records).filter(record => JSON.stringify(record).includes(keyword))) {
      const id = asId(record.id);
      if (item.offline) {
        await apiPost<boolean>(request, token, item.offline, { id }).catch(() => undefined);
      }
      if (item.remove) {
        await apiDelete(request, token, item.remove, id).catch(() => undefined);
      }
    }
  }
}

function flattenRecords(records: CmsRecord[]): CmsRecord[] {
  return records.flatMap(record => [record, ...flattenRecords((record.children || []) as CmsRecord[])]);
}

test.describe('CMS 管理后台 E2E', () => {
  test('CMS 资源管理、状态流转、发布关系和站点详情可用', async ({ page, request }) => {
    const unique = Date.now();
    const keyword = String(unique);
    const siteName = `E2E 官网 ${unique}`;
    const siteCode = `E2E_SITE_${unique}`;
    const updatedSiteName = `${siteName} 更新`;
    const categoryName = `E2E 分类 ${unique}`;
    const categoryCode = `E2E_CAT_${unique}`;
    const tagName = `E2E 标签 ${unique}`;
    const tagCode = `E2E_TAG_${unique}`;
    const siteCategoryName = `E2E 栏目 ${unique}`;
    const siteCategoryCode = `E2E_CHANNEL_${unique}`;
    const contentTitle = `E2E 内容 ${unique}`;
    const updatedContentTitle = `${contentTitle} 更新`;
    const navName = `E2E 导航 ${unique}`;
    const adName = `E2E 广告位 ${unique}`;
    const adCode = `E2E_AD_${unique}`;
    const adDeliveryName = `E2E 广告投放 ${unique}`;

    const token = await loginToken(request);
    await cleanupCmsData(request, token, keyword);

    await login(page);

    await openCmsPage(page, '/cms/sites', '站点管理');
    await searchKeyword(page, keyword);
    await createByDialog(page, siteName, async (dialog) => {
      await fillInput(dialog, '站点名称', siteName);
      await fillInput(dialog, '站点编码', siteCode);
      await fillInput(dialog, '域名', `${siteCode.toLowerCase()}.example.test`);
      await fillInput(dialog, '默认语言', 'zh-CN');
      await fillTextarea(dialog, '站点描述', `${keyword} 站点描述`);
      await fillInput(dialog, 'SEO 标题', `${keyword} SEO`);
      await fillInput(dialog, '版权信息', `${keyword} copyright`);
    });
    await editRow(page, siteName, async (dialog) => {
      await fillInput(dialog, '站点名称', updatedSiteName);
    });
    await expect(rowByText(page, updatedSiteName)).toBeVisible({ timeout: 10000 });
    await rowByText(page, updatedSiteName).getByRole('button', { name: '详情' }).click();
    await expect(page.getByRole('dialog', { name: '站点管理详情' })).toContainText(`${keyword} SEO`, { timeout: 10000 });
    await page.getByRole('button', { name: '关闭' }).click();
    await rowByText(page, updatedSiteName).getByRole('button', { name: '禁用' }).click();
    await expect(page.getByText('状态已更新')).toBeVisible({ timeout: 10000 });
    await expect(rowByText(page, updatedSiteName)).toContainText('禁用', { timeout: 10000 });
    await rowByText(page, updatedSiteName).getByRole('button', { name: '启用' }).click();
    await expect(rowByText(page, updatedSiteName)).toContainText('启用', { timeout: 10000 });

    const site = (await apiGet<PageResult<CmsRecord>>(request, token, '/cms/sites/page', { page: 1, size: 10, keyword })).list?.[0];
    const siteId = asId(site?.id);
    expect(site?.siteName).toBe(updatedSiteName);

    await openCmsPage(page, '/cms/content-categories', '内容分类');
    await searchKeyword(page, keyword);
    await createByDialog(page, categoryName, async (dialog) => {
      await fillInput(dialog, '分类名称', categoryName);
      await fillInput(dialog, '分类编码', categoryCode);
      await dialog.getByLabel('排序', { exact: true }).fill('10');
      await fillTextarea(dialog, '备注', `${keyword} 分类备注`);
    });
    const contentCategory = (await apiGet<PageResult<CmsRecord>>(request, token, '/cms/content-categories/page', { page: 1, size: 10, keyword })).list?.[0];
    const contentCategoryId = asId(contentCategory?.id);

    await openCmsPage(page, '/cms/content-tags', '内容标签');
    await searchKeyword(page, keyword);
    await createByDialog(page, tagName, async (dialog) => {
      await fillInput(dialog, '标签名称', tagName);
      await fillInput(dialog, '标签编码', tagCode);
      await dialog.getByLabel('排序', { exact: true }).fill('20');
      await fillTextarea(dialog, '备注', `${keyword} 标签备注`);
    });

    await openCmsPage(page, '/cms/site-categories', '站点栏目');
    await selectSearchSite(page, updatedSiteName);
    await createByDialog(page, siteCategoryName, async (dialog) => {
      await selectValue(dialog, '所属站点', updatedSiteName);
      await fillInput(dialog, '栏目名称', siteCategoryName);
      await fillInput(dialog, '访问路径', `/e2e/${siteCategoryCode.toLowerCase()}`);
      await selectValue(dialog, '显示状态', '启用');
      await dialog.getByLabel('排序', { exact: true }).fill('30');
    });
    const siteCategories = await apiGet<CmsRecord[]>(request, token, '/cms/site-categories/tree', { siteId });
    const siteCategoryId = asId(flattenRecords(siteCategories).find(item => item.categoryName === siteCategoryName)?.id);
    await rowByText(page, siteCategoryName).getByRole('button', { name: '禁用' }).click();
    await expect(rowByText(page, siteCategoryName)).toContainText('禁用', { timeout: 10000 });
    await rowByText(page, siteCategoryName).getByRole('button', { name: '启用' }).click();
    await expect(rowByText(page, siteCategoryName)).toContainText('启用', { timeout: 10000 });

    await openCmsPage(page, '/cms/contents', '内容管理');
    await searchKeyword(page, keyword);
    await createByDialog(page, contentTitle, async (dialog) => {
      await fillInput(dialog, '标题', contentTitle);
      await fillInput(dialog, '副标题', `${keyword} 副标题`);
      await selectValue(dialog, '内容类型', '文章');
      await selectValue(dialog, '内容分类', categoryName);
      await fillInput(dialog, '作者', 'E2E');
      await fillTextarea(dialog, '摘要', `${keyword} 摘要`);
      await fillRichText(dialog, '正文', `${keyword} 富文本正文，包含 CMS 编辑器录入内容。`);
    });
    await editRow(page, contentTitle, async (dialog) => {
      await fillInput(dialog, '标题', updatedContentTitle);
    });
    await expect(rowByText(page, updatedContentTitle)).toContainText('草稿', { timeout: 10000 });
    await rowByText(page, updatedContentTitle).getByRole('button', { name: '提交' }).click();
    await expect(rowByText(page, updatedContentTitle)).toContainText('待审核', { timeout: 10000 });
    await rowByText(page, updatedContentTitle).getByRole('button', { name: '通过' }).click();
    await expect(rowByText(page, updatedContentTitle)).toContainText('已发布', { timeout: 10000 });
    const content = (await apiGet<PageResult<CmsRecord>>(request, token, '/cms/contents/page', { page: 1, size: 10, keyword })).list?.[0];
    const contentId = asId(content?.id);
    expect(content?.status).toBe('PUBLISHED');

    await openCmsPage(page, '/cms/content-publishes', '内容发布');
    await selectSearchSite(page, updatedSiteName);
    await createByDialog(page, updatedContentTitle, async (dialog) => {
      await selectValues(dialog, '发布内容', [updatedContentTitle]);
      await selectValue(dialog, '所属站点', updatedSiteName);
      await selectValues(dialog, '发布栏目', [siteCategoryName]);
      await dialog.getByLabel('排序', { exact: true }).fill('1');
    });
    await expect(rowByText(page, updatedContentTitle)).toContainText('已发布', { timeout: 10000 });
    await rowByText(page, updatedContentTitle).getByRole('button', { name: '下线' }).click();
    await expect(page.getByText('发布关系已下线')).toBeVisible({ timeout: 10000 });
    const publish = (await apiGet<PageResult<CmsRecord>>(request, token, '/cms/content-publishes/page', { page: 1, size: 10, siteId })).list
      ?.find(item => String(item.contentId) === contentId);
    expect(publish?.publishStatus).toBe('OFFLINE');

    await openCmsPage(page, '/cms/navigations', '导航管理');
    await selectSearchSite(page, updatedSiteName);
    await createByDialog(page, navName, async (dialog) => {
      await selectValue(dialog, '所属站点', updatedSiteName);
      await fillInput(dialog, '导航名称', navName);
      await selectValue(dialog, '导航位置', '顶部导航');
      await selectValue(dialog, '导航类型', '栏目');
      await selectValue(dialog, '栏目', siteCategoryName);
      await dialog.getByLabel('排序', { exact: true }).fill('1');
    });

    await openCmsPage(page, '/cms/advertisements', '广告位管理');
    await selectSearchSite(page, updatedSiteName);
    await createByDialog(page, adName, async (dialog) => {
      await selectValue(dialog, '所属站点', updatedSiteName);
      await fillInput(dialog, '广告位名称', adName);
      await fillInput(dialog, '广告位编码', adCode);
      await selectValue(dialog, '位置类型', '首页推荐');
      await fillInput(dialog, '位置编码', 'HOME_SIDE');
      await checkButton(dialog, '支持物料', '文本');
      await dialog.getByLabel('排序', { exact: true }).fill('1');
    });

    await openCmsPage(page, '/cms/ad-deliveries', '广告投放管理');
    await selectSearchSite(page, updatedSiteName);
    await createByDialog(page, adDeliveryName, async (dialog) => {
      await selectValue(dialog, '所属站点', updatedSiteName);
      await selectValue(dialog, '广告位', adName);
      await fillInput(dialog, '投放名称', adDeliveryName);
      await selectValue(dialog, '物料类型', '文本');
      await fillInput(dialog, '标题', `${keyword} 投放标题`);
      await fillTextarea(dialog, '文本内容', `${keyword} 广告投放文本`);
      await fillInput(dialog, '跳转链接', `/e2e/${siteCategoryCode.toLowerCase()}`);
      await dialog.getByLabel('排序', { exact: true }).fill('1');
    });

    await openCmsPage(page, '/cms/contents', '内容管理');
    await searchKeyword(page, keyword);
    await rowByText(page, updatedContentTitle).getByRole('button', { name: '下线' }).click();
    await expect(rowByText(page, updatedContentTitle)).toContainText('已下线', { timeout: 10000 });

    await cleanupCmsData(request, token, keyword);
  });
});
