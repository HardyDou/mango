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
import { defineAsyncComponent, onMounted, ref } from 'vue';
import { useThemeConfig } from '@/stores/themeConfig';
import { useScrollbar } from '@/composables/useScrollbar';

const LayoutAside = defineAsyncComponent(() => import('../component/aside.vue'));
const LayoutHeader = defineAsyncComponent(() => import('../component/header.vue'));
const LayoutMain = defineAsyncComponent(() => import('../component/main.vue'));

const layoutScrollbarRef = ref();
const layoutMainRef = ref();
const storesThemeConfig = useThemeConfig();

const { updateScrollbar, initScrollHeight } = useScrollbar(layoutMainRef, layoutScrollbarRef);

onMounted(() => {
  initScrollHeight();
});
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
</style>
