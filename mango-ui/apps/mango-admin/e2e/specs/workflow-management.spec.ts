import { expect, test, type APIRequestContext, type Page } from '@playwright/test';

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

async function loginPage(page: Page, tenant: LoginTenant) {
  await page.goto('/#/login');
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: new RegExp(tenant.tenantName) }).click();
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  await page.click('button:has-text("登 录")');
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

async function cleanupWorkflow(request: APIRequestContext, token: string, keyword: string) {
  const headers = { Authorization: `Bearer ${token}` };
  const definitionsResponse = await request.get(`http://localhost:5555/workflow/definitions/page?page=1&size=100&keyword=${keyword}`, {
    headers,
  });
  if (definitionsResponse.status() === 200) {
    const definitionsBody = await definitionsResponse.json();
    for (const definition of definitionsBody.data?.list || []) {
      await request.put('http://localhost:5555/workflow/definitions/status', {
        headers,
        data: { id: definition.id, status: 'DISABLED' },
      });
      await request.delete(`http://localhost:5555/workflow/definitions?id=${definition.id}`, {
        headers,
      });
    }
  }

  const groupsResponse = await request.get(`http://localhost:5555/workflow/groups/page?page=1&size=100&keyword=${keyword}`, {
    headers,
  });
  if (groupsResponse.status() === 200) {
    const groupsBody = await groupsResponse.json();
    for (const group of groupsBody.data?.list || []) {
      await request.delete(`http://localhost:5555/workflow/groups?id=${group.id}`, {
        headers,
      });
    }
  }
}

function designerJson(unique: number) {
  return JSON.stringify({
    id: 'startEvent',
    nodeName: '发起人',
    nodeType: 'ROOT',
    childNode: {
      id: `risk_review_${unique}`,
      nodeName: '元丰行风控初审',
      nodeType: 'GUARANTEE_RISK_REVIEW',
      childNode: {
        id: `guarantor_approve_${unique}`,
        nodeName: '担保机构审批',
        nodeType: 'GUARANTEE_GUARANTOR_APPROVE',
        childNode: null,
        conditionNodes: [],
        properties: {},
      },
      conditionNodes: [],
      properties: {},
    },
    conditionNodes: [],
    properties: {},
  });
}

async function expectNoAuthError(page: Page) {
  await expect(page.locator('.el-message--error')).toHaveCount(0);
}

test.describe('工作流配置真实接口闭环', () => {
  test('创建流程进入独立三步设计工作台', async ({ page, request }) => {
    const unique = Date.now();
    const keyword = `e2e_workflow_ui_${unique}`;
    const groupName = `E2E设计分组${unique}`;
    const groupCode = keyword;
    const definitionName = `E2E设计流程${unique}`;
    const definitionKey = `e2e_ui_process_${unique}`;
    const token = await loginToken(request, platformTenant);
    const headers = { Authorization: `Bearer ${token}` };

    try {
      await cleanupWorkflow(request, token, keyword);

      const createGroupResponse = await request.post('http://localhost:5555/workflow/groups', {
        headers,
        data: {
          groupName,
          groupCode,
          sort: 98,
          status: 1,
          remark: 'E2E设计工作台验证数据',
        },
      });
      expect(createGroupResponse.status()).toBe(200);

      await loginPage(page, platformTenant);
      await page.goto('/#/system/workflow');
      await expect(page.getByText('流程定义').first()).toBeVisible({ timeout: 10000 });
      await page.getByRole('button', { name: '创建流程' }).click();

      await expect(page.getByRole('navigation', { name: '工作流设计步骤' })).toBeVisible();
      await expect(page.getByRole('button', { name: /基础信息/ })).toBeVisible();
      await expect(page.getByRole('button', { name: /表单信息/ })).toBeVisible();
      await expect(page.getByRole('button', { name: /流程设计/ })).toBeVisible();

      await page.locator('.builder-form .el-select').first().click();
      await page.getByRole('option', { name: groupName }).click();
      await page.getByPlaceholder('请输入流程名称').fill(definitionName);
      await page.getByPlaceholder('如 guarantee_approve').fill(definitionKey);
      await page.getByRole('button', { name: '下一步' }).click();

      await expect(page.locator('.form-mode-card', { hasText: '动态表单' })).toBeVisible();
      await expect(page.locator('.form-mode-card.active', { hasText: '动态表单' })).toBeVisible();
      await expect(page.locator('.form-mode-card', { hasText: '自定义表单' })).toBeVisible();
      await page.getByPlaceholder('如 guarantee_apply_form').fill(`form_${keyword}`);
      await expect(page.locator('.workflow-form-designer')).toBeVisible();
      await expect(page.locator('.workflow-form-designer')).toContainText('流程金额');
      await expect(page.locator('.workflow-form-designer')).toContainText('流程类型');
      await page.getByRole('button', { name: '下一步' }).click();

      await expect(page.getByText('流程设计器')).toBeVisible();
      await expect(page.getByText('发起人')).toBeVisible();
      await expect(page.locator('.node-canvas').getByText('人工审批', { exact: true })).toBeVisible();
      await page.locator('.node-canvas').getByText('人工审批', { exact: true }).click();
      await expect(page.locator('.node-config-section')).toContainText('办理人类型');
      await page.getByPlaceholder('${initiator}').fill('${applicant.id}');
      await expect(page.locator('.node-config-section')).toContainText('办理人表达式');
      await page.locator('.add-node-button').last().click();
      await page.getByRole('button', { name: /条件分支/ }).click();
      await expect(page.locator('.branch-card', { hasText: '分支1' })).toBeVisible();
      await page.locator('.branch-card', { hasText: '分支1' }).click();
      await page.locator('.condition-row').first().locator('.condition-variable').click();
      await expect(page.getByRole('option', { name: /当前机构ID（tenantId）/ })).toBeVisible();
      await expect(page.getByRole('option', { name: /申请人ID（applicant.id）/ })).toBeVisible();
      await page.getByRole('option', { name: /流程金额/ }).click();
      await page.locator('.condition-row').first().locator('.condition-operator').click();
      await page.getByRole('option', { name: '等于 ==' }).click();
      await page.locator('.condition-row').first().getByPlaceholder('比较值').fill('1000');
      await page.getByRole('button', { name: '添加条件' }).click();
      await page.locator('.condition-row').nth(1).locator('.el-select').first().click();
      await page.getByRole('option', { name: '或者 OR' }).click();
      await page.locator('.condition-row').nth(1).locator('.condition-variable').click();
      await page.getByRole('option', { name: /当前机构ID（tenantId）/ }).click();
      await page.locator('.condition-row').nth(1).getByPlaceholder('比较值').fill('1');
      await page.locator('.condition-row').nth(1).getByPlaceholder('比较值').blur();
      await expect(page.getByPlaceholder('${amount > 100000 && tenantId == 1}')).toHaveValue('${amount == 1000 || tenantId == 1}');
      await page.getByRole('button', { name: '发布流程' }).last().click();
      await expect(page.getByRole('dialog', { name: '发布前检查' })).toBeVisible();
      await expect(page.getByText('检查通过')).toBeVisible({ timeout: 10000 });
      await expectNoAuthError(page);
    } finally {
      await cleanupWorkflow(request, token, keyword).catch(() => undefined);
    }
  });

  test('平台管理员可维护流程分组、流程定义并发布到 Flowable', async ({ page, request }) => {
    const unique = Date.now();
    const keyword = `e2e_workflow_${unique}`;
    const groupName = `E2E流程分组${unique}`;
    const groupCode = keyword;
    const definitionName = `E2E流程${unique}`;
    const definitionKey = `e2e_process_${unique}`;
    const token = await loginToken(request, platformTenant);
    const headers = { Authorization: `Bearer ${token}` };

    try {
      await cleanupWorkflow(request, token, keyword);

      const createGroupResponse = await request.post('http://localhost:5555/workflow/groups', {
        headers,
        data: {
          groupName,
          groupCode,
          sort: 99,
          status: 1,
          remark: 'E2E验证数据',
        },
      });
      expect(createGroupResponse.status()).toBe(200);
      const createGroupBody = await createGroupResponse.json();
      expect(createGroupBody.success || createGroupBody.code === 200).toBeTruthy();
      const groupId = createGroupBody.data;

      const createDefinitionResponse = await request.post('http://localhost:5555/workflow/definitions', {
        headers,
        data: {
          groupId,
          definitionName,
          definitionKey,
          designerJson: designerJson(unique),
          formCode: `form_${keyword}`,
          formJson: JSON.stringify([
            {
              type: 'inputNumber',
              field: 'amount',
              title: '流程金额',
              props: {
                placeholder: '请输入流程金额',
              },
              validate: [
                { required: true, message: '流程金额不能为空', trigger: 'change' },
              ],
            },
          ]),
          status: 'DRAFT',
          remark: 'E2E验证数据',
        },
      });
      expect(createDefinitionResponse.status()).toBe(200);
      const createDefinitionBody = await createDefinitionResponse.json();
      expect(createDefinitionBody.success || createDefinitionBody.code === 200).toBeTruthy();
      const definitionId = createDefinitionBody.data;

      const deployResponse = await request.post(`http://localhost:5555/workflow/definitions/deploy?id=${definitionId}`, {
        headers,
      });
      expect(deployResponse.status()).toBe(200);
      const deployBody = await deployResponse.json();
      expect(deployBody.success || deployBody.code === 200).toBeTruthy();
      expect(deployBody.data.deploymentId).toBeTruthy();
      expect(deployBody.data.processDefinitionId).toBeTruthy();
      expect(deployBody.data.versionNo).toBe(1);

      const versionsResponse = await request.get(`http://localhost:5555/workflow/definitions/versions?definitionId=${definitionId}`, {
        headers,
      });
      expect(versionsResponse.status()).toBe(200);
      const versionsBody = await versionsResponse.json();
      expect(versionsBody.success || versionsBody.code === 200).toBeTruthy();
      expect(versionsBody.data?.[0]?.versionNo).toBe(1);
      expect(versionsBody.data?.[0]?.bpmnXml).toContain('<process');
      expect(versionsBody.data?.[0]?.formJson).toContain('流程金额');

      await loginPage(page, platformTenant);
      const pageResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/definitions/page') && response.status() === 200
      );
      await page.goto('/#/system/workflow');
      await pageResponsePromise;
      await expect(page.getByText('流程定义').first()).toBeVisible({ timeout: 10000 });
      await expect(page.getByRole('button', { name: '创建流程' })).toBeVisible();
      await expect(page.getByRole('button', { name: '创建分组' })).toBeVisible();
      await page.locator('.query-form .el-form-item', { hasText: '流程分组' }).locator('.el-select').click();
      await page.getByRole('option', { name: groupName }).click();
      await page.getByPlaceholder('流程名称/编码').fill(definitionName);
      await page.getByRole('button', { name: '查询' }).first().click();
      await expect(page.locator('.el-table__row', { hasText: definitionName })).toBeVisible({ timeout: 10000 });
      await expect(page.locator('.el-table__row', { hasText: definitionKey })).toContainText('已发布');
      await expect(page.locator('.el-table__row', { hasText: definitionKey })).toContainText('1');
      await expectNoAuthError(page);
    } finally {
      await cleanupWorkflow(request, token, keyword).catch(() => undefined);
    }
  });
});
