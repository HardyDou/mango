import { expect, type Page, type Response } from '@playwright/test';

export type LoginInstitution = {
  tenantId: string;
  tenantCode: string;
  tenantName: string;
};

type LoginOptions = {
  username?: string;
  password?: string;
  tenantCode?: string;
};

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function successfulData(body: unknown, operation: string): unknown {
  // Do not include response bodies in errors: login responses contain credentials.
  if (!isRecord(body) || body.code !== 200 || body.success !== true) {
    throw new Error(`${operation}未成功`);
  }
  return body.data;
}

function nonEmptyString(value: unknown): value is string {
  return typeof value === 'string' && value.trim().length > 0;
}

export function resolveLoginInstitution(body: unknown, tenantCode: string) {
  const data = successfulData(body, '查询账号可登录机构');
  if (!Array.isArray(data) || data.length === 0) {
    throw new Error('账号没有可登录机构');
  }
  const institutions: LoginInstitution[] = data.map((item: unknown) => {
    if (
      !isRecord(item) ||
      !nonEmptyString(item.tenantId) ||
      !nonEmptyString(item.tenantCode) ||
      !nonEmptyString(item.tenantName)
    ) {
      throw new Error('可登录机构数据不完整');
    }
    return { tenantId: item.tenantId, tenantCode: item.tenantCode, tenantName: item.tenantName };
  });
  const matches = institutions.filter((institution) => institution.tenantCode === tenantCode);
  if (matches.length !== 1) {
    throw new Error(`目标机构 ${tenantCode} 必须唯一且属于当前账号`);
  }
  if (new Set(institutions.map((institution) => institution.tenantId)).size !== institutions.length) {
    throw new Error('可登录机构 ID 重复');
  }
  return { institution: matches[0], multiple: institutions.length > 1 };
}

export function assertLoginOutcome(body: unknown, institution: LoginInstitution): void {
  const data = successfulData(body, '登录');
  if (!isRecord(data) || data.passwordResetRequired === true || !nonEmptyString(data.accessToken || data.token)) {
    throw new Error('登录未建立有效会话，或账号需要先修改密码');
  }
  if (String(data.tenantId) !== institution.tenantId || data.tenantCode !== institution.tenantCode) {
    throw new Error('登录返回的机构与目标机构不一致');
  }
}

function isPostResponse(response: Response, path: string): boolean {
  return response.request().method() === 'POST' && new URL(response.url()).pathname === `/api${path}`;
}

export async function login(page: Page, options: LoginOptions = {}): Promise<LoginInstitution> {
  const { username = 'admin', password = 'admin123', tenantCode = 'default' } = options;
  await page.goto('/#/login');
  await page.getByPlaceholder('请输入用户名', { exact: true }).fill(username);
  await page.getByPlaceholder('请输入密码', { exact: true }).fill(password);
  const [institutionsResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        isPostResponse(response, '/auth/login-institutions') &&
        response.request().postDataJSON()?.username === username,
      { timeout: 10000 },
    ),
    page.getByPlaceholder('请输入密码', { exact: true }).blur(),
  ]);
  expect(institutionsResponse.status(), '查询账号可登录机构 HTTP 状态').toBe(200);
  const { institution, multiple } = resolveLoginInstitution(await institutionsResponse.json(), tenantCode);
  const institutionSelect = page.getByRole('combobox');
  if (multiple) {
    await expect(institutionSelect, '多机构账号应显示机构选择器').toBeVisible();
    // Keyboard opening avoids Element Plus's selected-label overlay without forced clicks.
    await institutionSelect.press('ArrowDown');
    await page
      .getByRole('option')
      .filter({ has: page.getByText(institution.tenantCode, { exact: true }) })
      .click();
  } else {
    await expect(institutionSelect, '单机构账号应自动选择机构，不显示下拉框').toHaveCount(0);
  }

  const [loginResponse] = await Promise.all([
    page.waitForResponse((response) => isPostResponse(response, '/auth/login'), { timeout: 10000 }),
    page.getByRole('button', { name: /^登\s*录$/ }).click(),
  ]);
  const requestData = loginResponse.request().postDataJSON();
  expect({ tenantId: requestData.tenantId, tenantCode: requestData.tenantCode }).toEqual({
    tenantId: institution.tenantId,
    tenantCode,
  });
  expect(loginResponse.status(), '登录 HTTP 状态').toBe(200);
  assertLoginOutcome(await loginResponse.json(), institution);
  await page.waitForURL('**/#/home', { timeout: 10000 });
  await expect(page.getByRole('main')).toBeVisible();
  await expect.poll(() => page.evaluate(() => sessionStorage.getItem('tenantId'))).toBe(institution.tenantId);
  await expect.poll(() => page.evaluate(() => Boolean(sessionStorage.getItem('MANGO_TOKEN')))).toBe(true);
  return institution;
}
