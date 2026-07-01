import { expect, request as playwrightRequest, test, type APIRequestContext, type Page } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { existsSync, readFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';

test.setTimeout(60 * 1000);

type MenuNode = {
  menuName?: string;
  children?: MenuNode[];
};

type WorkspaceDbConfig = {
  host: string;
  port: string;
  database: string;
  username: string;
  password: string;
};

async function login(page: Page) {
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

  await page.goto('/#/home');
  await expect(page).toHaveURL(/#\/home$/, { timeout: 15000 });
}

async function loginByApi(request: APIRequestContext) {
  const response = await request.post('/api/auth/login', {
    data: {
      username: 'admin',
      password: 'admin123',
      tenantId: '1',
      tenantCode: 'default',
      realm: 'INTERNAL',
      actorType: 'INTERNAL_USER',
      partyType: 'INTERNAL_ORG',
      appCode: 'internal-admin',
    },
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.success || body.code === 200).toBeTruthy();
  expect(body.data?.accessToken).toBeTruthy();
  return body.data.accessToken as string;
}

async function expectApiData<T>(response: Awaited<ReturnType<APIRequestContext['get']>>) {
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.success || body.code === 200).toBeTruthy();
  return body.data as T;
}

function readWorkspaceDbConfig(): WorkspaceDbConfig {
  const values: Record<string, string> = {};
  let current = resolve(process.cwd());
  while (current !== dirname(current)) {
    const envPath = join(current, '.mango', 'dev-workspace.env');
    if (existsSync(envPath)) {
      for (const line of readFileSync(envPath, 'utf8').split(/\r?\n/)) {
        const match = line.match(/^([A-Z0-9_]+)=(.*)$/);
        if (match) {
          values[match[1]] = match[2].trim().replace(/^['"]|['"]$/g, '');
        }
      }
      break;
    }
    current = dirname(current);
  }
  return {
    host: process.env.MANGO_DB_HOST || values.MANGO_DB_HOST || '127.0.0.1',
    port: process.env.MANGO_DB_PORT || values.MANGO_DB_PORT || '3306',
    database: process.env.MANGO_DB_NAME || values.MANGO_DB_NAME || 'mango',
    username: process.env.MANGO_DB_USERNAME || values.MANGO_DB_USERNAME || 'root',
    password: process.env.MANGO_DB_PASSWORD || values.MANGO_DB_PASSWORD || '',
  };
}

function mysqlScalar(sql: string) {
  const db = readWorkspaceDbConfig();
  const args = [
    '--protocol=TCP',
    '-h',
    db.host,
    '-P',
    db.port,
    '-u',
    db.username,
    '--batch',
    '--skip-column-names',
    db.database,
    '-e',
    sql,
  ];
  return execFileSync('mysql', args, {
    env: {
      ...process.env,
      ...(db.password ? { MYSQL_PWD: db.password } : {}),
    },
    stdio: 'pipe',
    encoding: 'utf8',
  }).trim();
}

function collectMenuNames(menus: MenuNode[]): string[] {
  return menus.flatMap((menu) => [
    menu.menuName,
    ...collectMenuNames(menu.children || []),
  ]).filter(Boolean) as string[];
}

async function waitForLinkAuthorizationReady(request: APIRequestContext, authHeaders: Record<string, string>) {
  await expect.poll(async () => {
    const response = await request.get('/api/authorization/menus/user?fmt=tree', { headers: authHeaders });
    if (response.status() !== 200) {
      return false;
    }
    const body = await response.json();
    return collectMenuNames(body.data || []).includes('网址导航');
  }, {
    timeout: 20000,
    intervals: [500, 1000, 2000],
  }).toBe(true);
}

async function waitForLinkMenuReady(page: Page) {
  await expect.poll(async () => page.evaluate(async () => {
    const response = await fetch('/api/authorization/menus/user?fmt=tree');
    if (!response.ok) {
      return false;
    }
    const body = await response.json();
    const names: string[] = [];
    const walk = (menus: MenuNode[]) => {
      for (const menu of menus || []) {
        if (menu.menuName) {
          names.push(menu.menuName);
        }
        walk(menu.children || []);
      }
    };
    walk(body.data || []);
    return names.includes('网址导航');
  }), {
    timeout: 20000,
    intervals: [500, 1000, 2000],
  }).toBe(true);
}

test.describe('网址导航菜单 E2E', () => {
  test('@p0 @mango-link 匿名访问边界正确', async ({ request }) => {
    const publicResponse = await request.get('/api/link/open/public-links/list?tenantId=1');
    expect(publicResponse.status()).toBe(200);
    await expect(publicResponse).toBeOK();
    const publicBody = await publicResponse.json();
    expect(publicBody.code).toBe(200);
    expect(publicBody.success).toBe(true);
    expect(Array.isArray(publicBody.data)).toBe(true);

    const companyResponse = await request.get('/api/link/company-links/list?tenantId=1');
    expect(companyResponse.status()).toBe(401);

    const favoriteResponse = await request.get('/api/link/favorites/list?tenantId=1');
    expect(favoriteResponse.status()).toBe(401);

    const adminResponse = await request.get('/api/link/items/page?page=1&size=10');
    expect(adminResponse.status()).toBe(401);
  });

  test('@p0 @mango-link 公开接口登录后返回当前用户完整导航数据', async ({ request, baseURL }, testInfo) => {
    const token = await loginByApi(request);
    const authHeaders = { Authorization: `Bearer ${token}` };
    await waitForLinkAuthorizationReady(request, authHeaders);
    const unique = `E2E_LINK_NAV_${testInfo.project.name}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
    const tag = `e2e-${testInfo.project.name}`;
    let categoryId: string | undefined;
    let companyLinkId: string | undefined;
    let personalLinkId: string | undefined;

    try {
      categoryId = await expectApiData<string>(await request.post('/api/link/categories/create', {
        headers: authHeaders,
        data: {
          name: unique,
          sortNo: 0,
          remark: unique,
        },
      }));

      companyLinkId = await expectApiData<string>(await request.post('/api/link/items/create', {
        headers: authHeaders,
        data: {
          name: `${unique}_COMPANY`,
          url: `https://example.com/${unique.toLowerCase()}/company`,
          categoryId,
          summary: unique,
          tags: [tag],
          visibilityScope: 'COMPANY',
          recommended: true,
          sortNo: 0,
        },
      }));

      personalLinkId = await expectApiData<string>(await request.post('/api/link/personal-links/create', {
        headers: authHeaders,
        data: {
          name: `${unique}_PERSONAL`,
          url: `https://example.com/${unique.toLowerCase()}/personal`,
          categoryId,
          summary: unique,
          tags: [tag],
        },
      }));

      await expectApiData<boolean>(await request.post('/api/link/favorites/create', {
        headers: authHeaders,
        data: { linkId: companyLinkId },
      }));

      const anonymousContext = await playwrightRequest.newContext({ baseURL });
      try {
        const anonymousLinks = await expectApiData<Array<{ name?: string }>>(
          await anonymousContext.get(`/api/link/open/public-links/list?tenantId=1&keyword=${encodeURIComponent(unique)}`),
        );
        expect(anonymousLinks).toHaveLength(0);
      } finally {
        await anonymousContext.dispose();
      }

      const visibleLinks = await expectApiData<Array<{
        id?: string;
        name?: string;
        source?: string;
        favorited?: boolean;
        redirectUrl?: string;
        url?: string;
      }>>(
        await request.get(`/api/link/open/public-links/list?keyword=${encodeURIComponent(unique)}`, {
          headers: authHeaders,
        }),
      );
      expect(visibleLinks).toEqual(expect.arrayContaining([
        expect.objectContaining({ id: String(companyLinkId), source: 'COMPANY', favorited: true }),
        expect.objectContaining({ id: String(companyLinkId), source: 'FAVORITE', favorited: true }),
        expect.objectContaining({ id: String(personalLinkId), source: 'PERSONAL' }),
      ]));

      const companyEntry = visibleLinks.find((item) => item.id === String(companyLinkId) && item.source === 'COMPANY');
      expect(companyEntry?.redirectUrl).toBe(`/link/open/jump?url=${encodeURIComponent(companyEntry?.url || '')}&source=COMPANY`);
      const beforeAccessCount = Number(mysqlScalar(
        `SELECT COUNT(*) FROM link_access_record WHERE link_id = ${companyLinkId} AND source = 'COMPANY'`,
      ));
      const redirectResponse = await request.get(`/api${companyEntry?.redirectUrl}`, {
        headers: authHeaders,
        maxRedirects: 0,
      });
      expect(redirectResponse.status()).toBe(302);
      expect(redirectResponse.headers().location).toBe(companyEntry?.url);
      const afterAccessCount = Number(mysqlScalar(
        `SELECT COUNT(*) FROM link_access_record WHERE link_id = ${companyLinkId} AND source = 'COMPANY'`,
      ));
      expect(afterAccessCount).toBe(beforeAccessCount + 1);
    } finally {
      if (companyLinkId) {
        await request.delete(`/api/link/favorites/delete`, {
          headers: authHeaders,
          data: { linkId: companyLinkId },
        });
        await request.delete(`/api/link/items/delete?id=${companyLinkId}`, { headers: authHeaders });
      }
      if (personalLinkId) {
        await request.delete(`/api/link/personal-links/delete?id=${personalLinkId}`, { headers: authHeaders });
      }
      if (categoryId) {
        await request.delete(`/api/link/categories/delete?id=${categoryId}`, { headers: authHeaders });
      }
    }
  });

  test('@p0 @mango-link 管理员可看到网址导航菜单并打开网址列表页面', async ({ page }) => {
    const menuResponsePromise = page.waitForResponse((response) => {
      const url = response.url();
      return response.status() === 200
        && url.includes('/api/authorization/menus/user')
        && url.includes('fmt=tree');
    });

    await login(page);
    await waitForLinkMenuReady(page);

    await menuResponsePromise;
    const menuBody = await (await page.request.get('/api/authorization/menus/user?fmt=tree')).json();
    const menuNames = collectMenuNames(menuBody.data || []);
    expect(menuNames).toContain('网址导航');
    expect(menuNames).toContain('公司网址');
    expect(menuNames).toContain('我的收藏');
    expect(menuNames).toContain('我的网址');
    expect(menuNames).toContain('网址管理');
    expect(menuNames).toContain('网址分类');
    expect(menuNames).toContain('网址列表');

    await page.goto('/#/data/link/items');
    await expect(page.locator('[data-page="link-items"]')).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('heading', { name: '网址列表' })).toBeVisible();
    await expect(page.getByRole('button', { name: '新增' })).toBeVisible();
    await expect(page.getByPlaceholder('名称/地址/标签')).toBeVisible();

    await page.goto('/#/link/company');
    await expect(page.locator('[data-page="link-company"]')).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('heading', { name: '公司网址' })).toBeVisible();
  });
});
