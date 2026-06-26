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
import { resolveCmsComponent } from './cmsRuntimeMap';
import { isDeprecatedCmsMenu } from './cmsMenuPolicy';
import type { CmsMenu } from './types';

const props = defineProps<{
  menu?: CmsMenu;
  emptyDescription?: string;
}>();

const runtime = inject<MangoAppRuntime | undefined>('mangoRuntime', undefined);

const activeMenu = computed(() => props.menu || runtime?.menu as CmsMenu | undefined);
const activeComponent = computed(() => {
  if (isDeprecatedCmsMenu(activeMenu.value)) {
    return undefined;
  }
  const loader = resolveCmsComponent(activeMenu.value?.component);
  return loader ? defineAsyncComponent(async () => {
    const module = await loader();
    return (module as any).default || module;
  }) : undefined;
});
const emptyText = computed(() => {
  const menu = activeMenu.value;
  if (!menu) {
    return props.emptyDescription || '请选择CMS 内容中心菜单';
  }
  return `缺少CMS 内容中心页面映射：${menu.component || menu.path || menu.menuName}`;
});
</script>
