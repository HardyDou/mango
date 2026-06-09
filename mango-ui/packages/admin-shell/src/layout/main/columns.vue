<template>
  <el-container class="layout-container">
    <ColumnsAside />
    <el-container class="layout-columns-warp layout-container-view h100">
      <LayoutAside />
      <div class="layout-backtop layout-backtop--fixed-shell">
        <LayoutHeader />
        <LayoutWorkspaceNav :tags-view="isTagsview" />
        <LayoutMain ref="layoutMainRef" />
      </div>
    </el-container>
  </el-container>
</template>

<script setup lang="ts" name="layoutColumns">
import { defineAsyncComponent, ref } from 'vue';
import { storeToRefs } from 'pinia';
import { useLayoutStore } from '../../stores/layout';

const LayoutAside = defineAsyncComponent(() => import('../component/aside.vue'));
const LayoutHeader = defineAsyncComponent(() => import('../component/header.vue'));
const LayoutMain = defineAsyncComponent(() => import('../component/main.vue'));
const ColumnsAside = defineAsyncComponent(() => import('../component/columnsAside.vue'));
const LayoutWorkspaceNav = defineAsyncComponent(() => import('../navBars/workspaceNav/index.vue'));

const layoutMainRef = ref();
const layoutStore = useLayoutStore();
const { isTagsview } = storeToRefs(layoutStore);
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
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
}

.layout-backtop--fixed-shell {
  height: 100%;
}
</style>
