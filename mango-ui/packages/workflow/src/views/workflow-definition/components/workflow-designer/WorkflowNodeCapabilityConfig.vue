<template>
  <div class="workflow-node-capability-config">
    <div class="capability-header">
      <div>
        <h4>处理动作</h4>
        <p>开启节点可用动作，按钮名称会显示在审批页底部。</p>
      </div>
    </div>

    <div class="action-config-table">
      <div class="action-config-head">
        <span>动作</span>
        <span>按钮名称</span>
        <span>意见</span>
      </div>
      <div v-for="action in actionOptions" :key="action.key" class="action-config-row" :class="{ 'is-disabled': actionConfig(action.key).enabled === false }">
        <div class="action-enable-cell">
          <el-switch
            :model-value="actionConfig(action.key).enabled !== false"
            size="small"
            @change="value => updateAction(action.key, { enabled: Boolean(value) })"
          />
          <span>{{ action.defaultLabel }}</span>
        </div>
        <el-input
          :model-value="actionConfig(action.key).label || action.defaultLabel"
          size="small"
          placeholder="按钮名称"
          @input="value => updateAction(action.key, { label: String(value || '') })"
        />
        <el-checkbox
          :model-value="Boolean(actionConfig(action.key).requireComment)"
          @change="value => updateAction(action.key, { requireComment: Boolean(value) })"
        >
          必填
        </el-checkbox>
      </div>
    </div>

    <el-collapse class="capability-extra-collapse">
      <el-collapse-item title="确认提示" name="confirm-text">
        <div class="confirm-text-list">
          <div v-for="action in actionOptions" :key="`${action.key}-confirm`" class="confirm-text-row">
            <span>{{ action.defaultLabel }}</span>
            <el-input
              :model-value="actionConfig(action.key).confirmText || ''"
              size="small"
              placeholder="可不填"
              @input="value => updateAction(action.key, { confirmText: String(value || '') })"
            />
          </div>
        </div>
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<script setup lang="ts">
import type { WorkflowApprovalNodeConfig, WorkflowNodeActionConfig, WorkflowTaskActionKey } from '../../../../api/workflow';

const props = defineProps<{
  config: WorkflowApprovalNodeConfig;
}>();

const emit = defineEmits<{
  update: [patch: Partial<WorkflowApprovalNodeConfig>];
}>();

const actionOptions: Array<{ key: WorkflowTaskActionKey; defaultLabel: string }> = [
  { key: 'complete', defaultLabel: '通过' },
  { key: 'reject', defaultLabel: '驳回' },
  { key: 'save', defaultLabel: '暂存' },
  { key: 'transfer', defaultLabel: '转办' },
  { key: 'addSign', defaultLabel: '加签' },
];

function actionConfig(key: WorkflowTaskActionKey): WorkflowNodeActionConfig {
  return props.config.actions?.[key] || defaultActionConfig(key);
}

function updateAction(key: WorkflowTaskActionKey, patch: WorkflowNodeActionConfig) {
  emit('update', {
    actions: {
      ...defaultActions(),
      ...(props.config.actions || {}),
      [key]: {
        ...actionConfig(key),
        ...patch,
      },
    },
  });
}

function defaultActions(): Record<WorkflowTaskActionKey, WorkflowNodeActionConfig> {
  return actionOptions.reduce((result, action, index) => {
    result[action.key] = defaultActionConfig(action.key, index);
    return result;
  }, {} as Record<WorkflowTaskActionKey, WorkflowNodeActionConfig>);
}

function defaultActionConfig(key: WorkflowTaskActionKey, index = actionOptions.findIndex(item => item.key === key)): WorkflowNodeActionConfig {
  const label = actionOptions.find(item => item.key === key)?.defaultLabel || key;
  return {
    enabled: key === 'complete' || key === 'reject',
    label,
    requireComment: key === 'reject',
    danger: key === 'reject',
    order: (index + 1) * 10,
  };
}
</script>

<style scoped>
.workflow-node-capability-config {
  display: grid;
  gap: 8px;
}

.capability-header h4 {
  margin: 0;
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 700;
}

.capability-header p {
  margin: 2px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.action-config-table {
  display: grid;
  gap: 6px;
}

.action-config-head {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr) 54px;
  gap: 8px;
  padding: 0 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-weight: 700;
}

.action-config-row {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr) 54px;
  gap: 8px;
  align-items: center;
  padding: 6px 8px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-bg-color);
}

.action-config-row.is-disabled {
  background: var(--el-fill-color-extra-light);
}

.action-enable-cell {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 700;
}

.action-config-row :deep(.el-checkbox) {
  height: 24px;
  margin-right: 0;
}

.action-config-row :deep(.el-checkbox__label) {
  padding-left: 5px;
  font-size: 12px;
}

.capability-extra-collapse {
  border-top: 1px solid var(--el-border-color-light);
}

.capability-extra-collapse :deep(.el-collapse-item__header) {
  height: 32px;
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 700;
}

.capability-extra-collapse :deep(.el-collapse-item__content) {
  padding-bottom: 8px;
}

.confirm-text-list {
  display: grid;
  gap: 6px;
}

.confirm-text-row {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-weight: 700;
}
</style>
