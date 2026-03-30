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
      document.documentElement.setAttribute('data-theme', this.isDark ? 'dark' : 'light');
      if (this.isDark) {
        document.documentElement.style.removeProperty('--mango-color-primary');
        document.documentElement.style.removeProperty('--mango-bg-top-bar');
        document.documentElement.style.removeProperty('--mango-bg-menu-bar');
        document.documentElement.style.removeProperty('--mango-bg-columns-menu-bar');
      } else {
        document.documentElement.style.setProperty('--mango-color-primary', this.primary);
        document.documentElement.style.setProperty('--mango-bg-top-bar', this.topBar);
        document.documentElement.style.setProperty('--mango-bg-menu-bar', this.menuBar);
        document.documentElement.style.setProperty('--mango-bg-columns-menu-bar', this.columnsMenuBar);
      }
    },
    setTopBar(color: string) {
      this.topBar = color;
      document.documentElement.style.setProperty('--mango-bg-top-bar', color);
    },
    setMenuBar(color: string) {
      this.menuBar = color;
      document.documentElement.style.setProperty('--mango-bg-menu-bar', color);
    },
    setColumnsMenuBar(color: string) {
      this.columnsMenuBar = color;
      document.documentElement.style.setProperty('--mango-bg-columns-menu-bar', color);
    },
  },
  persist: false,
});
