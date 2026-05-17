/**
 * Workflow API - 工作流配置。
 */

import { del, get, post, put } from '@mango/common/utils/request';

export type WorkflowStatus = 'DRAFT' | 'PUBLISHED' | 'DISABLED';

export interface WorkflowGroup {
  id?: number;
  groupName: string;
  groupCode: string;
  sort?: number;
  status?: number;
  remark?: string;
  createdTime?: string;
  updatedTime?: string;
}

export interface WorkflowDefinition {
  id?: number;
  groupId: number;
  groupName?: string;
  definitionName: string;
  definitionKey: string;
  deploymentId?: string;
  processDefinitionId?: string;
  processDefinitionVersion?: number;
  publishedVersionNo?: number;
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

export interface WorkflowPageQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  groupId?: number | '';
  status?: string;
  categoryCode?: string;
  bpmnType?: string;
  executionType?: string;
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
  id: number;
  definitionId: number;
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
  id?: number;
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

export interface WorkflowNodeDefinition extends WorkflowNodeCatalog {
  createdTime?: string;
  updatedTime?: string;
}

export const workflowApi = {
  groupsPage: (params?: WorkflowPageQuery) => get<any>('/workflow/groups/page', { params: toBackendPageParams(params) })
    .then(data => fromBackendPageResult(data, normalizeGroup, params)),
  groupsList: (status?: number) => get<WorkflowGroup[]>('/workflow/groups/list', { params: { status } })
    .then(list => (Array.isArray(list) ? list.map(normalizeGroup) : [])),
  groupDetail: (id: number) => get<WorkflowGroup>('/workflow/groups/detail', { params: { id } }).then(normalizeGroup),
  createGroup: (data: WorkflowGroup) => post<number>('/workflow/groups', data),
  updateGroup: (data: WorkflowGroup) => put<boolean>('/workflow/groups', data),
  deleteGroup: (id: number) => del<boolean>('/workflow/groups', { params: { id } }),

  definitionsPage: (params?: WorkflowPageQuery) => get<any>('/workflow/definitions/page', { params: toBackendPageParams(params) })
    .then(data => fromBackendPageResult(data, normalizeDefinition, params)),
  definitionDetail: (id: number) => get<WorkflowDefinition>('/workflow/definitions/detail', { params: { id } }).then(normalizeDefinition),
  createDefinition: (data: WorkflowDefinition) => post<number>('/workflow/definitions', data),
  updateDefinition: (data: WorkflowDefinition) => put<boolean>('/workflow/definitions', data),
  deleteDefinition: (id: number) => del<boolean>('/workflow/definitions', { params: { id } }),
  updateDefinitionStatus: (id: number, status: WorkflowStatus) => put<boolean>('/workflow/definitions/status', { id, status }),
  deployDefinition: (id: number) => post<any>('/workflow/definitions/deploy', undefined, { params: { id } }),
  definitionVersions: (definitionId: number) => get<WorkflowDefinitionVersion[]>('/workflow/definitions/versions', { params: { definitionId } })
    .then(list => Array.isArray(list) ? list.map(normalizeVersion) : []),
  definitionVersionDetail: (id: number) => get<WorkflowDefinitionVersion>('/workflow/definitions/version-detail', { params: { id } }).then(normalizeVersion),
  nodeCatalog: () => get<WorkflowNodeCatalog[]>('/workflow/definitions/node-catalog')
    .then(list => Array.isArray(list) ? list.map(normalizeNodeCatalog) : []),

  nodeDefinitionsPage: (params?: WorkflowPageQuery) => get<any>('/workflow/node-definitions/page', { params: toBackendPageParams(params) })
    .then(data => fromBackendPageResult(data, normalizeNodeDefinition, params)),
  nodeDefinitionsList: (status?: number) => get<WorkflowNodeDefinition[]>('/workflow/node-definitions/list', { params: { status } })
    .then(list => Array.isArray(list) ? list.map(normalizeNodeDefinition) : []),
  nodeDefinitionDetail: (id: number) => get<WorkflowNodeDefinition>('/workflow/node-definitions/detail', { params: { id } }).then(normalizeNodeDefinition),
  createNodeDefinition: (data: WorkflowNodeDefinition) => post<number>('/workflow/node-definitions', data),
  updateNodeDefinition: (data: WorkflowNodeDefinition) => put<boolean>('/workflow/node-definitions', data),
  updateNodeDefinitionStatus: (id: number, status: number) => put<boolean>('/workflow/node-definitions/status', { id, status }),
  deleteNodeDefinition: (id: number) => del<boolean>('/workflow/node-definitions', { params: { id } }),
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
      properties: {},
    },
    conditionNodes: [],
    properties: {},
  }, null, 2);
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
  const { pageNum, pageSize, groupId, ...rest } = params;
  return {
    ...rest,
    groupId: groupId === '' ? undefined : groupId,
    page: pageNum,
    size: pageSize,
  };
}

function fromBackendPageResult<T>(
  data: any,
  mapper: (item: any) => T,
  params?: { pageNum?: number; pageSize?: number },
): PageResult<T> {
  const list = Array.isArray(data?.list) ? data.list.map(mapper) : [];
  return {
    list,
    total: Number(data?.total ?? list.length),
    pageNum: Number(data?.page ?? params?.pageNum ?? 1),
    pageSize: Number(data?.size ?? params?.pageSize ?? list.length ?? 10),
  };
}

function normalizeGroup(item: any): WorkflowGroup {
  return {
    ...item,
    createdTime: normalizeDateTime(item?.createdTime),
    updatedTime: normalizeDateTime(item?.updatedTime),
  };
}

function normalizeDefinition(item: any): WorkflowDefinition {
  return {
    ...item,
    designerJson: item?.designerJson || defaultDesignerJson(),
    createdTime: normalizeDateTime(item?.createdTime),
    updatedTime: normalizeDateTime(item?.updatedTime),
    lastDeployTime: normalizeDateTime(item?.lastDeployTime),
  };
}

function normalizeVersion(item: any): WorkflowDefinitionVersion {
  return {
    ...item,
    publishTime: normalizeDateTime(item?.publishTime),
  };
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

function normalizeNodeDefinition(item: any): WorkflowNodeDefinition {
  return {
    ...normalizeNodeCatalog(item),
    id: item?.id,
    status: item?.status,
    createdTime: normalizeDateTime(item?.createdTime),
    updatedTime: normalizeDateTime(item?.updatedTime),
  };
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
