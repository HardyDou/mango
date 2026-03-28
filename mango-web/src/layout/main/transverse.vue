<template>
  <el-container class="layout-container">
    <LayoutHeader />
    <el-container class="layout-mian-height-50">
      <LayoutAside />
      <div class="flex-center layout-backtop">
        <LayoutTagsView v-if="themeConfig.isTagsview" />
        <LayoutMain ref="layoutMainRef" />
      </div>
    </el-container>
  </el-container>
</template>

<script setup lang="ts" name="layoutTransverse">
import { defineAsyncComponent, nextTick, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useThemeConfig } from '@/stores/themeConfig';

const LayoutAside = defineAsyncComponent(() => import('../component/aside.vue'));
const LayoutHeader = defineAsyncComponent(() => import('../component/header.vue'));
const LayoutMain = defineAsyncComponent(() => import('../component/main.vue'));
const LayoutTagsView = defineAsyncComponent(() => import('../navBars/tagsView/tagsView.vue'));

const route = useRoute();
const layoutMainRef = ref();
const storesThemeConfig = useThemeConfig();
const { themeConfig } = storeToRefs(storesThemeConfig);

const updateScrollbar = () => {
  layoutMainRef.value?.layoutMainScrollbarRef?.update();
};

const initScrollHeight = () => {
  nextTick(() => {
    setTimeout(() => {
      updateScrollbar();
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

.layout-mian-height-50 {
  display: flex;
  height: calc(100vh - 56px);
}

.flex-center {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}

.layout-backtop {
  flex: 1;
  overflow: hidden;
}
</style>
