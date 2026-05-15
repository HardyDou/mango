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
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  const accountTenantsResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/auth/login-institutions') && response.status() === 200
  );
  await page.locator('input[placeholder="密码"]').blur();
  await accountTenantsResponsePromise;
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: new RegExp(tenant.tenantName) }).click();
  await page.locator('.login-btn').click();
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

async function openWorkflowManage(page: Page) {
  await page.getByRole('button', { name: '协同办公' }).click();
  await page.getByRole('menuitem', { name: '流程管理' }).click();
  await page.waitForURL('**/#/workflow/manage', { timeout: 10000 });
}

async function openStartProcess(page: Page) {
  await page.getByRole('button', { name: '协同办公' }).click();
  await page.getByRole('menuitem', { name: '发起流程' }).click();
  await page.waitForURL('**/#/workflow/start-process', { timeout: 10000 });
}

async function openInitiatedTasks(page: Page) {
  await page.getByRole('button', { name: '协同办公' }).click();
  await page.getByRole('menubar').getByText('任务管理', { exact: true }).click();
  await page.getByRole('menuitem', { name: '我的发起' }).click();
  await page.waitForURL('**/#/workflow/task/initiated', { timeout: 10000 });
}

async function openTodoTasks(page: Page) {
  await page.getByRole('button', { name: '协同办公' }).click();
  await page.getByRole('menubar').getByText('任务管理', { exact: true }).click();
  await page.getByRole('menuitem', { name: '我的待办' }).click();
  await page.waitForURL('**/#/workflow/task/todo', { timeout: 10000 });
}

async function openDoneTasks(page: Page) {
  await page.getByRole('button', { name: '协同办公' }).click();
  await page.getByRole('menubar').getByText('任务管理', { exact: true }).click();
  await page.getByRole('menuitem', { name: '我的已办' }).click();
  await page.waitForURL('**/#/workflow/task/done', { timeout: 10000 });
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

function approvalDesignerJson(unique: number) {
  return JSON.stringify({
    id: 'startEvent',
    nodeName: '发起人',
    nodeType: 'ROOT',
    childNode: {
      id: `leave_approve_${unique}`,
      nodeName: '主管审批',
      nodeType: 'APPROVAL',
      bpmnType: 'userTask',
      executionType: 'USER_TASK',
      childNode: null,
      conditionNodes: [],
      properties: {
        approvalConfig: {
          assigneeType: 'SPECIFIED_USER',
          assigneeIds: ['admin'],
          roleIds: [],
          postIds: [],
          orgIds: [],
          approvalMode: 'COUNTERSIGN',
          emptyAssigneeStrategy: 'TO_ADMIN',
          emptyAssigneeUserIds: [],
          rejectStrategy: 'END_PROCESS',
          formPermissions: {},
          eventNotify: {
            enabled: false,
            type: 'HTTP',
            method: 'POST',
            timeoutMillis: 5000,
          },
          initiatorSelectMultiple: false,
        },
      },
    },
    conditionNodes: [],
    properties: {},
  });
}

function initiatorSelectDesignerJson(unique: number) {
  return JSON.stringify({
    id: 'startEvent',
    nodeName: '发起人',
    nodeType: 'ROOT',
    childNode: {
      id: `starter_select_approve_${unique}`,
      nodeName: '发起人自选审批',
      nodeType: 'APPROVAL',
      bpmnType: 'userTask',
      executionType: 'USER_TASK',
      childNode: null,
      conditionNodes: [],
      properties: {
        approvalConfig: {
          assigneeType: 'INITIATOR_SELECT',
          assigneeIds: [],
          roleIds: [],
          postIds: [],
          orgIds: [],
          approvalMode: 'COUNTERSIGN',
          emptyAssigneeStrategy: 'TO_ADMIN',
          emptyAssigneeUserIds: [],
          rejectStrategy: 'END_PROCESS',
          formPermissions: {},
          eventNotify: {
            enabled: false,
            type: 'HTTP',
            method: 'POST',
            timeoutMillis: 5000,
          },
          initiatorSelectMultiple: false,
        },
      },
    },
    conditionNodes: [],
    properties: {},
  });
}

function initiatorSelfDesignerJson(unique: number) {
  return JSON.stringify({
    id: 'startEvent',
    nodeName: '发起人',
    nodeType: 'ROOT',
    childNode: {
      id: `initiator_approve_${unique}`,
      nodeName: '发起人自己审批',
      nodeType: 'APPROVAL',
      bpmnType: 'userTask',
      executionType: 'USER_TASK',
      childNode: null,
      conditionNodes: [],
      properties: {
        approvalConfig: {
          assigneeType: 'INITIATOR',
          assigneeIds: [],
          roleIds: [],
          postIds: [],
          orgIds: [],
          approvalMode: 'COUNTERSIGN',
          emptyAssigneeStrategy: 'TO_ADMIN',
          emptyAssigneeUserIds: [],
          rejectStrategy: 'END_PROCESS',
          formPermissions: {},
          eventNotify: {
            enabled: false,
            type: 'HTTP',
            method: 'POST',
            timeoutMillis: 5000,
          },
          initiatorSelectMultiple: false,
        },
      },
    },
    conditionNodes: [],
    properties: {},
  });
}

function leaveFormJson() {
  return JSON.stringify([
    {
      type: 'inputNumber',
      field: 'days',
      title: '请假天数',
      props: {
        placeholder: '请输入请假天数',
        min: 1,
      },
      validate: [
        { required: true, message: '请假天数不能为空', trigger: 'change' },
      ],
    },
    {
      type: 'textarea',
      field: 'reason',
      title: '请假原因',
      props: {
        placeholder: '请输入请假原因',
      },
      validate: [
        { required: true, message: '请假原因不能为空', trigger: 'blur' },
      ],
    },
  ]);
}

async function prepareLeaveWorkflow(request: APIRequestContext, token: string, unique: number, keyword: string) {
  const headers = { Authorization: `Bearer ${token}` };
  const createGroupResponse = await request.post('http://localhost:5555/workflow/groups', {
    headers,
    data: {
      groupName: `E2E审批分组${unique}`,
      groupCode: keyword,
      sort: 96,
      status: 1,
      remark: 'E2E审批闭环验证数据',
    },
  });
  expect(createGroupResponse.status()).toBe(200);
  const createGroupBody = await createGroupResponse.json();
  expect(createGroupBody.success || createGroupBody.code === 200).toBeTruthy();

  const createDefinitionResponse = await request.post('http://localhost:5555/workflow/definitions', {
    headers,
    data: {
      groupId: createGroupBody.data,
      definitionName: `E2E审批请假流程${unique}`,
      definitionKey: `e2e_approval_leave_${unique}`,
      designerJson: approvalDesignerJson(unique),
      formCode: `form_${keyword}`,
      formJson: leaveFormJson(),
      status: 'DRAFT',
      remark: 'E2E审批闭环验证数据',
    },
  });
  expect(createDefinitionResponse.status()).toBe(200);
  const createDefinitionBody = await createDefinitionResponse.json();
  expect(createDefinitionBody.success || createDefinitionBody.code === 200).toBeTruthy();

  const deployResponse = await request.post(`http://localhost:5555/workflow/definitions/deploy?id=${createDefinitionBody.data}`, {
    headers,
  });
  expect(deployResponse.status()).toBe(200);
  const deployBody = await deployResponse.json();
  expect(deployBody.success || deployBody.code === 200).toBeTruthy();

  return {
    definitionId: createDefinitionBody.data as string,
    definitionName: `E2E审批请假流程${unique}`,
    definitionKey: `e2e_approval_leave_${unique}`,
  };
}

async function prepareInitiatorSelectWorkflow(request: APIRequestContext, token: string, unique: number, keyword: string) {
  const headers = { Authorization: `Bearer ${token}` };
  const createGroupResponse = await request.post('http://localhost:5555/workflow/groups', {
    headers,
    data: {
      groupName: `E2E自选分组${unique}`,
      groupCode: keyword,
      sort: 95,
      status: 1,
      remark: 'E2E发起人自选验证数据',
    },
  });
  expect(createGroupResponse.status()).toBe(200);
  const createGroupBody = await createGroupResponse.json();
  expect(createGroupBody.success || createGroupBody.code === 200).toBeTruthy();

  const createDefinitionResponse = await request.post('http://localhost:5555/workflow/definitions', {
    headers,
    data: {
      groupId: createGroupBody.data,
      definitionName: `E2E发起人自选流程${unique}`,
      definitionKey: `e2e_initiator_select_${unique}`,
      designerJson: initiatorSelectDesignerJson(unique),
      formCode: `form_${keyword}`,
      formJson: leaveFormJson(),
      status: 'DRAFT',
      remark: 'E2E发起人自选验证数据',
    },
  });
  expect(createDefinitionResponse.status()).toBe(200);
  const createDefinitionBody = await createDefinitionResponse.json();
  expect(createDefinitionBody.success || createDefinitionBody.code === 200).toBeTruthy();

  const deployResponse = await request.post(`http://localhost:5555/workflow/definitions/deploy?id=${createDefinitionBody.data}`, {
    headers,
  });
  expect(deployResponse.status()).toBe(200);
  const deployBody = await deployResponse.json();
  expect(deployBody.success || deployBody.code === 200).toBeTruthy();

  return {
    definitionId: createDefinitionBody.data as string,
    definitionName: `E2E发起人自选流程${unique}`,
    definitionKey: `e2e_initiator_select_${unique}`,
  };
}

async function prepareInitiatorSelfWorkflow(request: APIRequestContext, token: string, unique: number, keyword: string) {
  const headers = { Authorization: `Bearer ${token}` };
  const createGroupResponse = await request.post('http://localhost:5555/workflow/groups', {
    headers,
    data: {
      groupName: `E2E自审分组${unique}`,
      groupCode: keyword,
      sort: 94,
      status: 1,
      remark: 'E2E发起人自己审批验证数据',
    },
  });
  expect(createGroupResponse.status()).toBe(200);
  const createGroupBody = await createGroupResponse.json();
  expect(createGroupBody.success || createGroupBody.code === 200).toBeTruthy();

  const createDefinitionResponse = await request.post('http://localhost:5555/workflow/definitions', {
    headers,
    data: {
      groupId: createGroupBody.data,
      definitionName: `E2E发起人自己审批流程${unique}`,
      definitionKey: `e2e_initiator_self_${unique}`,
      designerJson: initiatorSelfDesignerJson(unique),
      formCode: `form_${keyword}`,
      formJson: leaveFormJson(),
      status: 'DRAFT',
      remark: 'E2E发起人自己审批验证数据',
    },
  });
  expect(createDefinitionResponse.status()).toBe(200);
  const createDefinitionBody = await createDefinitionResponse.json();
  expect(createDefinitionBody.success || createDefinitionBody.code === 200).toBeTruthy();

  const deployResponse = await request.post(`http://localhost:5555/workflow/definitions/deploy?id=${createDefinitionBody.data}`, {
    headers,
  });
  expect(deployResponse.status()).toBe(200);
  const deployBody = await deployResponse.json();
  expect(deployBody.success || deployBody.code === 200).toBeTruthy();

  return {
    definitionId: createDefinitionBody.data as string,
    definitionName: `E2E发起人自己审批流程${unique}`,
    definitionKey: `e2e_initiator_self_${unique}`,
  };
}

async function startLeaveProcess(request: APIRequestContext, token: string, definitionId: string, businessKey: string, days: number, reason: string) {
  const response = await request.post('http://localhost:5555/workflow/processes/start', {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      definitionId,
      businessKey,
      variables: {
        days,
        reason,
        applicant: 'admin',
      },
    },
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.success || body.code === 200).toBeTruthy();
  expect(body.data.businessKey).toBe(businessKey);
  return body.data.processInstanceId as string;
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
      await openWorkflowManage(page);
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
      await expect(page.locator('.workflow-form-designer')).toContainText('辅助组件');
      await expect(page.locator('.workflow-form-designer')).toContainText('子表单组件');
      await expect(page.locator('.workflow-form-designer')).toContainText('基础组件');
      await expect(page.locator('.workflow-form-designer')).toContainText('业务组件');
      await expect(page.locator('.workflow-form-designer')).toContainText('人员');
      await expect(page.locator('.workflow-form-designer')).toContainText('部门');
      await expect(page.locator('.workflow-form-designer')).toContainText('签名');
      await expect(page.locator('.workflow-form-designer')).toContainText('流水号');
      await expect(page.locator('.workflow-form-designer')).toContainText('流程金额');
      await expect(page.locator('.workflow-form-designer')).toContainText('流程类型');
      await page.getByRole('button', { name: '下一步' }).click();

      await expect(page.getByText('流程设计器')).toBeVisible();
      await expect(page.getByText('发起人')).toBeVisible();
      const approvalNode = page.locator('.workflow-node-card:not(.root)', { hasText: '人工审批' });
      await expect(approvalNode).toBeVisible();
      await approvalNode.click();
      const approvalDrawer = page.locator('.workflow-node-drawer');
      await expect(approvalDrawer).toBeVisible();
      await expect(approvalDrawer).toContainText('审批人设置');
      await expect(approvalDrawer.getByRole('tab', { name: '设置审批人' })).toBeVisible();
      await expect(approvalDrawer.getByRole('tab', { name: '表单权限' })).toBeVisible();
      await expect(approvalDrawer.getByRole('tab', { name: '事件通知' })).toBeVisible();
      await expect(approvalDrawer.getByText('指定成员')).toBeVisible();
      await expect(approvalDrawer.getByText('部门主管')).toBeVisible();
      await expect(approvalDrawer.getByText('角色')).toBeVisible();
      await expect(approvalDrawer.getByText('发起人自选')).toBeVisible();
      await expect(approvalDrawer.getByText('发起人自己')).toBeVisible();
      await expect(approvalDrawer.getByText('表单人员')).toBeVisible();
      await expect(approvalDrawer.getByText('流程表达式')).toBeVisible();
      await expect(approvalDrawer.getByText('指定岗位')).toHaveCount(0);
      await expect(approvalDrawer.getByText('指定组织')).toHaveCount(0);
      await expect(approvalDrawer.getByText('审批人为空时')).toBeVisible();
      await expect(approvalDrawer.getByText('审批被拒绝')).toBeVisible();
      await approvalDrawer.getByText('发起人自选').click();
      await expect(approvalDrawer.getByText('选择方式')).toBeVisible();
      await expect(approvalDrawer.getByText('多人审批时采用的审批方式')).toHaveCount(0);
      await approvalDrawer.getByText('多选').click();
      await expect(approvalDrawer.getByText('多人审批时采用的审批方式')).toBeVisible();
      await approvalDrawer.getByText('发起人自己').click();
      await expect(approvalDrawer.getByText('选择方式')).toHaveCount(0);
      await expect(approvalDrawer.getByText('多人审批时采用的审批方式')).toHaveCount(0);
      await approvalDrawer.getByText('指定成员').click();
      await approvalDrawer.locator('.approval-target-select').first().click();
      await expect(page.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: /admin/ })).toBeVisible({ timeout: 10000 });
      await page.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: /admin/ }).first().click();
      await expect(approvalDrawer.locator('.el-tag', { hasText: /admin/ })).toBeVisible();
      await page.locator('.el-overlay').last().click({ position: { x: 8, y: 8 } });
      await expect(approvalDrawer).toBeHidden();
      await page.locator('.add-node-button').last().click();
      await page.getByRole('button', { name: /条件分支/ }).click();
      await page.getByRole('dialog', { name: '节点属性' }).getByRole('button', { name: 'Close this dialog' }).click();
      await expect(page.getByRole('dialog', { name: '节点属性' })).toBeHidden();
      await expect(page.locator('.branch-card', { hasText: '分支1' })).toBeVisible();
      await page.locator('.branch-card', { hasText: '分支1' }).click();
      await expect(page.getByRole('dialog', { name: '条件设置' })).toBeVisible();
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
      await page.getByRole('dialog', { name: '条件设置' }).getByRole('button', { name: 'Close this dialog' }).click();
      await expect(page.getByRole('dialog', { name: '条件设置' })).toBeHidden();
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
      await openWorkflowManage(page);
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

  test('平台管理员可从发起流程页面提交已发布流程并在我的发起回显', async ({ page, request }) => {
    const unique = Date.now();
    const keyword = `e2e_workflow_start_${unique}`;
    const groupName = `E2E发起分组${unique}`;
    const groupCode = keyword;
    const definitionName = `E2E请假流程${unique}`;
    const definitionKey = `e2e_leave_${unique}`;
    const businessKey = `leave-${unique}`;
    const token = await loginToken(request, platformTenant);
    const headers = { Authorization: `Bearer ${token}` };

    try {
      await cleanupWorkflow(request, token, keyword);

      const createGroupResponse = await request.post('http://localhost:5555/workflow/groups', {
        headers,
        data: {
          groupName,
          groupCode,
          sort: 97,
          status: 1,
          remark: 'E2E发起流程验证数据',
        },
      });
      expect(createGroupResponse.status()).toBe(200);
      const createGroupBody = await createGroupResponse.json();
      expect(createGroupBody.success || createGroupBody.code === 200).toBeTruthy();

      const createDefinitionResponse = await request.post('http://localhost:5555/workflow/definitions', {
        headers,
        data: {
          groupId: createGroupBody.data,
          definitionName,
          definitionKey,
          designerJson: designerJson(unique),
          formCode: `form_${keyword}`,
          formJson: leaveFormJson(),
          status: 'DRAFT',
          remark: 'E2E发起流程验证数据',
        },
      });
      expect(createDefinitionResponse.status()).toBe(200);
      const createDefinitionBody = await createDefinitionResponse.json();
      expect(createDefinitionBody.success || createDefinitionBody.code === 200).toBeTruthy();

      const deployResponse = await request.post(`http://localhost:5555/workflow/definitions/deploy?id=${createDefinitionBody.data}`, {
        headers,
      });
      expect(deployResponse.status()).toBe(200);
      const deployBody = await deployResponse.json();
      expect(deployBody.success || deployBody.code === 200).toBeTruthy();
      expect(deployBody.data.processDefinitionId).toBeTruthy();

      await loginPage(page, platformTenant);
      const definitionsResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/definitions/page') && response.status() === 200
      );
      await openStartProcess(page);
      await definitionsResponsePromise;
      await page.getByPlaceholder('搜索流程名称/编码').fill(definitionName);
      await page.getByRole('button', { name: '查询' }).click();
      await expect(page.locator('.el-table__row', { hasText: definitionName })).toBeVisible({ timeout: 10000 });

      await page.locator('.el-table__row', { hasText: definitionName }).getByRole('button', { name: '发起' }).click();
      await expect(page.getByRole('dialog', { name: '发起流程' })).toBeVisible();
      await expect(page.getByText('流程表单')).toBeVisible();
      await page.locator('.el-form-item', { hasText: '业务主键' }).getByRole('textbox').fill(businessKey);
      await page.getByPlaceholder('请输入请假天数').fill('2');
      await page.getByPlaceholder('请输入请假原因').fill('E2E 请假发起验证');

      const startResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/processes/start') && response.status() === 200
      );
      await page.getByRole('button', { name: '确认发起' }).click();
      const startResponse = await startResponsePromise;
      const startBody = await startResponse.json();
      expect(startBody.success || startBody.code === 200).toBeTruthy();
      expect(startBody.data.businessKey).toBe(businessKey);
      await expect(page.getByText(/流程已发起/)).toBeVisible({ timeout: 10000 });

      const initiatedResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/processes/initiated') && response.status() === 200
      );
      await openInitiatedTasks(page);
      await initiatedResponsePromise;
      await expect(page.locator('.el-table__row', { hasText: businessKey })).toBeVisible({ timeout: 10000 });
      await expect(page.locator('.el-table__row', { hasText: businessKey })).toContainText(definitionKey);
      await expectNoAuthError(page);
    } finally {
      await cleanupWorkflow(request, token, keyword).catch(() => undefined);
    }
  });

  test('发起人自选审批人使用成员选择器并进入所选人待办', async ({ page, request }) => {
    test.setTimeout(60000);
    const unique = Date.now();
    const keyword = `e2e_workflow_select_${unique}`;
    const businessKey = `starter-select-${unique}`;
    const token = await loginToken(request, platformTenant);

    try {
      await cleanupWorkflow(request, token, keyword);
      const workflow = await prepareInitiatorSelectWorkflow(request, token, unique, keyword);

      await loginPage(page, platformTenant);
      const definitionsResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/definitions/page') && response.status() === 200
      );
      await openStartProcess(page);
      await definitionsResponsePromise;
      await page.getByPlaceholder('搜索流程名称/编码').fill(workflow.definitionName);
      await page.getByRole('button', { name: '查询' }).click();
      await expect(page.locator('.el-table__row', { hasText: workflow.definitionName })).toBeVisible({ timeout: 10000 });

      await page.locator('.el-table__row', { hasText: workflow.definitionName }).getByRole('button', { name: '发起' }).click();
      const startDialog = page.getByRole('dialog', { name: '发起流程' });
      await expect(startDialog).toBeVisible();
      await expect(startDialog.getByText('发起人自选审批人')).toBeVisible();
      await page.locator('.el-form-item', { hasText: '业务主键' }).getByRole('textbox').fill(businessKey);
      await page.getByPlaceholder('请输入请假天数').fill('4');
      await page.getByPlaceholder('请输入请假原因').fill('E2E 发起人自选验证');

      await startDialog.getByRole('button', { name: '确认发起' }).click();
      await expect(page.getByText('请选择「发起人自选审批」审批人')).toBeVisible({ timeout: 10000 });

      await startDialog.locator('.selected-assignee-select').click();
      await expect(page.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: /admin/ })).toBeVisible({ timeout: 10000 });
      await page.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: /admin/ }).first().click();

      const startResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/processes/start') && response.status() === 200
      );
      await startDialog.getByRole('button', { name: '确认发起' }).click();
      const startResponse = await startResponsePromise;
      const startBody = await startResponse.json();
      expect(startBody.success || startBody.code === 200).toBeTruthy();
      expect(startBody.data.businessKey).toBe(businessKey);
      await expect(page.getByText(/流程已发起/)).toBeVisible({ timeout: 10000 });

      const todoResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/tasks/todo') && response.status() === 200
      );
      await openTodoTasks(page);
      await todoResponsePromise;
      await expect(page.locator('.el-table__row', { hasText: businessKey })).toBeVisible({ timeout: 10000 });
      await expect(page.locator('.el-table__row', { hasText: businessKey })).toContainText('发起人自选审批');
      await expectNoAuthError(page);
    } finally {
      await cleanupWorkflow(request, token, keyword).catch(() => undefined);
    }
  });

  test('发起人自己审批进入本人待办并可通过', async ({ page, request }) => {
    test.setTimeout(60000);
    const unique = Date.now();
    const keyword = `e2e_workflow_self_${unique}`;
    const businessKey = `initiator-self-${unique}`;
    const token = await loginToken(request, platformTenant);

    try {
      await cleanupWorkflow(request, token, keyword);
      const workflow = await prepareInitiatorSelfWorkflow(request, token, unique, keyword);
      await startLeaveProcess(request, token, workflow.definitionId, businessKey, 5, 'E2E 发起人自己审批验证');

      await loginPage(page, platformTenant);
      const todoResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/tasks/todo') && response.status() === 200
      );
      await openTodoTasks(page);
      await todoResponsePromise;
      const taskRow = page.locator('.el-table__row', { hasText: businessKey });
      await expect(taskRow).toBeVisible({ timeout: 10000 });
      await expect(taskRow).toContainText('发起人自己审批');

      const detailResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/tasks/detail') && response.status() === 200
      );
      await taskRow.getByRole('button', { name: '处理' }).click();
      await page.waitForURL('**/#/workflow/task/detail**', { timeout: 10000 });
      await detailResponsePromise;
      await expect(page.getByText('处理任务')).toBeVisible();
      await expect(page.getByRole('spinbutton', { name: '请假天数' })).toHaveValue('5');
      await expect(page.getByRole('textbox', { name: '请假原因' })).toHaveValue('E2E 发起人自己审批验证');
      await expect(page.getByText('发起 · admin')).toBeVisible();

      await page.getByPlaceholder('请输入审批意见').fill('本人确认通过');
      await page.getByPlaceholder('例如：{"approved":true}').fill('{"approved":true,"selfApproved":true}');
      const completeResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/tasks/complete') && response.status() === 200
      );
      await page.getByRole('button', { name: '通过' }).click();
      await page.getByRole('dialog', { name: '审批通过' }).getByRole('button', { name: /^(OK|确定)$/ }).click();
      const completeResponse = await completeResponsePromise;
      const completeBody = await completeResponse.json();
      expect(completeBody.success || completeBody.code === 200).toBeTruthy();
      await page.waitForURL('**/#/workflow/task/done', { timeout: 10000 });
      await expect(page.locator('.el-table__row', { hasText: businessKey })).toBeVisible({ timeout: 10000 });
      await expectNoAuthError(page);
    } finally {
      await cleanupWorkflow(request, token, keyword).catch(() => undefined);
    }
  });

  test('指定成员审批进入所选人待办并可通过', async ({ page, request }) => {
    test.setTimeout(60000);
    const unique = Date.now();
    const keyword = `e2e_workflow_approve_${unique}`;
    const businessKey = `leave-approve-${unique}`;
    const token = await loginToken(request, platformTenant);

    try {
      await cleanupWorkflow(request, token, keyword);
      const workflow = await prepareLeaveWorkflow(request, token, unique, keyword);
      await startLeaveProcess(request, token, workflow.definitionId, businessKey, 3, 'E2E 审批通过验证');

      await loginPage(page, platformTenant);
      const todoResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/tasks/todo') && response.status() === 200
      );
      await openTodoTasks(page);
      await todoResponsePromise;
      await expect(page.locator('.el-table__row', { hasText: businessKey })).toBeVisible({ timeout: 10000 });

      const detailResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/tasks/detail') && response.status() === 200
      );
      await page.locator('.el-table__row', { hasText: businessKey }).getByRole('button', { name: '处理' }).click();
      await page.waitForURL('**/#/workflow/task/detail**', { timeout: 10000 });
      await detailResponsePromise;
      await expect(page.getByText('处理任务')).toBeVisible();
      await expect(page.getByText('请假天数')).toBeVisible();
      await expect(page.getByRole('spinbutton', { name: '请假天数' })).toHaveValue('3');
      await expect(page.getByRole('textbox', { name: '请假原因' })).toHaveValue('E2E 审批通过验证');
      await expect(page.getByText('发起 · admin')).toBeVisible();

      await page.getByPlaceholder('请输入审批意见').fill('同意，UI E2E');
      await page.getByPlaceholder('例如：{"approved":true}').fill('{"approved":true,"approveRemark":"ui complete"}');
      const completeResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/tasks/complete') && response.status() === 200
      );
      await page.getByRole('button', { name: '通过' }).click();
      await page.getByRole('dialog', { name: '审批通过' }).getByRole('button', { name: /^(OK|确定)$/ }).click();
      const completeResponse = await completeResponsePromise;
      const completeBody = await completeResponse.json();
      expect(completeBody.success || completeBody.code === 200).toBeTruthy();
      await page.waitForURL('**/#/workflow/task/done', { timeout: 10000 });
      await expect(page.locator('.el-table__row', { hasText: businessKey })).toBeVisible({ timeout: 10000 });
      await expectNoAuthError(page);
    } finally {
      await cleanupWorkflow(request, token, keyword).catch(() => undefined);
    }
  });

  test('平台管理员可在我的待办拒绝审批并在我的发起查看已拒绝详情', async ({ page, request }) => {
    test.setTimeout(60000);
    const unique = Date.now();
    const keyword = `e2e_workflow_reject_${unique}`;
    const businessKey = `leave-reject-${unique}`;
    const token = await loginToken(request, platformTenant);

    try {
      await cleanupWorkflow(request, token, keyword);
      const workflow = await prepareLeaveWorkflow(request, token, unique, keyword);
      await startLeaveProcess(request, token, workflow.definitionId, businessKey, 1, 'E2E 审批拒绝验证');

      await loginPage(page, platformTenant);
      const todoResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/tasks/todo') && response.status() === 200
      );
      await openTodoTasks(page);
      await todoResponsePromise;
      await expect(page.locator('.el-table__row', { hasText: businessKey })).toBeVisible({ timeout: 10000 });

      await page.locator('.el-table__row', { hasText: businessKey }).getByRole('button', { name: '处理' }).click();
      await page.waitForURL('**/#/workflow/task/detail**', { timeout: 10000 });
      await expect(page.getByText('E2E 审批拒绝验证')).toBeVisible();
      await page.getByPlaceholder('请输入审批意见').fill('拒绝，UI E2E');
      await page.getByPlaceholder('例如：{"approved":true}').fill('{"approved":false,"rejectReason":"ui reject"}');
      const rejectResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/tasks/reject') && response.status() === 200
      );
      await page.getByRole('button', { name: '拒绝' }).click();
      await page.getByRole('dialog', { name: '审批拒绝' }).getByRole('button', { name: /^(OK|确定)$/ }).click();
      const rejectResponse = await rejectResponsePromise;
      const rejectBody = await rejectResponse.json();
      expect(rejectBody.success || rejectBody.code === 200).toBeTruthy();

      const initiatedResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/processes/initiated') && response.status() === 200
      );
      await openInitiatedTasks(page);
      await initiatedResponsePromise;
      await expect(page.locator('.el-table__row', { hasText: businessKey })).toBeVisible({ timeout: 10000 });
      await expect(page.locator('.el-table__row', { hasText: businessKey })).toContainText('已拒绝');

      const processDetailResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/processes/detail') && response.status() === 200
      );
      await page.locator('.el-table__row', { hasText: businessKey }).getByRole('button', { name: '查看' }).click();
      await page.waitForURL('**/#/workflow/task/detail**', { timeout: 10000 });
      await processDetailResponsePromise;
      await expect(page.getByText('流程详情')).toBeVisible();
      await expect(page.locator('.header-actions').getByText('已拒绝', { exact: true })).toBeVisible();
      await expect(page.getByText('拒绝 · admin')).toBeVisible();
      await expect(page.getByText('拒绝，UI E2E')).toBeVisible();
      await expectNoAuthError(page);
    } finally {
      await cleanupWorkflow(request, token, keyword).catch(() => undefined);
    }
  });
});
