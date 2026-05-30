<template>
  <el-container class="layout-container">
    <LayoutHeader />
    <div class="layout-transverse-menu">
      <NavMenuHorizontal :menu-list="menuList" />
    </div>
    <el-container class="layout-main-container">
      <div class="flex-center layout-backtop">
        <LayoutWorkspaceNav :tags-view="isTagsview" />
        <LayoutMain ref="layoutMainRef" />
      </div>
    </el-container>
  </el-container>
</template>

<script setup lang="ts" name="layoutTransverse">
import { defineAsyncComponent, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useLayoutStore } from '../../stores/layout';
import { useRoutesList } from '../../stores/routesList';
import { useScrollbar } from '../../composables/useScrollbar';
import { containsMenuPath } from '@mango/common';

const LayoutHeader = defineAsyncComponent(() => import('../component/header.vue'));
const LayoutMain = defineAsyncComponent(() => import('../component/main.vue'));
const LayoutWorkspaceNav = defineAsyncComponent(() => import('../navBars/workspaceNav/index.vue'));
const NavMenuHorizontal = defineAsyncComponent(() => import('../navMenu/horizontal.vue'));

const route = useRoute();
const layoutMainRef = ref();
const layoutStore = useLayoutStore();
const { isCollapse, isFixedHeader, isTagsview, layout } = storeToRefs(layoutStore);
const storesRoutesList = useRoutesList();
const { routesList, activeTopRoutePath } = storeToRefs(storesRoutesList);

const menuList = ref<any[]>([]);

watch(
  () => [routesList.value, activeTopRoutePath.value, route.path],
  () => {
    const activeTop = activeTopRoutePath.value
      ? routesList.value.find(item => item.path === activeTopRoutePath.value)
      : routesList.value.find(item => containsMenuPath(item, route.path));
    menuList.value = activeTop?.children?.length ? activeTop.children : [];
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

// Watch only properties that affect scrollbar layout
watch(
  () => [
    isCollapse.value,
    isFixedHeader.value,
    isTagsview.value,
    layout.value
  ],
  () => {
    updateScrollbar();
  }
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
