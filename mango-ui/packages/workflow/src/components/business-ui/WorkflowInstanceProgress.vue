<template>
  <WorkflowNodeTimeline
    :node="node"
    :current-node-key="currentNodeKey"
    :visited-node-keys="visitedNodeKeys"
    :status="status"
    :records="records"
  />
</template>

<script setup lang="ts">
import WorkflowNodeTimeline from '../trace/WorkflowNodeTimeline.vue';
import type { WorkflowInstanceProgressProps } from './types';

defineOptions({ name: 'WorkflowInstanceProgress' });

withDefaults(defineProps<WorkflowInstanceProgressProps>(), {
  node: null,
  currentNodeKey: '',
  visitedNodeKeys: () => [],
  status: '',
  records: () => [],
});
</script>
