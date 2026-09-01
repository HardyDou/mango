import { expect, test, type APIRequestContext, type APIResponse, type Page } from '@playwright/test';
import { api as e2eApi } from '../support/api';
import { collectBrowserDiagnostics } from '../support/browser-diagnostics';
import {
  chooseElementPlusOption,
  openUnlabelledElementPlusCombobox,
  setElementPlusCheckbox,
} from '../support/element-plus';

interface ApiEnvelope<T> {
  code?: number;
  data: T;
  msg?: string;
  success?: boolean;
}

interface OrgNode {
  id: string;
  orgName: string;
  orgCode: string;
}

interface UserRecord {
  userId: string;
  username: string;
}

interface RoleRecord {
  roleCode: string;
  roleName: string;
  status: number;
}

interface LoginTenant {
  tenantId: string;
  tenantCode: string;
  tenantName: string;
}

const authHeaders = (token: string) => ({ Authorization: `Bearer ${token}` });

async function responseData<T>(response: APIResponse): Promise<T> {
  expect(response.ok()).toBeTruthy();
  const body = (await response.json()) as ApiEnvelope<T>;
  expect(body.success === true || body.code === 200, body.msg).toBeTruthy();
  return body.data;
}

async function adminLoginTenant(request: APIRequestContext) {
  const response = await request.post(e2eApi('/auth/login-institutions'), {
    data: {
      username: 'admin',
      realm: 'INTERNAL',
      appCode: 'internal-admin',
    },
  });
  const tenants = await responseData<LoginTenant[]>(response);
  const tenant = tenants.find((item) => item.tenantCode === 'company_a');
  expect(tenant, 'demo 数据缺少 company_a').toBeDefined();
  return tenant!;
}

async function loginAsTenant(page: Page, tenant: LoginTenant) {
  await page.goto('/#/login');
  await openUnlabelledElementPlusCombobox(page);
  await page.getByRole('option', { name: new RegExp(tenant.tenantName) }).click();
  await page.getByPlaceholder('用户名').fill('admin');
  await page.getByPlaceholder('密码').fill('admin123');
  await page.getByRole('button', { name: /登\s*录/ }).click();
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

async function loginToken(request: APIRequestContext, tenant: LoginTenant) {
  const response = await request.post(e2eApi('/auth/login'), {
    data: {
      username: 'admin',
      password: 'admin123',
      tenantId: tenant.tenantId,
      tenantCode: tenant.tenantCode,
      realm: 'INTERNAL',
      actorType: 'INTERNAL_USER',
      partyType: 'INTERNAL_ORG',
      appCode: 'internal-admin',
    },
  });
  return (await responseData<{ accessToken: string }>(response)).accessToken;
}

async function rootOrganization(request: APIRequestContext, token: string) {
  const response = await request.get(e2eApi('/org/tree?parentId=0'), {
    headers: authHeaders(token),
  });
  const roots = await responseData<OrgNode[]>(response);
  expect(roots).toHaveLength(1);
  return roots[0];
}

async function createOrganization(
  request: APIRequestContext,
  token: string,
  rootId: string,
  orgName: string,
  orgCode: string,
) {
  const response = await request.post(e2eApi('/org'), {
    headers: authHeaders(token),
    data: {
      pid: rootId,
      orgName,
      orgCode,
      orgType: 3,
      orgSort: 10,
      orgStatus: '1',
    },
  });
  return responseData<string>(response);
}

async function createMemberAccount(
  request: APIRequestContext,
  token: string,
  orgId: string,
  username: string,
  nickname: string,
  email: string,
) {
  const response = await request.post(e2eApi('/org/member-accounts'), {
    headers: authHeaders(token),
    data: {
      orgId,
      username,
      password: 'Mango@123456',
      nickname,
      email,
      phone: '13900000002',
      status: 1,
      primaryFlag: true,
      leaderFlag: false,
    },
  });
  await responseData<string>(response);
}

async function cleanupUser(request: APIRequestContext, token: string, username: string) {
  const response = await request.get(
    e2eApi(`/identity/users/page?page=1&size=20&username=${encodeURIComponent(username)}`),
    { headers: authHeaders(token) },
  );
  if (!response.ok()) return;
  const body = (await response.json()) as ApiEnvelope<{ records?: UserRecord[]; list?: UserRecord[] }>;
  const records = body.data?.records || body.data?.list || [];
  for (const user of records.filter((item) => item.username === username)) {
    await request.delete(e2eApi(`/identity/users?userId=${user.userId}`), {
      headers: authHeaders(token),
    });
  }
}

async function cleanupOrganization(request: APIRequestContext, token: string, orgId?: string) {
  if (!orgId) return;
  await request.delete(e2eApi(`/org?id=${orgId}`), { headers: authHeaders(token) });
}

async function enabledRole(request: APIRequestContext, token: string) {
  const response = await request.get(e2eApi('/authorization/roles'), {
    headers: authHeaders(token),
  });
  const roles = await responseData<RoleRecord[]>(response);
  const role = roles.find((item) => item.status === 1);
  expect(role).toBeDefined();
  return role!;
}

function identityPageResponse(page: Page, predicate?: (url: URL) => boolean) {
  return page.waitForResponse((response) => {
    const url = new URL(response.url());
    return (
      url.pathname.endsWith('/api/identity/users/page') && response.status() === 200 && (!predicate || predicate(url))
    );
  });
}

test.describe('用户管理优化 @user-management', () => {
  test('@p0 管理员按组织维护成员并查看直接角色', async ({ page, request }, testInfo) => {
    test.setTimeout(60_000);
    const unique = Date.now();
    const targetOrgName = `E2E目标部门${unique}`;
    const sourceOrgName = `E2E来源部门${unique}`;
    const targetOrgCode = `E2E_TARGET_${unique}`;
    const sourceOrgCode = `E2E_SOURCE_${unique}`;
    const username = `E2E_USER_${unique}`;
    const nickname = `E2E用户${unique}`;
    const candidateUsername = `E2E_CANDIDATE_${unique}`;
    const candidateNickname = `E2E候选${unique}`;
    const candidateEmail = `candidate-${unique}@example.com`;
    const tenant = await adminLoginTenant(request);
    const token = await loginToken(request, tenant);
    let targetOrgId: string | undefined;
    let sourceOrgId: string | undefined;

    try {
      const root = await rootOrganization(request, token);
      const role = await enabledRole(request, token);
      targetOrgId = await createOrganization(request, token, root.id, targetOrgName, targetOrgCode);
      sourceOrgId = await createOrganization(request, token, root.id, sourceOrgName, sourceOrgCode);
      await createMemberAccount(request, token, sourceOrgId, candidateUsername, candidateNickname, candidateEmail);

      await loginAsTenant(page, tenant);
      const diagnostics = collectBrowserDiagnostics(page);
      let roleBatchRequests = 0;
      page.on('request', (requestEvent) => {
        if (new URL(requestEvent.url()).pathname.endsWith('/api/authorization/roles/subjects/batch')) {
          roleBatchRequests += 1;
        }
      });

      const initialPageResponse = identityPageResponse(page);
      await page.goto('/#/system/user');
      await initialPageResponse;
      const userPage = page.locator('[data-page="user.management"]');
      await expect(userPage).toBeVisible();
      await expect(userPage.locator('[data-record-key="org:__all_members__"]')).toHaveAttribute(
        'data-state',
        'selected',
      );
      await expect(userPage.locator('[data-field="user.scope.name"]')).toHaveText('全部成员');
      await expect(userPage.locator('[data-record-key="user:admin"]')).toBeVisible();
      const searchForm = userPage.locator('.search-form');

      const rootScopeResponse = page.waitForResponse(
        (response) =>
          response.url().includes('/api/org/member-scope') &&
          response.url().includes(`orgId=${encodeURIComponent(root.id)}`) &&
          response.status() === 200,
      );
      await userPage.locator(`[data-record-key="org:${root.id}"]`).click();
      await rootScopeResponse;
      await searchForm.getByLabel('用户名', { exact: true }).fill(candidateUsername);
      const roleBatchBeforeHierarchySearch = roleBatchRequests;
      const hierarchyPageResponse = identityPageResponse(
        page,
        (url) => url.searchParams.get('username') === candidateUsername,
      );
      await userPage.getByRole('button', { name: '查询', exact: true }).click();
      await hierarchyPageResponse;
      await expect(userPage.locator(`[data-record-key="user:${candidateUsername}"]`)).toBeVisible();
      await expect.poll(() => roleBatchRequests - roleBatchBeforeHierarchySearch).toBeGreaterThanOrEqual(1);

      const targetScopeResponse = page.waitForResponse(
        (response) =>
          response.url().includes('/api/org/member-scope') &&
          response.url().includes(`orgId=${encodeURIComponent(targetOrgId!)}`) &&
          response.status() === 200,
      );
      await userPage.locator(`[data-record-key="org:${targetOrgId}"]`).click();
      await targetScopeResponse;
      await userPage.getByRole('button', { name: '重置', exact: true }).click();

      await userPage.locator('[data-action="user.create"]').click();
      const createDialog = page.getByRole('dialog', { name: '新增成员' });
      await expect(createDialog).toBeVisible();
      await expect(createDialog.getByLabel('登录域', { exact: true })).toHaveCount(0);
      await expect(createDialog.getByLabel('操作者类型', { exact: true })).toHaveCount(0);
      await expect(createDialog.getByLabel('归属主体类型', { exact: true })).toHaveCount(0);
      await expect(createDialog.getByLabel('归属主体ID', { exact: true })).toHaveCount(0);
      await expect(createDialog.getByLabel('所属机构', { exact: true })).toHaveValue(tenant.tenantName);
      await expect(createDialog.locator('[data-field="user.org"]')).toContainText(targetOrgName);
      await expect(createDialog.locator('[data-field="user.org.path"]')).toContainText(
        `${tenant.tenantName} / ${targetOrgName}`,
      );
      await createDialog.getByLabel('用户名', { exact: true }).fill(username);
      await createDialog.getByLabel('初始密码', { exact: true }).fill(`E2E@${unique}`);
      await createDialog.getByLabel('姓名', { exact: true }).fill(nickname);
      await createDialog.getByLabel('手机号', { exact: true }).fill('13900000001');
      await createDialog.getByLabel('邮箱', { exact: true }).fill(`${username.toLowerCase()}@example.com`);

      const createResponse = page.waitForResponse(
        (response) =>
          response.url().includes('/api/org/member-accounts') &&
          response.request().method() === 'POST' &&
          response.status() === 200,
      );
      await createDialog.getByRole('button', { name: '确定', exact: true }).click();
      await createResponse;
      await expect(page.getByText('新增成功', { exact: true })).toBeVisible();

      await searchForm.getByLabel('用户名', { exact: true }).fill(username);
      const roleBatchBeforeUserSearch = roleBatchRequests;
      const userSearchResponse = identityPageResponse(page, (url) => url.searchParams.get('username') === username);
      await userPage.getByRole('button', { name: '查询', exact: true }).click();
      await userSearchResponse;
      await expect(userPage.locator(`[data-record-key="user:${username}"]`)).toBeVisible();
      await expect.poll(() => roleBatchRequests - roleBatchBeforeUserSearch).toBeGreaterThanOrEqual(1);

      await userPage
        .locator(`[data-record-key="user-actions:${username}"]`)
        .getByRole('button', { name: '分配角色', exact: true })
        .click();
      const assignDialog = page.getByRole('dialog', { name: '分配成员角色' });
      const roleLabel = `${role.roleName}（${role.roleCode}）`;
      await setElementPlusCheckbox(assignDialog, roleLabel, true);
      const assignResponse = page.waitForResponse(
        (response) =>
          new URL(response.url()).pathname.endsWith('/api/authorization/roles/subjects') &&
          response.request().method() === 'POST' &&
          response.status() === 200,
      );
      await assignDialog.getByRole('button', { name: '确定', exact: true }).click();
      await assignResponse;
      const assignedRoleCell = userPage.locator(`[data-record-key="user-roles:${username}"]`);
      await expect(assignedRoleCell).toContainText(role.roleName);
      await expect(userPage.locator('.user-table')).toHaveAttribute('data-state', 'ready');
      await expect(userPage.locator('.user-table .el-loading-mask')).toBeHidden();
      await userPage
        .locator('.user-table .el-table__body-wrapper .el-scrollbar__wrap')
        .evaluate((element) => element.scrollTo({ left: 500 }));
      await expect(assignedRoleCell).toBeVisible();
      await testInfo.attach('member-role-assignment', {
        body: await page.screenshot({ fullPage: true }),
        contentType: 'image/png',
      });

      const initialCandidatesResponse = identityPageResponse(
        page,
        (url) => url.searchParams.get('excludeOrgId') === targetOrgId,
      );
      await userPage.locator('[data-action="user.org.add-existing"]').click();
      await initialCandidatesResponse;
      const existingMemberDialog = page.getByRole('dialog', { name: '添加已有成员' });
      await expect(page.getByRole('option', { name: new RegExp(username) })).toHaveCount(0);
      const candidateCombobox = existingMemberDialog.getByRole('combobox').first();
      await existingMemberDialog.locator('.el-select__wrapper').first().click();
      const candidateSearchResponse = identityPageResponse(
        page,
        (url) =>
          url.searchParams.get('excludeOrgId') === targetOrgId && url.searchParams.get('keyword') === candidateEmail,
      );
      await candidateCombobox.fill(candidateEmail);
      await candidateSearchResponse;
      await chooseElementPlusOption(
        page.getByRole('option', { name: `${candidateNickname}（${candidateUsername}）`, exact: true }),
      );
      const addExistingResponse = page.waitForResponse(
        (response) =>
          new URL(response.url()).pathname.endsWith('/api/org/members') &&
          response.request().method() === 'POST' &&
          response.status() === 200,
      );
      await existingMemberDialog.getByRole('button', { name: '确定', exact: true }).click();
      await addExistingResponse;
      await expect(page.getByText('加入成功', { exact: true })).toBeVisible();

      await searchForm.getByLabel('用户名', { exact: true }).fill(candidateUsername);
      const candidatePageResponse = identityPageResponse(
        page,
        (url) => url.searchParams.get('username') === candidateUsername,
      );
      await userPage.getByRole('button', { name: '查询', exact: true }).click();
      await candidatePageResponse;
      await expect(userPage.locator(`[data-record-key="user:${candidateUsername}"]`)).toBeVisible();

      const allMembersResponse = identityPageResponse(page, (url) => !url.searchParams.has('orgIds'));
      await userPage.locator('[data-action="user.org.clear"]').click();
      await allMembersResponse;
      await expect(userPage.locator('[data-field="user.scope.name"]')).toHaveText('全部成员');
      await expect(userPage.locator('[data-record-key="org:__all_members__"]')).toHaveAttribute(
        'data-state',
        'selected',
      );
      await testInfo.attach('all-members-after-clear-org', {
        body: await page.screenshot({ fullPage: true }),
        contentType: 'image/png',
      });
      expect(diagnostics, diagnostics.join('\n')).toEqual([]);
    } finally {
      await cleanupUser(request, token, username);
      await cleanupUser(request, token, candidateUsername);
      await cleanupOrganization(request, token, targetOrgId);
      await cleanupOrganization(request, token, sourceOrgId);
    }
  });

  test('@p1 成员列表失败时显示可重试错误状态', async ({ page, request }, testInfo) => {
    const tenant = await adminLoginTenant(request);
    await loginAsTenant(page, tenant);
    await page.route('**/api/identity/users/page**', async (route) => {
      await route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ code: 500, success: false, msg: 'E2E injected failure' }),
      });
    });

    await page.goto('/#/system/user');
    const userPage = page.locator('[data-page="user.management"]');
    await expect(userPage.getByText('成员列表加载失败', { exact: true })).toBeVisible();
    await expect(userPage.getByRole('button', { name: '重试', exact: true })).toBeVisible();
    await expect(userPage.locator('[data-state="error"]')).toBeVisible();
    await testInfo.attach('retryable-list-error', {
      body: await page.screenshot({ fullPage: true }),
      contentType: 'image/png',
    });
  });
});
