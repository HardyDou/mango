<template>
  <component
    :is="activeComponent"
    v-if="activeComponent"
  />
  <el-empty
    v-else
    :description="emptyText"
  />
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, inject } from 'vue';
import type { MangoAppRuntime } from '@mango/app-runtime';
import { resolveWorkflowComponent } from './workflowRuntimeMap';
import type { WorkflowMenu } from './types';

const props = defineProps<{
  menu?: WorkflowMenu;
  emptyDescription?: string;
}>();

const runtime = inject<MangoAppRuntime | undefined>('mangoRuntime', undefined);

const activeMenu = computed(() => props.menu || runtime?.menu as WorkflowMenu | undefined);
const activeComponent = computed(() => {
  const loader = resolveWorkflowComponent(activeMenu.value?.component);
  return loader ? defineAsyncComponent(async () => {
    const module = await loader();
    return (module as any).default || module;
  }) : undefined;
});
const emptyText = computed(() => {
  const menu = activeMenu.value;
  if (!menu) {
    return props.emptyDescription || '请选择 Workflow 菜单';
  }
  return `缺少 Workflow 页面映射：${menu.component || menu.path || menu.menuName}`;
});
</script>
