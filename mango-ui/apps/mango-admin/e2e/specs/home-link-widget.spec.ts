import { expect, test } from '@playwright/test';
import type { Page } from '@playwright/test';

async function login(page: Page) {
  if (!page.url().startsWith('http')) {
    await page.goto('/#/login');
  }

  const loginData = await page.evaluate(async () => {
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
      throw new Error(`登录失败：${JSON.stringify(body)}`);
    }
    return body.data;
  });

  await page.evaluate((data) => {
    const userInfo = {
      ...data,
      tenantId: data.tenantId || '1',
      tenantCode: data.tenantCode || 'default',
      tenantName: data.tenantName || '芒果集团',
      realm: data.realm || 'INTERNAL',
      actorType: data.actorType || 'INTERNAL_USER',
      partyType: data.partyType || 'INTERNAL_ORG',
      partyId: data.partyId || '1',
      appCode: data.appCode || 'internal-admin',
    };
    sessionStorage.setItem('MANGO_TOKEN', data.accessToken);
    sessionStorage.setItem('MANGO_REFRESH_TOKEN', data.refreshToken || '');
    sessionStorage.setItem('MANGO_TOKEN_EXPIRES_AT', String(Date.now() + Number(data.expiresIn || 7200) * 1000));
    sessionStorage.setItem('userInfo', JSON.stringify(userInfo));
    sessionStorage.setItem('tenantId', String(userInfo.tenantId));
    document.cookie = `MANGO_TOKEN=${encodeURIComponent(data.accessToken)}; path=/; SameSite=Lax`;
  }, loginData);

  await page.goto('/#/home');
  await expect(page).toHaveURL(/#\/home$/, { timeout: 15000 });
  return loginData.accessToken as string;
}

async function expectPersonalLinkFavorited(page: Page, linkId: string, keyword: string, expected: boolean) {
  const body = await page.evaluate(async ({ linkId, keyword }) => {
    const response = await fetch(`/api/link/personal-links/page?page=1&size=20&keyword=${encodeURIComponent(keyword)}`);
    const result = await response.json();
    if (!response.ok || !(result.success || result.code === 200)) {
      throw new Error(`查询个人网址失败：${response.status} ${JSON.stringify(result)}`);
    }
    return result;
  }, { linkId, keyword });
  const records = body.data?.list || body.data?.records || [];
  const item = records.find((record: { id?: string | number }) => String(record.id) === linkId);
  expect(item).toBeTruthy();
  expect(item.favorited).toBe(expected);
}

test.describe('首页小组件-网址导航', () => {
  test.setTimeout(60 * 1000);

  test.beforeEach(async ({ page }) => {
    const token = await login(page);
    await page.evaluate(async (token) => {
      const response = await fetch(`/api/grid-layout/personal?pageCode=${encodeURIComponent('admin-home-workbench')}`, {
        method: 'DELETE',
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      if (!response.ok && response.status !== 404) {
        const body = await response.text();
        throw new Error(`重置个人首页布局失败：${response.status} ${body}`);
      }
    }, token);

    await page.goto('/#/home');
  });

  test('管理员首页可看到网址导航小组件并可执行基础交互', async ({ page }) => {
    const pageErrors: string[] = [];
    const failedResponses: string[] = [];

    page.on('pageerror', (error) => {
      pageErrors.push(error.message);
    });
    page.on('response', (response) => {
      const status = response.status();
      if (status >= 400 && !response.url().includes('/favicon')) {
        failedResponses.push(`${status} ${response.url()}`);
      }
    });

    await expect(page.getByText('工作台', { exact: true })).toBeVisible({ timeout: 15000 });

    const linkWidget = page.locator('[data-surface="home.link-navigation"]');
    await expect(linkWidget).toBeVisible({ timeout: 15000 });

    // 校验核心分组与入口
    await expect(linkWidget.getByRole('button', { name: '我的收藏' })).toBeVisible();
    await expect(linkWidget.getByRole('button', { name: '企业导航' })).toBeVisible();
    await expect(linkWidget.getByRole('button', { name: '百度' })).toBeVisible();
    await expect(linkWidget.getByRole('button', { name: '谷歌' })).toBeVisible();

    // 搜索输入与按钮可交互
    const keywordInput = page.locator('[data-field="link-navigation.keyword"]');
    await expect(keywordInput).toBeVisible();
    await keywordInput.fill('example');

    // 搜索按钮文案可见
    await expect(linkWidget.getByRole('button', { name: '百度' })).toBeVisible();
    await expect(linkWidget.getByRole('button', { name: '谷歌' })).toBeVisible();

    // 非个人分组不允许新增网址。
    const addLinkButton = page.locator('[data-action="link-navigation.link.create"]');
    await expect(addLinkButton).toBeHidden();
    await linkWidget.locator('[data-record-key="enterprise"]').click();
    await expect(addLinkButton).toBeHidden();

    // 收藏/删除按钮在列表项存在时可显示；为空时不应引发报错。
    await expect(linkWidget.locator('.mango-grid-widget-link-navigation__items')).toBeVisible();

    const unique = `E2E_WIDGET_${Date.now()}`;
    const categoryName = `${unique}_CAT`;
    const renamedCategoryName = `${unique}_CAT_EDIT`;
    const linkTitle = `${unique}_LINK`;
    const linkUrl = `https://example.com/${unique.toLowerCase()}`;
    const ungroupedLinkTitle = `${unique}_UNGROUPED`;
    const ungroupedLinkUrl = `https://example.com/${unique.toLowerCase()}-ungrouped`;

    await expect(linkWidget.locator('[data-record-key="favorites"]')).toHaveText('我的收藏');
    await expect(linkWidget.locator('[data-record-key="enterprise"]')).toHaveText('企业导航');
    await expect(linkWidget.locator('[data-record-key="personal-ungrouped"]')).toHaveText('未分组');

    const createCategoryResponse = page.waitForResponse(response => response.url().includes('/api/link/personal-categories/create')
      && response.request().method() === 'POST');
    await linkWidget.locator('[data-action="link-navigation.category.create"]').click();
    const categoryDialog = page.getByRole('dialog', { name: '新增分组' });
    await expect(categoryDialog).toBeVisible();
    await categoryDialog.locator('[data-field="link-navigation.category.name"]').fill(categoryName);
    await categoryDialog.locator('[data-action="link-navigation.category.submit"]').click();
    const categoryBody = await (await createCategoryResponse).json();
    expect(categoryBody.success || categoryBody.code === 200).toBeTruthy();
    const categoryId = String(categoryBody.data);
    await expect(categoryDialog).toBeHidden({ timeout: 10000 });
    await expect(linkWidget.getByRole('button', { name: categoryName })).toBeVisible({ timeout: 10000 });
    await expect(addLinkButton).toBeVisible();

    const updateCategoryResponse = page.waitForResponse(response => response.url().includes('/api/link/personal-categories/update')
      && response.request().method() === 'PUT');
    await linkWidget.locator('[data-action="link-navigation.category.edit"]').click();
    const editCategoryDialog = page.getByRole('dialog', { name: '编辑分组' });
    await expect(editCategoryDialog).toBeVisible();
    await editCategoryDialog.locator('[data-field="link-navigation.category.name"]').fill(renamedCategoryName);
    await editCategoryDialog.locator('[data-action="link-navigation.category.update"]').click();
    const updateCategoryBody = await (await updateCategoryResponse).json();
    expect(updateCategoryBody.success || updateCategoryBody.code === 200).toBeTruthy();
    await expect(editCategoryDialog).toBeHidden({ timeout: 10000 });
    await expect(linkWidget.getByRole('button', { name: renamedCategoryName })).toBeVisible({ timeout: 10000 });

    const createLinkResponse = page.waitForResponse(response => response.url().includes('/api/link/personal-links/create')
      && response.request().method() === 'POST');
    await linkWidget.locator('[data-action="link-navigation.link.create"]').click();
    const createLinkDialog = page.getByRole('dialog', { name: '新增网址' });
    await expect(createLinkDialog).toBeVisible();
    await createLinkDialog.locator('[data-field="link-navigation.link.name"]').fill(linkTitle);
    await createLinkDialog.locator('[data-field="link-navigation.link.url"]').fill(linkUrl);
    await createLinkDialog.locator('[data-action="link-navigation.link.submit"]').click();
    const linkResponse = await createLinkResponse;
    const linkRequestBody = JSON.parse(linkResponse.request().postData() || '{}');
    expect(String(linkRequestBody.categoryId)).toBe(categoryId);
    const linkBody = await linkResponse.json();
    expect(linkBody.success || linkBody.code === 200).toBeTruthy();
    const linkId = String(linkBody.data);
    await expectPersonalLinkFavorited(page, linkId, linkTitle, false);
    await expect(createLinkDialog).toBeHidden({ timeout: 10000 });
    const categoryItem = linkWidget.locator(`[data-record-key="${linkId}"]`);
    await expect(categoryItem).toContainText(linkTitle, { timeout: 10000 });

    await linkWidget.locator('[data-record-key="favorites"]').click();
    const favoriteItem = linkWidget.locator(`[data-record-key="${linkId}"]`);
    await expect(favoriteItem).toBeHidden({ timeout: 10000 });

    await linkWidget.getByRole('button', { name: renamedCategoryName }).click();
    const createFavoriteResponse = page.waitForResponse(response => response.url().includes('/api/link/favorites/create')
      && response.request().method() === 'POST');
    await categoryItem.hover();
    await categoryItem.locator('[data-action="link-navigation.favorite"]').click();
    const favoriteBody = await (await createFavoriteResponse).json();
    expect(favoriteBody.success || favoriteBody.code === 200).toBeTruthy();
    await expectPersonalLinkFavorited(page, linkId, linkTitle, true);

    await linkWidget.locator('[data-record-key="favorites"]').click();
    await expect(favoriteItem).toContainText(linkTitle, { timeout: 10000 });
    const deleteFavoriteResponse = page.waitForResponse(response => response.url().includes('/api/link/favorites/delete')
      && response.request().method() === 'DELETE');
    await favoriteItem.hover();
    await favoriteItem.locator('[data-action="link-navigation.favorite"]').click();
    const deleteFavoriteBody = await (await deleteFavoriteResponse).json();
    expect(deleteFavoriteBody.success || deleteFavoriteBody.code === 200).toBeTruthy();
    await expect(favoriteItem).toBeHidden({ timeout: 10000 });
    await expectPersonalLinkFavorited(page, linkId, linkTitle, false);
    await linkWidget.getByRole('button', { name: renamedCategoryName }).click();
    await expect(categoryItem).toContainText(linkTitle, { timeout: 10000 });
    await expect(categoryItem.locator('[data-action="link-navigation.link.delete"]')).toHaveCount(0);
    await page.evaluate(async (id) => {
      const response = await fetch(`/api/link/personal-links/delete?id=${encodeURIComponent(id)}`, {
        method: 'DELETE',
      });
      const result = await response.json();
      if (!response.ok || !(result.success || result.code === 200)) {
        throw new Error(`清理测试网址失败：${response.status} ${JSON.stringify(result)}`);
      }
    }, linkId);
    await categoryItem.hover();

    const deleteCategoryResponse = page.waitForResponse(response => response.url().includes('/api/link/personal-categories/delete')
      && response.request().method() === 'DELETE');
    await linkWidget.locator('[data-action="link-navigation.category.delete"]').click();
    await page.getByRole('dialog', { name: '删除分组' }).getByRole('button', { name: '删除' }).click();
    const deleteCategoryBody = await (await deleteCategoryResponse).json();
    expect(deleteCategoryBody.success || deleteCategoryBody.code === 200).toBeTruthy();
    await expect(linkWidget.getByRole('button', { name: renamedCategoryName })).toBeHidden({ timeout: 10000 });

    await linkWidget.locator('[data-record-key="personal-ungrouped"]').click();
    await expect(addLinkButton).toBeVisible();
    const createUngroupedResponse = page.waitForResponse(response => response.url().includes('/api/link/personal-links/create')
      && response.request().method() === 'POST');
    await linkWidget.locator('[data-action="link-navigation.link.create"]').click();
    await expect(createLinkDialog).toBeVisible();
    await createLinkDialog.locator('[data-field="link-navigation.link.name"]').fill(ungroupedLinkTitle);
    await createLinkDialog.locator('[data-field="link-navigation.link.url"]').fill(ungroupedLinkUrl);
    await createLinkDialog.locator('[data-action="link-navigation.link.submit"]').click();
    const ungroupedResponse = await createUngroupedResponse;
    const ungroupedRequestBody = JSON.parse(ungroupedResponse.request().postData() || '{}');
    expect(ungroupedRequestBody.categoryId).toBeUndefined();
    const ungroupedBody = await ungroupedResponse.json();
    expect(ungroupedBody.success || ungroupedBody.code === 200).toBeTruthy();
    const ungroupedLinkId = String(ungroupedBody.data);
    await expectPersonalLinkFavorited(page, ungroupedLinkId, ungroupedLinkTitle, false);
    await expect(createLinkDialog).toBeHidden({ timeout: 10000 });
    const ungroupedItem = linkWidget.locator(`[data-record-key="${ungroupedLinkId}"]`);
    await expect(ungroupedItem).toContainText(ungroupedLinkTitle, { timeout: 10000 });

    expect(categoryId).toBeTruthy();

    expect(pageErrors.length).toBe(0);
    expect(failedResponses.length).toBe(0);
  });
});
