<template>
  <el-main
    class="layout-main"
  >
    <div class="layout-main-breadcrumb">
      <BreadcrumbIndex />
    </div>
    <el-scrollbar
      ref="layoutMainScrollbarRef"
      class="layout-main-scroll layout-backtop-header-fixed"
      wrap-class="layout-main-scroll"
      view-class="layout-main-scroll"
    >
      <LayoutParentView />
    </el-scrollbar>
    <el-backtop :target="setBacktopClass" />
  </el-main>
</template>

<script setup lang="ts" name="layoutMain">
import { computed, defineAsyncComponent, ref } from 'vue';

const LayoutParentView = defineAsyncComponent(() => import('../routerView/parent.vue'));
const BreadcrumbIndex = defineAsyncComponent(() => import('../navBars/breadcrumb/breadcrumb.vue'));

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
  --layout-content-space: 12px;
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

.layout-main-breadcrumb {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  min-height: 28px;
  padding: 0 var(--layout-content-space);
  background: var(--mango-bg-main);
}

:deep(.router-view-parent.is-root) {
  padding: 6px var(--layout-content-space) var(--layout-content-space);
}

.layout-backtop-header-fixed {
  height: 100% !important;
}
</style>
