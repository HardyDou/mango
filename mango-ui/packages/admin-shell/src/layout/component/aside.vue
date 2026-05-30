<template>
  <div
    v-show="!isTagsViewCurrenFull"
    class="h100"
  >
    <!-- 移动端遮罩层 -->
    <div
      v-if="layoutStore.isMobileMenuOpen"
      class="layout-aside-overlay"
      :class="{ visible: layoutStore.isMobileMenuOpen }"
      @click="onCloseMobileMenu"
    />
    <el-aside
      v-if="setShowAside"
      class="layout-aside"
      :class="[setCollapseStyle, { 'aside-mobile-open': layoutStore.isMobileMenuOpen }]"
    >
      <Logo v-if="layoutStore.isShowLogo && (layoutStore.layout === 'defaults' || layoutStore.layout === 'columns')" />
      <el-scrollbar
        class="flex-auto"
        @mouseenter="onAsideEnterLeave(true)"
        @mouseleave="onAsideEnterLeave(false)"
      >
        <Vertical
          v-if="layoutStore.layout !== 'columns'"
          :menu-list="menuList"
          :disable-collapse="layoutStore.isMobileMenuOpen"
        />
        <Vertical
          v-else
          :menu-list="columnsChildren.length > 0 ? columnsChildren : menuList"
          :disable-collapse="true"
        />
      </el-scrollbar>
    </el-aside>
  </div>
</template>

<script setup lang="ts" name="layoutAside">
import { computed, defineAsyncComponent, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useRoutesList } from '../../stores/routesList';
import { useLayoutStore } from '../../stores/layout';
import { useTagsViewRoutes } from '../../stores/tagsViewRoutes';
import { containsMenuPath, mittBus, type MangoMenuTreeNode } from '@mango/common';

const Logo = defineAsyncComponent(() => import('../logo/index.vue'));
const Vertical = defineAsyncComponent(() => import('../navMenu/vertical.vue'));
const route = useRoute();

const layoutAsideScrollbarRef = ref();
const storesRoutesList = useRoutesList();
const layoutStore = useLayoutStore();
const storesTagsViewRoutes = useTagsViewRoutes();
const { routesList, activeTopRoutePath } = storeToRefs(storesRoutesList);
const { isTagsViewCurrenFull } = storeToRefs(storesTagsViewRoutes);

const menuList = ref<any[]>([]);
const columnsChildren = ref<any[]>([]);
const clientWidth = ref(document.body.clientWidth);

const findRouteTop = (items: MangoMenuTreeNode[], path: string): MangoMenuTreeNode | undefined => {
  for (const item of items) {
    if (containsMenuPath(item, path)) {
      return item;
    }
  }
  return items[0];
};

const getSideMenus = (items: any[], topPath?: string): any[] => {
  if (!items || items.length === 0) {
    return [];
  }
  const activeTop = topPath
    ? items.find(item => item.path === topPath)
    : findRouteTop(items, route.path);
  if (!activeTop) {
    return filterVisibleMenus(items);
  }
  const menus = activeTop.children && activeTop.children.length > 0 ? activeTop.children : [activeTop];
  return filterVisibleMenus(menus);
};

const filterVisibleMenus = <T extends { meta?: { isHide?: boolean }; children?: T[] }>(items: T[]): T[] => {
  return items
    .filter(item => !item.meta?.isHide)
    .map(item => ({
      ...item,
      children: item.children ? filterVisibleMenus(item.children) : item.children,
    }));
};

watch(
  () => [routesList.value, activeTopRoutePath.value, route.path],
  () => {
    if (layoutStore.layout === 'defaults') {
      menuList.value = routesList.value.filter(item => !item.meta?.isHide);
      return;
    }
    if (layoutStore.layout !== 'columns') {
      menuList.value = getSideMenus(routesList.value, activeTopRoutePath.value);
    }
  },
  { immediate: true }
);

// 路由变化时关闭移动端菜单
watch(
  () => route.path,
  () => {
    const matchedTop = findRouteTop(routesList.value, route.path);
    if (matchedTop && matchedTop.path !== activeTopRoutePath.value) {
      storesRoutesList.setActiveTopRoutePath(matchedTop.path);
    }
    layoutStore.closeMobileMenu();
  }
);

const setShowAside = computed(() => {
  return true;
});

const setCollapseStyle = computed(() => {
  // 移动端（<= 1000px）或移动端菜单打开时，不使用 collapse 样式
  if (clientWidth.value <= 1000 || layoutStore.isMobileMenuOpen) {
    return '';
  }
  if (layoutStore.layout === 'columns') {
    return layoutStore.isColumnsAsideOpen ? 'layout-aside-pc-220' : 'layout-aside-pc-0';
  }
  return layoutStore.isCollapse ? 'layout-aside-pc-64' : 'layout-aside-pc-220';
});

const onAsideEnterLeave = (_bool: boolean) => {};

const onCloseMobileMenu = () => {
  layoutStore.closeMobileMenu();
};

// mittBus event handling
let cleanupSendColumns: (() => void) | undefined;
let cleanupRestore: (() => void) | undefined;
let cleanupMobileResize: (() => void) | undefined;

onMounted(() => {
  // Listen for columns children data
  cleanupSendColumns = mittBus.on('setSendColumnsChildren', (data: any) => {
    if (data?.children) {
      columnsChildren.value = data.children;
    }
  });

  // Listen for restore default
  cleanupRestore = mittBus.on('restoreDefault', () => {
    columnsChildren.value = [];
  });

  // Listen for mobile resize events
  cleanupMobileResize = mittBus.on('layoutMobileResize', (res: { isMobile: boolean; windowWidth: number }) => {
    clientWidth.value = res.windowWidth;
    if (!res.isMobile) {
      // Desktop: close mobile menu if open
      if (layoutStore.isMobileMenuOpen) {
        layoutStore.closeMobileMenu();
      }
    }
  });
});

onUnmounted(() => {
  cleanupSendColumns?.();
  cleanupRestore?.();
  cleanupMobileResize?.();
});
</script>

<style scoped lang="scss">
.layout-aside-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1999;
  opacity: 0;
  transition: opacity var(--mango-transition-duration);

  &.visible {
    opacity: 1;
  }
}

.layout-aside {
  background: var(--mango-bg-menu-bar);
  box-shadow: 2px 0 6px rgb(0 21 41 / 8%);
  height: 100%;
  position: relative;
  z-index: 2000;
  display: flex;
  flex-direction: column;
  overflow-x: hidden !important;
  transition: width var(--mango-transition-duration), left var(--mango-transition-duration);
  width: var(--mango-aside-width) !important;
  min-width: var(--mango-aside-width) !important;
  max-width: var(--mango-aside-width) !important;
  flex-shrink: 0;

  &.aside-collapse,
  &.layout-aside-pc-64 {
    width: 64px !important;
    min-width: 64px !important;
    max-width: 64px !important;
  }

  &.layout-aside-pc-1 {
    width: 1px !important;
    min-width: 1px !important;
    max-width: 1px !important;
  }

  &.layout-aside-pc-0 {
    width: 0 !important;
    min-width: 0 !important;
    max-width: 0 !important;
    box-shadow: none;
  }

  &.layout-aside-pc-220 {
    width: 220px !important;
    min-width: 220px !important;
    max-width: 220px !important;
  }

  &.aside-mobile-open {
    left: 0 !important;
    width: var(--mango-aside-width) !important;
    min-width: var(--mango-aside-width) !important;
    max-width: var(--mango-aside-width) !important;
  }

  :deep(.el-scrollbar__view) {
    overflow: hidden;
  }
}

// Columns mode: when hovering on columnsAside, show full menu
:deep(.layout-columns-warp) {
  .layout-aside {
    box-shadow: none;
    border-right: 1px solid var(--mango-border-color);
  }
}
</style>

<style lang="scss">
// Non-scoped for mobile responsive sidebar positioning (1000px breakpoint to match JS)
@media screen and (max-width: 1000px) {
  .layout-aside {
    position: fixed !important;
    left: -220px !important;
    top: 0 !important;
    height: 100vh !important;
    z-index: 2000 !important;
    transition: left var(--mango-transition-duration);

    &.aside-mobile-open {
      left: 0 !important;
      width: var(--mango-aside-width) !important;
      min-width: var(--mango-aside-width) !important;
      max-width: var(--mango-aside-width) !important;
    }
  }
}
</style>
