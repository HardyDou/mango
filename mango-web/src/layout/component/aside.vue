<template>
  <div class="h100" v-show="!isTagsViewCurrenFull">
    <el-aside
      class="layout-aside"
      :class="setCollapseStyle"
      v-if="setShowAside"
    >
      <Logo v-if="themeConfig.isShowLogo && (themeConfig.layout === 'defaults' || themeConfig.layout === 'columns')" />
      <el-scrollbar
        class="flex-auto"
        @mouseenter="onAsideEnterLeave(true)"
        @mouseleave="onAsideEnterLeave(false)"
      >
        <Vertical v-if="themeConfig.layout !== 'columns'" :menu-list="menuList" />
        <Vertical v-else :menu-list="columnsChildren.length > 0 ? columnsChildren : menuList" :disable-collapse="true" />
      </el-scrollbar>
    </el-aside>
  </div>
</template>

<script setup lang="ts" name="layoutAside">
import { computed, defineAsyncComponent, onMounted, onUnmounted, ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useRoutesList } from '@/stores/routesList';
import { useThemeConfig } from '@/stores/themeConfig';
import { useTagsViewRoutes } from '@/stores/tagsViewRoutes';

const Logo = defineAsyncComponent(() => import('../logo/index.vue'));
const Vertical = defineAsyncComponent(() => import('../navMenu/vertical.vue'));

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

// mittBus event handling
const mittBusOn = (name: string, callback: (data: unknown) => void) => {
  const handler = (e: CustomEvent) => callback(e.detail);
  window.addEventListener(name, handler as EventListener);
  return () => window.removeEventListener(name, handler as EventListener);
};

const mittBusEmit = (name: string, data?: unknown) => {
  window.dispatchEvent(new CustomEvent(name, { detail: data }));
};

let cleanupSendColumns: (() => void) | undefined;
let cleanupRestore: (() => void) | undefined;

onMounted(() => {
  // Listen for columns children data
  cleanupSendColumns = mittBusOn('setSendColumnsChildren', (data: any) => {
    if (data?.children) {
      columnsChildren.value = data.children;
    }
  });

  // Listen for restore default
  cleanupRestore = mittBusOn('restoreDefault', () => {
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
.layout-aside {
  background: var(--mango-bg-menu-bar);
  box-shadow: 2px 0 6px rgb(0 21 41 / 8%);
  height: inherit;
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  overflow-x: hidden !important;
  transition: width var(--mango-transition-duration);
  width: var(--mango-aside-width) !important;
  min-width: var(--mango-aside-width) !important;
  max-width: var(--mango-aside-width) !important;
  flex-shrink: 0;

  &.aside-collapse {
    width: 64px !important;
    min-width: 64px !important;
    max-width: 64px !important;
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
