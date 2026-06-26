import type {
  WorkflowBusinessApply,
  WorkflowDesignerNode,
  WorkflowTaskRecord,
} from '../../api/workflow';

export interface WorkflowInstanceSummaryData {
  currentNodeName?: string;
  status?: string;
  initiatorName?: string;
  assigneeName?: string;
  startTime?: string;
}

export interface WorkflowDefinitionGraphProps {
  node?: WorkflowDesignerNode | null;
  currentNodeKey?: string;
  visitedNodeKeys?: string[];
  status?: string;
}

export interface WorkflowInstanceProgressProps extends WorkflowDefinitionGraphProps {
  records?: WorkflowTaskRecord[];
}

export interface WorkflowInstanceHistoryQuery {
  businessType?: string;
  businessKey?: string;
  pageSize?: number;
}

export interface WorkflowInstanceHistoryRecord extends WorkflowBusinessApply {}

export type WorkflowSidebarRecordMode = 'PROGRESS' | 'APPROVAL_RECORDS' | 'CUSTOM' | 'HIDDEN';
