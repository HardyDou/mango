<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="min(960px, 92vw)"
    class="workflow-instance-history-dialog"
    append-to-body
    destroy-on-close
  >
    <WorkflowInstanceHistory
      :business-type="businessType"
      :business-key="businessKey"
      :page-size="pageSize"
      :records="records"
      :loading="loading"
      :total="total"
      :title="title"
      :subtitle="subtitle"
      :empty-text="emptyText"
      :show-title="false"
    >
      <template #summary="slotProps">
        <slot name="summary" v-bind="slotProps" />
      </template>
      <template #record-extra="slotProps">
        <slot name="record-extra" v-bind="slotProps" />
      </template>
    </WorkflowInstanceHistory>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import WorkflowInstanceHistory from './WorkflowInstanceHistory.vue';
import type { WorkflowInstanceHistoryQuery } from './types';
import type { WorkflowBusinessApply } from '../../api/workflow';

defineOptions({ name: 'WorkflowInstanceHistoryDialog' });

const props = withDefaults(defineProps<WorkflowInstanceHistoryQuery & {
  modelValue: boolean;
  title?: string;
  subtitle?: string;
  emptyText?: string;
  records?: WorkflowBusinessApply[];
  loading?: boolean;
  total?: number | null;
}>(), {
  modelValue: false,
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

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
}>();

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
});
</script>
