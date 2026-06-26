<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="min(960px, 92vw)"
    append-to-body
    destroy-on-close
    class="workflow-definition-graph-dialog"
  >
    <div class="workflow-definition-graph-dialog__body">
      <WorkflowDefinitionGraph
        :node="node"
        :current-node-key="currentNodeKey"
        :visited-node-keys="visitedNodeKeys"
        :status="status"
      />
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import WorkflowDefinitionGraph from './WorkflowDefinitionGraph.vue';
import type { WorkflowDefinitionGraphProps } from './types';

defineOptions({ name: 'WorkflowDefinitionGraphDialog' });

const props = withDefaults(defineProps<WorkflowDefinitionGraphProps & {
  modelValue: boolean;
  title?: string;
}>(), {
  modelValue: false,
  title: '流程图',
  node: null,
  currentNodeKey: '',
  visitedNodeKeys: () => [],
  status: '',
});

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
}>();

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
});
</script>

<style scoped>
.workflow-definition-graph-dialog__body {
  max-width: 100%;
  overflow: auto;
}
</style>
