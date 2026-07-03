import { expect, test, type Page, type TestInfo } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

type ApiBody<T> = {
  code?: number | string;
  success?: boolean;
  message?: string;
  data?: T;
};

type LoginData = {
  accessToken: string;
  refreshToken?: string;
  expiresIn?: number;
  tenantId?: string;
  tenantCode?: string;
  tenantName?: string;
  userId?: string;
  partyId?: string;
};

type HomeTemplate = {
  id?: string | number;
  name: string;
  enabled?: boolean;
  activeVersionId?: string | number;
  activeVersionNo?: number;
  activeLayoutJson?: string;
  draftVersionId?: string | number;
  draftLayoutJson?: string;
  authorizationCount?: number;
};

type HomePage = {
  id?: string | number;
  routeKey?: string;
  templateId?: string | number;
  templateVersionId?: string | number;
  name: string;
  layoutJson?: string;
  defaultPage?: boolean;
  sourceType?: string;
  sourceLabel?: string;
  sourceLabels?: string[];
  readOnly?: boolean;
  canCopy?: boolean;
};

type PageResult<T> = {
  list: T[];
  total: number;
  page: number;
  size: number;
};

type AuthorizationItem = {
  subjectType: 'USER' | 'ORG' | 'ROLE';
  subjectId?: string | number;
  subjectCode?: string;
  subjectName?: string;
  defaultFlag?: boolean;
  sort?: number;
};

const EVIDENCE_DIR = resolve(
  __dirname,
  '../../../../../mango-docs/evidence/2026-07-03-issue-372-home-management/e2e',
);

function layoutJson(title: string): string {
  return JSON.stringify({
    schemaVersion: 1,
    items: [
      {
        id: 'welcome',
        widgetType: 'system.quick-entry',
        title,
        layout: { x: 0, y: 0, w: 6, h: 18 },
        props: { title },
      },
    ],
  });
}

async function login(page: Page): Promise<LoginData> {
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

  return loginData;
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

async function apiFailure(page: Page, token: string, path: string, options: RequestInit = {}): Promise<ApiBody<unknown>> {
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
      if (response.ok && (body.success || body.code === 200 || body.code === '200')) {
        throw new Error(`接口预期失败但成功：${path} ${JSON.stringify(body)}`);
      }
      return body;
    },
    { path, options, token },
  );
}

async function capture(page: Page, testInfo: TestInfo, name: string) {
  mkdirSync(EVIDENCE_DIR, { recursive: true });
  const path = resolve(EVIDENCE_DIR, name);
  await expect(page.locator('.el-loading-mask:visible')).toHaveCount(0, { timeout: 10000 });
  await page.waitForLoadState('networkidle').catch(() => undefined);
  await page.screenshot({ path, fullPage: true });
  await testInfo.attach(name, { path, contentType: 'image/png' });
}

async function cleanupTemplates(page: Page, token: string, prefix: string) {
  const templates = await api<HomeTemplate[]>(page, token, `/home/templates?keyword=${encodeURIComponent(prefix)}`);
  for (const template of templates) {
    if (!template.id || !template.name.startsWith(prefix)) {
      continue;
    }
    await api(page, token, '/home/templates/authorizations', {
      method: 'PUT',
      body: JSON.stringify({ templateId: template.id, authorizations: [] }),
    }).catch(() => undefined);
    await api(page, token, '/home/templates', {
      method: 'DELETE',
      body: JSON.stringify({ id: template.id }),
    }).catch(() => undefined);
  }
}

async function listHomePages(page: Page, token: string): Promise<HomePage[]> {
  return api<HomePage[]>(page, token, '/home/pages');
}

async function cleanupHomePages(page: Page, token: string, prefix: string) {
  const pages = await listHomePages(page, token);
  for (const item of pages) {
    if (item.id && item.name?.startsWith(prefix)) {
      await api<HomePage>(page, token, '/home/pages', {
        method: 'DELETE',
        body: JSON.stringify({ id: item.id }),
      }).catch(() => undefined);
    }
  }
}

async function createPublishedTemplate(page: Page, token: string, name: string, marker: string): Promise<HomeTemplate> {
  const created = await api<HomeTemplate>(page, token, '/home/templates', {
    method: 'POST',
    body: JSON.stringify({ name, layoutJson: layoutJson(marker) }),
  });
  return api<HomeTemplate>(page, token, '/home/templates/publish', {
    method: 'PUT',
    body: JSON.stringify({ id: created.id }),
  });
}

async function saveTemplateAuthorizations(
  page: Page,
  token: string,
  templateId: string | number | undefined,
  authorizations: AuthorizationItem[],
) {
  expect(templateId).toBeTruthy();
  return api(page, token, '/home/templates/authorizations', {
    method: 'PUT',
    body: JSON.stringify({ templateId, authorizations }),
  });
}

async function resolveUserPages(
  page: Page,
  token: string,
  query: { userId: string | number; memberId?: string | number; orgId?: string | number },
): Promise<HomePage[]> {
  const params = new URLSearchParams({ userId: String(query.userId) });
  if (query.memberId) params.set('memberId', String(query.memberId));
  if (query.orgId) params.set('orgId', String(query.orgId));
  return api<HomePage[]>(page, token, `/home/templates/user-pages?${params.toString()}`);
}

function routeOf(template: HomeTemplate): string {
  expect(template.id).toBeTruthy();
  return `template:${template.id}`;
}

function defaultPageOf(pages: HomePage[]): HomePage | undefined {
  return pages.find(item => item.defaultPage);
}

test.describe('首页管理 E2E', () => {
  test.setTimeout(90 * 1000);

  test('平台模板发布、授权、默认首页解析和页面管理可用', async ({ page }, testInfo) => {
    const prefix = `E2E首页管理${Date.now()}`;
    const originalName = `${prefix}-模板`;
    const copyName = `${prefix}-模板副本`;
    const token = (await login(page)).accessToken;
    const userId = '1';
    const memberId = '1001';
    const orgId = '1';

    await cleanupTemplates(page, token, prefix);

    const created = await api<HomeTemplate>(page, token, '/home/templates', {
      method: 'POST',
      body: JSON.stringify({ name: originalName, layoutJson: layoutJson('draft-v1') }),
    });
    expect(created.name).toBe(originalName);
    expect(created.draftLayoutJson).toContain('draft-v1');

    const published = await api<HomeTemplate>(page, token, '/home/templates/publish', {
      method: 'PUT',
      body: JSON.stringify({ id: created.id }),
    });
    expect(published.activeVersionNo).toBe(1);
    expect(published.activeLayoutJson).toContain('draft-v1');

    const immutableResult = await apiFailure(page, token, '/home/templates/draft', {
      method: 'PUT',
      body: JSON.stringify({
        id: created.id,
        name: `${originalName}-非法修改`,
        layoutJson: layoutJson('should-not-save'),
      }),
    });
    expect(JSON.stringify(immutableResult)).toMatch(/草稿|不存在|失败|错误/);

    const copied = await api<HomeTemplate>(page, token, '/home/templates/copy', {
      method: 'POST',
      body: JSON.stringify({ id: created.id }),
    });
    expect(copied.name).toContain('副本');

    const updatedCopy = await api<HomeTemplate>(page, token, '/home/templates/draft', {
      method: 'PUT',
      body: JSON.stringify({
        id: copied.id,
        name: copyName,
        layoutJson: layoutJson('copy-draft-v2'),
      }),
    });
    expect(updatedCopy.name).toBe(copyName);
    expect(updatedCopy.draftLayoutJson).toContain('copy-draft-v2');

    const publishedCopy = await api<HomeTemplate>(page, token, '/home/templates/publish', {
      method: 'PUT',
      body: JSON.stringify({ id: copied.id }),
    });
    expect(publishedCopy.activeLayoutJson).toContain('copy-draft-v2');

    const authorizations = await api(page, token, '/home/templates/authorizations', {
      method: 'PUT',
      body: JSON.stringify({
        templateId: copied.id,
        authorizations: [
          {
            subjectType: 'USER',
            subjectId: Number(userId),
            subjectName: 'admin',
            defaultFlag: true,
            sort: 10,
          },
          {
            subjectType: 'ORG',
            subjectId: Number(orgId),
            subjectName: '芒果集团',
            defaultFlag: false,
            sort: 20,
          },
        ],
      }),
    });
    expect(Array.isArray(authorizations)).toBe(true);

    const userPages = await api<HomePage[]>(
      page,
      token,
      `/home/templates/user-pages?userId=${userId}&memberId=${memberId}&orgId=${orgId}`,
    );
    const authorized = userPages.find(item => item.routeKey === `template:${copied.id}`);
    expect(authorized).toBeTruthy();
    expect(authorized?.readOnly).toBe(true);
    expect(authorized?.canCopy).toBe(true);
    expect(authorized?.sourceLabels?.join(',') || authorized?.sourceLabel || '').toMatch(/个人|部门|admin|芒果集团/);

    const defaultPage = await api<HomePage>(page, token, '/home/pages/default', {
      method: 'PUT',
      body: JSON.stringify({ homeId: `template:${copied.id}` }),
    });
    expect(defaultPage.routeKey).toBe(`template:${copied.id}`);
    expect(defaultPage.defaultPage).toBe(true);

    const resolved = await api<HomePage>(page, token, '/home/pages/resolve');
    expect(resolved.routeKey).toBe(`template:${copied.id}`);
    expect(resolved.layoutJson).toContain('copy-draft-v2');
    expect(resolved.readOnly).toBe(true);

    const personalCopy = await api<HomePage>(page, token, '/home/pages', {
      method: 'POST',
      body: JSON.stringify({
        name: `${resolved.name} 副本`,
        layoutJson: resolved.layoutJson,
        setDefault: false,
      }),
    });
    expect(personalCopy.id).toBeTruthy();
    expect(personalCopy.templateId).toBeFalsy();
    expect(personalCopy.readOnly).not.toBe(true);

    await page.goto(`/#/home-management/templates?keyword=${encodeURIComponent(prefix)}`);
    await expect(page.locator('[data-page="home.template"]')).toBeVisible({ timeout: 15000 });
    await page.locator('[data-field="home.template.keyword"]').fill(prefix);
    await page.locator('[data-action="home.template.search"]').click();
    await expect(page.getByText(copyName).first()).toBeVisible({ timeout: 15000 });
    await capture(page, testInfo, '01-home-management-template-list.png');

    await page.goto('/#/home-management/user');
    await expect(page.locator('[data-page="home.user"]')).toBeVisible({ timeout: 15000 });
    await page.locator('[data-field="home.user.user-id"]').fill(userId);
    await page.locator('[data-field="home.user.member-id"]').fill(memberId);
    await page.locator('[data-field="home.user.org-id"]').fill(orgId);
    await page.locator('[data-action="home.user.view"]').click();
    await expect(page.locator(`[data-record-key="home-user-page:template:${copied.id}"]`)).toBeVisible({ timeout: 15000 });
    await expect(page.getByText(copyName).first()).toBeVisible();
    await expect(page.locator('[data-surface="home.user.tabs"]').getByText(personalCopy.name).first()).toBeVisible({ timeout: 15000 });
    await page.locator('[data-surface="home.user.tabs"]').getByText(personalCopy.name).first().click();
    await expect(page.locator('[data-surface="home.user.layout"]')).toBeVisible({ timeout: 15000 });
    await capture(page, testInfo, '02-home-user-view.png');

    await expect(page.locator('.el-message--error')).toHaveCount(0);
  });

  test('授权来源覆盖个人、部门继承、角色和默认优先级', async ({ page }) => {
    const prefix = `E2E首页授权矩阵${Date.now()}`;
    const token = (await login(page)).accessToken;
    const syntheticUserId = `900${Date.now()}`.slice(0, 12);
    const memberId = '1001';
    const rootOrgId = '1';
    const childOrgId = '2';

    await cleanupTemplates(page, token, 'E2E首页授权矩阵');
    await cleanupTemplates(page, token, prefix);

    const roleTemplate = await createPublishedTemplate(page, token, `${prefix}-角色默认`, 'role-default');
    await saveTemplateAuthorizations(page, token, roleTemplate.id, [
      {
        subjectType: 'ROLE',
        subjectCode: 'ROLE_ADMIN',
        subjectName: '超级管理员',
        defaultFlag: true,
        sort: 30,
      },
    ]);

    const roleOnlyPages = await resolveUserPages(page, token, {
      userId: Number(syntheticUserId) + 2,
      memberId,
      orgId: rootOrgId,
    });
    expect(roleOnlyPages.find(item => item.routeKey === routeOf(roleTemplate))).toBeTruthy();
    expect(defaultPageOf(roleOnlyPages)?.routeKey).toBe(routeOf(roleTemplate));

    const orgTemplate = await createPublishedTemplate(page, token, `${prefix}-部门默认`, 'org-default');
    await saveTemplateAuthorizations(page, token, orgTemplate.id, [
      {
        subjectType: 'ORG',
        subjectId: Number(rootOrgId),
        subjectName: '芒果集团',
        defaultFlag: true,
        sort: 20,
      },
    ]);

    const userTemplate = await createPublishedTemplate(page, token, `${prefix}-个人默认`, 'user-default');
    await saveTemplateAuthorizations(page, token, userTemplate.id, [
      {
        subjectType: 'USER',
        subjectId: Number(syntheticUserId),
        subjectName: `用户${syntheticUserId}`,
        defaultFlag: true,
        sort: 10,
      },
    ]);

    const pages = await resolveUserPages(page, token, {
      userId: syntheticUserId,
      memberId,
      orgId: rootOrgId,
    });
    expect(pages.map(item => item.routeKey)).toEqual(
      expect.arrayContaining([routeOf(userTemplate), routeOf(orgTemplate), routeOf(roleTemplate)]),
    );
    expect(defaultPageOf(pages)?.routeKey).toBe(routeOf(userTemplate));
    expect(pages.find(item => item.routeKey === routeOf(userTemplate))?.sourceType).toBe('PERSONAL_AUTH');
    expect(pages.find(item => item.routeKey === routeOf(orgTemplate))?.sourceType).toBe('ORG_AUTH');
    expect(pages.find(item => item.routeKey === routeOf(roleTemplate))?.sourceType).toBe('ROLE_AUTH');

    const inheritedPages = await resolveUserPages(page, token, {
      userId: Number(syntheticUserId) + 1,
      orgId: childOrgId,
    });
    const inheritedOrg = inheritedPages.find(item => item.routeKey === routeOf(orgTemplate));
    expect(inheritedOrg).toBeTruthy();
    expect(inheritedOrg?.sourceLabel || inheritedOrg?.sourceLabels?.join(',') || '').toMatch(/部门授权|芒果集团/);
    expect(defaultPageOf(inheritedPages)?.routeKey).toBe(routeOf(orgTemplate));

    const manualPrefix = `${prefix}-个人页`;
    await cleanupHomePages(page, token, manualPrefix);
    const personalPage = await api<HomePage>(page, token, '/home/pages', {
      method: 'POST',
      body: JSON.stringify({
        name: `${manualPrefix}-默认`,
        layoutJson: layoutJson('manual-default'),
        setDefault: true,
      }),
    });
    const resolvedManual = await api<HomePage>(page, token, '/home/pages/resolve');
    expect([`user:${personalPage.id}`, String(personalPage.id)]).toContain(String(resolvedManual.routeKey));
    expect(resolvedManual.layoutJson).toContain('manual-default');
  });

  test('模板停用后不可见，已授权模板禁止删除，清空授权后可删除', async ({ page }) => {
    const prefix = `E2E首页状态删除${Date.now()}`;
    const token = (await login(page)).accessToken;
    const userId = '1';

    await cleanupTemplates(page, token, 'E2E首页状态删除');
    await cleanupTemplates(page, token, prefix);

    const template = await createPublishedTemplate(page, token, `${prefix}-受控模板`, 'status-delete');
    await saveTemplateAuthorizations(page, token, template.id, [
      {
        subjectType: 'USER',
        subjectId: Number(userId),
        subjectName: 'admin',
        defaultFlag: true,
        sort: 10,
      },
    ]);

    let pages = await resolveUserPages(page, token, { userId });
    expect(pages.find(item => item.routeKey === routeOf(template))).toBeTruthy();

    const disabled = await api<HomeTemplate>(page, token, '/home/templates/status', {
      method: 'PUT',
      body: JSON.stringify({ id: template.id, enabled: false }),
    });
    expect(disabled.enabled).toBe(false);
    pages = await resolveUserPages(page, token, { userId });
    expect(pages.find(item => item.routeKey === routeOf(template))).toBeFalsy();

    const enabled = await api<HomeTemplate>(page, token, '/home/templates/status', {
      method: 'PUT',
      body: JSON.stringify({ id: template.id, enabled: true }),
    });
    expect(enabled.enabled).toBe(true);
    pages = await resolveUserPages(page, token, { userId });
    expect(pages.find(item => item.routeKey === routeOf(template))).toBeTruthy();

    const deleteWhileAuthorized = await apiFailure(page, token, '/home/templates', {
      method: 'DELETE',
      body: JSON.stringify({ id: template.id }),
    });
    expect(JSON.stringify(deleteWhileAuthorized)).toMatch(/授权|删除|失败|错误/);

    await saveTemplateAuthorizations(page, token, template.id, []);
    await api(page, token, '/home/templates', {
      method: 'DELETE',
      body: JSON.stringify({ id: template.id }),
    });
    const list = await api<HomeTemplate[]>(page, token, `/home/templates?keyword=${encodeURIComponent(prefix)}`);
    expect(list.find(item => item.id === template.id)).toBeFalsy();
  });

  test('后台页面支持新建模板、复制、授权和独立用户首页查询', async ({ page }, testInfo) => {
    const prefix = `E2E首页UI${Date.now()}`;
    const uiTemplateName = `${prefix}-页面新建`;
    const copiedTemplateName = `${uiTemplateName} 副本`;
    const token = (await login(page)).accessToken;

    await cleanupTemplates(page, token, 'E2E首页UI');
    await cleanupTemplates(page, token, prefix);

    await page.goto('/#/home-management/templates');
    await expect(page.locator('[data-page="home.template"]')).toBeVisible({ timeout: 15000 });

    await page.locator('[data-action="home.template.create"]').click();
    await expect(page.locator('[data-surface="home.template.form"]')).toBeVisible();
    await page.locator('[data-field="home.template.name"]').fill(uiTemplateName);
    const designer = page.locator('[data-surface="home.template.designer"]');
    await expect(designer).toBeVisible();
    await expect(designer.locator('[data-surface="grid-designer.widget-library"]')).toBeVisible();
    await expect(designer.locator('[data-action="grid-designer.widget.add"]').first()).toBeVisible({ timeout: 15000 });
    const createResponsePromise = page.waitForResponse(response =>
      response.url().includes('/api/home/templates')
      && response.request().method() === 'POST'
      && response.status() === 200,
    );
    await page.locator('[data-action="home.template.save"]').click();
    await createResponsePromise;

    await page.locator('[data-field="home.template.keyword"]').fill(prefix);
    await page.locator('[data-action="home.template.search"]').click();
    await expect(page.getByText(uiTemplateName).first()).toBeVisible({ timeout: 15000 });

    const templatesAfterCreate = await api<HomeTemplate[]>(page, token, `/home/templates?keyword=${encodeURIComponent(uiTemplateName)}`);
    const created = templatesAfterCreate.find(item => item.name === uiTemplateName);
    expect(created?.draftLayoutJson).toContain('system.quick-entry');
    expect(created?.id).toBeTruthy();

    await page.locator(`[data-action="home.template.copy"][data-record-key="home-template:${created?.id}"]`).click();
    await expect(page.getByText(copiedTemplateName).first()).toBeVisible({ timeout: 15000 });

    const copied = (await api<HomeTemplate[]>(page, token, `/home/templates?keyword=${encodeURIComponent(copiedTemplateName)}`))
      .find(item => item.name === copiedTemplateName);
    expect(copied?.id).toBeTruthy();

    await page.locator(`[data-action="home.template.more"][data-record-key="home-template:${copied?.id}"]`).click();
    const publishResponsePromise = page.waitForResponse(response =>
      response.url().includes('/api/home/templates/publish')
      && response.request().method() === 'PUT'
      && response.status() === 200,
    );
    await page.locator('[data-action="home.template.publish"]').last().click();
    await page.getByRole('button', { name: '确定' }).click();
    await publishResponsePromise;
    await expect(page.getByText(copiedTemplateName).first()).toBeVisible({ timeout: 15000 });

    await page.locator(`[data-action="home.template.more"][data-record-key="home-template:${copied?.id}"]`).click();
    await page.locator('[data-action="home.template.auth"]').last().click();
    await page.getByRole('button', { name: '添加个人' }).click();
    const authDialog = page.locator('.el-dialog', { hasText: '模板授权' }).last();
    await authDialog.getByPlaceholder('用户或部门 ID').last().fill('1');
    await authDialog.getByPlaceholder('授权来源展示名称').last().fill('admin');
    await authDialog.locator('[data-action="home.template.auth.save"]').click();
    await expect(authDialog).toBeHidden({ timeout: 15000 });

    await page.goto('/#/home-management/user');
    await expect(page.locator('[data-page="home.user"]')).toBeVisible({ timeout: 15000 });
    await page.locator('[data-field="home.user.user-id"]').fill('1');
    const userPagesResponsePromise = page.waitForResponse(response =>
      response.url().includes('/api/home/templates/user-pages') && response.status() === 200
    );
    await page.locator('[data-action="home.user.view"]').click();
    await userPagesResponsePromise;
    await expect(page.locator(`[data-record-key="home-user-page:template:${copied?.id}"]`)).toBeVisible({ timeout: 15000 });
    await expect(page.getByText(copiedTemplateName).first()).toBeVisible({ timeout: 15000 });
    await capture(page, testInfo, '03-home-management-ui-flow.png');

    await expect(page.locator('.el-message--error')).toHaveCount(0);
  });

  test('首页列表默认展示所有用户定义首页且不要求输入用户 ID', async ({ page }, testInfo) => {
    const prefix = `E2E首页列表${Date.now()}`;
    const token = (await login(page)).accessToken;

    await cleanupHomePages(page, token, 'E2E首页列表');
    const created = await api<HomePage>(page, token, '/home/pages', {
      method: 'POST',
      body: JSON.stringify({
        name: `${prefix}-用户定义页`,
        layoutJson: layoutJson('user-defined-list'),
        setDefault: false,
      }),
    });
    expect(created.id).toBeTruthy();

    const pageResult = await api<PageResult<HomePage>>(page, token, '/home/pages/user-pages?page=1&size=20');
    expect(pageResult.list.some(item => item.id === created.id && item.sourceType === 'USER')).toBe(true);
    expect(pageResult.list.every(item => item.sourceType === 'USER')).toBe(true);

    await page.goto('/#/home-management/list');
    await expect(page.locator('[data-page="home.list"]')).toBeVisible({ timeout: 15000 });
    await expect(page.getByText(`${prefix}-用户定义页`).first()).toBeVisible({ timeout: 15000 });
    await expect(page.getByText('用户定义').first()).toBeVisible();
    await capture(page, testInfo, '04-home-list-default-user-pages.png');

    await page.locator('[data-field="home.list.keyword"]').fill(prefix);
    await page.locator('[data-action="home.list.search"]').click();
    await expect(page.getByText(`${prefix}-用户定义页`).first()).toBeVisible({ timeout: 15000 });
    await page.locator('[data-action="home.list.reset"]').click();
    await expect(page.getByText(`${prefix}-用户定义页`).first()).toBeVisible({ timeout: 15000 });
  });
});
