<template>
  <div class="layout-breadcrumb-settings" @click="openDrawer">
    <el-icon :size="20">
      <Setting />
    </el-icon>
  </div>

  <el-drawer
    title="布局配置"
    v-model="themeConfig.isDrawer"
    direction="rtl"
    destroy-on-close
    size="280px"
  >
    <el-scrollbar class="layout-breadcrumb-settings-bar">
      <!-- 布局切换 -->
      <el-divider content-position="left">布局切换</el-divider>
      <div class="layout-drawer-content-flex">
        <div class="layout-drawer-content-item" @click="onSetLayout('defaults')">
          <section class="el-container el-circular" :class="{ 'drawer-layout-active': getThemeConfig.layout === 'defaults' }">
            <aside class="el-aside" style="width: 20px"></aside>
            <section class="el-container is-vertical">
              <header class="el-header" style="height: 10px"></header>
              <main class="el-main"></main>
            </section>
          </section>
          <div class="layout-tips-warp" :class="{ 'layout-tips-warp-active': getThemeConfig.layout === 'defaults' }">
            <div class="layout-tips-box">
              <p class="layout-tips-txt">默认</p>
            </div>
          </div>
        </div>

        <div class="layout-drawer-content-item" @click="onSetLayout('classic')">
          <section class="el-container is-vertical el-circular" :class="{ 'drawer-layout-active': getThemeConfig.layout === 'classic' }">
            <header class="el-header" style="height: 10px"></header>
            <section class="el-container">
              <aside class="el-aside" style="width: 20px"></aside>
              <section class="el-container is-vertical">
                <main class="el-main"></main>
              </section>
            </section>
          </section>
          <div class="layout-tips-warp" :class="{ 'layout-tips-warp-active': getThemeConfig.layout === 'classic' }">
            <div class="layout-tips-box">
              <p class="layout-tips-txt">经典</p>
            </div>
          </div>
        </div>

        <div class="layout-drawer-content-item" @click="onSetLayout('transverse')">
          <section class="el-container is-vertical el-circular" :class="{ 'drawer-layout-active': getThemeConfig.layout === 'transverse' }">
            <header class="el-header" style="height: 10px"></header>
            <section class="el-container">
              <section class="el-container is-vertical">
                <main class="el-main"></main>
              </section>
            </section>
          </section>
          <div class="layout-tips-warp" :class="{ 'layout-tips-warp-active': getThemeConfig.layout === 'transverse' }">
            <div class="layout-tips-box">
              <p class="layout-tips-txt">横向</p>
            </div>
          </div>
        </div>

        <div class="layout-drawer-content-item" @click="onSetLayout('columns')">
          <section class="el-container el-circular" :class="{ 'drawer-layout-active': getThemeConfig.layout === 'columns' }">
            <aside class="el-aside-dark" style="width: 10px"></aside>
            <aside class="el-aside" style="width: 20px"></aside>
            <section class="el-container is-vertical">
              <header class="el-header" style="height: 10px"></header>
              <main class="el-main"></main>
            </section>
          </section>
          <div class="layout-tips-warp" :class="{ 'layout-tips-warp-active': getThemeConfig.layout === 'columns' }">
            <div class="layout-tips-box">
              <p class="layout-tips-txt">分栏</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 全局主题 -->
      <el-divider content-position="left">全局主题</el-divider>
      <div class="layout-settings-item">
        <span class="layout-settings-label">primary 主题色</span>
        <div class="layout-settings-value">
          <el-color-picker v-model="themeConfig.primary" @change="onColorPickerChange" :disabled="themeConfig.isDark" />
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">深色模式</span>
        <div class="layout-settings-value">
          <el-switch v-model="themeConfig.isDark" size="small" @change="onAddDarkChange"></el-switch>
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">组件大小</span>
        <div class="layout-settings-value">
          <el-select v-model="themeConfig.globalComponentSize" style="width: 90px" @change="onComponentSizeChange">
            <el-option label="大型" value="large"></el-option>
            <el-option label="默认" value="default"></el-option>
            <el-option label="小型" value="small"></el-option>
          </el-select>
        </div>
      </div>

      <!-- 顶栏设置 -->
      <el-divider content-position="left">顶栏设置</el-divider>
      <div class="layout-settings-item">
        <span class="layout-settings-label">背景颜色</span>
        <div class="layout-settings-value">
          <el-color-picker v-model="themeConfig.topBar" @change="onBgColorPickerChange('topBar')" :disabled="getThemeConfig.isDark" />
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">文字颜色</span>
        <div class="layout-settings-value">
          <el-color-picker v-model="themeConfig.topBarColor" @change="onBgColorPickerChange('topBarColor')" :disabled="getThemeConfig.isDark" />
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">背景渐变</span>
        <div class="layout-settings-value">
          <el-switch v-model="themeConfig.isTopBarColorGradual" size="small" @change="onTopBarGradualChange"></el-switch>
        </div>
      </div>

      <!-- 菜单设置 -->
      <el-divider content-position="left">菜单设置</el-divider>
      <div class="layout-settings-item">
        <span class="layout-settings-label">背景颜色</span>
        <div class="layout-settings-value">
          <el-color-picker v-model="themeConfig.menuBar" @change="onBgColorPickerChange('menuBar')" :disabled="getThemeConfig.isDark" />
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">文字颜色</span>
        <div class="layout-settings-value">
          <el-color-picker v-model="themeConfig.menuBarColor" @change="onBgColorPickerChange('menuBarColor')" :disabled="getThemeConfig.isDark" />
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">高亮背景</span>
        <div class="layout-settings-value">
          <el-color-picker v-model="themeConfig.menuBarActiveColor" show-alpha @change="onBgColorPickerChange('menuBarActiveColor')" :disabled="getThemeConfig.isDark" />
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">背景渐变</span>
        <div class="layout-settings-value">
          <el-switch v-model="themeConfig.isMenuBarColorGradual" size="small" @change="onMenuBarGradualChange"></el-switch>
        </div>
      </div>

      <!-- 分栏设置 -->
      <el-divider content-position="left" :style="{ opacity: getThemeConfig.layout !== 'columns' ? 0.5 : 1 }">分栏设置</el-divider>
      <div class="layout-settings-item" :style="{ opacity: getThemeConfig.layout !== 'columns' ? 0.5 : 1 }">
        <span class="layout-settings-label">分栏背景</span>
        <div class="layout-settings-value">
          <el-color-picker v-model="themeConfig.columnsMenuBar" @change="onBgColorPickerChange('columnsMenuBar')" :disabled="getThemeConfig.isDark || getThemeConfig.layout !== 'columns'" />
        </div>
      </div>
      <div class="layout-settings-item mt15" :style="{ opacity: getThemeConfig.layout !== 'columns' ? 0.5 : 1 }">
        <span class="layout-settings-label">分栏文字</span>
        <div class="layout-settings-value">
          <el-color-picker v-model="themeConfig.columnsMenuBarColor" @change="onBgColorPickerChange('columnsMenuBarColor')" :disabled="getThemeConfig.isDark || getThemeConfig.layout !== 'columns'" />
        </div>
      </div>
      <div class="layout-settings-item mt15" :style="{ opacity: getThemeConfig.layout !== 'columns' ? 0.5 : 1 }">
        <span class="layout-settings-label">分栏渐变</span>
        <div class="layout-settings-value">
          <el-switch v-model="themeConfig.isColumnsMenuBarColorGradual" size="small" @change="onColumnsMenuBarGradualChange" :disabled="getThemeConfig.isDark || getThemeConfig.layout !== 'columns'"></el-switch>
        </div>
      </div>

      <!-- 界面设置 -->
      <el-divider content-position="left">界面设置</el-divider>
      <div class="layout-settings-item" :style="{ opacity: getThemeConfig.layout === 'transverse' ? 0.5 : 1 }">
        <span class="layout-settings-label">菜单折叠</span>
        <div class="layout-settings-value">
          <el-switch v-model="themeConfig.isCollapse" :disabled="getThemeConfig.layout === 'transverse'" size="small" @change="onThemeConfigChange"></el-switch>
        </div>
      </div>
      <div class="layout-settings-item mt15" :style="{ opacity: getThemeConfig.layout === 'transverse' ? 0.5 : 1 }">
        <span class="layout-settings-label">手风琴模式</span>
        <div class="layout-settings-value">
          <el-switch v-model="themeConfig.isUniqueOpened" :disabled="getThemeConfig.layout === 'transverse'" size="small" @change="setLocalThemeConfig"></el-switch>
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">固定 Header</span>
        <div class="layout-settings-value">
          <el-switch v-model="themeConfig.isFixedHeader" size="small" @change="onIsFixedHeaderChange"></el-switch>
        </div>
      </div>
      <div class="layout-settings-item mt15" :style="{ opacity: getThemeConfig.layout !== 'classic' ? 0.5 : 1 }">
        <span class="layout-settings-label">经典分割菜单</span>
        <div class="layout-settings-value">
          <el-switch v-model="themeConfig.isClassicSplitMenu" :disabled="getThemeConfig.layout !== 'classic'" size="small" @change="setLocalThemeConfig"></el-switch>
        </div>
      </div>

      <!-- 界面显示 -->
      <el-divider content-position="left">界面显示</el-divider>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">显示 Logo</span>
        <div class="layout-settings-value">
          <el-switch v-model="themeConfig.isShowLogo" size="small" @change="onIsShowLogoChange"></el-switch>
        </div>
      </div>
      <div class="layout-settings-item mt15" :style="{ opacity: getThemeConfig.layout === 'classic' || getThemeConfig.layout === 'transverse' ? 0.5 : 1 }">
        <span class="layout-settings-label">显示面包屑</span>
        <div class="layout-settings-value">
          <el-switch v-model="themeConfig.isBreadcrumb" :disabled="getThemeConfig.layout === 'classic' || getThemeConfig.layout === 'transverse'" size="small" @change="setLocalThemeConfig"></el-switch>
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">显示 Tagsview</span>
        <div class="layout-settings-value">
          <el-switch v-model="themeConfig.isTagsview" size="small" @change="setLocalThemeConfig"></el-switch>
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">Tagsview 图标</span>
        <div class="layout-settings-value">
          <el-switch v-model="themeConfig.isTagsviewIcon" size="small" @change="setLocalThemeConfig"></el-switch>
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">缓存 Tagsview</span>
        <div class="layout-settings-value">
          <el-switch v-model="themeConfig.isCacheTagsView" size="small" @change="setLocalThemeConfig"></el-switch>
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">显示 Footer</span>
        <div class="layout-settings-value">
          <el-switch v-model="themeConfig.isFooter" size="small" @change="setLocalThemeConfig"></el-switch>
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">灰色模式</span>
        <div class="layout-settings-value">
          <el-switch v-model="themeConfig.isGrayscale" size="small" @change="onAddFilterChange('grayscale')"></el-switch>
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">色弱模式</span>
        <div class="layout-settings-value">
          <el-switch v-model="themeConfig.isInvert" size="small" @change="onAddFilterChange('invert')"></el-switch>
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">水印</span>
        <div class="layout-settings-value">
          <el-switch v-model="themeConfig.isWartermark" size="small" @change="onWartermarkChange"></el-switch>
        </div>
      </div>

      <!-- 其它设置 -->
      <el-divider content-position="left">其它设置</el-divider>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">页面动画</span>
        <div class="layout-settings-value">
          <el-select v-model="themeConfig.animation" style="width: 90px" @change="setLocalThemeConfig">
            <el-option label="右侧滑入" value="slide-right"></el-option>
            <el-option label="左侧滑入" value="slide-left"></el-option>
            <el-option label="渐变" value="opacitys"></el-option>
          </el-select>
        </div>
      </div>
      <div class="layout-settings-item mt15">
        <span class="layout-settings-label">Tagsview 样式</span>
        <div class="layout-settings-value">
          <el-select v-model="themeConfig.tagsStyle" style="width: 90px" @change="setLocalThemeConfig">
            <el-option label="风格1" value="tags-style-one"></el-option>
            <el-option label="风格4" value="tags-style-four"></el-option>
            <el-option label="风格5" value="tags-style-five"></el-option>
          </el-select>
        </div>
      </div>

      <!-- 恢复默认 -->
      <div class="copy-config">
        <el-alert title="点击" type="warning" :closable="false" show-icon>
          重置所有配置到默认状态
        </el-alert>
        <el-button type="primary" class="copy-config-btn" @click="onResetConfigClick">
          <el-icon class="mr5"><RefreshRight /></el-icon>
          恢复默认
        </el-button>
      </div>
    </el-scrollbar>
  </el-drawer>
</template>

<script setup lang="ts" name="layoutBreadcrumbSettings">
import { computed, nextTick, onMounted, reactive } from 'vue';
import { storeToRefs } from 'pinia';
import { useThemeConfig } from '@/stores/themeConfig';
import { useChangeColor } from '@/utils/theme';
import { Setting, RefreshRight } from '@element-plus/icons-vue';

const storesThemeConfig = useThemeConfig();
const { themeConfig } = storeToRefs(storesThemeConfig);
const { getLightColor, getDarkColor } = useChangeColor();

const state = reactive({
});

const getThemeConfig = computed(() => themeConfig.value);

// 验证颜色值是否为有效的 hex 格式，防止 CSS 注入
const isValidColor = (color: string): boolean => {
  return /^#[0-9A-Fa-f]{6}$/.test(color);
};

// 颜色选择器变化 - 全局主题
const onColorPickerChange = () => {
  if (!getThemeConfig.value.primary) return;
  // 设置 Element Plus 主题色
  document.documentElement.style.setProperty('--el-color-primary-dark-2', `${getDarkColor(getThemeConfig.value.primary, 0.1)}`);
  document.documentElement.style.setProperty('--el-color-primary', getThemeConfig.value.primary);
  // 设置颜色变浅
  for (let i = 1; i <= 9; i++) {
    document.documentElement.style.setProperty(`--el-color-primary-light-${i}`, `${getLightColor(getThemeConfig.value.primary, i / 10)}`);
  }
  // 更新 Mango 主题色
  document.documentElement.style.setProperty('--mango-color-primary', getThemeConfig.value.primary);
  setLocalThemeConfig();
};

// 背景颜色选择器变化
const onBgColorPickerChange = (bg: string) => {
  // 验证 bg 参数为安全的颜色属性名，防止 CSS 注入
  const validBgProps = ['topBar', 'topBarColor', 'menuBar', 'menuBarColor', 'menuBarActiveColor', 'columnsMenuBar', 'columnsMenuBarColor'];
  if (!validBgProps.includes(bg)) return;

  const colorValue = themeConfig.value[bg as keyof typeof themeConfig.value];
  if (!isValidColor(colorValue as string)) return;

  document.documentElement.style.setProperty(`--mango-bg-${bg}`, colorValue as string);
  if (bg === 'menuBar') {
    document.documentElement.style.setProperty(`--mango-bg-menuBar-light-1`, getLightColor(getThemeConfig.value.menuBar, 0.05));
  }
  onTopBarGradualChange();
  onMenuBarGradualChange();
  onColumnsMenuBarGradualChange();
  setLocalThemeConfig();
};

// 顶栏背景渐变
const onTopBarGradualChange = () => {
  setGraduaFun('.layout-navbars-container', getThemeConfig.value.isTopBarColorGradual, getThemeConfig.value.topBar);
};

// 菜单背景渐变
const onMenuBarGradualChange = () => {
  setGraduaFun('.layout-aside', getThemeConfig.value.isMenuBarColorGradual, getThemeConfig.value.menuBar);
};

// 分栏菜单背景渐变
const onColumnsMenuBarGradualChange = () => {
  setGraduaFun('.layout-columns-aside', getThemeConfig.value.isColumnsMenuBarColorGradual, getThemeConfig.value.columnsMenuBar);
};

// 渐变函数
const setGraduaFun = (el: string, bool: boolean, color: string) => {
  // 验证颜色格式防止 CSS 注入
  if (!isValidColor(color)) return;
  setTimeout(() => {
    const els = document.querySelector(el);
    if (!els) return false;
    if (bool) {
      els.setAttribute('style', `background: linear-gradient(to bottom left, ${color}, ${getLightColor(color, 0.6)}) !important;`);
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
  getThemeConfig.value.isFixedHeaderChange = getThemeConfig.value.isFixedHeader ? false : true;
  setLocalThemeConfig();
};

// 显示 Logo 变化
const onIsShowLogoChange = () => {
  getThemeConfig.value.isShowLogoChange = getThemeConfig.value.isShowLogo ? false : true;
  setLocalThemeConfig();
};

// 灰色/色弱模式
const onAddFilterChange = (attr: string) => {
  if (attr === 'grayscale') {
    if (getThemeConfig.value.isGrayscale) getThemeConfig.value.isInvert = false;
  } else {
    if (getThemeConfig.value.isInvert) getThemeConfig.value.isGrayscale = false;
  }
  const cssAttr = attr === 'grayscale' ? `grayscale(${getThemeConfig.value.isGrayscale ? 1 : 0})` : `invert(${getThemeConfig.value.isInvert ? '80%' : '0%'})`;
  document.body.setAttribute('style', `filter: ${cssAttr}`);
  setLocalThemeConfig();
};

// 深色模式
const onAddDarkChange = () => {
  if (getThemeConfig.value.isDark) {
    document.documentElement.setAttribute('data-theme', 'dark');
  } else {
    document.documentElement.setAttribute('data-theme', 'light');
  }
  setLocalThemeConfig();
};

// 水印
const onWartermarkChange = () => {
  setLocalThemeConfig();
};

// 布局切换
const onSetLayout = (layout: 'defaults' | 'classic' | 'transverse' | 'columns') => {
  if (getThemeConfig.value.layout === layout) return false;
  if (layout === 'transverse') getThemeConfig.value.isCollapse = false;
  getThemeConfig.value.layout = layout;
  getThemeConfig.value.isDrawer = false;
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
  getThemeConfig.value.isDrawer = true;
};

// 存储配置到本地
const setLocalThemeConfig = () => {
  localStorage.setItem('themeConfig', JSON.stringify(getThemeConfig.value));
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
      if (getThemeConfig.value.isGrayscale) onAddFilterChange('grayscale');
      // 色弱模式
      if (getThemeConfig.value.isInvert) onAddFilterChange('invert');
      // 深色模式
      if (getThemeConfig.value.isDark) onAddDarkChange();
      // 初始化菜单样式
      initSetStyle();
    }, 100);
  });
});
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
