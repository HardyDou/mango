<template>
  <div class="workflow-business-layout">
    <header class="workflow-business-layout__header">
      <div class="workflow-business-layout__title">
        <slot name="title">
          <span>{{ title || '流程详情' }}</span>
        </slot>
      </div>
      <div class="workflow-business-layout__actions">
        <slot name="actions" />
        <el-button :icon="ArrowLeft" @click="emit('back')">返回</el-button>
      </div>
    </header>

    <div class="workflow-business-layout__body">
      <main class="workflow-business-layout__main">
        <slot />
      </main>
      <aside v-if="$slots.sidebar" class="workflow-business-layout__sidebar">
        <slot name="sidebar" />
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue';

defineOptions({ name: 'WorkflowLayout' });

withDefaults(defineProps<{
  title?: string;
}>(), {
  title: '流程详情',
});

const emit = defineEmits<{
  back: [];
}>();
</script>

<style scoped>
.workflow-business-layout {
  min-width: 0;
}

.workflow-business-layout__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--el-border-color-light, #dcdfe6);
}

.workflow-business-layout__title {
  min-width: 0;
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 18px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workflow-business-layout__actions {
  display: inline-flex;
  flex: none;
  align-items: center;
  gap: 8px;
}

.workflow-business-layout__body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 16px;
  align-items: start;
}

.workflow-business-layout__main {
  min-width: 0;
}

.workflow-business-layout__sidebar {
  position: sticky;
  top: 12px;
  max-height: calc(100vh - 150px);
  overflow: auto;
}

@media (max-width: 1180px) {
  .workflow-business-layout__body {
    grid-template-columns: 1fr;
  }

  .workflow-business-layout__sidebar {
    position: static;
    max-height: none;
  }
}

@media (max-width: 640px) {
  .workflow-business-layout__header {
    align-items: flex-start;
  }
}
</style>
