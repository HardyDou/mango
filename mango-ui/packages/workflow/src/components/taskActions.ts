import type { WorkflowNodeActionConfig, WorkflowTaskActionKey } from '../api/workflow';

export type WorkflowActionOverrides = Partial<Record<WorkflowTaskActionKey, {
  visible?: boolean;
  disabled?: boolean;
  label?: string;
  tooltip?: string;
}>>;

export interface WorkflowResolvedAction extends WorkflowNodeActionConfig {
  key: WorkflowTaskActionKey;
  label: string;
  disabled: boolean;
  tooltip: string;
  visible: boolean;
  buttonType: 'primary' | 'danger' | 'default';
}

export const WORKFLOW_ACTION_DEFAULTS: Record<WorkflowTaskActionKey, WorkflowNodeActionConfig> = {
  save: { enabled: false, label: '暂存', order: 10 },
  transfer: { enabled: false, label: '转办', order: 20 },
  addSign: { enabled: false, label: '加签', order: 30 },
  reject: { enabled: true, label: '驳回', requireComment: true, danger: true, order: 40 },
  complete: { enabled: true, label: '通过', requireComment: false, order: 50 },
  claim: { enabled: false, label: '认领', requireComment: false, order: 5 },
  unclaim: { enabled: false, label: '释放', requireComment: false, order: 6 },
  read: { enabled: false, label: '已阅', requireComment: false, order: 60 },
};

export function defaultWorkflowActionLabel(action: WorkflowTaskActionKey) {
  return WORKFLOW_ACTION_DEFAULTS[action].label || action;
}

export function normalizeWorkflowNodeActions(
  nodeActions?: Partial<Record<WorkflowTaskActionKey, WorkflowNodeActionConfig>>,
): Record<WorkflowTaskActionKey, WorkflowNodeActionConfig> {
  return {
    ...WORKFLOW_ACTION_DEFAULTS,
    ...(nodeActions || {}),
  };
}

export function resolveVisibleWorkflowActions(
  nodeActions?: Partial<Record<WorkflowTaskActionKey, WorkflowNodeActionConfig>>,
  overrides?: WorkflowActionOverrides,
): WorkflowResolvedAction[] {
  return Object.entries(normalizeWorkflowNodeActions(nodeActions))
    .map(([key, action]) => {
      const actionKey = key as WorkflowTaskActionKey;
      const override = overrides?.[actionKey] || {};
      const label = override.label || action.label || defaultWorkflowActionLabel(actionKey);
      return {
        key: actionKey,
        ...action,
        ...override,
        label,
        disabled: Boolean(action.disabled || override.disabled),
        tooltip: override.tooltip || action.tooltip || '',
        visible: override.visible ?? action.enabled !== false,
        buttonType: actionKey === 'complete' ? 'primary' : action.danger || actionKey === 'reject' ? 'danger' : 'default',
      };
    })
    .filter(action => action.visible)
    .sort((a, b) => Number(a.order || 0) - Number(b.order || 0));
}

export function isWorkflowCommentRequired(actionConfig: Pick<WorkflowNodeActionConfig, 'requireComment'>, comment?: string) {
  return Boolean(actionConfig.requireComment && !String(comment || '').trim());
}
