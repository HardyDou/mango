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
import { resolveRbacComponent } from './rbacRuntimeMap';
import type { RbacMenu } from './types';

const props = defineProps<{
  menu?: RbacMenu;
  emptyDescription?: string;
}>();

const runtime = inject<MangoAppRuntime | undefined>('mangoRuntime', undefined);

const activeMenu = computed(() => props.menu || runtime?.menu as RbacMenu | undefined);
const activeComponent = computed(() => {
  const loader = resolveRbacComponent(activeMenu.value?.component);
  return loader ? defineAsyncComponent(async () => {
    const module = await loader();
    return (module as any).default || module;
  }) : undefined;
});
const emptyText = computed(() => {
  const menu = activeMenu.value;
  if (!menu) {
    return props.emptyDescription || '请选择 RBAC 菜单';
  }
  return `缺少 RBAC 页面映射：${menu.component || menu.path || menu.menuName}`;
});
</script>
