<template>
  <el-container class="layout-container">
    <LayoutHeader />
    <el-container class="layout-mian-height-50">
      <LayoutAside />
      <div class="flex-center layout-backtop">
        <LayoutWorkspaceNav :tags-view="isTagsview" />
        <LayoutMain ref="layoutMainRef" />
      </div>
    </el-container>
  </el-container>
</template>

<script setup lang="ts" name="layoutClassic">
import { defineAsyncComponent, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useLayoutStore } from '../../stores/layout';
import { useScrollbar } from '../../composables/useScrollbar';

const LayoutAside = defineAsyncComponent(() => import('../component/aside.vue'));
const LayoutHeader = defineAsyncComponent(() => import('../component/header.vue'));
const LayoutMain = defineAsyncComponent(() => import('../component/main.vue'));
const LayoutWorkspaceNav = defineAsyncComponent(() => import('../navBars/workspaceNav/index.vue'));

const route = useRoute();
const layoutMainRef = ref();
const layoutStore = useLayoutStore();
const { layout, isCollapse, isTagsview } = storeToRefs(layoutStore);

const { updateScrollbar, initScrollHeight } = useScrollbar(layoutMainRef);

watch(
  () => route.path,
  () => {
    initScrollHeight();
  },
  { immediate: true }
);

// Watch only properties that affect scrollbar layout
watch(
  () => [isCollapse.value, layout.value, isTagsview.value],
  () => {
    updateScrollbar();
  }
);
</script>

<style scoped lang="scss">
.layout-container {
  width: 100%;
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.layout-mian-height-50 {
  display: flex;
  flex: 1;
  min-height: 0;
  height: calc(100vh - 56px);
  overflow: hidden;
}

.flex-center {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  height: 100%;
  overflow: hidden;
}

.layout-backtop {
  flex: 1;
  min-width: 0;
  height: 100%;
  overflow: hidden;
}
</style>
