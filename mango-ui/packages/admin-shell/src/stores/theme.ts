import { defineStore } from 'pinia';

export interface ThemeState {
  primary: string;
  isDark: boolean;
  topBar: string;
  topBarColor: string;
  isTopBarColorGradual: boolean;
  menuBar: string;
  menuBarColor: string;
  menuBarActiveColor: string;
  isMenuBarColorGradual: boolean;
  columnsMenuBar: string;
  columnsMenuBarColor: string;
  isColumnsMenuBarColorGradual: boolean;
}

const themeColorVars: Array<[string, keyof ThemeState]> = [
  ['--mango-color-primary', 'primary'],
  ['--mango-bg-top-bar', 'topBar'],
  ['--mango-color-top-bar', 'topBarColor'],
  ['--mango-bg-menu-bar', 'menuBar'],
  ['--mango-color-menu-bar', 'menuBarColor'],
  ['--mango-color-menu-active-bg', 'menuBarActiveColor'],
  ['--mango-bg-columns-menu-bar', 'columnsMenuBar'],
  ['--mango-color-columns-menu-bar', 'columnsMenuBarColor'],
];

/**
 * 同步主题状态到 CSS 变量
 * 当状态变化时，确保 DOM 与状态同步
 */
function syncThemeToDOM(state: ThemeState) {
  if (state.isDark) {
    document.documentElement.setAttribute('data-theme', 'dark');
    themeColorVars.forEach(([name]) => {
      document.documentElement.style.removeProperty(name);
    });
  } else {
    document.documentElement.setAttribute('data-theme', 'light');
    themeColorVars.forEach(([name, key]) => {
      document.documentElement.style.setProperty(name, String(state[key]));
    });
  }
}

export const useThemeStore = defineStore('theme', {
  state: (): ThemeState => ({
    primary: '#2E5CF6',
    isDark: false,
    topBar: '#2E5CF6',
    topBarColor: '#FFFFFF',
    isTopBarColorGradual: false,
    menuBar: '#FFFFFF',
    menuBarColor: '#505968',
    menuBarActiveColor: 'rgba(242, 243, 245, 1)',
    isMenuBarColorGradual: false,
    columnsMenuBar: '#545c64',
    columnsMenuBarColor: '#e6e6e6',
    isColumnsMenuBarColorGradual: false,
  }),
  actions: {
    setPrimary(color: string) {
      this.primary = color;
      document.documentElement.style.setProperty('--mango-color-primary', color);
    },
    toggleDarkMode() {
      this.isDark = !this.isDark;
      syncThemeToDOM(this.$state);
    },
    setTopBar(color: string) {
      this.topBar = color;
      document.documentElement.style.setProperty('--mango-bg-top-bar', color);
    },
    setTopBarColor(color: string) {
      this.topBarColor = color;
      document.documentElement.style.setProperty('--mango-color-top-bar', color);
    },
    setMenuBar(color: string) {
      this.menuBar = color;
      document.documentElement.style.setProperty('--mango-bg-menu-bar', color);
    },
    setMenuBarColor(color: string) {
      this.menuBarColor = color;
      document.documentElement.style.setProperty('--mango-color-menu-bar', color);
    },
    setMenuBarActiveColor(color: string) {
      this.menuBarActiveColor = color;
      document.documentElement.style.setProperty('--mango-color-menu-active-bg', color);
    },
    setColumnsMenuBar(color: string) {
      this.columnsMenuBar = color;
      document.documentElement.style.setProperty('--mango-bg-columns-menu-bar', color);
    },
    setColumnsMenuBarColor(color: string) {
      this.columnsMenuBarColor = color;
      document.documentElement.style.setProperty('--mango-color-columns-menu-bar', color);
    },
  },
  persist: false,
});
