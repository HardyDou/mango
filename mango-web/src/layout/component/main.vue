<template>
  <el-main
    class="layout-main"
    :style="themeConfig.isFixedHeader ? `height: calc(100% - ${setMainHeight})` : `minHeight: calc(100% - ${setMainHeight})`"
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
import { storeToRefs } from 'pinia';
import { useThemeConfig } from '@/stores/themeConfig';
import { useTagsViewRoutes } from '@/stores/tagsViewRoutes';

const LayoutParentView = defineAsyncComponent(() => import('../routerView/parent.vue'));

const layoutMainScrollbarRef = ref();
const storesThemeConfig = useThemeConfig();
const storesTagsViewRoutes = useTagsViewRoutes();
const { themeConfig } = storeToRefs(storesThemeConfig);

const setMainHeight = computed(() => {
  let headerHeight = themeConfig.value.isTagsview ? '36px' : '0px';
  return themeConfig.value.isFixedHeader
    ? `calc(100% - ${themeConfig.value.isTagsview ? '92px' : '56px'})`
    : `calc(100% - ${themeConfig.value.isTagsview ? '92px' : '56px'})`;
});

const setBacktopClass = computed(() => {
  return layoutMainScrollbarRef.value?.wrapRef;
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
