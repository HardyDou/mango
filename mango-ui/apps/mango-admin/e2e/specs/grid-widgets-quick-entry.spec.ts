import { expect, test } from '@playwright/test';

const QUICK_ENTRY_STORAGE_MARK = ':quick-entry:';

test.describe('工作台快捷入口小组件', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/#/login');
    await page.evaluate((mark) => {
      Object.keys(window.localStorage)
        .filter(key => key.includes(mark))
        .forEach(key => window.localStorage.removeItem(key));
    }, QUICK_ENTRY_STORAGE_MARK);

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
  });

  test('服务代理和工作台默认快捷入口可用', async ({ page, request }) => {
    const healthResponse = await request.get('/api/actuator/health');
    expect(healthResponse.ok()).toBeTruthy();
    await expect(page.getByRole('button', { name: '编辑布局' })).toBeVisible();
    await expect(page.getByText('快捷入口', { exact: true })).toBeVisible();
    await expect(page.locator('.mango-grid-widget-quick-entry__setting')).toBeVisible();

    // 未保存过快捷入口时，组件应使用可见菜单中的前几项作为默认入口。
    await expect(page.locator('.mango-grid-widget-quick-entry__item')).not.toHaveCount(0);
  });

  test('快捷入口支持搜索、清空、保存和刷新后保留', async ({ page }) => {
    const pageErrors: string[] = [];
    const failedResponses: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        pageErrors.push(message.text());
      }
    });
    page.on('response', (response) => {
      const status = response.status();
      if (status >= 400 && !response.url().includes('/favicon')) {
        failedResponses.push(`${status} ${response.url()}`);
      }
    });

    await page.locator('.mango-grid-widget-quick-entry__setting').click();
    const dialog = page.locator('.mango-dialog');
    await expect(dialog).toBeVisible();
    await expect(dialog.getByText('设置快捷入口')).toBeVisible();
    await expect(dialog.getByText('可选菜单')).toBeVisible();
    await expect(dialog.getByText('已选快捷入口')).toBeVisible();

    await dialog.getByPlaceholder('搜索菜单名称或路径').fill('菜单管理');
    await expect(dialog.locator('.mango-grid-widget-quick-entry-config__option', { hasText: '菜单管理' })).toBeVisible();

    await dialog.getByRole('button', { name: '清空' }).click();
    await expect(dialog.getByText('还没有选择快捷入口')).toBeVisible();
    await dialog.getByRole('button', { name: '保存' }).click();
    await expect(dialog).toBeHidden();
    await expect(page.getByText('暂无快捷入口')).toBeVisible();

    let storedSelection = await readQuickEntryStorage(page);
    expect(storedSelection).toEqual(['[]']);

    await page.getByRole('button', { name: '去设置' }).click();
    await expect(dialog).toBeVisible();
    await dialog.getByPlaceholder('搜索菜单名称或路径').fill('菜单管理');
    await dialog.locator('.mango-grid-widget-quick-entry-config__option', { hasText: '菜单管理' }).click();
    await dialog.getByRole('button', { name: '保存' }).click();
    await expect(dialog).toBeHidden();
    await expect(page.getByRole('button', { name: '菜单管理' })).toBeVisible();

    storedSelection = await readQuickEntryStorage(page);
    expect(storedSelection.length).toBe(1);
    expect(storedSelection[0]).not.toBe('[]');

    await page.reload();
    await expect(page.getByRole('button', { name: '菜单管理' })).toBeVisible();

    await page.getByRole('button', { name: '菜单管理' }).click();
    await expect(page).not.toHaveURL(/#\/home$/);

    expect(pageErrors).toEqual([]);
    expect(failedResponses).toEqual([]);
  });
});

async function readQuickEntryStorage(page: import('@playwright/test').Page): Promise<string[]> {
  return page.evaluate((mark) => {
    return Object.keys(window.localStorage)
      .filter(key => key.includes(mark))
      .map(key => window.localStorage.getItem(key) || '');
  }, QUICK_ENTRY_STORAGE_MARK);
}
