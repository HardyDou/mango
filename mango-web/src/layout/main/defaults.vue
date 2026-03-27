<template>
  <el-container class="layout-container">
    <LayoutAside />
    <el-container class="layout-container-view h100">
      <el-scrollbar ref="layoutScrollbarRef" class="layout-backtop">
        <LayoutHeader />
        <LayoutMain ref="layoutMainRef" />
      </el-scrollbar>
    </el-container>
  </el-container>
</template>

<script setup lang="ts" name="layoutDefaults">
import { defineAsyncComponent, nextTick, ref } from 'vue';
import { useThemeConfig } from '@/stores/themeConfig';

const LayoutAside = defineAsyncComponent(() => import('../component/aside.vue'));
const LayoutHeader = defineAsyncComponent(() => import('../component/header.vue'));
const LayoutMain = defineAsyncComponent(() => import('../component/main.vue'));

const layoutScrollbarRef = ref();
const layoutMainRef = ref();
const storesThemeConfig = useThemeConfig();

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

onMounted(() => {
  initScrollHeight();
});
</script>
