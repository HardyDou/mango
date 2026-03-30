<template>
  <div class="layout-navbars-container">
    <div class="layout-navbars-container-left">
      <!-- 移动端汉堡菜单按钮 - 所有布局都显示 -->
      <div class="hamburger hamburger-mobile" @click="onToggleMobileMenu">
        <el-icon :size="20">
          <Fold v-if="!themeConfig.isMobileMenuOpen" />
          <Close v-else />
        </el-icon>
      </div>
      <!-- 经典布局：显示 Logo + 折叠按钮 -->
      <template v-if="themeConfig.layout === 'classic'">
        <Logo class="layout-logo-link" />
        <div class="hamburger" @click="toggleCollapse">
          <el-icon :size="20">
            <Fold v-if="!themeConfig.isCollapse" />
            <Expand v-else />
          </el-icon>
        </div>
      </template>
      <!-- 其他布局：显示面包屑 -->
      <BreadcrumbIndex v-else />
    </div>
    <div class="layout-navbars-container-right">
      <el-icon :size="20"><Search /></el-icon>
      <el-icon :size="20"><FullScreen /></el-icon>
      <Settings />
      <User />
    </div>
  </div>
</template>

<script setup lang="ts" name="layoutNavBars">
import { defineAsyncComponent } from 'vue';
import { storeToRefs } from 'pinia';
import { useThemeConfig } from '@/stores/themeConfig';
import { Fold, Expand, Search, FullScreen, Close } from '@element-plus/icons-vue';

const BreadcrumbIndex = defineAsyncComponent(() => import('./breadcrumb/breadcrumb.vue'));
const Logo = defineAsyncComponent(() => import('../logo/index.vue'));
const User = defineAsyncComponent(() => import('./breadcrumb/user.vue'));
const Settings = defineAsyncComponent(() => import('./breadcrumb/settings.vue'));

const storesThemeConfig = useThemeConfig();
const { themeConfig } = storeToRefs(storesThemeConfig);

const toggleCollapse = () => {
  themeConfig.value.isCollapse = !themeConfig.value.isCollapse;
};

const onToggleMobileMenu = () => {
  storesThemeConfig.toggleMobileMenu();
};
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
  }

  .layout-navbars-container-right {
    display: flex;
    align-items: center;
    height: 40px;
    gap: 12px;
    padding-right: 8px;
    margin-left: auto;
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
  .hamburger:not(.hamburger-mobile) {
    display: none !important;
  }
  .hamburger.hamburger-mobile {
    display: flex !important;
  }
}
</style>
