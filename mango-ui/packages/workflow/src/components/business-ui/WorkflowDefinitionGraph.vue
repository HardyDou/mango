<template>
  <WorkflowProgressTree
    :node="node"
    :current-node-key="currentNodeKey"
    :visited-node-keys="visitedNodeKeys"
    :status="status"
  />
</template>

<script setup lang="ts">
import WorkflowProgressTree from '../trace/WorkflowProgressTree.vue';
import type { WorkflowDefinitionGraphProps } from './types';

defineOptions({ name: 'WorkflowDefinitionGraph' });

withDefaults(defineProps<WorkflowDefinitionGraphProps>(), {
  node: null,
  currentNodeKey: '',
  visitedNodeKeys: () => [],
  status: '',
});
</script>
