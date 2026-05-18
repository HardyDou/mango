<template>
  <div class="layout-navbars-container">
    <div class="layout-navbars-container-left">
      <!-- 移动端汉堡菜单按钮 - 所有布局都显示 -->
      <div
        class="hamburger hamburger-mobile"
        @click="onToggleMobileMenu"
      >
        <el-icon :size="20">
          <Fold v-if="!layoutStore.isMobileMenuOpen" />
          <Close v-else />
        </el-icon>
      </div>
      <!-- 经典布局：显示 Logo + 折叠按钮 -->
      <template v-if="layoutStore.layout === 'classic' || layoutStore.layout === 'transverse'">
        <Logo class="layout-logo-link" />
        <div
          v-if="layoutStore.layout === 'classic'"
          class="hamburger"
          @click="toggleCollapse"
        >
          <el-icon :size="20">
            <Fold v-if="!layoutStore.isCollapse" />
            <Expand v-else />
          </el-icon>
        </div>
      </template>
      <template v-else-if="layoutStore.layout === 'defaults' || layoutStore.layout === 'columns'">
        <div
          class="hamburger"
          @click="toggleCollapse"
        >
          <el-icon :size="20">
            <Fold v-if="!headerAsideExpanded" />
            <Expand v-else />
          </el-icon>
        </div>
      </template>
    </div>
    <div
      v-if="showTopSystems"
      class="layout-top-systems"
    >
      <button
        v-for="item in topMenus"
        :key="item.path"
        type="button"
        class="layout-top-system-item"
        :class="{ active: item.path === activeTopRoutePath }"
        @click="onTopMenuClick(item)"
      >
        <el-icon
          v-if="item.meta?.icon && iconMap[item.meta.icon]"
          :size="16"
        >
          <component :is="iconMap[item.meta.icon]" />
        </el-icon>
        <span>{{ item.meta?.title || item.name }}</span>
      </button>
    </div>
    <div
      v-else
      class="layout-top-nav"
    >
      <BreadcrumbIndex />
    </div>
    <div class="layout-navbars-container-right">
      <el-icon :size="20">
        <Search />
      </el-icon>
      <el-icon :size="20">
        <FullScreen />
      </el-icon>
      <Settings />
      <User />
    </div>
  </div>
</template>

<script setup lang="ts" name="layoutNavBars">
import { computed, defineAsyncComponent, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useLayoutStore } from '@/stores/layout';
import { useRoutesList } from '@/stores/routesList';
import { iconMap } from '@mango/common/utils/iconConfig';
import { containsMenuPath, resolveFirstMenu, type ShellRouteMenu } from '@/runtime/menuHost';
import { Fold, Expand, Search, FullScreen, Close } from '@element-plus/icons-vue';

const Logo = defineAsyncComponent(() => import('../logo/index.vue'));
const BreadcrumbIndex = defineAsyncComponent(() => import('./breadcrumb/breadcrumb.vue'));
const User = defineAsyncComponent(() => import('./breadcrumb/user.vue'));
const Settings = defineAsyncComponent(() => import('./breadcrumb/settings.vue'));

const route = useRoute();
const router = useRouter();
const layoutStore = useLayoutStore();
const storesRoutesList = useRoutesList();
const { routesList, activeTopRoutePath } = storeToRefs(storesRoutesList);

const topMenus = computed(() => routesList.value.filter(item => !item.meta?.isHide));
const showTopSystems = computed(() => layoutStore.layout === 'classic' || layoutStore.layout === 'transverse');
const headerAsideExpanded = computed(() => {
  if (layoutStore.layout === 'columns') {
    return layoutStore.isColumnsAsideOpen;
  }
  return !layoutStore.isCollapse;
});

const findTopByPath = (path: string): ShellRouteMenu | undefined => {
  return topMenus.value.find(item => containsMenuPath(item, path))
    || topMenus.value[0];
};

const resolveFirstRoute = (item: ShellRouteMenu): string => {
  if (item.redirect && typeof item.redirect === 'string') {
    return item.redirect;
  }
  return resolveFirstMenu(item)?.path || item.path;
};

const toggleCollapse = () => {
  if (layoutStore.layout === 'columns') {
    layoutStore.toggleColumnsAside();
    return;
  }
  layoutStore.toggleCollapse();
};

const onToggleMobileMenu = () => {
  layoutStore.toggleMobileMenu();
};

const onTopMenuClick = (item: ShellRouteMenu) => {
  storesRoutesList.setActiveTopRoutePath(item.path);
  const targetPath = resolveFirstRoute(item);
  if (targetPath && targetPath !== route.path) {
    router.push(targetPath);
  }
};

watch(
  () => [route.path, topMenus.value],
  () => {
    const matchedTop = findTopByPath(route.path);
    if (matchedTop && matchedTop.path !== activeTopRoutePath.value) {
      storesRoutesList.setActiveTopRoutePath(matchedTop.path);
    }
  },
  { immediate: true }
);
</script>

<style scoped lang="scss">
.layout-navbars-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 100%;
  padding: 0px;
  background: var(--mango-bg-top-bar);
  color: var(--mango-color-top-bar);

  .layout-navbars-container-left {
    display: flex;
    align-items: center;
    height: 40px;
    gap: 12px;
    min-width: 0;
    flex-shrink: 0;
  }

  .layout-top-systems {
    display: flex;
    align-items: center;
    height: 100%;
    gap: 2px;
    margin-left: 12px;
    flex: 1;
    min-width: 0;
    overflow-x: auto;
    overflow-y: hidden;
    scrollbar-width: none;

    &::-webkit-scrollbar {
      display: none;
    }
  }

  .layout-top-nav {
    display: flex;
    align-items: center;
    height: 100%;
    margin-left: 8px;
    flex: 1;
    min-width: 0;
    overflow: hidden;

    :deep(.layout-breadcrumb) {
      min-height: auto;
      color: var(--mango-color-top-bar);

      .el-breadcrumb__inner,
      .el-breadcrumb__separator {
        color: rgba(255, 255, 255, 0.92);
        font-size: 15px;
        font-weight: 500;
      }

      .el-breadcrumb__inner.is-link:hover {
        color: #ffffff;
      }
    }
  }

  .layout-top-system-item {
    height: 34px;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 0 14px;
    border: 0;
    border-radius: 4px;
    background: transparent;
    color: var(--mango-color-top-bar);
    cursor: pointer;
    white-space: nowrap;
    font-size: 14px;
    line-height: 34px;

    &:hover,
    &.active {
      background: rgba(255, 255, 255, 0.16);
    }
  }

  .layout-navbars-container-right {
    display: flex;
    align-items: center;
    height: 40px;
    gap: 12px;
    padding-right: 8px;
    margin-left: auto;
    flex-shrink: 0;
  }

  .hamburger {
    display: flex;
    align-items: center;
    cursor: pointer;
    color: var(--mango-color-top-bar);
    width: 32px;
    height: 32px;
    justify-content: center;
    &:hover {
      opacity: 0.8;
    }

    // 移动端汉堡按钮的显示/隐藏由下面的 media query 处理
    &.hamburger-mobile {
      // display: none; // 移除这里，让 media query 控制
    }
  }

  .layout-logo-link {
    flex-shrink: 0;
    overflow: hidden;
    :deep(.layout-logo) {
      width: auto;
      height: 40px;
      background: transparent;
      box-shadow: none;
      font-size: 16px;
      padding: 0 8px;
      span {
        color: var(--mango-color-top-bar);
      }
    }
    :deep(.layout-logo-collapsed) {
      width: 40px !important;
      height: 40px;
      background: transparent;
      box-shadow: none;
      flex-shrink: 0;
      padding: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      .logo-icon {
        color: var(--mango-color-top-bar);
        font-size: 20px;
        font-weight: 700;
      }
    }
  }
}

// 桌面端：隐藏移动端汉堡按钮 (1000px breakpoint)
@media screen and (min-width: 1001px) {
  .hamburger.hamburger-mobile {
    display: none !important;
  }
}

// 移动端：显示汉堡按钮 + 隐藏经典布局的折叠按钮
@media screen and (max-width: 1000px) {
  .layout-top-systems {
    max-width: calc(100vw - 210px);
    margin-left: 4px;
  }

  .layout-top-nav {
    margin-left: 4px;
  }

  .layout-top-system-item {
    padding: 0 10px;
  }

  .hamburger:not(.hamburger-mobile) {
    display: none !important;
  }
  .hamburger.hamburger-mobile {
    display: flex !important;
  }
}
</style>
