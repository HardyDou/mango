import type { WorkflowFormPermission } from '../../../../api/workflow';

export type WorkflowVariableDataType = 'TEXT' | 'NUMBER' | 'DATE' | 'USER' | 'ORG' | 'POST' | 'ROLE' | string;

export interface WorkflowVariableOption {
  label: string;
  value: string;
  source?: string;
  dataType?: WorkflowVariableDataType;
}

export interface WorkflowVariableGroup {
  label: string;
  options: WorkflowVariableOption[];
}

export interface ConditionRow {
  id: string;
  connector: 'AND' | 'OR';
  variable: string;
  operator: string;
  value: string | number;
}

export interface ConditionGroup {
  id: string;
  connector: 'AND' | 'OR';
  rows: ConditionRow[];
}

export type ConditionEditMode = 'BUILDER' | 'EXPRESSION';

export interface ApprovalTargetOption {
  label: string;
  value: string;
}

export interface ApprovalOrgTreeOption extends ApprovalTargetOption {
  children?: ApprovalOrgTreeOption[];
}

export type FieldPermissionGetter = (field: string) => WorkflowFormPermission;
