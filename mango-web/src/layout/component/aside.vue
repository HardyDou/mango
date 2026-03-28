<template>
  <div class="h100" v-show="!isTagsViewCurrenFull">
    <!-- 移动端遮罩层 -->
    <div
      v-if="themeConfig.isMobileMenuOpen"
      class="layout-aside-overlay"
      :class="{ visible: themeConfig.isMobileMenuOpen }"
      @click="onCloseMobileMenu"
    />
    <el-aside
      class="layout-aside"
      :class="[setCollapseStyle, { 'aside-mobile-open': themeConfig.isMobileMenuOpen }]"
      v-if="setShowAside"
    >
      <Logo v-if="themeConfig.isShowLogo && (themeConfig.layout === 'defaults' || themeConfig.layout === 'columns')" />
      <el-scrollbar
        class="flex-auto"
        @mouseenter="onAsideEnterLeave(true)"
        @mouseleave="onAsideEnterLeave(false)"
      >
        <Vertical v-if="themeConfig.layout !== 'columns'" :menu-list="menuList" :disable-collapse="themeConfig.isMobileMenuOpen" />
        <Vertical v-else :menu-list="columnsChildren.length > 0 ? columnsChildren : menuList" :disable-collapse="true" />
      </el-scrollbar>
    </el-aside>
  </div>
</template>

<script setup lang="ts" name="layoutAside">
import { computed, defineAsyncComponent, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useRoutesList } from '@/stores/routesList';
import { useThemeConfig } from '@/stores/themeConfig';
import { useTagsViewRoutes } from '@/stores/tagsViewRoutes';
import { mittBus } from '@/utils/mitt';

const Logo = defineAsyncComponent(() => import('../logo/index.vue'));
const Vertical = defineAsyncComponent(() => import('../navMenu/vertical.vue'));
const route = useRoute();

const layoutAsideScrollbarRef = ref();
const storesRoutesList = useRoutesList();
const storesThemeConfig = useThemeConfig();
const storesTagsViewRoutes = useTagsViewRoutes();
const { routesList, isColumnsMenuHover, isColumnsNavHover } = storeToRefs(storesRoutesList);
const { themeConfig } = storeToRefs(storesThemeConfig);
const { isTagsViewCurrenFull } = storeToRefs(storesTagsViewRoutes);

const menuList = ref<any[]>([]);
const columnsChildren = ref<any[]>([]);

watch(
  () => routesList.value,
  (newVal) => {
    menuList.value = newVal;
  },
  { immediate: true }
);

// 路由变化时关闭移动端菜单
watch(
  () => route.path,
  () => {
    storesThemeConfig.closeMobileMenu();
  }
);

const setShowAside = computed(() => {
  const { layout } = themeConfig.value;
  // 分栏布局时，鼠标悬停在分栏菜单上才显示侧边菜单
  if (layout === 'columns') {
    return isColumnsMenuHover.value;
  }
  return true;
});

const setCollapseStyle = computed(() => {
  if (themeConfig.value.layout === 'columns') {
    return 'aside-collapse';
  }
  return themeConfig.value.isCollapse ? 'aside-collapse' : '';
});

const onAsideEnterLeave = (bool: boolean) => {
  if (themeConfig.value.layout === 'columns') {
    storesRoutesList.setColumnsMenuHover(bool);
  }
};

const onCloseMobileMenu = () => {
  storesThemeConfig.closeMobileMenu();
};

// mittBus event handling
let cleanupSendColumns: (() => void) | undefined;
let cleanupRestore: (() => void) | undefined;

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
});

onUnmounted(() => {
  cleanupSendColumns?.();
  cleanupRestore?.();
});

// When in columns mode, watch for hover state to update menu
watch(
  isColumnsMenuHover,
  (newVal) => {
    if (!newVal) {
      columnsChildren.value = [];
    }
  }
);
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

  &.aside-collapse {
    width: 64px !important;
    min-width: 64px !important;
    max-width: 64px !important;
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
    width: var(--mango-aside-width) !important;
    min-width: var(--mango-aside-width) !important;
    max-width: var(--mango-aside-width) !important;
    box-shadow: none;
  }
}
</style>

<style lang="scss">
// Non-scoped for mobile responsive sidebar positioning
@media screen and (max-width: 768px) {
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
