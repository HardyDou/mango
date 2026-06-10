import { expect, type Page, test } from '@playwright/test';

type LayoutName = 'defaults' | 'columns';

async function login(page: Page) {
  await page.goto('/#/login');
  const accountTenantsResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/auth/login-institutions') && response.status() === 200
  );
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  await page.locator('input[placeholder="密码"]').blur();
  await accountTenantsResponsePromise;
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: /芒果集团/ }).click();

  const menuResponsePromise = page.waitForResponse((response) => {
    const url = response.url();
    return response.status() === 200
      && url.includes('/api/authorization/menus/user')
      && url.includes('fmt=tree');
  });
  await page.locator('.login-btn').click();
  await page.waitForURL('**/#/home', { timeout: 10000 });
  await menuResponsePromise;
}

async function bootstrapTheme(page: Page, theme: Record<string, unknown>) {
  await page.addInitScript((themeConfig) => {
    window.localStorage.setItem('themeConfig', JSON.stringify(themeConfig));
    window.localStorage.removeItem('layoutRestored');
  }, theme);
}

function collectConsoleErrors(page: Page) {
  const consoleErrors: string[] = [];
  page.on('console', (message) => {
    if (message.type() === 'error') {
      consoleErrors.push(message.text());
    }
  });
  page.on('pageerror', (error) => {
    consoleErrors.push(error.message);
  });
  return consoleErrors;
}

async function expectFixedShell(page: Page, layout: LayoutName) {
  await login(page);
  await expect(page.locator('.layout-main--fixed-shell')).toBeVisible({ timeout: 10000 });
  await expect(page.locator('.layout-main-scroll--content')).toBeVisible();
  await expect(page.locator('.layout-footer')).toBeVisible();
  await expect(page.locator('.tags-view-container')).toBeVisible();

  const shellMetrics = await page.evaluate(() => {
    const main = document.querySelector('.layout-main');
    const body = document.querySelector('.layout-main-body--fixed-shell');
    const contentScroll = document.querySelector('.layout-main-scroll--content');
    const footer = document.querySelector('.layout-footer');
    if (!main || !body || !contentScroll || !footer) {
      return null;
    }

    const bodyRect = body.getBoundingClientRect();
    const scrollRect = contentScroll.getBoundingClientRect();
    const footerRect = footer.getBoundingClientRect();
    const viewportHeight = window.innerHeight;

    return {
      mainOverflow: getComputedStyle(main).overflow,
      bodyOverflow: getComputedStyle(body).overflow,
      scrollOverflowY: getComputedStyle(contentScroll).overflowY,
      scrollBottom: Math.round(scrollRect.bottom),
      footerTop: Math.round(footerRect.top),
      footerBottom: Math.round(footerRect.bottom),
      bodyHeight: Math.round(bodyRect.height),
      viewportHeight,
    };
  });

  expect(shellMetrics, `${layout} shell metrics`).not.toBeNull();
  expect(shellMetrics?.mainOverflow).toBe('hidden');
  expect(shellMetrics?.bodyOverflow).toBe('hidden');
  expect(shellMetrics?.scrollOverflowY).toBe('auto');
  expect(shellMetrics?.footerTop).toBe(shellMetrics?.scrollBottom);
  expect(shellMetrics?.footerBottom).toBeLessThanOrEqual(shellMetrics?.viewportHeight ?? 0);
  expect(shellMetrics?.bodyHeight).toBeGreaterThan(100);
}

test.describe('布局固定 shell 与 TagsView 样式兼容', () => {
  for (const layout of ['defaults', 'columns'] as const) {
    test(`${layout} 布局固定顶部和底部，仅中间内容滚动`, async ({ page }, testInfo) => {
      const consoleErrors = collectConsoleErrors(page);
      await bootstrapTheme(page, {
        layout,
        isTagsview: true,
        isFooter: true,
        tagsStyle: 'tags-style-classic',
      });

      await expectFixedShell(page, layout);
      await page.screenshot({
        path: testInfo.outputPath(`${layout}-fixed-shell.png`),
        fullPage: true,
      });
      expect(consoleErrors).toEqual([]);
    });
  }

  test('旧 TagsView 样式 key 会恢复为新的胶囊样式 class', async ({ page }, testInfo) => {
    const consoleErrors = collectConsoleErrors(page);
    await bootstrapTheme(page, {
      layout: 'classic',
      isTagsview: true,
      isFooter: true,
      tagsStyle: 'tags-style-one',
    });

    await login(page);
    await expect(page.locator('.tags-view-container.tags-style-capsule')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('.tags-view-item.active.tags-style-capsule')).toBeVisible();
    await expect(page.locator('.tags-view-container.tags-style-one')).toHaveCount(0);
    await page.screenshot({
      path: testInfo.outputPath('legacy-tags-style-capsule.png'),
      fullPage: true,
    });
    expect(consoleErrors).toEqual([]);
  });

  test('设置面板展示新的 TagsView 样式选项', async ({ page }, testInfo) => {
    const consoleErrors = collectConsoleErrors(page);
    await login(page);
    await page.locator('.layout-breadcrumb-settings').click();
    await expect(page.getByText('Tagsview 样式')).toBeVisible({ timeout: 10000 });
    await page.locator('.layout-settings-item', { hasText: 'Tagsview 样式' }).locator('.el-select').click();

    await expect(page.getByRole('option', { name: '胶囊' })).toBeVisible();
    await expect(page.getByRole('option', { name: '卡片' })).toBeVisible();
    await expect(page.getByRole('option', { name: '经典' })).toBeVisible();
    await expect(page.getByRole('option', { name: /样式一|样式四|样式五/ })).toHaveCount(0);
    await page.screenshot({
      path: testInfo.outputPath('settings-tags-style-options.png'),
      fullPage: true,
    });
    expect(consoleErrors).toEqual([]);
  });
});
