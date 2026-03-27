import type { ThemeConfigState } from '@/stores/themeConfig';

/**
 * 切换主题
 */
export function setTheme(themeConfig: ThemeConfigState): void {
  const { isDark, primary } = themeConfig;

  // 设置深色模式
  if (isDark) {
    document.documentElement.setAttribute('data-theme', 'dark');
  } else {
    document.documentElement.setAttribute('data-theme', 'light');
  }

  // 设置主题色
  document.documentElement.style.setProperty('--mango-color-primary', primary);
}

/**
 * 切换深色模式
 */
export function toggleDarkMode(isDark: boolean): void {
  if (isDark) {
    document.documentElement.setAttribute('data-theme', 'dark');
  } else {
    document.documentElement.setAttribute('data-theme', 'light');
  }
}

/**
 * 设置主题色
 */
export function setPrimaryColor(color: string): void {
  document.documentElement.style.setProperty('--mango-color-primary', color);
}

/**
 * 获取当前主题配置
 */
export function getCurrentTheme(): { isDark: boolean; primary: string } {
  const theme = document.documentElement.getAttribute('data-theme');
  const primary = getComputedStyle(document.documentElement)
    .getPropertyValue('--mango-color-primary')
    .trim();

  return {
    isDark: theme === 'dark',
    primary: primary || '#2E5CF6',
  };
}
