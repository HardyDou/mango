import { expect, test, type APIRequestContext, type Page } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { api as e2eApi } from '../support/api';

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
const defaultWorkflowDomainCode = 'COMMON';

function api(path: string) {
  return e2eApi(path);
}

async function routeMinioDirectAccess(page: Page, request: APIRequestContext) {
  await page.route('http://file.mango.io:9000/**', async (route) => {
    const url = new URL(route.request().url());
    const response = await request.get(`http://127.0.0.1:9000${url.pathname}${url.search}`, {
      headers: { Host: url.host },
    });
    await route.fulfill({
      status: response.status(),
      headers: response.headers(),
      body: await response.body(),
    });
  });
}

async function loginToken(request: APIRequestContext, tenant: LoginTenant) {
  return loginTokenAs(request, tenant, 'admin', 'admin123');
}

async function loginTokenAs(request: APIRequestContext, tenant: LoginTenant, username: string, password: string) {
  const response = await request.post(api(`/auth/login`), {
    data: {
      username,
      password,
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
    response.url().includes('/api/auth/login-institutions') && response.status() === 200,
    { timeout: 10000 },
  ).catch(() => null);
  await page.locator('input[placeholder="密码"]').blur();
  await page.locator('.tenant-select').click();
  await accountTenantsResponsePromise;
  await page.getByRole('option', { name: new RegExp(tenant.tenantName) }).click();
  await page.locator('.login-btn').click();
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

async function openWorkflowManage(page: Page) {
  await page.getByRole('button', { name: '审批中心' }).click();
  await page.getByRole('menuitem', { name: '流程管理' }).click();
  await page.getByRole('menuitem', { name: '流程定义' }).click();
  await page.waitForURL('**/#/workflow/manage/definition', { timeout: 10000 });
}

async function openStartProcess(page: Page) {
  await page.getByRole('button', { name: '审批中心' }).click();
  await page.getByRole('menuitem', { name: '发起流程' }).click();
  await page.waitForURL('**/#/workflow/start-process', { timeout: 10000 });
}

async function openInitiatedTasks(page: Page) {
  await page.getByRole('button', { name: '审批中心' }).click();
  await page.getByRole('menubar').getByText('流程办理', { exact: true }).click();
  await page.getByRole('menuitem', { name: '我的申请' }).click();
  await page.waitForURL('**/#/workflow/task/initiated', { timeout: 10000 });
}

async function waitForInitiatedTasksLoad(page: Page) {
  await page.waitForResponse((response) =>
    response.status() === 200
    && (
      response.url().includes('/api/workflow/business-applies/page')
      || response.url().includes('/api/workflow/processes/initiated')
    )
  );
}

async function openTodoTasks(page: Page) {
  await page.getByRole('button', { name: '审批中心' }).click();
  await page.getByRole('menubar').getByText('流程办理', { exact: true }).click();
  await page.getByRole('menuitem', { name: '我的待办' }).click();
  await page.waitForURL('**/#/workflow/task/todo', { timeout: 10000 });
}

async function selectWorkflowCategory(page: Page, categoryName: string) {
  await page.locator('.builder-form .el-form-item', { hasText: '流程分类' }).locator('.el-select').click();
  await page.getByRole('option', { name: categoryName }).click();
}

async function openClaimableTodoTab(page: Page, keyword?: string) {
  const claimableResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/workflow/tasks/todo')
    && response.url().includes('todoType=CLAIMABLE')
    && response.status() === 200
  );
  await page.getByRole('tab', { name: '待领取' }).click();
  await claimableResponsePromise;
  if (!keyword) {
    return;
  }
  const filteredResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/workflow/tasks/todo')
    && response.url().includes('todoType=CLAIMABLE')
    && response.url().includes(`keyword=${encodeURIComponent(keyword)}`)
    && response.status() === 200
  );
  await page.getByPlaceholder('搜索流程/任务名称').fill(keyword);
  await page.getByRole('button', { name: '查询' }).click();
  await filteredResponsePromise;
}

async function openDoneTasks(page: Page) {
  await page.getByRole('button', { name: '审批中心' }).click();
  await page.getByRole('menubar').getByText('流程办理', { exact: true }).click();
  await page.getByRole('menuitem', { name: '我的已办' }).click();
  await page.waitForURL('**/#/workflow/task/done', { timeout: 10000 });
}

async function openCopiedTasks(page: Page) {
  await page.getByRole('button', { name: '审批中心' }).click();
  await page.getByRole('menubar').getByText('流程办理', { exact: true }).click();
  await page.getByRole('menuitem', { name: '抄送给我' }).click();
  await page.waitForURL('**/#/workflow/task/copied', { timeout: 10000 });
}

async function cleanupWorkflow(request: APIRequestContext, token: string, keyword: string) {
  const headers = { Authorization: `Bearer ${token}` };
  const definitionsResponse = await request.get(api(`/workflow/definitions/page?page=1&size=100&keyword=${keyword}`), {
    headers,
  });
  if (definitionsResponse.status() === 200) {
    const definitionsBody = await definitionsResponse.json();
    for (const definition of definitionsBody.data?.list || []) {
      await request.put(api(`/workflow/definitions/status`), {
        headers,
        data: { id: definition.id, status: 'DISABLED' },
      });
      await request.delete(api(`/workflow/definitions?id=${definition.id}`), {
        headers,
      });
    }
  }

  const categoriesResponse = await request.get(api(`/workflow/categories/page?page=1&size=100&keyword=${keyword}`), {
    headers,
  });
  if (categoriesResponse.status() === 200) {
    const categoriesBody = await categoriesResponse.json();
    for (const category of categoriesBody.data?.list || []) {
      await request.delete(api(`/workflow/categories?id=${category.id}`), {
        headers,
      });
    }
  }
}

async function cleanupWorkflowTemplates(request: APIRequestContext, token: string, keyword: string) {
  const headers = { Authorization: `Bearer ${token}` };
  const templatesResponse = await request.get(api(`/workflow/templates/page?page=1&size=100&keyword=${keyword}`), {
    headers,
  });
  if (templatesResponse.status() === 200) {
    const templatesBody = await templatesResponse.json();
    for (const template of templatesBody.data?.list || []) {
      await request.delete(api(`/workflow/templates?id=${template.id}`), {
        headers,
      });
    }
  }

  const templateCategoriesResponse = await request.get(api(`/workflow/template-categories/page?page=1&size=100&keyword=${keyword}`), {
    headers,
  });
  if (templateCategoriesResponse.status() === 200) {
    const templateCategoriesBody = await templateCategoriesResponse.json();
    for (const category of templateCategoriesBody.data?.list || []) {
      await request.delete(api(`/workflow/template-categories?id=${category.id}`), {
        headers,
      });
    }
  }
}

async function cleanupWorkflowUploadFiles(request: APIRequestContext, token: string, fileIds: string[]) {
  const headers = { Authorization: `Bearer ${token}` };
  for (const id of fileIds) {
    await request.delete(api(`/file/files?id=${encodeURIComponent(id)}&reason=e2e-workflow-cleanup`), {
      headers,
    }).catch(() => undefined);
  }
}

async function expectWorkflowCategoryAndDefinitionInDomain(
  request: APIRequestContext,
  token: string,
  params: {
    categoryId: string;
    definitionId: string;
    domainCode: string;
    categoryName: string;
  },
) {
  const headers = { Authorization: `Bearer ${token}` };
  const categoryResponse = await request.get(api(`/workflow/categories/list?status=1&domainCode=${params.domainCode}`), {
    headers,
  });
  expect(categoryResponse.status()).toBe(200);
  const categoryBody = await categoryResponse.json();
  expectApiSuccess(categoryBody, '按业务域查询流程分类失败');
  const category = (categoryBody.data || []).find((item: any) => String(item.id) === String(params.categoryId));
  expect(category, `未在 ${params.domainCode} 业务域分类列表中找到测试分类`).toBeTruthy();
  expect(category.categoryName).toBe(params.categoryName);
  expect(category.domainCode).toBe(params.domainCode);

  const definitionResponse = await request.get(api(`/workflow/definitions/detail?id=${params.definitionId}`), {
    headers,
  });
  expect(definitionResponse.status()).toBe(200);
  const definitionBody = await definitionResponse.json();
  expectApiSuccess(definitionBody, '查询流程定义详情失败');
  expect(String(definitionBody.data.categoryId)).toBe(String(params.categoryId));
  expect(definitionBody.data.categoryName).toBe(params.categoryName);
  expect(definitionBody.data.domainCode).toBe(params.domainCode);
}

function expectApiSuccess(body: any, context: string) {
  expect(body.success || body.code === 200, `${context}: ${JSON.stringify(body, null, 2)}`).toBeTruthy();
}

function expectWorkflowUploadValueOnlyContainsFileIds(value: unknown, expectedIds: string[]) {
  expect(value).toEqual(expectedIds);
  const serialized = JSON.stringify(value);
  expect(serialized).not.toContain('http');
  expect(serialized).not.toContain('/api/file/files/download');
  expect(serialized).not.toContain('url');
  expect(serialized).not.toContain('downloadUrl');
  expect(serialized).not.toContain('directPreviewUrl');
  expect(serialized).not.toContain('directDownloadUrl');
}

function designerJson(unique: number) {
  return JSON.stringify({
    id: 'startEvent',
    nodeName: '发起人',
    nodeType: 'ROOT',
    childNode: {
      id: `risk_review_${unique}`,
      nodeName: '元丰行风控初审',
      nodeType: 'CONTRACT_RISK_REVIEW',
      childNode: {
        id: `guarantor_approve_${unique}`,
        nodeName: '合同相对方审批',
        nodeType: 'CONTRACT_COUNTERPARTY_APPROVE',
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

function expenseApprovalDesignerJson(unique: number) {
  return JSON.stringify({
    id: 'startEvent',
    nodeName: '发起人',
    nodeType: 'ROOT',
    childNode: {
      id: `manager_approve_${unique}`,
      nodeName: '部门经理审批',
      nodeType: 'APPROVAL',
      bpmnType: 'userTask',
      executionType: 'USER_TASK',
      childNode: {
        id: `finance_review_${unique}`,
        nodeName: '财务复核',
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
            actions: {
              complete: { enabled: true, label: '财务确认', requireComment: false, order: 50 },
              reject: { enabled: true, label: '退回补充', requireComment: true, danger: true, order: 40 },
              transfer: { enabled: true, label: '转办他人', requireComment: false, order: 20 },
            },
            extension: {
              approvePageKey: 'workflow.expense.approve.finance',
              sectionPreset: 'FINANCE_REVIEW',
            },
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
          actions: {
            complete: { enabled: true, label: '经理同意', requireComment: false, order: 50 },
            reject: { enabled: true, label: '退回修改', requireComment: true, danger: true, order: 40 },
          },
          extension: {
            approvePageKey: 'workflow.expense.approve.manager',
            sectionPreset: 'MANAGER_APPROVE',
          },
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

function actionCapabilityDesignerJson(unique: number, roleId: string) {
  return JSON.stringify({
    id: 'startEvent',
    nodeName: '发起人',
    nodeType: 'ROOT',
    childNode: {
      id: `action_approve_${unique}`,
      nodeName: '动作审批',
      nodeType: 'APPROVAL',
      bpmnType: 'userTask',
      executionType: 'USER_TASK',
      childNode: {
        id: `action_cc_${unique}`,
        nodeName: '动作抄送',
        nodeType: 'CC',
        bpmnType: 'serviceTask',
        executionType: 'EVENT_PUBLISH',
        childNode: null,
        conditionNodes: [],
        properties: {
          ccConfig: {
            userIds: ['admin'],
            messageTemplate: '动作审批 ${businessKey} 已通过',
          },
        },
      },
      conditionNodes: [],
      properties: {
        approvalConfig: {
          assigneeType: 'SPECIFIED_ROLE',
          assigneeIds: [],
          roleIds: [roleId],
          postIds: [],
          orgIds: [],
          approvalMode: 'COUNTERSIGN',
          emptyAssigneeStrategy: 'TO_ADMIN',
          emptyAssigneeUserIds: [],
          rejectStrategy: 'END_PROCESS',
          formPermissions: {
            reason: 'EDITABLE',
          },
          actions: {
            save: { enabled: true, label: '暂存', requireComment: false, order: 10 },
            transfer: { enabled: true, label: '转办', requireComment: true, order: 20 },
            addSign: { enabled: true, label: '加签', requireComment: false, order: 30 },
            reject: { enabled: true, label: '驳回', requireComment: true, danger: true, order: 40 },
            complete: { enabled: true, label: '通过', requireComment: false, order: 50 },
          },
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

function actionAddSignDesignerJson(unique: number, assigneeIds: string[]) {
  return JSON.stringify({
    id: 'startEvent',
    nodeName: '发起人',
    nodeType: 'ROOT',
    childNode: {
      id: `action_add_sign_${unique}`,
      nodeName: '加签审批',
      nodeType: 'APPROVAL',
      bpmnType: 'userTask',
      executionType: 'USER_TASK',
      childNode: null,
      conditionNodes: [],
      properties: {
        approvalConfig: {
          assigneeType: 'SPECIFIED_USER',
          assigneeIds,
          roleIds: [],
          postIds: [],
          orgIds: [],
          approvalMode: 'COUNTERSIGN',
          emptyAssigneeStrategy: 'TO_ADMIN',
          emptyAssigneeUserIds: [],
          rejectStrategy: 'END_PROCESS',
          formPermissions: {
            reason: 'EDITABLE',
          },
          actions: {
            addSign: { enabled: true, label: '加签', requireComment: false, order: 30 },
            complete: { enabled: true, label: '通过', requireComment: false, order: 50 },
          },
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

function runtimeComponentFormJson() {
  return JSON.stringify([
    {
      type: 'elCard',
      title: '申请信息',
      children: [
        {
          type: 'input',
          field: 'applySubject',
          title: '申请主题',
          props: {
            placeholder: '请输入申请主题',
          },
          validate: [
            { required: true, message: '申请主题不能为空', trigger: 'blur' },
          ],
        },
        {
          type: 'select',
          field: 'expenseType',
          title: '费用类型',
          props: {
            placeholder: '请选择费用类型',
            clearable: true,
            filterable: true,
          },
          options: [
            { label: '差旅费', value: 'TRAVEL' },
            { label: '办公费', value: 'OFFICE' },
          ],
          validate: [
            { required: true, message: '费用类型不能为空', trigger: 'change' },
          ],
        },
        {
          type: 'upload',
          field: 'attachments',
          title: '附件',
          props: {
            accept: '.pdf,.docx,.png',
            limit: 3,
          },
        },
        {
          type: 'upload',
          field: 'images',
          title: '图片',
          props: {
            accept: 'image/*',
            listType: 'picture-card',
            limit: 2,
          },
        },
      ],
    },
    {
      type: 'fcRow',
      title: '金额信息',
      children: [
        {
          type: 'inputNumber',
          field: 'amount',
          title: '申请金额',
          props: {
            placeholder: '请输入申请金额',
            min: 1,
          },
          validate: [
            { required: true, message: '申请金额不能为空', trigger: 'change' },
          ],
        },
        {
          type: 'elTreeSelect',
          field: 'deptId',
          title: '申请部门',
          props: {
            placeholder: '请选择申请部门',
            workflowDataType: 'systemDept',
            data: [
              {
                label: '芒果集团',
                value: '1',
                children: [
                  { label: '财务部', value: 'finance' },
                ],
              },
            ],
            nodeKey: 'value',
            checkStrictly: true,
          },
        },
        {
          type: 'select',
          field: 'expenseCategoryDict',
          title: '费用字典',
          props: {
            placeholder: '请选择费用字典',
            workflowDataType: 'systemDict',
            dictType: 'sys_normal_disable',
            clearable: true,
            filterable: true,
          },
          validate: [
            { required: true, message: '费用字典不能为空', trigger: 'change' },
          ],
        },
        {
          type: 'input',
          field: 'applicantSignature',
          title: '申请人签字',
          props: {
            placeholder: '请在此处签名',
            workflowDataType: 'signature',
            width: 520,
            height: 180,
          },
          validate: [
            { required: true, message: '申请人签字不能为空', trigger: 'change' },
          ],
        },
      ],
    },
    {
      type: 'elAlert',
      title: '表单说明',
      props: {
        type: 'info',
        content: '请按实际业务上传审批附件。',
      },
    },
  ]);
}

async function prepareExpenseWorkflow(request: APIRequestContext, token: string, unique: number, keyword: string) {
  const headers = { Authorization: `Bearer ${token}` };
  const createCategoryResponse = await request.post(api(`/workflow/categories`), {
    headers,
    data: {
      categoryName: `E2E费用报销分类${unique}`,
      categoryCode: keyword,
      domainCode: defaultWorkflowDomainCode,
      sort: 93,
      status: 1,
      remark: 'E2E费用报销业务接入验证数据',
    },
  });
  expect(createCategoryResponse.status()).toBe(200);
  const createCategoryBody = await createCategoryResponse.json();
  expect(createCategoryBody.success || createCategoryBody.code === 200).toBeTruthy();

  const createDefinitionResponse = await request.post(api(`/workflow/definitions`), {
    headers,
    data: {
      categoryId: createCategoryBody.data,
      domainCode: defaultWorkflowDomainCode,
      definitionName: `E2E费用报销审批${unique}`,
      definitionKey: `e2e_expense_reimbursement_${unique}`,
      designerJson: expenseApprovalDesignerJson(unique),
      formCode: `form_${keyword}`,
      formJson: JSON.stringify([]),
      status: 'DRAFT',
      remark: 'E2E费用报销业务接入验证数据',
    },
  });
  expect(createDefinitionResponse.status()).toBe(200);
  const createDefinitionBody = await createDefinitionResponse.json();
  expect(createDefinitionBody.success || createDefinitionBody.code === 200).toBeTruthy();

  const deployResponse = await request.post(api(`/workflow/definitions/deploy?id=${createDefinitionBody.data}`), {
    headers,
  });
  expect(deployResponse.status()).toBe(200);
  const deployBody = await deployResponse.json();
  expectApiSuccess(deployBody, '费用报销流程部署失败');

  return {
    definitionId: createDefinitionBody.data as string,
    definitionName: `E2E费用报销审批${unique}`,
    definitionKey: `e2e_expense_reimbursement_${unique}`,
  };
}

function expenseVariables(businessKey: string, applySequence: number, amount: number, reason: string) {
  const applyId = `APPLY-${businessKey}-${String(applySequence).padStart(3, '0')}`;
  return {
    businessType: 'EXPENSE_REIMBURSEMENT',
    businessKey,
    applyId,
    title: `费用报销 ${businessKey}`,
    summary: `差旅费 ¥${amount.toFixed(2)}`,
    expenseCode: businessKey,
    applicant: 'admin',
    category: '差旅费',
    amount,
    expenseDate: '2026-05-18',
    reason,
    invoiceCount: applySequence,
    bankAccount: `622202******${String(8000 + applySequence)}`,
    budgetSubject: '销售中心-差旅费',
    businessPermissions: {
      [`manager_approve_${businessKey.split('-').at(-1)}`]: {
        expenseReason: 'READONLY',
        invoiceInfo: 'READONLY',
        paymentInfo: 'HIDDEN',
        financeReview: 'HIDDEN',
      },
      [`finance_review_${businessKey.split('-').at(-1)}`]: {
        expenseReason: 'READONLY',
        invoiceInfo: 'READONLY',
        paymentInfo: 'READONLY',
        financeReview: 'EDITABLE',
      },
    },
  };
}

async function startExpenseProcess(
  request: APIRequestContext,
  token: string,
  definitionId: string,
  unique: number,
  businessKey: string,
  applySequence: number,
  amount: number,
  reason: string,
) {
  const variables = {
    ...expenseVariables(businessKey, applySequence, amount, reason),
    businessPermissions: {
      [`manager_approve_${unique}`]: {
        expenseReason: 'READONLY',
        invoiceInfo: 'READONLY',
        paymentInfo: 'HIDDEN',
        financeReview: 'HIDDEN',
      },
      [`finance_review_${unique}`]: {
        expenseReason: 'READONLY',
        invoiceInfo: 'READONLY',
        paymentInfo: 'READONLY',
        financeReview: 'EDITABLE',
      },
    },
  };
  const response = await request.post(api(`/workflow/processes/start`), {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      definitionId,
      businessType: 'EXPENSE_REIMBURSEMENT',
      businessKey,
      renderMode: 'CUSTOM_PAGE',
      applyPageKey: 'workflow.expense.apply',
      approvePageKey: 'workflow.expense.approve',
      snapshotRef: `EXPENSE_REIMBURSEMENT:${variables.applyId}`,
      variables,
    },
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.success || body.code === 200).toBeTruthy();
  return {
    processInstanceId: body.data.processInstanceId as string,
    variables,
  };
}

async function latestBusinessApply(request: APIRequestContext, token: string, businessKey: string) {
  const response = await request.get(api(`/workflow/business-applies/progress/latest`), {
    headers: { Authorization: `Bearer ${token}` },
    params: {
      businessType: 'EXPENSE_REIMBURSEMENT',
      businessKey,
    },
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.success || body.code === 200).toBeTruthy();
  return body.data;
}

async function businessApplyHistory(request: APIRequestContext, token: string, businessKey: string) {
  const response = await request.get(api(`/workflow/business-applies/history`), {
    headers: { Authorization: `Bearer ${token}` },
    params: {
      businessType: 'EXPENSE_REIMBURSEMENT',
      businessKey,
      page: 1,
      size: 50,
    },
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.success || body.code === 200).toBeTruthy();
  return body.data?.list || body.data?.records || [];
}

async function findTodoTask(request: APIRequestContext, token: string, businessKey: string, taskName: string) {
  const response = await request.get(api(`/workflow/tasks/todo`), {
    headers: { Authorization: `Bearer ${token}` },
    params: { page: 1, size: 50, keyword: businessKey, todoType: 'ALL' },
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.success || body.code === 200).toBeTruthy();
  const tasks = body.data?.list || body.data?.records || [];
  const task = tasks.find((item: any) => String(item.businessKey) === businessKey && String(item.taskName).includes(taskName));
  expect(task, `未找到 ${businessKey} 的待办 ${taskName}`).toBeTruthy();
  return task;
}

async function listTodoTasks(request: APIRequestContext, token: string, businessKey: string) {
  const response = await request.get(api(`/workflow/tasks/todo`), {
    headers: { Authorization: `Bearer ${token}` },
    params: { page: 1, size: 50, keyword: businessKey, todoType: 'ALL' },
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  expectApiSuccess(body, `查询待办失败: ${businessKey}`);
  return body.data?.list || body.data?.records || [];
}

async function expectReadonlyFieldValue(page: Page, label: string, value: string) {
  await expect(page.locator('.el-form-item', { hasText: label })).toContainText(value);
}

async function expectWorkflowTaskDetailSidebar(page: Page, currentNode: string) {
  const detailPage = page.locator('.workflow-task-detail-page');
  await expect(detailPage).toContainText('当前节点');
  await expect(detailPage).toContainText(currentNode);
  await expect(detailPage).toContainText('发起人');
  await expect(detailPage).toContainText('admin');
  await expect(detailPage).toContainText('当前节点待处理');
  await expect(detailPage.locator('.workflow-sidebar__actions .el-button').first()).toBeVisible();
}

async function pickUserFromDialog(page: Page, outerDialogName: string, pickerDialogName: string, username: string) {
  const outerDialog = page.locator('.el-dialog:visible', {
    has: page.getByRole('heading', { name: outerDialogName }),
  }).filter({ has: page.getByPlaceholder(/请选择/) }).first();
  await outerDialog.getByPlaceholder(new RegExp('请选择')).click();
  const pickerDialog = page.locator('.el-dialog:visible', {
    has: page.getByRole('heading', { name: pickerDialogName }),
  }).filter({ has: page.locator('.selector-dialog') }).first();
  await pickerDialog.getByPlaceholder('搜索姓名、用户名').fill(username);
  await pickerDialog.locator('.selector-item', { hasText: username }).click();
  await pickerDialog.locator('.el-dialog__footer').getByRole('button', { name: '确认' }).click();
  await outerDialog.locator('.el-dialog__footer').getByRole('button', { name: '确认' }).click();
}

async function completeTask(request: APIRequestContext, token: string, taskId: string, comment: string, variables: Record<string, any> = {}) {
  const response = await request.post(api(`/workflow/tasks/complete`), {
    headers: { Authorization: `Bearer ${token}` },
    data: { taskId, comment, variables },
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.success || body.code === 200).toBeTruthy();
}

async function prepareActionCapabilityWorkflow(
  request: APIRequestContext,
  token: string,
  unique: number,
  keyword: string,
  roleId: string,
) {
  const headers = { Authorization: `Bearer ${token}` };
  const createCategoryResponse = await request.post(api(`/workflow/categories`), {
    headers,
    data: {
      categoryName: `E2E动作分类${unique}`,
      categoryCode: keyword,
      domainCode: defaultWorkflowDomainCode,
      sort: 91,
      status: 1,
      remark: keyword,
    },
  });
  expect(createCategoryResponse.status()).toBe(200);
  const createCategoryBody = await createCategoryResponse.json();
  expectApiSuccess(createCategoryBody, '创建动作流程分类失败');

  const createDefinitionResponse = await request.post(api(`/workflow/definitions`), {
    headers,
    data: {
      categoryId: createCategoryBody.data,
      domainCode: defaultWorkflowDomainCode,
      definitionName: `E2E审批动作流程${unique}`,
      definitionKey: `e2e_action_capability_${unique}`,
      designerJson: actionCapabilityDesignerJson(unique, roleId),
      formCode: `form_${keyword}`,
      formJson: leaveFormJson(),
      status: 'DRAFT',
      remark: keyword,
    },
  });
  expect(createDefinitionResponse.status()).toBe(200);
  const createDefinitionBody = await createDefinitionResponse.json();
  expectApiSuccess(createDefinitionBody, '创建动作流程定义失败');

  const deployResponse = await request.post(api(`/workflow/definitions/deploy?id=${createDefinitionBody.data}`), {
    headers,
  });
  expect(deployResponse.status()).toBe(200);
  const deployBody = await deployResponse.json();
  expectApiSuccess(deployBody, '动作流程部署失败');

  return {
    definitionId: createDefinitionBody.data as string,
    definitionName: `E2E审批动作流程${unique}`,
    definitionKey: `e2e_action_capability_${unique}`,
  };
}

async function prepareActionAddSignWorkflow(
  request: APIRequestContext,
  token: string,
  unique: number,
  keyword: string,
  assigneeIds: string[],
) {
  const headers = { Authorization: `Bearer ${token}` };
  const createCategoryResponse = await request.post(api(`/workflow/categories`), {
    headers,
    data: {
      categoryName: `E2E加签分类${unique}`,
      categoryCode: keyword,
      domainCode: defaultWorkflowDomainCode,
      sort: 90,
      status: 1,
      remark: keyword,
    },
  });
  expect(createCategoryResponse.status()).toBe(200);
  const createCategoryBody = await createCategoryResponse.json();
  expectApiSuccess(createCategoryBody, '创建加签流程分类失败');

  const createDefinitionResponse = await request.post(api(`/workflow/definitions`), {
    headers,
    data: {
      categoryId: createCategoryBody.data,
      domainCode: defaultWorkflowDomainCode,
      definitionName: `E2E审批加签流程${unique}`,
      definitionKey: `e2e_action_add_sign_${unique}`,
      designerJson: actionAddSignDesignerJson(unique, assigneeIds),
      formCode: `form_${keyword}`,
      formJson: leaveFormJson(),
      status: 'DRAFT',
      remark: keyword,
    },
  });
  expect(createDefinitionResponse.status()).toBe(200);
  const createDefinitionBody = await createDefinitionResponse.json();
  expectApiSuccess(createDefinitionBody, '创建加签流程定义失败');

  const deployResponse = await request.post(api(`/workflow/definitions/deploy?id=${createDefinitionBody.data}`), {
    headers,
  });
  expect(deployResponse.status()).toBe(200);
  const deployBody = await deployResponse.json();
  expectApiSuccess(deployBody, '加签流程部署失败');

  return {
    definitionId: createDefinitionBody.data as string,
    definitionName: `E2E审批加签流程${unique}`,
    definitionKey: `e2e_action_add_sign_${unique}`,
  };
}

async function cleanupUser(request: APIRequestContext, token: string, username: string) {
  const headers = { Authorization: `Bearer ${token}` };
  const pageResponse = await request.get(api(`/identity/users/page`), {
    headers,
    params: { page: 1, size: 20, username },
  });
  if (!pageResponse.ok()) return;
  const body = await pageResponse.json();
  const records = body.data?.records || body.data?.list || [];
  for (const user of records.filter((item: any) => item.username === username)) {
    await request.delete(api(`/identity/users?userId=${user.userId}`), { headers }).catch(() => undefined);
  }
}

async function createTempUser(request: APIRequestContext, token: string, username: string) {
  const headers = { Authorization: `Bearer ${token}` };
  await cleanupUser(request, token, username);
  const response = await request.post(api(`/identity/users`), {
    headers,
    data: {
      username,
      password: 'E2E@123456',
      nickname: username,
      realm: 'INTERNAL',
      actorType: 'INTERNAL_USER',
      partyType: 'INTERNAL_ORG',
      partyId: 1,
      email: `${username.toLowerCase()}@example.com`,
      phone: '13800000009',
      status: 1,
      remark: 'E2E审批动作临时用户',
    },
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  expectApiSuccess(body, `创建临时用户失败: ${username}`);

  const detailResponse = await request.get(api(`/identity/users/page`), {
    headers,
    params: { page: 1, size: 20, username },
  });
  expect(detailResponse.status()).toBe(200);
  const detailBody = await detailResponse.json();
  expectApiSuccess(detailBody, `查询临时用户失败: ${username}`);
  const records = detailBody.data?.records || detailBody.data?.list || [];
  const user = records.find((item: any) => item.username === username);
  expect(user, `临时用户未返回: ${username}`).toBeTruthy();
  return user;
}

async function assignSubjectRoles(
  request: APIRequestContext,
  token: string,
  subjectId: number | string,
  roleIds: Array<number | string>,
) {
  const response = await request.post(api(`/authorization/roles/subjects`), {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      subjectId: String(subjectId),
      appCode: 'internal-admin',
      realm: 'INTERNAL',
      actorType: 'INTERNAL_USER',
      partyType: 'INTERNAL_ORG',
      partyId: 1,
      roleIds: roleIds.map(String),
    },
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  expectApiSuccess(body, `分配角色失败: ${subjectId}`);
}

async function readCopiedList(request: APIRequestContext, token: string, businessKey: string) {
  const response = await request.get(api(`/workflow/tasks/copied`), {
    headers: { Authorization: `Bearer ${token}` },
    params: { page: 1, size: 20, keyword: businessKey },
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  expectApiSuccess(body, `查询抄送失败: ${businessKey}`);
  return body.data?.list || body.data?.records || [];
}

async function waitForCopiedTask(request: APIRequestContext, token: string, businessKey: string) {
  for (let i = 0; i < 10; i += 1) {
    const copied = await readCopiedList(request, token, businessKey);
    const item = copied.find((row: any) => String(row.businessKey) === businessKey);
    if (item) return item;
    await new Promise(resolve => setTimeout(resolve, 300));
  }
  throw new Error(`未找到抄送记录: ${businessKey}`);
}

async function queryBusinessAppliesByCurrentNode(
  request: APIRequestContext,
  token: string,
  businessKey: string,
  taskDefinitionKey: string,
) {
  const response = await request.post(api(`/workflow/business-applies/page`), {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      page: 1,
      size: 20,
      businessType: 'EXPENSE_REIMBURSEMENT',
      businessKey,
      statuses: ['IN_APPROVAL'],
      currentTaskDefinitionKeys: [taskDefinitionKey],
    },
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.success || body.code === 200).toBeTruthy();
  return body.data?.list || body.data?.records || [];
}

function cleanupWorkflowBusinessApplies(businessKeyPrefix: string) {
  execFileSync('mysql', ['-uroot', 'mango', '-e', [
    `DELETE FROM workflow_business_apply_current_task WHERE business_key LIKE '${businessKeyPrefix}%'`,
    `DELETE FROM workflow_business_apply_status_log WHERE apply_id IN (SELECT id FROM workflow_business_apply WHERE business_key LIKE '${businessKeyPrefix}%')`,
    `DELETE FROM workflow_business_apply WHERE business_key LIKE '${businessKeyPrefix}%'`,
    `DELETE FROM workflow_task_record WHERE process_instance_id IN (SELECT process_instance_id FROM workflow_form_instance WHERE business_key LIKE '${businessKeyPrefix}%')`,
    `DELETE FROM workflow_form_instance WHERE business_key LIKE '${businessKeyPrefix}%'`,
  ].join('; ')]);
}

function cleanupWorkflowActionData(businessKeyPrefix: string) {
  execFileSync('mysql', ['-uroot', 'mango', '-e', [
    `DELETE FROM workflow_copied_task WHERE business_key LIKE '${businessKeyPrefix}%'`,
    `DELETE FROM workflow_business_apply_current_task WHERE business_key LIKE '${businessKeyPrefix}%'`,
    `DELETE FROM workflow_business_apply_status_log WHERE apply_id IN (SELECT id FROM workflow_business_apply WHERE business_key LIKE '${businessKeyPrefix}%')`,
    `DELETE FROM workflow_business_apply WHERE business_key LIKE '${businessKeyPrefix}%'`,
    `DELETE FROM workflow_task_record WHERE process_instance_id IN (SELECT process_instance_id FROM workflow_form_instance WHERE business_key LIKE '${businessKeyPrefix}%')`,
    `DELETE FROM workflow_form_instance WHERE business_key LIKE '${businessKeyPrefix}%'`,
  ].join('; ')]);
}

async function prepareLeaveWorkflow(request: APIRequestContext, token: string, unique: number, keyword: string) {
  const headers = { Authorization: `Bearer ${token}` };
  const createCategoryResponse = await request.post(api(`/workflow/categories`), {
    headers,
    data: {
      categoryName: `E2E审批分类${unique}`,
      categoryCode: keyword,
      domainCode: defaultWorkflowDomainCode,
      sort: 96,
      status: 1,
      remark: 'E2E审批闭环验证数据',
    },
  });
  expect(createCategoryResponse.status()).toBe(200);
  const createCategoryBody = await createCategoryResponse.json();
  expect(createCategoryBody.success || createCategoryBody.code === 200).toBeTruthy();

  const createDefinitionResponse = await request.post(api(`/workflow/definitions`), {
    headers,
    data: {
      categoryId: createCategoryBody.data,
      domainCode: defaultWorkflowDomainCode,
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

  const deployResponse = await request.post(api(`/workflow/definitions/deploy?id=${createDefinitionBody.data}`), {
    headers,
  });
  expect(deployResponse.status()).toBe(200);
  const deployBody = await deployResponse.json();
  expectApiSuccess(deployBody, '审批流程部署失败');

  return {
    definitionId: createDefinitionBody.data as string,
    definitionName: `E2E审批请假流程${unique}`,
    definitionKey: `e2e_approval_leave_${unique}`,
  };
}

async function prepareInitiatorSelectWorkflow(request: APIRequestContext, token: string, unique: number, keyword: string) {
  const headers = { Authorization: `Bearer ${token}` };
  const createCategoryResponse = await request.post(api(`/workflow/categories`), {
    headers,
    data: {
      categoryName: `E2E自选分类${unique}`,
      categoryCode: keyword,
      domainCode: defaultWorkflowDomainCode,
      sort: 95,
      status: 1,
      remark: 'E2E发起人自选验证数据',
    },
  });
  expect(createCategoryResponse.status()).toBe(200);
  const createCategoryBody = await createCategoryResponse.json();
  expect(createCategoryBody.success || createCategoryBody.code === 200).toBeTruthy();

  const createDefinitionResponse = await request.post(api(`/workflow/definitions`), {
    headers,
    data: {
      categoryId: createCategoryBody.data,
      domainCode: defaultWorkflowDomainCode,
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

  const deployResponse = await request.post(api(`/workflow/definitions/deploy?id=${createDefinitionBody.data}`), {
    headers,
  });
  expect(deployResponse.status()).toBe(200);
  const deployBody = await deployResponse.json();
  expectApiSuccess(deployBody, '发起人自选流程部署失败');

  return {
    definitionId: createDefinitionBody.data as string,
    definitionName: `E2E发起人自选流程${unique}`,
    definitionKey: `e2e_initiator_select_${unique}`,
  };
}

async function prepareInitiatorSelfWorkflow(request: APIRequestContext, token: string, unique: number, keyword: string) {
  const headers = { Authorization: `Bearer ${token}` };
  const createCategoryResponse = await request.post(api(`/workflow/categories`), {
    headers,
    data: {
      categoryName: `E2E自审分类${unique}`,
      categoryCode: keyword,
      domainCode: defaultWorkflowDomainCode,
      sort: 94,
      status: 1,
      remark: 'E2E发起人自己审批验证数据',
    },
  });
  expect(createCategoryResponse.status()).toBe(200);
  const createCategoryBody = await createCategoryResponse.json();
  expect(createCategoryBody.success || createCategoryBody.code === 200).toBeTruthy();

  const createDefinitionResponse = await request.post(api(`/workflow/definitions`), {
    headers,
    data: {
      categoryId: createCategoryBody.data,
      domainCode: defaultWorkflowDomainCode,
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

  const deployResponse = await request.post(api(`/workflow/definitions/deploy?id=${createDefinitionBody.data}`), {
    headers,
  });
  expect(deployResponse.status()).toBe(200);
  const deployBody = await deployResponse.json();
  expectApiSuccess(deployBody, '发起人自审流程部署失败');

  return {
    definitionId: createDefinitionBody.data as string,
    definitionName: `E2E发起人自己审批流程${unique}`,
    definitionKey: `e2e_initiator_self_${unique}`,
  };
}

async function startLeaveProcess(request: APIRequestContext, token: string, definitionId: string, businessKey: string, days: number, reason: string) {
  const response = await request.post(api(`/workflow/processes/start`), {
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

function startProcessCard(page: Page, name: string) {
  return page.locator('.workflow-launch-card', { hasText: name });
}

function startProcessDialog(page: Page, name: string) {
  return page.getByRole('dialog', { name });
}

test.describe('工作流配置真实接口闭环', () => {
  test('流程定义同时支持业务域侧栏和流程分类筛选并在发起页按分类分组', async ({ page, request }) => {
    test.setTimeout(90_000);
    const unique = Date.now();
    const domainCode = 'WORKFLOW';
    const keyword = `e2e_workflow_domain_category_${unique}`;
    const categoryName = `E2E域内分类${unique}`;
    const categoryCode = keyword;
    const definitionName = `E2E域内流程${unique}`;
    const definitionKey = `e2e_domain_category_${unique}`;
    const token = await loginToken(request, platformTenant);
    const headers = { Authorization: `Bearer ${token}` };

    try {
      await cleanupWorkflow(request, token, keyword);

      const createCategoryResponse = await request.post(api(`/workflow/categories`), {
        headers,
        data: {
          categoryName,
          categoryCode,
          domainCode,
          sort: 95,
          status: 1,
          remark: 'E2E业务域与流程分类并存验证数据',
        },
      });
      expect(createCategoryResponse.status()).toBe(200);
      const createCategoryBody = await createCategoryResponse.json();
      expectApiSuccess(createCategoryBody, '创建业务域内流程分类失败');
      const categoryId = String(createCategoryBody.data);

      const createDefinitionResponse = await request.post(api(`/workflow/definitions`), {
        headers,
        data: {
          categoryId,
          domainCode,
          definitionName,
          definitionKey,
          designerJson: designerJson(unique),
          formCode: `form_${keyword}`,
          formJson: leaveFormJson(),
          status: 'DRAFT',
          remark: 'E2E业务域与流程分类并存验证数据',
        },
      });
      expect(createDefinitionResponse.status()).toBe(200);
      const createDefinitionBody = await createDefinitionResponse.json();
      expectApiSuccess(createDefinitionBody, '创建业务域内流程定义失败');
      const definitionId = String(createDefinitionBody.data);

      const deployResponse = await request.post(api(`/workflow/definitions/deploy?id=${definitionId}`), {
        headers,
      });
      expect(deployResponse.status()).toBe(200);
      const deployBody = await deployResponse.json();
      expectApiSuccess(deployBody, '发布业务域内流程定义失败');
      expect(deployBody.data.processDefinitionId).toBeTruthy();

      await expectWorkflowCategoryAndDefinitionInDomain(request, token, {
        categoryId,
        definitionId,
        domainCode,
        categoryName,
      });

      await loginPage(page, platformTenant);
      await openWorkflowManage(page);
      await expect(page.getByRole('heading', { name: '业务域' })).toBeVisible({ timeout: 10000 });
      await page.getByRole('button', { name: /工作流域\s+WORKFLOW/ }).click();
      await expect(page.locator('.workflow-definition-panel')).toContainText('流程分类');

      await page.getByPlaceholder('流程名称/编码').fill(definitionName);
      await page.locator('.workflow-definition-panel').getByRole('button', { name: '查询' }).click();
      const definitionRow = page.locator('.el-table__row', { hasText: definitionName }).first();
      await expect(definitionRow).toBeVisible({ timeout: 10000 });
      await expect(definitionRow).toContainText(domainCode);
      await expect(definitionRow).toContainText(categoryName);

      await page.locator('.workflow-definition-panel .el-form-item', { hasText: '流程分类' }).locator('.el-select').click();
      await page.getByRole('option', { name: categoryName }).click();
      const filteredResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/definitions/page')
        && response.url().includes(`categoryId=${encodeURIComponent(categoryId)}`)
        && response.url().includes(`domainCode=${domainCode}`)
        && response.status() === 200
      );
      await page.locator('.workflow-definition-panel').getByRole('button', { name: '查询' }).click();
      await filteredResponsePromise;
      await expect(definitionRow).toBeVisible({ timeout: 10000 });

      const publishedResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/definitions/page')
        && response.url().includes('publishedOnly=true')
        && response.status() === 200
      );
      await openStartProcess(page);
      await publishedResponsePromise;
      await page.getByPlaceholder('搜索流程名称/编码').fill(definitionName);
      await page.getByRole('button', { name: '查询' }).click();
      const launchGroup = page.locator('.workflow-launch-group', { hasText: categoryName });
      await expect(launchGroup).toBeVisible({ timeout: 10000 });
      await expect(launchGroup.locator('.workflow-launch-card', { hasText: definitionName })).toBeVisible();
      await expectNoAuthError(page);
    } finally {
      await cleanupWorkflow(request, token, keyword).catch(() => undefined);
    }
  });

  test('流程定义图标上传使用文件组件直连地址并保存到流程', async ({ page, request }) => {
    test.setTimeout(90_000);
    const unique = Date.now();
    const keyword = `e2e_workflow_icon_${unique}`;
    const categoryName = `E2E图标分类${unique}`;
    const categoryCode = keyword;
    const definitionName = `E2E图标流程${unique}`;
    const definitionKey = `e2e_icon_process_${unique}`;
    const token = await loginToken(request, platformTenant);
    const headers = { Authorization: `Bearer ${token}` };
    const uploadedFileIds: string[] = [];
    const protectedDownloadRequests: string[] = [];

    page.on('request', (request) => {
      const url = request.url();
      if (url.includes('/api/file/files/download')) {
        protectedDownloadRequests.push(url);
      }
    });

    try {
      await cleanupWorkflow(request, token, keyword);
      await routeMinioDirectAccess(page, request);

      const createCategoryResponse = await request.post(api(`/workflow/categories`), {
        headers,
        data: {
          categoryName,
          categoryCode,
          sort: 92,
          status: 1,
          remark: 'E2E流程图标上传验证数据',
        },
      });
      expect(createCategoryResponse.status()).toBe(200);
      const createCategoryBody = await createCategoryResponse.json();
      expectApiSuccess(createCategoryBody, '创建流程图标分类失败');

      await loginPage(page, platformTenant);
      await openWorkflowManage(page);
      await expect(page.getByText('流程定义').first()).toBeVisible({ timeout: 10000 });
      await page.getByRole('button', { name: '创建流程' }).click();

      await selectWorkflowCategory(page, categoryName);
      await page.getByPlaceholder('请输入流程名称').fill(definitionName);
      await page.getByPlaceholder('如 contract_approve').fill(definitionKey);

      const iconPath = join(tmpdir(), `mango-workflow-icon-${unique}.png`);
      writeFileSync(iconPath, Buffer.from(
        'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=',
        'base64',
      ));
      const uploadResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/file/files')
        && response.request().method() === 'POST'
        && response.status() === 200
      );
      await page.locator('.workflow-icon-upload-control input[type="file"]').setInputFiles(iconPath);
      const uploadResponse = await uploadResponsePromise;
      const uploadBody = await uploadResponse.json();
      expectApiSuccess(uploadBody, '上传流程图标失败');
      const fileId = String(uploadBody.data.id);
      uploadedFileIds.push(fileId);
      const directIconUrl = String(uploadBody.data.directPreviewUrl || uploadBody.data.directDownloadUrl || uploadBody.data.url || '');
      expect(directIconUrl).toBeTruthy();
      expect(directIconUrl).not.toContain('/api/file/files/download');

      await expect(page.locator('.workflow-icon-upload-control .el-upload-list__item-thumbnail')).toBeVisible({ timeout: 10000 });
      const thumbnailSrc = await page.locator('.workflow-icon-upload-control .el-upload-list__item-thumbnail').first().getAttribute('src');
      expect(thumbnailSrc || '').toBeTruthy();
      expect(thumbnailSrc || '').not.toContain('/api/file/files/download');

      await page.getByRole('button', { name: '下一步' }).click();
      await page.getByPlaceholder('如 contract_apply_form').fill(`form_${keyword}`);

      const saveDraftResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/definitions')
        && response.request().method() === 'POST'
      );
      await page.getByRole('button', { name: '保存' }).click();
      const saveDraftResponse = await saveDraftResponsePromise;
      const saveDraftRequestBody = saveDraftResponse.request().postDataJSON() as { icon?: string };
      expect(saveDraftRequestBody.icon).toBeTruthy();
      expect(String(saveDraftRequestBody.icon)).not.toContain('/api/file/files/download');
      const saveDraftBody = await saveDraftResponse.json();
      expectApiSuccess(saveDraftBody, '保存流程图标草稿失败');
      await expect(page.getByText('保存成功')).toBeVisible({ timeout: 10000 });
      expect(protectedDownloadRequests).toEqual([]);
      await expectNoAuthError(page);
    } finally {
      await cleanupWorkflowUploadFiles(request, token, uploadedFileIds);
      await cleanupWorkflow(request, token, keyword).catch(() => undefined);
    }
  });

  test('创建流程进入独立三步设计工作台', async ({ page, request }) => {
    test.setTimeout(90_000);
    const pageErrors: string[] = [];
    const unique = Date.now();
    const keyword = `e2e_workflow_ui_${unique}`;
    const categoryName = `E2E设计分类${unique}`;
    const categoryCode = keyword;
    const definitionName = `E2E设计流程${unique}`;
    const definitionKey = `e2e_ui_process_${unique}`;
    const token = await loginToken(request, platformTenant);
    const headers = { Authorization: `Bearer ${token}` };
    page.on('pageerror', error => pageErrors.push(error.message));

    try {
      await cleanupWorkflow(request, token, keyword);

      const createCategoryResponse = await request.post(api(`/workflow/categories`), {
        headers,
        data: {
          categoryName,
          categoryCode,
          sort: 98,
          status: 1,
          remark: 'E2E设计工作台验证数据',
        },
      });
      expect(createCategoryResponse.status()).toBe(200);

      await loginPage(page, platformTenant);
      await openWorkflowManage(page);
      await expect(page.getByText('流程定义').first()).toBeVisible({ timeout: 10000 });
      await page.getByRole('button', { name: '创建流程' }).click();

      await expect(page.getByRole('navigation', { name: '工作流设计步骤' })).toBeVisible();
      await expect(page.getByRole('button', { name: /基础信息/ })).toBeVisible();
      await expect(page.getByRole('button', { name: /表单信息/ })).toBeVisible();
      await expect(page.getByRole('button', { name: /流程设计/ })).toBeVisible();

      await selectWorkflowCategory(page, categoryName);
      await page.getByPlaceholder('请输入流程名称').fill(definitionName);
      await page.getByPlaceholder('如 contract_approve').fill(definitionKey);
      await page.getByRole('button', { name: '下一步' }).click();

      await expect(page.getByRole('radio', { name: '内置设计器' })).toBeChecked();
      await expect(page.getByRole('radio', { name: '自定义页面' })).toBeVisible();
      await page.getByPlaceholder('如 contract_apply_form').fill(`form_${keyword}`);
      await expect(page.locator('.workflow-form-designer')).toBeVisible();
      await expect(page.locator('.workflow-form-designer')).toContainText('辅助组件');
      await expect(page.locator('.workflow-form-designer')).toContainText('子表单组件');
      await expect(page.locator('.workflow-form-designer')).toContainText('布局组件');
      await expect(page.locator('.workflow-form-designer')).toContainText('基础组件');
      await expect(page.locator('.workflow-form-designer')).toContainText('业务组件');
      await expect(page.locator('.workflow-form-designer')).toContainText('人员');
      await expect(page.locator('.workflow-form-designer')).toContainText('部门');
      await expect(page.locator('.workflow-form-designer')).toContainText('组织架构');
      await expect(page.locator('.workflow-form-designer')).toContainText('字典');
      await expect(page.locator('.workflow-form-designer')).toContainText('业务类型');
      await expect(page.locator('.workflow-form-designer')).toContainText('上传');
      await expect(page.locator('.workflow-form-designer')).toContainText('签名');
      await expect(page.locator('.workflow-form-designer')).toContainText('流水号');
      await expect(page.locator('.workflow-form-designer')).toContainText('流程金额');
      await expect(page.locator('.workflow-form-designer')).toContainText('流程类型');
      await page.getByRole('button', { name: '下一步' }).click();

      await expect(page.locator('.node-canvas')).toBeVisible();
      await expect(page.locator('.workflow-node-card.root', { hasText: '发起人' })).toBeVisible();
      await page.locator('.workflow-node-card.root', { hasText: '发起人' }).click();
      const nodePanel = page.locator('.workflow-node-property-panel');
      await expect(nodePanel).toBeVisible();
      await expect(nodePanel.getByRole('tab', { name: '基础' })).toBeVisible();
      await expect(nodePanel.getByRole('tab', { name: '表单权限' })).toBeVisible();
      await expect(nodePanel.getByRole('tab', { name: '节点属性' })).toBeVisible();
      await expect(nodePanel.getByRole('tab', { name: '高级' })).toBeVisible();
      await nodePanel.getByRole('tab', { name: '表单权限' }).click();
      await expect(nodePanel.locator('.form-permission-row', { hasText: '流程金额' })).toBeVisible();
      await expect(nodePanel.getByRole('radio', { name: '隐藏' }).first()).toBeVisible();
      await expect(nodePanel.getByRole('radio', { name: '只读' }).first()).toBeVisible();
      await expect(nodePanel.getByRole('radio', { name: '编辑' }).first()).toBeVisible();
      await nodePanel.getByRole('tab', { name: '节点属性' }).click();
      await expect(nodePanel.getByText('发起人范围')).toBeVisible();
      await nodePanel.locator('.workflow-participant-selector .participant-trigger').click();
      const participantDialog = page.locator('.participant-dialog', { hasText: '选择对象' });
      await expect(participantDialog).toBeVisible();
      await expect(participantDialog.getByRole('tab', { name: '用户' })).toBeVisible();
      await expect(participantDialog.getByRole('tab', { name: '部门范围' })).toBeVisible();
      await expect(participantDialog.getByRole('tab', { name: '岗位' })).toBeVisible();
      await expect(participantDialog.getByRole('tab', { name: '角色' })).toBeVisible();
      await expect(participantDialog.locator('.participant-item', { hasText: /admin/ }).first()).toBeVisible({ timeout: 10000 });
      await participantDialog.locator('.participant-item', { hasText: /admin/ }).first().click();
      await participantDialog.getByRole('tab', { name: '部门范围' }).click();
      await expect(participantDialog.locator('.participant-tree-wrap .el-tree')).toBeVisible({ timeout: 10000 });
      await participantDialog.getByRole('button', { name: '确认' }).click();
      await expect(nodePanel.locator('.participant-selected-group', { hasText: '用户：' })).toBeVisible();
      await nodePanel.getByRole('button', { name: '关闭节点配置' }).click();
      await expect(nodePanel).toBeHidden();
      const approvalNode = page.locator('.workflow-node-card:not(.root)', { hasText: '人工审批' });
      await expect(approvalNode).toBeVisible();
      await approvalNode.click();
      await expect(nodePanel).toBeVisible();
      await expect(nodePanel).toContainText('节点配置');
      await expect(nodePanel.getByRole('tab', { name: '基础' })).toBeVisible();
      await expect(nodePanel.getByRole('tab', { name: '表单权限' })).toBeVisible();
      await expect(nodePanel.getByRole('tab', { name: '审批设置' })).toBeVisible();
      await expect(nodePanel.getByRole('tab', { name: '节点能力' })).toBeVisible();
      await expect(nodePanel.getByRole('tab', { name: '高级' })).toBeVisible();
      await nodePanel.getByRole('tab', { name: '审批设置' }).click();
      await expect(nodePanel.getByText('指定成员')).toBeVisible();
      await expect(nodePanel.getByText('部门主管')).toBeVisible();
      await expect(nodePanel.getByText('角色')).toBeVisible();
      await expect(nodePanel.getByText('发起人自选')).toBeVisible();
      await expect(nodePanel.getByText('发起人自己')).toBeVisible();
      await expect(nodePanel.getByText('表单人员')).toBeVisible();
      await expect(nodePanel.getByText('流程表达式')).toBeVisible();
      await expect(nodePanel.getByText('指定岗位')).toBeVisible();
      await expect(nodePanel.getByText('指定组织')).toBeVisible();
      await expect(nodePanel.getByRole('button', { name: /审批人为空/ })).toBeVisible();
      await expect(nodePanel.getByRole('button', { name: /驳回策略/ })).toBeVisible();
      await nodePanel.getByRole('tab', { name: '表单权限' }).click();
      await expect(nodePanel.locator('.form-permission-row', { hasText: '流程金额' })).toBeVisible();
      await expect(nodePanel.getByRole('radio', { name: '隐藏' }).first()).toBeVisible();
      await expect(nodePanel.getByRole('radio', { name: '只读' }).first()).toBeVisible();
      await expect(nodePanel.getByRole('radio', { name: '编辑' }).first()).toBeVisible();
      await nodePanel.getByRole('tab', { name: '审批设置' }).click();
      await nodePanel.getByText('指定岗位').click();
      await expect(nodePanel.locator('.approval-target-select').first()).toBeVisible();
      await nodePanel.getByText('指定组织').click();
      await expect(nodePanel.locator('.approval-target-select').first()).toBeVisible();
      await nodePanel.getByText('发起人自选').click();
      await expect(nodePanel.getByRole('button', { name: /发起人自选/ })).toBeVisible();
      await expect(nodePanel.getByRole('button', { name: /多人审批/ })).toHaveCount(0);
      await nodePanel.getByRole('button', { name: /发起人自选/ }).click();
      await expect(nodePanel.getByText('单选')).toBeVisible();
      await nodePanel.getByText('多选').click();
      await expect(nodePanel.getByRole('button', { name: /多人审批/ })).toBeVisible();
      await nodePanel.getByText('发起人自己').click();
      await expect(nodePanel.getByRole('button', { name: /发起人自选/ })).toHaveCount(0);
      await expect(nodePanel.getByRole('button', { name: /多人审批/ })).toHaveCount(0);
      await nodePanel.getByText('指定成员').click();
      await nodePanel.locator('.approval-target-select').first().click();
      await expect(page.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: /admin/ })).toBeVisible({ timeout: 10000 });
      await page.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: /admin/ }).first().click();
      await expect(nodePanel.locator('.el-tag', { hasText: /admin/ })).toBeVisible();
      await nodePanel.getByRole('tab', { name: '高级' }).click();
      await nodePanel.getByRole('button', { name: '添加属性' }).click();
      await nodePanel.locator('.advanced-row').first().locator('.advanced-key input').fill('priority');
      await nodePanel.locator('.advanced-row').first().locator('.advanced-value input').fill('normal');
      await expect(nodePanel.locator('.advanced-row')).toHaveCount(1);
      await page.locator('.node-canvas').click({ position: { x: 48, y: 128 } });
      await expect(nodePanel).toBeHidden();
      await page.locator('.workflow-add-node-button').last().click();
      await page.getByRole('button', { name: /条件分支/ }).click();
      await expect(nodePanel).toBeVisible();
      await nodePanel.getByRole('button', { name: '关闭节点配置' }).click();
      await expect(nodePanel).toBeHidden();
      await expect(page.locator('.workflow-node-card.branch-node', { hasText: '分支1' })).toBeVisible();
      await expect(page.locator('.workflow-branch-box')).toBeVisible();
      await expect(page.locator('.workflow-branch-col')).toHaveCount(2);
      await expect(page.locator('.top-left-cover-line')).toBeVisible();
      await expect(page.locator('.bottom-right-cover-line')).toBeVisible();
      await page.locator('.workflow-node-card.branch-node', { hasText: '分支1' }).click();
      await expect(nodePanel).toBeVisible();
      await expect(nodePanel).toContainText('条件设置');
      await expect(nodePanel.getByRole('textbox', { name: '条件表达式' })).toHaveValue('${true}');
      await page.getByRole('button', { name: '添加条件' }).click();
      await page.locator('.condition-row').first().locator('.condition-variable').click();
      const visibleConditionOptions = page.locator('.el-select-dropdown:visible .el-select-dropdown__item');
      await expect(visibleConditionOptions.filter({ hasText: '当前机构ID' }).filter({ hasText: 'tenantId' })).toBeVisible();
      await expect(visibleConditionOptions.filter({ hasText: '申请人ID' }).filter({ hasText: 'applicant.id' })).toBeVisible();
      await expect(visibleConditionOptions.filter({ hasText: '申请人部门ID' }).filter({ hasText: 'applicant.orgId' })).toBeVisible();
      await expect(visibleConditionOptions.filter({ hasText: '申请人岗位ID' }).filter({ hasText: 'applicant.postId' })).toBeVisible();
      await visibleConditionOptions.filter({ hasText: '申请人部门ID' }).filter({ hasText: 'applicant.orgId' }).first().click();
      await expect(page.locator('.condition-row').first()).toContainText('选择部门');
      await page.locator('.condition-row').first().locator('.condition-value').click();
      await expect(page.locator('.el-tree-select__popper:visible').filter({ hasText: '芒果集团' }).first()).toBeVisible();
      await page.keyboard.press('Escape');
      await page.locator('.condition-row').first().locator('.condition-variable').click();
      await page.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: '申请人ID' }).filter({ hasText: 'applicant.id' }).first().click();
      await expect(page.locator('.condition-row').first()).toContainText('选择人员');
      await page.locator('.condition-row').first().locator('.condition-operator').click();
      await expect(page.getByRole('option', { name: '是', exact: true })).toBeVisible();
      await expect(page.getByRole('option', { name: '不是', exact: true })).toBeVisible();
      await expect(page.getByRole('option', { name: '属于/包含', exact: true })).toBeVisible();
      await expect(page.getByRole('option', { name: '不属于/不包含', exact: true })).toBeVisible();
      await page.keyboard.press('Escape');
      await page.locator('.condition-row').first().locator('.condition-variable').click();
      await visibleConditionOptions.filter({ hasText: '流程金额' }).first().click();
      await page.locator('.condition-row').first().locator('.condition-operator').click();
      await page.getByRole('option', { name: '等于 ==' }).click();
      await page.locator('.condition-row').first().getByPlaceholder('比较值').fill('1000');
      await page.getByRole('button', { name: '添加条件' }).click();
      await page.locator('.condition-row').nth(1).locator('.el-select').first().click();
      await page.getByRole('option', { name: '或者 OR' }).click();
      await page.locator('.condition-row').nth(1).locator('.condition-variable').click();
      await page.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: '当前机构ID' }).filter({ hasText: 'tenantId' }).first().click();
      await page.locator('.condition-row').nth(1).getByPlaceholder('比较值').fill('1');
      await page.locator('.condition-row').nth(1).getByPlaceholder('比较值').blur();
      await expect(nodePanel.getByRole('textbox', { name: '条件表达式' })).toHaveValue("${(amount == 1000 || tenantId == '1')}");
      await nodePanel.getByRole('button', { name: '关闭节点配置' }).click();
      await expect(nodePanel).toBeHidden();
      await expect(page.locator('.workflow-node-card.branch-node', { hasText: '分支1' })).toContainText('流程金额 是 1000');
      await expect(page.locator('.workflow-node-card.branch-node', { hasText: '分支1' })).toContainText('当前机构ID 是 1');
      await expect(page.locator('.workflow-node-card.branch-node', { hasText: '分支1' })).not.toContainText('${');
      await page.locator('.workflow-add-node-button').last().click();
      await page.getByRole('button', { name: /抄送节点/ }).click();
      await expect(nodePanel).toBeVisible();
      await expect(nodePanel.getByText('抄送对象')).toBeVisible();
      await nodePanel.locator('.workflow-participant-selector .participant-trigger').click();
      await expect(participantDialog).toBeVisible();
      await participantDialog.getByRole('tab', { name: '用户' }).click();
      await expect(participantDialog.locator('.participant-item', { hasText: /admin/ }).first()).toBeVisible({ timeout: 10000 });
      await participantDialog.locator('.participant-item', { hasText: /admin/ }).first().click();
      await participantDialog.getByRole('button', { name: '确认' }).click();
      await expect(nodePanel.locator('.participant-selected-group', { hasText: '用户：' })).toBeVisible();
      await nodePanel.getByPlaceholder('workflow.cc').fill('workflow.cc.e2e');
      await nodePanel.getByRole('button', { name: '关闭节点配置' }).click();
      await expect(nodePanel).toBeHidden();

      const saveDraftResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/definitions')
        && response.request().method() === 'POST'
      );
      await page.getByRole('button', { name: '保存' }).click();
      const saveDraftResponse = await saveDraftResponsePromise;
      const saveDraftBody = await saveDraftResponse.json();
      expect(saveDraftResponse.status(), JSON.stringify({
        request: saveDraftResponse.request().postDataJSON(),
        response: saveDraftBody,
      }, null, 2)).toBe(200);
      expect(saveDraftBody.success || saveDraftBody.code === 200).toBeTruthy();
      await expect(page.getByText('保存成功')).toBeVisible();

      await page.getByRole('button', { name: '发布流程' }).last().click();
      await expect(page.getByRole('dialog', { name: '发布前检查' })).toBeVisible();
      await expect(page.getByText('检查通过')).toBeVisible({ timeout: 10000 });
      const updateDefinitionResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/definitions')
        && response.request().method() === 'PUT'
      );
      const deployResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/definitions/deploy')
      );
      await page.getByRole('button', { name: '确认发布' }).click();
      const [updateDefinitionResponse, deployResponse] = await Promise.all([
        updateDefinitionResponsePromise,
        deployResponsePromise,
      ]);
      expect(updateDefinitionResponse.status()).toBe(200);
      const updateDefinitionBody = await updateDefinitionResponse.json();
      expect(updateDefinitionBody.success || updateDefinitionBody.code === 200).toBeTruthy();
      expect(deployResponse.status()).toBe(200);
      const deployBody = await deployResponse.json();
      expectApiSuccess(deployBody, '设计工作台流程部署失败');
      await expect(page.getByText('发布成功')).toBeVisible();
      await expect(page.getByRole('button', { name: '创建流程' })).toBeVisible({ timeout: 10000 });
      await expectNoAuthError(page);
      expect(pageErrors).toEqual([]);
    } finally {
      await cleanupWorkflow(request, token, keyword).catch(() => undefined);
    }
  });

  test('平台管理员可维护流程分类、流程定义并发布到 Flowable', async ({ page, request }) => {
    const unique = Date.now();
    const keyword = `e2e_workflow_${unique}`;
    const categoryName = `E2E流程分类${unique}`;
    const categoryCode = keyword;
    const domainCode = 'WORKFLOW';
    const definitionName = `E2E流程${unique}`;
    const definitionKey = `e2e_process_${unique}`;
    const token = await loginToken(request, platformTenant);
    const headers = { Authorization: `Bearer ${token}` };

    try {
      await cleanupWorkflow(request, token, keyword);

      const createCategoryResponse = await request.post(api(`/workflow/categories`), {
        headers,
        data: {
          categoryName,
          categoryCode,
          domainCode,
          sort: 99,
          status: 1,
          remark: 'E2E验证数据',
        },
      });
      expect(createCategoryResponse.status()).toBe(200);
      const createCategoryBody = await createCategoryResponse.json();
      expect(createCategoryBody.success || createCategoryBody.code === 200).toBeTruthy();
      const categoryId = createCategoryBody.data;

      const createDefinitionResponse = await request.post(api(`/workflow/definitions`), {
        headers,
        data: {
          categoryId,
          domainCode,
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

      const deployResponse = await request.post(api(`/workflow/definitions/deploy?id=${definitionId}`), {
        headers,
      });
      expect(deployResponse.status()).toBe(200);
      const deployBody = await deployResponse.json();
      expectApiSuccess(deployBody, '流程版本部署失败');
      expect(deployBody.data.deploymentId).toBeTruthy();
      expect(deployBody.data.processDefinitionId).toBeTruthy();
      expect(deployBody.data.versionNo).toBe(1);

      const versionsResponse = await request.get(api(`/workflow/definitions/versions?definitionId=${definitionId}`), {
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
      await page.getByRole('button', { name: '管理分类' }).click();
      const categoryManageDialog = page.getByRole('dialog', { name: '流程分类管理' });
      await expect(categoryManageDialog).toBeVisible();
      await expect(categoryManageDialog.getByRole('button', { name: '新增分类' })).toBeVisible();
      await expect(categoryManageDialog.locator('.el-table')).toContainText(categoryName);
      await page.keyboard.press('Escape');
      await expect(categoryManageDialog).toBeHidden();
      await page.getByRole('heading', { name: '全部流程定义' }).click();
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

  test('流程可生成模板，模板可按业务域和模板分类筛选且可导入流程', async ({ page, request }) => {
    test.setTimeout(90_000);
    const unique = Date.now();
    const keyword = `e2e_workflow_template_${unique}`;
    const domainCode = 'WORKFLOW';
    const categoryName = `E2E模板目标分类${unique}`;
    const categoryCode = keyword;
    const templateCategoryName = `E2E模板分类${unique}`;
    const templateCategoryCode = `template_${keyword}`;
    const definitionName = `E2E模板来源流程${unique}`;
    const definitionKey = `e2e_template_source_${unique}`;
    const templateName = `E2E流程模板${unique}`;
    const templateCode = keyword;
    const token = await loginToken(request, platformTenant);
    const headers = { Authorization: `Bearer ${token}` };

    try {
      await cleanupWorkflow(request, token, keyword);
      await cleanupWorkflowTemplates(request, token, keyword);

      const createCategoryResponse = await request.post(api(`/workflow/categories`), {
        headers,
        data: {
          categoryName,
          categoryCode,
          domainCode,
          sort: 98,
          status: 1,
          remark: 'E2E模板导入目标分类',
        },
      });
      expect(createCategoryResponse.status()).toBe(200);
      const createCategoryBody = await createCategoryResponse.json();
      expectApiSuccess(createCategoryBody, '创建流程分类失败');
      const categoryId = createCategoryBody.data;

      const createTemplateCategoryResponse = await request.post(api(`/workflow/template-categories`), {
        headers,
        data: {
          categoryName: templateCategoryName,
          categoryCode: templateCategoryCode,
          icon: 'CollectionTag',
          sort: 98,
          status: 1,
          remark: 'E2E模板分类',
        },
      });
      expect(createTemplateCategoryResponse.status()).toBe(200);
      const createTemplateCategoryBody = await createTemplateCategoryResponse.json();
      expectApiSuccess(createTemplateCategoryBody, '创建流程模板分类失败');
      const templateCategoryId = createTemplateCategoryBody.data;

      const createDefinitionResponse = await request.post(api(`/workflow/definitions`), {
        headers,
        data: {
          categoryId,
          domainCode,
          definitionName,
          definitionKey,
          designerJson: approvalDesignerJson(unique),
          formCode: `form_${keyword}`,
          formJson: leaveFormJson(),
          status: 'DRAFT',
          remark: 'E2E模板来源流程',
        },
      });
      expect(createDefinitionResponse.status()).toBe(200);
      const createDefinitionBody = await createDefinitionResponse.json();
      expectApiSuccess(createDefinitionBody, '创建模板来源流程失败');
      const definitionId = createDefinitionBody.data;

      const createTemplateResponse = await request.post(api(`/workflow/templates/from-definition`), {
        headers,
        data: {
          definitionId,
          templateName,
          templateCode,
          templateCategoryId,
          categoryCode: domainCode,
          categoryName: '工作流域',
          remark: 'E2E由流程生成模板',
        },
      });
      expect(createTemplateResponse.status()).toBe(200);
      const createTemplateBody = await createTemplateResponse.json();
      expectApiSuccess(createTemplateBody, '流程生成模板失败');
      const templateId = createTemplateBody.data;

      const pageResponse = await request.get(api(`/workflow/templates/page?page=1&size=10&categoryCode=${domainCode}&templateCategoryId=${templateCategoryId}&keyword=${keyword}`), {
        headers,
      });
      expect(pageResponse.status()).toBe(200);
      const pageBody = await pageResponse.json();
      expectApiSuccess(pageBody, '按业务域和模板分类查询流程模板失败');
      expect(pageBody.data?.list?.[0]?.templateCategoryName).toBe(templateCategoryName);
      expect(pageBody.data?.list?.[0]?.categoryCode).toBe(domainCode);

      await loginPage(page, platformTenant);
      const templatePageResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/templates/page')
        && response.url().includes(`categoryCode=${domainCode}`)
        && response.status() === 200
      );
      const templateCategoryListResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/template-categories/list')
        && response.status() === 200
      );
      await page.goto('/#/workflow/manage/template');
      const templateCategoryListResponse = await templateCategoryListResponsePromise;
      const templateCategoryListBody = await templateCategoryListResponse.json();
      expectApiSuccess(templateCategoryListBody, '查询流程模板分类选项失败');
      expect((templateCategoryListBody.data || []).some((item: any) => item.categoryName === templateCategoryName)).toBeTruthy();
      await page.getByRole('button', { name: /工作流域 WORKFLOW/ }).click();
      await templatePageResponsePromise;
      await page.getByPlaceholder('模板名称/编码/场景').fill(keyword);
      await expect(page.locator('.filter-form .el-form-item', { hasText: '模板分类' })).toBeVisible();
      await page.getByRole('button', { name: '管理分类' }).click();
      const templateCategoryManageDialog = page.getByRole('dialog', { name: '模板分类管理' });
      await expect(templateCategoryManageDialog).toBeVisible();
      await expect(templateCategoryManageDialog.getByRole('button', { name: '新增分类' })).toBeVisible();
      await expect(templateCategoryManageDialog.locator('.el-table')).toContainText(templateCategoryName);
      await page.keyboard.press('Escape');
      await expect(templateCategoryManageDialog).toBeHidden();
      const templateRow = page.locator('.el-table__row', { hasText: templateName }).first();
      await expect(templateRow).toBeVisible();
      await expect(templateRow).toContainText(templateCategoryName);
      await expect(templateRow).toContainText(domainCode);

      const singleImportKey = `${templateCode}_single`;
      const singleImportResponse = await request.post(api(`/workflow/templates/create-definition`), {
        headers,
        data: {
          templateId,
          categoryId,
          domainCode,
          targetTenantId: '1',
          orgId: '1',
          definitionName: `${templateName}单个导入`,
          definitionKey: singleImportKey,
          remark: 'E2E模板单个导入',
        },
      });
      expect(singleImportResponse.status()).toBe(200);
      const singleImportBody = await singleImportResponse.json();
      expectApiSuccess(singleImportBody, '模板单个导入流程失败');

      const importedDetailResponse = await request.get(api(`/workflow/definitions/detail?id=${singleImportBody.data}`), {
        headers,
      });
      expect(importedDetailResponse.status()).toBe(200);
      const importedDetailBody = await importedDetailResponse.json();
      expectApiSuccess(importedDetailBody, '查询模板导入流程失败');
      expect(importedDetailBody.data.sourceTemplateId).toBe(templateId);
      expect(importedDetailBody.data.sourceTemplateCode).toBe(templateCode);
      expect(importedDetailBody.data.orgId).toBe('1');

      const batchImportResponse = await request.post(api(`/workflow/templates/import`), {
        headers,
        data: {
          categoryId,
          domainCode,
          targetTenantId: '1',
          orgId: '1',
          templateCategoryId,
        },
      });
      expect(batchImportResponse.status()).toBe(200);
      const batchImportBody = await batchImportResponse.json();
      expectApiSuccess(batchImportBody, '按模板分类批量导入失败');
      expect(batchImportBody.data.definitionIds).toHaveLength(1);

      const duplicateImportResponse = await request.post(api(`/workflow/templates/import`), {
        headers,
        data: {
          categoryId,
          domainCode,
          targetTenantId: '1',
          orgId: '1',
          templateCategoryId,
        },
      });
      expect(duplicateImportResponse.status()).toBe(200);
      const duplicateImportBody = await duplicateImportResponse.json();
      expect(duplicateImportBody.success).toBeFalsy();
      expect(duplicateImportBody.msg).toContain('同编码流程');
      expect(duplicateImportBody.data.errors?.[0]?.templateCode).toBe(templateCode);
    } finally {
      await cleanupWorkflow(request, token, keyword).catch(() => undefined);
      await cleanupWorkflowTemplates(request, token, keyword).catch(() => undefined);
    }
  });

  test('平台管理员可从发起流程页面提交已发布流程并在我的发起回显', async ({ page, request }) => {
    const unique = Date.now();
    const keyword = `e2e_workflow_start_${unique}`;
    const categoryName = `E2E发起分类${unique}`;
    const categoryCode = keyword;
    const definitionName = `E2E请假流程${unique}`;
    const definitionKey = `e2e_leave_${unique}`;
    const token = await loginToken(request, platformTenant);
    const headers = { Authorization: `Bearer ${token}` };

    try {
      await cleanupWorkflow(request, token, keyword);

      const createCategoryResponse = await request.post(api(`/workflow/categories`), {
        headers,
        data: {
          categoryName,
          categoryCode,
          domainCode: defaultWorkflowDomainCode,
          sort: 97,
          status: 1,
          remark: 'E2E发起流程验证数据',
        },
      });
      expect(createCategoryResponse.status()).toBe(200);
      const createCategoryBody = await createCategoryResponse.json();
      expect(createCategoryBody.success || createCategoryBody.code === 200).toBeTruthy();

      const createDefinitionResponse = await request.post(api(`/workflow/definitions`), {
        headers,
        data: {
          categoryId: createCategoryBody.data,
          domainCode: defaultWorkflowDomainCode,
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

      const deployResponse = await request.post(api(`/workflow/definitions/deploy?id=${createDefinitionBody.data}`), {
        headers,
      });
      expect(deployResponse.status()).toBe(200);
      const deployBody = await deployResponse.json();
      expectApiSuccess(deployBody, '审批闭环流程部署失败');
      expect(deployBody.data.processDefinitionId).toBeTruthy();

      await loginPage(page, platformTenant);
      const definitionsResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/definitions/page') && response.status() === 200
      );
      await openStartProcess(page);
      await definitionsResponsePromise;
      await page.getByPlaceholder('搜索流程名称/编码').fill(definitionName);
      await page.getByRole('button', { name: '查询' }).click();
      const launchCard = startProcessCard(page, definitionName);
      await expect(launchCard).toBeVisible({ timeout: 10000 });

      await launchCard.click();
      await expect(startProcessDialog(page, definitionName)).toBeVisible();
      await expect(page.getByText('流程表单')).toBeVisible();
      await page.getByPlaceholder('请输入请假天数').fill('2');
      await page.getByPlaceholder('请输入请假原因').fill('E2E 请假发起验证');

      const startResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/processes/start') && response.status() === 200
      );
      await page.getByRole('button', { name: '确认发起' }).click();
      const startResponse = await startResponsePromise;
      const startBody = await startResponse.json();
      expect(startBody.success || startBody.code === 200).toBeTruthy();
      const startedBusinessKey = startBody.data.businessKey as string;
      expect(startedBusinessKey).toBeTruthy();
      await expect(page.getByText(/流程已发起/)).toBeVisible({ timeout: 10000 });

      const initiatedResponsePromise = waitForInitiatedTasksLoad(page);
      await openInitiatedTasks(page);
      await initiatedResponsePromise;
      await expect(page.locator('.el-table__row', { hasText: startedBusinessKey })).toBeVisible({ timeout: 10000 });
      await expect(page.locator('.el-table__row', { hasText: startedBusinessKey })).toContainText(definitionKey);
      await expectNoAuthError(page);
    } finally {
      await cleanupWorkflow(request, token, keyword).catch(() => undefined);
    }
  });

  test('发起流程弹窗可渲染布局、业务选择和上传类动态表单组件', async ({ page, request }) => {
    test.setTimeout(90_000);
    const unique = Date.now();
    const keyword = `e2e_workflow_runtime_form_${unique}`;
    const categoryName = `E2E表单组件分类${unique}`;
    const categoryCode = keyword;
    const definitionName = `E2E费用报销流程${unique}`;
    const definitionKey = `e2e_runtime_form_${unique}`;
    const token = await loginToken(request, platformTenant);
    const headers = { Authorization: `Bearer ${token}` };
    const uploadedFileIds: string[] = [];

    try {
      await cleanupWorkflow(request, token, keyword);

      const createCategoryResponse = await request.post(api(`/workflow/categories`), {
        headers,
        data: {
          categoryName,
          categoryCode,
          domainCode: defaultWorkflowDomainCode,
          sort: 96,
          status: 1,
          remark: 'E2E动态表单组件渲染验证数据',
        },
      });
      expect(createCategoryResponse.status()).toBe(200);
      const createCategoryBody = await createCategoryResponse.json();
      expect(createCategoryBody.success || createCategoryBody.code === 200).toBeTruthy();

      const createDefinitionResponse = await request.post(api(`/workflow/definitions`), {
        headers,
        data: {
          categoryId: createCategoryBody.data,
          domainCode: defaultWorkflowDomainCode,
          definitionName,
          definitionKey,
          designerJson: designerJson(unique),
          formCode: `form_${keyword}`,
          formJson: runtimeComponentFormJson(),
          status: 'DRAFT',
          remark: 'E2E动态表单组件渲染验证数据',
        },
      });
      expect(createDefinitionResponse.status()).toBe(200);
      const createDefinitionBody = await createDefinitionResponse.json();
      expect(createDefinitionBody.success || createDefinitionBody.code === 200).toBeTruthy();

      const deployResponse = await request.post(api(`/workflow/definitions/deploy?id=${createDefinitionBody.data}`), {
        headers,
      });
      expect(deployResponse.status()).toBe(200);
      const deployBody = await deployResponse.json();
      expectApiSuccess(deployBody, '动态表单流程部署失败');
      expect(deployBody.data.processDefinitionId).toBeTruthy();

      await loginPage(page, platformTenant);
      const definitionsResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/definitions/page') && response.status() === 200
      );
      await openStartProcess(page);
      await definitionsResponsePromise;
      await page.getByPlaceholder('搜索流程名称/编码').fill(definitionName);
      await page.getByRole('button', { name: '查询' }).click();
      const launchCard = startProcessCard(page, definitionName);
      await expect(launchCard).toBeVisible({ timeout: 10000 });

      await launchCard.click();
      const dialog = startProcessDialog(page, definitionName);
      await expect(dialog).toBeVisible();
      await expect(dialog.locator('.runtime-container-title', { hasText: '申请信息' })).toBeVisible();
      await expect(dialog.locator('.runtime-container-title', { hasText: '金额信息' })).toBeVisible();
      await expect(dialog.locator('.el-alert__title', { hasText: '请按实际业务上传审批附件。' })).toBeVisible();
      await expect(dialog.locator('.mango-file-upload')).toHaveCount(2);
      await expect(dialog.locator('.mango-file-upload.is-thumbnail')).toHaveCount(1);

      const attachmentPath = join(tmpdir(), `mango-workflow-attachment-${unique}.pdf`);
      const imagePath = join(tmpdir(), `mango-workflow-image-${unique}.png`);
      writeFileSync(attachmentPath, Buffer.from('%PDF-1.4\n% e2e workflow attachment\n', 'utf-8'));
      writeFileSync(imagePath, Buffer.from(
        'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=',
        'base64',
      ));

      const attachmentUploadResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/file/files')
        && response.request().method() === 'POST'
        && response.status() === 200
      );
      await dialog.locator('.el-form-item', { hasText: '附件' }).locator('input[type="file"]').setInputFiles(attachmentPath);
      const attachmentUploadResponse = await attachmentUploadResponsePromise;
      const attachmentUploadBody = await attachmentUploadResponse.json();
      expect(attachmentUploadBody.success || attachmentUploadBody.code === 200).toBeTruthy();
      const attachmentFileId = String(attachmentUploadBody.data.id);
      expect(attachmentFileId).toBeTruthy();
      uploadedFileIds.push(attachmentFileId);

      const imageUploadResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/file/files')
        && response.request().method() === 'POST'
        && response.status() === 200
      );
      await dialog.locator('.el-form-item', { hasText: '图片' }).locator('input[type="file"]').setInputFiles(imagePath);
      const imageUploadResponse = await imageUploadResponsePromise;
      const imageUploadBody = await imageUploadResponse.json();
      expect(imageUploadBody.success || imageUploadBody.code === 200).toBeTruthy();
      const imageFileId = String(imageUploadBody.data.id);
      expect(imageFileId).toBeTruthy();
      uploadedFileIds.push(imageFileId);

      await dialog.getByPlaceholder('请输入申请主题').fill('E2E 费用报销申请');
      await dialog.locator('.el-form-item', { hasText: '费用类型' }).locator('.el-select__wrapper').click();
      await page.getByRole('option', { name: '差旅费' }).click();
      await dialog.getByRole('spinbutton', { name: /申请金额/ }).fill('1280');
      await dialog.locator('.el-form-item', { hasText: '申请部门' }).locator('.el-select__wrapper').click();
      await page.getByRole('option', { name: '芒果集团' }).click();
      await dialog.locator('.el-form-item', { hasText: '费用字典' }).locator('.dict-select .el-select__wrapper').click();
      const dictDropdown = page.locator('.el-select-dropdown:visible');
      await expect(dictDropdown.getByRole('option', { name: '启用' })).toBeVisible({ timeout: 10000 });
      await dictDropdown.getByRole('option', { name: '启用' }).click();
      const signatureCanvas = dialog.locator('.el-form-item', { hasText: '申请人签字' }).locator('canvas');
      await expect(signatureCanvas).toBeVisible();
      const box = await signatureCanvas.boundingBox();
      expect(box).toBeTruthy();
      await page.mouse.move(box!.x + 24, box!.y + 36);
      await page.mouse.down();
      await page.mouse.move(box!.x + 120, box!.y + 72);
      await page.mouse.move(box!.x + 180, box!.y + 44);
      await page.mouse.up();

      const startResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/processes/start') && response.status() === 200
      );
      await page.getByRole('button', { name: '确认发起' }).click();
      const startResponse = await startResponsePromise;
      const startRequestBody = JSON.parse(startResponse.request().postData() || '{}');
      expectWorkflowUploadValueOnlyContainsFileIds(startRequestBody.variables.attachments, [attachmentFileId]);
      expectWorkflowUploadValueOnlyContainsFileIds(startRequestBody.variables.images, [imageFileId]);
      expect(startRequestBody.variables.expenseCategoryDict).toBeTruthy();
      expect(String(startRequestBody.variables.applicantSignature)).toMatch(/^data:image\/png;base64,/);
      const startBody = await startResponse.json();
      expect(startBody.success || startBody.code === 200).toBeTruthy();
      expect(startBody.data.businessKey).toBeTruthy();
      await expect(page.getByText(/流程已发起/)).toBeVisible({ timeout: 10000 });
      await expectNoAuthError(page);
    } finally {
      await cleanupWorkflowUploadFiles(request, token, uploadedFileIds);
      await cleanupWorkflow(request, token, keyword).catch(() => undefined);
    }
  });

  test('发起人自选审批人使用成员选择器并进入所选人待办', async ({ page, request }) => {
    test.setTimeout(60000);
    const unique = Date.now();
    const keyword = `e2e_workflow_select_${unique}`;
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
      const launchCard = startProcessCard(page, workflow.definitionName);
      await expect(launchCard).toBeVisible({ timeout: 10000 });

      await launchCard.click();
      const startDialog = startProcessDialog(page, workflow.definitionName);
      await expect(startDialog).toBeVisible();
      await expect(startDialog.getByText('发起人自选审批人')).toBeVisible();
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
      const startedBusinessKey = startBody.data.businessKey as string;
      expect(startedBusinessKey).toBeTruthy();
      await expect(page.getByText(/流程已发起/)).toBeVisible({ timeout: 10000 });

      const todoResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/tasks/todo') && response.status() === 200
      );
      await openTodoTasks(page);
      await todoResponsePromise;
      await expect(page.locator('.el-table__row', { hasText: startedBusinessKey })).toBeVisible({ timeout: 10000 });
      await expect(page.locator('.el-table__row', { hasText: startedBusinessKey })).toContainText('发起人自选审批');
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
      await expectReadonlyFieldValue(page, '请假天数', '5');
      await expectReadonlyFieldValue(page, '请假原因', 'E2E 发起人自己审批验证');
      await expectWorkflowTaskDetailSidebar(page, '发起人自己审批');

      await page.getByPlaceholder('请输入审批意见').fill('本人确认通过');
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
      await expect(page.getByText('请假天数')).toBeVisible();
      await expectReadonlyFieldValue(page, '请假天数', '3');
      await expectReadonlyFieldValue(page, '请假原因', 'E2E 审批通过验证');
      await expectWorkflowTaskDetailSidebar(page, '主管审批');

      await page.getByPlaceholder('请输入审批意见').fill('同意，UI E2E');
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

  test('平台管理员可在我的待办驳回审批并在我的发起查看已驳回详情', async ({ page, request }) => {
    test.setTimeout(60000);
    const unique = Date.now();
    const keyword = `e2e_workflow_reject_${unique}`;
    const businessKey = `leave-reject-${unique}`;
    const token = await loginToken(request, platformTenant);

    try {
      await cleanupWorkflow(request, token, keyword);
      const workflow = await prepareLeaveWorkflow(request, token, unique, keyword);
      await startLeaveProcess(request, token, workflow.definitionId, businessKey, 1, 'E2E 审批驳回验证');

      await loginPage(page, platformTenant);
      const todoResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/tasks/todo') && response.status() === 200
      );
      await openTodoTasks(page);
      await todoResponsePromise;
      await expect(page.locator('.el-table__row', { hasText: businessKey })).toBeVisible({ timeout: 10000 });

      await page.locator('.el-table__row', { hasText: businessKey }).getByRole('button', { name: '处理' }).click();
      await page.waitForURL('**/#/workflow/task/detail**', { timeout: 10000 });
      await expectReadonlyFieldValue(page, '请假原因', 'E2E 审批驳回验证');
      await page.getByPlaceholder('请输入审批意见').fill('驳回，UI E2E');
      const rejectResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/tasks/reject') && response.status() === 200
      );
      await page.getByRole('button', { name: '驳回' }).click();
      await page.getByRole('dialog', { name: '审批驳回' }).getByRole('button', { name: /^(OK|确定)$/ }).click();
      const rejectResponse = await rejectResponsePromise;
      const rejectBody = await rejectResponse.json();
      expect(rejectBody.success || rejectBody.code === 200).toBeTruthy();

      const initiatedResponsePromise = waitForInitiatedTasksLoad(page);
      await openInitiatedTasks(page);
      await initiatedResponsePromise;
      await expect(page.locator('.el-table__row', { hasText: businessKey })).toBeVisible({ timeout: 10000 });
      await expect(page.locator('.el-table__row', { hasText: businessKey })).toContainText('已驳回');

      const processDetailResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/processes/detail') && response.status() === 200
      );
      await page.locator('.el-table__row', { hasText: businessKey }).getByRole('button', { name: '查看' }).click();
      await page.waitForURL('**/#/workflow/task/detail**', { timeout: 10000 });
      await processDetailResponsePromise;
      await expect(page.locator('.workflow-task-detail-page')).toContainText('E2E 审批驳回验证');
      await expect(page.locator('.workflow-task-detail-page')).toContainText('驳回');
      await expect(page.locator('.workflow-task-detail-page')).toContainText('admin');
      await expect(page.getByText('驳回，UI E2E')).toBeVisible();
      await expectNoAuthError(page);
    } finally {
      await cleanupWorkflow(request, token, keyword).catch(() => undefined);
    }
  });

  test('审批动作按钮真实执行：认领、释放、暂存、转办、通过和抄送已阅', async ({ page, request }) => {
    test.setTimeout(120_000);
    const unique = Date.now();
    const keyword = `e2e_workflow_action_${unique}`;
    const businessKey = `ACTION-E2E-${unique}`;
    const transferUserName = `E2E_ACTION_TRANSFER_${unique}`;
    const token = await loginToken(request, platformTenant);
    const headers = { Authorization: `Bearer ${token}` };

    try {
      cleanupWorkflowActionData(businessKey);
      await cleanupWorkflow(request, token, keyword);
      await cleanupUser(request, token, transferUserName);

      const transferUser = await createTempUser(request, token, transferUserName);
      await assignSubjectRoles(request, token, transferUser.memberId, [1]);
      const roleId = '1';
      const workflow = await prepareActionCapabilityWorkflow(request, token, unique, keyword, roleId);
      await startLeaveProcess(request, token, workflow.definitionId, businessKey, 2, 'E2E 动作初始原因');

      let tasks = await listTodoTasks(request, token, businessKey);
      let actionTask = tasks.find((item: any) => String(item.businessKey) === businessKey && String(item.taskName).includes('动作审批'));
      expect(actionTask, '候选动作审批待办未生成').toBeTruthy();
      expect(String(actionTask.assigneeName || '')).toBe('');

      await loginPage(page, platformTenant);
      const todoResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/tasks/todo') && response.status() === 200
      );
      await openTodoTasks(page);
      await todoResponsePromise;
      await openClaimableTodoTab(page, businessKey);
      const taskRow = page.locator('.el-table__row', { hasText: businessKey });
      await expect(taskRow).toBeVisible({ timeout: 10000 });
      await expect(taskRow).toContainText('动作审批');

      await taskRow.getByRole('button', { name: '详情' }).click();
      await page.waitForURL('**/#/workflow/task/detail**', { timeout: 10000 });
      await expect(page.getByRole('button', { name: '认领' })).toBeVisible({ timeout: 10000 });
      await expect(page.getByRole('button', { name: '暂存' })).toBeVisible();
      await expect(page.getByRole('button', { name: '转办' })).toBeVisible();
      await expect(page.getByRole('button', { name: '加签' })).toBeVisible();
      await expect(page.getByRole('button', { name: '驳回' })).toBeVisible();
      await expect(page.getByRole('button', { name: '通过' })).toBeVisible();

      const claimResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/tasks/claim') && response.status() === 200
      );
      await page.getByRole('button', { name: '认领' }).click();
      await page.getByRole('dialog', { name: '审批认领' }).getByRole('button', { name: /^(OK|确定)$/ }).click();
      const claimBody = await (await claimResponsePromise).json();
      expectApiSuccess(claimBody, '认领失败');
      await page.waitForURL('**/#/workflow/task/todo', { timeout: 10000 });
      await page.reload();

      await expect(page.locator('.el-table__row', { hasText: businessKey })).toContainText('admin', { timeout: 10000 });
      actionTask = await findTodoTask(request, token, businessKey, '动作审批');
      expect(String(actionTask.assigneeName)).toBe('admin');

      await page.locator('.el-table__row', { hasText: businessKey }).getByRole('button', { name: '处理' }).click();
      await page.waitForURL('**/#/workflow/task/detail**', { timeout: 10000 });
      await expect(page.getByRole('button', { name: '释放' })).toBeVisible({ timeout: 10000 });
      const unclaimResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/tasks/unclaim') && response.status() === 200
      );
      await page.getByRole('button', { name: '释放' }).click();
      await page.getByRole('dialog', { name: '审批释放' }).getByRole('button', { name: /^(OK|确定)$/ }).click();
      const unclaimBody = await (await unclaimResponsePromise).json();
      expectApiSuccess(unclaimBody, '释放失败');
      await page.waitForURL('**/#/workflow/task/todo', { timeout: 10000 });
      await page.reload();

      await openClaimableTodoTab(page, businessKey);
      actionTask = await findTodoTask(request, token, businessKey, '动作审批');
      expect(String(actionTask.assigneeName || '')).toBe('');

      await page.locator('.el-table__row', { hasText: businessKey }).getByRole('button', { name: '详情' }).click();
      await page.waitForURL('**/#/workflow/task/detail**', { timeout: 10000 });
      const detailResponse = await request.get(api(`/workflow/tasks/detail`), {
        headers,
        params: { taskId: actionTask.id },
      });
      expect(detailResponse.status()).toBe(200);
      const detailBody = await detailResponse.json();
      expectApiSuccess(detailBody, '查询动作任务详情失败');
      expect(detailBody.data.renderConfig.nodeActions.save).toMatchObject({ enabled: true, label: '暂存' });
      expect(detailBody.data.renderConfig.nodeActions.transfer).toMatchObject({ enabled: true, label: '转办', requireComment: true });
      expect(detailBody.data.renderConfig.nodeActions.addSign).toMatchObject({ enabled: true, label: '加签' });
      expect(detailBody.data.renderConfig.nodeActions.reject).toMatchObject({ enabled: true, label: '驳回', requireComment: true });
      expect(detailBody.data.renderConfig.nodeActions.complete).toMatchObject({ enabled: true, label: '通过' });

      await page.getByRole('textbox', { name: '请假原因' }).fill('E2E 暂存后原因');
      await page.getByPlaceholder('请输入审批意见').fill('暂存草稿');
      const saveResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/tasks/save') && response.status() === 200
      );
      await page.getByRole('button', { name: '暂存' }).click();
      await page.getByRole('dialog', { name: '审批暂存' }).getByRole('button', { name: /^(OK|确定)$/ }).click();
      const saveResponse = await saveResponsePromise;
      const saveRequest = saveResponse.request().postDataJSON() as any;
      expect(saveRequest.variables.reason).toBe('E2E 暂存后原因');
      expectApiSuccess(await saveResponse.json(), '暂存失败');
      await expect(page.getByText('暂存成功')).toBeVisible({ timeout: 10000 });
      await expect(page.getByRole('textbox', { name: '请假原因' })).toHaveValue('E2E 暂存后原因');

      await page.getByPlaceholder('请输入审批意见').fill('');
      await page.getByRole('button', { name: '转办' }).click();
      await expect(page.getByText('请填写审批意见')).toBeVisible({ timeout: 10000 });

      await page.getByPlaceholder('请输入审批意见').fill('转给临时办理人');
      const transferResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/tasks/transfer') && response.status() === 200
      );
      await page.getByRole('button', { name: '转办' }).click();
      await pickUserFromDialog(page, '选择转办人员', '选择转办人员', transferUserName);
      await page.getByRole('dialog', { name: '审批转办' }).getByRole('button', { name: /^(OK|确定)$/ }).click();
      const transferBody = await (await transferResponsePromise).json();
      expectApiSuccess(transferBody, '转办失败');
      await page.waitForURL('**/#/workflow/task/done', { timeout: 10000 });
      await page.reload();

      const transferredUserToken = await loginTokenAs(request, platformTenant, transferUserName, 'E2E@123456');
      const transferredTask = await findTodoTask(request, transferredUserToken, businessKey, '动作审批');
      expect(String(transferredTask.assigneeName)).toBe(transferUserName);

      await completeTask(request, transferredUserToken, String(transferredTask.id), '临时办理人通过');
      const copied = await waitForCopiedTask(request, token, businessKey);
      expect(String(copied.status)).toBe('待阅');

      const copiedResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/tasks/copied') && response.status() === 200
      );
      await openCopiedTasks(page);
      await copiedResponsePromise;
      const copiedRow = page.locator('.el-table__row', { hasText: businessKey });
      await expect(copiedRow).toBeVisible({ timeout: 10000 });
      await expect(copiedRow).toContainText('待阅');
      const readResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/tasks/copied/read') && response.status() === 200
      );
      await copiedRow.getByRole('button', { name: '已阅' }).click();
      const readBody = await (await readResponsePromise).json();
      expectApiSuccess(readBody, '抄送已阅失败');
      await expect(page.getByText('已标记为已阅')).toBeVisible({ timeout: 10000 });
      await expect(page.locator('.el-table__row', { hasText: businessKey })).toContainText('已阅', { timeout: 10000 });
      const copiedAfterRead = await readCopiedList(request, token, businessKey);
      expect(copiedAfterRead.find((row: any) => String(row.businessKey) === businessKey)?.status).toBe('已阅');
      await expectNoAuthError(page);
    } finally {
      cleanupWorkflowActionData(businessKey);
      await cleanupWorkflow(request, token, keyword).catch(() => undefined);
      await cleanupUser(request, token, transferUserName).catch(() => undefined);
    }
  });

  test('会签多实例任务支持加签并生成新增办理人待办', async ({ page, request }) => {
    test.setTimeout(120_000);
    const unique = Date.now();
    const keyword = `e2e_workflow_add_sign_${unique}`;
    const businessKey = `ADD-SIGN-E2E-${unique}`;
    const firstUserName = `E2E_ADD_FIRST_${unique}`;
    const addedUserName = `E2E_ADD_TARGET_${unique}`;
    const token = await loginToken(request, platformTenant);

    try {
      cleanupWorkflowActionData(businessKey);
      await cleanupWorkflow(request, token, keyword);
      await cleanupUser(request, token, firstUserName);
      await cleanupUser(request, token, addedUserName);

      const firstUser = await createTempUser(request, token, firstUserName);
      const addedUser = await createTempUser(request, token, addedUserName);
      await assignSubjectRoles(request, token, firstUser.memberId, [1]);
      await assignSubjectRoles(request, token, addedUser.memberId, [1]);
      const workflow = await prepareActionAddSignWorkflow(request, token, unique, keyword, ['admin', firstUserName]);
      await startLeaveProcess(request, token, workflow.definitionId, businessKey, 1, 'E2E 加签初始原因');

      const firstUserToken = await loginTokenAs(request, platformTenant, firstUserName, 'E2E@123456');
      const addedUserToken = await loginTokenAs(request, platformTenant, addedUserName, 'E2E@123456');
      const adminTaskBeforeAddSign = await findTodoTask(request, token, businessKey, '加签审批');
      const firstUserTaskBeforeAddSign = await findTodoTask(request, firstUserToken, businessKey, '加签审批');
      expect(String(adminTaskBeforeAddSign.assigneeName)).toBe('admin');
      expect(String(firstUserTaskBeforeAddSign.assigneeName)).toBe(firstUserName);

      await loginPage(page, platformTenant);
      await openTodoTasks(page);
      await expect(page.locator('.el-table__row', { hasText: businessKey })).toBeVisible({ timeout: 10000 });
      await page.locator('.el-table__row', { hasText: businessKey }).first().getByRole('button', { name: '处理' }).click();
      await page.waitForURL('**/#/workflow/task/detail**', { timeout: 10000 });
      await expect(page.getByRole('button', { name: '加签' })).toBeVisible({ timeout: 10000 });

      const addSignResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/tasks/add-sign') && response.status() === 200
      );
      await page.getByRole('button', { name: '加签' }).click();
      await pickUserFromDialog(page, '选择加签人员', '选择加签人员', addedUserName);
      await page.getByRole('dialog', { name: '审批加签' }).getByRole('button', { name: /^(OK|确定)$/ }).click();
      const addSignResponse = await addSignResponsePromise;
      const addSignRequest = addSignResponse.request().postDataJSON() as any;
      expect(addSignRequest.targetUserIds).toEqual([addedUserName]);
      expectApiSuccess(await addSignResponse.json(), '加签失败');
      await page.waitForURL('**/#/workflow/task/done', { timeout: 10000 });

      const addedUserTask = await findTodoTask(request, addedUserToken, businessKey, '加签审批');
      expect(String(addedUserTask.assigneeName)).toBe(addedUserName);

      await completeTask(request, token, String(adminTaskBeforeAddSign.id), '管理员通过加签会签');
      await completeTask(request, firstUserToken, String(firstUserTaskBeforeAddSign.id), '首个临时用户通过加签会签');
      await completeTask(request, addedUserToken, String(addedUserTask.id), '新增加签用户通过');
      await expectNoAuthError(page);
    } finally {
      cleanupWorkflowActionData(businessKey);
      await cleanupWorkflow(request, token, keyword).catch(() => undefined);
      await cleanupUser(request, token, firstUserName).catch(() => undefined);
      await cleanupUser(request, token, addedUserName).catch(() => undefined);
    }
  });

  test('费用报销按业务接入模型支持申请、驳回、再申请、历史查看和自定义审批页', async ({ page, request }) => {
    test.setTimeout(120_000);
    const unique = Date.now();
    const keyword = `e2e_workflow_expense_${unique}`;
    const businessKey = `EXP-E2E-${unique}`;
    const token = await loginToken(request, platformTenant);

    try {
      cleanupWorkflowBusinessApplies(businessKey);
      await cleanupWorkflow(request, token, keyword);
      const workflow = await prepareExpenseWorkflow(request, token, unique, keyword);

      const firstStart = await startExpenseProcess(
        request,
        token,
        workflow.definitionId,
        unique,
        businessKey,
        1,
        1280,
        'E2E 首次差旅报销',
      );
      expect(firstStart.processInstanceId).toBeTruthy();

      const firstProgress = await latestBusinessApply(request, token, businessKey);
      expect(String(firstProgress.businessType)).toBe('EXPENSE_REIMBURSEMENT');
      expect(String(firstProgress.businessKey)).toBe(businessKey);
      expect(String(firstProgress.processInstanceId)).toBe(firstStart.processInstanceId);
      expect(String(firstProgress.applyStatus)).toBe('IN_APPROVAL');
      expect(String(firstProgress.currentTaskNames)).toContain('部门经理审批');
      expect(String(firstProgress.currentTaskDefinitionKeys)).toContain(`manager_approve_${unique}`);
      const nodeFiltered = await queryBusinessAppliesByCurrentNode(request, token, businessKey, `manager_approve_${unique}`);
      expect(nodeFiltered.some((item: any) => String(item.businessKey) === businessKey)).toBeTruthy();

      await loginPage(page, platformTenant);
      await openTodoTasks(page);
      const taskRow = page.locator('.el-table__row', { hasText: businessKey });
      await expect(taskRow).toBeVisible({ timeout: 10000 });
      await expect(taskRow).toContainText('部门经理审批');
      const detailResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/tasks/detail') && response.status() === 200
      );
      await taskRow.getByRole('button', { name: '处理' }).click();
      await page.waitForURL('**/#/workflow/task/detail**', { timeout: 10000 });
      await detailResponsePromise;
      await expect(page.locator('.workflow-task-detail-page')).toContainText('业务审批信息');
      await expect(page.locator('.workflow-task-detail-page')).toContainText('费用报销');
      await expect(page.locator('.workflow-task-detail-page')).toContainText('E2E 首次差旅报销');
      await expect(page.locator('.workflow-task-detail-page')).toContainText('¥1280.00');
      await expect(page.locator('.workflow-task-detail-page')).not.toContainText('622202******8001');
      await expect(page.locator('.workflow-task-detail-page')).toContainText('MANAGER_APPROVE');

      await page.getByPlaceholder('请输入审批意见').fill('预算说明不完整，驳回重提');
      const rejectResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/workflow/tasks/reject') && response.status() === 200
      );
      await page.getByRole('button', { name: '退回修改' }).click();
      await page.getByRole('dialog', { name: '审批退回修改' }).getByRole('button', { name: /^(OK|确定)$/ }).click();
      const rejectResponse = await rejectResponsePromise;
      const rejectBody = await rejectResponse.json();
      expect(rejectBody.success || rejectBody.code === 200).toBeTruthy();

      const rejectedProgress = await latestBusinessApply(request, token, businessKey);
      expect(String(rejectedProgress.applyStatus)).toBe('REJECTED');
      expect(String(rejectedProgress.applyStatusName)).toBe('已驳回');
      expect(String(rejectedProgress.currentTaskNames || '')).toBe('');

      const secondStart = await startExpenseProcess(
        request,
        token,
        workflow.definitionId,
        unique,
        businessKey,
        2,
        1399.5,
        'E2E 补充预算说明后重新报销',
      );
      expect(secondStart.processInstanceId).toBeTruthy();
      expect(secondStart.processInstanceId).not.toBe(firstStart.processInstanceId);
      const secondProgress = await latestBusinessApply(request, token, businessKey);
      expect(String(secondProgress.applyStatus)).toBe('IN_APPROVAL');
      expect(String(secondProgress.processInstanceId)).toBe(secondStart.processInstanceId);
      expect(String(secondProgress.applyId)).not.toBe(String(firstProgress.applyId));

      const historyAfterReapply = await businessApplyHistory(request, token, businessKey);
      expect(historyAfterReapply).toHaveLength(2);
      const firstHistory = historyAfterReapply.find((item: any) => String(item.processInstanceId) === firstStart.processInstanceId);
      const secondHistory = historyAfterReapply.find((item: any) => String(item.processInstanceId) === secondStart.processInstanceId);
      expect(firstHistory?.variables?.reason).toBe('E2E 首次差旅报销');
      expect(Number(firstHistory?.variables?.amount)).toBe(1280);
      expect(firstHistory?.applyStatus).toBe('REJECTED');
      expect(secondHistory?.variables?.reason).toBe('E2E 补充预算说明后重新报销');
      expect(Number(secondHistory?.variables?.amount)).toBe(1399.5);

      const managerTask = await findTodoTask(request, token, businessKey, '部门经理审批');
      await completeTask(request, token, String(managerTask.id), '部门经理同意');
      const financeProgress = await latestBusinessApply(request, token, businessKey);
      expect(String(financeProgress.applyStatus)).toBe('IN_APPROVAL');
      expect(String(financeProgress.currentTaskNames)).toContain('财务复核');
      expect(String(financeProgress.currentTaskDefinitionKeys)).toContain(`finance_review_${unique}`);

      const financeTask = await findTodoTask(request, token, businessKey, '财务复核');
      const financeDetailResponse = await request.get(api(`/workflow/tasks/detail`), {
        headers: { Authorization: `Bearer ${token}` },
        params: { taskId: financeTask.id },
      });
      expect(financeDetailResponse.status()).toBe(200);
      const financeDetailBody = await financeDetailResponse.json();
      expect(financeDetailBody.success || financeDetailBody.code === 200).toBeTruthy();
      expect(financeDetailBody.data.renderConfig.renderMode).toBe('CUSTOM_PAGE');
      expect(financeDetailBody.data.renderConfig.businessType).toBe('EXPENSE_REIMBURSEMENT');
      expect(financeDetailBody.data.renderConfig.taskDefinitionKey).toBe(`finance_review_${unique}`);
      expect(financeDetailBody.data.renderConfig.nodeExtension.sectionPreset).toBe('FINANCE_REVIEW');
      expect(financeDetailBody.data.renderConfig.businessPermissions.financeReview).toBe('EDITABLE');
      expect(financeDetailBody.data.renderConfig.nodeActions.complete).toMatchObject({
        enabled: true,
        label: '财务确认',
        requireComment: false,
      });
      expect(financeDetailBody.data.renderConfig.nodeActions.reject).toMatchObject({
        enabled: true,
        label: '退回补充',
        requireComment: true,
      });
      expect(financeDetailBody.data.renderConfig.nodeActions.transfer).toMatchObject({
        enabled: true,
        label: '转办他人',
        requireComment: false,
      });

      await completeTask(request, token, String(financeTask.id), '财务复核通过', { approvedAmount: 1399.5 });
      const approvedProgress = await latestBusinessApply(request, token, businessKey);
      expect(String(approvedProgress.applyStatus)).toBe('APPROVED');
      expect(String(approvedProgress.applyStatusName)).toBe('已通过');
      expect(String(approvedProgress.currentTaskNames || '')).toBe('');
    } finally {
      cleanupWorkflowBusinessApplies(businessKey);
      await cleanupWorkflow(request, token, keyword).catch(() => undefined);
    }
  });
});
