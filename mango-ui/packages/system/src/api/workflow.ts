/**
 * Workflow API - 工作流配置。
 */

import { del, get, post, put } from '@mango/common/utils/request';

export type WorkflowStatus = 'DRAFT' | 'PUBLISHED' | 'DISABLED';
export type WorkflowAssigneeType = 'SPECIFIED_USER' | 'SPECIFIED_ROLE' | 'SPECIFIED_POST' | 'SPECIFIED_ORG' | 'ORG_LEADER' | 'INITIATOR' | 'INITIATOR_SELECT' | 'FORM_USER' | 'EXPRESSION';
export type WorkflowApprovalMode = 'COUNTERSIGN' | 'OR_SIGN' | 'SEQUENTIAL';
export type WorkflowEmptyAssigneeStrategy = 'AUTO_PASS' | 'AUTO_REJECT' | 'AUTO_END' | 'TO_ADMIN' | 'TO_USER';
export type WorkflowRejectStrategy = 'END_PROCESS' | 'BACK_TO_START';
export type WorkflowFormPermission = 'HIDDEN' | 'READONLY' | 'EDITABLE';
export type WorkflowId = string;

export interface WorkflowEventNotifyConfig {
  enabled?: boolean;
  type?: 'HTTP' | 'EVENT';
  url?: string;
  eventName?: string;
  method?: 'POST' | 'GET' | 'PUT' | 'DELETE';
  timeoutMillis?: number;
  payloadTemplate?: string;
}

export interface WorkflowApprovalNodeConfig {
  assigneeType: WorkflowAssigneeType;
  assigneeIds?: string[];
  roleIds?: string[];
  postIds?: string[];
  orgIds?: string[];
  formUserField?: string;
  formUserFieldType?: 'USER' | 'ORG' | 'ROLE' | 'POST';
  expression?: string;
  expressionName?: string;
  approvalMode: WorkflowApprovalMode;
  emptyAssigneeStrategy: WorkflowEmptyAssigneeStrategy;
  emptyAssigneeUserIds?: string[];
  rejectStrategy: WorkflowRejectStrategy;
  formPermissions?: Record<string, WorkflowFormPermission>;
  eventNotify?: WorkflowEventNotifyConfig;
  extension?: Record<string, any>;
  initiatorSelectMultiple?: boolean;
  orgLeaderUseInitiatorOrg?: boolean;
}

export interface WorkflowCategory {
  id?: WorkflowId;
  categoryName: string;
  categoryCode: string;
  sort?: number;
  status?: number;
  remark?: string;
  createdTime?: string;
  updatedTime?: string;
}

export interface WorkflowDefinition {
  id?: WorkflowId;
  categoryId: WorkflowId;
  categoryName?: string;
  orgId?: WorkflowId;
  adminUsers?: string[];
  icon?: string;
  definitionName: string;
  definitionKey: string;
  deploymentId?: string;
  processDefinitionId?: string;
  processDefinitionVersion?: number;
  publishedVersionNo?: number;
  sourceTemplateId?: WorkflowId;
  sourceTemplateCode?: string;
  sourceTemplateVersion?: number;
  designerJson: string;
  bpmnXml?: string;
  formCode?: string;
  formJson?: string;
  status?: WorkflowStatus;
  lastDeployTime?: string;
  remark?: string;
  createdTime?: string;
  updatedTime?: string;
}

type WorkflowDefinitionCommand = Pick<WorkflowDefinition,
  'id' | 'categoryId' | 'orgId' | 'adminUsers' | 'icon' | 'definitionName' | 'definitionKey' | 'designerJson' | 'formCode' | 'formJson' | 'status' | 'remark'
>;

export interface WorkflowPageQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  categoryId?: WorkflowId | '';
  orgId?: WorkflowId | '';
  status?: string;
  categoryCode?: string;
  bpmnType?: string;
  executionType?: string;
}

export interface WorkflowTemplateCategory {
  id?: WorkflowId;
  parentId?: WorkflowId;
  categoryName: string;
  categoryCode: string;
  icon?: string;
  sort?: number;
  status?: number;
  remark?: string;
  createdTime?: string;
  updatedTime?: string;
}

export interface WorkflowTemplate {
  id?: WorkflowId;
  templateName: string;
  templateCode: string;
  templateCategoryId?: WorkflowId;
  templateCategoryName?: string;
  categoryCode?: string;
  categoryName?: string;
  icon?: string;
  adminUsers?: string[];
  designerJson: string;
  formCode?: string;
  formJson?: string;
  versionNo?: number;
  latestFlag?: boolean;
  status?: 'ENABLED' | 'DISABLED' | 'ARCHIVED' | 'DRAFT';
  statusName?: string;
  sourceDefinitionId?: WorkflowId;
  sourceDefinitionKey?: string;
  sourceDefinitionName?: string;
  remark?: string;
  createdTime?: string;
  updatedTime?: string;
}

export interface ImportWorkflowTemplatesCommand {
  categoryId: WorkflowId;
  targetTenantId?: WorkflowId;
  orgId?: WorkflowId;
  templateCategoryId?: WorkflowId;
  templateIds?: WorkflowId[];
  adminUsers?: string[];
}

export interface WorkflowTemplateImportError {
  templateId?: WorkflowId;
  templateName?: string;
  templateCode?: string;
  reason?: string;
}

export interface WorkflowTemplateImportResult {
  definitionIds: WorkflowId[];
  errors: WorkflowTemplateImportError[];
}

export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export interface WorkflowDesignerNode {
  id: string;
  nodeDefinitionCode?: string;
  nodeName: string;
  nodeType: string;
  bpmnType?: string;
  executionType?: string;
  description?: string;
  conditionExpression?: string;
  serviceHandler?: string;
  childNode?: WorkflowDesignerNode | null;
  conditionNodes?: WorkflowDesignerNode[];
  properties?: Record<string, any>;
}

export interface WorkflowDefinitionVersion {
  id: WorkflowId;
  definitionId: WorkflowId;
  versionNo: number;
  designerJson: string;
  formJson?: string;
  bpmnXml: string;
  deploymentId?: string;
  processDefinitionId?: string;
  processDefinitionVersion?: number;
  publishStatus?: string;
  publishMessage?: string;
  publishTime?: string;
}

export interface WorkflowNodeCatalog {
  id?: WorkflowId;
  nodeDefinitionCode: string;
  nodeType: string;
  nodeName: string;
  categoryCode: string;
  categoryName: string;
  groupName: string;
  description: string;
  bpmnType: string;
  executionType: string;
  color?: string;
  icon?: string;
  propertySchema?: string;
  defaultProperties?: string;
  sort?: number;
  status?: number;
}

export interface WorkflowTask {
  id: string;
  taskName: string;
  taskDefinitionKey?: string;
  processInstanceId: string;
  businessKey?: string;
  processName: string;
  processKey: string;
  processDefinitionId?: string;
  initiatorName?: string;
  assigneeName?: string;
  status: string;
  createTime?: string;
  endTime?: string;
}

export interface WorkflowProcessInstance {
  processInstanceId: string;
  businessKey?: string;
  definitionId?: WorkflowId;
  processName: string;
  processKey: string;
  processDefinitionId?: string;
  initiatorName?: string;
  currentTaskName?: string;
  currentTaskDefinitionKey?: string;
  status: string;
  startTime?: string;
  endTime?: string;
}

export interface StartWorkflowProcessCommand {
  definitionId: WorkflowId;
  businessKey?: string;
  businessType?: string;
  applyId?: WorkflowId;
  renderMode?: WorkflowApplyRenderMode;
  applyPageKey?: string;
  approvePageKey?: string;
  snapshotRef?: string;
  variables?: Record<string, any>;
  selectedAssignees?: Record<string, string[]>;
}

export type WorkflowApplyStatus = 'DRAFT' | 'SUBMITTED' | 'IN_APPROVAL' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN' | 'CANCELED' | 'TERMINATED';
export type WorkflowApplyRenderMode = 'DYNAMIC_FORM' | 'CUSTOM_PAGE';

export interface WorkflowBusinessApplyCurrentTask {
  taskId?: string;
  taskDefinitionKey?: string;
  taskName?: string;
  assigneeId?: WorkflowId;
  assigneeName?: string;
  arrivedAt?: string;
}

export interface WorkflowBusinessApply {
  id: WorkflowId;
  applyCode?: string;
  businessType: string;
  businessKey: string;
  applyTitle: string;
  applySummary?: string;
  applicantId?: WorkflowId;
  applicantName?: string;
  processDefinitionId?: WorkflowId;
  processDefinitionKey?: string;
  engineProcessDefinitionId?: string;
  processInstanceId?: string;
  processName?: string;
  applyStatus?: WorkflowApplyStatus;
  applyStatusName?: string;
  currentTaskNames?: string;
  currentTaskDefinitionKeys?: string;
  currentAssigneeNames?: string;
  renderMode?: WorkflowApplyRenderMode;
  applyPageKey?: string;
  approvePageKey?: string;
  formKey?: string;
  formVersion?: number;
  snapshotRef?: string;
  latestFlag?: boolean;
  variables?: Record<string, any>;
  extension?: Record<string, any>;
  currentTasks?: WorkflowBusinessApplyCurrentTask[];
  createdAt?: string;
  updatedAt?: string;
}

export type WorkflowBusinessApplyProgress = Pick<WorkflowBusinessApply,
  'businessType' | 'businessKey' | 'applyTitle' | 'processInstanceId' | 'processName' | 'applyStatus'
  | 'applyStatusName' | 'currentTaskNames' | 'currentTaskDefinitionKeys' | 'currentAssigneeNames' | 'currentTasks'
  | 'createdAt' | 'updatedAt'
> & {
  applyId?: WorkflowId;
  applyCode?: string;
};

export interface WorkflowBusinessApplyPageQuery extends WorkflowPageQuery {
  businessType?: string;
  businessKey?: string;
  statuses?: WorkflowApplyStatus[];
  latestOnly?: boolean;
  applicantId?: WorkflowId;
  currentTaskDefinitionKeys?: string[];
  currentAssigneeIds?: WorkflowId[];
  startedAtBegin?: string;
  startedAtEnd?: string;
}

export interface WorkflowUserOption {
  value: string;
  label: string;
  username?: string;
}

export interface WorkflowTaskRecord {
  id?: WorkflowId;
  processInstanceId: string;
  taskId?: string;
  taskName?: string;
  taskDefinitionKey?: string;
  action: string;
  actionName: string;
  operatorId?: WorkflowId;
  operatorName?: string;
  comment?: string;
  variables?: Record<string, any>;
  createdTime?: string;
}

export interface WorkflowRenderConfig {
  renderMode?: WorkflowApplyRenderMode;
  businessType?: string;
  businessKey?: string;
  applyId?: WorkflowId;
  processInstanceId?: string;
  applyPageKey?: string;
  approvePageKey?: string;
  formKey?: string;
  formVersion?: number;
  snapshotRef?: string;
  taskDefinitionKey?: string;
  nodeExtension?: Record<string, any>;
  formPermissions?: Record<string, WorkflowFormPermission>;
  businessPermissions?: Record<string, any>;
}

export interface WorkflowTaskDetail {
  task: WorkflowTask;
  process: WorkflowProcessInstance;
  formCode?: string;
  formJson?: string;
  variables: Record<string, any>;
  formPermissions?: Record<string, WorkflowFormPermission>;
  renderConfig?: WorkflowRenderConfig;
  records: WorkflowTaskRecord[];
}

export interface WorkflowProcessDetail {
  process: WorkflowProcessInstance;
  formCode?: string;
  formJson?: string;
  variables: Record<string, any>;
  renderConfig?: WorkflowRenderConfig;
  records: WorkflowTaskRecord[];
}

export interface WorkflowTaskActionCommand {
  taskId: string;
  comment?: string;
  variables?: Record<string, any>;
}

export const workflowApi = {
  categoriesPage: (params?: WorkflowPageQuery) => get<any>('/workflow/categories/page', { params: toBackendPageParams(params) })
    .then(data => fromBackendPageResult(data, normalizeCategory, params)),
  categoriesList: (status?: number) => get<WorkflowCategory[]>('/workflow/categories/list', { params: { status } })
    .then(list => (Array.isArray(list) ? list.map(normalizeCategory) : [])),
  categoryDetail: (id: WorkflowId) => get<WorkflowCategory>('/workflow/categories/detail', { params: { id } }).then(normalizeCategory),
  createCategory: (data: WorkflowCategory) => post<WorkflowId>('/workflow/categories', data),
  updateCategory: (data: WorkflowCategory) => put<boolean>('/workflow/categories', data),
  deleteCategory: (id: WorkflowId) => del<boolean>('/workflow/categories', { params: { id } }),

  definitionsPage: (params?: WorkflowPageQuery) => get<any>('/workflow/definitions/page', { params: toBackendPageParams(params) })
    .then(data => fromBackendPageResult(data, normalizeDefinition, params)),
  definitionDetail: (id: WorkflowId) => get<WorkflowDefinition>('/workflow/definitions/detail', { params: { id } }).then(normalizeDefinition),
  createDefinition: (data: WorkflowDefinition) => post<WorkflowId>('/workflow/definitions', toDefinitionCommand(data, false)),
  updateDefinition: (data: WorkflowDefinition) => put<boolean>('/workflow/definitions', toDefinitionCommand(data, true)),
  deleteDefinition: (id: WorkflowId) => del<boolean>('/workflow/definitions', { params: { id } }),
  updateDefinitionStatus: (id: WorkflowId, status: WorkflowStatus) => put<boolean>('/workflow/definitions/status', { id, status }),
  deployDefinition: (id: WorkflowId) => post<any>('/workflow/definitions/deploy', undefined, { params: { id } }),
  definitionVersions: (definitionId: WorkflowId) => get<WorkflowDefinitionVersion[]>('/workflow/definitions/versions', { params: { definitionId } })
    .then(list => Array.isArray(list) ? list.map(normalizeVersion) : []),
  definitionVersionDetail: (id: WorkflowId) => get<WorkflowDefinitionVersion>('/workflow/definitions/version-detail', { params: { id } }).then(normalizeVersion),
  nodeCatalog: () => get<WorkflowNodeCatalog[]>('/workflow/definitions/node-catalog')
    .then(list => Array.isArray(list) ? list.map(normalizeNodeCatalog) : []),

  templatesPage: (params?: WorkflowPageQuery & { templateCategoryId?: WorkflowId | '' }) => get<any>('/workflow/templates/page', { params: toBackendPageParams(params) })
    .then(data => fromBackendPageResult(data, normalizeTemplate, params)),
  templateDetail: (id: WorkflowId) => get<WorkflowTemplate>('/workflow/templates/detail', { params: { id } }).then(normalizeTemplate),
  createTemplate: (data: WorkflowTemplate) => post<WorkflowId>('/workflow/templates', toTemplateCommand(data)),
  deleteTemplate: (id: WorkflowId) => del<boolean>('/workflow/templates', { params: { id } }),
  createTemplateFromDefinition: (data: Record<string, any>) => post<WorkflowId>('/workflow/templates/from-definition', data),
  createDefinitionFromTemplate: (data: Record<string, any>) => post<WorkflowId>('/workflow/templates/create-definition', data),
  importTemplates: (data: ImportWorkflowTemplatesCommand) => post<WorkflowTemplateImportResult>('/workflow/templates/import', data)
    .then(normalizeTemplateImportResult),
  templateCategoriesPage: (params?: WorkflowPageQuery) => get<any>('/workflow/template-categories/page', { params: toBackendPageParams(params) })
    .then(data => fromBackendPageResult(data, normalizeTemplateCategory, params)),
  templateCategoriesList: (status?: number) => get<WorkflowTemplateCategory[]>('/workflow/template-categories/list', { params: { status } })
    .then(list => (Array.isArray(list) ? list.map(normalizeTemplateCategory) : [])),
  createTemplateCategory: (data: WorkflowTemplateCategory) => post<WorkflowId>('/workflow/template-categories', data),
  updateTemplateCategory: (data: WorkflowTemplateCategory) => put<boolean>('/workflow/template-categories', data),
  deleteTemplateCategory: (id: WorkflowId) => del<boolean>('/workflow/template-categories', { params: { id } }),

  todoTasks: (params?: WorkflowPageQuery) => get<any>('/workflow/tasks/todo', { params: toBackendPageParams(params) })
    .then(data => fromBackendPageResult(data, normalizeTask, params)),
  initiatedTasks: (params?: WorkflowPageQuery) => get<any>('/workflow/tasks/initiated', { params: toBackendPageParams(params) })
    .then(data => fromBackendPageResult(data, normalizeTask, params)),
  doneTasks: (params?: WorkflowPageQuery) => get<any>('/workflow/tasks/done', { params: toBackendPageParams(params) })
    .then(data => fromBackendPageResult(data, normalizeTask, params)),
  copiedTasks: (params?: WorkflowPageQuery) => get<any>('/workflow/tasks/copied', { params: toBackendPageParams(params) })
    .then(data => fromBackendPageResult(data, normalizeTask, params)),
  taskDetail: (taskId: string) => get<WorkflowTaskDetail>('/workflow/tasks/detail', { params: { taskId } })
    .then(normalizeTaskDetail),
  completeTask: (data: WorkflowTaskActionCommand) => post<boolean>('/workflow/tasks/complete', data),
  rejectTask: (data: WorkflowTaskActionCommand) => post<boolean>('/workflow/tasks/reject', data),

  startProcess: (data: StartWorkflowProcessCommand) => post<WorkflowProcessInstance>('/workflow/processes/start', data)
    .then(normalizeProcessInstance),
  businessAppliesPage: (params?: WorkflowBusinessApplyPageQuery) => post<any>('/workflow/business-applies/page', toBackendBusinessApplyPageParams(params))
    .then(data => fromBackendPageResult(data, normalizeBusinessApply, params)),
  businessApplyHistory: (businessType: string, businessKey: string, params?: WorkflowBusinessApplyPageQuery) => get<any>('/workflow/business-applies/history', {
    params: {
      ...toBackendPageParams(params),
      businessType,
      businessKey,
    },
  }).then(data => fromBackendPageResult(data, normalizeBusinessApply, params)),
  businessApplyLatestProgress: (businessType: string, businessKey: string) => get<WorkflowBusinessApplyProgress | null>('/workflow/business-applies/progress/latest', {
    params: { businessType, businessKey },
  }).then(data => data ? normalizeBusinessApplyProgress(data) : null),
  businessApplyByProcessInstance: (processInstanceId: string) => get<WorkflowBusinessApply>('/workflow/business-applies/progress/by-process-instance', {
    params: { processInstanceId },
  }).then(normalizeBusinessApply),
  businessApplyLatestProgressBatch: (businessType: string, businessKeys: string[]) => post<Record<string, WorkflowBusinessApplyProgress>>('/workflow/business-applies/progress/latest-batch', {
    businessType,
    businessKeys,
  }).then(data => Object.fromEntries(Object.entries(data || {}).map(([key, value]) => [key, normalizeBusinessApplyProgress(value)]))),
  initiatedProcesses: (params?: WorkflowPageQuery) => get<any>('/workflow/processes/initiated', { params: toBackendPageParams(params) })
    .then(data => fromBackendPageResult(data, normalizeProcessInstance, params)),
  processHistoryByBusinessKey: (businessKey: string, params?: WorkflowPageQuery) => get<any>('/workflow/processes/history', {
    params: {
      ...toBackendPageParams(params),
      businessKey,
    },
  }).then(data => fromBackendPageResult(data, normalizeProcessInstance, params)),
  processDetail: (processInstanceId: string) => get<WorkflowProcessDetail>('/workflow/processes/detail', { params: { processInstanceId } })
    .then(normalizeProcessDetail),
  users: (keyword = '') => get<any>('/identity/users/page', {
    params: {
      page: 1,
      size: 100,
      username: keyword || undefined,
      nickname: keyword || undefined,
    },
  }).then(data => toPageList<any>(data)
    .map(item => {
      const id = item.userId ?? item.id ?? item.memberId;
      const value = item.username ?? id;
      const name = item.nickname || item.memberName || item.username || id;
      const username = item.username && item.username !== name ? ` / ${item.username}` : '';
      return id === undefined ? undefined : {
        value: String(value),
        label: `${name}${username}`,
        username: item.username,
      };
    })
    .filter(Boolean) as WorkflowUserOption[]),
};

export const workflowStatusOptions: Array<{ label: string; value: WorkflowStatus; type: 'info' | 'success' | 'warning' }> = [
  { label: '草稿', value: 'DRAFT', type: 'info' },
  { label: '已发布', value: 'PUBLISHED', type: 'success' },
  { label: '停用', value: 'DISABLED', type: 'warning' },
];

export function workflowStatusLabel(value?: string) {
  return workflowStatusOptions.find(item => item.value === value)?.label || value || '-';
}

export function workflowStatusType(value?: string) {
  return workflowStatusOptions.find(item => item.value === value)?.type || 'info';
}

export function defaultBpmnXml(processKey = 'sample_process', processName = '示例流程') {
  return `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://mango.io/workflow">
  <process id="${processKey}" name="${processName}" isExecutable="true">
    <startEvent id="startEvent" name="开始" />
    <sequenceFlow id="flow_start_to_approve" sourceRef="startEvent" targetRef="approveTask" />
    <userTask id="approveTask" name="人工审批" flowable:assignee="${'${initiator}'}" />
    <sequenceFlow id="flow_approve_to_end" sourceRef="approveTask" targetRef="endEvent" />
    <endEvent id="endEvent" name="结束" />
  </process>
</definitions>`;
}

export function defaultDesignerJson(): string {
  return JSON.stringify({
    id: 'startEvent',
    nodeName: '发起人',
    nodeType: 'ROOT',
    childNode: {
      id: createNodeId('approve'),
      nodeDefinitionCode: 'APPROVAL',
      nodeName: '人工审批',
      nodeType: 'APPROVAL',
      bpmnType: 'userTask',
      executionType: 'USER_TASK',
      childNode: null,
      conditionNodes: [],
      properties: {
        approvalConfig: defaultApprovalConfig(),
      },
    },
    conditionNodes: [],
    properties: {},
  }, null, 2);
}

export function defaultApprovalConfig(): WorkflowApprovalNodeConfig {
  return {
    assigneeType: 'INITIATOR',
    assigneeIds: [],
    roleIds: [],
    postIds: [],
    orgIds: [],
    formUserFieldType: 'USER',
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
    extension: {},
    initiatorSelectMultiple: false,
    orgLeaderUseInitiatorOrg: true,
  };
}

export function parseDesignerJson(value?: string): WorkflowDesignerNode {
  if (!value) {
    return JSON.parse(defaultDesignerJson());
  }
  try {
    return JSON.parse(value);
  } catch {
    return JSON.parse(defaultDesignerJson());
  }
}

export function stringifyDesignerJson(node: WorkflowDesignerNode): string {
  return JSON.stringify(node, null, 2);
}

export function createNodeId(prefix = 'node'): string {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
}

function toBackendPageParams(params?: WorkflowPageQuery) {
  if (!params) return params;
  const { pageNum, pageSize, categoryId, orgId, ...rest } = params;
  return {
    ...rest,
    categoryId: categoryId === '' ? undefined : categoryId,
    orgId: orgId === '' ? undefined : orgId,
    page: pageNum,
    size: pageSize,
  };
}

function toBackendBusinessApplyPageParams(params?: WorkflowBusinessApplyPageQuery) {
  if (!params) return params;
  const { pageNum, pageSize, categoryId, orgId, ...rest } = params;
  return {
    ...rest,
    categoryId: categoryId === '' ? undefined : categoryId,
    orgId: orgId === '' ? undefined : orgId,
    page: pageNum,
    size: pageSize,
  };
}

function fromBackendPageResult<T>(
  data: any,
  mapper: (item: any) => T,
  params?: { pageNum?: number; pageSize?: number },
): PageResult<T> {
  const list = toPageList<any>(data).map(mapper);
  return {
    list,
    total: Number(data?.total ?? list.length),
    pageNum: Number(data?.page ?? params?.pageNum ?? 1),
    pageSize: Number(data?.size ?? params?.pageSize ?? list.length ?? 10),
  };
}

function toPageList<T>(data: any): T[] {
  if (Array.isArray(data)) {
    return data;
  }
  if (Array.isArray(data?.list)) {
    return data.list;
  }
  if (Array.isArray(data?.records)) {
    return data.records;
  }
  return [];
}

function normalizeCategory(item: any): WorkflowCategory {
  return {
    ...item,
    id: normalizeId(item?.id),
    categoryName: item?.categoryName ?? '',
    categoryCode: item?.categoryCode ?? '',
    createdTime: normalizeDateTime(item?.createdTime),
    updatedTime: normalizeDateTime(item?.updatedTime),
  };
}

function normalizeTemplateCategory(item: any): WorkflowTemplateCategory {
  return {
    ...item,
    id: normalizeId(item?.id),
    parentId: item?.parentId ? normalizeId(item.parentId) : undefined,
    createdTime: normalizeDateTime(item?.createdTime),
    updatedTime: normalizeDateTime(item?.updatedTime),
  };
}

function normalizeTemplate(item: any): WorkflowTemplate {
  return {
    ...item,
    id: normalizeId(item?.id),
    templateCategoryId: item?.templateCategoryId ? normalizeId(item.templateCategoryId) : undefined,
    adminUsers: normalizeStringList(item?.adminUsers),
    sourceDefinitionId: item?.sourceDefinitionId ? normalizeId(item.sourceDefinitionId) : undefined,
    designerJson: item?.designerJson || defaultDesignerJson(),
    createdTime: normalizeDateTime(item?.createdTime),
    updatedTime: normalizeDateTime(item?.updatedTime),
  };
}

function normalizeDefinition(item: any): WorkflowDefinition {
  return {
    ...item,
    id: normalizeId(item?.id),
    categoryId: normalizeId(item?.categoryId),
    categoryName: item?.categoryName,
    orgId: item?.orgId ? normalizeId(item.orgId) : undefined,
    sourceTemplateId: item?.sourceTemplateId ? normalizeId(item.sourceTemplateId) : undefined,
    adminUsers: normalizeStringList(item?.adminUsers),
    icon: item?.icon || 'Setting',
    designerJson: item?.designerJson || defaultDesignerJson(),
    createdTime: normalizeDateTime(item?.createdTime),
    updatedTime: normalizeDateTime(item?.updatedTime),
    lastDeployTime: normalizeDateTime(item?.lastDeployTime),
  };
}

function toDefinitionCommand(data: WorkflowDefinition, includeId: boolean): WorkflowDefinitionCommand {
  const command: WorkflowDefinitionCommand = {
    categoryId: data.categoryId,
    orgId: data.orgId,
    adminUsers: normalizeStringList(data.adminUsers),
    icon: data.icon || 'Setting',
    definitionName: data.definitionName,
    definitionKey: data.definitionKey,
    designerJson: data.designerJson,
    formCode: data.formCode,
    formJson: data.formJson,
    status: data.status,
    remark: data.remark,
  };
  if (includeId) {
    command.id = data.id;
  }
  return command;
}

function toTemplateCommand(data: WorkflowTemplate) {
  return {
    ...data,
    templateCategoryId: data.templateCategoryId || undefined,
    adminUsers: normalizeStringList(data.adminUsers),
  };
}

function normalizeTemplateImportResult(item: any): WorkflowTemplateImportResult {
  return {
    definitionIds: Array.isArray(item?.definitionIds) ? item.definitionIds.map(normalizeId) : [],
    errors: Array.isArray(item?.errors)
      ? item.errors.map((error: any) => ({
          ...error,
          templateId: error?.templateId ? normalizeId(error.templateId) : undefined,
        }))
      : [],
  };
}

function normalizeStringList(value: unknown): string[] {
  if (Array.isArray(value)) {
    return value.map(item => String(item).trim()).filter(Boolean);
  }
  if (typeof value === 'string') {
    const text = value.trim();
    if (!text) return [];
    try {
      const parsed = JSON.parse(text);
      if (Array.isArray(parsed)) {
        return parsed.map(item => String(item).trim()).filter(Boolean);
      }
    } catch {
      // Fallback to comma separated values.
    }
    return text.split(',').map(item => item.trim()).filter(Boolean);
  }
  return [];
}

function normalizeVersion(item: any): WorkflowDefinitionVersion {
  return {
    ...item,
    id: normalizeId(item?.id),
    definitionId: normalizeId(item?.definitionId),
    publishTime: normalizeDateTime(item?.publishTime),
  };
}

function normalizeId(value: unknown): WorkflowId {
  return value == null ? '' : String(value);
}

function normalizeNodeCatalog(item: any): WorkflowNodeCatalog {
  return {
    ...item,
    nodeDefinitionCode: item?.nodeDefinitionCode || item?.nodeType,
    categoryCode: item?.categoryCode || item?.groupName || 'BASIC',
    categoryName: item?.categoryName || item?.groupName || '基础节点',
    groupName: item?.groupName || item?.categoryName || '基础节点',
    executionType: item?.executionType || (item?.bpmnType === 'userTask' ? 'USER_TASK' : 'NONE'),
  };
}

function normalizeTask(item: any): WorkflowTask {
  return {
    id: item?.id ? String(item.id) : '',
    taskName: item?.taskName || '-',
    taskDefinitionKey: item?.taskDefinitionKey || item?.taskDefinitionCode || item?.activityId,
    processInstanceId: item?.processInstanceId ? String(item.processInstanceId) : '',
    businessKey: item?.businessKey,
    processName: item?.processName || '-',
    processKey: item?.processKey || '-',
    processDefinitionId: item?.processDefinitionId,
    initiatorName: item?.initiatorName,
    assigneeName: item?.assigneeName,
    status: item?.status || '-',
    createTime: normalizeDateTime(item?.createTime),
    endTime: normalizeDateTime(item?.endTime),
  };
}

function normalizeProcessInstance(item: any): WorkflowProcessInstance {
  return {
    processInstanceId: item?.processInstanceId ? String(item.processInstanceId) : '',
    businessKey: item?.businessKey,
    definitionId: item?.definitionId ? normalizeId(item.definitionId) : undefined,
    processName: item?.processName || '-',
    processKey: item?.processKey || '-',
    processDefinitionId: item?.processDefinitionId,
    initiatorName: item?.initiatorName,
    currentTaskName: item?.currentTaskName,
    currentTaskDefinitionKey: item?.currentTaskDefinitionKey,
    status: item?.status || '-',
    startTime: normalizeDateTime(item?.startTime),
    endTime: normalizeDateTime(item?.endTime),
  };
}

function normalizeBusinessApplyCurrentTask(item: any): WorkflowBusinessApplyCurrentTask {
  return {
    taskId: item?.taskId,
    taskDefinitionKey: item?.taskDefinitionKey,
    taskName: item?.taskName,
    assigneeId: item?.assigneeId ? normalizeId(item.assigneeId) : undefined,
    assigneeName: item?.assigneeName,
    arrivedAt: normalizeDateTime(item?.arrivedAt),
  };
}

function normalizeBusinessApply(item: any): WorkflowBusinessApply {
  return {
    ...item,
    id: normalizeId(item?.id),
    applicantId: item?.applicantId ? normalizeId(item.applicantId) : undefined,
    processDefinitionId: item?.processDefinitionId ? normalizeId(item.processDefinitionId) : undefined,
    currentTasks: Array.isArray(item?.currentTasks) ? item.currentTasks.map(normalizeBusinessApplyCurrentTask) : [],
    variables: normalizeVariables(item?.variables),
    extension: normalizeVariables(item?.extension),
    createdAt: normalizeDateTime(item?.createdAt),
    updatedAt: normalizeDateTime(item?.updatedAt),
  };
}

function normalizeBusinessApplyProgress(item: any): WorkflowBusinessApplyProgress {
  return {
    ...item,
    applyId: item?.applyId ? normalizeId(item.applyId) : undefined,
    currentTasks: Array.isArray(item?.currentTasks) ? item.currentTasks.map(normalizeBusinessApplyCurrentTask) : [],
    createdAt: normalizeDateTime(item?.createdAt),
    updatedAt: normalizeDateTime(item?.updatedAt),
  };
}

function normalizeTaskDetail(item: any): WorkflowTaskDetail {
  return {
    task: normalizeTask(item?.task || {}),
    process: normalizeProcessInstance(item?.process || {}),
    formCode: item?.formCode,
    formJson: item?.formJson,
    variables: normalizeVariables(item?.variables),
    formPermissions: normalizeVariables(item?.formPermissions) as Record<string, WorkflowFormPermission>,
    renderConfig: normalizeRenderConfig(item?.renderConfig),
    records: normalizeRecords(item?.records),
  };
}

function normalizeProcessDetail(item: any): WorkflowProcessDetail {
  return {
    process: normalizeProcessInstance(item?.process || {}),
    formCode: item?.formCode,
    formJson: item?.formJson,
    variables: normalizeVariables(item?.variables),
    renderConfig: normalizeRenderConfig(item?.renderConfig),
    records: normalizeRecords(item?.records),
  };
}

function normalizeRenderConfig(item: any): WorkflowRenderConfig | undefined {
  if (!item) {
    return undefined;
  }
  return {
    ...item,
    applyId: item?.applyId ? normalizeId(item.applyId) : undefined,
    formPermissions: normalizeVariables(item?.formPermissions) as Record<string, WorkflowFormPermission>,
    nodeExtension: normalizeVariables(item?.nodeExtension),
    businessPermissions: normalizeVariables(item?.businessPermissions),
  };
}

function normalizeRecords(records: any): WorkflowTaskRecord[] {
  if (!Array.isArray(records)) {
    return [];
  }
  return records.map((item) => ({
    id: item?.id ? normalizeId(item.id) : undefined,
    processInstanceId: item?.processInstanceId ? String(item.processInstanceId) : '',
    taskId: item?.taskId,
    taskName: item?.taskName,
    taskDefinitionKey: item?.taskDefinitionKey,
    action: item?.action || '',
    actionName: item?.actionName || item?.action || '-',
    operatorId: item?.operatorId ? normalizeId(item.operatorId) : undefined,
    operatorName: item?.operatorName,
    comment: item?.comment,
    variables: normalizeVariables(item?.variables),
    createdTime: normalizeDateTime(item?.createdTime),
  }));
}

function normalizeVariables(value: any): Record<string, any> {
  if (!value || Array.isArray(value) || typeof value !== 'object') {
    return {};
  }
  return value;
}

function normalizeDateTime(value: any): string {
  if (!value) return '';
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value;
    return `${year}-${pad(month)}-${pad(day)} ${pad(hour)}:${pad(minute)}:${pad(second)}`;
  }
  return String(value).replace('T', ' ');
}

function pad(value: number): string {
  return String(value).padStart(2, '0');
}
