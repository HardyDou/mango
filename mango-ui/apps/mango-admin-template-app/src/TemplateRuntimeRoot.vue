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
import { resolveTemplateComponent } from './templateRuntimeMap';
import type { TemplateMenu } from './types';

const props = defineProps<{
  menu?: TemplateMenu;
  emptyDescription?: string;
}>();

const runtime = inject<MangoAppRuntime | undefined>('mangoRuntime', undefined);

const activeMenu = computed(() => props.menu || runtime?.menu as TemplateMenu | undefined);
const activeComponent = computed(() => {
  const loader = resolveTemplateComponent(activeMenu.value?.component);
  return loader ? defineAsyncComponent(async () => {
    const module = await loader();
    return (module as any).default || module;
  }) : undefined;
});
const emptyText = computed(() => {
  const menu = activeMenu.value;
  if (!menu) {
    return props.emptyDescription || '请选择模板中心菜单';
  }
  return `缺少模板中心页面映射：${menu.component || menu.path || menu.menuName}`;
});
</script>
