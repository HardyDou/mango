/**
 * 主题初始化工具
 * 在 Vue 首帧渲染前同步恢复 store 状态，避免布局/颜色闪屏
 * 在 main.ts 中 app.use(pinia) 之后、app.mount() 之前调用
 */
import { useThemeStore } from '@/stores/theme';
import { useLayoutStore } from '@/stores/layout';
import { usePreferencesStore } from '@/stores/preferences';
import { Local } from '@/utils/storage';

const STORAGE_KEY = 'themeConfig';

function applyDarkMode(data: Record<string, unknown>) {
  if (data.isDark) {
    document.documentElement.setAttribute('data-theme', 'dark');
    document.documentElement.style.removeProperty('--mango-color-primary');
    document.documentElement.style.removeProperty('--mango-bg-top-bar');
    document.documentElement.style.removeProperty('--mango-bg-menu-bar');
    document.documentElement.style.removeProperty('--mango-bg-columns-menu-bar');
  } else {
    document.documentElement.setAttribute('data-theme', 'light');
    if (data.primary) {
      document.documentElement.style.setProperty('--mango-color-primary', data.primary as string);
    }
    if (data.topBar) {
      document.documentElement.style.setProperty('--mango-bg-top-bar', data.topBar as string);
    }
    if (data.menuBar) {
      document.documentElement.style.setProperty('--mango-bg-menu-bar', data.menuBar as string);
    }
    if (data.columnsMenuBar) {
      document.documentElement.style.setProperty('--mango-bg-columns-menu-bar', data.columnsMenuBar as string);
    }
  }

  if (data.isGrayscale) {
    document.body.setAttribute('style', 'filter: grayscale(1)');
  } else if (data.isInvert) {
    document.body.setAttribute('style', 'filter: invert(80%)');
  }

  if (data.animation) {
    document.body.setAttribute('data-animation', data.animation as string);
  }
}

export function initThemeBeforeRender(): void {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (!stored) return;
    const data = JSON.parse(stored) as Record<string, unknown>;

    const themeStore = useThemeStore();
    const layoutStore = useLayoutStore();
    const preferencesStore = usePreferencesStore();

    // 恢复 themeStore
    const themeKeys = [
      'primary', 'isDark', 'topBar', 'topBarColor', 'isTopBarColorGradual',
      'menuBar', 'menuBarColor', 'menuBarActiveColor', 'isMenuBarColorGradual',
      'columnsMenuBar', 'columnsMenuBarColor', 'isColumnsMenuBarColorGradual',
    ] as const;
    themeKeys.forEach((key) => {
      if (data[key] !== undefined) {
        (themeStore.$state as Record<string, unknown>)[key] = data[key];
      }
    });

    // 恢复 layoutStore
    const layoutKeys = [
      'layout', 'isCollapse', 'isFixedHeader', 'isFixedHeaderChange',
      'isClassicSplitMenu', 'isMobileMenuOpen', 'isUniqueOpened',
      'isShowLogo', 'isShowLogoChange', 'isBreadcrumb', 'isBreadcrumbIcon',
      'isTagsview', 'isTagsviewIcon', 'isCacheTagsView', 'isSortableTagsView',
      'isShareTagsView', 'isFooter', 'columnsAsideStyle', 'columnsAsideLayout',
      'isColumnsMenuHoverPreload',
    ] as const;
    layoutKeys.forEach((key) => {
      if (data[key] !== undefined) {
        (layoutStore.$state as Record<string, unknown>)[key] = data[key];
      }
    });

    // 恢复 preferencesStore
    const prefKeys = [
      'isDrawer', 'isLockScreen', 'lockScreenTime', 'isGrayscale', 'isInvert',
      'isWartermark', 'wartermarkText', 'tagsStyle', 'animation',
      'isRequestRoutes', 'language', 'size', 'globalI18n', 'globalComponentSize',
      'globalTitle', 'footerAuthor',
    ] as const;
    prefKeys.forEach((key) => {
      if (data[key] !== undefined) {
        (preferencesStore.$state as Record<string, unknown>)[key] = data[key];
      }
    });

    // 应用 CSS 变量
    applyDarkMode(data);

    // 标记布局已恢复，layout/index.vue 的 onBeforeMount 会检查此标记并跳过 onLayoutResize
    Local.set('layoutRestored', 'true');
  } catch {
    // ignore
  }
}
