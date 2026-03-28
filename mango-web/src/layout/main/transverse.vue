<template>
  <el-container class="layout-container">
    <LayoutHeader />
    <div class="layout-transverse-menu">
      <NavMenuHorizontal :menu-list="menuList" />
    </div>
    <el-container class="layout-main-container">
      <div class="flex-center layout-backtop">
        <LayoutTagsView v-if="themeConfig.isTagsview" />
        <LayoutMain ref="layoutMainRef" />
      </div>
    </el-container>
  </el-container>
</template>

<script setup lang="ts" name="layoutTransverse">
import { defineAsyncComponent, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useThemeConfig } from '@/stores/themeConfig';
import { useRoutesList } from '@/stores/routesList';
import { useScrollbar } from '@/composables/useScrollbar';

const LayoutHeader = defineAsyncComponent(() => import('../component/header.vue'));
const LayoutMain = defineAsyncComponent(() => import('../component/main.vue'));
const LayoutTagsView = defineAsyncComponent(() => import('../navBars/tagsView/tagsView.vue'));
const NavMenuHorizontal = defineAsyncComponent(() => import('../navMenu/horizontal.vue'));

const route = useRoute();
const layoutMainRef = ref();
const storesThemeConfig = useThemeConfig();
const { themeConfig } = storeToRefs(storesThemeConfig);
const storesRoutesList = useRoutesList();
const { routesList } = storeToRefs(storesRoutesList);

const menuList = ref<any[]>([]);

watch(
  () => routesList.value,
  (newVal) => {
    menuList.value = newVal;
  },
  { immediate: true }
);

const { updateScrollbar, initScrollHeight } = useScrollbar(layoutMainRef);

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
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.layout-transverse-menu {
  flex-shrink: 0;
  background: var(--mango-bg-menu-bar);
  border-bottom: 1px solid var(--mango-border-color);

  :deep(.nav-menu-horizontal) {
    border-bottom: none;
  }
}

.layout-main-container {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.flex-center {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.layout-backtop {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}
</style>
