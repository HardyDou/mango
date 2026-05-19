import type { Component } from 'vue';
import DocumentTableApprovalDetail from './business/DocumentTableApprovalDetail.vue';
import ExpenseApprovalDetail from './business/ExpenseApprovalDetail.vue';

export type BusinessPermission = 'HIDDEN' | 'READONLY' | 'EDITABLE';

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

const businessApprovalComponents: Record<string, Component> = {
  CONTRACT_SEAL_APPROVAL: DocumentTableApprovalDetail,
  'workflow.contractSeal.approve': DocumentTableApprovalDetail,
  EXPENSE_REIMBURSEMENT: ExpenseApprovalDetail,
  'workflow.expense.approve': ExpenseApprovalDetail,
};

export function resolveBusinessApprovalComponent(businessType?: string): Component | null {
  if (!businessType) {
    return null;
  }
  return businessApprovalComponents[businessType] || null;
}

export function businessTypeOf(variables?: Record<string, any>): string {
  return String(variables?.businessType || variables?.bizType || '').trim();
}

export function applyIdOf(variables?: Record<string, any>): string {
  return String(variables?.applyId || variables?.snapshotId || variables?.expenseApplyId || '').trim();
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
  businessType: string | undefined,
  variables: Record<string, any> | undefined,
  permissions: Record<string, BusinessPermission>,
): Record<string, any> {
  if (!businessType || !variables) {
    return {};
  }
  if (businessType === 'EXPENSE_REIMBURSEMENT' && permissions.financeReview === 'EDITABLE') {
    return {
      approvedAmount: variables.approvedAmount,
    };
  }
  if (businessType === 'CONTRACT_SEAL_APPROVAL') {
    const result: Record<string, any> = {};
    if (permissions.legalOpinion === 'EDITABLE') {
      result.legalOpinion = variables.legalOpinion;
    }
    if (permissions.financeOpinion === 'EDITABLE') {
      result.financeOpinion = variables.financeOpinion;
    }
    if (permissions.sealKeeperOpinion === 'EDITABLE') {
      result.approvedSealCount = variables.approvedSealCount;
      result.sealKeeperOpinion = variables.sealKeeperOpinion;
    }
    return result;
  }
  return {};
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
