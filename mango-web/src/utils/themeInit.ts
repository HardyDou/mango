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

/**
 * 验证是否为有效的 CSS 颜色值
 */
function isValidColor(value: unknown): value is string {
  if (typeof value !== 'string') return false;
  // 支持 hex, rgb, rgba, hsl, hsla 格式
  return /^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$/.test(value)
    || /^rgba?\(/.test(value)
    || /^hsla?\(/.test(value);
}

/**
 * 验证是否为有效的 CSS 变量名
 */
function isValidCssVarName(value: unknown): value is string {
  if (typeof value !== 'string') return false;
  return /^[a-zA-Z_][a-zA-Z0-9_-]*$/.test(value);
}

/**
 * 验证是否为安全的布尔值或字符串
 */
function isSafeValue(value: unknown): boolean {
  if (typeof value === 'boolean') return true;
  if (typeof value === 'string') {
    // 限制字符串长度防止 DoS
    return value.length <= 200;
  }
  return false;
}

function applyDarkMode(data: Record<string, unknown>) {
  // 验证 isDark
  const isDark = data.isDark === true || data.isDark === 'true';

  if (isDark) {
    document.documentElement.setAttribute('data-theme', 'dark');
    document.documentElement.style.removeProperty('--mango-color-primary');
    document.documentElement.style.removeProperty('--mango-bg-top-bar');
    document.documentElement.style.removeProperty('--mango-bg-menu-bar');
    document.documentElement.style.removeProperty('--mango-bg-columns-menu-bar');
  } else {
    document.documentElement.setAttribute('data-theme', 'light');
    // 只应用有效颜色值
    if (isValidColor(data.primary)) {
      document.documentElement.style.setProperty('--mango-color-primary', data.primary as string);
    }
    if (isValidColor(data.topBar)) {
      document.documentElement.style.setProperty('--mango-bg-top-bar', data.topBar as string);
    }
    if (isValidColor(data.menuBar)) {
      document.documentElement.style.setProperty('--mango-bg-menu-bar', data.menuBar as string);
    }
    if (isValidColor(data.columnsMenuBar)) {
      document.documentElement.style.setProperty('--mango-bg-columns-menu-bar', data.columnsMenuBar as string);
    }
  }

  // 验证 filter 值
  if (data.isGrayscale === true || data.isGrayscale === 'true') {
    document.body.setAttribute('style', 'filter: grayscale(1)');
  } else if (data.isInvert === true || data.isInvert === 'true') {
    document.body.setAttribute('style', 'filter: invert(80%)');
  }

  // 验证 animation 值
  if (typeof data.animation === 'string' && isValidCssVarName(data.animation)) {
    document.body.setAttribute('data-animation', data.animation);
  }
}

export function initThemeBeforeRender(): void {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (!stored) return;

    // 验证存储数据格式
    if (typeof stored !== 'string' || stored.length > 10000) {
      console.warn('[themeInit] Invalid theme storage data');
      return;
    }

    const data = JSON.parse(stored) as Record<string, unknown>;

    // 验证解析结果
    if (!data || typeof data !== 'object') {
      console.warn('[themeInit] Theme data is not an object');
      return;
    }

    const themeStore = useThemeStore();
    const layoutStore = useLayoutStore();
    const preferencesStore = usePreferencesStore();

    // 恢复 themeStore - 只应用白名单键且值安全
    const themeKeys = [
      'primary', 'isDark', 'topBar', 'topBarColor', 'isTopBarColorGradual',
      'menuBar', 'menuBarColor', 'menuBarActiveColor', 'isMenuBarColorGradual',
      'columnsMenuBar', 'columnsMenuBarColor', 'isColumnsMenuBarColorGradual',
    ] as const;
    themeKeys.forEach((key) => {
      if (data[key] !== undefined && isSafeValue(data[key])) {
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
      if (data[key] !== undefined && isSafeValue(data[key])) {
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
      if (data[key] !== undefined && isSafeValue(data[key])) {
        (preferencesStore.$state as Record<string, unknown>)[key] = data[key];
      }
    });

    // 应用 CSS 变量
    applyDarkMode(data);

    // 标记布局已恢复，layout/index.vue 的 onBeforeMount 会检查此标记并跳过 onLayoutResize
    Local.set('layoutRestored', 'true');
  } catch (e) {
    // 数据损坏时静默恢复默认状态，不影响应用启动
    console.warn('[themeInit] Failed to restore theme:', e);
  }
}
