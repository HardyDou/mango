<template>
  <el-main
    class="layout-main"
  >
    <el-scrollbar
      ref="layoutMainScrollbarRef"
      class="layout-main-scroll layout-backtop-header-fixed"
      wrap-class="layout-main-scroll"
      view-class="layout-main-scroll"
    >
      <div class="layout-main-body">
        <ShellRuntimeOutlet v-if="contentMode === 'runtime-outlet'" />
        <LayoutParentView v-else />
      </div>
    </el-scrollbar>
    <el-backtop :target="setBacktopClass" />
  </el-main>
</template>

<script setup lang="ts" name="layoutMain">
import { computed, defineAsyncComponent, ref } from 'vue';
import { getMangoAdminShellOptions } from '../../config';
import ShellRuntimeOutlet from '../../runtime/ShellRuntimeOutlet.vue';

const LayoutParentView = defineAsyncComponent(() => import('../routerView/parent.vue'));
const contentMode = computed(() => getMangoAdminShellOptions().contentMode || 'router-view');

const layoutMainScrollbarRef = ref();

const setBacktopClass = computed(() => {
  const wrapRef = layoutMainScrollbarRef.value?.wrapRef;
  // el-backtop expects a CSS selector string when passed as prop
  if (wrapRef) {
    return '.layout-main-scroll';
  }
  return '';
});
</script>

<style scoped lang="scss">
.layout-main {
  --layout-content-space: 16px;
}

.layout-main {
  padding: 0 !important;
  overflow: hidden;
  width: 100%;
  min-height: 0;
  flex: 1;
  background-color: var(--mango-bg-main);
  display: flex;
  flex-direction: column;
}

.layout-main-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.layout-main-body {
  min-height: 100%;
  padding: var(--layout-content-space);
  background: var(--mango-bg-main);
}

:deep(.router-view-parent.is-root) {
  min-height: calc(100vh - var(--mango-header-height) - var(--mango-tags-view-height) - 32px);
}

.layout-backtop-header-fixed {
  height: 100% !important;
}
</style>
