<template>
  <div class="workflow-approval-timeline">
    <el-timeline v-if="records.length" class="approval-timeline">
      <el-timeline-item
        v-for="record in records"
        :key="record.id || `${record.action}-${record.taskId || ''}-${record.createdTime || ''}`"
        :timestamp="record.createdTime"
        placement="top"
        :type="timelineItemType(record)"
      >
        <div class="approval-record">
          <div class="approval-record-title">
            <strong>{{ record.taskName || '流程节点' }}</strong>
            <el-tag :type="actionTagType(record)" size="small" effect="plain">{{ record.actionName || record.action || '-' }}</el-tag>
          </div>
          <div class="approval-record-meta">
            <span>{{ record.operatorName || '-' }}</span>
            <span v-if="record.taskDefinitionKey">{{ record.taskDefinitionKey }}</span>
          </div>
          <div v-if="record.comment" class="approval-record-comment">{{ record.comment }}</div>
          <slot name="record-extra" :record="record" />
          <el-collapse v-if="showVariables && hasVariables(record)" class="approval-record-vars">
            <el-collapse-item title="节点提交内容" :name="record.id || record.createdTime || record.taskId || record.action">
              <pre>{{ formatVariables(record.variables) }}</pre>
            </el-collapse-item>
          </el-collapse>
        </div>
      </el-timeline-item>
    </el-timeline>
    <el-empty v-else :description="emptyText" />
  </div>
</template>

<script setup lang="ts">
import type { WorkflowTaskRecord } from '../../api/workflow';

defineOptions({ name: 'WorkflowApprovalTimeline' });

withDefaults(defineProps<{
  records?: WorkflowTaskRecord[];
  emptyText?: string;
  showVariables?: boolean;
}>(), {
  records: () => [],
  emptyText: '暂无审批记录',
  showVariables: true,
});

type TimelineType = '' | 'primary' | 'success' | 'warning' | 'danger' | 'info';
type TagType = '' | 'primary' | 'success' | 'warning' | 'danger' | 'info';

function actionTagType(record: WorkflowTaskRecord): TagType {
  const action = String(record.action || record.actionName || '').toUpperCase();
  if (action.includes('REJECT') || action.includes('拒') || action.includes('驳回')) return 'danger';
  if (action.includes('COMPLETE') || action.includes('PASS') || action.includes('APPROVE') || action.includes('通过')) return 'success';
  if (action.includes('TRANSFER') || action.includes('转办')) return 'warning';
  return 'info';
}

function timelineItemType(record: WorkflowTaskRecord): TimelineType {
  const tagType = actionTagType(record);
  return tagType === 'warning' ? 'primary' : tagType;
}

function hasVariables(record: WorkflowTaskRecord) {
  return Boolean(record.variables && Object.keys(record.variables).length);
}

function formatVariables(variables?: Record<string, any>) {
  return JSON.stringify(variables || {}, null, 2);
}
</script>

<style scoped>
.workflow-approval-timeline {
  min-width: 0;
}

.approval-timeline {
  padding-left: 4px;
}

.approval-timeline :deep(.el-timeline-item__tail) {
  border-left-color: var(--el-border-color);
}

.approval-record {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.approval-record-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
}

.approval-record-title strong {
  min-width: 0;
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.approval-record-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.approval-record-meta span + span::before {
  content: '/';
  margin-right: 8px;
  color: var(--el-border-color);
}

.approval-record-comment {
  margin-top: 8px;
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.6;
}

.approval-record-vars {
  margin-top: 8px;
  border-top: 1px solid var(--el-border-color-lighter);
  border-bottom: 0;
}

.approval-record-vars :deep(.el-collapse-item__header) {
  height: 34px;
  border-bottom: 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.approval-record-vars :deep(.el-collapse-item__wrap) {
  border-bottom: 0;
}

.approval-record-vars pre {
  max-height: 220px;
  overflow: auto;
  margin: 0;
  padding: 10px;
  border-radius: 6px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-primary);
  font-size: 12px;
  line-height: 1.5;
}
</style>
