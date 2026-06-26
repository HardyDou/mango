<template>
  <div class="workflow-instance-summary">
    <div v-if="summary.currentNodeName" class="workflow-instance-summary__item">
      <span>当前节点</span>
      <strong>{{ summary.currentNodeName }}</strong>
    </div>
    <div v-if="summary.status" class="workflow-instance-summary__item">
      <span>状态</span>
      <strong>{{ summary.status }}</strong>
    </div>
    <div class="workflow-instance-summary__item">
      <span>发起人</span>
      <strong>{{ summary.initiatorName || '-' }}</strong>
    </div>
    <div class="workflow-instance-summary__item">
      <span>办理人</span>
      <strong>{{ summary.assigneeName || '-' }}</strong>
    </div>
    <div class="workflow-instance-summary__item is-wide">
      <span>开始时间</span>
      <strong>{{ summary.startTime || '-' }}</strong>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { WorkflowInstanceSummaryData } from './types';

defineOptions({ name: 'WorkflowInstanceSummary' });

withDefaults(defineProps<{
  summary?: WorkflowInstanceSummaryData;
}>(), {
  summary: () => ({}),
});
</script>

<style scoped>
.workflow-instance-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 12px;
}

.workflow-instance-summary__item {
  min-width: 0;
}

.workflow-instance-summary__item.is-wide {
  grid-column: 1 / -1;
}

.workflow-instance-summary__item span {
  display: block;
  margin-bottom: 2px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.workflow-instance-summary__item strong {
  display: block;
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 760px) {
  .workflow-instance-summary {
    grid-template-columns: 1fr;
  }
}
</style>
