<template>
  <el-main
    class="layout-main"
    :style="layoutStore.isFixedHeader ? `height: calc(100% - ${setMainHeight})` : `minHeight: calc(100% - ${setMainHeight})`"
  >
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
import { useLayoutStore } from '@/stores/layout';

const LayoutParentView = defineAsyncComponent(() => import('../routerView/parent.vue'));

const layoutMainScrollbarRef = ref();
const layoutStore = useLayoutStore();

const setMainHeight = computed(() => {
  return layoutStore.isFixedHeader
    ? `calc(100% - ${layoutStore.isTagsview ? '92px' : '56px'})`
    : `calc(100% - ${layoutStore.isTagsview ? '92px' : '56px'})`;
});

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
  padding: 0 !important;
  overflow: hidden;
  width: 100%;
  height: 100%;
  background-color: var(--mango-bg-main);
}

.layout-main-scroll {
  height: 100%;
  overflow-y: auto;
}

.layout-backtop-header-fixed {
  height: 100% !important;
}
</style>
