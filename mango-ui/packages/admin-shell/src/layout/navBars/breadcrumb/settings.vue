<template>
  <div
    class="layout-breadcrumb-settings"
    @click="openDrawer"
  >
    <el-icon :size="20">
      <Setting />
    </el-icon>
  </div>

  <el-drawer
    v-model="preferencesStore.isDrawer"
    title="布局配置"
    direction="rtl"
    destroy-on-close
    size="280px"
  >
    <el-scrollbar class="layout-breadcrumb-settings-bar">
      <!-- 布局切换 -->
      <el-divider content-position="left">
        布局切换
      </el-divider>
      <div class="layout-drawer-content-flex">
        <div
          class="layout-drawer-content-item"
          @click="onSetLayout('defaults')"
        >
          <section
            class="el-container el-circular"
            :class="{ 'drawer-layout-active': layoutStore.layout === 'defaults' }"
          >
            <aside
              class="el-aside"
              style="width: 20px"
            />
            <section class="el-container is-vertical">
              <header
                class="el-header"
                style="height: 10px"
              />
              <main class="el-main" />
            </section>
          </section>
          <div
            class="layout-tips-warp"
            :class="{ 'layout-tips-warp-active': layoutStore.layout === 'defaults' }"
          >
            <div class="layout-tips-box">
              <p class="layout-tips-txt">
                默认
              </p>
            </div>
          </div>
        </div>

        <div
          class="layout-drawer-content-item"
          @click="onSetLayout('classic')"
        >
          <section
            class="el-container is-vertical el-circular"
            :class="{ 'drawer-layout-active': layoutStore.layout === 'classic' }"
          >
            <header
              class="el-header"
              style="height: 10px"
            />
            <section class="el-container">
              <aside
                class="el-aside"
                style="width: 20px"
              />
              <section class="el-container is-vertical">
                <main class="el-main" />
              </section>
            </section>
          </section>
          <div
            class="layout-tips-warp"
            :class="{ 'layout-tips-warp-active': layoutStore.layout === 'classic' }"
          >
            <div class="layout-tips-box">
              <p class="layout-tips-txt">
                经典
              </p>
            </div>
          </div>
        </div>

        <div
          class="layout-drawer-content-item"
          @click="onSetLayout('transverse')"
        >
          <section
            class="el-container is-vertical el-circular"
            :class="{ 'drawer-layout-active': layoutStore.layout === 'transverse' }"
          >
            <header
              class="el-header"
              style="height: 10px"
            />
            <section class="el-container">
              <section class="el-container is-vertical">
                <main class="el-main" />
              </section>
            </section>
          </section>
          <div
            class="layout-tips-warp"
            :class="{ 'layout-tips-warp-active': layoutStore.layout === 'transverse' }"
          >
            <div class="layout-tips-box">
              <p class="layout-tips-txt">
                横向
              </p>
            </div>
          </div>
        </div>

        <div
          class="layout-drawer-content-item"
          @click="onSetLayout('columns')"
        >
          <section
            class="el-container el-circular"
            :class="{ 'drawer-layout-active': layoutStore.layout === 'columns' }"
          >
            <aside
              class="el-aside-dark"
              style="width: 10px"
            />
            <aside
              class="el-aside"
              style="width: 20px"
            />
            <section class="el-container is-vertical">
              <header
                class="el-header"
                style="height: 10px"
              />
              <main class="el-main" />
            </section>
          </section>
          <div
            class="layout-tips-warp"
            :class="{ 'layout-tips-warp-active': layoutStore.layout === 'columns' }"
          >
            <div class="layout-tips-box">
              <p class="layout-tips-txt">
                分栏
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- 全局主题 -->
      <el-divider content-position="left">
        全局主题
      </el-divider>
      <div class="layout-settings-item">
        <span class="layout-settings-label">primary 主题色</span>
        <div class="layout-settings-value">
          <el-color-picker
            v-model="themeStore.primary"
            :disabled="themeStore.isDark"
            @change="onColorPickerChange"
          />
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">深色模式</span>
        <div class="layout-settings-value">
          <el-switch
            v-model="themeStore.isDark"
            size="small"
            @change="onAddDarkChange"
          />
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">组件大小</span>
        <div class="layout-settings-value">
          <el-select
            v-model="preferencesStore.globalComponentSize"
            style="width: 90px"
            @change="onComponentSizeChange"
          >
            <el-option
              label="大型"
              value="large"
            />
            <el-option
              label="默认"
              value="default"
            />
            <el-option
              label="小型"
              value="small"
            />
          </el-select>
        </div>
      </div>

      <!-- 顶栏设置 -->
      <el-divider content-position="left">
        顶栏设置
      </el-divider>
      <div class="layout-settings-item">
        <span class="layout-settings-label">背景颜色</span>
        <div class="layout-settings-value">
          <el-color-picker
            v-model="themeStore.topBar"
            :disabled="themeStore.isDark"
            @change="onBgColorPickerChange('topBar')"
          />
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">文字颜色</span>
        <div class="layout-settings-value">
          <el-color-picker
            v-model="themeStore.topBarColor"
            :disabled="themeStore.isDark"
            @change="onBgColorPickerChange('topBarColor')"
          />
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">背景渐变</span>
        <div class="layout-settings-value">
          <el-switch
            v-model="themeStore.isTopBarColorGradual"
            size="small"
            @change="onTopBarGradualChange"
          />
        </div>
      </div>

      <!-- 菜单设置 -->
      <el-divider content-position="left">
        菜单设置
      </el-divider>
      <div class="layout-settings-item">
        <span class="layout-settings-label">背景颜色</span>
        <div class="layout-settings-value">
          <el-color-picker
            v-model="themeStore.menuBar"
            :disabled="themeStore.isDark"
            @change="onBgColorPickerChange('menuBar')"
          />
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">文字颜色</span>
        <div class="layout-settings-value">
          <el-color-picker
            v-model="themeStore.menuBarColor"
            :disabled="themeStore.isDark"
            @change="onBgColorPickerChange('menuBarColor')"
          />
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">高亮背景</span>
        <div class="layout-settings-value">
          <el-color-picker
            v-model="themeStore.menuBarActiveColor"
            show-alpha
            :disabled="themeStore.isDark"
            @change="onBgColorPickerChange('menuBarActiveColor')"
          />
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">背景渐变</span>
        <div class="layout-settings-value">
          <el-switch
            v-model="themeStore.isMenuBarColorGradual"
            size="small"
            @change="onMenuBarGradualChange"
          />
        </div>
      </div>

      <!-- 分栏设置 -->
      <el-divider
        content-position="left"
        :style="{ opacity: layoutStore.layout !== 'columns' ? 0.5 : 1 }"
      >
        分栏设置
      </el-divider>
      <div
        class="layout-settings-item"
        :style="{ opacity: layoutStore.layout !== 'columns' ? 0.5 : 1 }"
      >
        <span class="layout-settings-label">分栏背景</span>
        <div class="layout-settings-value">
          <el-color-picker
            v-model="themeStore.columnsMenuBar"
            :disabled="themeStore.isDark || layoutStore.layout !== 'columns'"
            @change="onBgColorPickerChange('columnsMenuBar')"
          />
        </div>
      </div>
      <div
        class="layout-settings-item mt15"
        :style="{ opacity: layoutStore.layout !== 'columns' ? 0.5 : 1 }"
      >
        <span class="layout-settings-label">分栏文字</span>
        <div class="layout-settings-value">
          <el-color-picker
            v-model="themeStore.columnsMenuBarColor"
            :disabled="themeStore.isDark || layoutStore.layout !== 'columns'"
            @change="onBgColorPickerChange('columnsMenuBarColor')"
          />
        </div>
      </div>
      <div
        class="layout-settings-item mt15"
        :style="{ opacity: layoutStore.layout !== 'columns' ? 0.5 : 1 }"
      >
        <span class="layout-settings-label">分栏渐变</span>
        <div class="layout-settings-value">
          <el-switch
            v-model="themeStore.isColumnsMenuBarColorGradual"
            size="small"
            :disabled="themeStore.isDark || layoutStore.layout !== 'columns'"
            @change="onColumnsMenuBarGradualChange"
          />
        </div>
      </div>

      <!-- 界面设置 -->
      <el-divider content-position="left">
        界面设置
      </el-divider>
      <div
        class="layout-settings-item"
        :style="{ opacity: layoutStore.layout === 'transverse' ? 0.5 : 1 }"
      >
        <span class="layout-settings-label">菜单折叠</span>
        <div class="layout-settings-value">
          <el-switch
            v-model="layoutStore.isCollapse"
            :disabled="layoutStore.layout === 'transverse'"
            size="small"
            @change="onThemeConfigChange"
          />
        </div>
      </div>
      <div
        class="layout-settings-item mt15"
        :style="{ opacity: layoutStore.layout === 'transverse' ? 0.5 : 1 }"
      >
        <span class="layout-settings-label">手风琴模式</span>
        <div class="layout-settings-value">
          <el-switch
            v-model="layoutStore.isUniqueOpened"
            :disabled="layoutStore.layout === 'transverse'"
            size="small"
            @change="setLocalThemeConfig"
          />
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">固定 Header</span>
        <div class="layout-settings-value">
          <el-switch
            v-model="layoutStore.isFixedHeader"
            size="small"
            @change="onIsFixedHeaderChange"
          />
        </div>
      </div>
      <div
        class="layout-settings-item mt15"
        :style="{ opacity: layoutStore.layout !== 'classic' ? 0.5 : 1 }"
      >
        <span class="layout-settings-label">经典分割菜单</span>
        <div class="layout-settings-value">
          <el-switch
            v-model="layoutStore.isClassicSplitMenu"
            :disabled="layoutStore.layout !== 'classic'"
            size="small"
            @change="setLocalThemeConfig"
          />
        </div>
      </div>

      <!-- 界面显示 -->
      <el-divider content-position="left">
        界面显示
      </el-divider>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">显示 Logo</span>
        <div class="layout-settings-value">
          <el-switch
            v-model="layoutStore.isShowLogo"
            size="small"
            @change="onIsShowLogoChange"
          />
        </div>
      </div>
      <div
        class="layout-settings-item mt15"
        :style="{ opacity: layoutStore.layout === 'classic' || layoutStore.layout === 'transverse' ? 0.5 : 1 }"
      >
        <span class="layout-settings-label">显示面包屑</span>
        <div class="layout-settings-value">
          <el-switch
            v-model="layoutStore.isBreadcrumb"
            :disabled="layoutStore.layout === 'classic' || layoutStore.layout === 'transverse'"
            size="small"
            @change="setLocalThemeConfig"
          />
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">显示 Tagsview</span>
        <div class="layout-settings-value">
          <el-switch
            v-model="layoutStore.isTagsview"
            size="small"
            @change="setLocalThemeConfig"
          />
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">Tagsview 图标</span>
        <div class="layout-settings-value">
          <el-switch
            v-model="layoutStore.isTagsviewIcon"
            size="small"
            @change="setLocalThemeConfig"
          />
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">缓存 Tagsview</span>
        <div class="layout-settings-value">
          <el-switch
            v-model="layoutStore.isCacheTagsView"
            size="small"
            @change="setLocalThemeConfig"
          />
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">显示 Footer</span>
        <div class="layout-settings-value">
          <el-switch
            v-model="layoutStore.isFooter"
            size="small"
            @change="setLocalThemeConfig"
          />
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">灰色模式</span>
        <div class="layout-settings-value">
          <el-switch
            v-model="preferencesStore.isGrayscale"
            size="small"
            @change="onAddFilterChange('grayscale')"
          />
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">色弱模式</span>
        <div class="layout-settings-value">
          <el-switch
            v-model="preferencesStore.isInvert"
            size="small"
            @change="onAddFilterChange('invert')"
          />
        </div>
      </div>
      <!--
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">水印</span>
        <div class="layout-settings-value">
          <el-switch
            v-model="preferencesStore.isWartermark"
            size="small"
            @change="onWartermarkChange"
          />
        </div>
      </div>
      -->

      <!-- 其它设置 -->
      <el-divider content-position="left">
        其它设置
      </el-divider>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">页面动画</span>
        <div class="layout-settings-value">
          <el-select
            v-model="preferencesStore.animation"
            style="width: 90px"
            @change="setLocalThemeConfig"
          >
            <el-option
              label="右侧滑入"
              value="slide-right"
            />
            <el-option
              label="左侧滑入"
              value="slide-left"
            />
            <el-option
              label="渐变"
              value="opacitys"
            />
          </el-select>
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">Tagsview 样式</span>
        <div class="layout-settings-value">
          <el-select
            v-model="preferencesStore.tagsStyle"
            style="width: 90px"
            @change="setLocalThemeConfig"
          >
            <el-option
              label="胶囊"
              value="tags-style-capsule"
            />
            <el-option
              label="卡片"
              value="tags-style-card"
            />
            <el-option
              label="经典"
              value="tags-style-classic"
            />
          </el-select>
        </div>
      </div>

      <!-- 恢复默认 -->
      <div class="copy-config">
        <el-alert
          title="点击"
          type="warning"
          :closable="false"
          show-icon
        >
          重置所有配置到默认状态
        </el-alert>
        <el-button
          type="primary"
          class="copy-config-btn"
          @click="onResetConfigClick"
        >
          <el-icon class="mr5">
            <RefreshRight />
          </el-icon>
          恢复默认
        </el-button>
      </div>
    </el-scrollbar>
  </el-drawer>
</template>

<script setup lang="ts" name="layoutBreadcrumbSettings">
import { nextTick, onMounted, onUnmounted, reactive } from 'vue';
import { storeToRefs } from 'pinia';
import { useThemeStore } from '../../../stores/theme';
import { useLayoutStore } from '../../../stores/layout';
import { normalizeTagsStyle, usePreferencesStore } from '../../../stores/preferences';
import { mittBus } from '@mango/common/utils/mitt';
import { useChangeColor } from '@mango/common/utils/theme';
import { Setting, RefreshRight } from '@element-plus/icons-vue';

const themeStore = useThemeStore();
const layoutStore = useLayoutStore();
const preferencesStore = usePreferencesStore();
const { getLightColor, getDarkColor } = useChangeColor();

const state = reactive({
});

// 验证颜色值是否为有效的 hex 格式，防止 CSS 注入
const isValidColor = (color: string): boolean => {
  return /^#[0-9A-Fa-f]{6}$/.test(color);
};

// 颜色选择器变化 - 全局主题
const onColorPickerChange = () => {
  if (!themeStore.primary) return;
  // 颜色变化现在由 layout/index.vue 的 watcher 处理
  // 这里只更新本地存储
  setLocalThemeConfig();
};

// 背景颜色选择器变化
type BgColorProperty = 'topBar' | 'topBarColor' | 'menuBar' | 'menuBarColor' | 'menuBarActiveColor' | 'columnsMenuBar' | 'columnsMenuBarColor';

const onBgColorPickerChange = (bg: BgColorProperty) => {
  const colorValue = themeStore[bg as keyof typeof themeStore];
  if (!isValidColor(colorValue)) return;

  // 颜色变化现在由 layout/index.vue 的 watcher 处理
  // 只触发相关的 gradient 函数
  if (bg === 'topBar' || bg === 'topBarColor') {
    onTopBarGradualChange();
  } else if (bg === 'menuBar' || bg === 'menuBarColor' || bg === 'menuBarActiveColor') {
    onMenuBarGradualChange();
  } else if (bg === 'columnsMenuBar' || bg === 'columnsMenuBarColor') {
    onColumnsMenuBarGradualChange();
  }
  setLocalThemeConfig();
};

// 顶栏背景渐变
const onTopBarGradualChange = () => {
  setGraduaFun('.layout-navbars-container', themeStore.isTopBarColorGradual, themeStore.topBar);
};

// 菜单背景渐变
const onMenuBarGradualChange = () => {
  setGraduaFun('.layout-aside', themeStore.isMenuBarColorGradual, themeStore.menuBar);
};

// 分栏菜单背景渐变
const onColumnsMenuBarGradualChange = () => {
  setGraduaFun('.layout-columns-aside', themeStore.isColumnsMenuBarColorGradual, themeStore.columnsMenuBar);
};

// 渐变函数
const setGraduaFun = (el: string, bool: boolean, color: string) => {
  // 验证颜色格式防止 CSS 注入
  if (!isValidColor(color)) return;
  setTimeout(() => {
    const els = document.querySelector(el);
    if (!els) return false;
    if (bool) {
      els.setAttribute('style', `background: linear-gradient(to bottom left, ${color}, ${getLightColor(color, 0.6)});`);
    } else {
      els.setAttribute('style', '');
    }
    setLocalThemeConfig();
  }, 200);
};

// 界面设置变化
const onThemeConfigChange = () => {
  setLocalThemeConfig();
};

// 固定 Header 变化
const onIsFixedHeaderChange = () => {
  layoutStore.isFixedHeaderChange = layoutStore.isFixedHeader ? false : true;
  setLocalThemeConfig();
};

// 显示 Logo 变化
const onIsShowLogoChange = () => {
  layoutStore.isShowLogoChange = layoutStore.isShowLogo ? false : true;
  setLocalThemeConfig();
};

// 灰色/色弱模式
const onAddFilterChange = (attr: string) => {
  if (attr === 'grayscale') {
    if (preferencesStore.isGrayscale) preferencesStore.isInvert = false;
  } else {
    if (preferencesStore.isInvert) preferencesStore.isGrayscale = false;
  }
  const cssAttr = attr === 'grayscale' ? `grayscale(${preferencesStore.isGrayscale ? 1 : 0})` : `invert(${preferencesStore.isInvert ? '80%' : '0%'})`;
  document.body.setAttribute('style', `filter: ${cssAttr}`);
  setLocalThemeConfig();
};

// 深色模式
const onAddDarkChange = () => {
  if (themeStore.isDark) {
    document.documentElement.setAttribute('data-theme', 'dark');
    // Clear inline color styles so dark CSS variables take precedence
    document.documentElement.style.removeProperty('--mango-color-primary');
    document.documentElement.style.removeProperty('--mango-bg-top-bar');
    document.documentElement.style.removeProperty('--mango-bg-menu-bar');
    document.documentElement.style.removeProperty('--mango-bg-columns-menu-bar');
  } else {
    document.documentElement.setAttribute('data-theme', 'light');
    // Re-apply light mode colors as inline styles
    document.documentElement.style.setProperty('--mango-color-primary', themeStore.primary);
    document.documentElement.style.setProperty('--mango-bg-top-bar', themeStore.topBar);
    document.documentElement.style.setProperty('--mango-bg-menu-bar', themeStore.menuBar);
    document.documentElement.style.setProperty('--mango-bg-columns-menu-bar', themeStore.columnsMenuBar);
  }
  setLocalThemeConfig();
};

// 水印功能暂不开放，保留配置项逻辑，后续实现水印渲染后再恢复入口。
// const onWartermarkChange = () => {
//   setLocalThemeConfig();
// };

// 布局切换
const onSetLayout = (layout: 'defaults' | 'classic' | 'transverse' | 'columns') => {
  if (layoutStore.layout === layout) return false;
  if (layout === 'transverse') layoutStore.isCollapse = false;
  layoutStore.setLayout(layout);
  preferencesStore.isDrawer = false;
  initLayoutChangeFun();
};

// 初始化布局变化
const initLayoutChangeFun = () => {
  onBgColorPickerChange('menuBar');
  onBgColorPickerChange('menuBarColor');
  onBgColorPickerChange('menuBarActiveColor');
  onBgColorPickerChange('topBar');
  onBgColorPickerChange('topBarColor');
  onBgColorPickerChange('columnsMenuBar');
  onBgColorPickerChange('columnsMenuBarColor');
};

// 组件大小变化
const onComponentSizeChange = () => {
  setLocalThemeConfig();
  // 需要刷新页面应用组件大小
  // window.location.reload();
};

// 打开抽屉
const openDrawer = () => {
  preferencesStore.isDrawer = true;
};

// 存储配置到本地
const setLocalThemeConfig = () => {
  preferencesStore.tagsStyle = normalizeTagsStyle(preferencesStore.tagsStyle);
  // 合并三个 store 的状态保存到 localStorage
  const combined = {
    ...themeStore.$state,
    ...layoutStore.$state,
    ...preferencesStore.$state,
    isDrawer: false,
  };
  localStorage.setItem('themeConfig', JSON.stringify(combined));
};

// 恢复默认配置
const onResetConfigClick = () => {
  localStorage.removeItem('themeConfig');
  window.location.reload();
};

// 初始化样式
const initSetStyle = () => {
  onTopBarGradualChange();
  onMenuBarGradualChange();
  onColumnsMenuBarGradualChange();
};

onMounted(() => {
  nextTick(() => {
    setTimeout(() => {
      // 默认样式
      onColorPickerChange();
      // 灰色模式
      if (preferencesStore.isGrayscale) onAddFilterChange('grayscale');
      // 色弱模式
      if (preferencesStore.isInvert) onAddFilterChange('invert');
      // 深色模式
      if (themeStore.isDark) onAddDarkChange();
      // 初始化菜单样式
      initSetStyle();
    }, 100);
  });

  // 监听窗口大小改变，非默认布局，设置成默认布局（适配移动端）
  mittBus.on('layoutMobileResize', handleMobileResize);
});

onUnmounted(() => {
  mittBus.off('layoutMobileResize', handleMobileResize);
});

// 监听窗口大小改变，非默认布局，设置成默认布局（适配移动端）
const handleMobileResize = (res: { isMobile: boolean; windowWidth: number; layout?: string }) => {
  if (res.layout && res.layout !== layoutStore.layout) {
    layoutStore.setLayout(res.layout as 'defaults' | 'classic' | 'transverse' | 'columns');
    preferencesStore.isDrawer = false;
    initLayoutChangeFun();
  }
};
</script>

<style scoped lang="scss">
.layout-breadcrumb-settings {
  display: flex;
  align-items: center;
  padding: 0 12px;
  height: 40px;
  cursor: pointer;
  color: var(--mango-color-top-bar);
  transition: opacity 0.2s;

  &:hover {
    opacity: 0.8;
  }
}

.layout-breadcrumb-settings-bar {
  height: calc(100vh - 50px);
  padding: 0 15px;

  :deep(.el-scrollbar__view) {
    overflow-x: hidden !important;
  }
}

.layout-settings-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 5px;

  .layout-settings-label {
    font-size: 13px;
    color: var(--el-text-color-primary);
  }

  .layout-settings-value {
    display: flex;
    align-items: center;
  }
}

.mt15 {
  margin-top: 15px;
}

.layout-drawer-content-flex {
  overflow: hidden;
  display: flex;
  flex-wrap: wrap;
  align-content: flex-start;
  margin: 0 -5px;

  .layout-drawer-content-item {
    width: 50%;
    height: 70px;
    cursor: pointer;
    border: 1px solid transparent;
    position: relative;
    padding: 5px;

    .el-container {
      height: 100%;

      .el-aside-dark {
        background-color: #545c64;
      }

      .el-aside {
        background-color: #ffffff;
      }

      .el-header {
        background-color: #2e5cf6;
      }

      .el-main {
        background-color: #f5f5f5;
      }
    }

    .el-circular {
      border-radius: 3px;
      overflow: hidden;
      border: 1px solid transparent;
      transition: all 0.3s ease-in-out;
    }

    .drawer-layout-active {
      border: 1px solid var(--mango-color-primary);
    }

    .layout-tips-warp {
      transition: all 0.3s ease-in-out;
      position: absolute;
      left: 50%;
      top: 50%;
      transform: translate(-50%, -50%);
      border: 1px solid var(--mango-color-primary);
      border-radius: 100%;
      padding: 4px;

      .layout-tips-box {
        transition: inherit;
        width: 30px;
        height: 30px;
        z-index: 9;
        border: 1px solid var(--mango-color-primary);
        border-radius: 100%;
        display: flex;
        align-items: center;
        justify-content: center;

        .layout-tips-txt {
          transition: inherit;
          font-size: 8px;
          line-height: 1;
          white-space: nowrap;
          color: var(--mango-color-primary);
          transform: rotate(30deg);
        }
      }
    }

    .layout-tips-warp-active {
      border-color: var(--mango-color-primary);

      .layout-tips-box {
        border-color: var(--mango-color-primary);

        .layout-tips-txt {
          color: var(--mango-color-primary) !important;
        }
      }
    }

    &:hover {
      .el-circular {
        border: 1px solid var(--mango-color-primary);
      }
    }
  }
}

.copy-config {
  margin: 20px 0 30px;

  .copy-config-btn {
    width: 100%;
    margin-top: 15px;
  }
}

:deep(.el-divider--horizontal) {
  margin: 16px 0;
}
</style>
