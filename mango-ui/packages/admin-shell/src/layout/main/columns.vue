<template>
  <el-container class="layout-container">
    <ColumnsAside />
    <el-container class="layout-columns-warp layout-container-view h100">
      <LayoutAside />
      <el-scrollbar
        ref="layoutScrollbarRef"
        class="layout-backtop"
      >
        <LayoutHeader />
        <LayoutWorkspaceNav :tags-view="isTagsview" />
        <LayoutMain ref="layoutMainRef" />
      </el-scrollbar>
    </el-container>
  </el-container>
</template>

<script setup lang="ts" name="layoutColumns">
import { defineAsyncComponent, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useLayoutStore } from '../../stores/layout';
import { useScrollbar } from '../../composables/useScrollbar';

const LayoutAside = defineAsyncComponent(() => import('../component/aside.vue'));
const LayoutHeader = defineAsyncComponent(() => import('../component/header.vue'));
const LayoutMain = defineAsyncComponent(() => import('../component/main.vue'));
const ColumnsAside = defineAsyncComponent(() => import('../component/columnsAside.vue'));
const LayoutWorkspaceNav = defineAsyncComponent(() => import('../navBars/workspaceNav/index.vue'));

const layoutScrollbarRef = ref();
const layoutMainRef = ref();
const route = useRoute();
const layoutStore = useLayoutStore();
const { isCollapse, isFixedHeader, isTagsview, layout } = storeToRefs(layoutStore);

const { updateScrollbar, initScrollHeight } = useScrollbar(layoutMainRef, layoutScrollbarRef);

watch(
  () => route.path,
  () => {
    initScrollHeight();
  },
  { immediate: true }
);

// Watch only properties that affect scrollbar layout
watch(
  () => [
    isCollapse.value,
    isFixedHeader.value,
    isTagsview.value,
    layout.value
  ],
  () => {
    updateScrollbar();
  }
);
</script>

<style scoped lang="scss">
.layout-container {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: row;
  overflow: hidden;
}

.layout-columns-warp {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.layout-container-view {
  display: flex;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.h100 {
  height: 100%;
}

.layout-backtop {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}
</style>
