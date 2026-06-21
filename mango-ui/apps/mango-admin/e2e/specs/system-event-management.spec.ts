import { expect, test, type APIRequestContext, type Page } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { existsSync, readFileSync } from 'node:fs';
import { mkdir } from 'node:fs/promises';
import { resolve } from 'node:path';
import { api as e2eApi } from '../support/api';

test.describe.configure({ mode: 'serial' });

type LoginData = {
  accessToken: string;
  refreshToken?: string;
  expiresIn?: number | string;
  userId?: string | number;
  memberId?: string | number;
  username?: string;
  nickname?: string;
  tenantId?: string | number;
  tenantCode?: string;
  tenantName?: string;
  appCode?: string;
  realm?: string;
  actorType?: string;
  partyType?: string;
  partyId?: string | number;
  roles?: string[];
  permissions?: string[];
};

type ApiResponse<T> = {
  code?: number;
  success?: boolean;
  data?: T;
  msg?: string;
  message?: string;
};

type PageResult<T> = {
  list?: T[];
  records?: T[];
  rows?: T[];
  data?: T[];
  total?: number | string;
};

type SystemEventRecord = {
  messageId?: string;
  status?: string;
  eventType?: string;
};

const evidenceDir = resolve(__dirname, '../../../../../mango-docs/evidence/2026-06-12-issue-137-domain-event-reliable');
const workspaceEnvPath = resolve(__dirname, '../../../../../.mango/dev-workspace.env');
const outboxPrefix = 'mango:kv:default:outbox';
const fixtureMessageId = 'e2e-system-event-issue-137';
const fixtureBusinessKey = 'E2E-ISSUE-137-SYSTEM-EVENT';
const fixtureEventType = 'e2e.system.event.failed';

type WorkspaceDbConfig = {
  host: string;
  port: string;
  database: string;
  username: string;
  password: string;
};

async function login(request: APIRequestContext): Promise<LoginData> {
  const response = await request.post(e2eApi('/auth/login'), {
    data: {
      username: 'admin',
      password: 'admin123',
      tenantId: 1,
      tenantCode: 'default',
      realm: 'INTERNAL',
      actorType: 'INTERNAL_USER',
      partyType: 'INTERNAL_ORG',
      appCode: 'internal-admin',
    },
  });
  expect(response.status()).toBe(200);
  const body = await response.json() as ApiResponse<LoginData>;
  expect(body.success || body.code === 200).toBeTruthy();
  expect(body.data?.accessToken).toBeTruthy();
  return body.data as LoginData;
}

async function installSession(page: Page, loginData: LoginData) {
  await page.goto('/#/login');
  await page.evaluate((data) => {
    sessionStorage.setItem('MANGO_TOKEN', data.accessToken);
    sessionStorage.setItem('MANGO_REFRESH_TOKEN', data.refreshToken || '');
    sessionStorage.setItem(
      'MANGO_TOKEN_EXPIRES_AT',
      String(Date.now() + Number(data.expiresIn || 7200) * 1000),
    );
    sessionStorage.setItem('tenantId', String(data.tenantId || 1));
    sessionStorage.setItem('userInfo', JSON.stringify({
      userId: data.userId,
      memberId: data.memberId,
      username: data.username,
      nickname: data.nickname,
      tenantId: data.tenantId,
      tenantCode: data.tenantCode,
      tenantName: data.tenantName,
      appCode: data.appCode,
      realm: data.realm,
      actorType: data.actorType,
      partyType: data.partyType,
      partyId: data.partyId,
      roles: data.roles || [],
      permissions: data.permissions || [],
    }));
    document.cookie = `MANGO_TOKEN=${encodeURIComponent(data.accessToken)}; path=/; SameSite=Lax`;
  }, loginData);
}

async function expectNoAuthOrNetworkError(page: Page) {
  await expect(page.locator('.el-message--error')).toHaveCount(0);
  await expect(page.locator('.el-alert--error, .el-result, .error-page, .app-error')).toHaveCount(0);
}

function recordsOf<T>(pageResult: PageResult<T> | undefined): T[] {
  return pageResult?.list ?? pageResult?.records ?? pageResult?.rows ?? pageResult?.data ?? [];
}

function readWorkspaceDbConfig(): WorkspaceDbConfig {
  const values: Record<string, string> = {};
  if (existsSync(workspaceEnvPath)) {
    for (const line of readFileSync(workspaceEnvPath, 'utf-8').split(/\r?\n/)) {
      const match = line.match(/^([A-Z0-9_]+)=(.*)$/);
      if (!match) continue;
      values[match[1]] = match[2].trim().replace(/^['"]|['"]$/g, '');
    }
  }
  return {
    host: process.env.MANGO_DB_HOST || values.MANGO_DB_HOST || '127.0.0.1',
    port: process.env.MANGO_DB_PORT || values.MANGO_DB_PORT || '3306',
    database: process.env.MANGO_DB_NAME || values.MANGO_DB_NAME || 'mango',
    username: process.env.MANGO_DB_USERNAME || values.MANGO_DB_USERNAME || 'root',
    password: process.env.MANGO_DB_PASSWORD || values.MANGO_DB_PASSWORD || '',
  };
}

function runMysql(sql: string) {
  const db = readWorkspaceDbConfig();
  const args = [
    '--protocol=TCP',
    '-h',
    db.host,
    '-P',
    db.port,
    '-u',
    db.username,
    db.database,
    '-e',
    sql,
  ];
  execFileSync('mysql', args, {
    env: {
      ...process.env,
      ...(db.password ? { MYSQL_PWD: db.password } : {}),
    },
    stdio: 'pipe',
  });
}

function cleanupSystemEventFixture() {
  runMysql([
    'DELETE FROM infra_kv_entry',
    `WHERE kv_key IN (${[
      `${outboxPrefix}:message:${fixtureMessageId}`,
      `${outboxPrefix}:all:member:${fixtureMessageId}`,
      `${outboxPrefix}:pending:member:${fixtureMessageId}`,
    ].map((key) => `'${key}'`).join(',')})`,
  ].join(' '));
}

function prepareSystemEventFixture() {
  cleanupSystemEventFixture();
  const occurredAt = new Date().toISOString();
  const expireTimeSql = 'DATE_ADD(NOW(), INTERVAL 7 DAY)';
  const message = JSON.stringify({
    messageId: fixtureMessageId,
    eventType: fixtureEventType,
    businessType: 'E2E_SYSTEM_EVENT',
    businessKey: fixtureBusinessKey,
    aggregateId: 'issue-137',
    occurredAt,
    status: 'FAILED',
    attemptCount: 3,
    nextAttemptAt: null,
    lockedAt: occurredAt,
    lockedBy: 'e2e-worker',
    errorMessage: 'E2E fixture consumer failure',
    payload: {
      source: 'playwright',
      issue: 137,
    },
    headers: {
      traceId: 'e2e-issue-137',
    },
  }).replace(/'/g, "''");
  const score = Date.parse(occurredAt);
  runMysql([
    'INSERT INTO infra_kv_entry (id, kv_key, kv_value, expire_time) VALUES',
    `(UNIX_TIMESTAMP(NOW(6)) * 1000000 + 1, '${outboxPrefix}:message:${fixtureMessageId}', '${message}', ${expireTimeSql}),`,
    `(UNIX_TIMESTAMP(NOW(6)) * 1000000 + 2, '${outboxPrefix}:all:member:${fixtureMessageId}', '${score}', ${expireTimeSql})`,
  ].join(' '));
}

test.beforeAll(() => {
  prepareSystemEventFixture();
});

test.afterAll(() => {
  cleanupSystemEventFixture();
});

test('系统维护/系统事件页面使用真实后端查询 Outbox 并保留验收截图', async ({ page, request }) => {
  await mkdir(evidenceDir, { recursive: true });
  const loginData = await login(request);

  const apiResponse = await request.get(e2eApi('/system/events?pageNum=1&pageSize=20&abnormalOnly=false'), {
    headers: { Authorization: `Bearer ${loginData.accessToken}` },
  });
  expect(apiResponse.status()).toBe(200);
  const apiBody = await apiResponse.json() as ApiResponse<PageResult<SystemEventRecord>>;
  expect(apiBody.success || apiBody.code === 200).toBeTruthy();
  const apiRecords = recordsOf(apiBody.data);
  expect(apiRecords.some((record) =>
    record.messageId === fixtureMessageId
    && record.status === 'FAILED'
    && record.eventType === fixtureEventType,
  )).toBeTruthy();

  await installSession(page, loginData);

  const menuResponsePromise = page.waitForResponse((response) => {
    const url = response.url();
    return response.status() === 200
      && url.includes('/api/authorization/menus/user')
      && url.includes('fmt=tree');
  });
  await page.goto('/#/home');
  const menuResponse = await menuResponsePromise;
  const menuBody = await menuResponse.json();
  const menuText = JSON.stringify(menuBody.data || []);
  expect(menuText).toContain('系统维护');
  expect(menuText).toContain('系统事件');
  expect(menuText).toContain('/system/events');

  const eventsResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/system/events')
      && response.request().method() === 'GET'
      && response.status() === 200,
  );
  await page.goto('/#/system/events');
  const eventsResponse = await eventsResponsePromise;
  const eventsBody = await eventsResponse.json() as ApiResponse<PageResult<SystemEventRecord>>;
  expect(eventsBody.success || eventsBody.code === 200).toBeTruthy();

  await expect(page.getByText('关键词')).toBeVisible();
  await expect(page.getByText('异常范围')).toBeVisible();
  await expect(page.getByRole('button', { name: '查询' })).toBeVisible();
  await expect(page.getByRole('button', { name: '重置' })).toBeVisible();
  await expect(page.getByRole('button', { name: '刷新' })).toBeVisible();
  await expect(page.locator('.el-table')).toBeVisible();
  await expect(page.getByText(fixtureBusinessKey)).toBeVisible();
  await expect(page.getByText(fixtureEventType)).toBeVisible();
  await expectNoAuthOrNetworkError(page);

  await page.screenshot({
    path: resolve(evidenceDir, 'system-event-list.png'),
    fullPage: true,
  });

  const detailResponse = await request.get(e2eApi(`/system/events/detail?messageId=${fixtureMessageId}`), {
    headers: { Authorization: `Bearer ${loginData.accessToken}` },
  });
  expect(detailResponse.status()).toBe(200);
  const detailBody = await detailResponse.json() as ApiResponse<SystemEventRecord>;
  expect(detailBody.success || detailBody.code === 200).toBeTruthy();
  expect(detailBody.data?.messageId).toBe(fixtureMessageId);

  const fixtureRow = page.locator('.el-table__row', { hasText: fixtureBusinessKey });
  await fixtureRow.getByRole('button', { name: '详情' }).click();
  await expect(page.getByRole('dialog', { name: '系统事件详情' })).toBeVisible();
  const detailDialog = page.getByRole('dialog', { name: '系统事件详情' });
  await expect(detailDialog.getByText('事件头')).toBeVisible();
  await expect(detailDialog.getByText('事件载荷')).toBeVisible();
  await expect(detailDialog.getByText('E2E fixture consumer failure')).toBeVisible();
  await page.screenshot({
    path: resolve(evidenceDir, 'system-event-detail.png'),
    fullPage: true,
  });

  const reconsumeResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/system/events/reconsume')
    && response.request().method() === 'POST'
    && response.status() === 200,
  );
  await detailDialog
    .getByRole('button', { name: '重新投递' })
    .click();
  await page.getByRole('button', { name: '确认重投' }).click();
  const reconsumeResponse = await reconsumeResponsePromise;
  const reconsumeBody = await reconsumeResponse.json() as ApiResponse<boolean>;
  expect(reconsumeBody.success || reconsumeBody.code === 200).toBeTruthy();
  await expect(page.getByText('已放回待投递队列')).toBeVisible();
});
