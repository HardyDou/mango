<template>
  <component :is="layouts[themeConfig.layout]" />
</template>

<script setup lang="ts" name="layout">
import { onBeforeMount, onUnmounted, onMounted, watch, defineAsyncComponent } from 'vue';
import { storeToRefs } from 'pinia';
import { useThemeConfig } from '@/stores/themeConfig';
import { Local } from '@/utils/storage';
import { mittBus } from '@/utils/mitt';
import { useChangeColor } from '@/utils/theme';

const layouts: Record<string, any> = {
  defaults: defineAsyncComponent(() => import('./main/defaults.vue')),
  classic: defineAsyncComponent(() => import('./main/classic.vue')),
  transverse: defineAsyncComponent(() => import('./main/transverse.vue')),
  columns: defineAsyncComponent(() => import('./main/columns.vue')),
};

const storesThemeConfig = useThemeConfig();
const { themeConfig } = storeToRefs(storesThemeConfig);
const { getDarkColor, getLightColor } = useChangeColor();

// 验证颜色值是否为有效的 hex 格式
const isValidColor = (color: string): boolean => {
  return /^#[0-9A-Fa-f]{6}$/.test(color);
};

// 应用主题颜色到 CSS 变量
const applyThemeColor = (color: string, variable: string) => {
  if (!isValidColor(color)) return;
  document.documentElement.style.setProperty(variable, color);
};

// 应用 primary 主题色
const applyPrimaryColor = (primary: string) => {
  if (!isValidColor(primary)) return;
  // Element Plus 主题色
  document.documentElement.style.setProperty('--el-color-primary', primary);
  document.documentElement.style.setProperty('--el-color-primary-dark-2', getDarkColor(primary, 0.1));
  for (let i = 1; i <= 9; i++) {
    document.documentElement.style.setProperty(`--el-color-primary-light-${i}`, getLightColor(primary, i / 10));
  }
  // Mango 主题色
  document.documentElement.style.setProperty('--mango-color-primary', primary);
};

// 应用背景颜色
const applyBgColor = (bg: string, variable: string) => {
  if (!isValidColor(bg)) return;
  document.documentElement.style.setProperty(variable, bg);
};

// 初始化主题颜色
const initTheme = () => {
  const { primary, topBar, menuBar, columnsMenuBar } = themeConfig.value;
  applyPrimaryColor(primary);
  applyBgColor(topBar, '--mango-bg-top-bar');
  applyBgColor(menuBar, '--mango-bg-menu-bar');
  applyBgColor(columnsMenuBar, '--mango-bg-columns-menu-bar');
};

// 监听主题颜色变化
watch(
  () => themeConfig.value.primary,
  (newPrimary) => {
    applyPrimaryColor(newPrimary);
  }
);

watch(
  () => themeConfig.value.topBar,
  (newTopBar) => {
    applyBgColor(newTopBar, '--mango-bg-top-bar');
  }
);

watch(
  () => themeConfig.value.menuBar,
  (newMenuBar) => {
    applyBgColor(newMenuBar, '--mango-bg-menu-bar');
    // menuBar 渐变需要同时设置 light-1
    document.documentElement.style.setProperty('--mango-bg-menuBar-light-1', getLightColor(newMenuBar, 0.05));
  }
);

watch(
  () => themeConfig.value.columnsMenuBar,
  (newColumnsMenuBar) => {
    applyBgColor(newColumnsMenuBar, '--mango-bg-columns-menu-bar');
  }
);

// 窗口大小改变时(适配移动端) - 硬断点 1000px 保留
const onLayoutResize = () => {
  if (!Local.get('oldLayout')) Local.set('oldLayout', themeConfig.value.layout);
  const clientWidth = document.body.clientWidth;
  if (clientWidth < 1000) {
    // 移动端：确保侧边栏展开（不折叠），关闭移动菜单，切换到默认布局
    themeConfig.value.isCollapse = false;
    themeConfig.value.isMobileMenuOpen = false;
    // 记住当前布局，切换到 defaults 布局（与 pigx-ui 一致）
    const currentLayout = themeConfig.value.layout;
    if (currentLayout !== 'defaults') {
      Local.set('oldLayout', currentLayout);
      themeConfig.value.layout = 'defaults';
    }
    mittBus.emit('layoutMobileResize', {
      isMobile: true,
      windowWidth: clientWidth,
      layout: 'defaults',
    });
  } else {
    // PC端：恢复之前的布局
    const oldLayout = Local.get('oldLayout');
    if (oldLayout && oldLayout !== 'defaults' && themeConfig.value.layout === 'defaults') {
      themeConfig.value.layout = oldLayout as 'defaults' | 'classic' | 'transverse' | 'columns';
    }
    mittBus.emit('layoutMobileResize', {
      isMobile: false,
      windowWidth: clientWidth,
      layout: themeConfig.value.layout,
    });
  }
};

onBeforeMount(() => {
  onLayoutResize();
  window.addEventListener('resize', onLayoutResize);
});

onMounted(() => {
  initTheme();
});

onUnmounted(() => {
  window.removeEventListener('resize', onLayoutResize);
});
</script>
