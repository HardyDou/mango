<template>
  <el-container class="layout-container">
    <LayoutAside />
    <el-container class="layout-container-view h100">
      <div class="layout-backtop layout-backtop--fixed-shell">
        <LayoutHeader />
        <LayoutWorkspaceNav :tags-view="isTagsView" />
        <LayoutMain ref="layoutMainRef" />
      </div>
    </el-container>
  </el-container>
</template>

<script setup lang="ts" name="layoutDefaults">
import { defineAsyncComponent, computed, ref } from 'vue';
import { useLayoutStore } from '../../stores/layout';

const LayoutAside = defineAsyncComponent(() => import('../component/aside.vue'));
const LayoutHeader = defineAsyncComponent(() => import('../component/header.vue'));
const LayoutMain = defineAsyncComponent(() => import('../component/main.vue'));
const LayoutWorkspaceNav = defineAsyncComponent(() => import('../navBars/workspaceNav/index.vue'));

const layoutMainRef = ref();
const layoutStore = useLayoutStore();

const isTagsView = computed(() => layoutStore.isTagsview);
</script>

<style scoped lang="scss">
.layout-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: row;
}

.layout-container-view {
  flex: 1;
  flex-direction: column;
  min-width: 0;
  width: 100%;
  height: 100%;
}

.h100 {
  height: 100%;
}

.layout-backtop {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.layout-backtop--fixed-shell {
  min-height: 0;
  height: 100%;
}
</style>
