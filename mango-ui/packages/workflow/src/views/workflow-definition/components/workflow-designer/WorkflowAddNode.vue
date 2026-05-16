<!--
  Approval-flow add node control for Mango Workflow.
  Layout follows the mature tree-line structure used by AntFlow-Designer
  (Apache-2.0) without importing runtime code from that project.
-->
<template>
  <div class="workflow-add-node">
    <el-popover
      v-model:visible="visible"
      trigger="click"
      width="520"
      placement="right"
      popper-class="workflow-node-picker-popper"
    >
      <template #reference>
        <el-button class="workflow-add-node-button" :icon="Plus" circle type="primary" title="添加节点" />
      </template>
      <div class="node-picker">
        <section v-for="group in groupedCatalog" :key="group.name" class="node-picker-group">
          <div class="node-picker-title">{{ group.name }}</div>
          <div class="node-picker-grid">
            <button v-for="item in group.items" :key="item.nodeDefinitionCode" class="node-picker-item" type="button" @click="selectNode(item)">
              <span class="node-picker-icon" :style="{ '--node-color': item.color || '#2563eb' }">
                <el-icon><component :is="resolveNodeIcon(item)" /></el-icon>
              </span>
              <span class="node-picker-name">{{ item.nodeName }}</span>
            </button>
          </div>
        </section>
        <div v-if="groupedCatalog.length === 0" class="node-picker-empty">暂无可添加节点，请检查系统内置节点目录。</div>
      </div>
    </el-popover>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { Bell, Box, Cloudy, Connection, ForkSpoon, Plus, Setting, Share, User } from '@element-plus/icons-vue';
import type { WorkflowNodeCatalog } from '../../../../api/workflow';

defineOptions({ name: 'WorkflowAddNode' });

const props = defineProps<{
  catalog: WorkflowNodeCatalog[];
}>();

const emit = defineEmits<{
  add: [item: WorkflowNodeCatalog];
}>();

const visible = ref(false);

const commonDesignerNodeTypes = new Set([
  'APPROVAL',
  'CC',
  'EXCLUSIVE_GATEWAY',
  'PARALLEL_GATEWAY',
  'SERVICE_BEAN',
  'SERVICE_HTTP',
  'SERVICE_REMOTE',
  'EVENT_PUBLISH',
]);

const nodeIconMap: Record<string, any> = {
  ROOT: User,
  APPROVAL: User,
  CC: Bell,
  EXCLUSIVE_GATEWAY: ForkSpoon,
  PARALLEL_GATEWAY: Share,
  SERVICE: Setting,
  SERVICE_BEAN: Box,
  SERVICE_HTTP: Cloudy,
  SERVICE_REMOTE: Connection,
  EVENT_PUBLISH: Bell,
  EXCLUSIVE_BRANCH: Share,
};

const groupedCatalog = computed(() => {
  const groupsMap = new Map<string, WorkflowNodeCatalog[]>();
  for (const item of props.catalog) {
    if (item.nodeType === 'ROOT') continue;
    if (!commonDesignerNodeTypes.has(item.nodeType) && !commonDesignerNodeTypes.has(item.nodeDefinitionCode)) continue;
    const list = groupsMap.get(item.groupName) || [];
    list.push(item);
    groupsMap.set(item.groupName, list);
  }
  return Array.from(groupsMap.entries()).map(([name, items]) => ({ name, items }));
});

function selectNode(item: WorkflowNodeCatalog) {
  visible.value = false;
  emit('add', item);
}

function resolveNodeIcon(item: Partial<WorkflowNodeCatalog>) {
  const iconName = 'icon' in item ? item.icon : undefined;
  if (iconName && iconName in nodeIconMap) {
    return nodeIconMap[iconName];
  }
  return nodeIconMap[item.nodeType || ''] || nodeIconMap[item.executionType || ''] || Setting;
}
</script>

<style scoped>
.workflow-add-node {
  position: relative;
  z-index: 3;
  display: inline-flex;
  justify-content: center;
  width: 240px;
  flex-shrink: 0;
  flex-grow: 1;
}

.workflow-add-node::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: -1;
  margin: auto;
  width: var(--workflow-line-width);
  height: 100%;
  background: var(--workflow-line-color);
}

.workflow-add-node-button {
  width: 32px;
  height: 32px;
  margin: 18px 0 28px;
  padding: 0;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.18);
  transition: transform 0.16s ease, box-shadow 0.16s ease;
}

.workflow-add-node-button:hover {
  transform: scale(1.12);
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.24);
}

.node-picker {
  display: grid;
  gap: 14px;
}

.node-picker-group {
  display: grid;
  gap: 8px;
}

.node-picker-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.node-picker-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(72px, 1fr));
  gap: 8px;
}

.node-picker-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 78px;
  padding: 10px 8px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
  text-align: center;
  cursor: pointer;
}

.node-picker-item:hover {
  border-color: color-mix(in srgb, var(--node-color, #2563eb) 42%, var(--el-border-color));
  background: var(--el-fill-color-lighter);
}

.node-picker-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: color-mix(in srgb, var(--node-color, #2563eb) 14%, #fff);
  color: var(--node-color, #2563eb);
}

.node-picker-name {
  min-width: 0;
  width: 100%;
  font-weight: 700;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-picker-empty {
  padding: 18px 12px;
  color: var(--el-text-color-secondary);
  text-align: center;
}
</style>
