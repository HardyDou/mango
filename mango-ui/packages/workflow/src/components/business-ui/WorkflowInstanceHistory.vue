<template>
  <div class="workflow-instance-history" v-loading="effectiveLoading">
    <div class="workflow-instance-history__header">
      <div>
        <h3>{{ title }}</h3>
        <p>{{ subtitle }}</p>
      </div>
      <span v-if="total !== null">{{ total }} 条</span>
    </div>

    <slot name="summary" :records="records" :loading="loading" />

    <el-timeline v-if="records.length" class="workflow-instance-history__timeline">
      <el-timeline-item
        v-for="record in records"
        :key="record.id || `${record.applyCode || ''}-${record.processInstanceId || ''}-${record.updatedAt || ''}`"
        :timestamp="record.createdAt || record.updatedAt"
        placement="top"
      >
        <div class="workflow-instance-history__record">
          <div class="workflow-instance-history__record-title">
            <strong>{{ record.applyTitle || record.applyCode || '历史申请' }}</strong>
            <el-tag size="small" effect="plain">{{ record.applyStatusName || record.applyStatus || '-' }}</el-tag>
          </div>
          <div class="workflow-instance-history__record-meta">
            <span>{{ record.applicantName || '-' }}</span>
            <span>{{ record.currentTaskNames || record.processName || '-' }}</span>
          </div>
          <div v-if="record.applySummary" class="workflow-instance-history__record-summary">
            {{ record.applySummary }}
          </div>
          <slot name="record-extra" :record="record" />
        </div>
      </el-timeline-item>
    </el-timeline>

    <el-empty v-else-if="!effectiveLoading" :description="emptyText" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { workflowApi, type WorkflowBusinessApply } from '../../api/workflow';
import type { WorkflowInstanceHistoryQuery } from './types';

defineOptions({ name: 'WorkflowInstanceHistory' });

const props = withDefaults(defineProps<WorkflowInstanceHistoryQuery & {
  title?: string;
  subtitle?: string;
  emptyText?: string;
  records?: WorkflowBusinessApply[];
  loading?: boolean;
  total?: number | null;
}>(), {
  businessType: '',
  businessKey: '',
  pageSize: 10,
  title: '历史申请',
  subtitle: '同一业务单据的多次申请记录',
  emptyText: '暂无申请记录',
  records: undefined,
  loading: undefined,
  total: null,
});

const loading = ref(false);
const internalRecords = ref<WorkflowBusinessApply[]>([]);
const internalTotal = ref<number | null>(null);

const records = computed(() => props.records ?? internalRecords.value);
const total = computed(() => props.total ?? internalTotal.value);
const effectiveLoading = computed(() => props.loading ?? loading.value);

watch(
  () => [props.records, props.businessType, props.businessKey, props.pageSize] as const,
  async () => {
    if (props.records || !props.businessType || !props.businessKey) {
      return;
    }
    loading.value = true;
    try {
      const page = await workflowApi.businessApplyHistory(props.businessType, props.businessKey, { pageNum: 1, pageSize: props.pageSize });
      internalRecords.value = page.list;
      internalTotal.value = page.total;
    } catch {
      internalRecords.value = [];
      internalTotal.value = 0;
    } finally {
      loading.value = false;
    }
  },
  { immediate: true },
);
</script>

<style scoped>
.workflow-instance-history {
  min-width: 0;
}

.workflow-instance-history__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.workflow-instance-history__header h3 {
  margin: 0;
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 600;
}

.workflow-instance-history__header p,
.workflow-instance-history__header span {
  margin: 4px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.workflow-instance-history__timeline {
  padding-left: 4px;
}

.workflow-instance-history__record {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.workflow-instance-history__record-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.workflow-instance-history__record-title strong {
  overflow: hidden;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workflow-instance-history__record-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.workflow-instance-history__record-meta span + span::before {
  content: '/';
  margin-right: 8px;
  color: var(--el-border-color);
}

.workflow-instance-history__record-summary {
  margin-top: 8px;
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.6;
}
</style>
