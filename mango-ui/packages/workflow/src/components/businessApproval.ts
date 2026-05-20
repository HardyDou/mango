import type { Component } from 'vue';
import type { WorkflowTaskActionKey } from '../api/workflow';

export type BusinessPermission = 'HIDDEN' | 'READONLY' | 'EDITABLE';
export type WorkflowCommentMode = 'ACTION_BAR' | 'BUSINESS_FORM' | 'NONE';

export interface BusinessApprovalContext {
  businessType: string;
  businessKey?: string;
  applyId?: string;
  processInstanceId?: string;
  taskId?: string;
  taskDefinitionKey?: string;
  nodeName?: string;
  nodeExtension?: Record<string, any>;
  readonly: boolean;
  variables: Record<string, any>;
  permissions: Record<string, BusinessPermission>;
}

export interface BusinessApprovalRegistration {
  component: Component;
  collectVariables?: (context: BusinessApprovalContext, action: WorkflowTaskActionKey) => Record<string, any>;
  commentMode?: WorkflowCommentMode;
  collectComment?: (context: BusinessApprovalContext, action: WorkflowTaskActionKey) => string | undefined;
  validateBeforeAction?: (context: BusinessApprovalContext, action: WorkflowTaskActionKey) => Promise<void>;
  beforeAction?: (context: BusinessApprovalContext, action: WorkflowTaskActionKey) => Promise<void>;
  afterAction?: (context: BusinessApprovalContext, action: WorkflowTaskActionKey, result: unknown) => Promise<void>;
  getActionOverrides?: (context: BusinessApprovalContext) => Partial<Record<WorkflowTaskActionKey, {
    visible?: boolean;
    disabled?: boolean;
    label?: string;
    tooltip?: string;
  }>>;
}

const businessApprovalRegistrations = new Map<string, BusinessApprovalRegistration>();

export function registerBusinessApprovalComponent(key: string, registration: BusinessApprovalRegistration) {
  const normalizedKey = normalizeRegistryKey(key);
  if (!normalizedKey) {
    return;
  }
  businessApprovalRegistrations.set(normalizedKey, registration);
}

export function registerBusinessApprovalComponents(registrations: Record<string, BusinessApprovalRegistration>) {
  Object.entries(registrations).forEach(([key, registration]) => {
    registerBusinessApprovalComponent(key, registration);
  });
}

export function resolveBusinessApprovalRegistration(key?: string): BusinessApprovalRegistration | null {
  return businessApprovalRegistrations.get(normalizeRegistryKey(key)) || null;
}

export function resolveBusinessApprovalComponent(key?: string): Component | null {
  return resolveBusinessApprovalRegistration(key)?.component || null;
}

export function businessTypeOf(variables?: Record<string, any>): string {
  return String(variables?.businessType || variables?.bizType || '').trim();
}

export function applyIdOf(variables?: Record<string, any>): string {
  return String(variables?.applyId || variables?.workflowApplyId || variables?.businessApplyId || variables?.snapshotId || '').trim();
}

export function businessPermissionsOf(
  variables: Record<string, any> | undefined,
  taskDefinitionKey?: string,
): Record<string, BusinessPermission> {
  const permissions = variables?.businessPermissions;
  if (!permissions || typeof permissions !== 'object' || Array.isArray(permissions)) {
    return {};
  }
  const nodePermissions = taskDefinitionKey ? permissions[taskDefinitionKey] : undefined;
  return normalizePermissions(nodePermissions || permissions);
}

export function collectBusinessApprovalVariables(
  registration: BusinessApprovalRegistration | null | undefined,
  context: BusinessApprovalContext | null | undefined,
  action: WorkflowTaskActionKey,
): Record<string, any> {
  if (!registration?.collectVariables || !context) {
    return {};
  }
  return registration.collectVariables(context, action);
}

export function collectBusinessApprovalComment(
  registration: BusinessApprovalRegistration | null | undefined,
  context: BusinessApprovalContext | null | undefined,
  action: WorkflowTaskActionKey,
  fallbackComment = '',
): string {
  if (registration?.collectComment && context) {
    return String(registration.collectComment(context, action) || '').trim();
  }
  return fallbackComment;
}

function normalizePermissions(value: any): Record<string, BusinessPermission> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return {};
  }
  return Object.entries(value).reduce<Record<string, BusinessPermission>>((result, [key, permission]) => {
    if (permission === 'HIDDEN' || permission === 'READONLY' || permission === 'EDITABLE') {
      result[key] = permission;
    }
    return result;
  }, {});
}

function normalizeRegistryKey(key?: string) {
  return String(key || '').trim();
}
