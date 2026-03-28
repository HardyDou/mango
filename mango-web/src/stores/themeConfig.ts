import { defineStore } from 'pinia';

export const useThemeConfig = defineStore('themeConfig', {
  state: (): { themeConfig: ThemeConfigState['themeConfig'] } => ({
    themeConfig: {
      // 布局配置抽屉
      isDrawer: false,

      // 全局主题
      primary: '#2E5CF6',
      isDark: false,

      // 顶栏设置
      topBar: '#2E5CF6',
      topBarColor: '#FFFFFF',
      isTopBarColorGradual: false,

      // 菜单设置
      menuBar: '#FFFFFF',
      menuBarColor: '#505968',
      menuBarActiveColor: 'rgba(242, 243, 245, 1)',
      isMenuBarColorGradual: false,

      // 分栏设置
      columnsMenuBar: '#545c64',
      columnsMenuBarColor: '#e6e6e6',
      isColumnsMenuBarColorGradual: false,
      isColumnsMenuHoverPreload: false,

      // 界面设置
      isCollapse: false,
      isUniqueOpened: true,
      isFixedHeader: false,
      isFixedHeaderChange: false,
      isClassicSplitMenu: true,
      isLockScreen: false,
      lockScreenTime: 30,

      // 界面显示
      isShowLogo: true,
      isShowLogoChange: false,
      isBreadcrumb: true,
      isTagsview: true,
      isBreadcrumbIcon: false,
      isTagsviewIcon: false,
      isCacheTagsView: true,
      isSortableTagsView: true,
      isShareTagsView: false,
      isFooter: true,
      isGrayscale: false,
      isInvert: false,
      isWartermark: true,
      wartermarkText: 'Mango',

      // 布局切换
      layout: 'classic',

      // 后端控制路由
      isRequestRoutes: false,

      // 语言
      language: 'zh-cn',

      // 组件大小
      size: 'default',

      // 全局 I18n
      globalI18n: 'zh-cn',
      globalComponentSize: 'default',
      globalTitle: import.meta.env.VITE_APP_TITLE || 'Mango',
      footerAuthor: 'Mango',
    },
  }),
  actions: {
    setThemeConfig(data: Partial<ThemeConfigState['themeConfig']> | { themeConfig: Partial<ThemeConfigState['themeConfig']> }) {
      if ('themeConfig' in data) {
        this.themeConfig = { ...this.themeConfig, ...data.themeConfig };
      } else {
        this.themeConfig = { ...this.themeConfig, ...data };
      }
    },
    updateThemeConfig(partial: Partial<ThemeConfigState['themeConfig']>) {
      Object.assign(this.themeConfig, partial);
    },
    toggleCollapse() {
      this.themeConfig.isCollapse = !this.themeConfig.isCollapse;
    },
    toggleDarkMode() {
      this.themeConfig.isDark = !this.themeConfig.isDark;
      document.documentElement.setAttribute(
        'data-theme',
        this.themeConfig.isDark ? 'dark' : 'light'
      );
    },
    setLayout(layout: 'defaults' | 'classic' | 'transverse' | 'columns') {
      this.themeConfig.layout = layout;
    },
    setLanguage(lang: string) {
      this.themeConfig.language = lang;
    },
    setPrimary(color: string) {
      this.themeConfig.primary = color;
      document.documentElement.style.setProperty('--mango-color-primary', color);
    },
  },
  persist: false,
});

export interface ThemeConfigState {
  themeConfig: {
    isDrawer: boolean;
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
    isColumnsMenuHoverPreload: boolean;
    isCollapse: boolean;
    isUniqueOpened: boolean;
    isFixedHeader: boolean;
    isFixedHeaderChange: boolean;
    isClassicSplitMenu: boolean;
    isLockScreen: boolean;
    lockScreenTime: number;
    isShowLogo: boolean;
    isShowLogoChange: boolean;
    isBreadcrumb: boolean;
    isTagsview: boolean;
    isBreadcrumbIcon: boolean;
    isTagsviewIcon: boolean;
    isCacheTagsView: boolean;
    isSortableTagsView: boolean;
    isShareTagsView: boolean;
    isFooter: boolean;
    isGrayscale: boolean;
    isInvert: boolean;
    isWartermark: boolean;
    wartermarkText: string;
    layout: 'defaults' | 'classic' | 'transverse' | 'columns';
    isRequestRoutes: boolean;
    language: string;
    size: 'large' | 'default' | 'small';
    globalI18n: string;
    globalComponentSize: string;
    globalTitle: string;
    footerAuthor: string;
  };
}
