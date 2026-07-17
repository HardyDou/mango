import { expect, test, type Page, type TestInfo } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';
import { collectBrowserDiagnostics } from '../support/browser-diagnostics';

type HomePage = {
  id?: string | number;
  name?: string;
  defaultPage?: boolean;
  builtIn?: boolean;
};

const EVIDENCE_DIR = resolve(
  __dirname,
  '../../../../../mango-docs/evidence/baselines/home/latest/e2e',
);

async function login(page: Page): Promise<string> {
  await page.goto('/#/login');
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

  return loginData.accessToken as string;
}

async function api<T>(page: Page, token: string, path: string, options: RequestInit = {}): Promise<T> {
  return page.evaluate(
    async ({ path, options, token }) => {
      const response = await fetch(`/api${path}`, {
        ...options,
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
          ...(options.headers || {}),
        },
      });
      const body = await response.json().catch(() => ({}));
      if (!response.ok || !(body.success || body.code === 200 || body.code === '200')) {
        throw new Error(`接口请求失败：${path} ${response.status} ${JSON.stringify(body)}`);
      }
      return body.data;
    },
    { path, options, token },
  );
}

async function listHomePages(page: Page, token: string): Promise<HomePage[]> {
  return api<HomePage[]>(page, token, '/home/pages');
}

async function cleanupHomePages(page: Page, token: string, prefix: string) {
  const pages = await listHomePages(page, token);
  for (const item of pages) {
    if (item.id && item.name?.startsWith(prefix)) {
      await api<HomePage>(page, token, '/home/pages', { method: 'DELETE', body: JSON.stringify({ id: item.id }) });
    }
  }
}

async function capture(page: Page, testInfo: TestInfo, name: string) {
  mkdirSync(EVIDENCE_DIR, { recursive: true });
  const path = resolve(EVIDENCE_DIR, name);
  await page.screenshot({ path, fullPage: true });
  await testInfo.attach(name, { path, contentType: 'image/png' });
}

async function createHomePage(page: Page, name: string) {
  await page.locator('[data-action="home.create"]').click();
  const dialog = page.locator('[data-surface="home.name-form"]');
  await expect(dialog).toBeVisible();
  await page.locator('.el-dialog input.el-input__inner').fill(name);
  await Promise.all([
    page.waitForResponse(response => response.url().includes('/api/home/pages') && response.request().method() === 'POST'),
    page.locator('[data-action="home.name.submit"]').click(),
  ]);
  await expectActiveHomeTab(page, name);
}

async function expectActiveHomeTab(page: Page, name: string) {
  await expect(page.locator('[data-field="home.current-page"] [data-state="active"]')).toContainText(name, { timeout: 10000 });
}

test.describe('用户多首页工作台', () => {
  test.setTimeout(90 * 1000);
  const browserDiagnostics = new WeakMap<Page, string[]>();

  test.beforeEach(async ({ page }) => {
    browserDiagnostics.set(page, collectBrowserDiagnostics(page));
  });

  test.afterEach(async ({ page }) => {
    expect(browserDiagnostics.get(page)).toEqual([]);
  });

  test('@p0 @home 支持创建、重命名、复制、排序、默认首页和指定首页路由', async ({ page }, testInfo) => {
    const prefix = `E2E首页${Date.now()}`;
    const token = await login(page);
    await cleanupHomePages(page, token, prefix);

    await page.goto('/#/home');
    await expect(page.locator('[data-page="home.workbench"]')).toBeVisible({ timeout: 15000 });
    await expect(page.locator('[data-action="home.layout.edit"]')).toBeVisible();
    await capture(page, testInfo, '01-home-current-default.png');

    const salesHome = `${prefix}-销售`;
    await createHomePage(page, salesHome);
    const firstSetDefault = page.locator('[data-action="home.set-default"]');
    const defaultIndicator = page.locator(
      '[data-field="home.current-page"] [data-state="active"] [data-field="home.default-indicator"]',
    );
    if (!(await defaultIndicator.isVisible())) {
      await expect(firstSetDefault).toBeEnabled();
      await Promise.all([
        page.waitForResponse(response =>
          response.url().includes('/api/home/pages/default') && response.request().method() === 'PUT'
        ),
        firstSetDefault.click(),
      ]);
    }
    await expect(defaultIndicator).toBeVisible();
    const afterSalesDefault = await api<HomePage>(page, token, '/home/pages/resolve');
    expect(afterSalesDefault.name).toBe(salesHome);
    await capture(page, testInfo, '02-home-created-default.png');

    const projectHome = `${prefix}-项目`;
    await createHomePage(page, projectHome);
    await page.locator('[data-action="home.set-default"]').click();
    await page.goto('/#/home');
    await expectActiveHomeTab(page, projectHome);
    const resolvedDefault = await api<HomePage>(page, token, '/home/pages/resolve');
    expect(resolvedDefault.name).toBe(projectHome);
    await capture(page, testInfo, '03-home-resolved-default.png');

    const renamedHome = `${prefix}-项目看板`;
    await page.locator('[data-action="home.rename"]').click();
    const dialog = page.locator('[data-surface="home.name-form"]');
    await expect(dialog).toBeVisible();
    await page.locator('.el-dialog input.el-input__inner').fill(renamedHome);
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/home/pages/name') && response.request().method() === 'PUT'),
      page.locator('[data-action="home.name.submit"]').click(),
    ]);
    await expectActiveHomeTab(page, renamedHome);

    const projectPage = (await listHomePages(page, token)).find(item => item.name === renamedHome);
    expect(projectPage?.id).toBeTruthy();

    await page.locator('[data-action="home.duplicate"]').click();
    await expectActiveHomeTab(page, `${renamedHome} 副本`);
    const duplicatePage = (await listHomePages(page, token)).find(item => item.name === `${renamedHome} 副本`);
    expect(duplicatePage?.id).toBeTruthy();

    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/home/pages/sort') && response.request().method() === 'PUT'),
      page.locator('[data-action="home.sort-up"]').click(),
    ]);
    const sortedPages = await listHomePages(page, token);
    const duplicateIndex = sortedPages.findIndex(item => String(item.id) === String(duplicatePage?.id));
    const projectIndex = sortedPages.findIndex(item => String(item.id) === String(projectPage?.id));
    expect(duplicateIndex).toBeLessThan(projectIndex);
    await capture(page, testInfo, '04-home-duplicate-sorted.png');

    await page.goto(`/#/home/${projectPage?.id}`);
    await expect(page).toHaveURL(new RegExp(`#\\/home\\/${projectPage?.id}$`), { timeout: 10000 });
    await expectActiveHomeTab(page, renamedHome);

    await page.locator('[data-action="home.layout.edit"]').click();
    await expect(page.locator('[data-surface="grid-designer.widget-library"]')).toBeVisible();
    await capture(page, testInfo, '05-home-layout-editing.png');
    await Promise.all([
      page.waitForResponse(response => response.url().includes('/api/home/pages/layout') && response.request().method() === 'PUT'),
      page.locator('[data-action="home.layout.save"]').click(),
    ]);
    await expect(page.locator('[data-action="home.layout.edit"]')).toBeVisible({ timeout: 10000 });

    await cleanupHomePages(page, token, prefix);
  });
});
