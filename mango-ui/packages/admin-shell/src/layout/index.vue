<template>
  <div class="mango-layout-root" :class="`mango-layout-root--${layoutStore.layout}`">
    <LayoutHeader v-if="showTopHeader" key="top-header" />
    <div v-if="isTransverse" key="transverse-menu" class="layout-transverse-menu">
      <NavMenuHorizontal :menu-list="menuList" />
    </div>

    <div class="mango-layout-body">
      <ColumnsAside v-if="isColumns" key="columns-aside" />
      <LayoutAside v-if="showAside" key="layout-aside" />
      <div class="mango-layout-content">
        <LayoutHeader v-if="showContentHeader" key="content-header" />
        <LayoutWorkspaceNav key="workspace-nav" :tags-view="isTagsview" />
        <!-- Keep this component at one stable vnode location across layout changes. -->
        <LayoutMain ref="layoutMainRef" key="layout-main" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts" name="layout">
import { computed, defineAsyncComponent, onBeforeMount, onUnmounted, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useThemeStore } from '../stores/theme';
import { useLayoutStore } from '../stores/layout';
import { useRoutesList } from '../stores/routesList';
import { useScrollbar } from '../composables/useScrollbar';
import { Local } from '@mango/common/utils/storage';
import { mittBus } from '@mango/common/utils/mitt';
import { containsMenuPath } from '@mango/common/utils/menuTree';
import { useChangeColor } from '@mango/common/utils/theme';

const LayoutAside = defineAsyncComponent(() => import('./component/aside.vue'));
const LayoutHeader = defineAsyncComponent(() => import('./component/header.vue'));
const LayoutMain = defineAsyncComponent(() => import('./component/main.vue'));
const LayoutWorkspaceNav = defineAsyncComponent(() => import('./navBars/workspaceNav/index.vue'));
const ColumnsAside = defineAsyncComponent(() => import('./component/columnsAside.vue'));
const NavMenuHorizontal = defineAsyncComponent(() => import('./navMenu/horizontal.vue'));

const themeStore = useThemeStore();
const layoutStore = useLayoutStore();
const storesRoutesList = useRoutesList();
const route = useRoute();
const { routesList, activeTopRoutePath } = storeToRefs(storesRoutesList);
const { isTagsview } = storeToRefs(layoutStore);
const layoutMainRef = ref();
const { updateScrollbar, initScrollHeight } = useScrollbar(layoutMainRef);
const { getDarkColor, getLightColor } = useChangeColor();

const isTransverse = computed(() => layoutStore.layout === 'transverse');
const isColumns = computed(() => layoutStore.layout === 'columns');
const showTopHeader = computed(() => layoutStore.layout === 'classic' || isTransverse.value);
const showContentHeader = computed(() => layoutStore.layout === 'defaults' || isColumns.value);
const showAside = computed(() => !isTransverse.value);
const menuList = ref<any[]>([]);

watch(
  () => [routesList.value, activeTopRoutePath.value, route.path],
  () => {
    const activeTop = activeTopRoutePath.value
      ? routesList.value.find((item) => item.path === activeTopRoutePath.value)
      : routesList.value.find((item) => containsMenuPath(item as never, route.path));
    menuList.value = activeTop?.children?.length ? activeTop.children : [];
  },
  { immediate: true },
);

watch(
  () => route.path,
  () => initScrollHeight(),
  { immediate: true },
);

watch(
  () => [layoutStore.isCollapse, layoutStore.isFixedHeader, layoutStore.isTagsview, layoutStore.layout],
  () => updateScrollbar(),
);

// 验证颜色值是否为有效的 hex 格式
const isValidColor = (color: string): boolean => {
  return /^#[0-9A-Fa-f]{6}$/.test(color);
};

const isValidCssColor = (color?: string): color is string => {
  if (!color) return false;
  return (
    /^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$/.test(color) || /^rgba?\(/.test(color) || /^hsla?\(/.test(color)
  );
};

// 应用 primary 主题色
const applyPrimaryColor = (primary: string) => {
  if (!isValidColor(primary)) return;
  document.documentElement.style.setProperty('--el-color-primary', primary);
  document.documentElement.style.setProperty('--el-color-primary-dark-2', getDarkColor(primary, 0.1));
  for (let i = 1; i <= 9; i++) {
    document.documentElement.style.setProperty(`--el-color-primary-light-${i}`, getLightColor(primary, i / 10));
  }
  document.documentElement.style.setProperty('--mango-color-primary', primary);
};

// 应用背景颜色
const applyBgColor = (bg: string, variable: string) => {
  if (!isValidColor(bg)) return;
  document.documentElement.style.setProperty(variable, bg);
};

const applyCssColor = (color: string, variable: string) => {
  if (!isValidCssColor(color)) return;
  document.documentElement.style.setProperty(variable, color);
};

// 初始化主题颜色
const initTheme = () => {
  applyPrimaryColor(themeStore.primary);
  applyBgColor(themeStore.topBar, '--mango-bg-top-bar');
  applyCssColor(themeStore.topBarColor, '--mango-color-top-bar');
  applyBgColor(themeStore.menuBar, '--mango-bg-menu-bar');
  applyCssColor(themeStore.menuBarColor, '--mango-color-menu-bar');
  applyCssColor(themeStore.menuBarActiveColor, '--mango-color-menu-active-bg');
  applyBgColor(themeStore.columnsMenuBar, '--mango-bg-columns-menu-bar');
  applyCssColor(themeStore.columnsMenuBarColor, '--mango-color-columns-menu-bar');
};

// 监听主题颜色变化
watch(
  () => themeStore.primary,
  (newPrimary) => {
    applyPrimaryColor(newPrimary);
  },
);

watch(
  () => themeStore.topBar,
  (newTopBar) => {
    applyBgColor(newTopBar, '--mango-bg-top-bar');
  },
);

watch(
  () => themeStore.topBarColor,
  (newTopBarColor) => {
    applyCssColor(newTopBarColor, '--mango-color-top-bar');
  },
);

watch(
  () => themeStore.menuBar,
  (newMenuBar) => {
    applyBgColor(newMenuBar, '--mango-bg-menu-bar');
    document.documentElement.style.setProperty('--mango-bg-menuBar-light-1', getLightColor(newMenuBar, 0.05));
  },
);

watch(
  () => themeStore.menuBarColor,
  (newMenuBarColor) => {
    applyCssColor(newMenuBarColor, '--mango-color-menu-bar');
  },
);

watch(
  () => themeStore.menuBarActiveColor,
  (newMenuBarActiveColor) => {
    applyCssColor(newMenuBarActiveColor, '--mango-color-menu-active-bg');
  },
);

watch(
  () => themeStore.columnsMenuBar,
  (newColumnsMenuBar) => {
    applyBgColor(newColumnsMenuBar, '--mango-bg-columns-menu-bar');
  },
);

watch(
  () => themeStore.columnsMenuBarColor,
  (newColumnsMenuBarColor) => {
    applyCssColor(newColumnsMenuBarColor, '--mango-color-columns-menu-bar');
  },
);

// 窗口大小改变时(适配移动端) - 硬断点 1000px 保留
const onLayoutResize = () => {
  if (!Local.get('oldLayout')) Local.set('oldLayout', layoutStore.layout);
  const clientWidth = document.body.clientWidth;
  if (clientWidth < 1000) {
    layoutStore.isCollapse = false;
    layoutStore.isMobileMenuOpen = false;
    const currentLayout = layoutStore.layout;
    if (currentLayout !== 'defaults') {
      Local.set('oldLayout', currentLayout);
      layoutStore.layout = 'defaults';
    }
    mittBus.emit('layoutMobileResize', {
      isMobile: true,
      windowWidth: clientWidth,
      layout: 'defaults',
    });
  } else {
    const oldLayout = Local.get('oldLayout');
    if (oldLayout && oldLayout !== 'defaults' && layoutStore.layout === 'defaults') {
      layoutStore.layout = oldLayout as 'defaults' | 'classic' | 'transverse' | 'columns';
    }
    mittBus.emit('layoutMobileResize', {
      isMobile: false,
      windowWidth: clientWidth,
      layout: layoutStore.layout,
    });
  }
};

onBeforeMount(() => {
  // 如果 initThemeBeforeRender 已恢复过布局，跳过 onLayoutResize
  // 防止首次加载时 onLayoutResize 在 store 恢复后执行导致布局被覆盖
  if (Local.get('layoutRestored')) {
    Local.remove('layoutRestored');
  } else {
    onLayoutResize();
  }
  window.addEventListener('resize', onLayoutResize);
});

onMounted(() => {
  initTheme();
});

onUnmounted(() => {
  window.removeEventListener('resize', onLayoutResize);
});
</script>

<style scoped lang="scss">
.mango-layout-root {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.mango-layout-body {
  display: flex;
  flex: 1;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
}

.mango-layout-content {
  display: flex;
  flex: 1;
  min-height: 0;
  min-width: 0;
  flex-direction: column;
  overflow: hidden;
}

.mango-layout-root--classic .mango-layout-body,
.mango-layout-root--transverse .mango-layout-body {
  height: calc(100vh - var(--mango-header-height));
}

.mango-layout-root--transverse .layout-transverse-menu {
  flex-shrink: 0;
  background: var(--mango-bg-menu-bar);
  border-bottom: 1px solid var(--mango-border-color);

  :deep(.nav-menu-horizontal) {
    border-bottom: none;
  }
}

/* Keep the router outlet's parent stable while only the shell chrome changes. */
.mango-layout-content :deep(.layout-main) {
  min-width: 0;
}
</style>
