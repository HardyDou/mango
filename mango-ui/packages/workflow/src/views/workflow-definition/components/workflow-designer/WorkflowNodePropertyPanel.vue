<template>
  <transition name="node-panel-slide">
    <aside v-if="visible && node" class="workflow-node-property-panel node-panel node-panel-floating" :aria-label="title">
      <div class="node-panel-header">
        <div class="node-panel-heading">
          <span class="node-panel-icon">
            <el-icon><component :is="icon" /></el-icon>
          </span>
          <div>
            <strong>{{ title }}</strong>
            <el-tag effect="plain" size="small">{{ typeLabel }}</el-tag>
          </div>
        </div>
        <el-button :icon="Close" aria-label="关闭节点配置" circle text @click="$emit('close')" />
      </div>
      <div class="node-panel-body">
        <slot />
      </div>
    </aside>
  </transition>
</template>

<script setup lang="ts">
import { Close } from '@element-plus/icons-vue';
import type { WorkflowDesignerNode } from '../../../../api/workflow';

defineProps<{
  visible: boolean;
  node?: WorkflowDesignerNode;
  title: string;
  typeLabel: string;
  icon: any;
}>();

defineEmits<{
  close: [];
}>();
</script>

<style scoped>
.node-panel-floating {
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 260px);
  max-height: calc(100vh - 260px);
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  background: var(--el-bg-color);
  box-shadow: 0 10px 32px rgba(15, 23, 42, 0.08);
}

.node-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 16px 14px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.node-panel-heading {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.node-panel-heading strong {
  display: block;
  margin-bottom: 4px;
  color: var(--el-text-color-primary);
  font-size: 15px;
  font-weight: 700;
}

.node-panel-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  border-radius: 10px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-size: 18px;
}

.node-panel-body {
  flex: 1;
  overflow: auto;
  padding: 16px;
}

.node-panel-slide-enter-active,
.node-panel-slide-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.node-panel-slide-enter-from,
.node-panel-slide-leave-to {
  opacity: 0;
  transform: translateX(12px);
}
</style>
