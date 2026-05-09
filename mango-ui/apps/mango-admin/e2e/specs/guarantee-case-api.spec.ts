import { expect, test, type APIRequestContext } from '@playwright/test';

type LoginTenant = {
  tenantId: string;
  tenantCode: string;
  tenantName: string;
};

const platformTenant: LoginTenant = {
  tenantId: '1',
  tenantCode: 'default',
  tenantName: '芒果集团',
};

const companyATenant: LoginTenant = {
  tenantId: '2',
  tenantCode: 'company_a',
  tenantName: 'A公司',
};

async function loginToken(request: APIRequestContext, tenant: LoginTenant) {
  const response = await request.post('http://localhost:5555/auth/login', {
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
  expect(response.ok()).toBeTruthy();
  const body = await response.json();
  expect(body.success || body.code === 200).toBeTruthy();
  return body.data.accessToken as string;
}

async function postJson(request: APIRequestContext, token: string, path: string, data: unknown) {
  const response = await request.post(`http://localhost:5555${path}`, {
    headers: { Authorization: `Bearer ${token}` },
    data,
  });
  return { response, body: await response.json() };
}

async function getJson(request: APIRequestContext, token: string, path: string) {
  const response = await request.get(`http://localhost:5555${path}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return { response, body: await response.json() };
}

async function putJson(request: APIRequestContext, token: string, path: string, data: unknown) {
  const response = await request.put(`http://localhost:5555${path}`, {
    headers: { Authorization: `Bearer ${token}` },
    data,
  });
  return { response, body: await response.json() };
}

async function deleteJson(request: APIRequestContext, token: string, path: string) {
  const response = await request.delete(`http://localhost:5555${path}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return { response, body: await response.json() };
}

test.describe('G2 保函业务单 API 闭环', () => {
  test('来源机构创建、查询、修改、删除业务单，非参与机构不可见', async ({ request }) => {
    const platformToken = await loginToken(request, platformTenant);
    const companyAToken = await loginToken(request, companyATenant);
    const unique = Date.now();
    const title = `E2E保函业务单${unique}`;
    const updatedTitle = `${title}-编辑`;
    let caseId: string | undefined;

    try {
      const create = await postJson(request, platformToken, '/guarantee/cases', {
        title,
        applicantName: 'E2E申请人',
        beneficiaryName: 'E2E受益人',
        guaranteeType: 'BID',
        amount: 100000.5,
        currency: 'CNY',
        expectedIssueDate: '2026-06-01',
        remark: 'G2 API E2E',
      });
      expect(create.response.status()).toBe(200);
      expect(create.body.success || create.body.code === 200).toBeTruthy();
      expect(create.body.data).toBeTruthy();
      caseId = String(create.body.data);

      const platformDetail = await getJson(request, platformToken, `/guarantee/cases/detail?caseId=${caseId}`);
      expect(platformDetail.response.status()).toBe(200);
      expect(platformDetail.body.success || platformDetail.body.code === 200).toBeTruthy();
      expect(platformDetail.body.data).toMatchObject({
        caseId,
        title,
        sourceTenantId: platformTenant.tenantId,
        sourceTenantName: platformTenant.tenantName,
        applicantName: 'E2E申请人',
        guaranteeType: 'BID',
        currency: 'CNY',
        status: 0,
      });

      const platformList = await getJson(
        request,
        platformToken,
        `/guarantee/cases?title=${encodeURIComponent(title)}&page=1&size=10`
      );
      expect(platformList.response.status()).toBe(200);
      expect(platformList.body.success || platformList.body.code === 200).toBeTruthy();
      expect(platformList.body.data.records.some((item: any) => String(item.caseId) === caseId)).toBeTruthy();

      const companyDetail = await getJson(request, companyAToken, `/guarantee/cases/detail?caseId=${caseId}`);
      expect(companyDetail.response.status()).toBe(200);
      expect(companyDetail.body.success).toBeFalsy();
      expect(companyDetail.body.code).toBe(3404);

      const update = await putJson(request, platformToken, '/guarantee/cases', {
        caseId,
        title: updatedTitle,
        applicantName: 'E2E申请人-编辑',
        beneficiaryName: 'E2E受益人',
        guaranteeType: 'PERFORMANCE',
        amount: 200000,
        currency: 'CNY',
        expectedIssueDate: '2026-07-01',
        status: 1,
        remark: 'G2 API E2E updated',
      });
      expect(update.response.status()).toBe(200);
      expect(update.body.success || update.body.code === 200).toBeTruthy();

      const updatedDetail = await getJson(request, platformToken, `/guarantee/cases/detail?caseId=${caseId}`);
      expect(updatedDetail.body.data).toMatchObject({
        caseId,
        title: updatedTitle,
        applicantName: 'E2E申请人-编辑',
        guaranteeType: 'PERFORMANCE',
        status: 1,
      });

      const deleteResult = await deleteJson(request, platformToken, `/guarantee/cases?caseId=${caseId}`);
      expect(deleteResult.response.status()).toBe(200);
      expect(deleteResult.body.success || deleteResult.body.code === 200).toBeTruthy();

      const deletedDetail = await getJson(request, platformToken, `/guarantee/cases/detail?caseId=${caseId}`);
      expect(deletedDetail.response.status()).toBe(200);
      expect(deletedDetail.body.success).toBeFalsy();
      expect(deletedDetail.body.code).toBe(3404);
      caseId = undefined;
    } finally {
      if (caseId) {
        await deleteJson(request, platformToken, `/guarantee/cases?caseId=${caseId}`).catch(() => undefined);
      }
    }
  });
});
