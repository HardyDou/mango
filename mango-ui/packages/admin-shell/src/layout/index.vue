<template>
  <component :is="layouts[layoutStore.layout]" />
</template>

<script setup lang="ts" name="layout">
import { onBeforeMount, onUnmounted, onMounted, watch } from 'vue';
import { useThemeStore } from '../stores/theme';
import { useLayoutStore } from '../stores/layout';
import { Local } from '@mango/common/utils/storage';
import { mittBus } from '@mango/common/utils/mitt';
import { useChangeColor } from '@mango/common/utils/theme';
import LayoutMainDefaults from './main/defaults.vue';
import LayoutMainClassic from './main/classic.vue';
import LayoutMainTransverse from './main/transverse.vue';
import LayoutMainColumns from './main/columns.vue';

const layouts: Record<string, any> = {
  defaults: LayoutMainDefaults,
  classic: LayoutMainClassic,
  transverse: LayoutMainTransverse,
  columns: LayoutMainColumns,
};

const themeStore = useThemeStore();
const layoutStore = useLayoutStore();
const { getDarkColor, getLightColor } = useChangeColor();

// 验证颜色值是否为有效的 hex 格式
const isValidColor = (color: string): boolean => {
  return /^#[0-9A-Fa-f]{6}$/.test(color);
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

// 初始化主题颜色
const initTheme = () => {
  applyPrimaryColor(themeStore.primary);
  applyBgColor(themeStore.topBar, '--mango-bg-top-bar');
  applyBgColor(themeStore.menuBar, '--mango-bg-menu-bar');
  applyBgColor(themeStore.columnsMenuBar, '--mango-bg-columns-menu-bar');
};

// 监听主题颜色变化
watch(
  () => themeStore.primary,
  (newPrimary) => {
    applyPrimaryColor(newPrimary);
  }
);

watch(
  () => themeStore.topBar,
  (newTopBar) => {
    applyBgColor(newTopBar, '--mango-bg-top-bar');
  }
);

watch(
  () => themeStore.menuBar,
  (newMenuBar) => {
    applyBgColor(newMenuBar, '--mango-bg-menu-bar');
    document.documentElement.style.setProperty('--mango-bg-menuBar-light-1', getLightColor(newMenuBar, 0.05));
  }
);

watch(
  () => themeStore.columnsMenuBar,
  (newColumnsMenuBar) => {
    applyBgColor(newColumnsMenuBar, '--mango-bg-columns-menu-bar');
  }
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
    return;
  }
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
