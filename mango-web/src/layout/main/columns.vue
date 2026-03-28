<template>
  <el-container class="layout-container">
    <ColumnsAside />
    <el-container class="layout-columns-warp layout-container-view h100">
      <LayoutAside />
      <el-scrollbar ref="layoutScrollbarRef" class="layout-backtop">
        <LayoutHeader />
        <LayoutMain ref="layoutMainRef" />
      </el-scrollbar>
    </el-container>
  </el-container>
</template>

<script setup lang="ts" name="layoutColumns">
import { defineAsyncComponent, nextTick, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useThemeConfig } from '@/stores/themeConfig';

const LayoutAside = defineAsyncComponent(() => import('../component/aside.vue'));
const LayoutHeader = defineAsyncComponent(() => import('../component/header.vue'));
const LayoutMain = defineAsyncComponent(() => import('../component/main.vue'));
const ColumnsAside = defineAsyncComponent(() => import('../component/columnsAside.vue'));

const layoutScrollbarRef = ref();
const layoutMainRef = ref();
const route = useRoute();
const storesThemeConfig = useThemeConfig();
const { themeConfig } = storeToRefs(storesThemeConfig);

const updateScrollbar = () => {
  layoutScrollbarRef.value?.update();
  layoutMainRef.value?.layoutMainScrollbarRef?.update();
};

const initScrollHeight = () => {
  nextTick(() => {
    setTimeout(() => {
      updateScrollbar();
      layoutScrollbarRef.value?.wrapRef && (layoutScrollbarRef.value.wrapRef.scrollTop = 0);
      layoutMainRef.value?.layoutMainScrollbarRef?.wrapRef &&
        (layoutMainRef.value.layoutMainScrollbarRef.wrapRef.scrollTop = 0);
    }, 500);
  });
};

watch(
  () => route.path,
  () => {
    initScrollHeight();
  },
  { immediate: true }
);

watch(
  themeConfig,
  () => {
    updateScrollbar();
  },
  { deep: true }
);
</script>

<style scoped lang="scss">
.layout-container {
  width: 100%;
  height: 100%;
}

.layout-columns-warp {
  display: flex;
  flex-direction: column;
}

.layout-container-view {
  flex-direction: column;
}

.h100 {
  height: 100%;
}

.layout-backtop {
  flex: 1;
  overflow: hidden;
}
</style>
